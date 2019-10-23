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

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping(value = "/start-editing")
    public DirectedBigraphBuilder createBuilder() {

        System.out.format("Creo il builder...");
        currentBuilder = new DirectedBigraphBuilder(currentSignature);
        System.out.println("OK!\n");

        return currentBuilder;

    }

    /**
     * @param position posizione della radice del bigrafo nella lista delle radici disponibili
     * @return l'intera lista di radici del builder se {@param position} è -1; la root in posizione {@param position} altrimenti
     */
    @GetMapping(value = "/roots")
    private Object getRoot(@RequestParam(name = "position", defaultValue = "-1") int position) {
        if (position == -1) {
            return currentBuilder.getRoots();
        } else {
            return currentBuilder.getRoots().get(position);
        }
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
        } else {
            // Aggiungo una radice con località
            root = currentBuilder.addRoot(locality);
        }
        return root;
    }

    @GetMapping(value = "/signatures")
    private DirectedSignature getSignature() {
        return currentBuilder.getSignature();
    }

    @GetMapping(value = "/innerInterfaces")
    private Interface getInnerInterface() {
        return currentBuilder.getInnerInterface();
    }

    @GetMapping(value = "/outerInterfaces")
    private Interface getOuterInterface() {
        return currentBuilder.getOuterInterface();
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


    @GetMapping(value = "/sites")
    private Interface getSites() {
        return (Interface) currentBuilder.getSites();
    }

    @GetMapping(value = "/edges")
    private Collection<? extends Edge> getEdges() {
        return currentBuilder.getEdges();
    }

    @GetMapping(value = "/handles")
    private List<Handle> getHandles() {
        return handles;
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
     *
     * @param localityIndex indice della località
     * @param name nome del servizio scelto
     * @param handleType tpo di Handle
     * @param handleIndex indice dell'handle se in una lista
     * @param portMode se specificato questo parametro, che deve essere 0 o 1,
     *                 allora il valore di handleType deve essere inPort,
     *                 handleIndex deve indicare un nodo nella lista {@code nodes},
     *                 --> chiamata del tipo addDescNameOuterInterface(1, "ilMioServizio", "inPort", 0, 0)
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

    /**
     *
     * @param handleType uno tipo tra "outerName", "editableHandle", "inPort", "edge"
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

    @PostMapping(value = "/addAscNameOuterInterface")
    private OuterName addAscNameOuterInterface(@RequestParam(name = "locality", defaultValue = "-1") int localityIndex,
                                               @RequestParam(name = "name", defaultValue = "") String name,
                                               @RequestParam(name = "nameType", defaultValue = "other") String nameType) {
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
                                               @RequestParam(name = "handle", defaultValue = "-1") int handleIndex) {
        if (localityIndex == -1) {
            return null;
        } else {
            if (name.equals("") && handleIndex == -1) {     // Solo la località specificata
                return currentBuilder.addAscNameInnerInterface(localityIndex);
            } else if (name.equals("")) {                   // Località e Handle specificati
                return currentBuilder.addAscNameInnerInterface(localityIndex, handles.get(handleIndex));
            } else if (handleIndex == -1) {                 // Località e Name specificati
                return currentBuilder.addAscNameInnerInterface(localityIndex, name);
            } else {
                return currentBuilder.addAscNameInnerInterface(localityIndex, name, handles.get(handleIndex));
            }
        }
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
     * @param controlName nome del controllo da inserire
     * @param parentType  uno tra "root", "editableParent", "node"
     * @param parentIndex indice del parent nella relativa lista
     * @return il nodo che risulta dall'inserimento
     */
    @PostMapping(value = "/nodes")
    private Node addNode(@RequestParam(name = "controlName") String controlName,
                         @RequestParam(name = "parentType") String parentType,
                         @RequestParam(name = "parentIndex") int parentIndex) {

        Node resultNode = null;
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

        resultNode = currentBuilder.addNode(controlName, parentNode);
        nodes.add(resultNode);
        System.out.println("Numero di nodi nella lista 'nodes': " + nodes.size());
        return resultNode;

    }

    @GetMapping(value = "/nodes")
    private Object getNodes(@RequestParam(value = "nodeIndex", defaultValue = "-1") int nodeIndex) {
        if (nodeIndex == -1) {
            return currentBuilder.getNodes().toArray();
        } else {
            Node[] nodes = (Node[]) currentBuilder.getNodes().toArray();
            if (nodeIndex < nodes.length) {
                return nodes[nodeIndex];
            } else {
                return "Node index not valid";
            }

        }
    }


    @PostMapping(value = "setPortMode")
    private void setPortMode(@RequestParam(name = "mode", defaultValue = "read") String portAttachMode,
                      @RequestParam(name = "handleIndex", defaultValue = "-1") int handleIndex,
                      @RequestParam(name = "nodeIndex", defaultValue = "-1") int nodeIndex) {

        if (handleIndex == -1) {
            System.out.println("Indice del parametro 'Handle' non valido");
        } else if (nodeIndex == -1) {
            System.out.println("Indice del parametro 'Node' non valido");
        }

        Node node = nodes.get(nodeIndex);
        Handle handle = handles.get(handleIndex);

        if (portAttachMode.equals("read")) {
            node.getOutPort(READ_MODE).getEditable().setHandle(handle.getEditable());
        } else {
            node.getOutPort(WRITE_MODE).getEditable().setHandle(handle.getEditable());
        }
    }

    @PostMapping(value = "directedControls")
    private String addBigraphControl(@RequestParam(name = "controlName") String controlName,
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

    @GetMapping(value = "directedControls")
    private List<DirectedControl> getBigraphControls() {
        return bigraphControls;
    }

    @GetMapping(value = "directedSignatures")
    private String createDirectedSignature() {
        System.out.format("Creazione della directedSignature...");
        currentSignature = new DirectedSignature(bigraphControls);
        System.out.format("OK!\n");
        return currentSignature.toString();
    }

    @GetMapping(value = "services")
    private List<String> gerServices() {return services; }

    /**
     *
     * @param nodeIndex indice del nodo a cui collegare l'outerName
     * @param outerNameIndex indice dell'outerName da collegare al nodo
     * @param linkMode modalità del collegamento che può essere 0 (per READ MODE) o 1 (per WRITE MODE)
     * @return un messaggio di riassunto del collegamento se quest'ultimo è andato a buon fine
     */
    @PostMapping(value = "linkAndSetMode")
    private String linkAndSetMode(@RequestParam(name = "nodeIndex", defaultValue = "-1") int nodeIndex,
                                  @RequestParam(name = "outerNameIndex", defaultValue = "-1") int outerNameIndex,
                                  @RequestParam(name = "linkMode") int linkMode) {
        Node selectedNode = null;
        if (nodeIndex > -1 && nodeIndex < nodes.size()) {
            selectedNode = nodes.get(nodeIndex);
        } else {
            return "Indice del nodo non valido";
        }

        OuterName selectedOuterName = null;
        if (outerNameIndex > -1 && outerNameIndex < outerNames.size()) {
            selectedOuterName = outerNames.get(outerNameIndex);
        } else {
            return "indice dell'outerName non valido";
        }

        if (linkMode < 0 || linkMode > 1) {
            return "Modalità di collegamento del nome non valida. Modalità disponibili: 0 per lettura, 1 per scrittura";
        }

        selectedNode.getOutPort(linkMode).getEditable().setHandle(selectedOuterName.getEditable()); // link the net to the node in read mode
        return String.format("OuterName '%s' collegato al nodo '%s' in modalità %s",  selectedOuterName.toString(), selectedNode.toString(),
                linkMode == 0 ? "lettura" : "scrittura");
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

    /**
     *
     * @param nodeIndex indice del nodo a cui collegare l'handle
     * @param portMode modalità con cui collegare l'handle al nodo: 0 per READ_MODE, 1 per WRITE_MODE
     * @param handleType tipo di handle nell'insieme {"outerName", "editableHandle", "inPort", "edge"}
     * @param handleIndex indice dell'handle nella sua relativa lista
     */
    @GetMapping(value = "/linkNameToNode")
    private void linkNameToNode(@RequestParam(name = "nodeIndex", defaultValue = "-1") int nodeIndex,
                        @RequestParam(name = "portMode", defaultValue = "-1") int portMode,
                        @RequestParam(name = "handleType") String handleType,
                        @RequestParam(name = "handleIndex") int handleIndex) {

        if (portMode < 0 || portMode > 1) {
            System.out.println("Modalità di collegamento non valida. Scegliere 0 per READ o 1 per WRITE.");
        } else {
            Handle selectedHandle = null;
            switch (handleType) {
                case HANDLE_OUTER_NAME:
                    selectedHandle = outerNames.get(handleIndex).getEditable();
                    break;
                case HANDLE_IN_PORT:
                    selectedHandle = inPorts.get(handleIndex).getEditable();
                    break;
                case HANDLE_EDITABLE_HANDLE:
                    selectedHandle = editableHandles.get(handleIndex);
                    break;
                case HANDLE_EDGE:
                    selectedHandle = edges.get(handleIndex).getEditable();
                    break;
                default:
                    return;
            }

            if (nodeIndex == -1) {
                System.out.println("Indice del nodo non valido");
            } else {
                Node selectedNode = nodes.get(nodeIndex);
                selectedNode.getOutPort(portMode).getEditable().setHandle(selectedHandle.getEditable());
                System.out.format("Collegato '%s' al nodo %s!\n", selectedHandle.toString(), selectedNode.toString());
            }
        }
    }

    @GetMapping(value = "makeBigraph")
    private DirectedBigraph makeBigraph(@RequestParam(name = "closeBigraph", defaultValue = "false") boolean closeBigraph) {
        return currentBuilder.makeBigraph(closeBigraph);
    }
}