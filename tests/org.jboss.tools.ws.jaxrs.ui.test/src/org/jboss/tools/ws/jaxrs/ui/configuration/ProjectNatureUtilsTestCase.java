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

package org.jboss.tools.ws.jaxrs.ui.configuration;

import static org.hamcrest.Matchers.equalTo;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.deleteJaxrsMarkers;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.findJaxrsMarkers;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.wst.validation.ReporterHelper;
import org.eclipse.wst.validation.internal.core.ValidationException;
import org.jboss.tools.common.validation.ContextValidationHelper;
import org.jboss.tools.common.validation.ValidatorManager;
import org.jboss.tools.common.validation.internal.ProjectValidationContext;
import org.jboss.tools.ws.jaxrs.core.configuration.ProjectNatureUtils;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.AbstractJaxrsBaseElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.TestProjectMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.ui.internal.validation.JaxrsMetamodelValidator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Xi
 * 
 */
@SuppressWarnings("restriction")
public class ProjectNatureUtilsTestCase {

	@ClassRule
	public static WorkspaceSetupRule workspaceSetupRule = new WorkspaceSetupRule(
			"org.jboss.tools.ws.jaxrs.tests.sampleproject");

	@Rule
	public TestProjectMonitor projectMonitor = new TestProjectMonitor("org.jboss.tools.ws.jaxrs.tests.sampleproject");

	@Rule
	public JaxrsMetamodelMonitor metamodelMonitor = new JaxrsMetamodelMonitor(
			"org.jboss.tools.ws.jaxrs.tests.sampleproject", true);

	private JaxrsMetamodel metamodel = null;

	private IProject project = null;
	
	@Before
	public void setup() {
		this.project = projectMonitor.getProject();
	}

	@Test
	public void shouldVerifyProjectNatureIsNotInstalled() throws Exception {
		// when
		removeJaxrsNature(project);
		// then
		Assert.assertFalse("Wrong result", isJaxrsNatureInstalled(project));
	}

	@Test
	public void shouldVerifyProjectNatureIsInstalled() throws Exception {
		Assert.assertTrue("Wrong result", ProjectNatureUtils.isProjectNatureInstalled(projectMonitor.getProject(),
				"org.eclipse.jdt.core.javanature"));
	}

	@Test
	public void shouldInstallAndUninstallProjectNature() throws Exception {
		// when
		removeJaxrsNature(project);
		// then
		Assert.assertFalse("Wrong result", isJaxrsNatureInstalled(project));
		// when
		addJaxrsNature(project);
		// then
		Assert.assertTrue("Wrong result", isJaxrsNatureInstalled(project));
		// when
		removeJaxrsNature(project);
		// then
		Assert.assertFalse("Wrong result",isJaxrsNatureInstalled(project));
	}

	@Test
	public void shouldRemoveMarkersWhenProjectNatureIsRemoved() throws CoreException, ValidationException {
		// only keep CustomerResource, have a marker for missing Application
		ProjectNatureUtils.installProjectNature(projectMonitor.getProject(), ProjectNatureUtils.JAXRS_NATURE_ID);
		metamodel = metamodelMonitor.initMetamodel();
		project = metamodel.getProject();
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
		new JaxrsMetamodelValidator().validateAll(project, new ContextValidationHelper(), new ProjectValidationContext(), new ValidatorManager(), new ReporterHelper(new NullProgressMonitor()));
		assertThat(findJaxrsMarkers(project).length, equalTo(1));
		// operation
		removeJaxrsNature(project);
		// validation: JAX-RS markers were removed
		assertThat(findJaxrsMarkers(project).length, equalTo(0));
	}

	static void addJaxrsNature(final IProject project) {
		final AddNatureAction addNatureAction = new AddNatureAction();
		addNatureAction.selectionChanged(null, new StructuredSelection(project));
		addNatureAction.run(null);
	}
	
	static boolean isJaxrsNatureInstalled(final IProject project) throws CoreException {
		return ProjectNatureUtils.isProjectNatureInstalled(project, ProjectNatureUtils.JAXRS_NATURE_ID);
	}


	static void removeJaxrsNature(final IProject project) {
		final RemoveNatureAction removeNatureAction = new RemoveNatureAction();
		removeNatureAction.selectionChanged(null, new StructuredSelection(project));
		removeNatureAction.run(null);
	}
}
