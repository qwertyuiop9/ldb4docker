package com.andreavendrame.ldb4docker.myexport;

import com.andreavendrame.ldb4docker.myjlibbig.ldb.DirectedBigraph;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

import static com.andreavendrame.ldb4docker.editor.EditingEnvironment.sourceBigraphToCompose;
import static com.andreavendrame.ldb4docker.editor.Editor.INVALID_NAME;

@RestController
@RequestMapping(value = "/export")
public class DirectedBigraphExporter {

    public DirectedBigraphExporter() {
    }

    @GetMapping(value = "/exportBigraph")
    private String exportBigraphToYml(DirectedBigraph exploredBigraph, List<DirectedBigraph> sourceBigraphs) throws Exception {

        String bigraph = exploredBigraph.toString();
        System.out.println("DIMENSIONE LISTA TO COMPOSE :"  + sourceBigraphToCompose.size());

        List<String> innerInterfaceServices = getSusbtringList(bigraph, "I", "}", false);

        String[] locatedServices = getServicesByLocality(innerInterfaceServices);
        List<String> partialInterfaces = getSusbtringList(exploredBigraph.getInnerInterface().toString(), "(", ")", false);
        int[] serviceInnerPorts = new int[locatedServices.length];
        int[] serviceOuterPorts = new int[locatedServices.length];

        for (int i=0; i<locatedServices.length; i++) {
            int inPort = getServiceInnerPort(partialInterfaces.get(i+1), locatedServices[i]);
            serviceInnerPorts[i] = inPort;
            int outPort = getServiceOuterPort(exploredBigraph.toString(), locatedServices[i], inPort);
            serviceOuterPorts[i] = outPort;
        }

        List<String> links = getInterfaceList(exploredBigraph.getInnerInterface().toString());
        List<String[]> linkSourceDestination = new LinkedList<>();
        for (String link : links) {
            linkSourceDestination.add(getServiceLinkedTo(link, locatedServices));
        }

        System.out.println("\n\n");

        Map<String, List<String>> destinationOf = getDestinationOf(exploredBigraph.toString(), locatedServices);
        for (String service : destinationOf.keySet()) {
            List<String> sourceOf = destinationOf.get(service);
            System.out.println("Key: " + service);
            for (String source: sourceOf) {
                System.out.format("Sorgente: %s, destinazione: %s\n", source, service);
            }
        }

        Map<String, List<String[]>> finalLinks = new HashMap<>();
        for (String key : destinationOf.keySet()) {
            List<String[]> correcLinks = new LinkedList<>();
            List<String> sources = destinationOf.get(key);
            for (String source : sources) {
                for (String[] link : linkSourceDestination) {
                    if (link[0].equals(source)) {
                        correcLinks.add(new String[]{link[0], key, link[2]});
                    }
                }
            }
            finalLinks.put(key, correcLinks);
        }

        for (String key : finalLinks.keySet()) {
            List<String[]> arrays = finalLinks.get(key);
            for (String[] array : arrays) {
                System.out.format("Final links (key:%S)-- Source: %s, destination: %s, name: %s\n",key, array[0], array[1], array[2]);
            }
        }

        Map<String, List<String>> networks = new HashMap<>();
        for (DirectedBigraph composingBigraph : sourceBigraphToCompose) {
            String service = getServiceFromSimpleBigraph(composingBigraph.toString());
            List<String> nets = getNetworks(composingBigraph.toString(), service);
            networks.put(service, nets);
        }

        for (String key : networks.keySet()) {
            List<String> temp = networks.get(key);
            for (String s : temp) {
                System.out.format("Service %s connected to network %s\n", key, s);
            }
        }

        System.out.println("\n");
        Map<String, List<String>> volumes = new HashMap<>();
        Map<String, Boolean> isVolumeReadOnly = new HashMap<>();
        for (DirectedBigraph composingBigraph : sourceBigraphToCompose) {
            String currentService = getServiceFromSimpleBigraph(composingBigraph.toString());
            List<String> currentVolumes = getVolumes(composingBigraph.toString());
            volumes.put(currentService, currentVolumes);
            for (String volumeName : currentVolumes) {
                isVolumeReadOnly.put(volumeName, isReadOnly(composingBigraph.toString(), volumeName));
            }
        }

        for (String key : volumes.keySet()) {
            List<String> list = volumes.get(key);
            for (String obj : list) {
                System.out.format("Chiave %s --> %s\n", key, obj);
            }
        }

        for (String volume : isVolumeReadOnly.keySet()) {
            System.out.format("Volume '%s' is read only: %b\n", volume, isVolumeReadOnly.get(volume));
        }

        StringBuilder outYaml = new StringBuilder();
        for (int i=0; i<locatedServices.length; i++) {
            String service = locatedServices[i];
            outYaml.append(service).append(":\n");
            List<String[]> outLinks = getRelatedLinks(finalLinks, service);

            System.out.println("Checking if there are links to insert...");
            if (outLinks.size() > 0) {
                outYaml.append("    links:\n");
                for (String[] outLink : outLinks) {
                    StringBuilder pendingLink = new StringBuilder();
                    pendingLink.append("      ");
                    pendingLink.append("-").append(outLink[1]);
                    if (!outLink[2].equals(INVALID_NAME)) {
                        pendingLink.append(":").append(outLink[2]);
                    }
                    outYaml.append(pendingLink.toString()).append("\n");
                }
            }

            System.out.println("Checking if there are ports (external)");
            if (serviceOuterPorts[i] != -1) {
                System.out.println("This service has ports to show");
                outYaml.append("    ports:\n");
                outYaml.append("      -\"").append(serviceOuterPorts[i]);
                outYaml.append(":").append(serviceInnerPorts[i]).append("\"\n");
            } else {
                System.out.println("This service has only an internal port to show to the other services (expose)");
                outYaml.append("    expose:\n");
                outYaml.append("      -\"").append(serviceInnerPorts[i]);
                outYaml.append("\"\n");
            }

            System.out.println("Checking if there are networks to insert...");
            List<String> currentNetworks = networks.get(service);
            if (currentNetworks == null) {
                System.out.format("List of attached networks to the service %s null", service);
            } else {
                if (currentNetworks.size() > 0) {
                    outYaml.append("    networks:\n");
                    for (String network : currentNetworks) {
                        outYaml.append("      -").append(network).append("\n");
                    }
                }
            }

            System.out.println("Checking if there are volumes to insert...");
            List<String> currentVolumes = volumes.get(service);
            if (currentVolumes == null) {
                System.out.format("List of attached volumes to the service %s null", service);
            } else {
                if (currentVolumes.size() > 0) {
                    outYaml.append("    volumes:\n");
                    for (String volume : currentVolumes) {
                        outYaml.append("      - datavolume:").append(volume);
                        if (isVolumeReadOnly.get(volume)) {
                            outYaml.append(":ro");
                        }
                        outYaml.append("\n");
                    }
                }
            }


            System.out.println("Adding all the used networks...");
            outYaml.append("networks:\n");
            for (String network : getFinalValues(networks)) {
                outYaml.append("  ").append(network).append(":\n");
                outYaml.append("     ").append("driver:bridge\n");
            }

            System.out.println("Adding all the used volumes...");
            outYaml.append("volumes:\n");
            List<String> baseVolumes = getBaseVolumes(exploredBigraph.getOuterInterface().toString(), networks);
            for (String baseVolume : baseVolumes) {
                outYaml.append("   ").append(baseVolume).append(":\n");
                outYaml.append("      external:true\n");
            }

        }

        System.out.println("OUTPUT ------------------------->\n\n");
        System.out.println(outYaml.toString());

        return outYaml.toString();

    }

