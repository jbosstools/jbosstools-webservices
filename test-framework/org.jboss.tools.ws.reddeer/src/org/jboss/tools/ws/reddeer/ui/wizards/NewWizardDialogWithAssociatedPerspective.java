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

import org.eclipse.reddeer.common.condition.WaitCondition;
import org.eclipse.reddeer.swt.condition.ShellIsActive;
import org.eclipse.reddeer.swt.impl.button.CheckBox;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
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
		String shellText = new DefaultShell().getText();

		new PushButton(IDELabel.Button.FINISH).click();

		closeOpenAssociatedPerspectiveDialog();

		new WaitWhile(new ShellIsActive(shellText), TimePeriod.LONG);
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
	}

	private void closeOpenAssociatedPerspectiveDialog() {
		WaitCondition condition = new ShellIsActive(
				IDELabel.Shell.OPEN_ASSOCIATED_PERSPECTIVE);
		new WaitUntil(condition, TimePeriod.DEFAULT, false);
		if(condition.test()) {
			CheckBox checkbox = new CheckBox(IDELabel.Shell.REMEMBER_MY_DECISION);
			if(!checkbox.isChecked()) {
				checkbox.click();
			}
			new PushButton(IDELabel.Button.NO).click();
		}
	}
}
