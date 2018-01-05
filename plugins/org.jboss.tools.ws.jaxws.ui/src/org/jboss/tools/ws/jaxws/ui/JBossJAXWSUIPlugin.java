/******************************************************************************* 
 * Copyright (c) 2017 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.ws.jaxws.ui;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.jboss.tools.usage.event.UsageEvent;
import org.jboss.tools.usage.event.UsageEventType;
import org.jboss.tools.usage.event.UsageReporter;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class JBossJAXWSUIPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.jboss.tools.ws.jaxws.ui"; //$NON-NLS-1$
	public static final String JAX_WS = "JAX-WS"; //$NON-NLS-1$
	public static final String IMG_DESC_WSDL = "obj16/wsdl.gif"; //$NON-NLS-1$

	// The shared instance
	private static JBossJAXWSUIPlugin plugin;

	// record event of requests submitted with the WS Tester
	private final UsageEventType requestSubmittedEventType;
	
	private long generateTime;

	/**
	 * The constructor
	 */
	public JBossJAXWSUIPlugin() {
		this.requestSubmittedEventType = new UsageEventType(this, "jaxwstester", "Request method (JAX-WS)", //$NON-NLS-1$ //$NON-NLS-2$
				UsageEventType.HOW_MANY_TIMES_VALUE_DESCRIPTION);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.
	 * BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		UsageReporter.getInstance().registerEvent(requestSubmittedEventType);
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

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static JBossJAXWSUIPlugin getDefault() {
		return plugin;
	}

	public static ImageDescriptor getImageDescriptor(String path) {
		path = "icons/" + path; //$NON-NLS-1$
		return AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, path); //$NON-NLS-1$
	}

	public void logError(Throwable ex) {
		plugin.getLog().log(
				new Status(Status.ERROR, PLUGIN_ID, Status.OK, JBossJAXWSUIMessages.JBossWS_UI_PLUGIN_NO_MESSAGES, ex));
	}
	
	public void logError(String message) {
		plugin.getLog().log(
				new Status(Status.ERROR, PLUGIN_ID, Status.OK, message, null));
	}
	
	public void logWarning(String errorMessage) {
		plugin.getLog().log(new Status(Status.WARNING, PLUGIN_ID, Status.OK, errorMessage, null));
	}

	/**
	 * Counts a request submission with the Web Service Tester, using the given
	 * {@code method}.
	 * 
	 * @param method
	 *            the request method to track or count.
	 */
	public void countRequestSubmitted(final String method) {
		final UsageEvent requestSubmittedEvent = requestSubmittedEventType.event(method);
		UsageReporter.getInstance().countEvent(requestSubmittedEvent);
	}
	
	public long getGenerateTime() {
		return generateTime;
	}

	public void setGenerateTime(long generateTime) {
		this.generateTime = generateTime;
	}
}
