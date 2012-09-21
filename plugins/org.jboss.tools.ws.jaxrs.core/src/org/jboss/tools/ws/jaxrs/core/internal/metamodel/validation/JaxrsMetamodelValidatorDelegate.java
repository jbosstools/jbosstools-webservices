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
import org.jboss.tools.common.validation.TempMarkerManager;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsApplication;
import org.jboss.tools.ws.jaxrs.core.preferences.JaxrsPreferences;

/**
 * JAX-RS Metamodel validator: validate the total number of applications (which should be exactly one)
 * 
 * @author Xavier Coulon
 * 
 */
public class JaxrsMetamodelValidatorDelegate extends AbstractJaxrsElementValidatorDelegate<JaxrsMetamodel> {

	public JaxrsMetamodelValidatorDelegate(TempMarkerManager markerManager, JaxrsMetamodel metamodel) {
		super(markerManager, metamodel);
	}

	@Override
	public void validate() throws CoreException {
		final JaxrsMetamodel metamodel = getElement();
		final IProject project = metamodel.getProject();
		deleteJaxrsMarkers(project);
		final List<IJaxrsApplication> allApplications = metamodel.getAllApplications();
		if (allApplications.isEmpty()) {
			addProblem(JaxrsValidationMessages.APPLICATION_NO_OCCURRENCE_FOUND,
					JaxrsPreferences.APPLICATION_NO_OCCURRENCE_FOUND, new String[0], 0, 0, project);

		}
	}

}
