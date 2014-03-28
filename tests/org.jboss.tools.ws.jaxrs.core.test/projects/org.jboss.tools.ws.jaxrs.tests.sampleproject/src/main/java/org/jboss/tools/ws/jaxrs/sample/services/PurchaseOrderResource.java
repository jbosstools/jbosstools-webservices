package org.jboss.tools.ws.jaxrs.sample.services;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.MatrixParam; // DO NOT REMOVE

import org.jboss.tools.ws.jaxrs.sample.domain.PurchaseOrder;

@Path("/orders")
public class PurchaseOrderResource {


	@GET
	@Path("/{id}")
	@Produces({ MediaType.APPLICATION_XML, "application/json" })
	public PurchaseOrder getOrder(@PathParam("id") Integer id, @Context UriInfo uriInfo) {
		return null;
	}
	
	//PLACEHOLDER

}
