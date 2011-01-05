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

import java.io.File;
import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jst.j2ee.model.IModelProvider;
import org.eclipse.jst.j2ee.model.ModelProviderManager;
import org.eclipse.jst.javaee.core.DisplayName;
import org.eclipse.jst.javaee.core.JavaeeFactory;
import org.eclipse.jst.javaee.core.UrlPatternType;
import org.eclipse.jst.javaee.web.Servlet;
import org.eclipse.jst.javaee.web.ServletMapping;
import org.eclipse.jst.javaee.web.WebApp;
import org.eclipse.jst.javaee.web.WebFactory;
import org.eclipse.jst.jee.project.facet.ICreateDeploymentFilesDataModelProperties;
import org.eclipse.jst.jee.project.facet.WebCreateDeploymentFilesDataModelProvider;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.frameworks.datamodel.AbstractDataModelOperation;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.frameworks.datamodel.IDataModelOperation;
import org.jboss.tools.ws.core.utils.StatusUtils;
import org.jboss.tools.ws.creation.core.data.ServiceModel;
import org.jboss.tools.ws.creation.core.data.ServletDescriptor;
import org.jboss.tools.ws.creation.core.messages.JBossWSCreationCoreMessages;
import org.jboss.tools.ws.creation.core.utils.JBossWSCreationUtils;

/**
 * @author Grid Qian
 */
public class MergeWebXMLCommand extends AbstractDataModelOperation {

	private ServiceModel model;
	IStatus status;
	private static String WEB_XML = "web.xml"; //$NON-NLS-1$

