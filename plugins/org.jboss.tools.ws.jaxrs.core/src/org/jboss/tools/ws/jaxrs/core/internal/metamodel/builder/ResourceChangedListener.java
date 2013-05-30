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
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsMetamodelLocator;

/**
 * @author Xavier Coulon
 * 
 */
public class ResourceChangedListener implements IResourceChangeListener {

	/** Listener state. */
	private boolean active = true;

	/**
	 * Stops processing the incoming {@link IResourceChangeEvent} in the implemented {@link IResourceChangeListener#resourceChanged(IResourceChangeEvent)} method
	 */
	public void pause() {
		this.active = false;
	}

	/**
	 * Stops processing the incoming {@link JavaElementDelta}
	 */
	public void resume() {
		this.active = true;
	}
	
	/**
	 * @see
	 * org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org
	 * .eclipse.core.resources.IResourceChangeEvent)
	 */
	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		if(!active) {
			return;
		}
		try {
			if (event.getType() == IResourceChangeEvent.PRE_CLOSE && event.getResource() != null
					&& event.getResource().getType() == IResource.PROJECT) {
				final IProject project = (IProject) event.getResource();
				final JaxrsMetamodel jaxrsMetamodel = JaxrsMetamodelLocator.get(project);
				if (jaxrsMetamodel != null) {
					Logger.debug("Removing JAX-RS Metamodel before closing project " + project.getName());
					jaxrsMetamodel.remove();
				}
			}
		} catch (Exception e) {
			Logger.error("Error while removing JAX-RS Metamodel", e);
		}

	}
}
