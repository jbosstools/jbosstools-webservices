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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isIn;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils.removeFirstOccurrenceOfCode;
import static org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils.replaceAllOccurrencesOfCode;
import static org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils.replaceFirstOccurrenceOfCode;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PROVIDER;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.deleteJaxrsMarkers;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.findJaxrsMarkers;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.toSet;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
import org.eclipse.jdt.core.IType;
import org.eclipse.wst.validation.ReporterHelper;
import org.eclipse.wst.validation.internal.core.ValidationException;
import org.eclipse.wst.validation.internal.provisional.core.IReporter;
import org.jboss.tools.common.validation.ContextValidationHelper;
import org.jboss.tools.common.validation.IProjectValidationContext;
import org.jboss.tools.common.validation.ValidatorManager;
import org.jboss.tools.common.validation.internal.ProjectValidationContext;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.AbstractJaxrsBaseElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsProvider;
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsProvider;
import org.jboss.tools.ws.jaxrs.core.utils.Annotation;
import org.jboss.tools.ws.jaxrs.ui.JBossJaxrsUIPlugin;
import org.jboss.tools.ws.jaxrs.ui.preferences.JaxrsPreferences;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Xi
 * 
 */
@SuppressWarnings("restriction")
public class Jaxrs11ProviderValidatorTestCase {

	private static final Logger LOGGER = LoggerFactory.getLogger(Jaxrs11ProviderValidatorTestCase.class);
	
	private final IReporter reporter = new ReporterHelper(new NullProgressMonitor());
	private final ContextValidationHelper validationHelper = new ContextValidationHelper();
	private final IProjectValidationContext context = new ProjectValidationContext();
	private final ValidatorManager validatorManager = new ValidatorManager();

	protected void removeAllElementsExcept(final IJaxrsElement... elementsToKeep) throws CoreException {

		final Set<String> resourcesToKeep = new HashSet<String>();
		for (IJaxrsElement element : elementsToKeep) {
			resourcesToKeep.add(element.getIdentifier());
		}
		final List<IJaxrsElement> allElements = metamodel.getAllElements();
		for (Iterator<IJaxrsElement> iterator = allElements.iterator(); iterator.hasNext();) {
			AbstractJaxrsBaseElement element = (AbstractJaxrsBaseElement)iterator.next();
			if (element.getResource() == null || !resourcesToKeep.contains(element.getIdentifier())) {
				element.remove();
			}
		}
	}
	
	@ClassRule
	public static WorkspaceSetupRule workspaceSetupRule = new WorkspaceSetupRule("org.jboss.tools.ws.jaxrs.tests.sampleproject");
	
	@Rule
	public JaxrsMetamodelMonitor metamodelMonitor = new JaxrsMetamodelMonitor("org.jboss.tools.ws.jaxrs.tests.sampleproject", true);
	
	private JaxrsMetamodel metamodel = null;

	private IProject project = null;

	@Before
	public void setup() throws CoreException {
		metamodel = metamodelMonitor.getMetamodel();
		project = metamodel.getProject();
	}
	
	@After
	public void resetProblemLevelPreferences() {
		final IEclipsePreferences defaultPreferences = ((IScopeContext)DefaultScope.INSTANCE).getNode(JBossJaxrsUIPlugin.PLUGIN_ID);
		defaultPreferences.put(JaxrsPreferences.PROVIDER_MISSING_IMPLEMENTATION, JaxrsPreferences.ERROR);
	}

