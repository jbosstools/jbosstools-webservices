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

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaModelException;
import org.jboss.tools.common.validation.TempMarkerManager;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.metamodel.quickfix.JaxrsValidationQuickFixes;
import org.jboss.tools.ws.jaxrs.core.preferences.JaxrsPreferences;

/**
 * HTTP Method validator.
 * 
 * @author Xavier Coulon
 * 
 */
public class JaxrsHttpMethodValidatorDelegate extends AbstractJaxrsElementValidatorDelegate<JaxrsHttpMethod> {

	public JaxrsHttpMethodValidatorDelegate(TempMarkerManager markerManager, JaxrsHttpMethod element) {
		super(markerManager, element);
	}

	@Override
	public void validate() throws CoreException {
		final JaxrsHttpMethod httpMethod = getElement();
		JaxrsMetamodelValidator.deleteJaxrsMarkers(httpMethod);
		Logger.debug("Validating element {}", getElement());
		if (!httpMethod.isBuiltIn()) {
			validateHttpMethodAnnotation(httpMethod);
			validateRetentionAnnotation(getElement());
			validateTargetAnnotation(getElement());
		}
	}

	/**
	 * Validates that annotation value is not null nor empty
	 * 
	 * @param messages
	 * @throws JavaModelException
	 */
	private void validateHttpMethodAnnotation(final JaxrsHttpMethod httpMethod) {
		final Annotation annotation = httpMethod.getHttpMethodAnnotation();
		if (annotation != null) { // if annotation is null, the resource is not a JaxrsHttpMethod anymore.
			final String httpValue = annotation.getValue("value");
			if (httpValue == null || httpValue.isEmpty()) {
				addProblem(JaxrsValidationMessages.HTTP_METHOD_INVALID_HTTP_METHOD_ANNOTATION_VALUE,
						JaxrsPreferences.HTTP_METHOD_INVALID_HTTP_METHOD_ANNOTATION_VALUE, new String[0], annotation
								.getSourceRange().getLength(), annotation.getSourceRange().getOffset(),
						httpMethod.getResource());
			}
		}
	}

	/**
	 * Validate that annotation exists and value is Target.METHOD
	 * 
	 * @param messages
	 * @throws JavaModelException
	 */
	private void validateTargetAnnotation(final JaxrsHttpMethod httpMethod) {
		final Annotation targetAnnotation = httpMethod.getTargetAnnotation();
		final Annotation httpMethodAnnotation = httpMethod.getHttpMethodAnnotation();
		if (targetAnnotation == null) {
			addProblem(JaxrsValidationMessages.HTTP_METHOD_MISSING_TARGET_ANNOTATION,
					JaxrsPreferences.HTTP_METHOD_MISSING_TARGET_ANNOTATION, new String[0], httpMethodAnnotation
							.getSourceRange().getLength(), httpMethodAnnotation.getSourceRange().getOffset(),
					httpMethod.getResource(), JaxrsValidationQuickFixes.HTTP_METHOD_MISSING_TARGET_ANNOTATION_ID);
		} else {
			final String annotationValue = targetAnnotation.getValue("value");
			if (annotationValue == null || !annotationValue.equals(ElementType.METHOD.name())) {
				addProblem(JaxrsValidationMessages.HTTP_METHOD_INVALID_TARGET_ANNOTATION_VALUE,
						JaxrsPreferences.HTTP_METHOD_INVALID_TARGET_ANNOTATION_VALUE, new String[0],
						httpMethodAnnotation.getSourceRange().getLength(), httpMethodAnnotation.getSourceRange()
								.getOffset(), httpMethod.getResource(),
						JaxrsValidationQuickFixes.HTTP_METHOD_INVALID_TARGET_ANNOTATION_VALUE_ID);
			}
		}
	}

	/**
	 * Validate that annotation exists and value is Retention.RUNTIME
	 * 
	 * @param messages
	 * @throws JavaModelException
	 */
	private void validateRetentionAnnotation(final JaxrsHttpMethod httpMethod) {
		final Annotation retentionAnnotation = httpMethod.getRetentionAnnotation();
		final Annotation httpMethodAnnotation = httpMethod.getHttpMethodAnnotation();
		if (retentionAnnotation == null) {
			addProblem(JaxrsValidationMessages.HTTP_METHOD_MISSING_RETENTION_ANNOTATION,
					JaxrsPreferences.HTTP_METHOD_MISSING_RETENTION_ANNOTATION, new String[0], httpMethodAnnotation
							.getSourceRange().getLength(), httpMethodAnnotation.getSourceRange().getOffset(),
					httpMethod.getResource(), JaxrsValidationQuickFixes.HTTP_METHOD_MISSING_RETENTION_ANNOTATION_ID);
		} else {
			final String annotationValue = retentionAnnotation.getValue("value");
			if (annotationValue == null || !annotationValue.equals(RetentionPolicy.RUNTIME.name())) {
				addProblem(JaxrsValidationMessages.HTTP_METHOD_INVALID_RETENTION_ANNOTATION_VALUE,
						JaxrsPreferences.HTTP_METHOD_INVALID_RETENTION_ANNOTATION_VALUE, new String[0],
						httpMethodAnnotation.getSourceRange().getLength(), httpMethodAnnotation.getSourceRange()
								.getOffset(), httpMethod.getResource(),
						JaxrsValidationQuickFixes.HTTP_METHOD_INVALID_RETENTION_ANNOTATION_VALUE_ID);
			}
		}
	}

}
