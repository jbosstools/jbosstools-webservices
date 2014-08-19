/*******************************************************************************
 * Copyright (c) 2012 - 2014 Red Hat, Inc. and others.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/

package org.jboss.tools.ws.jaxrs.ui.quickfix;

import org.eclipse.osgi.util.NLS;

/**
 * @author Alexey Kazakov
 * @author Xavier Coulon
 */
public class JaxrsQuickFixMessages extends NLS {
	private static final String BUNDLE_NAME = "org.jboss.tools.ws.jaxrs.ui.quickfix.JaxrsQuickFixMessages"; //$NON-NLS-1$

	public static String ADD_TARGET_ANNOTATION_MARKER_RESOLUTION_TITLE;

	public static String ADD_RETENTION_ANNOTATION_MARKER_RESOLUTION_TITLE;

	public static String UPDATE_TARGET_ANNOTATION_VALUE_MARKER_RESOLUTION_TITLE;
	
	public static String UPDATE_RETENTION_ANNOTATION_VALUE_MARKER_RESOLUTION_TITLE;

	public static String UPDATE_HTTP_METHOD_ANNOTATION_VALUE_MARKER_RESOLUTION_TITLE;
	
	static {
		NLS.initializeMessages(BUNDLE_NAME, JaxrsQuickFixMessages.class);
	}
}