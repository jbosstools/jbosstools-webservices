package org.jboss.tools.ws.jaxrs.sample.services.providers;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.jboss.tools.ws.jaxrs.sample.services.dto.CustomerDTO;

@Provider
@Produces("text/x-vcard")
public class CustomerVCardMessageBodyWriter implements
		MessageBodyWriter<CustomerDTO> {

	public long getSize(CustomerDTO t, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return -1;
	}

	public boolean isWriteable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return true;
	}

	public void writeTo(CustomerDTO customerDTO, Class<?> type,
			Type genericType, Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders,
			OutputStream entityStream) throws IOException,
			WebApplicationException {
		OutputStreamWriter writer = new OutputStreamWriter(entityStream,
				"UTF-8");
		writer.write("BEGIN:VCARD\n");
		writer.write("VERSION:2.1\n");
		writer.write("N:" + customerDTO.getFirstName() + " "
				+ customerDTO.getLastName() + "\n");
		writer.write("END:VCARD\n");
		writer.close();
	}

}
