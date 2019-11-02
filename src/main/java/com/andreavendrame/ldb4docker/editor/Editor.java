package com.andreavendrame.ldb4docker.editor;

import com.andreavendrame.ldb4docker.myjlibbig.Interface;
import com.andreavendrame.ldb4docker.myjlibbig.ldb.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedList;
import java.util.List;

import static com.andreavendrame.ldb4docker.editor.EditingEnvironment.*;

@RestController
@RequestMapping(value = "/editor")
public class Editor {

    private static final String INVALID_TYPE = "noType";
    public static final String INVALID_NAME = "invalidName";
    public static final String INVALID_HANDLE = "invalidHandle";
    public static final String INVALID_INDEX = "-1";
    public static final int INVALID_INDEX_NUMBER = -1;

    @Autowired
    private RestTemplate restTemplate;

    @PostMapping(value = "/directedControls")
    private String addDirectedControl(@RequestParam(name = "controlName") String controlName,
                                      @RequestParam(name = "arityIn") int arityIn,
                                      @RequestParam(name = "arityOut") int arityOut,
                                      @RequestParam(name = "active") boolean active) {

        System.out.format("Verifica della validità delle arietà...");
        if (arityIn < 0 || arityOut < 0 || arityIn > 2 || arityOut > 2) {
            System.out.format("Errore - Una o più arietà non valide\n");
            return "L'arietà di almeno una porta non è valida";
        } else {
            System.out.format("OK!\n");
            System.out.format("Aggiunta del directedControl '%s'...", controlName);
            bigraphControls.add(new DirectedControl(controlName, active, arityOut, arityIn));
            System.out.format("OK!\n");
            return String.format("Aggiunto il controllo '%s' (attivo: %b) con arityIn %d e arityOut %d", controlName, active, arityIn, arityOut);
        }
    }

    @GetMapping(value = "/directedControls")
    private List<DirectedControl> getBigraphControls() {
        return bigraphControls;
    }

    @PostMapping(value = "/directedSignatures")
    private String createDirectedSignature() {
        System.out.format("Creazione della directedSignature...");
        currentSignature = new DirectedSignature(bigraphControls);
        System.out.format("OK!\n");
        return currentSignature.toString();
    }

    @PostMapping(value = "/start-editing")
    public DirectedBigraphBuilder createBuilder() {

        System.out.format("Creating the builder instance...");
        currentBuilder = new DirectedBigraphBuilder(currentSignature);
        System.out.println("OK!\n");
        return currentBuilder;

    }

    /**
     * @param locality of the bigraph in which add the root
     * @return the newly added root if it has been possible to add it without errors
     */
    @PostMapping(value = "/roots")
    private Root addRoot(@RequestParam(value = "locality", defaultValue = "-1") int locality) {
        System.out.println("Indice della radice: " + locality);
        Root root;
        if (locality == -1) {
            // Add a root at the last locality
            root = currentBuilder.addRoot();
        } else {
            // Adding a root at the selected locality (if valid)
            root = currentBuilder.addRoot(locality);
        }
        return root;
    }

    /**
     * @param index    position of the root to get in the root list
     * @param rootName name of the root to get
     * @return there can be 3 possibility specifing one or the other parameter
     * 1) index not specified or index == -1: return the entire root list;
     * 2) index specified: return a one item list with the specified root;
     * 3) name specificied: return a one item list with the specified root.
     * Method notes:
     * - If the index is not valid (e.g. index out of bound)
     * or the name is not valid the method return an empty list
     */
    @GetMapping(value = "/roots")
    private List<Root> getRoot(@RequestParam(name = "index", defaultValue = "-1") int index,
                               @RequestParam(name = "name", defaultValue = INVALID_NAME) String rootName) {

        List<Root> rootList = new LinkedList<>();
        if (rootName.equals(INVALID_NAME)) {
            if (index == -1) {
                return new LinkedList<>(currentBuilder.getRoots());
            } else {
                rootList = new LinkedList<>(currentBuilder.getRoots());
                Root selectedRoot = rootList.get(index);
                rootList.clear();
                rootList.add(selectedRoot);
                return rootList;
            }
        } else {
            rootList.add(getRootByName(rootName));
            return rootList;
        }
    }

    @DeleteMapping(value = "/roots")
    private void removeRoot(@RequestParam(name = "locality", defaultValue = "-1") int locality) {

        if (locality != -1) {
            currentBuilder.removeRoot(locality);
        } else {
            System.out.println("Index of the root to delete not specified... Try again specifing the locality");
        }
    }

