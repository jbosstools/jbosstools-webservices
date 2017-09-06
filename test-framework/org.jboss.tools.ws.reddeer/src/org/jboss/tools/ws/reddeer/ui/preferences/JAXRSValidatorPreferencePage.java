/*******************************************************************************
 * Copyright (c) 2016-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.ws.reddeer.ui.preferences;

import org.eclipse.reddeer.core.reference.ReferencedComposite;
import org.eclipse.reddeer.jface.preference.PreferencePage;
import org.eclipse.reddeer.swt.impl.button.CheckBox;

/**
 * JBoss Tools > JAX-RS > JAX-RS Validator page in Preferences dialog.
 * 
 * @author Radoslav Rabara
 *
 */
public class JAXRSValidatorPreferencePage extends PreferencePage {

	public JAXRSValidatorPreferencePage(ReferencedComposite referencedComposite) {
		super(referencedComposite, "JBoss Tools", "JAX-RS", "JAX-RS Validator");
	}

	/**
	 * Enables or disables validation.
	 *
	 * @param enable if it's <code>true</code> then validation will be enabled,
	 * 			otherwise validation
	 */
	public void setEnableValidation(boolean enable) {
		new CheckBox("Enable validation").toggle(enable);
	}
}
