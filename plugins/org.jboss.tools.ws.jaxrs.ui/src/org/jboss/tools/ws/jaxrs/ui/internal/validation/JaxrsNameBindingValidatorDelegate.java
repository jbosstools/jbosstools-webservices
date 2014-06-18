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
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsNameBinding;
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
public class JaxrsNameBindingValidatorDelegate extends AbstractJaxrsElementValidatorDelegate<JaxrsNameBinding> {

	/** The underlying marker manager.*/
	private final IMarkerManager markerManager;

	/**
	 * Constructor
	 * @param markerManager the underlying marker manager to use
	 */
	public JaxrsNameBindingValidatorDelegate(final IMarkerManager markerManager) {
		this.markerManager = markerManager;
	}

	/**
	 * @see org.jboss.tools.ws.jaxrs.ui.internal.validation.AbstractJaxrsElementValidatorDelegate#internalValidate(Object)
	 */
	@Override
	void internalValidate(final JaxrsNameBinding nameBinding) throws CoreException {
		Logger.debug("Validating element {}", nameBinding);
		if (!nameBinding.isBuiltIn()) {
			validateRetentionAnnotation(nameBinding);
			validateTargetAnnotation(nameBinding);
		}
	}

	/**
	 * Validate that annotation exists and value is Target.METHOD and
	 * Target.TYPE
	 * 
	 * @param messages
	 * @throws CoreException
	 */
	private void validateTargetAnnotation(final JaxrsNameBinding nameBinding) throws CoreException {
		final Annotation targetAnnotation = nameBinding.getTargetAnnotation();
		if (targetAnnotation == null) {
			final ISourceRange range = nameBinding.getJavaElement().getNameRange();
			markerManager.addMarker(nameBinding, range, JaxrsValidationMessages.NAME_BINDING_MISSING_TARGET_ANNOTATION,
					new String[0], JaxrsPreferences.NAME_BINDING_MISSING_TARGET_ANNOTATION,
					JaxrsMarkerResolutionIds.NAME_BINDING_MISSING_TARGET_ANNOTATION_QUICKFIX_ID);
		} else {
			final List<String> annotationValues = targetAnnotation.getValues("value");
			final List<String> expectedValues = Arrays.asList(ElementType.METHOD.name(), ElementType.TYPE.name());
			if (!CollectionUtils.containsInAnyOrder(annotationValues, expectedValues)) {
				final ISourceRange range = JdtUtils.resolveMemberPairValueRange(targetAnnotation.getJavaAnnotation(),
						VALUE);
				markerManager.addMarker(nameBinding, range,
						JaxrsValidationMessages.NAME_BINDING_INVALID_TARGET_ANNOTATION_VALUE, new String[0],
						JaxrsPreferences.NAME_BINDING_INVALID_TARGET_ANNOTATION_VALUE,
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
			markerManager.addMarker(nameBinding, range,
					JaxrsValidationMessages.NAME_BINDING_MISSING_RETENTION_ANNOTATION, new String[0],
					JaxrsPreferences.NAME_BINDING_MISSING_RETENTION_ANNOTATION,
					JaxrsMarkerResolutionIds.NAME_BINDING_MISSING_RETENTION_ANNOTATION_QUICKFIX_ID);
		} else if (retentionAnnotation.getValue() != null && !retentionAnnotation.getValue().equals(RetentionPolicy.RUNTIME.name())) {
			final ISourceRange range = JdtUtils.resolveMemberPairValueRange(retentionAnnotation.getJavaAnnotation(),
					VALUE);
			markerManager.addMarker(nameBinding, range,
					JaxrsValidationMessages.NAME_BINDING_INVALID_RETENTION_ANNOTATION_VALUE, new String[0],
					JaxrsPreferences.NAME_BINDING_INVALID_RETENTION_ANNOTATION_VALUE,
					JaxrsMarkerResolutionIds.NAME_BINDING_INVALID_RETENTION_ANNOTATION_VALUE_QUICKFIX_ID);

		}
	}

}