    @PostMapping(value = "/addDescNameInnerInterface")
    private OuterName addDescNameInnerInterface(@RequestParam(name = "locality", defaultValue = "-1") int localityIndex,
                                                @RequestParam(name = "name", defaultValue = "") String name) {
        if (localityIndex == -1) {
            return null;
        } else {
            OuterName outerName;
            if (name.equals("")) {
                outerName = currentBuilder.addDescNameInnerInterface(localityIndex);
            } else {
                outerName = currentBuilder.addDescNameInnerInterface(localityIndex, name);
            }
            outerNames.add(outerName);
            return outerName;
        }
    }

    /**
     * @param locality to use
     * @param name of the innerName to create
     * @param edgeIndex if specified this parameter, an instance of "Edge", will be used as the "Handle" parameter
     * @param outerNameIndex if specified this parameter, an instance of "OuterName", will be used as the "Handle" parameter
     * @param inPortIndex if specified this parameter, an instance of "InPort", will be used as the "Handle" parameter
     * @return the new innerName
     */
    @PostMapping(value = "/addDescNameOuterInterface")
    private InnerName addDescNameOuterInterface(@RequestParam(name = "locality", defaultValue = "-1") int locality,
                                                @RequestParam(name = "name", defaultValue = INVALID_NAME) String name,
                                                @RequestParam(name = "edgeIndex", defaultValue = INVALID_INDEX ) int edgeIndex,
                                                @RequestParam(name = "outerNameIndex", defaultValue = INVALID_INDEX) int outerNameIndex,
                                                @RequestParam(name = "inPortIndex", defaultValue = INVALID_INDEX) int inPortIndex) {
        InnerName innerName;

        if (locality == -1) {
            System.out.println("Method 'addDescNameOuterInterface' invalid 'locality parameter");
            innerName = currentBuilder.addDescNameOuterInterface(locality);
        } else {
            Handle handle = getHandle(outerNameIndex, edgeIndex, inPortIndex);
            if (handle != null && name.equals(INVALID_NAME)) {
                innerName = currentBuilder.addDescNameOuterInterface(locality, handle);
            } else if (!name.equals(INVALID_NAME) && handle == null) {
                innerName = currentBuilder.addDescNameOuterInterface(locality, name);
            } else if (!name.equals(INVALID_NAME)) {
                innerName = currentBuilder.addDescNameOuterInterface(locality, name, handle);
            } else {
                System.out.println("One o more parameter not valid");
                innerName = null;
            }
        }

        innerNames.add(innerName);
        return innerName;

    }

    @PostMapping(value = "/addAscNameOuterInterface")
    private OuterName addAscNameOuterInterface(@RequestParam(name = "locality", defaultValue = "-1") int localityIndex,
                                               @RequestParam(name = "name", defaultValue = "") String name) {
        if (localityIndex == -1) {
            return null;
        } else {
            OuterName outerName;
            if (name.equals("")) {
                outerName = currentBuilder.addAscNameOuterInterface(localityIndex);
            } else {
                outerName = currentBuilder.addAscNameOuterInterface(localityIndex, name);
            }
            outerNames.add(outerName);
            System.out.format("Nome '%s' aggiunto alla lista degli outerName\n", name);
            return outerName;
        }
    }

    @PostMapping(value = "/addAscNameInnerInterface")
    private InnerName addAscNameInnerInterface(@RequestParam(name = "locality", defaultValue = "-1") int localityIndex,
                                               @RequestParam(name = "name", defaultValue = "") String name,
                                               @RequestParam(name = "handleType", defaultValue = "") String handleType,
                                               @RequestParam(name = "handleIndex", defaultValue = "-1") int handleIndex) {

        if (localityIndex == INVALID_INDEX_NUMBER) {
            return null;
        } else {
            if (name.equals("") && handleIndex == -1) {     // Solo la località specificata
                return currentBuilder.addAscNameInnerInterface(localityIndex);
            } else if (name.equals("")) {                   // Località e Handle specificati
                return currentBuilder.addAscNameInnerInterface(localityIndex, getHandle(handleType, handleIndex));
            } else if (handleIndex == -1) {                 // Località e Name specificati
                return currentBuilder.addAscNameInnerInterface(localityIndex, name);
            } else {
                return currentBuilder.addAscNameInnerInterface(localityIndex, name, getHandle(handleType, handleIndex));
            }
        }
    }

