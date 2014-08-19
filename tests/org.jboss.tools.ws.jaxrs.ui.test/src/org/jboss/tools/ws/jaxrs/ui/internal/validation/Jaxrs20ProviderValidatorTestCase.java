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
import static org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils.replaceFirstOccurrenceOfCode;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.deleteJaxrsMarkers;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.findJaxrsMarkers;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.matchesLocation;
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
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.wst.validation.ReporterHelper;
import org.eclipse.wst.validation.internal.core.ValidationException;
import org.eclipse.wst.validation.internal.provisional.core.IReporter;
import org.jboss.tools.common.validation.ContextValidationHelper;
import org.jboss.tools.common.validation.IProjectValidationContext;
import org.jboss.tools.common.validation.ValidatorManager;
import org.jboss.tools.common.validation.internal.ProjectValidationContext;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsJavaApplication;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsProvider;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.TestWatcher;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementCategory;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsApplication;
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
public class Jaxrs20ProviderValidatorTestCase {
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
	private IJavaProject javaProject = null;

	@Rule
	public TestWatcher watcher = new TestWatcher();

	@Before
	public void setup() throws CoreException {
		metamodel = metamodelMonitor.getMetamodel();
		project = metamodel.getProject();
		javaProject = metamodel.getJavaProject();
	}

	@After
	public void resetProblemLevelPreferences() {
		final IEclipsePreferences defaultPreferences = ((IScopeContext) DefaultScope.INSTANCE)
				.getNode(JBossJaxrsUIPlugin.PLUGIN_ID);
		defaultPreferences.put(JaxrsPreferences.PROVIDER_MISSING_IMPLEMENTATION, JaxrsPreferences.ERROR);
	}

	@Test
	public void shouldNotReportProblemOnContainerRequestFilter() throws CoreException, ValidationException {
		// preconditions
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomRequestFilter");
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomRequestFilter",
				EnumElementCategory.PROVIDER);
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();

		// operation
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation: no need to report further problems, JDT Validation
		// already reports compilation errors
		final IMarker[] providerMarkers = findJaxrsMarkers(provider);
		for (IMarker marker : providerMarkers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(providerMarkers.length, equalTo(0));
	}

	@Test
	public void shouldNotReportProblemOnContainerResponseFilter() throws CoreException, ValidationException {
		// preconditions
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilter");
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilter",
				EnumElementCategory.PROVIDER);
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();

