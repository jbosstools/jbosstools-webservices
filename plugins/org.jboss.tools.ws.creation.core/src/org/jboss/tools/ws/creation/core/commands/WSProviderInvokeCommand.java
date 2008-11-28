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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jst.server.core.internal.RuntimeClasspathContainer;
import org.jboss.tools.ws.core.classpath.JBossWSRuntimeClassPathInitializer.JBossWSRuntimeClasspathContainer;
import org.jboss.tools.ws.creation.core.data.ServiceModel;
import org.jboss.tools.ws.creation.core.utils.JBossWSCreationUtils;

/**
 * @author Grid Qian
 */
@SuppressWarnings("restriction")
public class WSProviderInvokeCommand extends AbstractGenerateCodeCommand {

	private static String WSPROVIDER_FILE_NAME_LINUX = "wsprovide.sh";
	private static String WSPROVIDER_FILE_NAME_WIN = "wsprovide.bat";
	private static String SEPERATOR_WIN = ";";
	private static String SEPERATOR_LINUX = ":";
	private String seperator;

	public WSProviderInvokeCommand(ServiceModel model) {
		super(model);
	}

	@Override
	protected String getCommandLineFileName_linux() {
		return WSPROVIDER_FILE_NAME_LINUX;
	}

	@Override
	protected String getCommandLineFileName_win() {
		return WSPROVIDER_FILE_NAME_WIN;
	}

	@Override
	protected String getCommandlineArgs() {
		if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) {
			seperator = SEPERATOR_WIN;
		} else {
			seperator = SEPERATOR_LINUX;
		}
		StringBuffer commandLine = new StringBuffer();
		String projectName = model.getWebProjectName();
		String projectRoot = JBossWSCreationUtils.getProjectRoot(projectName)
				.toOSString();

		IProject iProject = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(projectName);
		IJavaProject javaProject = JavaCore.create(iProject);

		commandLine.append(" -s ").append(projectRoot).append(Path.SEPARATOR)
				.append("src");

		try {
			commandLine.append(" -c ");
			if(seperator.equals(SEPERATOR_WIN)){
				commandLine.append("\"");
			}

			commandLine.append(projectRoot).append(Path.SEPARATOR).append(
					javaProject.getOutputLocation().removeFirstSegments(1)
							.toOSString()).append(seperator);
			commandLine.append(getClasspathEntries(javaProject));
			if(seperator.equals(SEPERATOR_WIN)){
				commandLine.append("\"");
			}
			commandLine.append(" ");
		} catch (JavaModelException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if (model.isGenWSDL()) {
			commandLine.append(" -w ");
		}
		commandLine.append(" -r ").append(projectRoot).append(Path.SEPARATOR).append("wsdl ");
		commandLine.append(model.getServiceClasses().get(0));

		return commandLine.toString();

	}

	private String getClasspathEntries(IJavaProject javaProject) {
		IClasspathEntry[] iniEntries = null;
		List<IClasspathEntry> pathList = new ArrayList<IClasspathEntry>();
		IClasspathEntry[] resolvedEntries = null;
		try {
			iniEntries = javaProject.getRawClasspath();
			for (IClasspathEntry entry : iniEntries) {
				IClasspathContainer container = JavaCore.getClasspathContainer(
						entry.getPath(), javaProject);
				if (!(container instanceof JBossWSRuntimeClasspathContainer)) {
					if (!(container instanceof RuntimeClasspathContainer && container
							.getDescription().contains("JBoss"))) {
						pathList.add(entry);
					}
				}
			}
			resolvedEntries = ((JavaProject) javaProject)
					.resolveClasspath(pathList
							.toArray(new IClasspathEntry[pathList.size()]));
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		StringBuffer path = new StringBuffer();
		for (IClasspathEntry entry : resolvedEntries) {
			if (entry.getEntryKind() == 3 || entry.getEntryKind() == 2) {
				// path.append(workspaceRoot);
				continue;
			}
			path.append(entry.getPath().toOSString()).append(seperator);
		}
		String str = path.toString();
		if(str.endsWith(seperator)){
			str = str.substring(0, str.length()-1);
		}
		return str;

	}
}
