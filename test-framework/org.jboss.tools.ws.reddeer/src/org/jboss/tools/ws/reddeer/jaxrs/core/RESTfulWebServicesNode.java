/*******************************************************************************
 * Copyright (c) 2016-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.ws.reddeer.jaxrs.core;

import java.util.ArrayList;
import java.util.List;

import org.jboss.reddeer.eclipse.core.resources.ProjectItem;
import org.jboss.reddeer.eclipse.jdt.ui.ProjectExplorer;

/**
 * Represents the JAX-RS Web Services node in Project explorer. Contains several useful method for 
 * obtaining information about JAX-RS web services.
 * 
 * @author mlabuda@redhat.com
 */
public class RESTfulWebServicesNode {

	private ProjectExplorer explorer;
	private ProjectItem projectItem;
	
	public RESTfulWebServicesNode(String projectName) {
		if (projectName == null) {
			throw new IllegalArgumentException("Project name cannot be null.");
		}
		explorer = new ProjectExplorer();
		explorer.open();
		projectItem = explorer.getProject(projectName).getProjectItem(RESTfulLabel.REST_WS_NODE);
	}
	
	public void select() {
		explorer.activate();
		projectItem.select();
	}
	
	public void refresh() {
		explorer.activate();
		projectItem.refresh();
	}

	/**
	 * Gets RESTful web services currently defined for a project.
	 * @return list of RESTful web services for a project
	 */
	public List<RESTfulWebService> getWebServices() {
		explorer.activate();
		List<RESTfulWebService> webServices = new ArrayList<RESTfulWebService>();
		for (ProjectItem webServiceProjectItem: projectItem.getChildren()) {
			webServices.add(new RESTfulWebService(webServiceProjectItem));
		}
		return webServices;
	}
	
	/**
	 * Gets RESTful web services with specific HTTP method.
	 * 
	 * @param method HTTP method
	 * @return list of RESTful web services
	 */
	public List<RESTfulWebService> getWebServiceByMethod(String method) {
		List<RESTfulWebService> webServices = new ArrayList<RESTfulWebService>();
		for (RESTfulWebService webService: getWebServices()) {
			if (webService.getMethod().equals(method)) {
				webServices.add(webService);
			}
		}
		return webServices;
	}
	
	/**
	 * Gets RESTful web services with specific URI matching pattern.
	 * @param path URI matching pattern
	 * @return list of RESTful web services
	 */
	public List<RESTfulWebService> getWebServiceByPath(String path) {
		List<RESTfulWebService> webServices = new ArrayList<RESTfulWebService>();
		for (RESTfulWebService webService: getWebServices()) {
			if (webService.getPath().equals(path)) {
				webServices.add(webService);
			}
		}
		return webServices;
	}
	
	/** Gets RESTful web services with specific URI matching pattern and HTTP method.
	 * 
	 * @param path URI matching pattern
	 * @param method HTTP method
	 * @return list of RESTful web services
	 */
	public List<RESTfulWebService> getWebServiceByPathAndMethod(String path, String method) {
		List<RESTfulWebService> webServices = new ArrayList<RESTfulWebService>();
		for (RESTfulWebService webService: getWebServiceByMethod(method)) {
			if (webService.getPath().equals(path)) {
				webServices.add(webService);
			}
		}
		return webServices;
	}
}
