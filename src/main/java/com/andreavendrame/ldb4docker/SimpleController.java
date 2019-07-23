package com.andreavendrame.ldb4docker;

import it.uniud.mads.jlibbig.core.ldb.*;
import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.alg.TransitiveClosure;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.apache.commons.cli.*;
import org.yaml.snakeyaml.Yaml;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;


@Controller
public class SimpleController {

    // Variabili d'ambiente
    private Options options = new Options();
    private HomePageState state = new HomePageState();

    @Autowired
    UserConfigurationService configurationService;

    private static void checkLinks(DirectedBigraph bigraph) throws Exception {
        Map<String, List<String>> links = new HashMap<>();
        Map<String, List<String>> nets = new HashMap<>();
        Map<String, String> names = new HashMap<>();

        for (Node n : bigraph.getNodes()) {
            // search for links and names of services
            if (n.getControl().getName().equals("container")) {
                for (Point p : n.getInPort(0).getPoints()) {
                    if (p.toString().startsWith("l_")) { // found a links
                        if (!links.containsKey(n.getName())) {
                            List<String> temp = new ArrayList<>();
                            temp.add(p.toString());
                            links.put(n.getName(), temp);
                        } else {
                            links.get(n.getName()).add(p.toString());
                        }
                    } else { // it must be the name
                        names.put(p.toString(), n.getName());
                    }
                }
                // find networks for every node
                for (Child child : n.getChildren()) {
                    if (child.isNode()) {
                        Node n1 = (Node) child;
                        if (n1.getControl().getName().equals("network")) {
                            String ro_net = n1.getOutPort(0).getHandle().toString();
                            if (!nets.containsKey(n.getName())) {
                                List<String> temp = new ArrayList<>();
                                temp.add(ro_net);
                                nets.put(n.getName(), temp);
                            } else {
                                nets.get(n.getName()).add(ro_net);
                            }
                        }
                    }
                }
            }
        }
        for (String node : links.keySet()) {
            for (String link : links.get(node)) {
                String[] ss2 = link.split("_");
                // links is in the form l_dest_source, so splitting we can retrieve containers involved
                String src = ss2[2];
                List<String> src_nets = nets.get(names.get(src));
                List<String> dst_nets = nets.get(node);
                boolean net_in_common = false;
                // intersect networks
                for (String n : dst_nets) {
                    if (src_nets.contains(n)) {
                        net_in_common = true;
                        break;
                    }
                }
                if (!net_in_common) {
                    System.err.println("[WARNING] You cannot link two containers that are not in the same network.");
                }
            }
        }
    }

    @RequestMapping(value = "/exampleList", method = RequestMethod.GET)
    public ModelAndView configuration() {

        String[] books = {"Libro 1", "Libro 2", "Libro 3"};

        ModelAndView mav = new ModelAndView();
        mav.setViewName("configs");
        mav.addObject("objs", books);
        return mav;

    }

    @RequestMapping(value = "doLogin", method = RequestMethod.POST)
    public String doLogin(@ModelAttribute Login login) {

        System.out.println(login.toString());

        return "test_form";

    }

    private static void checkSecurityLevel(DirectedBigraph bigraph, String securityLevelsFilePath) throws Exception {
        // use a Scanner to read the lines from the file
        File file = new File(securityLevelsFilePath);
        Scanner sc = new Scanner(file);

        // create a graph to store the network hierarchy
        // acyclic because loop means error
        DirectedAcyclicGraph<String, DefaultEdge> g = new DirectedAcyclicGraph<>(DefaultEdge.class);

        while (sc.hasNextLine()) {
            String line = sc.nextLine();

            Scanner lineScanner = new Scanner(line);
            lineScanner.useDelimiter(">");

            if (lineScanner.hasNext()) { // a line in "n1 > n2" form
                String parent = lineScanner.next().trim(); // first member (whitespaces removed)
                String son = lineScanner.next().trim(); // second member (whitespaces removed)

                // add a vertex for each network specified
                g.addVertex(parent);
                g.addVertex(son);

                // link the two networks
                g.addEdge(parent, son);
            } else {
                throw new Exception("Empty or invalid line. Unable to process.");
            }
        }
        sc.close();


        // support graph
        DirectedGraph<String, DefaultEdge> netSecurityGraph = (DirectedGraph<String, DefaultEdge>) new DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge.class);

