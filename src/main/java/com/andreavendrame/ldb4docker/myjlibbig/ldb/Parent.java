package com.andreavendrame.ldb4docker.myjlibbig.ldb;

import java.util.Collection;

/**
 * Place graph entities are organised in tree-like structures.
 *
 * @see Child
 */
public interface Parent extends PlaceEntity, com.andreavendrame.ldb4docker.myjlibbig.Parent {
    @Override
    Collection<? extends Child> getChildren();

    EditableParent getEditable();
}
