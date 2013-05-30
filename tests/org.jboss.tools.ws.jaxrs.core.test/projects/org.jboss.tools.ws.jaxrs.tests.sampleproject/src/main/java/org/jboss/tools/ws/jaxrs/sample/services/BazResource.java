package org.jboss.tools.ws.jaxrs.sample.services;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.PUT;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * Resource without any template parameter on the @Path annotation at the type level.
 * 
 * @author Xavier Coulon
 * 
 */
@Path("/foo/baz")
public class BazResource {

	// missing @PathParam("param2") annotation
	@GET
	@Path("/{param2}")
	public Response getContent1() {
		return null;
	}

	// missing @PathParam("id") annotation
	// unbound @PathParam("i") annotation
	@GET
	@Path("/user/{id}/{format:(/format/[^/]+?)?}/{encoding:(/encoding/[^/]+?)?}")
	public Response getContent2(@PathParam("i") int id, @PathParam("format") String format,
			@PathParam("encoding") String encoding, @QueryParam("start") int start) {
		return null;
	}

	// invalid "{param2}" value (because of "{" and "}" chars)
	// unbound path template parameter "param2"
	@PUT
	@Path("{param2}")
	public Response update1(@Context HttpServletRequest requestContext, String bar, 
			@PathParam("{param2}") String param2) throws Exception {
		return null;
	}

	//OK
	@PUT
	@Path("{param2}")
	public Response update2(@Context HttpServletRequest requestContext, String bar, @PathParam("param2") String param2)
			throws Exception {
		return null;
	}

	// more than 1 parameter without annotation
	@FOO
	@Path("{param3}")
	public Response update3(@Context HttpServletRequest requestContext, 
			@PathParam("param3") String param2, String bar, String foo) throws Exception {
		return null;
	}

}
