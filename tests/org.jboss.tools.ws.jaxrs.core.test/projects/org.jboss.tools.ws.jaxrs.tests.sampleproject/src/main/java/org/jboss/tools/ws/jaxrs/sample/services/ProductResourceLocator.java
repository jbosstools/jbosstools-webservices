package org.jboss.tools.ws.jaxrs.sample.services;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

@Path("/products")
public class ProductResourceLocator {

	// no real usage, just for junit tests
	@QueryParam("foo")
	@DefaultValue("foo!")
	private String foo;
	
	// no real usage, just for junit tests
	@SuppressWarnings("unused")
	@MatrixParam("bar") 
	private String bar;
	
	@PathParam("productType") 
	private String productType = null;
	
	@Path("/{productType}")
	public Object getProductResourceLocator() {
		if ("books".equals(productType)) {
			return new BookResource();
		}
		if ("games".equals(productType)) {
			return new GameResource();
		}
		throw new WebApplicationException(Status.NOT_FOUND);
	}

}
