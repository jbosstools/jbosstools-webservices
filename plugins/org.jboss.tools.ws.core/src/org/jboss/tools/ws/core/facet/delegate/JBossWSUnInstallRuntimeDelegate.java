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

package org.jboss.tools.ws.core.facet.delegate;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IDelegate;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.jboss.tools.ws.core.command.UninstallJBossWSClassPathCommand;
import org.jboss.tools.ws.core.messages.JBossWSCoreMessages;

/**
 * @author Grid Qian
 */
public class JBossWSUnInstallRuntimeDelegate implements IDelegate {

	public void execute(IProject project, IProjectFacetVersion arg1,
			Object arg2, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(
				JBossWSCoreMessages.Progress_UnInstall_JBossWS_Runtime, 2);

		IDataModel model = (IDataModel) arg2;

		UninstallJBossWSClassPathCommand command = new UninstallJBossWSClassPathCommand(
				project, model);
		command.executeOverride(monitor);
		monitor.worked(1);
		monitor.done();
	}

}
