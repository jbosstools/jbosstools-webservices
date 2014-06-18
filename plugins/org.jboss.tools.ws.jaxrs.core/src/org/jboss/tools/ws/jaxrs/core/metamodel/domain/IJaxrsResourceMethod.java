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
package org.jboss.tools.ws.jaxrs.core.metamodel.domain;

import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.IMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;

public interface IJaxrsResourceMethod extends IJaxrsElement {

	abstract IMethod getJavaElement();

	abstract String getPathTemplate();

	abstract String getHttpMethodClassName();

	abstract List<String> getConsumedMediaTypes();

	abstract List<String> getProducedMediaTypes();

	abstract List<IJavaMethodParameter> getJavaMethodParameters();

	/**
	 * Returns all current values for the {@code @Path} annotations on {@code this} {@link JaxrsResourceMethod} and on the parent {@link JaxrsResource} 
	 * This result can be used to determine the proposals for the PathParam annotated method parameters of
	 * the underlying Java Method. 
	 * 
	 * @return A {@link Map} containing the {@code @Path} annotations
	 * found on {@code this} {@link JaxrsResourceMethod} and on the parent {@link JaxrsResource}, indexed by their extracted value (ie, with no regexp)
	 */
	abstract Map<String, Annotation> getPathTemplateParameters();

	abstract boolean hasPathTemplate();

	abstract IJaxrsResource getParentResource();
	
	abstract Map<String, Annotation> getNameBindingAnnotations();

}