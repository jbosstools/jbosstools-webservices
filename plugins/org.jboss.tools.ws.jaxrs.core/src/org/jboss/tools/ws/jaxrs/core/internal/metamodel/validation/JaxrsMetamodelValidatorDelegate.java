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
public class JaxrsMetamodelValidatorDelegate {

	private final IMarkerManager markerManager;
	
	public JaxrsMetamodelValidatorDelegate(final IMarkerManager markerManager) {
		this.markerManager = markerManager;
	}

	void validate(JaxrsMetamodel metamodel) throws CoreException {
		Logger.debug("Validating element {}", metamodel);
		final IProject project = metamodel.getProject();
		JaxrsMetamodelValidator.deleteJaxrsMarkers(metamodel, project);
		metamodel.resetProblemLevel();
		final List<IJaxrsApplication> allApplications = metamodel.getAllApplications();
		if (allApplications.isEmpty()) {
			markerManager.addMarker(metamodel,
					JaxrsValidationMessages.APPLICATION_NO_OCCURRENCE_FOUND, new String[0], JaxrsPreferences.APPLICATION_NO_OCCURRENCE_FOUND);
		} else if (allApplications.size() > 1) {
			for(IJaxrsApplication application: allApplications) {
				if(application.isJavaApplication()) {
					final JaxrsJavaApplication javaApplication = (JaxrsJavaApplication) application;
					// skip if the application is overridden
					if(javaApplication.isOverriden()) {
						continue;
					}
					final ISourceRange javaNameRange = javaApplication.getJavaElement().getNameRange();
					markerManager.addMarker(javaApplication,
							javaNameRange, JaxrsValidationMessages.APPLICATION_TOO_MANY_OCCURRENCES, new String[0], JaxrsPreferences.APPLICATION_TOO_MANY_OCCURRENCES);
				} else {
					final JaxrsWebxmlApplication webxmlApplication = (JaxrsWebxmlApplication) application;
					final ISourceRange webxmlNameRange = WtpUtils.getApplicationPathLocation(webxmlApplication.getResource(),
							webxmlApplication.getJavaClassName());
					if (webxmlNameRange == null) {
						Logger.warn("Cannot add a problem marker: unable to locate '" + webxmlApplication.getJavaClassName()
								+ "' in resource '" + webxmlApplication.getResource().getFullPath().toString() + "'. ");
					} else {
						markerManager.addMarker(webxmlApplication,
								webxmlNameRange, JaxrsValidationMessages.APPLICATION_TOO_MANY_OCCURRENCES, new String[0],
								JaxrsPreferences.APPLICATION_TOO_MANY_OCCURRENCES);
					}
				}
			}
		}
	}

	

}
