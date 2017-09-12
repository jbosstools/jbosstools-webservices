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

import org.eclipse.reddeer.eclipse.selectionwizard.NewMenuWizard;

/**
 * JAX-RS Application wizard.
 *
 * Web Services > JAX-RS Application
 *
 * Has only one wizard page - {@link JAXRSApplicationWizardPage}
 *
 * @author Radoslav Rabara
 * @since JBT 4.2.0 Beta2
 * @see http://tools.jboss.org/documentation/whatsnew/jbosstools/4.2.0.Beta2.html#webservices
 */
public class JAXRSApplicationWizard extends NewMenuWizard {
	
	public JAXRSApplicationWizard() {
		super("", "Web Services", "JAX-RS Application");
	}
}
