package org.jboss.tools.ws.jaxrs.core.internal.eventprocessing.engine;

import java.util.LinkedList;
import java.util.List;

public class EventChannel {

	private final List<IEventProcessingStep> steps = new LinkedList<IEventProcessingStep>();

	private final Class<?> eventType;

	public EventChannel(Class<?> eventType) {
		this.eventType = eventType;
	}

	/**
	 * @return the eventType
	 */
	public Class<?> getEventType() {
		return eventType;
	}

	public void addStep(final IEventProcessingStep processor) {
		this.steps.add(processor);
	}

	public void process(final Object e) {
		Object event = e;
		for (IEventProcessingStep step : steps) {
			if (step instanceof IEventFilter) {
				if (!((IEventFilter) step).filter(event)) {
					// filtering result: discard event.
					break;
				}
			} else if (step instanceof IEventTransformer) {
				event = ((IEventTransformer) step).transform(event);
			} else if (step instanceof IEventProcessor) {
				((IEventProcessor) step).process(event);
			}

		}
	}
}
