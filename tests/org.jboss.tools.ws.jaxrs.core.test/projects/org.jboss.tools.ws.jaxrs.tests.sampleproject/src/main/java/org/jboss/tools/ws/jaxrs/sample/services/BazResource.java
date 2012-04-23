package org.bytesparadise.pastebin.service;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

@Path("/foo/baz")
public class BazResource {

	@GET
	@Path("/{id}")
	public Response getContent(@PathParam("id") int id) {
		return null;
	}
	
	@GET
	@Path("/user/{id}/{format:(/format/[^/]+?)?}/{encoding:(/encoding/[^/]+?)?}")
	public Response getContent(@PathParam("id") int id,
				  @PathParam("format") String format,
				  @PathParam("encoding") String encoding, 
				  @QueryParam("start") int start) {
		return null;
	}

	@GET
	@Path("/user2/{id}/{format:(/format/[^/]+?)?}/{encoding:(/encoding/[^/]+?)?}")
	public Response getContent2(@PathParam("id") int id,
				  @PathParam("format") String format,
				  @PathParam("encoding") String encoding, 
				  @QueryParam("start") int start) {
		return null;
	}

}
