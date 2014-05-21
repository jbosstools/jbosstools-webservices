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
 * Completion proposal for the {@code @java.lang.annotation.Target} annotation
 * @author xcoulon
 *
 */
public class AddTargetValuesCompletionProposal extends BasicCompletionProposal {

	/**
	 * Full constructor
	 * @param compilationUnit
	 * @param annotationValues
	 * @param sourceRange
	 */
	public AddTargetValuesCompletionProposal(final ICompilationUnit compilationUnit, final String annotationValues,
			final SourceRange sourceRange) {
		super("@Target(" + annotationValues + ")", NLS.bind(JaxrsQuickFixMessages.UPDATE_TARGET_ANNOTATION_VALUE_MARKER_RESOLUTION_TITLE,
				annotationValues), sourceRange.getOffset(), sourceRange.getLength(), JBossJaxrsUIPlugin.getDefault().getImage(
				"annotation_obj.gif"), null);
		includeImportDeclarationAddition(compilationUnit, JaxrsClassnames.ELEMENT_TYPE);
	}
	
	

}
