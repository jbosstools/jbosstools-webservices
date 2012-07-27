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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;

public class Annotation {

	private final IAnnotation javaAnnotation;

	private final String javaAnnotationName;

	private final Map<String, List<String>> javaAnnotationElements;

	private ISourceRange sourceRange;

	/**
	 * Full constructor
	 * 
	 * @param annotation
	 * @param annotationName
	 * @param annotationElements
	 * @param sourceRange
	 */
	public Annotation(final IAnnotation annotation, final String annotationName, final Map<String, List<String>> annotationElements,
			final ISourceRange sourceRange) {
		this.javaAnnotation = annotation;
		this.javaAnnotationName = annotationName;
		this.javaAnnotationElements = new HashMap<String, List<String>>(annotationElements);
		this.sourceRange = sourceRange;
	}
	
	/**
	 * Full constructor with a single unnamed 'value'
	 * 
	 * @param annotation
	 * @param annotationName
	 * @param annotationValue
	 * @param sourceRange
	 */
	public Annotation(final IAnnotation annotation, final String annotationName, final String annotationValue,
			final ISourceRange sourceRange) {
		this.javaAnnotation = annotation;
		this.javaAnnotationName = annotationName;
		this.javaAnnotationElements = new HashMap<String, List<String>>();
		this.javaAnnotationElements.put("value", Arrays.asList(annotationValue));
		this.sourceRange = sourceRange;
	}

	public boolean update(Annotation annotation) {
		assert annotation != null;

		if (this.javaAnnotationElements.equals(annotation.getJavaAnnotationElements())) {
			return false;
		}
		this.javaAnnotationElements.clear();
		this.javaAnnotationElements.putAll(annotation.getJavaAnnotationElements());
		if (annotation.getSourceRange() != null) {
			this.sourceRange = annotation.getSourceRange();
		}
		return true;
	}

	public IAnnotation getJavaAnnotation() {
		return javaAnnotation;
	}

	public IJavaElement getJavaParent() {
		return javaAnnotation.getParent();
	}

	public String getName() {
		return javaAnnotationName;
	}

	public Map<String, List<String>> getJavaAnnotationElements() {
		return javaAnnotationElements;
	}

	public ISourceRange getSourceRange() {
		return sourceRange;
	}

	/** @return the value */
	public List<String> getValues(String elementName) {
		return javaAnnotationElements.get(elementName);
	}

	/** @return the value */
	public String getValue(String elementName) {
		final List<String> values = javaAnnotationElements.get(elementName);
		if (values != null) {
			assert !(values.size() > 1);
			if (values.size() == 1) {
				return values.get(0);
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Annotation [" + javaAnnotationName + " " + javaAnnotationElements + "]";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((javaAnnotation == null) ? 0 : javaAnnotation.getHandleIdentifier().hashCode());
		result = prime * result + ((javaAnnotationElements == null) ? 0 : javaAnnotationElements.hashCode());
		result = prime * result + ((javaAnnotationName == null) ? 0 : javaAnnotationName.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
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
		Annotation other = (Annotation) obj;
		if (javaAnnotationElements == null) {
			if (other.javaAnnotationElements != null) {
				return false;
			}
		} else if (!javaAnnotationElements.equals(other.javaAnnotationElements)) {
			return false;
		}
		if (javaAnnotationName == null) {
			if (other.javaAnnotationName != null) {
				return false;
			}
		} else if (!javaAnnotationName.equals(other.javaAnnotationName)) {
			return false;
		}
		return true;
	}

}
