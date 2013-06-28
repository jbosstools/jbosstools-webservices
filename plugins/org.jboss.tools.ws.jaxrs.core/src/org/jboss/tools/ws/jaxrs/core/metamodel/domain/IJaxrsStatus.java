package org.jboss.tools.ws.jaxrs.core.metamodel.domain;

import org.eclipse.core.resources.IMarker;

public interface IJaxrsStatus {

	/**
	 * @return the highest level problem this resource method has.
	 * @see {@link IMarker} for severity levels and values.
	 */
	public abstract int getProblemLevel();

}