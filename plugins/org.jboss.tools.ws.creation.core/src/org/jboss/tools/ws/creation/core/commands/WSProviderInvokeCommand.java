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

package org.jboss.tools.ws.creation.core.commands;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.common.frameworks.datamodel.AbstractDataModelOperation;
import org.jboss.tools.ws.core.utils.StatusUtils;
import org.jboss.tools.ws.creation.core.JBossWSCreationCore;
import org.jboss.tools.ws.creation.core.data.ServiceModel;
import org.jboss.tools.ws.creation.core.utils.JBossWSCreationUtils;

/**
 * @author Grid Qian
 */
public class WSProviderInvokeCommand extends AbstractDataModelOperation {

	private ServiceModel model;

	public WSProviderInvokeCommand(ServiceModel model) {
		this.model = model;
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {

		String runtimeLocation;
		try {
			runtimeLocation = JBossWSCreationUtils.getJbossWSRuntimeLocation(ResourcesPlugin.getWorkspace().getRoot().getProject(model
					.getWebProjectName()));
		} catch (CoreException e1) {
			JBossWSCreationCore.getDefault().logError(e1);
			//unable to get runtime location
			return e1.getStatus();
		}
		String commandLocation = runtimeLocation + Path.SEPARATOR + "bin";
		String command = "sh wsprovide.sh ";
		if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) {
			command = "cmd.exe /C wsprovide.bat";
		}
		String args = getCommandlineArgs();
		command += " -k " + args;

		try {
			Runtime rt = Runtime.getRuntime();
			Process proc = rt.exec(command, null, new File(commandLocation));
			InputStreamReader ir = new InputStreamReader(proc.getErrorStream());
			LineNumberReader input = new LineNumberReader(ir);
			String str = input.readLine();
			StringBuffer result = new StringBuffer();
			while (str != null) {
				result.append(str).append("\t\r");
				str = input.readLine();

			}
			int exitValue = proc.waitFor();
			if (exitValue != 0) {
				return StatusUtils.errorStatus(result.toString());
			}

		} catch (IOException e) {
			JBossWSCreationCore.getDefault().logError(e);
		} catch (InterruptedException e) {
			JBossWSCreationCore.getDefault().logError(e);
		}

		refreshProject(model.getWebProjectName(), monitor);

		return Status.OK_STATUS;
	}

	private void refreshProject(String project, IProgressMonitor monitor) {
		try {
			JBossWSCreationUtils.getProjectByName(project).refreshLocal(2,
					monitor);
		} catch (CoreException e) {
			JBossWSCreationCore.getDefault().logError(e);
		}
	}

	private String getCommandlineArgs() {
		String commandLine;
		String project = model.getWebProjectName();
		String projectRoot = JBossWSCreationUtils.getProjectRoot(project)
				.toOSString();
		commandLine = "-s " + projectRoot + Path.SEPARATOR + "src";

		if (model.isGenWSDL()) {
			commandLine += " -w ";
		}

		commandLine += " -o " + projectRoot + Path.SEPARATOR
				+ "build/classes/ ";

		commandLine += " -r " + projectRoot + Path.SEPARATOR + "WebContent"
				+ Path.SEPARATOR + "wsdl ";

		commandLine += " -c " + projectRoot + Path.SEPARATOR
				+ "build/classes/ ";

		commandLine += model.getServiceClasses().get(0);

		return commandLine;

	}
}
