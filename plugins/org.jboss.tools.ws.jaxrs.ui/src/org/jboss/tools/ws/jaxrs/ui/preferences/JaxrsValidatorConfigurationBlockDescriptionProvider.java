/******************************************************************************* 
 * Copyright (c) 2011 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.ws.jaxrs.ui.preferences;

import org.jboss.tools.common.ui.preferences.SeverityConfigurationBlock.SectionDescription;
import org.jboss.tools.ws.jaxrs.ui.JBossJaxrsUIPlugin;

/**
 * 
 * @author Alexey Kazakov & Viacheslav Kabanovich
 * @author xcoulon
 *
 */
public class JaxrsValidatorConfigurationBlockDescriptionProvider {

	private static JaxrsValidatorConfigurationBlockDescriptionProvider INSTANCE = null;

	private JaxrsValidatorConfigurationBlockDescriptionProvider() {
	}

	public static JaxrsValidatorConfigurationBlockDescriptionProvider getInstance() {
		if(INSTANCE == null) {
			JaxrsValidatorConfigurationBlockDescriptionProvider q = new JaxrsValidatorConfigurationBlockDescriptionProvider();
			INSTANCE = q;
		}
		return INSTANCE;
	}

	private SectionDescription SECTION_ACTIVATORS = new SectionDescription(
		JaxrsPreferencesMessages.JaxrsValidatorConfigurationBlock_section_applications,
		new String[][]{
			{JaxrsPreferences.APPLICATION_NO_OCCURRENCE_FOUND, JaxrsPreferencesMessages.JaxrsValidatorConfigurationBlock_pb_applicationNoOccurrenceFound_label},
			{JaxrsPreferences.APPLICATION_TOO_MANY_OCCURRENCES, JaxrsPreferencesMessages.JaxrsValidatorConfigurationBlock_pb_applicationTooManyOccurrencesFound_label},
			{JaxrsPreferences.JAVA_APPLICATION_MISSING_APPLICATION_PATH_ANNOTATION, JaxrsPreferencesMessages.JaxrsValidatorConfigurationBlock_pb_applicationMissingApplicationPathAnnotation_label},
			{JaxrsPreferences.JAVA_APPLICATION_INVALID_TYPE_HIERARCHY, JaxrsPreferencesMessages.JaxrsValidatorConfigurationBlock_pb_applicationInvalidTypeHierarchy_label}
		},
		JBossJaxrsUIPlugin.PLUGIN_ID
	);
	private SectionDescription SECTION_HTTP_METHODS = new SectionDescription(
			JaxrsPreferencesMessages.JaxrsValidatorConfigurationBlock_section_httpMethods,
			new String[][]{
					{JaxrsPreferences.HTTP_METHOD_INVALID_HTTP_METHOD_ANNOTATION_VALUE, JaxrsPreferencesMessages.JaxrsValidatorConfigurationBlock_pb_httpMethodInvalidHttpMethodAnnotationValue_label},
					{JaxrsPreferences.HTTP_METHOD_MISSING_RETENTION_ANNOTATION, JaxrsPreferencesMessages.JaxrsValidatorConfigurationBlock_pb_httpMethodMissingRetentionAnnotation_label},
					{JaxrsPreferences.HTTP_METHOD_INVALID_RETENTION_ANNOTATION_VALUE, JaxrsPreferencesMessages.JaxrsValidatorConfigurationBlock_pb_httpMethodInvalidRetentionAnnotationValue_label},
					{JaxrsPreferences.HTTP_METHOD_MISSING_TARGET_ANNOTATION, JaxrsPreferencesMessages.JaxrsValidatorConfigurationBlock_pb_httpMethodMissingTargetAnnotation_label},
					{JaxrsPreferences.HTTP_METHOD_INVALID_TARGET_ANNOTATION_VALUE, JaxrsPreferencesMessages.JaxrsValidatorConfigurationBlock_pb_httpMethodInvalidTargetAnnotationValue_label},
			},
			JBossJaxrsUIPlugin.PLUGIN_ID
			);
	private SectionDescription SECTION_RESOURCES = new SectionDescription(
			JaxrsPreferencesMessages.JaxrsValidatorConfigurationBlock_section_resources,
			new String[][]{
					{JaxrsPreferences.RESOURCE_INVALID_PATH_ANNOTATION_VALUE, JaxrsPreferencesMessages.JaxrsValidatorConfigurationBlock_pb_resourceInvalidPathAnnotationValue_label}
			},
			JBossJaxrsUIPlugin.PLUGIN_ID
			);

