/******************************************************************************* 
 * Copyright (c) 2008 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/

package org.jboss.tools.ws.jaxrs.ui.wizards;

import org.eclipse.osgi.util.NLS;

/**
 * @author xcoulon
 *
 */
public class JaxrsApplicationCreationMessages extends NLS {

	private static final String BUNDLE_NAME = JaxrsApplicationCreationMessages.class.getName();

	private JaxrsApplicationCreationMessages() {
		// Do not instantiate
	}
	public static String JaxrsApplicationCreationWizardPage_Name;
	public static String JaxrsApplicationCreationWizardPage_Title;
	public static String JaxrsApplicationCreationWizardPage_Description;

	public static String JaxrsApplicationCreationWizardPage_ApplicationStyle;		
	public static String JaxrsApplicationCreationWizardPage_JavaApplicationCreation;		
	public static String JaxrsApplicationCreationWizardPage_WebxmlApplicationCreation;		
	public static String JaxrsApplicationCreationWizardPage_SkipApplicationCreation;		
	public static String JaxrsApplicationCreationWizardPage_SkipApplicationCreationWarning;		
	public static String JaxrsApplicationCreationWizardPage_ApplicationAlreadyExistsWarning;

	public static String JaxrsApplicationCreationWizardPage_EmptyApplicationPath;
	public static String JaxrsApplicationCreationWizardPage_IllegalTypeHierarchy;

	static {
		NLS.initializeMessages(BUNDLE_NAME, JaxrsApplicationCreationMessages.class);
	}
}
