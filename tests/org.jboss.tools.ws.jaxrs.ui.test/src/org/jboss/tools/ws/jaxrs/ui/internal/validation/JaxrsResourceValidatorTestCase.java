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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils.removeFirstOccurrenceOfCode;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils.replaceFirstOccurrenceOfCode;
import static org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils.replaceAllOccurrencesOfCode;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.deleteJaxrsMarkers;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.findJaxrsMarkers;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.hasPreferenceKey;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.toSet;
import static org.jboss.tools.ws.jaxrs.ui.preferences.JaxrsPreferences.RESOURCE_METHOD_UNBOUND_PATH_ANNOTATION_TEMPLATE_PARAMETER;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
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
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsParamConverterProvider;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsApplication;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsParamConverterProvider;
import org.jboss.tools.ws.jaxrs.ui.preferences.JaxrsPreferences;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Xi
 * 
 */
@SuppressWarnings("restriction")
public class JaxrsResourceValidatorTestCase {

	private static final Logger LOGGER = LoggerFactory.getLogger(JaxrsResourceValidatorTestCase.class);

	private final IReporter reporter = new ReporterHelper(new NullProgressMonitor());
	private final ContextValidationHelper validationHelper = new ContextValidationHelper();
	private final IProjectValidationContext context = new ProjectValidationContext();
	private final ValidatorManager validatorManager = new ValidatorManager();

	@ClassRule
	public static WorkspaceSetupRule workspaceSetupRule = new WorkspaceSetupRule(
			"org.jboss.tools.ws.jaxrs.tests.sampleproject");

