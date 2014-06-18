/******************************************************************************* 
 * Copyright  = c) 2012 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.ws.jaxrs.core.utils;


/**
 * @author Xavier Coulon
 *
 */
public interface JaxrsClassnames {
	
	public static final String DELETE = "javax.ws.rs.DELETE";
	
	public static final String GET = "javax.ws.rs.GET";
	
	public static final String POST = "javax.ws.rs.POST";
	
	public static final String PUT = "javax.ws.rs.PUT";
	
	public static final String HEAD = "javax.ws.rs.HEAD";
	
	public static final String OPTIONS = "javax.ws.rs.OPTIONS";
	
	public static final String HTTP_METHOD = "javax.ws.rs.HttpMethod";
	
	public static final String TARGET = "java.lang.annotation.Target";

	public static final String ELEMENT_TYPE = "java.lang.annotation.ElementType";
	
	public static final String RETENTION = "java.lang.annotation.Retention";
	
	public static final String RETENTION_POLICY = "java.lang.annotation.RetentionPolicy";
	
	public static final String APPLICATION = "javax.ws.rs.core.Application";
	
	public static final String APPLICATION_PATH = "javax.ws.rs.ApplicationPath";
	
	public static final String MESSAGE_BODY_READER = "javax.ws.rs.ext.MessageBodyReader";

	public static final String MESSAGE_BODY_WRITER = "javax.ws.rs.ext.MessageBodyWriter";
	
	public static final String EXCEPTION_MAPPER = "javax.ws.rs.ext.ExceptionMapper";

	public static final String CONTAINER_REQUEST_FILTER = "javax.ws.rs.container.ContainerRequestFilter";
	
	public static final String CONTAINER_RESPONSE_FILTER = "javax.ws.rs.container.ContainerResponseFilter";
	
	public static final String ENTITY_READER_INTERCEPTOR = "javax.ws.rs.ext.ReaderInterceptor";
	
	public static final String ENTITY_WRITER_INTERCEPTOR = "javax.ws.rs.ext.WriterInterceptor";
	
	public static final String NAME_BINDING = "javax.ws.rs.NameBinding";
	
	public static final String PRE_MATCHING = "javax.ws.rs.container.PreMatching";
	
	public static final String CONTEXT_RESOLVER = "javax.ws.rs.ext.ContextResolver";
	
	public static final String PATH = "javax.ws.rs.Path";
	
	public static final String PATH_PARAM = "javax.ws.rs.PathParam";
	
	public static final String CONSUMES = "javax.ws.rs.Consumes";
	
	public static final String PRODUCES = "javax.ws.rs.Produces";
	
	public static final String DEFAULT_VALUE = "javax.ws.rs.DefaultValue";

	public static final String COOKIE_PARAM = "javax.ws.rs.CookieParam";

	public static final String HEADER_PARAM = "javax.ws.rs.HeaderParam";

	public static final String FORM_PARAM = "javax.ws.rs.FormParam";
	
	public static final String MATRIX_PARAM = "javax.ws.rs.MatrixParam";
	
	public static final String QUERY_PARAM = "javax.ws.rs.QueryParam";
	
	public static final String CONTEXT = "javax.ws.rs.core.Context";
	
	public static final String HTTP_HEADERS = "javax.ws.rs.core.HttpHeaders";

	public static final String REQUEST = "javax.ws.rs.core.Request";

	public static final String RESPONSE = "javax.ws.rs.core.Response";
	
	public static final String RESPONSE_STATUS = "javax.ws.rs.core.Response.Status";
	
	public static final String URI_INFO = "javax.ws.rs.core.UriInfo";
	
	public static final String ENCODED = "javax.ws.rs.Encoded";
	
	public static final String PROVIDER = "javax.ws.rs.ext.Provider";
	
	public static final String REQUEST_SCOPED = "javax.enterprise.context.RequestScoped";

	public static final String URI_BUILDER = "javax.ws.rs.core.UriBuilder";

	public static final String PARAM_CONVERTER_PROVIDER = "javax.ws.rs.ext.ParamConverterProvider";

	public static final String BEAN_PARAM = "javax.ws.rs.BeanParam";
	
}
