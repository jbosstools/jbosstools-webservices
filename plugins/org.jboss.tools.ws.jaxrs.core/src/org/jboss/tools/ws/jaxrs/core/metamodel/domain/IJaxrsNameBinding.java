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

import org.eclipse.jdt.core.IType;

/**
 * A JAX-RS 2.0 Interceptor/Filter Name Binding Annotation.
 * <p>
 * "A filter or interceptor can be associated with a resource class or method by declaring a new binding annota- tion à la CDI. 
 * These annotations are declared using the JAX-RS meta-annotation @NameBinding and are used to decorate both the filter (or interceptor) 
 * and the resource method or resource class."
 * (JAX-RS 2.0 Spec, chap 6.)
 * </p>
 * 
 * @author xcoulon
 *
 */
public interface IJaxrsNameBinding extends IJaxrsElement {
	
	/**
	 * @return the fully qualified name of the underlying {@link IType} of this
	 *         JAX-RS element
	 */
	public String getJavaClassName();
}
