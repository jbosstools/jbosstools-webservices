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
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.validation;

import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.CONTEXT;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.PATH_PARAM;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.jboss.tools.common.validation.TempMarkerManager;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.JavaMethodParameter;
import org.jboss.tools.ws.jaxrs.core.preferences.JaxrsPreferences;

/**
 * JAX-RS Resource Method validator.
 * 
 * @author Xavier Coulon
 * 
 */
public class JaxrsResourceMethodValidatorDelegate extends AbstractJaxrsElementValidatorDelegate<JaxrsResourceMethod> {

	/** The parameter type names that can be annotated with <code>Context</code>. */
	private final static List<String> CONTEXT_TYPE_NAMES = new ArrayList<String>(Arrays.asList(
			"javax.ws.rs.core.HttpHeaders", "javax.ws.rs.core.UriInfo", "javax.ws.rs.core.Request",
			"javax.servlet.http.HttpServletRequest", "javax.servlet.http.HttpServletResponse",
			"javax.servlet.ServletConfig", "javax.servlet.ServletContext", "javax.ws.rs.core.SecurityContext"));

	private static final Pattern pattern = Pattern.compile("[a-zA-Z1-9]+");

	public JaxrsResourceMethodValidatorDelegate(final TempMarkerManager markerManager,
			final JaxrsResourceMethod resourceMethod) {
		super(markerManager, resourceMethod);

	}

	@Override
	public void validate() {
		final JaxrsResourceMethod resourceMethod = getElement();
		try {
			resourceMethod.hasErrors(false);
			validatePublicModifierOnJavaMethod(resourceMethod);
			validateNoUnboundPathAnnotationTemplateParameters(resourceMethod);
			validateNoUnboundPathParamAnnotationValues(resourceMethod);
			validateNoUnauthorizedContextAnnotationOnJavaMethodParameters(resourceMethod);
			validateAtMostOneMethodParameterWithoutAnnotation(resourceMethod);
		} catch (JavaModelException e) {
			Logger.error("Failed to validate JAX-RS Resource Method '" + resourceMethod.getName() + "'", e);
		}
	}

	/**
	 * Validate that at most one method parameter is not annotated with a JAX-RS annotation. This non-annotated
	 * parameter is the "Entity parameter", coming from the client's request body, unmarshalled by the appropriate
	 * {@link MesssageBodyReader}.
	 * 
	 * @return
	 * @throws JavaModelException
	 */
	private void validateAtMostOneMethodParameterWithoutAnnotation(final JaxrsResourceMethod resourceMethod)
			throws JavaModelException {
		int counter = 0;
		for (JavaMethodParameter parameter : resourceMethod.getJavaMethodParameters()) {
			// Should count parameters annotated with:
			// @MatrixParam, @QueryParam, @PathParam, @CookieParam, @HeaderParam, @Context or @FormParam
			final List<Annotation> jaxrsAnnotations = parameter.getAnnotations();
			if (jaxrsAnnotations.size() == 0) {
				counter++;
			}
		}
		if (counter > 1) {
			final ISourceRange nameRange = resourceMethod.getJavaElement().getNameRange();
			addProblem(JaxrsValidationMessages.RESOURCE_METHOD_MORE_THAN_ONE_UNANNOTATED_PARAMETER,
					JaxrsPreferences.RESOURCE_METHOD_MORE_THAN_ONE_UNANNOTATED_PARAMETER, new String[0],
					nameRange.getLength(), nameRange.getOffset(), resourceMethod.getResource());
			resourceMethod.hasErrors(true);
		}
	}

	/**
	 * Validates that the method parameters annotated with <code>Context</code> are of the supported types in the spec:
	 * <code>UriInfo</code>, <code>HttpHeaders<code>, <code>ServletConfig</code>, <code>ServletContext</code>,
	 * <code>HttpServletRequest</code> , <code>Request</code>, <code>HttpServletResponse</code> and
	 * <code>@link Response</code>.
	 * 
	 * @return
	 * @throws JavaModelException
	 */
	private void validateNoUnauthorizedContextAnnotationOnJavaMethodParameters(final JaxrsResourceMethod resourceMethod) {
		for (JavaMethodParameter parameter : resourceMethod.getJavaMethodParameters()) {
			final Annotation contextAnnotation = parameter.getAnnotation(CONTEXT.qualifiedName);
			final String typeName = parameter.getTypeName();
			if (contextAnnotation != null && typeName != null && !CONTEXT_TYPE_NAMES.contains(typeName)) {
				addProblem(JaxrsValidationMessages.RESOURCE_METHOD_ILLEGAL_CONTEXT_ANNOTATION,
						JaxrsPreferences.RESOURCE_METHOD_ILLEGAL_CONTEXT_ANNOTATION,
						new String[] { CONTEXT_TYPE_NAMES.toString() }, contextAnnotation.getSourceRange().getLength(),
						contextAnnotation.getSourceRange().getOffset(), resourceMethod.getResource());
				resourceMethod.hasErrors(true);
			}
		}
	}

