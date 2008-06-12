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

import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.wst.common.project.facet.core.IActionConfigFactory;


public interface IJBossWSFacetDataModelProperties extends IActionConfigFactory {

	// Seam Preferences names constants

	String JBOSS_WS_FACET_ID = "jbossws.core";
	String JBOSS_WS_DEPLOY = "jboss.ws.deploy";	
	String QUALIFIEDNAME_IDENTIFIER = "jboss.tools.jbossws";	
	String JBOSS_WS_RUNTIME_IS_SERVER_SUPPLIED = "jboss.ws.is.server.supplied";
	String JBOSS_WS_RUNTIME_ID = "jboss.ws.runtime_id";
	
	String PERSISTENT_PROPERTY_IS_SERVER_SUPPLIED_RUNTIME = "is.server.supplied.runtime";
	String JBOSS_WS_RUNTIME_HOME = "jboss.ws.runtime.home";
	String DEFAULT_VALUE_IS_SERVER_SUPPLIED = "1";
	
	
	static QualifiedName PERSISTENCE_PROPERTY_QNAME_RUNTIME_NAME = new QualifiedName(QUALIFIEDNAME_IDENTIFIER,
			JBOSS_WS_RUNTIME_ID);
	static QualifiedName PERSISTENCE_PROPERTY_RNTIME_LOCATION = new QualifiedName(QUALIFIEDNAME_IDENTIFIER,
			JBOSS_WS_RUNTIME_HOME);
	static QualifiedName PERSISTENCE_PROPERTY_SERVER_SUPPLIED_RUNTIME = new QualifiedName(
			QUALIFIEDNAME_IDENTIFIER,
			PERSISTENT_PROPERTY_IS_SERVER_SUPPLIED_RUNTIME);

}