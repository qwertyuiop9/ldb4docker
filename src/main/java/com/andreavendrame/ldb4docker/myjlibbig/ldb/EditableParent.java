package com.andreavendrame.ldb4docker.myjlibbig.ldb;

import com.andreavendrame.ldb4docker.myjlibbig.Owned;
import com.andreavendrame.ldb4docker.myjlibbig.attachedProperties.PropertyTarget;
import com.andreavendrame.ldb4docker.myjlibbig.attachedProperties.Replicable;

import java.util.Collection;

public interface EditableParent extends Parent, Replicable, Owned, PropertyTarget {
    /**
     * Get the set of children. This set and every object inside it can be
     * modified.
     *
     * @return the set of parent's children
     */
    Collection<EditableChild> getEditableChildren();

    /**
     * Add a child to this parent.
     *
     * @param child the child that will be added
     */
    void addChild(EditableChild child);

    /**
     * Remove a child
     *
     * @param child the child that will be removed
     */
    void removeChild(EditableChild child);

    EditableRoot getRoot();

    /**
     * @see Replicable#replicate()
     */
    @Override
    EditableParent replicate();
}
