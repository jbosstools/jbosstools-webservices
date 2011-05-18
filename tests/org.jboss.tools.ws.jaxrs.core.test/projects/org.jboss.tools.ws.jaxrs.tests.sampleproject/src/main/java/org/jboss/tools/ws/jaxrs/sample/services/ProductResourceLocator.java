package org.jboss.tools.ws.jaxrs.sample.services;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

@Path("/products")
public class ProductResourceLocator {

	@Path("/{type}")
	public Object getProductResourceLocator(@PathParam("type") String type) {
		if ("books".equals(type)) {
			return new BookResource();
		}
		if ("games".equals(type)) {
			return new GameResource();
		}
		throw new WebApplicationException(Status.NOT_FOUND);
	}

}
