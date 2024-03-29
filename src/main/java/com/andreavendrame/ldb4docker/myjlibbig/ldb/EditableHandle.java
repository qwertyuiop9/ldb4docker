package com.andreavendrame.ldb4docker.myjlibbig.ldb;

import com.andreavendrame.ldb4docker.myjlibbig.attachedProperties.Replicable;

import java.util.Collection;

public interface EditableHandle extends Handle, Replicable, EditableOwned {
    /**
     * Get the set of control's ports and innernames of an handle
     *
     * @return set of points
     */
    Collection<EditablePoint> getEditablePoints();

    /**
     * Add an innername or port to this handle.
     *
     * @param point innername or port that will be added
     */
    void linkPoint(EditablePoint point);

    /**
     * remove a point from this handle
     *
     * @param point innername or port that will be removed
     */
    void unlinkPoint(EditablePoint point);

    @Override
    EditableHandle replicate();
}
