/******************************************************************************* 
 * Copyright (c) 2008 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 ******************************************************************************/

package org.jboss.tools.ws.jaxrs.ui.internal.launcher;

import org.eclipse.core.resources.IProject;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModuleArtifact;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.model.ModuleArtifactAdapterDelegate;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.ui.cnf.UriPathTemplateElement;

/**
 * This class adapts {@link UriPathTemplateElement} into WebResources that Eclipse Run|Debug On Server functionallity
 * can handle.
 * 
 * This allows you to use the Run As functionality on the JAX-RS node elements which represent a path.
 * 
 * @author max
 * @author Xavier Coulon
 * 
 */
public class JaxrsPathModuleArtifactAdapter extends ModuleArtifactAdapterDelegate {

	/**
	 * Returns a JaxrsEndpointModuleArtifact if the given obj is an UriPathTemplateElement, null otherwise.
	 */
	@Override
	public IModuleArtifact getModuleArtifact(Object obj) {
		if (obj instanceof UriPathTemplateElement) {
			UriPathTemplateElement element = (UriPathTemplateElement) obj;
			final IJaxrsEndpoint endpoint = element.getEndpoint();
			if (endpoint != null) {
				IProject project = endpoint.getJavaProject().getProject();
				IModule module = ServerUtil.getModule(project);
				return new JaxrsEndpointModuleArtifact(module, endpoint);
			}
		}

		return null;
	}

}
