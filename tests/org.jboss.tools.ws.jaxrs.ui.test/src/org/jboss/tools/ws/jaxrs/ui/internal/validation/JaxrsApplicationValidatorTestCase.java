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
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils.replaceAllOccurrencesOfCode;
import static org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils.replaceContent;
import static org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils.replaceFirstOccurrenceOfCode;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.deleteJaxrsMarkers;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.findJaxrsMarkers;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.hasPreferenceKey;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.toSet;
import static org.jboss.tools.ws.jaxrs.ui.preferences.JaxrsPreferences.APPLICATION_NO_OCCURRENCE_FOUND;
import static org.jboss.tools.ws.jaxrs.ui.preferences.JaxrsPreferences.APPLICATION_TOO_MANY_OCCURRENCES;
import static org.jboss.tools.ws.jaxrs.ui.preferences.JaxrsPreferences.JAVA_APPLICATION_INVALID_TYPE_HIERARCHY;
import static org.jboss.tools.ws.jaxrs.ui.preferences.JaxrsPreferences.JAVA_APPLICATION_MISSING_APPLICATION_PATH_ANNOTATION;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
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
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsWebxmlApplication;
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsApplication;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResource;
import org.jboss.tools.ws.jaxrs.core.utils.Annotation;
import org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames;
import org.jboss.tools.ws.jaxrs.ui.JBossJaxrsUIPlugin;
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
public class JaxrsApplicationValidatorTestCase {

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
	public void setup() throws CoreException, ValidationException {
		metamodel = metamodelMonitor.getMetamodel();
		project = metamodel.getProject();
		// remove all but CustomerResource elements to avoid false-positive on
		// problem level assertions
		final IType customerJavaType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final List<IJaxrsResource> allResources = metamodel.findAllResources();
		for (IJaxrsResource resource : allResources) {
			if (resource.getJavaElement().equals(customerJavaType)) {
				continue;
			}
			((JaxrsResource) resource).remove();
		}
		// a first full validation is required (as it happens when the project
		// is validated for the first time)
		//new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
	}

	@After
	public void resetProblemLevelPreferences() {
		final IEclipsePreferences defaultPreferences = ((IScopeContext) DefaultScope.INSTANCE)
				.getNode(JBossJaxrsUIPlugin.PLUGIN_ID);
		defaultPreferences.put(JaxrsPreferences.APPLICATION_NO_OCCURRENCE_FOUND, JaxrsPreferences.WARNING);
	}

	@Test
	public void shouldNotReportProblemIfOneJavaApplicationExists() throws CoreException, ValidationException {
		// preconditions
		final List<IJaxrsApplication> applications = metamodel.findAllApplications();
		for (IJaxrsApplication application : applications) {
			if (application.isWebXmlApplication()) {
				((JaxrsWebxmlApplication) application).remove();
			}
		}
		deleteJaxrsMarkers(metamodel);
		metamodelMonitor.resetElementChangesNotifications();
		assertThat(metamodel.findAllApplications().size(), equalTo(1));
		assertThat(metamodel.findApplication().isJavaApplication(), equalTo(true));
		// operation
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(project);
		assertThat(markers.length, equalTo(0));
	}

	@Test
	public void shouldNotReportProblemIfOneWebxmlApplicationExists() throws CoreException, ValidationException {
		// preconditions
		final List<IJaxrsApplication> applications = metamodel.findAllApplications();
		for (IJaxrsApplication application : applications) {
			if (application.isJavaApplication()) {
				((JaxrsJavaApplication) application).remove();
			}
		}
		assertThat(metamodel.findAllApplications().size(), equalTo(1));
		assertThat(metamodel.findApplication().isWebXmlApplication(), equalTo(true));
		deleteJaxrsMarkers(metamodel);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation
		final IMarker[] merkers = findJaxrsMarkers(project);
		assertThat(merkers.length, equalTo(0));
	}

