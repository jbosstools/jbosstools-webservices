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

import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;

public interface IJaxrsHttpMethod extends IJaxrsElement, Comparable<IJaxrsHttpMethod> {

	/** @return the httpVerb */
	String getHttpVerb();

	/** @return the name */
	String getFullyQualifiedName();

	Annotation getHttpMethodAnnotation();

	IType getJavaElement();

}