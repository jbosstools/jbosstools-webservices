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
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.APPLICATION_PATH;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.HTTP_METHOD;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PATH;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PATH_PARAM;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PROVIDER;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionUtils;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsParameterAggregator;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResourceField;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResourceProperty;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta;

/**
 * A JAX-RS Bean Param element, i.e., a type 
 * @author xcoulon
 *
 */
public class JaxrsParameterAggregator extends JaxrsJavaElement<IType> implements IJaxrsParameterAggregator {

	
	/**
	 * Builder initializer
	 * 
	 * @param javaElement
	 *            the underlying {@link IJavaElement} that on which this JAX-RS
	 *            Element will be built.
	 * @return the Builder
	 * @throws JavaModelException
	 */
	public static Builder from(final IJavaElement javaElement) throws JavaModelException {
		final CompilationUnit ast = JdtUtils.parse(javaElement, new NullProgressMonitor());
		return from(javaElement, ast);
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
	public static Builder from(final IJavaElement javaElement, final CompilationUnit ast) {
		switch (javaElement.getElementType()) {
		case IJavaElement.COMPILATION_UNIT:
			return new Builder(((ICompilationUnit) javaElement).findPrimaryType(), ast);
		case IJavaElement.TYPE:
			return new Builder((IType) javaElement, ast);
		}
		return null;
	}

	/**
	 * Internal Builder
	 * 
	 * @author xcoulon
	 * 
	 */
	public static class Builder {

		private final IType javaType;
		private final CompilationUnit ast;
		private JaxrsMetamodel metamodel;

		private Builder(final IType javaType, final CompilationUnit ast) {
			this.javaType = javaType;
			this.ast = ast;
		}

		public JaxrsParameterAggregator buildTransient() throws CoreException {
			return buildInMetamodel(null);
		}
		
		public JaxrsParameterAggregator buildInMetamodel(final JaxrsMetamodel metamodel) throws CoreException {
			final long start = System.currentTimeMillis();
			try {
				// skip if element does not exist or if it has compilation errors
				if (javaType == null || !javaType.exists() || !javaType.isStructureKnown()) {
					return null;
				}
				this.metamodel = metamodel;
				// do not expect JAX-RS annotations on the type !
				final Map<String, Annotation> typeAnnotations = JdtUtils.resolveAllAnnotations(javaType, ast);
				if(CollectionUtils.hasIntersection(typeAnnotations.keySet(), Arrays.asList(APPLICATION_PATH, PATH, PROVIDER, HTTP_METHOD))) {
					return null;
				}
				// but expect annotations on fields
				final IField[] allFields = javaType.getFields();
				final List<IField> relevantFields = new ArrayList<IField>();
				for(IField field : allFields) {
					final Map<String, Annotation> fieldAnnotations = JdtUtils.resolveAllAnnotations(field, ast);
					if(JaxrsParamAnnotations.matchesAtLeastOne(fieldAnnotations.keySet())) {
						relevantFields.add(field);
					}
				}
				// or on properties, too
				final IMethod[] allProperties = javaType.getMethods();
				final List<IMethod> relevantProperties = new ArrayList<IMethod>();
				for(IMethod method : allProperties) {
					final Map<String, Annotation> methodAnnotations = JdtUtils.resolveAllAnnotations(method, ast);
					if(JaxrsParamAnnotations.matchesAtLeastOne(methodAnnotations.keySet())) {
						relevantProperties.add(method);
					}
				}
				
				if (!relevantFields.isEmpty() || !relevantProperties.isEmpty()) {
					final JaxrsParameterAggregator parameterAggregator = new JaxrsParameterAggregator(this);
					// this operation is only performed after creation, but before children elements are added
					if(this.metamodel != null) {
						parameterAggregator.joinMetamodel();
					}
					// include fields and properties
					for(IField field : relevantFields) {
						JaxrsParameterAggregatorField.from(field, ast).buildInParentAggregator(parameterAggregator);
					}
					for(IMethod method : relevantProperties) {
						JaxrsParameterAggregatorProperty.from(method, ast).buildInParentAggregator(parameterAggregator);
					}
					
					return parameterAggregator;
				}
				return null;
			} finally {
				final long end = System.currentTimeMillis();
				Logger.tracePerf("Built JAX-RS Parameter Aggreagator in {}ms", (end - start));
			}
		}

	}

	/** The map of {@link JaxrsParameterAggregatorField} indexed by the underlying java element identifier. */
	private final Map<String, JaxrsParameterAggregatorField> fields = new HashMap<String, JaxrsParameterAggregatorField>();

	/** The map of {@link JaxrsParameterAggregatorProperty} indexed by the underlying java element identifier. */
	private final Map<String, JaxrsParameterAggregatorProperty> properties = new HashMap<String, JaxrsParameterAggregatorProperty>();

	private JaxrsParameterAggregator(final Builder builder) {
		super(builder.javaType, null, builder.metamodel);
		
	}
	
	public void addElement(final JaxrsParameterAggregatorField field) {
		if (field != null) {
			this.fields.put(field.getIdentifier(), field);
		}
	}

	public void addElement(final JaxrsParameterAggregatorProperty method) {
		if (method != null) {
			this.properties.put(method.getJavaElement().getHandleIdentifier(), method);
		}
	}

		/**
	 * {@inheritDoc}
	 */
	@Override
	public EnumElementKind getElementKind() {
		return EnumElementKind.PARAMETER_AGGREGATOR;
	}
	
	/**
	 * @return the fully qualified name of the underlying {@link IJavaElement}.
	 */
	@Override
	public final String getName() {
		return getJavaElement().getFullyQualifiedName();
	}
	
	@Override
	public List<JaxrsParameterAggregatorField> getAllFields() {
		return Collections.unmodifiableList(new ArrayList<JaxrsParameterAggregatorField>(fields.values()));
	}
	
	/**
	 * @return the {@link JaxrsParameterAggregatorField} indexed by their underlying {@link JaxrsParameterAggregatorField#getIdentifier()} value
	 */
	public Map<String, JaxrsParameterAggregatorField> getFields() {
		return Collections.unmodifiableMap(this.fields);
	}
	
	/**
	 * Returns the {@link JaxrsParameterAggregatorField}s having an annotation whose fully qualified name is given.
	 * @param annotationName the fully qualified name of the annotation to find
	 * @return the list of matching fields
	 */
	public List<JaxrsParameterAggregatorField> getFieldsAnnotatedWith(final String annotationName) {
		final List<JaxrsParameterAggregatorField> matches = new ArrayList<JaxrsParameterAggregatorField>();
		for(JaxrsParameterAggregatorField field : fields.values()) {
			if(field.hasAnnotation(annotationName)) {
				matches.add(field);
			}
		}
		return matches;
	}
	
	/**
	 * Returns the {@link JaxrsParameterAggregatorField} which is annotated with the given
	 * annotation fully qualified name and which has the given annotationValue
	 *
	 * @param annotationName
	 *            the annotation's fully qualified name
	 * @return the {@link JaxrsParameterAggregatorField} or {@code null}
	 */
	public JaxrsParameterAggregatorField getFieldAnnotatedWith(final String annotationName, final String annotationValue) {
		for (Entry<String, JaxrsParameterAggregatorField> entry : fields.entrySet()) {
			final JaxrsParameterAggregatorField field = entry.getValue();
			final Annotation annotation = field.getAnnotation(annotationName);
			if (annotation != null && annotationValue.equals(annotation.getValue())) {
				return field;
			}
		}
		return null;
	}
	
	

	@Override
	public List<JaxrsParameterAggregatorProperty> getAllProperties() {
		return Collections.unmodifiableList(new ArrayList<JaxrsParameterAggregatorProperty>(this.properties.values()));
	}
	
	/**
	 * @return the {@link JaxrsParameterAggregatorProperty} indexed by their underlying {@link JaxrsParameterAggregatorProperty#getIdentifier()} value
	 */
	public Map<String, JaxrsParameterAggregatorProperty> getProperties() {
		return Collections.unmodifiableMap(this.properties);
	}

	/**
	 * Returns the {@link JaxrsParameterAggregatorProperty}s having an annotation whose fully qualified name is given.
	 * @param annotationName the fully qualified name of the annotation to find
	 * @return the list of matching properties
	 */
	public List<JaxrsParameterAggregatorProperty> getPropertiesAnnotatedWith(final String annotationName) {
		final List<JaxrsParameterAggregatorProperty> matches = new ArrayList<JaxrsParameterAggregatorProperty>();
		for(JaxrsParameterAggregatorProperty method : properties.values()) {
			if(method.hasAnnotation(annotationName)) {
				matches.add(method);
			}
		}
		return matches;
	}
	
	/**
	 * Returns the {@link JaxrsResourceProperty} which is annotated with the given
	 * annotation fully qualified name and which has the given annotationValue
	 *
	 * @param annotationName
	 *            the annotation's fully qualified name
	 * @return the JAX-RS Resource Field or {@code null}
	 */
	public JaxrsParameterAggregatorProperty getPropertyAnnotatedWith(final String annotationName, final String annotationValue) {
		for (Entry<String, JaxrsParameterAggregatorProperty> entry : properties.entrySet()) {
			final JaxrsParameterAggregatorProperty property = entry.getValue();
			final Annotation annotation = property.getAnnotation(annotationName);
			if (annotation != null && annotationValue.equals(annotation.getValue())) {
				return property;
			}
		}
		return null;
	}
	
	/**
	 * Removes the given {@link JaxrsParameterAggregatorField} from the internal collection.
	 * @param parameterAggregatorField the field to remove
	 */
	public void removeField(final JaxrsParameterAggregatorField parameterAggregatorField) {
		this.fields.remove(parameterAggregatorField.getIdentifier());
	}

	/**
	 * Removes the given {@link JaxrsParameterAggregatorProperty} from the internal collection.
	 * @param parameterAggregatorProperty the field to remove
	 */
	public void removeProperty(final JaxrsParameterAggregatorProperty parameterAggregatorProperty) {
		this.properties.remove(parameterAggregatorProperty.getIdentifier());
	}
	
	/**
	 * @return the values of all annotations whose fully qualified name is
	 *         {@code javax.ws.rs.PathParam}. These annotation can be found on
	 *         any {@link JaxrsParameterAggregatorField} and
	 *         {@link JaxrsParameterAggregatorProperty} of this
	 *         {@link JaxrsParameterAggregator}.
	 */
	public List<String> getPathParamValues() {
		final List<String> pathParamValues = new ArrayList<String>();
		for (JaxrsParameterAggregatorField aggregatorField : this.fields.values()) {
			final Annotation aggregatorFieldPathParamAnnotation = aggregatorField.getAnnotation(PATH_PARAM);
			if (aggregatorFieldPathParamAnnotation != null && aggregatorFieldPathParamAnnotation.getValue() != null) {
				pathParamValues.add(aggregatorFieldPathParamAnnotation.getValue());
			}
		}
		for (JaxrsParameterAggregatorProperty aggregatorProperties : this.properties.values()) {
			final Annotation aggregatorFieldPathParamAnnotation = aggregatorProperties.getAnnotation(PATH_PARAM);
			if (aggregatorFieldPathParamAnnotation != null && aggregatorFieldPathParamAnnotation.getValue() != null) {
				pathParamValues.add(aggregatorFieldPathParamAnnotation.getValue());
			}
		}
		return pathParamValues;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void update(final IJavaElement javaElement, final CompilationUnit ast) throws CoreException {
		final JaxrsParameterAggregator transientAggregator = from(javaElement, ast).buildTransient();
		if (transientAggregator == null) {
			remove();
			return;
		} 
		final Flags updateAnnotationsFlags = updateAnnotations(transientAggregator.getAnnotations());
		final JaxrsElementDelta delta = new JaxrsElementDelta(this, CHANGED, updateAnnotationsFlags);
		updateProperties(transientAggregator, ast);
		updateFields(transientAggregator, ast);

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
	 * @param transientAggregator the resource to analyze
	 * @param ast its associated AST.
	 * @throws CoreException
	 */
	private void updateProperties(final JaxrsParameterAggregator transientAggregator, final CompilationUnit ast) throws CoreException {
		final List<JaxrsParameterAggregatorProperty> allTransientInstanceProperties = transientAggregator.getAllProperties();
		final List<JaxrsParameterAggregatorProperty> allCurrentProperties = this.getAllProperties();
		final List<JaxrsParameterAggregatorProperty> addedProperties = CollectionUtils.difference(allTransientInstanceProperties,
				allCurrentProperties);
		for (JaxrsParameterAggregatorProperty addedProperty : addedProperties) {
			// create the Resource Field by attaching it to the metamodel
			// and to this parent resource.
			JaxrsParameterAggregatorProperty.from(addedProperty.getJavaElement(), ast).buildInParentAggregator(this);
		}
		final Collection<JaxrsParameterAggregatorProperty> changedProperties = CollectionUtils.intersection(allCurrentProperties,
				allTransientInstanceProperties);
		for (JaxrsParameterAggregatorProperty changedProperty: changedProperties) {
			((JaxrsParameterAggregatorProperty) changedProperty).update(transientAggregator.getProperties().get(
					changedProperty.getIdentifier()));
		}
		final List<JaxrsParameterAggregatorProperty> removedProperties = CollectionUtils.difference(allCurrentProperties,
				allTransientInstanceProperties);
		for (JaxrsParameterAggregatorProperty removedProperty: removedProperties) {
			removedProperty.remove();
		}
	}
	
	/**
	 * Updates the {@link IJaxrsResourceField}s of {@code this} from the ones provided by the given {@link JaxrsResource}
	 * @param transientAggregator the resource to analyze
	 * @param ast its associated AST.
	 * @throws CoreException
	 */
	private void updateFields(final JaxrsParameterAggregator transientAggregator, final CompilationUnit ast) throws CoreException {
		final List<JaxrsParameterAggregatorField> allTransientInstanceFields = transientAggregator.getAllFields();
		final List<JaxrsParameterAggregatorField> allCurrentFields = this.getAllFields();
		final List<JaxrsParameterAggregatorField> addedFields = CollectionUtils.difference(allTransientInstanceFields,
				allCurrentFields);
		for (JaxrsParameterAggregatorField addedField : addedFields) {
			// create the Resource Field by attaching it to the metamodel
			// and to this parent resource.
			JaxrsParameterAggregatorField.from(addedField.getJavaElement(), ast).buildInParentAggregator(this);
		}
		final Collection<JaxrsParameterAggregatorField> changedFields = CollectionUtils.intersection(allCurrentFields,
				allTransientInstanceFields);
		for (JaxrsParameterAggregatorField changedField : changedFields) {
			((JaxrsParameterAggregatorField) changedField).update(transientAggregator.getFields().get(
					changedField.getIdentifier()));
		}
		final List<JaxrsParameterAggregatorField> removedFields = CollectionUtils.difference(allCurrentFields,
				allTransientInstanceFields);
		for (JaxrsParameterAggregatorField removedField : removedFields) {
			removedField.remove();
		}
	}



	/**
	 * {@inheritDoc}
	 */
	@Override
	boolean isMarkedForRemoval() {
		return getAllFields().isEmpty() && getAllProperties().isEmpty();
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
		for (JaxrsParameterAggregatorField field : getAllFields()) {
			((JaxrsParameterAggregatorField) field).remove();
		}
		for (JaxrsParameterAggregatorProperty property : getAllProperties()) {
			((JaxrsParameterAggregatorProperty) property).remove();
		}
	}

}
