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

/**
 * @author Xavier Coulon
 *
 */
public enum EnumJaxrsElements {
	
	DELETE("javax.ws.rs.DELETE"),
	
	GET("javax.ws.rs.GET"),
	
	POST("javax.ws.rs.POST"),
	
	PUT("javax.ws.rs.PUT"),
	
	HEAD("javax.ws.rs.HEAD"),
	
	OPTIONS("javax.ws.rs.OPTIONS"),
	
	HTTP_METHOD("javax.ws.rs.HttpMethod"),
	
	APPLICATION("javax.ws.rs.core.Application"), 
	
	APPLICATION_PATH("javax.ws.rs.ApplicationPath"),
	
	PATH("javax.ws.rs.Path"),
	
	PATH_PARAM("javax.ws.rs.PathParam"),
	
	CONSUMES("javax.ws.rs.Consumes"),
	
	PRODUCES("javax.ws.rs.Produces"),
	
	DEFAULT_VALUE("javax.ws.rs.DefaultValue"),

	COOKIE_PARAM("javax.ws.rs.CookieParam"),

	HEADER_PARAM("javax.ws.rs.HeaderParam"),

	MATRIX_PARAM("javax.ws.rs.MatrixParam"),
	
	QUERY_PARAM("javax.ws.rs.QueryParam"),
	
	CONTEXT("javax.ws.rs.core.Context"),
	
	HTTP_HEADERS("javax.ws.rs.core.HttpHeaders"),

	REQUEST("javax.ws.rs.core.Request"),

	RESPONSE("javax.ws.rs.core.Response"),
	
	URI_INFO("javax.ws.rs.core.UriInfo"),
	
	ENCODED("javax.ws.rs.Encoded"),
	
	PROVIDER("javax.ws.rs.ext.Provider");
	
	public final String qualifiedName;
	
	private EnumJaxrsElements(final String qualifiedName) {
		this.qualifiedName = qualifiedName;
	}

}
