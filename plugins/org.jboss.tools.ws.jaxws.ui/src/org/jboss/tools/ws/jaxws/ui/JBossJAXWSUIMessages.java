/******************************************************************************* 
 * Copyright (c) 2017 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.ws.jaxws.ui;

import org.eclipse.osgi.util.NLS;

/**
 * @author Grid Qian
 */
public class JBossJAXWSUIMessages extends NLS {

	private static final String BUNDLE_NAME = "org.jboss.tools.ws.jaxws.ui.JBossJAXWSUI"; //$NON-NLS-1$

	private JBossJAXWSUIMessages() {
		// Do not instantiate
	}

	public static String JBossWS_UI_PLUGIN_NO_MESSAGES;
	public static String Webservice_Annotation;
	public static String Error_Message_Failed_to_Generate_Implementation;
	public static String Confirm_Override_ImplClass;
	public static String Error_JBossWS_GenerateWizard_WSImpl_Overwrite;
	public static String Separator_Java;
	public static String Confirm_Override_Servlet;
	public static String Error_JBossWS_GenerateWizard_WSName_Same;
	public static String Value_Target_0;
	public static String Value_Target_1;
	public static String Value_Target_2;
	public static String Label_SourceFolder_Name;
	public static String Tooltip_SourceFolder;
	public static String Error_Message_No_SourceFolder;
	public static String Label_Generate_WSDL;
	public static String Label_Update_Webxml;
	public static String Label_Service_Name;
	public static String Tooltip_Service;
	public static String Error_Message_No_ServicePort;
	public static String Label_Custom_Package_Name;
	public static String Tooltip_Custom_Package;
	public static String Label__Browse_Button;
	public static String Label_JaxWS_Target;
	public static String Tooltip_JaxWS_Target;
	public static String Label_Catalog_File;
	public static String Tooltip_Catalog_File;
	public static String Label_Add_Button;
	public static String Label_EnableSOAP12_Binding_Extension;
	public static String Label_Generate_Impelemtation;
	public static String Label_AdditionalOption_Name;
	public static String Tooltip_AdditionalOption;
	public static String Label_Help_Button;
	public static String AdditionalOption_Dialog_Title;
	public static String Error_Message_No_ServletName;
	public static String Label_Binding_File;
	public static String Tooltip_BindingFile;
	public static String Label_Remove_Button;
	public static String Error_Message_No_Service;
	public static String No_Message_AdditionalOptions_Dialog;
	public static String JBossWS_WebServiceDataModel;
	public static String JBossWSWSDL2JavaConfigWidgetFactory_Description;
	public static String JBossWSWSDL2JavaConfigWidgetFactory_Title;
	public static String JBossWSJava2WSDLConfigWidgetFactory_Description;
	public static String JBossWSJava2WSDLConfigWidgetFactory_Title;
	public static String JBossWS_Runtime_Lib;
	
	public static String Bin;
	public static String Command;
	
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
	public static String Runtime_Details;
	public static String Runtime_Details_Impl;
	public static String Runtime_Details_Version;
	public static String JBossWSLibraryListFieldEditor_ActionAdd;
	public static String JBossWSLibraryListFieldEditor_ActionRemove;
	public static String JBossWSLibraryListFieldEditor_LIBRARY_JARS;
	public static String JBossWS_GenerateWizard_Title;
	public static String JBossWS_GenerateWizard_GenerateWizardPage_Title;
	public static String JBossWS_GenerateWizard_GenerateWizardPage_Description;
	public static String JBossWS_GenerateWizard_GenerateWizardPage_ServiceName_Label;
	public static String JBossWS_GenerateWizard_GenerateWizardPage_Package_Label;
	public static String JBossWS_GenerateWizard_GenerateWizardPage_ClassName_Label;
	public static String JBossWS_GenerateWizard_MessageDialog_Title;
	
	public static String JBossWS_GenerateWizard_GenerateWizardPage_Project_Group;
	public static String JBossWS_GenerateWizard_GenerateWizardPage_Project_Group_Tooltip;
	public static String JBossWS_GenerateWizard_GenerateWizardPage_Web_Service_Group;
	public static String JBossWS_GenerateWizard_GenerateWizardPage_Class_Group;
	
	public static String JBossWSFacetInstallPage_ServerSuppliedJBossWS;
	public static String JBossWSFacetInstallPage_New;
	public static String JBossWSFacetInstallPage_Deploy;
	public static String JBossWSRuntimeInstallPage_NoTargetRuntime;
	public static String JBossWSRuntimeInstallPage_NoValidJBossWSRuntime;
	public static String JBossWSFacetInstallPage_Title;
	public static String JBossWSFacetInstallPage_Description;
	
