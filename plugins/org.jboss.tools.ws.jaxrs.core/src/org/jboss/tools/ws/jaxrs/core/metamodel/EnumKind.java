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
package org.jboss.tools.ws.jaxrs.core.metamodel;

/** The functional type of the JAX-RS ElementKind. */
public enum EnumKind {
	/**
	 * UNDEFINED KIND (when no annotation nor any other java element can define
	 * precisely what the element is.
	 */
	UNDEFINED,
	/** An application */
	APPLICATION,
	/** A root resource. */
	ROOT_RESOURCE,
	/** A subresource. */
	SUBRESOURCE,
	/** A resource method. */
	RESOURCE_METHOD,
	/** A subresource method. */
	SUBRESOURCE_METHOD,
	/** A subresource locator. */
	SUBRESOURCE_LOCATOR,
	/** A message producer. */
	PRODUCER,
	/** A message consumer. */
	CONSUMER,
	/** An exception mapper. */
	EXCEPTION_MAPPER,
	/** PathParam field. */
	PATH_PARAM_FIELD,
	/** QueryParam field. */
	QUERY_PARAM_FIELD,
	/** MatrixParam field. */
	MATRIX_PARAM_FIELD,
	/** An HTTP method. */
	HTTP_METHOD;
}
