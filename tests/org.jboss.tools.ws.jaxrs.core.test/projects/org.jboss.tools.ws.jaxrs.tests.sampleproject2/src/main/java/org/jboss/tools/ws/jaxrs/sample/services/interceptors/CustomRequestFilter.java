/**
 * 
 */
package org.jboss.tools.ws.jaxrs.sample.services.interceptors;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.ext.Provider;

/**
 * 
 * @author xcoulon
 *
 */
@Provider
@PreMatching
public class CustomRequestFilter implements ContainerRequestFilter {

	@Override
	public void filter(final ContainerRequestContext requestContext)
			throws IOException {
	
	}

}
