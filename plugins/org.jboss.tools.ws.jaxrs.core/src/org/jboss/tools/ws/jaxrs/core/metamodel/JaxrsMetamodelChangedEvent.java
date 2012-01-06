/**
 * 
 */
package org.jboss.tools.ws.jaxrs.core.metamodel;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsEndpointChangedEvent;

/**
 * @author Xavier Coulon
 *
 */
public class JaxrsMetamodelChangedEvent extends EventObject {

	/**
	 * generated Serial Version UID.
	 */
	private static final long serialVersionUID = -5946354315088280844L;

	/** List of underlying JarxEndpoints change events carried by this event.*/
	private final List<IJaxrsEndpointChangedEvent> endpointChangedEvents;
	
	/**
	 * Full constructor.
	 * @param events
	 */
	public JaxrsMetamodelChangedEvent(List<JaxrsEndpointChangedEvent> events) {
		super(events);
		this.endpointChangedEvents = new ArrayList<IJaxrsEndpointChangedEvent>(events);
	}

	/**
	 * @return the endpointChangedEvents
	 */
	public List<IJaxrsEndpointChangedEvent> getEndpointChangedEvents() {
		return endpointChangedEvents;
	}


}
