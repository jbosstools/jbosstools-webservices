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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.jdt.Flags;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;

public abstract class JaxrsBaseElement implements IJaxrsElement {

	/**
	 * The associated metamodel (or <code>null</code> if this element is not
	 * attached to any metamodel: the element is transient).
	 */
	private final JaxrsMetamodel metamodel;

	/** Indicates if the element has problems. */
	private int markerSeverity;
	
	/** Exist until its is removed. */
	public boolean exist = true;

	/** The working copy associated with this element, or {@code null} if it does not exist. */
	private JaxrsBaseElement workingCopy;

	/** The working copy associated with this element, or {@code null} if it does not exist. */
	private JaxrsBaseElement primaryCopy;
	
	/** A {@link boolean} flag to indicate if this instance is a working copy or not. */
	private final boolean isWorkingCopy;

	/**
	 * Public constructor.
	 * 
	 * @param metamodel
	 *            the metamodel in which this element exist, or null if this
	 *            element is transient.
	 * @param isWorkingCopy
	 *            flag to indicate if this instance is a working copy or not.
	 */
	JaxrsBaseElement(final JaxrsMetamodel metamodel, final JaxrsBaseElement primaryCopy) {
		this.metamodel = metamodel;
		this.isWorkingCopy = (primaryCopy != null);
		this.primaryCopy = primaryCopy;
		if(isWorkingCopy) {
			this.primaryCopy.setWorkingCopy(this);
		} 
	}
	
	public boolean exists() {
		return exist;
	}
	
	/**
	 * Sets the problem level to {@code 0} for this element.
	 */
	public void resetProblemLevel() {
		this.markerSeverity = 0;
	}
	
	/**
	 * Registers a marker (from the underlying {@link IResource}) and sets the
	 * problem level on this element. If this element already has a problem
	 * level, the highest value is kept.
	 * 
	 * @param marker: the marker that has been added to the underlying resource.
	 *            
	 * @throws CoreException
	 */
	public void registerMarker(final IMarker marker) {
		this.markerSeverity = Math.max(this.markerSeverity, marker.getAttribute(IMarker.SEVERITY, 0));
	}

	/**
	 * @return the problem level.
	 * @see IMarker for the severity level (value "0" meaning
	 *      "no problem, dude")
	 */
	public final int getMarkerSeverity() {
		return markerSeverity;
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
	public void remove(final Flags flags) throws CoreException {
		// mark this element as non existing so that it can be filtered from search results.
		this.exist = false;
		if (getMetamodel().containsElement(this)) {
			getMetamodel().remove(this, flags);
		} else {
			Logger.debug("Element {} was already removed from the metamodel", this.getName());
		}
	}
	
	/**
	 * @return {@code true} if this element should be removed (ie, it does not meet the requirements to be the {@link AbstractJaxrsElement} subclass it was anymore) 
	 */
	abstract boolean isMarkedForRemoval();


	/**
	 * @return true if this element is attached to a JAX-RS Metamodel, false
	 *         otherwise.
	 */
	protected boolean hasMetamodel() {
		return getMetamodel() != null;
	}

	/**
	 * @return an new working copy (ie, a clone with a reference to the same
	 *         {@link JaxrsMetamodel}, but not part of the metamodel's elements)
	 *         of {@code this} element.
	 * @see JaxrsBaseElement#getWorkingCopy()
	 * @see JaxrsBaseElement#isWorkingCopy()
	 */
	public abstract JaxrsBaseElement createWorkingCopy();
	
	/**
	 * @return an the working copy (ie, a clone with a reference to the same
	 *         {@link JaxrsMetamodel}, but not part of the metamodel's elements)
	 *         of {@code this} element, or create a new one if none already existed.
	 * @see JaxrsBaseElement#createWorkingCopy()
	 * @see JaxrsBaseElement#isWorkingCopy()
	 */
	public JaxrsBaseElement getWorkingCopy() {
		this.workingCopy = createWorkingCopy();
		return this.workingCopy;
	}
	
	private void setWorkingCopy(final JaxrsBaseElement workingCopy) {
		this.workingCopy = workingCopy;
	}
	
	public JaxrsBaseElement getPrimaryCopy() {
		return primaryCopy;
	}
	
	public void setPrimaryCopy(final JaxrsBaseElement primaryCopy) {
		this.primaryCopy = primaryCopy;
	}
	
	/**
	 * @return {@code true} if this instance is a working copy, {@code false otherwise}.
	 * 
	 * @see JaxrsBaseElement#createWorkingCopy()
	 * @see JaxrsBaseElement#getWorkingCopy()
	 */
	public boolean isWorkingCopy() {
		return isWorkingCopy;
	}
}
