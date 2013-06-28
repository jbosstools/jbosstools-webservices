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
import org.jboss.tools.common.validation.TempMarkerManager;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsBaseElement;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;

/**
 * Abstract validator delegate with bits of generics for better readability in subclasses
 * 
 * @author Xavier Coulon
 *
 */
public abstract class AbstractJaxrsElementValidatorDelegate<T extends JaxrsBaseElement> extends AbstractValidatorDelegate<T> {
	
	public AbstractJaxrsElementValidatorDelegate(TempMarkerManager markerManager) {
		super(markerManager);
	}

	public void validate(final T element) throws CoreException {
		final int previousProblemLevel = element.getProblemLevel();
		internalValidate(element);
		final int currentProblemLevel = element.getProblemLevel();
		if(currentProblemLevel != previousProblemLevel) {
			Logger.debug("Informing metamodel that problem level changed from {} to {}", previousProblemLevel,
					currentProblemLevel);
			element.getMetamodel().notifyElementProblemLevelChanged(element);
		}
	}

	abstract void internalValidate(final T element) throws CoreException;

}
