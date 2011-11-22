package org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder;

import org.eclipse.jdt.core.IJavaElementDelta;

public interface IJavaElementDeltaFlag extends IJavaElementDelta {

	public static final int F_MARKER_ADDED = 0x800000;

	public static final int F_MARKER_REMOVED = 0x1000000;

	public static final int F_SIGNATURE = 0x2000000;

	public static final int F_PROBLEM_SOLVED = 0x8000000;

}
