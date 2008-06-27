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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jst.j2ee.classpathdep.ClasspathDependencyUtil;
import org.eclipse.jst.j2ee.classpathdep.IClasspathDependencyConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.common.frameworks.datamodel.AbstractDataModelOperation;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.jboss.tools.ws.core.facet.delegate.IJBossWSFacetDataModelProperties;
import org.jboss.tools.ws.core.messages.JBossWSCoreMessages;
import org.jboss.tools.ws.core.utils.StatusUtils;

/**
 * @author Grid Qian
 */
public class JBossWSClassPathCommand extends AbstractDataModelOperation {

	IProject project;
	private IDataModel model;

	public JBossWSClassPathCommand(IProject project, IDataModel model) {
		this.project = project;
		this.model = model;
	}

	public IStatus execute(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {
		return executeOverride(monitor);
	}

	public IStatus executeOverride(IProgressMonitor monitor) {
		IStatus status = Status.OK_STATUS;
		try {
			boolean isServerSupplied = model
					.getBooleanProperty(IJBossWSFacetDataModelProperties.JBOSS_WS_RUNTIME_IS_SERVER_SUPPLIED);
			if (isServerSupplied) {
				project
						.setPersistentProperty(
								IJBossWSFacetDataModelProperties.PERSISTENCE_PROPERTY_SERVER_SUPPLIED_RUNTIME,
								IJBossWSFacetDataModelProperties.DEFAULT_VALUE_IS_SERVER_SUPPLIED);
			} else {
				// store runtime name and runtime location to the project

				String runtimeName = model
						.getStringProperty(IJBossWSFacetDataModelProperties.JBOSS_WS_RUNTIME_ID);
				String runtimeLocation = model
						.getStringProperty(IJBossWSFacetDataModelProperties.JBOSS_WS_RUNTIME_HOME);
				project
						.setPersistentProperty(
								IJBossWSFacetDataModelProperties.PERSISTENCE_PROPERTY_QNAME_RUNTIME_NAME,
								runtimeName);
				project
						.setPersistentProperty(
								IJBossWSFacetDataModelProperties.PERSISTENCE_PROPERTY_RNTIME_LOCATION,
								runtimeLocation);
				boolean isDeployed = model
						.getBooleanProperty(IJBossWSFacetDataModelProperties.JBOSS_WS_DEPLOY);

				status = addClassPath(project, runtimeName, isDeployed);
			}

		} catch (CoreException e) {
			status = StatusUtils.errorStatus(
					JBossWSCoreMessages.Error_Add_Facet_JBossWS, e);
		}
		return status;
	}

	public IStatus addClassPath(IProject project, String segment,
			boolean isDeployed) {
		IStatus status = Status.OK_STATUS;
		try {

			IClasspathEntry newClasspath;
			IJavaProject javaProject = JavaCore.create(project);

			if (isDeployed) {
				IClasspathAttribute depAttrib = JavaCore
						.newClasspathAttribute(
								IClasspathDependencyConstants.CLASSPATH_COMPONENT_DEPENDENCY,
								ClasspathDependencyUtil.getDefaultRuntimePath(
										true).toString());
				newClasspath = JavaCore.newContainerEntry(new Path(
						JBossWSCoreMessages.JBossWS_Runtime_Lib)
						.append(segment), null,
						new IClasspathAttribute[] { depAttrib }, true);
			} else {
				newClasspath = JavaCore.newContainerEntry(new Path(
						JBossWSCoreMessages.JBossWS_Runtime_Lib)
						.append(segment));
			}

			IClasspathEntry[] oldClasspathEntries = javaProject
					.readRawClasspath();

			boolean isFolderInClassPathAlready = false;
			for (int i = 0; i < oldClasspathEntries.length
					&& !isFolderInClassPathAlready; i++) {
				if (oldClasspathEntries[i].getPath().equals(
						newClasspath.getPath())) {
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
					JBossWSCoreMessages.Error_Copy, new String[] { e
							.getLocalizedMessage() }), e);
			return status;
		}

		return status;
	}

}