    private DirectedBigraph getBigraphByServiceName(List<DirectedBigraph> sourceBigraphToCompose, String locatedService) {

        int count = 0;
        DirectedBigraph selectedBigraph = null;
        for (DirectedBigraph bigraph : sourceBigraphToCompose) {
            if (bigraph.toString().contains(locatedService)) {
                selectedBigraph = bigraph;
                count++;
            }
        }

        if (count > 1) {
            System.out.println("ERROR in the method 'getBigraphByServiceName'");
            return null;
        } else {
            return selectedBigraph;
        }

    }

    private static List<String> getSusbtringList(String text, String start, String end, boolean onlyServiceName) {

        String textToAnalize = text;
        List<String> output = new LinkedList<>();

        int index = textToAnalize.indexOf(start);
        int indexOfEnd = text.length();

        while (index != -1 && indexOfEnd != -1) {
            textToAnalize = textToAnalize.substring(index+1);
            indexOfEnd = textToAnalize.indexOf(end);
            if (indexOfEnd != -1) {
                output.add(textToAnalize.substring(start.length()-1, indexOfEnd));
                index = textToAnalize.indexOf(start);
            }
        }

        List<String> cleanedServices = new LinkedList<>();
        if (onlyServiceName) {
            for (String service : output) {
                if (service.contains(":i")) {
                    cleanedServices.add(service.substring(0, service.indexOf(":i")));
                } else {
                    cleanedServices.add(service);
                }
            }
            return cleanedServices;
        } else {
            return output;
        }
    }

