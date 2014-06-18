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
import static org.jboss.tools.ws.jaxrs.core.jdt.Annotation.VALUE;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.CONSUMES;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PATH;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PATH_PARAM;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PRODUCES;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.AnnotationUtils;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJavaMethodSignature;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResource;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResourceField;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResourceProperty;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta;

/**
 * From the spec : A resource class is a Java class that uses JAX-RS annotations
 * to implement a corresponding Web resource. Resource classes are POJOs that
 * have at least one method annotated with @Path or a request method designator.
 * 
 * @author xcoulon
 */
public final class JaxrsResource extends JaxrsJavaElement<IType> implements IJaxrsResource {

	/** The map of {@link JaxrsResourceField} indexed by the underlying java element identifier. */
	private final Map<String, JaxrsResourceField> resourceFields = new HashMap<String, JaxrsResourceField>();

	/** The map of {@link JaxrsResourceProperty} indexed by the underlying java element identifier. */
	private final Map<String, JaxrsResourceProperty> resourceProperties = new HashMap<String, JaxrsResourceProperty>();
	
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
	public static Builder from(final IJavaElement javaElement, final Set<String> httpMethodNames)
			throws JavaModelException {
		final CompilationUnit ast = JdtUtils.parse(javaElement, new NullProgressMonitor());
		switch (javaElement.getElementType()) {
		case IJavaElement.COMPILATION_UNIT:
			return new Builder(((ICompilationUnit) javaElement).findPrimaryType(), ast, httpMethodNames);
		case IJavaElement.TYPE:
			return new Builder((IType) javaElement, ast, httpMethodNames);
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
			final Set<String> httpMethodNames) {
		switch (javaElement.getElementType()) {
		case IJavaElement.COMPILATION_UNIT:
			return new Builder(((ICompilationUnit) javaElement).findPrimaryType(), ast, httpMethodNames);
		case IJavaElement.TYPE:
			return new Builder((IType) javaElement, ast, httpMethodNames);
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
		private final Set<String> httpMethodNames;
		private Map<String, Annotation> annotations;
		private JaxrsMetamodel metamodel;

		private Builder(final IType javaType, final CompilationUnit ast, final Set<String> httpMethodNames) {
			this.javaType = javaType;
			this.ast = ast;
			this.httpMethodNames = httpMethodNames;
		}

		public Builder withMetamodel(final JaxrsMetamodel metamodel) {
			this.metamodel = metamodel;
			return this;
		}

		public JaxrsResource build() throws CoreException {
			return build(true);
		}

		JaxrsResource build(final boolean joinMetamodel) throws CoreException {
			final long start = System.currentTimeMillis();
			try {
				if (javaType == null || !javaType.exists()) {
					return null;
				}
				Logger.trace("Building a new JAX-RS Resource from {}", javaType.getElementName());
				// create the resource
				this.annotations = JdtUtils.resolveAllAnnotations(javaType, ast);
				final JaxrsResource resource = new JaxrsResource(this);
				// find the available fields
				for (IField javaField : javaType.getFields()) {
					JaxrsResourceField.from(javaField, ast).withParentResource(resource).withMetamodel(metamodel).build();
				}
				// find the available properties
				for (IMethod javaMethod : javaType.getMethods()) {
					JaxrsResourceProperty.from(javaMethod, ast).buildInResource(resource);
				}
				// find the resource methods, subresource methods and
				// subresource
				// locators of this resource:
				final Set<IMethod> javaMethods = JavaElementsSearcher.findResourceMethods(javaType, this.httpMethodNames,
						new NullProgressMonitor());
				for (IMethod javaMethod : javaMethods) {
					final IJavaMethodSignature methodSignature = JdtUtils.resolveMethodSignature(javaMethod, ast);
					JaxrsResourceMethod.from(javaMethod, ast, httpMethodNames).withJavaMethodSignature(methodSignature)
							.buildInResource(resource);
				}
				// well, sorry.. this is not a valid JAX-RS resource (requires at least one method)
				if (resource.isSubresource() && resource.resourceMethods.isEmpty()) {
					return null;
				}
				// this operation is only performed if the resource is acceptable
				// (ie, not UNDEFINED)
				if(joinMetamodel) {
					resource.joinMetamodel();
				}
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
		final JaxrsResource transientResource = from(javaElement, ast, getMetamodel().findAllHttpMethodNames()).build(false);
		if (transientResource == null) {
			remove();
			return;
		} 
		final Flags updateAnnotationsFlags = updateAnnotations(transientResource.getAnnotations());
		final JaxrsElementDelta delta = new JaxrsElementDelta(this, CHANGED, updateAnnotationsFlags);
		updateMethods(transientResource, ast);
		updateProperties(transientResource, ast);
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
	 * Updates the {@link IJaxrsResourceProperty}s of {@code this} from the ones provided by the given {@link JaxrsResource}
	 * @param transientResource the resource to analyze
	 * @param ast its associated AST.
	 * @throws CoreException
	 */
	private void updateProperties(final JaxrsResource transientResource, final CompilationUnit ast) throws CoreException {
		final List<JaxrsResourceProperty> allTransientInstanceProperties = transientResource.getAllProperties();
		final List<JaxrsResourceProperty> allCurrentProperties = this.getAllProperties();
		final List<JaxrsResourceProperty> addedProperties = CollectionUtils.difference(allTransientInstanceProperties,
				allCurrentProperties);
		for (JaxrsResourceProperty addedProperty : addedProperties) {
			// create the Resource Field by attaching it to the metamodel
			// and to this parent resource.
			JaxrsResourceProperty.from(addedProperty.getJavaElement(), ast).buildInResource(this);
		}
		final Collection<JaxrsResourceProperty> changedProperties = CollectionUtils.intersection(allCurrentProperties,
				allTransientInstanceProperties);
		for (JaxrsResourceProperty changedProperty: changedProperties) {
			((JaxrsResourceProperty) changedProperty).update(transientResource.getProperties().get(
					changedProperty.getIdentifier()));
		}
		final List<JaxrsResourceProperty> removedProperties = CollectionUtils.difference(allCurrentProperties,
				allTransientInstanceProperties);
		for (JaxrsResourceProperty removedProperty: removedProperties) {
			removedProperty.remove();
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
			removedField.remove();
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
			JaxrsResourceMethod.from(addedMethod.getJavaElement(), ast, getMetamodel().findAllHttpMethodNames()).buildInResource(this);
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
			((JaxrsResourceMethod)removedMethod).remove();
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
		// JAX-RS method. Having JAX-RS fields only is not enough.
		return !(hasPathAnnotation || resourceMethods.size() > 0);
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

	/**
	 * @return the fully qualified name of the underlying {@link IJavaElement}.
	 */
	@Override
	public final String getName() {
		return getJavaElement().getFullyQualifiedName();
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
	public Map<String, Annotation> getPathTemplateParameters() {
		final Map<String, Annotation> proposals = new HashMap<String, Annotation>();
		proposals.putAll(AnnotationUtils.extractTemplateParameters(getPathAnnotation()));
		return proposals;
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
	
	public final List<JaxrsResourceField> getAllFields() {
		return Collections.unmodifiableList(new ArrayList<JaxrsResourceField>(resourceFields.values()));
	}
	
	public List<JaxrsResourceProperty> getAllProperties() {
		return Collections.unmodifiableList(new ArrayList<JaxrsResourceProperty>(resourceProperties.values()));
	}

	/**
	 * @return the values of all annotations whose fully qualified name is
	 *         {@code javax.ws.rs.PathParam}. These annotation can be found on
	 *         any {@link JaxrsResourceField} and
	 *         {@link JaxrsResourceProperty} of this
	 *         {@link JaxrsResource}.
	 */
	public List<String> getPathParamValues() {
		final List<String> pathParamValues = new ArrayList<String>();
		for (JaxrsResourceField field : this.resourceFields.values()) {
			final Annotation aggregatorFieldPathParamAnnotation = field.getAnnotation(PATH_PARAM);
			if (aggregatorFieldPathParamAnnotation != null && aggregatorFieldPathParamAnnotation.getValue() != null) {
				pathParamValues.add(aggregatorFieldPathParamAnnotation.getValue());
			}
		}
		for (JaxrsResourceProperty properties : this.resourceProperties.values()) {
			final Annotation aggregatorFieldPathParamAnnotation = properties.getAnnotation(PATH_PARAM);
			if (aggregatorFieldPathParamAnnotation != null && aggregatorFieldPathParamAnnotation.getValue() != null) {
				pathParamValues.add(aggregatorFieldPathParamAnnotation.getValue());
			}
		}
		return pathParamValues;
	}


	@Override
	public String toString() {
		return new StringBuffer().append("Resource '").append(getName()).append("' (root=").append(isRootResource())
				.append(") ").toString();
	}

	/**
	 * Adds the given {@link JaxrsResourceField} to this {@link JaxrsResource}.
	 * @param resourceField the element to add.
	 */
	protected void addField(final JaxrsResourceField resourceField) {
		if (resourceField != null) {
			this.resourceFields.put(resourceField.getJavaElement().getHandleIdentifier(), resourceField);
		}
	}
	
	/**
	 * @return an <strong>unmodifiable</strong map of {@link JaxrsResourceField} indexed by the underlying
	 *         java element identifier.
	 */
	public Map<String, JaxrsResourceField> getFields() {
		return Collections.unmodifiableMap(resourceFields);
	}

	/**
	 * Removes the given {@link IJaxrsResourceField} from this {@link JaxrsResource}.
	 * @param resourceField the resource field to remove
	 * @throws CoreException
	 */
	protected void removeField(final IJaxrsResourceField resourceField) throws CoreException {
		this.resourceFields.remove(resourceField.getJavaElement().getHandleIdentifier());
	}
	
	/**
	 * Adds the given {@link JaxrsResourceProperty} to this {@link JaxrsResource}.
	 * @param resourceProperty the element to add.
	 */
	protected void addProperty(final JaxrsResourceProperty resourceProperty) {
		if (resourceProperty != null) {
			this.resourceProperties.put(resourceProperty.getJavaElement().getHandleIdentifier(), resourceProperty);
		}
	}

	/**
	 * @return an <strong>unmodifiable</strong map of {@link JaxrsResourceProperty} indexed by the underlying
	 *         java element identifier.
	 */
	public Map<String, JaxrsResourceProperty> getProperties() {
		return Collections.unmodifiableMap(this.resourceProperties);
	}

	/**
	 * Removes the given {@link JaxrsResourceProperty} from this {@link JaxrsResource}.
	 * @param resourceProperty the resource property to remove
	 * @throws CoreException
	 */
	protected void removeProperty(final JaxrsResourceProperty resourceProperty) throws CoreException {
		this.resourceProperties.remove(resourceProperty.getJavaElement().getHandleIdentifier());
	}
	
	

	protected void addMethod(final JaxrsResourceMethod resourceMethod) {
		if (resourceMethod != null) {
			this.resourceMethods.put(resourceMethod.getJavaElement().getHandleIdentifier(), resourceMethod);
		}
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
	protected void removeMethod(final IJaxrsResourceMethod method) throws CoreException {
		this.resourceMethods.remove(method.getJavaElement().getHandleIdentifier());
		//((JaxrsResourceMethod) method).remove();
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
	 * Returns the list of {@link JaxrsResourceField} which are annotated with the given
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
	 * Returns the list of {@link JaxrsResourceProperty} which are annotated with the given
	 * annotation fully qualified name
	 *
	 * @param annotationName
	 *            the annotation's fully qualified name
	 * @return the JAX-RS Resource Fields or empty list
	 */
	public List<JaxrsResourceProperty> getPropertiesAnnotatedWith(String annotationName) {
		final List<JaxrsResourceProperty> annotatedProperties = new ArrayList<JaxrsResourceProperty>();
		for (Entry<String, JaxrsResourceProperty> entry : resourceProperties.entrySet()) {
			JaxrsResourceProperty property = entry.getValue();
			if (property.hasAnnotation(annotationName)) {
				annotatedProperties.add(property);
			}
		}
		return annotatedProperties;
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * @see org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsBaseElement
	 *      #remove()
	 */
	@Override
	public void remove() throws CoreException {
		super.remove();
		// removing methods first will remove associated endpoints immediately. 
		for (IJaxrsResourceMethod method : getAllMethods()) {
			((JaxrsResourceMethod) method).remove();
		}
		// if we removed fields first, there could be some unnecessary 'CHANGED' events for fields with 
		// @QueryParam or @MatrixParam annotations, that affect the Endpoint's URL Path Template.
		// 
		for (IJaxrsResourceField field : getAllFields()) {
			((JaxrsResourceField) field).remove();
		}
		for (IJaxrsResourceProperty property : getAllProperties()) {
			((JaxrsResourceProperty) property).remove();
		}
	}

}