    @GetMapping(value = "/signatures")
    private DirectedSignature getSignature() {
        return currentBuilder.getSignature();
    }

    @GetMapping(value = "/outerNames")
    private List<OuterName> getOuterNames(@RequestParam(name = "index", defaultValue = "-1") int index,
                                          @RequestParam(name = "name", defaultValue = INVALID_NAME) String name) {

        if (name.equals(INVALID_NAME)) {
            if (index == -1) {
                return outerNames;
            } else {
                List<OuterName> oneItemList = new LinkedList<>();
                oneItemList.add(outerNames.get(index));
                return oneItemList;
            }
        } else {
            List<OuterName> oneItemList = new LinkedList<>();
            oneItemList.add(getOuterNameByName(name));
            return oneItemList;
        }
    }

    @GetMapping(value = "innerNames")
    private List<InnerName> getInnerNames(@RequestParam(name = "index", defaultValue = "-1") int index) {

        if (index == -1) {
            return innerNames;
        } else {
            List<InnerName> oneItemList = new LinkedList<>();
            oneItemList.add(innerNames.get(index));
            return oneItemList;
        }
    }

    @GetMapping(value = "/edges")
    public static List<Edge> getEdges(@RequestParam(name = "name", defaultValue = INVALID_NAME) String name,
                                @RequestParam(name = "index", defaultValue = INVALID_INDEX) int index) {

        System.out.println("Method 'getEdges' ---->");

        List<Edge> edges = new LinkedList<>(currentBuilder.getEdges());
        for (Edge e : edges) {
            System.out.format("---------- Edge name: '%s'\n", e.getEditable().getName());
        }

        if (name.equals(INVALID_NAME)) {
            if (index == -1) {
                return edges;
            } else {
                List<Edge> oneItemList = new LinkedList<>();
                oneItemList.add(edges.get(index));
                return oneItemList;
            }
        } else {
            List<Edge> oneItemList = new LinkedList<>();
            for (Edge edge : edges) {
                if (edge.getEditable().getName().equals(name)) {
                    oneItemList.add(edge);
                }
            }

            System.out.format("---------- List length: %d\n", oneItemList.size());
            return oneItemList;
        }
    }

    @GetMapping(value = "/nodes")
    private List<Node> getNodes(@RequestParam(value = "index", defaultValue = "-1") int index,
                                @RequestParam(name = "name", defaultValue = INVALID_NAME) String name) {

        List<Node> oneItemList = new LinkedList<>();
        if (name.equals(INVALID_NAME)) {
            if (index == -1) {
                return new LinkedList<>(currentBuilder.getNodes());
            } else {
                oneItemList.add(new LinkedList<>(currentBuilder.getNodes()).get(index));
                return oneItemList;
            }
        } else {
            oneItemList.add(getNodeByName(name));
            return oneItemList;
        }
    }

    /**
     * @param controlName name of the control to insert
     * @param parentType  one between "root", "editableParent", "node"
     * @param parentIndex index of the parent to get from the related list
     * @param parentName  name of the parent to get from the related list
     *                    Method notes:
     *                    - The parameter "controlName" must be always setted
     *                    Method uses - Parameter combinations:
     *                    1) "parentType" + "parentIndex"
     *                    2) "parentType" + "parentName"
     *                    3) "name" only
     * @return the insertion resulting node
     */
    @PostMapping(value = "/nodes")
    private Node addNode(@RequestParam(name = "controlName") String controlName,
                         @RequestParam(name = "parentType", defaultValue = INVALID_TYPE) String parentType,
                         @RequestParam(name = "parentIndex", defaultValue = "-1") int parentIndex,
                         @RequestParam(name = "parentName", defaultValue = "") String parentName) {

        Node resultNode;
        Parent parent = null;
        System.out.format("---------- Parent type '%s', ", parentType);
        if (parentType.equals(INVALID_TYPE)) {
            parent = getParentByName(parentName);
        } else {
            if (parentIndex != -1) {
                switch (parentType) {
                    case PARENT_NODE:
                        parent = new LinkedList<>(currentBuilder.getNodes()).get(parentIndex);
                        break;
                    case PARENT_ROOT:
                        parent = new LinkedList<>(currentBuilder.getNodes()).get(parentIndex);
                        break;
                    case PARENT_EDITABLE_PARENT:
                        //parent = editableParents.get(parentIndex);
                        break;
                    default:
                        parent = null;
                        break;
                }
            } else {
                switch (parentType) {
                    case PARENT_NODE:
                        parent = getNodeByName(parentName);
                        break;
                    case PARENT_ROOT:
                        parent = getRootByName(parentName);
                        break;
                    case PARENT_EDITABLE_PARENT:
                        //parent = getEditableParentByName(parentName);
                        break;
                    default:
                        parent = null;
                        break;
                }
            }
        }

        resultNode = currentBuilder.addNode(controlName, parent);
        System.out.println("Number of nodes in the list: " + new LinkedList<>(currentBuilder.getNodes()).size());
        return resultNode;

    }

