package com.andreavendrame.ldb4docker.myjlibbig;

/**
 * Describes entities owned by some {@link Owner}.
 * For instance, nodes are owned by the bigraph containing them.
 */
public interface Owned {
	/**
	 * Gets the current owner of this object
	 * 
	 * @return the owner
	 */
	Owner getOwner();
}
