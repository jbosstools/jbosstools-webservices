/**
 * 
 */
package org.jboss.tools.ws.jaxrs.sample.services.interceptors;

import java.io.IOException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import javax.ws.rs.container.PreMatching; // do not remove, used in a test.

/**
 * 
 * @author xcoulon
 *
 */
@Provider
public class CustomWriterInterceptor implements WriterInterceptor {

	@Override
	public void aroundWriteTo(WriterInterceptorContext arg0) throws IOException, WebApplicationException {
		// TODO Auto-generated method stub
		
	}


}
