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

    private final List<Handle> handles = new LinkedList<>();
    // private final List<Root> roots = new LinkedList<>();
    private final List<OuterName> outerNames = new LinkedList<>();
    private final List<OuterName> innerNames = new LinkedList<>();
    private final List<Site> sites = new LinkedList<>();
    private final List<Edge> edges = new LinkedList<>();
    private final List<Node> nodes = new LinkedList<>();
    private final List<EditableParent> editableParents = new LinkedList<>();
    private final List<DirectedControl> bigraphControls = new LinkedList<>();

    // Variabili di controllo
    private boolean useDefaultNetwork = true;
    Map<String, OuterName> networkNames = new HashMap<>();
    Map<String, OuterName> volumeNames = new HashMap<>();
    Map<String, OuterName> otherNames = new HashMap<>();

    @GetMapping(value = "/start-editing")
    public DirectedBigraphBuilder createBuilder() {

        System.out.format("Creo il builder...");
        this.currentBuilder = new DirectedBigraphBuilder(this.currentSignature);
        System.out.println("OK!\n");

        return this.currentBuilder;

    }

    @GetMapping(value = "useDefaultNetwork")
    public String getUseDefaultNetworkProperty() {
        return "Default network used: " + this.useDefaultNetwork;
    }

    @PostMapping(value = "useDefaultNetwork")
    public String getUseDefaultNetworkProperty(@RequestParam(value = "enable", defaultValue = "false") boolean useDefaultNetwork) {
        this.useDefaultNetwork = useDefaultNetwork;
        return "Default network used: " + this.useDefaultNetwork;
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
                stringBuilder.append("Figlio ").append(childIndex).append(" : ");
                stringBuilder.append(editableChild.toString());
                stringBuilder.append(", ");
            });
        } else {
            for (Root root : this.currentBuilder.getRoots()) {
                EditableRoot editableRoot = root.getEditable();
                stringBuilder.append(editableRoot.toString());
                stringBuilder.append(" - Figli: [");
                AtomicInteger childIndex = new AtomicInteger();
                editableRoot.getEditableChildren().forEach(editableChild -> {
                    stringBuilder.append("Figlio ").append(childIndex.get()).append(" : ");
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
    private OuterName[] getOuterNames(@RequestParam(name = "nameType", defaultValue = "other") String nameType) {

        List<OuterName> list = new LinkedList<>();
        switch (nameType) {
            case NETWORK:
                return (OuterName[]) this.networkNames.values().toArray();
            case VOLUME:
                return (OuterName[]) this.volumeNames.values().toArray();
            default:
                return (OuterName[]) this.otherNames.values().toArray();
        }
    }

    @GetMapping(value = "/networkNames")
    private Map<String, OuterName> getNetworkNames() {
        return this.networkNames;
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
            if (name.equals("")) {
                return this.currentBuilder.addDescNameInnerInterface(localityIndex);
            } else {
                return this.currentBuilder.addDescNameInnerInterface(localityIndex, name);
            }
        }
    }

    @PostMapping(value = "/addDescNameOuterInterface")
    private InnerName addDescNameOuterInterface(@RequestParam(name = "locality", defaultValue = "-1") int localityIndex,
                                                @RequestParam(name = "name", defaultValue = "") String name,
                                                @RequestParam(name = "handle", defaultValue = "-1") int handleIndex) {
        if (localityIndex == -1) {
            return null;
        } else {
            if (name.equals("") && handleIndex == -1) {     // Solo la località specificata
                return this.currentBuilder.addDescNameOuterInterface(localityIndex);
            } else if (name.equals("")) {                   // Località e Handle specificati
                return this.currentBuilder.addDescNameOuterInterface(localityIndex, handles.get(handleIndex));
            } else if (handleIndex == -1) {                 // Località e Name specificati
                return this.currentBuilder.addDescNameOuterInterface(localityIndex, name);
            } else {
                return this.currentBuilder.addDescNameOuterInterface(localityIndex, name, handles.get(handleIndex));
            }
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
            switch (nameType) {
                case NETWORK:
                    networkNames.put(name, outerName);
                    System.out.format("Nome %s aggiunto alla mappa delle reti\n", name);
                    break;
                case VOLUME:
                    volumeNames.put(name, outerName);
                    System.out.format("Nome %s aggiunto alla mappa dei volumi\n", name);
                    break;
                default:
                    otherNames.put(name, outerName);
                    System.out.format("Nome %s aggiunto alla mappa dei nomi generici\n", name);
                    break;
            }

            this.outerNames.add(outerName);
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

        if (arityIn < 0 || arityOut < 0 || arityIn > 2 || arityOut > 2) {
            return "L'arietà di almeno una porta non è valida";
        } else {
            bigraphControls.add(new DirectedControl(controlName, active, arityOut, arityIn));
            return String.format("Aggiunto il controllo '%s' (attivo: %b) con arityIn %d e arityOut %d", controlName, active, arityIn, arityOut);
        }
    }

    @GetMapping(value = "directedControls")
    private List<DirectedControl> getBigraphControls() {
        return this.bigraphControls;
    }

    @GetMapping(value = "directedSignatures")
    private String createDirectedSignature() {
        this.currentSignature = new DirectedSignature(bigraphControls);
        return this.currentSignature.toString();
    }
}