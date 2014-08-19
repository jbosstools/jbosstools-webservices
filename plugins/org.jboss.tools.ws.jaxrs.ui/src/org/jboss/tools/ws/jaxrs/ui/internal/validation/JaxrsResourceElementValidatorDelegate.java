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

package org.jboss.tools.ws.jaxrs.ui.internal.validation;

import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PATH_PARAM;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsBaseElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.jdt.SourceType;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsParamConverterProvider;
import org.jboss.tools.ws.jaxrs.ui.preferences.JaxrsPreferences;

/**
 * @author xcoulon
 *
 */
public abstract class JaxrsResourceElementValidatorDelegate<T extends JaxrsResourceElement<?>> extends AbstractJaxrsElementValidatorDelegate<T> {

	/**
	 * @param markerManager
	 */
	public JaxrsResourceElementValidatorDelegate(IMarkerManager markerManager) {
		super(markerManager);
	}

	/**
	 * Validate that the {@code @PathParam} annotation value matches a given
	 * {@code @Path} template parameter at the parent {@link JaxrsResource}
	 * level or on any sibling {@link JaxrsResourceMethod}.
	 * 
	 * @param resourceElement the resource field to validate.
	 * @param ast the associated compilation unit
	 * @throws CoreException 
	 */
	void validateNotUnboundPathParamAnnotationValue(final T resourceElement, final CompilationUnit ast) throws CoreException {
		final Annotation pathParamAnnotation = resourceElement.getAnnotation(PATH_PARAM);
		if(pathParamAnnotation == null) {
			return;
		}
		final JaxrsResource parentResource = resourceElement.getParentResource();
		final Set<String> pathTemplateParameters = new HashSet<String>();
		// put all path template parameters at the parent resource level
		pathTemplateParameters.addAll(parentResource.getPathTemplateParameters().keySet());
		// also include all template parameters at the sibling resource methods level
		final Collection<JaxrsResourceMethod> resourceMethods = parentResource.getMethods().values();
		for(JaxrsResourceMethod resourceMethod : resourceMethods) {
			pathTemplateParameters.addAll(resourceMethod.getPathTemplateParameters().keySet());
		}
		// now, check:
		if(!pathTemplateParameters.contains(pathParamAnnotation.getValue())) {
			final ISourceRange annotationValueRange = JdtUtils.resolveMemberPairValueRange(pathParamAnnotation.getJavaAnnotation(), Annotation.VALUE, ast);
			markerManager.addMarker((JaxrsBaseElement)resourceElement, annotationValueRange,
					JaxrsValidationMessages.RESOURCE_FIELD_UNBOUND_PATHPARAM_ANNOTATION_VALUE,
					new String[] { pathParamAnnotation.getValue(), parentResource.getJavaElement().getFullyQualifiedName() },
					JaxrsPreferences.RESOURCE_ELEMENT_UNBOUND_PATHPARAM_ANNOTATION_VALUE);
		}
	}

	/**
	 * Validates the type of the parameter when annotated with {@code @PathParam},
	 * {@code @QueryParam} and {@code @MatrixParam}.
	 * 
	 * @param resourceElement
	 *            the resource field to validate
	 * @throws CoreException
	 * @see JaxrsParameterValidatorDelegate
	 */
	void validateParameterType(final T resourceElement) throws CoreException {
		// for now, we bypass this validation if the metamodel has at least one ParamConverterProvider
		final Collection<IJaxrsParamConverterProvider> allParamConverterProviders = resourceElement.getMetamodel().findAllParamConverterProviders();
		if(allParamConverterProviders != null && ! allParamConverterProviders.isEmpty()) {
			return;
		}
		
		final JaxrsParameterValidatorDelegate parameterValidatorDelegate = new JaxrsParameterValidatorDelegate();
		final SourceType type = resourceElement.getType();
		// skip if the type does not exist, there will already be a compilation error reported by JDT.
		if(!type.exists()) {
			return;
		}
		final boolean isValid = parameterValidatorDelegate.validate(type);
		if (!isValid) {
			markerManager.addMarker((JaxrsBaseElement)resourceElement, resourceElement.getJavaElement().getNameRange(),
					JaxrsValidationMessages.RESOURCE_METHOD_INVALID_ANNOTATED_PARAMETER_TYPE,
					new String[] { type.getErasureName() },
					JaxrsPreferences.RESOURCE_METHOD_INVALID_ANNOTATED_PARAMETER_TYPE);
		}
	}
}
