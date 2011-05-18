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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.tools.ws.jaxrs.core.utils.JdtUtils;

/**
 * Base class for all elements in the JAX-RS Metamodel.
 * 
 * @author xcoulon
 * 
 * @param <T>
 *            the underlying Java type managed by the JAX-RS Element.
 */
public abstract class BaseElement<T extends IMember> implements Mergeable<T>, Validable {

	/** The functional type of the JAX-RS Element. */
	public enum EnumType {
		/** A root resource. */
		ROOT_RESOURCE,
		/** A subresource. */
		SUBRESOURCE,
		/** A resource method. */
		RESOURCE_METHOD,
		/** A subresource method. */
		SUBRESOURCE_METHOD,
		/** A subresource locator. */
		SUBRESOURCE_LOCATOR,
		/** A provider. */
		PROVIDER,
		/** An HTTP method. */
		HTTP_METHOD;
	}

	/**
	 * The state of the instance.
	 * 
	 * @author xcoulon
	 * 
	 */
	enum EnumState {
		/** unknown state (before initialization). */
		UNKNOWN,
		/** During initilization. */
		CREATING,
		/** After initialization. */
		CREATED;
	}

	/** The current instance state. */
	private EnumState state = EnumState.UNKNOWN;

	/** The associated metamodel. */
	private final Metamodel metamodel;

	/** The underlying java element. */
	private T javaElement = null;

	/** Indicates if the underlying java element has compiltation errors. */
	private boolean hasErrors;

	/**
	 * Full constructor.
	 * 
	 * @param model
	 *            the associated metamodel
	 * @param element
	 *            the underlying java element
	 */
	public BaseElement(final Metamodel model, final T element) {
		this.metamodel = model;
		this.javaElement = element;
	}

	/**
	 * @return the underlying java element
	 */
	public final T getJavaElement() {
		return javaElement;
	}

	/**
	 * @param javaElement the javaElement to set
	 */
	public void setJavaElement(T javaElement) {
		this.javaElement = javaElement;
	}

	/**
	 * @return the functional kind of the JAX-RS element.
	 */
	public abstract EnumType getKind();

	/**
	 * Sets a flag of whether the underlying java element has compilation errors
	 * or not.
	 * 
	 * @param h
	 *            : true if the java element has errors, false otherwise
	 */
	public void hasErrors(final boolean h) {
		this.hasErrors = h;
	}

	/**
	 * @return true if the java element has errors, false otherwise.
	 */
	public final boolean hasErrors() {
		return hasErrors;
	}

	/**
	 * Returns the CompilationUnit (AST3/DOM) of the given java element.
	 * @param element the java element
	 * @param progressMonitor the progress monitor
	 * @return the compilation unit or null
	 * @throws JavaModelException in case of underlying exception
	 */
	final CompilationUnit getCompilationUnit(final T element, final IProgressMonitor progressMonitor)
			throws JavaModelException {
		return JdtUtils.parse(element, progressMonitor);
	}

	/**
	 * @return the state
	 */
	public final EnumState getState() {
		return state;
	}

	/**
	 * @param s the state to set
	 */
	public final void setState(final EnumState s) {
		this.state = s;
	}

	/**
	 * @return the metamodel
	 */
	public final Metamodel getMetamodel() {
		return metamodel;
	}
}
