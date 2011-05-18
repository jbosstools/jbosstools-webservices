package org.jboss.tools.ws.jaxrs.sample.services;

import static javax.ws.rs.core.MediaType.APPLICATION_XML;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.jboss.tools.ws.jaxrs.sample.domain.Game;
import org.jboss.tools.ws.jaxrs.sample.services.dto.GameDTO;

@Consumes({ APPLICATION_XML })
@Produces({ "application/vnd.bytesparadise.game+xml", "application/xml", "application/json" })
public class GameResource {


	@PersistenceContext
	private EntityManager entityManager = null;

	@GET
	@Path("/{id}")
	public GameDTO getProduct(@PathParam("id") Integer id) {
		Game product = entityManager.find(Game.class, id);
		if (product == null) {
			throw new EntityNotFoundException("Game with id " + id + " not found");
		}
		return new GameDTO(product);
	}

	@POST
	@Consumes({ "application/vnd.bytesparadise.game+xml", "application/xml", "application/json" })
	public void createProduct(GameDTO gameDTO) {
		throw new WebApplicationException(Status.FORBIDDEN);
	}

}
