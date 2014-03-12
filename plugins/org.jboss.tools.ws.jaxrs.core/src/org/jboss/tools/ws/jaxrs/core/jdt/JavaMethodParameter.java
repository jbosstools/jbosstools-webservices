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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionUtils;
import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionUtils.MapComparison;

/**
 * Wrapper for a method parameter, exposing its name, type (fully qualified name) and annotations.
 * @author Xavier Coulon
 *
 */
public class JavaMethodParameter {

	/** Parameter name (can change, it doesn't matter). */
	private String name;

	/** Parameter fully qualified type name. */
	private final String typeName;

	/** Parameter annotations, indexed by their fully qualified name. */
	private final Map<String, Annotation> annotations;

	/**
	 * Full constructor.
	 * @param name
	 * @param typeName
	 * @param annotations
	 */
	public JavaMethodParameter(final String name, final String typeName, final List<Annotation> annotations) {
		this.name = name;
		this.typeName = typeName;
		this.annotations = new HashMap<String, Annotation>(annotations.size() * 2);
		for (Annotation annotation : annotations) {
			this.annotations.put(annotation.getFullyQualifiedName(), annotation);
		}
	}

	/** @return the parameter name */
	public String getName() {
		return this.name;
	}
	
	/** @return the parameter fully qualified type name */
	public String getTypeName() {
		return this.typeName;
	}

	/** @return the parameter type name in a shorter/displayable form. */
	public String getDisplayableTypeName() {
		return JdtUtils.toDisplayableTypeName(typeName);
	}
	
	/**
	 * @return all annotations.
	 */
	public Map<String, Annotation> getAnnotations() {
		return annotations;
	}

	/**
	 * Return the annotation whose name matches the given fully qualified name
	 * @param fullyQualifiedName the fully qualified name of the annotation to look-up
	 * @return the annotation or null if this method parameter has no such annotation.
	 */
	public Annotation getAnnotation(String fullyQualifiedName) {
		return annotations.get(fullyQualifiedName);
	}
	
	/**
	 * @return {@code true} if this {@link JavaMethodParameter} has an annotation with the given fully qualified name, {@code false} otherwise
	 * @param fullyQualifiedName the fully qualified name of the annotation to look-up
	 */
	public boolean hasAnnotation(String fullyQualifiedName) {
		return getAnnotation(fullyQualifiedName) != null;
	}
	
	public void removeAnnotation(Annotation annotation) {
		annotations.remove(annotation.getFullyQualifiedName());
	}
	
	public void removeAnnotation(String qualifiedName) {
		annotations.remove(qualifiedName);
	}

	
	public void addAnnotation(Annotation annotation) {
		annotations.put(annotation.getFullyQualifiedName(), annotation);
	}

	
	/**
	 * @return true if this {@link JavaMethodParameter} is different from the
	 *         given {@link JavaMethodParameter}, including differences at the
	 *         {@link Annotation} level. This method is just form comparison, it does not update the values.
	 * 
	 * @param otherMethodParameter
	 */
	public boolean hasChanges(final JavaMethodParameter otherMethodParameter) {
		final Map<String, Annotation> otherAnnotations = otherMethodParameter.getAnnotations();
		final MapComparison<String, Annotation> comparison = CollectionUtils.compare(this.annotations, otherAnnotations);
		return comparison.hasDifferences();
	}
	
	@Override
	public String toString() {
		return "ResourceMethodAnnotatedParameter [type=" + typeName + ", annotations=" + annotations + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((typeName == null) ? 0 : typeName.hashCode());
		result = prime * result + ((annotations == null) ? 0 : annotations.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
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
		JavaMethodParameter other = (JavaMethodParameter) obj;
		if(!this.name.equals(other.name)) {
			return false;
		}
		if (typeName == null) {
			if (other.typeName != null) {
				return false;
			}
		} else if (!typeName.equals(other.typeName)) {
			return false;
		}
		if (annotations == null) {
			if (other.annotations != null) {
				return false;
			}
		} else if (!annotations.equals(other.annotations)) {
			return false;
		}
		return true;
	}

}
