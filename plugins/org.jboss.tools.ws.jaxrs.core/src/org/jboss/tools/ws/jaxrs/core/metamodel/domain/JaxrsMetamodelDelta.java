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
package org.jboss.tools.ws.jaxrs.core.metamodel.domain;

import org.eclipse.jdt.core.IJavaElementDelta;
import org.jboss.tools.ws.jaxrs.core.internal.utils.ConstantUtils;

/**
 * Delta in a given {@link IJaxrsMetamodel}.
 * @author xcoulon
 *
 */
public class JaxrsMetamodelDelta {

	private final IJaxrsMetamodel metamodel;

	private final int deltaKind;

	/**
	 * Full constructor.
	 * 
	 * @param metamodel the {@link IJaxrsMetamodel} that changed
	 * @param deltaKind the kind of change
	 * @see {@link IJavaElementDelta.ADDED}, {@link IJavaElementDelta.CHANGED}, {@link IJavaElementDelta.REMOVED}
	 */
	public JaxrsMetamodelDelta(final IJaxrsMetamodel metamodel, final int deltaKind) {
		this.metamodel = metamodel;
		this.deltaKind = deltaKind;
	}

	public IJaxrsMetamodel getMetamodel() {
		return metamodel;
	}

	/**
	 * Returns the kind of delta
	 * @return the kind of delta
	 * @see {@link IJavaElementDelta.ADDED}, {@link IJavaElementDelta.CHANGED}, {@link IJavaElementDelta.REMOVED}
	 */
	public int getKind() {
		return deltaKind;
	}

	/**
	 * {@inheritDoc} (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "JaxrsMetamodelChange: [" + ConstantUtils.getStaticFieldName(IJavaElementDelta.class, deltaKind) + "] "
				+ metamodel.toString();
	}
}
