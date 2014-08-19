/******************************************************************************* 
 * Copyright (c) 2008 - 2014 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/

package org.jboss.tools.ws.jaxrs.ui.internal.validation;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsParameterAggregator;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsParameterAggregatorField;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsParameterAggregatorProperty;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;

/**
 * @author xcoulon
 *
 */
public class JaxrsParameterAggregatorValidatorDelegate extends AbstractJaxrsElementValidatorDelegate<JaxrsParameterAggregator> {

	/**
	 * Constructor
	 * @param markerManager the underlying marker manager to use
	 */
	public JaxrsParameterAggregatorValidatorDelegate(final JaxrsMetamodelValidator markerManager) {
		super(markerManager);
	}

	@Override
	void internalValidate(final JaxrsParameterAggregator parameterAggregator, final CompilationUnit ast) throws CoreException {
		Logger.debug("Validating element {}", parameterAggregator);
		for (JaxrsParameterAggregatorField parameterAggregatorField : parameterAggregator.getAllFields()) {
			new JaxrsParameterAggregatorFieldValidatorDelegate(markerManager).validate(parameterAggregatorField, ast, false);
		}
		for (JaxrsParameterAggregatorProperty parameterAggregatorProperty : parameterAggregator.getAllProperties()) {
			new JaxrsParameterAggregatorPropertyValidatorDelegate(markerManager).validate(parameterAggregatorProperty, ast, false);
		}
	}

}
