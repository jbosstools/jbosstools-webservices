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
package org.jboss.tools.ws.reddeer.ui.wizards.jst.j2ee;

import org.jboss.tools.ws.reddeer.ui.wizards.NewWizardDialogWithAssociatedPerspective;

/**
 * Enterprise Application Project wizard.
 *
 * Java EE > Enterprise Application Project
 *
 * @author Radoslav Rabara
 *
 */
public class EARProjectWizard extends NewWizardDialogWithAssociatedPerspective {
	public EARProjectWizard() {
		super("New EAR Application Project", "Java EE", "Enterprise Application Project");
	}
}
