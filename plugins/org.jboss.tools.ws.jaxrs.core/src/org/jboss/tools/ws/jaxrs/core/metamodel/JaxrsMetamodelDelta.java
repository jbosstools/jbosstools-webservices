/**
 * 
 */
package org.jboss.tools.ws.jaxrs.core.metamodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementDelta;

/**
 * @author Xavier Coulon
 * 
 */
public class JaxrsMetamodelDelta {

	/** The JAX-RS Metamodel that changed. */
	private final IJaxrsMetamodel metamodel;

	/** The kind of change at the JAX-RS Metamodel level. */
	private final int deltaKind;

	/** List of underlying JarxEndpoints change events carried by this event. */
	private final List<JaxrsEndpointDelta> affectedEndpointd;

	private final List<JaxrsElementDelta> affectedElements;

	/**
	 * Full constructor.
	 * 
	 */
	public JaxrsMetamodelDelta(IJaxrsMetamodel metamodel, int deltaKind) {
		this.metamodel = metamodel;
		this.deltaKind = deltaKind;
		this.affectedElements = new ArrayList<JaxrsElementDelta>();
		this.affectedEndpointd = new ArrayList<JaxrsEndpointDelta>();
	}

	/**
	 * Full constructor.
	 * 
	 * @param affectedEndpointd
	 */
	public JaxrsMetamodelDelta(IJaxrsMetamodel metamodel, int deltaKind, List<JaxrsElementDelta> affectedElements) {
		this.metamodel = metamodel;
		this.deltaKind = deltaKind;
		this.affectedElements = affectedElements;
		this.affectedEndpointd = new ArrayList<JaxrsEndpointDelta>();
	}

	/**
	 * @return the metamodel
	 */
	public final IJaxrsMetamodel getMetamodel() {
		return metamodel;
	}

	/**
	 * @return the deltaKind
	 */
	public final int getDeltaKind() {
		return deltaKind;
	}

	/**
	 * @return the endpointDeltas
	 */
	public final List<JaxrsEndpointDelta> getAffectedEndpoints() {
		return Collections.unmodifiableList(affectedEndpointd);
	}

	/**
	 * @return the elementChangedEvents
	 */
	public final List<JaxrsElementDelta> getAffectedElements() {
		// this collection can be modified by processors, so don't return an unmodifiable List !
		return affectedElements;
	}

	public void addAffectedEndpoint(List<JaxrsEndpointDelta> affectedEndpoints) {
		if (affectedEndpoints != null) {
			this.affectedEndpointd.addAll(affectedEndpoints);
		}

	}

	/**
	 * Adds an affected JAX-RS element.
	 * 
	 * @param affectedElement
	 */
	public void add(JaxrsElementDelta affectedElement) {
		if (affectedElement != null) {
			this.affectedElements.add(affectedElement);
		}
	}

	public void addAll(List<JaxrsElementDelta> affectedElements) {
		if (affectedElements != null) {
			this.affectedElements.addAll(affectedElements);
		}
	}

}
