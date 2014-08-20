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
import static org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils.removeFirstOccurrenceOfCode;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils.replaceFirstOccurrenceOfCode;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsElementsUtils.replaceFirstOccurrenceOfCode;
import static org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils.replaceAllOccurrencesOfCode;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.deleteJaxrsMarkers;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.findJaxrsMarkers;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.findJaxrsMessages;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.hasPreferenceKey;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.toSet;
import static org.jboss.tools.ws.jaxrs.ui.preferences.JaxrsPreferences.RESOURCE_METHOD_UNBOUND_PATH_ANNOTATION_TEMPLATE_PARAMETER;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.wst.validation.ReporterHelper;
import org.eclipse.wst.validation.internal.core.ValidationException;
import org.eclipse.wst.validation.internal.provisional.core.IMessage;
import org.eclipse.wst.validation.internal.provisional.core.IReporter;
import org.jboss.tools.common.validation.ContextValidationHelper;
import org.jboss.tools.common.validation.EditorValidationContext;
import org.jboss.tools.common.validation.IProjectValidationContext;
import org.jboss.tools.common.validation.ValidatorManager;
import org.jboss.tools.common.validation.internal.ProjectValidationContext;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsBaseElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils;
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementCategory;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.TestLogger;
import org.jboss.tools.ws.jaxrs.ui.preferences.JaxrsPreferences;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * @author Xi
 * 
 */