	public MergeWebXMLCommand(ServiceModel model) {
		this.model = model;
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {
		status = Status.OK_STATUS;
		if (!model.isUpdateWebxml()) {
			return status;
		}

		ServletDescriptor[] servletDescriptors = new ServletDescriptor[model
				.getServiceClasses().size()];
		List<String> serviceClasses = model.getServiceClasses();
		for (int i = 0; i < serviceClasses.size(); i++) {
			servletDescriptors[i] = getServletDescriptor(serviceClasses.get(i));
		}
		IProject pro = JBossWSCreationUtils.getProjectByName(model
				.getWebProjectName());
		if (!hasWebXML(pro)) {
			IVirtualComponent vc = ComponentCore.createComponent(pro);
			IDataModel model = DataModelFactory
					.createDataModel(new WebCreateDeploymentFilesDataModelProvider());
			model.setProperty(
					ICreateDeploymentFilesDataModelProperties.GENERATE_DD, vc);
			model.setProperty(
					ICreateDeploymentFilesDataModelProperties.TARGET_PROJECT,
					pro);
			IDataModelOperation op = model.getDefaultOperation();
			try {
				op.execute(new NullProgressMonitor(), null);
			} catch (ExecutionException e1) {
				// Ignore
			}
		}
		mergeWebXML(servletDescriptors);
		return status;
	}

	private void mergeWebXML(final ServletDescriptor[] servletDescriptors) {
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
	}

	@SuppressWarnings("unchecked")
	protected void addServlet(IProject projectByName,
			ServletDescriptor servletDescriptor,
			org.eclipse.jst.j2ee.webapplication.WebApp webapp) {
		@SuppressWarnings("rawtypes")
		List theServlets = webapp.getServlets();
		boolean b = false;
		for (int i = 0; i < theServlets.size(); i++) {
			org.eclipse.jst.j2ee.webapplication.Servlet aServlet = (org.eclipse.jst.j2ee.webapplication.Servlet) theServlets
					.get(i);
			if (aServlet.getServletName().equals(servletDescriptor.getName())) {
				if (b) {
					theServlets.remove(aServlet);
				} else {
					b = isOverrideServlet();
					if (b) {
						theServlets.remove(aServlet);
						break;
					} else {
						status = StatusUtils.errorStatus(""); //$NON-NLS-1$
						return;
					}
				}
			}
		}
		@SuppressWarnings("rawtypes")
		List theServletMapplings = webapp.getServletMappings();
		for (int i = 0; i < theServletMapplings.size(); i++) {
			org.eclipse.jst.j2ee.webapplication.ServletMapping aServletMapping = (org.eclipse.jst.j2ee.webapplication.ServletMapping) theServletMapplings
					.get(i);
			if (aServletMapping.getServlet().getServletName()
					.equals(servletDescriptor.getName())
					|| aServletMapping.getUrlPattern().equals(
							servletDescriptor.getMappings())) {
				if (b) {
					theServletMapplings.remove(aServletMapping);
				} else {
					b = isOverrideServlet();
					if (b) {
						theServletMapplings.remove(aServletMapping);
						break;
					} else {
						status = StatusUtils.errorStatus(""); //$NON-NLS-1$
						return;
					}
				}
			}
		}
		org.eclipse.jst.j2ee.webapplication.WebapplicationFactory factory = org.eclipse.jst.j2ee.webapplication.WebapplicationFactory.eINSTANCE;
		org.eclipse.jst.j2ee.webapplication.Servlet servlet = factory
				.createServlet();
		org.eclipse.jst.j2ee.webapplication.ServletType servletType = factory
				.createServletType();
		servlet.setWebType(servletType);
		servlet.setServletName(servletDescriptor.getName());
		servletType.setClassName(servletDescriptor.getClassName());
		if (servletDescriptor.getDisplayName() != null) {
			servlet.setDisplayName(servletDescriptor.getDisplayName());
		}
		webapp.getServlets().add(servlet);

		if (servletDescriptor.getMappings() != null) {
			org.eclipse.jst.j2ee.webapplication.ServletMapping servletMapping = factory
					.createServletMapping();
			servletMapping.setServlet(servlet);
			servletMapping.setUrlPattern(servletDescriptor.getMappings());
			webapp.getServletMappings().add(servletMapping);
		}
	}

	public void addjeeServlet(IProject webProject,
			ServletDescriptor servletDescriptor, WebApp webapp) {
		@SuppressWarnings("rawtypes")
		List theServlets = webapp.getServlets();

		boolean b = false;
		for (int i = 0; i < theServlets.size(); i++) {
			Servlet aServlet = (Servlet) theServlets.get(i);
			if (aServlet.getServletName().equals(servletDescriptor.getName())) {
				b = isOverrideServlet();
				if (b) {
					theServlets.remove(aServlet);
					break;
				} else {
					status = StatusUtils.errorStatus(""); //$NON-NLS-1$
					return;
				}
			}
		}

		List<ServletMapping> theServletMapplings = webapp.getServletMappings();
		for (int i = 0; i < theServletMapplings.size(); i++) {
			ServletMapping aServletMapping = (ServletMapping) theServletMapplings
					.get(i);
			if (aServletMapping.getServletName().equals(
					servletDescriptor.getName())) {
				if (b) {
					theServletMapplings.remove(aServletMapping);
				} else {
					b = isOverrideServlet();
					if (b) {
						theServletMapplings.remove(aServletMapping);
						break;
					} else {
						status = StatusUtils.errorStatus(""); //$NON-NLS-1$
						return;
					}
				}
			}
			List<UrlPatternType> list = aServletMapping.getUrlPatterns();
			if (list != null) {
				for (int j = 0; j < list.size(); j++) {
					UrlPatternType url = (UrlPatternType) list.get(j);
					if (url.getValue().equals(servletDescriptor.getMappings())) {
						if (b) {
							theServletMapplings.remove(aServletMapping);
						} else {
							if (isOverrideServlet()) {
								theServletMapplings.remove(aServletMapping);
								break;
							} else {
								status = StatusUtils.errorStatus(""); //$NON-NLS-1$
								return;
							}
						}
					}
				}
			}
		}

		WebFactory factory = WebFactory.eINSTANCE;
		Servlet servlet = factory.createServlet();
		servlet.setServletName(servletDescriptor.getName());
		servlet.setServletClass(servletDescriptor.getClassName());
		if (servletDescriptor.getDisplayName() != null) {
			DisplayName displayNameObj = JavaeeFactory.eINSTANCE
					.createDisplayName();
			displayNameObj.setValue(servletDescriptor.getDisplayName());
			servlet.getDisplayNames().add(displayNameObj);
		}
		webapp.getServlets().add(servlet);

		if (servletDescriptor.getMappings() != null) {
			ServletMapping servletMapping = factory.createServletMapping();
			servletMapping.setServletName(servlet.getServletName());
			UrlPatternType url = JavaeeFactory.eINSTANCE.createUrlPatternType();
			url.setValue(servletDescriptor.getMappings());
			servletMapping.getUrlPatterns().add(url);
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
		sd.setName(servletName);
		sd.setDisplayName(sd.getName());
		sd.setClassName(clsName);
		sd.setMappings(JBossWSCreationCoreMessages.Separator_Java
				+ sd.getName());
		return sd;
	}
	
	private boolean hasWebXML(IProject pro) {
		// we are looking for this recursively because though application.xml
		// is always in META-INF, it's not always in "earcontent" since the
		// earcontent folder name can be custom
		File file = JBossWSCreationUtils.findFileByPath(WEB_XML, pro
				.getLocation().toOSString());
		if (file == null) {
			return false;
		}
		return true;
	}

	private boolean isOverrideServlet() {
		boolean b = MessageDialog
				.openConfirm(
						PlatformUI.getWorkbench().getActiveWorkbenchWindow()
								.getShell(),
						JBossWSCreationCoreMessages.Confirm_Override_Servlet,
						JBossWSCreationCoreMessages.Error_JBossWS_GenerateWizard_WSName_Same);
		return b;
	}
}
