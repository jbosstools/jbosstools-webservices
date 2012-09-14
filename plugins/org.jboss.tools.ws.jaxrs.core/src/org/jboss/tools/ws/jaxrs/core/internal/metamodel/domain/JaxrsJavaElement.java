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

import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementDelta.F_APPLICATION_PATH_VALUE;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementDelta.F_CONSUMED_MEDIATYPES_VALUE;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementDelta.F_DEFAULT_VALUE_VALUE;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementDelta.F_ELEMENT_KIND;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementDelta.F_HTTP_METHOD_VALUE;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementDelta.F_MATRIX_PARAM_VALUE;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementDelta.F_NONE;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementDelta.F_PATH_PARAM_VALUE;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementDelta.F_PATH_VALUE;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementDelta.F_PRODUCED_MEDIATYPES_VALUE;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementDelta.F_QUERY_PARAM_VALUE;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsElements.APPLICATION_PATH;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsElements.CONSUMES;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsElements.DEFAULT_VALUE;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsElements.HTTP_METHOD;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsElements.MATRIX_PARAM;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsElements.PATH;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsElements.PATH_PARAM;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsElements.PRODUCES;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsElements.QUERY_PARAM;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IMember;
import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionUtils;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsHttpMethod;

/**
 * Base class for all elements in the JAX-RS Metamodel.
 * 
 * @author xcoulon
 * 
 * @param <T>
 *            the underlying Java type managed by the JAX-RS ElementKind.
 */
public abstract class JaxrsJavaElement<T extends IMember> extends JaxrsBaseElement {

	/** The underlying java element. */
	private final T javaElement;

	/**
	 * Map of Annotations on the associated Java Element, indexed by the
	 * annotation class name.
	 */
	private final Map<String, Annotation> annotations = new HashMap<String, Annotation>();

	/**
	 * Full constructor.
	 * 
	 * @param model
	 *            the associated metamodel
	 * @param element
	 *            the underlying java element
	 */
	public JaxrsJavaElement(final T element, final Annotation annotation, final JaxrsMetamodel metamodel) {
		this(element, Arrays.asList(annotation), metamodel);
	}

	/**
	 * Full constructor.
	 * 
	 * @param model
	 *            the associated metamodel
	 * @param element
	 *            the underlying java element
	 **/
	public JaxrsJavaElement(final T element, final List<Annotation> annotations, final JaxrsMetamodel metamodel) {
		super(metamodel);
		this.javaElement = element;
		if (annotations != null) {
			for (Annotation annotation : annotations) {
				this.annotations.put(annotation.getName(), annotation);
			}
		}
	}

	Annotation getAnnotation(String className) {
		return annotations.get(className);
	}

	/** @return the underlying java element */
	public final T getJavaElement() {
		return javaElement;
	}
	
	@Override
	public String getName() {
		return javaElement != null ? javaElement.getElementName() : "*unknown java element*";
	}

	public Map<String, Annotation> getAnnotations() {
		return annotations;
	}

	public int addOrUpdateAnnotation(final Annotation annotation) {
		if (annotation == null) {
			return 0;
		}
		boolean changed = false;
		final EnumKind previousKind = getKind();
		final String annotationName = annotation.getName();
		if (annotations.containsKey(annotation.getName())) {
			changed = annotations.get(annotation.getName()).update(annotation);
		} else {
			annotations.put(annotation.getName(), annotation);
			changed = true;
		}
		if (changed) {
			getMetamodel().indexElement(this, annotation);
			return qualifyChange(annotationName, previousKind);
		}
		return F_NONE;
	}

	public int mergeAnnotations(Map<String, Annotation> otherAnnotations) {
		int flags = 0;
		// keep values in the 'otherAnnotations' map
		final Map<String, Annotation> addedAnnotations = CollectionUtils.difference(otherAnnotations, this.annotations);
		// keep values in the 'this.annotations' map
		final Map<String, Annotation> removedAnnotations = CollectionUtils.difference(this.annotations,
				otherAnnotations);
		// keep values in the 'otherAnnotations' map
		final Map<String, Annotation> changedAnnotations = CollectionUtils.intersection(otherAnnotations,
				this.annotations);

		for (Entry<String, Annotation> entry : addedAnnotations.entrySet()) {
			flags += this.addOrUpdateAnnotation(entry.getValue());
		}
		for (Entry<String, Annotation> entry : changedAnnotations.entrySet()) {
			flags += this.addOrUpdateAnnotation(entry.getValue());
		}
		for (Entry<String, Annotation> entry : removedAnnotations.entrySet()) {
			flags += this.removeAnnotation(entry.getValue());
		}
		return flags;
	}