	private SectionDescription SECTION_RESOURCE_METHODS = new SectionDescription(
			JaxrsPreferencesMessages.JaxrsValidatorConfigurationBlock_section_resourceMethods,
			new String[][]{
					{JaxrsPreferences.RESOURCE_METHOD_NO_PUBLIC_MODIFIER, JaxrsPreferencesMessages.JaxrsValidatorConfigurationBlock_pb_resourceMethodNoPublicModifier_label},
					{JaxrsPreferences.RESOURCE_METHOD_UNBOUND_PATH_ANNOTATION_TEMPLATE_PARAMETER, JaxrsPreferencesMessages.JaxrsValidatorConfigurationBlock_pb_resourceMethodUnboundPathAnnotationTemplateParameter_label},
					{JaxrsPreferences.RESOURCE_METHOD_UNBOUND_PATHPARAM_ANNOTATION_VALUE, JaxrsPreferencesMessages.JaxrsValidatorConfigurationBlock_pb_resourceMethodUnboundPathParameterAnnotationValue_label},
					{JaxrsPreferences.RESOURCE_METHOD_INVALID_PATH_ANNOTATION_VALUE, JaxrsPreferencesMessages.JaxrsValidatorConfigurationBlock_pb_resourceMethodInvalidPathAnnotationValue_label},
					{JaxrsPreferences.RESOURCE_METHOD_INVALID_PATHPARAM_ANNOTATION_VALUE, JaxrsPreferencesMessages.JaxrsValidatorConfigurationBlock_pb_resourceMethodInvalidPathParamAnnotationValue_label},
					{JaxrsPreferences.RESOURCE_METHOD_INVALID_ANNOTATED_PARAMETER_TYPE, JaxrsPreferencesMessages.JaxrsValidatorConfigurationBlock_pb_resourceMethodInvalidAnnotatedParameterType_label},
					{JaxrsPreferences.RESOURCE_METHOD_MORE_THAN_ONE_UNANNOTATED_PARAMETER, JaxrsPreferencesMessages.JaxrsValidatorConfigurationBlock_pb_resourceMethodMoreThanOneUnannotatedParameter_label},
					{JaxrsPreferences.RESOURCE_METHOD_ILLEGAL_CONTEXT_ANNOTATION, JaxrsPreferencesMessages.JaxrsValidatorConfigurationBlock_pb_resourceMethodIllegalContextAnnotation_label}
			},
			JBossJaxrsUIPlugin.PLUGIN_ID
			);

	private SectionDescription SECTION_PROVIDERS = new SectionDescription(
			JaxrsPreferencesMessages.JaxrsValidatorConfigurationBlock_section_providers,
			new String[][]{
					{JaxrsPreferences.PROVIDER_MISSING_VALID_CONSTRUCTOR, JaxrsPreferencesMessages.JaxrsValidatorConfigurationBlock_pb_providerMissingValidConstructor_label},
					{JaxrsPreferences.PROVIDER_MISSING_ANNOTATION, JaxrsPreferencesMessages.JaxrsValidatorConfigurationBlock_pb_providerMissingAnnotation_label},
					{JaxrsPreferences.PROVIDER_MISSING_IMPLEMENTATION, JaxrsPreferencesMessages.JaxrsValidatorConfigurationBlock_pb_providerMissingImplementation_label},
					{JaxrsPreferences.PROVIDER_DUPLICATE_MESSAGE_BODY_READER, JaxrsPreferencesMessages.JaxrsValidatorConfigurationBlock_pb_providerDuplicateMessageBodyReader_label},
					{JaxrsPreferences.PROVIDER_DUPLICATE_MESSAGE_BODY_WRITER, JaxrsPreferencesMessages.JaxrsValidatorConfigurationBlock_pb_providerDuplicateMessageBodyWriter_label},
					{JaxrsPreferences.PROVIDER_DUPLICATE_EXCEPTION_MAPPER, JaxrsPreferencesMessages.JaxrsValidatorConfigurationBlock_pb_providerDuplicateExceptionMapper_label}
			},
			JBossJaxrsUIPlugin.PLUGIN_ID
			);

	private SectionDescription[] ALL_SECTIONS = new SectionDescription[]{
			SECTION_ACTIVATORS,
			SECTION_HTTP_METHODS,
			SECTION_RESOURCES,
			SECTION_RESOURCE_METHODS,
			SECTION_PROVIDERS
	};

	public SectionDescription[] getSections() {
		return ALL_SECTIONS;
	}


}
