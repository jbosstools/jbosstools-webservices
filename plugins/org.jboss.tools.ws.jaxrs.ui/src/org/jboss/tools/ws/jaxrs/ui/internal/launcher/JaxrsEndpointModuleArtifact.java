/******************************************************************************* 
 * Copyright (c) 2008 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/

package org.jboss.tools.ws.jaxrs.ui.internal.launcher;

import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModuleArtifact;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsEndpoint;

/**
 * Module Artifact that carries the JAX-RS Endpoint deployed on a server.
 * 
 * @author Xavier Coulon
 *
 */
public class JaxrsEndpointModuleArtifact implements IModuleArtifact {

	/** the module.*/
	private final IModule module;
	
	/** the JAX-RS Endpoint that should be called.*/
	private final IJaxrsEndpoint endpoint;
	
	/**
	 * Full constructor.
	 * @param module
	 * @param endpoint
	 */
	public JaxrsEndpointModuleArtifact(final IModule module, final IJaxrsEndpoint endpoint) {
		this.module = module;
		this.endpoint = endpoint;
	}
	
	@Override
	public IModule getModule() {
		return module;
	}
	
	public IJaxrsEndpoint getEndpoint() {
		return endpoint;
	}

}
