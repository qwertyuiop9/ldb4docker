package com.andreavendrame.ldb4docker.editor;

import com.andreavendrame.ldb4docker.myjlibbig.ldb.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicInteger;

import static com.andreavendrame.ldb4docker.editor.EditingEnvironment.outerNames;

@RestController
@RequestMapping(value = "/inspector")
public class EditorInspector {

    @GetMapping(value = "showNodes")
    private String showNodes(@RequestParam(value = "nodeIndex", defaultValue = "-1") int nodeIndex) {

        StringBuilder stringBuilder = new StringBuilder();

        if (nodeIndex != -1 ) {
            addNodeDescription(stringBuilder, nodeIndex);
        } else {
            for (int i = 0; i< EditingEnvironment.nodes.size(); i++) {
                addNodeDescription(stringBuilder, i);
            }
        }

        return stringBuilder.toString() ;
    }

    /**
     *
     * @param stringBuilder in cui salvare le informazioni del nodo considerato
     * @param i indice del nodo nella lista interna all'editor
     */
    private void addNodeDescription(StringBuilder stringBuilder, int i) {
        Node currentNode = EditingEnvironment.nodes.get(i);
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

    /**
     * @param index indice della radice da ispezionare
     * @return una stringa che descrive il nome della radice e quello dei figli ad essa direttamente collegati (se presenti) se
     * il parametro {@param index} è valido, mentre la lista di tutte le radici con i figli direttamente connessi altrimenti
     */
    @GetMapping(value = "/showRoots")
    private String showRoots(@RequestParam(value = "index", defaultValue = "-1") int index) {
        StringBuilder stringBuilder = new StringBuilder();

        if (index != -1) {
            EditableRoot editableRoot = EditingEnvironment.currentBuilder.getRoots().get(index).getEditable();
            stringBuilder.append("Radice ").append(editableRoot.toString()).append(" --> ");
            int childIndex = 0;
            editableRoot.getEditableChildren().forEach(editableChild -> {
                stringBuilder.append("Child ").append(childIndex).append(" : ");
                stringBuilder.append(editableChild.toString());
                stringBuilder.append(", ");
            });
        } else {
            for (Root root : EditingEnvironment.currentBuilder.getRoots()) {
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

    @GetMapping(value = "showOuterNames")
    private String showOuterNames(@RequestParam(value = "index", defaultValue = "-1") int index) {

        StringBuilder stringBuilder = new StringBuilder();

        if (index != -1 ) {
            addOuterNameDescription(stringBuilder, index);
        } else {
            for (int i=0; i<outerNames.size(); i++) {
                addOuterNameDescription(stringBuilder, i);
            }
        }

        return stringBuilder.toString() ;
    }

    private void addOuterNameDescription(StringBuilder stringBuilder, int i) {
        OuterName outerName = outerNames.get(i);
        stringBuilder.append("OuterName ").append(i).append(") ").append(outerName.toString()).append(", ");
    }

}