	public static String Error_JBossWS_GenerateWizard_NoProjectSelected;
	public static String Error_JBossWS_Label_Runtime_Load;
	public static String Error_JBossWS_Basic_Editor_Composite;
	public static String Error_JBossWS_Basic_Editor_Support;
	public static String Error_JBossWS_Runtime_List_Field_Editor_Path_To_Home_Diretory_Cannot_Be_Empty;
	public static String Error_JBossWS_Runtime_List_Field_Editor_Runtime_Name_Is_Not_Correct;
	public static String Error_JBossWS_Runtime_List_Field_Editor_Name_Cannot_Be_Empty;
	public static String Error_JBossWS_Runtime_List_Field_Editor_Inputelement_Must_Be_An_Instance_Of_List;
	public static String Error_JBossWS_GenerateWizard_NotDynamicWebProject;
	public static String Error_JBossWS_GenerateWizard_NotDynamicWebProject2;
	public static String Error_JBossWS_GenerateWizard_NoWebXML;
	public static String Error_JBossWS_GenerateWizard_ClassName_Same;
	public static String Error_JBossWS_GenerateWizard_PackageName_Cannot_Be_Empty;
	public static String Error_JBossWS_GenerateWizard_ClassName_Cannot_Be_Empty;
	public static String Error_JBossWS_GenerateWizard_ServiceName_Empty;
	public static String Error_JBossWS_GenerateWizard_IsOutputFolder;
	public static String Error_JBossWS_GenerateWizard_PackageExists;
	public static String Error_JBossWS_GenerateWizard_PackageNotShown;
	public static String Error_JBossWS_GenerateWizard_PackageExistsDifferentCase;
	public static String Error_JBossWS_GenerateWizard_NoSrcInProject;

	public static String Error_WS_No_Runtime_Specifed;
	public static String Error_WS_Chose_runtime;
	public static String Error_JBossWSRuntimeConfigBlock_Duplicated_Jar;
	
	public static String JBossWSAnnotatedClassWizardPage_package_browse_btn;
	public static String JBossWSAnnotatedClassWizardPage_package_name_field;
	public static String JBossWSAnnotatedClassWizardPage_PageDescription;
	public static String JBossWSAnnotatedClassWizardPage_PageTitle;
	public static String JBossWSAnnotatedClassWizardPage_Project_Group;
	public static String JBossWSAnnotatedClassWizardPage_Projects_Combo_Tooltip;
	public static String JBossWSAnnotatedClassWizardPage_Service_class_Browse_btn;
	public static String JBossWSAnnotatedClassWizardPage_Service_class_field;
	public static String JBossWSAnnotatedClassWizardPage_Service_implementation_group;
	public static String JBossWSAnnotatedClassWizardPage_Service_Name_field;
	public static String JBossWSAnnotatedClassWizardPage_Update_Web_xml_checkbox;
	public static String JBossWSAnnotatedClassWizardPage_Web_Service_Group;
	
	public static String JAXRSWSTestView_Exception_Status;
	public static String JAXRSWSTestView_JAXWS_Success_Status;
	public static String JAXRSWSTestView_Message_Service_Invocation_Cancelled;
	public static String JAXRSWSTestView_Message_Unsuccessful_Test;
	public static String JAXRSWSTestView2_GetFromWSDL_Tooltip;

	public static String JAXRSWSTestView2_Title_Msg_May_Be_Out_of_Date;

	public static String TesterWSDLUtils_WSDL_HTTPS_Secured_Inaccessible;
	public static String TesterWSDLUtils_WSDL_Inaccessible;
	

	public static String WSDLBrowseDialog_Dialog_Title;
	public static String WSDLBrowseDialog_Error_Msg_Invalid_URL;
	public static String WSDLBrowseDialog_Error_Msg_Parse_Error;
	public static String WSDLBrowseDialog_Error_Retrieving_WSDL;
	public static String WSDLBrowseDialog_FS_Browse;
	public static String WSDLBrowseDialog_Group_Title;
	public static String WSDLBrowseDialog_Message;
	public static String WSDLBrowseDialog_No_Services_Available;
	public static String WSDLBrowseDialog_No_Services_Available_Warning;
	public static String WSDLBrowseDialog_Operation_Field;
	public static String WSDLBrowseDialog_Port_Field;
	public static String WSDLBrowseDialog_Service_Field;
	public static String WSDLBrowseDialog_Status_Invalid_URL;
	public static String WSDLBrowseDialog_Status_ParsingWSDLFromURL;
	public static String WSDLBrowseDialog_Title;
	public static String WSDLBrowseDialog_URL_Browse;
	public static String WSDLBrowseDialog_WS_Browse;
	public static String WSDLBrowseDialog_WS_Browse_Msg;
	public static String WSDLBrowseDialog_WS_Browse_Select_WSDL_Msg;
	public static String WSDLBrowseDialog_WS_Browse_Select_WSDL_Title;
	public static String WSDLBrowseDialog_WSDL_URI_Field;
	public static String WSDLBrowseDialog_WSDL_URL_Dialog_Title;
	public static String WSDLBrowseDialog_WSDL_URL_Prompt;
	public static String JAXRSWSTestView2_Text_Msg_May_Be_Out_of_Date;
	public static String JBossWSAnnotatedClassWizard_Annotated_Class_WS_Wizard_Title;
	
	static {
		NLS.initializeMessages(BUNDLE_NAME, JBossJAXWSUIMessages.class);
	}
}
