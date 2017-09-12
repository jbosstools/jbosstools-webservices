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

import org.hamcrest.core.AnyOf;
import org.hamcrest.core.Is;
import org.eclipse.reddeer.jface.wizard.WizardPage;
import org.eclipse.reddeer.swt.api.Text;
import org.eclipse.reddeer.swt.impl.text.DefaultText;
import org.eclipse.reddeer.core.matcher.WithLabelMatcher;
import org.eclipse.reddeer.core.reference.ReferencedComposite;

/**
 * Second {@link WebServiceWizard} page.
 *
 * @author jjankovi
 * @author Radoslav Rabara
 *
 */
public class WebServiceSecondWizardPage extends WizardPage {

	public WebServiceSecondWizardPage(ReferencedComposite referencedComposite) {
		super(referencedComposite);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Sets package name.
	 * 
	 * @param pkgName package name
	 */
	public void setPackageName(String pkgName) {
		getPackageNameText().setText(pkgName);
	}

	/**
	 * Return package name.
	 */
	public String getPackageName() {
		return getPackageNameText().getText();
	}

	private Text getPackageNameText() {
		return new DefaultText(new WithLabelMatcher(AnyOf.anyOf(Is.is("Package name"), 
				Is.is("Package Name:"))));
	}
}
