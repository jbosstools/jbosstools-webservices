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
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.validation.MarkerUtils.deleteJaxrsMarkers;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.validation.MarkerUtils.findJaxrsMarkers;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.validation.MarkerUtils.hasPreferenceKey;
import static org.jboss.tools.ws.jaxrs.core.preferences.JaxrsPreferences.APPLICATION_NO_OCCURRENCE_FOUND;
import static org.jboss.tools.ws.jaxrs.core.preferences.JaxrsPreferences.APPLICATION_TOO_MANY_OCCURRENCES;
import static org.jboss.tools.ws.jaxrs.core.preferences.JaxrsPreferences.JAVA_APPLICATION_INVALID_TYPE_HIERARCHY;
import static org.jboss.tools.ws.jaxrs.core.preferences.JaxrsPreferences.JAVA_APPLICATION_MISSING_APPLICATION_PATH_ANNOTATION;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
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
import org.jboss.tools.ws.jaxrs.core.JBossJaxrsCorePlugin;
import org.jboss.tools.ws.jaxrs.core.WorkbenchUtils;
import org.jboss.tools.ws.jaxrs.core.builder.AbstractMetamodelBuilderTestCase;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsBaseElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsJavaApplication;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsWebxmlApplication;
import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionUtils;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsApplication;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.preferences.JaxrsPreferences;
import org.junit.After;
import org.junit.Test;

/**
 * @author Xi
 * 
 */
@SuppressWarnings("restriction")
public class JaxrsApplicationValidatorTestCase extends AbstractMetamodelBuilderTestCase {

	private final IReporter reporter = new ReporterHelper(new NullProgressMonitor());
	private final ContextValidationHelper validationHelper = new ContextValidationHelper();
	private final IProjectValidationContext context = new ProjectValidationContext();
	private final ValidatorManager validatorManager = new ValidatorManager();

	@After
	public void resetProblemLevelPreferences() {
		final IEclipsePreferences defaultPreferences = ((IScopeContext)DefaultScope.INSTANCE).getNode(JBossJaxrsCorePlugin.PLUGIN_ID);
		defaultPreferences.put(JaxrsPreferences.APPLICATION_NO_OCCURRENCE_FOUND, JaxrsPreferences.ERROR);
	}

	@Test
	public void shouldNotReportProblemIfOneJavaApplicationExists() throws CoreException, ValidationException {
		// preconditions
		final List<IJaxrsApplication> applications = metamodel.getAllApplications();
		for (IJaxrsApplication application : applications) {
			if (application.isWebXmlApplication()) {
				((JaxrsWebxmlApplication)application).remove();
			}
		}
		deleteJaxrsMarkers(project);
		resetElementChangesNotifications();
		assertThat(metamodel.getAllApplications().size(), equalTo(1));
		assertThat(metamodel.getApplication().isJavaApplication(), equalTo(true));
		// operation
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(project);
		assertThat(markers.length, equalTo(0));
	}
	
	@Test
	public void shouldNotReportProblemIfOneWebxmlApplicationExists() throws CoreException, ValidationException {
		// preconditions
		final List<IJaxrsApplication> applications = metamodel.getAllApplications();
		for (IJaxrsApplication application : applications) {
			if (application.isJavaApplication()) {
				((JaxrsJavaApplication) application).remove();
			}
		}
		assertThat(metamodel.getAllApplications().size(), equalTo(1));
		assertThat(metamodel.getApplication().isWebXmlApplication(), equalTo(true));
		deleteJaxrsMarkers(project);
		resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation
		final IMarker[] merkers = findJaxrsMarkers(project);
		assertThat(merkers.length, equalTo(0));
	}

	@Test
	public void shouldReportProblemOnProjectIfNoApplicationExists() throws CoreException, ValidationException {
		// preconditions
		final List<IJaxrsApplication> applications = metamodel.getAllApplications();
		for (IJaxrsApplication application : applications) {
			((JaxrsBaseElement) application).remove();
		}
		deleteJaxrsMarkers(project);
		resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(project);
		assertThat(markers.length, equalTo(1));
		assertThat(markers, hasPreferenceKey(APPLICATION_NO_OCCURRENCE_FOUND));
		assertThat(metamodelProblemLevelChanges.contains(metamodel), is(true));
	}

