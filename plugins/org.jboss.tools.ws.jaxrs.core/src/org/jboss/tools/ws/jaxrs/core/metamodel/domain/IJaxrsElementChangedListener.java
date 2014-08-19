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
 * Interface to get notified of the JAX-RS Element changes.
 * 
 * @author xcoulon
 * 
 */
public interface IJaxrsElementChangedListener {

	/**
	 * Method called when a JAX-RS Element changed (ADDED, CHANGED or REMOVED)
	 * in the Metamodel.
	 * 
	 * @param delta
	 */
	public void notifyElementChanged(final JaxrsElementDelta delta);

}
