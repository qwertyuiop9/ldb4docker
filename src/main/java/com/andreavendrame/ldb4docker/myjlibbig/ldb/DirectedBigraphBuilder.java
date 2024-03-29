package com.andreavendrame.ldb4docker.myjlibbig.ldb;

import com.andreavendrame.ldb4docker.myjlibbig.Interface;
import com.andreavendrame.ldb4docker.myjlibbig.Owned;
import com.andreavendrame.ldb4docker.myjlibbig.Owner;
import com.andreavendrame.ldb4docker.myjlibbig.exceptions.IncompatibleInterfaceException;
import com.andreavendrame.ldb4docker.myjlibbig.exceptions.IncompatibleSignatureException;
import com.andreavendrame.ldb4docker.myjlibbig.exceptions.NameClashException;
import com.andreavendrame.ldb4docker.myjlibbig.exceptions.UnexpectedOwnerException;

import java.util.*;

import static com.andreavendrame.ldb4docker.myjlibbig.ldb.DirectedBigraph.Interface.intersectNames;

/**
 * This class provides services for the creation and manipulation of bigraphs
 * since instances of {@link DirectedBigraph} are immutable.
 * <p>
 * For efficiency reasons immutability can be relaxed by the user (cf.
 * {@link #outerCompose(DirectedBigraph, boolean)}) by allowing the reuse of (parts) of
 * the arguments. Notice that, if not handled properly, the reuse of bigraphs
 * can cause inconsistencies e.g. as a consequence of the reuse of a bigraphs
 * held by a rewriting rule as its redex or reactum.
 */
