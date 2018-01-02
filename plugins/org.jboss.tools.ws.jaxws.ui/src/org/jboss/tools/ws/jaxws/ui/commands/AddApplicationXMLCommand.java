/******************************************************************************* 
 * Copyright (c) 2010 - 2014 Red Hat, Inc. and others.  
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.ws.jaxws.ui.commands;

import java.io.File;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.jee.project.facet.EarCreateDeploymentFilesDataModelProvider;
import org.eclipse.jst.jee.project.facet.ICreateDeploymentFilesDataModelProperties;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.frameworks.datamodel.AbstractDataModelOperation;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.frameworks.datamodel.IDataModelOperation;
import org.jboss.tools.ws.jaxws.ui.utils.JBossWSCreationUtils;

/**
 * @author Grid Qian
 */
public class AddApplicationXMLCommand extends AbstractDataModelOperation {

	private String earProject;
	IStatus status;
	
	private static String APPLICATION_XML = "application.xml"; //$NON-NLS-1$
	
	public AddApplicationXMLCommand(String earProject){
		this.earProject = earProject;
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {
		status = Status.OK_STATUS;
		IProject pro = ResourcesPlugin.getWorkspace().getRoot().getProject(earProject);
		
		// we only are concerned with creating the application.xml if it doesn't exist.
		// the WTP code updates any existing application.xml appropriately if it finds it.
		if(!hasApplicationXML(pro)){
			IVirtualComponent vc = ComponentCore.createComponent(pro);
			IDataModel model = DataModelFactory.createDataModel(new EarCreateDeploymentFilesDataModelProvider());
			model.setProperty(ICreateDeploymentFilesDataModelProperties.GENERATE_DD, vc);
			model.setProperty(ICreateDeploymentFilesDataModelProperties.TARGET_PROJECT, pro);
			IDataModelOperation op = model.getDefaultOperation();
			try {
				op.execute(new NullProgressMonitor(), null);
			} catch (ExecutionException e1) {
				// Ignore
			}	
		}
		return status;
	}

	private boolean hasApplicationXML(IProject pro) {
		// we are looking for this recursively because though application.xml
		// is always in META-INF, it's not always in "earcontent" since the
		// earcontent folder name can be custom
		File file = JBossWSCreationUtils.findFileByPath(APPLICATION_XML, pro.getLocation().toOSString());
        if(file == null){
        	return false;
        }
		return true;
	}
	
}
