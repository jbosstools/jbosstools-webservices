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
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsElementsUtils.replaceFirstOccurrenceOfCode;
import static org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils.replaceFirstOccurrenceOfCode;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.HTTP_METHOD;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.RETENTION;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.TARGET;
import static org.jboss.tools.ws.jaxrs.core.validation.IJaxrsValidation.JAXRS_PROBLEM_MARKER_ID;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.deleteJaxrsMarkers;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.findJaxrsMarkers;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.havePreferenceKey;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.toSet;
import static org.jboss.tools.ws.jaxrs.ui.preferences.JaxrsPreferences.HTTP_METHOD_INVALID_RETENTION_ANNOTATION_VALUE;
import static org.jboss.tools.ws.jaxrs.ui.preferences.JaxrsPreferences.HTTP_METHOD_INVALID_TARGET_ANNOTATION_VALUE;
import static org.jboss.tools.ws.jaxrs.ui.preferences.JaxrsPreferences.HTTP_METHOD_MISSING_RETENTION_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.ui.preferences.JaxrsPreferences.HTTP_METHOD_MISSING_TARGET_ANNOTATION;
import static org.junit.Assert.assertThat;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
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
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementCategory;
import org.jboss.tools.ws.jaxrs.ui.JBossJaxrsUIPlugin;
import org.jboss.tools.ws.jaxrs.ui.preferences.JaxrsPreferences;
import org.jboss.tools.ws.jaxrs.ui.quickfix.JaxrsMarkerResolutionGenerator;
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
public class JaxrsHttpMethodValidatorTestCase {

	private final IReporter reporter = new ReporterHelper(new NullProgressMonitor());
	private final ContextValidationHelper validationHelper = new ContextValidationHelper();
	private final IProjectValidationContext context = new ProjectValidationContext();
	private final ValidatorManager validatorManager = new ValidatorManager();

	@ClassRule
	public static WorkspaceSetupRule workspaceSetupRule = new WorkspaceSetupRule("org.jboss.tools.ws.jaxrs.tests.sampleproject");
	
	@Rule
	public JaxrsMetamodelMonitor metamodelMonitor = new JaxrsMetamodelMonitor("org.jboss.tools.ws.jaxrs.tests.sampleproject", false);
	
	private JaxrsMetamodel metamodel = null;

	private IProject project = null;

	private IJavaProject javaProject = null;

	@Before
	public void setup() throws CoreException {
		metamodel = metamodelMonitor.getMetamodel();
		project = metamodel.getProject();
		javaProject = metamodel.getJavaProject();
		// remove 'org.jboss.tools.ws.jaxrs.sample.services.BazResource' to avoid side effects on this resource which uses the 'FOO' HTTP Method annotation.
		final IResource bazResource = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BazResource").getResource();
		ResourcesUtils.delete(bazResource);
	}
	
	@After
	public void resetProblemLevelPreferences() {
		final IEclipsePreferences defaultPreferences = ((IScopeContext)DefaultScope.INSTANCE).getNode(JBossJaxrsUIPlugin.PLUGIN_ID);
		defaultPreferences.put(JaxrsPreferences.HTTP_METHOD_INVALID_RETENTION_ANNOTATION_VALUE, JaxrsPreferences.ERROR);
	}

	@Test
	public void shouldValidateHttpMethod() throws CoreException, ValidationException {
		// preconditions
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final JaxrsHttpMethod httpMethod = (JaxrsHttpMethod) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.FOO", EnumElementCategory.HTTP_METHOD);
		deleteJaxrsMarkers(httpMethod);
		assertThat(findJaxrsMarkers(httpMethod).length, equalTo(0));
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		
		// validation
		assertThat(findJaxrsMarkers(httpMethod).length, equalTo(0));
	}

	@Test
	public void shouldSkipValidationOnBinaryHttpMethod() throws CoreException, ValidationException {
		// preconditions: create an HttpMethod from the binary annotation, then try to validate
		metamodelMonitor.createElements("javax.ws.rs.GET");
		final JaxrsHttpMethod httpMethod = (JaxrsHttpMethod) metamodel.findElement("javax.ws.rs.GET", EnumElementCategory.HTTP_METHOD);
		deleteJaxrsMarkers(httpMethod);
		assertThat(findJaxrsMarkers(httpMethod).length, equalTo(0));
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		
		// validation
		assertThat(findJaxrsMarkers(httpMethod).length, equalTo(0));
	}

