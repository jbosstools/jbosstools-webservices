/******************************************************************************* 
 * Copyright (c) 2008 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/

package org.jboss.tools.ws.jaxrs.core.junitrules;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.internal.ide.filesystem.FileSystemStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
public class WorkbenchTasks {

	static final Logger LOGGER = LoggerFactory.getLogger(WorkbenchTasks.class);

	

	public static void copyFile(IPath projectSourcePath, IResource resource, IWorkspace targetWorkspace)
			throws InvocationTargetException, InterruptedException {
		List<File> filesToImport = new ArrayList<File>(1);
		File file = projectSourcePath.append(resource.getProjectRelativePath()).toFile();
		if (!file.exists()) {
			return;
		}
		filesToImport.add(file);
		IPath resourcePath = resource.getProjectRelativePath().removeLastSegments(1);
		IPath containerPath = null;
		if (resourcePath.segmentCount() > 0) {
			containerPath = resource.getProject().getFolder(resourcePath).getFullPath();
		} else {
			containerPath = resource.getProject().getFullPath();
		}
		ImportOperation operation = new ImportOperation(containerPath, new FileSystemStructureProvider(),
				new IOverwriteQuery() {
					@Override
					public String queryOverwrite(String pathString) {
						return IOverwriteQuery.YES;
					}
				}, filesToImport);
		operation.setContext(null);
		// need to overwrite modified files
		operation.setOverwriteResources(true);
		operation.setCreateContainerStructure(false);
		LOGGER.debug("Copying {} into {}", file.getAbsolutePath(), resource.getProject().getLocation());
		operation.run(new NullProgressMonitor());
	}

	public static IProject getTargetWorkspaceProject(final String projectName)
			throws CoreException, InvocationTargetException {
		final IPath projectSourcePath = getProjectSourcePath(projectName);
		final IWorkspace targetWorkspace = ResourcesPlugin.getWorkspace();
		final IPath dotProjectPath = projectSourcePath.addTrailingSeparator().append(".project");
		final IProjectDescription description = targetWorkspace.loadProjectDescription(dotProjectPath);
		final IProject project = targetWorkspace.getRoot().getProject(projectName);
		if (project.exists() && !targetWorkspace.getRoot().getFile(project.getFile(".project").getFullPath()).exists()) {
			LOGGER.warn("Deleting (with force!) the project as it seems to be in an invalid state...");
			project.delete(true, new NullProgressMonitor());
		} else if (project.exists() && !project.isOpen()) {
			project.open(new NullProgressMonitor());
		}
		if (!project.exists()) {
			JavaElementsUtils.createProject(description, projectName, targetWorkspace, project);
		}
		return project;
	}

	
	/**
	 * Synchronize and builds the target project with the path given in parameter.
	 * 
	 */
	public static IProject synchronizeProject(final String projectName)
			throws InvocationTargetException, InterruptedException, CoreException {
		Long start = new Date().getTime();
		try {
			IPath projectSourcePath = getProjectSourcePath(projectName);
			
			LOGGER.debug("Source project path: " + projectSourcePath.toPortableString());
			final IProject project = getTargetWorkspaceProject(projectName);
			LOGGER.debug("Target project path: " + project.getRawLocation());
			LOGGER.debug("Removing added files from the target workspace");
			// reverse detection operation
			SyncFileSystemStructureProvider syncFileSystemStructureProvider = new SyncFileSystemStructureProvider(
					project.getLocation(), projectSourcePath);
			syncFileSystemStructureProvider.ignoreRelativeSourcePaths("target", "bin", ".svn", ".git", ".project",
					".classpath", ".settings");
			List<File> filesToRemove = syncFileSystemStructureProvider.getChildren(project.getLocation().toFile());
			for (File fileToRemove : filesToRemove) {
				Assert.assertTrue("File not deleted : " + fileToRemove, fileToRemove.delete());
			}
			LOGGER.info("adding missing or modified files in the target workspace...");
			syncFileSystemStructureProvider = new SyncFileSystemStructureProvider(projectSourcePath,
					project.getLocation());
			syncFileSystemStructureProvider.ignoreRelativeSourcePath(".svn");
			syncFileSystemStructureProvider.ignoreRelativeSourcePath("target");
			syncFileSystemStructureProvider.ignoreRelativeSourcePath("bin");
			List<File> filesToImport = syncFileSystemStructureProvider.getChildren(projectSourcePath.toFile());
			if (filesToImport != null && filesToImport.size() > 0) {
				LOGGER.info(" about to synchronize {} files...", filesToImport.size());
				ImportOperation operation = new ImportOperation(project.getFullPath(), projectSourcePath.toFile(),
						syncFileSystemStructureProvider, new IOverwriteQuery() {
							@Override
							public String queryOverwrite(String pathString) {
								return IOverwriteQuery.YES;
							}
						}, filesToImport);
				operation.setContext(null);
				// need to overwrite modified files
				operation.setOverwriteResources(true);
				operation.setCreateContainerStructure(false);
				operation.run(new NullProgressMonitor());
			}
			ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, null);
			project.open(new NullProgressMonitor());
			project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
			Job.getJobManager().join(ResourcesPlugin.FAMILY_MANUAL_BUILD, null);
			IMarker[] projectMarkers = project.findMarkers(null, true, IResource.DEPTH_INFINITE);
			for(IMarker marker : projectMarkers) {
				if(marker.getAttribute(IMarker.SEVERITY, 0) == IMarker.SEVERITY_ERROR) {
					System.err.println(" " + marker.getAttribute(IMarker.MESSAGE, ""));
				}
			}
			return project;
		} finally {
			LOGGER.debug("Sync'ing sample project done in " + (new Date().getTime() - start) + " millis");
		}
	}

	public static IPath getProjectSourcePath(String projectName) {
		IPath path = null;
		if (System.getProperty("user.dir") != null) {
			path = new Path(System.getProperty("user.dir")).removeLastSegments(1).append("org.jboss.tools.ws.jaxrs.core.test").append("projects").append(projectName).makeAbsolute();
		} else {
			Assert.fail("The sample project was not found in the launcher workspace under name '" + projectName + "'");

		}
		LOGGER.debug(projectName + " path=" + path.toOSString());
		return path;
	}
}
