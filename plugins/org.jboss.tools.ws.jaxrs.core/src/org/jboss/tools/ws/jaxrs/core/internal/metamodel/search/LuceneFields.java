/******************************************************************************* 
 * Copyright (c) 2011 - 2014 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.search;

public interface LuceneFields {

	/** The type of indexed element.*/
	public static final String FIELD_TYPE = "type";
	public static final String FIELD_IDENTIFIER = "elementIdentifier";
	public static final String FIELD_PRODUCED_MEDIA_TYPE = "producedMediaType";
	public static final String FIELD_CONSUMED_MEDIA_TYPE = "consumedMediaType";
	public static final String FIELD_HTTP_VERB = "httpVerb";
	public static final String FIELD_BUILT_IN_HTTP_METHOD = "builtInHttpMethod";
	public static final String FIELD_APPLICATION_PATH = "applicationPath";
	public static final String FIELD_JAVA_CLASS_NAME = "javaClassName";
	public static final String FIELD_RESOURCE_PATH = "resource";
	public static final String FIELD_COMPILATION_UNIT_IDENTIFIER = "compilationUnitIdentifier";
	public static final String FIELD_ANNOTATION_NAME = "annotationName";
	public static final String FIELD_PARENT_IDENTIFIER = "parentIdentifier";
	public static final String FIELD_WEBXML_APPLICATION = "webXmlApplication";
	public static final String FIELD_WEBXML_APPLICATION_OVERRIDES_JAVA_APPLICATION = "webXmlApplicationOverridesJavaApplication";
	public static final String FIELD_JAVA_APPLICATION = "javaApplication";
	public static final String FIELD_JAVA_APPLICATION_OVERRIDEN = "javaApplicationOverriden";
	public static final String FIELD_PACKAGE_FRAGMENT_ROOT_IDENTIFIER = "packageFragmentRootIdentifier";
	public static final String FIELD_JAVA_PROJECT_IDENTIFIER = "javaProjectIdentifier";
	public static final String FIELD_JAVA_ELEMENT = "javaElement";
	public static final String FIELD_URI_PATH_TEMPLATE = "uriPathTemplate";
	public static final String FIELD_JAXRS_ELEMENT = "jaxrsElement";
	public static final String FIELD_RETURNED_TYPE_NAME = "returnedTypeName";
	// special field with is used in conjunction with an EnumProviderKind enum item
	public static final String FIELD_PROVIDER_KIND = "providerKind:";
	/** The type of problem associated with a marker.*/
	public static final String FIELD_MARKER_IDENTIFIER = "markerIdentifier";
	public static final String FIELD_JAXRS_PROBLEM_TYPE = "markerType";
}