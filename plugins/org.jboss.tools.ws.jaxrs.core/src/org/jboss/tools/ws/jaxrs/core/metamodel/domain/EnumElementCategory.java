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

public enum EnumElementCategory {
	/** Application */
	APPLICATION,
	/** HTTP Method */
	HTTP_METHOD, 
	/** Provider */
	PROVIDER, 
	/** Name Binding */
	NAME_BINDING, 
	/** Resource */
	RESOURCE, 
	/** Resource Field */
	RESOURCE_FIELD, 
	/** Resource Property */
	RESOURCE_PROPERTY, 
	/** Resource Method */
	RESOURCE_METHOD, 
	/** Parameter Converter Provider */
	PARAM_CONVERTER_PROVIDER, 
	/** Parameter Aggregator */
	PARAMETER_AGGREGATOR, 
	/** Parameter Aggregator Field */
	PARAMETER_AGGREGATOR_FIELD, 
	/** Parameter Aggregator Property*/
	PARAMETER_AGGREGATOR_PROPERTY, 
	/** Endpoint */
	ENDPOINT, 
	/** Undefined */
	UNDEFINED;

}
