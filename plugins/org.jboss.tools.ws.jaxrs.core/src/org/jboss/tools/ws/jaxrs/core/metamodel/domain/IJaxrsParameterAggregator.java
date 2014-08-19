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

import java.util.List;

import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsParameterAggregatorField;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsParameterAggregatorProperty;

/**
 * @author xcoulon
 *
 */
public interface IJaxrsParameterAggregator extends IJaxrsJavaElement {

	/**
	 * @return the List {@link JaxrsParameterAggregatorField} found on this {@link IJaxrsParameterAggregator}.
	 */
	public List<JaxrsParameterAggregatorField> getAllFields();

	/**
	 * @return the List {@link JaxrsParameterAggregatorProperty} found on this {@link IJaxrsParameterAggregator}.
	 */
	public List<JaxrsParameterAggregatorProperty> getAllProperties();

}
