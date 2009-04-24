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
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.jboss.tools.common.util.FileUtil;
import org.jboss.tools.ws.creation.core.data.ServiceModel;
import org.jboss.tools.ws.creation.core.utils.ClasspathParser;
import org.jboss.tools.ws.creation.core.utils.JBossWSCreationUtils;

/**
 * @author Grid Qian
 */
public class Java2WSCommand extends AbstractGenerateCodeCommand {

	private static String WSPROVIDER_FILE_NAME_LINUX = "wsprovide.sh";
	private static String WSPROVIDER_FILE_NAME_WIN = "wsprovide.bat";

	public Java2WSCommand(ServiceModel model) {
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
	protected void addCommandlineArgs(List<String> command) throws IOException {

		String projectName = model.getWebProjectName();
		String projectRoot = JBossWSCreationUtils.getProjectRoot(projectName)
				.toOSString();

		IProject iProject = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(projectName);
		
		if (model.isGenWSDL()) {
			command.add("-w");
		}
		command.add("-r");
		command.add(projectRoot + Path.SEPARATOR + "wsdl");
		command.add(model.getServiceClasses().get(0));

		command.add("-c");
		command.add(getClasspath(iProject));
	}

	private String getClasspath(IProject iProject) throws IOException {
		String cpStr = ClasspathParser.getInstance().getClasspathString(
				iProject);

		if (isLongCommandLines(cpStr)) {
			File tempJar = null;
			tempJar = File.createTempFile("temp", ".jar");
			Manifest mf = new Manifest();

			// replace space with "%20"
			cpStr = cpStr.replace(" ", "%20");
			// replace \ with /
			cpStr = cpStr.replace("\\", "/");
			// get classpath array
			String[] cpArray = cpStr.split(";");
			StringBuffer cpBF = new StringBuffer();
			for (int i = 0; i < cpArray.length; i++) {
				// if the classpath entry is a directory, it should be added a /
				// after it
				if (!cpArray[i].endsWith(".jar") && !cpArray[i].endsWith("/")) {
					cpArray[i] = cpArray[i] + "/";
				}
				// if the system is windows, the classpath entry should be added
				// a / before it
				if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0)
					cpArray[i] = "/" + cpArray[i];
				// there should be a space between classpath entries
				cpBF.append(cpArray[i]).append(" ");
			}
			cpStr = cpBF.toString();
			cpStr.trim();

			mf.getMainAttributes().put(Attributes.Name.CLASS_PATH, cpStr);
			mf.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.2");

			try {
				FileUtil.jar(new File[0], tempJar.getAbsolutePath(), mf);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			cpStr = tempJar.getAbsolutePath();
		} else {
			// if the system is not windows, the split should be :
			if (System.getProperty("os.name").toLowerCase().indexOf("win") < 0)
				cpStr = cpStr.replace(";", ":");
		}
		return cpStr;
	}

	public boolean isLongCommandLines(String command) {
		if (command == null)
			return false;
		return command.length() > 1024 ? true : false;
	}

	// private String getClasspathEntries(IJavaProject javaProject) {
	// IClasspathEntry[] iniEntries = null;
	// List<IClasspathEntry> pathList = new ArrayList<IClasspathEntry>();
	// IClasspathEntry[] resolvedEntries = null;
	// try {
	// iniEntries = javaProject.getRawClasspath();
	// for (IClasspathEntry entry : iniEntries) {
	// IClasspathContainer container = JavaCore.getClasspathContainer(
	// entry.getPath(), javaProject);
	// if (!(container instanceof JBossWSRuntimeClasspathContainer)) {
	// if (!(container instanceof RuntimeClasspathContainer && container
	// .getDescription().contains("JBoss"))) {
	// pathList.add(entry);
	// }
	// }
	// }
	// resolvedEntries = ((JavaProject) javaProject)
	// .resolveClasspath(pathList
	// .toArray(new IClasspathEntry[pathList.size()]));
	// } catch (JavaModelException e) { // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// StringBuffer path = new StringBuffer();
	// for (IClasspathEntry entry : resolvedEntries) {
	// if (entry.getEntryKind() == 3) {
	// path.append(workspaceRoot);
	// continue;
	// }
	// if (entry.getEntryKind() == 2) {
	// String projectRoot = JBossWSCreationUtils.getProjectRoot(
	// entry.getPath().segment(0)).toOSString();
	// IProject refProject = ResourcesPlugin.getWorkspace().getRoot()
	// .getProject(entry.getPath().segment(0));
	// IJavaProject refJavaProject = JavaCore.create(refProject);
	// try {
	// path.append(projectRoot).append(Path.SEPARATOR).append(
	// refJavaProject.getOutputLocation()
	// .removeFirstSegments(1).toOSString())
	// .append(seperator);
	// } catch (JavaModelException e) {
	// e.printStackTrace();
	// }
	// } else {
	// path.append(entry.getPath().toOSString()).append(seperator);
	// }
	// }
	// String str = path.toString();
	// if (str.endsWith(seperator)) {
	// str = str.substring(0, str.length() - 1);
	// }
	// return str;
	//
	// }

}