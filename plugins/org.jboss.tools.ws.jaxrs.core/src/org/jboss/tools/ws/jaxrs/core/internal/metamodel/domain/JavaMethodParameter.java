/******************************************************************************* 
 * Copyright (c) 2008 - 2014 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/

package org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionUtils;
import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionUtils.MapComparison;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.AnnotationUtils;
import org.jboss.tools.ws.jaxrs.core.jdt.SourceType;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJavaMethodParameter;

/**
 * Wrapper for a method parameter, exposing its name, type (fully qualified
 * name) and annotations.
 * 
 * @author Xavier Coulon
 *
 */
public class JavaMethodParameter implements IJavaMethodParameter {

	/** The name the parameter name. */
	private final String name;

	/** The parameter type.*/
	private final SourceType type;
	
	/** Parameter annotations, indexed by their fully qualified name. */
	private final Map<String, Annotation> annotations;
	
	/** the underlying resource. */
	private final IResource resource;
	
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
	public JavaMethodParameter(final String name, final SourceType type, final List<Annotation> annotations, final IResource resource) {
		this.name = name;
		this.type = type;
		this.resource = resource;
		this.annotations = new HashMap<String, Annotation>(annotations.size() * 2);
		for (Annotation annotation : annotations) {
			this.annotations.put(annotation.getFullyQualifiedName(), annotation);
		}
	}

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
	private JavaMethodParameter(final String name, final SourceType type, final Map<String, Annotation> annotations, final IResource resource) {
		this.name = name;
		this.type = type;
		this.resource = resource;
		this.annotations = annotations;
	}
	
	public JavaMethodParameter createWorkingCopy() {
		return new JavaMethodParameter(new String(name), type.createWorkingCopy(), AnnotationUtils.createWorkingCopies(getAnnotations()), resource);
		
	}
	
	/**
	 * @see org.jboss.tools.ws.jaxrs.core.utils.IJavaMethodParameter#getName()
	 */
	@Override
	public String getName() {
		return this.name;
	}

	/** @return the parameter type */
	public SourceType getType() {
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
	public boolean hasChanges(final IJavaMethodParameter otherMethodParameter) {
		final Map<String, Annotation> otherAnnotations = otherMethodParameter.getAnnotations();
		final MapComparison<String, Annotation> comparison = CollectionUtils
				.compare(this.annotations, otherAnnotations);
		return comparison.hasDifferences();
	}
	
	@Override
	public IResource getResource() {
		return this.resource;
	}

	@Override
	public String toString() {
		final StringBuilder builder =new StringBuilder();
		for(Annotation annotation : annotations.values()) {
			builder.append("@").append(annotation.getJavaAnnotation().getElementName());
			if(annotation.getValue() != null && !annotation.getValue().isEmpty()) {
				builder.append('(').append(annotation.getValue()).append(')');
			}
			builder.append(' ');
		}
		builder.append(getType().getDisplayableTypeName()).append(' ').append(getName());
		return builder.toString();
		
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
