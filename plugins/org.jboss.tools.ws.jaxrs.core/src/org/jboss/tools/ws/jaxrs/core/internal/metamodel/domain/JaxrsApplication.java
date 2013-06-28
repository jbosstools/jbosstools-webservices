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

package org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain;

import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsApplication;

/**
 * Base class for JAX-RS Applications. Mostly justified for usage with generic types.
 * @author xcoulon
 *
 */
public abstract class JaxrsApplication extends JaxrsBaseElement implements IJaxrsApplication {

	/**
	 * Mandatory constructor.
	 * @param metamodel
	 */
	public JaxrsApplication(JaxrsMetamodel metamodel) {
		super(metamodel);
	}

	
}
