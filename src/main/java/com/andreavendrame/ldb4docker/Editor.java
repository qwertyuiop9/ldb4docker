package com.andreavendrame.ldb4docker;

import com.andreavendrame.ldb4docker.myjlibbig.Interface;
import com.andreavendrame.ldb4docker.myjlibbig.ldb.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.andreavendrame.ldb4docker.BigraphImportController.*;

@RestController
@RequestMapping(value = "/editor")
public class Editor {

    @Autowired
    private RestTemplate restTemplate;

    private DirectedBigraphBuilder currentBuilder;
    private DirectedSignature currentSignature;
    private List<DirectedBigraphBuilder> builderList = new LinkedList<>();

    private static final String REST_CALL_PARAMETER_EXAMPLE = "/authors?name=Andrea&name=Anna";
    private static final String VOLUME = "volume";
    public static final String NETWORK = "network";
    public static final String PARENT_ROOT = "root";
    public static final String PARENT_EDITABLE_PARENT = "editableParent";
    public static final String PARENT_NODE = "node";
    public static final String HANDLE_OUTER_NAME = "outerName";
    public static final String HANDLE_EDITABLE_HANDLE = "editableHandle";
    public static final String HANDLE_IN_PORT = "inPort";
    public static final String HANDLE_EDGE = "edge";

    private final List<Handle> handles = new LinkedList<>();
    // private final List<Root> roots = new LinkedList<>();
    private final List<OuterName> outerNames = new LinkedList<>();
    private final List<InnerName> innerNames = new LinkedList<>();
    private final List<Site> sites = new LinkedList<>();
    private final List<Edge> edges = new LinkedList<>();
    private final List<Node> nodes = new LinkedList<>();
    private final List<EditableParent> editableParents = new LinkedList<>();
    private final List<DirectedControl> bigraphControls = new LinkedList<>();
    private final List<String> services = new LinkedList<>();
    private final List<EditableHandle> editableHandles = new LinkedList<>();
    private final List<InPort> inPorts = new LinkedList<>();


    @GetMapping(value = "/start-editing")
    public DirectedBigraphBuilder createBuilder() {

        System.out.format("Creo il builder...");
        this.currentBuilder = new DirectedBigraphBuilder(this.currentSignature);
        System.out.println("OK!\n");

        return this.currentBuilder;

    }

    /**
     * @param position posizione della radice del bigrafo nella lista delle radici disponibili
     * @return l'intera lista di radici del builder se {@param position} è -1; la root in posizione {@param position} altrimenti
     */
    @GetMapping(value = "/roots")
    private Object getRoot(@RequestParam(name = "position", defaultValue = "-1") int position) {
        if (position == -1) {
            return this.currentBuilder.getRoots();
        } else {
            return this.currentBuilder.getRoots().get(position);
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
            root = this.currentBuilder.addRoot();
        } else {
            // Aggiungo una radice con località
            root = this.currentBuilder.addRoot(locality);
        }
        return root;
    }

    /**
     * @param index indice della radice da ispezionare
     * @return una stringa che descrive il nome della radice e quello dei figli ad essa direttamente collegati (se presenti) se
     * il parametro {@param index} è valido, mentre la lista di tutte le radici con i figli direttamente connessi altrimenti
     */
    @GetMapping(value = "/showRoots")
    private String showRoots(@RequestParam(value = "index", defaultValue = "-1") int index) {
        StringBuilder stringBuilder = new StringBuilder();

        if (index != -1) {
            EditableRoot editableRoot = this.currentBuilder.getRoots().get(index).getEditable();
            stringBuilder.append("Radice ").append(editableRoot.toString()).append(" --> ");
            int childIndex = 0;
            editableRoot.getEditableChildren().forEach(editableChild -> {
                stringBuilder.append("Child ").append(childIndex).append(" : ");
                stringBuilder.append(editableChild.toString());
                stringBuilder.append(", ");
            });
        } else {
            for (Root root : this.currentBuilder.getRoots()) {
                EditableRoot editableRoot = root.getEditable();
                stringBuilder.append(editableRoot.toString());
                stringBuilder.append(" - Children: [");
                AtomicInteger childIndex = new AtomicInteger();
                editableRoot.getEditableChildren().forEach(editableChild -> {
                    stringBuilder.append("Child ").append(childIndex.get()).append(" : ");
                    stringBuilder.append(editableChild.toString());
                    stringBuilder.append(", ");
                    childIndex.getAndIncrement();
                });
                stringBuilder.append("]");
                stringBuilder.append("\n");
            }
        }

        return stringBuilder.toString();
    }

    @GetMapping(value = "/signatures")
    private DirectedSignature getSignature() {
        return this.currentBuilder.getSignature();
    }

    @GetMapping(value = "/innerInterfaces")
    private Interface getInnerInterface() {
        return this.currentBuilder.getInnerInterface();
    }

    @GetMapping(value = "/outerInterfaces")
    private Interface getOuterInterface() {
        return this.currentBuilder.getOuterInterface();
    }

