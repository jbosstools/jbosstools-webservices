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

package org.jboss.tools.ws.jaxrs.core;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.internal.ide.filesystem.FileSystemStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
public class WorkbenchTasks {

	static final Logger LOGGER = LoggerFactory.getLogger(WorkbenchTasks.class);

	/**
	 * Synchronize the target project with the path given in parameter.
	 * 
	 * @param sourcePath
	 * @param monitor
	 * @return true if ImportOperation to synchronize was performed (ie, was required), false otherwise.
	 * @throws InvocationTargetException
	 * @throws InterruptedException
	 * @throws CoreException
	 */
	// FIXME : when samples sources are modified in the dev workspace, they are
	// not published in the junit-workspace...
	public static void syncProject(IPath projectSourcePath, IWorkspace targetWorkspace, IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException, CoreException {
		IProject project = getTargetWorkspaceProject(projectSourcePath, targetWorkspace, monitor);
		LOGGER.info("adding missing or modified files in the target workspace...");
		LOGGER.info("Source path: " + projectSourcePath.toPortableString());
		LOGGER.info("Target workspace location: " + targetWorkspace.getRoot().getRawLocation());
		SyncFileSystemStructureProvider syncFileSystemStructureProvider = new SyncFileSystemStructureProvider(
				projectSourcePath, project.getLocation());
		syncFileSystemStructureProvider.ignoreRelativeSourcePath(".svn");
		syncFileSystemStructureProvider.ignoreRelativeSourcePath("target");
		syncFileSystemStructureProvider.ignoreRelativeSourcePath("bin");
		List<File> filesToImport = syncFileSystemStructureProvider.getChildren(projectSourcePath.toFile());
		if (filesToImport != null && filesToImport.size() > 0) {
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
			operation.run(monitor);
		}
		LOGGER.info("Removing added files from the target workspace");
		// reverse detection operation
		syncFileSystemStructureProvider = new SyncFileSystemStructureProvider(project.getLocation(), projectSourcePath);
		syncFileSystemStructureProvider.ignoreRelativeSourcePath("target");
		syncFileSystemStructureProvider.ignoreRelativeSourcePath("bin");
		syncFileSystemStructureProvider.ignoreRelativeSourcePath(".svn");
		syncFileSystemStructureProvider.ignoreRelativeSourcePath(".project");
		syncFileSystemStructureProvider.ignoreRelativeSourcePath(".classpath");
		syncFileSystemStructureProvider.ignoreRelativeSourcePath(".settings");
		List<File> filesToRemove = syncFileSystemStructureProvider.getChildren(project.getLocation().toFile());
		for (File fileToRemove : filesToRemove) {
			Assert.assertTrue("File not deleted : " + fileToRemove, fileToRemove.delete());
		}
		buildProject(project, monitor);
	}

	public static void copyFile(IPath projectSourcePath, IResource resource, IWorkspace targetWorkspace,
			IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
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
		operation.run(monitor);
	}

	/**
	 * @param sourceWorkspace
	 * @param targetWorkspace
	 * @param path
	 * @param monitor
	 * @return
	 * @throws CoreException
	 * @throws InvocationTargetException
	 */
	public static IProject getTargetWorkspaceProject(IPath projectSourcePath, IWorkspace targetWorkspace,
			IProgressMonitor monitor) throws CoreException, InvocationTargetException {
		IPath dotProjectPath = projectSourcePath.addTrailingSeparator().append(".project");
		IProjectDescription description = targetWorkspace.loadProjectDescription(dotProjectPath);
		String projectName = description.getName();
		IProject project = targetWorkspace.getRoot().getProject(projectName);
		if (project.exists() && !targetWorkspace.getRoot().getFile(project.getFile(".project").getFullPath()).exists()) {
			LOGGER.warn("Deleting (with force!) the project as it seems to be in an invalid state...");
			project.delete(true, monitor);
		} else if (project.exists() && !project.isOpen()) {
			project.open(monitor);
		}
		if (!project.exists()) {
			WorkbenchUtils.createProject(monitor, description, projectName, targetWorkspace, project);
		}
		return project;
	}

	public static void buildWorkspace(final IProgressMonitor progressMonitor) throws CoreException,
			OperationCanceledException, InterruptedException {
		ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.AUTO_BUILD, progressMonitor);
		Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
	}

	public static void buildProject(final IProject project, final IProgressMonitor progressMonitor)
			throws CoreException, OperationCanceledException, InterruptedException {
		buildProject(project, IncrementalProjectBuilder.FULL_BUILD, progressMonitor);
	}

	public static void buildProject(final IProject project, final int buildKind, final IProgressMonitor progressMonitor)
			throws CoreException, OperationCanceledException, InterruptedException {
		project.refreshLocal(IResource.DEPTH_INFINITE, progressMonitor);
		project.build(buildKind, progressMonitor);
		Job.getJobManager().join(ResourcesPlugin.FAMILY_MANUAL_BUILD, null);
	}

	public static IPackageFragmentRoot addClasspathEntry(IJavaProject javaProject, String name,
			IProgressMonitor progressMonitor) throws CoreException, OperationCanceledException, InterruptedException {
		IPath path = javaProject.getProject().getLocation().append("lib").addTrailingSeparator().append(name);
		if (!path.toFile().exists() || !path.toFile().canRead()) {
			LOGGER.warn("Following library does not exist or is not readable: {} ", path.toFile());
		}
		IClasspathEntry[] classpathEntries = javaProject.getRawClasspath();
		IClasspathEntry newLibraryEntry = JavaCore.newLibraryEntry(path, null, null);
		classpathEntries = (IClasspathEntry[]) ArrayUtils.add(classpathEntries, newLibraryEntry);
		javaProject.setRawClasspath(classpathEntries, progressMonitor);
		buildProject(javaProject.getProject(), progressMonitor);
		for (IPackageFragmentRoot fragment : javaProject.getAllPackageFragmentRoots()) {
			if (fragment.getRawClasspathEntry().equals(newLibraryEntry)) {
				return fragment;
			}
		}
		return null;
	}

	/**
	 * Called by subclasses to setup the workspace with project and files (xml, java, etc.)
	 * 
	 * @param projectName
	 * 
	 * @throws Exception
	 */
	public static IProject syncSampleProject(String projectName) throws Exception {
		AbstractCommonTestCase.LOGGER.debug("Sync'ing sample project " + projectName);
		IWorkspace junitWorkspace = ResourcesPlugin.getWorkspace();
		IPath projectSourcePath = WorkbenchUtils.getSampleProjectPath(projectName);
		Long start = new Date().getTime();
		NullProgressMonitor monitor = new NullProgressMonitor();
		syncProject(projectSourcePath, junitWorkspace, monitor);
		// TODO : wait until processes are finished
		AbstractCommonTestCase.LOGGER.debug("Sync'ing sample project done in " + (new Date().getTime() - start)
				+ " millis");
		junitWorkspace.getRoot().refreshLocal(IResource.DEPTH_INFINITE, null);
		IProject project = junitWorkspace.getRoot().getProject(projectName);
		project.open(monitor);
		return project;
	}

}
