package org.jboss.tools.ws.jaxrs.core.internal.eventprocessing.engine;

public interface IEventTransformer extends IEventProcessingStep {

	public Object transform(Object event);
}
