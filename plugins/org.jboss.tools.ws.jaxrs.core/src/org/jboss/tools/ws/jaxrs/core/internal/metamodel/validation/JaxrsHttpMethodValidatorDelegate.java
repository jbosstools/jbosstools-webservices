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
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.jboss.tools.common.validation.TempMarkerManager;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.validation.JaxrsMetamodelValidationConstants;
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
	private void validateHttpMethodAnnotation(final JaxrsHttpMethod httpMethod) throws JavaModelException {
		final Annotation httpMethodAnnotation = httpMethod.getHttpMethodAnnotation();
		if (httpMethodAnnotation != null) { // if annotation is null, the
											// resource is not a JaxrsHttpMethod
											// anymore.
			final String httpValue = httpMethodAnnotation.getValue("value");
			if (httpValue == null) {
				final ISourceRange range = httpMethodAnnotation.getJavaAnnotation().getNameRange();
				addProblem(JaxrsValidationMessages.HTTP_METHOD_INVALID_HTTP_METHOD_ANNOTATION_VALUE,
						JaxrsPreferences.HTTP_METHOD_INVALID_HTTP_METHOD_ANNOTATION_VALUE, new String[0],
						range.getLength(), range.getOffset(), httpMethod.getResource());
			} else if (httpValue.isEmpty()) {
				final ISourceRange range = JdtUtils.resolveMemberPairValueRange(
						httpMethodAnnotation.getJavaAnnotation(), "value");
				addProblem(JaxrsValidationMessages.HTTP_METHOD_INVALID_HTTP_METHOD_ANNOTATION_VALUE,
						JaxrsPreferences.HTTP_METHOD_INVALID_HTTP_METHOD_ANNOTATION_VALUE, new String[0],
						range.getLength(), range.getOffset(), httpMethod.getResource());
			}
		}
	}

	/**
	 * Validate that annotation exists and value is Target.METHOD
	 * 
	 * @param messages
	 * @throws JavaModelException
	 */
	private void validateTargetAnnotation(final JaxrsHttpMethod httpMethod) throws JavaModelException {
		final Annotation targetAnnotation = httpMethod.getTargetAnnotation();
		if (targetAnnotation == null) {
			final ISourceRange range = httpMethod.getJavaElement().getNameRange();
			addProblem(JaxrsValidationMessages.HTTP_METHOD_MISSING_TARGET_ANNOTATION,
					JaxrsPreferences.HTTP_METHOD_MISSING_TARGET_ANNOTATION, new String[0], range.getLength(),
					range.getOffset(), httpMethod.getResource(),
					JaxrsMetamodelValidationConstants.HTTP_METHOD_MISSING_TARGET_ANNOTATION_QUICKFIX_ID);
		} else {
			final String annotationValue = targetAnnotation.getValue("value");
			if (annotationValue == null) {
				final ISourceRange range = targetAnnotation.getJavaAnnotation().getNameRange();
				addProblem(JaxrsValidationMessages.HTTP_METHOD_INVALID_TARGET_ANNOTATION_VALUE,
						JaxrsPreferences.HTTP_METHOD_INVALID_TARGET_ANNOTATION_VALUE, new String[0], range.getLength(),
						range.getOffset(), httpMethod.getResource(),
						JaxrsMetamodelValidationConstants.HTTP_METHOD_INVALID_TARGET_ANNOTATION_VALUE_QUICKFIX_ID);
			} else if (!annotationValue.equals(ElementType.METHOD.name())) {
				final ISourceRange range = JdtUtils.resolveMemberPairValueRange(targetAnnotation.getJavaAnnotation(),
						"value");
				addProblem(JaxrsValidationMessages.HTTP_METHOD_INVALID_TARGET_ANNOTATION_VALUE,
						JaxrsPreferences.HTTP_METHOD_INVALID_TARGET_ANNOTATION_VALUE, new String[0], range.getLength(),
						range.getOffset(), httpMethod.getResource(),
						JaxrsMetamodelValidationConstants.HTTP_METHOD_INVALID_TARGET_ANNOTATION_VALUE_QUICKFIX_ID);

			}
		}
	}

	/**
	 * Validate that annotation exists and value is Retention.RUNTIME
	 * 
	 * @param messages
	 * @throws JavaModelException
	 */
	private void validateRetentionAnnotation(final JaxrsHttpMethod httpMethod) throws JavaModelException {
		final Annotation retentionAnnotation = httpMethod.getRetentionAnnotation();
		if (retentionAnnotation == null) {
			final ISourceRange range = httpMethod.getJavaElement().getNameRange();
			addProblem(JaxrsValidationMessages.HTTP_METHOD_MISSING_RETENTION_ANNOTATION,
					JaxrsPreferences.HTTP_METHOD_MISSING_RETENTION_ANNOTATION, new String[0], range.getLength(),
					range.getOffset(), httpMethod.getResource(),
					JaxrsMetamodelValidationConstants.HTTP_METHOD_MISSING_RETENTION_ANNOTATION_QUICKFIX_ID);
		} else {
			final String annotationValue = retentionAnnotation.getValue("value");
			if (annotationValue == null) {
				final ISourceRange range = retentionAnnotation.getJavaAnnotation().getNameRange();
				addProblem(JaxrsValidationMessages.HTTP_METHOD_INVALID_RETENTION_ANNOTATION_VALUE,
						JaxrsPreferences.HTTP_METHOD_INVALID_RETENTION_ANNOTATION_VALUE, new String[0],
						range.getLength(), range.getOffset(), httpMethod.getResource(),
						JaxrsMetamodelValidationConstants.HTTP_METHOD_INVALID_RETENTION_ANNOTATION_VALUE_QUICKFIX_ID);
			} else if (!annotationValue.equals(RetentionPolicy.RUNTIME.name())) {
				final ISourceRange range = JdtUtils.resolveMemberPairValueRange(
						retentionAnnotation.getJavaAnnotation(), "value");
				addProblem(JaxrsValidationMessages.HTTP_METHOD_INVALID_RETENTION_ANNOTATION_VALUE,
						JaxrsPreferences.HTTP_METHOD_INVALID_RETENTION_ANNOTATION_VALUE, new String[0],
						range.getLength(), range.getOffset(), httpMethod.getResource(),
						JaxrsMetamodelValidationConstants.HTTP_METHOD_INVALID_RETENTION_ANNOTATION_VALUE_QUICKFIX_ID);

			}
		}
	}

}
