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

package org.jboss.tools.ws.jaxrs.core.internal.configuration;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsMetamodelBuilder.BUILDER_ID;
import static org.junit.Assert.assertThat;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.jboss.tools.ws.jaxrs.core.configuration.ProjectBuilderUtils;
import org.jboss.tools.ws.jaxrs.core.junitrules.TestProjectMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

public class ProjectBuilderUtilsTestCase {
	
	@ClassRule
	public static WorkspaceSetupRule workspaceSetupRule = new WorkspaceSetupRule("org.jboss.tools.ws.jaxrs.tests.sampleproject");
	
	@Rule
	public TestProjectMonitor projectMonitor = new TestProjectMonitor("org.jboss.tools.ws.jaxrs.tests.sampleproject");

	
	/**
	 * Returns an array containing the name of the {@link ICommand} configured for this project
	 * 
	 * @param project
	 *            the project
	 * @param builderId
	 *            the builder ID
	 * @return the index or -1 if not found
	 * @throws CoreException
	 *             in case of underlying exception
	 */
	private static String[] getCommandNames(final IProject project) throws CoreException {
		IProjectDescription desc = project.getDescription();
		ICommand[] commands = desc.getBuildSpec();
		final String[] names = new String[commands.length];
		for (int i = 0; i < commands.length; i++) {
			names[i]  = commands[i].getBuilderName();
		}
		// not found
		return names;
	}
	
	@Test
	public void shouldVerifyProjectBuilderIsNotInstalled() throws Exception {
		Assert.assertFalse("Wrong result",
				ProjectBuilderUtils.isProjectBuilderInstalled(projectMonitor.getProject(), BUILDER_ID));
	}

	@Test
	public void shouldVerifyProjectBuilderIsInstalled() throws Exception {
		Assert.assertTrue("Wrong result", ProjectBuilderUtils.isProjectBuilderInstalled(projectMonitor.getProject(),
				"org.eclipse.jdt.core.javabuilder"));
	}

	@Test
	public void shouldInstallAndUninstallProjectBuilderWithValidationBuilderMissing() throws Exception {
		// pre-conditions
		Assert.assertFalse("Wrong result",
				ProjectBuilderUtils.isProjectBuilderInstalled(projectMonitor.getProject(), ProjectBuilderUtils.VALIDATOR_BUILDER_ID));
		ProjectBuilderUtils.uninstallProjectBuilder(projectMonitor.getProject(), BUILDER_ID);
		Assert.assertFalse("Wrong result",
				ProjectBuilderUtils.isProjectBuilderInstalled(projectMonitor.getProject(), BUILDER_ID));
		Assert.assertTrue("Wrong result",
				ProjectBuilderUtils.installProjectBuilder(projectMonitor.getProject(), BUILDER_ID));
		Assert.assertFalse("Wrong result",
				ProjectBuilderUtils.installProjectBuilder(projectMonitor.getProject(), BUILDER_ID));
		Assert.assertTrue("Wrong result",
				ProjectBuilderUtils.isProjectBuilderInstalled(projectMonitor.getProject(), BUILDER_ID));
		Assert.assertTrue("Wrong result",
				ProjectBuilderUtils.uninstallProjectBuilder(projectMonitor.getProject(), BUILDER_ID));
		Assert.assertFalse("Wrong result",
				ProjectBuilderUtils.uninstallProjectBuilder(projectMonitor.getProject(), BUILDER_ID));
		Assert.assertFalse("Wrong result",
				ProjectBuilderUtils.isProjectBuilderInstalled(projectMonitor.getProject(), BUILDER_ID));
	}

	@Test
	public void shouldInstallAndUninstallProjectBuilderWithValidationBuilderAlreadyInstalled() throws Exception {
		// pre-conditions
		projectMonitor.replaceDotProjectFileWith("dotProject.txt");
		Assert.assertTrue("Wrong result",
				ProjectBuilderUtils.isProjectBuilderInstalled(projectMonitor.getProject(), ProjectBuilderUtils.VALIDATOR_BUILDER_ID));
		ProjectBuilderUtils.uninstallProjectBuilder(projectMonitor.getProject(), BUILDER_ID);
		Assert.assertFalse("Wrong result",
				ProjectBuilderUtils.isProjectBuilderInstalled(projectMonitor.getProject(), BUILDER_ID));
		Assert.assertTrue("Wrong result",
				ProjectBuilderUtils.installProjectBuilder(projectMonitor.getProject(), BUILDER_ID));
		Assert.assertFalse("Wrong result",
				ProjectBuilderUtils.installProjectBuilder(projectMonitor.getProject(), BUILDER_ID));
		Assert.assertTrue("Wrong result",
				ProjectBuilderUtils.isProjectBuilderInstalled(projectMonitor.getProject(), BUILDER_ID));
		Assert.assertTrue("Wrong result",
				ProjectBuilderUtils.isProjectBuilderInstalled(projectMonitor.getProject(), ProjectBuilderUtils.VALIDATOR_BUILDER_ID));
		Assert.assertTrue("Wrong result",
				ProjectBuilderUtils.uninstallProjectBuilder(projectMonitor.getProject(), BUILDER_ID));
		Assert.assertFalse("Wrong result",
				ProjectBuilderUtils.uninstallProjectBuilder(projectMonitor.getProject(), BUILDER_ID));
		Assert.assertFalse("Wrong result",
				ProjectBuilderUtils.isProjectBuilderInstalled(projectMonitor.getProject(), BUILDER_ID));
	}

	@Test
	public void shouldInstallProjectFacetAndCheckPositionIsBeforeValidator() throws Exception {
		// pre-conditions
		ProjectBuilderUtils.uninstallProjectBuilder(projectMonitor.getProject(), BUILDER_ID);
		Assert.assertFalse("Wrong result",
				ProjectBuilderUtils.isProjectBuilderInstalled(projectMonitor.getProject(), BUILDER_ID));
		// pre-conditions: activating the validation builder
		projectMonitor.replaceDotProjectFileWith("dotProject.txt");
		assertThat(getCommandNames(projectMonitor.getProject()).length, equalTo(3));
		// operation
		ProjectBuilderUtils.installProjectBuilder(projectMonitor.getProject(), BUILDER_ID);
		// post-conditions
		final int p = ProjectBuilderUtils.getBuilderPosition(projectMonitor.getProject(), BUILDER_ID);
		assertThat(p, equalTo(2));
		final String[] names = getCommandNames(projectMonitor.getProject());
		assertThat(names.length, equalTo(4));
		for(int i = 0; i < names.length; i++) {
			assertThat(names[i], notNullValue());
		}
	}

	@Test
	public void shouldInstallProjectFacetAndCheckPositionIsLast() throws Exception {
		// pre-conditions
		ProjectBuilderUtils.uninstallProjectBuilder(projectMonitor.getProject(), BUILDER_ID);
		Assert.assertFalse("Wrong result",
				ProjectBuilderUtils.isProjectBuilderInstalled(projectMonitor.getProject(), BUILDER_ID));
		// operation
		ProjectBuilderUtils.installProjectBuilder(projectMonitor.getProject(), BUILDER_ID);
		// post-conditions
		int p = ProjectBuilderUtils.getBuilderPosition(projectMonitor.getProject(), BUILDER_ID);
		assertThat(p, equalTo(2));
		final String[] names = getCommandNames(projectMonitor.getProject());
		assertThat(names.length, equalTo(4));
		for(int i = 0; i < names.length; i++) {
			assertThat(names[i], notNullValue());
		}
	}
}
