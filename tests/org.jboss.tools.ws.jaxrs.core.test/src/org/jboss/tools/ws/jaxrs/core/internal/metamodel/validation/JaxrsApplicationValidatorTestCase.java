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
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.wst.validation.ReporterHelper;
import org.eclipse.wst.validation.internal.core.ValidationException;
import org.eclipse.wst.validation.internal.provisional.core.IReporter;
import org.jboss.tools.common.validation.ContextValidationHelper;
import org.jboss.tools.common.validation.IProjectValidationContext;
import org.jboss.tools.common.validation.ValidatorManager;
import org.jboss.tools.common.validation.internal.ProjectValidationContext;
import org.jboss.tools.ws.jaxrs.core.WorkbenchUtils;
import org.jboss.tools.ws.jaxrs.core.builder.AbstractMetamodelBuilderTestCase;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsBaseElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsJavaApplication;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsWebxmlApplication;
import org.jboss.tools.ws.jaxrs.core.internal.utils.WtpUtils;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsApplication;
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

	/**
	 * Creates a web.xml based JAX-RS Application element
	 * 
	 * @param applicationPath
	 * @return
	 * @throws JavaModelException
	 */
	private JaxrsWebxmlApplication createWebxmlApplication(final String applicationClassName, final String applicationPath) throws JavaModelException {
		final IResource webDeploymentDescriptor = WtpUtils.getWebDeploymentDescriptor(project);
		return new JaxrsWebxmlApplication(applicationClassName, applicationPath, webDeploymentDescriptor, metamodel);
	}

	@Test
	public void shouldNotReportProblemIfOneJavaApplicationExists() throws CoreException, ValidationException {
		// preconditions
		final List<IJaxrsApplication> applications = metamodel.getAllApplications();
		for (IJaxrsApplication application : applications) {
			if (application.getElementKind() == EnumElementKind.APPLICATION_WEBXML) {
				metamodel.remove((JaxrsBaseElement) application);
			}
		}
		MarkerUtils.deleteJaxrsMarkers(project);
		assertThat(metamodel.getAllApplications().size(), equalTo(1));
		assertThat(metamodel.getApplication().getElementKind(), equalTo(EnumElementKind.APPLICATION_JAVA));
		// operation
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation
		assertThat(project.findMarkers(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, false, 0).length, equalTo(0));
	}

	@Test
	public void shouldNotReportProblemIfOneWebxmlApplicationExists() throws CoreException, ValidationException {
		// preconditions
		final List<IJaxrsApplication> applications = metamodel.getAllApplications();
		for (IJaxrsApplication application : applications) {
			if (application.getElementKind() == EnumElementKind.APPLICATION_JAVA) {
				metamodel.remove((JaxrsBaseElement) application);
			}
		}
		assertThat(metamodel.getAllApplications().size(), equalTo(1));
		assertThat(metamodel.getApplication().getElementKind(), equalTo(EnumElementKind.APPLICATION_WEBXML));
		MarkerUtils.deleteJaxrsMarkers(project);
		// operation
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation
		assertThat(project.findMarkers(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, false, 0).length, equalTo(0));
	}

	@Test
	public void shouldReportProblemOnProjectIfNoApplicationExists() throws CoreException, ValidationException {
		// preconditions
		final List<IJaxrsApplication> applications = metamodel.getAllApplications();
		for (IJaxrsApplication application : applications) {
			metamodel.remove((JaxrsBaseElement) application);
		}
		MarkerUtils.deleteJaxrsMarkers(project);
		// operation
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation
		final IMarker[] projectMarkers = project.findMarkers(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, false, 0);
		assertThat(projectMarkers.length, equalTo(1));
	}

	@Test
	public void shouldReportProblemOnApplicationsIfMultipleOnesExist() throws CoreException, ValidationException {
		// preconditions
		final List<IJaxrsApplication> applications = metamodel.getAllApplications();
		assertThat(applications, hasSize(2));
		MarkerUtils.deleteJaxrsMarkers(project);
		// operation
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation
		final IMarker[] projectMarkers = project.findMarkers(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, false, 0);
		assertThat(projectMarkers.length, equalTo(0));
		for (IJaxrsApplication application : metamodel.getAllApplications()) {
			final IMarker[] appMarkers = ((JaxrsBaseElement) application).getResource().findMarkers(
					JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, true, IResource.DEPTH_INFINITE);
			assertThat(appMarkers.length, equalTo(1));
		}
	}

	@Test
	public void shouldReportProblemOnJavaApplicationIfMissingApplicationPathAnnotationWithoutOverride()
			throws CoreException, ValidationException {
		// preconditions
		final List<IJaxrsApplication> applications = metamodel.getAllApplications();
		assertThat(applications, hasSize(2));
		MarkerUtils.deleteJaxrsMarkers(project);
		JaxrsJavaApplication javaApplication = null;
		// remove web.xml-based application and remove @ApplicationPath annotation on java-based application
		for (IJaxrsApplication application : applications) {
			if (application.getElementKind() == EnumElementKind.APPLICATION_WEBXML) {
				metamodel.remove((JaxrsBaseElement) application);
			} else {
				javaApplication = (JaxrsJavaApplication) application;
				final Annotation appPathAnnotation = javaApplication
						.getAnnotation(EnumJaxrsClassname.APPLICATION_PATH.qualifiedName);
				javaApplication.removeAnnotation(appPathAnnotation);
			}
		}
		// operation
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation
		final IMarker[] projectMarkers = javaApplication.getJavaElement().getResource()
				.findMarkers(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, false, 0);
		assertThat(projectMarkers.length, equalTo(1));
	}

	@Test
	public void shouldReportProblemOnJavaApplicationIfInvalidTypeHierarchy() throws CoreException, ValidationException {
		// preconditions
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication",
				javaProject, new NullProgressMonitor());
		WorkbenchUtils.replaceAllOccurrencesOfCode(type.getCompilationUnit(), "extends Application", "", false);
		final JaxrsJavaApplication javaApplication = metamodel.getJavaApplications().get(0);
		MarkerUtils.deleteJaxrsMarkers(project);
		// operation
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation
		final IMarker[] projectMarkers = javaApplication.getJavaElement().getResource()
				.findMarkers(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, false, 0);
		assertThat(projectMarkers.length, equalTo(1));
	}

	@Test
	public void shouldNotReportProblemOnApplicationIfMissingApplicationPathAnnotationWithOverride()
			throws Exception {
		// preconditions
		final JaxrsJavaApplication javaApplication = metamodel.getJavaApplications().get(0);
		final Annotation appPathAnnotation = javaApplication.getAnnotation(EnumJaxrsClassname.APPLICATION_PATH.qualifiedName);
		javaApplication.removeAnnotation(appPathAnnotation);
		final JaxrsWebxmlApplication webxmlDefaultApplication = (JaxrsWebxmlApplication) metamodel.getApplication();
		metamodel.remove(webxmlDefaultApplication);
		final JaxrsWebxmlApplication webxmlApplication = createWebxmlApplication(javaApplication.getJavaClassName(), "/foo");
		metamodel.add(webxmlApplication);
		MarkerUtils.deleteJaxrsMarkers(project);
		// operation
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation
		final IMarker[] projectMarkers = javaApplication.getJavaElement().getResource()
				.findMarkers(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, false, 0);
		assertThat(projectMarkers.length, equalTo(0));
	}

	@Test
	public void shouldNotReportProblemOnApplicationIfAnnotationExistsAndHierarchyValid() throws CoreException,
			ValidationException {
		// preconditions
		final JaxrsJavaApplication javaApplication = metamodel.getJavaApplications().get(0);
		final JaxrsWebxmlApplication webxmlDefaultApplication = (JaxrsWebxmlApplication) metamodel.getApplication();
		metamodel.remove(webxmlDefaultApplication);
		MarkerUtils.deleteJaxrsMarkers(project);
		// operation
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation
		final IMarker[] projectMarkers = javaApplication.getJavaElement().getResource()
				.findMarkers(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, false, 0);
		assertThat(projectMarkers.length, equalTo(0));
	}
}
