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
import static org.jboss.tools.ws.jaxrs.core.utils.Annotation.VALUE;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.CONSUMES;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PATH;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PRODUCES;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.Flags;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.search.JavaElementsSearcher;
import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionUtils;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResource;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResourceField;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta;
import org.jboss.tools.ws.jaxrs.core.utils.Annotation;
import org.jboss.tools.ws.jaxrs.core.utils.JavaMethodSignature;
import org.jboss.tools.ws.jaxrs.core.utils.JdtUtils;

/**
 * From the spec : A resource class is a Java class that uses JAX-RS annotations
 * to implement a corresponding Web resource. Resource classes are POJOs that
 * have at least one method annotated with @Path or a request method designator.
 * 
 * @author xcoulon
 */
public final class JaxrsResource extends AbstractJaxrsJavaTypeElement implements IJaxrsResource {

	/** The map of {@link JaxrsResourceField} indexed by the underlying java element identifier. */
	private final Map<String, JaxrsResourceField> resourceFields = new HashMap<String, JaxrsResourceField>();

	/** The map of {@link JaxrsResourceMethod} indexed by the underlying java element identifier. */
	private final Map<String, JaxrsResourceMethod> resourceMethods = new HashMap<String, JaxrsResourceMethod>();

	/**
	 * Builder initializer
	 * 
	 * @param javaElement
	 *            the underlying {@link IJavaElement} that on which this JAX-RS
	 *            Element will be built.
	 * @return the Builder
	 * @throws JavaModelException
	 */
	public static Builder from(final IJavaElement javaElement, final List<IJaxrsHttpMethod> httpMethods)
			throws JavaModelException {
		final CompilationUnit ast = JdtUtils.parse(javaElement, new NullProgressMonitor());
		switch (javaElement.getElementType()) {
		case IJavaElement.COMPILATION_UNIT:
			return new Builder(((ICompilationUnit) javaElement).findPrimaryType(), ast, httpMethods);
		case IJavaElement.TYPE:
			return new Builder((IType) javaElement, ast, httpMethods);
		}
		return null;
	}

	/**
	 * Builder initializer
	 * 
	 * @param javaElement
	 *            the underlying {@link IJavaElement} that on which this JAX-RS
	 *            Element will be built.
	 * @param ast
	 *            the associated AST
	 * @return the Builder
	 * @throws JavaModelException
	 */
	public static Builder from(final IJavaElement javaElement, final CompilationUnit ast,
			final List<IJaxrsHttpMethod> httpMethods) {
		switch (javaElement.getElementType()) {
		case IJavaElement.COMPILATION_UNIT:
			return new Builder(((ICompilationUnit) javaElement).findPrimaryType(), ast, httpMethods);
		case IJavaElement.TYPE:
			return new Builder((IType) javaElement, ast, httpMethods);
		}
		return null;
	}

	/**
	 * Internal Builder
	 * 
	 * @author xcoulon
	 * 
	 */
	public static final class Builder {

		private final IType javaType;
		private final CompilationUnit ast;
		private final List<IJaxrsHttpMethod> httpMethods;
		private Map<String, Annotation> annotations;
		private JaxrsMetamodel metamodel;

		private Builder(final IType javaType, final CompilationUnit ast, final List<IJaxrsHttpMethod> httpMethods) {
			this.javaType = javaType;
			this.ast = ast;
			this.httpMethods = httpMethods;
		}

		public Builder withMetamodel(final JaxrsMetamodel metamodel) {
			this.metamodel = metamodel;
			return this;
		}

