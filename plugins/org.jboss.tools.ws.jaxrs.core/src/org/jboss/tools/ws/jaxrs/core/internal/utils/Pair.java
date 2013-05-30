package org.jboss.tools.ws.jaxrs.core.internal.utils;

public class Pair<A, B> {

	public static <P, Q> Pair<P, Q> makePair(P p, Q q) {
		return new Pair<P, Q>(p, q);
	}

	public final A a;
	public final B b;

	public Pair(A a, B b) {
		this.a = a;
		this.b = b;
	}

	


}