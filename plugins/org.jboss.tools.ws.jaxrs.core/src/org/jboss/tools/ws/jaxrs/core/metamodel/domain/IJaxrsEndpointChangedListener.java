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

package org.jboss.tools.ws.jaxrs.core.metamodel.domain;



/**
 * Interface to get notified of the JAX-RS Endpoint changes.
 * 
 * @author xcoulon
 * 
 */
public interface IJaxrsEndpointChangedListener {


	/**
	 * Method called when a JAX-RS Endpoint changed (ADDED, CHANGED or REMOVED)
	 * in the Metamodel.
	 * 
	 * @param delta
	 */
	public void notifyEndpointChanged(JaxrsEndpointDelta delta);

	/**
	 * Method called when the problem level of the given {@link IJaxrsEndpoint} changed
	 * @param affectedEndpoint the endpoint whose problem level changed
	 */
	public void notifyEndpointProblemLevelChanged(IJaxrsEndpoint affectedEndpoint);

	/**
	 * Method called when the problem level of the given {@link IJaxrsMetamodel} changed
	 * @param metamodel the metamodel whose problem level changed
	 */
	public void notifyMetamodelProblemLevelChanged(IJaxrsMetamodel jaxrsMetamodel);

}
