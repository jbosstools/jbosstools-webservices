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
package org.jboss.tools.ws.jaxrs.core.metamodel;

import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.IMethod;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.JavaMethodParameter;

public interface IJaxrsResourceMethod {

	abstract IMethod getJavaElement();

	abstract boolean hasErrors();

	/**
	 * Sets a flag of whether the underlying java method has compilation errors
	 * or not. If true, also marke the parent resource with errors flag.
	 * 
	 * @param h
	 *            : true if the java element has errors, false otherwise
	 */
	abstract void hasErrors(final boolean h);

	abstract EnumElementKind getElementKind();

	abstract String getPathTemplate();

	abstract String getHttpMethod();

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

}