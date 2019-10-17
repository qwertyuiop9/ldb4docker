package com.andreavendrame.ldb4docker;

import com.andreavendrame.ldb4docker.myjlibbig.Interface;
import com.andreavendrame.ldb4docker.myjlibbig.ldb.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static com.andreavendrame.ldb4docker.BigraphImportController.READ_MODE;
import static com.andreavendrame.ldb4docker.BigraphImportController.prepareBigraphControls;

@RestController
@RequestMapping(value = "/editor")
public class Editor {

    @Autowired
    private RestTemplate restTemplate;

    private DirectedBigraphBuilder builder;
    private static final String REST_CALL_PARAMETER_EXAMPLE = "/authors?name=Andrea&name=Anna";

    private final List<Handle> handles = new LinkedList<>();
    private final List<Node> nodes = new LinkedList<>();
    private final List<Root> roots = new LinkedList<>();
    private final List<OuterName> outerNames = new LinkedList<>();
    private final List<OuterName> innerNames = new LinkedList<>();
    private final List<Site> sites = new LinkedList<>();

    // Variabili di controllo
    private boolean isEditing = false;


    @RequestMapping(value = "/start-editing")
    public DirectedBigraphBuilder startEditing() {

        // Preparo i controlli, la signature e il bigrafo vuoto
        List<DirectedControl> bigraphControls = prepareBigraphControls();
        DirectedSignature signature = new DirectedSignature(bigraphControls);
        this.builder = new DirectedBigraphBuilder(signature);
        this.isEditing = true;
        return this.builder;

    }


    @PostMapping(value = "/roots")
    private Object addRoot(@RequestParam(value = "rootIndex", defaultValue = "-1") int rootIndex) {
        if (isEditing) {
            System.out.println("Indice della radice: " + rootIndex);
            Root root;
            if (rootIndex == -1) {
                // Aggiungo una radice senza località
                root = this.builder.addRoot();
            } else {
                // Aggiungo una radice con località
                root = this.builder.addRoot(rootIndex);
            }
            this.roots.add(root);
            return root;
        } else {
            System.out.println("Errore - Builder non inizializzato");
            return "Errore - Builder non inizializzato";
        }
    }

    @GetMapping(value = "/roots")
    private Object getRoot(@RequestParam(name = "position", defaultValue = "-1") int position) {
        if (position == -1) {
            return this.builder.getRoots();
        } else {
            return this.builder.getRoots().get(position);
        }
    }

    @GetMapping(value = "/nodes")
    private Collection<? extends Node> getNodes() {return this.builder.getNodes();}

    @GetMapping(value = "/signatures")
    private DirectedSignature getSignature() { return this.builder.getSignature();}

    @GetMapping(value = "/innerInterfaces")
    private Interface getInnerInterface() {return this.builder.getInnerInterface();}

    @GetMapping(value = "/outerInterfaces")
    private Interface getOuterInterface() {return this.builder.getOuterInterface();}

    @GetMapping(value = "/sites")
    private Interface getSites() {return (Interface) this.builder.getSites();}

    @GetMapping(value = "/edges")
    private Collection<? extends Edge> getEdges() {return this.builder.getEdges();}

    @GetMapping(value = "/handles")
    private List<Handle> getHandles() { return this.handles;}

    @PostMapping(value = "/addDescNameInnerInterface")
    private OuterName addDescNameInnerInterface(@RequestParam(name = "locality", defaultValue = "-1") int localityIndex,
                                               @RequestParam(name = "name", defaultValue = "") String name) {
        if (localityIndex == -1) {
            return null;
        } else {
            if (name.equals("")) {
                return this.builder.addDescNameInnerInterface(localityIndex);
            } else {
                return this.builder.addDescNameInnerInterface(localityIndex, name);
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
                return this.builder.addDescNameOuterInterface(localityIndex);
            } else if (name.equals("")) {                   // Località e Handle specificati
                return this.builder.addDescNameOuterInterface(localityIndex, handles.get(handleIndex));
            } else if (handleIndex == -1) {                 // Località e Name specificati
                return this.builder.addDescNameOuterInterface(localityIndex, name);
            } else {
                return this.builder.addDescNameOuterInterface(localityIndex, name, handles.get(handleIndex));
            }
        }
    }

    @PostMapping(value = "/addAscNameOuterInterface")
    private OuterName addAscNameOuterInterface(@RequestParam(name = "locality", defaultValue = "-1") int localityIndex,
                                               @RequestParam(name = "name", defaultValue = "") String name) {
        if (localityIndex == -1) {
            return null;
        } else {
            if (name.equals("")) {
                return this.builder.addAscNameOuterInterface(localityIndex);
            } else {
                return this.builder.addAscNameOuterInterface(localityIndex, name);
            }
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
                return this.builder.addAscNameInnerInterface(localityIndex);
            } else if (name.equals("")) {                   // Località e Handle specificati
                return this.builder.addAscNameInnerInterface(localityIndex, handles.get(handleIndex));
            } else if (handleIndex == -1) {                 // Località e Name specificati
                return this.builder.addAscNameInnerInterface(localityIndex, name);
            } else {
                return this.builder.addAscNameInnerInterface(localityIndex, name, handles.get(handleIndex));
            }
        }
    }

    @PostMapping(value = "/sites")
    private Site addSite(@RequestParam(name = "parentPosition") int position) {
        return this.builder.addSite(this.nodes.get(position));
    }

    @PostMapping(value = "/nodes")
    private Node addNode(@RequestParam(name = "controlName") String controlName,
                         @RequestParam(name = "nodePosition") int nodePosition) {

        // Il metodo va completato in quanto ci sono molte varianti di parametri
        return this.builder.addNode(controlName, (Node) getNodes().toArray()[nodePosition]);

    }


}