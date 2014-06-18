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

import org.eclipse.jdt.core.IMethod;
import org.jboss.tools.ws.jaxrs.core.jdt.SourceType;

/**
 * 
 * @author xcoulon
 *
 */
public interface IJavaMethodSignature {

	/** @return the method */
	public abstract IMethod getJavaMethod();

	/**
	 * @return the java method return type.
	 */
	public abstract SourceType getReturnedType();

	/**
	 * The java method parameters.
	 * @return
	 */
	public abstract List<IJavaMethodParameter> getMethodParameters();

	/**
	 * Returns the {@link IJavaMethodParameter} whose name is the given name  
	 * @param name
	 * @return
	 */
	public abstract IJavaMethodParameter getMethodParameter(String name);

}