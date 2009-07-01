package org.jboss.tools.ws.creation.ui;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.jboss.tools.ws.creation.ui.messages"; //$NON-NLS-1$
	public static String JBossWSConfigWidgetFactory_Description;
	public static String JBossWSConfigWidgetFactory_Title;
	public static String JBossWSProviderInvokeConfigWidgetFactory_Description;
	public static String JBossWSProviderInvokeConfigWidgetFactory_Title;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
