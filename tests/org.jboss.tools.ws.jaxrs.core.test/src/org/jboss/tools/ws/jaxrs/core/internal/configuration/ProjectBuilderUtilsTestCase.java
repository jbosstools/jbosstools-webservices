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

import junit.framework.Assert;

import org.jboss.tools.ws.jaxrs.core.AbstractCommonTestCase;
import org.jboss.tools.ws.jaxrs.core.configuration.ProjectBuilderUtils;
import org.junit.Test;

/** @author Xi */
public class ProjectBuilderUtilsTestCase extends AbstractCommonTestCase {

	private static String BUILDER_ID = "org.jboss.tools.ws.jaxrs.core.builder.JaxrsMetamodelBuilder";

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
	public void shouldInstallProjectFacetAndCheckPositionWithValidation() throws Exception {
		// pre-conditions
		ProjectBuilderUtils.installProjectBuilder(javaProject.getProject(), ProjectBuilderUtils.VALIDATION_BUILDER_ID);
		Assert.assertTrue("Wrong result", ProjectBuilderUtils.isProjectBuilderInstalled(javaProject.getProject(),
				ProjectBuilderUtils.VALIDATION_BUILDER_ID));
		ProjectBuilderUtils.uninstallProjectBuilder(javaProject.getProject(), BUILDER_ID);
		Assert.assertFalse("Wrong result",
				ProjectBuilderUtils.isProjectBuilderInstalled(javaProject.getProject(), BUILDER_ID));
		// operation
		ProjectBuilderUtils.installProjectBuilder(javaProject.getProject(), BUILDER_ID);
		// post-conditions
		int customBuilderPosition = ProjectBuilderUtils.getBuilderPosition(project, BUILDER_ID);
		int validationBuilderPosition = ProjectBuilderUtils.getBuilderPosition(project,
				ProjectBuilderUtils.VALIDATION_BUILDER_ID);
		Assert.assertTrue("Wrong ordering:" + customBuilderPosition + " < " + validationBuilderPosition,
				customBuilderPosition == validationBuilderPosition - 1);
	}

	@Test
	public void shouldInstallProjectFacetAndCheckPositionWithoutValidation() throws Exception {
		// pre-conditions
		ProjectBuilderUtils
				.uninstallProjectBuilder(javaProject.getProject(), ProjectBuilderUtils.VALIDATION_BUILDER_ID);
		Assert.assertFalse("Wrong result", ProjectBuilderUtils.isProjectBuilderInstalled(javaProject.getProject(),
				ProjectBuilderUtils.VALIDATION_BUILDER_ID));
		ProjectBuilderUtils.uninstallProjectBuilder(javaProject.getProject(), BUILDER_ID);
		Assert.assertFalse("Wrong result",
				ProjectBuilderUtils.isProjectBuilderInstalled(javaProject.getProject(), BUILDER_ID));
		// operation
		ProjectBuilderUtils.installProjectBuilder(javaProject.getProject(), BUILDER_ID);
		// post-conditions
		int p = ProjectBuilderUtils.getBuilderPosition(project, BUILDER_ID);
		Assert.assertTrue("Wrong index" + p, p != -1);

	}
}
