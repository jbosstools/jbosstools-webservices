/******************************************************************************* 
 * Copyright (c) 2012 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.ws.jaxrs.core.jdt;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * @author Xavier Coulon
 *
 */
public enum EnumJaxrsClassname {
	
	DELETE("javax.ws.rs.DELETE", "DELETE"),
	
	GET("javax.ws.rs.GET", "GET"),
	
	POST("javax.ws.rs.POST", "POST"),
	
	PUT("javax.ws.rs.PUT", "PUT"),
	
	HEAD("javax.ws.rs.HEAD", "HEAD"),
	
	OPTIONS("javax.ws.rs.OPTIONS", "OPTIONS"),
	
	HTTP_METHOD("javax.ws.rs.HttpMethod", "HttpMethod"),
	
	TARGET(Target.class.getName(), "Target"),
	
	RETENTION(Retention.class.getName(), "Retention"),
	
	APPLICATION("javax.ws.rs.core.Application", "Application"), 
	
	APPLICATION_PATH("javax.ws.rs.ApplicationPath", "ApplicationPath"),
	
	MESSAGE_BODY_READER("javax.ws.rs.ext.MessageBodyReader"),

	MESSAGE_BODY_WRITER("javax.ws.rs.ext.MessageBodyWriter"),
	
	EXCEPTION_MAPPER("javax.ws.rs.ext.ExceptionMapper"),
	
	PATH("javax.ws.rs.Path", "Path"),
	
	PATH_PARAM("javax.ws.rs.PathParam", "PathParam"),
	
	CONSUMES("javax.ws.rs.Consumes", "Consumes"),
	
	PRODUCES("javax.ws.rs.Produces", "Produces"),
	
	DEFAULT_VALUE("javax.ws.rs.DefaultValue", "DefaultValue"),

	COOKIE_PARAM("javax.ws.rs.CookieParam", "CookieParam"),

	HEADER_PARAM("javax.ws.rs.HeaderParam", "HeaderParam"),

	MATRIX_PARAM("javax.ws.rs.MatrixParam", "MatrixParam"),
	
	QUERY_PARAM("javax.ws.rs.QueryParam", "QueryParam"),
	
	CONTEXT("javax.ws.rs.core.Context", "Context"),
	
	HTTP_HEADERS("javax.ws.rs.core.HttpHeaders"),

	REQUEST("javax.ws.rs.core.Request"),

	RESPONSE("javax.ws.rs.core.Response"),
	
	URI_INFO("javax.ws.rs.core.UriInfo"),
	
	ENCODED("javax.ws.rs.Encoded", "Encoded"),
	
	PROVIDER("javax.ws.rs.ext.Provider", "Provider");
	
	public final String qualifiedName;

	public final String simpleName;
	
	private EnumJaxrsClassname(final String qualifiedName) {
		this.qualifiedName = qualifiedName;
		this.simpleName = null;
	}

	private EnumJaxrsClassname(final String qualifiedName, final String simpleName) {
		this.qualifiedName = qualifiedName;
		this.simpleName = simpleName;
	}

}
