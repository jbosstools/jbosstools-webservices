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

package org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public interface IValidable {

	/**
	 * Validates the element against JAX-RS business/technical rules. 
	 * In case of errors or warning, creates appropriate markers on the underlying resource.
	 * @param progressMonitor the progress monitor
	 * @throws CoreException 
	 */
	abstract void validate(IProgressMonitor progressMonitor) throws CoreException;
	
	
}
