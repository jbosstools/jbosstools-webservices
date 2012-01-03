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
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.pubsub.EventService;

public class JaxrsElementChangedPublisher {

	public void publish(List<JaxrsEndpointChangedEvent> jaxrsEndpointChanges, IProgressMonitor progressMonitor) {
		if (jaxrsEndpointChanges == null || jaxrsEndpointChanges.isEmpty()) {
			Logger.debug("No JAX-RS change to publish to the UI");
		} else {
			for (JaxrsEndpointChangedEvent change : jaxrsEndpointChanges) {
				Logger.debug(change.toString());
				EventService.getInstance().publish(change);
			}
		}
	}
}