	private int qualifyChange(final String annotationName, EnumKind previousKind) {
		int flag = F_NONE;
		final EnumKind currentKind = getKind();
		if (annotationName.equals(PATH.qualifiedName)) {
			flag = F_PATH_VALUE;
		} else if (annotationName.equals(APPLICATION_PATH.qualifiedName)) {
			flag = F_APPLICATION_PATH_VALUE;
		} else if (annotationName.equals(HTTP_METHOD.qualifiedName)) {
			flag = F_HTTP_METHOD_VALUE;
		} else if (annotationName.equals(PATH_PARAM.qualifiedName)) {
			flag = F_PATH_PARAM_VALUE;
		} else if (annotationName.equals(QUERY_PARAM.qualifiedName)) {
			flag = F_QUERY_PARAM_VALUE;
		} else if (annotationName.equals(MATRIX_PARAM.qualifiedName)) {
			flag = F_MATRIX_PARAM_VALUE;
		} else if (annotationName.equals(DEFAULT_VALUE.qualifiedName)) {
			flag = F_DEFAULT_VALUE_VALUE;
		} else if (annotationName.equals(CONSUMES.qualifiedName)) {
			flag = F_CONSUMED_MEDIATYPES_VALUE;
		} else if (annotationName.equals(PRODUCES.qualifiedName)) {
			flag = F_PRODUCED_MEDIATYPES_VALUE;
		} else {
			for (IJaxrsHttpMethod httpMethod : metamodel.getAllHttpMethods()) {
				if (httpMethod.getFullyQualifiedName().equals(annotationName)) {
					flag = F_HTTP_METHOD_VALUE;
					break;
				}
			}
		}

		if (currentKind != previousKind) {
			flag += F_ELEMENT_KIND;
		}
		return flag;
	}

	public int removeAnnotation(Annotation annotation) {
		return removeAnnotation(annotation.getJavaAnnotation().getHandleIdentifier());
	}

	public int removeAnnotation(final String handleIdentifier) {
		int flag = F_NONE;
		for (Iterator<Entry<String, Annotation>> iterator = annotations.entrySet().iterator(); iterator.hasNext();) {
			Entry<String, Annotation> entry = iterator.next();
			Annotation annotation = entry.getValue();
			if (annotation.getJavaAnnotation().getHandleIdentifier().equals(handleIdentifier)) {
				this.metamodel.unindexElement(this, handleIdentifier);
				final EnumKind previousKind = getKind();
				final String annotationName = entry.getKey();
				iterator.remove();
				if (annotationName.equals(PATH.qualifiedName)) {
					flag = F_PATH_VALUE;
				}else if (annotationName.equals(APPLICATION_PATH.qualifiedName)) {
					flag = F_APPLICATION_PATH_VALUE;
				} else if (annotationName.equals(HTTP_METHOD.qualifiedName)) {
					flag = F_HTTP_METHOD_VALUE;
				} else if (annotationName.equals(PATH_PARAM.qualifiedName)) {
					flag = F_PATH_PARAM_VALUE;
				} else if (annotationName.equals(QUERY_PARAM.qualifiedName)) {
					flag = F_QUERY_PARAM_VALUE;
				} else if (annotationName.equals(MATRIX_PARAM.qualifiedName)) {
					flag = F_MATRIX_PARAM_VALUE;
				} else if (annotationName.equals(CONSUMES.qualifiedName)) {
					flag = F_CONSUMED_MEDIATYPES_VALUE;
				} else if (annotationName.equals(PRODUCES.qualifiedName)) {
					flag = F_PRODUCED_MEDIATYPES_VALUE;
				} else {
					for (IJaxrsHttpMethod httpMethod : metamodel.getAllHttpMethods()) {
						if (httpMethod.getFullyQualifiedName().equals(annotationName)) {
							flag = F_HTTP_METHOD_VALUE;
							break;
						}
					}
				}
				final EnumKind currentKind = getKind();
				if (currentKind != previousKind) {
					flag += F_ELEMENT_KIND;
				}
				break;
			}
		}
		return flag;
	}
	
	public IResource getResource() {
		return this.javaElement.getResource();
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
		result = prime * result + ((javaElement == null) ? 0 : javaElement.getHandleIdentifier().hashCode());
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
		JaxrsJavaElement<?> other = (JaxrsJavaElement<?>) obj;
		if (javaElement == null) {
			if (other.javaElement != null) {
				return false;
			}
		} else if (!javaElement.getHandleIdentifier().equals(other.javaElement.getHandleIdentifier())) {
			return false;
		}
		return true;
	}

}
