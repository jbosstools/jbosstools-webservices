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
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isIn;
import static org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils.replaceFirstOccurrenceOfCode;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.deleteJaxrsMarkers;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.findJaxrsMarkers;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.toSet;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
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
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsProvider;
import org.jboss.tools.ws.jaxrs.ui.JBossJaxrsUIPlugin;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.TestLogger;
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
public class Jaxrs11ProviderValidatorTestCase {

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
		this.metamodel = metamodelMonitor.getMetamodel();
		this.project = metamodel.getProject();
		this.javaProject = metamodel.getJavaProject();
	}
	
	@After
	public void resetProblemLevelPreferences() {
		final IEclipsePreferences defaultPreferences = ((IScopeContext)DefaultScope.INSTANCE).getNode(JBossJaxrsUIPlugin.PLUGIN_ID);
		defaultPreferences.put(JaxrsPreferences.PROVIDER_MISSING_IMPLEMENTATION, JaxrsPreferences.ERROR);
	}

	@Test
	public void shouldNotReportProblemIfNoExplicitConstructor() throws CoreException, ValidationException {
		// preconditions
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final IJaxrsProvider provider = metamodel.findProvider("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final Set<IFile> changedResources = toSet( provider.getResource());
		new JaxrsMetamodelValidator().validate(changedResources, project, validationHelper, context, validatorManager,
				reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(provider);
		for (IMarker marker : markers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(0));
	}

	@Test
	public void shouldNotReportProblemIfExplicitEmptyConstructor() throws CoreException, ValidationException {
		// preconditions
		final IType providerType = replaceFirstOccurrenceOfCode(
				"org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper",
				javaProject,
				"public Response toResponse(EntityNotFoundException exception)",
				"public EntityNotFoundExceptionMapper() {} public Response toResponse(EntityNotFoundException exception)",
				false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final IJaxrsProvider provider = metamodel.findProvider(providerType);
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final Set<IFile> changedResources = toSet( provider.getResource());
		new JaxrsMetamodelValidator().validate(changedResources, project, validationHelper, context, validatorManager,
				reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(provider);
		for (IMarker marker : markers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(0));
	}

	@Test
	public void shouldReportErrorIfExplicitNonPublicEmptyConstructor() throws CoreException, ValidationException {
		// preconditions
		final IType providerType = replaceFirstOccurrenceOfCode(
				"org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper",
				javaProject,
				"public Response toResponse(EntityNotFoundException exception)",
				"EntityNotFoundExceptionMapper() {} public Response toResponse(EntityNotFoundException exception)",
				false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final IJaxrsProvider provider = metamodel.findProvider(providerType);
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation
		final Set<IFile> changedResources = toSet( provider.getResource());
		new JaxrsMetamodelValidator().validate(changedResources, project, validationHelper, context, validatorManager,
				reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(provider);
		for (IMarker marker : markers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(1));
		assertThat(markers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""), equalTo(JaxrsPreferences.PROVIDER_MISSING_VALID_CONSTRUCTOR));
		assertThat(markers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
	}

	@Test
	public void shouldReportErrorIfConstructorWithNonAnnotatedParameter() throws CoreException, ValidationException {
		// preconditions
		final IType providerType = replaceFirstOccurrenceOfCode(
				"org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper",
				javaProject,
				"public Response toResponse(EntityNotFoundException exception)",
				"public EntityNotFoundExceptionMapper(String foo) {} public Response toResponse(EntityNotFoundException exception)",
				false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final IJaxrsProvider provider = metamodel.findProvider(providerType);
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation
		final Set<IFile> changedResources = toSet( provider.getResource());
		new JaxrsMetamodelValidator().validate(changedResources, project, validationHelper, context, validatorManager,
				reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(provider);
		for (IMarker marker : markers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(1));
		assertThat(markers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""), equalTo(JaxrsPreferences.PROVIDER_MISSING_VALID_CONSTRUCTOR));
		assertThat(markers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
	}
	
	@Test
	public void shouldReportErrorIfConstructorWithInvalidAnnotatedParameterType() throws CoreException, ValidationException {
		// preconditions
		final IType providerType = replaceFirstOccurrenceOfCode(
				"org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper",
				javaProject,
				"public Response toResponse(EntityNotFoundException exception)",
				"public EntityNotFoundExceptionMapper(@Context String foo) {} public Response toResponse(EntityNotFoundException exception)",
				false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final IJaxrsProvider provider = metamodel.findProvider(providerType);
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation
		final Set<IFile> changedResources = toSet( provider.getResource());
		new JaxrsMetamodelValidator().validate(changedResources, project, validationHelper, context, validatorManager,
				reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(provider);
		for (IMarker marker : markers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(1));
		assertThat(markers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""), equalTo(JaxrsPreferences.PROVIDER_MISSING_VALID_CONSTRUCTOR));
		assertThat(markers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
	}

	@Test
	public void shouldNotReportProblemIfValidContextConstructor() throws CoreException, ValidationException {
		// preconditions
		final IType providerType = replaceFirstOccurrenceOfCode(
				"org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper",
				javaProject,
				"public Response toResponse(EntityNotFoundException exception)",
				"public EntityNotFoundExceptionMapper(@Context ServletContext context) {} public Response toResponse(EntityNotFoundException exception)",
				false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final IJaxrsProvider provider = metamodel.findProvider(providerType);
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation
		final Set<IFile> changedResources = toSet( provider.getResource());
		new JaxrsMetamodelValidator().validate(changedResources, project, validationHelper, context, validatorManager,
				reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(provider);
		for (IMarker marker : markers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(0));
	}

	@Test
	public void shouldReportErrorIfConstructorWithMixedParams() throws CoreException, ValidationException {
		// preconditions
		final IType providerType = replaceFirstOccurrenceOfCode(
				"org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper",
				javaProject,
				"public Response toResponse(EntityNotFoundException exception)",
				"public EntityNotFoundExceptionMapper(@Context ServletContext context, String foo) {} public Response toResponse(EntityNotFoundException exception)",
				false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final IJaxrsProvider provider = metamodel.findProvider(providerType);
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation
		final Set<IFile> changedResources = toSet( provider.getResource());
		new JaxrsMetamodelValidator().validate(changedResources, project, validationHelper, context, validatorManager,
				reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(provider);
		for (IMarker marker : markers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(1));
		assertThat(markers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""), equalTo(JaxrsPreferences.PROVIDER_MISSING_VALID_CONSTRUCTOR));
		assertThat(markers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
	}

	@Test
	public void shouldReportWarningIfTwoSameExceptionMappersExist() throws CoreException, ValidationException {
		// preconditions
		// adding another Provider with same characteristics
		final ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit(
				"EntityNotFoundExceptionMapper2.txt", "org.jboss.tools.ws.jaxrs.sample.services.providers",
				"EntityNotFoundExceptionMapper2.java");
		metamodelMonitor.createElements(
				"org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper",
				"org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper2");
		final IJaxrsProvider otherProvider = metamodel.findProvider(compilationUnit.findPrimaryType());
		final IJaxrsProvider provider = metamodel.findProvider("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation
		final Set<IFile> changedResources = toSet( provider.getResource(),
				(IFile) otherProvider.getResource());
		new JaxrsMetamodelValidator().validate(changedResources, project, validationHelper, context, validatorManager,
				reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(provider, otherProvider);
		for (IMarker marker : markers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(2));
		for(IMarker marker : markers) {
			assertThat(marker.getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""), equalTo(JaxrsPreferences.PROVIDER_DUPLICATE_EXCEPTION_MAPPER));
			assertThat(marker.getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
		}
	}

	@Test
	public void shouldReportWarningIfTwoSameEntityMappersWithIdenticalMediaTypesExist() throws CoreException, ValidationException {
		// preconditions
		// adding another Provider with same characteristics
		final ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit(
				"DummyProvider2.txt", "org.jboss.tools.ws.jaxrs.sample.services",
				"DummyProvider2.java");
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.extra.DummyProvider", "org.jboss.tools.ws.jaxrs.sample.services.DummyProvider2");
		final IJaxrsProvider provider = metamodel.findProvider("org.jboss.tools.ws.jaxrs.sample.extra.DummyProvider");
		final IJaxrsProvider otherProvider = metamodel.findProvider(compilationUnit.findPrimaryType());
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation
		final Set<IFile> changedResources = toSet( provider.getResource(),
				(IFile) otherProvider.getResource());
		new JaxrsMetamodelValidator().validate(changedResources, project, validationHelper, context, validatorManager,
				reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(provider, otherProvider);
		for (IMarker marker : markers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(4));
		for(IMarker marker: markers) {
			assertThat(marker.getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""), isIn(Arrays.asList(
					JaxrsPreferences.PROVIDER_DUPLICATE_MESSAGE_BODY_READER,
					JaxrsPreferences.PROVIDER_DUPLICATE_MESSAGE_BODY_WRITER)));
			assertThat(marker.getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
		}
	}

	@Test
	public void shouldReportWarningIfTwoSameEntityMappersWithOverlappingMediasTypeExist() throws CoreException, ValidationException {
		// preconditions
		// adding another Provider with same characteristics
		final ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit(
				"DummyProvider4.txt", "org.jboss.tools.ws.jaxrs.sample.services",
				"DummyProvider4.java");
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.extra.DummyProvider", "org.jboss.tools.ws.jaxrs.sample.services.DummyProvider4");
		final IJaxrsProvider provider = metamodel.findProvider("org.jboss.tools.ws.jaxrs.sample.extra.DummyProvider");
		final IJaxrsProvider otherProvider = metamodel.findProvider(compilationUnit.findPrimaryType());
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation
		final Set<IFile> changedResources = toSet( provider.getResource(),
				(IFile) otherProvider.getResource());
		new JaxrsMetamodelValidator().validate(changedResources, project, validationHelper, context, validatorManager,
				reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(provider, otherProvider);
		for (IMarker marker : markers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		// both @Consumes and @Produces collides on both Providers
		assertThat(markers.length, equalTo(4));
		for(IMarker marker: markers) {
			assertThat(marker.getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""), isIn(Arrays.asList(
					JaxrsPreferences.PROVIDER_DUPLICATE_MESSAGE_BODY_READER,
					JaxrsPreferences.PROVIDER_DUPLICATE_MESSAGE_BODY_WRITER)));
			assertThat(marker.getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
		}
	}
	
	@Test
	public void shouldNotReportProblemIfTwoSameEntityMappersWithDifferentMediaTypesExist() throws CoreException, ValidationException {
		// preconditions
		final ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit(
				"DummyProvider3.txt", "org.jboss.tools.ws.jaxrs.sample.services",
				"DummyProvider3.java");
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.extra.DummyProvider", "org.jboss.tools.ws.jaxrs.sample.services.DummyProvider3");
		final IJaxrsProvider provider = metamodel.findProvider("org.jboss.tools.ws.jaxrs.sample.extra.DummyProvider");
		deleteJaxrsMarkers(project);
		final IJaxrsProvider otherProvider = metamodel.findProvider(compilationUnit.findPrimaryType());
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation
		final Set<IFile> changedResources = toSet( provider.getResource(),
				(IFile) otherProvider.getResource());
		new JaxrsMetamodelValidator().validate(changedResources, project, validationHelper, context, validatorManager,
				reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(provider, otherProvider);
		for (IMarker marker : markers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(0));
	}
	
	@Test
	public void shouldReportWarningIfProviderAnnotationIsMissing() throws CoreException, ValidationException {
		// preconditions
		final IType providerType = replaceFirstOccurrenceOfCode(
				"org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper",
				javaProject,
				"@Provider", "",
				false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final IJaxrsProvider provider = metamodel.findProvider(providerType);
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation
		final Set<IFile> changedResources = toSet( provider.getResource());
		new JaxrsMetamodelValidator().validate(changedResources, project, validationHelper, context, validatorManager,
				reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(provider);
		for (IMarker marker : markers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(1));
		assertThat(markers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""), equalTo(JaxrsPreferences.PROVIDER_MISSING_ANNOTATION));
		assertThat(markers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
	}

	@Test
	public void shouldReportWarningIfProviderInterfaceImplementationIsMissing() throws CoreException,
			ValidationException {
		// preconditions
		final IType providerType = replaceFirstOccurrenceOfCode(
				"org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper",
				javaProject,
				"ExceptionMapper<EntityNotFoundException>", "",
				false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final IJaxrsProvider provider = metamodel.findProvider(providerType);
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation
		final Set<IFile> changedResources = toSet( provider.getResource());
		new JaxrsMetamodelValidator().validate(changedResources, project, validationHelper, context, validatorManager,
				reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(provider);
		for (IMarker marker : markers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(1));
		assertThat(markers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""), equalTo(JaxrsPreferences.PROVIDER_MISSING_IMPLEMENTATION));
		assertThat(markers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
	}

	@Test
	public void shouldNotReportProblemIfImplementedTypesAreUnknown() throws CoreException, ValidationException {
		// preconditions
		final IType providerType = replaceFirstOccurrenceOfCode(
				"org.jboss.tools.ws.jaxrs.sample.extra.DummyProvider",
				javaProject,
				"AbstractEntityProvider<String, Number>",
				"AbstractEntityProvider<Foo, Bar>",
				false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.extra.DummyProvider");
		final IJaxrsProvider provider = metamodel.findProvider(providerType);
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation: no need to report further problem, the JDT validation should already report compilation errors.
		final IMarker[] markers = findJaxrsMarkers(provider);
		assertThat(markers.length, equalTo(0));
	}

	@Test
	public void shouldNotReportProblemIfImplementedTypeIsUnknown() throws CoreException, ValidationException {
		// preconditions
		final IType providerType = replaceFirstOccurrenceOfCode(
				"org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper",
				javaProject,
				"ExceptionMapper<EntityNotFoundException>",
				"ExceptionMapper<Foobar>",
				false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final IJaxrsProvider provider = metamodel.findProvider(providerType);
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation: no need to report further problems, JDT Validation already reports compilation errors
		final IMarker[] markers = findJaxrsMarkers(provider);
		assertThat(markers.length, equalTo(0));
	}

	@Test
	public void shouldNotFailOnProblemIfSeverityLevelIsIgnore() throws CoreException, ValidationException {
		// preconditions
		final IType providerType = replaceFirstOccurrenceOfCode(
				"org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper",
				javaProject,
				"ExceptionMapper<EntityNotFoundException>",
				"ExceptionMapper<Foobar>",
				false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final IJaxrsProvider provider = metamodel.findProvider(providerType);
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();
		final IEclipsePreferences defaultPreferences = ((IScopeContext)DefaultScope.INSTANCE).getNode(JBossJaxrsUIPlugin.PLUGIN_ID);
		defaultPreferences.put(JaxrsPreferences.PROVIDER_MISSING_IMPLEMENTATION, JaxrsPreferences.IGNORE);
		
		// operation
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(provider);
		assertThat(markers.length, equalTo(0));
	}
	
}
