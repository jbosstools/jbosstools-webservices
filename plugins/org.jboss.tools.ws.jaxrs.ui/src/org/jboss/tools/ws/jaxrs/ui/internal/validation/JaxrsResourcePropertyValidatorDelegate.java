/******************************************************************************* 
 * Copyright (c) 2012 - 2014 Red Hat, Inc. and others.  
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
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceProperty;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;

/**
 * JAX-RS Resource Method validator.
 * 
 * @author Xavier Coulon
 * 
 */
public class JaxrsResourcePropertyValidatorDelegate extends JaxrsResourceElementValidatorDelegate<JaxrsResourceProperty> {

	/**
	 * Constructor
	 * @param markerManager the underlying marker manager to use
	 */
	public JaxrsResourcePropertyValidatorDelegate(final IMarkerManager markerManager) {
		super(markerManager);
	}

	/**
	 * @throws CoreException
	 * @see org.jboss.tools.ws.jaxrs.ui.internal.validation.AbstractJaxrsElementValidatorDelegate#internalValidate(Object)
	 */
	@Override
	void internalValidate(final JaxrsResourceProperty resourceProperty, final CompilationUnit ast) throws CoreException {
		Logger.debug("Validating element {}", resourceProperty);
		// markers were already removed at the Resource level, they should *not*
		// be removed again here (because another resource method
		// of the same parent resource may already have been validated and have
		// markers created.
		validateParameterType(resourceProperty);
		validateNotUnboundPathParamAnnotationValue(resourceProperty, ast);
	}

}
