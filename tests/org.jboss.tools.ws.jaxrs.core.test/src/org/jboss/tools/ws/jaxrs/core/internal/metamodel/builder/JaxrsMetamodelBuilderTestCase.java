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
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils.createFileFromStream;
import static org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils.replaceContent;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IJavaProject;
import org.jboss.tools.test.util.JobUtils;
import org.jboss.tools.ws.jaxrs.core.JBossJaxrsCorePlugin;
import org.jboss.tools.ws.jaxrs.core.configuration.ProjectNatureUtils;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.TestBanner;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsMetamodelLocator;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Xavier Coulon
 * 
 */
public class JaxrsMetamodelBuilderTestCase {

	@ClassRule
	public static WorkspaceSetupRule workspaceSetupRule = new WorkspaceSetupRule("org.jboss.tools.ws.jaxrs.tests.sampleproject");
	
	@Rule
	public JaxrsMetamodelMonitor metamodelMonitor = new JaxrsMetamodelMonitor("org.jboss.tools.ws.jaxrs.tests.sampleproject", true);
	
	@Rule
	public TestBanner watcher = new TestBanner();
	
	private JaxrsMetamodel metamodel = null;

	private IJavaProject javaProject = null;

	private IProject project = null;
	
	@Before
	public void setup() throws CoreException {
		metamodel = metamodelMonitor.getMetamodel();
		javaProject = metamodel.getJavaProject();
		project = metamodel.getProject();
		JobUtils.waitForIdle();
	}

	@After
	public void reopenJavaProject() throws CoreException {
		if(project !=null && !project.isOpen()) {
			project.open(new NullProgressMonitor());
		}
		if(javaProject !=null && !javaProject.isOpen()) {
			javaProject.open(new NullProgressMonitor());
		}
		
	}
	
	@Test
	public void shouldFullBuildJaxrsProjectWithExistingMetamodel() throws CoreException, OperationCanceledException,
			InterruptedException {
		// pre-conditions
		assertThat(JaxrsMetamodelLocator.get(javaProject), notNullValue());
		// operation: rebuilt the project, including the jaxrs metamodel
		metamodelMonitor.buildProject(new NullProgressMonitor(), IncrementalProjectBuilder.CLEAN_BUILD);
		metamodelMonitor.buildProject(new NullProgressMonitor(), IncrementalProjectBuilder.FULL_BUILD);
		// verification
		final IJaxrsMetamodel metamodel = JaxrsMetamodelLocator.get(javaProject);
		assertThat(metamodel, notNullValue());
		assertThat(metamodel.getAllEndpoints().size(), equalTo(22));
	}

	@Test
	public void shouldFullBuildJaxrsProjectWithoutExistingMetamodel() throws CoreException, OperationCanceledException,
			InterruptedException {
		// pre-conditions
		if (JaxrsMetamodelLocator.get(javaProject) != null) {
			JaxrsMetamodelLocator.get(javaProject).remove();
		}
		assertThat(JaxrsMetamodelLocator.get(javaProject), nullValue());
		// operation
		metamodelMonitor.buildProject(new NullProgressMonitor(), IncrementalProjectBuilder.CLEAN_BUILD);
		metamodelMonitor.buildProject(new NullProgressMonitor(), IncrementalProjectBuilder.FULL_BUILD);
		// verification
		final IJaxrsMetamodel metamodel = JaxrsMetamodelLocator.get(javaProject);
		assertThat(metamodel, notNullValue());
		assertThat(metamodel.getAllEndpoints().size(), equalTo(22));
	}

	@Test
	public void shouldNotBuildMetamodelWhenProjectIsClosed() throws CoreException, OperationCanceledException,
	InterruptedException {
		// pre-conditions
		if (JaxrsMetamodelLocator.get(javaProject) != null) {
			JaxrsMetamodelLocator.get(javaProject).remove();
		}
		project.close(new NullProgressMonitor());
		assertThat(JaxrsMetamodelLocator.get(javaProject), nullValue());
		// operation
		metamodelMonitor.buildProject(new NullProgressMonitor(), IncrementalProjectBuilder.CLEAN_BUILD);
		metamodelMonitor.buildProject(new NullProgressMonitor(), IncrementalProjectBuilder.FULL_BUILD);
		// verification
		final IJaxrsMetamodel metamodel = JaxrsMetamodelLocator.get(javaProject);
		assertThat(metamodel, nullValue());
	}
	
	@Test
	public void shouldNotFullBuildNonJaxrsProject() throws CoreException, OperationCanceledException,
			InterruptedException {
		// pre-conditions
		if (JaxrsMetamodelLocator.get(javaProject) != null) {
			JaxrsMetamodelLocator.get(javaProject).remove();
		}
		assertThat(JaxrsMetamodelLocator.get(javaProject), nullValue());
		ProjectNatureUtils.uninstallProjectNature(project, ProjectNatureUtils.JAXRS_NATURE_ID);
		assertFalse(ProjectNatureUtils.isProjectNatureInstalled(project, ProjectNatureUtils.JAXRS_NATURE_ID));
		// operation
		metamodelMonitor.buildProject(new NullProgressMonitor(), IncrementalProjectBuilder.FULL_BUILD);
		// verification
		final IJaxrsMetamodel metamodel = JaxrsMetamodelLocator.get(javaProject);
		assertThat(metamodel, nullValue());
	}
	
