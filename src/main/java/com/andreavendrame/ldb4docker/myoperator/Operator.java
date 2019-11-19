package com.andreavendrame.ldb4docker.myoperator;

import com.andreavendrame.ldb4docker.editor.NamedDirectedBigraph;
import com.andreavendrame.ldb4docker.myjlibbig.ldb.Child;
import com.andreavendrame.ldb4docker.myjlibbig.ldb.DirectedBigraph;
import com.andreavendrame.ldb4docker.myjlibbig.ldb.Node;
import com.andreavendrame.ldb4docker.myjlibbig.ldb.Point;
import org.springframework.web.bind.annotation.*;
import com.andreavendrame.ldb4docker.myimport.*;

import java.util.*;

import static com.andreavendrame.ldb4docker.editor.EditingEnvironment.myDirectedBigraphs;
import static com.andreavendrame.ldb4docker.editor.Editor.INVALID_NAME;

@RestController
//@RequestMapping(value = "/operate")
public class Operator {

    @GetMapping(name = "/connection")
    private String isConnected() {
        return "Operate module connected";
    }

    @PostMapping(name = "/testCheckLinks")
    private static List<String> prepareTest() {

        DirectedBigraph testBigraph = DirectedBigraphImporter.importTest();
        List<String> containersNotConnected = new LinkedList<>();
        try {
            containersNotConnected = checkLinks(testBigraph);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return containersNotConnected;
    }

    @GetMapping(name = "checkLinks")
    private static List<String> checkBigraphLinks(@RequestParam(name = "bigraphName", defaultValue = INVALID_NAME) String bigraphName) {

        if (bigraphName.equals(INVALID_NAME)) {
            return null;
        } else {
            DirectedBigraph bigraphSelected = null;
            for (NamedDirectedBigraph namedDirectedBigraph : myDirectedBigraphs) {
                if (namedDirectedBigraph.getName().equals(bigraphName)) {
                    try {
                        return checkLinks(namedDirectedBigraph.getDirectedBigraph());
                    } catch (Exception e) {
                        System.out.println("Error in the method 'checkLinks'");
                        return null;
                    }
                }
            }
            return null;
        }
    }

    private static List<String> checkLinks(DirectedBigraph bigraph) throws Exception {

        Map<String, List<String>> links = new HashMap<>();
        Map<String, List<String>> nets = new HashMap<>();
        Map<String, String> names = new HashMap<>();

        List<String> containersNotConnected = new LinkedList<>();

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
                    String notConnected = ss2[1] + "-" + ss2[2];
                    containersNotConnected.add(notConnected);
                    System.err.println("[WARNING] You cannot link two containers that are not in the same network.");
                }
            }
        }

        System.out.format("Container not connected: %s", containersNotConnected);
        return containersNotConnected;
    }

}
