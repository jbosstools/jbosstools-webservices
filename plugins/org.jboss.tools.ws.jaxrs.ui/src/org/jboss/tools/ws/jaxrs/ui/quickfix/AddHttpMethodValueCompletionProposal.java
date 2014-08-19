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

package org.jboss.tools.ws.jaxrs.ui.quickfix;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.SourceRange;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.ws.jaxrs.ui.JBossJaxrsUIPlugin;
import org.jboss.tools.ws.jaxrs.ui.internal.text.BasicCompletionProposal;

/**
 * Completion proposal for the {@code @java.lang.annotation.Retention} annotation
 * @author xcoulon
 *
 */
public class AddHttpMethodValueCompletionProposal extends BasicCompletionProposal {

	/**
	 * Full constructor
	 * @param compilationUnit
	 * @param annotationValue
	 * @param sourceRange
	 */
	public AddHttpMethodValueCompletionProposal(final ICompilationUnit compilationUnit, final String annotationValue,
			final SourceRange sourceRange) {
		super(compilationUnit, "@HttpMethod(" + annotationValue + ")", NLS.bind(JaxrsQuickFixMessages.UPDATE_HTTP_METHOD_ANNOTATION_VALUE_MARKER_RESOLUTION_TITLE,
				annotationValue), sourceRange.getOffset(), sourceRange.getLength(), JBossJaxrsUIPlugin.getDefault().getImage(
				"annotation_obj.gif"), null);
	}

}
