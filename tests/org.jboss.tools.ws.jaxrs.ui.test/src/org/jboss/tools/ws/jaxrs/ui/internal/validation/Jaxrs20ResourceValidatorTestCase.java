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
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.findJaxrsMarkers;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.toSet;
import static org.junit.Assert.assertThat;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
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
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsElementsUtils;
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.jboss.tools.ws.jaxrs.ui.preferences.JaxrsPreferences;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Xi
 * 
 */
@SuppressWarnings("restriction")
public class Jaxrs20ResourceValidatorTestCase {

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
	
	@Before
	public void setup() throws CoreException {
		metamodel = metamodelMonitor.getMetamodel();
		project = metamodel.getProject();
	}

	@Test
	public void shouldReportProblemWhenUnboundPathParamAnnotatedParamAggregatorField() throws CoreException, ValidationException {
		final ICompilationUnit boatResourceCompilationUnit = metamodelMonitor.createCompilationUnit("BoatResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "BoatResource.java");
		metamodelMonitor.createCompilationUnit("BoatParameterAggregator.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "BoatParameterAggregator.java");
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "@PathParam(\"id\") int id", "@BeanParam BoatParameterAggregator aggregator", false);
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "@PathParam(\"type\") //field", "//@PathParam(\"type\")", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.BoatResource", "org.jboss.tools.ws.jaxrs.sample.services.BoatParameterAggregator");
		final JaxrsResource boatResource = metamodel.findResource(boatResourceCompilationUnit.findPrimaryType());
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation: validate
		new JaxrsMetamodelValidator().validate(toSet(boatResource.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		
		// verifications
		final IMarker[] markers = findJaxrsMarkers(boatResource);
		assertThat(markers.length, equalTo(1));
		assertThat(markers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
		assertThat((String) markers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE),
				equalTo(JaxrsPreferences.RESOURCE_METHOD_UNBOUND_PATHPARAM_ANNOTATION_VALUE));
	}
	
	@Test
	public void shouldReportAndFixProblemWhenUnboundPathParamAnnotatedParamAggregatorFieldFixedInResourceMethod() throws CoreException, ValidationException {
		final ICompilationUnit boatResourceCompilationUnit = metamodelMonitor.createCompilationUnit("BoatResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "BoatResource.java");
		final ICompilationUnit boatParameterAggregatorCompilationUnit = metamodelMonitor.createCompilationUnit("BoatParameterAggregator.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "BoatParameterAggregator.java");
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "@PathParam(\"id\") int id", "@BeanParam BoatParameterAggregator aggregator", false);
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "@PathParam(\"type\") //field", "", false);
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "@Path(\"{id}\")", "@Path(\"{i}\")", false);
		ResourcesUtils.replaceAllOccurrencesOfCode(boatParameterAggregatorCompilationUnit, "@PathParam(\"type\") //field", "", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.BoatResource", "org.jboss.tools.ws.jaxrs.sample.services.BoatParameterAggregator");
		final JaxrsResource boatResource = (JaxrsResource) metamodel.findElement(boatResourceCompilationUnit.findPrimaryType());
		final JaxrsParameterAggregator boatParameterAggregator = (JaxrsParameterAggregator) metamodel.findElement(boatParameterAggregatorCompilationUnit.findPrimaryType());
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation 1 : validate
		new JaxrsMetamodelValidator().validate(toSet(boatResourceCompilationUnit.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		
		// verifications 1: 1 problem reported
		final IMarker[] markers = findJaxrsMarkers(boatParameterAggregator);
		assertThat(markers.length, equalTo(1));
		assertThat(markers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
		assertThat((String) markers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE),
				equalTo(JaxrsPreferences.RESOURCE_ELEMENT_UNBOUND_PATHPARAM_ANNOTATION_VALUE));

		// operation 2: fix problem on resource method and revalidate
		JaxrsElementsUtils.replaceFirstOccurrenceOfCode(boatResource, "@Path(\"{i}\")", "@Path(\"{id}\")", false);
		new JaxrsMetamodelValidator().validate(toSet(boatResourceCompilationUnit.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		
		// verifications 2: no problem reported
		final IMarker[] updatedMarkers = findJaxrsMarkers(boatParameterAggregator);
		assertThat(updatedMarkers.length, equalTo(0));
	}
	
	@Test
	public void shouldReportAndFixProblemWhenUnboundPathParamAnnotatedParamAggregatorFieldFixedInParameterAggregator() throws CoreException, ValidationException {
		final ICompilationUnit boatResourceCompilationUnit = metamodelMonitor.createCompilationUnit("BoatResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "BoatResource.java");
		final ICompilationUnit boatParameterAggregatorCompilationUnit = metamodelMonitor.createCompilationUnit("BoatParameterAggregator.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "BoatParameterAggregator.java");
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "@PathParam(\"id\") int id", "@BeanParam BoatParameterAggregator aggregator", false);
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "@PathParam(\"type\") //field", "", false);
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "@Path(\"{id}\")", "@Path(\"{i}\")", false);
		ResourcesUtils.replaceAllOccurrencesOfCode(boatParameterAggregatorCompilationUnit, "@PathParam(\"type\") //field", "", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.BoatResource", "org.jboss.tools.ws.jaxrs.sample.services.BoatParameterAggregator");
		final JaxrsResource boatResource = metamodel.findResource(boatResourceCompilationUnit.findPrimaryType());
		final JaxrsParameterAggregator boatParameterAggregator = (JaxrsParameterAggregator) metamodel.findElement(boatParameterAggregatorCompilationUnit.findPrimaryType());
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation 1 : validate
		new JaxrsMetamodelValidator().validate(toSet(boatResource.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		
		// verifications 1: 1 problem reported
		final IMarker[] markers = findJaxrsMarkers(boatParameterAggregator);
		assertThat(markers.length, equalTo(1));
		assertThat(markers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
		assertThat((String) markers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE),
				equalTo(JaxrsPreferences.RESOURCE_ELEMENT_UNBOUND_PATHPARAM_ANNOTATION_VALUE));
		
		// operation 2: fix problem on resource method and revalidate
		JaxrsElementsUtils.replaceFirstOccurrenceOfCode(boatParameterAggregator, "@PathParam(\"id\") //field", "@PathParam(\"i\") //field", false);
		new JaxrsMetamodelValidator().validate(toSet(boatParameterAggregator.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		
		// verifications 2: no problem reported
		final IMarker[] updatedMarkers = findJaxrsMarkers(boatParameterAggregator);
		assertThat(updatedMarkers.length, equalTo(0));
	}
	
	@Test
	public void shouldNotReportProblemWhenPathParamAnnotatedParamAggregatorFieldBoundToPath() throws CoreException, ValidationException {
		final ICompilationUnit boatResourceCompilationUnit = metamodelMonitor.createCompilationUnit("BoatResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "BoatResource.java");
		metamodelMonitor.createCompilationUnit("BoatParameterAggregator.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "BoatParameterAggregator.java");
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "@Path(\"{id}\")", "@Path(\"{type}/{id}\")", false);
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "@PathParam(\"type\") //field", "//@PathParam(\"type\")", false);
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "@PathParam(\"id\") int id", "@BeanParam BoatParameterAggregator aggregator", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.BoatResource", "org.jboss.tools.ws.jaxrs.sample.services.BoatParameterAggregator");
		final JaxrsResource boatResource = metamodel.findResource(boatResourceCompilationUnit.findPrimaryType());
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation: validate
		new JaxrsMetamodelValidator().validate(toSet(boatResource.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		
		// verifications
		final IMarker[] markers = findJaxrsMarkers(boatResource);
		assertThat(markers.length, equalTo(0));
	}
	
	@Test
	public void shouldReportProblemWhenUnboundPathParamAnnotatedWithParamAggregatorProperty() throws CoreException, ValidationException {
		final ICompilationUnit boatResourceCompilationUnit = metamodelMonitor.createCompilationUnit("BoatResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "BoatResource.java");
		final ICompilationUnit boatParamAggregatorCompilationUnit = metamodelMonitor.createCompilationUnit("BoatParameterAggregator.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "BoatParameterAggregator.java");
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "@PathParam(\"id\") int id", "@BeanParam BoatParameterAggregator aggregator", false);
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "@PathParam(\"type\") //field", "//@PathParam(\"type\")", false);
		ResourcesUtils.replaceAllOccurrencesOfCode(boatParamAggregatorCompilationUnit, "@PathParam(\"type\") //field", "//@PathParam(\"type\")", false);
		ResourcesUtils.replaceAllOccurrencesOfCode(boatParamAggregatorCompilationUnit, "//@PathParam(\"type\") //property", "@PathParam(\"type\")", false);
		
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.BoatResource", "org.jboss.tools.ws.jaxrs.sample.services.BoatParameterAggregator");
		final JaxrsResource boatResource = metamodel.findResource(boatResourceCompilationUnit.findPrimaryType());
		final JaxrsParameterAggregator boatAggregator = metamodel.findParameterAggregator("org.jboss.tools.ws.jaxrs.sample.services.BoatParameterAggregator");
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation: validate
		new JaxrsMetamodelValidator().validate(toSet(boatResource.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		
		// verifications
		final IMarker[] resourceMarkers = findJaxrsMarkers(boatResource);
		assertThat(resourceMarkers.length, equalTo(1));
		assertThat(resourceMarkers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
		assertThat((String) resourceMarkers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE),
				equalTo(JaxrsPreferences.RESOURCE_METHOD_UNBOUND_PATHPARAM_ANNOTATION_VALUE));
		final IMarker[] aggregatorMarker = findJaxrsMarkers(boatAggregator);
		assertThat(aggregatorMarker.length, equalTo(1));
		assertThat((String) aggregatorMarker[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE),
				equalTo(JaxrsPreferences.RESOURCE_ELEMENT_UNBOUND_PATHPARAM_ANNOTATION_VALUE));
	}
	
	@Test
	public void shouldNotReportProblemWhenPathParamAnnotatedParamAggregatorPropertyBoundToPath() throws CoreException, ValidationException {
		final ICompilationUnit boatResourceCompilationUnit = metamodelMonitor.createCompilationUnit("BoatResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "BoatResource.java");
		final ICompilationUnit boatParamAggregator = metamodelMonitor.createCompilationUnit("BoatParameterAggregator.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "BoatParameterAggregator.java");
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "@PathParam(\"id\") int id", "@BeanParam BoatParameterAggregator aggregator", false);
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "@Path(\"{id}\")", "@Path(\"{type}/{id}\")", false);
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "@PathParam(\"type\") //field", "//@PathParam(\"type\")", false);
		ResourcesUtils.replaceAllOccurrencesOfCode(boatParamAggregator, "@PathParam(\"type\") //field", "//@PathParam(\"type\")", false);
		ResourcesUtils.replaceAllOccurrencesOfCode(boatParamAggregator, "//@PathParam(\"type\") //property", "@PathParam(\"type\")", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.BoatResource", "org.jboss.tools.ws.jaxrs.sample.services.BoatParameterAggregator");
		final JaxrsResource boatResource = metamodel.findResource(boatResourceCompilationUnit.findPrimaryType());
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation: validate
		new JaxrsMetamodelValidator().validate(toSet(boatResource.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		
		// verifications
		final IMarker[] markers = findJaxrsMarkers(boatResource);
		assertThat(markers.length, equalTo(0));
	}
	
	@Test
	public void shouldNotReportProblemOnMethodParameterWhenBoundToMissingParamAggregator() throws CoreException, ValidationException {
		final ICompilationUnit boatResourceCompilationUnit = metamodelMonitor.createCompilationUnit("BoatResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "BoatResource.java");
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "@Path(\"{id}\")", "@Path(\"{type}/{id}\")", false);
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "@PathParam(\"id\") int id", "@BeanParam BoatParameterAggregator aggregator", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.BoatResource");
		final JaxrsResource boatResource = metamodel.findResource(boatResourceCompilationUnit.findPrimaryType());
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation: validate
		new JaxrsMetamodelValidator().validate(toSet(boatResource.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		
		// verifications
		final IMarker[] markers = findJaxrsMarkers(boatResource);
		assertThat(markers.length, equalTo(1));
		assertThat(markers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""),
				equalTo(JaxrsPreferences.RESOURCE_METHOD_UNBOUND_PATH_ANNOTATION_TEMPLATE_PARAMETER));
	}

	@Test
	public void shouldNotReportProblemOnResourceFieldWhenBoundToMissingParamAggregator() throws CoreException, ValidationException {
		final ICompilationUnit boatResourceCompilationUnit = metamodelMonitor.createCompilationUnit("BoatResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "BoatResource.java");
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "@Path(\"{id}\")", "@Path(\"{type}/{id}\")", false);
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "private String type;", "private BoatParameterAggregator aggregator", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.BoatResource");
		final JaxrsResource boatResource = metamodel.findResource(boatResourceCompilationUnit.findPrimaryType());
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation: validate
		new JaxrsMetamodelValidator().validate(toSet(boatResource.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		
		// verifications
		final IMarker[] markers = findJaxrsMarkers(boatResource);
		assertThat(markers.length, equalTo(0));
	}
	
	@Test
	public void shouldNotReportProblemOnResourcePropertyWhenBoundToMissingParamAggregator() throws CoreException, ValidationException {
		final ICompilationUnit boatResourceCompilationUnit = metamodelMonitor.createCompilationUnit("BoatResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "BoatResource.java");
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "public void setType(String type)", "public void setType(BoatParameterAggregator aggregator)", false);
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "@Path(\"{id}\")", "@Path(\"{type}/{id}\")", false);
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "//@PathParam(\"type\") //property", "@PathParam(\"type\")", false);
		ResourcesUtils.replaceAllOccurrencesOfCode(boatResourceCompilationUnit, "@PathParam(\"type\") //field", "", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.BoatResource");
		final JaxrsResource boatResource = metamodel.findResource(boatResourceCompilationUnit.findPrimaryType());
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation: validate
		new JaxrsMetamodelValidator().validate(toSet(boatResource.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		
		// verifications
		final IMarker[] markers = findJaxrsMarkers(boatResource);
		assertThat(markers.length, equalTo(0));
	}
	
}
