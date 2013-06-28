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
import org.eclipse.jdt.core.ISourceRange;
import org.jboss.tools.common.validation.TempMarkerManager;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsWebxmlApplication;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.internal.utils.WtpUtils;
import org.jboss.tools.ws.jaxrs.core.preferences.JaxrsPreferences;

/**
 * Java-based JAX-RS Application validator
 * 
 * @author Xavier Coulon
 * 
 */
public class JaxrsWebxmlApplicationValidatorDelegate extends AbstractJaxrsElementValidatorDelegate<JaxrsWebxmlApplication> {

	public JaxrsWebxmlApplicationValidatorDelegate(final TempMarkerManager markerManager) {
		super(markerManager);
	}

	/**
	 * @see org.jboss.tools.ws.jaxrs.core.internal.metamodel.validation.
	 * AbstractJaxrsElementValidatorDelegate#internalValidate(Object)
	 */
	@Override
	void internalValidate(final JaxrsWebxmlApplication webxmlApplication) throws CoreException {
		Logger.debug("Validating element {}", webxmlApplication);
		JaxrsMetamodelValidator.deleteJaxrsMarkers(webxmlApplication);
		webxmlApplication.resetProblemLevel();
		if (webxmlApplication.getMetamodel().hasMultipleApplications()) {
			ISourceRange webxmlNameRange = WtpUtils.getApplicationPathLocation(webxmlApplication.getResource(),
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
