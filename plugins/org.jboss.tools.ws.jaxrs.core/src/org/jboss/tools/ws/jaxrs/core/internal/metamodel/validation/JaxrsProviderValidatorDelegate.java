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

package org.jboss.tools.ws.jaxrs.core.internal.metamodel.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.jboss.tools.common.validation.TempMarkerManager;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsProvider;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname;
import org.jboss.tools.ws.jaxrs.core.jdt.JavaMethodParameter;
import org.jboss.tools.ws.jaxrs.core.jdt.JavaMethodSignature;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.preferences.JaxrsPreferences;

/**
 * JAX-RS Provider validator.
 * 
 * @author xcoulon
 * 
 */
public class JaxrsProviderValidatorDelegate extends AbstractJaxrsElementValidatorDelegate<JaxrsProvider> {

	private final static List<String> CONTEXT_TYPE_NAMES = new ArrayList<String>(Arrays.asList(
			"javax.ws.rs.core.UriInfo", "javax.servlet.ServletConfig", "javax.servlet.ServletContext",
			"javax.ws.rs.core.SecurityContext"));

	/**
	 * Constructor.
	 * 
	 * @param markerManager
	 */
	public JaxrsProviderValidatorDelegate(final TempMarkerManager markerManager) {
		super(markerManager);
	}

	/**
	 * @see org.jboss.tools.ws.jaxrs.core.internal.metamodel.validation.AbstractJaxrsElementValidatorDelegate#validate()
	 */
	@Override
	public void validate(JaxrsProvider provider) throws CoreException {
		Logger.debug("Validating element {}", provider);
		try {
			provider.resetProblemLevel();
			validateAtLeastOneValidConstructor(provider);
			validateNoMissingProviderAnnotation(provider);
			validateNoDuplicateProvider(provider);
			validateAtLeastOneImplementation(provider);
		} catch (JavaModelException e) {
			Logger.error("Failed to validate JAX-RS Resource Method '" + provider.getName() + "'", e);
		}
	}

	/**
	 * A per spec (chap 4.1.1):
	 * <p>
	 * Provider classes are instantiated by the JAX-RS runtime and MUST have a
	 * public constructor for which the JAX-RS runtime can provide all parameter
	 * values. Note that a zero argument constructor is permissible under this
	 * rule.
	 * </p>
	 * <p>
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
	 * </p>
	 * 
	 * @throws JavaModelException
	 * */
	private void validateAtLeastOneValidConstructor(JaxrsProvider provider) throws JavaModelException {
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
			addProblem(JaxrsValidationMessages.PROVIDER_MISSING_VALID_CONSTRUCTOR,
					JaxrsPreferences.PROVIDER_MISSING_VALID_CONSTRUCTOR, new String[0], nameRange, provider);
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
		final JavaMethodSignature methodSignature = JdtUtils.resolveMethodSignature(method,
				JdtUtils.parse(method, new NullProgressMonitor()));
		for (JavaMethodParameter parameter : methodSignature.getMethodParameters()) {
			if (parameter.getAnnotations().isEmpty()) {
				return false;
			}
			for (Entry<String, Annotation> annotation : parameter.getAnnotations().entrySet()) {
				if (!annotation.getValue().getFullyQualifiedName().equals(EnumJaxrsClassname.CONTEXT.qualifiedName)
						|| !CONTEXT_TYPE_NAMES.contains(parameter.getTypeName())) {
					return false;
				}
			}
		}
		return true;
	}

	private void validateAtLeastOneImplementation(final JaxrsProvider provider) throws JavaModelException {
		if (provider.getProvidedTypes().size() == 0) {
			final ISourceRange nameRange = provider.getJavaElement().getNameRange();
			addProblem(JaxrsValidationMessages.PROVIDER_MISSING_IMPLEMENTATION,
					JaxrsPreferences.PROVIDER_MISSING_IMPLEMENTATION, new String[0], nameRange, provider);
		}
	}

