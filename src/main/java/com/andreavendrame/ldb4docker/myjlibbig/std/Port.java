package com.andreavendrame.ldb4docker.myjlibbig.std;

/**
 * Ports are end-points for hyper-edges composing the link graphs and are
 * exposed by nodes structured in the place graph composing the bigraph together
 * with the aforementioned link graph. Despite a link graph {@link Point}, ports
 * belong to a node and are identified by their number or position w.r.t. that
 * node.
 */
public interface Port extends Point, com.andreavendrame.ldb4docker.myjlibbig.Port<Control> {

	@Override
	public abstract Node getNode();

	@Override
	public abstract EditableNode.EditablePort getEditable();
}