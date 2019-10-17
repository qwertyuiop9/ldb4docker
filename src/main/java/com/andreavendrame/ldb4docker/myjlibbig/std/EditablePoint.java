package com.andreavendrame.ldb4docker.myjlibbig.std;


interface EditablePoint extends Point {
	/**
	 * Set point's handle.
	 * 
	 * @param handle
	 *            point's new handle
	 */
	public abstract void setHandle(EditableHandle handle);

	@Override
	public abstract EditableHandle getHandle();
}