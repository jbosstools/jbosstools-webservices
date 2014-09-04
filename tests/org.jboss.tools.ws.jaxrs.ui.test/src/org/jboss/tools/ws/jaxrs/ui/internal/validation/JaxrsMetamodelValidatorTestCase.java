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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.deleteJaxrsMarkers;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.havePreferenceKey;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.toSet;
import static org.jboss.tools.ws.jaxrs.ui.preferences.JaxrsPreferences.HTTP_METHOD_MISSING_RETENTION_ANNOTATION;
import static org.junit.Assert.assertThat;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.wst.validation.ReporterHelper;
import org.eclipse.wst.validation.internal.core.ValidationException;
import org.eclipse.wst.validation.internal.provisional.core.IReporter;
import org.jboss.tools.common.validation.ContextValidationHelper;
import org.jboss.tools.common.validation.IProjectValidationContext;
import org.jboss.tools.common.validation.ValidatorManager;
import org.jboss.tools.common.validation.internal.ProjectValidationContext;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Xi
 * 
 */
@SuppressWarnings("restriction")
public class JaxrsMetamodelValidatorTestCase {

	private final IReporter reporter = new ReporterHelper(new NullProgressMonitor());
	private final ContextValidationHelper validationHelper = new ContextValidationHelper();
	private final IProjectValidationContext context = new ProjectValidationContext();
	private final ValidatorManager validatorManager = new ValidatorManager();

	
	@ClassRule
	public static WorkspaceSetupRule workspaceSetupRule = new WorkspaceSetupRule(
			"org.jboss.tools.ws.jaxrs.tests.sampleproject");

	@Rule
	public JaxrsMetamodelMonitor metamodelMonitor = new JaxrsMetamodelMonitor(
			"org.jboss.tools.ws.jaxrs.tests.sampleproject", false);

	private JaxrsMetamodel metamodel = null;

	private IProject project = null;

	private IJavaProject javaProject = null;

	@Before
	public void setup() throws CoreException {
		metamodel = metamodelMonitor.getMetamodel();
		project = metamodel.getProject();
		javaProject = metamodel.getJavaProject();
	}

	@Test
	public void shouldRemoveMarkersWhenElementRemovedAfterProjectSettingsChanged() throws CoreException, ValidationException, OperationCanceledException, InterruptedException {
		// preconditions
		ResourcesUtils.replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject, "@Retention(value=RetentionPolicy.RUNTIME)", "", true);
		final JaxrsHttpMethod fooHttpMethod = metamodelMonitor.createHttpMethod("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		deleteJaxrsMarkers(project);
		new JaxrsMetamodelValidator().validate(toSet(fooHttpMethod.getResource()), project, validationHelper, context,
						validatorManager, reporter);
		final IMarker[] markers = ValidationUtils.findJaxrsMarkers(fooHttpMethod);
		assertThat(markers.length, equalTo(1));
		assertThat(markers, havePreferenceKey(HTTP_METHOD_MISSING_RETENTION_ANNOTATION));
		// operation: remove the jaxrs-api-2.0.1.GA.jar classpath entry
		metamodelMonitor.removeClasspathEntry("jaxrs-api-2.0.1.GA.jar");
		metamodelMonitor.processEvent(javaProject, IJavaElementDelta.CHANGED);
		// validation
		final IMarker[] updatedMarkers = ValidationUtils.findJaxrsMarkers(fooHttpMethod);
		assertThat(updatedMarkers.length, equalTo(0));
	}
	
	@Test
	public void shouldRemoveJaxrsMarkersWhenElementIsRemoved() throws CoreException, ValidationException {
		// preconditions
		ResourcesUtils.replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.BarResource", javaProject, "getContent1(@PathParam(\"param1\") int id)", "getContent1(@PathParam(\"param3\") int id)", true);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.BarResource");
		final IType barType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BarResource");
		final JaxrsResource barResource = metamodel.findResource(barType);
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();
		final IResource settingsFile = project.findMember(".settings/org.eclipse.wst.common.project.facet.core.xml");
		// operation
		new JaxrsMetamodelValidator().validate(toSet(settingsFile), project, validationHelper, context,
						validatorManager, reporter);
				
		// validation
		final IMarker[] markers = ValidationUtils.findJaxrsMarkers(barResource);
		assertThat(markers.length, greaterThanOrEqualTo(1));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(1));
		
	}
	
}
