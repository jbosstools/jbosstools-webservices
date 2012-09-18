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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;

/**
 * @author Xavier Coulon
 * The class name says it all. 
 */
public class MarkerUtils {

	public static void clearMarkers(final IResource resource) throws CoreException {
		if (resource == null) {
			return;
		}
		Logger.debug("Clearing JAX-RS markers for resource " + resource.getName());
		resource.deleteMarkers(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, true, IResource.DEPTH_ONE);
	}

}
