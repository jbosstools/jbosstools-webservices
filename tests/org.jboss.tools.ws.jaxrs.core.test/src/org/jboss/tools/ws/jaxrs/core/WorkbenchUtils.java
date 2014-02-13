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

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.runtime.CoreException;
import org.junit.Assert;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkbenchUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkbenchUtils.class);

	protected static Bundle bundle = JBossJaxrsCoreTestsPlugin.getDefault().getBundle();

	/** @throws CoreException */
	public static void setAutoBuild(IWorkspace workspace, boolean value) throws CoreException {
		if (workspace.isAutoBuilding() != value) {
			IWorkspaceDescription description = workspace.getDescription();
			description.setAutoBuilding(value);
			workspace.setDescription(description);
		}
	}

	public static String retrieveSampleProjectName(Class<?> clazz) {
		RunWithProject annotation = clazz.getAnnotation(RunWithProject.class);
		while (annotation == null && clazz.getSuperclass() != null) {
			clazz = clazz.getSuperclass();
			annotation = clazz.getAnnotation(RunWithProject.class);
		}
		Assert.assertNotNull("Unable to locate @RunWithProject annotation", annotation);
		return annotation.value();

	}

	

	

	/**
	 * 
	 */
	public static void resetElementChangesNotifications() {
		LOGGER.info("Reseting Changes Notifications before test operation");
		//elementChanges.clear();
		//endpointChanges.clear();
	}
	
	

	/*
	 * public static CompilationUnitEditor getCompilationUnitEditor(IFile file) throws PartInitException { IEditorPart
	 * editorPart = null; PlatformUI.isWorkbenchRunning(); PlatformUI.getWorkbench().isStarting(); IWorkbenchPage page =
	 * PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(); editorPart = page.openEditor(new
	 * FileEditorInput(file), IDE.getEditorDescriptor(file).getId()); return (CompilationUnitEditor) editorPart; }
	 */

	

}
