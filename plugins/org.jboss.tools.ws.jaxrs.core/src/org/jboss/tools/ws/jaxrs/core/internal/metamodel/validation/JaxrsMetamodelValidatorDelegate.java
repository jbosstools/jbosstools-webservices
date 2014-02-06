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

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsBaseElement;
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

	/**
	 * Validates the given {@link JaxrsMetamodel}.
	 * 
	 * @param metamodel the metamodel to validate
	 * @throws CoreException
	 */
	void validate(JaxrsMetamodel metamodel) throws CoreException {
		Logger.debug("Validating element {}", metamodel);
		final IProject project = metamodel.getProject();
		JaxrsMetamodelValidator.removeMarkers(metamodel, project);
		metamodel.resetProblemLevel();
		final List<JaxrsJavaApplication> javaApplications = metamodel.getJavaApplications();
		final List<JaxrsWebxmlApplication> webxmlApplications = metamodel.findWebxmlApplications();
		if (javaApplications.isEmpty() && webxmlApplications.isEmpty()) {
			markerManager.addMarker(metamodel,
					JaxrsValidationMessages.APPLICATION_NO_OCCURRENCE_FOUND, new String[0], JaxrsPreferences.APPLICATION_NO_OCCURRENCE_FOUND);
		} else if (javaApplications.size() >= 2 || (javaApplications.size() >= 1 && webxmlApplications.size() >= 1)) {
			for(JaxrsJavaApplication javaApplication: javaApplications) {
				validateApplication(javaApplication);
			}
			for(IJaxrsApplication webxmlapplication: webxmlApplications) {
				validateWebxmlApplication(webxmlapplication, javaApplications);
			}
		}
	}

	/**
	 * Validate the given {@link JaxrsWebxmlApplication} against the existing list of {@link JaxrsJavaApplication} of the metamodel.
	 * 
	 * @param webxmlapplication
	 * @param javaApplications
	 * @throws CoreException
	 * @throws JavaModelException
	 */
	private void validateWebxmlApplication(final IJaxrsApplication webxmlapplication,
			final List<JaxrsJavaApplication> javaApplications) throws CoreException, JavaModelException {
		final JaxrsWebxmlApplication webxmlApplication = (JaxrsWebxmlApplication) webxmlapplication;
		// remove previous marker of type APPLICATION_TOO_MANY_OCCURRENCES
		removeMarkers(webxmlapplication, JaxrsPreferences.APPLICATION_TOO_MANY_OCCURRENCES);
		if((webxmlApplication.isOverride() && javaApplications.size() >= 2)
				|| (!webxmlApplication.isOverride() && javaApplications.size() >= 1)) {
			final ISourceRange webxmlNameRange = WtpUtils.getApplicationPathLocation(webxmlApplication.getResource(),
					webxmlApplication.getJavaClassName());
			if (webxmlNameRange == null) {
				Logger.warn("Cannot add a problem marker: unable to locate '" + webxmlApplication.getJavaClassName()
						+ "' in resource '" + webxmlApplication.getResource().getFullPath().toString() + "'. ");
			} else {
				addTooManyOccurrencesMarker(webxmlApplication, webxmlNameRange);
				if(webxmlApplication.isOverride()) {
					final JaxrsJavaApplication javaApplication = webxmlApplication.getOverridenJaxrsJavaApplication();
					final ISourceRange javaNameRange = javaApplication.getJavaElement().getNameRange();
					addTooManyOccurrencesMarker(javaApplication, javaNameRange);
				}
			}
		}
	}

	/**
	 * Validates the given {@link JaxrsJavaApplication}
	 * 
	 * @param javaApplication
	 * @throws CoreException
	 * @throws JavaModelException
	 */
	private void validateApplication(JaxrsJavaApplication javaApplication) throws CoreException,
			JavaModelException {
		// remove previous marker of type APPLICATION_TOO_MANY_OCCURRENCES
		removeMarkers(javaApplication, JaxrsPreferences.APPLICATION_TOO_MANY_OCCURRENCES);
		if(javaApplication.isOverriden()) {
			return;
		}
		final ISourceRange javaNameRange = javaApplication.getJavaElement().getNameRange();
		addTooManyOccurrencesMarker(javaApplication, javaNameRange);
	}

	/**
	 * Removes all {@link IMarker} whose {@link JaxrsMetamodelValidator#JAXRS_PROBLEM_TYPE} attribute is equal to the given problemType argument.
	 * @param application the {@link IJaxrsApplication}
	 * @param problemType the problem type attribute value of the marker to remove.
	 * @throws CoreException
	 */
	public void removeMarkers(final IJaxrsApplication application, final String problemType) throws CoreException {
		final IMarker[] jaxrsMarkers = application.getResource().findMarkers(JaxrsMetamodelValidator.JAXRS_PROBLEM_MARKER_ID, true, IResource.DEPTH_INFINITE);
		for(IMarker marker : jaxrsMarkers) { 
			if(marker.getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE).equals(problemType)) {
				((JaxrsMetamodel)application.getMetamodel()).removeMarker(marker);
			}
		}
	}

	/**
	 * @param application
	 * @param javaNameRange
	 * @throws CoreException
	 */
	public void addTooManyOccurrencesMarker(final JaxrsBaseElement application, final ISourceRange javaNameRange)
			throws CoreException {
		markerManager.addMarker(application,
				javaNameRange, JaxrsValidationMessages.APPLICATION_TOO_MANY_OCCURRENCES, new String[0], JaxrsPreferences.APPLICATION_TOO_MANY_OCCURRENCES);
	}

	

}
