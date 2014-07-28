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

package org.jboss.tools.ws.jaxrs.ui.internal.validation;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsParameterAggregatorProperty;

/**
 * @author xcoulon
 *
 */
public class JaxrsParameterAggregatorPropertyValidatorDelegate extends
		JaxrsParameterAggregatorElementValidatorDelegate<JaxrsParameterAggregatorProperty> {

	/**
	 * Constructor
	 * @param markerManager the underlying marker manager to use
	 */
	public JaxrsParameterAggregatorPropertyValidatorDelegate(final IMarkerManager markerManager) {
		super(markerManager);
	}

	@Override
	void internalValidate(final JaxrsParameterAggregatorProperty element, final CompilationUnit ast) throws CoreException {
		validateNoUnboundPathAnnotationTemplateParameters(element, ast);
	}

}
