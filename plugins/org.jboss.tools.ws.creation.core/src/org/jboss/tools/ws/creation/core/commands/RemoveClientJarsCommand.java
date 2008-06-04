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

package org.jboss.tools.ws.creation.core.commands;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.wst.common.frameworks.datamodel.AbstractDataModelOperation;
import org.jboss.tools.ws.core.classpath.JbossWSRuntimeClassPathInitializer.JbossWSRuntimeClasspathContainer;
import org.jboss.tools.ws.core.messages.JbossWSCoreMessages;
import org.jboss.tools.ws.core.utils.StatusUtils;
import org.jboss.tools.ws.creation.core.data.ServiceModel;
import org.jboss.tools.ws.creation.core.messages.JBossWSCreationCoreMessages;
import org.jboss.tools.ws.creation.core.utils.JBossWSCreationUtils;

/**
 * @author Grid Qian
 * 
 * remove two jars from web service client project's classpath
 * because can not run client sample if have these two jars
 */
public class RemoveClientJarsCommand extends AbstractDataModelOperation{

	private ServiceModel model;
	
	public RemoveClientJarsCommand(ServiceModel model) {
		this.model = model;
	}
	
	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {		
		return executeOverride(monitor);
	}
	
	public IStatus executeOverride(IProgressMonitor monitor) {
		IStatus status = Status.OK_STATUS;
		IJavaProject project = null;
		try {
			project = JBossWSCreationUtils.getJavaProjectByName(model
					.getWebProjectName());
		} catch (JavaModelException e) {
			return StatusUtils.errorStatus(JBossWSCreationCoreMessages.ERROR_CREATECLIENTSAMPLE);
		}
		status = removeClassPath(project);
		return status;
	}

	private IStatus removeClassPath(IJavaProject project) {
		IStatus status = Status.OK_STATUS;
		try {
			IClasspathEntry[] entries = project.getRawClasspath();
			
			for(IClasspathEntry entry:entries){
					IClasspathContainer container = JavaCore.getClasspathContainer(entry.getPath(), project);
					if(container instanceof JbossWSRuntimeClasspathContainer){
						((JbossWSRuntimeClasspathContainer)container).removeEntry("jaxws-rt.jar");
						((JbossWSRuntimeClasspathContainer)container).removeEntry("jaxws-tools.jar");
					}
			}
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			project.getRawClasspath();
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return status;
	}

}