		public JaxrsResource build() throws CoreException {
			final long start = System.currentTimeMillis();
			try {
				if (javaType == null || !javaType.exists()) {
					return null;
				}
				this.annotations = JdtUtils.resolveAllAnnotations(javaType, ast);
				// create the resource
				final JaxrsResource resource = new JaxrsResource(this);
				// find the available type fields
				for (IField javaField : javaType.getFields()) {
					JaxrsResourceField.from(javaField, ast).withParentResource(resource).withMetamodel(metamodel).build();
				}
				// retrieve all JavaMethodSignatures at once
				final Map<String, JavaMethodSignature> methodSignatures = JdtUtils.resolveMethodSignatures(javaType, ast);
				// find the resource methods, subresource methods and
				// subresource
				// locators of this resource:
				final List<IMethod> javaMethods = JavaElementsSearcher.findResourceMethods(javaType, this.httpMethods,
						new NullProgressMonitor());
				for (IMethod javaMethod : javaMethods) {
					JaxrsResourceMethod.from(javaMethod, ast, httpMethods).withParentResource(resource)
							.withJavaMethodSignature(methodSignatures.get(javaMethod.getHandleIdentifier())).withMetamodel(metamodel).build();
				}
				// well, sorry.. this is not a valid JAX-RS resource..
				if (resource.isSubresource() && resource.resourceFields.isEmpty() && resource.resourceMethods.isEmpty()) {
					return null;
				}
				// this operation is only performed if the resource is acceptable
				// (ie, not UNDEFINED)
				resource.joinMetamodel();
				return resource;
			} finally {
				final long end = System.currentTimeMillis();
				Logger.tracePerf("Built JAX-RS Resource in {}ms", (end - start));
			}

		}

	}

	/**
	 * Full constructor.
	 * 
	 * @param builder
	 *            the fluent builder.
	 */
	private JaxrsResource(final Builder builder) {
		super(builder.javaType, builder.annotations, builder.metamodel);
	}

	@Override
	public final boolean isRootResource() {
		return getPathAnnotation() != null;
	}

	@Override
	public boolean isSubresource() {
		return getPathAnnotation() == null;
	}

	/**
	 * Update this JAX-RS Resource from the given {@link IJavaElement} and its
	 * associated {@link CompilationUnit}.
	 * 
	 * @param javaElement
	 *            the underlying {@link IJavaElement}
	 * @param ast
	 *            the associated{@link CompilationUnit}
	 * 
	 * @return flags indicating the nature of the changes
	 * @throws CoreException
	 */
	@Override
	public void update(final IJavaElement javaElement, final CompilationUnit ast) throws CoreException {
		final JaxrsResource transientResource = from(javaElement, ast, getMetamodel().findAllHttpMethods()).build();
		if (transientResource == null) {
			remove();
			return;
		} 
		final Flags updateAnnotationsFlags = updateAnnotations(transientResource.getAnnotations());
		final JaxrsElementDelta delta = new JaxrsElementDelta(this, CHANGED, updateAnnotationsFlags);
		updateMethods(transientResource, ast);
		updateFields(transientResource, ast);

		if (isMarkedForRemoval()) {
			remove();
		}
		// update indexes for this element.
		else if (hasMetamodel()) {
			getMetamodel().update(delta);
		}
	}

	/**
	 * Updates the {@link IJaxrsResourceField}s of {@code this} from the ones provided by the given {@link JaxrsResource}
	 * @param transientResource the resource to analyze
	 * @param ast its associated AST.
	 * @throws CoreException
	 */
	private void updateFields(final JaxrsResource transientResource, final CompilationUnit ast) throws CoreException {
		final List<JaxrsResourceField> allTransientInstanceFields = transientResource.getAllFields();
		final List<JaxrsResourceField> allCurrentFields = this.getAllFields();
		final List<JaxrsResourceField> addedFields = CollectionUtils.difference(allTransientInstanceFields,
				allCurrentFields);
		for (IJaxrsResourceField addedField : addedFields) {
			// create the Resource Field by attaching it to the metamodel
			// and to this parent resource.
			JaxrsResourceField.from(addedField.getJavaElement(), ast).withMetamodel(getMetamodel())
					.withParentResource(this).build();
		}
		final Collection<JaxrsResourceField> changedFields = CollectionUtils.intersection(allCurrentFields,
				allTransientInstanceFields);
		for (JaxrsResourceField changedField : changedFields) {
			((JaxrsResourceField) changedField).update(transientResource.getFields().get(
					changedField.getIdentifier()));
		}
		final List<JaxrsResourceField> removedFields = CollectionUtils.difference(allCurrentFields,
				allTransientInstanceFields);
		for (JaxrsResourceField removedField : removedFields) {
			this.removeField(removedField);
		}
	}

