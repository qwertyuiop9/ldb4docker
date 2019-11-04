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
    public static final String INVALID_POINT = "invalidPoint";
    public static final int INVALID_INDEX_NUMBER = -1;

    public static final String INNER_INTERFACE = "inner";
    public static final String OUTER_INTERFACE = "outer";

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
     * @param outerNameName if specified this parameter, an instance of "Edge", will be used as the "Handle" parameter
     * @param edgeName if specified this parameter, an instance of "OuterName", will be used as the "Handle" parameter
     * @param nodeName if specified this parameter, an instance of "InPort", will be used as the "Handle" parameter
     * @param portIndex index of the port to use as Handle
     * @return the new innerName
     */
    @PostMapping(value = "/addDescNameOuterInterface")
    private InnerName addDescNameOuterInterface(@RequestParam(name = "locality", defaultValue = "-1") int locality,
                                                @RequestParam(name = "name", defaultValue = INVALID_NAME) String name,
                                                @RequestParam(name = "outerName", defaultValue = INVALID_NAME ) String outerNameName,
                                                @RequestParam(name = "edge", defaultValue = INVALID_HANDLE) String edgeName,
                                                @RequestParam(name = "node", defaultValue = INVALID_HANDLE) String nodeName,
                                                @RequestParam(name = "portIndex", defaultValue = INVALID_INDEX) int portIndex) {
        InnerName innerName;

        if (locality == -1) {
            // Invalid locality
            System.out.println("Method 'addDescNameOuterInterface' invalid 'locality parameter");
            return null;
        } else {
            Handle handle = getHandle(outerNameName, edgeName, nodeName, portIndex);
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
    private InnerName addAscNameInnerInterface(@RequestParam(name = "locality", defaultValue = "-1") int locality,
                                               @RequestParam(name = "name", defaultValue = INVALID_NAME) String name,
                                               @RequestParam(name = "outerName", defaultValue = INVALID_NAME ) String outerNameName,
                                               @RequestParam(name = "edge", defaultValue = INVALID_HANDLE) String edgeName,
                                               @RequestParam(name = "node", defaultValue = INVALID_HANDLE) String nodeName,
                                               @RequestParam(name = "portIndex", defaultValue = INVALID_INDEX) int portIndex) {

        InnerName innerName;

        if (locality == -1) {
            return null;
        } else {
            Handle handle = getHandle(outerNameName, edgeName, nodeName, portIndex);
            if (handle != null && name.equals(INVALID_NAME)) {
                innerName = currentBuilder.addAscNameInnerInterface(locality, handle);
            } else if (!name.equals(INVALID_NAME) && handle == null) {
                innerName = currentBuilder.addAscNameInnerInterface(locality, name);
            } else if (!name.equals(INVALID_NAME)) {
                innerName = currentBuilder.addAscNameInnerInterface(locality, name, handle);
            } else {
                System.out.println("One o more parameter not valid");
                innerName = null;
            }
            innerNames.add(innerName);
            return innerName;
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
     * @param parentName  name of the parent the node will be attached to
     * @param useTempHandleList set to true if you want to use the tempHandle list
     * @return the insertion resulting node
     */
    @PostMapping(value = "/nodes")
    private Node addNode(@RequestParam(name = "controlName") String controlName,
                         @RequestParam(name = "parentName", defaultValue = INVALID_NAME) String parentName,
                         @RequestParam(name = "useTempHandleList", defaultValue = "false") boolean useTempHandleList) {

        Node resultNode;
        Parent parent = getParentByName(parentName);
        if (useTempHandleList) {
            resultNode = currentBuilder.addNode(controlName, parent, NamedHandle.getHandleList(tempHandles));
        } else {
            resultNode = currentBuilder.addNode(controlName, parent);
        }
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

    @PostMapping(value = "makeBigraph")
    private DirectedBigraph makeBigraph(@RequestParam(name = "closeBigraph", defaultValue = "false") boolean closeBigraph,
                                                  @RequestParam(name = "name", defaultValue = INVALID_NAME) String name) {
        if (name.equals(INVALID_NAME)) {
            System.out.println("A name for the bigraph must be specified");
            return null;
        }
        boolean nameAlreadyExists = false;
        for (NamedDirectedBigraph namedDirectedBigraph : myDirectedBigraphs) {
            if (namedDirectedBigraph.getName().equals(name)) {
                nameAlreadyExists = true;
                System.out.println("The name for the new bigraph must be unique.");
                return null;
            }
        }
        DirectedBigraph resultingBigraph =  currentBuilder.makeBigraph(closeBigraph);
        myDirectedBigraphs.add(new NamedDirectedBigraph(resultingBigraph, name));
        return resultingBigraph;
    }
    @PostMapping("/clearDirectedBigraphList")
    private String clearDirectedBigraphList() {
        myDirectedBigraphs.clear();
        return "DirectedBigraph list empty.";
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
            OuterName outerName = (OuterName) getHandle(outerNameName, edgeName, nodeName, portIndex);
            if (outerName == null) {
                System.out.println("Invalid outerName specified");
            } else {
                tempHandles.add(new NamedHandle(outerName, "OuterName: " + outerNameName));
                System.out.format("OuterName '%s' added to the temporary handle list\n", outerName.getName());
            }
        } else if (!edgeName.equals(INVALID_HANDLE)) {
            // Specified an Edge instance as Handle
            Edge edge = (Edge) getHandle(outerNameName, edgeName, nodeName, portIndex);
            if (edge == null) {
                System.out.println("Invalid edge specified");
            } else {
                tempHandles.add(new NamedHandle(edge, "Edge: " + edgeName));
                System.out.format("Edge '%s' added to the temporary handle list\n", edge.getEditable().getName());
            }
        } else if (!nodeName.equals(INVALID_HANDLE)) {
            // Specified a InPort instance as Handle
            Node node = getNodeByName(nodeName);
            System.out.format("Node '%s' selected", node.getName());
            if (portIndex != -1) {
                InPort inPort = (InPort) getHandle(outerNameName, edgeName, nodeName, portIndex);
                if (inPort == null) {
                    System.out.println("Invalid port index or node name specified");
                } else {
                    System.out.format("InPort number '%d' (of node '%s') added to the temporary handle list\n", inPort.getNumber(), inPort.getNode().getName());
                    tempHandles.add(new NamedHandle(inPort, "Node: " + nodeName + ", port: " + portIndex));
                }
            } else {
                System.out.println("Please specify a port index");
            }
        } else {
            System.out.println("No valid handle specified...");
        }
    }

    @PostMapping("/addRootToTempList")
    private void addRootToTemp(@RequestParam(name = "name") String rootName) {

        Root selectedRoot = getRootByName(rootName);
        if (selectedRoot == null) {
            System.out.println("The root specified doesn't exist");
        } else {
            tempRoots.add(selectedRoot);
            System.out.format("Root '%s' added to the temporary root list\n", selectedRoot.toString());
        }
    }

    @PutMapping("/clearTempRootList")
    private String clearTempRootList() {
        tempRoots.clear();
        return "Root list empty.";
    }

    @GetMapping("/handles")
    private Handle getHandle(@RequestParam(name = "outerName", defaultValue = INVALID_HANDLE) String outerNameName,
                             @RequestParam(name = "edge", defaultValue = INVALID_HANDLE) String edgeName,
                             @RequestParam(name = "node", defaultValue = INVALID_HANDLE) String nodeName,
                             @RequestParam(name = "portIndex", defaultValue = INVALID_INDEX) int portIndex) {

        Handle resultHandle = null;

        if (!outerNameName.equals(INVALID_HANDLE)) {
            // Specified an OuterName instance as Handle
            resultHandle = getOuterNameByName(outerNameName);
        } else if (!edgeName.equals(INVALID_HANDLE)) {
            // Specified an Edge instance as Handle
            resultHandle = getEdgeByName(edgeName);
        } else if (!nodeName.equals(INVALID_HANDLE)) {
            // Specified a InPort instance as Handle
            Node associatedNode = getNodeByName(nodeName);
            System.out.format("Node '%s' selected", associatedNode.getName());
            if (portIndex != -1) {
                resultHandle =  associatedNode.getInPort(portIndex);
            } else {
                System.out.println("Please specify a port index");
            }
        } else {
            System.out.println("No valid handle specified...");
        }

        return resultHandle;
    }

    @PutMapping("/clearTempHandleList")
    private String clearTempHandleList() {
        tempHandles.clear();
        return "Handles list empty.";
    }

    @PutMapping("/relink")
    public Handle relink(@RequestParam(name = "outerName", defaultValue = INVALID_HANDLE) String outerNameName,
                         @RequestParam(name = "edge", defaultValue = INVALID_HANDLE) String edgeName,
                         @RequestParam(name = "node", defaultValue = INVALID_HANDLE) String nodeName,
                         @RequestParam(name = "portIndex", defaultValue = INVALID_INDEX) int portIndex) {

        Handle specifiedHandle = getHandle(outerNameName, edgeName, nodeName, portIndex);
        Point[] points = (Point[]) tempPoints.toArray();
        Handle resultHandle = null;
        if (specifiedHandle == null) {
            // Link the points to a new fresh edge
            resultHandle = currentBuilder.relink(points);
        } else {
            // Link the point to se specified handle
            resultHandle = (Handle) currentBuilder.relink(specifiedHandle, points);
        }

        System.out.println("Points relinked.");
        return resultHandle;
    }

    @DeleteMapping("outerNames")
    private Edge closeOuterNameFromInterface(@RequestParam(name = "outerName", defaultValue = INVALID_NAME) String outerNameName,
                                             @RequestParam(name = "locality", defaultValue = INVALID_INDEX) int locality,
                                             @RequestParam(name = "interface") String interfaceType) {

        OuterName outerNameToDelete = getOuterNameByName(outerNameName);
        Edge resultEdge = null;
        if (outerNameToDelete == null) {
            System.out.format("No outerName founded with the name '%s'\n", outerNameName);
        } else {
            if (interfaceType.equals(OUTER_INTERFACE)) {
                resultEdge = currentBuilder.closeOuterNameOuterInterface(locality, outerNameToDelete);
            } else if (interfaceType.equals(INNER_INTERFACE)) {
                resultEdge = currentBuilder.closeOuterNameInnerInterface(locality, outerNameToDelete);
            } else {
                return null;
            }
        }

        return resultEdge;
    }

    @DeleteMapping("innerNames")
    private void closeInnerNameFromInterface(@RequestParam(name = "outerName", defaultValue = INVALID_NAME) String innerNameName,
                                             @RequestParam(name = "locality", defaultValue = INVALID_INDEX) int locality,
                                             @RequestParam(name = "interface") String interfaceType) {

        InnerName innerNameToDelete = getInnerNameByName(innerNameName);
        if (innerNameToDelete == null) {
            System.out.format("No innerName founded with the name '%s'\n", innerNameName);
        } else {
            if (interfaceType.equals(OUTER_INTERFACE)) {
                currentBuilder.closeInnerNameOuterInterface(locality, innerNameToDelete);
            } else if (interfaceType.equals(INNER_INTERFACE)) {
                currentBuilder.closeInnerNameInnerInterface(locality, innerNameToDelete);
            }
        }
    }

    @PutMapping(value = "outerNames")
    private void renameOuterNameFromInterface(@RequestParam(name = "oldOuterName", defaultValue = INVALID_NAME) String oldOuterName,
                                              @RequestParam(name = "newOuterName", defaultValue = INVALID_NAME) String newOuterName,
                                              @RequestParam(name = "locality", defaultValue = INVALID_INDEX) int locality,
                                              @RequestParam(name = "interface") String interfaceType) {

        if (oldOuterName.equals(newOuterName)) {
            System.out.println("The new OuterName and the old one must be different");
        } else {
            if (!oldOuterName.equals(INVALID_NAME) && !newOuterName.equals(INVALID_NAME)) {
                OuterName oldOuter = getOuterNameByName(oldOuterName);
                if (oldOuter == null) {
                    System.out.println("The specified outerName doesn't exists");
                } else {
                    if (interfaceType.equals(INNER_INTERFACE)) {
                        currentBuilder.renameOuterNameInnerInterface(locality, oldOuter, newOuterName);
                    } else if (interfaceType.equals(OUTER_INTERFACE)) {
                        currentBuilder.renameOuterNameOuterInterface(locality, oldOuter, newOuterName);
                    }
                }
            }
        }
    }

    @PutMapping(value = "innerNames")
    private void renameInnerNameFromInterface(@RequestParam(name = "oldInnerName", defaultValue = INVALID_NAME) String oldInnerName,
                                              @RequestParam(name = "newInnerName", defaultValue = INVALID_NAME) String newInnerName,
                                              @RequestParam(name = "locality", defaultValue = INVALID_INDEX) int locality,
                                              @RequestParam(name = "interface") String interfaceType) {

        if (oldInnerName.equals(newInnerName)) {
            System.out.println("The new InnerName and the old one must be different");
        } else {
            if (!oldInnerName.equals(INVALID_NAME) && !newInnerName.equals(INVALID_NAME)) {
                InnerName oldInner = getInnerNameByName(oldInnerName);
                if (oldInner == null) {
                    System.out.println("The specified innerName doesn't exists");
                } else {
                    if (interfaceType.equals(INNER_INTERFACE)) {
                        currentBuilder.renameInnerNameInnerInterface(locality, oldInner, newInnerName);
                    } else if (interfaceType.equals(OUTER_INTERFACE)) {
                        currentBuilder.renameInnerNameOuterInterface(locality, oldInner, newInnerName);
                    }
                }
            }
        }
    }

    @PostMapping("/addPointToTempList")
    private void addPointToTempList(@RequestParam(name = "innerName", defaultValue = INVALID_POINT) String innerNameName,
                                    @RequestParam(name = "node", defaultValue = INVALID_POINT) String nodeName,
                                    @RequestParam(name = "portIndex", defaultValue = INVALID_INDEX) int portIndex) {

        InnerName innerName = getInnerNameByName(innerNameName);
        if (!innerNameName.equals(INVALID_POINT)) {
            // Specified an InnerName instance as Point
            if (innerName == null) {
                System.out.println("Invalid innerName specified");
            } else {
                tempPoints.add(new NamedPoint(innerName, "InnerName: " + innerNameName));
                System.out.format("InnerName '%s' added to the temporary point list\n", innerName.getName());
            }
        } else if (!nodeName.equals(INVALID_POINT)) {
            // Specified a OutPort instance as Point
            Node node = getNodeByName(nodeName);
            System.out.format("Node '%s' selected", node.getName());
            if (portIndex != -1) {
                OutPort outPort = node.getOutPort(portIndex);
                if (outPort == null) {
                    System.out.println("Invalid port index or node name specified");
                } else {
                    System.out.format("OutPort number '%d' (of node '%s') added to the temporary point list\n", outPort.getNumber(), outPort.getNode().getName());
                    tempPoints.add(new NamedPoint(outPort, "Node: " + nodeName + ", port: " + portIndex));
                }
            } else {
                System.out.println("Please specify a port index");
            }
        } else {
            System.out.println("No valid point specified...");
        }
    }

    @PutMapping("/clearTempPointList")
    private void clearTempPointList() { tempPoints.clear(); }

    @PutMapping(value = "/mergeBigraph")
    private Root merge() {
        return currentBuilder.merge();
    }

    @PutMapping(value = "/mergeRegions")
    private Root mergeBigraphRegions(@RequestParam(name = "index", defaultValue = INVALID_INDEX) int index) {

        if (index == -1) {
            System.out.println("A valid index has to be specified");
            return null;
        } else {
            int[] roots = new int[tempRoots.size()];
            for (int i=0; i<roots.length; i++) {
                roots[i] = Integer.parseInt(tempRoots.get(i).getEditable().toString().substring(0,1));
            }
            return currentBuilder.merge(index, roots);
        }

    }

    @PutMapping(value = "/ground")
    private void ground() { currentBuilder.ground(); }

    @PutMapping(value = "/leftJuxtapose")
    private String leftJuxtapose(@RequestParam(name = "bigraphName", defaultValue = INVALID_NAME) String bigraphName,
                                 @RequestParam(name = "reuse", defaultValue = "false") boolean reuse) {

        DirectedBigraph selectedBigraph = null;
        for (NamedDirectedBigraph namedDirectedBigraph : myDirectedBigraphs) {
            if (namedDirectedBigraph.getName().equals(bigraphName)) {
                selectedBigraph = namedDirectedBigraph.getDirectedBigraph();
            }
        }

        if (selectedBigraph == null) {
            return "Searched bigraph not found in the list of available bigraphs";
        }

        currentBuilder.leftJuxtapose(selectedBigraph, reuse);
        return "Left juxtapose done!";
    }

    @PutMapping(value = "/rightJuxtapose")
    private String rightJuxtapose(@RequestParam(name = "bigraphName", defaultValue = INVALID_NAME) String bigraphName,
                                  @RequestParam(name = "reuse", defaultValue = "false") boolean reuse) {

        DirectedBigraph selectedBigraph = null;
        for (NamedDirectedBigraph namedDirectedBigraph : myDirectedBigraphs) {
            if (namedDirectedBigraph.getName().equals(bigraphName)) {
                selectedBigraph = namedDirectedBigraph.getDirectedBigraph();
            }
        }

        if (selectedBigraph == null) {
            return "Searched bigraph not found in the list of available bigraphs";
        }

        currentBuilder.rightJuxtapose(selectedBigraph, reuse);
        return "Right juxtapose done!";
    }

    @PutMapping(value = "/innerCompose")
    private String innerCompose(@RequestParam(name = "bigraphName", defaultValue = INVALID_NAME) String bigraphName,
                                @RequestParam(name = "reuse", defaultValue = "false") boolean reuse) {

        DirectedBigraph selectedBigraph = null;
        for (NamedDirectedBigraph namedDirectedBigraph : myDirectedBigraphs) {
            if (namedDirectedBigraph.getName().equals(bigraphName)) {
                selectedBigraph = namedDirectedBigraph.getDirectedBigraph();
            }
        }

        if (selectedBigraph == null) {
            return "Searched bigraph not found in the list of available bigraphs";
        }

        currentBuilder.innerCompose(selectedBigraph, reuse);
        return "Inner composition done!";
    }

    @PutMapping(value = "/outerCompose")
    private String outerCompose(@RequestParam(name = "bigraphName", defaultValue = INVALID_NAME) String bigraphName,
                                @RequestParam(name = "reuse", defaultValue = "false") boolean reuse) {

        DirectedBigraph selectedBigraph = null;
        for (NamedDirectedBigraph namedDirectedBigraph : myDirectedBigraphs) {
            if (namedDirectedBigraph.getName().equals(bigraphName)) {
                selectedBigraph = namedDirectedBigraph.getDirectedBigraph();
            }
        }

        if (selectedBigraph == null) {
            return "Searched bigraph not found in the list of available bigraphs";
        }

        currentBuilder.outerCompose(selectedBigraph, reuse);
        return "Outer composition done!";
    }

    @PutMapping(value = "/leftParallelProduct")
    private String leftParallelProduct(@RequestParam(name = "bigraphName", defaultValue = INVALID_NAME) String bigraphName,
                                       @RequestParam(name = "reuse", defaultValue = "false") boolean reuse) {

        DirectedBigraph selectedBigraph = null;
        for (NamedDirectedBigraph namedDirectedBigraph : myDirectedBigraphs) {
            if (namedDirectedBigraph.getName().equals(bigraphName)) {
                selectedBigraph = namedDirectedBigraph.getDirectedBigraph();
            }
        }

        if (selectedBigraph == null) {
            return "Searched bigraph not found in the list of available bigraphs";
        }

        currentBuilder.leftParallelProduct(selectedBigraph, reuse);
        return "Left parallel product done!";
    }

    @PutMapping(value = "/rightParallelProduct")
    private String rightParallelProduct(@RequestParam(name = "bigraphName", defaultValue = INVALID_NAME) String bigraphName,
                                       @RequestParam(name = "reuse", defaultValue = "false") boolean reuse) {

        DirectedBigraph selectedBigraph = null;
        for (NamedDirectedBigraph namedDirectedBigraph : myDirectedBigraphs) {
            if (namedDirectedBigraph.getName().equals(bigraphName)) {
                selectedBigraph = namedDirectedBigraph.getDirectedBigraph();
            }
        }

        if (selectedBigraph == null) {
            return "Searched bigraph not found in the list of available bigraphs";
        }

        currentBuilder.rightParallelProduct(selectedBigraph, reuse);
        return "Right parallel product done!";
    }

    @PutMapping(value = "/leftMergeProduct")
    private String leftMergeProduct(@RequestParam(name = "bigraphName", defaultValue = INVALID_NAME) String bigraphName,
                                    @RequestParam(name = "reuse", defaultValue = "false") boolean reuse) {

        DirectedBigraph selectedBigraph = null;
        for (NamedDirectedBigraph namedDirectedBigraph : myDirectedBigraphs) {
            if (namedDirectedBigraph.getName().equals(bigraphName)) {
                selectedBigraph = namedDirectedBigraph.getDirectedBigraph();
            }
        }

        if (selectedBigraph == null) {
            return "Searched bigraph not found in the list of available bigraphs";
        }

        currentBuilder.leftMergeProduct(selectedBigraph, reuse);
        return "Left merge product done!";
    }

    @PutMapping(value = "/rightMergeProduct")
    private String rightMergeProduct(@RequestParam(name = "bigraphName", defaultValue = INVALID_NAME) String bigraphName,
                                     @RequestParam(name = "reuse", defaultValue = "false") boolean reuse) {

        DirectedBigraph selectedBigraph = null;
        for (NamedDirectedBigraph namedDirectedBigraph : myDirectedBigraphs) {
            if (namedDirectedBigraph.getName().equals(bigraphName)) {
                selectedBigraph = namedDirectedBigraph.getDirectedBigraph();
            }
        }

        if (selectedBigraph == null) {
            return "Searched bigraph not found in the list of available bigraphs";
        }

        currentBuilder.rightMergeProduct(selectedBigraph, reuse);
        return "Right merge product done!";
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