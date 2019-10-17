package com.andreavendrame.ldb4docker.myjlibbig.std;

/**
 * Sites are leaves in the place graph structure and compose its inner interface.
 * Sites are identified within a bigraph by their index (position).
 * 
 * @see Root
 */
public interface Site extends Child, com.andreavendrame.ldb4docker.myjlibbig.Site {
	@Override
	public abstract EditableSite getEditable();
}
