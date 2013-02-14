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

package org.jboss.tools.ws.jaxrs.core.jdt;

/**
 * Utility class to work on {@link Annotation}s
 * 
 * @author xcoulon
 * 
 */
public class AnnotationUtils {

	/**
	 * Utility static method to compare 2 annotations.
	 * 
	 * @param annotation
	 * @param otherAnnotation
	 * @return Return true if both are null or if both are non-null and have
	 *         equal values.
	 */
	public static boolean equals(final Annotation annotation, final Annotation otherAnnotation) {
		if ((annotation == null && otherAnnotation != null) || (annotation != null && otherAnnotation == null)) {
			return false;
		}
		if (annotation == null && otherAnnotation == null) {
			return true;
		}
		if ((annotation.getValue() == null && otherAnnotation.getValue() != null) || (annotation.getValue() != null && otherAnnotation.getValue() == null)) {
			return false;
		}
		if (annotation.getValue() == null && otherAnnotation.getValue() == null) {
			return true;
		}
		return annotation.getValue().equals(otherAnnotation.getValue());
	}

}
