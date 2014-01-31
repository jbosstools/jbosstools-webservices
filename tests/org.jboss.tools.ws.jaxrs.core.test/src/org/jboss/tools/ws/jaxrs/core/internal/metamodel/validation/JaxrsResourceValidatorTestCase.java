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
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.validation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.validation.ValidationUtils.deleteJaxrsMarkers;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.validation.ValidationUtils.findJaxrsMarkers;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.validation.ValidationUtils.hasPreferenceKey;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.validation.ValidationUtils.toSet;
import static org.jboss.tools.ws.jaxrs.core.preferences.JaxrsPreferences.RESOURCE_METHOD_UNBOUND_PATH_ANNOTATION_TEMPLATE_PARAMETER;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IMarker;
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
import org.jboss.tools.ws.jaxrs.core.WorkbenchUtils;
import org.jboss.tools.ws.jaxrs.core.builder.AbstractMetamodelBuilderTestCase;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsBaseElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.preferences.JaxrsPreferences;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
/**
 * @author Xi
 * 
 */
@SuppressWarnings("restriction")
public class JaxrsResourceValidatorTestCase extends AbstractMetamodelBuilderTestCase {

	private final IReporter reporter = new ReporterHelper(new NullProgressMonitor());
	private final ContextValidationHelper validationHelper = new ContextValidationHelper();
	private final IProjectValidationContext context = new ProjectValidationContext();
	private final ValidatorManager validatorManager = new ValidatorManager();

	@Before
	public void removeExtraJaxrsJavaApplications() throws CoreException {
		removeApplications(metamodel.getJavaApplications());
	}

