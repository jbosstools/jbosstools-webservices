package org.jboss.tools.ws.jaxrs.sample.services;
 
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.tools.ws.jaxrs.sample.domain.Book;
import org.jboss.tools.ws.jaxrs.sample.services.dto.BookDTO;

@Produces({ MediaType.APPLICATION_XML, "application/json" })
public class BookResource {

	@PersistenceContext
	private EntityManager entityManager = null;

	@GET
	@Path("/{id}")
	@Produces({ "application/vnd.bytesparadise.book+xml", "application/xml", "application/json" })
	public BookDTO getProduct(@PathParam("id") Integer id) {
		Book product = entityManager.find(Book.class, id);
		if (product == null) {
			throw new EntityNotFoundException("Book with id " + id + " not found");
		}
		return new BookDTO(product);
	}

}
