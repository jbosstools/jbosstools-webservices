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

/**
 * @author Xavier Coulon
 *
 */
public abstract class AbstractJaxrsElementValidatorDelegate<T extends JaxrsBaseElement> {
	
	private final T element;
	
	private final TempMarkerManager markerManager;
	
	public AbstractJaxrsElementValidatorDelegate(TempMarkerManager markerManager, T element) {
		this.markerManager = markerManager;
		this.element = element;
	}
	
	public abstract void validate() throws CoreException;

	public T getElement() {
		return element;
	}

	/**
	 * @return the validator
	 */
	public TempMarkerManager getMarkerManager() {
		return markerManager;
	}

}
