package org.jboss.tools.ws.core;

import org.eclipse.osgi.util.NLS;

public class JbossWSCoreMessage {

	private static final String BUNDLE_NAME = "org.jboss.tools.ws.ui.JbossWSCore"; //$NON-NLS-1$

	private JbossWSCoreMessage() {
		// Do not instantiate
	}

	public static String PROGRESS_INSTALL_JBOSSWS_RUNTIME;


	static {
		NLS.initializeMessages(BUNDLE_NAME, JbossWSCoreMessage.class);
	}
}