/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.ws.reddeer.ui.wizards.wst;

import org.eclipse.reddeer.core.reference.ReferencedComposite;

/**
 * Wizard page of {@link WebServiceClientWizard}.
 *
 * @author jjankovi
 * @author Radoslav Rabara
 *
 */
public class WebServiceClientWizardPage extends WebServiceWizardPageBase {

	public WebServiceClientWizardPage(ReferencedComposite referencedComposite) {
		super(referencedComposite);
	}

	/**
	 * Sets the client project.
	 *
	 * @param projectName name of the client project.
	 */
	public void setClientProject(String projectName) {
		setTargetProject("Client project:", projectName);//, "Specify Client Project Settings"
	}

	/**
	 * Sets the client EAR project.
	 *
	 * @param clientEARProjectName name of the client EAR project
	 */
	public void setClientEARProject(String clientEARProjectName) {
		setTargetProject("Client EAR project:", clientEARProjectName);
	}

	/**
	 * Sets level of the client generation.
	 *
	 * @param level level of client generation
	 */
	public void setClientSlider(SliderLevel level) {
		setSlider(level, 0);
	}

	@Override
	public String getSourceComboLabel() {
		return "Service definition:";
	}
}
