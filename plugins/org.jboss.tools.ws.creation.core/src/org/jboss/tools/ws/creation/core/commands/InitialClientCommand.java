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

import java.util.List;

import javax.wsdl.Definition;
import javax.wsdl.WSDLException;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.wst.common.frameworks.datamodel.AbstractDataModelOperation;
import org.eclipse.wst.ws.internal.wsrt.IWebServiceClient;
import org.eclipse.wst.ws.internal.wsrt.WebServiceScenario;
import org.jboss.tools.ws.core.utils.StatusUtils;
import org.jboss.tools.ws.creation.core.data.ServiceModel;
import org.jboss.tools.ws.creation.core.messages.JBossWSCreationCoreMessages;
import org.jboss.tools.ws.creation.core.utils.JBossWSCreationUtils;

/**
 * @author Grid Qian
 */
@SuppressWarnings("restriction")
public class InitialClientCommand extends AbstractDataModelOperation {
	private ServiceModel model;
	private IWebServiceClient wsClient;
	private int scenario;

	public InitialClientCommand(ServiceModel model, IWebServiceClient wsClient,
			int scenario) {
		this.model = model;
		this.wsClient = wsClient;
		this.scenario = scenario;
		model.setWsScenario(scenario);

	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {
		IJavaProject project = null;
		try {
			project = JavaCore.create(JBossWSCreationUtils.getProjectByName(model.getWebProjectName()));
			model.setJavaProject(project);
			String location = JBossWSCreationUtils.getJBossWSRuntimeLocation(project.getProject());
			if (location.equals("")) { //$NON-NLS-1$
				return StatusUtils
						.errorStatus(JBossWSCreationCoreMessages.Error_WS_Location);
			} else if (!new Path(location)
					.append(JBossWSCreationCoreMessages.Bin)
					.append(JBossWSCreationCoreMessages.Command).toFile()
					.exists()) {
				return StatusUtils
						.errorStatus(JBossWSCreationCoreMessages.Error_WS_Location);
			}
		} catch (CoreException e1) {
			return StatusUtils
					.errorStatus(JBossWSCreationCoreMessages.Error_WS_Location);
		}
		model.setTarget(JBossWSCreationCoreMessages.Value_Target_0);
		try {
			List<String> list = JBossWSCreationUtils.getJavaProjectSrcFolder(project.getProject());
			if (list != null && list.size() > 0) {
				model.setSrcList(list);
				model.setJavaSourceFolder(list.get(0));
			} else {
				return StatusUtils.errorStatus(JBossWSCreationCoreMessages.Error_Message_No_SourceFolder);
			}
		} catch (JavaModelException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if (scenario == WebServiceScenario.CLIENT) {
			model.setWsdlURI(wsClient.getWebServiceClientInfo().getWsdlURL());
			Definition definition = null;
			try {
				definition = JBossWSCreationUtils.readWSDL(model.getWsdlURI());
			} catch (WSDLException e) {
				return StatusUtils.errorStatus(JBossWSCreationCoreMessages.Error_Read_WSDL);
			}
			model.setWsdlDefinition(definition);
			model.setCustomPackage(""); //$NON-NLS-1$
		}

		return Status.OK_STATUS;
	}

	public ServiceModel getWebServiceDataModel() {

		return model;
	}
}
