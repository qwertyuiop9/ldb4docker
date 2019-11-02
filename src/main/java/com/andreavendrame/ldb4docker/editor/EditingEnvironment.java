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

    // Environment variables
    public static List<OuterName> outerNames = new LinkedList<>();
    public static List<InnerName> innerNames = new LinkedList<>();
    public static List<Site> sites = new LinkedList<>();
    public static List<DirectedControl> bigraphControls = new LinkedList<>();
    public static List<String> services = new LinkedList<>();
    public static List<InPort> inPorts = new LinkedList<>();
    public static List<Point> points = new LinkedList<>();


}
