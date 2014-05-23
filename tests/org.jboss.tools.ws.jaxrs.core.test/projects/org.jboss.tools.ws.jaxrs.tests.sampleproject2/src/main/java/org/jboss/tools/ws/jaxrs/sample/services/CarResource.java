package org.jboss.tools.ws.jaxrs.sample.services;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.jboss.tools.ws.jaxrs.sample.domain.Car;

@Path("/cars")
public class CarResource {

	@GET
	public Response findById(@QueryParam("car") final Car car) {
		return Response.ok(car).build();
	}

}
