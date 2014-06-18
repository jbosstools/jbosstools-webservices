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

package org.jboss.tools.ws.jaxrs.ui.wizards;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Collection;

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsBaseElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsApplication;
import org.jboss.tools.ws.jaxrs.ui.cnf.UriMappingsContentProvider;
import org.jboss.tools.ws.jaxrs.ui.cnf.UriPathTemplateCategory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author xcoulon
 *
 */
public class JaxrsApplicationCreationWizardPageTestCase {

	@ClassRule
	public static WorkspaceSetupRule workspaceSetupRule = new WorkspaceSetupRule(
			"org.jboss.tools.ws.jaxrs.tests.sampleproject");

	@Rule
	public JaxrsMetamodelMonitor metamodelMonitor = new JaxrsMetamodelMonitor(
			"org.jboss.tools.ws.jaxrs.tests.sampleproject", true);

	private JaxrsMetamodel metamodel = null;

	private IJavaProject javaProject = null;

	@Before
	public void setup() throws CoreException {
		metamodel = metamodelMonitor.getMetamodel();
		javaProject = metamodel.getJavaProject();
	}

	@Test
	public void shouldInitializeControlsWhenCompilationUnitSelected() throws CoreException {
		// given
		final JaxrsApplicationCreationWizardPage wizardPage = new JaxrsApplicationCreationWizardPage(true);
		final IType customerType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.domain.Customer", javaProject,
				new NullProgressMonitor());
		final IStructuredSelection selection = new StructuredSelection(customerType.getCompilationUnit());
		// when
		wizardPage.init(selection);
		// then
		Assert.assertThat(wizardPage.getPackageFragmentRootText(), equalTo(javaProject.getElementName()
				+ "/src/main/java"));
		Assert.assertThat(wizardPage.getPackageText(), equalTo("org.jboss.tools.ws.jaxrs.sample.rest"));
		Assert.assertThat(wizardPage.getTypeName(), equalTo("RestApplication"));
		Assert.assertThat(wizardPage.getApplicationPath(), equalTo("/rest"));
		Assert.assertThat(wizardPage.isPageComplete(), equalTo(true));
	}

	@Test
	public void shouldInitializeControlsWhenTypeSelected() throws CoreException {
		// given
		final JaxrsApplicationCreationWizardPage wizardPage = new JaxrsApplicationCreationWizardPage(true);
		final IType customerType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.domain.Customer", javaProject,
				new NullProgressMonitor());
		final IStructuredSelection selection = new StructuredSelection(customerType);
		// when
		wizardPage.init(selection);
		// then
		Assert.assertThat(wizardPage.getPackageFragmentRootText(), equalTo(javaProject.getElementName()
				+ "/src/main/java"));
		Assert.assertThat(wizardPage.getPackageText(), equalTo("org.jboss.tools.ws.jaxrs.sample.rest"));
		Assert.assertThat(wizardPage.getTypeName(), equalTo("RestApplication"));
		Assert.assertThat(wizardPage.getApplicationPath(), equalTo("/rest"));
		Assert.assertThat(wizardPage.isPageComplete(), equalTo(true));
	}

	@Test
	public void shouldInitializeControlsWhenPackageSelected() throws CoreException {
		// given
		final JaxrsApplicationCreationWizardPage wizardPage = new JaxrsApplicationCreationWizardPage(true);
		final IType customerType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.domain.Customer", javaProject,
				new NullProgressMonitor());
		final IStructuredSelection selection = new StructuredSelection(customerType.getPackageFragment());
		// when
		wizardPage.init(selection);
		// then
		Assert.assertThat(wizardPage.getPackageFragmentRootText(), equalTo(javaProject.getElementName()
				+ "/src/main/java"));
		Assert.assertThat(wizardPage.getPackageText(), equalTo("org.jboss.tools.ws.jaxrs.sample.domain"));
		Assert.assertThat(wizardPage.getTypeName(), equalTo("RestApplication"));
		Assert.assertThat(wizardPage.getApplicationPath(), equalTo("/rest"));
		Assert.assertThat(wizardPage.isPageComplete(), equalTo(true));
	}

	@Test
	public void shouldInitializeControlsWhenSourceFolderSelected() throws CoreException {
		// given
		final JaxrsApplicationCreationWizardPage wizardPage = new JaxrsApplicationCreationWizardPage(true);
		final IType customerType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.domain.Customer", javaProject,
				new NullProgressMonitor());
		final IStructuredSelection selection = new StructuredSelection(customerType.getPackageFragment().getParent());
		// when
		wizardPage.init(selection);
		// then
		Assert.assertThat(wizardPage.getPackageFragmentRootText(), equalTo(javaProject.getElementName()
				+ "/src/main/java"));
		Assert.assertThat(wizardPage.getPackageText(), equalTo(""));
		Assert.assertThat(wizardPage.getTypeName(), equalTo(""));
		Assert.assertThat(wizardPage.getApplicationPath(), equalTo("/rest"));
		Assert.assertThat(wizardPage.isPageComplete(), equalTo(false));
	}

	@Test
	public void shouldInitializeControlsWhenUriPathTemplateCategorySelected() throws CoreException {
		// given
		final JaxrsApplicationCreationWizardPage wizardPage = new JaxrsApplicationCreationWizardPage(true);
		final UriPathTemplateCategory category = new UriPathTemplateCategory(new UriMappingsContentProvider(),
				javaProject);
		final IStructuredSelection selection = new StructuredSelection(category);
		// when
		wizardPage.init(selection);
		// then
		Assert.assertThat(wizardPage.getPackageFragmentRootText(), equalTo(javaProject.getElementName()
				+ "/src/main/java"));
		Assert.assertThat(wizardPage.getPackageText(), equalTo(""));
		Assert.assertThat(wizardPage.getTypeName(), equalTo(""));
		Assert.assertThat(wizardPage.getApplicationPath(), equalTo("/rest"));
		Assert.assertThat(wizardPage.isPageComplete(), equalTo(false));
	}

	@Test
	public void shouldCreateJavaApplicationClass() throws CoreException, InterruptedException {
		// given
		final Collection<IJaxrsApplication> allApplications = metamodel.findAllApplications();
		for(IJaxrsApplication application : allApplications) {
			((JaxrsBaseElement) application).remove();
			((JaxrsBaseElement) application).getResource().delete(true, new NullProgressMonitor());
		}
		final JaxrsApplicationCreationWizardPage wizardPage = new JaxrsApplicationCreationWizardPage(true);
		final IType customerType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.domain.Customer", javaProject,
				new NullProgressMonitor());
		final IStructuredSelection selection = new StructuredSelection(customerType);
		// when
		wizardPage.init(selection);
		wizardPage.createType(new NullProgressMonitor());
		// then
		final IType createdType = wizardPage.getCreatedType();
		assertThat(createdType, notNullValue());
		assertThat(createdType.getMethods().length, equalTo(0));
		// trigger a clean build before asserting the new JAX-RS elements
		metamodelMonitor.buildProject(IncrementalProjectBuilder.FULL_BUILD);
		// 6 new elements: 1 resource + 5 resource methods
		final IJaxrsApplication createdApplication = metamodel.findApplication();
		assertThat(createdApplication, notNullValue());
		assertThat(createdApplication.getElementKind(), equalTo(EnumElementKind.APPLICATION_JAVA));
		assertThat(createdApplication.getApplicationPath(), equalTo("/rest"));
	}

	@Test
	public void shouldCreateWebxmlApplicationWhenNoWebxmlExists() throws CoreException, InterruptedException {
		// given
		final Collection<IJaxrsApplication> allApplications = metamodel.findAllApplications();
		for(IJaxrsApplication application : allApplications) {
			((JaxrsBaseElement) application).remove();
			((JaxrsBaseElement) application).getResource().delete(true, new NullProgressMonitor());
		}

		final JaxrsApplicationCreationWizardPage wizardPage = new JaxrsApplicationCreationWizardPage(true);
		final IType customerType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.domain.Customer", javaProject,
				new NullProgressMonitor());
		final IStructuredSelection selection = new StructuredSelection(customerType);
		// when
		wizardPage.init(selection);
		wizardPage.setApplicationMode(JaxrsApplicationCreationWizardPage.APPLICATION_WEB_XML);
		wizardPage.createType(new NullProgressMonitor());
		// then
		//final IResource webxmlResource = wizardPage.getCreatedWebxmlResource();
		//assertThat(webxmlResource, nullValue());
		final IType createdType = wizardPage.getCreatedType();
		assertThat(createdType, nullValue());
		// trigger a clean build before asserting the new JAX-RS elements
		metamodelMonitor.buildProject(IncrementalProjectBuilder.FULL_BUILD);
		// 1 new element: 1 Java Application 
		final IJaxrsApplication createdApplication = metamodel.findApplication();
		assertThat(createdApplication, notNullValue());
		assertThat(createdApplication.getElementKind(), equalTo(EnumElementKind.APPLICATION_WEBXML));
		assertThat(createdApplication.getApplicationPath(), equalTo("/rest"));
	}

	@Test
	public void shouldCreateWebxmlApplicationWhenWebxmlExists() throws Exception {
		// given
		final Collection<IJaxrsApplication> allApplications = metamodel.findAllApplications();
		for(IJaxrsApplication application : allApplications) {
			((JaxrsBaseElement) application).remove();
			((JaxrsBaseElement) application).getResource().delete(true, new NullProgressMonitor());
		}
		metamodelMonitor.replaceDeploymentDescriptorWith("web-3_0-without-servlet-mapping.xml");
		final JaxrsApplicationCreationWizardPage wizardPage = new JaxrsApplicationCreationWizardPage(true);
		final IType customerType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.domain.Customer", javaProject,
				new NullProgressMonitor());
		final IStructuredSelection selection = new StructuredSelection(customerType);
		// when
		wizardPage.init(selection);
		wizardPage.setApplicationMode(JaxrsApplicationCreationWizardPage.APPLICATION_WEB_XML);
		wizardPage.createType(new NullProgressMonitor());
		// then
		final IType createdType = wizardPage.getCreatedType();
		assertThat(createdType, nullValue());
		// trigger a clean build before asserting the new JAX-RS elements
		metamodelMonitor.buildProject(IncrementalProjectBuilder.FULL_BUILD);
		// 1 new element: 1 web.xml Application 
		final IJaxrsApplication createdApplication = metamodel.findApplication();
		assertThat(createdApplication, notNullValue());
		assertThat(createdApplication.getElementKind(), equalTo(EnumElementKind.APPLICATION_WEBXML));
		assertThat(createdApplication.getApplicationPath(), equalTo("/rest"));
	}

	@Test
	public void shouldSkipApplicationCreation() throws CoreException, InterruptedException {
		// given
		final Collection<IJaxrsApplication> allApplications = metamodel.findAllApplications();
		for(IJaxrsApplication application : allApplications) {
			((JaxrsBaseElement) application).remove();
			((JaxrsBaseElement) application).getResource().delete(true, new NullProgressMonitor());
		}

		final JaxrsApplicationCreationWizardPage wizardPage = new JaxrsApplicationCreationWizardPage(true);
		final IType customerType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.domain.Customer", javaProject,
				new NullProgressMonitor());
		final IStructuredSelection selection = new StructuredSelection(customerType);
		// when
		wizardPage.init(selection);
		wizardPage.setApplicationMode(JaxrsApplicationCreationWizardPage.SKIP_APPLICATION);
		wizardPage.createType(new NullProgressMonitor());
		// then
		final IType createdType = wizardPage.getCreatedType();
		assertThat(createdType, nullValue());
		// trigger a clean build before asserting the new JAX-RS elements
		metamodelMonitor.buildProject(IncrementalProjectBuilder.FULL_BUILD);
		// 0 new element: metamodel has no application
		final IJaxrsApplication createdApplication = metamodel.findApplication();
		assertThat(createdApplication, nullValue());
	}
	
	
}
