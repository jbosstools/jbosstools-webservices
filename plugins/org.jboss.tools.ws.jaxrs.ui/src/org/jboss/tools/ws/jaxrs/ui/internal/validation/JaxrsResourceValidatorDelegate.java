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
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsNameBinding;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsProvider;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceField;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.AnnotationUtils;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsProvider;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResourceField;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.ui.preferences.JaxrsPreferences;

/**
 * JAX-RS Resource validator.
 * 
 * @author Xavier Coulon
 * 
 */
public class JaxrsResourceValidatorDelegate extends AbstractJaxrsElementValidatorDelegate<JaxrsResource> {
	
	/** The underlying marker manager.*/
	private final IMarkerManager markerManager;

	/**
	 * Constructor
	 * @param markerManager the underlying marker manager to use
	 */
	public JaxrsResourceValidatorDelegate(final IMarkerManager markerManager) {
		this.markerManager = markerManager;
	}

	/**
	 * @see org.jboss.tools.ws.jaxrs.ui.internal.validation.AbstractJaxrsElementValidatorDelegate#internalValidate(Object)
	 */
	@Override
	void internalValidate(final JaxrsResource resource) throws CoreException {
		Logger.debug("Validating element {}", resource);
		validatePathAnnotationValue(resource);
		validateAtLeastOneProviderWithBinding(resource);
		for (IJaxrsResourceMethod resourceMethod : resource.getAllMethods()) {
			new JaxrsResourceMethodValidatorDelegate(markerManager).validate((JaxrsResourceMethod) resourceMethod, false);
		}
		for (IJaxrsResourceField resourceField : resource.getAllFields()) {
			new JaxrsResourceFieldValidatorDelegate(markerManager).validate((JaxrsResourceField) resourceField, false);
		}
	}

	/**
	 * Validates that if the {@code @Path} annotation value contains brackets, the end bracket is not missing.
	 * 
	 * @param resource
	 *            the resource to validate
	 * @throws JavaModelException 
	 * @throws CoreException
	 * @see JaxrsParameterValidatorDelegate
	 */
	private void validatePathAnnotationValue(final JaxrsResource resource) throws JavaModelException, CoreException {
		final Annotation pathAnnotation = resource.getPathAnnotation();
		if(pathAnnotation != null && pathAnnotation.getValue() != null) {
			if(!AnnotationUtils.isValidAnnotationValue(pathAnnotation.getValue())) {
				final ISourceRange range = JdtUtils.resolveMemberPairValueRange(
						pathAnnotation.getJavaAnnotation(), VALUE);
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
		// take the first NameBinding annotation and look for Providers that
		// have this annotation, too
		final String firstNameBindingAnnotationClassName = nameBindingAnnotations.keySet().iterator().next();
		final Set<String> allBindingAnnotationNames = nameBindingAnnotations.keySet();
		final Collection<IJaxrsProvider> annotatedProviders = metamodel
				.findProvidersByAnnotation(firstNameBindingAnnotationClassName);
		for (IJaxrsProvider provider : annotatedProviders) {
			if (provider.getNameBindingAnnotations().keySet().equals(allBindingAnnotationNames)) {
				// provider is valid, at least one method has all those bindings
				return;
			}
		}
		// otherwise, add a problem marker
		final ISourceRange nameRange = nameBindingAnnotations.get(firstNameBindingAnnotationClassName)
				.getJavaAnnotation().getNameRange();
		markerManager.addMarker(resource, nameRange, JaxrsValidationMessages.PROVIDER_MISSING_BINDING, new String[]{firstNameBindingAnnotationClassName},
				JaxrsPreferences.PROVIDER_MISSING_BINDING);
	}

	@SuppressWarnings("unused")
	private void validateConstructorParameters() {
		// TODO...
	}
}
