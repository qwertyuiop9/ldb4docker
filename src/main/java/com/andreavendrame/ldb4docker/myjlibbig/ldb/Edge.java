package com.andreavendrame.ldb4docker.myjlibbig.ldb;

/**
 * Edges are handles not accessible through the outer interface on the contrary
 * of outer names ({@link OuterName}).
 */
public interface Edge extends Handle, com.andreavendrame.ldb4docker.myjlibbig.Edge {
	@Override
	public abstract EditableEdge getEditable();
}
