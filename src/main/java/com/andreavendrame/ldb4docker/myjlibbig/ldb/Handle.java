package com.andreavendrame.ldb4docker.myjlibbig.ldb;

import com.andreavendrame.ldb4docker.myjlibbig.Owned;

import java.util.Collection;

/**
 * Handles are link entities identifying the hyper-edges that compose link
 * graphs and that link points. Handles are outer names or edges depending on
 * whereas they belong to an outer interface or not.
 */
public interface Handle extends Owned, LinkEntity,
        com.andreavendrame.ldb4docker.myjlibbig.Handle {

    @Override
    Collection<? extends Point> getPoints();

    EditableHandle getEditable();
}
