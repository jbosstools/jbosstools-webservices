package org.jboss.tools.ws.jaxrs.sample.services;

import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.core.Context;

public class CarParameterAggregator {

	@PathParam("id1") private String id1;
	
	@PathParam("id2") public void setId2(String id2) { }
		
	@QueryParam("color") @DefaultValue("color!") private String color;
	
	@QueryParam("shape") @DefaultValue("shape!") public void setShape(String shape) { }
		

}
