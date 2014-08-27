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

package org.jboss.tools.ws.ui;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.jboss.tools.usage.event.UsageEvent;
import org.jboss.tools.usage.event.UsageEventType;
import org.jboss.tools.usage.event.UsageReporter;
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
	
	// record event of requests submitted with the WS Tester
	private final UsageEventType requestSubmittedEventType;

	/**
	 * The constructor
	 */
	public JBossWSUIPlugin() {
		this.requestSubmittedEventType = new UsageEventType(this, "wstester", "Request method (JAX-WS|GET|POST|PUT|DELETE|OPTIONS)", UsageEventType.HOW_MANY_TIMES_VALUE_DESCRIPTION); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		UsageReporter.getInstance().registerEvent(requestSubmittedEventType);
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
	
	public static void log(Throwable ex) {
		plugin.getLog().log(new Status(Status.ERROR, PLUGIN_ID, Status.OK, JBossWSUIMessages.JBossWS_UI_PLUGIN_NO_MESSAGES, ex)); 
	}
	
	private IWorkbenchPage internalGetActivePage() {
		IWorkbenchWindow window= getWorkbench().getActiveWorkbenchWindow();
		if (window == null)
			return null;
		return window.getActivePage();
	}
	
	private IWorkbenchWindow internalGetActiveWorkbenchWindow() {
		IWorkbenchWindow window= getWorkbench().getActiveWorkbenchWindow();
		if (window == null)
			return null;
		return window;
	}

	public static IWorkbenchPage getActivePage() {
		return getDefault().internalGetActivePage();
	}
	
	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		return getDefault().internalGetActiveWorkbenchWindow();
	}
	
	/**
	 * Counts a request submission with the Web Service Tester, using the given {@code method}.
	 * @param method the request method to track or count. 
	 */
	public void countRequestSubmitted(final String method) {
		final UsageEvent requestSubmittedEvent = requestSubmittedEventType.event(method);
		UsageReporter.getInstance().countEvent(requestSubmittedEvent);
	}
}
