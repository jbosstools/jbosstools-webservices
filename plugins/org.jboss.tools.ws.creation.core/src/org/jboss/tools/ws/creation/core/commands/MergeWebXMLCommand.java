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
		if (!model.isUpdateWebxml()) {
			return Status.OK_STATUS;
		}

		IEnvironment environment = getEnvironment();
		IStatus status = null;
		ServletDescriptor[] servletDescriptors = new ServletDescriptor[model
				.getServiceClasses().size()];
		List<String> serviceClasses = model.getServiceClasses();
		for (int i = 0; i < serviceClasses.size(); i++) {
			servletDescriptors[i] = getServletDescriptor(serviceClasses.get(i));
		}

		status = mergeWebXML(servletDescriptors);
		if (status.getSeverity() == Status.ERROR) {
			environment.getStatusHandler().reportError(status);
			return status;
		}
		return Status.OK_STATUS;
	}

	private IStatus mergeWebXML(final ServletDescriptor[] servletDescriptors) {
		IStatus status = Status.OK_STATUS;
		final IModelProvider provider = ModelProviderManager
				.getModelProvider(JBossWSCreationUtils.getProjectByName(model
						.getWebProjectName()));
		provider.modify(new Runnable() {
			public void run() {
				Object object = provider.getModelObject();
				if (object instanceof WebApp) {
					WebApp webApp = (WebApp) object;
					for (int i = 0; i < servletDescriptors.length; i++) {
						addjeeServlet(JBossWSCreationUtils
								.getProjectByName(model.getWebProjectName()),
								servletDescriptors[i], webApp);
					}
				}
				if (object instanceof org.eclipse.jst.j2ee.webapplication.WebApp) {
					org.eclipse.jst.j2ee.webapplication.WebApp webApp = (org.eclipse.jst.j2ee.webapplication.WebApp) object;
					for (int i = 0; i < servletDescriptors.length; i++) {
						addServlet(JBossWSCreationUtils.getProjectByName(model
								.getWebProjectName()), servletDescriptors[i],
								webApp);
					}
				}
			}

		}, null);
		return status;
	}

	@SuppressWarnings("unchecked")
	protected void addServlet(IProject projectByName,
			ServletDescriptor servletDescriptor,
			org.eclipse.jst.j2ee.webapplication.WebApp webapp) {
		List theServlets = webapp.getServlets();
		for (int i = 0; i < theServlets.size(); i++) {
			org.eclipse.jst.j2ee.webapplication.Servlet aServlet = (org.eclipse.jst.j2ee.webapplication.Servlet) theServlets
					.get(i);
			if (aServlet.getServletName().equals(servletDescriptor._name)) {
				return;
			}
		}
		org.eclipse.jst.j2ee.webapplication.WebapplicationFactory factory = org.eclipse.jst.j2ee.webapplication.WebapplicationFactory.eINSTANCE;
		org.eclipse.jst.j2ee.webapplication.Servlet servlet = factory
				.createServlet();
		org.eclipse.jst.j2ee.webapplication.ServletType servletType = factory
				.createServletType();
		servlet.setWebType(servletType);
		servlet.setServletName(servletDescriptor._name);
		servletType.setClassName(servletDescriptor._className);
		if (servletDescriptor._displayName != null) {
			servlet.setDisplayName(servletDescriptor._displayName);
		}
		webapp.getServlets().add(servlet);

		if (servletDescriptor._mappings != null) {
			org.eclipse.jst.j2ee.webapplication.ServletMapping servletMapping = factory
					.createServletMapping();
			servletMapping.setServlet(servlet);
			servletMapping.setUrlPattern(servletDescriptor._mappings);
			webapp.getServletMappings().add(servletMapping);
		}

	}

	private ServletDescriptor getServletDescriptor(String clsName) {
		String servletName = model.getServiceName();
		if (servletName == null) {
			servletName = JBossWSCreationUtils
					.classNameFromQualifiedName(clsName);
		}
		if (servletName.endsWith("Impl") && servletName.length() > 4) { //$NON-NLS-1$
			servletName = servletName.substring(0, servletName.length() - 4);
		}
		ServletDescriptor sd = new ServletDescriptor();
		sd._name = servletName;
		sd._displayName = sd._name;
		sd._className = clsName;
		sd._mappings = JBossWSCreationCoreMessages.Separator_Java + sd._name;
		return sd;
	}

	@SuppressWarnings("unchecked")
	public void addjeeServlet(IProject webProject,
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
