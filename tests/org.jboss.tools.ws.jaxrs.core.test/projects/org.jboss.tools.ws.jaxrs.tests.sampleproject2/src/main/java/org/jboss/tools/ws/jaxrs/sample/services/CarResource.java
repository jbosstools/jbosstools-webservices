package org.jboss.tools.ws.jaxrs.sample.services;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.BeanParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.jboss.tools.ws.jaxrs.sample.domain.Car;

@Path("/cars")
public class CarResource {

	//PlaceHolder
	
	@GET
	@Path("/{id}")
	public Response findById(@QueryParam("car") final Car car) {
		return Response.ok(car).build();
	}

	@PUT
	@Path("/{id1}-{id2}")
	public Response update(final CarParameterAggregator car) { // no @BeanParam annotation by default.
		return Response.ok(car).build();
	}

}
