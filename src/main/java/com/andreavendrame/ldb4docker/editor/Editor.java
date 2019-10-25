package com.andreavendrame.ldb4docker.editor;

import com.andreavendrame.ldb4docker.myjlibbig.Interface;
import com.andreavendrame.ldb4docker.myjlibbig.ldb.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static com.andreavendrame.ldb4docker.BigraphImportController.READ_MODE;
import static com.andreavendrame.ldb4docker.BigraphImportController.WRITE_MODE;
import static com.andreavendrame.ldb4docker.editor.EditingEnvironment.*;

@RestController
@RequestMapping(value = "/editor")
public class Editor {

    private static final String INVALID_TYPE = "noType";

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

        System.out.format("Creo il builder...");
        currentBuilder = new DirectedBigraphBuilder(currentSignature);
        System.out.println("OK!\n");
        return currentBuilder;

    }

    /**
     * @param locality località del bigrafo in cui aggiungere la radice
     * @return la radice appena aggiunta se è stato possibile aggiungerla senza errori
     */
    @PostMapping(value = "/roots")
    private Object addRoot(@RequestParam(value = "locality", defaultValue = "-1") int locality) {
        System.out.println("Indice della radice: " + locality);
        Root root;
        if (locality == -1) {
            // Aggiungo una radice senza località
            root = currentBuilder.addRoot();
            roots.add(root);
        } else {
            // Aggiungo una radice con località
            root = currentBuilder.addRoot(locality);
            roots.add(locality, root);
        }
        return root;
    }

    /**
     * @param index posizione della radice del bigrafo nella lista delle radici disponibili
     * @return l'intera lista di radici del builder se {@param index} è -1; la root in posizione {@param index} altrimenti
     */
    @GetMapping(value = "/roots")
    private Object getRoot(@RequestParam(name = "index", defaultValue = "-1") int index) {
        if (index == -1) {
            return currentBuilder.getRoots();
        } else {
            return currentBuilder.getRoots().get(index);
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
     * @param localityIndex indice della località
     * @param name          nome del servizio scelto
     * @param handleType    tpo di Handle
     * @param handleIndex   indice dell'handle se in una lista
     * @param portMode      se specificato questo parametro, che deve essere 0 o 1,
     *                      allora il valore di handleType deve essere inPort,
     *                      handleIndex deve indicare un nodo nella lista {@code nodes},
     *                      --> chiamata del tipo addDescNameOuterInterface(1, "ilMioServizio", "inPort", 0, 0)
     * @return l'handle scelto
     */
    @PostMapping(value = "/addDescNameOuterInterface")
    private InnerName addDescNameOuterInterface(@RequestParam(name = "locality", defaultValue = "-1") int localityIndex,
                                                @RequestParam(name = "name", defaultValue = "") String name,
                                                @RequestParam(name = "handleType") String handleType,
                                                @RequestParam(name = "handleIndex", defaultValue = "-1") int handleIndex,
                                                @RequestParam(name = "portMode", defaultValue = "-1") int portMode) {

        InnerName innerName;

        if (localityIndex == -1) {
            return null;
        } else if (portMode != -1 && handleType.equals(HANDLE_IN_PORT)) {
            innerName = currentBuilder.addDescNameOuterInterface(localityIndex, name, nodes.get(handleIndex).getInPort(portMode).getEditable());
            innerNames.add(innerName);
            return innerName;
        } else {
            if (name.equals("") && handleIndex == -1) {     // Solo la località specificata
                innerName = currentBuilder.addDescNameOuterInterface(localityIndex);
            } else if (name.equals("")) {                   // Località e Handle specificati
                innerName = currentBuilder.addDescNameOuterInterface(localityIndex, getHandle(handleType, handleIndex));
            } else if (handleIndex == -1) {                 // Località e Name specificati
                innerName = currentBuilder.addDescNameOuterInterface(localityIndex, name);
            } else {
                innerName = currentBuilder.addDescNameOuterInterface(localityIndex, name, getHandle(handleType, handleIndex));
            }
            innerNames.add(innerName);
            return innerName;
        }
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
        if (localityIndex == -1) {
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
    private List<OuterName> getOuterNames(@RequestParam(name = "index", defaultValue = "-1") int index) {

        if (index == -1) {
            return outerNames;
        } else {
            List<OuterName> oneItemList = new LinkedList<>();
            oneItemList.add(outerNames.get(index));
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

    @GetMapping(value = "/sites")
    private List<Site> getSites(@RequestParam(name = "index", defaultValue = "-1") int index) {
        if (index == -1) {
            return sites;
        } else {
            List<Site> oneItemList = new LinkedList<>();
            oneItemList.add(sites.get(index));
            return oneItemList;
        }
    }

    @GetMapping(value = "/edges")
    private List<Edge> getEdges(@RequestParam(name = "index", defaultValue = "-1") int index) {
        if (index == -1) {
            return edges;
        } else {
            List<Edge> oneItemList = new LinkedList<>();
            oneItemList.add(edges.get(index));
            return oneItemList;
        }
    }

    @GetMapping(value = "/nodes")
    private List<Node> getNodes(@RequestParam(value = "index", defaultValue = "-1") int index) {
        if (index == -1) {
            return nodes;
        } else {
            List<Node> oneItemList = new LinkedList<>();
            oneItemList.add(nodes.get(index));
            return oneItemList;
        }
    }

    /**
     * @param controlName name of the control to insert
     * @param parentType  one between "root", "editableParent", "node"
     * @param parentIndex index of the parent to get from the related list
     * @param parentName name of the parent to get from the related list
     *                   Method notes:
     *                   - The parameter "controlName" must be always setted
     *                   Method uses - Parameter combinations:
     *                   1) "parentType" + "parentIndex"
     *                   2) "parentType" + "parentName"
     *                   3) "name" only
     * @return the insertion resulting node
     */
    @PostMapping(value = "/nodes")
    private Node addNode(@RequestParam(name = "controlName") String controlName,
                         @RequestParam(name = "parentType", defaultValue = INVALID_TYPE) String parentType,
                         @RequestParam(name = "parentIndex", defaultValue = "-1") int parentIndex,
                         @RequestParam(name = "parentName", defaultValue = "") String parentName) {

        Node resultNode;
        Parent parent;
        System.out.format("Parent type '%s', ", parentType);

        if (parentType.equals(INVALID_TYPE)) {
            parent = getParentByName(parentName);
        } else {
            if (parentIndex != -1) {
                switch (parentType) {
                    case PARENT_NODE:
                        parent = nodes.get(parentIndex);
                        break;
                    case PARENT_ROOT:
                        parent = roots.get(parentIndex);
                        break;
                    case PARENT_EDITABLE_PARENT:
                        parent = editableParents.get(parentIndex);
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
                        parent = getEditableParentByName(parentName);
                        break;
                    default:
                        parent = null;
                        break;
                }
            }
        }

        resultNode = currentBuilder.addNode(controlName, parent);
        nodes.add(resultNode);
        System.out.println("Number of nodes in the list: " + nodes.size());
        return resultNode;

    }

    @PostMapping(value = "/sites")
    private Site addSite(@RequestParam(name = "parentType") String parentType,
                         @RequestParam(name = "parentIndex") int parentIndex) {

        Site resultSite = null;
        Parent parentNode = null;

        switch (parentType) {

            case PARENT_NODE:
                parentNode = nodes.get(parentIndex);
                break;
            case PARENT_ROOT:
                parentNode = currentBuilder.getRoots().get(parentIndex);
                break;
            case PARENT_EDITABLE_PARENT:
                parentNode = editableParents.get(parentIndex);
                break;
        }

        resultSite = currentBuilder.addSite(parentNode);
        sites.add(resultSite);
        return resultSite;
    }

    /**
     * @param nodeIndex   indice del nodo a cui collegare l'handle
     * @param portMode    modalità con cui collegare l'handle al nodo: 0 per READ_MODE, 1 per WRITE_MODE
     * @param handleType  tipo di handle nell'insieme {"outerName", "editableHandle", "inPort", "edge"}
     * @param handleIndex indice dell'handle nella sua relativa lista
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
                Node selectedNode = nodes.get(nodeIndex);
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
    private DirectedBigraph makeBigraph(@RequestParam(name = "closeBigraph", defaultValue = "false") boolean closeBigraph) {
        return currentBuilder.makeBigraph(closeBigraph);
    }

    /**
     * @param handleType  uno tipo tra "outerName", "editableHandle", "inPort", "edge"
     * @param handleIndex indice dell'handle nella lista selezionata
     * @return l'istanza di handle selezionata
     */
    private Handle getHandle(String handleType, int handleIndex) {

        switch (handleType) {
            case HANDLE_OUTER_NAME:
                return outerNames.get(handleIndex);
            case HANDLE_EDITABLE_HANDLE:
                return editableHandles.get(handleIndex);
            case HANDLE_IN_PORT:
                return inPorts.get(handleIndex);
            default:
                return edges.get(handleIndex);
        }

    }

    /**
     *
     * @param rootName name of the root to get. Every root name must be unique for the considered builder
     * @return the root searched if it is present in the list, null else
     */
    private Root getRootByName(String rootName) {
        Root requestedRoot = null;
        for (Root root : roots) {
            if (root.toString().equals(rootName)) {
                requestedRoot = root;
            }
        }

        return requestedRoot;
    }

    /**
     *
     * @param nodeName name of the node to get. Every node name must be unique for the considered builder
     * @return the node searched if it is present in the list, null else
     */
    private Node getNodeByName(String nodeName) {
        Node requestedNode = null;
        for (Node node : nodes) {
            if (node.toString().equals(nodeName)) {
                requestedNode = node;
            }
        }

        return requestedNode;
    }

    /**
     *
     * @param editableParentName name of the editableParent to get. Every node name must be unique for the considered builder
     * @return the editableParent searched if it is present in the list, null else
     */
    private EditableParent getEditableParentByName(String editableParentName) {
        EditableParent requestedEditableParent = null;
        for (EditableParent editableParent : editableParents) {
            if (editableParent.toString().equals(editableParentName)) {
                requestedEditableParent = editableParent;
            }
        }

        return requestedEditableParent;
    }

    /**
     *
     * @param parentName name of the parent to search
     * @return the searched parent if it has been founded, null else
     */
    private Parent getParentByName(String parentName) {

        for (Root root : roots) {
            if (root.toString().equals(parentName)) {
                return root;
            }
        }
        for (Node node : nodes) {
            if (node.toString().equals(parentName)) {
                return node;
            }
        }
        for (EditableParent editableParent : editableParents) {
            if (editableParent.toString().equals(parentName)) {
                return editableParent;
            }
        }

        return null;
    }
}