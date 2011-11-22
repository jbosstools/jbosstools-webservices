package org.jboss.tools.ws.jaxrs.core.internal.eventprocessing.engine;

import org.eclipse.jdt.core.ElementChangedEvent;

class EventWrapper implements Comparable<EventWrapper> {

	private final Object event;

	private final int priority;

	public EventWrapper(Object event) {
		this.event = event;
		this.priority = EventPriorityFactory.computePriority(this.event);
	}

	Object getEvent() {
		return event;
	}

	@Override
	public int compareTo(EventWrapper other) {
		return this.priority - other.priority;
	}

	static class EventPriorityFactory {

		static int computePriority(Object event) {
			if (event instanceof ElementChangedEvent) {
				return 10;
			}

			return 0;
		}
	}
}