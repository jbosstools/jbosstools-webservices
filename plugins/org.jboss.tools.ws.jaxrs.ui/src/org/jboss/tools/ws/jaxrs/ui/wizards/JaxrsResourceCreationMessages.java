/******************************************************************************* 
 * Copyright (c) 2008 - 2014 Red Hat, Inc. and others. 
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
public class JaxrsResourceCreationMessages extends NLS {

	private static final String BUNDLE_NAME = JaxrsResourceCreationMessages.class.getName();		

	private JaxrsResourceCreationMessages() {
		// Do not instantiate
	}
	public static String JaxrsResourceCreationWizardPage_Name;
	public static String JaxrsResourceCreationWizardPage_Title;
	public static String JaxrsResourceCreationWizardPage_Description;

	public static String JaxrsResourceCreationWizardPage_EmptyResourcePath;
	public static String JaxrsResourceCreationWizardPage_EmptyTargetClass;
	public static String JaxrsResourceCreationWizardPage_InvalidTargetClass;
	public static String JaxrsResourceCreationWizardPage_CreateMethodSkeleton;
	public static String JaxrsResourceCreationWizardPage_FindByIdMethodSkeleton;
	public static String JaxrsResourceCreationWizardPage_ListAllMethodSkeleton;
	public static String JaxrsResourceCreationWizardPage_UpdateMethodSkeleton;
	public static String JaxrsResourceCreationWizardPage_DeleteByIdMethodSkeleton;
	
	static {
		NLS.initializeMessages(BUNDLE_NAME, JaxrsResourceCreationMessages.class);
	}
}
