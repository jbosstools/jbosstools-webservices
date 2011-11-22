package org.jboss.tools.ws.jaxrs.core.internal.eventprocessing.engine;

public interface IEventProcessingEngine {

	public abstract void notifyEvent(Object event);

}