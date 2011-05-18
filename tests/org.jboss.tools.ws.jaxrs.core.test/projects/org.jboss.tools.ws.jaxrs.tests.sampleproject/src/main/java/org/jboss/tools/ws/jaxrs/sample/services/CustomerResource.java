package org.jboss.tools.ws.jaxrs.sample.services;

import java.net.URI;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.jboss.tools.ws.jaxrs.sample.domain.Address;
import org.jboss.tools.ws.jaxrs.sample.domain.Customer;
import org.jboss.tools.ws.jaxrs.sample.domain.PurchaseOrder;
import org.jboss.tools.ws.jaxrs.sample.services.dto.CustomerDTO;
import org.jboss.tools.ws.jaxrs.sample.services.dto.CustomersDTO;
import org.jboss.tools.ws.jaxrs.sample.services.dto.Link;
import org.jboss.tools.ws.jaxrs.sample.services.dto.Page;
import org.jboss.tools.ws.jaxrs.sample.services.dto.PurchaseOrderDTO;

@Path(CustomerResource.URI_BASE)
@Consumes(MediaType.APPLICATION_XML)
@Produces({ "application/vnd.bytesparadise.customer+xml", MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
public class CustomerResource {

	@PersistenceContext
	private EntityManager entityManager = null;

	public static final String URI_BASE = "/customers";
	
	@POST
	public Response createCustomer(CustomerDTO customerDTO) {
		Customer customer = customerDTO.toCustomer();
		entityManager.persist(customer);
		return Response.created(URI.create(URI_BASE + customer.getId())).build();
	}

	@GET
	@Path("{id}")
	public Response getCustomer(@PathParam("id") Integer id, @Context UriInfo uriInfo) {
		Customer customer = entityManager.find(Customer.class, id);
		if (customer == null) {
			throw new EntityNotFoundException("Customer with id " + id + " not found");
		}
		CustomerDTO customerDTO = new CustomerDTO(customer);
		UriBuilder orderUriBuilder = uriInfo.getBaseUriBuilder().clone().path(PurchaseOrderResource.class).path("{id}");
		for (PurchaseOrder order : customer.getOrders()) {
			PurchaseOrderDTO orderDTO = new PurchaseOrderDTO(order);
			orderDTO.addSelfLink(new Link("self", orderUriBuilder.build(order.getId()).toString()));
			customerDTO.addOrder(orderDTO);
		}

		customerDTO.addSelfLink(new Link("self", uriInfo.getAbsolutePath().toString()));

		ResponseBuilder responseBuilder = Response.ok().entity(customerDTO);
		return responseBuilder.build();
	}

	@GET
	@Path("{id}")
	@Produces({ "text/x-vcard" })
	public Response getCustomerAsVCard(@PathParam("id") Integer id, @Context UriInfo uriInfo) {
		Customer customer = entityManager.find(Customer.class, id);
		if (customer == null) {
			throw new EntityNotFoundException("Customer with id " + id + " not found");
		}
		CustomerDTO customerDTO = new CustomerDTO(customer);
		ResponseBuilder responseBuilder = Response.ok().entity(customerDTO);
		CacheControl cacheControl = new CacheControl();
		cacheControl.setMaxAge(300);
		responseBuilder.cacheControl(cacheControl);

		return responseBuilder.build();
	}

	@GET
	public CustomersDTO getCustomers(@QueryParam("start") int start, @QueryParam("size") @DefaultValue("2") int size,
			@Context UriInfo uriInfo) {
		UriBuilder builder = uriInfo.getAbsolutePathBuilder();
		builder.queryParam("start", "{start}");
		builder.queryParam("size", "{size}");

		Page<Customer> customersPage = new Page<Customer>(entityManager.createNamedQuery("Customer.findAll"), start,
				size);
		CustomersDTO customerDTOs = new CustomersDTO();
		for (Customer customer : customersPage.getList()) {
			CustomerDTO customerDTO = new CustomerDTO(customer);
			URI selfUri = uriInfo.getAbsolutePathBuilder().clone().path("{id}").build(customer.getId());
			Link selfLink = new Link("self", selfUri.toString());
			customerDTO.getSelfLinks().add(selfLink);
			customerDTOs.addCustomerDTO(customerDTO);
		}
		// next link
		if (customersPage.hasNextPage()) {
			int next = customersPage.getStart() + customersPage.getPageSize();
			URI nextUri = builder.clone().build(next, customersPage.getPageSize());
			Link nextLink = new Link("next", nextUri.toString(), "application/xml");
			customerDTOs.addLink(nextLink);
		}
		// previous link
		if (customersPage.hasPreviousPage()) {
			int previous = customersPage.getStart() - customersPage.getPageSize();
			if (previous < 0) {
				previous = 0;
			}
			URI previousUri = builder.clone().build(previous, customersPage.getPageSize());
			Link previousLink = new Link("previous", previousUri.toString(), "application/xml");
			customerDTOs.addLink(previousLink);
		}
		return customerDTOs;
	}

	@PUT
	@Path("{id}")
	public void updateCustomer(@PathParam("id") int id, Customer update) {
		Customer current = entityManager.find(Customer.class, id);
		if (current == null) {
			throw new EntityNotFoundException("Customer with id " + id + " not found");
		}
		current.setFirstName(update.getFirstName());
		current.setLastName(update.getLastName());
		Address address = new Address();
		current.setAddress(address);
		address.setStreet(update.getAddress().getStreet());
		address.setCity(update.getAddress().getCity());
		address.setZip(update.getAddress().getZip());
		address.setCountry(update.getAddress().getCountry());

	}

	@DELETE
	@Path("{id}")
	public void deleteCustomer(@PathParam("id") Integer id) {
		Customer customer = entityManager.find(Customer.class, id);
		entityManager.remove(customer);
	}

	/**
	 * @return the entityManager
	 */
	public EntityManager getEntityManager() {
		return entityManager;
	}
}
