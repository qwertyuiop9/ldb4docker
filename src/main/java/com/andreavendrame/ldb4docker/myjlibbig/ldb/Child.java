package com.andreavendrame.ldb4docker.myjlibbig.ldb;


/**
 * Place graph entities are organised in tree-like structures.
 *
 * @see Parent
 */
public interface Child extends PlaceEntity, com.andreavendrame.ldb4docker.myjlibbig.Child {
    @Override
    public abstract Parent getParent();

    public abstract EditableChild getEditable();
}
