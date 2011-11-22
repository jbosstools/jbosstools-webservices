package org.jboss.tools.ws.jaxrs.core.metamodel;

/** The functional type of the JAX-RS ElementKind. */
public enum EnumKind {
	/** UNDEFINED KIND (when no annotation nor any other java element can define
	 * precisely what the element is. */
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