@SuppressWarnings("restriction")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class Jaxrs11ResourceValidatorTestCase {

	private final IReporter reporter = new ReporterHelper(new NullProgressMonitor());
	private final ContextValidationHelper validationHelper = new ContextValidationHelper();
	private final IProjectValidationContext context = new ProjectValidationContext();
	private final ValidatorManager validatorManager = new ValidatorManager();

	@ClassRule
	public static WorkspaceSetupRule workspaceSetupRule = new WorkspaceSetupRule(
			"org.jboss.tools.ws.jaxrs.tests.sampleproject");

	@Rule
	public JaxrsMetamodelMonitor metamodelMonitor = new JaxrsMetamodelMonitor(
			"org.jboss.tools.ws.jaxrs.tests.sampleproject", false);

	private JaxrsMetamodel metamodel = null;

	private IProject project = null;
	
	@Before
	public void setup() throws CoreException {
		metamodel = metamodelMonitor.getMetamodel();
		project = metamodel.getProject();
	}

	@Test
	public void shouldValidateCustomerResourceMethod() throws CoreException, ValidationException {
		// preconditions
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResource customerResource = (JaxrsResource) metamodel
				.findElement("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", EnumElementCategory.RESOURCE);
		deleteJaxrsMarkers(customerResource);
		metamodelMonitor.resetElementChangesNotifications();

		validate(customerResource);
		
		// validation
		final IMarker[] markers = findJaxrsMarkers(customerResource);
		assertThat(markers.length, equalTo(0));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(0));
	}

	@Test
	public void shouldReportProblemsOnBarResourceMethods() throws CoreException, ValidationException {
		// preconditions
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.BarResource");
		final JaxrsResource barResource = (JaxrsResource) metamodel
				.findElement("org.jboss.tools.ws.jaxrs.sample.services.BarResource", EnumElementCategory.RESOURCE);
		deleteJaxrsMarkers(barResource);
		metamodelMonitor.resetElementChangesNotifications();

		validate(barResource);
		
		// validation
		final IMarker[] markers = findJaxrsMarkers(barResource);
		for (IMarker marker : markers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(8));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(true));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(1));
	}

	@Test
	public void shouldReportProblemsOnBazResourceMethods() throws CoreException, ValidationException {
		// preconditions
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.BazResource");
		final JaxrsResource bazResource = (JaxrsResource) metamodel
				.findElement("org.jboss.tools.ws.jaxrs.sample.services.BazResource", EnumElementCategory.RESOURCE);
		deleteJaxrsMarkers(bazResource);
		metamodelMonitor.resetElementChangesNotifications();

		validate(bazResource);
		
		// validation
		final IMarker[] markers = findJaxrsMarkers(bazResource);
		assertThat(markers.length, equalTo(6));
		final Map<String, JaxrsResourceMethod> resourceMethods = bazResource.getMethods();
		for (Entry<String, JaxrsResourceMethod> entry : resourceMethods.entrySet()) {
			final IMarker[] methodMarkers = findJaxrsMarkers(entry.getValue());
			if (entry.getKey().contains("getContent1")) {
				assertThat(entry.getValue().getMarkerSeverity(), is(IMarker.SEVERITY_WARNING));
				assertThat(methodMarkers.length, equalTo(IMarker.SEVERITY_WARNING));
				assertThat(methodMarkers, hasPreferenceKey(RESOURCE_METHOD_UNBOUND_PATH_ANNOTATION_TEMPLATE_PARAMETER));
			} else if (entry.getKey().contains("getContent2")) {
				assertThat(entry.getValue().getMarkerSeverity(), is(IMarker.SEVERITY_ERROR));
				assertThat(methodMarkers.length, equalTo(2));
			} else if (entry.getKey().contains("update1")) {
				assertThat(entry.getValue().getMarkerSeverity(), is(IMarker.SEVERITY_ERROR));
				assertThat(methodMarkers.length, equalTo(2));
			} else if (entry.getKey().contains("update2")) {
				assertThat(entry.getValue().getMarkerSeverity(), is(IMarker.SEVERITY_INFO));
				assertThat(methodMarkers.length, equalTo(0));
			} else if (entry.getKey().contains("update3")) {
				assertThat(entry.getValue().getMarkerSeverity(), is(IMarker.SEVERITY_ERROR));
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
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.ValidationResource");
		final JaxrsResource resource = (JaxrsResource) metamodel.findElement(compilationUnit.findPrimaryType());
		deleteJaxrsMarkers(resource);
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation
		new JaxrsMetamodelValidator().validate(toSet(compilationUnit.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		
		// validation
		final IMarker[] markers = findJaxrsMarkers(resource);
		assertThat(markers.length, equalTo(0));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(0));
	}

	@Test
	public void shouldReportProblemOnNonPublicJavaMethodInImplementationClass() throws CoreException,
			ValidationException {
		// pre-conditions
		final ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit("ValidationResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "ValidationResource.java");
		replaceFirstOccurrenceOfCode(compilationUnit, "public Response", "private Response", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.ValidationResource");
		final JaxrsResource resource = (JaxrsResource) metamodel.findElement(compilationUnit.findPrimaryType());
		deleteJaxrsMarkers(resource);
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation
		new JaxrsMetamodelValidator().validate(toSet(compilationUnit.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		
		// validation
		final IMarker[] markers = findJaxrsMarkers(resource);
		assertThat(markers.length, equalTo(1));
		assertThat(markers[0].getAttribute(IMarker.LINE_NUMBER, 0), equalTo(15));
		assertThat(markers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(true));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(1));
	}

	@Test
	public void shouldNotReportProblemOnNonPublicJavaMethodInInterface() throws CoreException, ValidationException {
		// pre-conditions
		ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit("IValidationResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "IValidationResource.java");
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.IValidationResource");
		final JaxrsResource resource = (JaxrsResource) metamodel.findElement(compilationUnit.findPrimaryType());
		deleteJaxrsMarkers(resource);
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation
		new JaxrsMetamodelValidator().validate(toSet(compilationUnit.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		
		// validation
		final IMarker[] markers = findJaxrsMarkers(resource);
		assertThat(markers.length, equalTo(0));
	}

	@Test
	public void shouldReportProblemOnUnboundTypePathArgument() throws ValidationException, CoreException {
		// pre-conditions
		final ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit("ValidationResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "ValidationResource.java");
		removeFirstOccurrenceOfCode(compilationUnit, "@PathParam(\"type\") String type,", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.ValidationResource");
		final JaxrsResource resource = (JaxrsResource) metamodel.findElement(compilationUnit.findPrimaryType());
		deleteJaxrsMarkers(resource);
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation: remove the @PathParam, so that some @Path value has no
		// counterpart
		
		new JaxrsMetamodelValidator().validate(toSet(compilationUnit.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		
		// validation
		final IMarker[] markers = findJaxrsMarkers(resource);
		assertThat(markers.length, equalTo(1));
		final IMarker marker = markers[0];
		assertThat(marker.getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
		assertThat(marker.getAttribute(IMarker.SEVERITY, 0), equalTo(IMarker.SEVERITY_WARNING));
		assertThat(marker.getAttribute(IMarker.LINE_NUMBER, 0), equalTo(10));
		assertThat(marker.getAttribute(IMarker.CHAR_END, 0) - marker.getAttribute(IMarker.CHAR_START, 0), equalTo(6));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(true));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(1));
	}

	@Test
	public void shouldReportProblemOnUnboundMethodPathArgument() throws ValidationException, CoreException {
		// pre-conditions
		final ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit("ValidationResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "ValidationResource.java");
		removeFirstOccurrenceOfCode(compilationUnit, "@PathParam(\"format\") String format,", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.ValidationResource");
		final JaxrsResource resource = (JaxrsResource) metamodel.findElement(compilationUnit.findPrimaryType());
		deleteJaxrsMarkers(resource);
		metamodelMonitor.resetElementChangesNotifications();

		// operation: remove the @PathParam, so that some @Path value has no
		// counterpart
		new JaxrsMetamodelValidator().validate(toSet(compilationUnit.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		
		// validation
		final IMarker[] markers = findJaxrsMarkers(resource);
		assertThat(markers.length, equalTo(1));
		final IMarker marker = markers[0];
		assertThat(marker.getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
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
		final ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit("ValidationResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "ValidationResource.java");
		removeFirstOccurrenceOfCode(compilationUnit, "@PathParam(\"format\") String format,", false);
		replaceFirstOccurrenceOfCode(compilationUnit, "{format", "{  format", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.ValidationResource");
		final JaxrsResource resource = (JaxrsResource) metamodel.findElement(compilationUnit.findPrimaryType());
		deleteJaxrsMarkers(resource);
		metamodelMonitor.resetElementChangesNotifications();

		// operation: remove the @PathParam, so that some @Path value has no
		// counterpart
		new JaxrsMetamodelValidator().validate(toSet(compilationUnit.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		
		// validation
		final IMarker[] markers = findJaxrsMarkers(resource);
		assertThat(markers.length, equalTo(1));
		final IMarker marker = markers[0];
		assertThat(marker.getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
		assertThat(marker.getAttribute(IMarker.SEVERITY, 0), equalTo(IMarker.SEVERITY_WARNING));
		assertThat(marker.getAttribute(IMarker.LINE_NUMBER, 0), equalTo(14));
		assertThat(marker.getAttribute(IMarker.CHAR_END, 0) - marker.getAttribute(IMarker.CHAR_START, 0), equalTo(28));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(true));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(1));
	}

	@Test
	public void shouldReportProblemOnUnboundPathParamArgument() throws CoreException, ValidationException {
		// pre-conditions
		final ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit("ValidationResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "ValidationResource.java");
		replaceFirstOccurrenceOfCode(compilationUnit, "@Path(\"/{id}", "@Path(\"", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.ValidationResource");
		final JaxrsResource resource = (JaxrsResource) metamodel.findElement(compilationUnit.findPrimaryType());
		deleteJaxrsMarkers(resource);
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation: remove the @PathParam, so that some @Path value has no
		// counterpart
		new JaxrsMetamodelValidator().validate(toSet(compilationUnit.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		
		// validation
		final IMarker[] markers = findJaxrsMarkers(resource);
		assertThat(markers.length, equalTo(1));
		final IMarker marker = markers[0];
		assertThat(marker.getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
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
		final ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit("ValidationResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "ValidationResource.java");
		replaceFirstOccurrenceOfCode(compilationUnit, "@QueryParam(\"start\")", "@Context", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.ValidationResource");
		final JaxrsResource resource = (JaxrsResource) metamodel.findElement(compilationUnit.findPrimaryType());
		deleteJaxrsMarkers(resource);
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation: remove the @PathParam, so that some @Path value has no
		// counterpart
		new JaxrsMetamodelValidator().validate(toSet(compilationUnit.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		
		// validation
		final IMarker[] markers = findJaxrsMarkers(resource);
		assertThat(markers.length, equalTo(1));
		final IMarker marker = markers[0];
		assertThat(marker.getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
		assertThat(marker.getAttribute(IMarker.SEVERITY, 0), equalTo(IMarker.SEVERITY_ERROR));
		assertThat(marker.getAttribute(IMarker.LINE_NUMBER, 0), equalTo(19));
		assertThat(marker.getAttribute(IMarker.CHAR_END, 0) - marker.getAttribute(IMarker.CHAR_START, 0), equalTo(8));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(true));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(1));
	}

	@Test
	public void shouldIncreaseAndResetProblemLevelOnResourceMethod() throws CoreException, ValidationException {
		// pre-conditions
		final ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit("ValidationResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "ValidationResource.java");
		replaceFirstOccurrenceOfCode(compilationUnit, "@PathParam(\"id\")", "@PathParam(\"ide\")", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.ValidationResource");
		final JaxrsResource resource = (JaxrsResource) metamodel.findElement(compilationUnit.findPrimaryType());
		deleteJaxrsMarkers(resource);
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation: remove the @PathParam, so that some @Path value has no
		// counterpart
		new JaxrsMetamodelValidator().validate(toSet(compilationUnit.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		
		// verification: problem level is set to '2'
		assertThat(resource.getAllMethods().get(0).getMarkerSeverity(), equalTo(2));
		// now, fix the problem
		replaceFirstOccurrenceOfCode(compilationUnit, "@PathParam(\"ide\")", "@PathParam(\"id\")", false);
		metamodelMonitor.processEvent(compilationUnit, IJavaElementDelta.CHANGED);
		// revalidate
		new JaxrsMetamodelValidator().validate(toSet(compilationUnit.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		// verification: problem level is set to '0'
		assertThat(resource.getAllMethods().get(0).getMarkerSeverity(), equalTo(0));
	}

	@Test
	public void shouldValidateCustomerResourceMethodWithDotCharacterInPathParam() throws CoreException,
			ValidationException {
		// preconditions
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IType customerJavaType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		replaceAllOccurrencesOfCode(customerJavaType, "@Path(\"{id}\")", "@Path(\"{i.d}\")", false);
		replaceAllOccurrencesOfCode(customerJavaType, "@PathParam(\"id\")", "@PathParam(\"i.d\")", false);
		final JaxrsBaseElement customerResource = (JaxrsBaseElement) metamodel
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
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IType customerJavaType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		replaceAllOccurrencesOfCode(customerJavaType, "@Path(\"{id}\")", "@Path(\"{.id}\")", false);
		replaceAllOccurrencesOfCode(customerJavaType, "@PathParam(\"id\")", "@PathParam(\".id\")", false);
		metamodelMonitor.processEvent(customerJavaType, IJavaElementDelta.CHANGED);
		final JaxrsBaseElement customerResource = (JaxrsBaseElement) metamodel
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
			assertThat(marker.getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
			assertThat((String) marker.getType(), equalTo(JaxrsMetamodelValidator.JAXRS_PROBLEM_MARKER_ID));
			assertThat((String) marker.getAttribute(JaxrsMetamodelValidator.PREFERENCE_KEY_ATTRIBUTE_NAME),
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
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsBaseElement customerResource = (JaxrsBaseElement) metamodel
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
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsBaseElement customerResource = (JaxrsBaseElement) metamodel
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
			assertThat(marker.getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
			assertThat((String) marker.getType(), equalTo(JaxrsMetamodelValidator.JAXRS_PROBLEM_MARKER_ID));
			assertThat((String) marker.getAttribute(JaxrsMetamodelValidator.PREFERENCE_KEY_ATTRIBUTE_NAME),
					equalTo(JaxrsPreferences.RESOURCE_METHOD_INVALID_PATHPARAM_ANNOTATION_VALUE));
		}
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(1));
	}

	@Test
	public void shouldValidateCustomerResourceMethodWithUnderscoreCharacterInPathParam() throws CoreException,
			ValidationException {
		// preconditions
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IType customerJavaType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		replaceAllOccurrencesOfCode(customerJavaType, "@Path(\"{id}\")", "@Path(\"{i_d}\")", false);
		replaceAllOccurrencesOfCode(customerJavaType, "@PathParam(\"id\")", "@PathParam(\"i_d\")", false);
		final JaxrsBaseElement customerResource = (JaxrsBaseElement) metamodel
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
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsBaseElement customerResource = (JaxrsBaseElement) metamodel
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
			assertThat(marker.getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
			assertThat((String) marker.getType(), equalTo(JaxrsMetamodelValidator.JAXRS_PROBLEM_MARKER_ID));
			assertThat((String) marker.getAttribute(JaxrsMetamodelValidator.PREFERENCE_KEY_ATTRIBUTE_NAME),
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
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsBaseElement customerResource = (JaxrsBaseElement) metamodel
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
			assertThat(marker.getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
			assertThat((String) marker.getType(), equalTo(JaxrsMetamodelValidator.JAXRS_PROBLEM_MARKER_ID));
			assertThat((String) marker.getAttribute(JaxrsMetamodelValidator.PREFERENCE_KEY_ATTRIBUTE_NAME),
					equalTo(JaxrsPreferences.RESOURCE_METHOD_INVALID_PATHPARAM_ANNOTATION_VALUE));
		}
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(1));
	}

	@Test
	public void shouldReportProblemOnCustomerResourceMethodWithSingleCharacterInPathParam() throws CoreException,
			ValidationException {
		// preconditions
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IType customerJavaType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		replaceAllOccurrencesOfCode(customerJavaType, "@Path(\"{id}\")", "@Path(\"{i}\")", false);
		replaceAllOccurrencesOfCode(customerJavaType, "@PathParam(\"id\")", "@PathParam(\"i\")", false);
		final JaxrsBaseElement customerResource = (JaxrsBaseElement) metamodel
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
		validate(carResource);
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
		validate(carResource);
		// validation
		final IMarker[] markers = findJaxrsMarkers(carResource);
		// 5 markers: missing import/unknown type does not count
				assertThat(markers.length, equalTo(5));
				for (IMarker marker : markers) {
			assertThat(marker.getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
			assertThat((String) marker.getType(), equalTo(JaxrsMetamodelValidator.JAXRS_PROBLEM_MARKER_ID));
			assertThat((String) marker.getAttribute(JaxrsMetamodelValidator.PREFERENCE_KEY_ATTRIBUTE_NAME),
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
		final JaxrsResource truckResource = metamodelMonitor.createResource(compilationUnit.findPrimaryType());
		metamodelMonitor.resetElementChangesNotifications();
		validate(truckResource);
		// validation
		final IMarker[] markers = findJaxrsMarkers(truckResource);
		// 5 markers: missing import/unknown type does not count
		assertThat(markers.length, equalTo(5));
		for (IMarker marker : markers) {
			assertThat(marker.getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
			assertThat((String) marker.getType(), equalTo(JaxrsMetamodelValidator.JAXRS_PROBLEM_MARKER_ID));
			assertThat((String) marker.getAttribute(JaxrsMetamodelValidator.PREFERENCE_KEY_ATTRIBUTE_NAME),
					equalTo(JaxrsPreferences.RESOURCE_METHOD_INVALID_ANNOTATED_PARAMETER_TYPE));
		}
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(1));
	}
	
	@Test
	public void shouldReportProblemsOnAllPropertiesParams() throws CoreException, ValidationException {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("Truck.txt", "org.jboss.tools.ws.jaxrs.sample.services", "Truck.java");
		final ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit("TruckResourceWithProperties.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "TruckResource.java");
		final JaxrsResource truckResource = metamodelMonitor.createResource(compilationUnit.findPrimaryType());
		metamodelMonitor.resetElementChangesNotifications();
		validate(truckResource);
		// validation
		final IMarker[] markers = findJaxrsMarkers(truckResource);
		// 5 markers: missing import/unknown type does not count
		assertThat(markers.length, equalTo(5));
		for (IMarker marker : markers) {
			assertThat(marker.getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
			assertThat((String) marker.getType(), equalTo(JaxrsMetamodelValidator.JAXRS_PROBLEM_MARKER_ID));
			assertThat((String) marker.getAttribute(JaxrsMetamodelValidator.PREFERENCE_KEY_ATTRIBUTE_NAME),
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
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.TruckResource");
		final JaxrsResource truckResource = metamodel.findResource(truckResourceCompilationUnit.findPrimaryType());
		metamodelMonitor.resetElementChangesNotifications();
		
		validate(truckResource);
		
		// validation 1: the JAX-RS resource methods have errors
		final IMarker[] markers = findJaxrsMarkers(truckResource);
		// 5 markers: missing import/unknown type does not count
				assertThat(markers.length, equalTo(5));
				for (IMarker marker : markers) {
			assertThat(marker.getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
			assertThat((String) marker.getType(), equalTo(JaxrsMetamodelValidator.JAXRS_PROBLEM_MARKER_ID));
			assertThat((String) marker.getAttribute(JaxrsMetamodelValidator.PREFERENCE_KEY_ATTRIBUTE_NAME),
					equalTo(JaxrsPreferences.RESOURCE_METHOD_INVALID_ANNOTATED_PARAMETER_TYPE));
		}
		
		// operation 2: now, let's update the 'Truck' class to fix the problem, by adding a 'valueOf(String)' method, and let's replace all 'ArrayList' with 'List'
		ResourcesUtils.replaceContent(truckCompilationUnit.getResource(), "public Truck valueOf(String value)", "public static Truck valueOf(String value)");
		ResourcesUtils.replaceContent(truckResourceCompilationUnit.getResource(), "ArrayList<String>", "List<String>");
		truckResource.update(truckResourceCompilationUnit, JdtUtils.parse(truckResourceCompilationUnit, null));
		
		// validate the 'Truck' domain class, not the JAX-RS 'TruckResource', since this one did not change during the operation above
		new JaxrsMetamodelValidator().validate(toSet(truckCompilationUnit.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		
		// validation 2: the JAX-RS resource methods errors are gone \o/
		final IMarker[] updatedMarkers = findJaxrsMarkers(truckResource);
		assertThat(updatedMarkers.length, equalTo(0));
	}
	
	@Test
	public void shouldReportProblemWhenUnboundPathParamAnnotatedResourceField() throws CoreException, ValidationException {
		final ICompilationUnit boatResourceCompilationUnit = metamodelMonitor.createCompilationUnit("BoatResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "BoatResource.java");
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.BoatResource");
		final JaxrsResource boatResource = metamodel.findResource(boatResourceCompilationUnit.findPrimaryType());
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation: validate
		validate(boatResource);
		
		// verifications
		final IMarker[] markers = findJaxrsMarkers(boatResource);
		assertThat(markers.length, equalTo(1));
		assertThat(markers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
		assertThat((String) markers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE),
				equalTo(JaxrsPreferences.RESOURCE_ELEMENT_UNBOUND_PATHPARAM_ANNOTATION_VALUE));
	}

	/**
	 * @param boatResource
	 * @throws ValidationException
	 */
	public void validate(final JaxrsResource boatResource) throws ValidationException {
		new JaxrsMetamodelValidator().validate(toSet(boatResource.getResource()), project, validationHelper, context,
				validatorManager, reporter);
	}

	@Test
	public void shouldReportProblemWhenUnboundPathParamAnnotatedResourceField_AsYouType() throws CoreException, ValidationException {
		final ICompilationUnit boatResourceCompilationUnit = metamodelMonitor.createCompilationUnit("BoatResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "BoatResource.java");
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "@Path(\"{id}\")", "@Path(\"{type}/{id}\")", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.BoatResource");
		final JaxrsResource boatResource = metamodel.findResource(boatResourceCompilationUnit.findPrimaryType());
		metamodelMonitor.resetElementChangesNotifications();
		final Annotation pathParamAnnotation = boatResource.getField("type").getPathParamAnnotation();
		replaceFirstOccurrenceOfCode(boatResource, "@PathParam(\"type\")", "@PathParam(\"t\")", true);
		final CompilationUnit ast = JdtUtils.parse(boatResourceCompilationUnit, null);
		final ISourceRange annotationValueRange = JdtUtils.resolveMemberPairValueRange(pathParamAnnotation.getJavaAnnotation(), Annotation.VALUE, ast);
		assertThat(boatResourceCompilationUnit.getSource().substring(annotationValueRange.getOffset(), annotationValueRange.getOffset() + annotationValueRange.getLength()), equalTo("\"t\""));
		
		// operation: validate
		final IRegion fieldRegion = new Region(annotationValueRange.getOffset(), annotationValueRange.getLength());
		final IDocument document = new Document(boatResourceCompilationUnit.getSource());
		final EditorValidationContext validationContext = new EditorValidationContext(project, document);
		new JaxrsMetamodelValidator().validate(null, project, Arrays.asList(fieldRegion), validationHelper, reporter, validationContext, null, (IFile)boatResourceCompilationUnit.getResource());
		
		// verifications
		final IMessage[] messages = findJaxrsMessages(reporter, boatResource);
		assertThat(messages.length, equalTo(2));
		assertThat(messages[0].getText(), not(containsString("{")));
		assertThat((String) messages[0].getAttribute(JaxrsMetamodelValidator.PREFERENCE_KEY_ATTRIBUTE_NAME),
				equalTo(JaxrsPreferences.RESOURCE_METHOD_UNBOUND_PATH_ANNOTATION_TEMPLATE_PARAMETER));
		assertThat(messages[1].getText(), not(containsString("{")));
		assertThat((String) messages[1].getAttribute(JaxrsMetamodelValidator.PREFERENCE_KEY_ATTRIBUTE_NAME),
				equalTo(JaxrsPreferences.RESOURCE_ELEMENT_UNBOUND_PATHPARAM_ANNOTATION_VALUE));
	}

	@Test
	public void shouldResolveProblemWhenPathParamAnnotatedResourceFieldBoundToResourceMethodPath_AsYouType() throws CoreException, ValidationException {
		final ICompilationUnit boatResourceCompilationUnit = metamodelMonitor.createCompilationUnit("BoatResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "BoatResource.java");
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "@Path(\"{id}\")", "@Path(\"{type}/{id}\")", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.BoatResource");
		final JaxrsResource boatResource = metamodel.findResource(boatResourceCompilationUnit.findPrimaryType());
		metamodelMonitor.resetElementChangesNotifications();
		final Annotation pathParamAnnotation = boatResource.getField("type").getPathParamAnnotation();
		replaceFirstOccurrenceOfCode(boatResource, "@PathParam(\"type\")", "@PathParam(\"t\")", true);
		final CompilationUnit ast = JdtUtils.parse(boatResourceCompilationUnit, null);
		final ISourceRange annotationValueRange = JdtUtils.resolveMemberPairValueRange(pathParamAnnotation.getJavaAnnotation(), Annotation.VALUE, ast);
		final IDocument document = new Document(boatResourceCompilationUnit.getSource());
		final EditorValidationContext editorValidationContext = new EditorValidationContext(project, document);
		assertThat(boatResourceCompilationUnit.getSource().substring(annotationValueRange.getOffset(), annotationValueRange.getOffset() + annotationValueRange.getLength()), equalTo("\"t\""));
		
		// operation 1: validate
		final IRegion modifiedRegion = new Region(annotationValueRange.getOffset(), annotationValueRange.getLength());
		new JaxrsMetamodelValidator().validate(null, project, Arrays.asList(modifiedRegion), validationHelper, reporter, editorValidationContext, null, (IFile)boatResourceCompilationUnit.getResource());
		
		// verifications 1 : expect 1 problem
		final IMessage[] messages = findJaxrsMessages(reporter, boatResource);
		assertThat(messages.length, equalTo(2));
		assertThat(messages[0].getText(), not(containsString("{")));
		assertThat((String) messages[0].getAttribute(JaxrsMetamodelValidator.PREFERENCE_KEY_ATTRIBUTE_NAME),
				equalTo(JaxrsPreferences.RESOURCE_METHOD_UNBOUND_PATH_ANNOTATION_TEMPLATE_PARAMETER));
		assertThat(messages[1].getText(), not(containsString("{")));
		assertThat((String) messages[1].getAttribute(JaxrsMetamodelValidator.PREFERENCE_KEY_ATTRIBUTE_NAME),
				equalTo(JaxrsPreferences.RESOURCE_ELEMENT_UNBOUND_PATHPARAM_ANNOTATION_VALUE));

		// operation 2: fix the value and revalidate
		replaceFirstOccurrenceOfCode(boatResource, "@PathParam(\"t\")", "@PathParam(\"type\")", false);
		document.set(boatResourceCompilationUnit.getSource());
		final IRegion fixedRegion = new Region(annotationValueRange.getOffset(), annotationValueRange.getLength());
		new JaxrsMetamodelValidator().validate(null, project, Arrays.asList(fixedRegion), validationHelper, reporter, new EditorValidationContext(project, document), null, (IFile)boatResourceCompilationUnit.getResource());
		
		// verifications 2 : expect 0 problem
		final IMessage[] updatedMessages = findJaxrsMessages(reporter, boatResource);
		assertThat(updatedMessages.length, equalTo(0));
	}
	
	@Test
	public void shouldResolveProblemWhenPathParamAnnotatedResourceFieldBoundToResourcePath_AsYouType() throws CoreException, ValidationException {
		final ICompilationUnit boatResourceCompilationUnit = metamodelMonitor.createCompilationUnit("BoatResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "BoatResource.java");
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "@Path(\"/\")", "@Path(\"/{type}\")", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.BoatResource");
		final JaxrsResource boatResource = metamodel.findResource(boatResourceCompilationUnit.findPrimaryType());
		metamodelMonitor.resetElementChangesNotifications();
		final Annotation pathParamAnnotation = boatResource.getField("type").getPathParamAnnotation();
		replaceFirstOccurrenceOfCode(boatResource, "@PathParam(\"type\")", "@PathParam(\"t\")", true);
		final CompilationUnit ast = JdtUtils.parse(boatResourceCompilationUnit, null);
		final ISourceRange annotationValueRange = JdtUtils.resolveMemberPairValueRange(pathParamAnnotation.getJavaAnnotation(), Annotation.VALUE, ast);
		final IDocument document = new Document(boatResourceCompilationUnit.getSource());
		final EditorValidationContext editorValidationContext = new EditorValidationContext(project, document);
		assertThat(boatResourceCompilationUnit.getSource().substring(annotationValueRange.getOffset(), annotationValueRange.getOffset() + annotationValueRange.getLength()), equalTo("\"t\""));
		
		// operation 1: validate
		final IRegion modifiedRegion = new Region(annotationValueRange.getOffset(), annotationValueRange.getLength());
		new JaxrsMetamodelValidator().validate(null, project, Arrays.asList(modifiedRegion), validationHelper, reporter, editorValidationContext, null, (IFile)boatResourceCompilationUnit.getResource());
		
		// verifications 1 : expect 1 problem
		final IMessage[] messages = findJaxrsMessages(reporter, boatResource);
		assertThat(messages.length, equalTo(2));
		assertThat(messages[0].getText(), not(containsString("{")));
		assertThat((String) messages[0].getAttribute(JaxrsMetamodelValidator.PREFERENCE_KEY_ATTRIBUTE_NAME),
				equalTo(JaxrsPreferences.RESOURCE_METHOD_UNBOUND_PATH_ANNOTATION_TEMPLATE_PARAMETER));
		assertThat(messages[1].getText(), not(containsString("{")));
		assertThat((String) messages[1].getAttribute(JaxrsMetamodelValidator.PREFERENCE_KEY_ATTRIBUTE_NAME),
				equalTo(JaxrsPreferences.RESOURCE_ELEMENT_UNBOUND_PATHPARAM_ANNOTATION_VALUE));
		
		// operation 2: fix the value and revalidate
		replaceFirstOccurrenceOfCode(boatResource, "@PathParam(\"t\")", "@PathParam(\"type\")", true);
		document.set(boatResourceCompilationUnit.getSource());
		final IRegion fixedRegion = new Region(annotationValueRange.getOffset(), annotationValueRange.getLength());
		new JaxrsMetamodelValidator().validate(null, project, Arrays.asList(fixedRegion), validationHelper, reporter, new EditorValidationContext(project, document), null, (IFile)boatResourceCompilationUnit.getResource());
		
		// verifications 2 : expect 0 problem
		final IMessage[] updatedMessages = findJaxrsMessages(reporter, boatResource);
		assertThat(updatedMessages.length, equalTo(0));
	}
	
	@Test
	public void shouldNotReportProblemWhenPathParamAnnotatedResourceFieldBoundToPath() throws CoreException, ValidationException {
		final ICompilationUnit boatResourceCompilationUnit = metamodelMonitor.createCompilationUnit("BoatResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "BoatResource.java");
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "@Path(\"{id}\")", "@Path(\"{type}/{id}\")", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.BoatResource");
		final JaxrsResource boatResource = metamodel.findResource(boatResourceCompilationUnit.findPrimaryType());
		metamodelMonitor.resetElementChangesNotifications();
		
		validate(boatResource);
		
		// verifications
		final IMarker[] markers = findJaxrsMarkers(boatResource);
		assertThat(markers.length, equalTo(0));
	}
	
	@Test
	public void shouldReportProblemWhenUnboundPathParamAnnotatedResourceProperty() throws CoreException, ValidationException {
		final ICompilationUnit boatResourceCompilationUnit = metamodelMonitor.createCompilationUnit("BoatResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "BoatResource.java");
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "@PathParam(\"type\") //field", "//@PathParam(\"type\")", false);
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "//@PathParam(\"type\") //property", "@PathParam(\"type\")", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.BoatResource");
		final JaxrsResource boatResource = metamodel.findResource(boatResourceCompilationUnit.findPrimaryType());
		metamodelMonitor.resetElementChangesNotifications();
		
		validate(boatResource);
		
		// verifications
		final IMarker[] markers = findJaxrsMarkers(boatResource);
		assertThat(markers.length, equalTo(1));
		assertThat(markers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
		assertThat((String) markers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE),
				equalTo(JaxrsPreferences.RESOURCE_ELEMENT_UNBOUND_PATHPARAM_ANNOTATION_VALUE));
	}
	
	@Test
	public void shouldReportProblemWhenUnboundPathParamAnnotatedResourceProperty_AsYouType() throws CoreException, ValidationException {
		final ICompilationUnit boatResourceCompilationUnit = metamodelMonitor.createCompilationUnit("BoatResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "BoatResource.java");
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "@PathParam(\"type\") //field", "", false);
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "//@PathParam(\"type\") //property", "@PathParam(\"type\")", false);
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "@Path(\"{id}\")", "@Path(\"{type}/{id}\")", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.BoatResource");
		final JaxrsResource boatResource = metamodel.findResource(boatResourceCompilationUnit.findPrimaryType());
		metamodelMonitor.resetElementChangesNotifications();
		final Annotation pathParamAnnotation = boatResource.getProperty("setType").getPathParamAnnotation();
		replaceFirstOccurrenceOfCode(boatResource, "@PathParam(\"type\")", "@PathParam(\"t\")", true);
		final CompilationUnit ast = JdtUtils.parse(boatResourceCompilationUnit, null);
		final ISourceRange annotationValueRange = JdtUtils.resolveMemberPairValueRange(pathParamAnnotation.getJavaAnnotation(), Annotation.VALUE, ast);
		final IDocument document = new Document(boatResourceCompilationUnit.getSource());
		final EditorValidationContext editorValidationContext = new EditorValidationContext(project, document);
		assertThat(boatResourceCompilationUnit.getSource().substring(annotationValueRange.getOffset(), annotationValueRange.getOffset() + annotationValueRange.getLength()), equalTo("\"t\""));
		
		// operation: validate
		final IRegion propertyRegion = new Region(annotationValueRange.getOffset(), annotationValueRange.getLength());
		new JaxrsMetamodelValidator().validate(null, project, Arrays.asList(propertyRegion), validationHelper, reporter, editorValidationContext, null, (IFile)boatResourceCompilationUnit.getResource());
		
		// verifications
		final IMessage[] messages = findJaxrsMessages(reporter, boatResource);
		assertThat(messages.length, equalTo(2));
		assertThat(messages[0].getText(), not(containsString("{")));
		assertThat((String) messages[0].getAttribute(JaxrsMetamodelValidator.PREFERENCE_KEY_ATTRIBUTE_NAME),
				equalTo(JaxrsPreferences.RESOURCE_METHOD_UNBOUND_PATH_ANNOTATION_TEMPLATE_PARAMETER));
		assertThat(messages[1].getText(), not(containsString("{")));
		assertThat((String) messages[1].getAttribute(JaxrsMetamodelValidator.PREFERENCE_KEY_ATTRIBUTE_NAME),
				equalTo(JaxrsPreferences.RESOURCE_ELEMENT_UNBOUND_PATHPARAM_ANNOTATION_VALUE));
	}
	
	@Test
	public void shouldResolveProblemWhenPathParamAnnotatedResourcePropertyBoundToResourceMethodPath_AsYouType() throws CoreException, ValidationException {
		final ICompilationUnit boatResourceCompilationUnit = metamodelMonitor.createCompilationUnit("BoatResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "BoatResource.java");
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "@Path(\"{id}\")", "@Path(\"{type}/{id}\")", false);
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "@PathParam(\"type\") //field", "", false);
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "//@PathParam(\"type\") //property", "@PathParam(\"type\")", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.BoatResource");
		final JaxrsResource boatResource = metamodel.findResource(boatResourceCompilationUnit.findPrimaryType());
		metamodelMonitor.resetElementChangesNotifications();
		final Annotation pathParamAnnotation = boatResource.getProperty("setType").getPathParamAnnotation();
		replaceFirstOccurrenceOfCode(boatResource, "@PathParam(\"type\")", "@PathParam(\"t\")", true);
		final CompilationUnit ast = JdtUtils.parse(boatResourceCompilationUnit, null);
		final ISourceRange annotationValueRange = JdtUtils.resolveMemberPairValueRange(pathParamAnnotation.getJavaAnnotation(), Annotation.VALUE, ast);
		final IDocument document = new Document(boatResourceCompilationUnit.getSource());
		final EditorValidationContext editorValidationContext = new EditorValidationContext(project, document);
		assertThat(boatResourceCompilationUnit.getSource().substring(annotationValueRange.getOffset(), annotationValueRange.getOffset() + annotationValueRange.getLength()), equalTo("\"t\""));
		
		// operation 1: validate
		final IRegion modifiedRegion = new Region(annotationValueRange.getOffset(), annotationValueRange.getLength());
		new JaxrsMetamodelValidator().validate(null, project, Arrays.asList(modifiedRegion), validationHelper, reporter, editorValidationContext, null, (IFile)boatResourceCompilationUnit.getResource());
		
		// verifications 1 : expect 1 problem
		final IMessage[] messages = findJaxrsMessages(reporter, boatResource);
		assertThat(messages.length, equalTo(2));
		assertThat(messages[0].getText(), not(containsString("{")));
		assertThat((String) messages[0].getAttribute(JaxrsMetamodelValidator.PREFERENCE_KEY_ATTRIBUTE_NAME),
				equalTo(JaxrsPreferences.RESOURCE_METHOD_UNBOUND_PATH_ANNOTATION_TEMPLATE_PARAMETER));
		assertThat(messages[1].getText(), not(containsString("{")));
		assertThat((String) messages[1].getAttribute(JaxrsMetamodelValidator.PREFERENCE_KEY_ATTRIBUTE_NAME),
				equalTo(JaxrsPreferences.RESOURCE_ELEMENT_UNBOUND_PATHPARAM_ANNOTATION_VALUE));

		// operation 2: fix the value and revalidate
		replaceFirstOccurrenceOfCode(boatResource, "@PathParam(\"t\")", "@PathParam(\"type\")", true);
		document.set(boatResourceCompilationUnit.getSource());
		final IRegion fixedRegion = new Region(annotationValueRange.getOffset(), annotationValueRange.getLength());
		new JaxrsMetamodelValidator().validate(null, project, Arrays.asList(fixedRegion), validationHelper, reporter, new EditorValidationContext(project, document), null, (IFile)boatResourceCompilationUnit.getResource());
		
		// verifications 2 : expect 0 problem
		final IMessage[] updatedMessages = findJaxrsMessages(reporter, boatResource);
		assertThat(updatedMessages.length, equalTo(0));
	}
	
	@Test
	public void shouldResolveProblemWhenPathParamAnnotatedResourcePropertyBoundToResourcePath_AsYouType() throws CoreException, ValidationException {
		final ICompilationUnit boatResourceCompilationUnit = metamodelMonitor.createCompilationUnit("BoatResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "BoatResource.java");
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "@Path(\"/\")", "@Path(\"/{type}\")", false);
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "@PathParam(\"type\") //field", "", false);
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "//@PathParam(\"type\") //property", "@PathParam(\"type\")", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.BoatResource");
		final JaxrsResource boatResource = metamodel.findResource(boatResourceCompilationUnit.findPrimaryType());
		metamodelMonitor.resetElementChangesNotifications();
		final Annotation pathParamAnnotation = boatResource.getProperty("setType").getPathParamAnnotation();
		replaceFirstOccurrenceOfCode(boatResource, "@PathParam(\"type\")", "@PathParam(\"t\")", true);
		final CompilationUnit ast = JdtUtils.parse(boatResourceCompilationUnit, null);
		final ISourceRange annotationValueRange = JdtUtils.resolveMemberPairValueRange(pathParamAnnotation.getJavaAnnotation(), Annotation.VALUE, ast);
		final IDocument document = new Document(boatResourceCompilationUnit.getSource());
		final EditorValidationContext editorValidationContext = new EditorValidationContext(project, document);
		assertThat(boatResourceCompilationUnit.getSource().substring(annotationValueRange.getOffset(), annotationValueRange.getOffset() + annotationValueRange.getLength()), equalTo("\"t\""));
		
		// operation 1: validate
		final IRegion modifiedRegion = new Region(annotationValueRange.getOffset(), annotationValueRange.getLength());
		new JaxrsMetamodelValidator().validate(null, project, Arrays.asList(modifiedRegion), validationHelper, reporter, editorValidationContext, null, (IFile)boatResourceCompilationUnit.getResource());
		
		// verifications 1 : expect 1 problem
		final IMessage[] messages = findJaxrsMessages(reporter, boatResource);
		assertThat(messages.length, equalTo(2));
		assertThat(messages[0].getText(), not(containsString("{")));
		assertThat((String) messages[0].getAttribute(JaxrsMetamodelValidator.PREFERENCE_KEY_ATTRIBUTE_NAME),
				equalTo(JaxrsPreferences.RESOURCE_METHOD_UNBOUND_PATH_ANNOTATION_TEMPLATE_PARAMETER));
		assertThat(messages[1].getText(), not(containsString("{")));
		assertThat((String) messages[1].getAttribute(JaxrsMetamodelValidator.PREFERENCE_KEY_ATTRIBUTE_NAME),
				equalTo(JaxrsPreferences.RESOURCE_ELEMENT_UNBOUND_PATHPARAM_ANNOTATION_VALUE));

		// operation 2: fix the value and revalidate
		JavaElementsUtils.replaceFirstOccurrenceOfCode(boatResourceCompilationUnit, "@PathParam(\"t\")", "@PathParam(\"type\")", true);
		document.set(boatResourceCompilationUnit.getSource());
		final IRegion fixedRegion = new Region(annotationValueRange.getOffset(), annotationValueRange.getLength());
		new JaxrsMetamodelValidator().validate(null, project, Arrays.asList(fixedRegion), validationHelper, reporter, new EditorValidationContext(project, document), null, (IFile)boatResourceCompilationUnit.getResource());
		
		// verifications 2 : expect 0 problem
		final IMessage[] updatedMessages = findJaxrsMessages(reporter, boatResource);
		assertThat(updatedMessages.length, equalTo(0));
	}
	
	@Test
	public void shouldNotReportProblemWhenPathParamAnnotatedResourcePropertyBoundToPath() throws CoreException, ValidationException {
		final ICompilationUnit boatResourceCompilationUnit = metamodelMonitor.createCompilationUnit("BoatResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "BoatResource.java");
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "@PathParam(\"type\") //field", "//@PathParam(\"type\")", false);
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "//@PathParam(\"type\") //property", "@PathParam(\"type\")", false);
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "@Path(\"{id}\")", "@Path(\"{type}/{id}\")", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.BoatResource");
		final JaxrsResource boatResource = metamodel.findResource(boatResourceCompilationUnit.findPrimaryType());
		metamodelMonitor.resetElementChangesNotifications();
		
		validate(boatResource);
		
		// verifications
		final IMarker[] markers = findJaxrsMarkers(boatResource);
		assertThat(markers.length, equalTo(0));
	}

	@Test
	public void shouldReporProblemWhenMissingBeginBracketInResourceMethodPathAnnotationValue() throws CoreException, ValidationException {
		final ICompilationUnit boatResourceCompilationUnit = metamodelMonitor.createCompilationUnit("BoatResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "BoatResource.java");
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "@Path(\"{id}\")", "@Path(\"id}\")", false);
		// removing other items to avoid unwanted markers here
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "@PathParam(\"type\") //field",
				"//@PathParam(\"type\")", false);
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "@PathParam(\"id\")", "", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.BoatResource");
		final JaxrsResource boatResource = metamodel.findResource(boatResourceCompilationUnit.findPrimaryType());
		metamodelMonitor.resetElementChangesNotifications();
		
		validate(boatResource);
		
		// verifications
		final IMarker[] markers = findJaxrsMarkers(boatResource);
		assertThat(markers.length, equalTo(1));
		assertThat((String) markers[0].getAttribute(JaxrsMetamodelValidator.PREFERENCE_KEY_ATTRIBUTE_NAME),
				equalTo(JaxrsPreferences.RESOURCE_METHOD_INVALID_PATH_ANNOTATION_VALUE));
		
	}

	@Test
	public void shouldReporProblemWhenMissingEndBracketInResourceMethodPathAnnotationValue() throws CoreException, ValidationException {
		final ICompilationUnit boatResourceCompilationUnit = metamodelMonitor.createCompilationUnit("BoatResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "BoatResource.java");
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "@Path(\"{id}\")", "@Path(\"{id\")", false);
		// removing other items to avoid unwanted markers here
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "@PathParam(\"type\") //field", "//@PathParam(\"type\")", false);
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "@PathParam(\"id\")", "", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.BoatResource");
		final JaxrsResource boatResource = metamodel.findResource(boatResourceCompilationUnit.findPrimaryType());
		metamodelMonitor.resetElementChangesNotifications();
		
		validate(boatResource);
		
		// verifications
		final IMarker[] markers = findJaxrsMarkers(boatResource);
		assertThat(markers.length, equalTo(1));
		assertThat((String) markers[0].getAttribute(JaxrsMetamodelValidator.PREFERENCE_KEY_ATTRIBUTE_NAME),
				equalTo(JaxrsPreferences.RESOURCE_METHOD_INVALID_PATH_ANNOTATION_VALUE));
		
	}

	@Test
	public void shouldReporProblemWhenMissingBeginBracketInResourcePathAnnotationValue() throws CoreException, ValidationException {
		final ICompilationUnit boatResourceCompilationUnit = metamodelMonitor.createCompilationUnit("BoatResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "BoatResource.java");
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "@Path(\"/\")", "@Path(\"{type}/foo}\")", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.BoatResource");
		final JaxrsResource boatResource = metamodel.findResource(boatResourceCompilationUnit.findPrimaryType());
		metamodelMonitor.resetElementChangesNotifications();
		
		validate(boatResource);
		
		// verifications
		final IMarker[] markers = findJaxrsMarkers(boatResource);
		assertThat(markers.length, equalTo(1));
		assertThat((String) markers[0].getAttribute(JaxrsMetamodelValidator.PREFERENCE_KEY_ATTRIBUTE_NAME),
				equalTo(JaxrsPreferences.RESOURCE_INVALID_PATH_ANNOTATION_VALUE));
		
	}

	@Test
	public void shouldReporProblemWhenMissingEndBracketInResourcePathAnnotationValue() throws CoreException, ValidationException {
		final ICompilationUnit boatResourceCompilationUnit = metamodelMonitor.createCompilationUnit("BoatResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "BoatResource.java");
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "@Path(\"/\")", "@Path(\"{type}/{foo\")", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.BoatResource");
		final JaxrsResource boatResource = metamodel.findResource(boatResourceCompilationUnit.findPrimaryType());
		metamodelMonitor.resetElementChangesNotifications();
		
		validate(boatResource);
		
		// verifications
		final IMarker[] markers = findJaxrsMarkers(boatResource);
		assertThat(markers.length, equalTo(1));
		assertThat((String) markers[0].getAttribute(JaxrsMetamodelValidator.PREFERENCE_KEY_ATTRIBUTE_NAME),
				equalTo(JaxrsPreferences.RESOURCE_INVALID_PATH_ANNOTATION_VALUE));
	}
	
	@Test
	public void shouldNotReportProblemOnMethodParamOfTypeEnumeration() throws CoreException, ValidationException {
		final ICompilationUnit boatResourceCompilationUnit = metamodelMonitor.createCompilationUnit("ResourceWithEnumMethodParams.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "HelloWorld.java");
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.HelloWorld");
		final JaxrsResource resource = metamodel.findResource(boatResourceCompilationUnit.findPrimaryType());
		metamodelMonitor.resetElementChangesNotifications();
		
		validate(resource);
		
		// verifications
		final IMarker[] markers = findJaxrsMarkers(resource);
		assertThat(markers.length, equalTo(0));
	}
	
}
