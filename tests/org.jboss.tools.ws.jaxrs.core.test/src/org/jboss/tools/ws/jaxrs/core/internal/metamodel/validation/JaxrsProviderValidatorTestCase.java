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

import static org.hamcrest.Matchers.equalTo;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.validation.ValidationUtils.deleteJaxrsMarkers;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.validation.ValidationUtils.findJaxrsMarkers;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.validation.ValidationUtils.toSet;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.PROVIDER;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
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
import org.jboss.tools.ws.jaxrs.core.JBossJaxrsCorePlugin;
import org.jboss.tools.ws.jaxrs.core.WorkbenchUtils;
import org.jboss.tools.ws.jaxrs.core.builder.AbstractMetamodelBuilderTestCase;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsBaseElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsProvider;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsProvider;
import org.jboss.tools.ws.jaxrs.core.preferences.JaxrsPreferences;
import org.junit.After;
import org.junit.Test;

/**
 * @author Xi
 * 
 */
@SuppressWarnings("restriction")
public class JaxrsProviderValidatorTestCase extends AbstractMetamodelBuilderTestCase {

	private final IReporter reporter = new ReporterHelper(new NullProgressMonitor());
	private final ContextValidationHelper validationHelper = new ContextValidationHelper();
	private final IProjectValidationContext context = new ProjectValidationContext();
	private final ValidatorManager validatorManager = new ValidatorManager();

	protected void removeAllElementsExcept(final IJaxrsElement... elementsToKeep) throws CoreException {

		final Set<String> resourcesToKeep = new HashSet<String>();
		for (IJaxrsElement element : elementsToKeep) {
			resourcesToKeep.add(element.getIdentifier());
		}
		final List<IJaxrsElement> allElements = new ArrayList<IJaxrsElement>(metamodel.getAllElements());
		for (Iterator<IJaxrsElement> iterator = allElements.iterator(); iterator.hasNext();) {
			JaxrsBaseElement element = (JaxrsBaseElement)iterator.next();
			if (element.getResource() == null || !resourcesToKeep.contains(element.getIdentifier())) {
				element.remove();
			}
		}
	}
	
	@After
	public void resetProblemLevelPreferences() {
		final IEclipsePreferences defaultPreferences = ((IScopeContext)DefaultScope.INSTANCE).getNode(JBossJaxrsCorePlugin.PLUGIN_ID);
		defaultPreferences.put(JaxrsPreferences.PROVIDER_MISSING_IMPLEMENTATION, JaxrsPreferences.ERROR);
	}

	@Test
	public void shouldNotReportErrorIfNoExplicitConstructor() throws CoreException, ValidationException {
		// preconditions
		final IType providerType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final IJaxrsProvider provider = metamodel.findProvider(providerType);
		removeAllElementsExcept(provider);
		deleteJaxrsMarkers(project);
		resetElementChangesNotifications();
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
	}

	@Test
	public void shouldNotReportErrorIfExplicitEmptyConstructor() throws CoreException, ValidationException {
		// preconditions
		final IType providerType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final IJaxrsProvider provider = metamodel.findProvider(providerType);
		WorkbenchUtils
				.replaceFirstOccurrenceOfCode(
						providerType,
						"public Response toResponse(EntityNotFoundException exception)",
						"public EntityNotFoundExceptionMapper() {} public Response toResponse(EntityNotFoundException exception)",
						false);
		removeAllElementsExcept(provider);
		deleteJaxrsMarkers(project);
		resetElementChangesNotifications();
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
	}

	@Test
	public void shouldReportErrorIfExplicitNonPublicEmptyConstructor() throws CoreException, ValidationException {
		// preconditions
		final IType providerType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final IJaxrsProvider provider = metamodel.findProvider(providerType);
		WorkbenchUtils.replaceFirstOccurrenceOfCode(providerType,
				"public Response toResponse(EntityNotFoundException exception)",
				"EntityNotFoundExceptionMapper() {} public Response toResponse(EntityNotFoundException exception)",
				false);
		removeAllElementsExcept(provider);
		deleteJaxrsMarkers(project);
		resetElementChangesNotifications();
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
	}

	@Test
	public void shouldReportErrorIfConstructorWithNonAnnotatedParameter() throws CoreException, ValidationException {
		// preconditions
		final IType providerType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final IJaxrsProvider provider = metamodel.findProvider(providerType);
		WorkbenchUtils
				.replaceFirstOccurrenceOfCode(
						providerType,
						"public Response toResponse(EntityNotFoundException exception)",
						"public EntityNotFoundExceptionMapper(String foo) {} public Response toResponse(EntityNotFoundException exception)",
						false);
		removeAllElementsExcept(provider);
		deleteJaxrsMarkers(project);
		resetElementChangesNotifications();
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
	}
	
