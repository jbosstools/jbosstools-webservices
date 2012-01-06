/******************************************************************************* 
 * Copyright (c) 2008 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.ws.jaxrs.core.pubsub;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;

/**
 * The Publish/Subscribe engine to broadcast notifications when changes occur in
 * the metamodel to the interested parties (ie, the UI)
 * 
 * @author xcoulon
 */
public class EventService {

	private static final EventService instance = new EventService();

	private final List<Subscriber> subscribers = new ArrayList<Subscriber>();

	/** Singleton constructor */
	private EventService() {
		super();
	}

	public static EventService getInstance() {
		return instance;
	}

	/**
	 * Notifies (only once) the subscribers that registered for the exact type
	 * of the given event, provided the accompanied filter matches.
	 * 
	 * @param event
	 */
	public void publish(EventObject event) {
		if (subscribers.size() > 0) {
			for (Subscriber subscriber : subscribers) {
				Logger.debug("Informing subscriber '{}' of {}", subscriber.getId(), event.getSource());
				subscriber.inform(event);
			}
		} else {
			Logger.debug("*** No subscriber to informing about {} ***", event.getSource());
		}
	}

	public void subscribe(Subscriber subscriber) {
		subscribers.add(subscriber);
	}

	public boolean unsubscribe(Subscriber subscriber) {
		return subscribers.remove(subscriber);
	}

	public void resetSubscribers() {
		subscribers.clear();

	}

}
