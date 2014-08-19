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

package org.jboss.tools.ws.jaxrs.ui.configuration;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.jboss.tools.ws.jaxrs.core.configuration.ProjectNatureUtils;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.ui.internal.validation.JaxrsMetamodelValidator;

/**
 * Action to remove the JAXRS Nature from a selected project.
 * 
 * @author xcoulon
 * 
 */
public class RemoveNatureAction implements IObjectActionDelegate {

	/** The current selection (a project). */
	private ISelection selection;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void run(final IAction action) {
		try {
			if (selection instanceof IStructuredSelection) {
				final IProject project = (IProject) ((IStructuredSelection) selection).getFirstElement();
				// remove the JAX-RS nature
				ProjectNatureUtils.uninstallProjectNature(
						project,
						ProjectNatureUtils.JAXRS_NATURE_ID);
				// remove all remaining JAX-RS markers
				project.deleteMarkers(JaxrsMetamodelValidator.JAXRS_PROBLEM_MARKER_ID, true, IResource.DEPTH_INFINITE);
			} else {
				Logger.error("Cannot remove JAX-RS support on selection of type " + selection.getClass().getName());
			}
		} catch (CoreException e) {
			Logger.error("Failed to remove support for JAX-RS in project", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void selectionChanged(final IAction action, final ISelection s) {
		this.selection = s;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setActivePart(final IAction action, final IWorkbenchPart targetPart) {
	}

}
