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
package org.jboss.tools.ws.jaxrs.ui.internal.validation;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.equalTo;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsElementsUtils.replaceFirstOccurrenceOfCode;
import static org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils.replaceFirstOccurrenceOfCode;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.RETENTION;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.TARGET;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.deleteJaxrsMarkers;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.findJaxrsMarkers;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.havePreferenceKey;
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
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.wst.validation.ReporterHelper;
import org.eclipse.wst.validation.internal.core.ValidationException;
import org.eclipse.wst.validation.internal.provisional.core.IReporter;
import org.jboss.tools.common.validation.ContextValidationHelper;
import org.jboss.tools.common.validation.IProjectValidationContext;
import org.jboss.tools.common.validation.ValidatorManager;
import org.jboss.tools.common.validation.internal.ProjectValidationContext;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsBaseElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsNameBinding;
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementCategory;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsEndpoint;
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
	public static WorkspaceSetupRule workspaceSetupRule = new WorkspaceSetupRule(
			"org.jboss.tools.ws.jaxrs.tests.sampleproject2");

	@Rule
	public JaxrsMetamodelMonitor metamodelMonitor = new JaxrsMetamodelMonitor(
			"org.jboss.tools.ws.jaxrs.tests.sampleproject2", false);

	private JaxrsMetamodel metamodel = null;

	private IProject project = null;

	private IJavaProject javaProject = null;
	
	@Before
	public void setup() throws CoreException {
		metamodel = metamodelMonitor.getMetamodel();
		project = metamodel.getProject();
		javaProject = metamodel.getJavaProject();
	}

	@After
	public void resetProblemLevelPreferences() {
		final IEclipsePreferences defaultPreferences = ((IScopeContext) DefaultScope.INSTANCE)
				.getNode(JBossJaxrsUIPlugin.PLUGIN_ID);
		defaultPreferences
				.put(JaxrsPreferences.NAME_BINDING_INVALID_RETENTION_ANNOTATION_VALUE, JaxrsPreferences.ERROR);
	}

	@Test
	public void shouldValidateNameBinding() throws CoreException, ValidationException {
		// preconditions
		// delete CarResource that has param validation errors
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding");
		final IType customNameBindingType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding");
		final JaxrsBaseElement customNameBinding = (JaxrsBaseElement) metamodel
				.findElement(customNameBindingType);
		deleteJaxrsMarkers(customNameBinding);
		assertThat(findJaxrsMarkers(customNameBinding).length, equalTo(0));
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation
		new JaxrsMetamodelValidator().validate(toSet(customNameBinding.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		
		// validation
		assertThat(findJaxrsMarkers(customNameBinding).length, equalTo(0));
		for (IJaxrsEndpoint endpoint : metamodel.findEndpoints(customNameBinding)) {
			assertThat(endpoint.getProblemLevel(), equalTo(0));
		}
		// no problem level change on the metamodel.
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(0));
	}

	@Test
	public void shouldReportProblemWhenTargetAnnotationMissing() throws CoreException, ValidationException {
		// preconditions
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding", javaProject,
				"@Target({ ElementType.TYPE, ElementType.METHOD })", "", true);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding");
		final JaxrsNameBinding customNameBinding = (JaxrsNameBinding) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding", EnumElementCategory.NAME_BINDING);
		deleteJaxrsMarkers(customNameBinding);
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation
		new JaxrsMetamodelValidator().validate(toSet(customNameBinding.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(customNameBinding);
		assertThat(markers.length, equalTo(1));
		assertThat(markers, havePreferenceKey(NAME_BINDING_MISSING_TARGET_ANNOTATION));
		assertThat(markers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
		for (IJaxrsEndpoint endpoint : metamodel.findEndpoints(customNameBinding)) {
			assertThat(endpoint.getProblemLevel(), not(equalTo(0)));
		}
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(true));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(1));
	}

	@Test
	public void shouldNotReportProblemWhenTargetAnnotationValueNull() throws CoreException, ValidationException {
		// preconditions
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding", javaProject,
				"@Target({ ElementType.TYPE, ElementType.METHOD })", "@Target", true);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding");
		final JaxrsNameBinding customNameBinding = (JaxrsNameBinding) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding", EnumElementCategory.NAME_BINDING);
		deleteJaxrsMarkers(customNameBinding);
		metamodelMonitor.resetElementChangesNotifications();

		// operation
		new JaxrsMetamodelValidator().validate(toSet(customNameBinding.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		
		// validation
		final IMarker[] markers = findJaxrsMarkers(customNameBinding);
		assertThat(markers.length, equalTo(0));
		final IJavaCompletionProposal[] proposals = ValidationUtils.getJavaCompletionProposals(customNameBinding
				.getAnnotation(TARGET));
		assertThat(proposals.length, equalTo(1));
	}

	@Test
	public void shouldApplyProposalWhenTargetAnnotationValueNull() throws CoreException, ValidationException {
		// preconditions
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding", javaProject,
				"@Target({ ElementType.TYPE, ElementType.METHOD })", "@Target", true);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding");
		final JaxrsNameBinding customNameBinding = (JaxrsNameBinding) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding", EnumElementCategory.NAME_BINDING);
		deleteJaxrsMarkers(customNameBinding);
		metamodelMonitor.resetElementChangesNotifications();
		final IJavaCompletionProposal[] proposals = ValidationUtils.getJavaCompletionProposals(customNameBinding
				.getAnnotation(TARGET));
		assertThat(proposals.length, equalTo(1));

		// operation
		ValidationUtils.applyProposals(customNameBinding, proposals);
		
		// validation
		assertThat(ValidationUtils.findJavaProblems(customNameBinding.getResource()).length, equalTo(0));
	}

	@Test
	public void shouldNotReportProblemWhenTargetAnnotationValueMissing() throws CoreException, ValidationException {
		// preconditions
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding", javaProject,
				"@Target({ ElementType.TYPE, ElementType.METHOD })", "@Target()", true);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding");
		final JaxrsNameBinding customNameBinding = (JaxrsNameBinding) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding", EnumElementCategory.NAME_BINDING);
		deleteJaxrsMarkers(customNameBinding);
		metamodelMonitor.resetElementChangesNotifications();

		// operation
		new JaxrsMetamodelValidator().validate(toSet(customNameBinding.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		
		// validation
		final IMarker[] markers = findJaxrsMarkers(customNameBinding);
		assertThat(markers.length, equalTo(0));
		final IJavaCompletionProposal[] proposals = ValidationUtils.getJavaCompletionProposals(customNameBinding
				.getAnnotation(TARGET));
		assertThat(proposals.length, equalTo(1));
	}
	
	@Test
	public void shouldApplyProposalWhenTargetAnnotationValueMissing() throws CoreException, ValidationException {
		// preconditions
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding", javaProject,
				"@Target({ ElementType.TYPE, ElementType.METHOD })", "@Target()", true);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding");
		final JaxrsNameBinding customNameBinding = (JaxrsNameBinding) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding", EnumElementCategory.NAME_BINDING);
		deleteJaxrsMarkers(customNameBinding);
		metamodelMonitor.resetElementChangesNotifications();

		final IJavaCompletionProposal[] proposals = ValidationUtils.getJavaCompletionProposals(customNameBinding
				.getAnnotation(TARGET));
		assertThat(proposals.length, equalTo(1));
		
		// operation
		ValidationUtils.applyProposals(customNameBinding, proposals);
		
		// validation
		assertThat(ValidationUtils.findJavaProblems(customNameBinding.getResource()).length, equalTo(0));
	}

	@Test
	public void shouldReportProblemWhenTargetAnnotationHasWrongValue() throws CoreException, ValidationException {
		// preconditions
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding", javaProject,
				"@Target({ ElementType.TYPE, ElementType.METHOD })", "@Target(value=ElementType.FIELD)", true);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding");
		final JaxrsNameBinding customNameBinding = (JaxrsNameBinding) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding", EnumElementCategory.NAME_BINDING);
		deleteJaxrsMarkers(customNameBinding);
		metamodelMonitor.resetElementChangesNotifications();

		// operation
		new JaxrsMetamodelValidator().validate(toSet(customNameBinding.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		
		// validation
		final IMarker[] markers = findJaxrsMarkers(customNameBinding);
		assertThat(markers.length, equalTo(1));
		assertThat(markers, havePreferenceKey(NAME_BINDING_INVALID_TARGET_ANNOTATION_VALUE));
		for (IJaxrsEndpoint endpoint : metamodel.findEndpoints(customNameBinding)) {
			assertThat(endpoint.getProblemLevel(), not(equalTo(0)));
		}
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(true));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(1));
	}

	@Test
	public void shouldReportProblemWhenRetentionAnnotationMissing() throws CoreException, ValidationException {
		// preconditions
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding", javaProject,
				"@Retention(RetentionPolicy.RUNTIME)", "", true);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding");
		final JaxrsNameBinding customNameBinding = (JaxrsNameBinding) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding", EnumElementCategory.NAME_BINDING);
		deleteJaxrsMarkers(customNameBinding);
		metamodelMonitor.resetElementChangesNotifications();

		// operation
		new JaxrsMetamodelValidator().validate(toSet(customNameBinding.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		
		// validation
		final IMarker[] markers = findJaxrsMarkers(customNameBinding);
		assertThat(markers.length, equalTo(1));
		assertThat(markers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
		assertThat(markers, havePreferenceKey(NAME_BINDING_MISSING_RETENTION_ANNOTATION));
		for (IJaxrsEndpoint endpoint : metamodel.findEndpoints(customNameBinding)) {
			assertThat(endpoint.getProblemLevel(), not(equalTo(0)));
		}
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(true));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(1));
	}

	@Test
	public void shouldNotReportProblemRetentionAnnotationValueNull() throws CoreException, ValidationException {
		// preconditions
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding", javaProject,
				"@Retention(RetentionPolicy.RUNTIME)", "@Retention", true);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding");
		final JaxrsNameBinding customNameBinding = (JaxrsNameBinding) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding", EnumElementCategory.NAME_BINDING);
		deleteJaxrsMarkers(customNameBinding);
		metamodelMonitor.resetElementChangesNotifications();

		// operation
		new JaxrsMetamodelValidator().validate(toSet(customNameBinding.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		
		// validation
		final IMarker[] markers = findJaxrsMarkers(customNameBinding);
		assertThat(markers.length, equalTo(0));
	}
	
	@Test
	public void shouldApplyProposalWhenRetentionAnnotationValueNull() throws CoreException, ValidationException {
		// preconditions
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding", javaProject,
				"@Retention(RetentionPolicy.RUNTIME)", "@Retention", true);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding");
		final JaxrsNameBinding customNameBinding = (JaxrsNameBinding) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding", EnumElementCategory.NAME_BINDING);
		deleteJaxrsMarkers(customNameBinding);
		metamodelMonitor.resetElementChangesNotifications();
		final IJavaCompletionProposal[] proposals = ValidationUtils.getJavaCompletionProposals(customNameBinding
				.getAnnotation(RETENTION));
		assertThat(proposals.length, equalTo(1));
		
		// operation
		ValidationUtils.applyProposals(customNameBinding, proposals);
		
		// validation
		assertThat(ValidationUtils.findJavaProblems(customNameBinding.getResource()).length, equalTo(0));
	}

	@Test
	public void shouldNotReportProblemRetentionAnnotationValueMissing() throws CoreException, ValidationException {
		// preconditions
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding", javaProject,
				"@Retention(RetentionPolicy.RUNTIME)", "@Retention()", true);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding");
		final JaxrsNameBinding customNameBinding = (JaxrsNameBinding) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding", EnumElementCategory.NAME_BINDING);
		deleteJaxrsMarkers(customNameBinding);
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation
		new JaxrsMetamodelValidator().validate(toSet(customNameBinding.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		
		// validation
		final IMarker[] markers = findJaxrsMarkers(customNameBinding);
		assertThat(markers.length, equalTo(0));
	}
	
	@Test
	public void shouldApplyProposalWhenRetentionAnnotationValueMissing() throws CoreException, ValidationException {
		// preconditions
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding", javaProject,
				"@Retention(RetentionPolicy.RUNTIME)", "@Retention()", true);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding");
		final JaxrsNameBinding customNameBinding = (JaxrsNameBinding) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding", EnumElementCategory.NAME_BINDING);
		deleteJaxrsMarkers(customNameBinding);
		metamodelMonitor.resetElementChangesNotifications();
		final IJavaCompletionProposal[] proposals = ValidationUtils.getJavaCompletionProposals(customNameBinding
				.getAnnotation(RETENTION));
		assertThat(proposals.length, equalTo(1));
		
		// operation
		ValidationUtils.applyProposals(customNameBinding, proposals);
		
		// validation
		assertThat(ValidationUtils.findJavaProblems(customNameBinding.getResource()).length, equalTo(0));
	}

	
	
	@Test
	public void shouldReportProblemWhenRetentionAnnotationHasMissingValue() throws CoreException, ValidationException {
		// preconditions
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding", javaProject,
				"@Retention(RetentionPolicy.RUNTIME)", "@Retention()", true);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding");
		final JaxrsNameBinding customNameBinding = (JaxrsNameBinding) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding", EnumElementCategory.NAME_BINDING);
		deleteJaxrsMarkers(customNameBinding);
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation
		new JaxrsMetamodelValidator().validate(toSet(customNameBinding.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		
		// validation
		final IMarker[] markers = findJaxrsMarkers(customNameBinding);
		assertThat(markers.length, equalTo(0));
		final IJavaCompletionProposal[] proposals = ValidationUtils.getJavaCompletionProposals(customNameBinding
				.getAnnotation(RETENTION));
		assertThat(proposals.length, equalTo(1));
	}

	@Test
	public void shouldReportProblemWhenRetentionAnnotationHasWrongValue() throws CoreException, ValidationException {
		// preconditions
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding", javaProject,
				"@Retention(RetentionPolicy.RUNTIME)", "@Retention(RetentionPolicy.SOURCE)", true);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding");
		final JaxrsNameBinding customNameBinding = (JaxrsNameBinding) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding", EnumElementCategory.NAME_BINDING);
		deleteJaxrsMarkers(customNameBinding);
		metamodelMonitor.resetElementChangesNotifications();

		// operation
		new JaxrsMetamodelValidator().validate(toSet(customNameBinding.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		
		// validation
		final IMarker[] markers = findJaxrsMarkers(customNameBinding);
		assertThat(markers.length, equalTo(1));
		assertThat(markers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
		assertThat(markers, havePreferenceKey(NAME_BINDING_INVALID_RETENTION_ANNOTATION_VALUE));
		for (IJaxrsEndpoint endpoint : metamodel.findEndpoints(customNameBinding)) {
			assertThat(endpoint.getProblemLevel(), not(equalTo(0)));
		}
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(true));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(1));
	}

	@Test
	public void shouldNotFailOnProblemIfSeverityLevelIsIgnore() throws CoreException, ValidationException {
		// preconditions
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding", javaProject,
				"@Retention(RetentionPolicy.RUNTIME)", "@Retention(RetentionPolicy.SOURCE)", true);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding");
		final JaxrsNameBinding customNameBinding = (JaxrsNameBinding) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding", EnumElementCategory.NAME_BINDING);
		deleteJaxrsMarkers(customNameBinding);
		metamodelMonitor.resetElementChangesNotifications();
		final IEclipsePreferences defaultPreferences = ((IScopeContext) DefaultScope.INSTANCE)
				.getNode(JBossJaxrsUIPlugin.PLUGIN_ID);
		defaultPreferences.put(JaxrsPreferences.NAME_BINDING_INVALID_RETENTION_ANNOTATION_VALUE,
				JaxrsPreferences.IGNORE);

		// operation
		new JaxrsMetamodelValidator().validate(toSet(customNameBinding.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		
		// validation
		final IMarker[] markers = findJaxrsMarkers(customNameBinding);
		assertThat(markers.length, equalTo(0));
	}

	@Test
	public void shouldIncreaseAndResetProblemLevelOnNameBinding() throws CoreException, ValidationException {
		// preconditions
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding", javaProject,
				"@Retention(RetentionPolicy.RUNTIME)", "@Retention(RetentionPolicy.SOURCE)", true);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding");
		final JaxrsNameBinding customNameBinding = (JaxrsNameBinding) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding", EnumElementCategory.NAME_BINDING);
		deleteJaxrsMarkers(customNameBinding);
		metamodelMonitor.resetElementChangesNotifications();

		// operation
		new JaxrsMetamodelValidator().validate(toSet(customNameBinding.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		// verification: problem level is set to '2'
		assertThat(customNameBinding.getProblemSeverity(), equalTo(2));
		// now, fix the problem
		replaceFirstOccurrenceOfCode(customNameBinding, "@Retention(RetentionPolicy.SOURCE)",
				"@Retention(RetentionPolicy.RUNTIME)", false);
		// revalidate
		new JaxrsMetamodelValidator().validate(toSet(customNameBinding.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		// verification: problem level is set to '0'
		assertThat(customNameBinding.getProblemSeverity(), equalTo(0));
	}

}
