/**
 * 
 */
package org.jboss.tools.ws.jaxrs.sample.services.providers;

import javax.persistence.EntityNotFoundException;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
// do not remove (used by validation tests)
import javax.ws.rs.core.Context;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletContext;


/**
 * @author xcoulon
 * 
 */
@Provider
@Produces("application/json")
@Consumes("application/json")
// do not remove (used by validation tests)
@SuppressWarnings("testing")
public class EntityNotFoundExceptionMapper implements ExceptionMapper<EntityNotFoundException> {

	
	public Response toResponse(EntityNotFoundException exception) {
		return Response.status(Status.NOT_FOUND).entity(exception.getMessage()).type(MediaType.TEXT_PLAIN)
				.build();
	}

}

