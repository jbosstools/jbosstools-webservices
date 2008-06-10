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

package org.jboss.tools.ws.core.command;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.common.frameworks.datamodel.AbstractDataModelOperation;
import org.jboss.tools.ws.core.messages.JbossWSCoreMessages;
import org.jboss.tools.ws.core.utils.JbossWSCoreUtils;
import org.jboss.tools.ws.core.utils.StatusUtils;

/**
 * @author Grid Qian
 */
public class JbossWSRuntimeCommand extends AbstractDataModelOperation {

	IProject project;
	String runtimeLocation;

	public JbossWSRuntimeCommand(IProject project) {
		this.project = project;
	}

	public IStatus execute(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {
		return executeOverride(monitor);
	}

	public IStatus executeOverride(IProgressMonitor monitor) {
		IStatus status = Status.OK_STATUS;

		// copy lib jars to project's folder
		IPath wsPath = new Path("");
		IPath libPath = null;
		try {
			libPath = wsPath.append(JbossWSCoreMessages.Dir_Lib);
		} catch (Exception e) {
			status = StatusUtils.errorStatus(NLS.bind(
					JbossWSCoreMessages.Error_WS_Location, new String[] { e
							.getLocalizedMessage() }), e);
			return status;
		}
		IPath targetPath = JbossWSCoreUtils.pathToWebProjectContainer(project
				.toString());
		targetPath = targetPath.append(JbossWSCoreMessages.Dir_Web_Inf).append(
				JbossWSCoreMessages.Dir_Lib);
		status = JbossWSCoreUtils.copy(libPath, targetPath);

		if (status == Status.OK_STATUS) {
			// copy client jars to project's folder
			libPath = wsPath.append(JbossWSCoreMessages.Dir_Client);
			status = JbossWSCoreUtils.copy(libPath, targetPath);
		}

		return status;
	}

}