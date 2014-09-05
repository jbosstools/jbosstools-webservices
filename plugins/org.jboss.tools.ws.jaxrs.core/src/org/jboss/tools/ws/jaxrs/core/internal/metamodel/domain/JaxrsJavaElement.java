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

import static org.eclipse.jdt.core.IJavaElementDelta.CHANGED;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_APPLICATION_PATH_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_CONSUMES_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_DEFAULT_VALUE_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_ELEMENT_KIND;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_ENCODED_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_HTTP_METHOD_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_MATRIX_PARAM_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_NAME_BINDING_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_PATH_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_PATH_PARAM_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_PRODUCES_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_PROVIDER_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_QUERY_PARAM_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_RETENTION_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_TARGET_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.APPLICATION_PATH;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.CONSUMES;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.DEFAULT_VALUE;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.ENCODED;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.HTTP_METHOD;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.MATRIX_PARAM;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.NAME_BINDING;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PATH;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PATH_PARAM;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PRODUCES;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PROVIDER;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.QUERY_PARAM;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.RETENTION;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.TARGET;

import java.util.Collection;
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
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionUtils;
import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionUtils.MapComparison;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.Flags;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsJavaElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsNameBinding;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta;

/**
 * Base class for all JAX-RS Java-based elements in the Metamodel. This base
 * class provides with information about the type annotations that were declared on the associated Java Member in the user code.
 * 
 * @author xcoulon
 * 
 * @param <T>
 *            the underlying Java type managed by the JAX-RS ElementKind.
 */
public abstract class JaxrsJavaElement<T extends IMember> extends JaxrsBaseElement implements IJaxrsJavaElement {

