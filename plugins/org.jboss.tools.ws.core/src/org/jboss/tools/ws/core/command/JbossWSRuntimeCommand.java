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

import java.util.List;

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
		return Status.OK_STATUS;
	}

	public IStatus executeOverride(IProgressMonitor monitor) {
		IStatus status = null;

		IPreferenceStore ps = JbossWSCorePlugin.getDefault()
				.getPreferenceStore();
		runtimeLocation = ps.getString("jbosswsruntimelocation");

		// copy lib to project's folder
		IPath libPath = new Path(runtimeLocation);
		libPath = libPath.append(JbossWSCoreMessages.DIR_LIB);

		IPath targetPath = JbossWSCoreUtils.pathToWebProjectContainer(project
				.toString());
		targetPath = targetPath
				.append(JbossWSCoreMessages.DIR_WEB_INF).append(
						JbossWSCoreMessages.DIR_LIB);
		JbossWSCoreUtils.copy(libPath, targetPath);

		status = Status.OK_STATUS;
		return status;
	}

	private void cleanupIfFacetStatusFailed(String runtimeLocation) {
		// File tempFacetDirectory = new File(runtimeLocation);
		// if (tempFacetDirectory.exists()) {
		// FileUtils.deleteDir(tempFacetDirectory);
		// }
	}

	private IStatus handleExceptionStatus(Exception e) {
		IStatus status = null;
		// status = new
		// Status(1,project.toString(),1,Axis2CoreUIMessages.ERROR_SERVER_IS_NOT_SET,e);
		// cleanupIfFacetStatusFailed(Axis2CoreUtils.tempAxis2Directory());
		return status;
	}

	/**
	 * Load the exact libraries list from the axis2 jars with the correct
	 * versions to the <code>path</code>. Doing it this way introduce the
	 * scalability to the solution where the need comes in the future releases
	 * to include additional libraries and if needed filter out some libraries.
	 * 
	 * @param runtimeLocation
	 * @param includeList
	 * @return loaded list
	 */
	private List loadIncludeListWithAxis2Libs(String path, List includeList) {
		// for (int i = 0; i < Axis2Constants.AXIS2_LIB_PREFIXES.length; i++) {
		// File[] jarFileList = FileUtils.getMatchingFiles(path,
		// Axis2Constants.AXIS2_LIB_PREFIXES[i],
		// Axis2Constants.JAR);
		// for (int j = 0; j < jarFileList.length; j++) {
		// includeList.add(jarFileList[j].getAbsolutePath());
		// }
		// // Fix for the 205972
		// File[] licenseFileList = FileUtils.getMatchingFiles(path,
		// Axis2Constants.AXIS2_LIB_PREFIXES[i],
		// Axis2Constants.TXT);
		// for (int k = 0; k < licenseFileList.length; k++) {
		// includeList.add(licenseFileList[k].getAbsolutePath());
		// }
		// }
		return includeList;
	}

}