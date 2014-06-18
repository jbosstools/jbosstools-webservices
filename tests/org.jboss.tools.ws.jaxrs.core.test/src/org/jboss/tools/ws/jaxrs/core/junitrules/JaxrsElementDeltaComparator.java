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

package org.jboss.tools.ws.jaxrs.core.junitrules;

import java.util.Comparator;

import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta;

/**
 * @author xcoulon
 *
 */
public class JaxrsElementDeltaComparator implements Comparator<JaxrsElementDelta> {

	@Override
	public int compare(final JaxrsElementDelta delta1, final JaxrsElementDelta delta2) {
		return delta1.getElement().getElementKind().getCategory().ordinal() - delta2.getElement().getElementKind().getCategory().ordinal();
	}

}
