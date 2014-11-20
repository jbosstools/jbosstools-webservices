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

package org.jboss.tools.ws.jaxrs.ui.wizards;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResource;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames;
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
public class JaxrsResourceCreationWizardPageTestCase {

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
		final JaxrsResourceCreationWizardPage wizardPage = new JaxrsResourceCreationWizardPage();
		final IType customerType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.domain.Customer", javaProject,
				new NullProgressMonitor());
		final IStructuredSelection selection = new StructuredSelection(customerType.getCompilationUnit());
		// when
		wizardPage.init(selection);
		// then
		Assert.assertThat(wizardPage.getPackageFragmentRootText(), equalTo(javaProject.getElementName()
				+ "/src/main/java"));
		Assert.assertThat(wizardPage.getPackageText(), equalTo("org.jboss.tools.ws.jaxrs.sample.rest"));
		Assert.assertThat(wizardPage.getTypeName(), equalTo("CustomerEndpoint"));
		Assert.assertThat(wizardPage.getResourcePath(), equalTo("/customers"));
		Assert.assertThat(wizardPage.getMediaTypes(), hasItems("application/json", "application/xml"));
		Assert.assertThat(wizardPage.getTargetClass(), equalTo(customerType.getFullyQualifiedName()));
		Assert.assertThat(wizardPage.isPageComplete(), equalTo(true));
	}

	@Test
	public void shouldInitializeControlsWhenTypeSelected() throws CoreException {
		// given
		final JaxrsResourceCreationWizardPage wizardPage = new JaxrsResourceCreationWizardPage();
		final IType customerType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.domain.Customer", javaProject,
				new NullProgressMonitor());
		final IStructuredSelection selection = new StructuredSelection(customerType);
		// when
		wizardPage.init(selection);
		// then
		Assert.assertThat(wizardPage.getPackageFragmentRootText(), equalTo(javaProject.getElementName()
				+ "/src/main/java"));
		Assert.assertThat(wizardPage.getPackageText(), equalTo("org.jboss.tools.ws.jaxrs.sample.rest"));
		Assert.assertThat(wizardPage.getTypeName(), equalTo("CustomerEndpoint"));
		Assert.assertThat(wizardPage.getResourcePath(), equalTo("/customers"));
		Assert.assertThat(wizardPage.getMediaTypes(), hasItems("application/json", "application/xml"));
		Assert.assertThat(wizardPage.getTargetClass(), equalTo(customerType.getFullyQualifiedName()));
		Assert.assertThat(wizardPage.isPageComplete(), equalTo(true));
	}

	@Test
	public void shouldInitializeControlsWhenPackageSelected() throws CoreException {
		// given
		final JaxrsResourceCreationWizardPage wizardPage = new JaxrsResourceCreationWizardPage();
		final IType customerType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.domain.Customer", javaProject,
				new NullProgressMonitor());
		final IStructuredSelection selection = new StructuredSelection(customerType.getPackageFragment());
		// when
		wizardPage.init(selection);
		// then
		Assert.assertThat(wizardPage.getPackageFragmentRootText(), equalTo(javaProject.getElementName()
				+ "/src/main/java"));
		Assert.assertThat(wizardPage.getPackageText(), equalTo("org.jboss.tools.ws.jaxrs.sample.domain"));
		Assert.assertThat(wizardPage.getTypeName(), equalTo(""));
		Assert.assertThat(wizardPage.getResourcePath(), equalTo(""));
		Assert.assertThat(wizardPage.getMediaTypes(), hasItems("application/json", "application/xml"));
		Assert.assertThat(wizardPage.getTargetClass(), equalTo(""));
		Assert.assertThat(wizardPage.isPageComplete(), equalTo(false));
	}

	@Test
	public void shouldInitializeControlsWhenSourceFolderSelected() throws CoreException {
		// given
		final JaxrsResourceCreationWizardPage wizardPage = new JaxrsResourceCreationWizardPage();
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
		Assert.assertThat(wizardPage.getResourcePath(), equalTo(""));
		Assert.assertThat(wizardPage.getMediaTypes(), hasItems("application/json", "application/xml"));
		Assert.assertThat(wizardPage.getTargetClass(), equalTo(""));
		Assert.assertThat(wizardPage.isPageComplete(), equalTo(false));
	}

	@Test
	public void shouldInitializeControlsWhenUriPathTemplateCategorySelected() throws CoreException {
		// given
		final JaxrsResourceCreationWizardPage wizardPage = new JaxrsResourceCreationWizardPage();
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
		Assert.assertThat(wizardPage.getResourcePath(), equalTo(""));
		Assert.assertThat(wizardPage.getMediaTypes(), hasItems("application/json", "application/xml"));
		Assert.assertThat(wizardPage.getTargetClass(), equalTo(""));
		Assert.assertThat(wizardPage.isPageComplete(), equalTo(false));
	}

	@Test
	public void shouldCreateResourceClassWithAllMethods() throws CoreException, InterruptedException {
		// given
		final JaxrsResourceCreationWizardPage wizardPage = new JaxrsResourceCreationWizardPage();
		final IType customerType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.domain.Customer", javaProject,
				new NullProgressMonitor());
		final IStructuredSelection selection = new StructuredSelection(customerType);
		// when
		wizardPage.init(selection);
		wizardPage.setIncludeCreateMethod(true);
		wizardPage.setIncludeDeleteByIdMethod(true);
		wizardPage.setIncludeFindByIdMethod(true);
		wizardPage.setIncludeListAllMethod(true);
		wizardPage.setIncludeUpdateMethod(true);
		wizardPage.createType(new NullProgressMonitor());
		// then
		final IType createdType = wizardPage.getCreatedType();
		assertThat(createdType, notNullValue());
		assertThat(createdType.getMethods().length, equalTo(5));
		// trigger a clean build before asserting the new JAX-RS elements
		metamodelMonitor.buildProject(IncrementalProjectBuilder.FULL_BUILD);
		// 6 new elements: 1 resource + 5 resource methods
		final IJaxrsResource createdResource = (IJaxrsResource) metamodel.findElement(createdType);
		assertThat(createdResource, notNullValue());
		assertThat(createdResource.getAllMethods().size(), equalTo(5));
		assertThat(createdResource.getPathTemplate(), equalTo("/customers"));
	}

	@Test
	public void shouldCreateResourceClassWithNoMethod() throws CoreException, InterruptedException {
		// given
		final JaxrsResourceCreationWizardPage wizardPage = new JaxrsResourceCreationWizardPage();
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
		final IJaxrsResource createdResource = (IJaxrsResource) metamodel.findElement(createdType);
		assertThat(createdResource, notNullValue());
		assertThat(createdResource.getAllMethods().size(), equalTo(0));
		assertThat(createdResource.getPathTemplate(), equalTo("/customers"));
	}

	@Test
	public void shouldCreateResourceClassWithNoMethodWhenNoTargetClass() throws CoreException, InterruptedException {
		// given
		final JaxrsResourceCreationWizardPage wizardPage = new JaxrsResourceCreationWizardPage();
		final IType customerType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.domain.Customer", javaProject,
				new NullProgressMonitor());
		final IStructuredSelection selection = new StructuredSelection(customerType);
		// when
		wizardPage.init(selection);
		wizardPage.setIncludeCreateMethod(true);
		wizardPage.setIncludeDeleteByIdMethod(true);
		wizardPage.setIncludeFindByIdMethod(true);
		wizardPage.setIncludeListAllMethod(true);
		wizardPage.setIncludeUpdateMethod(true);
		// remove the target class
		wizardPage.setTargetClass("");
		wizardPage.createType(new NullProgressMonitor());
		// then
		final IType createdType = wizardPage.getCreatedType();
		assertThat(createdType, notNullValue());
		assertThat(createdType.getMethods().length, equalTo(0));
		// trigger a clean build before asserting the new JAX-RS elements
		metamodelMonitor.buildProject(IncrementalProjectBuilder.FULL_BUILD);
		// 6 new elements: 1 resource + 5 resource methods
		final IJaxrsResource createdResource = (IJaxrsResource) metamodel.findElement(createdType);
		assertThat(createdResource, notNullValue());
		assertThat(createdResource.getAllMethods().size(), equalTo(0));
		assertThat(createdResource.getPathTemplate(), equalTo("/customers"));
	}
	
	@Test
	public void shouldCreateResourceClassWithCreateMethod() throws CoreException, InterruptedException {
		// given
		final JaxrsResourceCreationWizardPage wizardPage = new JaxrsResourceCreationWizardPage();
		final IType customerType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.domain.Customer", javaProject,
				new NullProgressMonitor());
		final IStructuredSelection selection = new StructuredSelection(customerType);
		// when
		wizardPage.init(selection);
		wizardPage.setIncludeCreateMethod(true);
		wizardPage.setIncludeDeleteByIdMethod(false);
		wizardPage.setIncludeFindByIdMethod(false);
		wizardPage.setIncludeListAllMethod(false);
		wizardPage.setIncludeUpdateMethod(false);
		wizardPage.createType(new NullProgressMonitor());
		// then
		final IType createdType = wizardPage.getCreatedType();
		assertThat(createdType, notNullValue());
		assertThat(createdType.getMethods().length, equalTo(1));
		// trigger a clean build before asserting the new JAX-RS elements
		metamodelMonitor.buildProject(IncrementalProjectBuilder.FULL_BUILD);
		// 6 new elements: 1 resource + 5 resource methods
		final IJaxrsResource createdResource = (IJaxrsResource) metamodel.findElement(createdType);
		assertThat(createdResource, notNullValue());
		assertThat(createdResource.getAllMethods().size(), equalTo(1));
		final IJaxrsResourceMethod resourceMethod = createdResource.getAllMethods().get(0);
		assertThat(resourceMethod.getName(), equalTo("create"));
		assertThat(resourceMethod.getHttpMethodClassName(), equalTo(JaxrsClassnames.POST));
		assertThat(resourceMethod.getPathTemplate(), nullValue());
		assertThat(resourceMethod.getConsumedMediaTypes(), hasItems("application/json", "application/xml"));
		assertThat(resourceMethod.getProducedMediaTypes().size(), equalTo(0));
	}
	@Test
	public void shouldCreateResourceClassWithDeleteMethod() throws CoreException, InterruptedException {
		// given
		final JaxrsResourceCreationWizardPage wizardPage = new JaxrsResourceCreationWizardPage();
		final IType customerType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.domain.Customer", javaProject,
				new NullProgressMonitor());
		final IStructuredSelection selection = new StructuredSelection(customerType);
		// when
		wizardPage.init(selection);
		wizardPage.setIncludeCreateMethod(false);
		wizardPage.setIncludeDeleteByIdMethod(true);
		wizardPage.setIncludeFindByIdMethod(false);
		wizardPage.setIncludeListAllMethod(false);
		wizardPage.setIncludeUpdateMethod(false);
		wizardPage.createType(new NullProgressMonitor());
		// then
		final IType createdType = wizardPage.getCreatedType();
		assertThat(createdType, notNullValue());
		assertThat(createdType.getMethods().length, equalTo(1));
		// trigger a clean build before asserting the new JAX-RS elements
		metamodelMonitor.buildProject(IncrementalProjectBuilder.FULL_BUILD);
		// 6 new elements: 1 resource + 5 resource methods
		final IJaxrsResource createdResource = (IJaxrsResource) metamodel.findElement(createdType);
		assertThat(createdResource, notNullValue());
		assertThat(createdResource.getAllMethods().size(), equalTo(1));
		final IJaxrsResourceMethod resourceMethod = createdResource.getAllMethods().get(0);
		assertThat(resourceMethod.getName(), equalTo("deleteById"));
		assertThat(resourceMethod.getHttpMethodClassName(), equalTo(JaxrsClassnames.DELETE));
		assertThat(resourceMethod.getPathTemplate(), equalTo("/{id:[0-9][0-9]*}"));
		assertThat(resourceMethod.getProducedMediaTypes().size(), equalTo(0));
		assertThat(resourceMethod.getConsumedMediaTypes().size(), equalTo(0));
	}
	@Test
	public void shouldCreateResourceClassWithFindByIdMethod() throws CoreException, InterruptedException {
		// given
		final JaxrsResourceCreationWizardPage wizardPage = new JaxrsResourceCreationWizardPage();
		final IType customerType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.domain.Customer", javaProject,
				new NullProgressMonitor());
		final IStructuredSelection selection = new StructuredSelection(customerType);
		// when
		wizardPage.init(selection);
		wizardPage.setIncludeCreateMethod(false);
		wizardPage.setIncludeDeleteByIdMethod(false);
		wizardPage.setIncludeFindByIdMethod(true);
		wizardPage.setIncludeListAllMethod(false);
		wizardPage.setIncludeUpdateMethod(false);
		wizardPage.createType(new NullProgressMonitor());
		// then
		final IType createdType = wizardPage.getCreatedType();
		assertThat(createdType, notNullValue());
		assertThat(createdType.getMethods().length, equalTo(1));
		// trigger a clean build before asserting the new JAX-RS elements
		metamodelMonitor.buildProject(IncrementalProjectBuilder.FULL_BUILD);
		// 6 new elements: 1 resource + 5 resource methods
		final IJaxrsResource createdResource = (IJaxrsResource) metamodel.findElement(createdType);
		assertThat(createdResource, notNullValue());
		assertThat(createdResource.getAllMethods().size(), equalTo(1));
		final IJaxrsResourceMethod resourceMethod = createdResource.getAllMethods().get(0);
		assertThat(resourceMethod.getName(), equalTo("findById"));
		assertThat(resourceMethod.getHttpMethodClassName(), equalTo(JaxrsClassnames.GET));
		assertThat(resourceMethod.getPathTemplate(), equalTo("/{id:[0-9][0-9]*}"));
		assertThat(resourceMethod.getProducedMediaTypes(), hasItems("application/json", "application/xml"));
		assertThat(resourceMethod.getConsumedMediaTypes().size(), equalTo(0));
	}
	@Test
	public void shouldCreateResourceClassWithListAllMethod() throws CoreException, InterruptedException {
		// given
		final JaxrsResourceCreationWizardPage wizardPage = new JaxrsResourceCreationWizardPage();
		final IType customerType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.domain.Customer", javaProject,
				new NullProgressMonitor());
		final IStructuredSelection selection = new StructuredSelection(customerType);
		// when
		wizardPage.init(selection);
		wizardPage.setIncludeCreateMethod(false);
		wizardPage.setIncludeDeleteByIdMethod(false);
		wizardPage.setIncludeFindByIdMethod(false);
		wizardPage.setIncludeListAllMethod(true);
		wizardPage.setIncludeUpdateMethod(false);
		wizardPage.createType(new NullProgressMonitor());
		// then
		final IType createdType = wizardPage.getCreatedType();
		assertThat(createdType, notNullValue());
		assertThat(createdType.getMethods().length, equalTo(1));
		// trigger a clean build before asserting the new JAX-RS elements
		metamodelMonitor.buildProject(IncrementalProjectBuilder.FULL_BUILD);
		// 6 new elements: 1 resource + 5 resource methods
		final IJaxrsResource createdResource = (IJaxrsResource) metamodel.findElement(createdType);
		assertThat(createdResource, notNullValue());
		assertThat(createdResource.getAllMethods().size(), equalTo(1));
		final IJaxrsResourceMethod resourceMethod = createdResource.getAllMethods().get(0);
		assertThat(resourceMethod.getName(), equalTo("listAll"));
		assertThat(resourceMethod.getHttpMethodClassName(), equalTo(JaxrsClassnames.GET));
		assertThat(resourceMethod.getPathTemplate(), nullValue());
		assertThat(resourceMethod.getProducedMediaTypes(), hasItems("application/json", "application/xml"));
		assertThat(resourceMethod.getConsumedMediaTypes().size(), equalTo(0));
	}
	
	@Test
	public void shouldCreateResourceClassWithUpdateMethod() throws CoreException, InterruptedException {
		// given
		final JaxrsResourceCreationWizardPage wizardPage = new JaxrsResourceCreationWizardPage();
		final IType customerType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.domain.Customer", javaProject,
				new NullProgressMonitor());
		final IStructuredSelection selection = new StructuredSelection(customerType);
		// when
		wizardPage.init(selection);
		wizardPage.setIncludeCreateMethod(false);
		wizardPage.setIncludeDeleteByIdMethod(false);
		wizardPage.setIncludeFindByIdMethod(false);
		wizardPage.setIncludeListAllMethod(false);
		wizardPage.setIncludeUpdateMethod(true);
		wizardPage.createType(new NullProgressMonitor());
		// then
		final IType createdType = wizardPage.getCreatedType();
		assertThat(createdType, notNullValue());
		assertThat(createdType.getMethods().length, equalTo(1));
		// trigger a clean build before asserting the new JAX-RS elements
		metamodelMonitor.buildProject(IncrementalProjectBuilder.FULL_BUILD);
		// 6 new elements: 1 resource + 5 resource methods
		final IJaxrsResource createdResource = (IJaxrsResource) metamodel.findElement(createdType);
		assertThat(createdResource, notNullValue());
		assertThat(createdResource.getAllMethods().size(), equalTo(1));
		final IJaxrsResourceMethod resourceMethod = createdResource.getAllMethods().get(0);
		assertThat(resourceMethod.getName(), equalTo("update"));
		assertThat(resourceMethod.getHttpMethodClassName(), equalTo(JaxrsClassnames.PUT));
		assertThat(resourceMethod.getPathTemplate(), equalTo("/{id:[0-9][0-9]*}"));
		assertThat(resourceMethod.getConsumedMediaTypes(), hasItems("application/json", "application/xml"));
		assertThat(resourceMethod.getProducedMediaTypes().size(), equalTo(0));
	}

}
