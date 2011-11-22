package org.jboss.tools.ws.jaxrs.core.internal.eventprocessing.engine;

public interface IEventFilter extends IEventProcessingStep {

	public boolean filter(Object event);

}
