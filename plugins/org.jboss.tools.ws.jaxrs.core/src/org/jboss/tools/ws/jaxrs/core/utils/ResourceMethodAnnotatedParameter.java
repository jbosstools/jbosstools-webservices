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

package org.jboss.tools.ws.jaxrs.core.utils;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.jboss.tools.ws.jaxrs.core.metamodel.Validable;

public class ResourceMethodAnnotatedParameter implements Validable, Comparable<ResourceMethodAnnotatedParameter> {

	private final String parameterType;

	private final String annotationType;

	private final String annotationValue;

	private final int charStart;

	private final int charEnd;

	private final int lineNumber;

	/**
	 * Full constructor
	 * 
	 * @param parent
	 * @param parameterName
	 * @param parameterType
	 * @param annotationType
	 * @param annotationValue
	 * @param charStart
	 * @param charEnd
	 * @param lineNumber
	 */
	public ResourceMethodAnnotatedParameter(String parameterType, String annotationType, String annotationValue,
			int charStart, int charEnd, int lineNumber) {
		super();
		this.parameterType = parameterType;
		this.annotationType = annotationType;
		this.annotationValue = annotationValue;
		this.charStart = charStart;
		this.charEnd = charEnd;
		this.lineNumber = lineNumber;
	}

	@Override
	public void validate(IProgressMonitor progressMonitor) throws CoreException {
		// TODO Auto-generated method stub
	}

	/**
	 * @return the parameterType
	 */
	public String getParameterType() {
		return parameterType;
	}

	/**
	 * @return the annotationType
	 */
	public String getAnnotationType() {
		return annotationType;
	}

	/**
	 * @return the annotationValue
	 */
	public String getAnnotationValue() {
		return annotationValue;
	}

	/**
	 * @return the charStart
	 */
	public int getCharStart() {
		return charStart;
	}

	/**
	 * @return the charEnd
	 */
	public int getCharEnd() {
		return charEnd;
	}

	/**
	 * @return the lineNumber
	 */
	public int getLineNumber() {
		return lineNumber;
	}

	/**
	 * Compares method parameters by their textual location {@inheritDoc}
	 * 
	 * @param otherParam
	 * @return
	 */
	@Override
	public int compareTo(ResourceMethodAnnotatedParameter otherParam) {
		return getCharStart() - otherParam.getCharStart();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ResourceMethodAnnotatedParameter [parameterType=" + parameterType + ", annotationType="
				+ annotationType + ", annotationValue=" + annotationValue + "]";
	}

}
