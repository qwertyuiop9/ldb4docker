package com.andreavendrame.ldb4docker.myjlibbig.std;

import com.andreavendrame.ldb4docker.myjlibbig.Owned;
import com.andreavendrame.ldb4docker.myjlibbig.attachedProperties.Replicable;

interface EditableChild extends Child, Replicable, Owned {
	void setParent(EditableParent parent);

	@Override
	public abstract EditableParent getParent();

	@Override
	public abstract EditableChild replicate();
}