	@Test
	public void shouldValidateCustomerResourceMethod() throws CoreException, ValidationException {
		// preconditions
		final IType customerJavaType = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsBaseElement customerResource = (JaxrsBaseElement) metamodel.findElement(customerJavaType);
		deleteJaxrsMarkers(customerResource);
		resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(customerResource.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(customerResource);
		assertThat(markers.length, equalTo(0));
		assertThat(metamodelProblemLevelChanges.size(), is(0));
	}

	@Test
	public void shouldReportProblemsOnBarResourceMethods() throws CoreException, ValidationException {
		// preconditions
		final IType barJavaType = getType("org.jboss.tools.ws.jaxrs.sample.services.BarResource");
		final JaxrsResource barResource = metamodel.findResource(barJavaType);
		deleteJaxrsMarkers(barResource);
		resetElementChangesNotifications();
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
		assertThat(metamodelProblemLevelChanges.contains(metamodel), is(true));
		assertThat(metamodelProblemLevelChanges.size(), is(1));
	}

	@Test
	public void shouldReportProblemsOnBazResourceMethods() throws CoreException, ValidationException {
		// preconditions
		final IType barJavaType = getType("org.jboss.tools.ws.jaxrs.sample.services.BazResource");
		final JaxrsResource barResource = (JaxrsResource) metamodel.findElement(barJavaType);
		deleteJaxrsMarkers(barResource);
		resetElementChangesNotifications();
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
		assertThat(metamodelProblemLevelChanges.contains(metamodel), is(true));
		assertThat(metamodelProblemLevelChanges.size(), is(1));
	}

	@Test
	public void shouldNotReportProblemOnValidResource() throws CoreException, ValidationException {
		// pre-conditions
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject, "ValidationResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "ValidationResource.java");
		final JaxrsResource resource = (JaxrsResource) metamodel.findElement(compilationUnit.findPrimaryType());
		deleteJaxrsMarkers(resource);
		resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(compilationUnit.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(resource);
		// verification
		assertThat(markers.length, equalTo(0));
		assertThat(metamodelProblemLevelChanges.size(), is(0));
	}

	@Test
	public void shouldReportProblemOnNonPublicJavaMethod() throws CoreException, ValidationException {
		// pre-conditions
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject, "ValidationResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "ValidationResource.java");
		WorkbenchUtils.replaceFirstOccurrenceOfCode(compilationUnit, "public Response", "private Response", false);
		final JaxrsResource resource = (JaxrsResource) metamodel.findElement(compilationUnit.findPrimaryType());
		deleteJaxrsMarkers(resource);
		resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(compilationUnit.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(resource);
		// verification
		assertThat(markers.length, equalTo(1));
		assertThat(markers[0].getAttribute(IMarker.LINE_NUMBER, 0), equalTo(15));
		assertThat(metamodelProblemLevelChanges.contains(metamodel), is(true));
		assertThat(metamodelProblemLevelChanges.size(), is(1));
	}

	@Test
	public void shouldReportProblemOnUnboundTypePathArgument() throws ValidationException, CoreException {
		// pre-conditions
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject, "ValidationResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "ValidationResource.java");
		WorkbenchUtils.removeFirstOccurrenceOfCode(compilationUnit, "@PathParam(\"type\") String type,", false);
		final JaxrsResource resource = (JaxrsResource) metamodel.findElement(compilationUnit.findPrimaryType());
		deleteJaxrsMarkers(resource);
		resetElementChangesNotifications();
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
		assertThat(metamodelProblemLevelChanges.contains(metamodel), is(true));
		assertThat(metamodelProblemLevelChanges.size(), is(1));
	}

	@Test
	public void shouldReportProblemOnUnboundMethodPathArgument() throws ValidationException, CoreException {
		// pre-conditions
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject, "ValidationResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "ValidationResource.java");
		WorkbenchUtils.removeFirstOccurrenceOfCode(compilationUnit, "@PathParam(\"format\") String format,", false);
		final JaxrsResource resource = (JaxrsResource) metamodel.findElement(compilationUnit.findPrimaryType());
		deleteJaxrsMarkers(resource);
		resetElementChangesNotifications();
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
		assertThat(metamodelProblemLevelChanges.contains(metamodel), is(true));
		assertThat(metamodelProblemLevelChanges.size(), is(1));
	}

	@Test
	public void shouldReportProblemOnUnboundMethodPathArgumentWithWhitespaces() throws ValidationException,
			CoreException {
		// pre-conditions
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject, "ValidationResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "ValidationResource.java");
		WorkbenchUtils.removeFirstOccurrenceOfCode(compilationUnit, "@PathParam(\"format\") String format,", false);
		WorkbenchUtils.replaceFirstOccurrenceOfCode(compilationUnit, "{format", "{  format", false);
		final JaxrsResource resource = (JaxrsResource) metamodel.findElement(compilationUnit.findPrimaryType());
		deleteJaxrsMarkers(resource);
		resetElementChangesNotifications();
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
		assertThat(metamodelProblemLevelChanges.contains(metamodel), is(true));
		assertThat(metamodelProblemLevelChanges.size(), is(1));
	}

	@Test
	public void shouldReportProblemOnUnboundPathParamArgument() throws CoreException, ValidationException {
		// pre-conditions
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject, "ValidationResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "ValidationResource.java");
		WorkbenchUtils.replaceFirstOccurrenceOfCode(compilationUnit, "@Path(\"/{id}", "@Path(\"", false);
		final JaxrsResource resource = (JaxrsResource) metamodel.findElement(compilationUnit.findPrimaryType());
		deleteJaxrsMarkers(resource);
		resetElementChangesNotifications();
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
		assertThat(metamodelProblemLevelChanges.contains(metamodel), is(true));
		assertThat(metamodelProblemLevelChanges.size(), is(1));
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
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject, "ValidationResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "ValidationResource.java");
		WorkbenchUtils.replaceFirstOccurrenceOfCode(compilationUnit, "@QueryParam(\"start\")", "@Context", false);
		final JaxrsResource resource = (JaxrsResource) metamodel.findElement(compilationUnit.findPrimaryType());
		deleteJaxrsMarkers(resource);
		resetElementChangesNotifications();
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
		assertThat(metamodelProblemLevelChanges.contains(metamodel), is(true));
		assertThat(metamodelProblemLevelChanges.size(), is(1));
	}

	@Test
	public void shouldIncreaseAndResetProblemLevelOnResourceMethod() throws CoreException, ValidationException {
		// pre-conditions
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject, "ValidationResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "ValidationResource.java");
		WorkbenchUtils.replaceFirstOccurrenceOfCode(compilationUnit, "@PathParam(\"id\")", "@PathParam(\"ide\")", false);
		final JaxrsResource resource = (JaxrsResource) metamodel.findElement(compilationUnit.findPrimaryType());
		deleteJaxrsMarkers(resource);
		resetElementChangesNotifications();
		// operation: remove the @PathParam, so that some @Path value has no
		// counterpart
		new JaxrsMetamodelValidator().validate(toSet(compilationUnit.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		// verification: problem level is set to '2'
		assertThat(resource.getAllMethods().get(0).getProblemLevel(), equalTo(2));
		// now, fix the problem 
		WorkbenchUtils.replaceFirstOccurrenceOfCode(compilationUnit, "@PathParam(\"ide\")", "@PathParam(\"id\")", false);
		// revalidate
		new JaxrsMetamodelValidator().validate(toSet(compilationUnit.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		// verification: problem level is set to '0'
		assertThat(resource.getAllMethods().get(0).getProblemLevel(), equalTo(0));
	}
	
	@Test
	public void shouldValidateCustomerResourceMethodWithDotCharacterInPathParam() throws CoreException, ValidationException {
		// preconditions
		final IType customerJavaType = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		WorkbenchUtils.replaceAllOccurrencesOfCode(customerJavaType, "@Path(\"{id}\")", "@Path(\"{i.d}\")", false);
		WorkbenchUtils.replaceAllOccurrencesOfCode(customerJavaType, "@PathParam(\"id\")", "@PathParam(\"i.d\")", false);
		final JaxrsBaseElement customerResource = (JaxrsBaseElement) metamodel.findElement(customerJavaType);
		deleteJaxrsMarkers(customerResource);
		resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(customerResource.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(customerResource);
		assertThat(markers.length, equalTo(0));
		assertThat(metamodelProblemLevelChanges.size(), is(0));
	}

	@Test
	public void shouldNotValidateCustomerResourceMethodWithDotCharacterInPathParamInFirstPlace() throws CoreException, ValidationException {
		// preconditions
		final IType customerJavaType = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		WorkbenchUtils.replaceAllOccurrencesOfCode(customerJavaType, "@Path(\"{id}\")", "@Path(\"{.id}\")", false);
		WorkbenchUtils.replaceAllOccurrencesOfCode(customerJavaType, "@PathParam(\"id\")", "@PathParam(\".id\")", false);
		final JaxrsBaseElement customerResource = (JaxrsBaseElement) metamodel.findElement(customerJavaType);
		deleteJaxrsMarkers(customerResource);
		resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(customerResource.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(customerResource);
		assertThat(markers.length, equalTo(4));
		for(IMarker marker : markers) {
			assertThat((String)marker.getType(), equalTo(JaxrsMetamodelValidator.JAXRS_PROBLEM_MARKER_ID));
			assertThat((String)marker.getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE), equalTo(JaxrsPreferences.RESOURCE_METHOD_INVALID_PATHPARAM_ANNOTATION_VALUE));
		}
		assertThat(metamodelProblemLevelChanges.size(), is(1));
	}
	
	@Test
	public void shouldValidateCustomerResourceMethodWithHyphenCharacterInPathParam() throws CoreException, ValidationException {
		// preconditions
		final IType customerJavaType = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		WorkbenchUtils.replaceAllOccurrencesOfCode(customerJavaType, "@Path(\"{id}\")", "@Path(\"{i-d}\")", false);
		WorkbenchUtils.replaceAllOccurrencesOfCode(customerJavaType, "@PathParam(\"id\")", "@PathParam(\"i-d\")", false);
		final JaxrsBaseElement customerResource = (JaxrsBaseElement) metamodel.findElement(customerJavaType);
		deleteJaxrsMarkers(customerResource);
		resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(customerResource.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(customerResource);
		assertThat(markers.length, equalTo(0));
		assertThat(metamodelProblemLevelChanges.size(), is(0));
	}
	
	@Test
	public void shouldNotValidateCustomerResourceMethodWithHyphenCharacterInPathParamInFirstPlace() throws CoreException, ValidationException {
		// preconditions
		final IType customerJavaType = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		WorkbenchUtils.replaceAllOccurrencesOfCode(customerJavaType, "@Path(\"{id}\")", "@Path(\"{-id}\")", false);
		WorkbenchUtils.replaceAllOccurrencesOfCode(customerJavaType, "@PathParam(\"id\")", "@PathParam(\"-id\")", false);
		final JaxrsBaseElement customerResource = (JaxrsBaseElement) metamodel.findElement(customerJavaType);
		deleteJaxrsMarkers(customerResource);
		resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(customerResource.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(customerResource);
		assertThat(markers.length, equalTo(4));
		for(IMarker marker : markers) {
			assertThat((String)marker.getType(), equalTo(JaxrsMetamodelValidator.JAXRS_PROBLEM_MARKER_ID));
			assertThat((String)marker.getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE), equalTo(JaxrsPreferences.RESOURCE_METHOD_INVALID_PATHPARAM_ANNOTATION_VALUE));
		}
		assertThat(metamodelProblemLevelChanges.size(), is(1));
	}
	


	@Test
	public void shouldValidateCustomerResourceMethodWithUnderscoreCharacterInPathParam() throws CoreException, ValidationException {
		// preconditions
		final IType customerJavaType = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		WorkbenchUtils.replaceAllOccurrencesOfCode(customerJavaType, "@Path(\"{id}\")", "@Path(\"{i_d}\")", false);
		WorkbenchUtils.replaceAllOccurrencesOfCode(customerJavaType, "@PathParam(\"id\")", "@PathParam(\"i_d\")", false);
		final JaxrsBaseElement customerResource = (JaxrsBaseElement) metamodel.findElement(customerJavaType);
		deleteJaxrsMarkers(customerResource);
		resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(customerResource.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(customerResource);
		assertThat(markers.length, equalTo(0));
		assertThat(metamodelProblemLevelChanges.size(), is(0));
	}
	
	@Test
	public void shouldNotValidateCustomerResourceMethodWithUnderscoreCharacterInPathParamInFirstPlace() throws CoreException, ValidationException {
		// preconditions
		final IType customerJavaType = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		WorkbenchUtils.replaceAllOccurrencesOfCode(customerJavaType, "@Path(\"{id}\")", "@Path(\"{_id}\")", false);
		WorkbenchUtils.replaceAllOccurrencesOfCode(customerJavaType, "@PathParam(\"id\")", "@PathParam(\"_id\")", false);
		final JaxrsBaseElement customerResource = (JaxrsBaseElement) metamodel.findElement(customerJavaType);
		deleteJaxrsMarkers(customerResource);
		resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(customerResource.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(customerResource);
		assertThat(markers.length, equalTo(4));
		for(IMarker marker : markers) {
			assertThat((String)marker.getType(), equalTo(JaxrsMetamodelValidator.JAXRS_PROBLEM_MARKER_ID));
			assertThat((String)marker.getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE), equalTo(JaxrsPreferences.RESOURCE_METHOD_INVALID_PATHPARAM_ANNOTATION_VALUE));
		}
		assertThat(metamodelProblemLevelChanges.size(), is(1));
	}
	


	@Test
	public void shouldNotValidateCustomerResourceMethodWithAtCharacterInPathParam() throws CoreException, ValidationException {
		// preconditions
		final IType customerJavaType = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		WorkbenchUtils.replaceAllOccurrencesOfCode(customerJavaType, "@Path(\"{id}\")", "@Path(\"{i@d}\")", false);
		WorkbenchUtils.replaceAllOccurrencesOfCode(customerJavaType, "@PathParam(\"id\")", "@PathParam(\"i@d\")", false);
		final JaxrsBaseElement customerResource = (JaxrsBaseElement) metamodel.findElement(customerJavaType);
		deleteJaxrsMarkers(customerResource);
		resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(customerResource.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(customerResource);
		assertThat(markers.length, equalTo(4));
		for(IMarker marker : markers) {
			assertThat((String)marker.getType(), equalTo(JaxrsMetamodelValidator.JAXRS_PROBLEM_MARKER_ID));
			assertThat((String)marker.getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE), equalTo(JaxrsPreferences.RESOURCE_METHOD_INVALID_PATHPARAM_ANNOTATION_VALUE));
		}
		assertThat(metamodelProblemLevelChanges.size(), is(1));
	}

	@Test
	public void shouldNotValidateCustomerResourceMethodWithSingleCharacterInPathParam() throws CoreException, ValidationException {
		// preconditions
		final IType customerJavaType = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		WorkbenchUtils.replaceAllOccurrencesOfCode(customerJavaType, "@Path(\"{id}\")", "@Path(\"{i}\")", false);
		WorkbenchUtils.replaceAllOccurrencesOfCode(customerJavaType, "@PathParam(\"id\")", "@PathParam(\"i\")", false);
		final JaxrsBaseElement customerResource = (JaxrsBaseElement) metamodel.findElement(customerJavaType);
		deleteJaxrsMarkers(customerResource);
		resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(customerResource.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(customerResource);
		assertThat(markers.length, equalTo(0));
	}

}
