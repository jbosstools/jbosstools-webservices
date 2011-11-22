package org.jboss.tools.ws.jaxrs.core.pubsub;

import java.util.EventObject;

/**
 * A filter that accepts any event ;-)
 * 
 * @author xcoulon
 * 
 */
public class NullEventFilter implements EventFilter {

	@Override
	public boolean apply(EventObject event) {
		return true;
	}

}
