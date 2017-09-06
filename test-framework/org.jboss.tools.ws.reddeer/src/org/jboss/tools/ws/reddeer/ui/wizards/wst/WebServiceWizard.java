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
package org.jboss.tools.ws.reddeer.ui.wizards.wst;

import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.eclipse.selectionwizard.NewMenuWizard;
import org.eclipse.reddeer.swt.condition.ShellIsActive;

/**
 * Web Service wizard.
 *
 * Web Services > Web Service
 *
 * @author Radoslav Rabara
 * @see WebServiceFirstWizardPage
 * @see WebServiceSecondWizardPage
 */
public class WebServiceWizard extends NewMenuWizard {
	public WebServiceWizard() {
		super("Web Service", "Web Services", "Web Service");
	}

	@Override
	public void finish() {
		handleProgressInformationDialog();
		super.finish();
	}

	private void handleProgressInformationDialog() {
		final String title = "Progress Information";
		new WaitUntil(new ShellIsActive(title), TimePeriod.SHORT, false);
		new WaitWhile(new ShellIsActive(title));
	}
}
