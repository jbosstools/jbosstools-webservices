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
import static org.junit.Assert.assertThat;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.jboss.tools.ws.jaxrs.core.AbstractCommonTestCase;
import org.jboss.tools.ws.jaxrs.core.WorkbenchUtils;
import org.jboss.tools.ws.jaxrs.core.configuration.ProjectBuilderUtils;
import org.junit.Assert;
import org.junit.Test;

/** @author Xi */
public class ProjectBuilderUtilsTestCase extends AbstractCommonTestCase {

	private static String BUILDER_ID = "org.jboss.tools.ws.jaxrs.core.builder.JaxrsMetamodelBuilder";

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
				ProjectBuilderUtils.isProjectBuilderInstalled(javaProject.getProject(), BUILDER_ID));
	}

	@Test
	public void shouldVerifyProjectBuilderIsInstalled() throws Exception {
		Assert.assertTrue("Wrong result", ProjectBuilderUtils.isProjectBuilderInstalled(javaProject.getProject(),
				"org.eclipse.jdt.core.javabuilder"));
	}

	@Test
	public void shouldInstallAndUninstallProjectBuilder() throws Exception {
		Assert.assertFalse("Wrong result",
				ProjectBuilderUtils.isProjectBuilderInstalled(javaProject.getProject(), BUILDER_ID));
		Assert.assertTrue("Wrong result",
				ProjectBuilderUtils.installProjectBuilder(javaProject.getProject(), BUILDER_ID));
		Assert.assertFalse("Wrong result",
				ProjectBuilderUtils.installProjectBuilder(javaProject.getProject(), BUILDER_ID));
		Assert.assertTrue("Wrong result",
				ProjectBuilderUtils.isProjectBuilderInstalled(javaProject.getProject(), BUILDER_ID));
		Assert.assertTrue("Wrong result",
				ProjectBuilderUtils.uninstallProjectBuilder(javaProject.getProject(), BUILDER_ID));
		Assert.assertFalse("Wrong result",
				ProjectBuilderUtils.uninstallProjectBuilder(javaProject.getProject(), BUILDER_ID));
		Assert.assertFalse("Wrong result",
				ProjectBuilderUtils.isProjectBuilderInstalled(javaProject.getProject(), BUILDER_ID));
	}

	@Test
	public void shouldInstallProjectFacetAndCheckPositionIsBeforeValidator() throws Exception {
		// pre-conditions
		ProjectBuilderUtils.uninstallProjectBuilder(javaProject.getProject(), BUILDER_ID);
		Assert.assertFalse("Wrong result",
				ProjectBuilderUtils.isProjectBuilderInstalled(javaProject.getProject(), BUILDER_ID));
		final IResource dotProjectFile = project.findMember(".project");
		// pre-conditions: activating the validation builder
		WorkbenchUtils
				.replaceContent(
						dotProjectFile,
						"<!-- buildCommand><name>org.eclipse.wst.validation.validationbuilder</name><arguments></arguments></buildCommand -->",
						"<buildCommand><name>org.eclipse.wst.validation.validationbuilder</name><arguments></arguments></buildCommand>");
		assertThat(getCommandNames(project).length, equalTo(3));
		// operation
		ProjectBuilderUtils.installProjectBuilder(javaProject.getProject(), BUILDER_ID);
		// post-conditions
		final int p = ProjectBuilderUtils.getBuilderPosition(project, BUILDER_ID);
		assertThat(p, equalTo(2));
		final String[] names = getCommandNames(project);
		assertThat(names.length, equalTo(4));
		for(int i = 0; i < names.length; i++) {
			assertThat(names[i], notNullValue());
		}
	}

	@Test
	public void shouldInstallProjectFacetAndCheckPositionIsLast() throws Exception {
		// pre-conditions
		ProjectBuilderUtils.uninstallProjectBuilder(javaProject.getProject(), BUILDER_ID);
		Assert.assertFalse("Wrong result",
				ProjectBuilderUtils.isProjectBuilderInstalled(javaProject.getProject(), BUILDER_ID));
		// operation
		ProjectBuilderUtils.installProjectBuilder(javaProject.getProject(), BUILDER_ID);
		// post-conditions
		int p = ProjectBuilderUtils.getBuilderPosition(project, BUILDER_ID);
		assertThat(p, equalTo(2));
		final String[] names = getCommandNames(project);
		assertThat(names.length, equalTo(3));
		for(int i = 0; i < names.length; i++) {
			assertThat(names[i], notNullValue());
		}
	}
}
