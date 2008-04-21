package org.jboss.tools.ws.core;

import org.eclipse.osgi.util.NLS;

public class JbossWSCoreMessages {

	private static final String BUNDLE_NAME = "org.jboss.tools.ws.core.JbossWSCore"; //$NON-NLS-1$

	private JbossWSCoreMessages() {
		// Do not instantiate
	}

	public static String PROGRESS_INSTALL_JBOSSWS_RUNTIME;
	public static String DIR_LIB;
	public static String DIR_WEB_INF;
	public static String DIR_WEB_CONTENT;


	static {
		NLS.initializeMessages(BUNDLE_NAME, JbossWSCoreMessages.class);
	}
}