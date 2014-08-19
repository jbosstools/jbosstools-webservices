/******************************************************************************* 
 * Copyright (c) 2011 - 2014 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/

package org.jboss.tools.ws.jaxrs.core.metamodel.domain;

import org.eclipse.jdt.core.IJavaElement;

/**
 * @author xcoulon
 *
 */
public interface IJaxrsJavaElement extends IJaxrsElement, IAnnotatedElement {

	/**
	 * @return the underlying {@link IJaxrsElement} for this JAX-RS Element.
	 */
	IJavaElement getJavaElement();
}
