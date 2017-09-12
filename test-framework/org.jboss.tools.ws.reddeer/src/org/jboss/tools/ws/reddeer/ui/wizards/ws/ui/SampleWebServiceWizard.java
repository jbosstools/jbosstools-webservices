/*******************************************************************************
 * Copyright (c) 2010-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.ws.reddeer.ui.wizards.ws.ui;

import org.eclipse.reddeer.eclipse.selectionwizard.NewMenuWizard;
import org.eclipse.reddeer.swt.api.Combo;
import org.eclipse.reddeer.swt.impl.combo.DefaultCombo;
import org.eclipse.reddeer.swt.impl.group.DefaultGroup;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.jboss.tools.ws.ui.messages.JBossWSUIMessages;

/**
 * Sample Web Service wizard
 *
 * Web Services > Sample Web Service
 *
 * @author jjankovi
 * @author Radoslav Rabara
 */
public class SampleWebServiceWizard extends NewMenuWizard {

	/**
	 * Constructs Sample Web Service wizard.
	 */
	public SampleWebServiceWizard() {
		super("Generate a Sample Web Service", "Web Services", "Create a Sample Web Service");
	}

	/**
	 * Sets the project name.
	 *
	 * @param projectName name to be set as project name
	 */
	public void setProjectName(String projectName) {
		Combo c = new DefaultCombo(
				new DefaultGroup(
						JBossWSUIMessages.JBossWS_GenerateWizard_GenerateWizardPage_Project_Group));
		c.setSelection(projectName);
	}

	/**
	 * Sets service name.
	 *
	 * @param serviceName name to be set as service name
	 */
	public void setServiceName(String serviceName) {
		setText(JBossWSUIMessages.JBossWS_GenerateWizard_GenerateWizardPage_ServiceName_Label,
				serviceName);
	}

	/**
	 * Sets package name.
	 *
	 * @param pkgName name to be set as package name
	 */
	public void setPackageName(String pkgName) {
		setText(JBossWSUIMessages.JBossWS_GenerateWizard_GenerateWizardPage_Package_Label,
				pkgName);
	}

	/**
	 * Sets class name.
	 *
	 * @param clsName name to be set as class name
	 */
	public void setClassName(String clsName) {
		setText(JBossWSUIMessages.JBossWS_GenerateWizard_GenerateWizardPage_ClassName_Label,
				clsName);
	}

	private void setText(String label, String text) {
		new LabeledText(label).setText(text);
	}
}
