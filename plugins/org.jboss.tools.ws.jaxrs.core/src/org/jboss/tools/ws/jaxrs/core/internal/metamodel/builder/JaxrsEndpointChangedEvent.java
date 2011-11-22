package org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder;

import java.util.EventObject;

import org.eclipse.jdt.core.IJavaElementDelta;
import org.jboss.tools.ws.jaxrs.core.internal.utils.ConstantUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsEndpointChangedEvent;

public class JaxrsEndpointChangedEvent extends EventObject implements IJaxrsEndpointChangedEvent {

	/** serialVersionUID */
	private static final long serialVersionUID = -1329818584745613481L;

	private final IJaxrsEndpoint endpoint;

	private final int deltaKind;

	/** Full constructor.
	 * 
	 * @param element
	 * @param deltaKind */
	public JaxrsEndpointChangedEvent(IJaxrsEndpoint endpoint, int deltaKind) {
		super(endpoint);
		this.endpoint = endpoint;
		this.deltaKind = deltaKind;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.
	 * IJaxrsEndpointChangedEvent#getEndpoint()
	 */
	@Override
	public IJaxrsEndpoint getEndpoint() {
		return endpoint;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.
	 * IJaxrsEndpointChangedEvent#getDeltaKind()
	 */
	@Override
	public int getDeltaKind() {
		return deltaKind;
	}

	/** {@inheritDoc} (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString() */
	@Override
	public String toString() {
		return "JaxrsEndpointChange: [" + ConstantUtils.getStaticFieldName(IJavaElementDelta.class, deltaKind) + "] "
				+ endpoint.toString();
	}
}
