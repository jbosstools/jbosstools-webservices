/******************************************************************************* 
 * Copyright (c) 2012 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.ws.jaxrs.ui.internal.validation;

import static org.jboss.tools.ws.jaxrs.core.jdt.Annotation.VALUE;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsNameBinding;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsProvider;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceField;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceProperty;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.AnnotationUtils;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsProvider;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResourceField;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResourceProperty;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.ui.preferences.JaxrsPreferences;

/**
 * JAX-RS Resource validator.
 * 
 * @author Xavier Coulon
 * 
 */
public class JaxrsResourceValidatorDelegate extends AbstractJaxrsElementValidatorDelegate<JaxrsResource> {

	/**
	 * Constructor
	 * 
	 * @param markerManager
	 *            the underlying marker manager to use
	 */
	public JaxrsResourceValidatorDelegate(final IMarkerManager markerManager) {
		super(markerManager);
	}

	/**
	 * @see org.jboss.tools.ws.jaxrs.ui.internal.validation.AbstractJaxrsElementValidatorDelegate#internalValidate(Object)
	 */
	@Override
	void internalValidate(final JaxrsResource resource, final CompilationUnit ast) throws CoreException {
		Logger.debug("Validating element {}", resource);
		validatePathAnnotationValue(resource, ast);
		validateAtLeastOneProviderWithBinding(resource);
		for (IJaxrsResourceMethod resourceMethod : resource.getAllMethods()) {
			new JaxrsResourceMethodValidatorDelegate(markerManager).validate((JaxrsResourceMethod) resourceMethod, ast,
					false);
		}
		for (IJaxrsResourceField resourceField : resource.getAllFields()) {
			new JaxrsResourceFieldValidatorDelegate(markerManager).validate((JaxrsResourceField) resourceField, ast,
					false);
		}
		for (IJaxrsResourceProperty resourceProperty : resource.getAllProperties()) {
			new JaxrsResourcePropertyValidatorDelegate(markerManager).validate(
					(JaxrsResourceProperty) resourceProperty, ast, false);
		}
	}

	/**
	 * Validates that if the {@code @Path} annotation value contains brackets,
	 * the end bracket is not missing.
	 * 
	 * @param resource
	 *            the resource to validate
	 * @throws JavaModelException
	 * @throws CoreException
	 * @see JaxrsParameterValidatorDelegate
	 */
	private void validatePathAnnotationValue(final JaxrsResource resource, final CompilationUnit ast)
			throws JavaModelException, CoreException {
		final Annotation pathAnnotation = resource.getPathAnnotation();
		if (pathAnnotation != null && pathAnnotation.getValue() != null) {
			if (!AnnotationUtils.isValidAnnotationValue(pathAnnotation.getValue())) {
				final ISourceRange range = JdtUtils.resolveMemberPairValueRange(pathAnnotation.getJavaAnnotation(),
						VALUE, ast);
				markerManager.addMarker(resource, range,
						JaxrsValidationMessages.RESOURCE_INVALID_PATH_ANNOTATION_VALUE,
						new String[] { pathAnnotation.getValue() },
						JaxrsPreferences.RESOURCE_INVALID_PATH_ANNOTATION_VALUE);
			}
		}
	}

	/**
	 * Validates that there is at least one {@link JaxrsProvider} annotated with
	 * exactly the same custom {@link JaxrsNameBinding} annotation(s) in the
	 * metamodel.
	 * 
	 * @param resource
	 *            the {@link JaxrsResource} to validate.
	 * @throws CoreException
	 */
	private void validateAtLeastOneProviderWithBinding(final JaxrsResource resource) throws CoreException {
		if (resource == null) {
			return;
		}
		final Map<String, Annotation> nameBindingAnnotations = resource.getNameBindingAnnotations();
		if (nameBindingAnnotations.isEmpty()) {
			return;
		}
		final JaxrsMetamodel metamodel = resource.getMetamodel();
		annotations_loop: for (Entry<String, Annotation> entry : nameBindingAnnotations.entrySet()) {
			final String nameBindingAnnotationClassName = entry.getKey();
			final Collection<IJaxrsProvider> annotatedProviders = metamodel
					.findProvidersByAnnotation(nameBindingAnnotationClassName);
			// if provider binding annotation(s) match the application binding
			// annotations
			for (IJaxrsProvider provider : annotatedProviders) {
				if (resource.getNameBindingAnnotations().keySet()
						.containsAll(provider.getNameBindingAnnotations().keySet())) {
					// provider is valid, at least one method has all those
					// bindings
					continue annotations_loop;
				}
			}
			// otherwise, add a problem marker
			final ISourceRange nameRange = entry.getValue().getJavaAnnotation().getNameRange();
			markerManager.addMarker(resource, nameRange, JaxrsValidationMessages.PROVIDER_MISSING_BINDING,
					new String[] { nameBindingAnnotationClassName }, JaxrsPreferences.PROVIDER_MISSING_BINDING);

		}
	}

	@SuppressWarnings("unused")
	private void validateConstructorParameters() {
		// TODO...
	}
}
