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

	
	/** The underlying field type. */
	private final SourceType fieldType;

	/** The surrounding parent element. */
	private final JaxrsParameterAggregator parentParameterAggregator;

	/**
	 * Full constructor.
	 * 
	 * @param builder
	 *            the fluent builder.
	 */
	JaxrsParameterAggregatorElement(final T javaElement, final Map<String, Annotation> annotations, final JaxrsMetamodel metamodel, final SourceType sourceType, final JaxrsParameterAggregator parentParameterAggregator) {
		super(javaElement, annotations, metamodel);
		this.fieldType = sourceType;
		this.parentParameterAggregator = parentParameterAggregator;
	}
	
	public JaxrsParameterAggregator getParentParameterAggregator() {
		return parentParameterAggregator;
	}

	public SourceType getType() {
		return this.fieldType;
	}
	

}
