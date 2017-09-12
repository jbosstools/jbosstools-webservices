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
import org.eclipse.reddeer.swt.impl.button.CheckBox;
import org.eclipse.reddeer.swt.impl.combo.DefaultCombo;
import org.eclipse.reddeer.swt.impl.text.LabeledText;

/**
 * Simple Web Service wizard.
 *
 * Web Services > Simple Web Service
 *
 * @author jjankovi
 * @author Radoslav Rabara
 */
public class SimpleWebServiceWizard extends NewMenuWizard {

	public SimpleWebServiceWizard() {
		super("Simple Web Service", "Web Services", "Simple Web Service");
	}

	public void setProjectName(String projectName) {
		new DefaultCombo(0).setSelection(projectName);
	}

	public void setServiceName(String serviceName) {
		new LabeledText("Service name").setText(serviceName);
	}

	public void setPackageName(String pkgName) {
		new LabeledText("Package").setText(pkgName);
	}

	public void setClassName(String className) {
		new LabeledText("Class").setText(className);
	}

	public void setUpdateWebXml(boolean check) {
		new CheckBox("Update web.xml").toggle(check);
	}
}