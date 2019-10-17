package com.andreavendrame.ldb4docker.myjlibbig.ldb;

import com.andreavendrame.ldb4docker.myjlibbig.Named;

interface EditableNamed extends Named {
    /**
     * Set the entity's name
     *
     * @param name entity's new name
     */
    void setName(String name);
}