    @GetMapping(value = "/outerNames")
    private List<OuterName> getOuterNames(@RequestParam(name = "index", defaultValue = "-1") int index) {

        if (index == -1) {
            return this.outerNames;
        } else {
          List<OuterName> oneItemList = new LinkedList<>();
          oneItemList.add(this.outerNames.get(index));
          return oneItemList;
        }
    }


    @GetMapping(value = "/sites")
    private Interface getSites() {
        return (Interface) this.currentBuilder.getSites();
    }

    @GetMapping(value = "/edges")
    private Collection<? extends Edge> getEdges() {
        return this.currentBuilder.getEdges();
    }

    @GetMapping(value = "/handles")
    private List<Handle> getHandles() {
        return this.handles;
    }

    @PostMapping(value = "/addDescNameInnerInterface")
    private OuterName addDescNameInnerInterface(@RequestParam(name = "locality", defaultValue = "-1") int localityIndex,
                                                @RequestParam(name = "name", defaultValue = "") String name) {
        if (localityIndex == -1) {
            return null;
        } else {
            OuterName outerName;
            if (name.equals("")) {
                outerName = this.currentBuilder.addDescNameInnerInterface(localityIndex);
            } else {
                outerName = this.currentBuilder.addDescNameInnerInterface(localityIndex, name);
            }
            this.outerNames.add(outerName);
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
     *                 handleIndex deve indicare un nodo nella lista {@code this.nodes},
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
            innerName = this.currentBuilder.addDescNameOuterInterface(localityIndex, name, this.nodes.get(handleIndex).getInPort(portMode).getEditable());
            this.innerNames.add(innerName);
            return innerName;
        } else {
            if (name.equals("") && handleIndex == -1) {     // Solo la località specificata
                innerName = this.currentBuilder.addDescNameOuterInterface(localityIndex);
            } else if (name.equals("")) {                   // Località e Handle specificati
                innerName = this.currentBuilder.addDescNameOuterInterface(localityIndex, getHandle(handleType, handleIndex));
            } else if (handleIndex == -1) {                 // Località e Name specificati
                innerName = this.currentBuilder.addDescNameOuterInterface(localityIndex, name);
            } else {
                innerName = this.currentBuilder.addDescNameOuterInterface(localityIndex, name, getHandle(handleType, handleIndex));
            }
            this.innerNames.add(innerName);
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
                return this.outerNames.get(handleIndex);
            case HANDLE_EDITABLE_HANDLE:
                return this.editableHandles.get(handleIndex);
            case HANDLE_IN_PORT:
                return this.inPorts.get(handleIndex);
            default:
                return this.edges.get(handleIndex);
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
                outerName = this.currentBuilder.addAscNameOuterInterface(localityIndex);
            } else {
                outerName = this.currentBuilder.addAscNameOuterInterface(localityIndex, name);
            }
            this.outerNames.add(outerName);
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
                return this.currentBuilder.addAscNameInnerInterface(localityIndex);
            } else if (name.equals("")) {                   // Località e Handle specificati
                return this.currentBuilder.addAscNameInnerInterface(localityIndex, handles.get(handleIndex));
            } else if (handleIndex == -1) {                 // Località e Name specificati
                return this.currentBuilder.addAscNameInnerInterface(localityIndex, name);
            } else {
                return this.currentBuilder.addAscNameInnerInterface(localityIndex, name, handles.get(handleIndex));
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
                parentNode = this.nodes.get(parentIndex);
                break;
            case PARENT_ROOT:
                parentNode = this.currentBuilder.getRoots().get(parentIndex);
                break;
            case PARENT_EDITABLE_PARENT:
                parentNode = this.editableParents.get(parentIndex);
                break;
        }

