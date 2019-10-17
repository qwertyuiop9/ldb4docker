package com.andreavendrame.ldb4docker.myjlibbig.std;

import com.andreavendrame.ldb4docker.myjlibbig.Owned;
import com.andreavendrame.ldb4docker.myjlibbig.Owner;

interface EditableOwned extends Owned {
	void setOwner(Owner value);
}
