/******************************************************************************* 
 * Copyright (c) 2012 - 2014 Red Hat, Inc. and others.  
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
public class AddHttpMethodTargetAnnotationMarkerResolution extends AbstractAnnotationMarkerResolution  {
	
	/**
	 * Constructor.
	 * @param type the type on which the {@code @java.lang.annotation.Target} annotation should be added 
	 */
	public AddHttpMethodTargetAnnotationMarkerResolution(final IType type){
		super(type, JaxrsClassnames.TARGET,  AddHttpMethodTargetValuesCompletionProposal.ANNOTATION_VALUE, AbstractAnnotationMarkerResolution.ADD,  NLS.bind(JaxrsQuickFixMessages.ADD_TARGET_ANNOTATION_MARKER_RESOLUTION_TITLE, type.getElementName()));
	}
	
	@Override
	String[] getImports() {
		return new String[]{"java.lang.annotation.Target", "java.lang.annotation.ElementType"};
	}

}
