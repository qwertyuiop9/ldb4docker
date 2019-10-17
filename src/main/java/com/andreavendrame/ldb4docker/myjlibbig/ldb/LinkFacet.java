package com.andreavendrame.ldb4docker.myjlibbig.ldb;

import com.andreavendrame.ldb4docker.myjlibbig.Named;

/**
 * Link facets are the components of link graph inner and outer interfaces and
 * are names specialised in inner and outer ones (cf. {@link InnerName} and
 * {@link OuterName} respectively). Comparison is based on the name facets.
 *
 * @see Named
 */
public interface LinkFacet extends LinkEntity,
        com.andreavendrame.ldb4docker.myjlibbig.LinkFacet {

    public abstract EditableLinkFacet getEditable();
}
