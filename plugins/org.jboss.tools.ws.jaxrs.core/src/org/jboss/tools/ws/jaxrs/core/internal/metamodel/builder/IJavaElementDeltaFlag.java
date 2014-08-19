/******************************************************************************* 
 * Copyright (c) 2008 - 2014 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/

package org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder;

import org.eclipse.jdt.core.IJavaElementDelta;

public interface IJavaElementDeltaFlag extends IJavaElementDelta {

	public static final int F_MARKER_ADDED = 0x800000;

	public static final int F_MARKER_REMOVED = 0x1000000;

	public static final int F_SIGNATURE = 0x2000000;

	public static final int F_PROBLEM_SOLVED = 0x8000000;

}
