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
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain;

import java.util.Map;

import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.utils.Annotation;

/**
 * Base class for all JAX-RS Java-based elements in the Metamodel. This base
 * class provides with information about the type annotations, the super type
 * and the super interfaces that were declared in the user code.
 * 
 * @author xcoulon
 * 
 * @param <T>
 *            the underlying Java type managed by the JAX-RS ElementKind.
 */
public abstract class AbstractJaxrsJavaTypeElement extends AbstractJaxrsJavaElement<IType> {
	/**
	 * Full constructor for element with multiple annotations.
	 * 
	 * @param element
	 *            the java element
	 * @param annotations
	 *            the java element annotations (or null)
	 * @param metamodel
	 *            the metamodel in which this element exist, or null if this
	 *            element is transient.
	 */
	public AbstractJaxrsJavaTypeElement(final IType element, final Map<String, Annotation> annotations,
			final JaxrsMetamodel metamodel) {
		super(element, annotations, metamodel);
	}

	/**
	 * @return the fully qualified name of the underlying {@link IType} of this
	 *         JAX-RS element
	 */
	public String getJavaClassName() {
		return getJavaElement().getFullyQualifiedName();
	}

}
