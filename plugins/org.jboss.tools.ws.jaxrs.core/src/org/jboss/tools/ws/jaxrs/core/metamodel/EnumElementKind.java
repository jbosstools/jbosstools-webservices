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

/**
 * The functional type of the JAX-RS ElementKind. Keeping that order ensures that the elements are sorted according to
 * the order of priority into which they should be processed
 * */
public enum EnumElementKind {

	/**
	 * UNDEFINED KIND (when no annotation nor any other java element can define precisely what the element is.
	 */
	UNDEFINED,
	/** JAX-RS Project. */
	METAMODEL,
	/** A JAX-RS Application declared in the web deployment descriptor.*/
	// must be declared *before* APPLICATION_JAVA constant to enable comparison ! 
	APPLICATION_WEBXML,
	/** A java annotated type based JAX-RS Application */
	APPLICATION_JAVA,
	/** An HTTP method. */
	HTTP_METHOD,
	/** A message producer. */
	MESSAGE_BODY_WRITER,
	/** A message consumer. */
	MESSAGE_BODY_READER,
	/** An Entity Mapper (ie, both reader and writer). */
	ENTITY_MAPPER,
	/** An exception mapper. */
	EXCEPTION_MAPPER,
	/** A Context Provider. */
	CONTEXT_PROVIDER,
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
	/** PathParam field. */
	PATH_PARAM_FIELD,
	/** QueryParam field. */
	QUERY_PARAM_FIELD,
	/** MatrixParam field. */
	MATRIX_PARAM_FIELD; 
}