    @PostMapping(value = "/sites")
    private Site addSite(@RequestParam(name = "parentName", defaultValue = INVALID_NAME) String parentName) {

        if (parentName.equals(INVALID_NAME)) {
            return null;
        } else {
            Parent parent = getParentByName(parentName);
            return currentBuilder.addSite(parent);
        }
    }

    @GetMapping(value = "/parents")
    private Parent getParent(@RequestParam( name = "parentName", defaultValue = INVALID_NAME) String parentName) {
        return getParentByName(parentName);
    }

    @GetMapping(value = "/sites")
    private List<Site> getSites(@RequestParam(name = "index", defaultValue = INVALID_INDEX) int index,
                                @RequestParam(name = "name", defaultValue = INVALID_NAME) String name) {

        if (name.equals(INVALID_NAME)) {
            if (index == -1) {
                return sites;
            } else {
                List<Site> oneItemList = new LinkedList<>();
                oneItemList.add(sites.get(index));
                return oneItemList;
            }
        } else {
            List<Site> oneItemList = new LinkedList<>();
            for (Site site : sites) {
                if (site.getEditable().toString().equals(name)) {
                    oneItemList.add(site);
                }
            }
            if (oneItemList.size() == 1) {
                return oneItemList;
            } else {
                return null;
            }
        }
    }

    @DeleteMapping(value = "/sites")
    private void removeSite(@RequestParam(name = "index", defaultValue = INVALID_INDEX) int index,
                            @RequestParam(name = "name", defaultValue = INVALID_NAME) String name) {

        if (index != -1) {
            currentBuilder.closeSite(index);
        } else if (!name.equals(INVALID_NAME)) {
            boolean deleted = false;
            for (Site site : sites) {
                if (site.getEditable().toString().equals(name)) {
                    currentBuilder.closeSite(site);
                    deleted = true;
                }
            }
            if (deleted) {
                System.out.format("Site '%s'specified deleted with success!\n", name);
            } else {
                System.out.format("Site '%s' specified not found...Try again with another name.\n", name);
            }
        } else {
            System.out.println("Index of the site to delete not specified... Try again by specifing the index");
        }
    }

    /**
     * @param nodeIndex   index of the node to which to connect the handle
     * @param portMode    how to connect the handle to the node: 0 for READ_MODE, 1 for WRITE_MODE
     * @param handleType  handle type in the set {"outerName", "editableHandle", "inPort", "edge"}
     * @param handleIndex index of the handle in its relative list
     */
    @PostMapping(value = "/linkNameToNode")
    private void linkNameToNode(@RequestParam(name = "nodeIndex", defaultValue = "-1") int nodeIndex,
                                @RequestParam(name = "portMode", defaultValue = "-1") int portMode,
                                @RequestParam(name = "handleType") String handleType,
                                @RequestParam(name = "handleIndex") int handleIndex) {

        if (portMode < 0 || portMode > 1) {
            System.out.println("Modalità di collegamento non valida. Scegliere 0 per READ o 1 per WRITE.");
        } else {
            Handle selectedHandle = getHandle(handleType, handleIndex);
            if (nodeIndex == -1) {
                System.out.println("Indice del nodo non valido");
            } else {
                Node selectedNode = new LinkedList<>(currentBuilder.getNodes()).get(nodeIndex);
                selectedNode.getOutPort(portMode).getEditable().setHandle(selectedHandle.getEditable());
                System.out.format("Collegato '%s' al nodo %s!\n", selectedHandle.toString(), selectedNode.toString());
            }
        }
    }

    @GetMapping(value = "/innerInterfaces")
    private Interface getInnerInterface() {
        return currentBuilder.getInnerInterface();
    }

