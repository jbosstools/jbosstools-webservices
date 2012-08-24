/******************************************************************************* 
 * Copyright (c) 2008 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/

package org.jboss.tools.ws.jaxrs.core;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JavaElementChangedListener;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.ResourceChangedListener;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class JBossJaxrsCorePlugin extends AbstractUIPlugin {

	/** The plug-in ID. */
	public static final String PLUGIN_ID = "org.jboss.tools.ws.jaxrs.core"; //$NON-NLS-1$

	/** The shared instance. */
	private static JBossJaxrsCorePlugin plugin;

	/** The Java changes listener. */
	private final JavaElementChangedListener javaElementChangedListener = new JavaElementChangedListener();

	/** The resource changes listener. */
	private final ResourceChangedListener resourceChangedListener = new ResourceChangedListener();

	/**
	 * The constructor.
	 */
	public JBossJaxrsCorePlugin() {
	}

	/**
	 * Register the listeners.
	 */
	public void registerListeners() {
		// the java changes are only captured during POST_RECONCILE (ie, during
		// live coding)
		JavaCore.addElementChangedListener(javaElementChangedListener);
		// the resource changes are only captured during POST_CHANGE (ie, when
		// the resource is saved, whatever the mean of changes in the file -
		// editor, refactoring, etc.)
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceChangedListener,
				IResourceChangeEvent.PRE_CLOSE);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		unregisterListeners();
		super.stop(context);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		registerListeners();
	}

	/**
	 * Unregister the listeners.
	 */
	public void unregisterListeners() {
		JavaCore.removeElementChangedListener(javaElementChangedListener);
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangedListener);
	}

	/**
	 * Returns the shared instance.
	 * 
	 * @return the shared instance
	 */
	public static JBossJaxrsCorePlugin getDefault() {
		return plugin;
	}

}
