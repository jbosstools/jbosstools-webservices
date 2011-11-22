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

@Deprecated
public class InvalidModelElementException extends Exception {

	/** serialVersionUID */
	private static final long serialVersionUID = 6333830911573630295L;

	public InvalidModelElementException(final String message) {
		super(message);
	}
}