	@Rule
	public JaxrsMetamodelMonitor metamodelMonitor = new JaxrsMetamodelMonitor(
			"org.jboss.tools.ws.jaxrs.tests.sampleproject", true);

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
		// remove the ParamConverterProvider
		for(IJaxrsParamConverterProvider paramConverterProvider : metamodel.findAllParamConverterProviders()) {
			((JaxrsParamConverterProvider)paramConverterProvider).remove();
		}
	}

	@Test
	public void shouldValidateCustomerResourceMethod() throws CoreException, ValidationException {
		// preconditions
		final IType customerJavaType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final AbstractJaxrsBaseElement customerResource = (AbstractJaxrsBaseElement) metamodel
				.findElement(customerJavaType);
		deleteJaxrsMarkers(customerResource);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(customerResource.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(customerResource);
		assertThat(markers.length, equalTo(0));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(0));
	}

	@Test
	public void shouldReportProblemsOnBarResourceMethods() throws CoreException, ValidationException {
		// preconditions
		final IType barJavaType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BarResource");
		final JaxrsResource barResource = metamodel.findResource(barJavaType);
		deleteJaxrsMarkers(barResource);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(barResource.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(barResource);
		for (IMarker marker : markers) {
			LOGGER.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(8));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(true));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(1));
	}

	@Test
	public void shouldReportProblemsOnBazResourceMethods() throws CoreException, ValidationException {
		// preconditions
		final IType barJavaType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BazResource");
		final JaxrsResource barResource = (JaxrsResource) metamodel.findElement(barJavaType);
		deleteJaxrsMarkers(barResource);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(barResource.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(barResource);
		assertThat(markers.length, equalTo(6));
		final Map<String, JaxrsResourceMethod> resourceMethods = barResource.getMethods();
		for (Entry<String, JaxrsResourceMethod> entry : resourceMethods.entrySet()) {
			final IMarker[] methodMarkers = findJaxrsMarkers(entry.getValue());
			if (entry.getKey().contains("getContent1")) {
				assertThat(entry.getValue().getProblemLevel(), is(IMarker.SEVERITY_WARNING));
				assertThat(methodMarkers.length, equalTo(IMarker.SEVERITY_WARNING));
				assertThat(methodMarkers, hasPreferenceKey(RESOURCE_METHOD_UNBOUND_PATH_ANNOTATION_TEMPLATE_PARAMETER));
			} else if (entry.getKey().contains("getContent2")) {
				assertThat(entry.getValue().getProblemLevel(), is(IMarker.SEVERITY_ERROR));
				assertThat(methodMarkers.length, equalTo(2));
			} else if (entry.getKey().contains("update1")) {
				assertThat(entry.getValue().getProblemLevel(), is(IMarker.SEVERITY_ERROR));
				assertThat(methodMarkers.length, equalTo(2));
			} else if (entry.getKey().contains("update2")) {
				assertThat(entry.getValue().getProblemLevel(), is(IMarker.SEVERITY_INFO));
				assertThat(methodMarkers.length, equalTo(0));
			} else if (entry.getKey().contains("update3")) {
				assertThat(entry.getValue().getProblemLevel(), is(IMarker.SEVERITY_ERROR));
				assertThat(methodMarkers.length, equalTo(1));
			} else {
				fail("Unexpected method " + entry.getKey());
			}
		}
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(true));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(1));
	}

	@Test
	public void shouldNotReportProblemOnValidResource() throws CoreException, ValidationException {
		// pre-conditions
		ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit("ValidationResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "ValidationResource.java");
		final JaxrsResource resource = (JaxrsResource) metamodel.findElement(compilationUnit.findPrimaryType());
		deleteJaxrsMarkers(resource);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(compilationUnit.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(resource);
		// verification
		assertThat(markers.length, equalTo(0));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(0));
	}

	@Test
	public void shouldReportProblemOnNonPublicJavaMethodInImplementationClass() throws CoreException,
			ValidationException {
		// pre-conditions
		ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit("ValidationResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "ValidationResource.java");
		replaceFirstOccurrenceOfCode(compilationUnit, "public Response", "private Response", false);
		final JaxrsResource resource = (JaxrsResource) metamodel.findElement(compilationUnit.findPrimaryType());
		deleteJaxrsMarkers(resource);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(compilationUnit.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(resource);
		// verification
		assertThat(markers.length, equalTo(1));
		assertThat(markers[0].getAttribute(IMarker.LINE_NUMBER, 0), equalTo(15));
		assertThat(markers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{0}")));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(true));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(1));
	}

	@Test
	public void shouldNotReportProblemOnNonPublicJavaMethodInInterface() throws CoreException, ValidationException {
		// pre-conditions
		ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit("IValidationResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "IValidationResource.java");
		final JaxrsResource resource = (JaxrsResource) metamodel.findElement(compilationUnit.findPrimaryType());
		deleteJaxrsMarkers(resource);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(compilationUnit.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(resource);
		// verification
		assertThat(markers.length, equalTo(0));
	}

	@Test
	public void shouldReportProblemOnUnboundTypePathArgument() throws ValidationException, CoreException {
		// pre-conditions
		ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit("ValidationResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "ValidationResource.java");
		removeFirstOccurrenceOfCode(compilationUnit, "@PathParam(\"type\") String type,", false);
		final JaxrsResource resource = (JaxrsResource) metamodel.findElement(compilationUnit.findPrimaryType());
		deleteJaxrsMarkers(resource);
		metamodelMonitor.resetElementChangesNotifications();
		// operation: remove the @PathParam, so that some @Path value has no
		// counterpart
		new JaxrsMetamodelValidator().validate(toSet(compilationUnit.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(resource);
		// verification
		assertThat(markers.length, equalTo(1));
		final IMarker marker = markers[0];
		assertThat(marker.getAttribute(IMarker.SEVERITY, 0), equalTo(IMarker.SEVERITY_WARNING));
		assertThat(marker.getAttribute(IMarker.LINE_NUMBER, 0), equalTo(10));
		assertThat(marker.getAttribute(IMarker.CHAR_END, 0) - marker.getAttribute(IMarker.CHAR_START, 0), equalTo(6));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(true));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(1));
	}

	@Test
	public void shouldReportProblemOnUnboundMethodPathArgument() throws ValidationException, CoreException {
		// pre-conditions
		ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit("ValidationResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "ValidationResource.java");
		removeFirstOccurrenceOfCode(compilationUnit, "@PathParam(\"format\") String format,", false);
		final JaxrsResource resource = (JaxrsResource) metamodel.findElement(compilationUnit.findPrimaryType());
		deleteJaxrsMarkers(resource);
		metamodelMonitor.resetElementChangesNotifications();
		// operation: remove the @PathParam, so that some @Path value has no
		// counterpart
		new JaxrsMetamodelValidator().validate(toSet(compilationUnit.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(resource);
		// verification
		assertThat(markers.length, equalTo(1));
		final IMarker marker = markers[0];
		assertThat(marker.getAttribute(IMarker.SEVERITY, 0), equalTo(IMarker.SEVERITY_WARNING));
		assertThat(marker.getAttribute(IMarker.LINE_NUMBER, 0), equalTo(14));
		assertThat(marker.getAttribute(IMarker.CHAR_END, 0) - marker.getAttribute(IMarker.CHAR_START, 0), equalTo(26));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(true));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(1));
	}

	@Test
	public void shouldReportProblemOnUnboundMethodPathArgumentWithWhitespaces() throws ValidationException,
			CoreException {
		// pre-conditions
		ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit("ValidationResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "ValidationResource.java");
		removeFirstOccurrenceOfCode(compilationUnit, "@PathParam(\"format\") String format,", false);
		replaceFirstOccurrenceOfCode(compilationUnit, "{format", "{  format", false);
		final JaxrsResource resource = (JaxrsResource) metamodel.findElement(compilationUnit.findPrimaryType());
		deleteJaxrsMarkers(resource);
		metamodelMonitor.resetElementChangesNotifications();
		// operation: remove the @PathParam, so that some @Path value has no
		// counterpart
		new JaxrsMetamodelValidator().validate(toSet(compilationUnit.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(resource);
		// verification
		assertThat(markers.length, equalTo(1));
		final IMarker marker = markers[0];
		assertThat(marker.getAttribute(IMarker.SEVERITY, 0), equalTo(IMarker.SEVERITY_WARNING));
		assertThat(marker.getAttribute(IMarker.LINE_NUMBER, 0), equalTo(14));
		assertThat(marker.getAttribute(IMarker.CHAR_END, 0) - marker.getAttribute(IMarker.CHAR_START, 0), equalTo(28));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(true));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(1));
	}

	@Test
	public void shouldReportProblemOnUnboundPathParamArgument() throws CoreException, ValidationException {
		// pre-conditions
		ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit("ValidationResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "ValidationResource.java");
		replaceFirstOccurrenceOfCode(compilationUnit, "@Path(\"/{id}", "@Path(\"", false);
		final JaxrsResource resource = (JaxrsResource) metamodel.findElement(compilationUnit.findPrimaryType());
		deleteJaxrsMarkers(resource);
		metamodelMonitor.resetElementChangesNotifications();
		// operation: remove the @PathParam, so that some @Path value has no
		// counterpart
		new JaxrsMetamodelValidator().validate(toSet(compilationUnit.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(resource);
		// verification
		assertThat(markers.length, equalTo(1));
		final IMarker marker = markers[0];
		assertThat(marker.getAttribute(IMarker.SEVERITY, 0), equalTo(IMarker.SEVERITY_ERROR));
		assertThat(marker.getAttribute(IMarker.LINE_NUMBER, 0), equalTo(15));
		assertThat(marker.getAttribute(IMarker.CHAR_END, 0) - marker.getAttribute(IMarker.CHAR_START, 0), equalTo(4));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(true));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(1));
	}

	@Test
	@Ignore
	public void shouldReportWarningIfNoProviderExists() throws CoreException, ValidationException {
		fail("Not implemented yet");
	}

	@Test
	public void shouldReportErrorWhenUnauthorizedContextAnnotationOnJavaMethodParameters() throws CoreException,
			ValidationException {
		// pre-conditions
		ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit("ValidationResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "ValidationResource.java");
		replaceFirstOccurrenceOfCode(compilationUnit, "@QueryParam(\"start\")", "@Context", false);
		final JaxrsResource resource = (JaxrsResource) metamodel.findElement(compilationUnit.findPrimaryType());
		deleteJaxrsMarkers(resource);
		metamodelMonitor.resetElementChangesNotifications();
		// operation: remove the @PathParam, so that some @Path value has no
		// counterpart
		new JaxrsMetamodelValidator().validate(toSet(compilationUnit.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(resource);
		// verification
		assertThat(markers.length, equalTo(1));
		final IMarker marker = markers[0];
		assertThat(marker.getAttribute(IMarker.SEVERITY, 0), equalTo(IMarker.SEVERITY_ERROR));
		assertThat(marker.getAttribute(IMarker.LINE_NUMBER, 0), equalTo(19));
		assertThat(marker.getAttribute(IMarker.CHAR_END, 0) - marker.getAttribute(IMarker.CHAR_START, 0), equalTo(8));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(true));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(1));
	}

	@Test
	public void shouldIncreaseAndResetProblemLevelOnResourceMethod() throws CoreException, ValidationException {
		// pre-conditions
		ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit("ValidationResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "ValidationResource.java");
		replaceFirstOccurrenceOfCode(compilationUnit, "@PathParam(\"id\")", "@PathParam(\"ide\")", false);
		final JaxrsResource resource = (JaxrsResource) metamodel.findElement(compilationUnit.findPrimaryType());
		deleteJaxrsMarkers(resource);
		metamodelMonitor.resetElementChangesNotifications();
		// operation: remove the @PathParam, so that some @Path value has no
		// counterpart
		new JaxrsMetamodelValidator().validate(toSet(compilationUnit.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		// verification: problem level is set to '2'
		assertThat(resource.getAllMethods().get(0).getProblemLevel(), equalTo(2));
		// now, fix the problem
		replaceFirstOccurrenceOfCode(compilationUnit, "@PathParam(\"ide\")", "@PathParam(\"id\")", false);
		// revalidate
		new JaxrsMetamodelValidator().validate(toSet(compilationUnit.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		// verification: problem level is set to '0'
		assertThat(resource.getAllMethods().get(0).getProblemLevel(), equalTo(0));
	}

	@Test
	public void shouldValidateCustomerResourceMethodWithDotCharacterInPathParam() throws CoreException,
			ValidationException {
		// preconditions
		final IType customerJavaType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		replaceAllOccurrencesOfCode(customerJavaType, "@Path(\"{id}\")", "@Path(\"{i.d}\")", false);
		replaceAllOccurrencesOfCode(customerJavaType, "@PathParam(\"id\")", "@PathParam(\"i.d\")", false);
		final AbstractJaxrsBaseElement customerResource = (AbstractJaxrsBaseElement) metamodel
				.findElement(customerJavaType);
		deleteJaxrsMarkers(customerResource);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(customerResource.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(customerResource);
		assertThat(markers.length, equalTo(0));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(0));
	}

	@Test
	public void shouldReportProblemOnCustomerResourceMethodWithDotCharacterInPathParamInFirstPlace()
			throws CoreException, ValidationException {
		// preconditions
		final IType customerJavaType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		replaceAllOccurrencesOfCode(customerJavaType, "@Path(\"{id}\")", "@Path(\"{.id}\")", false);
		replaceAllOccurrencesOfCode(customerJavaType, "@PathParam(\"id\")", "@PathParam(\".id\")", false);
		final AbstractJaxrsBaseElement customerResource = (AbstractJaxrsBaseElement) metamodel
				.findElement(customerJavaType);
		deleteJaxrsMarkers(customerResource);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(customerResource.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(customerResource);
		assertThat(markers.length, equalTo(4));
		for (IMarker marker : markers) {
			assertThat((String) marker.getType(), equalTo(JaxrsMetamodelValidator.JAXRS_PROBLEM_MARKER_ID));
			assertThat((String) marker.getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE),
					equalTo(JaxrsPreferences.RESOURCE_METHOD_INVALID_PATHPARAM_ANNOTATION_VALUE));
		}
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(1));
	}

	@Test
	public void shouldValidateCustomerResourceMethodWithHyphenCharacterInPathParam() throws CoreException,
			ValidationException {
		// preconditions
		final IType customerJavaType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		replaceAllOccurrencesOfCode(customerJavaType, "@Path(\"{id}\")", "@Path(\"{i-d}\")", false);
		replaceAllOccurrencesOfCode(customerJavaType, "@PathParam(\"id\")", "@PathParam(\"i-d\")", false);
		final AbstractJaxrsBaseElement customerResource = (AbstractJaxrsBaseElement) metamodel
				.findElement(customerJavaType);
		deleteJaxrsMarkers(customerResource);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(customerResource.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(customerResource);
		assertThat(markers.length, equalTo(0));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(0));
	}

	@Test
	public void shouldReportProblemOnCustomerResourceMethodWithHyphenCharacterInPathParamInFirstPlace()
			throws CoreException, ValidationException {
		// preconditions
		final IType customerJavaType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		replaceAllOccurrencesOfCode(customerJavaType, "@Path(\"{id}\")", "@Path(\"{-id}\")", false);
		replaceAllOccurrencesOfCode(customerJavaType, "@PathParam(\"id\")", "@PathParam(\"-id\")", false);
		final AbstractJaxrsBaseElement customerResource = (AbstractJaxrsBaseElement) metamodel
				.findElement(customerJavaType);
		deleteJaxrsMarkers(customerResource);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(customerResource.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(customerResource);
		assertThat(markers.length, equalTo(4));
		for (IMarker marker : markers) {
			assertThat((String) marker.getType(), equalTo(JaxrsMetamodelValidator.JAXRS_PROBLEM_MARKER_ID));
			assertThat((String) marker.getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE),
					equalTo(JaxrsPreferences.RESOURCE_METHOD_INVALID_PATHPARAM_ANNOTATION_VALUE));
		}
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(1));
	}

	@Test
	public void shouldValidateCustomerResourceMethodWithUnderscoreCharacterInPathParam() throws CoreException,
			ValidationException {
		// preconditions
		final IType customerJavaType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		replaceAllOccurrencesOfCode(customerJavaType, "@Path(\"{id}\")", "@Path(\"{i_d}\")", false);
		replaceAllOccurrencesOfCode(customerJavaType, "@PathParam(\"id\")", "@PathParam(\"i_d\")", false);
		final AbstractJaxrsBaseElement customerResource = (AbstractJaxrsBaseElement) metamodel
				.findElement(customerJavaType);
		deleteJaxrsMarkers(customerResource);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(customerResource.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(customerResource);
		assertThat(markers.length, equalTo(0));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(0));
	}

	@Test
	public void shouldReportProblemOnCustomerResourceMethodWithUnderscoreCharacterInPathParamInFirstPlace()
			throws CoreException, ValidationException {
		// preconditions
		final IType customerJavaType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		replaceAllOccurrencesOfCode(customerJavaType, "@Path(\"{id}\")", "@Path(\"{_id}\")", false);
		replaceAllOccurrencesOfCode(customerJavaType, "@PathParam(\"id\")", "@PathParam(\"_id\")", false);
		final AbstractJaxrsBaseElement customerResource = (AbstractJaxrsBaseElement) metamodel
				.findElement(customerJavaType);
		deleteJaxrsMarkers(customerResource);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(customerResource.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(customerResource);
		assertThat(markers.length, equalTo(4));
		for (IMarker marker : markers) {
			assertThat((String) marker.getType(), equalTo(JaxrsMetamodelValidator.JAXRS_PROBLEM_MARKER_ID));
			assertThat((String) marker.getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE),
					equalTo(JaxrsPreferences.RESOURCE_METHOD_INVALID_PATHPARAM_ANNOTATION_VALUE));
		}
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(1));
	}

	@Test
	public void shouldReportProblemOnCustomerResourceMethodWithAtCharacterInPathParam() throws CoreException,
			ValidationException {
		// preconditions
		final IType customerJavaType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		replaceAllOccurrencesOfCode(customerJavaType, "@Path(\"{id}\")", "@Path(\"{i@d}\")", false);
		replaceAllOccurrencesOfCode(customerJavaType, "@PathParam(\"id\")", "@PathParam(\"i@d\")", false);
		final AbstractJaxrsBaseElement customerResource = (AbstractJaxrsBaseElement) metamodel
				.findElement(customerJavaType);
		deleteJaxrsMarkers(customerResource);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(customerResource.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(customerResource);
		assertThat(markers.length, equalTo(4));
		for (IMarker marker : markers) {
			assertThat((String) marker.getType(), equalTo(JaxrsMetamodelValidator.JAXRS_PROBLEM_MARKER_ID));
			assertThat((String) marker.getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE),
					equalTo(JaxrsPreferences.RESOURCE_METHOD_INVALID_PATHPARAM_ANNOTATION_VALUE));
		}
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(1));
	}

	@Test
	public void shouldReportProblemOnCustomerResourceMethodWithSingleCharacterInPathParam() throws CoreException,
			ValidationException {
		// preconditions
		final IType customerJavaType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		replaceAllOccurrencesOfCode(customerJavaType, "@Path(\"{id}\")", "@Path(\"{i}\")", false);
		replaceAllOccurrencesOfCode(customerJavaType, "@PathParam(\"id\")", "@PathParam(\"i\")", false);
		final AbstractJaxrsBaseElement customerResource = (AbstractJaxrsBaseElement) metamodel
				.findElement(customerJavaType);
		deleteJaxrsMarkers(customerResource);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(customerResource.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(customerResource);
		assertThat(markers.length, equalTo(0));
	}

	@Test
	public void shouldValidateMethodWithPathParamBoundToField() throws CoreException, ValidationException {
		// pre-conditions
		final ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit("BoatResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "BoatResource.java");
		final JaxrsResource carResource = metamodelMonitor.createResource(compilationUnit.findPrimaryType());
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(carResource.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(carResource);
		assertThat(markers.length, equalTo(0));
	}

	@Test
	public void shouldValidateMethodParams() throws CoreException, ValidationException {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("Car.txt", "org.jboss.tools.ws.jaxrs.sample.services", "Car.java");
		metamodelMonitor.createCompilationUnit("CarValueOf.txt", "org.jboss.tools.ws.jaxrs.sample.services",
				"CarValueOf.java");
		metamodelMonitor.createCompilationUnit("CarFromString.txt", "org.jboss.tools.ws.jaxrs.sample.services",
				"CarFromString.java");
		final ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit("CarResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "CarResource.java");
		final JaxrsResource carResource = metamodelMonitor.createResource(compilationUnit.findPrimaryType());
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(carResource.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(carResource);
		assertThat(markers.length, equalTo(0));
	}

	@Test
	public void shouldValidateFieldParams() throws CoreException, ValidationException {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("Car.txt", "org.jboss.tools.ws.jaxrs.sample.services", "Car.java");
		metamodelMonitor.createCompilationUnit("CarValueOf.txt", "org.jboss.tools.ws.jaxrs.sample.services",
				"CarValueOf.java");
		metamodelMonitor.createCompilationUnit("CarFromString.txt", "org.jboss.tools.ws.jaxrs.sample.services",
				"CarFromString.java");
		final ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit("CarResourceWithFields.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "CarResource.java");
		final JaxrsResource carResource = metamodelMonitor.createResource(compilationUnit.findPrimaryType());
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(carResource.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(carResource);
		assertThat(markers.length, equalTo(0));
	}

	@Test
	public void shouldReportProblemsOnAllMethodParams() throws CoreException, ValidationException {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("Truck.txt", "org.jboss.tools.ws.jaxrs.sample.services", "Truck.java");
		final ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit("TruckResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "TruckResource.java");
		final JaxrsResource carResource = metamodelMonitor.createResource(compilationUnit.findPrimaryType());
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(carResource.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(carResource);
		assertThat(markers.length, equalTo(6));
		for (IMarker marker : markers) {
			assertThat((String) marker.getType(), equalTo(JaxrsMetamodelValidator.JAXRS_PROBLEM_MARKER_ID));
			assertThat((String) marker.getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE),
					equalTo(JaxrsPreferences.RESOURCE_METHOD_INVALID_ANNOTATED_PARAMETER_TYPE));
		}
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(1));
	}

	@Test
	public void shouldReportProblemsOnAllFieldParams() throws CoreException, ValidationException {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("Truck.txt", "org.jboss.tools.ws.jaxrs.sample.services", "Truck.java");
		final ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit("TruckResourceWithFields.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "TruckResource.java");
		final JaxrsResource carResource = metamodelMonitor.createResource(compilationUnit.findPrimaryType());
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(carResource.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(carResource);
		assertThat(markers.length, equalTo(6));
		for (IMarker marker : markers) {
			assertThat((String) marker.getType(), equalTo(JaxrsMetamodelValidator.JAXRS_PROBLEM_MARKER_ID));
			assertThat((String) marker.getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE),
					equalTo(JaxrsPreferences.RESOURCE_METHOD_INVALID_ANNOTATED_PARAMETER_TYPE));
		}
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(1));
	}
	
	@Test
	public void shouldRemoveProblemsOnMethodParams() throws CoreException, ValidationException, IOException {
		// pre-conditions
		final ICompilationUnit truckCompilationUnit = metamodelMonitor.createCompilationUnit("Truck.txt", "org.jboss.tools.ws.jaxrs.sample.services", "Truck.java");
		final ICompilationUnit truckResourceCompilationUnit = metamodelMonitor.createCompilationUnit("TruckResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "TruckResource.java");
		final JaxrsResource truckResource = metamodelMonitor.createResource(truckResourceCompilationUnit.findPrimaryType());
		metamodelMonitor.resetElementChangesNotifications();
		// operation 1 : first validation
		new JaxrsMetamodelValidator().validate(toSet(truckResource.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation 1: the JAX-RS resource methods have errors
		final IMarker[] markers = findJaxrsMarkers(truckResource);
		assertThat(markers.length, equalTo(6));
		for (IMarker marker : markers) {
			assertThat((String) marker.getType(), equalTo(JaxrsMetamodelValidator.JAXRS_PROBLEM_MARKER_ID));
			assertThat((String) marker.getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE),
					equalTo(JaxrsPreferences.RESOURCE_METHOD_INVALID_ANNOTATED_PARAMETER_TYPE));
		}
		// operation 2: now, let's update the 'Truck' class to fix the problem, by adding a 'valueOf(String)' method, and let's replace all 'ArrayList' with 'List'
		ResourcesUtils.replaceContent(truckCompilationUnit.getResource(), "public Truck valueOf(String value)", "public static Truck valueOf(String value)");
		ResourcesUtils.replaceContent(truckResourceCompilationUnit.getResource(), "ArrayList<String>", "List<String>");
		// validate the 'Truck' domain class, not the JAX-RS 'TruckResource', since this one did not change during the operation above
		new JaxrsMetamodelValidator().validate(toSet(truckCompilationUnit.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation 2: the JAX-RS resource methods errors are gone \o/
		final IMarker[] updatedMarkers = findJaxrsMarkers(truckResource);
		assertThat(updatedMarkers.length, equalTo(0));
	}
	
}
