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

package org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain;

import java.util.Comparator;

import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;

/**
 * @author xcoulon
 *
 */
public class JaxrsElementsComparator implements Comparator<IJaxrsElement> {

	@Override
	public int compare(final IJaxrsElement element1, final IJaxrsElement element2) {
		return element1.getElementKind().getCategory().ordinal() - element2.getElementKind().getCategory().ordinal();
	}

}
