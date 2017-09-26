/******************************************************************************* 
 * Copyright (c) 2008 - 2014 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 

package org.jboss.tools.ws.ui.messages;

import org.eclipse.osgi.util.NLS;

/**
 * @author Grid Qian
 */
public class JBossWSUIMessages extends NLS {

	private static final String BUNDLE_NAME = "org.jboss.tools.ws.ui.messages.JBossWSUI";		//$NON-NLS-1$

	private JBossWSUIMessages() {
		// Do not instantiate
	}
	
	public static String JBossWS_UI_PLUGIN_NO_MESSAGES;
	
	public static String JAXRSWSTestView_CopyResultMenu_Text;
	public static String JAXRSWSTestView_CopyResultsMenu;
	public static String JAXRSWSTestView_Invoking_WS_Status;
	public static String JAXRSWSTestView_Open_Result_in_XML_Editor;
	
	public static String JAXRSWSTestView2_BodyText_Section;
	public static String JAXRSWSTestView2_Checkbox_Basic_Authentication;
	public static String JAXRSWSTestView2_Go_Tooltip;
	
	public static String JAXRSWSTestView2_Name_column;
	public static String JAXRSWSTestView2_OpenInEditor_Action;
	public static String JAXRSWSTestView2_RequestDetails_Section;
	public static String JAXRSWSTestView2_ResponseBody_Section;
	public static String JAXRSWSTestView2_ResponseDetails_Section;
	public static String JAXRSWSTestView2_ResponseHeaders_Section;
	public static String JAXRSWSTestView2_SaveResponseText_Error;
	public static String JAXRSWSTestView2_SaveResponseText_tooltip;
	public static String JAXRSWSTestView2_ShowInBrowser_Tooltip;
	public static String JAXRSWSTestView2_ShowRaw_Tooltip;
	public static String JAXRSWSTestView2_ShowRequestTree_toolbar_btn;
	
	public static String JAXRSWSTestView2_Value_column;
	public static String ResultsXMLStorageInput_WS_Invocation_Results_Prefix;

	public static String UidPwdDialog_Description;
	public static String UidPwdDialog_PWD_Label;
	public static String UidPwdDialog_Title;
	public static String UidPwdDialog_UID_Label;
	
	public static String WSTestUtils_SaveResponseText_Error_Msg;
	public static String WSTestUtils_SaveResponseText_Title;

	static {
		NLS.initializeMessages(BUNDLE_NAME, JBossWSUIMessages.class);
	}
}
