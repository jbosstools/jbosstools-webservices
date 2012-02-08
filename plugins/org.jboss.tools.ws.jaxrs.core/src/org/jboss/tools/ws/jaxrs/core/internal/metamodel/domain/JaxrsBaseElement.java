package org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain;

import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.wst.validation.ValidatorMessage;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsElement;

public abstract class JaxrsBaseElement implements IJaxrsElement {

	/** The associated metamodel. */
	final JaxrsMetamodel metamodel;

	/** Indicates if the underlying java element has compiltation errors. */
	private boolean hasErrors;

	/**
	 * Full constructor.
	 * 
	 * @param metamodel
	 */
	public JaxrsBaseElement(JaxrsMetamodel metamodel) {
		this.metamodel = metamodel;
	}

	/**
	 * Sets a flag of whether the underlying java element has compilation errors or not.
	 * 
	 * @param h
	 *            : true if the java element has errors, false otherwise
	 */
	public void hasErrors(final boolean h) {
		this.hasErrors = h;
	}

	/** @return true if the java element has errors, false otherwise. */
	public final boolean hasErrors() {
		return hasErrors;
	}

	/** @return the metamodel */
	public final JaxrsMetamodel getMetamodel() {
		return metamodel;
	}

	public abstract EnumElementKind getElementKind();

	public abstract EnumKind getKind();

	public abstract IResource getResource();
	
	public abstract List<ValidatorMessage> validate() throws JavaModelException;

	public abstract String getName();
	
	
}
