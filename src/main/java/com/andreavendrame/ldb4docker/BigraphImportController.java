package com.andreavendrame.ldb4docker;

import com.andreavendrame.ldb4docker.myjlibbig.ldb.*;
import org.springframework.web.bind.annotation.*;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

@RestController
@RequestMapping(value = "/import-service")
public class BigraphImportController {

    private static final String DEFAULT_NETWORK_NAME = "default_network";
    static final int READ_MODE = 0;
    static final int WRITE_MODE = 1;


    @GetMapping(value = "/importBigraph/{bigraphPath}")
    public DirectedBigraph getBigraphBuilder(@PathVariable("bigraphPath") String bigraphFilePath) {

        DirectedBigraph bigraph = null;
        try {
            String correctFilePath = bigraphFilePath.replace("@", "/");
            System.out.println(correctFilePath);
            bigraph = importBigraph(correctFilePath);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return bigraph;
    }

    private DirectedBigraph importBigraph(String pathToYAML) throws Exception {
        // parsing yaml config file
        InputStream input = new FileInputStream(new File(pathToYAML));
        Yaml yaml = new Yaml();
        // declarations in the docker-compose.yml file
        Map<String, Map> o = (Map<String, Map>) yaml.load(input);

        System.out.println();
        Map<String, Map> services = o.get("services");
        Map<String, Map> networks = o.get("networks");
        Map<String, Map> volumes = o.get("volumes");

        System.out.println("YAML config file correctly loaded.");

        boolean useDefaultNetwork = (networks == null); // used to know if networks are used

        // Preparo i controlli, la signature e il bigrafo vuoto
        List<com.andreavendrame.ldb4docker.myjlibbig.ldb.DirectedControl> bigraphControls = prepareBigraphControls();
        DirectedSignature signature = new DirectedSignature(bigraphControls);
        DirectedBigraphBuilder directedBigraphBuilder = new DirectedBigraphBuilder(signature);

        Root rootZero = directedBigraphBuilder.addRoot(); // root 1

        System.out.println("Added a root to the bigraph.");

        // Networks -->
        Map<String, OuterName> networkNames = new HashMap<>();

        if (useDefaultNetwork) {
            OuterName outerName = directedBigraphBuilder.addAscNameOuterInterface(1, DEFAULT_NETWORK_NAME);
            networkNames.put(DEFAULT_NETWORK_NAME, outerName);
            System.out.println("Added \"default\" network.");
        } else {
            for (String network : networks.keySet()) {
                OuterName outerName = directedBigraphBuilder.addAscNameOuterInterface(1, network);
                networkNames.put(network, outerName);
                System.out.println("Added '" + network + "' network.");
            }
        }

        // Volumes -->
        Map<String, OuterName> volumeNames = new HashMap<>();
        if (volumes != null) {
            for (String volume : volumes.keySet()) {
                OuterName outerName = directedBigraphBuilder.addAscNameOuterInterface(1, volume);
                volumeNames.put(volume, outerName);
                System.out.format("Aggiunto un volume chiamato '%s' con outerName '%s'\n", volume, outerName);
            }
        }

        // save service outer names
        int locality = 1;
        Map<String, OuterName> outerNames = new HashMap<>();

        List<DirectedBigraph> graphs = new ArrayList<>(services.size());

        for (String service : services.keySet()) {
            Site site = directedBigraphBuilder.addSite(rootZero); // add a site
            System.out.println("Added a site " + site.toString() + " to the bigraph.");

            if (useDefaultNetwork) {
                directedBigraphBuilder.addAscNameInnerInterface(locality, DEFAULT_NETWORK_NAME, networkNames.get("default")); // add default net
            }

            OuterName outerName = directedBigraphBuilder.addDescNameInnerInterface(locality, service);
            outerNames.put(service, outerName);
            InnerName innerName = directedBigraphBuilder.addDescNameOuterInterface(1, service, outerNames.get(service)); // expose the name

            locality++;
        }

        locality = 1; // reset counter
        for (String service : services.keySet()) { // parse every service in docker-compose file
            System.out.format("Service: %s", service);
            List<String> currentNetworks = (List<String>) services.get(service).get("networks");
            List<String> currentVolumes = (List<String>) services.get(service).get("volumes");
            List<String> ports = (List<String>) services.get(service).get("expose");
            List<String> mappings = (List<String>) services.get(service).get("ports");
            List<String> links = (List<String>) services.get(service).get("links");

            DirectedBigraphBuilder currentBuilder = new DirectedBigraphBuilder(signature);
            System.out.println("Creating a bigraph for the service.");
            Root currentRoot = currentBuilder.addRoot(); // add a root
            Node node = currentBuilder.addNode("container", currentRoot);

            Site site = currentBuilder.addSite(node); // add a site for future purposes
            InnerName innerName = currentBuilder.addDescNameOuterInterface(1, service, node.getInPort(READ_MODE).getEditable());
            // networks
            if (useDefaultNetwork) {
                System.out.println("Service connects to network \"default\", adding it to the interface.");

                Node networkNode = currentBuilder.addNode("network", node);
                OuterName networkName = currentBuilder.addAscNameOuterInterface(1, DEFAULT_NETWORK_NAME);

                networkNode.getOutPort(READ_MODE).getEditable().setHandle(networkName.getEditable()); // link the net to the node in read mode
                networkNode.getOutPort(WRITE_MODE).getEditable().setHandle(networkName.getEditable()); // link the net to the node in write mode

            } else if (currentNetworks == null) {
                throw new Exception("You must declare networks service connects to, because you declared global networks!");
            } else {
                // local_nets cannot be null because previous exception was skipped
                for (String network : currentNetworks) {
                    if (!networks.containsKey(network)) {
                        throw new Exception("Network \"" + network + "\" not declared.");
                    }
                    System.out.println("Service " + service + " connects to network \"" + network + "\", adding it to the interface.");

                    Node networkNode = currentBuilder.addNode("network", node);

                    OuterName networkName = currentBuilder.addAscNameOuterInterface(1, network);

                    networkNode.getOutPort(READ_MODE).getEditable().setHandle(networkName.getEditable()); // link the net to the node in read mode
                    networkNode.getOutPort(WRITE_MODE).getEditable().setHandle(networkName.getEditable()); // link the net to the node in write mode
                    directedBigraphBuilder.addAscNameInnerInterface(locality, network, networkNames.get(network));
                }
            }

            //volumes
            if (currentVolumes != null) {
                for (String volume : currentVolumes) {
                    String[] vs = volume.split(":");
                    if (vs.length > 1) { // check if the volume must be generated
                        if (!vs[0].startsWith("/") && !vs[0].startsWith("./") && !vs[0].startsWith("~/") && (volumes == null || !volumes.containsKey(vs[0]))) {
                            throw new Exception("Volume '" + vs[0] + "' not declared.");
                        }
                        System.out.println("Service mounts volume \"" + vs[0] + "\" at path \"" + vs[1] + "\", adding it to the interface.");
                        if (!volumeNames.containsKey(vs[0])) {
                            OuterName outerName = directedBigraphBuilder.addAscNameOuterInterface(1, vs[0]);
                            volumeNames.put(vs[0], outerName);
                        }
                        directedBigraphBuilder.addAscNameInnerInterface(locality, vs[1], volumeNames.get(vs[0]));

                        Node volumeNode = currentBuilder.addNode("volume", node);
                        OuterName vol_name = currentBuilder.addAscNameOuterInterface(1, vs[1]);

                        volumeNode.getOutPort(READ_MODE).getEditable().setHandle(vol_name.getEditable()); // link the volume to the node in read mode
                        if (!(vs.length == 3 && vs[2].equals("ro"))) {
                            volumeNode.getOutPort(1).getEditable().setHandle(vol_name.getEditable()); // link the volume to the node in write mode
                        }
                    } else {
                        System.out.println("Service mounts volume at path \"" + vs[0] + "\", adding it to the interface.");
                        OuterName outerName = directedBigraphBuilder.addAscNameOuterInterface(1, locality + "_" + volume);
                        directedBigraphBuilder.addAscNameInnerInterface(locality, vs[0], outerName);

                        Node volumeNode = currentBuilder.addNode("volume", node);
                        OuterName vol_name = currentBuilder.addAscNameOuterInterface(1, vs[0]);

                        volumeNode.getOutPort(READ_MODE).getEditable().setHandle(vol_name.getEditable()); // link the volume to the node in read mode
                        if (!(vs.length == 3 && vs[2].equals("ro"))) {
                            volumeNode.getOutPort(WRITE_MODE).getEditable().setHandle(vol_name.getEditable()); // link the volume to the node in write mode
                        }
                    }
                }
            }
            // expose
            if (ports != null) {
                for (String port : ports) {
                    System.out.println("Service exposes port " + port + ", adding it to the interface.");
                    OuterName portOuterName = currentBuilder.addDescNameInnerInterface(1, service + "_" + port);
                    currentBuilder.addDescNameOuterInterface(1, service + "_" + port, portOuterName);
                    directedBigraphBuilder.addDescNameInnerInterface(locality, service + "_" + port);
                }
            }
            // ports
            if (mappings != null) {
                for (String map : mappings) {
                    String[] ps = map.split(":");
                    System.out.println("Service maps port " + ps[1] + " to port " + ps[0] + ", adding them to interfaces.");
                    OuterName mapOuterName = currentBuilder.addDescNameInnerInterface(1, service + "_" + ps[1]);
                    currentBuilder.addDescNameOuterInterface(1, service + "_" + ps[1], mapOuterName);
                    directedBigraphBuilder.addDescNameOuterInterface(1, ps[0], directedBigraphBuilder.addDescNameInnerInterface(locality, service + "_" + ps[1]));
                }
            }
            // links
            if (links != null) {
                for (String link : links) {
                    String[] ls = link.split(":");
                    if (ls.length > 1) {
                        if (!outerNames.containsKey(ls[0])) {
                            throw new Exception("Service \"" + ls[0] + "\" undefined.");
                        }
                        System.out.println("Service links to container \"" + ls[0] + "\", renaming it to \"" + ls[1] + "\" recreating this on interfaces.");
                        currentBuilder.addAscNameInnerInterface(1, "l_" + ls[1] + "_" + service, currentBuilder.addAscNameOuterInterface(1, "l_" + ls[1] + "_" + service));
                        directedBigraphBuilder.addAscNameInnerInterface(locality, "l_" + ls[1] + "_" + service, outerNames.get(ls[0]));
                    } else {
                        if (!outerNames.containsKey(ls[0])) {
                            throw new Exception("Service \"" + ls[0] + "\" undefined.");
                        }
                        System.out.println("Service links to container \"" + ls[0] + "\", recreating this on interfaces.");
                        currentBuilder.addAscNameInnerInterface(1, "l_" + ls[0] + "_" + service, currentBuilder.addAscNameOuterInterface(1, "l_" + ls[0] + "_" + service));
                        directedBigraphBuilder.addAscNameInnerInterface(locality, "l_" + ls[0] + "_" + service, outerNames.get(ls[0]));
                    }
                }
            }
            System.out.println("Resulting bigraph: \n" + currentBuilder);
            System.out.println("----------------------------------------------");

            graphs.add(currentBuilder.makeBigraph());
            locality++; // ready for the next
        }
        System.out.println("Compose bigraph: \n" + directedBigraphBuilder);
        System.out.println("----------------------------------------------");

        List<DirectedBigraph> outs = new ArrayList<>();
        outs.add(directedBigraphBuilder.makeBigraph());

        return DirectedBigraph.compose(outs, graphs);
    }

    /**
     *
     * @return una lista statica di controlli del bigrafo
     */
    public static List<com.andreavendrame.ldb4docker.myjlibbig.ldb.DirectedControl> prepareBigraphControls() {

        List<DirectedControl> bigraphControls = new LinkedList<>();

        // Il container ha una sola porta entrante con il nome del container stesso
        bigraphControls.add(new DirectedControl("container", true, 0, 1));
        // Le reti (networks) hanno due porte uscenti (arityOut = 2): una per leggere e una per scrivere
        bigraphControls.add(new DirectedControl("network", true, 2, 0));
        // I volumi (volumes) hanno due porte uscenti (arityOut = 2): una per leggere e una per scrivere
        bigraphControls.add(new DirectedControl("volume", true, 2, 0));

        return bigraphControls;
    }

}