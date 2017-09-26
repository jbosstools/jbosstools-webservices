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
package org.jboss.tools.ws.jaxws.core.messages;

import org.eclipse.osgi.util.NLS;

public class JBossJAXWSCoreMessages extends NLS {
	private static final String BUNDLE_NAME = "org.jboss.tools.ws.jaxws.core.messages.JBossJAXWSCore"; //$NON-NLS-1$

	public static String AddRestEasyJarsCommand_RestEasy_JARS_Not_Found;

	public static String Value_Target_0;
	public static String Value_Target_1;
	public static String Value_Target_2;
	public static String Separator_Java;
	public static String WebserviceClient_Annotation;
	public static String Webservice_Annotation;
	public static String Webservice_Annotation_Prefix;
    public static String WebEndpoint;
	public static String Client_Sample_Package_Name;
	public static String Client_Sample_Class_Name;
    public static String Command;
    public static String Bin;
    
	public static String Label_Custom_Package_Name;
	public static String Label_Catalog_File;
	public static String Label_Add_Button;  
	public static String Label_Binding_File;
	public static String Label_Generate_Impelemtation;
	public static String Label_Generate_WSDL;
	public static String Label_JaxWS_Target;
	public static String Label_Update_Webxml;
	public static String Label_EnableSOAP12_Binding_Extension;
	public static String Label_Remove_Button;
    public static String Label__Browse_Button;
	public static String Progress_Message_Generating;  
    public static String Client_Sample_Run_Over;   
    public static String Label_SourceFolder_Name;
    public static String Label_Service_Name;
    public static String Label_Implcls_Name;
    public static String Label_Servlet_Name;
    public static String Label_AdditionalOption_Name;
    public static String Label_Help_Button;
    public static String Tooltip_AdditionalOption;
    public static String Tooltip_Implcls;
    public static String Tooltip_Servlet;
    public static String Tooltip_Service;
    public static String Tooltip_SourceFolder;
    public static String Tooltip_JaxWS_Target;
    public static String Tooltip_Custom_Package;
    public static String Tooltip_Catalog_File;
    public static String Tooltip_BindingFile;

	public static String Error_Message_Invalid_Binding_File;
	public static String Error_Message_Failed_To_Generate_Code;
	public static String Error_Message_Failed_to_Generate_Implementation;	
	public static String Error_Message_Command_File_Not_Found;
	public static String Error_Message_No_SourceFolder;
	public static String Error_Message_No_Service;
	public static String Error_Message_No_ServletName;
	public static String Error_Message_No_ServicePort;
	public static String Error_No_Annotation;
	public static String Error_No_Class;
	public static String Error_No_Package;
	public static String Error_WS_Location;
	public static String Error_Create_Client_Sample;
    public static String Error_Message_No_Runtime_Specified;
    public static String Error_JBossWS_GenerateWizard_WSName_Same;
    public static String Error_JBossWS_GenerateWizard_WSImpl_Overwrite;
    public static String Error_Read_WSDL;

	public static String RestEasyLibUtils_Error_UnableToFindRuntimeForProject;

	public static String RSMergeWebXMLCommand_REST_App_Exists;

	public static String RSMergeWebXMLCommand_REST_Servlet_Exists;

	public static String RSMergeWebXMLCommand_REST_Servlet_Mapping_Exists;
	public static String AdditionalOption_Dialog_Title;
	public static String No_Message_AdditionalOptions_Dialog;
    
	public static String Confirm_Override_Servlet;
	public static String Confirm_Override_ImplClass;
	public static String Progress_UnInstall_JBossWS_Runtime;
	public static String Progress_Install_JBossWS_Runtime;
	public static String Error_Remove_Facet_JBossWS;
	public static String JBossWS_Runtime_Lib;
	public static String Error_Add_Facet_JBossWS;
	public static String Error_Copy;
	public static String JBossWS_Runtime;
	public static String WS_Location;
	public static String Dir_Lib;
	public static String Dir_Client;
    
	private JBossJAXWSCoreMessages() {
	}

	static {
		NLS.initializeMessages(BUNDLE_NAME, JBossJAXWSCoreMessages.class);
	}
	
}
