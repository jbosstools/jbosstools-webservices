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
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;

/**
 * Abstract validator delegate. 
 * 
 * @author Xavier Coulon
 *
 */
public abstract class AbstractJaxrsElementValidatorDelegate<T extends IJaxrsElement> implements IJaxrsElementValidator<T> {
	
	/**
	 * Validates the given {@link IJaxrsElement}.
	 * @param element the JAX-RS element to validate
	 * @throws CoreException
	 */
	public void validate(final T element) throws CoreException {
		final int previousProblemLevel = element.getProblemLevel();
		internalValidate(element);
		final int currentProblemLevel = element.getProblemLevel();
		if(currentProblemLevel != previousProblemLevel) {
			Logger.debug("Informing metamodel that problem level changed from {} to {}", previousProblemLevel,
					currentProblemLevel);
			((JaxrsMetamodel)element.getMetamodel()).notifyElementProblemLevelChanged(element);
		}
	}

	abstract void internalValidate(final T element) throws CoreException;


}
