/******************************************************************************* 
 * Copyright (c) 2008 - 2014 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/

package org.jboss.tools.ws.jaxrs.core;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.jdt.core.JavaCore;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JavaElementChangedListener;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.ResourceChangedListener;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsMetamodelChangedListener;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsEndpointDelta;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsMetamodelDelta;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class JBossJaxrsCorePlugin extends Plugin {

	/** The plug-in ID. */
	public static final String PLUGIN_ID = "org.jboss.tools.ws.jaxrs.core"; //$NON-NLS-1$

	/** The shared instance. */
	private static JBossJaxrsCorePlugin plugin;

	/** The Java changes listener. */
	private final JavaElementChangedListener javaElementChangedListener = new JavaElementChangedListener();

	/** The resource changes listener. */
	private final ResourceChangedListener resourceChangedListener = new ResourceChangedListener();

	/** The Listeners for JAX-RS Metamodel changes. */
	private final Set<IJaxrsMetamodelChangedListener> metamodelChangedListeners = new HashSet<IJaxrsMetamodelChangedListener>();

	/**
	 * The constructor.
	 */
	public JBossJaxrsCorePlugin() {
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
	 * {@inheritDoc}
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		unregisterListeners();
		super.stop(context);
	}


	/**
	 * Register the elementChangedListeners.
	 */
	private void registerListeners() {
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
	 * Unregister the elementChangedListeners.
	 */
	private void unregisterListeners() {
		JavaCore.removeElementChangedListener(javaElementChangedListener);
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangedListener);
	}

	/**
	 * Puts the change elementChangedListeners in 'pause' mode so that they won't process any incoming notification.
	 */
	public void pauseListeners() {
		javaElementChangedListener.pause();
		resourceChangedListener.pause();
	}
	
	/**
	 * Puts the change elementChangedListeners back in 'normal' mode so that they will process incoming notifications again.
	 */
	public void resumeListeners() {
		javaElementChangedListener.resume();
		resourceChangedListener.resume();
	}

	/**
	 * Returns the shared instance.
	 * 
	 * @return the shared instance
	 */
	public static JBossJaxrsCorePlugin getDefault() {
		return plugin;
	}
	
	/**
	 * Registers the given listener for further notifications when JAX-RS
	 * Endpoints changed in this metamodel.
	 * 
	 * @param listener
	 */
	public void addJaxrsMetamodelChangedListener(final IJaxrsMetamodelChangedListener listener) {
		if(!metamodelChangedListeners.contains(listener)) { 
			Logger.debug("Registering JaxrsMetamodelChangedListener");
			metamodelChangedListeners.add(listener);
		}
	}

	/**
	 * Unregisters the given listener for further notifications when JAX-RS
	 * Endpoints changed in this metamodel.
	 * 
	 * @param listener
	 */
	public void removeListener(final IJaxrsMetamodelChangedListener listener) {
		metamodelChangedListeners.remove(listener);
	}

	/**
	 * Notify that a JAX-RS Endpoint was added/changed/removed
	 * 
	 * @param endpoint
	 *            the endpoint that was added/changed/removed
	 * @param deltaKind
	 *            the kind of change
	 * @param flags
	 *            some optional flags (use {@link JaxrsElementDelta#F_NONE} if
	 *            no change occurred)
	 */
	public static void notifyEndpointChanged(final IJaxrsEndpoint endpoint, final int deltaKind) {
		if (endpoint != null && !getDefault().metamodelChangedListeners.isEmpty()) {
			final JaxrsEndpointDelta delta = new JaxrsEndpointDelta(endpoint, deltaKind);
			Logger.trace("Notify elementChangedListeners after {}", delta);
			for (IJaxrsMetamodelChangedListener listener : getDefault().metamodelChangedListeners) {
				listener.notifyEndpointChanged(delta);
			}
		} else if(getDefault().metamodelChangedListeners.isEmpty()) {
			Logger.trace(" No Listener to notify about endpoint changed (type={}): {}", deltaKind, endpoint);
		}
	}

	/**
	 * Notifies all registered listeners that the problem level of the given
	 * {@link IJaxrsEndpoint} changed
	 * 
	 * @param element
	 *            the JAX-RS {@link IJaxrsEndpoint} whose problem level changed
	 */
	public static void notifyEndpointProblemLevelChanged(final IJaxrsEndpoint endpoint) {
		Logger.debug("Notifying that problem severity changed to {} for endpoint {} {}", endpoint.getProblemLevel(),
				endpoint.getHttpMethod().getHttpVerb(), endpoint.getUriPathTemplate());
		for (IJaxrsMetamodelChangedListener listener : getDefault().metamodelChangedListeners) {
			listener.notifyEndpointProblemLevelChanged(endpoint);
		}
	}

	/**
	 * Notifies all registered listeners that the problem level of this {@link JaxrsMetamodel} changed
	 */
	public static void notifyMetamodelProblemLevelChanged(final IJaxrsMetamodel metamodel) {
		if(getDefault().metamodelChangedListeners.isEmpty()) {
			Logger.debug("No metamodelChangedListeners to notify that the metamodel problem level changed :(");
			return;
		}
		for (IJaxrsMetamodelChangedListener listener : getDefault().metamodelChangedListeners) {
			listener.notifyMetamodelProblemLevelChanged(metamodel);
		}
	}

	/**
	 * Notifies all registered listeners that this {@link JaxrsMetamodel} changed
	 */
	public static void notifyMetamodelChanged(final IJaxrsMetamodel metamodel, final int deltaKind) {
		if(getDefault().metamodelChangedListeners.isEmpty()) {
			Logger.debug("No metamodelChangedListener to notify of the metamodel changed :(");
			return;
		}
		for (IJaxrsMetamodelChangedListener listener : getDefault().metamodelChangedListeners) {
			listener.notifyMetamodelChanged(new JaxrsMetamodelDelta(metamodel, deltaKind));
		}
	}


}