    @GetMapping(value = "/outerInterfaces")
    private Interface getOuterInterface() {
        return currentBuilder.getOuterInterface();
    }


    @GetMapping(value = "services")
    private List<String> gerServices(@RequestParam(value = "index", defaultValue = "-1") int index) {
        if (index == -1) {
            return services;
        } else {
            List<String> oneItemList = new LinkedList<>();
            oneItemList.add(services.get(index));
            return oneItemList;
        }
    }

    @GetMapping(value = "makeBigraph")
    private DirectedBigraph.Interface makeBigraph(@RequestParam(name = "closeBigraph", defaultValue = "false") boolean closeBigraph) {
        DirectedBigraph resultingBigraph =  currentBuilder.makeBigraph(closeBigraph);
        return resultingBigraph.getOuterInterface();
    }

    /**
     * @param handleType  a type between "outerName", "editableHandle", "inPort", "edge"
     * @param handleIndex index of the handle in the selected list
     * @return the selected handle instance
     */
    private Handle getHandle(String handleType, int handleIndex) {

        switch (handleType) {
            case HANDLE_OUTER_NAME:
                return outerNames.get(handleIndex);
            case HANDLE_EDITABLE_HANDLE:
                return null;
                //return editableHandles.get(handleIndex);
            case HANDLE_IN_PORT:
                return inPorts.get(handleIndex);
            default:
                return getEdges(INVALID_NAME, handleIndex).get(0);
        }

    }

    private Handle getHandle(int outerNameIndex, int edgeIndex, int editableInPortIndex) {
        if (outerNameIndex != INVALID_INDEX_NUMBER && edgeIndex == INVALID_INDEX_NUMBER && editableInPortIndex == INVALID_INDEX_NUMBER) {
            // Selected an outerName as handle
            return outerNames.get(outerNameIndex);
        } else if (outerNameIndex == INVALID_INDEX_NUMBER && edgeIndex != INVALID_INDEX_NUMBER && editableInPortIndex == INVALID_INDEX_NUMBER) {
            // Selected an edge as handle
            return getEdges(INVALID_NAME, edgeIndex).get(0);
        } else if (outerNameIndex == INVALID_INDEX_NUMBER && edgeIndex == INVALID_INDEX_NUMBER && editableInPortIndex != INVALID_INDEX_NUMBER) {
            // Selected a inPort as handle
            return inPorts.get(editableInPortIndex);
        } else {
            System.out.println("Method 'getHandle' - Select only one type of handle");
            return null;
        }
    }

    /**
     * This method will reset all the builder variables and the builder instance itself
     */
    @PostMapping(value = "reset")
    private void resetCurrentBuilderToZero() {

        outerNames.clear();
        innerNames.clear();
        sites.clear();
        bigraphControls.clear();
        services.clear();
        inPorts.clear();
        points.clear();

        currentSignature = null;
        currentBuilder = null;

    }

    @GetMapping("/isClosed")
    private boolean isClosed() { return currentBuilder.isClosed(); }

    @GetMapping("/isEmpty")
    private boolean isEmpty() { return currentBuilder.isEmpty(); }

    @GetMapping("/isGround")
    private boolean isGround() { return currentBuilder.isGround(); }

    @GetMapping("/containsOuterName")
    public boolean containsOuterName(@RequestParam(value = "name", defaultValue = INVALID_NAME) String name) {

        if (name.equals(INVALID_NAME)) {
            System.out.println("The name of the 'OuterName' instance has to be specified");
            return false;
        } else {
            return currentBuilder.containsOuterName(name);
        }
    }

    @GetMapping("/containsInnerName")
    public boolean containsInnerName(@RequestParam(value = "name", defaultValue = INVALID_NAME) String name) {

        if (name.equals(INVALID_NAME)) {
            System.out.println("The name of the 'InnerName' instance has to be specified");
            return false;
        } else {
            return currentBuilder.containsInnerName(name);
        }
    }


    private void test(DirectedBigraphBuilder builder) {

        //builder.

    }

