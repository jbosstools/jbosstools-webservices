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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IType;
import org.jboss.tools.common.validation.TempMarkerManager;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsJavaApplication;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname;
import org.jboss.tools.ws.jaxrs.core.preferences.JaxrsPreferences;

/**
 * Java-based JAX-RS Application validator
 * 
 * @author Xavier Coulon
 * 
 */
public class JaxrsJavaApplicationValidatorDelegate extends AbstractJaxrsElementValidatorDelegate<JaxrsJavaApplication> {

	public JaxrsJavaApplicationValidatorDelegate(final TempMarkerManager markerManager) {
		super(markerManager);
	}

	/**
	 * @see org.jboss.tools.ws.jaxrs.core.internal.metamodel.validation.
	 * AbstractJaxrsElementValidatorDelegate#internalValidate(Object)
	 */
	@Override
	void internalValidate(final JaxrsJavaApplication application) throws CoreException {
		Logger.debug("Validating element {}", application);
		JaxrsMetamodelValidator.deleteJaxrsMarkers(application);
		application.resetProblemLevel();
		final Annotation applicationPathAnnotation = application
				.getAnnotation(EnumJaxrsClassname.APPLICATION_PATH.qualifiedName);
		final IType appJavaElement = application.getJavaElement();
		if (!application.isOverriden() && applicationPathAnnotation == null) {
			addProblem(JaxrsValidationMessages.JAVA_APPLICATION_MISSING_APPLICATION_PATH_ANNOTATION,
					JaxrsPreferences.JAVA_APPLICATION_MISSING_APPLICATION_PATH_ANNOTATION, new String[0],
					appJavaElement.getNameRange(), application,
					JaxrsValidationConstants.JAVA_APPLICATION_MISSING_APPLICATION_PATH_ANNOTATION_QUICKFIX_ID);
		}
		if (!application.isJaxrsCoreApplicationSubclass()) {
			addProblem(JaxrsValidationMessages.JAVA_APPLICATION_INVALID_TYPE_HIERARCHY,
					JaxrsPreferences.JAVA_APPLICATION_INVALID_TYPE_HIERARCHY,
					new String[] { appJavaElement.getFullyQualifiedName() }, application.getJavaElement()
							.getSourceRange(), application,
					JaxrsValidationConstants.JAVA_APPLICATION_INVALID_TYPE_HIERARCHY_QUICKFIX_ID);
		}
	}

}
