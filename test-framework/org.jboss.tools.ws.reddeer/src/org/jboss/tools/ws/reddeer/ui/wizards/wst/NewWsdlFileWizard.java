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

import org.jboss.reddeer.jface.wizard.NewWizardDialog;
import org.jboss.tools.ws.reddeer.ui.wizards.CreateNewFileWizardPage;

/**
 * WSDL File wizard.<br/>
 *
 * The first wizard page is being represented by {@link CreateNewFileWizardPage}.
 * Other wizard pages are not implemented yet.
 *
 * Web Services > WSDL File
 *
 * @author Radoslav Rabara
 *
 */
public class NewWsdlFileWizard extends NewWizardDialog {
	public NewWsdlFileWizard() {
		super("Web Services", "WSDL File");
	}
}
