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

package org.jboss.tools.ws.jaxrs.ui;

import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class JBossJaxrsUIPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.jboss.tools.ws.jaxrs.ui"; //$NON-NLS-1$

	// The shared instance
	private static JBossJaxrsUIPlugin plugin;
	
	/**
	 * The constructor
	 */
	public JBossJaxrsUIPlugin() {
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
	public static JBossJaxrsUIPlugin getDefault() {
		return plugin;
	}
	
	/**
	 * Creates an image by loading it from a file in the plugin's images
	 * directory.
	 * 
	 * @param imagePath path to the image, relative to the /icons directory of the plugin
	 * @return The image object loaded from the image file
	 */
	public final Image createImage(final String imagePath) {
		return createImageDescriptor(imagePath).createImage();
	}
	
	/**
	 * Creates an image descriptor by loading it from a file in the plugin's images
	 * directory.
	 * 
	 * @param imagePath path to the image, relative to the /icons directory of the plugin
	 * @return The image object loaded from the image file
	 */
	public final ImageDescriptor createImageDescriptor(final String imagePath) {
		IPath imageFilePath = new Path("/icons/" + imagePath);
		URL imageFileUrl = FileLocator.find(this.getBundle(), imageFilePath, null);
		return ImageDescriptor.createFromURL(imageFileUrl);
	}

}