	/**
	 * Updates the {@link IJaxrsResourceMethod}s of {@code this} from the ones provided by the given {@link JaxrsResource}
	 * @param transientResource the resource to analyze
	 * @param ast its associated AST.
	 * @throws CoreException
	 */
	private void updateMethods(final JaxrsResource transientResource, final CompilationUnit ast)
			throws CoreException {
		final List<IJaxrsResourceMethod> allTransientInstanceMethods = transientResource.getAllMethods();
		final List<IJaxrsResourceMethod> allCurrentMethods = this.getAllMethods();
		final List<IJaxrsResourceMethod> addedMethods = CollectionUtils.difference(
				allTransientInstanceMethods, allCurrentMethods);
		for (IJaxrsResourceMethod addedMethod : addedMethods) {
			// create the Resource Method by attaching it to the metamodel
			// and this parent resource.
			JaxrsResourceMethod.from(addedMethod.getJavaElement(), ast, getMetamodel().findAllHttpMethods())
					.withMetamodel(getMetamodel()).withParentResource(this).build();
		}
		final Collection<IJaxrsResourceMethod> changedMethods = CollectionUtils.intersection(allCurrentMethods,
				allTransientInstanceMethods);
		for (IJaxrsResourceMethod changedMethod : changedMethods) {
			((JaxrsResourceMethod) changedMethod).update(transientResource.getMethods().get(
					changedMethod.getIdentifier()));
		}
		final List<IJaxrsResourceMethod> removedMethods = CollectionUtils.difference(allCurrentMethods,
				allTransientInstanceMethods);
		for (IJaxrsResourceMethod removedMethod : removedMethods) {
			this.removeMethod(removedMethod);
		}
	}
	
	/**
	 * @return true if the current element has no <code>javax.ws.rs.Path</code>
	 *         annotation AND no JAX-RS Resource Method nor JAX-RS Resource
	 *         Field. <code>javax.ws.rs.Consumes</code>,
	 *         <code>javax.ws.rs.Produces</code> and other such annotations are
	 *         not sufficient to define a JAX-RS element per-se.
	 */
	@Override
	boolean isMarkedForRemoval() {
		final boolean hasPathAnnotation = hasAnnotation(PATH);
		// element should be removed if it has no @Path annotation and it has no
		// JAX-RS element
		return !(hasPathAnnotation || resourceMethods.size() > 0 || resourceFields.size() > 0);
	}

	@Override
	public final EnumElementKind getElementKind() {
		final Annotation pathAnnotation = getAnnotation(PATH);
		if (pathAnnotation != null) {
			return EnumElementKind.ROOT_RESOURCE;
		} else if (resourceMethods.size() > 0 || resourceFields.size() > 0) {
			return EnumElementKind.SUBRESOURCE;
		}
		return EnumElementKind.UNDEFINED_RESOURCE;
	}

	public final String getName() {
		return getJavaElement().getElementName();
	}

	@Override
	public String getPathTemplate() {
		final Annotation pathAnnotation = getAnnotation(PATH);
		if (pathAnnotation == null) {
			return null;
		}
		return pathAnnotation.getValue();
	}

		@Override
	public boolean hasPathTemplate() {
		final Annotation pathAnnotation = getPathAnnotation();
		return pathAnnotation != null && pathAnnotation.getValue() != null;
	}

	public Annotation getPathAnnotation() {
		return getAnnotation(PATH);
	}

	@Override
	public List<String> getConsumedMediaTypes() {
		final Annotation consumesAnnotation = getAnnotation(CONSUMES);
		if (consumesAnnotation != null) {
			return consumesAnnotation.getValues(VALUE);
		}
		return Collections.emptyList();
	}

	@Override
	public List<String> getProducedMediaTypes() {
		final Annotation producesAnnotation = getAnnotation(PRODUCES);
		if (producesAnnotation != null) {
			return producesAnnotation.getValues(VALUE);
		}
		return Collections.emptyList();
	}

	public Annotation getProducesAnnotation() {
		return getAnnotation(PRODUCES);
	}
	
