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

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ISourceRange;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.CollectionUtils;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.ui.preferences.JaxrsPreferences;

/**
 * HTTP Method validator.
 * 
 * @author Xavier Coulon
 * 
 */
public class JaxrsHttpMethodValidatorDelegate extends AbstractJaxrsElementValidatorDelegate<JaxrsHttpMethod> {

	/**
	 * Constructor
	 * @param markerManager the underlying marker manager to use
	 */
	public JaxrsHttpMethodValidatorDelegate(final IMarkerManager markerManager) {
		super(markerManager);
	}

	/**
	 * @see org.jboss.tools.ws.jaxrs.ui.internal.validation.AbstractJaxrsElementValidatorDelegate#internalValidate(Object)
	 */
	@Override
	void internalValidate(final JaxrsHttpMethod httpMethod) throws CoreException {
		Logger.debug("Validating element {}", httpMethod);
		if (!httpMethod.isBuiltIn()) {
			validateHttpMethodAnnotation(httpMethod);
			validateRetentionAnnotation(httpMethod);
			validateTargetAnnotation(httpMethod);
		}
	}

	/**
	 * Validates that annotation value is not null nor empty
	 * 
	 * @param messages
	 * @throws CoreException
	 */
	private void validateHttpMethodAnnotation(final JaxrsHttpMethod httpMethod) throws CoreException {
		final Annotation httpMethodAnnotation = httpMethod.getHttpMethodAnnotation();
		// if annotation is null, the resource is not a JaxrsHttpMethod anymore.
		if (httpMethodAnnotation != null) {
			final String httpValue = httpMethodAnnotation.getValue();
			if (httpValue != null && httpValue.isEmpty()) {
				final ISourceRange range = JdtUtils.resolveMemberPairValueRange(
						httpMethodAnnotation.getJavaAnnotation(), VALUE);
				markerManager.addMarker(httpMethod, range,
						JaxrsValidationMessages.HTTP_METHOD_INVALID_HTTP_METHOD_ANNOTATION_VALUE, new String[0],
						JaxrsPreferences.HTTP_METHOD_INVALID_HTTP_METHOD_ANNOTATION_VALUE);
			}
		}
	}

	/**
	 * Validate that annotation exists and value is Target.METHOD
	 * 
	 * @param messages
	 * @throws CoreException
	 */
	private void validateTargetAnnotation(final JaxrsHttpMethod httpMethod) throws CoreException {
		final Annotation targetAnnotation = httpMethod.getTargetAnnotation();
		if (targetAnnotation == null) {
			final ISourceRange range = httpMethod.getJavaElement().getNameRange();
			markerManager.addMarker(httpMethod, range, JaxrsValidationMessages.HTTP_METHOD_MISSING_TARGET_ANNOTATION,
					new String[0], JaxrsPreferences.HTTP_METHOD_MISSING_TARGET_ANNOTATION,
					JaxrsMarkerResolutionIds.HTTP_METHOD_MISSING_TARGET_ANNOTATION_QUICKFIX_ID);
		} else {
			final List<String> annotationValues = targetAnnotation.getValues();
			final List<String> expectedValue = Arrays.asList(ElementType.METHOD.name());
			if (!CollectionUtils.containsInAnyOrder(annotationValues, expectedValue)) {
				final ISourceRange range = JdtUtils.resolveMemberPairValueRange(targetAnnotation.getJavaAnnotation(),
						VALUE);
				markerManager.addMarker(httpMethod, range,
						JaxrsValidationMessages.HTTP_METHOD_INVALID_TARGET_ANNOTATION_VALUE, new String[0],
						JaxrsPreferences.HTTP_METHOD_INVALID_TARGET_ANNOTATION_VALUE,
						JaxrsMarkerResolutionIds.HTTP_METHOD_INVALID_TARGET_ANNOTATION_VALUE_QUICKFIX_ID);
			}
		}
	}

	/**
	 * Validate that annotation exists and value is Retention.RUNTIME
	 * 
	 * @param messages
	 * @throws CoreException
	 */
	private void validateRetentionAnnotation(final JaxrsHttpMethod httpMethod) throws CoreException {
		final Annotation retentionAnnotation = httpMethod.getRetentionAnnotation();
		if (retentionAnnotation == null) {
			final ISourceRange range = httpMethod.getJavaElement().getNameRange();
			markerManager.addMarker(httpMethod, range,
					JaxrsValidationMessages.HTTP_METHOD_MISSING_RETENTION_ANNOTATION, new String[0],
					JaxrsPreferences.HTTP_METHOD_MISSING_RETENTION_ANNOTATION,
					JaxrsMarkerResolutionIds.HTTP_METHOD_MISSING_RETENTION_ANNOTATION_QUICKFIX_ID);
		} else {
			final String annotationValue = retentionAnnotation.getValue();
			if (annotationValue != null && !annotationValue.equals(RetentionPolicy.RUNTIME.name())) {
				final ISourceRange range = JdtUtils.resolveMemberPairValueRange(
						retentionAnnotation.getJavaAnnotation(), VALUE);
				markerManager.addMarker(httpMethod, range,
						JaxrsValidationMessages.HTTP_METHOD_INVALID_RETENTION_ANNOTATION_VALUE, new String[0],
						JaxrsPreferences.HTTP_METHOD_INVALID_RETENTION_ANNOTATION_VALUE,
						JaxrsMarkerResolutionIds.HTTP_METHOD_INVALID_RETENTION_ANNOTATION_VALUE_QUICKFIX_ID);

			}
		}
	}

}
