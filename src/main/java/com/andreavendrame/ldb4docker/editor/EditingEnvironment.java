package com.andreavendrame.ldb4docker.editor;

import com.andreavendrame.ldb4docker.myjlibbig.ldb.*;

import java.util.LinkedList;
import java.util.List;

public class EditingEnvironment {

    /**
     * COSTANTI
     */
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

    public static DirectedBigraphBuilder currentBuilder;
    public static DirectedSignature currentSignature;

    public static final List<Handle> handles = new LinkedList<>();
    public static final List<OuterName> outerNames = new LinkedList<>();
    public static final List<InnerName> innerNames = new LinkedList<>();
    public static final List<Site> sites = new LinkedList<>();
    public static final List<Edge> edges = new LinkedList<>();
    public static final List<Node> nodes = new LinkedList<>();
    public static final List<EditableParent> editableParents = new LinkedList<>();
    public static final List<DirectedControl> bigraphControls = new LinkedList<>();
    public static final List<String> services = new LinkedList<>();
    public static final List<EditableHandle> editableHandles = new LinkedList<>();
    public static final List<InPort> inPorts = new LinkedList<>();


}
