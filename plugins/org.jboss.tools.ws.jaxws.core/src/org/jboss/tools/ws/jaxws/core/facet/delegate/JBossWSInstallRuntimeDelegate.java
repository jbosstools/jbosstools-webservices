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

package org.jboss.tools.ws.jaxws.core.facet.delegate;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IDelegate;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.jboss.tools.ws.jaxws.core.commands.JBossWSClassPathCommand;
import org.jboss.tools.ws.jaxws.core.messages.JBossJAXWSCoreMessages;

/**
 * @author Grid Qian
 */
public class JBossWSInstallRuntimeDelegate implements IDelegate {

	public void execute(IProject project, IProjectFacetVersion arg1,
			Object arg2, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(JBossJAXWSCoreMessages.Progress_Install_JBossWS_Runtime,
				2);

		IDataModel model = (IDataModel) arg2;

		JBossWSClassPathCommand command = new JBossWSClassPathCommand(project,
				model);
		IStatus status = command.executeOverride(monitor);
		if(!status.equals(Status.OK_STATUS)){
			throw new CoreException(status);
		}
		monitor.worked(1);
		monitor.done();
	}

}
