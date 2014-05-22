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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionUtils;
import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionUtils.MapComparison;

/**
 * Wrapper for a method parameter, exposing its name, type (fully qualified
 * name) and annotations.
 * 
 * @author Xavier Coulon
 *
 */
public class JavaMethodParameter {

	/** The name the parameter name. */
	private final String name;

	/** The parameter type.*/
	private final ParameterType type;
	
	/** Parameter annotations, indexed by their fully qualified name. */
	private final Map<String, Annotation> annotations;
	
	/**
	 * Full constructor.
	 * 
	 * @param name
	 *            the parameter name
	 * @param erasureName
	 *            the qualified name of the parameter's erasure
	 * @param typeArgumentNames
	 *            the qualified names of the parameter's type argument (or empty
	 *            list)
	 * @param annotations
	 *            the parameter's relevant annotations
	 */
	public JavaMethodParameter(final String name, final ParameterType type, final List<Annotation> annotations) {
		this.name = name;
		this.type = type;
		this.annotations = new HashMap<String, Annotation>(annotations.size() * 2);
		for (Annotation annotation : annotations) {
			this.annotations.put(annotation.getFullyQualifiedName(), annotation);
		}
	}

	/** @return the parameter name */
	public String getName() {
		return this.name;
	}

	/** @return the parameter type */
	public ParameterType getType() {
		return type;
	}
	
	/**
	 * @return all annotations.
	 */
	public Map<String, Annotation> getAnnotations() {
		return annotations;
	}

	/**
	 * Return the annotation whose name matches the given fully qualified name
	 * 
	 * @param fullyQualifiedName
	 *            the fully qualified name of the annotation to look-up
	 * @return the annotation or null if this method parameter has no such
	 *         annotation.
	 */
	public Annotation getAnnotation(String fullyQualifiedName) {
		return annotations.get(fullyQualifiedName);
	}

	/**
	 * @return {@code true} if this {@link JavaMethodParameter} has an
	 *         annotation with the given fully qualified name, {@code false}
	 *         otherwise
	 * @param fullyQualifiedName
	 *            the fully qualified name of the annotation to look-up
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
	 *         {@link Annotation} level. This method is just form comparison, it
	 *         does not update the values.
	 * 
	 * @param otherMethodParameter
	 */
	public boolean hasChanges(final JavaMethodParameter otherMethodParameter) {
		final Map<String, Annotation> otherAnnotations = otherMethodParameter.getAnnotations();
		final MapComparison<String, Annotation> comparison = CollectionUtils
				.compare(this.annotations, otherAnnotations);
		return comparison.hasDifferences();
	}

	@Override
	public String toString() {
		return "ResourceMethodAnnotatedParameter [type=" + getType().getQualifiedName() + ", annotations=" + annotations + "]";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((annotations == null) ? 0 : annotations.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	/* (non-Javadoc)
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
		if (annotations == null) {
			if (other.annotations != null) {
				return false;
			}
		} else if (!annotations.equals(other.annotations)) {
			return false;
		}
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		if (type == null) {
			if (other.type != null) {
				return false;
			}
		} else if (!type.equals(other.type)) {
			return false;
		}
		return true;
	}

	

}
