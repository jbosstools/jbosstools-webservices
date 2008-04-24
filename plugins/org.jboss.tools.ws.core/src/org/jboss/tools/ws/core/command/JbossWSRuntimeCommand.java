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
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.wst.common.frameworks.datamodel.AbstractDataModelOperation;
import org.jboss.tools.ws.core.JbossWSCoreMessages;
import org.jboss.tools.ws.core.JbossWSCorePlugin;
import org.jboss.tools.ws.core.utils.JbossWSCoreUtils;

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

		IPreferenceStore ps = JbossWSCorePlugin.getDefault()
				.getPreferenceStore();
		runtimeLocation = ps.getString("jbosswsruntimelocation");

		// copy jars to project's folder
		IPath libPath = new Path(runtimeLocation);
		libPath = libPath.append(JbossWSCoreMessages.DIR_LIB);

		IPath targetPath = JbossWSCoreUtils.pathToWebProjectContainer(project
				.toString());
		targetPath = targetPath
				.append(JbossWSCoreMessages.DIR_WEB_INF).append(
						JbossWSCoreMessages.DIR_LIB);
		status = JbossWSCoreUtils.copy(libPath, targetPath);

		return status;
	}

}