        resultSite = this.currentBuilder.addSite(parentNode);
        this.sites.add(resultSite);
        return resultSite;
    }

    /**
     * @param controlName
     * @param parentType  uno tra "root", "editableParent", "node"
     * @param parentIndex
     * @return
     */
    @PostMapping(value = "/nodes")
    private Node addNode(@RequestParam(name = "controlName") String controlName,
                         @RequestParam(name = "parentType") String parentType,
                         @RequestParam(name = "parentIndex") int parentIndex) {

        Node resultNode = null;
        Parent parentNode = null;

        switch (parentType) {

            case PARENT_NODE:
                parentNode = this.nodes.get(parentIndex);
                break;
            case PARENT_ROOT:
                parentNode = this.currentBuilder.getRoots().get(parentIndex);
                break;
            case PARENT_EDITABLE_PARENT:
                parentNode = this.editableParents.get(parentIndex);
                break;
        }

        resultNode = this.currentBuilder.addNode(controlName, parentNode);
        this.nodes.add(resultNode);
        System.out.println("Numero di nodi nella lista 'nodes': " + this.nodes.size());
        return resultNode;

    }

    @GetMapping(value = "/nodes")
    private Object getNodes(@RequestParam(value = "nodeIndex", defaultValue = "-1") int nodeIndex) {
        if (nodeIndex == -1) {
            return this.currentBuilder.getNodes().toArray();
        } else {
            Node[] nodes = (Node[]) this.currentBuilder.getNodes().toArray();
            if (nodeIndex < nodes.length) {
                return nodes[nodeIndex];
            } else {
                return "Node index not valid";
            }

        }
    }

    @GetMapping(value = "showNodes")
    private String showNodes(@RequestParam(value = "nodeIndex", defaultValue = "-1") int nodeIndex) {

        StringBuilder stringBuilder = new StringBuilder();

        if (nodeIndex != -1 ) {
            addNodeDescription(stringBuilder, nodeIndex);
        } else {
            for (int i=0; i<this.nodes.size(); i++) {
                addNodeDescription(stringBuilder, i);
            }
        }

        return stringBuilder.toString() ;
    }

    @GetMapping(value = "showOuterNames")
    private String showOuterNames(@RequestParam(value = "index", defaultValue = "-1") int index) {

        StringBuilder stringBuilder = new StringBuilder();

        if (index != -1 ) {
            addNodeDescription(stringBuilder, index);
        } else {
            for (int i=0; i<this.outerNames.size(); i++) {
                addOuterNameDescription(stringBuilder, i);
            }
        }

        return stringBuilder.toString() ;
    }

    private void addOuterNameDescription(StringBuilder stringBuilder, int i) {
        OuterName outerName = this.outerNames.get(i);
        stringBuilder.append("OuterName ").append(i).append(") ").append(outerName.toString()).append(", ");
    }

    /**
     *
     * @param stringBuilder in cui salvare le informazioni sul nodo considerato
     * @param i indice del nodo nella lista interna all'editor
     */
    private void addNodeDescription(StringBuilder stringBuilder, int i) {
        Node currentNode = this.nodes.get(i);
        stringBuilder.append("Nodo ").append(i).append(" ");
        stringBuilder.append(" nome: ").append(currentNode.getName());
        stringBuilder.append("- Porte in uscita [ ");
        for (OutPort outPort : currentNode.getOutPorts()) {
            stringBuilder.append(outPort.toString()).append(", ");
        }
        stringBuilder.append("], ");
        stringBuilder.append("- Porte in entrata [ ");
        for (InPort inPort : currentNode.getInPorts()) {
            stringBuilder.append(inPort.toString()).append(", ");
        }
        stringBuilder.append("], ");
    }

    @PostMapping(value = "setPortMode")
    private void test(@RequestParam(name = "mode", defaultValue = "read") String portAttachMode,
                      @RequestParam(name = "handleIndex", defaultValue = "-1") int handleIndex,
                      @RequestParam(name = "nodeIndex", defaultValue = "-1") int nodeIndex) {

        if (handleIndex == -1) {
            System.out.println("Indice del parametro 'Handle' non valido");
        } else if (nodeIndex == -1) {
            System.out.println("Indice del parametro 'Node' non valido");
        }

        Node node = this.nodes.get(nodeIndex);
        Handle handle = this.handles.get(handleIndex);

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
        return this.bigraphControls;
    }

    @GetMapping(value = "directedSignatures")
    private String createDirectedSignature() {
        System.out.format("Creazione della directedSignature...");
        this.currentSignature = new DirectedSignature(bigraphControls);
        System.out.format("OK!\n");
        return this.currentSignature.toString();
    }

    @GetMapping(value = "services")
    private List<String> gerServices() {return this.services; }

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
        if (nodeIndex > -1 && nodeIndex < this.nodes.size()) {
            selectedNode = this.nodes.get(nodeIndex);
        } else {
            return "Indice del nodo non valido";
        }

        OuterName selectedOuterName = null;
        if (outerNameIndex > -1 && outerNameIndex < this.outerNames.size()) {
            selectedOuterName = this.outerNames.get(outerNameIndex);
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
            return this.innerNames;
        } else {
            List<InnerName> oneItemList = new LinkedList<>();
            oneItemList.add(this.innerNames.get(index));
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
                // * @param handleType uno tipo tra "outerName", "editableHandle", "inPort", "edge"
                case HANDLE_OUTER_NAME:
                    selectedHandle = this.outerNames.get(handleIndex).getEditable();
                    break;
                case HANDLE_IN_PORT:
                    selectedHandle = this.inPorts.get(handleIndex).getEditable();
                    break;
                case HANDLE_EDITABLE_HANDLE:
                    selectedHandle = this.editableHandles.get(handleIndex);
                    break;
                case HANDLE_EDGE:
                    selectedHandle = this.edges.get(handleIndex).getEditable();
                    break;
                default:
                    return;
            }

            if (nodeIndex == -1) {
                System.out.println("Indice del nodo non valido");
            } else {
                Node selectedNode = this.nodes.get(nodeIndex);
                selectedNode.getOutPort(portMode).getEditable().setHandle(selectedHandle.getEditable());
                System.out.format("Collegato '%s' al nodo %s!\n", selectedHandle.toString(), selectedNode.toString());
            }
        }
    }
}