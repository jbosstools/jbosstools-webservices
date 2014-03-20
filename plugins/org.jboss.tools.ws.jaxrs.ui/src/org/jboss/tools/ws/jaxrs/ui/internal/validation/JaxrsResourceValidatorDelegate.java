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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ISourceRange;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsNameBinding;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsProvider;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsProvider;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.utils.Annotation;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.ui.preferences.JaxrsPreferences;

/**
 * JAX-RS Resource validator.
 * 
 * @author Xavier Coulon
 * 
 */
public class JaxrsResourceValidatorDelegate extends AbstractJaxrsElementValidatorDelegate<JaxrsResource> {
	private final IMarkerManager markerManager;

	public JaxrsResourceValidatorDelegate(final IMarkerManager markerManager) {
		this.markerManager = markerManager;
	}

	/**
	 * @see org.jboss.tools.ws.jaxrs.ui.internal.validation.AbstractJaxrsElementValidatorDelegate#internalValidate(Object)
	 */
	@Override
	void internalValidate(final JaxrsResource resource) throws CoreException {
		Logger.debug("Validating element {}", resource);
		removeMarkers(resource);
		validateAtLeastOneProviderWithBinding(resource);
		for (IJaxrsResourceMethod resourceMethod : resource.getAllMethods()) {
			new JaxrsResourceMethodValidatorDelegate(markerManager).validate((JaxrsResourceMethod) resourceMethod);
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
		final List<IJaxrsProvider> annotatedProviders = metamodel
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