	@Test
	public void shouldNotReportProblemIfNoExplicitConstructor() throws CoreException, ValidationException {
		// preconditions
		final IType providerType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final IJaxrsProvider provider = metamodel.findProvider(providerType);
		removeAllElementsExcept(provider);
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final Set<IFile> changedResources = toSet( provider.getResource());
		new JaxrsMetamodelValidator().validate(changedResources, project, validationHelper, context, validatorManager,
				reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(provider);
		for (IMarker marker : markers) {
			LOGGER.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(0));
	}

	@Test
	public void shouldNotReportProblemIfExplicitEmptyConstructor() throws CoreException, ValidationException {
		// preconditions
		final IType providerType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final IJaxrsProvider provider = metamodel.findProvider(providerType);
		replaceFirstOccurrenceOfCode(
						providerType,
						"public Response toResponse(EntityNotFoundException exception)",
						"public EntityNotFoundExceptionMapper() {} public Response toResponse(EntityNotFoundException exception)",
						false);
		removeAllElementsExcept(provider);
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final Set<IFile> changedResources = toSet( provider.getResource());
		new JaxrsMetamodelValidator().validate(changedResources, project, validationHelper, context, validatorManager,
				reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(provider);
		for (IMarker marker : markers) {
			LOGGER.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(0));
	}

	@Test
	public void shouldReportErrorIfExplicitNonPublicEmptyConstructor() throws CoreException, ValidationException {
		// preconditions
		final IType providerType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final IJaxrsProvider provider = metamodel.findProvider(providerType);
		replaceFirstOccurrenceOfCode(providerType,
				"public Response toResponse(EntityNotFoundException exception)",
				"EntityNotFoundExceptionMapper() {} public Response toResponse(EntityNotFoundException exception)",
				false);
		removeAllElementsExcept(provider);
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final Set<IFile> changedResources = toSet( provider.getResource());
		new JaxrsMetamodelValidator().validate(changedResources, project, validationHelper, context, validatorManager,
				reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(provider);
		for (IMarker marker : markers) {
			LOGGER.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(1));
		assertThat(markers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""), equalTo(JaxrsPreferences.PROVIDER_MISSING_VALID_CONSTRUCTOR));
	}

	@Test
	public void shouldReportErrorIfConstructorWithNonAnnotatedParameter() throws CoreException, ValidationException {
		// preconditions
		final IType providerType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final IJaxrsProvider provider = metamodel.findProvider(providerType);
		replaceFirstOccurrenceOfCode(
						providerType,
						"public Response toResponse(EntityNotFoundException exception)",
						"public EntityNotFoundExceptionMapper(String foo) {} public Response toResponse(EntityNotFoundException exception)",
						false);
		removeAllElementsExcept(provider);
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final Set<IFile> changedResources = toSet( provider.getResource());
		new JaxrsMetamodelValidator().validate(changedResources, project, validationHelper, context, validatorManager,
				reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(provider);
		for (IMarker marker : markers) {
			LOGGER.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(1));
		assertThat(markers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""), equalTo(JaxrsPreferences.PROVIDER_MISSING_VALID_CONSTRUCTOR));
	}
	
	@Test
	public void shouldReportErrorIfConstructorWithInvalidAnnotatedParameterType() throws CoreException, ValidationException {
		// preconditions
		final IType providerType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final IJaxrsProvider provider = metamodel.findProvider(providerType);
		replaceFirstOccurrenceOfCode(
						providerType,
						"public Response toResponse(EntityNotFoundException exception)",
						"public EntityNotFoundExceptionMapper(@Context HttpServletRequest request) {} public Response toResponse(EntityNotFoundException exception)",
						false);
		removeAllElementsExcept(provider);
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final Set<IFile> changedResources = toSet( provider.getResource());
		new JaxrsMetamodelValidator().validate(changedResources, project, validationHelper, context, validatorManager,
				reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(provider);
		for (IMarker marker : markers) {
			LOGGER.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(1));
		assertThat(markers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""), equalTo(JaxrsPreferences.PROVIDER_MISSING_VALID_CONSTRUCTOR));
	}

	@Test
	public void shouldNotReportProblemIfValidContextConstructor() throws CoreException, ValidationException {
		// preconditions
		final IType providerType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final IJaxrsProvider provider = metamodel.findProvider(providerType);
		replaceFirstOccurrenceOfCode(
						providerType,
						"public Response toResponse(EntityNotFoundException exception)",
						"public EntityNotFoundExceptionMapper(@Context ServletContext context) {} public Response toResponse(EntityNotFoundException exception)",
						false);
		removeAllElementsExcept(provider);
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final Set<IFile> changedResources = toSet( provider.getResource());
		new JaxrsMetamodelValidator().validate(changedResources, project, validationHelper, context, validatorManager,
				reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(provider);
		for (IMarker marker : markers) {
			LOGGER.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(0));
	}

	@Test
	public void shouldReportErrorIfConstructorWithMixedParams() throws CoreException, ValidationException {
		// preconditions
		final IType providerType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final IJaxrsProvider provider = metamodel.findProvider(providerType);
		replaceFirstOccurrenceOfCode(
				providerType,
				"public Response toResponse(EntityNotFoundException exception)",
				"public EntityNotFoundExceptionMapper(@Context ServletContext context, String foo) {} public Response toResponse(EntityNotFoundException exception)",
				false);
		removeAllElementsExcept(provider);
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final Set<IFile> changedResources = toSet( provider.getResource());
		new JaxrsMetamodelValidator().validate(changedResources, project, validationHelper, context, validatorManager,
				reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(provider);
		for (IMarker marker : markers) {
			LOGGER.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(1));
		assertThat(markers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""), equalTo(JaxrsPreferences.PROVIDER_MISSING_VALID_CONSTRUCTOR));
	}

	@Test
	public void shouldReportWarningIfTwoSameExceptionMappersExist() throws CoreException, ValidationException {
		// preconditions
		// adding another Provider with same characteristics
		final IType providerType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final IJaxrsProvider provider = metamodel.findProvider(providerType);
		// adding a new compilation unit automatically triggers a project build
		// which creates a new JAX-RS Provider
		final ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit(
				"EntityNotFoundExceptionMapper2.txt", "org.jboss.tools.ws.jaxrs.sample.services.providers",
				"EntityNotFoundExceptionMapper2.java");
		final IJaxrsProvider otherProvider = metamodel.findProvider(compilationUnit.findPrimaryType());
		removeAllElementsExcept(provider, otherProvider);
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
			LOGGER.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(2));
		for(IMarker marker : markers) {
			assertThat(marker.getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""), equalTo(JaxrsPreferences.PROVIDER_DUPLICATE_EXCEPTION_MAPPER));
		}
	}

	@Test
	public void shouldReportWarningIfTwoSameEntityMappersWithIdenticalMediaTypesExist() throws CoreException, ValidationException {
		// preconditions
		// adding another Provider with same characteristics
		final IType providerType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.extra.DummyProvider");
		final IJaxrsProvider provider = metamodel.findProvider(providerType);
		// adding a new compilation unit automatically triggers a project build
		// which creates a new JAX-RS Provider
		final ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit(
				"DummyProvider2.txt", "org.jboss.tools.ws.jaxrs.sample.services",
				"DummyProvider2.java");
		final IJaxrsProvider otherProvider = metamodel.findProvider(compilationUnit.findPrimaryType());
		removeAllElementsExcept(provider, otherProvider);
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
			LOGGER.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(4));
		for(IMarker marker: markers) {
			assertThat(marker.getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""), isIn(Arrays.asList(
					JaxrsPreferences.PROVIDER_DUPLICATE_MESSAGE_BODY_READER,
					JaxrsPreferences.PROVIDER_DUPLICATE_MESSAGE_BODY_WRITER)));
		}
	}

	@Test
	public void shouldReportWarningIfTwoSameEntityMappersWithOverlappingMediasTypeExist() throws CoreException, ValidationException {
		// preconditions
		// adding another Provider with same characteristics
		final IType providerType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.extra.DummyProvider");
		final IJaxrsProvider provider = metamodel.findProvider(providerType);
		// adding a new compilation unit automatically triggers a project build
		// which creates a new JAX-RS Provider
		final ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit(
				"DummyProvider4.txt", "org.jboss.tools.ws.jaxrs.sample.services",
				"DummyProvider4.java");
		final IJaxrsProvider otherProvider = metamodel.findProvider(compilationUnit.findPrimaryType());
		removeAllElementsExcept(provider, otherProvider);
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
			LOGGER.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		// both @Consumes and @Produces collides on both Providers
		assertThat(markers.length, equalTo(4));
		for(IMarker marker: markers) {
			assertThat(marker.getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""), isIn(Arrays.asList(
					JaxrsPreferences.PROVIDER_DUPLICATE_MESSAGE_BODY_READER,
					JaxrsPreferences.PROVIDER_DUPLICATE_MESSAGE_BODY_WRITER)));
		}
	}
	
	@Test
	public void shouldNotReportProblemIfTwoSameEntityMappersWithDifferentMediaTypesExist() throws CoreException, ValidationException {
		// preconditions
		// adding another Provider with same characteristics
		final IType providerType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.extra.DummyProvider");
		final IJaxrsProvider provider = metamodel.findProvider(providerType);
		// adding a new compilation unit automatically triggers a project build
		// which creates a new JAX-RS Provider
		final ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit(
				"DummyProvider3.txt", "org.jboss.tools.ws.jaxrs.sample.services",
				"DummyProvider3.java");
		final IJaxrsProvider otherProvider = metamodel.findProvider(compilationUnit.findPrimaryType());
		removeAllElementsExcept(provider, otherProvider);
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
			LOGGER.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(0));
	}
	
	@Test
	public void shouldReportWarningIfProviderAnnotationIsMissing() throws CoreException, ValidationException {
		// preconditions
		final IType providerType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(providerType);
		final Annotation providerAnnotation = provider.getAnnotation(PROVIDER);
		provider.removeAnnotation(providerAnnotation.getJavaAnnotation());
		removeAllElementsExcept(provider);
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final Set<IFile> changedResources = toSet( provider.getResource());
		new JaxrsMetamodelValidator().validate(changedResources, project, validationHelper, context, validatorManager,
				reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(provider);
		for (IMarker marker : markers) {
			LOGGER.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(1));
		assertThat(markers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""), equalTo(JaxrsPreferences.PROVIDER_MISSING_ANNOTATION));
	}

	@Test
	public void shouldReportWarningIfProviderInterfaceImplementationIsMissing() throws CoreException,
			ValidationException {
		// preconditions
		final IType providerType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		removeFirstOccurrenceOfCode(providerType, "ExceptionMapper<EntityNotFoundException>", false);
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(providerType);
		removeAllElementsExcept(provider);
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final Set<IFile> changedResources = toSet( provider.getResource());
		new JaxrsMetamodelValidator().validate(changedResources, project, validationHelper, context, validatorManager,
				reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(provider);
		for (IMarker marker : markers) {
			LOGGER.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(1));
		assertThat(markers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""), equalTo(JaxrsPreferences.PROVIDER_MISSING_IMPLEMENTATION));
	}

	@Test
	public void shouldNotReportProblemIfImplementedTypesAreUnknown() throws CoreException, ValidationException {
		// preconditions
		final IType providerType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.extra.DummyProvider");
		replaceAllOccurrencesOfCode(providerType, "AbstractEntityProvider<String, Number>",
				"AbstractEntityProvider<Foo, Bar>", false);
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(providerType);
		removeAllElementsExcept(provider);
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
		final IType providerType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		replaceFirstOccurrenceOfCode(providerType, "ExceptionMapper<EntityNotFoundException>",
				"ExceptionMapper<Foobar>", false);
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(providerType);
		removeAllElementsExcept(provider);
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
		final IType providerType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		replaceFirstOccurrenceOfCode(providerType, "ExceptionMapper<EntityNotFoundException>",
				"ExceptionMapper<Foobar>", false);
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(providerType);
		removeAllElementsExcept(provider);
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
