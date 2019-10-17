package com.andreavendrame.ldb4docker.myjlibbig.ldb;

interface EditablePoint extends Point {
    @Override
    EditableHandle getHandle();

    /**
     * Set point's handle.
     *
     * @param handle point's new handle
     */
    void setHandle(EditableHandle handle);
}