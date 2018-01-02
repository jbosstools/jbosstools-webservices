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

package org.jboss.tools.ws.jaxws.ui.commands;

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
import org.jboss.tools.ws.jaxws.core.data.ServiceModel;
import org.jboss.tools.ws.jaxws.ui.JBossJAXWSUIPlugin;
import org.jboss.tools.ws.jaxws.ui.classpath.JBossWSRuntimeClassPathInitializer.JBossWSRuntimeClasspathContainer;

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
		IJavaProject project = model.getJavaProject();
		status = removeClassPath(project);
		return status;
	}

	private IStatus removeClassPath(IJavaProject project) {
		IStatus status = Status.OK_STATUS;
		try {
			IClasspathEntry[] entries = project.getRawClasspath();
			
			for(IClasspathEntry entry:entries){
					IClasspathContainer container = JavaCore.getClasspathContainer(entry.getPath(), project);
					if(container instanceof JBossWSRuntimeClasspathContainer){
						((JBossWSRuntimeClasspathContainer)container).removeEntry("jaxws-rt.jar"); //$NON-NLS-1$
						((JBossWSRuntimeClasspathContainer)container).removeEntry("jaxws-tools.jar"); //$NON-NLS-1$
					}
			}
		} catch (JavaModelException e) {
			JBossJAXWSUIPlugin.getDefault().logError(e);
		}
		
		return status;
	}

}
