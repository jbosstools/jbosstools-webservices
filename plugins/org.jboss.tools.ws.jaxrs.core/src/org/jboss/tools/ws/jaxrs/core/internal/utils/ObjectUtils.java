/******************************************************************************* 
 * Copyright (c) 2008 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.ws.jaxrs.core.internal.utils;

public class ObjectUtils {

	private ObjectUtils() {

	}

	/**
	 * @param nextHTTPMethod
	 * @return
	 */
	public static boolean compare(Object o1, Object o2) {
		if ((o1 != null && !o1.equals(o2)) || (o2 != null && !o2.equals(o1))) {
			Logger.debug(" Value changed: " + o1 + " -> " + o2);
			return true;
		}
		return false;
	}
}