	@Test
	public void shouldReportProblemOnProjectIfNoApplicationExists() throws CoreException, ValidationException {
		// preconditions: only keep CustomerResource
		final IType customerJavaType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final List<IJaxrsElement> elements = metamodel.findAllElements();
		for (IJaxrsElement element : elements) {
			if (element.getElementKind() == EnumElementKind.ROOT_RESOURCE
					&& ((JaxrsResource) element).getJavaElement().equals(customerJavaType)) {
				continue;
			}
			((AbstractJaxrsBaseElement) element).remove();
		}

		deleteJaxrsMarkers(metamodel);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(project);
		assertThat(markers.length, equalTo(1));
		assertThat(markers, hasPreferenceKey(APPLICATION_NO_OCCURRENCE_FOUND));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(true));
		assertThat(metamodel.getProblemLevel(), equalTo(IMarker.SEVERITY_WARNING));
	}

	@Test
	public void shouldNotReportProblemOnProjectIfNoElementExists() throws CoreException, ValidationException {
		// preconditions
		final List<IJaxrsElement> elements = metamodel.findAllElements();
		for (IJaxrsElement element : elements) {
			((AbstractJaxrsBaseElement) element).remove();
		}
		deleteJaxrsMarkers(metamodel);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(project);
		assertThat(markers.length, equalTo(0));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(false));
		assertThat(metamodel.getProblemLevel(), equalTo(IMarker.SEVERITY_INFO));
	}

	@Test
	public void shouldReportProblemOnApplicationsIfMultipleOnesExist() throws CoreException, ValidationException {
		// preconditions
		final List<IJaxrsApplication> applications = metamodel.findAllApplications();
		assertThat(applications, hasSize(2));
		deleteJaxrsMarkers(metamodel);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(project);
		assertThat(markers.length, equalTo(0));
		for (IJaxrsApplication application : metamodel.findAllApplications()) {
			final IMarker[] appMarkers = findJaxrsMarkers(application);
			assertThat(appMarkers.length, equalTo(1));
			assertThat(appMarkers, hasPreferenceKey(APPLICATION_TOO_MANY_OCCURRENCES));
		}
		// associated endpoints don't have the problem, though
		for (IJaxrsEndpoint endpoint : metamodel.findEndpoints(metamodel.findApplication())) {
			assertThat(endpoint.getProblemLevel(), equalTo(0));
		}
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(true));
	}

	@Test
	public void shouldNotDuplicateProblemsOnApplicationsIfMultipleOnesExistWhenValidatingOtherFiles()
			throws CoreException, ValidationException {
		// preconditions
		final List<IJaxrsApplication> applications = metamodel.findAllApplications();
		assertThat(applications, hasSize(2));
		deleteJaxrsMarkers(metamodel);
		metamodelMonitor.resetElementChangesNotifications();
		// operation: validate project
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// operation: validate 2 unrelated files
		final IFile file1 = (IFile) project.findMember(".project");
		final IFile file2 = (IFile) project.findMember(".classpath");
		new JaxrsMetamodelValidator().validate(toSet(file1), project, validationHelper, context, validatorManager,
				reporter);
		new JaxrsMetamodelValidator().validate(toSet(file2), project, validationHelper, context, validatorManager,
				reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(project);
		assertThat(markers.length, equalTo(0));
		for (IJaxrsApplication application : metamodel.findAllApplications()) {
			final IMarker[] appMarkers = findJaxrsMarkers(application);
			assertThat(appMarkers.length, equalTo(1));
			assertThat(appMarkers, hasPreferenceKey(APPLICATION_TOO_MANY_OCCURRENCES));
		}
		// associated endpoints don't have the problem, though
		for (IJaxrsEndpoint endpoint : metamodel.findEndpoints(metamodel.findApplication())) {
			assertThat(endpoint.getProblemLevel(), equalTo(0));
		}
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(true));
	}

	@Test
	public void shouldReportProblemOnJavaApplicationIfMissingApplicationPathAnnotationWithoutOverride()
			throws CoreException, ValidationException, OperationCanceledException, InterruptedException {
		// preconditions
		final List<IJaxrsApplication> applications = metamodel.findAllApplications();
		assertThat(applications, hasSize(2));
		deleteJaxrsMarkers(metamodel);
		metamodelMonitor.resetElementChangesNotifications();
		JaxrsJavaApplication javaApplication = null;
		// operations:
		// remove web.xml-based application and remove @ApplicationPath
		// annotation on java-based application
		for (IJaxrsApplication application : applications) {
			if (application.isWebXmlApplication()) {
				((AbstractJaxrsBaseElement) application).remove();
				new JaxrsMetamodelValidator().validate(toSet(application.getResource()), project, validationHelper, context, validatorManager, reporter);
			} else {
				javaApplication = (JaxrsJavaApplication) application;
				final Annotation appPathAnnotation = javaApplication.getAnnotation(JaxrsClassnames.APPLICATION_PATH);
				javaApplication.removeAnnotation(appPathAnnotation.getJavaAnnotation());
				new JaxrsMetamodelValidator().validate(toSet(application.getResource()), project, validationHelper, context, validatorManager, reporter);
			}
		}
		// validation
		final IMarker[] markers = findJaxrsMarkers(javaApplication);
		assertThat(markers.length, equalTo(1));
		assertThat(markers, hasPreferenceKey(JAVA_APPLICATION_MISSING_APPLICATION_PATH_ANNOTATION));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(true));
	}

	@Test
	public void shouldReportProblemOnJavaApplicationIfInvalidTypeHierarchy() throws CoreException, ValidationException {
		// preconditions
		final IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		replaceAllOccurrencesOfCode(type.getCompilationUnit(), "extends Application", "", false);
		final JaxrsJavaApplication javaApplication = metamodel.findJavaApplications().get(0);
		deleteJaxrsMarkers(metamodel);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(javaApplication.getResource()), project, validationHelper, context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(javaApplication);
		assertThat(markers, hasPreferenceKey(JAVA_APPLICATION_INVALID_TYPE_HIERARCHY));
		// associated endpoints don't have the problem, though
		for (IJaxrsEndpoint endpoint : metamodel.findEndpoints(metamodel.findApplication())) {
			assertThat(endpoint.getProblemLevel(), equalTo(0));
		}
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(true));
	}

	@Test
	public void shouldNotReportProblemOnApplicationIfMissingApplicationPathAnnotationWithOverride() throws Exception {
		// preconditions
		final JaxrsJavaApplication javaApplication = metamodel.findJavaApplications().get(0);
		final Annotation appPathAnnotation = javaApplication.getAnnotation(JaxrsClassnames.APPLICATION_PATH);
		javaApplication.removeAnnotation(appPathAnnotation.getJavaAnnotation());
		metamodel.findWebxmlApplication().remove();
		final JaxrsWebxmlApplication webxmlApplication = metamodelMonitor.createWebxmlApplication(javaApplication.getJavaClassName(), "/foo");
		deleteJaxrsMarkers(metamodel);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(webxmlApplication.getResource()), project, validationHelper, context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(javaApplication);
		assertThat(markers.length, equalTo(0));
	}

	@Test
	public void shouldNotReportProblemOnApplicationIfAnnotationExistsAndHierarchyValid() throws CoreException,
			ValidationException {
		// preconditions
		final JaxrsJavaApplication javaApplication = metamodel.findJavaApplications().get(0);
		final JaxrsWebxmlApplication webxmlApplication = metamodel.findWebxmlApplication();
		webxmlApplication.remove();
		deleteJaxrsMarkers(metamodel);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(webxmlApplication.getResource()), project, validationHelper, context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(javaApplication);
		assertThat(markers.length, equalTo(0));
	}

	@Test
	public void shouldNotReportProblemOnApplicationWhenAnnotationRemovedAndSuperclassExtensionRemoved()
			throws CoreException, ValidationException {
		// preconditions operation #1
		final JaxrsJavaApplication javaApplication = metamodel.findJavaApplications().get(0);
		metamodel.findWebxmlApplication().remove();
		deleteJaxrsMarkers(metamodel);
		metamodelMonitor.resetElementChangesNotifications();
		// operation #1: remove annotation and validate
		replaceAllOccurrencesOfCode(javaApplication.getJavaElement().getCompilationUnit(),
				"@ApplicationPath(\"/app\")", "", false);
		new JaxrsMetamodelValidator().validate(toSet(javaApplication.getResource()), project, validationHelper, context, validatorManager, reporter);
		// validation after operation #1
		IMarker[] markers = findJaxrsMarkers(javaApplication);
		assertThat(markers, hasPreferenceKey(JAVA_APPLICATION_MISSING_APPLICATION_PATH_ANNOTATION));
		assertThat(markers.length, equalTo(1));
		// preconditions operation #2
		deleteJaxrsMarkers(metamodel);
		// operation #2: remove 'extends Application'
		replaceAllOccurrencesOfCode(javaApplication.getJavaElement().getCompilationUnit(), "extends Application", "",
				false);
		new JaxrsMetamodelValidator().validate(toSet(javaApplication.getResource()), project, validationHelper, context, validatorManager, reporter);
		// validation after operation #2
		markers = findJaxrsMarkers(javaApplication);
		assertThat(markers.length, equalTo(0));
	}

	@Test
	public void shouldReportProblemAfterBuildWhenMetamodelHasMultipleJavaApplicationsAndNoWebxml()
			throws CoreException, ValidationException, OperationCanceledException, InterruptedException {
		// preconditions
		final JaxrsWebxmlApplication webxmlApplication = metamodel.findWebxmlApplication();
		webxmlApplication.remove();
		final JaxrsJavaApplication javaApplication = metamodel.findJavaApplications().get(0);
		final IType restApplication2Type = metamodelMonitor.createCompilationUnit("RestApplication2.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "RestApplication2.java").findPrimaryType();
		final JaxrsJavaApplication javaApplication2 = JaxrsJavaApplication.from(restApplication2Type)
				.withMetamodel(metamodel).build();
		deleteJaxrsMarkers(metamodel);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(
				toSet(webxmlApplication.getResource(), javaApplication.getResource(), javaApplication2.getResource()),
				project, validationHelper, context, validatorManager, reporter);
		// validation
		for (JaxrsJavaApplication app : new JaxrsJavaApplication[] { javaApplication, javaApplication2 }) {
			final IMarker[] markers = findJaxrsMarkers(app);
			assertThat(markers, hasPreferenceKey(APPLICATION_TOO_MANY_OCCURRENCES));
			assertThat(markers.length, equalTo(1));
			// associated endpoints don't have the problem, though
			for (IJaxrsEndpoint endpoint : metamodel.findEndpoints(metamodel.findApplication())) {
				assertThat(endpoint.getProblemLevel(), equalTo(0));
			}
		}
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(true));
	}

	@Test
	public void shouldReportAndRemoveProblemAfterAppRemovalWhenMetamodelHasMultipleJavaApplicationsAndNoWebxml()
			throws CoreException, ValidationException, OperationCanceledException, InterruptedException {
		// preconditions
		final JaxrsWebxmlApplication webxmlApplication = metamodel.findWebxmlApplication();
		webxmlApplication.remove();
		final JaxrsJavaApplication javaApplication = metamodel.findJavaApplications().get(0);
		final IType restApplication2Type = metamodelMonitor.createCompilationUnit("RestApplication2.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "RestApplication2.java").findPrimaryType();
		final JaxrsJavaApplication javaApplication2 = JaxrsJavaApplication.from(restApplication2Type)
				.withMetamodel(metamodel).build();
		deleteJaxrsMarkers(metamodel);
		metamodelMonitor.resetElementChangesNotifications();
		// operation 1: validate when there are 2 applications, expecting
		// markers
		new JaxrsMetamodelValidator().validate(
				toSet(webxmlApplication.getResource(), javaApplication.getResource(), javaApplication2.getResource()),
				project, validationHelper, context, validatorManager, reporter);
		// validation
		for (JaxrsJavaApplication app : new JaxrsJavaApplication[] { javaApplication, javaApplication2 }) {
			final IMarker[] markers = findJaxrsMarkers(app);
			assertThat(markers.length, equalTo(1));
			assertThat(markers, hasPreferenceKey(APPLICATION_TOO_MANY_OCCURRENCES));
			// associated endpoints don't have the problem, though
			for (IJaxrsEndpoint endpoint : metamodel.findEndpoints(metamodel.findApplication())) {
				assertThat(endpoint.getProblemLevel(), equalTo(0));
			}
		}
		// operation 2: remove second JAX-RS application
		javaApplication2.remove();
		final IFile app2Resource = (IFile) restApplication2Type.getResource();
		// app2Resource.delete(true, new NullProgressMonitor());
		// then validate again, only the *changed files* (and without reset).
		// Expect marker on first application to be removed
		new JaxrsMetamodelValidator().validate(toSet(app2Resource), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		assertThat(findJaxrsMarkers(javaApplication).length, equalTo(0));
		assertThat(findJaxrsMarkers(javaApplication2).length, equalTo(0));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(true));
	}

	@Test
	public void shouldReportAndRemoveProblemAfterUnderlyingResourceRemovalWhenMetamodelHasMultipleJavaApplicationsAndNoWebxml()
			throws CoreException, ValidationException, OperationCanceledException, InterruptedException {
		// preconditions
		final JaxrsWebxmlApplication webxmlApplication = metamodel.findWebxmlApplication();
		webxmlApplication.remove();
		final JaxrsJavaApplication javaApplication = metamodel.findJavaApplications().get(0);
		final IType restApplication2Type = metamodelMonitor.createCompilationUnit("RestApplication2.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "RestApplication2.java").findPrimaryType();
		final JaxrsJavaApplication javaApplication2 = JaxrsJavaApplication.from(restApplication2Type)
				.withMetamodel(metamodel).build();
		deleteJaxrsMarkers(metamodel);
		metamodelMonitor.resetElementChangesNotifications();
		// operation 1: validate when there are 2 applications, expecting
		// markers
		new JaxrsMetamodelValidator().validate(
				toSet(webxmlApplication.getResource(), javaApplication.getResource(), javaApplication2.getResource()),
				project, validationHelper, context, validatorManager, reporter);
		// validation
		for (JaxrsJavaApplication app : new JaxrsJavaApplication[] { javaApplication, javaApplication2 }) {
			final IMarker[] markers = findJaxrsMarkers(app);
			assertThat(markers.length, equalTo(1));
			assertThat(markers, hasPreferenceKey(APPLICATION_TOO_MANY_OCCURRENCES));
			// associated endpoints don't have the problem, though
			for (IJaxrsEndpoint endpoint : metamodel.findEndpoints(metamodel.findApplication())) {
				assertThat(endpoint.getProblemLevel(), equalTo(0));
			}
		}
		// operation 2: remove second JAX-RS application
		javaApplication2.remove();
		final IFile app2Resource = (IFile) restApplication2Type.getResource();
		app2Resource.delete(true, new NullProgressMonitor());
		// then validate again, only the *changed files* (and without reset).
		// Expect marker on first application to be removed
		new JaxrsMetamodelValidator().validate(toSet(app2Resource), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		assertThat(findJaxrsMarkers(javaApplication).length, equalTo(0));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(true));
	}

	@Test
	public void shouldReportProblemAfterBuildWhenMetamodelHasMultipleJavaApplicationsAndWebxml() throws CoreException,
			ValidationException, OperationCanceledException, InterruptedException {
		// preconditions
		final JaxrsWebxmlApplication webxmlApplication = metamodel.findWebxmlApplication();
		final JaxrsJavaApplication javaApplication = metamodel.findJavaApplications().get(0);
		final IType restApplication2Type = metamodelMonitor.createCompilationUnit("RestApplication2.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "RestApplication2.java").findPrimaryType();
		final JaxrsJavaApplication javaApplication2 = JaxrsJavaApplication.from(restApplication2Type)
				.withMetamodel(metamodel).build();
		deleteJaxrsMarkers(metamodel);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(
				toSet(webxmlApplication.getResource(), javaApplication.getResource(), javaApplication2.getResource()),
				project, validationHelper, context, validatorManager, reporter);
		// validation
		for (IJaxrsApplication app : new IJaxrsApplication[] { webxmlApplication, javaApplication, javaApplication2 }) {
			final IMarker[] markers = findJaxrsMarkers(app);
			assertThat(markers, hasPreferenceKey(APPLICATION_TOO_MANY_OCCURRENCES));
			assertThat(markers.length, equalTo(1));
			// associated endpoints don't have the problem, though
			for (IJaxrsEndpoint endpoint : metamodel.findEndpoints(metamodel.findApplication())) {
				assertThat(endpoint.getProblemLevel(), equalTo(0));
			}
		}
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(true));
	}

	@Test
	public void shouldReportAndRemoveProblemAfterUnderlyingJavaApplicationResourceRemovalWhenMetamodelHasOneJavaApplicationAndOneWebxml()
			throws CoreException, ValidationException, OperationCanceledException, InterruptedException {
		// preconditions
		final JaxrsWebxmlApplication webxmlApplication = metamodel.findWebxmlApplication();
		final JaxrsJavaApplication javaApplication = metamodel.findJavaApplications().get(0);
		deleteJaxrsMarkers(metamodel);
		metamodelMonitor.resetElementChangesNotifications();
		// operation 1: validate when there are 2 applications, expecting
		// markers
		new JaxrsMetamodelValidator().validate(
				toSet(webxmlApplication.getResource(), javaApplication.getResource()),
				project, validationHelper, context, validatorManager, reporter);
		// validation
		for (IJaxrsApplication app : new IJaxrsApplication[] { javaApplication, webxmlApplication }) {
			final IMarker[] markers = findJaxrsMarkers(app);
			assertThat(markers.length, equalTo(1));
			assertThat(markers, hasPreferenceKey(APPLICATION_TOO_MANY_OCCURRENCES));
			// associated endpoints don't have the problem, though
			for (IJaxrsEndpoint endpoint : metamodel.findEndpoints(metamodel.findApplication())) {
				assertThat(endpoint.getProblemLevel(), equalTo(0));
			}
		}
		// operation 2: remove JAX-RS application
		javaApplication.remove();
		final IFile appResource = (IFile) javaApplication.getResource();
		appResource.delete(true, new NullProgressMonitor());
		// then validate again, only the *changed files* (and without reset).
		// Expect marker on we.xml application definition to be removed
		new JaxrsMetamodelValidator().validate(toSet(appResource), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		assertThat(findJaxrsMarkers(webxmlApplication).length, equalTo(0));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(true));
	}

	@Test
	public void shouldReportAndRemoveProblemAfterUnderlyingWebxmlApplicationResourceRemovalWhenMetamodelHasOneJavaApplicationAndOneWebxml()
			throws CoreException, ValidationException, OperationCanceledException, InterruptedException {
		// preconditions
		final JaxrsWebxmlApplication webxmlApplication = metamodel.findWebxmlApplication();
		final JaxrsJavaApplication javaApplication = metamodel.findJavaApplications().get(0);
		deleteJaxrsMarkers(metamodel);
		metamodelMonitor.resetElementChangesNotifications();
		// operation 1: validate when there are 2 applications, expecting
		// markers
		new JaxrsMetamodelValidator().validate(
				toSet(webxmlApplication.getResource(), javaApplication.getResource()),
				project, validationHelper, context, validatorManager, reporter);
		// validation
		for (IJaxrsApplication app : new IJaxrsApplication[] { javaApplication, webxmlApplication }) {
			final IMarker[] markers = findJaxrsMarkers(app);
			assertThat(markers.length, equalTo(1));
			assertThat(markers, hasPreferenceKey(APPLICATION_TOO_MANY_OCCURRENCES));
			// associated endpoints don't have the problem, though
			for (IJaxrsEndpoint endpoint : metamodel.findEndpoints(metamodel.findApplication())) {
				assertThat(endpoint.getProblemLevel(), equalTo(0));
			}
		}
		// operation 2: remove web.xml application definition
		webxmlApplication.remove();
		final IFile webxmlResource = (IFile) webxmlApplication.getResource();
		webxmlResource.delete(true, new NullProgressMonitor());
		// then validate again, only the *changed files* (and without reset).
		// Expect marker on first application to be removed
		new JaxrsMetamodelValidator().validate(toSet(webxmlResource), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		assertThat(findJaxrsMarkers(javaApplication).length, equalTo(0));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(true));
	}

	@Test
	public void shouldReportAndRemoveProblemAfterWebxmlApplicationCommentedWhenMetamodelHasOneJavaApplicationAndOneWebxml()
			throws Exception {
		// preconditions
		final JaxrsWebxmlApplication webxmlApplication = metamodel.findWebxmlApplication();
		final JaxrsJavaApplication javaApplication = metamodel.findJavaApplications().get(0);
		deleteJaxrsMarkers(metamodel);
		metamodelMonitor.resetElementChangesNotifications();
		// operation 1: validate when there are 2 applications, expecting
		// markers
		new JaxrsMetamodelValidator().validate(toSet(webxmlApplication.getResource()), project, validationHelper, context, validatorManager, reporter);
		new JaxrsMetamodelValidator().validate(toSet(javaApplication.getResource()), project, validationHelper, context, validatorManager, reporter);
		// validation
		for (IJaxrsApplication app : new IJaxrsApplication[] { javaApplication, webxmlApplication }) {
			final IMarker[] markers = findJaxrsMarkers(app);
			assertThat(markers.length, equalTo(1));
			assertThat(markers, hasPreferenceKey(APPLICATION_TOO_MANY_OCCURRENCES));
			// associated endpoints don't have the problem, though
			for (IJaxrsEndpoint endpoint : metamodel.findEndpoints(metamodel.findApplication())) {
				assertThat(endpoint.getProblemLevel(), equalTo(0));
			}
		}
		// operation 2: remove web.xml application definition
		webxmlApplication.remove();
		final IFile webxmlResource = metamodelMonitor.replaceDeploymentDescriptorWith(
				"web-3_0-without-servlet-mapping.xml");
		// then validate again, only the *changed files* (and without reset).
		// Expect marker on first application to be removed
		new JaxrsMetamodelValidator().validate(toSet(webxmlResource), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		assertThat(findJaxrsMarkers(javaApplication).length, equalTo(0));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(true));
	}

	@Test
	public void shouldStillReportProblemAfterUnderlyingWebxmlApplicationResourceRemovalWhenMetamodelHasTwoJavaApplicationsAndWebxmlOverride()
			throws CoreException, ValidationException, OperationCanceledException, InterruptedException, IOException {
		// preconditions
		final JaxrsWebxmlApplication webxmlApplication = metamodel.findWebxmlApplication();
		replaceContent(webxmlApplication.getResource(), "javax.ws.rs.core.Application",
				"org.jboss.tools.ws.jaxrs.sample.services.RestApplication2");
		final JaxrsJavaApplication javaApplication = metamodel.findJavaApplications().get(0);
		final IType restApplication2Type = metamodelMonitor.createCompilationUnit("RestApplication2.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "RestApplication2.java").findPrimaryType();
		replaceFirstOccurrenceOfCode(restApplication2Type, "@ApplicationPath(\"/app2\")", "", false);
		final JaxrsJavaApplication javaApplication2 = JaxrsJavaApplication.from(restApplication2Type)
				.withMetamodel(metamodel).build();
		deleteJaxrsMarkers(metamodel);
		metamodelMonitor.resetElementChangesNotifications();
		// operation 1: validate when there are 2 applications, expecting
		// markers
		new JaxrsMetamodelValidator().validate(
				toSet(webxmlApplication.getResource(), javaApplication.getResource(), javaApplication2.getResource()),
				project, validationHelper, context, validatorManager, reporter);
		// validation: markers are on first javaapp and one web.xml only
		for (IJaxrsApplication app : new IJaxrsApplication[] { javaApplication, javaApplication2, webxmlApplication }) {
			final IMarker[] markers = findJaxrsMarkers(app);
			assertThat(markers.length, equalTo(1));
			assertThat(markers, hasPreferenceKey(APPLICATION_TOO_MANY_OCCURRENCES));
			// associated endpoints don't have the problem, though
			for (IJaxrsEndpoint endpoint : metamodel.findEndpoints(metamodel.findApplication())) {
				assertThat(endpoint.getProblemLevel(), equalTo(0));
			}
		}
		// operation 2: remove Web.xml override
		webxmlApplication.remove();
		final IFile webxmlResource = (IFile) webxmlApplication.getResource();
		webxmlResource.delete(true, new NullProgressMonitor());
		// then validate again, only the *changed files* (and without reset).
		// Expect marker on both applications to be still there
		new JaxrsMetamodelValidator().validate(toSet(webxmlResource), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		assertThat(findJaxrsMarkers(javaApplication).length, equalTo(1));
		// restApplication2 has 2 markers: missing annotation and duplicate
		// application
		assertThat(findJaxrsMarkers(javaApplication2).length, equalTo(2));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(true));
	}

	@Test
	public void shouldStillReportProblemAfterUnderlyingWebxmlApplicationResourceEditWhenMetamodelHasTwoJavaApplicationsAndWebxmlOverride()
			throws Exception {
		// preconditions
		final JaxrsWebxmlApplication webxmlApplication = metamodel.findWebxmlApplication();
		replaceContent(webxmlApplication.getResource(), "javax.ws.rs.core.Application",
				"org.jboss.tools.ws.jaxrs.sample.services.RestApplication2");
		final JaxrsJavaApplication javaApplication = metamodel.findJavaApplications().get(0);
		final IType restApplication2Type = metamodelMonitor.createCompilationUnit("RestApplication2.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "RestApplication2.java").findPrimaryType();
		replaceFirstOccurrenceOfCode(restApplication2Type, "@ApplicationPath(\"/app2\")", "", false);
		final JaxrsJavaApplication javaApplication2 = JaxrsJavaApplication.from(restApplication2Type)
				.withMetamodel(metamodel).build();
		deleteJaxrsMarkers(metamodel);
		// operation 1: validate when there are 2 applications, expecting
		// markers
		new JaxrsMetamodelValidator().validate(
				toSet(webxmlApplication.getResource(), javaApplication.getResource(), javaApplication2.getResource()),
				project, validationHelper, context, validatorManager, reporter);
		// validation: markers are on first javaapp and one web.xml only
		for (IJaxrsApplication app : new IJaxrsApplication[] { javaApplication, webxmlApplication }) {
			final IMarker[] markers = findJaxrsMarkers(app);
			assertThat(markers.length, equalTo(1));
			assertThat(markers, hasPreferenceKey(APPLICATION_TOO_MANY_OCCURRENCES));
			// associated endpoints don't have the problem, though
			for (IJaxrsEndpoint endpoint : metamodel.findEndpoints(metamodel.findApplication())) {
				assertThat(endpoint.getProblemLevel(), equalTo(0));
			}
		}
		metamodelMonitor.resetElementChangesNotifications();
		// operation 2: remove Web.xml override
		webxmlApplication.remove();
		IFile webxmlResource = (IFile) webxmlApplication.getResource();
		// remove content, no remove resource
		webxmlResource = (IFile) metamodelMonitor
				.replaceDeploymentDescriptorWith("web-3_0-without-servlet-mapping.xml");
		// then validate again, only the *changed files* (and without reset).
		// Expect marker on both applications to be still there
		new JaxrsMetamodelValidator().validate(toSet(webxmlResource), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		assertThat(findJaxrsMarkers(javaApplication).length, equalTo(1));
		// restApplication2 has 2 markers: missing annotation and duplicate
		// application
		assertThat(findJaxrsMarkers(javaApplication2).length, equalTo(2));
		// no change in problem level, so don't expect anything here since last
		// full validation
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(false));
	}

	@Test
	public void shouldNotReportProblemOnApplicationsIfMultipleOnesExistWhenValidatingUnrelatedFile()
			throws CoreException, ValidationException {
		// preconditions
		final List<IJaxrsApplication> applications = metamodel.findAllApplications();
		assertThat(applications, hasSize(2));
		deleteJaxrsMarkers(metamodel);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(project.findMember(".project")), project, validationHelper,
				context, validatorManager, reporter);
		// validation: validation did not occur on JAX-RS applications.
		final IMarker[] markers = findJaxrsMarkers(project);
		assertThat(markers.length, equalTo(0));
		for (IJaxrsApplication application : metamodel.findAllApplications()) {
			final IMarker[] appMarkers = findJaxrsMarkers(application);
			assertThat(appMarkers.length, equalTo(0));
		}
		for (IJaxrsEndpoint endpoint : metamodel.findEndpoints(metamodel.findApplication())) {
			assertThat(endpoint.getProblemLevel(), equalTo(0));
		}
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().contains(metamodel), is(false));
	}

	@Test
	public void shouldNotFailOnProblemIfSeverityLevelIsIgnore() throws CoreException, ValidationException {
		// preconditions
		final List<IJaxrsApplication> applications = metamodel.findAllApplications();
		for (IJaxrsApplication application : applications) {
			((AbstractJaxrsBaseElement) application).remove();
		}
		deleteJaxrsMarkers(metamodel);
		metamodelMonitor.resetElementChangesNotifications();
		final IEclipsePreferences defaultPreferences = ((IScopeContext) DefaultScope.INSTANCE)
				.getNode(JBossJaxrsUIPlugin.PLUGIN_ID);
		defaultPreferences.put(JaxrsPreferences.APPLICATION_NO_OCCURRENCE_FOUND, JaxrsPreferences.IGNORE);
		// operation
		for(IJaxrsApplication application : applications) {
			new JaxrsMetamodelValidator().validate(
					toSet(application.getResource()),
					project, validationHelper, context, validatorManager, reporter);
		}
		// validation
		final IMarker[] markers = findJaxrsMarkers(project);
		assertThat(markers.length, equalTo(0));
	}

	@Test
	public void shouldIncreaseAndResetProblemLevelOnApplication() throws CoreException, ValidationException {
		// preconditions
		final JaxrsWebxmlApplication webxmlApplication = metamodel.findWebxmlApplication();
		webxmlApplication.remove();
		final IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		replaceAllOccurrencesOfCode(type.getCompilationUnit(), "extends Application", "extends Object", false);
		final JaxrsJavaApplication javaApplication = metamodel.findJavaApplications().get(0);
		deleteJaxrsMarkers(metamodel);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(
				toSet(webxmlApplication.getResource(), javaApplication.getResource()),
				project, validationHelper, context, validatorManager, reporter);
		// verification: problem level is set to '2'
		assertThat(javaApplication.getProblemLevel(), equalTo(2));
		// now, fix the problem
		replaceAllOccurrencesOfCode(type.getCompilationUnit(), "extends Object", "extends Application", false);
		// revalidate
		new JaxrsMetamodelValidator().validate(toSet(javaApplication.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		// verification: problem level is set to '0'
		assertThat(javaApplication.getProblemLevel(), equalTo(0));
	}

	// @see https://issues.jboss.org/browse/JBIDE-17276
	@Test
	public void shouldNotReportProblemOnEndpointWhenErrorIsOnApplication() throws CoreException, ValidationException {
		// pre-condition: remove web.xml application
		final JaxrsWebxmlApplication webxmlApplication = metamodel.findWebxmlApplication();
		webxmlApplication.remove();
		// remove the @ApplicationPath annotation on the JAX-RS Application
		final IType applicationType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		replaceAllOccurrencesOfCode(applicationType.getCompilationUnit(), "@ApplicationPath(\"/app\")", "", false);
		deleteJaxrsMarkers(metamodel);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(
				toSet(webxmlApplication.getResource(), applicationType.getResource()),
				project, validationHelper, context, validatorManager, reporter);
		// verification: problem level is set to '2' on the application but not
		// on endpoints
		final JaxrsJavaApplication javaApplication = metamodel.findJavaApplications().get(0);
		assertThat(javaApplication.getProblemLevel(), equalTo(2));
		for (IJaxrsEndpoint endpoint : metamodel.getAllEndpoints()) {
			assertThat(endpoint.getProblemLevel(), not(equalTo(2)));
		}

	}

}
