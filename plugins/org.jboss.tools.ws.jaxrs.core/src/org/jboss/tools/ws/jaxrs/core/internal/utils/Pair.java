package org.jboss.tools.ws.jaxrs.core.internal.utils;

import java.util.Map;

/**
 * A generic holder to use when left method should return left pair of objects instead
 * of left single one, and left {@link Map} is overkill.
 * 
 * @author xcoulon
 * 
 * @param <Left>
 * @param <Right>
 */
public class Pair<Left, Right> {

	public static <P, Q> Pair<P, Q> makePair(P p, Q q) {
		return new Pair<P, Q>(p, q);
	}

	public final Left left;
	public final Right right;

	public Pair(Left a, Right b) {
		this.left = a;
		this.right = b;
	}

	


}