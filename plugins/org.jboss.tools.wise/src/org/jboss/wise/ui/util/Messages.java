package org.jboss.wise.ui.util;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.jboss.wise.ui.util.messages"; //$NON-NLS-1$
    public static String WiseUtil_Generating_SOAP_task;
    public static String WiseUtil_Job_header;
    public static String WiseUtil_WISE_task;
    public static String WiseUtil_WSDL_task;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
