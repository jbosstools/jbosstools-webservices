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

import java.util.EventObject;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.jboss.tools.ws.jaxrs.core.internal.utils.ConstantUtils;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.metamodel.JaxrsMetamodelDelta;
import org.jboss.tools.ws.jaxrs.core.pubsub.EventService;

public class JaxrsElementChangedPublisher {

	public void publish(List<JaxrsMetamodelDelta> affectedMetamodel, IProgressMonitor progressMonitor) {
		for (JaxrsMetamodelDelta metamodelDelta : affectedMetamodel) {
			publish(metamodelDelta, progressMonitor);
		}
	}
	
	public void publish(JaxrsMetamodelDelta metamodelDelta, IProgressMonitor progressMonitor) {
		Logger.debug("*** Notifying the UI that JAX-RS metamodel was {} (including {} endpoint changes) ***",
				ConstantUtils.getStaticFieldName(IJavaElementDelta.class, metamodelDelta.getDeltaKind()),
				metamodelDelta.getAffectedEndpoints().size());
		EventService.getInstance().publish(new EventObject(metamodelDelta));
		progressMonitor.worked(1);
	}
}
