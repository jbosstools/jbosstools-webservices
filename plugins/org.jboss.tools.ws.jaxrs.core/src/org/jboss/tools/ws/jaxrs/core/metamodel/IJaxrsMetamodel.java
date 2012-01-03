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

import org.eclipse.jdt.core.IJavaElement;

public interface IJaxrsMetamodel {

	/** @return the JAX-RS Ednpoints */
	public abstract List<IJaxrsEndpoint> getAllEndpoints();

	public abstract <T> T getElement(IJavaElement invocationElement, Class<T> clazz);

}