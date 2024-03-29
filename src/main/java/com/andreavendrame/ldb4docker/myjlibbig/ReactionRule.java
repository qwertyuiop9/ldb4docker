package com.andreavendrame.ldb4docker.myjlibbig;

/**
 * A reaction rule describes how a state of a bigraphical reactive system 
 * (cf. {ReactiveSystem}) can
 * evolve i.e. it can transform the bigraphs representing these states.
 * 
 * @param <B>
 *            The kind of bigraphs the rules applies to.
 *            
 *@see RewritingRule
 */
public interface ReactionRule<B extends Bigraph<?>> {
	public Iterable<B> apply(B to);
}
