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

package org.jboss.tools.ws.jaxrs.core.metamodel;

import java.util.LinkedList;
import java.util.List;

public class Route implements Comparable<Route> {

	private final LinkedList<ResourceMethod> resourceMethods = new LinkedList<ResourceMethod>();

	private final RouteEndpoint routeEndpoint;

	/**
	 * Full constructor
	 * 
	 * @throws InvalidModelElementException
	 */
	public Route(List<ResourceMethod> resourceMethods) throws InvalidModelElementException {
		super();
		this.resourceMethods.addAll(resourceMethods);
		this.routeEndpoint = new RouteEndpoint(this);
	}

	/**
	 * @return the resourceMethods
	 */
	public LinkedList<ResourceMethod> getResourceMethods() {
		return resourceMethods;
	}

	/**
	 * @return the routeEndpoint
	 */
	public RouteEndpoint getEndpoint() {
		return routeEndpoint;
	}

	/**
	 * {inheritDoc
	 */
	@Override
	public final String toString() {
		return new StringBuffer().append(routeEndpoint.toString()).append("\n").append(this.resourceMethods.getLast())
				.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((resourceMethods == null) ? 0 : resourceMethods.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Route other = (Route) obj;
		if (resourceMethods == null) {
			if (other.resourceMethods != null)
				return false;
		} else if (!resourceMethods.equals(other.resourceMethods))
			return false;
		return true;
	}

	@Override
	public int compareTo(Route other) {
		return routeEndpoint.compareTo(other.getEndpoint());
	}

}