	@Test
	public void shouldNotReportProblemWhenHttpMethodVerbIsNull() throws CoreException, ValidationException {
		// preconditions
		final IType fooType = replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject, "@HttpMethod(\"FOO\")", "@HttpMethod", true);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final JaxrsHttpMethod httpMethod = (JaxrsHttpMethod) metamodel.findElement(fooType);
		deleteJaxrsMarkers(httpMethod);
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		
		// validation
		final IMarker[] markers = findJaxrsMarkers(httpMethod);
		assertThat(markers.length, equalTo(0));
	}

	@Test
	public void shouldApplyProposalWhenHttpMethodVerbIsNull() throws CoreException, ValidationException {
		// preconditions
		final IType fooType = replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject, "@HttpMethod(\"FOO\")", "@HttpMethod", true);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final JaxrsHttpMethod httpMethod = (JaxrsHttpMethod) metamodel.findElement(fooType);
		deleteJaxrsMarkers(httpMethod);
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation
		final IJavaCompletionProposal[] proposals = ValidationUtils.getJavaCompletionProposals(httpMethod.getAnnotation(HTTP_METHOD));
		assertThat(proposals.length, equalTo(1));
		ValidationUtils.applyProposals(httpMethod, proposals);
		
		// validation
		assertThat(ValidationUtils.findJavaProblems(httpMethod.getResource()).length, equalTo(0));
	}

	@Test
	public void shouldNotReportProblemWhenHttpMethodVerbMissing() throws CoreException, ValidationException {
		// preconditions
		final IType fooType = replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject, "@HttpMethod(\"FOO\")", "@HttpMethod()", true);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final JaxrsHttpMethod httpMethod = (JaxrsHttpMethod) metamodel.findElement(fooType);
		deleteJaxrsMarkers(httpMethod);
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		
		// validation
		final IMarker[] markers = findJaxrsMarkers(httpMethod);
		assertThat(markers.length, equalTo(0));
	}

	@Test
	public void shouldApplyProposalWhenHttpMethodVerbMissing() throws CoreException, ValidationException {
		// preconditions
		final IType fooType = replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject, "@HttpMethod(\"FOO\")", "@HttpMethod()", true);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final JaxrsHttpMethod httpMethod = (JaxrsHttpMethod) metamodel.findElement(fooType);
		deleteJaxrsMarkers(httpMethod);
		metamodelMonitor.resetElementChangesNotifications();
		final IJavaCompletionProposal[] proposals = ValidationUtils.getJavaCompletionProposals(httpMethod.getAnnotation(HTTP_METHOD));
		assertThat(proposals.length, equalTo(1));
		
		// operation
		ValidationUtils.applyProposals(httpMethod, proposals);
		
		// validation
		assertThat(ValidationUtils.findJavaProblems(httpMethod.getResource()).length, equalTo(0));
	}

	@Test
	public void shouldApplyProposalWhenHttpMethodVerbMissingWithSpaces() throws CoreException, ValidationException {
		// preconditions
		final IType fooType = replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject, "@HttpMethod(\"FOO\")", "@HttpMethod (  )", true);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final JaxrsHttpMethod httpMethod = (JaxrsHttpMethod) metamodel.findElement(fooType);
		deleteJaxrsMarkers(httpMethod);
		metamodelMonitor.resetElementChangesNotifications();
		final IJavaCompletionProposal[] proposals = ValidationUtils.getJavaCompletionProposals(httpMethod.getAnnotation(HTTP_METHOD));
		assertThat(proposals.length, equalTo(1));
		
		// operation
		ValidationUtils.applyProposals(httpMethod, proposals);
		
		// validation
		assertThat(ValidationUtils.findJavaProblems(httpMethod.getResource()).length, equalTo(0));
	}
	
	@Test
	public void shouldApplyProposalWhenHttpMethodVerbMissingWithComments() throws CoreException, ValidationException {
		// preconditions
		final IType fooType = replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject, "@HttpMethod(\"FOO\")", "@HttpMethod()//comment", true);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final JaxrsHttpMethod httpMethod = (JaxrsHttpMethod) metamodel.findElement(fooType);
		deleteJaxrsMarkers(httpMethod);
		metamodelMonitor.resetElementChangesNotifications();
		final IJavaCompletionProposal[] proposals = ValidationUtils.getJavaCompletionProposals(httpMethod.getAnnotation(HTTP_METHOD));
		assertThat(proposals.length, equalTo(1));
		// operation
		ValidationUtils.applyProposals(httpMethod, proposals);
		// validation
		assertThat(ValidationUtils.findJavaProblems(httpMethod.getResource()).length, equalTo(0));
	}
	
	@Test
	public void shouldReportProblemWhenTargetAnnotationMissing() throws CoreException, ValidationException {
		// preconditions
		final IType fooType = replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject, "@Target(value=ElementType.METHOD)", "", true);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final JaxrsHttpMethod httpMethod = (JaxrsHttpMethod) metamodel.findElement(fooType);
		deleteJaxrsMarkers(httpMethod);
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(httpMethod);
		assertThat(markers.length, equalTo(1));
		assertThat(markers, havePreferenceKey(HTTP_METHOD_MISSING_TARGET_ANNOTATION));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(true));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(1));
		assertThat(new JaxrsMarkerResolutionGenerator().getResolutions(markers[0]), notNullValue());
	}

	@Test
	public void shouldNotReportProblemWhenTargetAnnotationHasNullValue() throws CoreException,
			ValidationException {
		// preconditions
		final IType fooType = replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject, "@Target(value=ElementType.METHOD)", "@Target", true);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final JaxrsHttpMethod httpMethod = (JaxrsHttpMethod) metamodel.findElement(fooType);
		deleteJaxrsMarkers(httpMethod);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation: no marker but java proposal
		final IMarker[] markers = findJaxrsMarkers(httpMethod);
		assertThat(markers.length, equalTo(0));
		final IJavaCompletionProposal[] proposals = ValidationUtils.getJavaCompletionProposals(httpMethod.getAnnotation(TARGET));
		assertThat(proposals.length, equalTo(1));
	}

	@Test
	public void shouldNotReportProblemWhenTargetAnnotationValueMissing() throws CoreException,
	ValidationException {
		// preconditions
		final IType fooType = replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject, "@Target(value=ElementType.METHOD)", "@Target()", true);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final JaxrsHttpMethod httpMethod = (JaxrsHttpMethod) metamodel.findElement(fooType);
		deleteJaxrsMarkers(httpMethod);
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation: no marker but java proposal
		final IMarker[] markers = findJaxrsMarkers(httpMethod);
		assertThat(markers.length, equalTo(0));
		final IJavaCompletionProposal[] proposals = ValidationUtils.getJavaCompletionProposals(httpMethod.getAnnotation(TARGET));
		assertThat(proposals.length, equalTo(1));
	}

	@Test
	public void shouldApplyProposalWhenTargetAnnotationValueMissing() throws CoreException,
	ValidationException {
		// preconditions
		final IType fooType = replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject, "@Target(value=ElementType.METHOD)", "@Target()", true);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final JaxrsHttpMethod httpMethod = (JaxrsHttpMethod) metamodel.findElement(fooType);
		deleteJaxrsMarkers(httpMethod);
		metamodelMonitor.resetElementChangesNotifications();

		// operation
		final IJavaCompletionProposal[] proposals = ValidationUtils.getJavaCompletionProposals(httpMethod.getAnnotation(TARGET));
		assertThat(proposals.length, equalTo(1));
		ValidationUtils.applyProposals(httpMethod, proposals);
		
		// validation
		assertThat(ValidationUtils.findJavaProblems(httpMethod.getResource()).length, equalTo(0));
	}
	
	@Test
	public void shouldReportProblemWhenTargetAnnotationHasWrongValue() throws CoreException,
			ValidationException {
		// preconditions
		final IType fooType = replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject, "@Target(value=ElementType.METHOD)", "@Target(value=elementType.FIELD)", true);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final JaxrsHttpMethod httpMethod = (JaxrsHttpMethod) metamodel.findElement(fooType);
		deleteJaxrsMarkers(httpMethod);
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(httpMethod);
		assertThat(markers.length, equalTo(1));
		assertThat(markers, havePreferenceKey(HTTP_METHOD_INVALID_TARGET_ANNOTATION_VALUE));
		assertThat(markers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(true));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(1));
		assertThat(new JaxrsMarkerResolutionGenerator().getResolutions(markers[0]), notNullValue());
	}

	@Test
	public void shouldReportProblemWhenRetentionAnnotationMissing() throws CoreException, ValidationException {
		// preconditions
		final IType fooType = replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject, "@Retention(value=RetentionPolicy.RUNTIME)", "", true);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final JaxrsHttpMethod httpMethod = (JaxrsHttpMethod) metamodel.findElement(fooType);
		deleteJaxrsMarkers(httpMethod);
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);

		// validation
		final IMarker[] markers = findJaxrsMarkers(httpMethod);
		assertThat(markers.length, equalTo(1));
		assertThat(markers, havePreferenceKey(HTTP_METHOD_MISSING_RETENTION_ANNOTATION));
		assertThat(markers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(true));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(1));
		assertThat(new JaxrsMarkerResolutionGenerator().getResolutions(markers[0]), notNullValue());
	}
	
	@Test
	public void shouldNotReportProblemWhenRetentionAnnotationHasNullValue() throws CoreException,
	ValidationException {
		// preconditions
		final IType fooType = replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject, "@Retention(value=RetentionPolicy.RUNTIME)", "@Retention", true);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final JaxrsHttpMethod httpMethod = (JaxrsHttpMethod) metamodel.findElement(fooType);
		deleteJaxrsMarkers(httpMethod);
		metamodelMonitor.resetElementChangesNotifications();

		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);

		// validation
		final IMarker[] markers = findJaxrsMarkers(httpMethod);
		assertThat(markers.length, equalTo(0));
	}

	@Test
	public void shouldApplyProposalWhenRetentionAnnotationHasNullValue() throws CoreException,
	ValidationException {
		// preconditions
		final IType fooType = replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject, "@Retention(value=RetentionPolicy.RUNTIME)", "@Retention", true);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final JaxrsHttpMethod httpMethod = (JaxrsHttpMethod) metamodel.findElement(fooType);
		deleteJaxrsMarkers(httpMethod);
		metamodelMonitor.resetElementChangesNotifications();

		// operation
		final IJavaCompletionProposal[] proposals = ValidationUtils.getJavaCompletionProposals(httpMethod.getAnnotation(RETENTION));
		assertThat(proposals.length, equalTo(1));
		ValidationUtils.applyProposals(httpMethod, proposals);
		// validation
		assertThat(ValidationUtils.findJavaProblems(httpMethod.getResource()).length, equalTo(0));
	}


	@Test
	public void shouldNotReportProblemWhenRetentionAnnotationValueMissing() throws CoreException,
	ValidationException {
		// preconditions
		final IType fooType = replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject, "@Retention(value=RetentionPolicy.RUNTIME)", "@Retention()", true);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final JaxrsHttpMethod httpMethod = (JaxrsHttpMethod) metamodel.findElement(fooType);
		deleteJaxrsMarkers(httpMethod);
		metamodelMonitor.resetElementChangesNotifications();

		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		
		// validation
		final IMarker[] markers = findJaxrsMarkers(httpMethod);
		assertThat(markers.length, equalTo(0));
	}

	@Test
	public void shouldApplyProposalWhenRetentionAnnotationValueMissing() throws CoreException,
	ValidationException {
		// preconditions
		final IType fooType = replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject, "@Retention(value=RetentionPolicy.RUNTIME)", "@Retention()", true);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final JaxrsHttpMethod httpMethod = (JaxrsHttpMethod) metamodel.findElement(fooType);
		deleteJaxrsMarkers(httpMethod);
		metamodelMonitor.resetElementChangesNotifications();

		// operation
		final IJavaCompletionProposal[] proposals = ValidationUtils.getJavaCompletionProposals(httpMethod.getAnnotation(RETENTION));
		assertThat(proposals.length, equalTo(1));
		ValidationUtils.applyProposals(httpMethod, proposals);

		// validation
		assertThat(ValidationUtils.findJavaProblems(httpMethod.getResource()).length, equalTo(0));
	}

	@Test
	public void shouldReportProblemWhenRetentionAnnotationHasWrongValue() throws CoreException,
		ValidationException {
		// preconditions
		final IType fooType = replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject, "@Retention(value=RetentionPolicy.RUNTIME)", "@Retention(value=RetentionPolicy.SOURCE)", true);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final JaxrsHttpMethod httpMethod = (JaxrsHttpMethod) metamodel.findElement(fooType);
		deleteJaxrsMarkers(httpMethod);
		metamodelMonitor.resetElementChangesNotifications();

		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(httpMethod);
		assertThat(markers.length, equalTo(1));
		assertThat(markers, havePreferenceKey(HTTP_METHOD_INVALID_RETENTION_ANNOTATION_VALUE));
		assertThat(markers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(true));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(1));
		assertThat(new JaxrsMarkerResolutionGenerator().getResolutions(markers[0]), notNullValue());
	}


	/**
	 * @see 
	 * @throws CoreException
	 * @throws ValidationException
	 */
	@Test
	public void shouldNotReportProblemWhenRefactoringUnrelatedAnnotation() throws CoreException,
	ValidationException {
		// preconditions
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.CustomCDIQualifier");
		final IType customQualifierType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomCDIQualifier");
		assertThat(customQualifierType.exists(), is(true));
		metamodelMonitor.resetElementChangesNotifications();
		
		// operations: rename the Java type and attempt to create an HttpMethod and validate its underlying resource.
		customQualifierType.rename("FOOBAR", true, new NullProgressMonitor());
		final IType foobarType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOOBAR");
		metamodelMonitor.createHttpMethod(foobarType);
		new JaxrsMetamodelValidator().validate(toSet(foobarType.getResource()), project, validationHelper, context,
				validatorManager, reporter);

		// validation
		final IMarker[] markers = foobarType.getResource().findMarkers(JAXRS_PROBLEM_MARKER_ID, false, IResource.DEPTH_INFINITE);
		assertThat(markers.length, equalTo(0));
	}
	
	@Test
	public void shouldNotFailOnProblemIfSeverityLevelIsIgnore() throws CoreException, ValidationException {
		// preconditions
		final IType fooType = replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject, "@Retention(value=RetentionPolicy.RUNTIME)", "@Retention(value=RetentionPolicy.SOURCE)", true);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final JaxrsHttpMethod httpMethod = (JaxrsHttpMethod) metamodel.findElement(fooType);
		deleteJaxrsMarkers(httpMethod);
		metamodelMonitor.resetElementChangesNotifications();
		final IEclipsePreferences defaultPreferences = ((IScopeContext)DefaultScope.INSTANCE).getNode(JBossJaxrsUIPlugin.PLUGIN_ID);
		defaultPreferences.put(JaxrsPreferences.HTTP_METHOD_INVALID_RETENTION_ANNOTATION_VALUE, JaxrsPreferences.IGNORE);
		
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		
		// validation
		final IMarker[] markers = findJaxrsMarkers(httpMethod);
		assertThat(markers.length, equalTo(0));
	}
	
	@Test
	public void shouldIncreaseAndResetProblemLevelOnHttpMethod() throws CoreException, ValidationException {
		// preconditions
		final IType fooType = replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject, "@Retention(value=RetentionPolicy.RUNTIME)", "@Retention(value=RetentionPolicy.SOURCE)", true);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final JaxrsHttpMethod httpMethod = (JaxrsHttpMethod) metamodel.findElement(fooType);
		deleteJaxrsMarkers(httpMethod);
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		
		// verification: problem level is set to '2'
		assertThat(httpMethod.getProblemSeverity(), equalTo(2));
		
		// now, fix the problem 
		replaceFirstOccurrenceOfCode(httpMethod, "@Retention(value=RetentionPolicy.SOURCE)", "@Retention(value=RetentionPolicy.RUNTIME)", false);
		// revalidate
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		
		// verification: problem level is set to '0'
		assertThat(httpMethod.getProblemSeverity(), equalTo(0));
	}

}
