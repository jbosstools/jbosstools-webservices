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
package org.jboss.tools.ws.jaxrs.core.metamodel.domain;

/**
 * The functional type of the JAX-RS ElementKind. Keeping that order ensures that the elements are sorted according to
 * the order of priority into which they should be processed
 * */
public enum EnumElementKind {

	/** A JAX-RS Application declared in the web deployment descriptor.*/
	APPLICATION_WEBXML(EnumElementCategory.APPLICATION),
	/** A java annotated type based JAX-RS Application */
	APPLICATION_JAVA(EnumElementCategory.APPLICATION),

	/** An HTTP method. */
	HTTP_METHOD(EnumElementCategory.HTTP_METHOD),
	
	/** A message producer. */
	MESSAGE_BODY_WRITER(EnumElementCategory.PROVIDER),
	/** A message consumer. */
	MESSAGE_BODY_READER(EnumElementCategory.PROVIDER),
	/** An Entity Mapper (ie, both MessageBodyReader and MessageBodyWriter). */
	ENTITY_MAPPER(EnumElementCategory.PROVIDER),
	/** An exception mapper. */
	EXCEPTION_MAPPER(EnumElementCategory.PROVIDER),
	/** A Context Provider. */
	CONTEXT_RESOLVER(EnumElementCategory.PROVIDER),
	/** A Container Request Filter.*/
	CONTAINER_REQUEST_FILTER(EnumElementCategory.PROVIDER),
	/** A Container Response Filter.*/
	CONTAINER_RESPONSE_FILTER(EnumElementCategory.PROVIDER),
	/** A Container Filter (ie, both ContainerRequestFilter and ContainerResponseFilter).*/
	CONTAINER_FILTER(EnumElementCategory.PROVIDER),
	/** An Entity Reader Interceptor.*/
	ENTITY_READER_INTERCEPTOR(EnumElementCategory.PROVIDER),
	/** An Entity Writer Interceptor.*/
	ENTITY_WRITER_INTERCEPTOR(EnumElementCategory.PROVIDER),
	/** An Entity Interceptor (ie, both EntityReaderInterceptor and EntityWriterInterceptor).*/
	ENTITY_INTERCEPTOR(EnumElementCategory.PROVIDER),
	/** An Interceptor/Filter Name Binding Annotation.*/
	NAME_BINDING(EnumElementCategory.NAME_BINDING),
	
	/** Undefined Provider. */
	UNDEFINED_PROVIDER(EnumElementCategory.PROVIDER),
	
	/** A root resource. */
	ROOT_RESOURCE(EnumElementCategory.RESOURCE),
	/** A subresource. */
	SUBRESOURCE(EnumElementCategory.RESOURCE),
	/** Undefined Resource Method. */
	UNDEFINED_RESOURCE(EnumElementCategory.RESOURCE),
	
	/** A resource method. */
	RESOURCE_METHOD(EnumElementCategory.RESOURCE_METHOD),
	/** A subresource method. */
	SUBRESOURCE_METHOD(EnumElementCategory.RESOURCE_METHOD),
	/** A subresource locator. */
	SUBRESOURCE_LOCATOR(EnumElementCategory.RESOURCE_METHOD),
	/** Undefined Resource Method. */
	UNDEFINED_RESOURCE_METHOD(EnumElementCategory.RESOURCE_METHOD),
	
	/** PathParam field. */
	PATH_PARAM_FIELD(EnumElementCategory.RESOURCE_FIELD),
	/** QueryParam field. */
	QUERY_PARAM_FIELD(EnumElementCategory.RESOURCE_FIELD),
	/** MatrixParam field. */
	MATRIX_PARAM_FIELD(EnumElementCategory.RESOURCE_FIELD), 
	/** Undefined Resource Field. */
	UNDEFINED_RESOURCE_FIELD(EnumElementCategory.RESOURCE_FIELD);
	
	private final EnumElementCategory category;
	
	EnumElementKind(final EnumElementCategory category) {
		this.category = category;
	}
	
	public EnumElementCategory getCategory() {
		return this.category;
	}
	
}
