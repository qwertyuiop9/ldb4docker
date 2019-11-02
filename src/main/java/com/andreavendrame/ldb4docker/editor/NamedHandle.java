package com.andreavendrame.ldb4docker.editor;

import com.andreavendrame.ldb4docker.myjlibbig.ldb.Handle;

import java.util.LinkedList;
import java.util.List;

public class NamedHandle {

    private Handle handle;
    private String handleName;

    NamedHandle(Handle handle, String handleName) {
        this.handle = handle;
        this.handleName = handleName;
    }

    public Handle getHandle() {
        return handle;
    }

    public String getHandleName() {
        return handleName;
    }

    public static List<Handle> getHandleList(List<NamedHandle> namedHandles) {

        List<Handle> handles = new LinkedList<>();
        for (NamedHandle namedHandle : namedHandles) {
            handles.add(namedHandle.getHandle());
        }

        return handles;
    }
}
