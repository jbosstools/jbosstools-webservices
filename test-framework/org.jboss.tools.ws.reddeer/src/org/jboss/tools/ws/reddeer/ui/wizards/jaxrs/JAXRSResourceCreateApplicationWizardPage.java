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
package org.jboss.tools.ws.reddeer.ui.wizards.jaxrs;

import org.jboss.reddeer.swt.impl.button.RadioButton;

/**
 * {@link JAXRSResourceWizard}'s second wizard page
 *
 * Extends {@link JAXRSApplicationWizardPage} with another option:
 * - "Skip the JAX-RS Application creation" - {@link #useSkipTheJAXRSApplicationCreation()}
 *
 * @author Radoslav Rabara
 *
 */
public class JAXRSResourceCreateApplicationWizardPage extends JAXRSApplicationWizardPage {
	public void useSkipTheJAXRSApplicationCreation() {
		new RadioButton(2).click();//"Skip the JAX-RS Application creation"
	}
}
