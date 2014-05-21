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
package org.jboss.tools.ws.jaxrs.ui.quickfix;

import org.eclipse.jdt.core.IType;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames;

/**
 * @author Xavier Coulon
 *
 */
public class UpdateTargetAnnotationValueMarkerResolution extends AbstractAnnotationMarkerResolution {

	/**
	 * Constructor.
	 * 
	 * @param type
	 *            the type on which the {@code @java.lang.annotation.Target}
	 *            annotation should be updated
	 * @param annotationValue
	 *            the new annotation value(s) to set
	 */
	public UpdateTargetAnnotationValueMarkerResolution(final IType type, final String annotationValue) {
		super(type, JaxrsClassnames.TARGET, annotationValue, AbstractAnnotationMarkerResolution.UPDATE, NLS.bind(
				JaxrsQuickFixMessages.UPDATE_TARGET_ANNOTATION_VALUE_MARKER_RESOLUTION_TITLE, annotationValue));
	}

	@Override
	String[] getImports() {
		return new String[] { "java.lang.annotation.ElementType" };
	}

}