	@Test
	public void shouldReportProblemOnApplicationsIfMultipleOnesExist() throws CoreException, ValidationException {
		// preconditions
		final List<IJaxrsApplication> applications = metamodel.getAllApplications();
		assertThat(applications, hasSize(2));
		deleteJaxrsMarkers(project);
		resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(project);
		assertThat(markers.length, equalTo(0));
		for (IJaxrsApplication application : metamodel.getAllApplications()) {
			final IMarker[] appMarkers = findJaxrsMarkers(application);
			assertThat(appMarkers.length, equalTo(1));
			assertThat(appMarkers, hasPreferenceKey(APPLICATION_TOO_MANY_OCCURRENCES));
		}
		final List<JaxrsEndpoint> affectedEndpoints = metamodel.findEndpoints(metamodel.getApplication());
		for(IJaxrsEndpoint endpoint : affectedEndpoints) {
			assertThat(endpoint.getProblemLevel(), not(equalTo(0)));
		}
		assertThat(metamodelProblemLevelChanges.contains(metamodel), is(true));
	}

	@Test
	public void shouldNotDuplicateProblemsOnApplicationsIfMultipleOnesExistWhenValidatingOtherFiles() throws CoreException, ValidationException {
		// preconditions
		final List<IJaxrsApplication> applications = metamodel.getAllApplications();
		assertThat(applications, hasSize(2));
		deleteJaxrsMarkers(project);
		resetElementChangesNotifications();
		// operation: validate one app twice
		final IFile file1 = (IFile)project.findMember(".project");
		final IFile file2 = (IFile)project.findMember(".classpath");
		new JaxrsMetamodelValidator().validate(CollectionUtils.toSet(file1), project, validationHelper, context, validatorManager, reporter);
		new JaxrsMetamodelValidator().validate(CollectionUtils.toSet(file2), project, validationHelper, context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(project);
		assertThat(markers.length, equalTo(0));
		for (IJaxrsApplication application : metamodel.getAllApplications()) {
			final IMarker[] appMarkers = findJaxrsMarkers(application);
			assertThat(appMarkers.length, equalTo(1));
			assertThat(appMarkers, hasPreferenceKey(APPLICATION_TOO_MANY_OCCURRENCES));
		}
		final List<JaxrsEndpoint> affectedEndpoints = metamodel.findEndpoints(metamodel.getApplication());
		for(IJaxrsEndpoint endpoint : affectedEndpoints) {
			assertThat(endpoint.getProblemLevel(), not(equalTo(0)));
		}
		assertThat(metamodelProblemLevelChanges.contains(metamodel), is(true));
	}
	
	@Test
	public void shouldReportProblemOnJavaApplicationIfMissingApplicationPathAnnotationWithoutOverride()
			throws CoreException, ValidationException, OperationCanceledException, InterruptedException {
		// preconditions
		final List<IJaxrsApplication> applications = metamodel.getAllApplications();
		assertThat(applications, hasSize(2));
		deleteJaxrsMarkers(project);
		resetElementChangesNotifications();
		JaxrsJavaApplication javaApplication = null;
		// remove web.xml-based application and remove @ApplicationPath annotation on java-based application
		for (IJaxrsApplication application : applications) {
			if (application.isWebXmlApplication()) {
				((JaxrsBaseElement) application).remove();
			} else {
				javaApplication = (JaxrsJavaApplication) application;
				final Annotation appPathAnnotation = javaApplication
						.getAnnotation(EnumJaxrsClassname.APPLICATION_PATH.qualifiedName);
				javaApplication.removeAnnotation(appPathAnnotation.getJavaAnnotation());
			}
		}
		// operation
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(javaApplication);
		assertThat(markers.length, equalTo(1));
		assertThat(markers, hasPreferenceKey(JAVA_APPLICATION_MISSING_APPLICATION_PATH_ANNOTATION));
		assertThat(metamodelProblemLevelChanges.contains(metamodel), is(true));
	}

	@Test
	public void shouldReportProblemOnJavaApplicationIfInvalidTypeHierarchy() throws CoreException, ValidationException {
		// preconditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		WorkbenchUtils.replaceAllOccurrencesOfCode(type.getCompilationUnit(), "extends Application", "", false);
		final JaxrsJavaApplication javaApplication = metamodel.getJavaApplications().get(0);
		deleteJaxrsMarkers(project);
		resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(javaApplication);
		assertThat(markers, hasPreferenceKey(JAVA_APPLICATION_INVALID_TYPE_HIERARCHY));
		for(IJaxrsEndpoint endpoint : metamodel.findEndpoints(metamodel.getApplication())) {
			assertThat(endpoint.getProblemLevel(), not(equalTo(0)));
		}
		assertThat(metamodelProblemLevelChanges.contains(metamodel), is(true));
	}

	@Test
	public void shouldNotReportProblemOnApplicationIfMissingApplicationPathAnnotationWithOverride() throws Exception {
		// preconditions
		final JaxrsJavaApplication javaApplication = metamodel.getJavaApplications().get(0);
		final Annotation appPathAnnotation = javaApplication
				.getAnnotation(EnumJaxrsClassname.APPLICATION_PATH.qualifiedName);
		javaApplication.removeAnnotation(appPathAnnotation.getJavaAnnotation());
		metamodel.findWebxmlApplication().remove();
		createWebxmlApplication(javaApplication.getJavaClassName(), "/foo");
		deleteJaxrsMarkers(project);
		resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(javaApplication);
		assertThat(markers.length, equalTo(0));
	}

	@Test
	public void shouldNotReportProblemOnApplicationIfAnnotationExistsAndHierarchyValid() throws CoreException,
			ValidationException {
		// preconditions
		final JaxrsJavaApplication javaApplication = metamodel.getJavaApplications().get(0);
		metamodel.findWebxmlApplication().remove();
		deleteJaxrsMarkers(project);
		resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(javaApplication);
		assertThat(markers.length, equalTo(0));
	}

	@Test
	public void shouldNotReportProblemOnApplicationWhenAnnotationRemovedAndSuperclassExtensionRemoved()
			throws CoreException, ValidationException {
		// preconditions operation #1
		final JaxrsJavaApplication javaApplication = metamodel.getJavaApplications().get(0);
		metamodel.findWebxmlApplication().remove();
		deleteJaxrsMarkers(project);
		resetElementChangesNotifications();
		// operation #1: remove annotation and validate
		LOGGER.warn("*** Operation #1 ***");
		WorkbenchUtils.replaceAllOccurrencesOfCode(javaApplication.getJavaElement().getCompilationUnit(),
				"@ApplicationPath(\"/app\")", "", false);
		LOGGER.warn("*** Validating after Operation #1 ***");
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation after operation #1
		IMarker[] markers = findJaxrsMarkers(javaApplication);
		assertThat(markers, hasPreferenceKey(JAVA_APPLICATION_MISSING_APPLICATION_PATH_ANNOTATION));
		assertThat(markers.length, equalTo(1));
		// preconditions operation #2
		deleteJaxrsMarkers(project);
		// operation #2: remove 'extends Application'
		LOGGER.warn("*** Operation #2 ***");
		WorkbenchUtils.replaceAllOccurrencesOfCode(javaApplication.getJavaElement().getCompilationUnit(),
				"extends Application", "", false);
		LOGGER.warn("*** Validating after Operation #2 ***");
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation after operation #2
		markers = findJaxrsMarkers(javaApplication);
		assertThat(markers.length, equalTo(0));
	}
	
	@Test
	public void shouldReportProblemAfterBuildWhenMetamodelHasMultipleJavaApplicationsAndNoWebxml() throws CoreException, ValidationException, OperationCanceledException, InterruptedException {
		// preconditions
		metamodel.findWebxmlApplication().remove();
		final JaxrsJavaApplication javaApplication = metamodel.getJavaApplications().get(0);
		final IType restApplication2Type = WorkbenchUtils.createCompilationUnit(javaProject,
				"RestApplication2.txt", "org.jboss.tools.ws.jaxrs.sample.services", "RestApplication2.java").findPrimaryType();
		final JaxrsJavaApplication javaApplication2 = JaxrsJavaApplication.from(restApplication2Type).withMetamodel(metamodel).build();
		deleteJaxrsMarkers(project);
		resetElementChangesNotifications();
		// operation
		buildMetamodel();
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation
		for(JaxrsJavaApplication app : new JaxrsJavaApplication[]{javaApplication, javaApplication2}) {
			final IMarker[] markers = findJaxrsMarkers(app);
			assertThat(markers, hasPreferenceKey(APPLICATION_TOO_MANY_OCCURRENCES));
			assertThat(markers.length, equalTo(1));
			for(IJaxrsEndpoint endpoint : metamodel.findEndpoints(metamodel.getApplication())) {
				assertThat(endpoint.getProblemLevel(), not(equalTo(0)));
			}
		}
		assertThat(metamodelProblemLevelChanges.contains(metamodel), is(true));
	}

	@Test
	public void shouldReportAndRemoveProblemAfterAppRemovalWhenMetamodelHasMultipleJavaApplicationsAndNoWebxml() throws CoreException, ValidationException, OperationCanceledException, InterruptedException {
		// preconditions
		metamodel.findWebxmlApplication().remove();
		final JaxrsJavaApplication javaApplication = metamodel.getJavaApplications().get(0);
		final IType restApplication2Type = WorkbenchUtils.createCompilationUnit(javaProject,
				"RestApplication2.txt", "org.jboss.tools.ws.jaxrs.sample.services", "RestApplication2.java").findPrimaryType();
		final JaxrsJavaApplication javaApplication2 = JaxrsJavaApplication.from(restApplication2Type).withMetamodel(metamodel).build();
		deleteJaxrsMarkers(project);
		resetElementChangesNotifications();
		// operation 1: validate when there are 2 applications, expecting markers
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation
		for(JaxrsJavaApplication app : new JaxrsJavaApplication[]{javaApplication, javaApplication2}) {
			final IMarker[] markers = findJaxrsMarkers(app);
			assertThat(markers.length, equalTo(1));
			assertThat(markers, hasPreferenceKey(APPLICATION_TOO_MANY_OCCURRENCES));
			for(IJaxrsEndpoint endpoint : metamodel.findEndpoints(metamodel.getApplication())) {
				assertThat(endpoint.getProblemLevel(), not(equalTo(0)));
			}
		}
		// operation 2: remove second JAX-RS application
		javaApplication2.remove();
		final IFile app2Resource = (IFile) restApplication2Type.getResource();
		//app2Resource.delete(true, new NullProgressMonitor());
		// then validate again, only the *changed files* (and without reset). Expect marker on first application to be removed
		new JaxrsMetamodelValidator().validate(CollectionUtils.toSet(app2Resource), project, validationHelper, context, validatorManager, reporter);
		// validation
		assertThat(findJaxrsMarkers(javaApplication).length, equalTo(0));
		assertThat(findJaxrsMarkers(javaApplication2).length, equalTo(0));
		assertThat(metamodelProblemLevelChanges.contains(metamodel), is(true));
	}

	@Test
	public void shouldReportAndRemoveProblemAfterUnderlyingResourceRemovalWhenMetamodelHasMultipleJavaApplicationsAndNoWebxml() throws CoreException, ValidationException, OperationCanceledException, InterruptedException {
		// preconditions
		metamodel.findWebxmlApplication().remove();
		final JaxrsJavaApplication javaApplication = metamodel.getJavaApplications().get(0);
		final IType restApplication2Type = WorkbenchUtils.createCompilationUnit(javaProject,
				"RestApplication2.txt", "org.jboss.tools.ws.jaxrs.sample.services", "RestApplication2.java").findPrimaryType();
		final JaxrsJavaApplication javaApplication2 = JaxrsJavaApplication.from(restApplication2Type).withMetamodel(metamodel).build();
		deleteJaxrsMarkers(project);
		resetElementChangesNotifications();
		// operation 1: validate when there are 2 applications, expecting markers
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation
		for(JaxrsJavaApplication app : new JaxrsJavaApplication[]{javaApplication, javaApplication2}) {
			final IMarker[] markers = findJaxrsMarkers(app);
			assertThat(markers.length, equalTo(1));
			assertThat(markers, hasPreferenceKey(APPLICATION_TOO_MANY_OCCURRENCES));
			for(IJaxrsEndpoint endpoint : metamodel.findEndpoints(metamodel.getApplication())) {
				assertThat(endpoint.getProblemLevel(), not(equalTo(0)));
			}
		}
		// operation 2: remove second JAX-RS application
		javaApplication2.remove();
		final IFile app2Resource = (IFile) restApplication2Type.getResource();
		app2Resource.delete(true, new NullProgressMonitor());
		// then validate again, only the *changed files* (and without reset). Expect marker on first application to be removed
		new JaxrsMetamodelValidator().validate(CollectionUtils.toSet(app2Resource), project, validationHelper, context, validatorManager, reporter);
		// validation
		assertThat(findJaxrsMarkers(javaApplication).length, equalTo(0));
		assertThat(metamodelProblemLevelChanges.contains(metamodel), is(true));
	}

	@Test
	public void shouldReportProblemAfterBuildWhenMetamodelHasMultipleJavaApplicationsAndWebxml() throws CoreException, ValidationException, OperationCanceledException, InterruptedException {
		// preconditions
		final JaxrsWebxmlApplication webxmlApplication = metamodel.findWebxmlApplication();
		final JaxrsJavaApplication javaApplication = metamodel.getJavaApplications().get(0);
		final IType restApplication2Type = WorkbenchUtils.createCompilationUnit(javaProject,
				"RestApplication2.txt", "org.jboss.tools.ws.jaxrs.sample.services", "RestApplication2.java").findPrimaryType();
		final JaxrsJavaApplication javaApplication2 = JaxrsJavaApplication.from(restApplication2Type).withMetamodel(metamodel).build();
		deleteJaxrsMarkers(project);
		resetElementChangesNotifications();
		// operation
		buildMetamodel();
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation
		for(IJaxrsApplication app : new IJaxrsApplication[]{webxmlApplication, javaApplication, javaApplication2}) {
			final IMarker[] markers = findJaxrsMarkers(app);
			assertThat(markers, hasPreferenceKey(APPLICATION_TOO_MANY_OCCURRENCES));
			assertThat(markers.length, equalTo(1));
			for(IJaxrsEndpoint endpoint : metamodel.findEndpoints(metamodel.getApplication())) {
				assertThat(endpoint.getProblemLevel(), not(equalTo(0)));
			}
		}
		assertThat(metamodelProblemLevelChanges.contains(metamodel), is(true));
	}
	
	@Test
	public void shouldReportAndRemoveProblemAfterUnderlyingJavaApplicationResourceRemovalWhenMetamodelHasOneJavaApplicationAndOneWebxml() throws CoreException, ValidationException, OperationCanceledException, InterruptedException {
		// preconditions
		final JaxrsWebxmlApplication webxmlApplication = metamodel.findWebxmlApplication();
		final JaxrsJavaApplication javaApplication = metamodel.getJavaApplications().get(0);
		deleteJaxrsMarkers(project);
		resetElementChangesNotifications();
		// operation 1: validate when there are 2 applications, expecting markers
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation
		for(IJaxrsApplication app : new IJaxrsApplication[]{javaApplication, webxmlApplication}) {
			final IMarker[] markers = findJaxrsMarkers(app);
			assertThat(markers.length, equalTo(1));
			assertThat(markers, hasPreferenceKey(APPLICATION_TOO_MANY_OCCURRENCES));
			for(IJaxrsEndpoint endpoint : metamodel.findEndpoints(metamodel.getApplication())) {
				assertThat(endpoint.getProblemLevel(), not(equalTo(0)));
			}
		}
		// operation 2: remove JAX-RS application
		javaApplication.remove();
		final IFile appResource = (IFile) javaApplication.getResource();
		appResource.delete(true, new NullProgressMonitor());
		// then validate again, only the *changed files* (and without reset). Expect marker on we.xml application definition to be removed
		new JaxrsMetamodelValidator().validate(CollectionUtils.toSet(appResource), project, validationHelper, context, validatorManager, reporter);
		// validation
		assertThat(findJaxrsMarkers(webxmlApplication).length, equalTo(0));
		assertThat(metamodelProblemLevelChanges.contains(metamodel), is(true));
	}

	@Test
	public void shouldReportAndRemoveProblemAfterUnderlyingWebxmlApplicationResourceRemovalWhenMetamodelHasOneJavaApplicationAndOneWebxml() throws CoreException, ValidationException, OperationCanceledException, InterruptedException {
		// preconditions
		final JaxrsWebxmlApplication webxmlApplication = metamodel.findWebxmlApplication();
		final JaxrsJavaApplication javaApplication = metamodel.getJavaApplications().get(0);
		deleteJaxrsMarkers(project);
		resetElementChangesNotifications();
		// operation 1: validate when there are 2 applications, expecting markers
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation
		for(IJaxrsApplication app : new IJaxrsApplication[]{javaApplication, webxmlApplication}) {
			final IMarker[] markers = findJaxrsMarkers(app);
			assertThat(markers.length, equalTo(1));
			assertThat(markers, hasPreferenceKey(APPLICATION_TOO_MANY_OCCURRENCES));
			for(IJaxrsEndpoint endpoint : metamodel.findEndpoints(metamodel.getApplication())) {
				assertThat(endpoint.getProblemLevel(), not(equalTo(0)));
			}
		}
		// operation 2: remove web.xml application definition
		webxmlApplication.remove();
		final IFile webxmlResource = (IFile) webxmlApplication.getResource();
		webxmlResource.delete(true, new NullProgressMonitor());
		// then validate again, only the *changed files* (and without reset). Expect marker on first application to be removed
		new JaxrsMetamodelValidator().validate(CollectionUtils.toSet(webxmlResource), project, validationHelper, context, validatorManager, reporter);
		// validation
		assertThat(findJaxrsMarkers(javaApplication).length, equalTo(0));
		assertThat(metamodelProblemLevelChanges.contains(metamodel), is(true));
	}

	@Test
	public void shouldStillReportProblemAfterUnderlyingWebxmlApplicationResourceRemovalWhenMetamodelHasTwoJavaApplicationsAndWebxmlOverride() throws CoreException, ValidationException, OperationCanceledException, InterruptedException, IOException {
		// preconditions
		final JaxrsWebxmlApplication webxmlApplication = metamodel.findWebxmlApplication();
		WorkbenchUtils.replaceContent(webxmlApplication.getResource(), "javax.ws.rs.core.Application", "org.jboss.tools.ws.jaxrs.sample.services.RestApplication2");
		final JaxrsJavaApplication javaApplication = metamodel.getJavaApplications().get(0);
		final IType restApplication2Type = WorkbenchUtils.createCompilationUnit(javaProject,
				"RestApplication2.txt", "org.jboss.tools.ws.jaxrs.sample.services", "RestApplication2.java").findPrimaryType();
		WorkbenchUtils.replaceFirstOccurrenceOfCode(restApplication2Type, "@ApplicationPath(\"/app2\")", "", false);
		final JaxrsJavaApplication javaApplication2 = JaxrsJavaApplication.from(restApplication2Type).withMetamodel(metamodel).build();
		deleteJaxrsMarkers(project);
		resetElementChangesNotifications();
		// operation 1: validate when there are 2 applications, expecting markers
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation: markers are on first javaapp and one web.xml only
		for(IJaxrsApplication app : new IJaxrsApplication[]{javaApplication, javaApplication2, webxmlApplication}) {
			final IMarker[] markers = findJaxrsMarkers(app);
			assertThat(markers.length, equalTo(1));
			assertThat(markers, hasPreferenceKey(APPLICATION_TOO_MANY_OCCURRENCES));
			for(IJaxrsEndpoint endpoint : metamodel.findEndpoints(metamodel.getApplication())) {
				assertThat(endpoint.getProblemLevel(), not(equalTo(0)));
			}
		}
		// operation 2: remove Web.xml override
		webxmlApplication.remove();
		final IFile webxmlResource = (IFile) webxmlApplication.getResource();
		webxmlResource.delete(true, new NullProgressMonitor());
		// then validate again, only the *changed files* (and without reset). Expect marker on both applications to be still there
		new JaxrsMetamodelValidator().validate(CollectionUtils.toSet(webxmlResource), project, validationHelper, context, validatorManager, reporter);
		// validation
		assertThat(findJaxrsMarkers(javaApplication).length, equalTo(1));
		// restApplication2 has 2 markers: missing annotation and duplicate application
		assertThat(findJaxrsMarkers(javaApplication2).length, equalTo(2));
		assertThat(metamodelProblemLevelChanges.contains(metamodel), is(true));
	}
	
	@Test
	public void shouldStillReportProblemAfterUnderlyingWebxmlApplicationResourceEditWhenMetamodelHasTwoJavaApplicationsAndWebxmlOverride() throws Exception {
		// preconditions
		final JaxrsWebxmlApplication webxmlApplication = metamodel.findWebxmlApplication();
		WorkbenchUtils.replaceContent(webxmlApplication.getResource(), "javax.ws.rs.core.Application", "org.jboss.tools.ws.jaxrs.sample.services.RestApplication2");
		final JaxrsJavaApplication javaApplication = metamodel.getJavaApplications().get(0);
		final IType restApplication2Type = WorkbenchUtils.createCompilationUnit(javaProject,
				"RestApplication2.txt", "org.jboss.tools.ws.jaxrs.sample.services", "RestApplication2.java").findPrimaryType();
		WorkbenchUtils.replaceFirstOccurrenceOfCode(restApplication2Type, "@ApplicationPath(\"/app2\")", "", false);
		final JaxrsJavaApplication javaApplication2 = JaxrsJavaApplication.from(restApplication2Type).withMetamodel(metamodel).build();
		deleteJaxrsMarkers(project);
		resetElementChangesNotifications();
		// operation 1: validate when there are 2 applications, expecting markers
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation: markers are on first javaapp and one web.xml only
		for(IJaxrsApplication app : new IJaxrsApplication[]{javaApplication, webxmlApplication}) {
			final IMarker[] markers = findJaxrsMarkers(app);
			assertThat(markers.length, equalTo(1));
			assertThat(markers, hasPreferenceKey(APPLICATION_TOO_MANY_OCCURRENCES));
			for(IJaxrsEndpoint endpoint : metamodel.findEndpoints(metamodel.getApplication())) {
				assertThat(endpoint.getProblemLevel(), not(equalTo(0)));
			}
		}
		// operation 2: remove Web.xml override
		webxmlApplication.remove();
		IFile webxmlResource = (IFile) webxmlApplication.getResource();
		// remove content, no remove resource
		webxmlResource = (IFile) WorkbenchUtils.replaceDeploymentDescriptorWith(javaProject, "web-3_0-without-servlet-mapping.xml");
		// then validate again, only the *changed files* (and without reset). Expect marker on both applications to be still there
		new JaxrsMetamodelValidator().validate(CollectionUtils.toSet(webxmlResource), project, validationHelper, context, validatorManager, reporter);
		// validation
		assertThat(findJaxrsMarkers(javaApplication).length, equalTo(1));
		// restApplication2 has 2 markers: missing annotation and duplicate application
		assertThat(findJaxrsMarkers(javaApplication2).length, equalTo(2));
		assertThat(metamodelProblemLevelChanges.contains(metamodel), is(true));
	}
	
	@Test
	public void shouldReportProblemOnApplicationsIfMultipleOnesExistWhenValidatingUnrelatedFile() throws CoreException, ValidationException {
		// preconditions
		final List<IJaxrsApplication> applications = metamodel.getAllApplications();
		assertThat(applications, hasSize(2));
		deleteJaxrsMarkers(project);
		resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(CollectionUtils.toSet((IFile)project.findMember(".project")), project, validationHelper, context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(project);
		assertThat(markers.length, equalTo(0));
		for (IJaxrsApplication application : metamodel.getAllApplications()) {
			final IMarker[] appMarkers = findJaxrsMarkers(application);
			assertThat(appMarkers.length, equalTo(1));
		}
		final List<JaxrsEndpoint> affectedEndpoints = metamodel.findEndpoints(metamodel.getApplication());
		for(IJaxrsEndpoint endpoint : affectedEndpoints) {
			assertThat(endpoint.getProblemLevel(), not(equalTo(0)));
		}
		assertThat(metamodelProblemLevelChanges.contains(metamodel), is(true));
	}
	
	@Test
	public void shouldNotFailOnProblemIfSeverityLevelIsIgnore() throws CoreException, ValidationException {
		// preconditions
		final List<IJaxrsApplication> applications = metamodel.getAllApplications();
		for (IJaxrsApplication application : applications) {
			((JaxrsBaseElement) application).remove();
		}
		deleteJaxrsMarkers(project);
		resetElementChangesNotifications();
		final IEclipsePreferences defaultPreferences = ((IScopeContext)DefaultScope.INSTANCE).getNode(JBossJaxrsCorePlugin.PLUGIN_ID);
		defaultPreferences.put(JaxrsPreferences.APPLICATION_NO_OCCURRENCE_FOUND, JaxrsPreferences.IGNORE);
		// operation
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(project);
		assertThat(markers.length, equalTo(0));
	}

	
}
