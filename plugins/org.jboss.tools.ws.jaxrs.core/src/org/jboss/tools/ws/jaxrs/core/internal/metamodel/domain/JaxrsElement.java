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

import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementChangedEvent.F_APPLICATION_PATH_VALUE;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementChangedEvent.F_CONSUMED_MEDIATYPES_VALUE;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementChangedEvent.F_DEFAULT_VALUE_VALUE;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementChangedEvent.F_ELEMENT_KIND;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementChangedEvent.F_HTTP_METHOD_VALUE;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementChangedEvent.F_MATRIX_PARAM_VALUE;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementChangedEvent.F_NONE;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementChangedEvent.F_PATH_PARAM_VALUE;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementChangedEvent.F_PATH_VALUE;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementChangedEvent.F_PRODUCED_MEDIATYPES_VALUE;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementChangedEvent.F_QUERY_PARAM_VALUE;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.wst.validation.ValidatorMessage;
import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionUtils;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementKind;
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
public abstract class JaxrsElement<T extends IMember> {

	/** The associated metamodel. */
	private final JaxrsMetamodel metamodel;

	/** The underlying java element. */
	private final T javaElement;

	/** Indicates if the underlying java element has compiltation errors. */
	private boolean hasErrors;

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
	public JaxrsElement(final T element, final Annotation annotation, final JaxrsMetamodel metamodel) {
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
	public JaxrsElement(final T element, final List<Annotation> annotations, final JaxrsMetamodel metamodel) {
		this.metamodel = metamodel;
		this.javaElement = element;
		if (annotations != null) {
			for (Annotation annotation : annotations) {
				this.annotations.put(annotation.getName(), annotation);
			}
		}
	}

	public abstract EnumElementKind getElementKind();

	public abstract EnumKind getKind();

	Annotation getAnnotation(String className) {
		return annotations.get(className);
	}

	/** @return the underlying java element */
	public final T getJavaElement() {
		return javaElement;
	}

	/**
	 * Sets a flag of whether the underlying java element has compilation errors
	 * or not.
	 * 
	 * @param h
	 *            : true if the java element has errors, false otherwise
	 */
	public void hasErrors(final boolean h) {
		this.hasErrors = h;
	}

	/** @return true if the java element has errors, false otherwise. */
	public final boolean hasErrors() {
		return hasErrors;
	}

	/**
	 * Returns the CompilationUnit (AST3/DOM) of the given java element.
	 * 
	 * @param element
	 *            the java element
	 * @param progressMonitor
	 *            the progress monitor
	 * @return the compilation unit or null
	 * @throws JavaModelException
	 *             in case of underlying exception
	 */
	final CompilationUnit getCompilationUnit(final IProgressMonitor progressMonitor) throws JavaModelException {
		return JdtUtils.parse(javaElement, progressMonitor);
	}

	/** @return the metamodel */
	public final JaxrsMetamodel getMetamodel() {
		return metamodel;
	}

	public Map<String, Annotation> getAnnotations() {
		return annotations;
	}

	IResource getResource() {
		return this.javaElement.getResource();
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
		if (annotationName.equals(Path.class.getName())) {
			flag = F_PATH_VALUE;
		} else if (annotationName.equals(ApplicationPath.class.getName())) {
			flag = F_APPLICATION_PATH_VALUE;
		} else if (annotationName.equals(HttpMethod.class.getName())) {
			flag = F_HTTP_METHOD_VALUE;
		} else if (annotationName.equals(PathParam.class.getName())) {
			flag = F_PATH_PARAM_VALUE;
		} else if (annotationName.equals(QueryParam.class.getName())) {
			flag = F_QUERY_PARAM_VALUE;
		} else if (annotationName.equals(MatrixParam.class.getName())) {
			flag = F_MATRIX_PARAM_VALUE;
		} else if (annotationName.equals(DefaultValue.class.getName())) {
			flag = F_DEFAULT_VALUE_VALUE;
		} else if (annotationName.equals(Consumes.class.getName())) {
			flag = F_CONSUMED_MEDIATYPES_VALUE;
		} else if (annotationName.equals(Produces.class.getName())) {
			flag = F_PRODUCED_MEDIATYPES_VALUE;
		} else {
			for (IJaxrsHttpMethod httpMethod : metamodel.getAllHttpMethods()) {
				if (httpMethod.getJavaElement().getFullyQualifiedName().equals(annotationName)) {
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
				if (annotationName.equals(Path.class.getName())) {
					flag = F_PATH_VALUE;
				}else if (annotationName.equals(ApplicationPath.class.getName())) {
					flag = F_APPLICATION_PATH_VALUE;
				} else if (annotationName.equals(HttpMethod.class.getName())) {
					flag = F_HTTP_METHOD_VALUE;
				} else if (annotationName.equals(PathParam.class.getName())) {
					flag = F_PATH_PARAM_VALUE;
				} else if (annotationName.equals(QueryParam.class.getName())) {
					flag = F_QUERY_PARAM_VALUE;
				} else if (annotationName.equals(MatrixParam.class.getName())) {
					flag = F_MATRIX_PARAM_VALUE;
				} else if (annotationName.equals(Consumes.class.getName())) {
					flag = F_CONSUMED_MEDIATYPES_VALUE;
				} else if (annotationName.equals(Produces.class.getName())) {
					flag = F_PRODUCED_MEDIATYPES_VALUE;
				} else {
					for (IJaxrsHttpMethod httpMethod : metamodel.getAllHttpMethods()) {
						if (httpMethod.getJavaElement().getFullyQualifiedName().equals(annotationName)) {
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
		JaxrsElement<?> other = (JaxrsElement<?>) obj;
		if (javaElement == null) {
			if (other.javaElement != null) {
				return false;
			}
		} else if (!javaElement.getHandleIdentifier().equals(other.javaElement.getHandleIdentifier())) {
			return false;
		}
		return true;
	}

	public abstract List<ValidatorMessage> validate();

}
