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
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.jboss.tools.ws.jaxrs.core.AbstractCommonTestCase;
import org.jboss.tools.ws.jaxrs.core.JBossJaxrsCorePlugin;
import org.jboss.tools.ws.jaxrs.core.WorkbenchTasks;
import org.jboss.tools.ws.jaxrs.core.WorkbenchUtils;
import org.jboss.tools.ws.jaxrs.core.configuration.ProjectNatureUtils;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.metamodel.JaxrsMetamodelLocator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Xavier Coulon
 * 
 */
public class JaxrsMetamodelBuilderTestCase extends AbstractCommonTestCase {

	@BeforeClass
	public static void registerListeners() {
		JBossJaxrsCorePlugin.getDefault().registerListeners();
	}

	@Before
	public void installNature() throws CoreException {
		ProjectNatureUtils.installProjectNature(project, ProjectNatureUtils.JAXRS_NATURE_ID);
	}

	@Test
	public void shouldFullBuildJaxrsProjectWithExistingMetamodel() throws CoreException, OperationCanceledException,
			InterruptedException {
		// pre-conditions
		if (JaxrsMetamodelLocator.get(javaProject) == null) {
			JaxrsMetamodel.create(javaProject);
		}
		assertThat(JaxrsMetamodelLocator.get(javaProject), notNullValue());
		WorkbenchTasks.buildProject(project, IncrementalProjectBuilder.CLEAN_BUILD, new NullProgressMonitor());
		JaxrsMetamodelLocator.get(javaProject);
		WorkbenchTasks.buildProject(project, IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
		JaxrsMetamodelLocator.get(javaProject);
		// operation: rebuilt the project, including the jaxrs metamodel
		WorkbenchTasks.buildProject(project, IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
		JaxrsMetamodelLocator.get(javaProject);
		WorkbenchTasks.buildProject(project, IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
		JaxrsMetamodelLocator.get(javaProject);
		// verification
		final IJaxrsMetamodel metamodel = JaxrsMetamodelLocator.get(javaProject);
		assertThat(metamodel, notNullValue());
		assertThat(metamodel.getAllEndpoints().size(), equalTo(20));
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
		WorkbenchTasks.buildProject(project, IncrementalProjectBuilder.CLEAN_BUILD, new NullProgressMonitor());
		WorkbenchTasks.buildProject(project, IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
		// verification
		final IJaxrsMetamodel metamodel = JaxrsMetamodelLocator.get(javaProject);
		assertThat(metamodel, notNullValue());
		assertThat(metamodel.getAllEndpoints().size(), equalTo(20));
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
		WorkbenchTasks.buildProject(project, IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
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
		WorkbenchTasks.buildProject(project, IncrementalProjectBuilder.CLEAN_BUILD, new NullProgressMonitor());
		final IJaxrsMetamodel metamodel = JaxrsMetamodelLocator.get(javaProject);
		assertThat(metamodel, nullValue());
	}
	
	@Test
	public void shouldIncrementalBuildJaxrsProjectAfterResourceCreationWithExistingMetamodel() throws CoreException,
			OperationCanceledException, InterruptedException {
		// pre-conditions: trigger an initial build to have a delta later (when another build is triggered after the resource creation)
		WorkbenchTasks.buildProject(project, IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
		project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
		assertThat(JaxrsMetamodelLocator.get(javaProject), notNullValue());
		// operation
		WorkbenchUtils.createCompilationUnit(javaProject, "FooResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "FooResource.java");
		// verification
		final IJaxrsMetamodel metamodel = JaxrsMetamodelLocator.get(javaProject);
		assertThat(metamodel, notNullValue());
		// 13 usual endpoints + some newly created
		assertThat(metamodel.getAllEndpoints().size(), equalTo(22));
	}

	@Test
	public void shouldIncrementalBuildJaxrsProjectAfterResourceCreationWithoutExistingMetamodel() throws CoreException,
			OperationCanceledException, InterruptedException {
		// pre-conditions
		if (JaxrsMetamodelLocator.get(javaProject) != null) {
			JaxrsMetamodelLocator.get(javaProject).remove();
		}
		assertThat(JaxrsMetamodelLocator.get(javaProject), nullValue());
		// operation
		WorkbenchUtils.createCompilationUnit(javaProject, "FooResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "FooResource.java");
		// verification
		final IJaxrsMetamodel metamodel = JaxrsMetamodelLocator.get(javaProject);
		assertThat(metamodel, notNullValue());
		// 13 usual endpoints + some newly created
		assertThat(metamodel.getAllEndpoints().size(), equalTo(22));
	}
	
	@Test
	public void shouldDoNothingWhenPackageInfoAdded() throws CoreException, IOException {
		// pre-conditions
		IFolder folder = javaProject.getProject().getFolder("src/main/java/org/jboss/tools/ws/jaxrs/sample/services");
		// operation
		WorkbenchUtils.createFileFromStream(folder, "package-info.java",
						IOUtils.toInputStream("package org.jboss.tools.ws.jaxrs.sample.services;"));
		// explicitly trigger the project build
		javaProject.getProject().build(IncrementalProjectBuilder.AUTO_BUILD, null);
		// verifications: no exception should have been thrown
	
	}

	@Test
	public void shouldDoNothingWhenPackageInfoChanged() throws CoreException, IOException {
		// pre-conditions
		JBossJaxrsCorePlugin.getDefault().registerListeners();
		IFile pkgInfoFile = javaProject.getProject().getFile("src/main/java/org/jboss/tools/ws/jaxrs/sample/services/package-info.java");
		pkgInfoFile.create(IOUtils.toInputStream(""), true, new NullProgressMonitor());
		// operation
		WorkbenchUtils.replaceContent(pkgInfoFile, IOUtils.toInputStream("package org.jboss.tools.ws.jaxrs.sample;"), true);
		WorkbenchUtils.replaceContent(pkgInfoFile, IOUtils.toInputStream("package org.jboss.tools.ws.jaxrs.sample.services;"), true);
		// explicitly trigger the project build
		//javaProject.getProject().build(IncrementalProjectBuilder.AUTO_BUILD, null);
		// verifications: no exception should have been thrown
	
	}
	
	
}
