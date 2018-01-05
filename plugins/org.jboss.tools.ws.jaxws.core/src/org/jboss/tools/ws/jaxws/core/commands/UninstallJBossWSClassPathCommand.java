/******************************************************************************* 
 * Copyright (c) 2008 - 2014 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.ws.jaxws.core.commands;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
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
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.jboss.tools.ws.jaxws.core.facet.delegate.IJBossWSFacetDataModelProperties;
import org.jboss.tools.ws.jaxws.core.messages.JBossJAXWSCoreMessages;
import org.jboss.tools.ws.jaxws.core.util.StatusUtils;

public class UninstallJBossWSClassPathCommand {
	IProject project;

	public UninstallJBossWSClassPathCommand(IProject project, IDataModel model) {
		this.project = project;
	}

	public IStatus execute(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {
		return executeOverride(monitor);
	}

	public IStatus executeOverride(IProgressMonitor monitor) {
		IStatus status = Status.OK_STATUS;
		try {
			boolean isServerSupplied = Boolean
					.getBoolean(project
							.getPersistentProperty(IJBossWSFacetDataModelProperties.PERSISTENCE_PROPERTY_SERVER_SUPPLIED_RUNTIME));
			if (!isServerSupplied) {
				String runtimeName = project
						.getPersistentProperty(IJBossWSFacetDataModelProperties.PERSISTENCE_PROPERTY_QNAME_RUNTIME_NAME);

				status = removeClassPath(project, runtimeName);
			}

		} catch (CoreException e) {
			status = StatusUtils.errorStatus(
					JBossJAXWSCoreMessages.Error_Remove_Facet_JBossWS, e);
		}
		return status;
	}

	public IStatus removeClassPath(IProject project, String segment) {
		IStatus status = Status.OK_STATUS;
		if(segment == null || "".equals(segment)){ //$NON-NLS-1$
			return status;
		}
		
		try {
			IJavaProject javaProject = JavaCore.create(project);
			IClasspathEntry[] oldClasspathEntries = javaProject
					.readRawClasspath();

			boolean isFolderInClassPathAlready = false;
			List<IClasspathEntry> classpathEntries = new ArrayList<IClasspathEntry>();
			for (int i = 0; i < oldClasspathEntries.length
					&& !isFolderInClassPathAlready; i++) {
				if (!oldClasspathEntries[i].getPath().equals(
						new Path(JBossJAXWSCoreMessages.JBossWS_Runtime_Lib)
								.append(segment))) {
					classpathEntries.add(oldClasspathEntries[i]);
				}
			}
			if (classpathEntries.size() < oldClasspathEntries.length) {
				javaProject.setRawClasspath(classpathEntries
						.toArray(new IClasspathEntry[classpathEntries.size()]),
						new NullProgressMonitor());
			}
		} catch (JavaModelException e) {
			status = StatusUtils.errorStatus(NLS.bind(
					JBossJAXWSCoreMessages.Error_Remove_Facet_JBossWS,
					new String[] { e.getLocalizedMessage() }), e);
			return status;
		}

		return status;
	}

}
