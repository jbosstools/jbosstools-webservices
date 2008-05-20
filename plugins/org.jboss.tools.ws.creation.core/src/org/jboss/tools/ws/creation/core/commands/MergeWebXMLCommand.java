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

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.j2ee.model.IModelProvider;
import org.eclipse.jst.j2ee.model.ModelProviderManager;
import org.eclipse.jst.javaee.core.DisplayName;
import org.eclipse.jst.javaee.core.JavaeeFactory;
import org.eclipse.jst.javaee.core.UrlPatternType;
import org.eclipse.jst.javaee.web.Servlet;
import org.eclipse.jst.javaee.web.ServletMapping;
import org.eclipse.jst.javaee.web.WebApp;
import org.eclipse.jst.javaee.web.WebFactory;
import org.eclipse.wst.common.environment.IEnvironment;
import org.eclipse.wst.common.frameworks.datamodel.AbstractDataModelOperation;
import org.jboss.tools.ws.creation.core.data.ServiceModel;
import org.jboss.tools.ws.creation.core.messages.JBossWSCreationCoreMessages;
import org.jboss.tools.ws.creation.core.utils.JBossWSCreationUtils;

/**
 * @author Grid Qian
 */
public class MergeWebXMLCommand extends AbstractDataModelOperation {

	private ServiceModel model;

	public MergeWebXMLCommand(ServiceModel model) {
		this.model = model;
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {
		IEnvironment environment = getEnvironment();
		IStatus status = null;
		status = mergeWebXML(getAxisServletDescriptor());
		if (status.getSeverity() == Status.ERROR) {
			environment.getStatusHandler().reportError(status);
			return status;
		}
		return Status.OK_STATUS;
	}

	private IStatus mergeWebXML(final ServletDescriptor servletDescriptor) {
		IStatus status = Status.OK_STATUS;
		final IModelProvider provider = ModelProviderManager
				.getModelProvider(JBossWSCreationUtils.getProjectByName(model
						.getWebProjectName()));
		provider.modify(new Runnable() {
			public void run() {
				Object object = provider.getModelObject();
				if (object instanceof org.eclipse.jst.javaee.web.WebApp) {
					WebApp webApp = (WebApp) object;
					addServlet(JBossWSCreationUtils.getProjectByName(model
							.getWebProjectName()), servletDescriptor, webApp);
				}
			}

		}, null);
		return status;
	}

	private ServletDescriptor getAxisServletDescriptor() {

		ServletDescriptor sd = new ServletDescriptor();
		sd._name = JBossWSCreationUtils.classNameFromQualifiedName(model
				.getServiceClass());
		sd._displayName = sd._name;
		sd._className = model.getServiceClass();
		sd._mappings = JBossWSCreationCoreMessages.SEPARATOR_JAVA + sd._name;
		return sd;
	}

	@SuppressWarnings("unchecked")
	public void addServlet(IProject webProject,
			ServletDescriptor servletDescriptor, WebApp webapp) {
		List theServlets = webapp.getServlets();
		for (int i = 0; i < theServlets.size(); i++) {
			Servlet aServlet = (Servlet) theServlets.get(i);
			if (aServlet.getServletName().equals(servletDescriptor._name)) {
				return;
			}
		}
		WebFactory factory = WebFactory.eINSTANCE;
		Servlet servlet = factory.createServlet();
		servlet.setServletName(servletDescriptor._name);
		servlet.setServletClass(servletDescriptor._className);
		if (servletDescriptor._displayName != null) {
			DisplayName displayNameObj = JavaeeFactory.eINSTANCE
					.createDisplayName();
			displayNameObj.setValue(servletDescriptor._displayName);
			servlet.getDisplayNames().add(displayNameObj);
		}
		webapp.getServlets().add(servlet);

		if (servletDescriptor._mappings != null) {
			ServletMapping servletMapping = factory.createServletMapping();
			servletMapping.setServletName(servlet.getServletName());
			UrlPatternType url = JavaeeFactory.eINSTANCE.createUrlPatternType();
			url.setValue(servletDescriptor._mappings);
			servletMapping.getUrlPatterns().add(url);
			webapp.getServletMappings().add(servletMapping);
		}
	}

	public class ServletDescriptor {
		String _name;
		String _className;
		String _displayName;
		String _mappings;
	}

}