        // iterate the nodes of container type
        for (Node n : bigraph.getNodes()) {
            if (n.getControl().getName().equals("container")) {
                String name = n.getName();
                netSecurityGraph.addVertex(name); // add a node for the container to the graph

                for (Child child : n.getChildren()) { // find networks and volumes connected to the container
                    if (child.isNode()) {
                        Node n1 = (Node) child;
                        if (n1.getControl().getName().equals("network")) {
                            if (n1.getOutPort(0).getHandle() != null) { // there is a network the container reads from
                                String net = n1.getOutPort(0).getHandle().toString(); // the name of the network
                                netSecurityGraph.addVertex(net);
                                netSecurityGraph.addEdge(net, name);
                            }

                            if (n1.getOutPort(1).getHandle() != null) { // there is a network the container writes to
                                String net = n1.getOutPort(1).getHandle().toString(); // the name of the network
                                netSecurityGraph.addVertex(net);
                                netSecurityGraph.addEdge(name, net);
                            }
                        } else if (n1.getControl().getName().equals("volume")) {
                            if (n1.getOutPort(0).getHandle() != null) { // there is a volume the container reads from
                                String vol = n1.getOutPort(0).getHandle().toString(); // the name of the volume
                                netSecurityGraph.addVertex(vol);
                                netSecurityGraph.addEdge(vol, name); // read from network
                            }

                            if (n1.getOutPort(1).getHandle() != null) { // there is a volume the container writes to
                                String vol = n1.getOutPort(1).getHandle().toString(); // the name of the volume
                                netSecurityGraph.addVertex(vol);
                                netSecurityGraph.addEdge(name, vol); // write to network
                            }
                        }
                    }
                }
            }
        }

        // search network violations
        TransitiveClosure.INSTANCE.closeSimpleDirectedGraph(g); // compute transitive closure of g
        // create a connectivity inspector for the graph
        ConnectivityInspector<String, DefaultEdge> ci = new ConnectivityInspector<>(netSecurityGraph);

