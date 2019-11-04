package com.andreavendrame.ldb4docker.editor;

import com.andreavendrame.ldb4docker.myjlibbig.ldb.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedList;
import java.util.List;

import static com.andreavendrame.ldb4docker.editor.EditingEnvironment.*;
import static com.andreavendrame.ldb4docker.editor.Editor.INVALID_INDEX;
import static com.andreavendrame.ldb4docker.editor.Editor.INVALID_NAME;

@RestController
@RequestMapping(value = "/inspector")
public class EditorInspector {

    @GetMapping(value = "showNodes")
    private String showNodes(@RequestParam(value = "nodeIndex", defaultValue = "-1") int nodeIndex) {

        StringBuilder stringBuilder = new StringBuilder();
        List<Node> nodes = new LinkedList<>(currentBuilder.getNodes());

        if (nodeIndex != -1) {
            addNodeDescription(stringBuilder, nodeIndex, nodes);
        } else {
            for (int i = 0; i < nodes.size(); i++) {
                addNodeDescription(stringBuilder, i, nodes);
            }
        }

        return stringBuilder.toString();
    }

    /**
     * @param stringBuilder in cui salvare le informazioni del nodo considerato
     * @param i             indice del nodo nella lista interna all'editor
     */
    private void addNodeDescription(StringBuilder stringBuilder, int i, List<Node> nodes) {
        Node currentNode = nodes.get(i);
        stringBuilder.append("<<--- NODE (").append(i).append(") '").append(currentNode.getName()).append("' - ");
        stringBuilder.append("Parent: ' ").append(currentNode.getParent().getEditable().toString()).append("' ");
        stringBuilder.append("- Out ports [ ");
        for (OutPort outPort : currentNode.getOutPorts()) {
            stringBuilder.append(outPort.toString()).append(", ");
        }
        stringBuilder.append("], ");
        stringBuilder.append("- In ports [ ");
        for (InPort inPort : currentNode.getInPorts()) {
            stringBuilder.append(inPort.toString()).append(", ");
        }
        stringBuilder.append("] --->> ");
    }

    /**
     * @param index indice della radice da ispezionare
     * @return una stringa che descrive il nome della radice e quello dei figli ad essa direttamente collegati (se presenti) se
     * il parametro {@param index} è valido, mentre la lista di tutte le radici con i figli direttamente connessi altrimenti
     */
    @GetMapping(value = "/showRoots")
    private String showRoots(@RequestParam(value = "index", defaultValue = "-1") int index) {
        StringBuilder stringBuilder = new StringBuilder();

        List<Root> roots = new LinkedList<>(currentBuilder.getRoots());

        if (index != -1) {
            EditableRoot editableRoot = roots.get(index).getEditable();
            stringBuilder.append("'").append(editableRoot.toString()).append("'").append(" --> ");
            addChildrenInformation(stringBuilder, editableRoot);
        } else {
            for (Root root : roots) {
                EditableRoot editableRoot = root.getEditable();
                stringBuilder.append("'").append(editableRoot.toString()).append("'");
                stringBuilder.append(" - Children: [");
                addChildrenInformation(stringBuilder, editableRoot);
                stringBuilder.append("] ");
            }
        }

        return stringBuilder.toString();
    }

    @GetMapping(value = "showOuterNames")
    private String showOuterNames(@RequestParam(value = "index", defaultValue = "-1") int index) {

        StringBuilder stringBuilder = new StringBuilder();

        if (index != -1) {
            addOuterNameDescription(stringBuilder, index);
        } else {
            for (int i = 0; i < outerNames.size(); i++) {
                addOuterNameDescription(stringBuilder, i);
            }
        }

        return stringBuilder.toString();
    }

    @GetMapping(value = "showInnerNames")
    private String showInnerNames(@RequestParam(value = "index", defaultValue = "-1") int index) {

        StringBuilder stringBuilder = new StringBuilder();

        if (index != -1) {
            addInnerNameDescription(stringBuilder, index);
        } else {
            for (int i = 0; i < innerNames.size(); i++) {
                addInnerNameDescription(stringBuilder, i);
            }
        }

        return stringBuilder.toString();
    }

