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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ISourceRange;
import org.jboss.tools.common.validation.TempMarkerManager;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsWebxmlApplication;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.internal.utils.WtpUtils;
import org.jboss.tools.ws.jaxrs.core.preferences.JaxrsPreferences;

/**
 * Web.xml based JAX-RS Application validator (includes validation for both <code>javax.ws.rs.core.Application</code>
 * and project-specific subclasses).
 * 
 * @author Xavier Coulon
 * 
 */
public class JaxrsWebxmlApplicationValidatorDelegate extends
		AbstractJaxrsElementValidatorDelegate<JaxrsWebxmlApplication> {

	/**
	 * @param markerManager
	 * @param application
	 */
	public JaxrsWebxmlApplicationValidatorDelegate(TempMarkerManager markerManager, JaxrsWebxmlApplication application) {
		super(markerManager, application);
	}

	@Override
	public void validate() throws CoreException {
		JaxrsWebxmlApplication webxmlApplication = getElement();
		deleteJaxrsMarkers(webxmlApplication.getResource());
		
		if (webxmlApplication.getMetamodel().hasMultipleApplications()) {
			final IResource webxmlResource = webxmlApplication.getResource();
			ISourceRange webxmlNameRange = WtpUtils.getApplicationPathLocation(webxmlApplication.getResource(),
					webxmlApplication.getJavaClassName());
			if (webxmlNameRange == null) {
				Logger.warn("Cannot add a problem marker: unable to locate '" + webxmlApplication.getJavaClassName()
						+ "' in resource '" + webxmlApplication.getResource().getFullPath().toString() + "'. ");
			} else {
				addProblem(JaxrsValidationMessages.APPLICATION_TOO_MANY_OCCURRENCES,
						JaxrsPreferences.APPLICATION_TOO_MANY_OCCURRENCES, new String[0], webxmlNameRange.getLength(), webxmlNameRange.getOffset(),
						webxmlResource);
			}
		}
	}

}
