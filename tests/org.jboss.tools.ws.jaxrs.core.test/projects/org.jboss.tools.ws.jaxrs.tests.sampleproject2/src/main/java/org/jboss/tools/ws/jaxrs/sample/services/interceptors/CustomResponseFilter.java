/**
 * 
 */
package org.jboss.tools.ws.jaxrs.sample.services.interceptors;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

/**
 * @author xcoulon
 * 
 */
@Provider
public class CustomResponseFilter implements ContainerResponseFilter {

	@Override
	public void filter(final ContainerRequestContext requestContext,
			final ContainerResponseContext responseContext) throws IOException {
	}

}
