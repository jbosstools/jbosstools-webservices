package org.jboss.tools.ws.jaxrs.ui;

import org.eclipse.osgi.util.NLS;

public class JBossJAXRSUIMessages extends NLS {

	private static final String BUNDLE_NAME = "org.jboss.tools.ws.jaxrs.ui.JBossJAXRSUI"; //$NON-NLS-1$

	private JBossJAXRSUIMessages() {
		// Do not instantiate
	}
	
	public static String DelimitedStringList_Msg_No_Btn;
	public static String DelimitedStringList_Msg_Text_ParseByAmpersand;
	public static String DelimitedStringList_Msg_Text_ParseByComma;
	public static String DelimitedStringList_Msg_Title_ParseByAmpersand;
	public static String DelimitedStringList_Msg_Title_ParseByComma;
	public static String DelimitedStringList_Msg_Yes_Btn;
	public static String DelimitedStringList_NO_EQUALS_DELIMITER_WARNING;
	
	public static String JBossWS_DelimitedStringList_EditValue_Dialog_Title;
	public static String JBossWS_DelimitedStringList_EditValue_Dialog_Message;
	

	public static String JAXRSWSTestView_JAXRS_Success_Status;
	public static String JAXRSRSTestView_Message_Unsuccessful_Test;
	public static String JAXRSWSTestView2_Headers_Section;
	public static String JAXRSWSTestView2_Parameters_Section;
	public static String WSTesterURLInputsDialog_DialogMessage;
	public static String WSTesterURLInputsDialog_DialogTitle;
	public static String WSTesterURLInputsDialog_Type_Column;
	public static String WSTesterURLInputsDialog_URLParms_Label;
	public static String WSTesterURLInputsDialog_URLParms_Mandatory;
	public static String WSTesterURLInputsDialog_Validation_Error_Missing_Value;
	public static String WSTesterURLInputsDialog_Validation_Error_Invalid;
	public static String WSTesterURLInputsDialog_Window_Title;
	
	static {
		NLS.initializeMessages(BUNDLE_NAME, JBossJAXRSUIMessages.class);
	}
}