        for (DefaultEdge e : g.edgeSet()) {
            String hi = g.getEdgeSource(e);
            String low = g.getEdgeTarget(e);

            if (!netSecurityGraph.containsVertex(hi)) {
                throw new Exception("[ERROR] You specified a not existing network (" + hi + ")!");
            } else if (!netSecurityGraph.containsVertex(low)) {
                throw new Exception("[ERROR] You specified a not existing network (" + low + ")!");
            } else if (ci.pathExists(hi, low)) {
                System.err.println("[WARNING] Network \"" + low + "\" can read network \"" + hi + "\"!");
            }
        }
    }


    // MODEL DATA -->

    @ModelAttribute("allConfigs")
    public List<UserConfiguration> getConfigs() {

        return configurationService.findAllConfigurations();

    }

    @RequestMapping(value = {"/", "", "home"})
    public ModelAndView home() { return new ModelAndView("home"); }

    @RequestMapping(value = "configurations")
    public ModelAndView configurations() { return new ModelAndView("configurations"); }

    @RequestMapping(value = "about")
    public ModelAndView about() { return new ModelAndView("about"); }

    @ModelAttribute("pageState")
    public HomePageState getPageState() { return this.state; }

    // METODI AUSILIARI ALLA CREAZIONE DEL BIGRAFO -->

    /**
     *
     * @param opt sigla dell'opzione
     * @param longOpt nome dell'opzione
     * @param hasArg booleano (ha argomenti l'opzione?)
     * @param description descrizione dell'opzione
     * @return un'instanza di Option
     */
    private static Option createOption(String opt, String longOpt, boolean hasArg, String description) {

        System.out.println("Imposto le caratteristiche dell'opzione ( opt = " + opt + ", longOpt = " + longOpt + ", descrizione = '" + description + "' )...");
        return new Option(opt, longOpt, hasArg, description);

    }

    /**
     *
     * @param config instanza della classe Options
     * @param myOptions array di instanze di Option valido
     * Effetto collaterale: @config con aggiunge le opzioni contenute nell'array
     */
    private static void addOption(Options config, Option[] myOptions) {

        for (Option opt : myOptions) {
            System.out.println("Opzione aggiunta: " + opt.toString());
            config.addOption(opt);
        }
    }


    // METODI COPIATI DAL VECCHIO PROGETTO

    private static DirectedBigraph docker2ldb(String pathToYAML) throws Exception {
        // parsing yaml config file
        InputStream input = new FileInputStream(new File(pathToYAML));
        Yaml yaml = new Yaml();
        // declarations in the docker-compose.yml file
        Map<String, Map> o = (Map<String, Map>) yaml.load(input);
        Map<String, Map> services = o.get("services");
        Map<String, Map> networks = o.get("networks");
        Map<String, Map> volumes = o.get("volumes");

        System.out.println("YAML config file correctly loaded.");

        boolean default_net = (networks == null); // used to know if networks are used

        // preparing controls, signature and empty bigraph
        List<DirectedControl> controls = new ArrayList<>();

        controls.add(new DirectedControl("container", true, 0, 1));
        controls.add(new DirectedControl("network", true, 2, 0));
        controls.add(new DirectedControl("volume", true, 2, 0));

        DirectedSignature signature = new DirectedSignature(controls);
        DirectedBigraphBuilder cmp = new DirectedBigraphBuilder(signature);

        Root r0 = cmp.addRoot(); // root 0
        System.out.println("Added a root to the bigraph.");

        // networks
        Map<String, OuterName> net_names = new HashMap<>();
        if (default_net) {
            net_names.put("default", cmp.addAscNameOuterInterface(1, "default"));
            System.out.println("Added \"default\" network.");
        } else {
            for (String net : networks.keySet()) {
                net_names.put(net, cmp.addAscNameOuterInterface(1, net));
                System.out.println("Added \"" + net + "\" network.");
            }
        }
        //volumes
        Map<String, OuterName> vol_names = new HashMap<>();
        if (volumes != null) {
            for (String volume : volumes.keySet()) {
                vol_names.put(volume, cmp.addAscNameOuterInterface(1, volume));
                System.out.println("Added named volume \"" + volume + "\".");
            }
        }

        // save service outer names
        int locality = 1;
        Map<String, OuterName> onames = new HashMap<>();

        List<DirectedBigraph> graphs = new ArrayList<>(services.size());

        for (String service : services.keySet()) {
            cmp.addSite(r0); // add a site
            System.out.println("Added a site to the bigraph.");

            if (default_net) {
                cmp.addAscNameInnerInterface(locality, "default", net_names.get("default")); // add default net
            }

            onames.put(service, cmp.addDescNameInnerInterface(locality, service));
            cmp.addDescNameOuterInterface(1, service, onames.get(service)); // expose the name

            locality++;
        }

        locality = 1; // reset counter
        for (String service : services.keySet()) { // parse every service in docker-compose file
            System.out.println("Service: " + service);
            List<String> current_nets = (List<String>) services.get(service).get("networks");
            List<String> current_vols = (List<String>) services.get(service).get("volumes");
            List<String> ports = (List<String>) services.get(service).get("expose");
            List<String> mappings = (List<String>) services.get(service).get("ports");
            List<String> links = (List<String>) services.get(service).get("links");

            DirectedBigraphBuilder current = new DirectedBigraphBuilder(signature);
            System.out.println("Creating a bigraph for the service.");
            Root currentRoot = current.addRoot(); // add a root
            Node node = current.addNode("container", currentRoot);

            current.addSite(node); // add a site for future purposes
            current.addDescNameOuterInterface(1, service, node.getInPort(0).getEditable());
            // networks
            if (default_net) {
                System.out.println("Service connects to network \"default\", adding it to the interface.");

                Node net_node = current.addNode("network", node);
                OuterName net_name = current.addAscNameOuterInterface(1, "default");

                net_node.getOutPort(0).getEditable().setHandle(net_name.getEditable()); // link the net to the node in read mode
                net_node.getOutPort(1).getEditable().setHandle(net_name.getEditable()); // link the net to the node in write mode
            } else if (current_nets == null) {
                throw new Exception("You must declare networks service connects to, because you declared global networks!");
            } else {
                // local_nets cannot be null because previous exception was skipped
                for (String network : current_nets) {
                    if (!networks.keySet().contains(network)) {
                        throw new Exception("Network \"" + network + "\" not declared.");
                    }
                    System.out.println("Service connects to network \"" + network + "\", adding it to the interface.");

                    Node net_node = current.addNode("network", node);
                    OuterName net_name = current.addAscNameOuterInterface(1, network);

                    net_node.getOutPort(0).getEditable().setHandle(net_name.getEditable()); // link the net to the node in read mode
                    net_node.getOutPort(1).getEditable().setHandle(net_name.getEditable()); // link the net to the node in write mode
                    cmp.addAscNameInnerInterface(locality, network, net_names.get(network));
                }
            }
            //volumes
            if (current_vols != null) {
                for (String volume : current_vols) {
                    String[] vs = volume.split(":");
                    if (vs.length > 1) { // check if the volume must be generated
                        if (!vs[0].startsWith("/") && !vs[0].startsWith("./") && !vs[0].startsWith("~/") && (volumes == null || !volumes.keySet().contains(vs[0]))) {
                            throw new Exception("Volume \"" + vs[0] + "\" not declared.");
                        }
                        System.out.println("Service mounts volume \"" + vs[0] + "\" at path \"" + vs[1] + "\", adding it to the interface.");
                        if (!vol_names.containsKey(vs[0])) {
                            vol_names.put(vs[0], cmp.addAscNameOuterInterface(1, vs[0]));
                        }
                        cmp.addAscNameInnerInterface(locality, vs[1], vol_names.get(vs[0]));

                        Node vol_node = current.addNode("volume", node);
                        OuterName vol_name = current.addAscNameOuterInterface(1, vs[1]);

                        vol_node.getOutPort(0).getEditable().setHandle(vol_name.getEditable()); // link the volume to the node in read mode
                        if (!(vs.length == 3 && vs[2].equals("ro"))) {
                            vol_node.getOutPort(1).getEditable().setHandle(vol_name.getEditable()); // link the volume to the node in write mode
                        }
                    } else {
                        System.out.println("Service mounts volume at path \"" + vs[0] + "\", adding it to the interface.");
                        cmp.addAscNameInnerInterface(locality, vs[0], cmp.addAscNameOuterInterface(1, locality + "_" + volume));

                        Node vol_node = current.addNode("volume", node);
                        OuterName vol_name = current.addAscNameOuterInterface(1, vs[0]);

                        vol_node.getOutPort(0).getEditable().setHandle(vol_name.getEditable()); // link the volume to the node in read mode
                        if (!(vs.length == 3 && vs[2].equals("ro"))) {
                            vol_node.getOutPort(1).getEditable().setHandle(vol_name.getEditable()); // link the volume to the node in write mode
                        }
                    }
                }
            }
            // expose
            if (ports != null) {
                for (String port : ports) {
                    System.out.println("Service exposes port " + port + ", adding it to the interface.");
                    current.addDescNameOuterInterface(1, service + "_" + port, current.addDescNameInnerInterface(1, service + "_" + port));
                    cmp.addDescNameInnerInterface(locality, service + "_" + port);
                }
            }
            // ports
            if (mappings != null) {
                for (String map : mappings) {
                    String[] ps = map.split(":");
                    System.out.println("Service maps port " + ps[1] + " to port " + ps[0] + ", adding them to interfaces.");
                    current.addDescNameOuterInterface(1, service + "_" + ps[1], current.addDescNameInnerInterface(1, service + "_" + ps[1]));
                    cmp.addDescNameOuterInterface(1, ps[0], cmp.addDescNameInnerInterface(locality, service + "_" + ps[1]));
                }
            }
            // links
            if (links != null) {
                for (String link : links) {
                    String[] ls = link.split(":");
                    if (ls.length > 1) {
                        if (!onames.containsKey(ls[0])) {
                            throw new Exception("Service \"" + ls[0] + "\" undefined.");
                        }
                        System.out.println("Service links to container \"" + ls[0] + "\", renaming it to \"" + ls[1] + "\" recreating this on interfaces.");
                        current.addAscNameInnerInterface(1, "l_" + ls[1] + "_" + service, current.addAscNameOuterInterface(1, "l_" + ls[1] + "_" + service));
                        cmp.addAscNameInnerInterface(locality, "l_" + ls[1] + "_" + service, onames.get(ls[0]));
                    } else {
                        if (!onames.containsKey(ls[0])) {
                            throw new Exception("Service \"" + ls[0] + "\" undefined.");
                        }
                        System.out.println("Service links to container \"" + ls[0] + "\", recreating this on interfaces.");
                        current.addAscNameInnerInterface(1, "l_" + ls[0] + "_" + service, current.addAscNameOuterInterface(1, "l_" + ls[0] + "_" + service));
                        cmp.addAscNameInnerInterface(locality, "l_" + ls[0] + "_" + service, onames.get(ls[0]));
                    }
                }
            }
            System.out.println("Resulting bigraph: \n" + current);
            System.out.println("----------------------------------------------");
            graphs.add(current.makeBigraph());
            locality++; // ready for the next
        }
        System.out.println("Compose bigraph: \n" + cmp);
        System.out.println("----------------------------------------------");

        List<DirectedBigraph> outs = new ArrayList<>();
        outs.add(cmp.makeBigraph());

        return DirectedBigraph.compose(outs, graphs);
    }

    @RequestMapping(value = "setupPath", method = RequestMethod.POST)
    public String setupConfigurationPath(@ModelAttribute HomePageState myHomePageState) {

        // Aggiorno la variabile di classe
        String acquiredYmlFilePath = myHomePageState.getBigraphFilePathYml();
        System.out.println("DEBUG - Percorso dinamico ottenuto: " + acquiredYmlFilePath);
        this.state.setBigraphFilePathYml(acquiredYmlFilePath);

        // Carico il bigrafo
        DirectedBigraph myBigraph = null;
        if (!myHomePageState.getBigraphFilePathYml().equals("")) {
            try {
                myBigraph = docker2ldb(myHomePageState.getBigraphFilePathYml());
            } catch (Exception e) {
                System.out.println("DEBUG - Errore nel caricamento del bigrafo");
                e.printStackTrace();
            }
        } else {
            System.out.println("DEBUG - Ottenuto un percorso vuoto (stringa vuota)");
        }

        // Checkbox "Usa il file '.yml' fornito"
        boolean checkedYml = myHomePageState.isCbUseProvidedYmlFilePath();
        if (checkedYml) {
            System.out.println("DEBUG - Percorso file YML customizzato scelto");
            if (myHomePageState.isValidYml(myHomePageState.getBigraphFilePathYml())) {
                System.out.println("DEBUG - File YML valido --> carico il bigrafo");
                if (myBigraph != null) {
                    try {
                        checkLinks(myBigraph);
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("DEBUG - Errore nell'esecuzione del metodo 'checkLinks'");
                    }
                } else {
                    System.out.println("DEBUG - Bigrafo null");
                }
            } else {
                System.out.println("DEBUG - File YML non valido --> caricare un altro file");
            }
        } else {
            System.out.println("DEBUG - Utilizzare il percorso di default del file YML");
        }

        // Checkbox "Esegui il check sui link"
        boolean checkedLinks = myHomePageState.isCbDoInitialLinkCheck();
        if (checkedLinks) {
            System.out.println("Link da verificare --> eseguo metodo di verifica");
        }

        // Checkbox "Esegui il check sui livelli di sicurezza"
        boolean checkedSecurityLevels = myHomePageState.isCbDoInitialSecurityLevelsCheck();
        if (checkedSecurityLevels) {
            System.out.println("Livelli di sicurezza da verificare");
            if (myHomePageState.isValidSecurityLevelFile(myHomePageState.getSecurityLevelsFilePath())) {
                System.out.println("DEBUG - File livelli di sicurezza valido --> carico il file");
            } else {
                System.out.println("DEBUG - File livelli di sicurezza non valido --> caricare un altro file");
            }
        }

        return "home";
    }
}
