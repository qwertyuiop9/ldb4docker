package com.andreavendrame.ldb4docker.editor;

import com.andreavendrame.ldb4docker.myjlibbig.ldb.DirectedBigraph;

public class NamedDirectedBigraph {

    private DirectedBigraph directedBigraph;
    private String name;

    public NamedDirectedBigraph(DirectedBigraph directedBigraph, String name) {
        this.directedBigraph = directedBigraph;
        this.name = name;
    }

    public DirectedBigraph getDirectedBigraph() {
        return directedBigraph;
    }

    public String getName() {
        return name;
    }
}
