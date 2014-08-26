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
 * Interface to get notified of the JAX-RS Metamodel changes.
 * 
 * @author xcoulon
 * 
 */
public interface IJaxrsMetamodelChangedListener {

	/**
	 * Method called when a JAX-RS Metamodel was added
	 * 
	 * @param delta the metamodel delta that occurred.
	 */
	public void notifyMetamodelChanged(final JaxrsMetamodelDelta delta);

	/**
	 * Method called when the problem level of the given {@link IJaxrsMetamodel} changed
	 * @param metamodel the metamodel whose problem level changed
	 */
	public void notifyMetamodelProblemLevelChanged(final IJaxrsMetamodel jaxrsMetamodel);
	
	/**
	 * Method called when a JAX-RS Endpoint changed (ADDED, CHANGED or REMOVED)
	 * in the Metamodel.
	 * 
	 * @param delta
	 */
	public void notifyEndpointChanged(final JaxrsEndpointDelta delta);

	/**
	 * Method called when the problem level of the given {@link IJaxrsEndpoint} changed
	 * @param affectedEndpoint the endpoint whose problem level changed
	 */
	public void notifyEndpointProblemLevelChanged(final IJaxrsEndpoint affectedEndpoint);

}
