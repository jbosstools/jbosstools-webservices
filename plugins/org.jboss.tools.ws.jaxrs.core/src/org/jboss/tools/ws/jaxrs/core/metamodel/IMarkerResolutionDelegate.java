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
package org.jboss.tools.ws.jaxrs.core.metamodel;

import org.eclipse.core.runtime.CoreException;


/**
 * @author Xavier Coulon
 *
 */
public interface IMarkerResolutionDelegate {
	
	public static final String MARKER_RESOLUTION = "MARKER_RESOLUTION";

	public abstract void applyQuickFix() throws CoreException;

}
