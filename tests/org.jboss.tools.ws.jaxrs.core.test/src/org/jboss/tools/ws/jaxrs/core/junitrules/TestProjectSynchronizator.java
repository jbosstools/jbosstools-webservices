/******************************************************************************* 
 * Copyright (c) 2008 - 2014 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/

package org.jboss.tools.ws.jaxrs.core.junitrules;

import java.lang.reflect.InvocationTargetException;
import java.util.Stack;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.jboss.tools.ws.jaxrs.core.internal.utils.TestLogger;
import org.junit.Assert;

public class TestProjectSynchronizator implements IResourceChangeListener, IResourceDeltaVisitor {

	private final Stack<IResourceDelta> deltaStack = new Stack<IResourceDelta>();
	
	private final String projectName;

	public TestProjectSynchronizator(final String projectName) {
		this.projectName = projectName;
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		try {
			if (event.getDelta() != null) {
				event.getDelta().accept(this);
			}
		} catch (CoreException e) {
			TestLogger.error("Failed to visit delta", e);
		}
	}

	@Override
	public boolean visit(IResourceDelta delta) throws CoreException {
		String firstSegment = delta.getResource().getProjectRelativePath().segment(0);
		if (firstSegment != null && firstSegment.equals("target")) {
			return false;
		}
		// any CONTENT delta type on a file (not .project file) is put on top of
		// the stack
		if (delta.getResource().getType() == IResource.FILE) {
			switch (delta.getKind()) {
			case IResourceDelta.ADDED:
				TestLogger.trace("Resource added: {}", delta.getResource());
				deltaStack.add(delta);
				break;
			case IResourceDelta.CHANGED:
				if (delta.getFlags() == IResourceDelta.CONTENT) {
					TestLogger.trace("Resource changed: {}", delta.getResource());
					deltaStack.add(delta);
				}
				break;
			case IResourceDelta.REMOVED:
				TestLogger.trace("Resource removed: {}", delta.getResource());
				deltaStack.add(delta);
				break;
			}
		}
		// only creation and deletion on a folder is put on top of the stack
		else if (delta.getResource().getType() == IResource.FOLDER && delta.getKind() == IResourceDelta.ADDED) {
			TestLogger.trace("Resource added : {}", delta.getResource());
			deltaStack.add(delta);
		} else if (delta.getResource().getType() == IResource.FOLDER && delta.getKind() == IResourceDelta.REMOVED) {
			TestLogger.trace("Resource removed : {}", delta.getResource());
			deltaStack.add(delta);
		}
		return true;
	}

	/**
	 * Re-synchronizes the project if some files changed
	 * @return {@code true} if some files changed, {@code false} otherwise.
	 * @throws CoreException
	 * @throws InvocationTargetException
	 * @throws InterruptedException
	 */
	public boolean resync() throws CoreException, InvocationTargetException, InterruptedException {
		IWorkspace junitWorkspace = ResourcesPlugin.getWorkspace();
		NullProgressMonitor monitor = new NullProgressMonitor();
		IPath projectSourcePath = WorkbenchTasks.getProjectSourcePath(projectName);
		
		if(deltaStack.isEmpty()) {
			TestLogger.info("Skipping project resource resync' b/c no file changed during the test.");
			return false;
		}
		TestLogger.debug("Starting project resource resync'...");
		while (!deltaStack.isEmpty()) {
			final IResourceDelta delta = deltaStack.pop();
			switch (delta.getKind()) {
			case IResourceDelta.ADDED:
				// resource was added : needs to be removed
				TestLogger.trace("Removing " + delta.getResource().getFullPath());
				delta.getResource().delete(true, monitor);
				break;
			case IResourceDelta.CHANGED:
			case IResourceDelta.REMOVED:
				TestLogger.trace("Restoring " + delta.getResource().getFullPath());
				WorkbenchTasks.copyFile(projectSourcePath, delta.getResource(), junitWorkspace);
				break;
			}
		}
		TestLogger.debug("Done with project resource resync'...");
		return true;
	}

	@Deprecated
	public static IPath getSampleProjectPath(String projectName) {
		IPath path = null;
		if (System.getProperty("user.dir") != null) {
			path = new Path(System.getProperty("user.dir")).append("projects").append(projectName).makeAbsolute();
		} else {
			Assert.fail("The sample project was not found in the launcher workspace under name '" + projectName + "'");

		}
		TestLogger.debug(projectName + " path=" + path.toOSString());
		return path;
	}

}
