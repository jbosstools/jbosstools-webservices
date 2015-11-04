/******************************************************************************* 
 * Copyright (c) 2008 - 2014 Red Hat, Inc. and others. 
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
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.model.ModuleArtifactDelegate;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsEndpoint;

/**
 * Module Artifact that carries the JAX-RS Endpoint deployed on a server.
 * 
 * @author Xavier Coulon
 *
 */
public class JaxrsEndpointModuleArtifact extends ModuleArtifactDelegate {

	/** the module.*/
	private IModule module;
	
	/** the JAX-RS Endpoint that should be called.*/
	private IJaxrsEndpoint endpoint;
	private String endpointURI;
	private String httpVerb;
	
	
	// No-arg constructor used for deserialization
	public JaxrsEndpointModuleArtifact() {
		
	}
	
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
	
	@Deprecated
	public IJaxrsEndpoint getEndpoint() {
		return endpoint;
	}

	public String getEndpointURI() {
		if( endpointURI == null && endpoint != null) {
			endpointURI = computeEndpointURI(module, endpoint);
		}
		return endpointURI;
	}
	
	public String getHttpVerb() {
		if( endpoint != null )
			return endpoint.getHttpMethod().getHttpVerb();
		return httpVerb;
	}
	
	/**
	 * Creates the endpoint URI from the endpoint, by looking at the servers on which the module is deployed.
	 * 
	 * @param element
	 * @return
	 */
	private String computeEndpointURI(final IModule module, final IJaxrsEndpoint endpoint) {
		String uriPathTemplate = endpoint.getUriPathTemplate();
		if (!uriPathTemplate.startsWith("/")) { //$NON-NLS-1$
			uriPathTemplate = "/"+uriPathTemplate; //$NON-NLS-1$
		}
		String fullUriPathTemplate = module.getName() + uriPathTemplate;
		// check to see if this project has been deployed...
		IServer[] servers = ServerUtil.getServersByModule(module, null);
		if (servers == null || servers.length == 0) {
			return "http://[domain]:[port]/" + fullUriPathTemplate; //$NON-NLS-1$
		}
		// if it's been deployed, we can grab the domain and web port
		String domain = servers[0].getHost();
		String webport = servers[0].getAttribute("org.jboss.ide.eclipse.as.core.server.webPort", "8080");//$NON-NLS-1$ //$NON-NLS-2$
		return "http://" + domain + ':' + webport + "/" + fullUriPathTemplate; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

	}

	@Override
	public String getName() {
		return getEndpointURI();
	}
	
	// Very simple serialization, three lines:
	// 1)  module id
	// 2)  endpoint uri
	// 3)  http verb
	public String serialize() {
		StringBuffer sb = new StringBuffer();
		sb.append(module.getId());
		sb.append("\n");
		sb.append(getEndpointURI());
		sb.append("\n");
		sb.append(getHttpVerb());
		return sb.toString();
	}

	/**
	 * Deserialize this object from a serialized string.
	 * 
	 * @param s a serialized string.
	 */
	public void deserialize(String s) {
		String[] byLine = s.split("\n");
		if( byLine != null ) {
			if( byLine.length > 0 && byLine[0] != null)
				this.module = ServerUtil.getModule(byLine[0]);
			if( byLine.length > 1 && byLine[1] != null)
				this.endpointURI = byLine[1];
			if( byLine.length > 2 && byLine[2] != null)
				this.httpVerb = byLine[2];
		}
	}
}
