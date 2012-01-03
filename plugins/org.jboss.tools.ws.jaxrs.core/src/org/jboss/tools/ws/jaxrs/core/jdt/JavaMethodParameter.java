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

package org.jboss.tools.ws.jaxrs.core.jdt;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.IValidable;

public class JavaMethodParameter implements IValidable {

	private final String typeName;

	private final List<Annotation> annotations;

	public JavaMethodParameter(String name, String typeName, List<Annotation> annotations) {
		this.typeName = typeName;
		this.annotations = annotations;
	}

	@Override
	public void validate(IProgressMonitor progressMonitor) throws CoreException {
		// TODO Auto-generated method stub
	}

	/** @return the parameterType */
	public String getTypeName() {
		return this.typeName;
	}

	public List<Annotation> getAnnotations() {
		return annotations;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((annotations == null) ? 0 : annotations.hashCode());
		result = prime * result + ((typeName == null) ? 0 : typeName.hashCode());
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
		final JavaMethodParameter other = (JavaMethodParameter) obj;
		if (annotations == null) {
			if (other.annotations != null) {
				return false;
			}
		} else if (!annotations.equals(other.annotations)) {
			return false;
		}
		if (typeName == null) {
			if (other.typeName != null) {
				return false;
			}
		} else if (!typeName.equals(other.typeName)) {
			return false;
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ResourceMethodAnnotatedParameter [type=" + typeName + ", annotations=" + annotations + "]";
	}

	public Annotation getAnnotation(String name) {
		for (Annotation annotation : annotations) {
			if (annotation.getName().equals(name)) {
				return annotation;
			}
		}
		return null;
	}

}
