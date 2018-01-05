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
package org.jboss.tools.ws.reddeer.ui.wizards;

import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.eclipse.selectionwizard.NewMenuWizard;
import org.jboss.tools.common.reddeer.label.IDELabel;

/**
 * Specialized parent of wizards dialogs that has associated perspective.<br/>
 * 
 * Provides specialized {@link #finish()} method that handles opening of shell
 * with text {@link IDELabel.Shell.OPEN_ASSOCIATED_PERSPECTIVE}.
 *
 * @author Radoslav Rabara
 *
 */
public abstract class NewWizardDialogWithAssociatedPerspective extends NewMenuWizard {

	/**
	 * {@inheritDoc}
	 */
	public NewWizardDialogWithAssociatedPerspective(String shellText, String... path) {
		super(shellText, path);
	}

	/**
	 * Clicks on the button Finish, then if the shell open associated perspective
	 * is active, closes it permanently, and waits while any job is running.
	 */
	@Override
	public void finish() {
		super.finish(TimePeriod.LONG);
	}
}
