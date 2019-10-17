package com.andreavendrame.ldb4docker.myjlibbig;

/**
 * Interface for mutable bigraphs.
 * 
 * @see BigraphHandler
 */
public interface BigraphBuilder<C extends Control> extends BigraphHandler<C> {

	public abstract Bigraph<C> makeBigraph();
}
