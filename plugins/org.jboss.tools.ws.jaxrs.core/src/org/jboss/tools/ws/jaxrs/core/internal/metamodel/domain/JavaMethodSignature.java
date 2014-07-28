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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.jdt.core.IMethod;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.SourceType;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJavaMethodParameter;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJavaMethodSignature;

public class JavaMethodSignature implements IJavaMethodSignature {

	/** Underlying java method. */
	private final IMethod javaMethod;
	/** Java method return type.*/
	private final SourceType returnedType;
	/** Method parameters.*/
	private final List<IJavaMethodParameter> methodParameters;

	public JavaMethodSignature(final IMethod javaMethod, final SourceType returnedType, final List<IJavaMethodParameter> methodParameters) {
		this.javaMethod = javaMethod;
		this.returnedType = returnedType;
		this.methodParameters = new ArrayList<IJavaMethodParameter>(methodParameters);
	}
	
	public JavaMethodSignature createWorkingcopy() {
		synchronized (this) {
			return new JavaMethodSignature(javaMethod, returnedType, new ArrayList<IJavaMethodParameter>(methodParameters));
		}
	}

	/**
	 * @see org.jboss.tools.ws.jaxrs.core.utils.IJavaMethodSignature#getJavaMethod()
	 */
	@Override
	public IMethod getJavaMethod() {
		return javaMethod;
	}

	/**
	 * @see org.jboss.tools.ws.jaxrs.core.utils.IJavaMethodSignature#getReturnedType()
	 */
	@Override
	public SourceType getReturnedType() {
		return returnedType;
	}

	/**
	 * @see org.jboss.tools.ws.jaxrs.core.utils.IJavaMethodSignature#getMethodParameters()
	 */
	@Override
	public List<IJavaMethodParameter> getMethodParameters() {
		return Collections.unmodifiableList(methodParameters);
	}

	/**
	 * @see org.jboss.tools.ws.jaxrs.core.utils.IJavaMethodSignature#getMethodParameter(java.lang.String)
	 */
	@Override
	public IJavaMethodParameter getMethodParameter(String name) {
		for(IJavaMethodParameter parameter : methodParameters) {
			if(parameter.getName().equals(name)) {
				return parameter;
			}
		}
		return null;
	}


	@Override
	public String toString() {
		StringBuilder stb = new StringBuilder("JavaMethodSignature ");
		if (returnedType != null) {
			stb.append(returnedType.getDisplayableTypeName()).append(" ");
		} else {
			stb.append("void ");
		}
		stb.append(javaMethod.getElementName()).append("(");
		for (Iterator<IJavaMethodParameter> paramIterator = methodParameters.iterator(); paramIterator.hasNext();) {
			final IJavaMethodParameter methodParam = paramIterator.next();
			for (Entry<String, Annotation> entry : methodParam.getAnnotations().entrySet()) {
				stb.append(entry.getValue()).append(" ");
			}
			if(methodParam.getType() != null) {
				stb.append(methodParam.getType().getDisplayableTypeName());
			} else {
				stb.append("unknown");
			}
			if (paramIterator.hasNext()) {
				stb.append(", ");
			}
		}
		stb.append(")");
		return stb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((javaMethod == null) ? 0 : javaMethod.hashCode());
		result = prime * result + ((methodParameters == null) ? 0 : methodParameters.hashCode());
		result = prime * result + ((returnedType == null) ? 0 : returnedType.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		JavaMethodSignature other = (JavaMethodSignature) obj;
		if (javaMethod == null) {
			if (other.javaMethod != null) {
				return false;
			}
		} else if (!javaMethod.equals(other.javaMethod)) {
			return false;
		}
		if (methodParameters == null) {
			if (other.methodParameters != null) {
				return false;
			}
		} else if (!methodParameters.equals(other.methodParameters)) {
			return false;
		}

		if (returnedType == null) {
			if (other.returnedType != null)
				return false;
		} else if (!returnedType.equals(other.returnedType))
			return false;
		return true;
	}

}