package com.andreavendrame.ldb4docker.myjlibbig;

/**
 * 
 * @param <B> the kind of bigraphs the rule can be applied to.
 * 
 * @see RewritingRule
 */
public interface InstantiationRule<B extends Bigraph<?>> {
	Iterable<? extends B> instantiate(B parameters);
}