	/**
	 * Checks that there is no unbound Path template parameter in the <code>@Path</code> annotations by checking the
	 * method @PathParam annotated parameters. Report a problem if a Path template parameter has no equivalent in the
	 * java method's parameters.
	 * 
	 * @return errors in case of mismatch, empty list otherwise.
	 * @throws JavaModelException
	 */
	private void validateNoUnboundPathAnnotationTemplateParameters(final JaxrsResourceMethod resourceMethod)
			throws JavaModelException {
		final List<String> pathParamValueProposals = resourceMethod.getPathParamValueProposals();
		final List<String> pathParamValues = new ArrayList<String>();
		for (JavaMethodParameter parameter : resourceMethod.getJavaMethodParameters()) {
			final Annotation annotation = parameter.getAnnotation(PATH_PARAM.qualifiedName);
			if (annotation != null && annotation.getValue() != null) {
				pathParamValues.add(annotation.getValue());
			}
		}
		final ISourceRange nameRange = resourceMethod.getJavaElement().getNameRange();
		for (String pathTemplateParameter : pathParamValueProposals) {
			if (!pathParamValues.contains(pathTemplateParameter)) {
				addProblem(JaxrsValidationMessages.RESOURCE_METHOD_UNBOUND_PATH_ANNOTATION_TEMPLATE_PARAMETER,
						JaxrsPreferences.RESOURCE_METHOD_UNBOUND_PATH_ANNOTATION_TEMPLATE_PARAMETER,
						new String[] { pathTemplateParameter }, nameRange.getLength(), nameRange.getOffset(),
						resourceMethod.getResource());
				resourceMethod.hasErrors(true);
			}
		}
	}

	/**
	 * Report a problem for each <code>@PathParam</code> annotation value that have no counterpart in the
	 * <code>@Path</code> template parameters.
	 * 
	 * @return
	 * @throws JavaModelException
	 */
	private void validateNoUnboundPathParamAnnotationValues(final JaxrsResourceMethod resourceMethod)
			throws JavaModelException {
		final List<String> pathParamValueProposals = resourceMethod.getPathParamValueProposals();
		for (JavaMethodParameter parameter : resourceMethod.getJavaMethodParameters()) {
			final Annotation annotation = parameter.getAnnotation(PATH_PARAM.qualifiedName);
			if (annotation != null) {
				final String pathParamValue = annotation.getValue("value");
				if (pathParamValue != null) {
					if (!pattern.matcher(pathParamValue).matches()) {
						final ISourceRange sourceRange = annotation.getSourceRange();
						addProblem(JaxrsValidationMessages.RESOURCE_METHOD_UNBOUND_PATHPARAM_ANNOTATION_VALUE,
								JaxrsPreferences.RESOURCE_METHOD_UNBOUND_PATHPARAM_ANNOTATION_VALUE,
								new String[] { pathParamValue }, sourceRange.getLength(), sourceRange.getOffset(),
								resourceMethod.getResource());
						resourceMethod.hasErrors(true);
					} else if (!pathParamValueProposals.contains(pathParamValue)) {
						final ISourceRange sourceRange = annotation.getSourceRange();
						addProblem(JaxrsValidationMessages.RESOURCE_METHOD_UNBOUND_PATHPARAM_ANNOTATION_VALUE,
								JaxrsPreferences.RESOURCE_METHOD_UNBOUND_PATHPARAM_ANNOTATION_VALUE,
								new String[] { pathParamValue }, sourceRange.getLength(), sourceRange.getOffset(),
								resourceMethod.getResource());
						resourceMethod.hasErrors(true);
					}
				}
			}
		}
	}

	private void validatePublicModifierOnJavaMethod(final JaxrsResourceMethod resourceMethod) throws JavaModelException {
		final IMethod javaMethod = resourceMethod.getJavaElement();
		if (javaMethod != null && !Flags.isPublic(javaMethod.getFlags())) {
			final ISourceRange nameRange = javaMethod.getNameRange();
			addProblem(JaxrsValidationMessages.RESOURCE_METHOD_NO_PUBLIC_MODIFIER,
					JaxrsPreferences.RESOURCE_METHOD_NO_PUBLIC_MODIFIER, new String[0], nameRange.getLength(),
					nameRange.getOffset(), resourceMethod.getResource());
		}
	}

}