    @PostMapping("/addHandleToTempList")
    private void addHandleToTemp(@RequestParam(name = "outerName", defaultValue = INVALID_HANDLE) String outerNameName,
                                 @RequestParam(name = "edge", defaultValue = INVALID_HANDLE) String edgeName,
                                 @RequestParam(name = "node", defaultValue = INVALID_HANDLE) String nodeName,
                                 @RequestParam(name = "portIndex", defaultValue = INVALID_INDEX) int portIndex) {

        if (!outerNameName.equals(INVALID_HANDLE)) {
            // Specified an OuterName instance as Handle
            OuterName outerName = getOuterNameByName(outerNameName);
            if (outerName == null) {
                System.out.println("Invalid outerName specified");
            } else {
                tempHandle.add(new NamedHandle(outerName, outerNameName));
                System.out.format("OuterName '%s' added to the temporary handle list\n", outerName.getName());
            }
        } else if (!edgeName.equals(INVALID_HANDLE)) {
            // Specified an Edge instance as Handle
            Edge edge = getEdgeByName(edgeName);
            if (edge == null) {
                System.out.println("Invalid edge specified");
            } else {
                tempHandle.add(new NamedHandle(edge, edgeName));
                System.out.format("Edge '%s' added to the temporary handle list\n", edge.getEditable().getName());
            }
        } else if (!nodeName.equals(INVALID_HANDLE)) {
            // Specified a InPort instance as Handle
            Node node = getNodeByName(nodeName);
            System.out.format("Node '%s' selected", node.getName());
            if (portIndex != -1) {
                InPort inPort =  node.getInPort(portIndex);
                if (inPort == null) {
                    System.out.println("Invalid port index or node name specified");
                } else {
                    System.out.format("InPort number '%d' (of node '%s') added to the temporary handle list\n", inPort.getNumber(), inPort.getNode().getName());
                    tempHandle.add(new NamedHandle(inPort, "Node " + nodeName + ", port " + portIndex));
                }
            } else {
                System.out.println("Please specify a port index");
            }
        } else {
            System.out.println("No valid handle specified...");
        }
    }

    @PostMapping("/clearTempHandleList")
    private void clearTempHandleList() {
        tempHandle.clear();
    }

    /**
     * @param parentName name of the parent to search
     * @return the searched parent if it has been founded, null else
     */
    private Parent getParentByName(String parentName) {

        List<Root> roots = new LinkedList<>(currentBuilder.getRoots());
        List<Node> nodes = new LinkedList<>(currentBuilder.getNodes());

        for (Root root : roots) {
            if (root.toString().equals(parentName)) {
                return root;
            }
        }

        for (Node node : nodes) {
            if (node.getName().equals(parentName)) {
                return node;
            }
        }


        return null;
    }

    /**
     * @param rootName name of the root to get. Every root name must be unique for the considered builder
     * @return the root searched if it is present in the list, null else
     */
    private Root getRootByName(String rootName) {
        Root requestedRoot = null;
        for (Root root : currentBuilder.getRoots()) {
            if (root.toString().equals(rootName)) {
                requestedRoot = root;
            }
        }
        return requestedRoot;
    }

    /**
     * @param nodeName name of the node to get. Every node name must be unique for the considered builder
     * @return the node searched if it is present in the list, null else
     */
    private Node getNodeByName(String nodeName) {
        Node requestedNode = null;
        for (Node node : new LinkedList<>(currentBuilder.getNodes())) {
            if (node.getName().equals(nodeName)) {
                requestedNode = node;
            }
        }

        return requestedNode;
    }

    /**
     * @param name of the OuterName instance to get
     * @return the OuterName instance if founded, null else
     */
    private OuterName getOuterNameByName(String name) {
        OuterName selectedOuterName = null;
        for (OuterName outerName : outerNames) {
            if (outerName.getName().equals(name)) {
                selectedOuterName = outerName;
            }
        }
        return selectedOuterName;
    }

    /**
     * @param name of the InnerName instance to get
     * @return the InnerName instance if founded, null else
     */
    private InnerName getInnerNameByName(String name) {
        InnerName selectedInnerName = null;
        for (InnerName innerName : innerNames) {
            if (innerName.getName().equals(name)) {
                selectedInnerName = innerName;
            }
        }
        return selectedInnerName;
    }

    /**
     * @param name of the Edge instance to get
     * @return the Edge instance if founded, null else
     */
    private Edge getEdgeByName(String name) {
        Edge selectedEdge = null;
        for (Edge edge : new LinkedList<>(currentBuilder.getEdges())) {
            if (edge.getEditable().getName().equals(name)) {
                selectedEdge = edge;
            }
        }
        return selectedEdge;
    }

}