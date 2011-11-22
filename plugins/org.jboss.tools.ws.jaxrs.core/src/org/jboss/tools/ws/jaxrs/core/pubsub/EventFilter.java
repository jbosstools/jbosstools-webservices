package org.jboss.tools.ws.jaxrs.core.pubsub;

import java.util.EventObject;

public interface EventFilter {

	public boolean apply(EventObject event);

}
