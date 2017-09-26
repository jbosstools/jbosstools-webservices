/******************************************************************************* 
 * Copyright (c) 2008 - 2014 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/

package org.jboss.tools.ws.jaxws.core;

import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class JBossJAXWSCorePlugin extends Plugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.jboss.tools.jaxws.core"; //$NON-NLS-1$

	// The shared instance
	private static JBossJAXWSCorePlugin plugin;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.
	 * BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	public void logError(String errorMessage) {
		plugin.getLog().log(new Status(Status.ERROR, PLUGIN_ID, Status.OK, errorMessage, null));
	}

	public void logError(Throwable ex) {
		plugin.getLog().log(new Status(Status.ERROR, PLUGIN_ID, Status.OK, null, ex));
	}

	public void logWarning(String errorMessage) {
		plugin.getLog().log(new Status(Status.WARNING, PLUGIN_ID, Status.OK, errorMessage, null));
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static JBossJAXWSCorePlugin getDefault() {
		return plugin;
	}

}
