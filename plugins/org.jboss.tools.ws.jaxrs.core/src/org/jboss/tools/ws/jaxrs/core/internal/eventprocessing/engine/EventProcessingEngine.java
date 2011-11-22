package org.jboss.tools.ws.jaxrs.core.internal.eventprocessing.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public abstract class EventProcessingEngine implements IEventProcessingEngine {

	private final Map<Class<?>, EventChannel> channels = new HashMap<Class<?>, EventChannel>();

	private final PriorityQueue<EventWrapper> eventQueue = new PriorityQueue<EventWrapper>();

	public EventProcessingEngine() {
		configure();
	}

	public abstract void configure();

	/*
	 * { from(ElementChangedEvent.class).filter(new ElementChangedEventFilter())
	 * .transform(new JavaElementChangedEventTransformer()).process(new
	 * JavaElementChangedEventProcessor(this));
	 * from(IResourceChangeEvent.class); from(JavaElementChangedEvent.class);
	 * 
	 * }
	 */

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jboss.tools.ws.jaxrs.core.internal.eventprocessing.IEventProcessingEngine
	 * #process(java.lang.Object)
	 */
	@Override
	public void notifyEvent(Object event) {
		enqueue(event);
		processQueue();
	}

	protected void enqueue(Object event) {
		eventQueue.add(new EventWrapper(event));
	}

	private void processQueue() {
		EventWrapper eventWrapper = null;
		while ((eventWrapper = eventQueue.poll()) != null) {
			Object event = eventWrapper.getEvent();
			EventChannel channel = channels.get(event.getClass());
			if (channel != null) {
				channel.process(event);
			}
		}
	}

	public ChannelConfigurator from(Class<?> eventClazz) {
		EventChannel channel = new EventChannel(eventClazz);
		channels.put(channel.getEventType(), channel);
		return new ChannelConfigurator(channel);
	}

	static class ChannelConfigurator {

		private final EventChannel channel;

		ChannelConfigurator(EventChannel channel) {
			this.channel = channel;
		}

		ChannelConfigurator process(IEventProcessor processor) {
			channel.addStep(processor);
			return this;
		}

		ChannelConfigurator filter(IEventFilter filter) {
			channel.addStep(filter);
			return this;
		}

		ChannelConfigurator transform(IEventTransformer transformer) {
			channel.addStep(transformer);
			return this;
		}
	}
}
