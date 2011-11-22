package org.jboss.tools.ws.jaxrs.core.metamodel;


public interface IJaxrsEndpointChangedEvent {

	/** @return the endpoint */
	public abstract IJaxrsEndpoint getEndpoint();

	/** @return the deltaKind */
	public abstract int getDeltaKind();

}