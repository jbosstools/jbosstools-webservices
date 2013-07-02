package org.jboss.tools.ws.jaxrs.sample.services;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.jboss.tools.ws.jaxrs.sample.domain.Customer;

@Encoded
@Path(value=CustomerResource.URI_BASE) // leave as-is: this form is required by a test
@Consumes(MediaType.APPLICATION_XML)
@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
@SuppressWarnings("foo") // keep this for tests
public class CustomerResource {

	@PersistenceContext
	private EntityManager entityManager = null;

	public static final String URI_BASE = "/customers";
	
	@POST
	@Consumes(MediaType.APPLICATION_XML)
	public Response createCustomer(Customer customer) {
		return Response.created(null).build();
	}

	@GET
	@Path("{id}")
	public Response getCustomer(@PathParam("id") Integer id, @Context UriInfo uriInfo) {
		ResponseBuilder responseBuilder = Response.ok().entity(null);
		return responseBuilder.build();
	}

	@GET
	@Path("{id}")
	@Produces({ "text/x-vcard" })
	public Response getCustomerAsVCard(@PathParam("id") Integer id, @Context UriInfo uriInfo) {
		ResponseBuilder responseBuilder = Response.ok().entity(null);
		return responseBuilder.build();
	}

	@GET
	public List<Customer> getCustomers(@QueryParam("start") int start, @QueryParam("size") @DefaultValue("2") int size,
			@Context UriInfo uriInfo) {
		return null;
	}

	@PUT
	@Path("{id}")
	public void updateCustomer(@PathParam("id") int id, Customer update) {

	}

	@DELETE
	@Path("{id}")
	public void deleteCustomer(@PathParam("id") Integer id) {
		// keep that code to get error markers when field 'entityManager' is deleted
		Customer customer = entityManager.find(Customer.class, id);
		entityManager.remove(customer);
	}

	// DO NOT REMOVE: https://issues.jboss.org/browse/JBIDE-15084
	private Response.ResponseBuilder createViolationResponse() {
		return null;
	}
	
	/**
	 * @return the entityManager
	 */
	public EntityManager getEntityManager() {
		return entityManager;
	}
}
