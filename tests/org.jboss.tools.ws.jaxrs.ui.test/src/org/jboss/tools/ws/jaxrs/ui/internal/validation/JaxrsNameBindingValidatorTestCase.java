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
package org.jboss.tools.ws.jaxrs.ui.internal.validation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.equalTo;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils.createAnnotation;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils.getAnnotation;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils.replaceFirstOccurrenceOfCode;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.RETENTION;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.TARGET;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.deleteJaxrsMarkers;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.findJaxrsMarkers;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.hasPreferenceKey;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.toSet;
import static org.jboss.tools.ws.jaxrs.ui.preferences.JaxrsPreferences.NAME_BINDING_INVALID_RETENTION_ANNOTATION_VALUE;
import static org.jboss.tools.ws.jaxrs.ui.preferences.JaxrsPreferences.NAME_BINDING_INVALID_TARGET_ANNOTATION_VALUE;
import static org.jboss.tools.ws.jaxrs.ui.preferences.JaxrsPreferences.NAME_BINDING_MISSING_RETENTION_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.ui.preferences.JaxrsPreferences.NAME_BINDING_MISSING_TARGET_ANNOTATION;
import static org.junit.Assert.assertThat;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jdt.core.IType;
import org.eclipse.wst.validation.ReporterHelper;
import org.eclipse.wst.validation.internal.core.ValidationException;
import org.eclipse.wst.validation.internal.provisional.core.IReporter;
import org.jboss.tools.common.validation.ContextValidationHelper;
import org.jboss.tools.common.validation.IProjectValidationContext;
import org.jboss.tools.common.validation.ValidatorManager;
import org.jboss.tools.common.validation.internal.ProjectValidationContext;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.AbstractJaxrsBaseElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsJavaApplication;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsNameBinding;
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsApplication;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.utils.Annotation;
import org.jboss.tools.ws.jaxrs.ui.JBossJaxrsUIPlugin;
import org.jboss.tools.ws.jaxrs.ui.preferences.JaxrsPreferences;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Xi
 * 
 */
@SuppressWarnings("restriction")
public class JaxrsNameBindingValidatorTestCase {

	private final IReporter reporter = new ReporterHelper(new NullProgressMonitor());
	private final ContextValidationHelper validationHelper = new ContextValidationHelper();
	private final IProjectValidationContext context = new ProjectValidationContext();
	private final ValidatorManager validatorManager = new ValidatorManager();

	@ClassRule
	public static WorkspaceSetupRule workspaceSetupRule = new WorkspaceSetupRule("org.jboss.tools.ws.jaxrs.tests.sampleproject2");
	
	@Rule
	public JaxrsMetamodelMonitor metamodelMonitor = new JaxrsMetamodelMonitor("org.jboss.tools.ws.jaxrs.tests.sampleproject2", true);
	
	private JaxrsMetamodel metamodel = null;

	private IProject project = null;

	@Before
	public void setup() throws CoreException {
		metamodel = metamodelMonitor.getMetamodel();
		project = metamodel.getProject();
		// remove all applications here 
		for (IJaxrsApplication application : metamodel.findAllApplications()) {
			if (application.isJavaApplication()) {
				((JaxrsJavaApplication) application).remove();
			}
		}
	}
	
	@After
	public void resetProblemLevelPreferences() {
		final IEclipsePreferences defaultPreferences = ((IScopeContext)DefaultScope.INSTANCE).getNode(JBossJaxrsUIPlugin.PLUGIN_ID);
		defaultPreferences.put(JaxrsPreferences.NAME_BINDING_INVALID_RETENTION_ANNOTATION_VALUE, JaxrsPreferences.ERROR);
	}

	@Test
	public void shouldValidateNameBinding() throws CoreException, ValidationException {
		// preconditions
		final IType customNameBindingType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding");
		final AbstractJaxrsBaseElement customNameBinding = (AbstractJaxrsBaseElement) metamodel.findElement(customNameBindingType);
		assertThat(findJaxrsMarkers(customNameBinding).length, equalTo(0));
		deleteJaxrsMarkers(customNameBinding);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(customNameBinding.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		assertThat(findJaxrsMarkers(customNameBinding).length, equalTo(0));
		for(IJaxrsEndpoint endpoint : metamodel.findEndpoints(customNameBinding)) {
			assertThat(endpoint.getProblemLevel(), equalTo(0));
		}
		// no problem level change on the metamodel.
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(1));
	}

