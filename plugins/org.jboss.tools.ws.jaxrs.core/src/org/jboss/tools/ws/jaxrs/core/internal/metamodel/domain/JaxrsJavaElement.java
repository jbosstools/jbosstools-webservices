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

import static org.eclipse.jdt.core.IJavaElementDelta.CHANGED;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.APPLICATION_PATH;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.CONSUMES;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.DEFAULT_VALUE;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.ENCODED;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.HTTP_METHOD;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.MATRIX_PARAM;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.PATH;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.PATH_PARAM;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.PRODUCES;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.PROVIDER;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.QUERY_PARAM;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.RETENTION;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.TARGET;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_APPLICATION_PATH_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_CONSUMES_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_DEFAULT_VALUE_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_ELEMENT_KIND;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_ENCODED_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_HTTP_METHOD_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_MATRIX_PARAM_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_NONE;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_PATH_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_PATH_PARAM_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_PRODUCES_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_PROVIDER_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_QUERY_PARAM_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_RETENTION_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_TARGET_ANNOTATION;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.Flags;
import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionUtils;
import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionUtils.MapComparison;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta;

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
	 * Full constructor for element with multiple annotations.
	 * 
	 * @param element
	 *            the java element
	 * @param annotations
	 *            the java element annotations
	 * @param metamodel
	 *            the metamodel in which this element exist, or null if this
	 *            element is transient.
	 */
	public JaxrsJavaElement(final T element, final Map<String, Annotation> annotations, final JaxrsMetamodel metamodel) {
		super(metamodel);
		this.javaElement = element;
		if (annotations != null) {
			this.annotations.putAll(annotations);
		}
	}

	static Map<String, Annotation> singleToMap(final Annotation annotation) {
		if (annotation != null) {
			return CollectionUtils.toMap(annotation.getFullyQualifiedName(), annotation);
		}
		return Collections.emptyMap();
	}

	@Override
	public boolean isBinary() {
		if (this.javaElement == null) {
			return true;
		}
		return this.javaElement.isBinary();
	}

	/**
	 * @param className
	 *            the fully qualified name of the annotation to retrieve.
	 * @return the annotation matching the given java fully qualified name, null
	 *         otherwise.
	 */
	public Annotation getAnnotation(final String className) {
		return annotations.get(className);
	}

	/**
	 * @param className
	 *            the fully qualified name of the annotation to check.
	 * @return true if the given element has the annotation matching the given
	 *         java fully qualified name, false otherwise.
	 */
	public boolean hasAnnotation(final String className) {
		return annotations.get(className) != null;
	}

	/** @return the underlying java element */
	public final T getJavaElement() {
		return javaElement;
	}

	@Override
	public String getIdentifier() {
		return this.javaElement.getHandleIdentifier();
	}

	@Override
	public String getName() {
		return javaElement != null ? javaElement.getElementName() : "*unknown java element*";
	}

	/**
	 * @return the Map of {@link Annotation} for the given element, indexed by
	 *         their fully qualified java name.
	 */
	public Map<String, Annotation> getAnnotations() {
		return annotations;
	}

	/**
	 * Adds or Updates from the given annotation, <strong>without</strong>
	 * notifying the metamodel.
	 * 
	 * @param annotation
	 * @return
	 * @throws CoreException
	 */
	public void addOrUpdateAnnotation(final Annotation annotation) throws CoreException {
		if (annotation != null) {

			boolean changed = false;
			final EnumElementKind previousKind = getElementKind();
			final String annotationName = annotation.getFullyQualifiedName();
			if (annotations.containsKey(annotationName)) {
				changed = annotations.get(annotationName).update(annotation);
			} else {
				annotations.put(annotationName, annotation);
				changed = true;
			}
			if (changed) {
				final int flags = qualifyChange(annotationName, previousKind);
				final JaxrsElementDelta delta = new JaxrsElementDelta(this, CHANGED, flags);
				getMetamodel().update(delta);
			}
		}
	}

	/**
	 * Adds the given annotation.
	 * 
	 * @param annotation
	 * @throws CoreException
	 */
	public void addAnnotation(final Annotation annotation) throws CoreException {
		final JaxrsElementDelta delta = new JaxrsElementDelta(this, CHANGED);
		final int addAnnotationFlags = internalAddAnnotation(annotation);
		delta.addFlag(addAnnotationFlags);
		if (hasMetamodel()) {
			// update indexes for this element.
			getMetamodel().update(delta);
		}
	}

	/**
	 * Updates the given annotation.
	 * 
	 * @param annotation
	 * @return
	 * @throws CoreException
	 */
	public int updateAnnotation(final Annotation annotation) throws CoreException {
		final int flags = internalUpdateAnnotation(annotation);
		if (flags != F_NONE) {
			// update indexes for this element.
			final JaxrsElementDelta delta = new JaxrsElementDelta(this, CHANGED, flags);
			getMetamodel().update(delta);
		}
		return flags;
	}

	/**
	 * Adds or Updates from the given annotation, <strong>without</strong>
	 * notifying the metamodel.
	 * 
	 * @param annotation
	 * @return
	 */
	private int internalAddAnnotation(final Annotation annotation) {
		if (annotation == null) {
			return F_NONE;
		}
		final EnumElementKind previousKind = getElementKind();
		final String annotationName = annotation.getFullyQualifiedName();
		annotations.put(annotationName, annotation);
		final int flags = qualifyChange(annotationName, previousKind);
		// update indexes for this element.
		// getMetamodel().updateIndex(this, flags);
		return flags;
	}

	/**
	 * Adds or Updates from the given annotation, <strong>without</strong>
	 * notifying the metamodel.
	 * 
	 * @param annotation
	 * @return
	 */
	private int internalUpdateAnnotation(final Annotation annotation) {
		if (annotation == null || !hasAnnotation(annotation.getFullyQualifiedName())) {
			return F_NONE;
		}
		final EnumElementKind previousKind = getElementKind();
		final String annotationName = annotation.getFullyQualifiedName();
		if (annotations.get(annotationName).update(annotation)) {
			return qualifyChange(annotationName, previousKind);
		}
		return F_NONE;
	}

	Flags updateAnnotations(final Map<String, Annotation> otherAnnotations) {
		final Flags flags = new Flags();
		final MapComparison<String, Annotation> annotationsComparison = CollectionUtils.compare(this.annotations, otherAnnotations);
		// added annotations (ie: found in 'otherAnnotation' but not
		// this.annotations)
		final Map<String, Annotation> addedAnnotations = annotationsComparison.getAddedItems();
		// removed annotations (ie: found in this.annotations but not in
		// 'otherAnnotation')
		final Map<String, Annotation> removedAnnotations = annotationsComparison.getRemovedItems();
		// changed annotations 
		final Map<String, Annotation> changedAnnotations = annotationsComparison.getChangedItems();
		for (Entry<String, Annotation> entry : addedAnnotations.entrySet()) {
			flags.addFlags(internalAddAnnotation(entry.getValue()));
		}
		for (Entry<String, Annotation> entry : changedAnnotations.entrySet()) {
			flags.addFlags(internalUpdateAnnotation(entry.getValue()));
		}
		for (Entry<String, Annotation> entry : removedAnnotations.entrySet()) {
			flags.addFlags(internalRemoveAnnotation(entry.getValue().getJavaAnnotation()));
		}

		return flags;
	}

	private int qualifyChange(final String annotationName, EnumElementKind previousKind) {
		final EnumElementKind currentKind = getElementKind();
		int flag = computeChangeAnnotationFlag(annotationName);
		if (currentKind != previousKind) {
			flag += F_ELEMENT_KIND;
		}
		return flag;
	}
	
	/**
	 * Computes the flag associated with the given annotation name
	 * 
	 * @param annotationName
	 *            the annotation fully qualified name
	 * @return the flag, or {@link JaxrsElementDelta#F_NONE} if the given
	 *         annotation name is not relevant in the JAX-RS Metamodel
	 * @see {@link JaxrsElementDelta}
	 */
	private int computeChangeAnnotationFlag(final String annotationName) {
		if (annotationName.equals(PATH.qualifiedName)) {
			return F_PATH_ANNOTATION;
		} else if (annotationName.equals(APPLICATION_PATH.qualifiedName)) {
			return F_APPLICATION_PATH_ANNOTATION;
		} else if (annotationName.equals(HTTP_METHOD.qualifiedName)) {
			return F_HTTP_METHOD_ANNOTATION;
		} else if (annotationName.equals(TARGET.qualifiedName)) {
			return F_TARGET_ANNOTATION;
		} else if (annotationName.equals(RETENTION.qualifiedName)) {
			return F_RETENTION_ANNOTATION;
		} else if (annotationName.equals(PROVIDER.qualifiedName)) {
			return F_PROVIDER_ANNOTATION;
		} else if (annotationName.equals(PATH_PARAM.qualifiedName)) {
			return F_PATH_PARAM_ANNOTATION;
		} else if (annotationName.equals(QUERY_PARAM.qualifiedName)) {
			return F_QUERY_PARAM_ANNOTATION;
		} else if (annotationName.equals(MATRIX_PARAM.qualifiedName)) {
			return F_MATRIX_PARAM_ANNOTATION;
		} else if (annotationName.equals(DEFAULT_VALUE.qualifiedName)) {
			return F_DEFAULT_VALUE_ANNOTATION;
		} else if (annotationName.equals(ENCODED.qualifiedName)) {
			return F_ENCODED_ANNOTATION;
		} else if (annotationName.equals(CONSUMES.qualifiedName)) {
			return F_CONSUMES_ANNOTATION;
		} else if (annotationName.equals(PRODUCES.qualifiedName)) {
			return F_PRODUCES_ANNOTATION;
		} else {
			for (IJaxrsHttpMethod httpMethod : getMetamodel().findAllHttpMethods()) {
				if (httpMethod.getJavaClassName().equals(annotationName)) {
					return F_HTTP_METHOD_ANNOTATION;
				}
			}
		}
		return F_NONE;
	}

	/**
	 * Removes the given {@link IAnnotation} annotation and returns a flag to qualify the type of
	 * annotation that was removed. If the given annotation is null, the
	 * returned flag is {@link JaxrsElementDelta#F_NONE}
	 * 
	 * @param annotation
	 * @throws CoreException
	 */
	public void removeAnnotation(final IAnnotation annotation) throws CoreException {
		if (annotation != null) {
			final int flags = internalRemoveAnnotation(annotation);
			if (isMarkedForRemoval()) {
				remove();
			} else {
				final JaxrsElementDelta delta = new JaxrsElementDelta(this, CHANGED, flags);
				getMetamodel().update(delta);
			}
		}
	}

	/**
	 * Removes the annotation identified by the given parameter and returns a
	 * flag to qualify the type of annotation that was removed. If the given
	 * identifier is null, the returned flag is {@link JaxrsElementDelta#F_NONE}
	 * 
	 * @param annotation
	 * @return the change flag.
	 */
	private int internalRemoveAnnotation(final IAnnotation annotation) {
		int flags = F_NONE;
		String annotationName = null;
		if (annotation != null) {
			final EnumElementKind previousKind = getElementKind();
			for(Iterator<Entry<String, Annotation>> iterator = annotations.entrySet().iterator(); iterator.hasNext();) {
				final Entry<String, Annotation> entry = iterator.next();
				if(entry.getValue().getJavaAnnotation().getHandleIdentifier().equals(annotation.getHandleIdentifier())) {
					annotationName = entry.getKey();
					iterator.remove();
					break;
				}
			}
			// this removes the annotation, which can cause a change of the
			// element type as well.
			if(annotationName != null) {
				flags = qualifyChange(annotationName, previousKind);
			}
		}
		return flags;
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

	/**
	 * Updates the current {@link JaxrsJavaElement} from the given
	 * {@link IJavaElement}
	 * 
	 * @param javaElement
	 * @param ast
	 * @return
	 * @throws CoreException
	 */
	public abstract void update(final IJavaElement javaElement, final CompilationUnit ast) throws CoreException;

	public IJavaElement getJavaProject() {
		if (javaElement != null) {
			return javaElement.getJavaProject();
		}
		return null;
	}

}
