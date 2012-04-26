package org.jboss.tools.ws.jaxrs.sample.services;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

@Path("/foo/bar/{param1}")
public class BarResource {

	@PUT
	@Path("{param2}") 
	public Response update1(@Context HttpServletRequest requestContext,
			String bar, @PathParam("{param1}") String param1, @PathParam("{param2}") String param2) throws Exception {
		return null;
	}

	@PUT
	@Path("{param2}") 
	public Response update2(@Context HttpServletRequest requestContext,
			String bar, @PathParam("{param2}") String param2) throws Exception {
		return null;
	}
	
}
