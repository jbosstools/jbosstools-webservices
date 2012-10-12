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

package org.jboss.tools.ws.jaxrs.core.configuration;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsMetamodelBuilder;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.metamodel.validation.JaxrsMetamodelValidationConstants;

/**
 * Class to configure (add/remove) the JAX-RS Metamodel Builder on a project.
 * 
 * @author xcoulon
 */
public class ProjectBuilderConfigurer implements IProjectNature {

	/** the selected project. */
	private IProject project = null;

	@Override
	public final void configure() throws CoreException {
		if (project == null) {
			return;
		}
		// project nature installation triggers the project builder
		// installation, by configuration/association in the plugin.xml file.
		if (ProjectBuilderUtils.installProjectBuilder(project, JaxrsMetamodelBuilder.BUILDER_ID)) {
			Logger.info("JAX-RS Builder is now installed.");
		} else {
			Logger.info("JAX-RS Builder was already installed.");
		}
	}

	@Override
	public final void deconfigure() throws CoreException {
		if (project == null) {
			return;
		}
		if (ProjectBuilderUtils.uninstallProjectBuilder(project, JaxrsMetamodelBuilder.BUILDER_ID)) {
			Logger.debug("JAX-RS Metamodel Builder is now uninstalled.");
		} else {
			Logger.debug("JAX-RS Metamodel Builder was not installed.");
		}
		project.deleteMarkers(JaxrsMetamodelValidationConstants.JAXRS_PROBLEM_TYPE, true, IResource.DEPTH_INFINITE);
		Logger.debug("JAX-RS Problem markers removed.");
	}

	/** {@inheritDoc} */
	@Override
	public final IProject getProject() {
		return project;
	}

	/** {@inheritDoc} */
	@Override
	public final void setProject(final IProject p) {
		this.project = p;
	}

}
