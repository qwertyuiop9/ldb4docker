package com.andreavendrame.ldb4docker.myjlibbig.std;

import com.andreavendrame.ldb4docker.myjlibbig.attachedProperties.PropertyTarget;

import java.util.*;

/**
 * Describes nodes of bigraphs with abstract internal names. For this kind of
 * nodes identity is instance based. Nodes are assigned a {@link Control} which
 * specifies their arity i.e. the number of ports exposed by a node. Ports are
 * end-points for hyper-edges composing the link graph.
 */
public interface Node extends PropertyTarget, Parent, Child,
		com.andreavendrame.ldb4docker.myjlibbig.Node<Control> {
	public List<? extends Port> getPorts();

	@Override
	public abstract Port getPort(int index);

	@Override
	public abstract Control getControl();

	/*
	 * Actually this is a dirty trick prevent implementation of this interface
	 * outside of the package.
	 */
	@Override
	public abstract EditableNode getEditable();
}