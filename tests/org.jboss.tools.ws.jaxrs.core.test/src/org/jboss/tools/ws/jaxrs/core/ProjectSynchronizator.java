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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectSynchronizator implements IResourceChangeListener, IResourceDeltaVisitor {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProjectSynchronizator.class);

	private final Stack<IResourceDelta> deltaStack = new Stack<IResourceDelta>();

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		try {
			event.getDelta().accept(this);
		} catch (CoreException e) {
			LOGGER.error("Failed to visit delta", e);
		}
	}

	@Override
	public boolean visit(IResourceDelta delta) throws CoreException {
		String firstSegment = delta.getResource().getProjectRelativePath().segment(0);
		if (firstSegment != null && firstSegment.equals("target")) {
			return false;
		}
		// any CONTENT delta type on a file (not .project file) is put on top of
		// the
		// stack
		if (delta.getResource().getType() == IResource.FILE) {
			switch (delta.getKind()) {
			case IResourceDelta.ADDED:
				LOGGER.debug("Resource added: {}", delta.getResource());
				deltaStack.add(delta);
				break;
			case IResourceDelta.CHANGED:
				if (delta.getFlags() == IResourceDelta.CONTENT) {
					LOGGER.debug("Resource changed: {}", delta.getResource());
					deltaStack.add(delta);
				}
				break;
			case IResourceDelta.REMOVED:
				LOGGER.debug("Resource removed: {}", delta.getResource());
				deltaStack.add(delta);
				break;
			}

			// deltaStack.add(delta);

		}
		// only creation and deletion on a folder is put on top of the stack
		else if (delta.getResource().getType() == IResource.FOLDER && delta.getKind() == IResourceDelta.ADDED) {
			LOGGER.debug("Resource added : {}", delta.getResource());
			deltaStack.add(delta);
		} else if (delta.getResource().getType() == IResource.FOLDER && delta.getKind() == IResourceDelta.REMOVED) {
			LOGGER.debug("Resource removed : {}", delta.getResource());
			deltaStack.add(delta);
		}
		return true;
	}

	public void resync() throws CoreException, InvocationTargetException, InterruptedException {
		LOGGER.debug("Starting project resource resync'...");
		IWorkspace junitWorkspace = ResourcesPlugin.getWorkspace();
		NullProgressMonitor monitor = new NullProgressMonitor();
		IPath projectSourcePath = WorkbenchUtils
				.getSampleProjectPath(AbstractCommonTestCase.DEFAULT_SAMPLE_PROJECT_NAME);
		while (!deltaStack.isEmpty()) {
			IResourceDelta delta = deltaStack.pop();
			switch (delta.getKind()) {
			case IResourceDelta.ADDED:
				// resource was added : needs to be removed
				LOGGER.debug("Removing " + delta.getResource().getFullPath());
				delta.getResource().delete(true, monitor);
				break;
			case IResourceDelta.CHANGED:
			case IResourceDelta.REMOVED:
				LOGGER.debug("Restoring " + delta.getResource().getFullPath());
				WorkbenchTasks.copyFile(projectSourcePath, delta.getResource(), junitWorkspace, monitor);
				break;
			}
		}
		LOGGER.debug("Done with project resource resync'...");

	}

}
