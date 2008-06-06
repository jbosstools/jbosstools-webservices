package org.jboss.tools.ws.creation.core.messages;

import org.eclipse.osgi.util.NLS;

public class JBossWSCreationCoreMessages extends NLS {
	private static final String BUNDLE_NAME = "org.jboss.tools.ws.creation.core.messages.JBossWSCreationCore"; //$NON-NLS-1$

	public static String Label_Custom_Package_Name;
	public static String Label_Catalog_File;
	public static String Label_Button_Text_Seletion;  
	public static String Label_Binding_File;
	public static String Label_JaxWS_Target;
	public static String Value_Target_0;
	public static String Value_Target_1;
	public static String Label_Button_Text_Remove;

	public static String Error_Message_Invalid_Binding_File;
	public static String Error_Read_Binding_File;
	public static String Separator_Java;
	
	public static String Error_Implemetation_Code_Generation;
	public static String Error_No_Annotation;
	public static String WebserviceClient_Annotation;
	public static String Error_No_Class;
	public static String Error_WS_Location;
	public static String Webservice_Annotation_Check;
	public static String Webservice_Annotation;
	public static String Client_Sample_Package_Name;
	public static String Client_Sample_Class_Name;
	public static String Error_Create_Client_Sample;
    public static String WebEndpoint;

	private JBossWSCreationCoreMessages() {
	}

	static {
		NLS.initializeMessages(BUNDLE_NAME, JBossWSCreationCoreMessages.class);
	}
	
}
