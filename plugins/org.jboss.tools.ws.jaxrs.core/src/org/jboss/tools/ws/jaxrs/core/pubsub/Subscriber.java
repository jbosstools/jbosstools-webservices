package org.jboss.tools.ws.jaxrs.core.pubsub;

import java.util.EventObject;

public interface Subscriber {

	public void inform(EventObject event);

	public String getId();

}
