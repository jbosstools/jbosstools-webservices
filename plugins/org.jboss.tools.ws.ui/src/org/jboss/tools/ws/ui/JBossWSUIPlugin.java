/******************************************************************************* 
 * Copyright (c) 2008 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/

package org.jboss.tools.ws.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.jboss.tools.ws.ui.messages.JBossWSUIMessages;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class JBossWSUIPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.jboss.tools.ws.ui"; //$NON-NLS-1$

	// The shared instance
	private static JBossWSUIPlugin plugin;
	
	/**
	 * The constructor
	 */
	public JBossWSUIPlugin() {
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
	public static JBossWSUIPlugin getDefault() {
		return plugin;
	}

	public static ImageDescriptor getImageDescriptor(String path) {
		path = "icons/" + path; //$NON-NLS-1$
		return AbstractUIPlugin.imageDescriptorFromPlugin("org.jboss.tools.ws.ui", path); //$NON-NLS-1$
	}

	public static boolean isDebugEnabled() {
		return plugin.isDebugging();
	}
	
	public static void log(String msg) {
		if(isDebugEnabled()) plugin.getLog().log(new Status(Status.INFO, PLUGIN_ID, Status.OK, msg, null));		
	}
	
	public static void log(IStatus status) {
		if(isDebugEnabled() || !status.isOK()) plugin.getLog().log(status);
	}
	
	public static void log(String message, Throwable exception) {
		plugin.getLog().log(new Status(Status.ERROR, PLUGIN_ID, Status.OK, message, exception));		
	}
	
	public static void log(Throwable ex) {
		plugin.getLog().log(new Status(Status.ERROR, PLUGIN_ID, Status.OK, JBossWSUIMessages.JBossWS_UI_PLUGIN_NO_MESSAGES, ex)); 
	}

	public static Shell getShell() {
		return plugin.getWorkbench().getActiveWorkbenchWindow().getShell();
	}
}
