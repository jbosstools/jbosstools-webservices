/******************************************************************************* 
 * Copyright (c) 2012 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.validation;

import org.eclipse.core.runtime.CoreException;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;

/**
 * Interface for JAX-RS Element validators
 * 
 * @author Xavier Coulon
 *
 */
public interface IJaxrsElementValidator<T extends IJaxrsElement> {

	/**
	 * Validate the given {@link IJaxrsElement}.
	 * 
	 * @param element the JAX-RS element to validate
	 * @throws CoreException
	 */
	public abstract void validate(final T element) throws CoreException;

}