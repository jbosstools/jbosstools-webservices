package org.jboss.tools.ws.jaxrs.sample.extra;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.jboss.tools.ws.jaxrs.sample.extra.TestQualifiedException.TestException;

@Provider
public class TestQualifiedExceptionMapper implements ExceptionMapper<TestQualifiedException.TestException> {

	public Response toResponse(TestException arg0) {
		return null;
	}


	
}
