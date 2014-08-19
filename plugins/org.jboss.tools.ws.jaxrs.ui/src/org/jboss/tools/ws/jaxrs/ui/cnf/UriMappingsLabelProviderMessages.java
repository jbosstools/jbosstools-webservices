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

package org.jboss.tools.ws.jaxrs.ui.cnf;

import org.eclipse.osgi.util.NLS;

/** @author xcoulon */
public class UriMappingsLabelProviderMessages extends NLS {

	private static final String BUNDLE_NAME = UriMappingsLabelProviderMessages.class.getName();

	static {
		NLS.initializeMessages(BUNDLE_NAME, UriMappingsLabelProviderMessages.class);
	}

	private UriMappingsLabelProviderMessages() {
		// Do not instantiate
	}

	public static String JAXRS_WEB_SERVICES;
	public static String LOADING_CONTENT;
	public static String CONSUMED_MEDIATYPES;
	public static String PRODUCED_MEDIATYPES;
}