package com.andreavendrame.ldb4docker.myjlibbig;

import java.util.Collection;
/**
 * Place graph entities are organised in tree-like structures.
 * 
 * @see Child
 */
public interface Parent extends PlaceEntity {
	/**
	 * Gets the collection of children of this parent.
	 * 
	 * @return the children collection.
	 */
	public abstract Collection<? extends Child> getChildren();
}
