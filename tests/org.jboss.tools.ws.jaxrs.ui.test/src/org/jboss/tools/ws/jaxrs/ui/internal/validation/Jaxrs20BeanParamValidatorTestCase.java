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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.equalTo;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsElementsUtils.replaceFirstOccurrenceOfCode;
import static org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils.replaceFirstOccurrenceOfCode;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.deleteJaxrsMarkers;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.findJaxrsMarkers;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.toSet;
import static org.junit.Assert.assertThat;

import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.wst.validation.ReporterHelper;
import org.eclipse.wst.validation.internal.core.ValidationException;
import org.eclipse.wst.validation.internal.provisional.core.IReporter;
import org.jboss.tools.common.validation.ContextValidationHelper;
import org.jboss.tools.common.validation.IProjectValidationContext;
import org.jboss.tools.common.validation.ValidatorManager;
import org.jboss.tools.common.validation.internal.ProjectValidationContext;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsParameterAggregator;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils;
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.TestWatcher;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.jboss.tools.ws.jaxrs.ui.preferences.JaxrsPreferences;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author xcoulon
 *
 */
@SuppressWarnings("restriction")
public class Jaxrs20BeanParamValidatorTestCase {

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

	@Rule
	public TestWatcher testWatcher = new TestWatcher();

	private JaxrsMetamodel metamodel = null;
	private IProject project = null;
	private IJavaProject javaProject = null;

	@Before
	public void setup() throws CoreException {
		this.metamodel = metamodelMonitor.getMetamodel();
		this.project = metamodel.getProject();
		this.javaProject = metamodel.getJavaProject();
	}

