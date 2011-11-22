package org.jboss.tools.ws.jaxrs.core.pubsub;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.EventObject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PubSubServiceTestCase {

	public class MockSubscriber implements Subscriber {
		@Override
		public void inform(EventObject event) {
		}

		@Override
		public String getId() {
			return "MockSubscriber";
		}
	}

	private final EventService eventService = EventService.getInstance();

	private final Subscriber subscriber = spy(new MockSubscriber());

	@Before
	public void setup() {
		eventService.resetSubscribers();
	}

	@After
	public void tearDown() {

	}

	@Test
	public void shouldReceiveEvent() {
		eventService.subscribe(subscriber);
		eventService.publish(new EventObject(new Object()));
		verify(subscriber).inform(any(EventObject.class));
	}

	@Test
	public void shoudUnsubscribe() {
		eventService.subscribe(subscriber);
		assertThat(eventService.unsubscribe(subscriber), equalTo(true));
	}

}
