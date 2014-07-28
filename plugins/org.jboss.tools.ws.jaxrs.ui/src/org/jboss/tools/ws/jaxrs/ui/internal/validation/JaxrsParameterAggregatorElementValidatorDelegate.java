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

import static org.jboss.tools.ws.jaxrs.core.jdt.Annotation.VALUE;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.BEAN_PARAM;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PATH_PARAM;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsParameterAggregatorElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.search.JavaElementsSearcher;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementCategory;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJavaMethodParameter;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResource;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.ui.preferences.JaxrsPreferences;

/**
 * @author xcoulon
 *
 */
@SuppressWarnings("restriction")
public abstract class JaxrsParameterAggregatorElementValidatorDelegate<T extends JaxrsParameterAggregatorElement<?>> extends AbstractJaxrsElementValidatorDelegate<T> {

	/**
	 * Constructor
	 * @param markerManager the underlying marker manager to use
	 */
	public JaxrsParameterAggregatorElementValidatorDelegate(final IMarkerManager markerManager) {
		super(markerManager);
	}

	void validatePathParamAnnotation(final JaxrsParameterAggregatorElement<?> aggregatorElement, final CompilationUnit ast) throws CoreException {
		final Annotation pathParamAnnotation = aggregatorElement.getAnnotation(PATH_PARAM);
		if (pathParamAnnotation != null && pathParamAnnotation.getValue() != null) {
			final String pathParamValue = pathParamAnnotation.getValue();
			if (!alphaNumPattern.matcher(pathParamValue).matches()) {
				final ISourceRange range = JdtUtils.resolveMemberPairValueRange(
						pathParamAnnotation.getJavaAnnotation(), VALUE, ast);
				markerManager.addMarker(aggregatorElement, range,
						JaxrsValidationMessages.RESOURCE_METHOD_INVALID_PATHPARAM_ANNOTATION_VALUE,
						new String[] { pathParamValue },
						JaxrsPreferences.RESOURCE_METHOD_INVALID_PATHPARAM_ANNOTATION_VALUE);
			} 
		}		
	}
	
	/**
	 * @param element
	 * @throws CoreException
	 */
	void validateNoUnboundPathAnnotationTemplateParameters(final T element, final CompilationUnit ast)
			throws CoreException {
		// skip if the element to validate has no @PathParam annotation.
		if(!element.hasAnnotation(PATH_PARAM)) {
			return;
		}
		final JaxrsMetamodel metamodel = element.getMetamodel();
		final List<IType> knownTypes = metamodel.getAllJavaElements(IJavaElement.TYPE);
		final IType parentType = element.getJavaElement().getDeclaringType();
		final Set<IType> relatedTypes = JavaElementsSearcher.findRelatedTypes(parentType, knownTypes, new NullProgressMonitor());
		for(IType relatedType : relatedTypes) {
			final JaxrsResource relatedResource = (JaxrsResource) metamodel.findElement(relatedType.getFullyQualifiedName(), EnumElementCategory.RESOURCE);
			if(relatedResource == null) {
				continue;
			}
			for(IJaxrsResourceMethod resourceMethod : relatedResource.getAllMethods()) {
				for(IJavaMethodParameter methodParameter: resourceMethod.getJavaMethodParameters()) {
					if(methodParameter.hasAnnotation(BEAN_PARAM)) {
						validateNoUnboundPathAnnotationTemplateParameters(element, resourceMethod, ast);
					}
				}
			}
		}
	}

	/**
	 * Validates that the given {@code parameterAggregatorField}'s {@code @PathParam} annotation value matches a template parameter in the given {@link JaxrsResourceMethod} or its parent {@link JaxrsResource}.
	 * @param parameterAggregatorElement the {@link JaxrsParameterAggregatorElement} to validate 
	 * @param resourceMethod the {@link JaxrsResourceMethod} used during the validation
	 * @throws CoreException 
	 */
	private void validateNoUnboundPathAnnotationTemplateParameters(final T parameterAggregatorElement,
			final IJaxrsResourceMethod resourceMethod, final CompilationUnit ast) throws CoreException {
		final Annotation pathParamAnnotation = parameterAggregatorElement.getAnnotation(PATH_PARAM);
		final String pathParamValue = pathParamAnnotation.getValue();
		final Map<String, Annotation> resourceMethodPathTemplateParameters = resourceMethod.getPathTemplateParameters();
		// check if @PathParam annotation value matches an @Path template parameter at the Resource Method level.
		if(resourceMethodPathTemplateParameters.containsKey(pathParamValue)) {
			return;
		}
		// check if @PathParam annotation value matches an @Path template parameter at the parent Resource level.
		final IJaxrsResource parentResource = resourceMethod.getParentResource();
		final Map<String, Annotation> parentResourcePathTemplateParameters = parentResource.getPathTemplateParameters();
		if(parentResourcePathTemplateParameters.containsKey(pathParamValue)) {
			return;
		}
		// report a problem
		final ISourceRange range = JdtUtils.resolveMemberPairValueRange(pathParamAnnotation.getJavaAnnotation(), Annotation.VALUE, ast);
		markerManager.addMarker(
				parameterAggregatorElement,
				range,
				JaxrsValidationMessages.RESOURCE_FIELD_UNBOUND_PATHPARAM_ANNOTATION_VALUE,
				new String[] { pathParamValue, parentResource.getJavaElement().getFullyQualifiedName()},
				JaxrsPreferences.RESOURCE_ELEMENT_UNBOUND_PATHPARAM_ANNOTATION_VALUE);
	}

}
