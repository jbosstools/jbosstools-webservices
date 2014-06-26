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

package org.jboss.tools.ws.jaxrs.ui.quickfix;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.SourceRange;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames;
import org.jboss.tools.ws.jaxrs.ui.JBossJaxrsUIPlugin;
import org.jboss.tools.ws.jaxrs.ui.internal.text.BasicCompletionProposal;

/**
 * Completion proposal for the {@code @java.lang.annotation.Target} annotation on JAx-RS Name Binding annotations.
 * @author xcoulon
 *
 */
public class AddNameBindingTargetValuesCompletionProposal extends BasicCompletionProposal {

	public static final String ANNOTATION_VALUE =  "{ElementType.TYPE, ElementType.METHOD}"; 
	
	/**
	 * Full constructor
	 * @param compilationUnit
	 * @param sourceRange
	 */
	public AddNameBindingTargetValuesCompletionProposal(final ICompilationUnit compilationUnit,
			final SourceRange sourceRange) {
		super(compilationUnit, "@Target(" + ANNOTATION_VALUE + ")", NLS.bind(JaxrsQuickFixMessages.UPDATE_TARGET_ANNOTATION_VALUE_MARKER_RESOLUTION_TITLE,
				ANNOTATION_VALUE), sourceRange.getOffset(), sourceRange.getLength(), JBossJaxrsUIPlugin.getDefault().getImage(
				"annotation_obj.gif"), null);
		includeImportDeclarationAddition(JaxrsClassnames.ELEMENT_TYPE);
	}
	
	

}
