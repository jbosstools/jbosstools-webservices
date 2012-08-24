/******************************************************************************* 
 * Copyright (c) 2009 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.validation;

import org.eclipse.osgi.util.NLS;

/**
 * @author Alexey Kazakov
 */
public class JaxrsValidationMessages {

	private static final String BUNDLE_NAME = JaxrsValidationMessages.class.getName(); //$NON-NLS-1$

	public static String SEARCHING_RESOURCES;
	public static String VALIDATING_RESOURCE;
	public static String VALIDATING_PROJECT;

	public static String HTTP_METHOD_MISSING_RETENTION_ANNOTATION;
	public static String HTTP_METHOD_INVALID_RETENTION_ANNOTATION_VALUE;
	public static String HTTP_METHOD_MISSING_TARGET_ANNOTATION;
	public static String HTTP_METHOD_INVALID_TARGET_ANNOTATION_VALUE;

	public static String RESOURCE_METHOD_ILLEGAL_CONTEXT_ANNOTATION;
	public static String RESOURCE_METHOD_UNBOUND_PATHPARAM_ANNOTATION_VALUE;
	public static String RESOURCE_METHOD_UNBOUND_PATH_ANNOTATION_TEMPLATE_PARAMETER;
	public static String RESOURCE_METHOD_MORE_THAN_ONE_UNANNOTATED_PARAMETER;
	public static String RESOURCE_METHOD_INVALID_PATHPARAM_ANNOTATION_VALUE;
	public static String RESOURCE_METHOD_NO_PUBLIC_MODIFIER;

	public static String APPLICATION_NO_OCCURRENCE_FOUND;
	public static String APPLICATION_TOO_MANY_OCCURRENCES;
	
	static {
		NLS.initializeMessages(BUNDLE_NAME, JaxrsValidationMessages.class);
	}
}