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

package org.jboss.tools.ws.jaxrs.core.metamodel;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public interface Mergeable<T> {

	/**
	 * Merge of the current JAX-RS element with the java element given in parameter.
	 * @param element the java element that changed
	 * @param progressMonitor the progress monitor
	 * @throws InvalidModelElementException in case of underlying exception
	 * @throws CoreException in case of underlying exception
	 */
	abstract void merge(T element, IProgressMonitor progressMonitor) throws InvalidModelElementException, CoreException;

	
}
