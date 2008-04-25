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

package org.jboss.tools.ws.core.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.ws.internal.common.J2EEUtils;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.ws.core.JbossWSCoreMessages;

public class JbossWSCoreUtils {

	public static IPath pathToWebProjectContainer(String project) {
		IPath projectRoot = getProjectRoot(project);
		IPath currentDynamicWebProjectDir = J2EEUtils
				.getWebContentPath(getProjectName(project));
		IPath currentDynamicWebProjectDirWithoutProjectRoot = J2EEUtils
				.getWebContentPath(getProjectName(project))
				.removeFirstSegments(1).makeAbsolute();
		if (projectRoot.toOSString().contains(getWorkspace().toOSString())) {
			return getWorkspace().append(currentDynamicWebProjectDir);
		} else {
			return projectRoot
					.append(currentDynamicWebProjectDirWithoutProjectRoot);
		}

	}

	public static IPath getProjectRoot(String project) {
		String projectString = replaceEscapecharactors(project.toString());
		return ResourcesPlugin.getWorkspace().getRoot().getProject(
				getProjectNameFromFramewokNameString(projectString))
				.getLocation();
	}

	// Fix for the windows build not working
	private static String replaceEscapecharactors(String vulnarableString) {
		if (vulnarableString.indexOf("/") != -1) {
			vulnarableString = vulnarableString.replace('/', File.separator
					.charAt(0));
		}
		return vulnarableString;
	}

	private static String getProjectNameFromFramewokNameString(
			String frameworkProjectString) {
		if (frameworkProjectString.indexOf(getSplitCharactor()) == -1) {
			return frameworkProjectString;
		} else {
			return frameworkProjectString.split(getSplitCharactors())[1];
		}
	}

	private static String getSplitCharactor() {
		// Windows check (because from inside wtp in return I received a hard
		// coded path)
		if (File.separatorChar == '\\') {
			return "\\";
		} else {
			return File.separator;
		}
	}

	private static String getSplitCharactors() {
		// Windows check (because from inside wtp in return I received a hard
		// coded path)
		if (File.separatorChar == '\\') {
			return "\\" + File.separator;
		} else {
			return File.separator;
		}
	}

	public static IPath getWorkspace() {
		return ResourcesPlugin.getWorkspace().getRoot().getLocation();
	}

	public static IWorkspaceRoot getWorkspaceRoot() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}

	public static IProject getProjectName(String project) {
		String projectString = replaceEscapecharactors(project.toString());
		return ResourcesPlugin.getWorkspace().getRoot().getProject(
				getProjectNameFromFramewokNameString(projectString));
	}

	public static IStatus copy(IPath sourcePath, IPath targetPath) {
		System.out.println(sourcePath + " >> " + targetPath);

		IStatus status = Status.OK_STATUS;
		File sourceDir = sourcePath.toFile();
		File[] children = sourceDir.listFiles();

		FileInputStream finStream = null;
		for (int i = 0; i < children.length; i++) {
			if (children[i].isFile()) {
				try {
					finStream = new FileInputStream(children[i]);
				} catch (FileNotFoundException e) {
					status = StatusUtils.errorStatus(
							NLS.bind(JbossWSCoreMessages.ERROR_COPY,
									 new String[]{e.getLocalizedMessage()}), e);
				}
				try {
					makeFile(getWorkspaceRoot().getContainerForLocation(
							targetPath), children[i].getName(), finStream);
				} catch (CoreException e) {
					status = StatusUtils.errorStatus(
							NLS.bind(JbossWSCoreMessages.ERROR_COPY,
									 new String[]{e.getLocalizedMessage()}), e);
				}
			} else {
				try {
					IFolder temp = makeFolder(getWorkspaceRoot().getContainerForLocation(targetPath), children[i].getName());
					copy(sourcePath.append(children[i].getName()), temp.getLocation());
				} catch (CoreException e) {
					status = StatusUtils.errorStatus(
							NLS.bind(JbossWSCoreMessages.ERROR_COPY,
									 new String[]{e.getLocalizedMessage()}), e);
				}
			}
		}
		return status;
	}

	private static IFolder makeFolder(IContainer parent, String folderName)
			throws CoreException {
		IResource child = parent.findMember(folderName);
		if (child == null) {
			IFolder folder = parent.getFolder(new Path(folderName));
			try {
				folder.create(true, true, null);
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return folder;
		} else {
			if (child.getType() == IResource.FOLDER) {
				return (IFolder) child;
			} else {
				throw new CoreException(new Status(IStatus.ERROR,
						"ResourceUtils", 0, NLS.bind("ERROR",// EnvironmentMessages.MSG_ERROR_RESOURCE_NOT_FOLDER,
								new Object[] { parent.getFullPath().append(
										folderName).toString() }), null));
			}
		}

	}

	private static IFile makeFile(IContainer parent, String fileName,
			InputStream inputStream) throws CoreException {
		IResource child = parent.findMember(fileName);

		if (child != null) {
			if (child.getType() == IResource.FILE) {

				// We have permission to overwrite so check if file is read-only
				if (child.getResourceAttributes() != null
						&& child.getResourceAttributes().isReadOnly()) {
					IFile[] files = new IFile[1];
					files[0] = (IFile) child;
				}

				// Change the contents of the existing file.
				IFile file = parent.getFile(new Path(fileName));
				file.setContents(inputStream, true, true, null);

				return file;

			} else {
				throw new CoreException(new Status(IStatus.ERROR,
						"ResourceUtils", 0, NLS.bind("ERROR",
								new Object[] { parent.getFullPath().append(
										fileName) }), null));
			}
		} else {
			// Create a new file.
			IFile file = parent.getFile(new Path(fileName));
			file.create(inputStream, true, null);

			return file;
		}
	}

}
