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

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ISourceRange;
import org.jboss.tools.common.validation.TempMarkerManager;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsJavaApplication;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsWebxmlApplication;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.internal.utils.WtpUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsApplication;
import org.jboss.tools.ws.jaxrs.core.preferences.JaxrsPreferences;

/**
 * JAX-RS Metamodel validator: validate the total number of applications (which should be exactly one)
 * 
 * @author Xavier Coulon
 * 
 */
public class JaxrsMetamodelValidatorDelegate extends AbstractValidatorDelegate<JaxrsMetamodel> {

	public JaxrsMetamodelValidatorDelegate(final TempMarkerManager markerManager) {
		super(markerManager);
	}

	@Override
	void validate(JaxrsMetamodel metamodel) throws CoreException {
		Logger.debug("Validating element {}", metamodel);
		final IProject project = metamodel.getProject();
		JaxrsMetamodelValidator.deleteJaxrsMarkers(project);
		metamodel.resetProblemLevel();
		final List<IJaxrsApplication> allApplications = metamodel.getAllApplications();
		if (allApplications.isEmpty()) {
			addProblem(JaxrsValidationMessages.APPLICATION_NO_OCCURRENCE_FOUND,
					JaxrsPreferences.APPLICATION_NO_OCCURRENCE_FOUND, new String[0], metamodel);
		} else if (allApplications.size() > 1) {
			for(IJaxrsApplication application: allApplications) {
				if(application.isJavaApplication()) {
					final JaxrsJavaApplication javaApplication = (JaxrsJavaApplication) application;
					// skip if the application is overridden
					if(javaApplication.isOverriden()) {
						continue;
					}
					final ISourceRange javaNameRange = javaApplication.getJavaElement().getNameRange();
					addProblem(JaxrsValidationMessages.APPLICATION_TOO_MANY_OCCURRENCES,
							JaxrsPreferences.APPLICATION_TOO_MANY_OCCURRENCES, new String[0], javaNameRange, javaApplication);
				} else {
					final JaxrsWebxmlApplication webxmlApplication = (JaxrsWebxmlApplication) application;
					final ISourceRange webxmlNameRange = WtpUtils.getApplicationPathLocation(webxmlApplication.getResource(),
							webxmlApplication.getJavaClassName());
					if (webxmlNameRange == null) {
						Logger.warn("Cannot add a problem marker: unable to locate '" + webxmlApplication.getJavaClassName()
								+ "' in resource '" + webxmlApplication.getResource().getFullPath().toString() + "'. ");
					} else {
						addProblem(JaxrsValidationMessages.APPLICATION_TOO_MANY_OCCURRENCES,
								JaxrsPreferences.APPLICATION_TOO_MANY_OCCURRENCES, new String[0], webxmlNameRange,
								webxmlApplication);
					}
				}
			}
		}
	}

	

}
