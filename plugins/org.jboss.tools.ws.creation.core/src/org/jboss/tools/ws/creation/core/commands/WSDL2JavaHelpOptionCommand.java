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
package org.jboss.tools.ws.creation.core.commands;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.ws.core.utils.StatusUtils;
import org.jboss.tools.ws.creation.core.data.ServiceModel;
import org.jboss.tools.ws.creation.core.messages.JBossWSCreationCoreMessages;
import org.jboss.tools.ws.creation.core.utils.JBossWSCreationUtils;

public class WSDL2JavaHelpOptionCommand extends WSDL2JavaCommand {
	
	private String helpOptions;
	private Thread thread;

	public WSDL2JavaHelpOptionCommand(ServiceModel model){
		super(model);
	}
	protected void addCommandlineArgs(List<String> command) {
		command.add("-h"); //$NON-NLS-1$
	}
	public IStatus execute(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {
		IStatus status = Status.OK_STATUS;
		IProject project = model.getJavaProject().getProject();
		String runtimeLocation;
		try {
			runtimeLocation = JBossWSCreationUtils.getJBossWSRuntimeLocation(project);
		} catch (CoreException e1) {
			return StatusUtils.errorStatus(e1);
		}
		String commandLocation = runtimeLocation + Path.SEPARATOR+ "bin"; //$NON-NLS-1$
		IPath path = new Path(commandLocation);
		List<String> command = new ArrayList<String>();
		String[] env = getEnvironmentVariables(model.getJavaProject());
		if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) { //$NON-NLS-1$ //$NON-NLS-2$
			command.add("cmd.exe"); //$NON-NLS-1$
			command.add("/c"); //$NON-NLS-1$
			command.add(getCommandLineFileName_win());
			path = path.append(getCommandLineFileName_win());
		} else {
			command.add("sh"); //$NON-NLS-1$
			command.add(getCommandLineFileName_linux());
			path = path.append(getCommandLineFileName_linux());
		}
		if (!path.toFile().getAbsoluteFile().exists()) {
			return StatusUtils.errorStatus(NLS.bind(JBossWSCreationCoreMessages.Error_Message_Command_File_Not_Found, new String[] { path.toOSString() }));
		}
		addCommandlineArgs(command);
		Process proc = null;
		try {
			proc = DebugPlugin.exec(command.toArray(new String[command.size()]), new File(commandLocation), env);
		} catch (CoreException e) {
			return StatusUtils.errorStatus(e);
		}
		convertInputStreamToString(proc.getInputStream());
		return status;
	}
	
	public void convertInputStreamToString(final InputStream input) {
		final StringBuffer result = new StringBuffer();
		thread = new Thread() {
			public void run() {
				try {
					InputStreamReader ir = new InputStreamReader(input);
					LineNumberReader reader = new LineNumberReader(ir);
					String str;
					str = reader.readLine();
					boolean boo = false;
					while (str != null) {
						if (!boo && !"options:".equals(str.toLowerCase().trim())) { //$NON-NLS-1$
							str = reader.readLine();
							continue;
						} 
						if (!boo) {
							boo = true;
						}
						if (JBossWSCreationUtils.isOptions(str)) {
							str = str.replaceAll(" +", "   "); //$NON-NLS-1$ //$NON-NLS-2$
						    result.append(str).append("\n"); //$NON-NLS-1$
						}
						str = reader.readLine();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				helpOptions = result.toString();
			}
		};
		thread.start();
	}
	public Thread getThread() {
		return thread;
	}
	
	public String getHelpOptions() {
		return helpOptions;
	}
}