    private static String[] getServicesByLocality(List<String> innerInterface) {

        String[] locatedServices = new String[innerInterface.size()];

        for (String service : innerInterface) {
            int locality = Integer.parseInt(service.substring(0, service.indexOf("-")));
            locatedServices[locality-1] = service.substring(service.indexOf("-.")+2, service.indexOf("_"));
        }
        return locatedServices;
    }

    private static int getServiceInnerPort(String interfaceString, String service) {

        String toSearch = "{[" + service + "_";
        int startIndex = interfaceString.indexOf(toSearch) + toSearch.length();
        String portString = interfaceString.substring(startIndex);
        int endIndex = portString.indexOf("]}");
        portString = portString.substring(0, endIndex);

        return Integer.parseInt(portString);
    }

    private static int getServiceOuterPort(String text, String service, int inPort) {

        String toSearch = "-." + service + "_" + inPort + " <- {O-.";
        int start = text.indexOf(toSearch) + toSearch.length();
        String cut = text.substring(start);
        int end = cut.indexOf("}");
        int outPort = -1;
        if (end <= 4) {
            outPort = Integer.parseInt(cut.substring(0, end));
            System.out.format("The service '%s' has a mapping port from %d to %d.\n", service, inPort, outPort);
        }
        return outPort;
    }

    private static List<String> getInterfaceList(String innerInterface) {

        List<String> links = new LinkedList<>();
        String text = innerInterface;
        String toSearch = "({[l_";
        int start = innerInterface.indexOf(toSearch);
        while (start != -1) {
            text = text.substring(start + 3);
            int end = text.indexOf("]}+");
            String link = text.substring(0, end);
            links.add(link);
            start = text.indexOf(toSearch);
        }

        return links;
    }

    private static boolean isService(String[] services, String stringToTest) {

        boolean isService = false;
        for (String service : services) {
            if (service.equals(stringToTest)) {
                isService = true;
            }
        }

        return isService;

    }

    /**
     *
     *
     * @param link l_destinationService/destinationServiceName_sourceService
     * @return [sourceService, destinationService, destinationName]
     */
    private static String[] getServiceLinkedTo(String link, String[] serviceNames) {

        String[] output = new String[3];
        String text = link;
        for (int i=0; i<output.length; i++) {
            output[i] = INVALID_NAME;
        }

        text = text.substring(2);
        String destinationService = text.substring(0, text.indexOf("_"));
        String sourceService = text = text.substring(text.indexOf("_") + 1);

        output[0] = sourceService;
        if (isService(serviceNames, destinationService)) {
            output[1] = destinationService;
            output[2] = INVALID_NAME;
        } else {
            output[1] = INVALID_NAME;
            output[2] = destinationService;
        }

        System.out.format("Source service: %s, destinazione service: %s, destination name: %s\n", output[0], output[1], output[2]);

        return output;
    }

