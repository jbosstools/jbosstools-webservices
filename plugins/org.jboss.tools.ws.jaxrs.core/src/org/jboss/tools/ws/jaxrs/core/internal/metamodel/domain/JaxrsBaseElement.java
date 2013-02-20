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
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;

public abstract class JaxrsBaseElement implements IJaxrsElement {

	/**
	 * The associated metamodel (or <code>null</code> if this element is not
	 * attached to any metamodel: the element is transient).
	 */
	private final JaxrsMetamodel metamodel;

	/** Indicates if the element has problems. */
	private int problemLevel;
	
	/** Exist until its is removed. */
	public boolean exist = true;

	/**
	 * Public constructor.
	 * 
	 * @param metamodel
	 *            the metamodel in which this element exist, or null if this
	 *            element is transient.
	 */
	public JaxrsBaseElement(final JaxrsMetamodel metamodel) {
		this.metamodel = metamodel;
	}
	
	public boolean exists() {
		return exist;
	}
	
	/**
	 * Resets the problem level for this given element.
	 */
	public void resetProblemLevel() {
		this.problemLevel = 0;
	}

	/**
	 * Sets the problem level for this element. If this element already has a
	 * problem level, the highest value is kept.
	 * 
	 * @param problem
	 *            level: the incoming new problem level.
	 */
	public void setProblemLevel(final int problemLevel) {
		this.problemLevel = Math.max(this.problemLevel, problemLevel);
	}

	/**
	 * @return the problem level.
	 * @see IMarker for the severity level (value "0" meaning
	 *      "no problem, dude")
	 */
	public final int getProblemLevel() {
		return problemLevel;
	}

	/** @return the metamodel */
	public final JaxrsMetamodel getMetamodel() {
		return metamodel;
	}

	/**
	 * Adds this element to the metamodel if it has been defined, does silently
	 * nothing otherwise.
	 * @throws CoreException 
	 */
	protected void joinMetamodel() throws CoreException {
		if (metamodel != null) {
			metamodel.add(this);
		}
	}

	/**
	 * Removes this JAX-RS element from the Metamodel.
	 * @throws CoreException 
	 */
	public void remove() throws CoreException {
		// mark this element as non existing so that it can be filtered from search results.
		this.exist = false;
		if (getMetamodel().containsElement(this)) {
			getMetamodel().remove(this);
		} else {
			Logger.debug("Element {} was already removed from the metamodel", this.getName());
		}
	}

	/**
	 * @return true if this element is attached to a JAX-RS Metamodel, false
	 *         otherwise.
	 */
	protected boolean hasMetamodel() {
		return getMetamodel() != null;
	}

}
