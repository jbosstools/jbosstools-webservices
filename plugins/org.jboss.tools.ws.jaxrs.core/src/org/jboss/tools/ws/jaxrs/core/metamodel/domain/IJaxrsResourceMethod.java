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
import org.jboss.tools.ws.jaxrs.core.utils.Annotation;
import org.jboss.tools.ws.jaxrs.core.utils.JavaMethodParameter;

public interface IJaxrsResourceMethod extends IJaxrsElement {

	abstract IMethod getJavaElement();

	abstract String getPathTemplate();

	abstract String getHttpMethodClassName();

	abstract List<String> getConsumedMediaTypes();

	abstract List<String> getProducedMediaTypes();

	abstract List<JavaMethodParameter> getJavaMethodParameters();

	/**
	 * Determines the proposals for the PathParam annotated method parameters of
	 * the underlying Java Method. This map contains the @Path annotations
	 * found on the Java Method and on the parent Java Type, indexed by their extracted value (ie, with no regexp)
	 * 
	 * @return
	 */
	abstract Map<String, Annotation> getPathParamValueProposals();

	abstract boolean hasPathTemplate();

	abstract IJaxrsResource getParentResource();
	
	abstract Map<String, Annotation> getNameBindingAnnotations();

}