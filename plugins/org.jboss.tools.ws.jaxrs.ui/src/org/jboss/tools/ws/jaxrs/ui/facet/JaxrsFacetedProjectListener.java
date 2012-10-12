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

package org.jboss.tools.ws.jaxrs.ui.facet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.common.project.facet.core.events.IFacetedProjectEvent;
import org.eclipse.wst.common.project.facet.core.events.IFacetedProjectListener;
import org.eclipse.wst.common.project.facet.core.events.IProjectFacetActionEvent;
import org.jboss.tools.ws.jaxrs.core.configuration.ProjectNatureUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.validation.JaxrsMetamodelValidationConstants;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;

/**
 * Listener for specific Facet events, to bind the JAX-RS Nature with the JAX-RS
 * Facet.
 * 
 * @author xcoulon
 * 
 */
public class JaxrsFacetedProjectListener implements IFacetedProjectListener {

	/**
	 * Adds the JAX-RS Nature to the project (if it was not yet done by the
	 * user) at <code>POST_INSTALL</code> time, and removes the JAX-RS Nature
	 * from the project (if it was not yet done by the user) at
	 * <code>POST_UNINSTALL</code> time.
	 * 
	 * @see org.eclipse.wst.common.project.facet.core.events.IFacetedProjectListener#handleEvent(org.eclipse.wst.common.project.facet.core.events.IFacetedProjectEvent)
	 */
	@Override
	public void handleEvent(IFacetedProjectEvent e) {
		if (e instanceof IProjectFacetActionEvent) {
			IProjectFacetActionEvent event = (IProjectFacetActionEvent) e;
			if (event.getProjectFacet().getId().equals("jst.jaxrs")
					&& event.getProjectFacetVersion().getVersionString().equals("1.1")) {
				try {
					IProject project = event.getProject().getProject();
					switch (e.getType()) {
					case POST_INSTALL:
						if (!ProjectNatureUtils
								.isProjectNatureInstalled(project, ProjectNatureUtils.JAXRS_NATURE_ID)) {
							ProjectNatureUtils.installProjectNature(project, ProjectNatureUtils.JAXRS_NATURE_ID);

						}
						break;
					case POST_UNINSTALL:
						if (ProjectNatureUtils.isProjectNatureInstalled(project, ProjectNatureUtils.JAXRS_NATURE_ID)) {
							ProjectNatureUtils.uninstallProjectNature(project, ProjectNatureUtils.JAXRS_NATURE_ID);
						}
						project.deleteMarkers(JaxrsMetamodelValidationConstants.JAXRS_PROBLEM_TYPE, true, IResource.DEPTH_INFINITE);
						Logger.debug("JAX-RS Problem markers removed.");

						break;
					default:
						break;
					}
				} catch (CoreException ex) {
					Logger.error("Failed to add or remove JAX-RS 1.1 support Nature", ex);
				}
			}
		}
	}

}