	@Test
	public void shouldReportErrorIfConstructorWithInvalidAnnotatedParameterType() throws CoreException, ValidationException {
		// preconditions
		final IType providerType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final IJaxrsProvider provider = metamodel.findProvider(providerType);
		WorkbenchUtils
				.replaceFirstOccurrenceOfCode(
						providerType,
						"public Response toResponse(EntityNotFoundException exception)",
						"public EntityNotFoundExceptionMapper(@Context HttpServletRequest request) {} public Response toResponse(EntityNotFoundException exception)",
						false);
		removeAllElementsExcept(provider);
		deleteJaxrsMarkers(project);
		resetElementChangesNotifications();
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
	}

	@Test
	public void shouldNotReportErrorIfValidContextConstructor() throws CoreException, ValidationException {
		// preconditions
		final IType providerType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final IJaxrsProvider provider = metamodel.findProvider(providerType);
		WorkbenchUtils
				.replaceFirstOccurrenceOfCode(
						providerType,
						"public Response toResponse(EntityNotFoundException exception)",
						"public EntityNotFoundExceptionMapper(@Context ServletContext context) {} public Response toResponse(EntityNotFoundException exception)",
						false);
		removeAllElementsExcept(provider);
		deleteJaxrsMarkers(project);
		resetElementChangesNotifications();
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
		final IType providerType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final IJaxrsProvider provider = metamodel.findProvider(providerType);
		WorkbenchUtils
		.replaceFirstOccurrenceOfCode(
				providerType,
				"public Response toResponse(EntityNotFoundException exception)",
				"public EntityNotFoundExceptionMapper(@Context ServletContext context, String foo) {} public Response toResponse(EntityNotFoundException exception)",
				false);
		removeAllElementsExcept(provider);
		deleteJaxrsMarkers(project);
		resetElementChangesNotifications();
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
	}

