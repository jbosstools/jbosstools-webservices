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

import org.eclipse.jdt.core.IType;

public interface IJaxrsResource extends IJaxrsElement {

	public abstract IType getJavaElement();
	
	public abstract String getPathTemplate();

	public abstract boolean hasPathTemplate();

	public abstract List<String> getConsumedMediaTypes();

	public abstract List<String> getProducedMediaTypes();

	public abstract List<IJaxrsResourceMethod> getAllMethods();

	public abstract boolean isRootResource();

	public abstract boolean isSubresource();

}