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

package org.jboss.tools.ws.jaxrs.core.metamodel.domain;

import org.eclipse.jdt.core.IField;
import org.jboss.tools.ws.jaxrs.core.jdt.SourceType;

/**
 * @author xcoulon
 *
 */
public interface IAnnotatedSourceType extends IAnnotatedElement {

	/**
	 * @return the {@link SourceType} associated with the underlying java {@link IField} of this {@link IJaxrsResourceField}.
	 */
	public SourceType getType();
	
}
