/******************************************************************************* 
 * Copyright (c) 2007 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 

package org.jboss.tools.ws.core.facet.delegate;

import org.eclipse.wst.common.project.facet.core.IActionConfigFactory;

/**
 * Seam facet properties collected in Seam Web Project Wizard.
 * Only properties listed below are saved in Seam WebProject Preferences:
 * 		ISeamFacetDataModelProperties.SESSION_BEAN_SOURCE_FOLDER, 
 * 		ISeamFacetDataModelProperties.SESION_BEAN_PACKAGE_NAME,
 * 		ISeamFacetDataModelProperties.ENTITY_BEAN_SOURCE_FOLDER, 
 * 		ISeamFacetDataModelProperties.ENTITY_BEAN_PACKAGE_NAME,
 *  	ISeamFacetDataModelProperties.SEAM_CONNECTION_PROFILE,
 * 		ISeamFacetDataModelProperties.JBOSS_AS_DEPLOY_AS,
 * 		ISeamFacetDataModelProperties.SEAM_TEST_PROJECT,
 * 		ISeamFacetDataModelProperties.SEAM_RUNTIME_NAME,
 * 		ISeamFacetDataModelProperties.SEAM_SETTINGS_VERSION,
 * 		ISeamFacetDataModelProperties.TEST_SOURCE_FOLDER,
 * 		ISeamFacetDataModelProperties.TEST_CASES_PACKAGE_NAME,
 * 		ISeamFacetDataModelProperties.TEST_CREATING,
 * 		ISeamFacetDataModelProperties.SEAM_EJB_PROJECT,
 * 		ISeamFacetDataModelProperties.SEAM_EAR_PROJECT
 * @author eskimo
 */
public interface IJBossWSFacetDataModelProperties extends IActionConfigFactory {

	// Seam Preferences names constants

	String JBOSS_WS_FACET_ID = "jbossws.core";
	String JBOSS_WS_RUNTIME_IS_SERVER_SUPPLIED = "jboss.ws.is.server.supplied";
	String JBOSS_WS_RUNTIME_ID = "jboss.ws.runtime_id";
	String JBOSS_WS_DEPLOY = "jboss.ws.deploy";
	String JBOSS_WS_RUNTIME_HOME = "jboss.ws.runtime.home";
	String QUALIFIEDNAME_IDENTIFIER_IS_SERVER_SUPPLIED = "jboss.tools.jbossws";
	String PERSISTENT_PROPERTY_IS_SERVER_SUPPLIED_RUNTIME = "is.server.supplied.runtime";
	

}