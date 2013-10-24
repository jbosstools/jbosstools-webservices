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
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.SourceRange;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.JavaMethodParameter;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.preferences.JaxrsPreferences;

/**
 * JAX-RS Resource Method validator.
 * 
 * @author Xavier Coulon
 * 
 */
public class JaxrsResourceMethodValidatorDelegate extends AbstractJaxrsElementValidatorDelegate<JaxrsResourceMethod> {

	/**
	 * The parameter type names that can be annotated with <code>Context</code>.
	 */
	private final static List<String> CONTEXT_TYPE_NAMES = new ArrayList<String>(Arrays.asList(
			"javax.ws.rs.core.HttpHeaders", "javax.ws.rs.core.UriInfo", "javax.ws.rs.core.Request",
			"javax.servlet.http.HttpServletRequest", "javax.servlet.http.HttpServletResponse",
			"javax.servlet.ServletConfig", "javax.servlet.ServletContext", "javax.ws.rs.core.SecurityContext"));

	private static final Pattern alphaNumPattern = Pattern.compile("[a-zA-Z1-9]+");

	private final IMarkerManager markerManager;
	
	public JaxrsResourceMethodValidatorDelegate(final IMarkerManager markerManager) {
		this.markerManager = markerManager;
	}

	/**
	 * @throws CoreException 
	 * @see org.jboss.tools.ws.jaxrs.core.internal.metamodel.validation.
	 * AbstractJaxrsElementValidatorDelegate#internalValidate(Object)
	 */
	@Override
	void internalValidate(final JaxrsResourceMethod resourceMethod) throws CoreException {
		Logger.debug("Validating element {}", resourceMethod);
		validatePublicModifierOnJavaMethod(resourceMethod);
		validateNoUnboundPathAnnotationTemplateParameters(resourceMethod);
		validateNoUnboundPathParamAnnotationValues(resourceMethod);
		validateNoUnauthorizedContextAnnotationOnJavaMethodParameters(resourceMethod);
		validateAtMostOneMethodParameterWithoutAnnotation(resourceMethod);
	}

	/**
	 * Validate that at most one method parameter is not annotated with a JAX-RS
	 * annotation. This non-annotated parameter is the "Entity parameter",
	 * coming from the client's request body, unmarshalled by the appropriate
	 * {@link MesssageBodyReader}.
	 * 
	 * @return
	 * @throws CoreException 
	 */
	private void validateAtMostOneMethodParameterWithoutAnnotation(final JaxrsResourceMethod resourceMethod)
			throws CoreException {
		int counter = 0;
		for (JavaMethodParameter parameter : resourceMethod.getJavaMethodParameters()) {
			// Should count parameters annotated with:
			// @MatrixParam, @QueryParam, @PathParam, @CookieParam,
			// @HeaderParam, @Context or @FormParam
			final Map<String, Annotation> jaxrsAnnotations = parameter.getAnnotations();
			if (jaxrsAnnotations.size() == 0) {
				counter++;
			}
		}
		if (counter > 1) {
			final ISourceRange nameRange = resourceMethod.getJavaElement().getNameRange();
			markerManager.addMarker(resourceMethod,
					nameRange, JaxrsValidationMessages.RESOURCE_METHOD_MORE_THAN_ONE_UNANNOTATED_PARAMETER,
					new String[0], JaxrsPreferences.RESOURCE_METHOD_MORE_THAN_ONE_UNANNOTATED_PARAMETER);
		}
	}

	/**
	 * Validates that the method parameters annotated with <code>Context</code>
	 * are of the supported types in the spec: <code>UriInfo</code>,
	 * <code>HttpHeaders<code>, <code>ServletConfig</code>,
	 * <code>ServletContext</code>, <code>HttpServletRequest</code> ,
	 * <code>Request</code>, <code>HttpServletResponse</code> and
	 * <code>@link Response</code>.
	 * 
	 * @return
	 * @throws CoreException 
	 */
	private void validateNoUnauthorizedContextAnnotationOnJavaMethodParameters(final JaxrsResourceMethod resourceMethod)
			throws CoreException {
		for (JavaMethodParameter parameter : resourceMethod.getJavaMethodParameters()) {
			final Annotation contextAnnotation = parameter.getAnnotation(CONTEXT.qualifiedName);
			final String typeName = parameter.getTypeName();
			if (contextAnnotation != null && typeName != null && !CONTEXT_TYPE_NAMES.contains(typeName)) {
				final ISourceRange range = contextAnnotation.getJavaAnnotation().getSourceRange();
				markerManager.addMarker(resourceMethod,
						range,
						JaxrsValidationMessages.RESOURCE_METHOD_ILLEGAL_CONTEXT_ANNOTATION, new String[] { CONTEXT_TYPE_NAMES.toString() },
						JaxrsPreferences.RESOURCE_METHOD_ILLEGAL_CONTEXT_ANNOTATION);
			}
		}
	}

