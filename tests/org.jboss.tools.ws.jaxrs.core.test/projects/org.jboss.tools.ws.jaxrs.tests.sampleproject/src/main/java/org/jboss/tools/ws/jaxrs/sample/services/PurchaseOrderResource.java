package org.jboss.tools.ws.jaxrs.sample.services;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.jboss.tools.ws.jaxrs.sample.domain.Book;
import org.jboss.tools.ws.jaxrs.sample.domain.Game;
import org.jboss.tools.ws.jaxrs.sample.domain.Product;
import org.jboss.tools.ws.jaxrs.sample.domain.PurchaseOrder;
import org.jboss.tools.ws.jaxrs.sample.services.dto.BookDTO;
import org.jboss.tools.ws.jaxrs.sample.services.dto.CustomerDTO;
import org.jboss.tools.ws.jaxrs.sample.services.dto.GameDTO;
import org.jboss.tools.ws.jaxrs.sample.services.dto.Link;
import org.jboss.tools.ws.jaxrs.sample.services.dto.ProductDTO;
import org.jboss.tools.ws.jaxrs.sample.services.dto.PurchaseOrderDTO;

@Path("/orders")
public class PurchaseOrderResource {


	@PersistenceContext
	private EntityManager entityManager = null;

	@GET
	@Path("/{id}")
	@Produces({ "application/vnd.bytesparadise.order+xml", MediaType.APPLICATION_XML, "application/json" })
	public PurchaseOrderDTO getOrder(@PathParam("id") Integer id, @Context UriInfo uriInfo) {
		PurchaseOrder order = entityManager.find(PurchaseOrder.class, id);
		if (order == null) {
			throw new EntityNotFoundException("Order with id " + id + " not found");
		}
		
		UriBuilder customerUriBuilder = uriInfo.getBaseUriBuilder().clone().path(CustomerResource.class).path("{id}");
		String customerUri = customerUriBuilder.build(order.getCustomer().getId()).toString();
		
		PurchaseOrderDTO purchaseOrderDTO = new PurchaseOrderDTO(order);
		CustomerDTO customerDTO = new CustomerDTO(order.getCustomer());
		customerDTO.addSelfLink(new Link("self", customerUri));
		purchaseOrderDTO.setCustomer(customerDTO);
		
		UriBuilder gameUriBuilder = uriInfo.getBaseUriBuilder().clone().path(ProductResourceLocator.class).path("games")
				.path("{id}");
		UriBuilder bookUriBuilder = uriInfo.getBaseUriBuilder().clone().path(ProductResourceLocator.class).path("books")
				.path("{id}");
		for (Product product : order.getProducts()) {
			ProductDTO productDTO = null;
			if(product instanceof Book) {
				productDTO = new BookDTO((Book)product);
				productDTO.addSelfLink(new Link("self", bookUriBuilder.build(product.getId()).toString()));
				
			}
			if(product instanceof Game) {
				productDTO = new GameDTO((Game)product);
				productDTO.addSelfLink(new Link("self", gameUriBuilder.build(product.getId()).toString()));
			}
			purchaseOrderDTO.addProduct(productDTO);
			
		}
		String orderUri = uriInfo.getAbsolutePath().toString();
		purchaseOrderDTO.addSelfLink(new Link("self", orderUri));
		
		return purchaseOrderDTO;
	}

}