	@Test
	public void shouldValidateWhenAllPathParamsAreBoundToBeanParam() throws CoreException, ValidationException {
		// pre-conditions
		final IType carType = replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.CarResource", javaProject, "final CarParameterAggregator car", "@BeanParam final CarParameterAggregator car", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.CarResource", "org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final Set<IFile> changedResources = toSet(carType.getResource());
		new JaxrsMetamodelValidator().validate(changedResources, project, validationHelper, context, validatorManager,
				reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(metamodel.findElement(JavaElementsUtils.getMethod(carType, "update")));
		assertThat(markers.length, equalTo(0));
	}

	@Test
	public void shouldReportProblemWhenValidatingResourceMethodWithMissingAnnotationOnBeanParamField() throws CoreException, ValidationException {
		// pre-conditions: remove @PathParam on ParameterAggregator field ("id1")
		final IType carType = replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.CarResource", javaProject, "final CarParameterAggregator car", "@BeanParam final CarParameterAggregator car", false);
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator", javaProject, "@PathParam(\"id1\")", "", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.CarResource", "org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation: validate Resource
		final Set<IFile> changedResources = toSet(carType.getResource());
		new JaxrsMetamodelValidator().validate(changedResources, project, validationHelper, context, validatorManager,
				reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(metamodel.findElement(JavaElementsUtils.getMethod(carType, "update")));
		assertThat(markers.length, equalTo(1));
		assertThat(markers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""),
				equalTo(JaxrsPreferences.RESOURCE_METHOD_UNBOUND_PATH_ANNOTATION_TEMPLATE_PARAMETER));
		assertThat(markers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
	}

	@Test
	public void shouldReportProblemOnResourceMethodWhenValidatingParameterAggregatorWithMissingAnnotationOnField() throws CoreException, ValidationException {
		// pre-conditions: remove @PathParam on ParameterAggregator field ("id1")
		final IType carType = replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.CarResource", javaProject, "final CarParameterAggregator car", "@BeanParam final CarParameterAggregator car", false);
		final IType paramAggregatorType = replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator", javaProject, "@PathParam(\"id1\")", "", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.CarResource", "org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation: validate the *JAX-RS Parameter Aggregator*
		final Set<IFile> changedResources = toSet(paramAggregatorType.getResource());
		new JaxrsMetamodelValidator().validate(changedResources, project, validationHelper, context, validatorManager,
				reporter);
		
		// validation
		final IMarker[] markers = findJaxrsMarkers(metamodel.findElement(JavaElementsUtils.getMethod(carType, "update")));
		assertThat(markers.length, equalTo(1));
		assertThat(markers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""),
				equalTo(JaxrsPreferences.RESOURCE_METHOD_UNBOUND_PATH_ANNOTATION_TEMPLATE_PARAMETER));
		assertThat(markers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
	}

	@Test
	public void shouldReportProblemOnResourceMethodWhenValidatingParameterAggregatorWithMissingAnnotationOnProperty() throws CoreException, ValidationException {
		// pre-conditions: remove @PathParam on ParameterAggregator property ("setId2")
		final IType carType = replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.CarResource", javaProject, "final CarParameterAggregator car", "@BeanParam final CarParameterAggregator car", false);
		final IType paramAggregatorType = replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator", javaProject, "@PathParam(\"id2\")", "", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.CarResource", "org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation: validate the *JAX-RS Parameter Aggregator*
		final Set<IFile> changedResources = toSet(paramAggregatorType.getResource());
		new JaxrsMetamodelValidator().validate(changedResources, project, validationHelper, context, validatorManager,
				reporter);
		
		// validation
		final IMarker[] markers = findJaxrsMarkers(metamodel.findElement(JavaElementsUtils.getMethod(carType, "update")));
		assertThat(markers.length, equalTo(1));
		for (IMarker marker : markers) {
			assertThat(marker.getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""),
				equalTo(JaxrsPreferences.RESOURCE_METHOD_UNBOUND_PATH_ANNOTATION_TEMPLATE_PARAMETER));
			assertThat(marker.getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
		}
	}
	
	@Test
	public void shouldReportProblemWhenValidatingResourceMethodUsingParameterAggregatorWithMissingAnnotationOnField() throws CoreException, ValidationException {
		// pre-conditions: remove @PathParam on ParameterAggregator field ("id1")
		final IType carType = replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.CarResource", javaProject, "final CarParameterAggregator car", "@BeanParam final CarParameterAggregator car", false);
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator", javaProject, "@PathParam(\"id1\")", "", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.CarResource", "org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation: validate the *JAX-RS Resource*
		final Set<IFile> changedResources = toSet(carType.getResource());
		new JaxrsMetamodelValidator().validate(changedResources, project, validationHelper, context, validatorManager,
				reporter);
		
		// validation
		final IMarker[] markers = findJaxrsMarkers(metamodel.findElement(JavaElementsUtils.getMethod(carType, "update")));
		assertThat(markers.length, equalTo(1));
		assertThat(markers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""),
				equalTo(JaxrsPreferences.RESOURCE_METHOD_UNBOUND_PATH_ANNOTATION_TEMPLATE_PARAMETER));
		assertThat(markers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
	}

	@Test
	public void shouldReportAndFixProblemWhenValidatingResourceMethodUsingParameterAggregatorWithUnboundAnnotationValuesInParameterAggregator() throws CoreException, ValidationException {
		// pre-conditions: change @Path value in CarResource#update(CarParameterAggregator)
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.CarResource", javaProject, "final CarParameterAggregator car", "@BeanParam final CarParameterAggregator car", false);
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator", javaProject, "@PathParam(\"id1\")", "@PathParam(\"ide\")", false);
		final JaxrsResource carResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CarResource");
		final JaxrsParameterAggregator carParameterAggregator = metamodelMonitor.createParameterAggregator("org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation 1: validate the *JAX-RS Resource*
		final Set<IFile> resources = toSet(carParameterAggregator.getResource());
		new JaxrsMetamodelValidator().validate(resources, project, validationHelper, context, validatorManager,
				reporter);
		
		// validation 1: one problem reported in both classes
		final IMarker[] carResourceMarkers = findJaxrsMarkers(metamodel.findElement(JavaElementsUtils.getMethod(carResource.getJavaElement(), "update")));
		assertThat(carResourceMarkers.length, equalTo(2));
		final IMarker[] carParameterAggregatorMarkers = findJaxrsMarkers(metamodel.findElement(JavaElementsUtils.getField(carParameterAggregator.getJavaElement(), "id1")));
		assertThat(carParameterAggregatorMarkers.length, equalTo(1));
		assertThat(carParameterAggregatorMarkers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""),
				equalTo(JaxrsPreferences.RESOURCE_ELEMENT_UNBOUND_PATHPARAM_ANNOTATION_VALUE));
		assertThat(carParameterAggregatorMarkers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
		
		// operation 2: fix the problems
		replaceFirstOccurrenceOfCode(carParameterAggregator, "@PathParam(\"ide\")", "@PathParam(\"id1\")", false);
		final Set<IFile> changedResources = toSet(carParameterAggregator.getResource());
		new JaxrsMetamodelValidator().validate(changedResources, project, validationHelper, context, validatorManager,
				reporter);
		
		// validation 1: one problem reported
		final IMarker[] updatedCarResourceMarkers = findJaxrsMarkers(metamodel.findElement(JavaElementsUtils.getMethod(carResource.getJavaElement(), "update")));
		assertThat(updatedCarResourceMarkers.length, equalTo(0));
		final IMarker[] updatedCarParameterAggregatorMarkers = findJaxrsMarkers(metamodel.findElement(JavaElementsUtils.getField(carParameterAggregator.getJavaElement(), "id1")));
		assertThat(updatedCarParameterAggregatorMarkers.length, equalTo(0));
	}
	
	@Test
	public void shouldReportProblemWhenValidatingResourceMethodUsingParameterAggregatorWithMissingAnnotationOnProperty() throws CoreException, ValidationException {
		// pre-conditions: remove @PathParam on ParameterAggregator property ("setId2")
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator", javaProject, "@PathParam(\"id2\")", "", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.CarResource", "org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();
		final IType carType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CarResource");
		
		// operation: validate the *JAX-RS Resource*
		final Set<IFile> changedResources = toSet(carType.getResource());
		new JaxrsMetamodelValidator().validate(changedResources, project, validationHelper, context, validatorManager,
				reporter);
		
		// validation
		final IMarker[] markers = findJaxrsMarkers(metamodel.findElement(JavaElementsUtils.getMethod(carType, "update")));
		assertThat(markers.length, equalTo(2));
		for (IMarker marker : markers) {
			assertThat(marker.getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""),
				equalTo(JaxrsPreferences.RESOURCE_METHOD_UNBOUND_PATH_ANNOTATION_TEMPLATE_PARAMETER));
			assertThat(marker.getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
		}
	}
	
	@Test
	public void shouldReportAndFixProblemWhenValidatingResourceMethodUsingParameterAggregatorWithUnboundAnnotationValuesInResourceMethod() throws CoreException, ValidationException {
		// pre-conditions: change @Path value in CarResource#update(CarParameterAggregator)
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.CarResource", javaProject, "final CarParameterAggregator car", "@BeanParam final CarParameterAggregator car", false);
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.CarResource", javaProject, "@Path(\"/{id1}-{id2}\")", "@Path(\"/{id11}-{id22}\")", false);
		final JaxrsResource carResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CarResource");
		final JaxrsParameterAggregator carParameterAggregator = metamodelMonitor.createParameterAggregator("org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation 1: validate the *JAX-RS Resource*
		final Set<IFile> resources = toSet(carResource.getResource());
		new JaxrsMetamodelValidator().validate(resources, project, validationHelper, context, validatorManager,
				reporter);

		// validation 1: problems reported in both classes
		final IMarker[] carResourceMarkers = findJaxrsMarkers(metamodel.findElement(JavaElementsUtils.getMethod(carResource.getJavaElement(), "update")));
		assertThat(carResourceMarkers.length, equalTo(4));
		final IMarker[] carParameterAggregatorFieldMarkers = findJaxrsMarkers(metamodel.findElement(JavaElementsUtils.getField(carParameterAggregator.getJavaElement(), "id1")));
		assertThat(carParameterAggregatorFieldMarkers.length, equalTo(1));
		assertThat(carParameterAggregatorFieldMarkers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""),
				equalTo(JaxrsPreferences.RESOURCE_ELEMENT_UNBOUND_PATHPARAM_ANNOTATION_VALUE));
		assertThat(carParameterAggregatorFieldMarkers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
		final IMarker[] carParameterAggregatorMethodMarkers = findJaxrsMarkers(metamodel.findElement(JavaElementsUtils.getMethod(carParameterAggregator.getJavaElement(), "setId2")));
		assertThat(carParameterAggregatorMethodMarkers.length, equalTo(1));
		assertThat(carParameterAggregatorMethodMarkers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""),
				equalTo(JaxrsPreferences.RESOURCE_ELEMENT_UNBOUND_PATHPARAM_ANNOTATION_VALUE));
		assertThat(carParameterAggregatorMethodMarkers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));

		// operation 2: fix the problems
		replaceFirstOccurrenceOfCode(carResource, "@Path(\"/{id11}-{id22}\")", "@Path(\"/{id1}-{id2}\")", false);
		final Set<IFile> changedResources = toSet(carResource.getResource());
		new JaxrsMetamodelValidator().validate(changedResources, project, validationHelper, context, validatorManager,
				reporter);
		
		// validation 2: no problem reported
		final IMarker[] updatedCarResourceMarkers = findJaxrsMarkers(metamodel.findElement(JavaElementsUtils.getMethod(carResource.getJavaElement(), "update")));
		assertThat(updatedCarResourceMarkers.length, equalTo(0));
		final IMarker[] updatedCarParameterAggregatorMarkers = findJaxrsMarkers(metamodel.findElement(JavaElementsUtils.getField(carParameterAggregator.getJavaElement(), "id1")));
		assertThat(updatedCarParameterAggregatorMarkers.length, equalTo(0));
	}

	@Test
	public void shouldReportAndFixProblemWhenValidatingResourceMethodUsingParameterAggregatorWithUnboundAnnotationValuesInParameterAggregatorField() throws CoreException, ValidationException {
		// pre-conditions: change @Path value in CarResource#update(CarParameterAggregator)
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.CarResource", javaProject, "final CarParameterAggregator car", "@BeanParam final CarParameterAggregator car", false);
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator", javaProject, "@PathParam(\"id1\")", "@PathParam(\"ide\")", false);
		final JaxrsResource carResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CarResource");
		final JaxrsParameterAggregator carParameterAggregator = metamodelMonitor.createParameterAggregator("org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation 1: validate the *JAX-RS Resource*
		final Set<IFile> resources = toSet(carParameterAggregator.getResource());
		new JaxrsMetamodelValidator().validate(resources, project, validationHelper, context, validatorManager,
				reporter);
		
		// validation 1: problems reported in both classes
		final IMarker[] carResourceMarkers = findJaxrsMarkers(metamodel.findElement(JavaElementsUtils.getMethod(carResource.getJavaElement(), "update")));
		assertThat(carResourceMarkers.length, equalTo(2));
		final IMarker[] carParameterAggregatorMarkers = findJaxrsMarkers(metamodel.findElement(JavaElementsUtils.getField(carParameterAggregator.getJavaElement(), "id1")));
		assertThat(carParameterAggregatorMarkers.length, equalTo(1));
		assertThat(carParameterAggregatorMarkers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""),
				equalTo(JaxrsPreferences.RESOURCE_ELEMENT_UNBOUND_PATHPARAM_ANNOTATION_VALUE));
		assertThat(carParameterAggregatorMarkers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
		
		// operation 2: fix the problems
		replaceFirstOccurrenceOfCode(carParameterAggregator, "@PathParam(\"ide\")", "@PathParam(\"id1\")", false);
		final Set<IFile> changedResources = toSet(carParameterAggregator.getResource());
		new JaxrsMetamodelValidator().validate(changedResources, project, validationHelper, context, validatorManager,
				reporter);
		
		// validation 1: no more problem reported
		final IMarker[] updatedCarResourceMarkers = findJaxrsMarkers(metamodel.findElement(JavaElementsUtils.getMethod(carResource.getJavaElement(), "update")));
		assertThat(updatedCarResourceMarkers.length, equalTo(0));
		final IMarker[] updatedCarParameterAggregatorMarkers = findJaxrsMarkers(metamodel.findElement(JavaElementsUtils.getField(carParameterAggregator.getJavaElement(), "id1")));
		assertThat(updatedCarParameterAggregatorMarkers.length, equalTo(0));
	}

	@Test
	public void shouldReportAndFixProblemWhenValidatingResourceMethodUsingParameterAggregatorWithUnboundAnnotationValuesInParameterAggregatorProperty() throws CoreException, ValidationException {
		// pre-conditions: change @Path value in CarResource#update(CarParameterAggregator)
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.CarResource", javaProject, "final CarParameterAggregator car", "@BeanParam final CarParameterAggregator car", false);
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator", javaProject, "@PathParam(\"id2\")", "@PathParam(\"ide\")", false);
		final JaxrsResource carResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CarResource");
		final JaxrsParameterAggregator carParameterAggregator = metamodelMonitor.createParameterAggregator("org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation 1: validate the *JAX-RS Resource*
		final Set<IFile> resources = toSet(carParameterAggregator.getResource());
		new JaxrsMetamodelValidator().validate(resources, project, validationHelper, context, validatorManager,
				reporter);
		
		// validation 1: one problem reported in both classes
		final IMarker[] carResourceMarkers = findJaxrsMarkers(metamodel.findElement(JavaElementsUtils.getMethod(carResource.getJavaElement(), "update")));
		assertThat(carResourceMarkers.length, equalTo(2));
		final IMarker[] carParameterAggregatorMarkers = findJaxrsMarkers(metamodel.findElement(JavaElementsUtils.getMethod(carParameterAggregator.getJavaElement(), "setId2")));
		assertThat(carParameterAggregatorMarkers.length, equalTo(1));
		assertThat(carParameterAggregatorMarkers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""),
				equalTo(JaxrsPreferences.RESOURCE_ELEMENT_UNBOUND_PATHPARAM_ANNOTATION_VALUE));
		assertThat(carParameterAggregatorMarkers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
		
		// operation 2: fix the problems
		replaceFirstOccurrenceOfCode(carParameterAggregator, "@PathParam(\"ide\")", "@PathParam(\"id2\")", false);
		final Set<IFile> changedResources = toSet(carParameterAggregator.getResource());
		new JaxrsMetamodelValidator().validate(changedResources, project, validationHelper, context, validatorManager,
				reporter);
		
		// validation 1: one problem reported
		final IMarker[] updatedCarResourceMarkers = findJaxrsMarkers(metamodel.findElement(JavaElementsUtils.getMethod(carResource.getJavaElement(), "update")));
		assertThat(updatedCarResourceMarkers.length, equalTo(0));
		final IMarker[] updatedCarParameterAggregatorMarkers = findJaxrsMarkers(metamodel.findElement(JavaElementsUtils.getMethod(carParameterAggregator.getJavaElement(), "setId2")));
		assertThat(updatedCarParameterAggregatorMarkers.length, equalTo(0));
	}
	

}