	/**
	 * Verifies that the given {@link JaxrsProvider} as the expected
	 * <code>@Provider</code> annotation.
	 * 
	 * @param provider
	 * @throws JavaModelException
	 */
	private void validateNoMissingProviderAnnotation(final JaxrsProvider provider) throws JavaModelException {
		final Annotation annotation = provider.getAnnotation(EnumJaxrsClassname.PROVIDER.qualifiedName);
		if (annotation == null) {
			final ISourceRange nameRange = provider.getJavaElement().getNameRange();
			addProblem(JaxrsValidationMessages.PROVIDER_MISSING_ANNOTATION,
					JaxrsPreferences.PROVIDER_MISSING_ANNOTATION, new String[0], nameRange, provider);
		}
	}

	/**
	 * Verifies that the given {@link JaxrsProvider} as the expected
	 * <code>@Provider</code> annotation.
	 * 
	 * @param provider
	 * @throws JavaModelException
	 */
	@SuppressWarnings("incomplete-switch")
	private void validateNoDuplicateProvider(final JaxrsProvider provider) throws JavaModelException {
		final JaxrsMetamodel metamodel = provider.getMetamodel();
		for (Entry<EnumElementKind, IType> entry : provider.getProvidedTypes().entrySet()) {
			final EnumElementKind elementKind = entry.getKey();
			final IType providedType = provider.getProvidedType(elementKind);
			final List<JaxrsProvider> providers = metamodel.findProviders(elementKind,
					providedType.getFullyQualifiedName());
			for (JaxrsProvider p : providers) {
				if (p == provider) {
					continue;
				}
				if (provider.collidesWith(p, elementKind)) {
					switch (elementKind) {
					case MESSAGE_BODY_READER:
						addDuplicateProviderProblem(provider, providedType,
								JaxrsValidationMessages.PROVIDER_DUPLICATE_MESSAGE_BODY_READER,
								JaxrsPreferences.PROVIDER_DUPLICATE_MESSAGE_BODY_READER);
						break;
					case MESSAGE_BODY_WRITER:
						addDuplicateProviderProblem(provider, providedType,
								JaxrsValidationMessages.PROVIDER_DUPLICATE_MESSAGE_BODY_WRITER,
								JaxrsPreferences.PROVIDER_DUPLICATE_MESSAGE_BODY_WRITER);
						break;
					case EXCEPTION_MAPPER:
						addDuplicateProviderProblem(provider, providedType,
								JaxrsValidationMessages.PROVIDER_DUPLICATE_EXCEPTION_MAPPER,
								JaxrsPreferences.PROVIDER_DUPLICATE_EXCEPTION_MAPPER);
						break;
					}
				}
			}
		}
	}

	/**
	 * @param provider
	 * @param providedType
	 * @param message
	 * @param preferenceKey
	 * @throws JavaModelException
	 */
	private void addDuplicateProviderProblem(final JaxrsProvider provider, final IType providedType,
			final String message, final String preferenceKey) throws JavaModelException {
		final ISourceRange nameRange = getTypeParameterNameRange(provider.getJavaElement(), providedType);
		addProblem(message, preferenceKey, new String[] { provider.getJavaElement().getFullyQualifiedName() },
				nameRange, provider);
	}

	/**
	 * Returns the name range for the given type parameter. If it cannot be
	 * found, the return range for the given type is returned instead.
	 * 
	 * @param type
	 * @param parameterType
	 * @return
	 * @throws JavaModelException
	 */
	private ISourceRange getTypeParameterNameRange(final IType type, final IType parameterType)
			throws JavaModelException {
		if (type.getTypeParameter(parameterType.getElementName()).exists()) {
			return type.getTypeParameter(parameterType.getElementName()).getNameRange();
		}
		if (type.getTypeParameter(parameterType.getFullyQualifiedName()).exists()) {
			return type.getTypeParameter(parameterType.getFullyQualifiedName()).getNameRange();
		}
		return type.getNameRange();

	}

}
