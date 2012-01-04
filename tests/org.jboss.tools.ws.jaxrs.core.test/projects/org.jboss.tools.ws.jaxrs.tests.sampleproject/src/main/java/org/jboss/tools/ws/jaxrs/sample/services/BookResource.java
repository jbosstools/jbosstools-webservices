package org.jboss.tools.ws.jaxrs.sample.services;
 
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.tools.ws.jaxrs.sample.domain.Book;

@Produces({ MediaType.APPLICATION_XML, "application/json" })
public class BookResource {

	@GET
	@Path("/{id}")
	@Produces({ "application/xml", "application/json" })
	public Book getProduct(@PathParam("id") Integer id) {
		return null;
	}

	@GET
	@Path("/{id}")
	@Produces({ "image/jpeg" })
	public Object getPicture(@PathParam("id") Integer id, @MatrixParam("color") String color) {
		return null;
	}
	
	@GET
	@Produces({ "application/xml", "application/json" })
	public List<Book> getAllProducts() {
		return null;
	}
}
