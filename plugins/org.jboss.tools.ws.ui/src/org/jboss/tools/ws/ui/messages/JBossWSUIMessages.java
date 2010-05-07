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

	public static String Bin;
	public static String Command;
	public static String Client;
	public static String Lib;
	public static String Endorsed;
	
	public static String JBossRuntimeListFieldEditor_ActionAdd;
	public static String JBossRuntimeListFieldEditor_ActionEdit;
	public static String JBossRuntimeListFieldEditor_ActionRemove;
	public static String JBossRuntimeListFieldEditor_ErrorMessageAtLeastOneJar;
	public static String JBossWS_Runtime_List_Field_Editor_Name;
	public static String JBossWS_Runtime_List_Field_Editor_Version;
	public static String JBossWS_Runtime_List_Field_Editor_Path;
	public static String JBossWS_Runtime_Delete_Confirm_Title;
	public static String JBossWS_Runtime_Delete_Used_Confirm;
	public static String JBossWS_Runtime_Delete_Not_Used_Confirm;
	public static String JBossWS_Runtime_List_Field_Editor_Edit_Runtime;
	public static String JBossWS_Runtime_List_Field_Editor_Modify_Runtime;
	public static String JBossWS_Runtime_List_Field_Editor_New_Runtime;
	public static String JBossWS_Runtime_List_Field_Editor_Runtime_Already_Exists;
	public static String JBossWS_Runtime_List_Field_Editor_Runtime;
	public static String JBossWS_Runtime_List_Field_Editor_Create_A_Runtime;
	public static String JBossWS_Runtime_List_Field_Editor_Home_Folder;
	public static String JBossWS_SWT_Field_Editor_Factory_Select_Home_Folder;
	public static String JBossWS_Runtime_List_Field_Editor_Name2;
	public static String JBossWS_Runtime_Check_Field_Default_Classpath;
	public static String JBossWS_Preference_Page_Runtimes;
	public static String JBossWSLibraryListFieldEditor_ActionAdd;
	public static String JBossWSLibraryListFieldEditor_ActionRemove;
	public static String JBossWSLibraryListFieldEditor_LIBRARY_JARS;
	public static String JBossWS_GenerateWizard_Title;
	public static String JBossWS_GenerateWizard_GenerateWebXmlPage_Title;
	public static String JBossWS_GenerateWizard_GenerateWebXmlPage_Description;
	public static String JBossWS_GenerateWizard_GenerateSampleClassPage_Title;
	public static String JBossWS_GenerateWizard_GenerateSampleClassPage_Description;
	public static String JBossWS_GenerateWizard_GenerateWebXmlPage_ServiceName_Label;
	public static String JBossWS_GenerateWizard_WizardPage_CheckButton_Label;
	public static String JBossWS_GenerateWizard_GenerateSampleClassPage_Package_Label;
	public static String JBossWS_GenerateWizard_GenerateSampleClassPage_ClassName_Label;
	public static String JBossWS_GenerateWizard_MessageDialog_Title;
	public static String JBossWS_UI_PLUGIN_NO_MESSAGES;
	public static String JBossWSGenerateWebXmlWizardPage_Project_Group;
	public static String JBossWSGenerateWebXmlWizardPage_Project_Group_Tooltip;
	public static String JBossWSGenerateWebXmlWizardPage_Web_Service_Group;
	
	public static String Error_JBossWS_GenerateWizard_NoProjectSelected;
	public static String Error_JBossWS_Label_Runtime_Load;
	public static String Error_JBossWS_Basic_Editor_Composite;
	public static String Error_JBossWS_Basic_Editor_Support;
	public static String Error_JBossWS_Runtime_List_Field_Editor_Path_To_Home_Diretory_Cannot_Be_Empty;
	public static String Error_JBossWS_Runtime_List_Field_Editor_Runtime_Name_Is_Not_Correct;
	public static String Error_JBossWS_Runtime_List_Field_Editor_Name_Cannot_Be_Empty;
	public static String Error_JBossWS_Runtime_List_Field_Editor_Inputelement_Must_Be_An_Instance_Of_List;
	public static String Error_JBossWS_GenerateWizard_NotDynamicWebProject;
	public static String Error_JBossWS_GenerateWizard_ClassName_Same;
	public static String Error_JBossWS_GenerateWizard_PackageName_Cannot_Be_Empty;
	public static String Error_JBossWS_GenerateWizard_ClassName_Cannot_Be_Empty;
	public static String Error_JBossWS_GenerateWizard_ServiceName_Empty;
	public static String Error_JBossWS_GenerateWizard_IsOutputFolder;
	public static String Error_JBossWS_GenerateWizard_PackageExists;
	public static String Error_JBossWS_GenerateWizard_PackageNotShown;
	public static String Error_JBossWS_GenerateWizard_PackageExistsDifferentCase;
	public static String Error_JBossWS_GenerateWizard_NoSrcInProject;
	
	public static String JBossWS_DelimitedStringList_EditValue_Dialog_Title;
	public static String JBossWS_DelimitedStringList_EditValue_Dialog_Message;
	public static String JAXRSWSTestView_Action_URL_Label;
	public static String JAXRSWSTestView_Configure_Monitor_Button;
	public static String JAXRSWSTestView_HTTP_Method_Label;
	public static String JAXRSWSTestView_Invoke_Label;
	public static String JAXRSWSTestView_Open_Monitor_Button;
	public static String JAXRSWSTestView_Request_Body_Label;
	public static String JAXRSWSTestView_Request_Header_Label;
	public static String JAXRSWSTestView_Request_Parameters_Label;
	public static String JAXRSWSTestView_Results_Body_Label;
	public static String JAXRSWSTestView_Results_Header_Label;
	public static String JAXRSWSTestView_Service_URL_Label;
	public static String JAXRSWSTestView_Set_Sample_Data_Label;
	public static String JAXRSWSTestView_Web_Service_Type_Label;
	
	static {
		NLS.initializeMessages(BUNDLE_NAME, JBossWSUIMessages.class);
	}
}
