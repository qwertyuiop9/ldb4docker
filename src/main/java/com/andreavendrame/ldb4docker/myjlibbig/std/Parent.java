package com.andreavendrame.ldb4docker.myjlibbig.std;

import java.util.Collection;

/**
 * Place graph entities are organised in tree-like structures.
 * 
 * @see Child
 */
public interface Parent extends PlaceEntity, com.andreavendrame.ldb4docker.myjlibbig.Parent {
	@Override
	public abstract Collection<? extends Child> getChildren();

	public abstract EditableParent getEditable();
}
