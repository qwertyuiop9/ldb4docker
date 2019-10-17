package com.andreavendrame.ldb4docker.myjlibbig.std;


/**
 * Points are link entities connected by the hyper-edges composing the link
 * graphs. Points are inner names or ports depending on whereas they belong to
 * an inner interface or to a node.
 */
public interface Point extends LinkEntity, com.andreavendrame.ldb4docker.myjlibbig.Point {
	@Override
	public abstract EditableHandle getHandle();

	public abstract EditablePoint getEditable();
}
