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

package org.jboss.tools.ws.jaxrs.ui.internal.validation;

import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsParamConverterProvider;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsProvider;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.CompilationUnitsRepository;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJavaMethodParameter;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJavaMethodSignature;
import org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.ui.preferences.JaxrsPreferences;

/**
 * JAX-RS Provider validator.
 * 
 * @author xcoulon
 * 
 */
public class JaxrsParamConverterProviderValidatorDelegate extends AbstractJaxrsElementValidatorDelegate<JaxrsParamConverterProvider> {

	/** The underlying marker manager.*/
	private final IMarkerManager markerManager;

	/**
	 * Constructor
	 * @param markerManager the underlying marker manager to use
	 */
	public JaxrsParamConverterProviderValidatorDelegate(final IMarkerManager markerManager) {
		this.markerManager = markerManager;
	}

	/**
	 * @see org.jboss.tools.ws.jaxrs.ui.internal.validation.AbstractJaxrsElementValidatorDelegate#internalValidate(Object)
	 */
	@Override
	void internalValidate(final JaxrsParamConverterProvider provider) throws CoreException {
		Logger.debug("Validating element {}", provider);
		validateAtLeastOneValidConstructor(provider);
		validateNoMissingProviderAnnotation(provider);
	}

	
	/**
	 * A per spec (chap 4.1.1):
	 * <quote>
	 * Provider classes are instantiated by the JAX-RS runtime and MUST have a
	 * public constructor for which the JAX-RS runtime can provide all parameter
	 * values. Note that a zero argument constructor is permissible under this
	 * rule.
	 * </quote>
	 * <quote>
	 * A public constructor MAY include parameters annotated with @Context-
	 * chapter 5 defines the parameter types permitted for this annotation.
	 * Since providers may be created outside the scope of a particular request,
	 * only deployment-specific properties may be available from injected
	 * interfaces at construction time - request- specific properties are
	 * available when a provider method is called. If more than one public
	 * constructor can be used then an implementation MUST use the one with the
	 * most parameters. Choosing amongst constructors with the same number of
	 * parameters is implementation specific, implementations SHOULD generate a
	 * warning about such ambiguity.
	 * </quote>
	 * @throws CoreException 
	 * */
	private void validateAtLeastOneValidConstructor(final JaxrsParamConverterProvider provider) throws CoreException {
		final IType providerType = provider.getJavaElement();
		final IMethod[] methods = providerType.getMethods();
		int validConstructorsCounter = 0;
		// indicates if the given java element has at least one constructor
		// (otherwise, it will be the default no-arg constructor)
		boolean hasConstructors = false;
		for (IMethod method : methods) {
			if (isContructor(method)) {
				hasConstructors = true;
				if (isValidConstructor(method)) {
					validConstructorsCounter++;
				}
			}
		}
		if (hasConstructors && validConstructorsCounter == 0) {
			final ISourceRange nameRange = providerType.getNameRange();
			markerManager.addMarker(provider,
					nameRange, JaxrsValidationMessages.PROVIDER_MISSING_VALID_CONSTRUCTOR, new String[0], JaxrsPreferences.PROVIDER_MISSING_VALID_CONSTRUCTOR);
		}

	}

	private static boolean isContructor(final IMethod method) {
		return method.getElementName().equals(method.getParent().getElementName());
	}

	private static boolean isValidConstructor(final IMethod method) throws JavaModelException {
		// refusing non-public contructors
		if ((method.getFlags() & Flags.AccPublic) == 0) {
			return false;
		}
		final ILocalVariable[] parameters = method.getParameters();
		// accepting public empty constructor
		if (parameters.length == 0) {
			return true;
		}
		// only accepting constructors with parameters annotated with
		// @javax.ws.rs.core.Context
		final IJavaMethodSignature methodSignature = CompilationUnitsRepository.getInstance().getMethodSignature(method);
		if(methodSignature != null) {
			for (IJavaMethodParameter parameter : methodSignature.getMethodParameters()) {
				if (parameter.getAnnotations().isEmpty()) {
					return false;
				}
				for (Entry<String, Annotation> annotation : parameter.getAnnotations().entrySet()) {
					if (!annotation.getValue().getFullyQualifiedName().equals(JaxrsClassnames.CONTEXT)
							|| !CONTEXT_TYPE_NAMES.contains(parameter.getType().getErasureName())) {
						return false;
					}
				}
			}
		}
		return true;
	}

	

	/**
	 * Verifies that the given {@link JaxrsProvider} as the expected
	 * <code>@Provider</code> annotation.
	 * 
	 * @param provider
	 * @throws CoreException 
	 */
	private void validateNoMissingProviderAnnotation(final JaxrsParamConverterProvider provider) throws CoreException {
		final Annotation annotation = provider.getAnnotation(JaxrsClassnames.PROVIDER);
		if (annotation == null) {
			final ISourceRange nameRange = provider.getJavaElement().getNameRange();
			markerManager.addMarker(provider,
					nameRange, JaxrsValidationMessages.PROVIDER_MISSING_ANNOTATION, new String[0], JaxrsPreferences.PROVIDER_MISSING_ANNOTATION);
		}
	}


}
