package org.jboss.tools.ws.jaxrs.core.internal.eventprocessing.engine;

import org.eclipse.jdt.core.ElementChangedEvent;

public class JavaElementChangedEventProcessor extends AbstractEventProcessor<ElementChangedEvent> {

	public JavaElementChangedEventProcessor(IEventProcessingEngine engine) {
		super(engine);
	}

	@Override
	boolean processEvent(ElementChangedEvent event) {
		// getEngine().notifyEvent(null);
		return true;
	}

}
