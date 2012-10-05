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
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsResourceMethod;

/**
 * JAX-RS Resource validator.
 * 
 * @author Xavier Coulon
 * 
 */
public class JaxrsResourceValidatorDelegate extends AbstractJaxrsElementValidatorDelegate<JaxrsResource> {

	public JaxrsResourceValidatorDelegate(TempMarkerManager markerManager, JaxrsResource element) {
		super(markerManager, element);
	}

	@Override
	public void validate() throws CoreException {
		final JaxrsResource resource = getElement();
		JaxrsMetamodelValidator.deleteJaxrsMarkers(resource);
		Logger.debug("Validating element {}", getElement());
		for(IJaxrsResourceMethod resourceMethod : resource.getAllMethods()) {
			new JaxrsResourceMethodValidatorDelegate(getMarkerManager(), (JaxrsResourceMethod) resourceMethod).validate();
		}
	}
	
	@SuppressWarnings("unused")
	private void validateConstructorParameters() {
		//TODO...
	}

}
