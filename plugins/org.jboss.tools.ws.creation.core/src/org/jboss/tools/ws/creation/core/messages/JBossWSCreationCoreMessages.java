package org.jboss.tools.ws.creation.core.messages;

import org.eclipse.osgi.util.NLS;

public class JBossWSCreationCoreMessages extends NLS {
	private static final String BUNDLE_NAME = "org.jboss.tools.ws.creation.core.messages.JBossWSCreationCore"; //$NON-NLS-1$

	public static String LABEL_CUSTOM_PACKAGE_NAME;
	public static String LABEL_CATALOG_FILE;
	public static String LABEL_BUTTON_TEXT_SELECTION;  
	public static String LABEL_BINDING_FILE;
	public static String LABEL_JAXWS_TARGET;
	public static String VALUE_TARGET_0;
	public static String VALUE_TARGET_1;
	public static String LABEL_BUTTON_TEXT_REMOVE;

	public static String ERROR_MESSAGE_INVALID_BINDING_FILE;
	public static String ERROR_READ_BINDING_FILE;

	private JBossWSCreationCoreMessages() {
	}

	static {
		NLS.initializeMessages(BUNDLE_NAME, JBossWSCreationCoreMessages.class);
	}
	
}
