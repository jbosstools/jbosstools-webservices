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

package org.jboss.tools.ws.jaxrs.core.junitrules;

import org.eclipse.core.runtime.CoreException;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsJavaElement;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;

/**
 * @author xcoulon
 *
 */
public class JaxrsElementsUtils {

	/**
	 * Private constructor of the utility class
	 */
	private JaxrsElementsUtils() {
	}

	/**
	 * Replace the first occurrence of the given old content with the new
	 * content. Fails if the old content is not found (avoids weird side effects
	 * in the rest of the test).
	 * 
	 * @param element the JAX-RS Element to modify and update
	 * @param oldContent the content to remove
	 * @param newContent the content to use
	 * @param useWorkingCopy true or false..
	 * @throws CoreException 
	 */
	public static void replaceFirstOccurrenceOfCode(final JaxrsJavaElement<?> element, final String oldContent,
			final String newContent, final boolean useWorkingCopy) throws CoreException {
		JavaElementsUtils.replaceFirstOccurrenceOfCode(element.getJavaElement().getCompilationUnit(), oldContent, newContent, useWorkingCopy);
		if(!useWorkingCopy) {
			element.update(element.getJavaElement(), JdtUtils.parse(element.getJavaElement().getCompilationUnit(), null));
		}
	}
}
