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
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain;

import java.util.Map;

import org.eclipse.jdt.core.IMember;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.SourceType;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IAnnotatedSourceType;

/**
 * JAX-RS Parameter Aggregator Field.
 * 
 * @author xcoulon
 */
public abstract class JaxrsParameterAggregatorElement<T extends IMember> extends JaxrsJavaElement<T> implements IAnnotatedSourceType {

	
	/** The underlying element type. */
	private final SourceType elementType;

	/** The surrounding parent element. */
	private final JaxrsParameterAggregator parentParameterAggregator;

	/**
	 * 
	 * @param javaElement
	 * @param annotations
	 * @param metamodel
	 * @param sourceType
	 * @param parentParameterAggregator
	 * @param primaryCopy
	 *            the associated primary copy element, or {@code null} if this
	 *            instance is already the primary element
	 */
	JaxrsParameterAggregatorElement(final T javaElement, final Map<String, Annotation> annotations,
			final JaxrsMetamodel metamodel, final SourceType sourceType,
			final JaxrsParameterAggregator parentParameterAggregator, final JaxrsParameterAggregatorElement<T> primaryCopy) {
		super(javaElement, annotations, metamodel, primaryCopy);
		this.elementType = sourceType;
		this.parentParameterAggregator = parentParameterAggregator;
	}
	
	public JaxrsParameterAggregator getParentParameterAggregator() {
		return parentParameterAggregator;
	}

	public SourceType getType() {
		return this.elementType;
	}
	

}