	@Test
	public void shouldReportProblemWhenNameBindingTypeMissesTargetAnnotation() throws CoreException, ValidationException {
		// preconditions
		final IType customNameBindingType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding");
		final JaxrsNameBinding customNameBinding = (JaxrsNameBinding) metamodel.findElement(customNameBindingType);
		final Annotation targetAnnotation = getAnnotation(customNameBindingType, TARGET);
		customNameBinding.removeAnnotation(targetAnnotation.getJavaAnnotation());
		replaceFirstOccurrenceOfCode(customNameBindingType.getCompilationUnit(), "@Target({ ElementType.TYPE, ElementType.METHOD })", "", true);
		deleteJaxrsMarkers(customNameBinding);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(customNameBinding.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(customNameBinding);
		assertThat(markers.length, equalTo(1));
		assertThat(markers, hasPreferenceKey(NAME_BINDING_MISSING_TARGET_ANNOTATION));
		for(IJaxrsEndpoint endpoint : metamodel.findEndpoints(customNameBinding)) {
			assertThat(endpoint.getProblemLevel(), not(equalTo(0)));
		}
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(true));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(1));
	}

	@Test
	public void shouldReportProblemWhenNameBindingTypeTargetAnnotationHasNullValue() throws CoreException,
			ValidationException {
		// preconditions
		final IType customNameBindingType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding");
		final JaxrsNameBinding customNameBinding = (JaxrsNameBinding) metamodel.findElement(customNameBindingType);
		final Annotation targetAnnotation = customNameBinding.getAnnotation(TARGET);
		customNameBinding.addOrUpdateAnnotation(createAnnotation(targetAnnotation, (String) null));
		replaceFirstOccurrenceOfCode(customNameBindingType.getCompilationUnit(), "@Target({ ElementType.TYPE, ElementType.METHOD })", "@Target", true);
		deleteJaxrsMarkers(customNameBinding);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(customNameBinding.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(customNameBinding);
		assertThat(markers.length, equalTo(1));
		assertThat(markers, hasPreferenceKey(NAME_BINDING_INVALID_TARGET_ANNOTATION_VALUE));
		for(IJaxrsEndpoint endpoint : metamodel.findEndpoints(customNameBinding)) {
			assertThat(endpoint.getProblemLevel(), not(equalTo(0)));
		}
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(true));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(1));
	}

	@Test
	public void shouldReportProblemWhenNameBindingTypeTargetAnnotationHasWrongValue() throws CoreException,
			ValidationException {
		// preconditions
		final IType customNameBindingType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding");
		final JaxrsNameBinding customNameBinding = (JaxrsNameBinding) metamodel.findElement(customNameBindingType);
		final Annotation targetAnnotation = customNameBinding.getAnnotation(TARGET);
		customNameBinding.addOrUpdateAnnotation(createAnnotation(targetAnnotation, "FOO"));
		replaceFirstOccurrenceOfCode(customNameBindingType.getCompilationUnit(), "@Target({ ElementType.TYPE, ElementType.METHOD })", "@Target(value=ElementType.FIELD)", true);
		deleteJaxrsMarkers(customNameBinding);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(customNameBinding.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(customNameBinding);
		assertThat(markers.length, equalTo(1));
		assertThat(markers, hasPreferenceKey(NAME_BINDING_INVALID_TARGET_ANNOTATION_VALUE));
		for(IJaxrsEndpoint endpoint : metamodel.findEndpoints(customNameBinding)) {
			assertThat(endpoint.getProblemLevel(), not(equalTo(0)));
		}
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(true));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(1));
	}

	@Test
	public void shouldReportProblemWhenNameBindingTypeMissesRetentionAnnotation() throws CoreException, ValidationException {
		// preconditions
		final IType customNameBindingType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding");
		final JaxrsNameBinding customNameBinding = (JaxrsNameBinding) metamodel.findElement(customNameBindingType);
		final Annotation targetAnnotation = getAnnotation(customNameBindingType, RETENTION);
		customNameBinding.removeAnnotation(targetAnnotation.getJavaAnnotation());
		replaceFirstOccurrenceOfCode(customNameBindingType.getCompilationUnit(), "@Retention(RetentionPolicy.RUNTIME)", "", true);
		deleteJaxrsMarkers(customNameBinding);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(customNameBinding.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(customNameBinding);
		assertThat(markers.length, equalTo(1));
		assertThat(markers, hasPreferenceKey(NAME_BINDING_MISSING_RETENTION_ANNOTATION));
		for(IJaxrsEndpoint endpoint : metamodel.findEndpoints(customNameBinding)) {
			assertThat(endpoint.getProblemLevel(), not(equalTo(0)));
		}
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(true));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(1));
	}
	
	@Test
	public void shouldReportProblemWhenNameBindingTypeRetentionAnnotationHasNullValue() throws CoreException,
	ValidationException {
		// preconditions
		final IType customNameBindingType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding");
		final JaxrsNameBinding customNameBinding = (JaxrsNameBinding) metamodel.findElement(customNameBindingType);
		final Annotation retentionAnnotation = customNameBinding.getAnnotation(RETENTION);
		customNameBinding.addOrUpdateAnnotation(createAnnotation(retentionAnnotation, (String)null));
		replaceFirstOccurrenceOfCode(customNameBindingType.getCompilationUnit(), "@Retention(RetentionPolicy.RUNTIME)", "@Retention", true);
		deleteJaxrsMarkers(customNameBinding);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(customNameBinding.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(customNameBinding);
		assertThat(markers.length, equalTo(1));
		assertThat(markers, hasPreferenceKey(NAME_BINDING_INVALID_RETENTION_ANNOTATION_VALUE));
		for(IJaxrsEndpoint endpoint : metamodel.findEndpoints(customNameBinding)) {
			assertThat(endpoint.getProblemLevel(), not(equalTo(0)));
		}
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(true));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(1));
	}
	
	@Test
	public void shouldReportProblemWhenNameBindingTypeRetentionAnnotationHasWrongValue() throws CoreException,
		ValidationException {
		// preconditions
		final IType customNameBindingType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding");
		final JaxrsNameBinding customNameBinding = (JaxrsNameBinding) metamodel.findElement(customNameBindingType);
		final Annotation retentionAnnotation = customNameBinding.getAnnotation(RETENTION);
		customNameBinding.addOrUpdateAnnotation(createAnnotation(retentionAnnotation, "FOO"));
		replaceFirstOccurrenceOfCode(customNameBindingType.getCompilationUnit(), "@Retention(RetentionPolicy.RUNTIME)", "@Retention(RetentionPolicy.SOURCE)", true);
		deleteJaxrsMarkers(customNameBinding);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(customNameBinding.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(customNameBinding);
		assertThat(markers.length, equalTo(1));
		assertThat(markers, hasPreferenceKey(NAME_BINDING_INVALID_RETENTION_ANNOTATION_VALUE));
		for(IJaxrsEndpoint endpoint : metamodel.findEndpoints(customNameBinding)) {
			assertThat(endpoint.getProblemLevel(), not(equalTo(0)));
		}
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(true));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(1));
	}

	@Test
	public void shouldNotFailOnProblemIfSeverityLevelIsIgnore() throws CoreException, ValidationException {
		// preconditions
		final IType customNameBindingType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding");
		final JaxrsNameBinding customNameBinding = (JaxrsNameBinding) metamodel.findElement(customNameBindingType);
		final Annotation retentionAnnotation = customNameBinding.getAnnotation(RETENTION);
		customNameBinding.addOrUpdateAnnotation(createAnnotation(retentionAnnotation, "FOO"));
		replaceFirstOccurrenceOfCode(customNameBindingType.getCompilationUnit(), "@Retention(RetentionPolicy.RUNTIME)", "@Retention(RetentionPolicy.SOURCE)", true);
		deleteJaxrsMarkers(customNameBinding);
		metamodelMonitor.resetElementChangesNotifications();
		final IEclipsePreferences defaultPreferences = ((IScopeContext)DefaultScope.INSTANCE).getNode(JBossJaxrsUIPlugin.PLUGIN_ID);
		defaultPreferences.put(JaxrsPreferences.NAME_BINDING_INVALID_RETENTION_ANNOTATION_VALUE, JaxrsPreferences.IGNORE);
		// operation
		new JaxrsMetamodelValidator().validate(toSet(customNameBinding.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(customNameBinding);
		assertThat(markers.length, equalTo(0));
	}
	
	@Test
	public void shouldIncreaseAndResetProblemLevelOnNameBinding() throws CoreException, ValidationException {
		// preconditions
		final IType customNameBindingType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding");
		final JaxrsNameBinding customNameBinding = (JaxrsNameBinding) metamodel.findElement(customNameBindingType);
		final Annotation retentionAnnotation = customNameBinding.getAnnotation(RETENTION);
		customNameBinding.addOrUpdateAnnotation(createAnnotation(retentionAnnotation, "FOO"));
		replaceFirstOccurrenceOfCode(customNameBindingType.getCompilationUnit(), "@Retention(RetentionPolicy.RUNTIME)", "@Retention(RetentionPolicy.SOURCE)", true);
		deleteJaxrsMarkers(customNameBinding);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(customNameBinding.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// verification: problem level is set to '2'
		assertThat(customNameBinding.getProblemLevel(), equalTo(2));
		// now, fix the problem 
		replaceFirstOccurrenceOfCode(customNameBindingType.getCompilationUnit(), "@Retention(RetentionPolicy.SOURCE)", "@Retention(RetentionPolicy.RUNTIME)", true);
		// revalidate
		new JaxrsMetamodelValidator().validate(toSet(customNameBinding.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// verification: problem level is set to '0'
		assertThat(customNameBinding.getProblemLevel(), equalTo(0));
	}

}
