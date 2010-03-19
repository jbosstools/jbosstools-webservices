package org.jboss.tools.ws.creation.ui.messages;

import org.eclipse.osgi.util.NLS;

public class JBossWSCreationUIMessages extends NLS {
	private static final String BUNDLE_NAME = "org.jboss.tools.ws.creation.ui.messages.JBossWSCreationUI"; //$NON-NLS-1$

	private JBossWSCreationUIMessages() {
	}
	public static String JBossWSConfigWidgetFactory_Description;
	public static String JBossWSConfigWidgetFactory_Title;
	public static String JBossWSProviderInvokeConfigWidgetFactory_Description;
	public static String JBossWSProviderInvokeConfigWidgetFactory_Title;
	
	static {
		NLS.initializeMessages(BUNDLE_NAME, JBossWSCreationUIMessages.class);
	}
}