	@Test
	public void shouldNotCleanBuildNonJaxrsProject() throws CoreException, OperationCanceledException,
			InterruptedException {
		// pre-conditions
		if (JaxrsMetamodelLocator.get(javaProject) != null) {
			JaxrsMetamodelLocator.get(javaProject).remove();
		}
		assertThat(JaxrsMetamodelLocator.get(javaProject), nullValue());
		ProjectNatureUtils.uninstallProjectNature(project, ProjectNatureUtils.JAXRS_NATURE_ID);
		assertFalse(ProjectNatureUtils.isProjectNatureInstalled(project, ProjectNatureUtils.JAXRS_NATURE_ID));
		// operation
		metamodelMonitor.buildProject(new NullProgressMonitor(), IncrementalProjectBuilder.CLEAN_BUILD);
		final IJaxrsMetamodel metamodel = JaxrsMetamodelLocator.get(javaProject);
		assertThat(metamodel, nullValue());
	}
	
	@Test
	public void shouldIncrementalBuildJaxrsProjectAfterResourceCreationWithExistingMetamodel() throws CoreException,
			OperationCanceledException, InterruptedException {
		// pre-conditions: trigger an initial build to have a delta later (when another build is triggered after the resource creation)
		metamodelMonitor.buildProject(new NullProgressMonitor(), IncrementalProjectBuilder.FULL_BUILD);
		assertThat(JaxrsMetamodelLocator.get(javaProject), notNullValue());
		// operation
		metamodelMonitor.createCompilationUnit("FooResource.txt", "org.jboss.tools.ws.jaxrs.sample.services",
				"FooResource.java");
		// verification
		final IJaxrsMetamodel metamodel = JaxrsMetamodelLocator.get(javaProject);
		assertThat(metamodel, notNullValue());
		// 13 usual endpoints + some newly created
		assertThat(metamodel.getAllEndpoints().size(), equalTo(24));
	}

	@Test
	public void shouldIncrementalBuildJaxrsProjectAfterResourceCreationWithoutExistingMetamodel() throws CoreException,
			OperationCanceledException, InterruptedException {
		// pre-conditions
		if (metamodel != null) {
			metamodel.remove();
		}
		assertThat(JaxrsMetamodelLocator.get(javaProject), nullValue());
		// operation
		metamodelMonitor.createCompilationUnit("FooResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "FooResource.java");
		// verification
		metamodel = JaxrsMetamodelLocator.get(javaProject);
		assertThat(metamodel, notNullValue());
		// 13 usual endpoints + some newly created
		assertThat(metamodel.getAllEndpoints().size(), equalTo(24));
	}
	
	@Test
	public void shouldDoNothingWhenPackageInfoAdded() throws CoreException, IOException {
		// pre-conditions
		IFolder folder = javaProject.getProject().getFolder("src/main/java/org/jboss/tools/ws/jaxrs/sample/services");
		// operation
		createFileFromStream(folder, "package-info.java",
						IOUtils.toInputStream("package org.jboss.tools.ws.jaxrs.sample.services;"));
		// explicitly trigger the project build
		javaProject.getProject().build(IncrementalProjectBuilder.AUTO_BUILD, null);
		// verifications: no exception should have been thrown
	
	}

	@Test
	public void shouldDoNothingWhenPackageInfoChanged() throws CoreException, IOException {
		// pre-conditions
		JBossJaxrsCorePlugin.getDefault().resumeListeners();
		IFile pkgInfoFile = javaProject.getProject().getFile("src/main/java/org/jboss/tools/ws/jaxrs/sample/services/package-info.java");
		pkgInfoFile.create(IOUtils.toInputStream(""), true, new NullProgressMonitor());
		// operation
		replaceContent(pkgInfoFile, IOUtils.toInputStream("package org.jboss.tools.ws.jaxrs.sample;"), true);
		replaceContent(pkgInfoFile, IOUtils.toInputStream("package org.jboss.tools.ws.jaxrs.sample.services;"), true);
		// explicitly trigger the project build
		//javaProject.getProject().build(IncrementalProjectBuilder.AUTO_BUILD, null);
		// verifications: no exception should have been thrown
	}

	@Test
	public void shouldNotFailBuildingJaxrsProjectWhenMissingLibraries() throws CoreException, OperationCanceledException,
			InterruptedException {
		// pre-conditions
		if (JaxrsMetamodelLocator.get(javaProject) != null) {
			JaxrsMetamodelLocator.get(javaProject).remove();
		}
		metamodelMonitor.removeClasspathEntry("jaxrs-api-2.0.1.GA.jar");
		// operation: built the project, including the jaxrs metamodel
		metamodelMonitor.buildProject(new NullProgressMonitor(), IncrementalProjectBuilder.FULL_BUILD);
		// verification
		final JaxrsMetamodel metamodel = JaxrsMetamodelLocator.get(javaProject);
		assertThat(metamodel, notNullValue());
		assertThat(metamodel.getBuildStatus().isOK(), equalTo(true));
		assertThat(metamodel.getAllEndpoints().size(), equalTo(0));
	}

	@Test
	public void shouldNotFailRebuildingJaxrsProjectWhenMissingLibraries() throws CoreException, OperationCanceledException,
	InterruptedException {
		// pre-conditions
		metamodelMonitor.removeClasspathEntry("jaxrs-api-2.0.1.GA.jar");
		// operation: call the JAX-RS builer for the project
		metamodelMonitor.buildProject(new NullProgressMonitor(),IncrementalProjectBuilder.CLEAN_BUILD);
		// verification
		final JaxrsMetamodel metamodel = JaxrsMetamodelLocator.get(javaProject);
		assertThat(metamodel, notNullValue());
		assertThat(metamodel.getBuildStatus().isOK(), equalTo(true));
		assertThat(metamodel.getAllEndpoints().size(), equalTo(0));
	}
}
