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
package org.jboss.tools.ws.jaxrs.core.preferences;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.jboss.tools.common.validation.ValidationSeverityPreferences;
import org.jboss.tools.ws.jaxrs.core.JBossJaxrsCorePlugin;

/**
 * @author Alexey Kazakov
 */
public class JaxrsPreferences extends ValidationSeverityPreferences {

	public static final Set<String> SEVERITY_OPTION_NAMES = new HashSet<String>();

	private static JaxrsPreferences INSTANCE = new JaxrsPreferences();

	public static final String WARNING_GROUP_ID = "jaxrs";

	// HTTP Method group
	public static final String HTTP_METHOD_INVALID_HTTP_METHOD_ANNOTATION_VALUE = INSTANCE
			.createSeverityOption("httpMethodInvalidHttpMethodAnnotationValue"); //$NON-NLS-1$

	public static final String HTTP_METHOD_MISSING_RETENTION_ANNOTATION = INSTANCE
			.createSeverityOption("httpMethodMissingRetentionAnnotation"); //$NON-NLS-1$
	
	public static final String HTTP_METHOD_INVALID_RETENTION_ANNOTATION_VALUE = INSTANCE
			.createSeverityOption("httpMethodInvalidRetentionAnnotationValue"); //$NON-NLS-1$
	
	public static final String HTTP_METHOD_MISSING_TARGET_ANNOTATION = INSTANCE
			.createSeverityOption("httpMethodMissingTargetAnnotation"); //$NON-NLS-1$
	
	public static final String HTTP_METHOD_INVALID_TARGET_ANNOTATION_VALUE = INSTANCE
			.createSeverityOption("httpMethodInvalidTargetAnnotationValue"); //$NON-NLS-1$

	// Resource Method group
	public static final String RESOURCE_METHOD_ILLEGAL_CONTEXT_ANNOTATION = INSTANCE
			.createSeverityOption("resourceMethodIllegalContextAnnotation"); //$NON-NLS-1$

	public static final String RESOURCE_METHOD_UNBOUND_PATHPARAM_ANNOTATION_VALUE = INSTANCE
			.createSeverityOption("resourceMethodUnboundPathParameterAnnotationValue"); //$NON-NLS-1$

	public static final String RESOURCE_METHOD_UNBOUND_PATH_ANNOTATION_TEMPLATE_PARAMETER = INSTANCE
			.createSeverityOption("resourceMethodUnboundPathAnnotationTemplateParameter"); //$NON-NLS-1$

	public static final String RESOURCE_METHOD_MORE_THAN_ONE_UNANNOTATED_PARAMETER = INSTANCE
			.createSeverityOption("resourceMethodMoreThanOneUnannotatedParameter"); //$NON-NLS-1$

	public static final String RESOURCE_METHOD_INVALID_PATHPARAM_ANNOTATION_VALUE = INSTANCE
			.createSeverityOption("resourceMethodInvalidPathParamAnnotationValue"); //$NON-NLS-1$

	public static final String RESOURCE_METHOD_NO_PUBLIC_MODIFIER = INSTANCE
			.createSeverityOption("resourceMethodNoPublicModifier"); //$NON-NLS-1$

	public static final String APPLICATION_NO_OCCURRENCE_FOUND = INSTANCE
			.createSeverityOption("applicationNoOccurrenceFound"); //$NON-NLS-1$

	public static final String APPLICATION_TOO_MANY_OCCURRENCES = INSTANCE
			.createSeverityOption("applicationTooManyOccurrencesFound"); //$NON-NLS-1$

	public static final String JAVA_APPLICATION_MISSING_APPLICATION_PATH_ANNOTATION = INSTANCE
			.createSeverityOption("applicationMissingApplicationPathAnnotation"); //$NON-NLS-1$

	public static final String JAVA_APPLICATION_INVALID_TYPE_HIERARCHY = INSTANCE
			.createSeverityOption("applicationInvalidTypeHierarchy"); //$NON-NLS-1$


	/**
	 * @return the only instance of {@link JaxrsPreferences}
	 */
	public static JaxrsPreferences getInstance() {
		return INSTANCE;
	}

	private JaxrsPreferences() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.jboss.tools.common.validation.ValidationSeverityPreferences#getWarningGroupID()
	 */
	@Override
	public String getWarningGroupID() {
		return WARNING_GROUP_ID;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jboss.tools.common.preferences.SeverityPreferences#createSeverityOption(java.lang.String)
	 */
	@Override
	protected String createSeverityOption(String shortName) {
		String name = getPluginId() + ".validator.problem." + shortName; //$NON-NLS-1$
		SEVERITY_OPTION_NAMES.add(name);
		return name;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jboss.tools.common.preferences.SeverityPreferences#getPluginId()
	 */
	@Override
	protected String getPluginId() {
		return JBossJaxrsCorePlugin.PLUGIN_ID;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jboss.tools.common.preferences.SeverityPreferences#getSeverityOptionNames()
	 */
	@Override
	protected Set<String> getSeverityOptionNames() {
		return SEVERITY_OPTION_NAMES;
	}

	public static boolean shouldValidateCore(IProject project) {
		return true;
	}

	public static boolean isValidationEnabled(IProject project) {
		return INSTANCE.isEnabled(project);
	}

	public static int getMaxNumberOfProblemMarkersPerFile(IProject project) {
		return INSTANCE.getMaxNumberOfProblemMarkersPerResource(project);
	}
}