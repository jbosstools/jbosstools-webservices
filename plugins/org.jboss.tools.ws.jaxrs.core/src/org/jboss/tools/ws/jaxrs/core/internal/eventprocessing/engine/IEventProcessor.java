package org.jboss.tools.ws.jaxrs.core.internal.eventprocessing.engine;

public interface IEventProcessor extends IEventProcessingStep {

	public void process(Object event);

}
