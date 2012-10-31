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

import java.util.LinkedList;
import java.util.List;

import org.eclipse.jdt.core.IJavaProject;

public interface IJaxrsEndpoint extends Comparable<IJaxrsEndpoint> {

	/** @return the httpMethod */
	public abstract IJaxrsHttpMethod getHttpMethod();

	/** @return the uriPathTemplate */
	public abstract String getUriPathTemplate();

	/** @return the consumedMediaTypes */
	public abstract List<String> getConsumedMediaTypes();

	/** @return the producedMediaTypes */
	public abstract List<String> getProducedMediaTypes();

	public abstract LinkedList<IJaxrsResourceMethod> getResourceMethods();

	public abstract IJavaProject getJavaProject();

	int getProblemLevel();

}