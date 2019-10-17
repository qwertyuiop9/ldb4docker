package com.andreavendrame.ldb4docker.myjlibbig.ldb;

/**
 * In ports are
 */
public interface InPort extends Handle, com.andreavendrame.ldb4docker.myjlibbig.Port<DirectedControl> {

    @Override
    public abstract Node getNode();

    @Override
    public abstract EditableNode.EditableInPort getEditable();
}