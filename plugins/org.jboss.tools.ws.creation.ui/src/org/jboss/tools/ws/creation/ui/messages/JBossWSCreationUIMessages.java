package org.jboss.tools.ws.creation.ui.messages;

import org.eclipse.osgi.util.NLS;

public class JBossWSCreationUIMessages extends NLS {
	private static final String BUNDLE_NAME = "org.jboss.tools.ws.creation.ui.messages.JBossWSCreationUI"; //$NON-NLS-1$

	private JBossWSCreationUIMessages() {
	}
	
	public static String JBossWS_WebServiceDataModel;
	
	public static String JBossWSWSDL2JavaConfigWidgetFactory_Description;
	public static String JBossWSWSDL2JavaConfigWidgetFactory_Title;
	public static String JBossWSJava2WSDLConfigWidgetFactory_Description;
	public static String JBossWSJava2WSDLConfigWidgetFactory_Title;
	
	static {
		NLS.initializeMessages(BUNDLE_NAME, JBossWSCreationUIMessages.class);
	}
}