	/** The underlying java element. */
	protected final T javaElement;

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
	 *            the java element annotations (or null)
	 * @param metamodel
	 *            the metamodel in which this element exist, or null if this
	 *            element is transient.
	 * @param primaryCopy
	 * 			the associated primary copy or {@code null} if this instance is already the primary copy.
	 */
	JaxrsJavaElement(final T element, final Map<String, Annotation> annotations, final JaxrsMetamodel metamodel, final JaxrsJavaElement<?> primaryCopy) {
		super(metamodel, primaryCopy);
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
	 * @return {@code true} if the underlying {@link IJavaElement} is not
	 *         {@code null} and is an {@link IType}, {@code false} otherwise.
	 */
	public boolean isBasedOnJavaType() {
		return this.javaElement != null && this.javaElement.getElementType() == IJavaElement.TYPE;
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
	@Override
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
				final Flags flags = qualifyChange(annotationName, previousKind);
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
		final Flags annotationAdditionFlags = internalAddAnnotation(annotation);
		final JaxrsElementDelta delta = new JaxrsElementDelta(this, CHANGED, annotationAdditionFlags);
		if (hasMetamodel() && annotationAdditionFlags.hasValue()) {
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
	public Flags updateAnnotation(final Annotation annotation) throws CoreException {
		final Flags flags = internalUpdateAnnotation(annotation);
		if (flags.hasValue()) {
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
	private Flags internalAddAnnotation(final Annotation annotation) {
		if (annotation == null) {
			return Flags.NONE;
		}
		final EnumElementKind previousKind = getElementKind();
		final String annotationName = annotation.getFullyQualifiedName();
		annotations.put(annotationName, annotation);
		final Flags flags = qualifyChange(annotationName, previousKind);
		// update indexes for this element.
		// getMetamodel().updateIndex(this, flags);
		return flags;
	}

	/**
	 * Adds or Updates from the given annotation, <strong>without</strong>
	 * notifying the metamodel.
	 * 
	 * @param annotation
	 */
	private Flags internalUpdateAnnotation(final Annotation annotation) {
		if (annotation == null || !hasAnnotation(annotation.getFullyQualifiedName())) {
			return Flags.NONE;
		}
		final EnumElementKind previousKind = getElementKind();
		final String annotationName = annotation.getFullyQualifiedName();
		if (annotations.get(annotationName).update(annotation)) {
			return qualifyChange(annotationName, previousKind);
		}
		return Flags.NONE;
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
			flags.addFlags(internalRemoveAnnotation(entry.getValue()));
		}

		return flags;
	}

	private Flags qualifyChange(final String annotationName, EnumElementKind previousKind) {
		final EnumElementKind currentKind = getElementKind();
		final Flags flags = computeChangeAnnotationFlag(annotationName);
		if (currentKind != previousKind) {
			flags.addFlags(F_ELEMENT_KIND);
		}
		return flags;
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
	private Flags computeChangeAnnotationFlag(final String annotationName) {
		if (annotationName.equals(PATH)) {
			return new Flags(F_PATH_ANNOTATION);
		} else if (annotationName.equals(APPLICATION_PATH)) {
			return new Flags(F_APPLICATION_PATH_ANNOTATION);
		} else if (annotationName.equals(HTTP_METHOD)) {
			return new Flags(F_HTTP_METHOD_ANNOTATION);
		} else if (annotationName.equals(NAME_BINDING)) {
			return new Flags(F_NAME_BINDING_ANNOTATION);
		} else if (annotationName.equals(TARGET)) {
			return new Flags(F_TARGET_ANNOTATION);
		} else if (annotationName.equals(RETENTION)) {
			return new Flags(F_RETENTION_ANNOTATION);
		} else if (annotationName.equals(PROVIDER)) {
			return new Flags(F_PROVIDER_ANNOTATION);
		} else if (annotationName.equals(PATH_PARAM)) {
			return new Flags(F_PATH_PARAM_ANNOTATION);
		} else if (annotationName.equals(QUERY_PARAM)) {
			return new Flags(F_QUERY_PARAM_ANNOTATION);
		} else if (annotationName.equals(MATRIX_PARAM)) {
			return new Flags(F_MATRIX_PARAM_ANNOTATION);
		} else if (annotationName.equals(DEFAULT_VALUE)) {
			return new Flags(F_DEFAULT_VALUE_ANNOTATION);
		} else if (annotationName.equals(ENCODED)) {
			return new Flags(F_ENCODED_ANNOTATION);
		} else if (annotationName.equals(CONSUMES)) {
			return new Flags(F_CONSUMES_ANNOTATION);
		} else if (annotationName.equals(PRODUCES)) {
			return new Flags(F_PRODUCES_ANNOTATION);
		} else {
			for (IJaxrsHttpMethod httpMethod : getMetamodel().findAllHttpMethods()) {
				if (httpMethod.getJavaClassName().equals(annotationName)) {
					return new Flags(F_HTTP_METHOD_ANNOTATION);
				}
			}
			for (IJaxrsNameBinding nameBinding: getMetamodel().findAllNameBindings()) {
				if (nameBinding.getJavaClassName().equals(annotationName)) {
					return new Flags(F_NAME_BINDING_ANNOTATION);
				}
			}
		}
		return Flags.NONE;
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
			final Flags removalFlags = internalRemoveAnnotation(annotation);
			if (isMarkedForRemoval()) {
				remove(removalFlags);
			} else {
				final JaxrsElementDelta delta = new JaxrsElementDelta(this, CHANGED, removalFlags);
				getMetamodel().update(delta);
			}
		}
	}
	
	/**
	 * Removes the annotation identified by the given parameter and returns a
	 * flag to qualify the type of annotation that was removed. If the given
	 * identifier is null, the returned flag is {@link JaxrsElementDelta#F_NONE}
	 * 
	 * @param javaAnnotation
	 * @return the change flag.
	 */
	private Flags internalRemoveAnnotation(final IAnnotation javaAnnotation) {
		if (javaAnnotation != null) {
			for(Iterator<Entry<String, Annotation>> iterator = annotations.entrySet().iterator(); iterator.hasNext();) {
				final Entry<String, Annotation> entry = iterator.next();
				final Annotation annotation = entry.getValue();
				if(annotation.getJavaAnnotation().getHandleIdentifier().equals(javaAnnotation.getHandleIdentifier())) {
					return internalRemoveAnnotation(annotation);
				}
			}
		}
		return Flags.NONE;
	}
	
	/**
	 * Removes the given {@link Annotation} and returns a
	 * flag to qualify the type of annotation that was removed or {@link JaxrsElementDelta#F_NONE}
	 * 
	 * @param annotation the annotation to remove
	 * @return the change flag to qualify the type of change
	 */
	private Flags internalRemoveAnnotation(final Annotation annotation) {
		if (annotation != null) {
			final EnumElementKind previousKind = getElementKind();
			annotations.remove(annotation.getFullyQualifiedName());
			return qualifyChange(annotation.getFullyQualifiedName(), previousKind);
		}
		return Flags.NONE;
	}

	public IResource getResource() {
		return this.javaElement.getResource();
	}

	/**
	 * @return the list of Name Binding annotations indexed by their fully qualified name on the element (relevant for
	 *         {@link JaxrsResource}, {@link JaxrsResourceMethod} and
	 *         {@link JaxrsJavaApplication}).
	 */
	public Map<String, Annotation> getNameBindingAnnotations() {
		final Collection<Annotation> annotations = getAnnotations().values();
		final Map<String, Annotation> nameBindingAnnotations = new HashMap<String, Annotation>();
		for (Annotation annotation : annotations) {
			final String annotationClassName = annotation.getFullyQualifiedName();
			if (getMetamodel().findNameBinding(annotationClassName) != null) {
				nameBindingAnnotations.put(annotation.getFullyQualifiedName(), annotation);
			}
		}
		return nameBindingAnnotations;
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
	
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		if(hasMetamodel()) {
			builder.append(getElementKind().toString());
		} else {
			builder.append("TRANSIENT");
		}
		builder.append(" ").append(getJavaElement().getElementName()).append(" ");
		if(getAnnotations().isEmpty()) {
			builder.append(" (no annotation)");
		} else {
			for(Entry<String, Annotation> entry : getAnnotations().entrySet()) {
				final Annotation annotation = entry.getValue();
				builder.append(" ").append(annotation);
			}
		}
		return builder.toString();
	}


}
