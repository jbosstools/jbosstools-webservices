/******************************************************************************* 
 * Copyright (c) 2008 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 ******************************************************************************/
package org.jboss.tools.ws.jaxrs.ui.internal.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.server.core.IModuleArtifact;
import org.eclipse.wst.server.core.IServer;

public class WSTesterLaunchableAdapterDelegate extends org.eclipse.wst.server.core.model.LaunchableAdapterDelegate {

	public WSTesterLaunchableAdapterDelegate() {
	}

	/**
	 * Returns the given moduleArtifact if it is an JaxrsEndpointModuleArtifact, null otherwise.
	 */
	@Override
	public Object getLaunchable(IServer server, IModuleArtifact moduleArtifact) throws CoreException {
		if (moduleArtifact instanceof JaxrsEndpointModuleArtifact) {
			return moduleArtifact;
		}
		return null;
	}
}
