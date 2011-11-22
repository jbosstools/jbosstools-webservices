package org.jboss.tools.ws.jaxrs.core.internal.eventprocessing.engine;

public abstract class AbstractEventProcessor<T> implements IEventProcessor {

	private final IEventProcessingEngine engine;

	public AbstractEventProcessor(IEventProcessingEngine engine) {
		this.engine = engine;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void process(Object event) {
		processEvent((T) event);
	}

	abstract boolean processEvent(T event);

	/**
	 * @return the engine
	 */
	IEventProcessingEngine getEngine() {
		return engine;
	}

}