	/**
	 * Checks that there is no unbound Path template parameter in the
	 * <code>@Path</code> annotations by checking the method @PathParam
	 * annotated parameters. Report a problem if a Path template parameter has
	 * no equivalent in the java method's parameters.
	 * 
	 * @return errors in case of mismatch, empty list otherwise.
	 * @throws CoreException 
	 */
	private void validateNoUnboundPathAnnotationTemplateParameters(final JaxrsResourceMethod resourceMethod)
			throws CoreException {
		final Map<String, Annotation> pathParamValueProposals = resourceMethod.getPathParamValueProposals();
		final List<String> pathParamValues = new ArrayList<String>();
		// retrieve all @Path
		for (JavaMethodParameter parameter : resourceMethod.getJavaMethodParameters()) {
			final Annotation annotation = parameter.getAnnotation(PATH_PARAM.qualifiedName);
			if (annotation != null && annotation.getValue() != null) {
				pathParamValues.add(annotation.getValue());
			}
		}
		for (Entry<String, Annotation> pathTemplateParameterEntry : pathParamValueProposals.entrySet()) {
			final String pathTemplateParameter = pathTemplateParameterEntry.getKey();
			if (!pathParamValues.contains(pathTemplateParameter)) {
				final Annotation pathTemplateParameterAnnotation = pathTemplateParameterEntry.getValue();
				// look-up source range for annotation value
				final ISourceRange range = resolveAnnotationParamSourceRange(pathTemplateParameterAnnotation,
						pathTemplateParameter);
				markerManager.addMarker(
						resourceMethod,
						range,
						JaxrsValidationMessages.RESOURCE_METHOD_UNBOUND_PATH_ANNOTATION_TEMPLATE_PARAMETER,
						new String[] { pathTemplateParameter,
								JdtUtils.getReadableMethodSignature(resourceMethod.getJavaElement()) }, JaxrsPreferences.RESOURCE_METHOD_UNBOUND_PATH_ANNOTATION_TEMPLATE_PARAMETER);
			}
		}
	}

	/**
	 * Report a problem for each <code>@PathParam</code> annotation value that
	 * have no counterpart in the <code>@Path</code> template parameters.
	 * 
	 * @return
	 * @throws CoreException 
	 */
	private void validateNoUnboundPathParamAnnotationValues(final JaxrsResourceMethod resourceMethod)
			throws CoreException {
		final Map<String, Annotation> pathParamValueProposals = resourceMethod.getPathParamValueProposals();
		for (JavaMethodParameter parameter : resourceMethod.getJavaMethodParameters()) {
			final Annotation pathParamAnnotation = parameter.getAnnotation(PATH_PARAM.qualifiedName);
			if (pathParamAnnotation != null) {
				final String pathParamValue = pathParamAnnotation.getValue("value");
				if (pathParamValue != null) {
					if (!alphaNumPattern.matcher(pathParamValue).matches()) {
						final ISourceRange range = JdtUtils.resolveMemberPairValueRange(
								pathParamAnnotation.getJavaAnnotation(), "value");
						markerManager.addMarker(
								resourceMethod,
								range,
								JaxrsValidationMessages.RESOURCE_METHOD_UNBOUND_PATHPARAM_ANNOTATION_VALUE, new String[] { pathParamValue },
								JaxrsPreferences.RESOURCE_METHOD_UNBOUND_PATHPARAM_ANNOTATION_VALUE);
					} else if (!pathParamValueProposals.keySet().contains(pathParamValue)) {
						final ISourceRange range = JdtUtils.resolveMemberPairValueRange(
								pathParamAnnotation.getJavaAnnotation(), "value");
						markerManager.addMarker(
								resourceMethod,
								range,
								JaxrsValidationMessages.RESOURCE_METHOD_UNBOUND_PATHPARAM_ANNOTATION_VALUE, new String[] { pathParamValue },
								JaxrsPreferences.RESOURCE_METHOD_UNBOUND_PATHPARAM_ANNOTATION_VALUE);
					}
				}
			}
		}
	}

	/**
	 * Resolves the location of the Path parameter in the source range, narrowed
	 * to the minimal value. For instance, the returned range corresponds to the
	 * location of the path parameter, including curly brackets and the the
	 * optional regexp in the given <code>@Path</code> annotation
	 * 
	 * @param pathTemplateParameterAnnotation
	 *            the <code>@Path</code> annotation
	 * @param pathTemplateParameter
	 *            the parameter that should be found in the given annotation
	 *            value
	 * @return
	 * @throws JavaModelException
	 */
	private ISourceRange resolveAnnotationParamSourceRange(final Annotation pathTemplateParameterAnnotation,
			final String pathTemplateParameter) throws JavaModelException {
		// refine source range for path parameter in the value (including
		// whitespaces between starting curly bracket and param name)
		final ISourceRange valueRange = JdtUtils.resolveMemberPairValueRange(
				pathTemplateParameterAnnotation.getJavaAnnotation(), "value");
		final String annotationValue = pathTemplateParameterAnnotation.getValue();
		final Pattern p = Pattern.compile("\\{\\s*" + Pattern.quote(pathTemplateParameter));
		final Matcher matcher = p.matcher(annotationValue);
		if (matcher.find()) {
			final int start = matcher.start();
			final int end = annotationValue.indexOf("}", start);
			return new SourceRange(valueRange.getOffset() + start + 1, end - start + 1);
		}
		return valueRange;
	}

	/**
	 * As per specification, the java method should have a public modifier.
	 * 
	 * @param resourceMethod
	 * @throws CoreException 
	 */
	private void validatePublicModifierOnJavaMethod(final JaxrsResourceMethod resourceMethod) throws CoreException {
		final IMethod javaMethod = resourceMethod.getJavaElement();
		if (javaMethod != null && !Flags.isPublic(javaMethod.getFlags())) {
			final ISourceRange nameRange = javaMethod.getNameRange();
			markerManager.addMarker(resourceMethod,
					nameRange, JaxrsValidationMessages.RESOURCE_METHOD_NO_PUBLIC_MODIFIER, new String[0], JaxrsPreferences.RESOURCE_METHOD_NO_PUBLIC_MODIFIER);
		}
	}

}
