/******************************************************************************* 
 * Copyright (c) 2008 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 

package org.jboss.tools.ws.core.messages;

import org.eclipse.osgi.util.NLS;

/**
 * @author Grid Qian
 */
public class JbossWSCoreMessages {

	private static final String BUNDLE_NAME = "org.jboss.tools.ws.core.messages.JbossWSCore"; //$NON-NLS-1$

	private JbossWSCoreMessages() {
		// Do not instantiate
	}

	public static String Progress_Install_JBossWS_Runtime;
	public static String Dir_Lib;
	public static String Dir_Client;
	public static String Dir_Web_Inf;
	public static String Dir_Web_Content;
	public static String Error_Copy;
	public static String WS_Location;
	public static String JBossWS_Runtime_Lib;
	public static String JBossWS_Runtime;
	public static String JBossAS;
	public static String Error_WS_Location;
	public static String Error_WS_Classpath;
	public static String ERROR_ADD_FACET_JBOSSWS;


	static {
		NLS.initializeMessages(BUNDLE_NAME, JbossWSCoreMessages.class);
	}
}