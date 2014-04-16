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

package org.jboss.tools.ws.jaxrs.ui.messages;

import org.eclipse.osgi.util.NLS;

/**
 * @author Grid Qian
 */
public class JBossWSUIMessages extends NLS {

	private static final String BUNDLE_NAME = "org.jboss.tools.ws.jaxrs.ui.messages.JBossWSUI";		//$NON-NLS-1$

	private JBossWSUIMessages() {
		// Do not instantiate
	}

	public static String JBossRSGenerateWizard_RS_Wizard_Window_Title;
	public static String JBossRSGenerateWizardPage_AddJarsIfFoundCheckbox;
	public static String JBossRSGenerateWizardPage_Error_RestEasyJarsNotFoundInRuntime;
	public static String JBossRSGenerateWizardPage_Label_Application_Class_Name;
	public static String JBossRSGenerateWizardPage_Page_title;
	public static String JBossRSGenerateWizardPage_ServiceName_Tooltip;
	public static String JBossRSGenerateWizardPage_UpdateWebXMLCheckbox;
	public static String JBossRSGenerateWizardValidator_ERROR_Can_Only_Add_Sample_Once;
	
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

	public static String JBossWSAnnotatedClassWizard_Annotated_Class_WS_Wizard_Title;
	public static String JBossWSAnnotatedClassWizardPage_Application_Class_Browse_btn;
	public static String JBossWSAnnotatedClassWizardPage_Application_Class_field;
	public static String JBossWSAnnotatedClassWizardPage_JAXRS_Button;
	public static String JBossWSAnnotatedClassWizardPage_JAXWS_Button;
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
	public static String JBossWSAnnotatedClassWizardPage_WS_Tech_Group;
	
	static {
		NLS.initializeMessages(BUNDLE_NAME, JBossWSUIMessages.class);
	}
}
