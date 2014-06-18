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

	//PlaceHolder (DO NOT REMOVE)
	
	// no real usage, just for junit tests
	@QueryParam("foo")
	@DefaultValue("foo!")
	private String _foo;
	
	// no real usage, just for junit tests
	@SuppressWarnings("unused")
	@MatrixParam("bar") 
	private String _bar;
	
	@PathParam("productType") 
	private String _pType = null;

	
	@QueryParam("qux1")
	@DefaultValue("qux1!")
	public void setQux1(String qux) {
	}
	
	@MatrixParam("qux2")
	@DefaultValue("qux2!")
	public String getQux2() {
		return null;
	}
	
	@PathParam("qux3")
	public String getQux3() {
		return null;
	}
	
	@Path("/{productType}")
	public Object getProductResourceLocator() {
		if ("books".equals(_pType)) {
			return new BookResource();
		}
		if ("games".equals(_pType)) {
			return new GameResource();
		}
		throw new WebApplicationException(Status.NOT_FOUND);
	}

}
