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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.common.frameworks.datamodel.AbstractDataModelOperation;
import org.jboss.tools.ws.core.messages.JbossWSCoreMessages;
import org.jboss.tools.ws.core.utils.StatusUtils;

/**
 * @author Grid Qian
 */
public class JbossWSClassPathCommand extends AbstractDataModelOperation {

	IProject project;
	String runtimeLocation;

	public JbossWSClassPathCommand(IProject project) {
		this.project = project;
	}

	public IStatus execute(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {
		return executeOverride(monitor);
	}

	public IStatus executeOverride(IProgressMonitor monitor) {
		IStatus status = Status.OK_STATUS;
		status = addClassPath(project);
		return status;
	}

	public IStatus addClassPath(IProject project) {
		IStatus status = Status.OK_STATUS;
		try {

			IJavaProject javaProject = JavaCore.create(project);

			IClasspathEntry newClasspath = JavaCore.newContainerEntry(new Path(
					JbossWSCoreMessages.JBossWS_Runtime_Lib));

			IClasspathEntry[] oldClasspathEntries = javaProject
					.readRawClasspath();

			boolean isFolderInClassPathAlready = false;
			for (int i = 0; i < oldClasspathEntries.length
					&& !isFolderInClassPathAlready; i++) {
				if (oldClasspathEntries[i].getPath().equals(
						project.getFullPath())
						|| oldClasspathEntries[i].getPath().lastSegment()
								.toUpperCase().contains(
										JbossWSCoreMessages.JBossAS)) {
					isFolderInClassPathAlready = true;
					break;
				}
			}

			if (!isFolderInClassPathAlready) {

				IClasspathEntry[] newClasspathEntries = new IClasspathEntry[oldClasspathEntries.length + 1];
				for (int i = 0; i < oldClasspathEntries.length; i++) {
					newClasspathEntries[i] = oldClasspathEntries[i];
				}
				newClasspathEntries[oldClasspathEntries.length] = newClasspath;

				javaProject.setRawClasspath(newClasspathEntries,
						new NullProgressMonitor());
			}
		} catch (JavaModelException e) {
			status = StatusUtils.errorStatus(NLS.bind(
					JbossWSCoreMessages.Error_Copy, new String[] { e
							.getLocalizedMessage() }), e);
			return status;
		}

		return status;
	}

}