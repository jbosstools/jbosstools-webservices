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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.jboss.tools.usage.event.UsageEvent;
import org.jboss.tools.usage.event.UsageEventType;
import org.jboss.tools.usage.event.UsageReporter;
import org.jboss.tools.ws.ui.messages.JBossWSUIMessages;
import org.jboss.tools.ws.ui.views.WSType;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class JBossWSUIPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.jboss.tools.ws.ui"; //$NON-NLS-1$

	/**
	 * @since 2.0
	 */
	public static final String WSTYPE_EXTENSION_POINT = "org.jboss.tools.ws.ui.wsType"; //$NON-NLS-1$
	
	/**
	 * @since 2.0
	 */
	public static final String IMG_DESC_START = "obj16/run.gif"; //$NON-NLS-1$;
	/**
	 * @since 2.0
	 */
	public static final String IMG_DESC_SHOWTREE = "obj16/hierarchicalLayout.gif"; //$NON-NLS-1$
	/**
	 * @since 2.0
	 */
	public static final String IMG_DESC_SHOWRAW = "obj16/binary.gif"; //$NON-NLS-1$
	/**
	 * @since 2.0
	 */
	public static final String IMG_DESC_SHOWWEB = "obj16/web.gif"; //$NON-NLS-1$
	/**
	 * @since 2.0
	 */
	public static final String IMG_DESC_SHOWEDITOR = "obj16/properties.gif"; //$NON-NLS-1$
	/**
	 * @since 2.0
	 */
	public static final String IMG_DESC_SAVE = "obj16/save_edit.gif"; //$NON-NLS-1$

	// The shared instance
	private static JBossWSUIPlugin plugin;

	// record event of requests submitted with the WS Tester
	private final UsageEventType requestSubmittedEventType;

	private static List<WSType> wsTypes;

	/**
	 * The constructor
	 */
	public JBossWSUIPlugin() {
		this.requestSubmittedEventType = new UsageEventType(this, "wstester", //$NON-NLS-1$
				"Request method (JAX-WS|GET|POST|PUT|DELETE|OPTIONS)", UsageEventType.HOW_MANY_TIMES_VALUE_DESCRIPTION); //$NON-NLS-1$
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
	public static JBossWSUIPlugin getDefault() {
		return plugin;
	}

	public static ImageDescriptor getImageDescriptor(String path) {
		path = "icons/" + path; //$NON-NLS-1$
		return AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	public static void log(Throwable ex) {
		plugin.getLog().log(
				new Status(Status.ERROR, PLUGIN_ID, Status.OK, JBossWSUIMessages.JBossWS_UI_PLUGIN_NO_MESSAGES, ex));
	}

	private IWorkbenchPage internalGetActivePage() {
		IWorkbenchWindow window = getWorkbench().getActiveWorkbenchWindow();
		if (window == null)
			return null;
		return window.getActivePage();
	}

	private IWorkbenchWindow internalGetActiveWorkbenchWindow() {
		IWorkbenchWindow window = getWorkbench().getActiveWorkbenchWindow();
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

	/**
	 * @since 2.0
	 */
	public static List<WSType> getSupportedWSTypes() {
		if (wsTypes == null) {
			wsTypes = new ArrayList<>();
			IConfigurationElement[] elements = Platform.getExtensionRegistry()
					.getConfigurationElementsFor(WSTYPE_EXTENSION_POINT);
			for (IConfigurationElement e : elements) {
				try {
					final WSType wsType = (WSType) e.createExecutableExtension("class"); //$NON-NLS-1$
					wsTypes.add(wsType);
				} catch (Exception ex) {
					log(ex);
				}
			}
		}
		return wsTypes;
	}
}
