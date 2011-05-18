package org.jboss.tools.ws.jaxrs.sample.extra;

import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

public interface EntityProvider<X,Y> extends MessageBodyReader<X>, MessageBodyWriter<Y> {

}
