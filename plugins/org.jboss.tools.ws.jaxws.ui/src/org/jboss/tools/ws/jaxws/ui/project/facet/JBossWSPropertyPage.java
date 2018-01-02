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
package org.jboss.tools.ws.jaxws.ui.project.facet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.wst.common.componentcore.datamodel.properties.IFacetDataModelProperties;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.jboss.tools.ws.jaxws.core.commands.JBossWSClassPathCommand;
import org.jboss.tools.ws.jaxws.core.commands.UninstallJBossWSClassPathCommand;
import org.jboss.tools.ws.jaxws.core.facet.delegate.IJBossWSFacetDataModelProperties;
import org.jboss.tools.ws.jaxws.core.facet.delegate.JBossWSFacetInstallDataModelProvider;

public class JBossWSPropertyPage extends PropertyPage implements
		IMessageNotifier {

	private IDataModel model;
	private IProject project;
	
	public JBossWSPropertyPage() {
		super();
	}

	@Override
	protected Control createContents(Composite parent) {
		if(this.getElement() instanceof IJavaProject){
			IJavaProject javaProject = (IJavaProject)this.getElement();
			project = javaProject.getProject();
		}else if(this.getElement() instanceof IProject){
			project = (IProject)this.getElement();
		}
		
		model = (IDataModel)new JBossWSFacetInstallDataModelProvider().create();
		try {
			String isDeploy = project.getPersistentProperty(IJBossWSFacetDataModelProperties.PERSISTENCE_PROPERTY_ISDEPLOYED);
			String runtimeid = project.getPersistentProperty(IJBossWSFacetDataModelProperties.PERSISTENCE_PROPERTY_QNAME_RUNTIME_NAME);
			String runtimeLocation = project.getPersistentProperty(IJBossWSFacetDataModelProperties.PERSISTENCE_PROPERTY_RNTIME_LOCATION);
			String serverSupplied = project.getPersistentProperty(
					IJBossWSFacetDataModelProperties.PERSISTENCE_PROPERTY_SERVER_SUPPLIED_RUNTIME);
			model.setProperty(IJBossWSFacetDataModelProperties.JBOSS_WS_DEPLOY, Boolean.valueOf(isDeploy));
			model.setProperty(IJBossWSFacetDataModelProperties.JBOSS_WS_RUNTIME_HOME, runtimeLocation);
			model.setProperty(IJBossWSFacetDataModelProperties.JBOSS_WS_RUNTIME_ID, runtimeid);
			model.setStringProperty(IFacetDataModelProperties.FACET_PROJECT_NAME, project.getName());
			model.setBooleanProperty(IJBossWSFacetDataModelProperties.JBOSS_WS_RUNTIME_IS_SERVER_SUPPLIED, Boolean.valueOf(serverSupplied));
			//model.setProperty(IJBossWSFacetDataModelProperties.PERSISTENT_PROPERTY_IS_SERVER_SUPPLIED_RUNTIME, Boolean.valueOf(serverSupplied));
		} catch (CoreException e) {
			//ignore 
		}
		
		JBossWSRuntimeConfigBlock block = new JBossWSRuntimeConfigBlock(model);
		block.setMessageNotifier(this);
		return block.createControl(parent);
	}

	public void notify(String msg) {
		setErrorMessage(msg);
	}

	@Override
	protected void performApply() {
		
		UninstallJBossWSClassPathCommand uninstall = new UninstallJBossWSClassPathCommand(
				project, model);
		uninstall.executeOverride(null);

		JBossWSClassPathCommand install = new JBossWSClassPathCommand(project, model);
		install.executeOverride(null);

		super.performApply();
	}
	
	

}