		// operation
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation: no need to report further problems, JDT Validation
		// already reports compilation errors
		final IMarker[] markers = findJaxrsMarkers(provider);
		for (IMarker marker : markers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(0));
	}

	@Test
	public void shouldNotReportProblemOnReaderInterceptor() throws CoreException, ValidationException {
		// preconditions
		metamodelMonitor
				.createElements("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomReaderInterceptor");
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomReaderInterceptor",
				EnumElementCategory.PROVIDER);
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();

		// operation
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation: no need to report further problems, JDT Validation
		// already reports compilation errors
		final IMarker[] markers = findJaxrsMarkers(provider);
		for (IMarker marker : markers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(0));
	}

	@Test
	public void shouldNotReportProblemOnWriterInterceptor() throws CoreException, ValidationException {
		// preconditions
		metamodelMonitor
				.createElements("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomWriterInterceptor");
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomWriterInterceptor",
				EnumElementCategory.PROVIDER);
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();

		// operation
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation: no need to report further problems, JDT Validation
		// already reports compilation errors
		final IMarker[] markers = findJaxrsMarkers(provider);
		for (IMarker marker : markers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(0));
	}

	@Test
	public void shouldReportWarningWhenPreMatchingAnnotationUsedOnWrongKindOfProvider() throws CoreException,
			ValidationException {
		// preconditions
		final IType providerType = replaceFirstOccurrenceOfCode(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomWriterInterceptor", javaProject,
				"@Provider", "@Provider @PreMatching", false);
		metamodelMonitor
				.createElements("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomWriterInterceptor");
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(providerType.getFullyQualifiedName(),
				EnumElementCategory.PROVIDER);
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();

		// operation
		new JaxrsMetamodelValidator().validate(ValidationUtils.toSet(provider.getResource()), project,
				validationHelper, context, validatorManager, reporter);
		// validation: no need to report further problems, JDT Validation
		// already reports compilation errors
		final IMarker[] markers = findJaxrsMarkers(provider);
		for (IMarker marker : markers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(1));
		assertThat(markers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""),
				equalTo(JaxrsPreferences.PROVIDER_INVALID_PRE_MATCHING_ANNOTATION_USAGE));
		assertThat(markers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
	}

	// **************************************************************************************************************************
	// Testing the validation of binding provider with a resource, a resource
	// method or an application using 1 or more name annotations
	// **************************************************************************************************************************

	@Test
	public void shouldNotReportProblemWhenMissingNameBindingAnnotationOnMetaAnnotation() throws CoreException,
			ValidationException {
		// pre-condition: remove @NameBinding annotation on
		// CustomInterceptorBinding meta-annotation -> ie, there is NO binding !
		final IType nameBindingType = replaceFirstOccurrenceOfCode(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding", javaProject,
				"@NameBinding", "", false);
		metamodelMonitor.createElements(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		final IType gameResourceType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		final JaxrsResource gameResource = (JaxrsResource) metamodel.findElement(gameResourceType);
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();

		// operation: validate the *nameBinding* that just changed
		new JaxrsMetamodelValidator().validate(ValidationUtils.toSet(nameBindingType.getResource()), project,
				validationHelper, context, validatorManager, reporter);
		// verification: there's no problem, the Interceptor became a Global
		// Interceptor ;-)
		final IMarker[] markers = nameBindingType.getResource().findMarkers(
				JaxrsMetamodelValidator.JAXRS_PROBLEM_MARKER_ID, true, IResource.DEPTH_INFINITE);
		for (IMarker marker : markers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(0));
		final IMarker[] resourceMarkers = findJaxrsMarkers(gameResource);
		for (IMarker marker : resourceMarkers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(resourceMarkers.length, equalTo(0));
	}

	@Test
	public void shouldNotReportProblemWhenApplicationBoundToProviderWithSingleAnnotation() throws CoreException,
			ValidationException {
		// pre-condition: remove custom Name Binding meta-annotation on
		// CustomInterceptorBinding annotation
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.RestApplication", javaProject,
				"public class", "@CustomInterceptorBinding public class", false);
		metamodelMonitor.createElements(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding",
				EnumElementCategory.PROVIDER);
		final IJaxrsApplication application = (IJaxrsApplication) metamodel.findElement(
				"org.jboss.tools.ws.jaxrs.sample.services.RestApplication", EnumElementCategory.APPLICATION);

		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();

		// operation: validate the *application* that just changed
		new JaxrsMetamodelValidator().validate(ValidationUtils.toSet(application.getResource()), project,
				validationHelper, context, validatorManager, reporter);
		// verification: no error on RestApplication: there's a
		// Filter/Interceptor
		// with such a Name Binding
		final IMarker[] applicationMarkers = findJaxrsMarkers(application);
		for (IMarker marker : applicationMarkers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(applicationMarkers.length, equalTo(0));
		// verification: no error on Provider: there's an Application
		// with such a Name Binding
		final IMarker[] providerMarkers = findJaxrsMarkers(provider);
		for (IMarker marker : providerMarkers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(providerMarkers.length, equalTo(0));
	}

	@Test
	public void shouldNotReportProblemWhenApplicationBoundToProviderWithTwoAnnotations() throws CoreException,
			ValidationException {
		// pre-condition: remove custom Name Binding meta-annotation on
		// CustomInterceptorBinding annotation
		replaceFirstOccurrenceOfCode(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding", javaProject,
				"@CustomInterceptorBinding", "@CustomInterceptorBinding @AnotherCustomInterceptorBinding", false);
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.RestApplication", javaProject,
				"public class", "@CustomInterceptorBinding @AnotherCustomInterceptorBinding public class", false);
		metamodelMonitor.createElements(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.AnotherCustomInterceptorBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding",
				EnumElementCategory.PROVIDER);
		final IJaxrsApplication application = (IJaxrsApplication) metamodel.findElement(
				"org.jboss.tools.ws.jaxrs.sample.services.RestApplication", EnumElementCategory.APPLICATION);

		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();

		// operation: validate the *provider* that just changed
		new JaxrsMetamodelValidator().validate(ValidationUtils.toSet(provider.getResource()), project,
				validationHelper, context, validatorManager, reporter);
		// verification: no error on RestApplication: there's a
		// Filter/Interceptor
		// with such a Name Binding
		final IMarker[] applicationMarkers = findJaxrsMarkers(application);
		for (IMarker marker : applicationMarkers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(applicationMarkers.length, equalTo(0));
		// verification: no error on Provider: there's an Application
		// with such a Name Binding
		final IMarker[] providerMarkers = findJaxrsMarkers(provider);
		for (IMarker marker : providerMarkers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(providerMarkers.length, equalTo(0));
	}

	@Test
	public void shouldReportProblemOnApplicationWhenMissingBindingAnnotationToProvider() throws CoreException,
			ValidationException {
		// pre-condition: remove custom Name Binding meta-annotation on
		// CustomInterceptorBinding annotation
		replaceFirstOccurrenceOfCode(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding", javaProject,
				"@CustomInterceptorBinding", "@CustomInterceptorBinding @AnotherCustomInterceptorBinding", false);
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.RestApplication", javaProject,
				"public class", "@CustomInterceptorBinding @AnotherCustomInterceptorBinding public class", false);
		metamodelMonitor.createElements(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding",
				EnumElementCategory.PROVIDER);
		final IJaxrsApplication application = (IJaxrsApplication) metamodel.findElement(
				"org.jboss.tools.ws.jaxrs.sample.services.RestApplication", EnumElementCategory.APPLICATION);

		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();

		// operation: validate the *provider* that just changed
		new JaxrsMetamodelValidator().validate(ValidationUtils.toSet(provider.getResource()), project,
				validationHelper, context, validatorManager, reporter);
		// verification: no error on RestApplication: there's a
		// Filter/Interceptor
		// with such a Name Binding
		final IMarker[] applicationMarkers = findJaxrsMarkers(application);
		for (IMarker marker : applicationMarkers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(applicationMarkers.length, equalTo(0));
		// verification: no error on Provider: there's an Application
		// with such a Name Binding
		final IMarker[] providerMarkers = findJaxrsMarkers(provider);
		for (IMarker marker : providerMarkers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(providerMarkers.length, equalTo(0));
	}

	@Test
	public void shouldReportProblemOnApplicationWhenNoProviderMatchesSingleBindingAnnotation() throws CoreException,
			ValidationException {
		// pre-condition: add @CustomInterceptorBinding annotation on
		// RestApplication for application-wide binding
		// but remove the @CustomInterceptorBinding annotation on the
		// ResponseFilter.
		final IType applicationType = replaceFirstOccurrenceOfCode(
				"org.jboss.tools.ws.jaxrs.sample.services.RestApplication", javaProject, "public class",
				"@CustomInterceptorBinding public class", false);
		final IType providerType = replaceFirstOccurrenceOfCode(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding", javaProject,
				"@CustomInterceptorBinding", "", false);
		metamodelMonitor.createElements(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.RestApplication",
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding");
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(providerType);
		final JaxrsJavaApplication application = (JaxrsJavaApplication) metamodel.findElement(applicationType);
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();

		// operation: validate the *provider* that just changed
		new JaxrsMetamodelValidator().validate(ValidationUtils.toSet(provider.getResource()), project,
				validationHelper, context, validatorManager, reporter);
		// verification: error on *RestApplication*: no Filter/Interceptor
		// with such a Name Binding
		final IMarker[] applicationMarkers = findJaxrsMarkers(application);
		for (IMarker marker : applicationMarkers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(applicationMarkers.length, equalTo(1));
		assertThat(applicationMarkers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""),
				equalTo(JaxrsPreferences.PROVIDER_MISSING_BINDING));
		assertThat(applicationMarkers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
		final IMarker[] providerMarkers = findJaxrsMarkers(provider);
		for (IMarker marker : providerMarkers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		// no problem on the provider, since it has NO binding annotation.
		assertThat(providerMarkers.length, equalTo(0));
	}

	@Test
	public void shouldReportProblemOnApplicationWhenDeclaringIncompleteSetOfAnnotationsToBindWithProvider()
			throws CoreException, ValidationException {
		// pre-condition: add @CustomInterceptorBinding annotation on
		// RestApplication for application-wide binding
		// and add the @AnotherCustomInterceptorBinding annotation on the
		// ResponseFilter.
		final IType applicationType = replaceFirstOccurrenceOfCode(
				"org.jboss.tools.ws.jaxrs.sample.services.RestApplication", javaProject, "public class",
				"@CustomInterceptorBinding public class", false);
		final IType providerType = replaceFirstOccurrenceOfCode(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding", javaProject,
				"@CustomInterceptorBinding", "@CustomInterceptorBinding @AnotherCustomInterceptorBinding", false);
		metamodelMonitor.createElements(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.RestApplication",
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.AnotherCustomInterceptorBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding");
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(providerType);
		final JaxrsJavaApplication application = (JaxrsJavaApplication) metamodel.findElement(applicationType);
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();

		// operation: validate the *provider* that just changed
		new JaxrsMetamodelValidator().validate(ValidationUtils.toSet(provider.getResource()), project,
				validationHelper, context, validatorManager, reporter);
		// verification: error on *RestApplication*: no Filter/Interceptor
		// with such a Name Bindings
		final IMarker[] applicationMarkers = findJaxrsMarkers(application);
		for (IMarker marker : applicationMarkers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(applicationMarkers.length, equalTo(1));
		assertThat(applicationMarkers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""),
				equalTo(JaxrsPreferences.PROVIDER_MISSING_BINDING));
		assertThat(applicationMarkers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
		final IMarker[] providerMarkers = findJaxrsMarkers(provider);
		for (IMarker marker : providerMarkers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		// also problem on the provider, since it has no binding with the
		// application.
		assertThat(providerMarkers.length, equalTo(1));
		assertThat(providerMarkers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""),
				equalTo(JaxrsPreferences.PROVIDER_UNUSED_BINDING));
		assertThat(providerMarkers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
	}

	@Test
	public void shouldReportProblemOnApplicationWhenNoOtherProviderMatchesSecondBindingAnnotation()
			throws CoreException, ValidationException {
		// pre-condition: add @CustomInterceptorBinding and
		// @AnotherCustomInterceptorBinding annotations on
		// RestApplication for application-wide binding
		final IType applicationType = replaceFirstOccurrenceOfCode(
				"org.jboss.tools.ws.jaxrs.sample.services.RestApplication", javaProject, "public class",
				"@CustomInterceptorBinding @AnotherCustomInterceptorBinding public class", false);
		metamodelMonitor.createElements(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.RestApplication",
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.AnotherCustomInterceptorBinding");
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding",
				EnumElementCategory.PROVIDER);
		final JaxrsJavaApplication application = (JaxrsJavaApplication) metamodel.findElement(applicationType);
		final Annotation anotherCustomInterceptorBindingAnnotation = application
				.getAnnotation("org.jboss.tools.ws.jaxrs.sample.services.interceptors.AnotherCustomInterceptorBinding");
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();

		// operation: validate the *application* that just changed
		new JaxrsMetamodelValidator().validate(ValidationUtils.toSet(application.getResource()), project,
				validationHelper, context, validatorManager, reporter);
		// verification: error on *RestApplication*: no Filter/Interceptor
		// with such a (second) Name Binding
		final IMarker[] applicationMarkers = findJaxrsMarkers(application);
		for (IMarker marker : applicationMarkers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(applicationMarkers.length, equalTo(1));
		assertThat(applicationMarkers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""),
				equalTo(JaxrsPreferences.PROVIDER_MISSING_BINDING));
		assertThat(applicationMarkers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
		assertThat(applicationMarkers[0], matchesLocation(anotherCustomInterceptorBindingAnnotation.getJavaAnnotation()
				.getNameRange()));
		// no problem on the provider, since it is already bound with its single
		// annotation.
		final IMarker[] providerMarkers = findJaxrsMarkers(provider);
		for (IMarker marker : providerMarkers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(providerMarkers.length, equalTo(0));
	}

	@Test
	public void shouldNotReportProblemWhenResourceBoundToProviderWithSingleAnnotation() throws CoreException,
			ValidationException {
		// pre-condition
		metamodelMonitor.createElements(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding",
				EnumElementCategory.PROVIDER);
		final IType gameResourceType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		final JaxrsResource gameResource = (JaxrsResource) metamodel.findElement(gameResourceType);
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();

		// operation: validate the *provider* that just changed
		new JaxrsMetamodelValidator().validate(ValidationUtils.toSet(provider.getResource()), project,
				validationHelper, context, validatorManager, reporter);
		// verification
		final IMarker[] providerMarkers = findJaxrsMarkers(provider);
		for (IMarker marker : providerMarkers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(providerMarkers.length, equalTo(0));
		final IMarker[] resourceMarkers = findJaxrsMarkers(gameResource);
		for (IMarker marker : resourceMarkers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(resourceMarkers.length, equalTo(0));
	}

	@Test
	public void shouldNotReportProblemWhenResourceBoundToProviderWithTwoAnnotations() throws CoreException,
			ValidationException {
		// pre-condition
		replaceFirstOccurrenceOfCode(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding", javaProject,
				"@CustomInterceptorBinding", "@CustomInterceptorBinding @AnotherCustomInterceptorBinding", false);
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.GameResource", javaProject,
				"@CustomInterceptorBinding", "@CustomInterceptorBinding @AnotherCustomInterceptorBinding", false);
		metamodelMonitor.createElements(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.AnotherCustomInterceptorBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding",
				EnumElementCategory.PROVIDER);
		final IType gameResourceType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		final JaxrsResource gameResource = (JaxrsResource) metamodel.findElement(gameResourceType);
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();

		// operation: validate the *provider* that just changed
		new JaxrsMetamodelValidator().validate(ValidationUtils.toSet(provider.getResource()), project,
				validationHelper, context, validatorManager, reporter);
		// verification
		final IMarker[] providerMarkers = findJaxrsMarkers(provider);
		for (IMarker marker : providerMarkers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(providerMarkers.length, equalTo(0));
		final IMarker[] resourceMarkers = findJaxrsMarkers(gameResource);
		for (IMarker marker : resourceMarkers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(resourceMarkers.length, equalTo(0));
	}

	@Test
	public void shouldReportProblemOnResourceWhenNoProviderMatchesSingleBindingAnnotation() throws CoreException,
			ValidationException {
		// pre-condition
		final IType providerType = replaceFirstOccurrenceOfCode(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding", javaProject,
				"@CustomInterceptorBinding", "", false);
		metamodelMonitor.createElements(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(providerType);
		final IType gameResourceType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		final JaxrsResource gameResource = (JaxrsResource) metamodel.findElement(gameResourceType);
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();

		// operation: validate the *provider* that just changed
		new JaxrsMetamodelValidator().validate(ValidationUtils.toSet(provider.getResource()), project,
				validationHelper, context, validatorManager, reporter);
		// verification: error on *Game Resource*: no Filter/Interceptor
		// with such a Name Binding
		final IMarker[] markers = findJaxrsMarkers(gameResource);
		for (IMarker marker : markers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(1));
		assertThat(markers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""),
				equalTo(JaxrsPreferences.PROVIDER_MISSING_BINDING));
		assertThat(markers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
	}

	@Test
	public void shouldReportProblemOnResourceWhenNoProviderMatchesSecondBindingAnnotation() throws CoreException,
			ValidationException {
		// pre-condition
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.GameResource", javaProject,
				"@CustomInterceptorBinding", "@CustomInterceptorBinding @AnotherCustomInterceptorBinding", false);
		metamodelMonitor.createElements(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.AnotherCustomInterceptorBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding",
				EnumElementCategory.PROVIDER);
		final IType gameResourceType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		final JaxrsResource gameResource = (JaxrsResource) metamodel.findElement(gameResourceType);
		final Annotation anotherCustomInterceptorBindingAnnotation = gameResource
				.getAnnotation("org.jboss.tools.ws.jaxrs.sample.services.interceptors.AnotherCustomInterceptorBinding");

		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();

		// operation: validate the *gameResource* that just changed
		new JaxrsMetamodelValidator().validate(ValidationUtils.toSet(gameResource.getResource()), project,
				validationHelper, context, validatorManager, reporter);
		// verification: error on *Game Resource*: no Filter/Interceptor
		// with such a Name Binding
		final IMarker[] resourceMarkers = findJaxrsMarkers(gameResource);
		for (IMarker marker : resourceMarkers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(resourceMarkers.length, equalTo(1));
		assertThat(resourceMarkers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""),
				equalTo(JaxrsPreferences.PROVIDER_MISSING_BINDING));
		assertThat(resourceMarkers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
		assertThat(resourceMarkers[0], matchesLocation(anotherCustomInterceptorBindingAnnotation.getJavaAnnotation()
				.getNameRange()));
		// existing provider is bound, so no problem reported
		final IMarker[] providerMarkers = findJaxrsMarkers(provider);
		for (IMarker marker : providerMarkers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(providerMarkers.length, equalTo(0));
	}

	@Test
	public void shouldReportProblemOnResourceWhenDeclaringIncompleteSetOfAnnotationsToBindWithProvider()
			throws CoreException, ValidationException {
		// pre-condition: add a second binding annotation on the provider only
		// (missing on 'GameResource')
		replaceFirstOccurrenceOfCode(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding", javaProject,
				"@CustomInterceptorBinding", "@CustomInterceptorBinding @AnotherCustomInterceptorBinding", false);
		metamodelMonitor.createElements(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.AnotherCustomInterceptorBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding",
				EnumElementCategory.PROVIDER);
		final IType gameResourceType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		final JaxrsResource gameResource = (JaxrsResource) metamodel.findElement(gameResourceType);
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();

		// operation: validate the *provider*
		new JaxrsMetamodelValidator().validate(ValidationUtils.toSet(provider.getResource()), project,
				validationHelper, context, validatorManager, reporter);
		// verification: error on *Game Resource*: no Filter/Interceptor
		// with such a Name Binding
		final IMarker[] resourceMarkers = findJaxrsMarkers(gameResource);
		for (IMarker marker : resourceMarkers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(resourceMarkers.length, equalTo(1));
		assertThat(resourceMarkers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""),
				equalTo(JaxrsPreferences.PROVIDER_MISSING_BINDING));
		assertThat(resourceMarkers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
		// existing provider is not bound, so a problem is reported, too
		final IMarker[] providerMarkers = findJaxrsMarkers(provider);
		for (IMarker marker : providerMarkers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(providerMarkers.length, equalTo(1));
		assertThat(resourceMarkers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""),
				equalTo(JaxrsPreferences.PROVIDER_MISSING_BINDING));
		assertThat(resourceMarkers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
	}

	@Test
	public void shouldNotReportProblemWhenResourceMethodBoundToProviderWithSingleAnnotation() throws CoreException,
			ValidationException {
		// pre-condition
		metamodelMonitor.createElements(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding",
				EnumElementCategory.PROVIDER);
		final IType customerResourceType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResource customerResource = (JaxrsResource) metamodel.findElement(customerResourceType);
		final IMethod getCustomerMethod = metamodelMonitor.resolveMethod(customerResourceType, "getCustomer");
		final JaxrsResourceMethod customerResourceMethod = customerResource.getMethods().get(
				getCustomerMethod.getHandleIdentifier());
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();

		// operation: validate the *provider*
		new JaxrsMetamodelValidator().validate(ValidationUtils.toSet(provider.getResource()), project,
				validationHelper, context, validatorManager, reporter);
		// verification
		final IMarker[] providerMarkers = findJaxrsMarkers(provider);
		for (IMarker marker : providerMarkers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(providerMarkers.length, equalTo(0));
		final IMarker[] resourceMarkers = findJaxrsMarkers(customerResourceMethod);
		for (IMarker marker : resourceMarkers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(resourceMarkers.length, equalTo(0));
	}

	@Test
	public void shouldNotReportProblemWhenResourceMethodBoundToProviderWithTwoAnnotations() throws CoreException,
			ValidationException {
		// pre-condition
		replaceFirstOccurrenceOfCode(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding", javaProject,
				"@CustomInterceptorBinding", "@CustomInterceptorBinding @AnotherCustomInterceptorBinding", false);
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				"@CustomInterceptorBinding", "@CustomInterceptorBinding @AnotherCustomInterceptorBinding", false);
		metamodelMonitor.createElements(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.AnotherCustomInterceptorBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding",
				EnumElementCategory.PROVIDER);
		final IType customerResourceType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResource customerResource = (JaxrsResource) metamodel.findElement(customerResourceType);
		final IMethod getCustomerMethod = metamodelMonitor.resolveMethod(customerResourceType, "getCustomer");
		final JaxrsResourceMethod customerResourceMethod = customerResource.getMethods().get(
				getCustomerMethod.getHandleIdentifier());
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();

		// operation: validate the *customerResource* that just changed
		new JaxrsMetamodelValidator().validate(ValidationUtils.toSet(customerResource.getResource()), project,
				validationHelper, context, validatorManager, reporter);
		// verification
		final IMarker[] providerMarkers = findJaxrsMarkers(provider);
		for (IMarker marker : providerMarkers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(providerMarkers.length, equalTo(0));
		final IMarker[] resourceMarkers = findJaxrsMarkers(customerResourceMethod);
		for (IMarker marker : resourceMarkers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(resourceMarkers.length, equalTo(0));
	}

	@Test
	public void shouldReportProblemOnResourceMethodWhenNoProviderMatchesSingleBindingAnnotation() throws CoreException,
			ValidationException {
		// pre-condition: remove @CustomInterceptorBinding on
		// CustomResponseFilterWithBinding
		replaceFirstOccurrenceOfCode(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding", javaProject,
				"@CustomInterceptorBinding", "", false);
		metamodelMonitor.createElements(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding",
				EnumElementCategory.PROVIDER);
		final JaxrsResource customerResource = (JaxrsResource) metamodel.findElement(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", EnumElementCategory.RESOURCE);
		final IMethod getCustomerMethod = metamodelMonitor.resolveMethod(customerResource.getJavaElement(),
				"getCustomer");
		final JaxrsResourceMethod customerResourceMethod = customerResource.getMethods().get(
				getCustomerMethod.getHandleIdentifier());
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();

		// operation: validate the *provider* that just changed
		new JaxrsMetamodelValidator().validate(ValidationUtils.toSet(provider.getResource()), project,
				validationHelper, context, validatorManager, reporter);
		// verification: error on *Customer Resource Method*: no
		// Filter/Interceptor
		// with such a Name Binding
		final IMarker[] markers = findJaxrsMarkers(customerResourceMethod);
		for (IMarker marker : markers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(1));
		assertThat(markers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""),
				equalTo(JaxrsPreferences.PROVIDER_MISSING_BINDING));
		assertThat(markers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
	}

	@Test
	public void shouldReportProblemOnResourceMethodWhenNoProviderMatchesSecondBindingAnnotation() throws CoreException,
			ValidationException {
		// pre-condition: add @AnotherCustomInterceptorBinding on resource
		// method but not on any provider
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				"@CustomInterceptorBinding", "@CustomInterceptorBinding @AnotherCustomInterceptorBinding", false);
		metamodelMonitor.createElements(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.AnotherCustomInterceptorBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding",
				EnumElementCategory.PROVIDER);
		final JaxrsResource customerResource = (JaxrsResource) metamodel.findElement(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", EnumElementCategory.RESOURCE);
		final IMethod getCustomerMethod = metamodelMonitor.resolveMethod(customerResource.getJavaElement(),
				"getCustomer");
		final JaxrsResourceMethod customerResourceMethod = customerResource.getMethods().get(
				getCustomerMethod.getHandleIdentifier());
		final Annotation anotherCustomInterceptorBindingAnnotation = customerResourceMethod
				.getAnnotation("org.jboss.tools.ws.jaxrs.sample.services.interceptors.AnotherCustomInterceptorBinding");

		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();

		// operation: validate the *customerResource* that just changed
		new JaxrsMetamodelValidator().validate(ValidationUtils.toSet(customerResource.getResource()), project,
				validationHelper, context, validatorManager, reporter);
		// verification: error on *Customer Resource Method*: no
		// Filter/Interceptor
		// with such a Name Binding
		final IMarker[] resourceMarkers = findJaxrsMarkers(customerResourceMethod);
		for (IMarker marker : resourceMarkers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(resourceMarkers.length, equalTo(1));
		assertThat(resourceMarkers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""),
				equalTo(JaxrsPreferences.PROVIDER_MISSING_BINDING));
		assertThat(resourceMarkers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
		assertThat(resourceMarkers[0], matchesLocation(anotherCustomInterceptorBindingAnnotation.getJavaAnnotation()
				.getNameRange()));
		// no problem reported on the provider, since it is already bound
		final IMarker[] providerMarkers = findJaxrsMarkers(provider);
		for (IMarker marker : providerMarkers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(providerMarkers.length, equalTo(0));
	}

	@Test
	public void shouldReportProblemOnResourceMethodWhenDeclaringIncompleteSetOfAnnotationsToBindWithProvider()
			throws CoreException, ValidationException {
		// pre-condition: add @AnotherCustomInterceptorBinding on provider but
		// not on resource method
		replaceFirstOccurrenceOfCode(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding", javaProject,
				"@CustomInterceptorBinding", "@CustomInterceptorBinding @AnotherCustomInterceptorBinding", false);
		metamodelMonitor.createElements(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.AnotherCustomInterceptorBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding",
				EnumElementCategory.PROVIDER);
		final JaxrsResource customerResource = (JaxrsResource) metamodel.findElement(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", EnumElementCategory.RESOURCE);
		final IMethod getCustomerMethod = metamodelMonitor.resolveMethod(customerResource.getJavaElement(),
				"getCustomer");
		final JaxrsResourceMethod customerResourceMethod = customerResource.getMethods().get(
				getCustomerMethod.getHandleIdentifier());
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();

		// operation: validate the *customerResource* that just changed
		new JaxrsMetamodelValidator().validate(ValidationUtils.toSet(customerResource.getResource()), project,
				validationHelper, context, validatorManager, reporter);
		// verification: error on *Customer Resource Method*: no
		// Filter/Interceptor
		// with such a Name Binding
		final IMarker[] resourceMarkers = findJaxrsMarkers(customerResourceMethod);
		for (IMarker marker : resourceMarkers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(resourceMarkers.length, equalTo(1));
		assertThat(resourceMarkers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""),
				equalTo(JaxrsPreferences.PROVIDER_MISSING_BINDING));
		assertThat(resourceMarkers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
		// problem also reported on the provider, since it is not bound
		final IMarker[] providerMarkers = findJaxrsMarkers(provider);
		for (IMarker marker : providerMarkers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(providerMarkers.length, equalTo(1));
		assertThat(resourceMarkers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""),
				equalTo(JaxrsPreferences.PROVIDER_MISSING_BINDING));
		assertThat(resourceMarkers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
	}

	@Test
	public void shouldReportProblemOnProviderWhenNoElementMatchesSingleBindingAnnotation() throws CoreException,
			ValidationException {
		// pre-condition: remove binding annotation on the resource method
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				"@CustomInterceptorBinding", "", false);
		metamodelMonitor.createElements(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding",
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding",
				EnumElementCategory.PROVIDER);
		final JaxrsResource customerResource = (JaxrsResource) metamodel.findElement(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", EnumElementCategory.RESOURCE);
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();

		// operation: validate the *customer resource* that just changed
		new JaxrsMetamodelValidator().validate(ValidationUtils.toSet(customerResource.getResource()), project,
				validationHelper, context, validatorManager, reporter);
		// verification: error on *Provider*: no other element
		// with such a Name Binding
		final IMarker[] markers = findJaxrsMarkers(provider);
		for (IMarker marker : markers) {
			TestLogger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(1));
		assertThat(markers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""),
				equalTo(JaxrsPreferences.PROVIDER_UNUSED_BINDING));
		assertThat(markers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("{")));
	}

}
