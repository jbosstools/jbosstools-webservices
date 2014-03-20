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

import static org.jboss.tools.ws.jaxrs.core.utils.Annotation.VALUE;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ISourceRange;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsNameBinding;
import org.jboss.tools.ws.jaxrs.core.utils.Annotation;
import org.jboss.tools.ws.jaxrs.core.utils.JdtUtils;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.ui.preferences.JaxrsPreferences;

/**
 * HTTP Method validator.
 * 
 * @author Xavier Coulon
 * 
 */
public class JaxrsNameBindingValidatorDelegate extends AbstractJaxrsElementValidatorDelegate<JaxrsNameBinding> {

	private final IMarkerManager markerManager;
	
	public JaxrsNameBindingValidatorDelegate(final IMarkerManager markerManager) {
		this.markerManager = markerManager;
	}

	/**
	 * @see org.jboss.tools.ws.jaxrs.ui.internal.validation.AbstractJaxrsElementValidatorDelegate#internalValidate(Object)
	 */
	@Override
	void internalValidate(final JaxrsNameBinding nameBinding) throws CoreException {
		removeMarkers(nameBinding);
		Logger.debug("Validating element {}", nameBinding);
		if (!nameBinding.isBuiltIn()) {
			validateRetentionAnnotation(nameBinding);
			validateTargetAnnotation(nameBinding);
		}
	}

	/**
	 * Validate that annotation exists and value is Target.METHOD and Target.TYPE
	 * 
	 * @param messages
	 * @throws CoreException 
	 */
	private void validateTargetAnnotation(final JaxrsNameBinding nameBinding) throws CoreException {
		final Annotation targetAnnotation = nameBinding.getTargetAnnotation();
		if (targetAnnotation == null) {
			final ISourceRange range = nameBinding.getJavaElement().getNameRange();
			markerManager.addMarker(nameBinding,
					range, JaxrsValidationMessages.NAME_BINDING_MISSING_TARGET_ANNOTATION, new String[0], JaxrsPreferences.NAME_BINDING_MISSING_TARGET_ANNOTATION,
					JaxrsMarkerResolutionIds.NAME_BINDING_MISSING_TARGET_ANNOTATION_QUICKFIX_ID);
		} else {
			final List<String> annotationValues = targetAnnotation.getValues("value");
			if (annotationValues == null || annotationValues.isEmpty()) {
				final ISourceRange range = targetAnnotation.getJavaAnnotation().getNameRange();
				markerManager.addMarker(nameBinding,
						range, JaxrsValidationMessages.NAME_BINDING_INVALID_TARGET_ANNOTATION_VALUE, new String[0], JaxrsPreferences.NAME_BINDING_INVALID_TARGET_ANNOTATION_VALUE,
						JaxrsMarkerResolutionIds.NAME_BINDING_INVALID_TARGET_ANNOTATION_VALUE_QUICKFIX_ID);
			} else if (annotationValues.size() != 2 || !annotationValues.contains(ElementType.TYPE.name()) || !annotationValues.contains(ElementType.METHOD.name())) {
				final ISourceRange range = JdtUtils.resolveMemberPairValueRange(targetAnnotation.getJavaAnnotation(),
						VALUE);
				markerManager.addMarker(nameBinding,
						range, JaxrsValidationMessages.NAME_BINDING_INVALID_TARGET_ANNOTATION_VALUE, new String[0], JaxrsPreferences.NAME_BINDING_INVALID_TARGET_ANNOTATION_VALUE,
						JaxrsMarkerResolutionIds.NAME_BINDING_INVALID_TARGET_ANNOTATION_VALUE_QUICKFIX_ID);

			}
		}
	}

	/**
	 * Validate that annotation exists and value is Retention.RUNTIME
	 * 
	 * @param messages
	 * @throws CoreException 
	 */
	private void validateRetentionAnnotation(final JaxrsNameBinding nameBinding) throws CoreException {
		final Annotation retentionAnnotation = nameBinding.getRetentionAnnotation();
		if (retentionAnnotation == null) {
			final ISourceRange range = nameBinding.getJavaElement().getNameRange();
			markerManager.addMarker(nameBinding,
					range, JaxrsValidationMessages.NAME_BINDING_MISSING_RETENTION_ANNOTATION, new String[0], JaxrsPreferences.NAME_BINDING_MISSING_RETENTION_ANNOTATION,
					JaxrsMarkerResolutionIds.NAME_BINDING_MISSING_RETENTION_ANNOTATION_QUICKFIX_ID);
		} else {
			final String annotationValue = retentionAnnotation.getValue();
			if (annotationValue == null) {
				final ISourceRange range = retentionAnnotation.getJavaAnnotation().getNameRange();
				markerManager.addMarker(nameBinding,
						range, JaxrsValidationMessages.NAME_BINDING_INVALID_RETENTION_ANNOTATION_VALUE, new String[0],
						JaxrsPreferences.NAME_BINDING_INVALID_RETENTION_ANNOTATION_VALUE, JaxrsMarkerResolutionIds.NAME_BINDING_INVALID_RETENTION_ANNOTATION_VALUE_QUICKFIX_ID);
			} else if (!annotationValue.equals(RetentionPolicy.RUNTIME.name())) {
				final ISourceRange range = JdtUtils.resolveMemberPairValueRange(
						retentionAnnotation.getJavaAnnotation(), VALUE);
				markerManager.addMarker(nameBinding,
						range, JaxrsValidationMessages.NAME_BINDING_INVALID_RETENTION_ANNOTATION_VALUE, new String[0],
						JaxrsPreferences.NAME_BINDING_INVALID_RETENTION_ANNOTATION_VALUE, JaxrsMarkerResolutionIds.NAME_BINDING_INVALID_RETENTION_ANNOTATION_VALUE_QUICKFIX_ID);

			}
		}
	}

}
