/******************************************************************************* 
 * Copyright (c) 2008 Red Hat, Inc. 
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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.common.project.facet.core.IDelegate;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.jboss.tools.ws.core.JbossWSCoreMessages;
import org.jboss.tools.ws.core.command.JbossWSRuntimeCommand;

public class JbossWSInstallRuntimeDelegate implements IDelegate {

	private IStatus status;

	public void execute(IProject project, IProjectFacetVersion arg1, Object arg2,
			IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(JbossWSCoreMessages.PROGRESS_INSTALL_JBOSSWS_RUNTIME, 2 );

		JbossWSRuntimeCommand command = new JbossWSRuntimeCommand(project); 
		status = command.executeOverride(monitor);
//		if (status.getCode() == Status.OK_STATUS.getCode() ){
//			RuntimePropertyUtils.writeServerStausToPropertiesFile(
//					Axis2Constants.SERVER_STATUS_PASS);
//		}else{
//			RuntimePropertyUtils.writeServerStausToPropertiesFile(
//					Axis2Constants.SERVER_STATUS_FAIL);
//			throw new CoreException(status);
//		}
		monitor.worked( 1 );
		monitor.done();
	}

}