    private static Map<String, List<String>> getDestinationOf(String text, String[] locatedServices) {

        Map<String, List<String>> destinationOf = new HashMap<>();
        for (String s : getSusbtringList(text, ":container <- {",":i}",false)) {
            String currentService;
            if (s.contains(":i")) {
                currentService = s.substring(0, s.indexOf(":i"));
            } else {
                currentService = s;
            }
            String[] toTest = s.split(" ");

            List<String> serviceConnected = new LinkedList<>();
            for (int i=0; i<toTest.length; i++) {
                if (toTest[i].contains("l")) {
                    // It is a link
                    for (String service : locatedServices) {
                        if (toTest[i].contains(service) && !service.equals(currentService)) {
                            serviceConnected.add(service);
                        }
                    }
                    destinationOf.put(currentService, serviceConnected);
                }
            }
        }
        return destinationOf;
    }

    private static List<String> getNetworks(String text, String service) {

        List<String> networks = new LinkedList<>();

        if (text.contains(service)) {
            List<String> rowsToCheck = getSusbtringList(text, "O", "}", false);
            List<String> tempList = getStringListWith(rowsToCheck, "network");
            for (String string : tempList) {
                String networkName = string.substring(string.indexOf("+.") + 2, string.indexOf(" <- {"));
                System.out.format("SERVICE %s, NETWORK NAME: %s\n", service, networkName);
                networks.add(networkName);
            }
        }


        return networks;
    }

    private static List<String> getVolumes(String text) {

        List<String> volumes = new LinkedList<>();
        List<String> rowsToCheck = getSusbtringList(text, "O", "}", false);
        List<String> volumeRows = getStringListWith(rowsToCheck, "volume");
        for (String string : volumeRows) {
            String volumeName = string.substring(string.indexOf("+.") + 2, string.indexOf(" <- {"));
            volumes.add(volumeName);
        }

        return volumes;
    }

    private static List<String> getStringListWith(List<String> source, String target) {

        for (int i = source.size()-1; i>=0; i--) {
            if (!source.get(i).contains(target)) {
                source.remove(i);
            }
        }

        return source;
    }

    private static String getServiceFromSimpleBigraph(String text) {

        String toSearch = ":container <- {";
        String endString = ":i}";

        int start = text.indexOf(toSearch) + toSearch.length();
        String temp = text.substring(start);
        return temp.substring(0, temp.indexOf(endString));

    }

    private static boolean isReadOnly(String text, String volumeName) {

        String toSearch = volumeName + " <- ";
        String temp = text;
        temp = temp.substring(temp.indexOf(toSearch) + toSearch.length());
        temp = temp.substring(temp.indexOf("{") + 1);
        temp = temp.substring(0, temp.indexOf("}"));
        return temp.contains(",");
    }

    private static List<String[]> getRelatedLinks(Map<String, List<String[]>> map, String source) {

        Collection<List<String[]>> set = map.values();

        List<String[]> out = new LinkedList<>();

        for (List<String[]> links : set) {
            for (String[] link : links) {
                if (link[0].equals(source)) {
                    out.add(link);
                }
            }
        }

        return out;

    }

    private static List<String> getFinalValues(Map<String, List<String>> data) {

        List<String> all = new LinkedList<>();
        for (String key : data.keySet()) {
            for (String value : data.get(key)) {
                if (!all.contains(value)) {
                    all.add(value);
                }
            }
        }

        return all;

    }

    private static List<String> getBaseVolumes(String outerInterface, Map<String, List<String>> networks) {

        List<String> baseVolumes = new LinkedList<>();
        String temp = outerInterface.substring(outerInterface.indexOf(","));
        temp = temp.substring(temp.indexOf("[")+1);
        temp = temp.substring(temp.indexOf("[") + 1);
        temp = temp.substring(0, temp.indexOf("]"));
        System.out.println("TEMP TEMP: " + temp);
        String[] objects = temp.split(",");
        for (int i=0; i<objects.length; i++) {
            objects[i] = objects[i].trim();
        }

        for (int i=0; i<objects.length; i++) {
            System.out.println("Base volume: " + objects[i]);
            for (String object : getFinalValues(networks)) {
                if (objects[i].equals(object)) {
                    objects[i] = "";
                }
            }
        }

        for (int i=0; i<objects.length; i++) {
            if (!objects[i].equals("")) {
                baseVolumes.add(objects[i]);
            }
        }

        return baseVolumes;
    }

}
