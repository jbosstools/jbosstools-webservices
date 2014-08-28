/******************************************************************************* 
 * Copyright (c) 2013 - 2014 Red Hat, Inc. and others. 
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
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PROVIDER;
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
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.wst.validation.ReporterHelper;
import org.eclipse.wst.validation.internal.core.ValidationException;
import org.eclipse.wst.validation.internal.provisional.core.IReporter;
import org.jboss.tools.common.validation.ContextValidationHelper;
import org.jboss.tools.common.validation.IProjectValidationContext;
import org.jboss.tools.common.validation.ValidatorManager;
import org.jboss.tools.common.validation.internal.ProjectValidationContext;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsParamConverterProvider;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.Flags;
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.TestWatcher;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementCategory;
import org.jboss.tools.ws.jaxrs.ui.JBossJaxrsUIPlugin;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.TestLogger;
import org.jboss.tools.ws.jaxrs.ui.preferences.JaxrsPreferences;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author xcoulon
 *
 */
@SuppressWarnings("restriction")
public class Jaxrs20ParamConverterProviderTestCase {
	
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

	@Before
	public void setup() throws CoreException {
		metamodel = metamodelMonitor.getMetamodel();
		project = metamodel.getProject();
	}

	@After
	public void resetProblemLevelPreferences() {
		final IEclipsePreferences defaultPreferences = ((IScopeContext) DefaultScope.INSTANCE)
				.getNode(JBossJaxrsUIPlugin.PLUGIN_ID);
		defaultPreferences.put(JaxrsPreferences.PROVIDER_MISSING_IMPLEMENTATION, JaxrsPreferences.ERROR);
	}

	@Test
	public void shouldReportWarningIfProviderAnnotationIsMissing() throws CoreException, ValidationException {
		// preconditions
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.CarParamConverterProvider");
		final JaxrsParamConverterProvider providerConverterProvider = (JaxrsParamConverterProvider) metamodel
				.findElement("org.jboss.tools.ws.jaxrs.sample.services.CarParamConverterProvider", EnumElementCategory.PARAM_CONVERTER_PROVIDER);
		final Annotation providerAnnotation = providerConverterProvider.getAnnotation(PROVIDER);
		providerConverterProvider.removeAnnotation(providerAnnotation.getJavaAnnotation());
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation
		final Set<IFile> changedResources = toSet(providerConverterProvider.getResource());
		new JaxrsMetamodelValidator().validate(changedResources, project, validationHelper, context, validatorManager,
				reporter);
		
		// validation
		final IMarker[] markers = findJaxrsMarkers(providerConverterProvider);
		for (IMarker marker : markers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(1));
		assertThat(markers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""),
				equalTo(JaxrsPreferences.PROVIDER_MISSING_ANNOTATION));
		assertThat(markers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
	}
	
	@Test
	public void shouldNotReportProblemsOnAllMethodParams() throws CoreException, ValidationException {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("Truck.txt", "org.jboss.tools.ws.jaxrs.sample.services", "Truck.java");
		final ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit("TruckResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "TruckResource.java");
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.RestApplication",
				"org.jboss.tools.ws.jaxrs.sample.services.Truck",
				"org.jboss.tools.ws.jaxrs.sample.services.TruckResource",
				"org.jboss.tools.ws.jaxrs.sample.services.CarParamConverterProvider");
		final JaxrsResource truckResource = metamodelMonitor.createResource(compilationUnit.findPrimaryType());
		metamodelMonitor.resetElementChangesNotifications();

		// operation
		new JaxrsMetamodelValidator().validate(toSet(truckResource.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		
		// validation
		final IMarker[] markers = findJaxrsMarkers(truckResource);
		assertThat(markers.length, equalTo(0));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), equalTo(0));
	}
	
	@Test
	public void shouldResolveProblemWhenAddingParamConverterProvider() throws CoreException, ValidationException {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("Plane.txt", "org.jboss.tools.ws.jaxrs.sample.services", "Plane.java");
		final ICompilationUnit planeResourceCompilationUnit = metamodelMonitor.createCompilationUnit("PlaneResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "PlaneResource.java");
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.Plane", "org.jboss.tools.ws.jaxrs.sample.services.PlaneResource");
		final JaxrsResource planeResource = metamodelMonitor.createResource(planeResourceCompilationUnit.findPrimaryType());
		metamodelMonitor.resetElementChangesNotifications();

		// operation 1 : validate without ParamConverterProvider
		new JaxrsMetamodelValidator().validate(toSet(planeResource.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		
		// validation: expect 1 problem
		final IMarker[] markers = findJaxrsMarkers(planeResource);
		assertThat(markers.length, equalTo(1));

		// operation 2: add the param converter provider
		final ICompilationUnit planeParamConverterProviderCompilationUnit = metamodelMonitor.createCompilationUnit("PlaneParamConverterProvider.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "PlaneParamConverterProvider.java");
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.PlaneParamConverterProvider");

		metamodelMonitor.resetElementChangesNotifications();

		// operation 2 : validate from ParamConverterProvider
		new JaxrsMetamodelValidator().validate(toSet(planeParamConverterProviderCompilationUnit.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		
		// validation: expect 0 problem on PlaneResource since there's now a ParamConverterProvider
		final IMarker[] updatedMarkers = findJaxrsMarkers(planeResource);
		assertThat(updatedMarkers.length, equalTo(0));
	}
	
	@Test
	public void shouldReportProblemWhenRemovingParamConverterProvider() throws CoreException, ValidationException {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("Plane.txt", "org.jboss.tools.ws.jaxrs.sample.services", "Plane.java");
		final ICompilationUnit planeResourceCompilationUnit = metamodelMonitor.createCompilationUnit("PlaneResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "PlaneResource.java");
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.Plane", "org.jboss.tools.ws.jaxrs.sample.services.PlaneResource");
		final JaxrsResource planeResource = metamodelMonitor.createResource(planeResourceCompilationUnit.findPrimaryType());
		metamodelMonitor.createCompilationUnit("PlaneParamConverterProvider.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "PlaneParamConverterProvider.java");
		final JaxrsParamConverterProvider planeParameterConverterProvider = metamodelMonitor.createParameterConverterProvider("org.jboss.tools.ws.jaxrs.sample.services.PlaneParamConverterProvider");
		metamodelMonitor.resetElementChangesNotifications();

		// operation 1 : validate with the ParamConverterProvider
		new JaxrsMetamodelValidator().validate(toSet(planeResource.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		
		// validation: expect 0 problem
		final IMarker[] markers = findJaxrsMarkers(planeResource);
		assertThat(markers.length, equalTo(0));

		// operation 2: remove the param converter provider
		planeParameterConverterProvider.remove(Flags.NONE);
		metamodelMonitor.resetElementChangesNotifications();

		// operation 2 : validate from ParamConverterProvider
		new JaxrsMetamodelValidator().validate(toSet(planeParameterConverterProvider.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		
		// validation: expect 1 problem on PlaneResource since there's no ParamConverterProvider
		final IMarker[] updatedMarkers = findJaxrsMarkers(planeResource);
		assertThat(updatedMarkers.length, equalTo(1));
	}
	
}
