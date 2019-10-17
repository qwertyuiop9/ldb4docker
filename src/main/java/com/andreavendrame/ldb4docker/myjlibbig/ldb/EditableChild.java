package com.andreavendrame.ldb4docker.myjlibbig.ldb;

import com.andreavendrame.ldb4docker.myjlibbig.Owned;
import com.andreavendrame.ldb4docker.myjlibbig.attachedProperties.Replicable;

interface EditableChild extends Child, Replicable, Owned {
    @Override
    EditableParent getParent();

    void setParent(EditableParent parent);

    @Override
    EditableChild replicate();
}