final public class DirectedBigraphBuilder implements
        com.andreavendrame.ldb4docker.myjlibbig.DirectedBigraphBuilder<DirectedControl>, Cloneable {

    private final static boolean DEBUG_CONSISTENCY_CHECK = Boolean.getBoolean("it.uniud.mads.jlibbig.consistency")
            || Boolean.getBoolean("it.uniud.mads.jlibbig.consistency.bigraphops");

    private DirectedBigraph directedBigraph;
    private boolean closed = false;

    /**
     * Initially the builder describes an empty bigraph for the given signature.
     *
     * @param directedSignature the signature to be used.
     */
    public DirectedBigraphBuilder(DirectedSignature directedSignature) {
        this.directedBigraph = DirectedBigraph.makeEmpty(directedSignature);
    }

    /**
     * Creates a builder from (a copy of) the given bigraph.
     *
     * @param directedBigraph the bigraph describing the starting state.
     */
    public DirectedBigraphBuilder(DirectedBigraph directedBigraph) {
        this(directedBigraph, false);
    }

    /**
     * @param directedBigraph the directed bigraph describing the starting state.
     * @param reuse           whether the argument can be reused as it is or should be
     *                        cloned.
     */
    DirectedBigraphBuilder(DirectedBigraph directedBigraph, boolean reuse) {
        if (directedBigraph == null)
            throw new IllegalArgumentException("Argument can not be null.");
        if (!this.directedBigraph.isConsistent())
            throw new IllegalArgumentException("Inconsistent bigraph.");
        this.directedBigraph = (reuse) ? this.directedBigraph.setOwner(this) : this.directedBigraph.clone(this);
    }

    private static void clearOwnedCollection(
            Collection<? extends EditableOwned> col) {
        for (EditableOwned i : col) {
            i.setOwner(null);
        }
        col.clear();
    }

    private static void clearChildCollection(Collection<? extends EditableChild> col) {
        for (EditableChild i : col) {
            i.setParent(null);
        }
        col.clear();
    }

    private static void clearOuterInterface(DirectedBigraph.Interface<EditableOuterName, EditableInnerName> i) {
        i.names.clear();
    }

    private static void clearInnerInterface(DirectedBigraph.Interface<EditableInnerName, EditableOuterName> i) {
        i.names.clear();
    }

    @Override
    public String toString() {
        assertOpen();
        return this.directedBigraph.toString();
    }

    /**
     * Returns the bigraph build so far. The new bigraph is independent from any
     * other operation done by the builder.
     *
     * @return a bigraph.
     */
    public DirectedBigraph makeBigraph() {
        return makeBigraph(false);
    }

    /**
     * Return the bigraph build so far. The new bigraph is independent from any
     * other operation done by the builder. The new bigraph can be created more
     * efficiently if the builder is closed by the same method call.
     *
     * @param close disables the builder to perform any other operation.
     * @return a bigraph.
     */
    public DirectedBigraph makeBigraph(boolean close) {
        assertOpen();
        assertConsistency();
        DirectedBigraph b;
        if (close) {
            b = this.directedBigraph.setOwner(this.directedBigraph);
            closed = true;
        } else {
            b = this.directedBigraph.clone();
            if (DEBUG_CONSISTENCY_CHECK && !b.isConsistent())
                throw new RuntimeException("Inconsistent bigraph.");
        }
        return b;
    }

    /**
     * A closed builder can not perform any operation.
     *
     * @return a boolean indicating whether the builder is disabled to perform
     * any operation.
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Asserts that the builder is not closed and operations on it are allowd.
     * If called on a closed builder, an exception is trown.
     */
    private void assertOpen() throws UnsupportedOperationException {
        if (this.closed)
            throw new UnsupportedOperationException("The operation is not supported by a closed BigraphBuilder");
    }

    @Override
    public DirectedBigraphBuilder clone() {
        assertOpen();
        DirectedBigraphBuilder bb = new DirectedBigraphBuilder(this.directedBigraph.getSignature());
        bb.directedBigraph = this.directedBigraph.clone(bb);
        return bb;
    }

    @Override
    public DirectedSignature getSignature() {
        assertOpen();
        return this.directedBigraph.getSignature();
    }

    @Override
    public boolean isEmpty() {
        assertOpen();
        return this.directedBigraph.isEmpty();
    }

    @Override
    public boolean isGround() {
        assertOpen();
        return this.directedBigraph.isGround();
    }

    @Override
    public List<? extends Root> getRoots() {
        assertOpen();
        return this.directedBigraph.getRoots();
    }

    @Override
    public List<? extends Site> getSites() {
        assertOpen();
        return this.directedBigraph.getSites();
    }

    @Override
    public Interface getOuterInterface() {
        return this.directedBigraph.outers;
    }

    @Override
    public Interface getInnerInterface() {
        return this.directedBigraph.inners;
    }

    public boolean containsOuterName(String name) {
        assertOpen();
        return this.directedBigraph.outers.getAsc().containsKey(name) || this.directedBigraph.inners.getDesc().containsKey(name);
    }

    public boolean containsInnerName(String name) {
        assertOpen();
        return this.directedBigraph.inners.getAsc().containsKey(name) || this.directedBigraph.outers.getDesc().containsKey(name);
    }

    @Override
    public Collection<? extends Node> getNodes() {
        assertOpen();
        return this.directedBigraph.getNodes();
    }

    @Override
    public Collection<? extends Edge> getEdges() {
        assertOpen();
        return this.directedBigraph.getEdges();
    }

    /**
     * Adds a root to the current bigraph.
     *
     * @return the new root.
     */
    public Root addRoot() {
        assertOpen();
        EditableRoot r = new EditableRoot();
        r.setOwner(this);
        this.directedBigraph.roots.add(r);
        this.directedBigraph.outers.addPair(new InterfacePair<>(new HashSet<EditableOuterName>(), new HashSet<EditableInnerName>()));
        assertConsistency();
        return r;
    }

    /**
     * Adds a root to the current bigraph.
     *
     * @param index the region to be assigned to the root.
     * @return the new root.
     */
    public Root addRoot(int index) {
        assertOpen();
        EditableRoot r = new EditableRoot();
        r.setOwner(this);
        this.directedBigraph.roots.add(index, r);
        this.directedBigraph.outers.addPair(new InterfacePair<>(new HashSet<EditableOuterName>(), new HashSet<EditableInnerName>()));
        assertConsistency();
        return r;
    }

    /**
     * Adds a site to the current bigraph.
     *
     * @param parent the parent of the site.
     * @return the new site.
     */
    public Site addSite(Parent parent) {
        if (parent == null)
            throw new IllegalArgumentException("Argument can not be null.");
        assertOpen();
        assertOwner(parent, "Parent");
        EditableSite s = new EditableSite((EditableParent) parent);
        this.directedBigraph.sites.add(s);
        this.directedBigraph.inners.addPair(new InterfacePair<>(new HashSet<EditableInnerName>(), new HashSet<EditableOuterName>()));
        assertConsistency();
        return s;
    }

    /**
     * Adds a new node to the bigraph.
     *
     * @param controlName the control's name of the new node.
     * @param parent      the father of the new node, in the place graph.
     * @return the new node.
     */
    public Node addNode(String controlName, Parent parent) {
        return addNode(controlName, parent, new LinkedList<Handle>());
    }

    /**
     * Adds a new node to the bigraph.
     *
     * @param controlName the control's name of the new node.
     * @param parent      the father of the new node, in the place graph.
     * @param handles     Handles (outernames or edges) that will be linked to new
     *                    node's ports (an array of Handle).
     * @return the new node.
     */
    public Node addNode(String controlName, Parent parent, Handle... handles) {
        if (controlName == null)
            throw new IllegalArgumentException("Control name can not be null.");
        if (parent == null)
            throw new IllegalArgumentException("Parent can not be null.");
        assertOpen();
        DirectedControl directedControl = this.directedBigraph.getSignature().getByName(controlName);
        if (directedControl == null)
            throw new IllegalArgumentException("Control should be in the signature.");
        assertOwner(parent, "Parent");
        EditableHandle[] editableHandles = new EditableHandle[directedControl.getArityIn()];
        for (int i = 0; i < editableHandles.length; i++) {
            EditableHandle editableHandle = null;
            if (i < handles.length) {
                editableHandle = (EditableHandle) handles[i];
            }
            if (editableHandle != null) {
                assertOwner(editableHandle, "Handle");
            } else {
                EditableEdge editableEdge = new EditableEdge(this);
                this.directedBigraph.onEdgeAdded(editableEdge);
                editableHandle = editableEdge;
            }
            editableHandles[i] = editableHandle;
        }
        EditableNode editableNode = new EditableNode(directedControl, (EditableParent) parent, editableHandles);
        this.directedBigraph.onNodeAdded(editableNode);
        System.out.format("Name of the node added: '%s'\n", editableNode.getName());
        assertConsistency();
        return editableNode;
    }

    /**
     * Adds a new node to the bigraph.
     *
     * @param controlName the control's name of the new node.
     * @param parent      the father of the new node, in the place graph.
     * @param handles     list of handles (outernames or edges) that will be linked to
     *                    new node's ports.
     * @return the new node.
     */
    public Node addNode(String controlName, Parent parent, List<Handle> handles) {
        System.out.format("\nMethod 'addNode': handles list size %d\n", handles.size());
        System.out.format("---------- Control name %s\n", controlName);
        if (controlName == null)
            throw new IllegalArgumentException("Control name can not be null.");
        if (parent == null)
            throw new IllegalArgumentException("Parent can not be null.");
        assertOpen();
        DirectedControl directedControl = this.directedBigraph.getSignature().getByName(controlName);
        if (directedControl == null)
            throw new IllegalArgumentException("Control should be in the signature.");
        assertOwner(parent, "Parent");
        int arityIn = directedControl.getArityIn();
        System.out.format("---------- Arity in %d\n", arityIn);
        List<EditableHandle> editableHandles = new ArrayList<>(arityIn);
        Iterator<Handle> handleIterator = (handles == null) ? null : handles.iterator();
        System.out.format("---------- Empty iterator %b\n", !handleIterator.hasNext());
        for (int i = 0; i < arityIn; i++) {
            EditableHandle editableHandle = null;
            if (handleIterator != null && handleIterator.hasNext()) {
                editableHandle = (EditableHandle) handleIterator.next();
            }
            if (editableHandle != null) {
                assertOwner(editableHandle, "Handle");
            } else {
                EditableEdge editableEdge = new EditableEdge(this);
                this.directedBigraph.onEdgeAdded(editableEdge);
                editableHandle = editableEdge;
            }
            editableHandles.add(editableHandle);
            System.out.format("---------- Handle of type port: %b; point %b, edge %b, innerName %b, outerName %b, handle %b\n",
                    editableHandle.isPort(), editableHandle.isPoint(), editableHandle.isEdge(),
                    editableHandle.isInnerName(), editableHandle.isOuterName(), editableHandle.isHandle());
        }
        EditableNode editableNode = new EditableNode(directedControl, (EditableParent) parent, editableHandles);
        this.directedBigraph.onNodeAdded(editableNode);
        System.out.format("---------- Name of the node added: '%s'\n", editableNode.getName());
        assertConsistency();
        return editableNode;
    }

    /**
     * Adds a fresh outer name to the current bigraph's outer interface at the specified locality.
     *
     * @param locality the locality where to look.
     * @return the new outer name.
     */
    public OuterName addAscNameOuterInterface(int locality) {
        return addAscNameOuterInterface(locality, new EditableOuterName());
    }

    /**
     * Add an outer name to the current bigraph's outer interface at the specified locality.
     *
     * @param locality the locality where to look.
     * @param name     the name of the new outer name.
     * @return the new outer name.
     */
    public OuterName addAscNameOuterInterface(int locality, String name) {
        if (locality < 0 || locality >= this.directedBigraph.outers.getWidth()) {
            throw new IndexOutOfBoundsException("Locality '" + locality + "' is not valid.");
        }
        if (name == null || name.length() == 0)
            throw new IllegalArgumentException("Argument can not be null.");
        return addAscNameOuterInterface(locality, new EditableOuterName(name));
    }

    /**
     * Adds an outer name to the current bigraph's outer interface at the specified locality.
     *
     * @param locality the locality where to look.
     * @param name     the outer name that will be added.
     * @return new outer name.
     */
    private OuterName addAscNameOuterInterface(int locality, EditableOuterName name) {
        assertOpen();
        if (this.directedBigraph.outers.getAsc().containsKey(locality + "#" + name.getName())) {
            throw new IllegalArgumentException("Name '" + name.getName() + "' already present.");
        }
        name.setOwner(this);
        this.directedBigraph.outers.addAsc(locality, name);
        assertConsistency();
        return name;
    }

    /**
     * Adds a fresh outer name to the current bigraph's inner interface at the specified locality.
     *
     * @param locality the locality where to look.
     * @return the new outer name.
     */
    public OuterName addDescNameInnerInterface(int locality) {
        return addDescNameInnerInterface(locality, new EditableOuterName());
    }

    /**
     * Add an outer name to the current bigraph's inner interface at the specified locality.
     *
     * @param locality the locality where to look.
     * @param name     the name of the new outer name.
     * @return the new outer name.
     */
    public OuterName addDescNameInnerInterface(int locality, String name) {
        if (locality < 0 || locality >= this.directedBigraph.inners.getWidth()) {
            throw new IndexOutOfBoundsException("Locality '" + locality + "' is not valid.");
        }
        if (name == null || name.length() == 0)
            throw new IllegalArgumentException("Argument can not be null.");
        return addDescNameInnerInterface(locality, new EditableOuterName(name));
    }

    /**
     * Adds an outer name to the current bigraph's inner interface at the specified locality.
     *
     * @param locality the locality where to look.
     * @param name     the outer name that will be added.
     * @return new outer name.
     */
    private OuterName addDescNameInnerInterface(int locality, EditableOuterName name) {
        assertOpen();
        if (this.directedBigraph.inners.getDesc().containsKey(locality + "#" + name.getName())) {
            throw new IllegalArgumentException("Name '" + name.getName() + "' already present.");
        }
        name.setOwner(this);
        this.directedBigraph.inners.addDesc(locality, name);
        assertConsistency();
        return name;
    }

    /**
     * Adds a fresh inner name to the current bigraph's outer interface at the specified locality. The name will be the only point of a fresh edge.
     *
     * @param locality the locality where to look.
     * @return the new inner name.
     */
    public InnerName addDescNameOuterInterface(int locality) {
        EditableEdge e = new EditableEdge(this);
        this.directedBigraph.onEdgeAdded(e);
        return addDescNameOuterInterface(locality, new EditableInnerName(), e);
    }

    /**
     * Adds a new inner name to the current bigraph's outer interface at the specified locality.
     *
     * @param locality the locality where to look.
     * @param handle   the outer name or the edge linking the new inner name.
     * @return the new inner name
     */
    public InnerName addDescNameOuterInterface(int locality, Handle handle) {
        Owner o = handle.getOwner();
        assertOrSetOwner(handle, "Handle");
        if (handle.isEdge() && o != null) {
            this.directedBigraph.onEdgeAdded((EditableEdge) handle);
        }
        return addDescNameOuterInterface(locality, new EditableInnerName(), (EditableHandle) handle);
    }

    /**
     * Adds an inner name to the current bigraph's outer interface at the specified locality. The name will be the only point of a fresh edge.
     *
     * @param locality the locality where to look.
     * @param name     name of the new inner name.
     * @return the new inner name.
     */
    public InnerName addDescNameOuterInterface(int locality, String name) {
        if (name == null || name.length() == 0)
            throw new IllegalArgumentException("Name can not be null.");
        EditableEdge e = new EditableEdge(this);
        this.directedBigraph.onEdgeAdded(e);
        return addDescNameOuterInterface(locality, name, e);
    }

    /**
     * Adds an inner name to the current bigraph's outer interface at the specified locality.
     *
     * @param locality the locality where to look.
     * @param name     name of the new inner name.
     * @param handle   the outer name or the edge linking the new inner name.
     * @return the new inner name.
     */
    public InnerName addDescNameOuterInterface(int locality, String name, Handle handle) {
        if (name == null)
            throw new IllegalArgumentException("Name can not be null.");
        Owner o = handle.getOwner();
        assertOrSetOwner(handle, "Handle");
        if (handle.isEdge() && o != null) {
            this.directedBigraph.onEdgeAdded((EditableEdge) handle);
        }
        return addDescNameOuterInterface(locality, new EditableInnerName(name), (EditableHandle) handle);
    }

    /**
     * Add an innername to the current bigraph's outer interface at the specified locality.
     *
     * @param locality  the locality where to look.
     * @param innerName innername that will be added.
     * @param handle    outername or edge that will be linked with the innername in input.
     * @return the inner name
     */
    private InnerName addDescNameOuterInterface(int locality, EditableInnerName innerName, EditableHandle handle) {
        assertOpen();
        if (this.directedBigraph.outers.getDesc(locality).containsKey(innerName.getName())) {
            throw new IllegalArgumentException("Name already present.");
        }
        innerName.setHandle(handle);
        this.directedBigraph.outers.addDesc(locality, innerName);
        assertConsistency();
        return innerName;
    }

    /**
     * Adds a fresh inner name to the current bigraph's inner interface at the specified locality. The name will be the only point of a fresh edge.
     *
     * @param locality the locality where to look.
     * @return the new inner name.
     */
    public InnerName addAscNameInnerInterface(int locality) {
        EditableEdge e = new EditableEdge(this);
        this.directedBigraph.onEdgeAdded(e);
        return addAscNameInnerInterface(locality, new EditableInnerName(), e);
    }

    /**
     * Adds a new inner name to the current bigraph's inner interface at the specified locality.
     *
     * @param locality the locality where to look.
     * @param handle   the outer name or the edge linking the new inner name.
     * @return the new inner name
     */
    public InnerName addAscNameInnerInterface(int locality, Handle handle) {
        Owner o = handle.getOwner();
        assertOrSetOwner(handle, "Handle");
        if (handle.isEdge() && o != null) {
            this.directedBigraph.onEdgeAdded((EditableEdge) handle);
        }
        return addAscNameInnerInterface(locality, new EditableInnerName(), (EditableHandle) handle);
    }

    /**
     * Adds an inner name to the current bigraph's inner interface at the specified locality. The name will be the only point of a fresh edge.
     *
     * @param locality the locality where to look.
     * @param name     name of the new inner name.
     * @return the new inner name.
     */
    public InnerName addAscNameInnerInterface(int locality, String name) {
        if (name == null || name.length() == 0)
            throw new IllegalArgumentException("Name can not be null.");
        EditableEdge e = new EditableEdge(this);
        this.directedBigraph.onEdgeAdded(e);
        return addAscNameInnerInterface(locality, name, e);
    }

    /**
     * Adds an inner name to the current bigraph's inner interface at the specified locality.
     *
     * @param locality the locality where to look.
     * @param name     name of the new inner name.
     * @param handle   the outer name or the edge linking the new inner name.
     * @return the new inner name.
     */
    public InnerName addAscNameInnerInterface(int locality, String name, Handle handle) {
        if (name == null)
            throw new IllegalArgumentException("Name can not be null.");
        Owner o = handle.getOwner();
        assertOrSetOwner(handle, "Handle");
        if (handle.isEdge() && o != null) {
            this.directedBigraph.onEdgeAdded((EditableEdge) handle);
        }
        return addAscNameInnerInterface(locality, new EditableInnerName(name), (EditableHandle) handle);
    }

    /**
     * Add an innername to the current bigraph's inner interface at the specified locality.
     *
     * @param locality the locality where to look.
     * @param n        innername that will be added.
     * @param h        outername or edge that will be linked with the innername in input.
     * @return the inner name
     */
    private InnerName addAscNameInnerInterface(int locality, EditableInnerName n, EditableHandle h) {
        assertOpen();
        if (this.directedBigraph.inners.getAsc(locality).containsKey(n.getName())) {
            throw new IllegalArgumentException("Name already present.");
        }
        n.setHandle(h);
        this.directedBigraph.inners.addAsc(locality, n);
        assertConsistency();
        return n;
    }

    /**
     * Links a set of points with a fresh edge.
     *
     * @param points the points to be linked.
     * @return the new edge connecting the points in input.
     */
    public Edge relink(Point... points) {
        return (Edge) relink(new EditableEdge(), points);
    }

    public Edge relink(Collection<? extends Point> points) {
        if (points == null)
            throw new IllegalArgumentException("Argument can not be null.");
        return relink(points.toArray(new EditablePoint[points.size()]));
    }

    /**
     * Links a set of points with the given handle.
     *
     * @param handle the handle to be used.
     * @param points the points to be linked.
     * @return the handle.
     */
    public Handle relink(Handle handle, Point... points) {
        assertOpen();
        assertOrSetOwner(handle, "Handle");
        EditablePoint[] ps = new EditablePoint[points.length];
        EditableHandle h = (EditableHandle) handle;
        for (int i = 0; i < points.length; i++) {
            ps[i] = (EditablePoint) points[i];
            assertOwner(ps[i], "Point");
        }
        for (int i = 0; i < points.length; i++) {
            Handle old = ps[i].getHandle();
            ps[i].setHandle(h);
            if (old.isEdge() && old.getPoints().isEmpty()) {
                this.directedBigraph.onEdgeRemoved((EditableEdge) old);
            }
        }
        if (handle.isEdge()) { // checking owner to be null is not enough to find new edges
            this.directedBigraph.onEdgeAdded((EditableEdge) handle);
        }
        assertConsistency();
        return h;
    }

    public Handle relink(Handle handle, Collection<? extends Point> points) {
        if (points == null)
            throw new IllegalArgumentException("Argument can not be null.");
        return relink(handle, points.toArray(new EditablePoint[points.size()]));
    }

    /**
     * Disconnects a point from its current handle and connect it with a fresh edge.
     *
     * @param point the point that will be unlinked
     * @return the new edge
     */
    public Edge unlink(Point point) {
        return relink(point);
    }

    /**
     * Closes an outer name of the outer interface, at the specified locality, turning it into an edge.
     *
     * @param locality the locality where to look.
     * @param name     the outer name as string.
     * @return the new edge.
     */
    public Edge closeOuterNameOuterInterface(int locality, String name) {
        return closeOuterNameOuterInterface(locality, this.directedBigraph.outers.getAsc(locality).get(name));
    }

    /**
     * Closes an outer name of the outer interface at the specified locality, turning it into an edge.
     *
     * @param locality the locality where to look.
     * @param name     the outer name to close.
     * @return the new edge.
     */
    public Edge closeOuterNameOuterInterface(int locality, OuterName name) {
        assertOwner(name, "OuterName ");
        if (!this.directedBigraph.outers.getAsc(locality).containsKey(name.getName())) {
            throw new IllegalArgumentException("Name '" + name.getName() + "' not present.");
        }
        EditableOuterName n1 = (EditableOuterName) name;
        Edge e = relink(n1.getEditablePoints());
        this.directedBigraph.outers.removeAsc(locality, name.getName());
        n1.setOwner(null);
        return e;
    }

    /**
     * Closes an outer name of the inner interface, at the specified locality, turning it into an edge.
     *
     * @param locality the locality where to look.
     * @param name     the outer name as string.
     * @return the new edge.
     */
    public Edge closeOuterNameInnerInterface(int locality, String name) {
        return closeOuterNameInnerInterface(locality, this.directedBigraph.inners.getDesc(locality).get(name));
    }

    /**
     * Closes an outer name of the inner interface, at the specified locality, turning it into an edge.
     *
     * @param locality the locality where to look.
     * @param name     the outer name to close.
     * @return the new edge.
     */
    public Edge closeOuterNameInnerInterface(int locality, OuterName name) {
        assertOwner(name, "OuterName ");
        if (!this.directedBigraph.inners.getDesc(locality).containsKey(name.getName())) {
            throw new IllegalArgumentException("Name '" + name.getName() + "' not present.");
        }
        EditableOuterName n1 = (EditableOuterName) name;
        Edge e = relink(n1.getEditablePoints());
        this.directedBigraph.inners.removeDesc(locality, name.getName());
        n1.setOwner(null);
        return e;
    }

    /**
     * Closes an inner name at the specified locality.
     *
     * @param locality the locality where to look.
     * @param name     the inner name as string.
     */
    public void closeInnerNameOuterInterface(int locality, String name) {
        closeInnerNameOuterInterface(locality, this.directedBigraph.outers.getDesc(locality).get(name));
    }

    /**
     * Closes an inner name at the specified locality.
     *
     * @param locality the locality where to look.
     * @param name     the inner name to close.
     */
    public void closeInnerNameOuterInterface(int locality, InnerName name) {
        assertOwner(name, "InnerName ");
        if (!this.directedBigraph.outers.getDesc(locality).containsKey(name.getName())) {
            throw new IllegalArgumentException("Name '" + name.getName() + "' not present.");
        }
        EditableInnerName n1 = (EditableInnerName) name;
        Handle h = n1.getHandle();
        n1.setHandle(null);
        this.directedBigraph.outers.removeDesc(locality, n1.getName());
        if (h.isEdge() && h.getPoints().isEmpty()) {
            this.directedBigraph.onEdgeRemoved((EditableEdge) h);
        }
    }

    /**
     * Closes an inner name at the specified locality.
     *
     * @param locality the locality where to look.
     * @param name     the inner name as string.
     */
    public void closeInnerNameInnerInterface(int locality, String name) {
        closeInnerNameInnerInterface(locality, this.directedBigraph.inners.getAsc(locality).get(name));
    }

    /**
     * Closes an inner name at the specified locality.
     *
     * @param locality the locality where to look.
     * @param name     the inner name to close.
     */
    public void closeInnerNameInnerInterface(int locality, InnerName name) {
        assertOwner(name, "InnerName ");
        if (!this.directedBigraph.inners.getAsc(locality).containsKey(name.getName())) {
            throw new IllegalArgumentException("Name '" + name.getName() + "' not present.");
        }
        EditableInnerName n1 = (EditableInnerName) name;
        Handle h = n1.getHandle();
        n1.setHandle(null);
        this.directedBigraph.inners.removeAsc(locality, n1.getName());
        if (h.isEdge() && h.getPoints().isEmpty()) {
            this.directedBigraph.onEdgeRemoved((EditableEdge) h);
        }
    }

    /**
     * Renames an outer name of the inner interface in the specified locality.
     *
     * @param locality the locality where to look.
     * @param oldName  the outer name to be renamed.
     * @param newName  the new name.
     */
    public void renameOuterNameOuterInterface(int locality, String oldName, String newName) {
        if (newName == null || oldName == null)
            throw new IllegalArgumentException("Arguments can not be null");
        EditableOuterName n1 = this.directedBigraph.outers.getAsc(locality).get(oldName);
        if (n1 == null) {
            throw new IllegalArgumentException("Name '" + oldName + "' is not present.");
        } else {
            renameOuterNameOuterInterface(locality, n1, newName);
        }
    }

    /**
     * Renames an outer name of the inner interface in the specified locality.
     *
     * @param locality the locality where to look.
     * @param oldName  the outer name to be renamed.
     * @param newName  the new name.
     */
    public void renameOuterNameOuterInterface(int locality, OuterName oldName, String newName) {
        if (newName == null || oldName == null)
            throw new IllegalArgumentException("Arguments can not be null");
        assertOwner(oldName, "OuterName ");
        if (newName.equals(oldName.getName()))
            return;
        EditableOuterName n2 = this.directedBigraph.outers.getAsc(locality).get(newName);
        if (n2 == null) {
            ((EditableOuterName) oldName).setName(newName);
        } else {
            throw new IllegalArgumentException("Name '" + newName + "' already in use");
        }
    }

    /**
     * Renames an outer name of the inner interface in the specified locality.
     *
     * @param locality the locality where to look.
     * @param oldName  the outer name to be renamed.
     * @param newName  the new name.
     */
    public void renameOuterNameInnerInterface(int locality, String oldName, String newName) {
        if (newName == null || oldName == null)
            throw new IllegalArgumentException("Arguments can not be null");
        EditableOuterName n1 = this.directedBigraph.inners.getDesc(locality).get(oldName);
        if (n1 == null) {
            throw new IllegalArgumentException("Name '" + oldName + "' is not present.");
        } else {
            renameOuterNameInnerInterface(locality, n1, newName);
        }
    }

    /**
     * Renames an outer name of the inner interface in the specified locality.
     *
     * @param locality the locality where to look.
     * @param oldName  the outer name to be renamed.
     * @param newName  the new name.
     */
    public void renameOuterNameInnerInterface(int locality, OuterName oldName, String newName) {
        if (newName == null || oldName == null)
            throw new IllegalArgumentException("Arguments can not be null");
        assertOwner(oldName, "OuterName ");
        if (newName.equals(oldName.getName()))
            return;
        EditableOuterName n2 = this.directedBigraph.inners.getDesc(locality).get(newName);
        if (n2 == null) {
            ((EditableOuterName) oldName).setName(newName);
        } else {
            throw new IllegalArgumentException("Name '" + newName + "' already in use");
        }
    }

    /**
     * Renames an inner name.
     *
     * @param locality the locality where to look.
     * @param oldName  the inner name to be renamed.
     * @param newName  the new name.
     */
    public void renameInnerNameOuterInterface(int locality, String oldName, String newName) {
        if (newName == null || oldName == null)
            throw new IllegalArgumentException("Arguments can not be null");
        EditableInnerName n1 = this.directedBigraph.outers.getDesc(locality).get(oldName);
        if (n1 == null) {
            throw new IllegalArgumentException("Name '" + oldName + "' is not present.");
        } else {
            renameInnerNameOuterInterface(locality, n1, newName);
        }
    }

    /**
     * Renames an inner name.
     *
     * @param locality the locality where to look.
     * @param oldName  the inner name to be renamed.
     * @param newName  the new name.
     */
    public void renameInnerNameOuterInterface(int locality, InnerName oldName, String newName) {
        if (newName == null || oldName == null)
            throw new IllegalArgumentException("Arguments can not be null");
        assertOwner(oldName, "InnerName ");
        if (newName.equals(oldName.getName()))
            return;
        EditableInnerName n2 = this.directedBigraph.outers.getDesc(locality).get(newName);
        if (n2 == null) {
            ((EditableInnerName) oldName).setName(newName);
        } else {
            throw new IllegalArgumentException("Name '" + newName + "' is present already.");
        }
    }

    /**
     * Renames an inner name.
     *
     * @param locality the locality where to look.
     * @param oldName  the inner name to be renamed.
     * @param newName  the new name.
     */
    public void renameInnerNameInnerInterface(int locality, String oldName, String newName) {
        if (newName == null || oldName == null)
            throw new IllegalArgumentException("Arguments can not be null");
        EditableInnerName n1 = this.directedBigraph.inners.getAsc(locality).get(oldName);
        if (n1 == null) {
            throw new IllegalArgumentException("Name '" + oldName + "' is not present.");
        } else {
            renameInnerNameInnerInterface(locality, n1, newName);
        }
    }

    /**
     * Renames an inner name.
     *
     * @param locality the locality where to look.
     * @param oldName  the inner name to be renamed.
     * @param newName  the new name.
     */
    public void renameInnerNameInnerInterface(int locality, InnerName oldName, String newName) {
        if (newName == null || oldName == null)
            throw new IllegalArgumentException("Arguments can not be null");
        assertOwner(oldName, "InnerName ");
        if (newName.equals(oldName.getName()))
            return;
        EditableInnerName n2 = this.directedBigraph.inners.getAsc(locality).get(newName);
        if (n2 == null) {
            ((EditableInnerName) oldName).setName(newName);
        } else {
            throw new IllegalArgumentException("Name '" + newName + "' is present already.");
        }
    }

    /**
     * Merges every region of the bigraph into one.
     *
     * @return the new root.
     */
    public Root merge() {
        assertOpen();
        EditableRoot r = new EditableRoot();
        r.setOwner(this);
        for (EditableParent p : this.directedBigraph.roots) {
            for (EditableChild c : new HashSet<>(p.getEditableChildren())) {
                c.setParent(r);
            }
        }
        clearOwnedCollection(this.directedBigraph.roots);// .clear();
        this.directedBigraph.roots.add(r);
        assertConsistency();
        return r;
    }

    /**
     * Merges the given regions of the bigraph into one.
     *
     * @param index the index of the new region.
     * @param roots the index of the regions to be merged.
     * @return the new root.
     */
    public Root merge(int index, int... roots) {
        assertOpen();
        EditableRoot r = new EditableRoot();
        r.setOwner(this);
        EditableRoot[] rs = new EditableRoot[roots.length];
        for (int i = 0; i < roots.length; i++) {
            rs[i] = this.directedBigraph.roots.get(roots[i]);
            for (EditableChild editableChild : rs[i].getEditableChildren()) {
                editableChild.setParent(r);
            }
        }
        for (EditableRoot r1 : rs) {
            this.directedBigraph.roots.remove(r1);
            r1.setOwner(null);
        }
        this.directedBigraph.roots.add(index, r);
        assertConsistency();
        return r;
    }

    public void removeRoot(Root root) {
        assertOwner(root, "Root ");
        if (!root.getChildren().isEmpty())
            throw new IllegalArgumentException("Unempty region.");
        EditableRoot editableRoot = (EditableRoot) root;
        editableRoot.setOwner(null);
        this.directedBigraph.roots.remove(editableRoot);
        if (!root.getChildren().isEmpty()) {
            this.directedBigraph.onNodeSetChanged(); // retrieving its descendants is not cheap and the cache may already be invalid anyway.
        }
        assertConsistency();
    }

    public void removeRoot(int index) {
        if (index < 0 || index >= this.directedBigraph.roots.size())
            throw new IndexOutOfBoundsException(
                    "The argument does not refer to a root.");
        removeRoot(this.directedBigraph.roots.get(index));
    }

    /**
     * Removes a site from the bigraph.
     *
     * @param site the site to be removed.
     */
    public void closeSite(Site site) {
        assertOwner(site, "Site ");
        EditableSite editableSite = (EditableSite) site;
        editableSite.setParent(null);
        this.directedBigraph.sites.remove(editableSite);
        assertConsistency();
    }

    /**
     * Removes a site from the bigraph.
     *
     * @param index the index of site to be removed.
     */
    public void closeSite(int index) {
        if (index < 0 || index >= this.directedBigraph.sites.size())
            throw new IndexOutOfBoundsException(
                    "The argument does not refer to a site.");
        closeSite(this.directedBigraph.sites.get(index));
    }

    /**
     * Close all site an innernames of the current bigraph, generating a ground
     * bigraph.
     */
    public void ground() {
        assertOpen();
        clearChildCollection(this.directedBigraph.sites);// .clear();
        clearInnerInterface(this.directedBigraph.inners);// .clear();
        assertConsistency();
    }

    /**
     * Juxtapose the current bigraphbuilder with the bigraph in input. <br />
     * Roots and sites of the bigraph will precede those of the bigraphbuilder
     * in the resulting bigraphbuilder.
     *
     * @param graph bigraph that will be juxtaposed.
     */
    public void leftJuxtapose(DirectedBigraph graph) {
        leftJuxtapose(graph, false);
    }

    /**
     * Juxtapose the current bigraphbuilder with the bigraph in input. <br />
     * Roots and sites of the bigraph will precede those of the bigraphbuilder
     * in the resulting bigraphbuilder.
     *
     * @param otherDirectedBigraph bigraph that will be juxtaposed.
     * @param reuse                flag. If true, the bigraph in input won't be copied.
     */
    public void leftJuxtapose(DirectedBigraph otherDirectedBigraph, boolean reuse) {
        assertOpen();
        DirectedBigraph left = otherDirectedBigraph;
        DirectedBigraph right = this.directedBigraph;
        if (left == right)
            throw new IllegalArgumentException("Operand shuld be distinct; a bigraph can not be juxtaposed with itself.");
        // Arguments are assumed to be consistent (e.g. parent and links are well defined)
        if (!left.signature.equals(right.signature)) {
            throw new IncompatibleSignatureException(left.getSignature(), right.getSignature());
        }
        if (!Collections.disjoint(left.inners.getAsc(0).keySet(), right.inners.getAsc(0).keySet())
                || !Collections.disjoint(left.inners.getDesc(0).keySet(), right.inners.getDesc(0).keySet())
                || !Collections.disjoint(left.outers.getAsc(0).keySet(), right.outers.getAsc(0).keySet())
                || !Collections.disjoint(left.outers.getDesc(0).keySet(), right.outers.getDesc(0).keySet())) {
            throw new IncompatibleInterfaceException(new NameClashException(
                    intersectNames(
                            left.inners.getAsc(0).values(),
                            right.inners.getAsc(0).values(),
                            intersectNames(left.outers.getAsc(0).values(),
                                    right.outers.getAsc(0).values(),
                                    intersectNames(left.outers.getDesc(0).values(),
                                            right.outers.getDesc(0).values(),
                                            intersectNames(left.inners.getDesc(0).values(),
                                                    right.inners.getDesc(0).values()))))));
        }
        DirectedBigraph l = (reuse) ? left : left.clone();
        DirectedBigraph r = right;
        for (EditableOwned o : l.roots) {
            o.setOwner(this);
        }
        for (EditableOwned o : l.outers.getAsc().values()) {
            o.setOwner(this);
        }
        for (EditableOwned o : l.inners.getDesc().values()) {
            o.setOwner(this);
        }
        Collection<EditableEdge> es = l.edgesProxy.get();
        for (EditableEdge e : es) {
            e.setOwner(this);
        }
        r.onEdgeAdded(es);
        r.onNodeAdded(l.nodesProxy.get());
        l.onEdgeSetChanged();
        l.onNodeSetChanged();
        r.roots.addAll(0, l.roots);
        r.sites.addAll(0, l.sites);
        r.outers.join(l.outers);
        r.inners.join(l.inners);
        assertConsistency();
    }

    /**
     * Juxtapose the current bigraphbuilder with the bigraph in input. <br />
     * Roots and sites of the bigraphbuilder will precede those of the bigraph
     * in the resulting bigraphbuilder.
     *
     * @param otherDirectedBigraph bigraph that will be juxtaposed.
     */
    public void rightJuxtapose(DirectedBigraph otherDirectedBigraph) {
        rightJuxtapose(otherDirectedBigraph, false);
    }

    /**
     * Juxtapose the current bigraphbuilder with the bigraph in input. <br />
     * Roots and sites of the bigraphbuilder will precede those of the bigraph
     * in the resulting bigraphbuilder.
     *
     * @param otherDirectedBigraph bigraph that will be juxtaposed.
     * @param reuse                flag. If true, the bigraph in input won't be copied.
     */
    public void rightJuxtapose(DirectedBigraph otherDirectedBigraph, boolean reuse) {
        assertOpen();
        DirectedBigraph left = this.directedBigraph;
        DirectedBigraph right = otherDirectedBigraph;
        if (left == right)
            throw new IllegalArgumentException("Operand shuld be distinct; a bigraph can not be juxtaposed with itself.");
        // Arguments are assumed to be consistent (e.g. parent and links are well defined)
        if (!left.signature.equals(right.signature)) {
            throw new IncompatibleSignatureException(left.signature, right.signature);
        }
        if (!Collections.disjoint(left.inners.getAsc(0).keySet(), right.inners.getAsc(0).keySet())
                || !Collections.disjoint(left.inners.getDesc(0).keySet(), right.inners.getDesc(0).keySet())
                || !Collections.disjoint(left.outers.getAsc(0).keySet(), right.outers.getAsc(0).keySet())
                || !Collections.disjoint(left.outers.getDesc(0).keySet(), right.outers.getDesc(0).keySet())) {
            throw new IncompatibleInterfaceException(new NameClashException(
                    intersectNames(
                            left.inners.getAsc(0).values(),
                            right.inners.getAsc(0).values(),
                            intersectNames(left.outers.getAsc(0).values(),
                                    right.outers.getAsc(0).values(),
                                    intersectNames(left.outers.getDesc(0).values(),
                                            right.outers.getDesc(0).values(),
                                            intersectNames(left.inners.getDesc(0).values(),
                                                    right.inners.getDesc(0).values()))))));
        }
        DirectedBigraph l = left;
        DirectedBigraph r = (reuse) ? right : right.clone();
        for (EditableOwned o : r.roots) {
            o.setOwner(this);
        }
        for (EditableOwned o : r.outers.getAsc().values()) {
            o.setOwner(this);
        }
        for (EditableOwned o : r.inners.getDesc().values()) {
            o.setOwner(this);
        }
        Collection<EditableEdge> es = r.edgesProxy.get();
        for (EditableEdge e : es) {
            e.setOwner(this);
        }
        l.onEdgeAdded(es);
        l.onNodeAdded(r.nodesProxy.get());
        r.onEdgeSetChanged();
        r.onNodeSetChanged();
        l.roots.addAll(r.roots);
        l.sites.addAll(r.sites);
        l.outers.join(r.outers);
        l.inners.join(r.inners);
        assertConsistency();
    }

    /**
     * Compose the current bigraphbuilder with the bigraph in input.
     *
     * @param graph the "inner" bigraph
     */
    public void innerCompose(DirectedBigraph graph) {
        innerCompose(graph, false);
    }

    /**
     * Compose the current bigraphbuilder with the bigraph in input.
     *
     * @param graph the "inner" bigraph
     * @param reuse flag. If true, the bigraph in input won't be copied.
     */
    public void innerCompose(DirectedBigraph graph, boolean reuse) {
        assertOpen();
        DirectedBigraph in = graph;
        DirectedBigraph out = this.directedBigraph;
        // Arguments are assumed to be consistent (e.g. parent and links are
        // well defined)
        if (out == in)
            throw new IllegalArgumentException("Operand shuld be distinct; a bigraph can not be composed with itself.");
        if (!out.signature.equals(in.signature)) {
            throw new IncompatibleSignatureException(out.signature, in.signature);
        }
        Set<String> xs = new HashSet<>(out.inners.keySet());
        Set<String> ys = new HashSet<>(in.outers.keySet());
        Set<String> zs = new HashSet<>(xs);
        xs.removeAll(ys);
        ys.removeAll(zs);

        if (!xs.isEmpty() || !ys.isEmpty() || out.sites.size() != in.roots.size()) {
            throw new IncompatibleInterfaceException("The outer face of the first graph must be equal to inner face of the second");
        }
        DirectedBigraph a = out;
        DirectedBigraph b = (reuse) ? in : in.clone();
        Collection<EditableEdge> es = b.edgesProxy.get();
        Collection<EditableNode> ns = b.nodesProxy.get();
        // iterate over sites and roots of a and b respectively and glue them
        Iterator<EditableRoot> ir = b.roots.iterator();
        Iterator<EditableSite> is = a.sites.iterator();
        while (ir.hasNext()) { // |ir| == |is|
            EditableSite s = is.next();
            EditableParent p = s.getParent();
            p.removeChild(s);
            for (EditableChild c : new HashSet<>(ir.next().getEditableChildren())) {
                c.setParent(p);
            }
        }
        // iterate over inner and outer names of a and b respectively and glue them
        for (int l = 0; l < a.inners.getWidth(); l++) {
            Map<String, EditableHandle> handleMap = new HashMap<>();
            for (EditableInnerName i : a.inners.getAsc(l).values()) {
                handleMap.put("A" + i.getName(), i.getHandle());
                i.setHandle(null);
            }
            for (EditableOuterName o : b.outers.getAsc(l).values()) {
                EditableHandle h = handleMap.get("A" + o.getName());
                for (EditablePoint p : new HashSet<>(o.getEditablePoints())) {
                    p.setHandle(h);
                }
            }
            for (EditableInnerName i : b.outers.getDesc(l).values()) {
                handleMap.put("D" + i.getName(), i.getHandle());
                i.setHandle(null);
            }
            for (EditableOuterName o : a.inners.getDesc(l).values()) {
                EditableHandle h = handleMap.get("D" + o.getName());
                for (EditablePoint p : new HashSet<>(o.getEditablePoints())) {
                    p.setHandle(h);
                }
            }
        }
        // update inner interfaces
        clearInnerInterface(a.inners);// .clear();
        clearChildCollection(a.sites);// .clear();
        a.inners.names.addAll(b.inners.names);
        a.sites.addAll(b.sites);
        a.onNodeAdded(ns);
        b.onNodeSetChanged();
        for (EditableEdge e : es) {
            e.setOwner(this);
        }
        a.onEdgeAdded(es);
        b.onEdgeSetChanged();
        assertConsistency();
    }

    /**
     * Compose bigraph in input with the current bigraphbuilder
     *
     * @param graph the "outer" bigraph
     */
    public void outerCompose(DirectedBigraph graph) {
        outerCompose(graph, false);
    }

    /**
     * Compose the current bigraph in input with the bigraphbuilder.
     *
     * @param graph the "outer" bigraph
     * @param reuse flag. If true, the bigraph in input will not be copied.
     */
    public void outerCompose(DirectedBigraph graph, boolean reuse) {
        assertOpen();
        DirectedBigraph in = this.directedBigraph;
        DirectedBigraph out = graph;
        // Arguments are assumed to be consistent (e.g. parent and links are
        // well defined)
        if (out == in)
            throw new IllegalArgumentException("Operand shuld be distinct; a bigraph can not be composed with itself.");
        if (!out.signature.equals(in.signature)) {
            throw new IncompatibleSignatureException(out.signature, in.signature);
        }
        Set<String> xs = new HashSet<>(out.inners.keySet());
        Set<String> ys = new HashSet<>(in.outers.keySet());
        Set<String> zs = new HashSet<>(xs);
        xs.removeAll(ys);
        ys.removeAll(zs);

        if (!xs.isEmpty() || !ys.isEmpty() || out.sites.size() != in.roots.size()) {
            throw new IncompatibleInterfaceException("The outer face of the first graph must be equal to inner face of the second");
        }
        DirectedBigraph a = (reuse) ? out : out.clone();
        DirectedBigraph b = in; // this BB
        Collection<EditableEdge> es = a.edgesProxy.get();
        Collection<EditableNode> ns = a.nodesProxy.get();
        // iterates over sites and roots of a and b respectively and glues them
        Iterator<EditableRoot> ir = b.roots.iterator();
        Iterator<EditableSite> is = a.sites.iterator();
        while (ir.hasNext()) { // |ir| == |is|
            EditableSite s = is.next();
            EditableParent p = s.getParent();
            p.removeChild(s);
            for (EditableChild c : new HashSet<>(ir.next().getEditableChildren())) {
                c.setParent(p);
            }
        }
        // iterate over inner and outer names of a and b respectively and glue them
        for (int l = 0; l < a.inners.getWidth(); l++) {
            Map<String, EditableHandle> handleMap = new HashMap<>();
            for (EditableInnerName i : a.inners.getAsc(l).values()) {
                handleMap.put("A" + i.getName(), i.getHandle());
                i.setHandle(null);
            }
            for (EditableOuterName o : b.outers.getAsc(l).values()) {
                EditableHandle h = handleMap.get("A" + o.getName());
                for (EditablePoint p : new HashSet<>(o.getEditablePoints())) {
                    p.setHandle(h);
                }
            }
            for (EditableInnerName i : b.outers.getDesc(l).values()) {
                handleMap.put("D" + i.getName(), i.getHandle());
                i.setHandle(null);
            }
            for (EditableOuterName o : a.inners.getDesc(l).values()) {
                EditableHandle h = handleMap.get("D" + o.getName());
                for (EditablePoint p : new HashSet<>(o.getEditablePoints())) {
                    p.setHandle(h);
                }
            }
        }
        // updates inner interfaces
        clearOuterInterface(b.outers);// .clear();
        clearOwnedCollection(b.roots);// .clear();
        b.outers.names.addAll(a.outers.names);
        b.roots.addAll(a.roots);
        for (EditableOwned o : b.roots) {
            o.setOwner(this);
        }
        for (EditableOwned o : b.outers.getAsc().values()) {
            o.setOwner(this);
        }

        for (EditableOwned o : b.inners.getDesc().values()) {
            o.setOwner(this);
        }
        b.onNodeAdded(ns);
        a.onNodeSetChanged();
        for (EditableEdge e : es) {
            e.setOwner(this);
        }
        b.onEdgeAdded(es);
        a.onEdgeSetChanged();
        assertConsistency();
    }

    /**
     * Juxtapose bigraph in input with the current bigraphbuilder. <br />
     * ParallelProduct, differently from the normal juxtapose, doesn't need
     * disjoint sets of outernames for the two bigraphshs. Common outernames will
     * be merged. <br />
     * Roots and sites of the bigraph will precede those of the bigraphbuilder
     * in the resulting bigraphbuilder.
     *
     * @param graph bigraph that will be juxtaposed.
     */
    public void leftParallelProduct(DirectedBigraph graph) {
        leftParallelProduct(graph, false);
    }

    /**
     * Juxtapose bigraph in input with the current bigraphbuilder. <br />
     * ParallelProduct, differently from the normal juxtapose, doesn't need
     * disjoint sets of outernames for the two bigraphs. Common outernames will
     * be merged. <br />
     * Roots and sites of the bigraph will precede those of the bigraphbuilder
     * in the resulting bigraphbuilder.
     *
     * @param graph bigraph that will be juxtaposed.
     * @param reuse flag. If true, the bigraph in input won't be copied.
     */
    public void leftParallelProduct(DirectedBigraph graph, boolean reuse) {
        assertOpen();
        DirectedBigraph left = graph;
        DirectedBigraph right = this.directedBigraph;
        // Arguments are assumed to be consistent (e.g. parent and links are well defined)
        if (!left.signature.equals(right.signature)) {
            throw new IncompatibleSignatureException(left.signature, right.signature);
        }
        if (!Collections.disjoint(left.inners.keySet(), right.inners.keySet())) {
            throw new IncompatibleInterfaceException(new NameClashException(
                    intersectNames(
                            left.inners.getAsc().values(),
                            right.inners.getAsc().values(),
                            intersectNames(left.outers.getAsc().values(),
                                    right.outers.getAsc().values(),
                                    intersectNames(left.outers.getDesc().values(),
                                            right.outers.getDesc().values(),
                                            intersectNames(left.inners.getDesc().values(),
                                                    right.inners.getDesc().values()))))));
        }
        DirectedBigraph l = (reuse) ? left : left.clone();
        DirectedBigraph r = right;
        for (EditableOwned o : l.roots) {
            o.setOwner(this);
        }
        // merge outer interface
        for (int loc = 0; loc < l.outers.getWidth(); loc++) {
            for (EditableOuterName o : l.outers.getAsc(loc).values()) {
                EditableOuterName q = null;
                for (EditableOuterName p : r.outers.getAsc(loc).values()) {
                    if (p.getName().equals(o.getName())) {
                        q = p;
                        break;
                    }
                }
                if (q == null) {
                    // o is not part of r.outerface
                    r.outers.addAsc(loc, o);
                    o.setOwner(this);
                } else {
                    // this name apperas also in r, merge points
                    for (EditablePoint p : new HashSet<>(o.getEditablePoints())) {
                        q.linkPoint(p);
                    }
                }
            }
            for (EditableInnerName i : l.outers.getDesc(loc).values()) {
                EditableInnerName j = null;
                for (EditableInnerName k : r.outers.getDesc(loc).values()) {
                    if (k.getName().equals(i.getName())) {
                        j = k;
                        break;
                    }
                }
                if (j == null) {
                    // i is not part of r.outerface
                    r.outers.addDesc(loc, i);
                }
            }
        }
        Collection<EditableEdge> es = l.edgesProxy.get();
        for (EditableEdge e : es) {
            e.setOwner(this);
        }
        r.onEdgeAdded(es);
        r.onNodeAdded(l.nodesProxy.get());
        l.onEdgeSetChanged();
        l.onNodeSetChanged();
        r.roots.addAll(l.roots);
        r.sites.addAll(l.sites);
        r.inners.join(l.inners);
        assertConsistency();
    }

    // /////////////////////////////////////////////////////////////////////////

    /**
     * Juxtapose the current bigraphbuilder with the bigraph in input. <br />
     * ParallelProduct, differently from the normal juxtapose, doesn't need
     * disjoint sets of outernames for the two bigraphs. Common outernames will
     * be merged. <br />
     * Roots and sites of the bigraphbuilder will precede those of the bigraph
     * in the resulting bigraphbuilder.
     *
     * @param graph bigraph that will be juxtaposed.
     */
    public void rightParallelProduct(DirectedBigraph graph) {
        rightParallelProduct(graph, false);
    }

    /**
     * Juxtapose the current bigraphbuilder with the bigraph in input. <br/>
     * ParallelProduct, differently from the normal juxtapose, doesn't need
     * disjoint sets of outernames for the two bigraphs. Common outernames will
     * be merged. <br/>
     * Roots and sites of the bigraphbuilder will precede those of the bigraph
     * in the resulting bigraphbuilder.
     *
     * @param graph bigraph that will be juxtaposed.
     * @param reuse flag. If true, the bigraph in input won't be copied.
     */
    public void rightParallelProduct(DirectedBigraph graph, boolean reuse) {
        assertOpen();
        DirectedBigraph left = this.directedBigraph;
        DirectedBigraph right = graph;
        // Arguments are assumed to be consistent (e.g. parent and links are well defined)
        if (left == right)
            throw new IllegalArgumentException("Operand shuld be distinct; a bigraph can not be juxtaposed with itself.");
        if (!left.signature.equals(right.signature)) {
            throw new IncompatibleSignatureException(left.signature, right.signature);
        }
        if (!Collections.disjoint(left.inners.keySet(), right.inners.keySet())) {
            throw new IncompatibleInterfaceException(
                    new NameClashException(intersectNames(
                            left.inners.getAsc().values(),
                            right.inners.getAsc().values(),
                            intersectNames(left.outers.getAsc().values(),
                                    right.outers.getAsc().values(),
                                    intersectNames(left.outers.getDesc().values(),
                                            right.outers.getDesc().values(),
                                            intersectNames(left.inners.getDesc().values(),
                                                    right.inners.getDesc().values()))))));
        }
        DirectedBigraph l = left;
        DirectedBigraph r = (reuse) ? right : right.clone();
        for (EditableOwned o : r.roots) {
            o.setOwner(this);
        }
        Map<String, EditableOuterName> os = new HashMap<>();
        // merge outer interface
        for (int loc = 0; loc < r.outers.getWidth(); loc++) {
            for (EditableOuterName o : r.outers.getAsc(loc).values()) {
                EditableOuterName q = null;
                for (EditableOuterName p : l.outers.getAsc(loc).values()) {
                    if (p.getName().equals(o.getName())) {
                        q = p;
                        break;
                    }
                }
                if (q == null) {
                    // o is not part of l.outerface
                    l.outers.addAsc(loc, o);
                    o.setOwner(this);
                } else {
                    // this name apperas also in l, merge points
                    for (EditablePoint p : new HashSet<>(o.getEditablePoints())) {
                        q.linkPoint(p);
                    }
                }
            }
            for (EditableInnerName i : r.outers.getDesc(loc).values()) {
                EditableInnerName j = null;
                for (EditableInnerName k : l.outers.getDesc(loc).values()) {
                    if (k.getName().equals(i.getName())) {
                        j = k;
                        break;
                    }
                }
                if (j == null) {
                    // i is not part of r.outerface
                    r.outers.addDesc(loc, i);
                }
            }
        }
        Collection<EditableEdge> es = r.edgesProxy.get();
        for (EditableEdge e : es) {
            e.setOwner(this);
        }
        l.onEdgeAdded(es);
        l.onNodeAdded(r.nodesProxy.get());
        r.onEdgeSetChanged();
        r.onNodeSetChanged();
        l.roots.addAll(r.roots);
        l.sites.addAll(r.sites);
        l.inners.join(r.inners);
        assertConsistency();
    }

    /**
     * Juxtapose bigraph in input with the current bigraphbuilder. <br />
     * Perform then {@link DirectedBigraphBuilder#merge()} on the resulting
     * bigraphbuilder. <br />
     * Sites of the bigraph will precede those of the bigraphbuilder in the
     * resulting bigraphbuilder.
     *
     * @param graph bigraph that will be juxtaposed.
     */
    public void leftMergeProduct(DirectedBigraph graph) {
        leftMergeProduct(graph, false);
    }

    /**
     * Juxtapose bigraph in input with the current bigraphbuilder. <br />
     * Perform then {@link DirectedBigraphBuilder#merge()} on the resulting
     * bigraphbuilder. <br />
     * Sites of the bigraph will precede those of the bigraphbuilder in the
     * resulting bigraphbuilder.
     *
     * @param graph bigraph that will be juxtaposed.
     * @param reuse flag. If true, the bigraph in input won't be copied.
     */
    public void leftMergeProduct(DirectedBigraph graph, boolean reuse) {
        leftJuxtapose(graph, reuse);
        merge();
    }

    /**
     * Juxtapose the current bigraphbuilder with the bigraph in input. <br />
     * Perform then {@link DirectedBigraphBuilder#merge()} on the resulting
     * bigraphbuilder. <br />
     * Sites of the bigraphbuilder will precede those of the bigraph in the
     * resulting bigraphbuilder.
     *
     * @param graph bigraph that will be juxtaposed.
     */
    public void rightMergeProduct(DirectedBigraph graph) {
        rightMergeProduct(graph, false);
    }

    /**
     * Juxtapose the current bigraphbuilder with the bigraph in input. <br />
     * Perform then {@link DirectedBigraphBuilder#merge()} on the resulting
     * bigraphbuilder. <br />
     * Sites of the bigraphbuilder will precede those of the bigraph in the
     * resulting bigraphbuilder.
     *
     * @param graph bigraph that will be juxtaposed.
     * @param reuse flag. If true, the bigraph in input won't be copied.
     */
    public void rightMergeProduct(DirectedBigraph graph, boolean reuse) {
        rightJuxtapose(graph, reuse);
        merge();
    }

    private void assertConsistency() {
        if (DEBUG_CONSISTENCY_CHECK && !this.directedBigraph.isConsistent(this))
            throw new RuntimeException("Inconsistent bigraph.");
    }

    private void assertOwner(Owned owned, String obj) {
        if (owned == null)
            throw new IllegalArgumentException(obj + " can not be null.");
        Owner o = owned.getOwner();
        if (o != this)
            throw new UnexpectedOwnerException(obj + " should be owned by this structure.");
    }

    private void assertOrSetOwner(Owned owned, String obj) {
        if (owned == null)
            throw new IllegalArgumentException(obj + " can not be null.");
        Owner o = owned.getOwner();
        if (o == null)
            ((EditableOwned) owned).setOwner(this);
        else if (o != this)
            throw new UnexpectedOwnerException(obj + " already owned by an other structure.");
    }
}
