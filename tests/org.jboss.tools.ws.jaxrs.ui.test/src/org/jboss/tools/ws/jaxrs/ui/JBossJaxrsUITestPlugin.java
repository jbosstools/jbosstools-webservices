package org.jboss.tools.ws.jaxrs.ui;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

public class JBossJaxrsUITestPlugin extends Plugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.jboss.tools.ws.jaxrs.ui.test"; //$NON-NLS-1$

	// The shared instance
	private static JBossJaxrsUITestPlugin plugin;
	
	/**
	 * The constructor
	 */
	public JBossJaxrsUITestPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static JBossJaxrsUITestPlugin getDefault() {
		return plugin;
	}
}