	@Test
	public void shouldReportWarningIfTwoSameExceptionMappersExist() throws CoreException, ValidationException {
		// preconditions
		// adding another Provider with same characteristics
		final IType providerType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final IJaxrsProvider provider = metamodel.findProvider(providerType);
		// adding a new compilation unit automatically triggers a project build
		// which creates a new JAX-RS Provider
		final ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject,
				"EntityNotFoundExceptionMapper2.txt", "org.jboss.tools.ws.jaxrs.sample.services.providers",
				"EntityNotFoundExceptionMapper2.java");
		final IJaxrsProvider otherProvider = metamodel.findProvider(compilationUnit.findPrimaryType());
		removeAllElementsExcept(provider, otherProvider);
		deleteJaxrsMarkers(project);
		resetElementChangesNotifications();
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
	}

	@Test
	public void shouldReportWarningIfTwoSameEntityMappersWithIdenticalMediaTypesExist() throws CoreException, ValidationException {
		// preconditions
		// adding another Provider with same characteristics
		final IType providerType = resolveType("org.jboss.tools.ws.jaxrs.sample.extra.DummyProvider");
		final IJaxrsProvider provider = metamodel.findProvider(providerType);
		// adding a new compilation unit automatically triggers a project build
		// which creates a new JAX-RS Provider
		final ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject,
				"DummyProvider2.txt", "org.jboss.tools.ws.jaxrs.sample.services",
				"DummyProvider2.java");
		final IJaxrsProvider otherProvider = metamodel.findProvider(compilationUnit.findPrimaryType());
		removeAllElementsExcept(provider, otherProvider);
		deleteJaxrsMarkers(project);
		resetElementChangesNotifications();
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
	}

	@Test
	public void shouldReportWarningIfTwoSameEntityMappersWithOverlappingMediasTypeExist() throws CoreException, ValidationException {
		// preconditions
		// adding another Provider with same characteristics
		final IType providerType = resolveType("org.jboss.tools.ws.jaxrs.sample.extra.DummyProvider");
		final IJaxrsProvider provider = metamodel.findProvider(providerType);
		// adding a new compilation unit automatically triggers a project build
		// which creates a new JAX-RS Provider
		final ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject,
				"DummyProvider4.txt", "org.jboss.tools.ws.jaxrs.sample.services",
				"DummyProvider4.java");
		final IJaxrsProvider otherProvider = metamodel.findProvider(compilationUnit.findPrimaryType());
		removeAllElementsExcept(provider, otherProvider);
		deleteJaxrsMarkers(project);
		resetElementChangesNotifications();
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
	}
	
	@Test
	public void shouldNotReportWarningIfTwoSameEntityMappersWithDifferentMediaTypesExist() throws CoreException, ValidationException {
		// preconditions
		// adding another Provider with same characteristics
		final IType providerType = resolveType("org.jboss.tools.ws.jaxrs.sample.extra.DummyProvider");
		final IJaxrsProvider provider = metamodel.findProvider(providerType);
		// adding a new compilation unit automatically triggers a project build
		// which creates a new JAX-RS Provider
		final ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject,
				"DummyProvider3.txt", "org.jboss.tools.ws.jaxrs.sample.services",
				"DummyProvider3.java");
		final IJaxrsProvider otherProvider = metamodel.findProvider(compilationUnit.findPrimaryType());
		removeAllElementsExcept(provider, otherProvider);
		deleteJaxrsMarkers(project);
		resetElementChangesNotifications();
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
		final IType providerType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(providerType);
		final Annotation providerAnnotation = provider.getAnnotation(PROVIDER.qualifiedName);
		provider.removeAnnotation(providerAnnotation.getJavaAnnotation());
		removeAllElementsExcept(provider);
		deleteJaxrsMarkers(project);
		resetElementChangesNotifications();
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
	}

	@Test
	public void shouldReportWarningIfProviderInterfaceImplementationIsMissing() throws CoreException,
			ValidationException {
		// preconditions
		final IType providerType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		WorkbenchUtils.removeFirstOccurrenceOfCode(providerType, "ExceptionMapper<EntityNotFoundException>", false);
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(providerType);
		removeAllElementsExcept(provider);
		deleteJaxrsMarkers(project);
		resetElementChangesNotifications();
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
	}

	@Test
	public void shouldReportWarningIfImplementedTypesAreUnknown() throws CoreException, ValidationException {
		// preconditions
		final IType providerType = resolveType("org.jboss.tools.ws.jaxrs.sample.extra.DummyProvider");
		WorkbenchUtils.replaceAllOccurrencesOfCode(providerType, "AbstractEntityProvider<String, Number>",
				"AbstractEntityProvider<Foo, Bar>", false);
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(providerType);
		removeAllElementsExcept(provider);
		deleteJaxrsMarkers(project);
		resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(provider);
		for (IMarker marker : markers) {
			LOGGER.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(1));
	}

	@Test
	public void shouldReportWarningIfImplementedTypeIsUnknown() throws CoreException, ValidationException {
		// preconditions
		final IType providerType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		WorkbenchUtils.replaceFirstOccurrenceOfCode(providerType, "ExceptionMapper<EntityNotFoundException>",
				"ExceptionMapper<Foobar>", false);
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(providerType);
		removeAllElementsExcept(provider);
		deleteJaxrsMarkers(project);
		resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(provider);
		for (IMarker marker : markers) {
			LOGGER.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(1));
	}

	@Test
	public void shouldNotFailOnProblemIfSeverityLevelIsIgnore() throws CoreException, ValidationException {
		// preconditions
		final IType providerType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		WorkbenchUtils.replaceFirstOccurrenceOfCode(providerType, "ExceptionMapper<EntityNotFoundException>",
				"ExceptionMapper<Foobar>", false);
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(providerType);
		removeAllElementsExcept(provider);
		deleteJaxrsMarkers(project);
		resetElementChangesNotifications();
		final IEclipsePreferences defaultPreferences = ((IScopeContext)DefaultScope.INSTANCE).getNode(JBossJaxrsCorePlugin.PLUGIN_ID);
		defaultPreferences.put(JaxrsPreferences.PROVIDER_MISSING_IMPLEMENTATION, JaxrsPreferences.IGNORE);
		// operation
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(provider);
		assertThat(markers.length, equalTo(0));
	}
	
	@Test
	public void shouldIncreaseAndResetProblemLevelOnHttpMethod() throws CoreException, ValidationException {
		// preconditions
		final IType providerType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		WorkbenchUtils.replaceFirstOccurrenceOfCode(providerType, "ExceptionMapper<EntityNotFoundException>",
				"ExceptionMapper<Foobar>", false);
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(providerType);
		removeAllElementsExcept(provider);
		deleteJaxrsMarkers(project);
		resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(provider.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// verification: problem level is set to '2'
		assertThat(provider.getProblemLevel(), equalTo(2));
		// now, fix the problem 
		WorkbenchUtils.replaceFirstOccurrenceOfCode(providerType, "ExceptionMapper<Foobar>",
				"ExceptionMapper<EntityNotFoundException>", false);
		// revalidate
		new JaxrsMetamodelValidator().validate(toSet(provider.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// verification: problem level is set to '0'
		assertThat(provider.getProblemLevel(), equalTo(0));
	}


}
