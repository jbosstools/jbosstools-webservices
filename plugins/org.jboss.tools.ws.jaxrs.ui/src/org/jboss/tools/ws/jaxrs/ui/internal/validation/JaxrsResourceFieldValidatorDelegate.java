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
package org.jboss.tools.ws.jaxrs.ui.internal.validation;

import org.eclipse.core.runtime.CoreException;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceField;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;

/**
 * JAX-RS Resource Method validator.
 * 
 * @author Xavier Coulon
 * 
 */
public class JaxrsResourceFieldValidatorDelegate extends JaxrsResourceElementValidatorDelegate<JaxrsResourceField> {

	/**
	 * Constructor
	 * @param markerManager the underlying marker manager to use
	 */
	public JaxrsResourceFieldValidatorDelegate(final IMarkerManager markerManager) {
		super(markerManager);
	}

	/**
	 * @throws CoreException
	 * @see org.jboss.tools.ws.jaxrs.ui.internal.validation.AbstractJaxrsElementValidatorDelegate#internalValidate(Object)
	 */
	@Override
	void internalValidate(final JaxrsResourceField resourceField) throws CoreException {
		Logger.debug("Validating element {}", resourceField);
		// markers were already removed at the Resource level, they should *not*
		// be removed again here (because another resource method
		// of the same parent resource may already have been validated and have
		// markers created.
		validateParameterType(resourceField);
		validateNotUnboundPathParamAnnotationValue(resourceField);
	}

	

}