    /**
     * @param index indice del sito da ispezionare
     * @return una stringa che descrive il nome del nodo e quello del suo parent ad esso direttamente collegato se
     * il parametro {@param index} è valido, mentre la lista di tutte le istanze di site con i parent direttamente connessi altrimenti
     */
    @GetMapping(value = "/showSites")
    private String showSites(@RequestParam(value = "index", defaultValue = INVALID_INDEX) int index,
                             @RequestParam(value = "name", defaultValue = INVALID_NAME) String name) {

        StringBuilder stringBuilder = new StringBuilder();
        if (name.equals(INVALID_NAME)) {
            if (index != -1) {
                EditableSite editableSite = EditingEnvironment.sites.get(index).getEditable();
                addSiteInformation(stringBuilder, editableSite);
            } else {
                for (Site site : EditingEnvironment.sites) {
                    EditableSite editableSite = site.getEditable();
                    addSiteInformation(stringBuilder, editableSite);
                }
            }
        } else {
            Site targetSite = null;
            for (Site site : sites) {
                if (site.getEditable().toString().equals(name)) {
                    targetSite = site;
                }
            }
            if (targetSite == null) {
                stringBuilder.append("No site founded with name '").append(name).append("'...");
            } else {
                addSiteInformation(stringBuilder, targetSite.getEditable());
            }
        }

        return stringBuilder.toString();
    }

    /**
     * @param index of the edge to inspect, if invalid all the edges have to be inspected
     * @param name  name of the edge to inspect
     * @return a description of the edge/s got
     */
    @GetMapping(value = "/showEdges")
    private String showEdges(@RequestParam(value = "index", defaultValue = INVALID_INDEX) int index,
                             @RequestParam(value = "name", defaultValue = INVALID_NAME) String name) {

        List<Edge> edges;
        StringBuilder stringBuilder = new StringBuilder();

        if (name.equals(INVALID_NAME)) {
            if (index == -1) {
                edges = Editor.getEdges(INVALID_NAME, -1);
                for (Edge edge : edges) {
                    addEdgeInformation(stringBuilder, edge.getEditable());
                }
            } else {
                edges = Editor.getEdges(INVALID_NAME, index);
                EditableEdge editableEdge = edges.get(index).getEditable();
                addEdgeInformation(stringBuilder, editableEdge);
            }
        } else {
            Edge targetEdge = Editor.getEdges(name, -1).get(0);
            addEdgeInformation(stringBuilder, targetEdge.getEditable());
        }

        return stringBuilder.toString();
    }

    private void addSiteInformation(StringBuilder stringBuilder, EditableSite editableSite) {
        stringBuilder.append("<-- Site: '").append(editableSite.toString()).append("'").append(", ");
        stringBuilder.append("[parent: '").append(editableSite.getParent().toString()).append("]");
        stringBuilder.append(" -->");
    }

    private void addOuterNameDescription(StringBuilder stringBuilder, int i) {
        OuterName outerName = outerNames.get(i);
        addNameDescription(stringBuilder, outerName.getName(), "OuterName");
    }

    private void addInnerNameDescription(StringBuilder stringBuilder, int i) {
        InnerName innerName = innerNames.get(i);
        addNameDescription(stringBuilder, innerName.getName(), "InnerName");
    }

    private void addNameDescription(StringBuilder stringBuilder, String name, String nameType) {

        if (name.equals("InnerName")) {
            stringBuilder.append("InnerName '").append(name).append("', ");
        } else if (name.equals("OuterName")) {
            stringBuilder.append("OuterName '").append(name).append("', ");
        } else {
            System.out.println("No valid name type specified...");
        }
    }

    private static void addChildrenInformation(StringBuilder stringBuilder, EditableRoot editableRoot) {
        editableRoot.getEditableChildren().forEach(editableChild -> {
            stringBuilder.append("'").append(editableChild.toString()).append("'");
            stringBuilder.append(", ");
        });
    }

    private static void addEdgeInformation(StringBuilder stringBuilder, EditableEdge editableEdge) {

        stringBuilder.append("Edge: '").append(editableEdge.getName()).append("', ");
    }

    @GetMapping("/showTemporaryHandleList")
    private static String showTemporaryHandleList() {

        StringBuilder stringBuilder = new StringBuilder();
        for (NamedHandle namedHandle : tempHandles) {
            stringBuilder.append("[").append(namedHandle.getHandleName()).append("],");
        }

        return stringBuilder.toString();
    }

    @GetMapping("/showTemporaryPointList")
    private static String showTemporaryPointList() {

        StringBuilder stringBuilder = new StringBuilder();
        for (NamedPoint namedPoint : tempPoints) {
            stringBuilder.append("[").append(namedPoint.getName()).append("],");
        }

        return stringBuilder.toString();
    }

    @GetMapping("/showTemporaryRootList")
    private static String showTemporaryRootList() {

        StringBuilder stringBuilder = new StringBuilder();
        for (Root root : tempRoots) {
            stringBuilder.append("[").append(root.getEditable().toString()).append("],");
        }

        return stringBuilder.toString();
    }

    @GetMapping("/showDirectedBigraphList")
    private static String showDirectedBigraphList() {

        StringBuilder stringBuilder = new StringBuilder();
        for (NamedDirectedBigraph namedDirectedBigraph : myDirectedBigraphs) {
            stringBuilder.append("[Name: '").append(namedDirectedBigraph.getName()).append("'],");
        }

        return stringBuilder.toString();
    }
}
