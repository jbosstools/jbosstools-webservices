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
package org.jboss.tools.ws.jaxrs.ui.internal.validation;

import org.eclipse.osgi.util.NLS;

/**
 * JAX-RS Validation messages.
 * 
 * @author Xavier Coulon
 */
public class JaxrsValidationMessages {

	private static final String BUNDLE_NAME = JaxrsValidationMessages.class.getName(); //$NON-NLS-1$

	public static String SEARCHING_RESOURCES;
	public static String VALIDATING_RESOURCE;
	public static String VALIDATING_PROJECT;

	public static String APPLICATION_NO_OCCURRENCE_FOUND;
	public static String APPLICATION_TOO_MANY_OCCURRENCES;
	public static String JAVA_APPLICATION_MISSING_APPLICATION_PATH_ANNOTATION;
	public static String JAVA_APPLICATION_INVALID_TYPE_HIERARCHY;

	public static String HTTP_METHOD_INVALID_HTTP_METHOD_ANNOTATION_VALUE;
	public static String HTTP_METHOD_MISSING_RETENTION_ANNOTATION;
	public static String HTTP_METHOD_INVALID_RETENTION_ANNOTATION_VALUE;
	public static String HTTP_METHOD_MISSING_TARGET_ANNOTATION;
	public static String HTTP_METHOD_INVALID_TARGET_ANNOTATION_VALUE;

	public static String NAME_BINDING_MISSING_RETENTION_ANNOTATION;
	public static String NAME_BINDING_INVALID_RETENTION_ANNOTATION_VALUE;
	public static String NAME_BINDING_MISSING_TARGET_ANNOTATION;
	public static String NAME_BINDING_INVALID_TARGET_ANNOTATION_VALUE;
	
	public static String RESOURCE_METHOD_ILLEGAL_CONTEXT_ANNOTATION;
	public static String RESOURCE_METHOD_UNBOUND_PATH_ANNOTATION_TEMPLATE_PARAMETER;
	public static String RESOURCE_METHOD_UNBOUND_PATHPARAM_ANNOTATION_VALUE;
	public static String RESOURCE_METHOD_UNBOUND_PATHPARAM_ANNOTATION_VALUE_IN_AGGREGATOR;
	public static String RESOURCE_METHOD_MORE_THAN_ONE_UNANNOTATED_PARAMETER;
	public static String RESOURCE_METHOD_INVALID_PATHPARAM_ANNOTATION_VALUE;
	public static String RESOURCE_METHOD_INVALID_ANNOTATED_PARAMETER_TYPE;
	public static String RESOURCE_METHOD_NO_PUBLIC_MODIFIER;

	public static String PROVIDER_MISSING_VALID_CONSTRUCTOR;
	public static String PROVIDER_MISSING_ANNOTATION;
	public static String PROVIDER_MISSING_IMPLEMENTATION;
	public static String PROVIDER_DUPLICATE_MESSAGE_BODY_READER;
	public static String PROVIDER_DUPLICATE_MESSAGE_BODY_WRITER;
	public static String PROVIDER_DUPLICATE_EXCEPTION_MAPPER;
	public static String PROVIDER_INVALID_PRE_MATCHING_ANNOTATION_USAGE;
	public static String PROVIDER_MISSING_BINDING;
	public static String PROVIDER_UNUSED_BINDING;

	static {
		NLS.initializeMessages(BUNDLE_NAME, JaxrsValidationMessages.class);
	}
	
	/**
	 * Private constructor for the utility class.
	 */
	private JaxrsValidationMessages() {
		super();
	}
}