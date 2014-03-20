/**
 * 
 */
package org.jboss.tools.ws.jaxrs.sample.services.interceptors;

import java.io.IOException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;

/**
 * 
 * @author xcoulon
 *
 */
@Provider
public class CustomReaderInterceptor implements ReaderInterceptor {

	@Override
	public Object aroundReadFrom(ReaderInterceptorContext arg0) throws IOException, WebApplicationException {
		// TODO Auto-generated method stub
		return null;
	}

}
