/******************************************************************************* 
 * Copyright (c) 2010 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.ws.ui.wizards;

import java.io.File;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jst.j2ee.model.IModelProvider;
import org.eclipse.jst.j2ee.model.ModelProviderManager;
import org.eclipse.jst.javaee.core.UrlPatternType;
import org.eclipse.jst.javaee.web.Servlet;
import org.eclipse.jst.javaee.web.ServletMapping;
import org.eclipse.jst.javaee.web.WebApp;
import org.jboss.tools.ws.core.utils.StatusUtils;
import org.jboss.tools.ws.creation.core.data.ServiceModel;
import org.jboss.tools.ws.creation.core.messages.JBossWSCreationCoreMessages;
import org.jboss.tools.ws.creation.core.utils.JBossWSCreationUtils;
import org.jboss.tools.ws.ui.messages.JBossWSUIMessages;

public class JBossWSGenerateWizardValidator {
	
	private static ServiceModel model;
	private static ServletDescriptor[] descriptors;
	
	private static String JAVA = ".java"; //$NON-NLS-1$

	public static void setServiceModel ( ServiceModel inModel ) {
		model = inModel;

		descriptors = new ServletDescriptor[model.getServiceClasses().size()];
   		List<String> serviceClasses = model.getServiceClasses();
   		for (int i = 0; i < serviceClasses.size(); i++) {
   			descriptors[i] = getServletDescriptor(serviceClasses.get(i));
   		}
	}

	public static IStatus isWSNameValid() {
		if (model.getWebProjectName() == null) {
			return StatusUtils.errorStatus(JBossWSUIMessages.JBossWSGenerateWizard_NoProjectSelected);
		}
		else {
			try {
				ModelProviderManager
					.getModelProvider(JBossWSCreationUtils.getProjectByName(model
							.getWebProjectName()));
			} catch (IllegalArgumentException iae) {
				// ignore
				model.setWebProjectName(null);
				return StatusUtils.errorStatus(JBossWSUIMessages.JBossWSGenerateWizard_NoProjectSelected);
			}
		}
		final IModelProvider provider = ModelProviderManager
			.getModelProvider(JBossWSCreationUtils.getProjectByName(model
				.getWebProjectName()));
		Object object = provider.getModelObject();
		if (object instanceof WebApp) {
			WebApp webApp = (WebApp) object;
			if (model != null) {
				for (int i = 0; i < descriptors.length; i++) {
					if (descriptors[i].getName().trim().length() == 0 ) {
						return StatusUtils.errorStatus(JBossWSUIMessages.Error_JBossWS_GenerateWizard_ServiceName_Empty);
					}
					List<?> theServlets = webApp.getServlets();
					for (int j = 0; j < theServlets.size(); j++) {
						Servlet aServlet = (Servlet) theServlets
								.get(j);
						if (aServlet.getServletName().equals(descriptors[i].getName())) {
							return StatusUtils.errorStatus(JBossWSCreationCoreMessages.Error_JBossWS_GenerateWizard_WSName_Same);
						}
					}
					List<?> theServletMappings = webApp.getServletMappings();
					for (int j = 0; j < theServletMappings.size(); j++) {
						ServletMapping aServletMapping = (ServletMapping) theServletMappings
								.get(j);
						List<?> urlPatterns = aServletMapping.getUrlPatterns();
						for (int k = 0; k < urlPatterns.size(); k++) {
							UrlPatternType upt = (UrlPatternType) urlPatterns.get(k);
							if (aServletMapping.getServletName().equals(descriptors[i].getName()) ||
									upt.getValue().equals(descriptors[i].getMappings())) {
								return StatusUtils.errorStatus(JBossWSCreationCoreMessages.Error_JBossWS_GenerateWizard_WSName_Same);
							}
						}
					}
				}
			}
			return null;
		}
		return null;
	}
	
	public static IStatus isWSClassValid(String className, IProject project) {
		if (model.getCustomPackage().trim().length() == 0) {
			// empty package name
			return StatusUtils.errorStatus(JBossWSUIMessages.Error_JBossWS_GenerateWizard_ClassName_Same);
		}
		else if (model.getCustomClassName().trim().length() == 0 ) {
			// empty class name
			return StatusUtils.errorStatus(JBossWSUIMessages.Error_JBossWS_GenerateWizard_ClassName_Same);
		}
		else if (project == null) {
			return StatusUtils.errorStatus(JBossWSUIMessages.JBossWSGenerateWizard_NoProjectSelected);
		}
		else {
			File file = findFileByPath(className + JAVA, project
					.getLocation().toOSString());
			if (file != null) {
				// class already exists
				return StatusUtils.errorStatus(JBossWSUIMessages.Error_JBossWS_GenerateWizard_ClassName_Same);
			}
		}
		return null;
	}

	private static ServletDescriptor getServletDescriptor(String clsName) {
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
		sd.setMappings(JBossWSCreationCoreMessages.Separator_Java + sd.getDisplayName());
		return sd;
	}

	private static File findFileByPath(String name, String path) {
		File ret = null;
		File folder = new File(path);
		if (folder.isDirectory()) {
			File[] files = folder.listFiles();
			for (File file : files) {
				ret = findFileByPath(name, file.getAbsolutePath());
				if (ret != null) {
					break;
				}
			}
		} else {
			if (name.equals(folder.getName())) {
				ret = folder;
			}
		}
		return ret;
	}
}
