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

package org.jboss.tools.ws.jaxrs.ui.configuration;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Base class to use to add/remove the JAX-RS nature on a project
 * @author xcoulon
 *
 */
public abstract class AbstractJaxrsNatureConfigurationAction implements IObjectActionDelegate {

	/** the currently selected project. */
	IProject selectedProject = null;
	
	@Override
	public void run(final IAction action) {
		configure(this.selectedProject);
	}

	/**
	 * Triggers the real configuration call on the project (ie, add or remove the JAX-RS nature, as defined in subclasses).
	 * @param project the project to configure
	 * @return {@code true} if the project configuration changed, {@code false} otherwise.
	 * @throws CoreException 
	 */
	public abstract boolean configure(final IProject project);

	/**
	 * Called when the selection changes.
	 * 
	 * @param action
	 *            the current action (not used).
	 * @param s
	 *            the current selection.
	 */
	@Override
	public final void selectionChanged(final IAction action, final ISelection selection) {
		if(selection instanceof IStructuredSelection && ((IStructuredSelection) selection).getFirstElement() instanceof IProject) {
			this.selectedProject = (IProject) ((IStructuredSelection) selection).getFirstElement();
		}
	}

	@Override
	public void setActivePart(final IAction action, final IWorkbenchPart targetPart) {
	}

}