	@Override
	public final List<IJaxrsResourceMethod> getAllMethods() {
		return Collections.unmodifiableList(new ArrayList<IJaxrsResourceMethod>(resourceMethods.values()));
	}
	
	/**
	 * 
	 * @param resourceMethod the resource method to look for
	 * @return {@code true} if this {@link JaxrsResource} contains the given {@link IJaxrsResourceMethod}, {@code false} otherwise. 
	 */
	public boolean hasMethod(final IJaxrsResourceMethod resourceMethod) {
		return resourceMethods.containsKey(resourceMethod.getJavaElement().getHandleIdentifier());
	}

	public final List<JaxrsResourceField> getAllFields() {
		return Collections.unmodifiableList(new ArrayList<JaxrsResourceField>(resourceFields.values()));
	}

	@Override
	public String toString() {
		return new StringBuffer().append("Resource '").append(getName()).append("' (root=").append(isRootResource())
				.append(") ").toString();
	}

	@SuppressWarnings("incomplete-switch")
	public void addElement(JaxrsResourceElement<?> element) {
		if (element != null) {
			switch (element.getElementKind().getCategory()) {
			case RESOURCE_FIELD:
				this.resourceFields.put(element.getJavaElement().getHandleIdentifier(), (JaxrsResourceField) element);
				return;
			case RESOURCE_METHOD:
				this.resourceMethods.put(element.getJavaElement().getHandleIdentifier(), (JaxrsResourceMethod) element);
				return;
			}
		}
	}

	public void removeField(final IJaxrsResourceField resourceField) throws CoreException {
		this.resourceFields.remove(resourceField.getJavaElement().getHandleIdentifier());
		((JaxrsResourceField) resourceField).remove();
	}

	/**
	 * @return the resource methods indexed by their associated java method
	 *         handleIdentifier
	 */
	public Map<String, JaxrsResourceMethod> getMethods() {
		return resourceMethods;
	}

	/**
	 * Removes the given {@link IJaxrsResourceMethod} from {@link JaxrsResource}
	 * and from the JAX-RS Metamodel as well..
	 * 
	 * @param method
	 *            the JAX-RS Resource Method to remove
	 * @throws CoreException
	 */
	public void removeMethod(final IJaxrsResourceMethod method) throws CoreException {
		this.resourceMethods.remove(method.getJavaElement().getHandleIdentifier());
		((JaxrsResourceMethod) method).remove();
	}

	/**
	 * @return the map of {@link JaxrsResourceField} indexed by the underlying
	 *         java element identifier.
	 */
	public Map<String, JaxrsResourceField> getFields() {
		return resourceFields;
	}

	/**
	 * Returns the JAX-RS Resource Field whose name matches the given fieldName
	 * 
	 * @param fieldName
	 * @return the JAX-RS Resource Field or null
	 */
	public JaxrsResourceField getField(final String fieldName) {
		for (IJaxrsResourceField field : resourceFields.values()) {
			if (field.getJavaElement().getElementName().equals(fieldName)) {
				return (JaxrsResourceField) field;
			}
		}
		return null;
	}

	/**
	 * Returns the JAX-RS Resource Field which are annotated with the given
	 * annotation fully qualified name
	 *
	 * @param annotationName
	 *            the annotation's fully qualified name
	 * @return the JAX-RS Resource Fields or empty list
	 */
	public List<JaxrsResourceField> getFieldsAnnotatedWith(String annotationName) {
		final List<JaxrsResourceField> annotatedFields = new ArrayList<JaxrsResourceField>();
		for (Entry<String, JaxrsResourceField> entry : resourceFields.entrySet()) {
			JaxrsResourceField field = entry.getValue();
			if (field.hasAnnotation(annotationName)) {
				annotatedFields.add(field);
			}
		}
		return annotatedFields;
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * @see org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.AbstractJaxrsBaseElement
	 *      #remove()
	 */
	@Override
	public void remove() throws CoreException {
		super.remove();
		for (IJaxrsResourceField field : getAllFields()) {
			((JaxrsResourceField) field).remove();
		}
		for (IJaxrsResourceMethod method : getAllMethods()) {
			((JaxrsResourceMethod) method).remove();
		}
	}


}
