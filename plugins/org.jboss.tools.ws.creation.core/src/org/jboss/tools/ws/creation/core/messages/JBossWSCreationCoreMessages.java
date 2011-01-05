package org.jboss.tools.ws.creation.core.messages;

import org.eclipse.osgi.util.NLS;

public class JBossWSCreationCoreMessages extends NLS {
	private static final String BUNDLE_NAME = "org.jboss.tools.ws.creation.core.messages.JBossWSCreationCore"; //$NON-NLS-1$

	public static String AddRestEasyJarsCommand_RestEasy_JARS_Not_Found;

	public static String Value_Target_0;
	public static String Value_Target_1;
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
	public static String Label_Button_Text_Seletion;  
	public static String Label_Binding_File;
	public static String Label_Generate_Impelemtation;
	public static String Label_Generate_WSDL;
	public static String Label_JaxWS_Target;
	public static String Label_Update_Webxml;
	public static String Label_EnableSOAP12_Binding_Extension;
	public static String Label_Button_Text_Remove;
	public static String Progress_Message_Generating;  
    public static String Client_Sample_Run_Over;   

	public static String Error_Message_Invalid_Binding_File;
	public static String Error_Message_Failed_To_Generate_Code;
	public static String Error_Message_Failed_to_Generate_Implementation;	
	public static String Error_Message_Command_File_Not_Found;
	public static String Error_No_Annotation;
	public static String Error_No_Class;
	public static String Error_No_Package;
	public static String Error_WS_Location;
	public static String Error_Create_Client_Sample;
    public static String Error_Message_No_Runtime_Specified;
    public static String Error_JBossWS_GenerateWizard_WSName_Same;
    public static String Error_JBossWS_GenerateWizard_WSImpl_Overwrite;

	public static String RestEasyLibUtils_Error_UnableToFindRuntimeForProject;

	public static String RSMergeWebXMLCommand_REST_App_Exists;

	public static String RSMergeWebXMLCommand_REST_Servlet_Exists;

	public static String RSMergeWebXMLCommand_REST_Servlet_Mapping_Exists;
    
	public static String Confirm_Override_Servlet;
	public static String Confirm_Override_ImplClass;
    
	private JBossWSCreationCoreMessages() {
	}

	static {
		NLS.initializeMessages(BUNDLE_NAME, JBossWSCreationCoreMessages.class);
	}
	
}
