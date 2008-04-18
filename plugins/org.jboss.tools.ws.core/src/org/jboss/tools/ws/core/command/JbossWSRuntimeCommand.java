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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.wst.common.frameworks.datamodel.AbstractDataModelOperation;


public class JbossWSRuntimeCommand extends
AbstractDataModelOperation {

	IProject project;
	String runtimelocation;
	
	public JbossWSRuntimeCommand(IProject project){
		PreferenceStore ps = new PreferenceStore("jbosswsui.properties");
	    try {
	        ps.load();
	    } catch (IOException e) {
	        // Ignore
	    }
	    runtimelocation = ps.getString("jbosswsruntimelocation");
		this.project = project;
	}

	public IStatus execute(IProgressMonitor monitor, IAdaptable info)
	throws ExecutionException {
		return Status.OK_STATUS;
	}
	
	public IStatus executeOverride(IProgressMonitor monitor)  {
		// Copy the axis2 facet in to this project
		IStatus status =null;
//		String runtimeLocation = null;
//		ContentCopyUtils contentCopyUtils = new ContentCopyUtils();
//		try {
//			if(context.isAxis2ServerPathRepresentsWar() 
//					|| RuntimePropertyUtils.getWarStatusFromPropertiesFile()){
//				runtimeLocation = Axis2RuntimeUtils.copyAxis2War(
//														monitor,
//														Axis2Constants.PROPERTY_KEY_PATH);
//			}else{
//
//				runtimeLocation = Axis2WebappUtils.copyAxis2War(
//														monitor,
//														Axis2Constants.PROPERTY_KEY_PATH);
//			}
//		} catch (FileNotFoundException e) {
//			return handleExceptionStatus(e);
//		} catch (IOException e) {
//			return handleExceptionStatus(e);
//		} catch (Exception e) {
//			return handleExceptionStatus(e);
//		}
//		
//		// First Setting the libs folder as ignored and then copy the content of
//		// the runtime
//		IPath libPath = new Path(runtimeLocation);
//		libPath = libPath.append(Axis2Constants.DIR_WEB_INF);
//		libPath = libPath.append(Axis2Constants.DIR_LIB);
//		List<String> ignoreList = new ArrayList<String>();
//		ignoreList.add(libPath.toOSString());
//		contentCopyUtils.updateCheckList(ignoreList);
//		
//		status = contentCopyUtils.copyDirectoryRecursivelyIntoWorkspace(
//				runtimeLocation, 
//				FacetContainerUtils.pathToWebProjectContainer(project.toString()), 
//				monitor,
//				false
//		);
//		
//		// After that copy the nesessery set of libraries to the project again
//		List<String> includeList = new ArrayList<String>();
//		contentCopyUtils.updateCheckList(loadIncludeListWithAxis2Libs(libPath.toOSString(),
//				includeList));
//		String[] nodes = {Axis2Constants.DIR_WEB_INF,Axis2Constants.DIR_LIB};
//		status = contentCopyUtils.copyDirectoryRecursivelyIntoWorkspace(
//				libPath.toOSString(), 
//				FileUtils.addNodesToPath(
//						FacetContainerUtils.pathToWebProjectContainer(project.toString()), 
//						nodes), 
//				monitor,
//				true
//		);
//
//		// Merge web.xml Files
//		MergeWEBXMLCommand mergeWebXMLCommand = new MergeWEBXMLCommand();
//		mergeWebXMLCommand.setExtraAxis2TagsAdded(false);
//		mergeWebXMLCommand.setServerProject(project);
//		mergeWebXMLCommand.exexuteOverride(monitor);
//
//		// clean up tempery files
//		File tempFacetDirectory = new File(runtimeLocation);
//		if (tempFacetDirectory.exists() && context.isAxis2ServerPathRepresentsWar()) {
//			FileUtils.deleteDir(tempFacetDirectory);
//		}
//		status = Status.OK_STATUS;
		return status;
	}
	
	private void cleanupIfFacetStatusFailed(String runtimeLocation){
//		File tempFacetDirectory = new File(runtimeLocation);
//		if (tempFacetDirectory.exists()) {
//			FileUtils.deleteDir(tempFacetDirectory);
//		}
	}
	
	private IStatus handleExceptionStatus(Exception e){
		IStatus status = null;
//		status = new Status(1,project.toString(),1,Axis2CoreUIMessages.ERROR_SERVER_IS_NOT_SET,e);
//		cleanupIfFacetStatusFailed(Axis2CoreUtils.tempAxis2Directory());
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
	private List loadIncludeListWithAxis2Libs(String path, List includeList){
//		for (int i = 0; i < Axis2Constants.AXIS2_LIB_PREFIXES.length; i++) {
//			File[] jarFileList = FileUtils.getMatchingFiles(path,
//					Axis2Constants.AXIS2_LIB_PREFIXES[i], 
//					Axis2Constants.JAR);
//			for (int j = 0; j < jarFileList.length; j++) {
//				includeList.add(jarFileList[j].getAbsolutePath());
//			}
//			// Fix for the 205972
//			File[] licenseFileList = FileUtils.getMatchingFiles(path,
//					Axis2Constants.AXIS2_LIB_PREFIXES[i], 
//					Axis2Constants.TXT);
//			for (int k = 0; k < licenseFileList.length; k++) {
//				includeList.add(licenseFileList[k].getAbsolutePath());
//			}
//		}
		return includeList;
	}
}