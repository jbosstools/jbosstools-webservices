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

import org.eclipse.jface.viewers.StructuredSelection;
import org.jboss.tools.ws.jaxrs.core.configuration.ProjectNatureUtils;
import org.jboss.tools.ws.jaxrs.core.junitrules.TestProjectMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.jboss.tools.ws.jaxrs.ui.configuration.AddNatureAction;
import org.jboss.tools.ws.jaxrs.ui.configuration.RemoveNatureAction;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Xi
 * 
 */
public class ProjectNatureUtilsTestCase {

	@ClassRule
	public static WorkspaceSetupRule workspaceSetupRule = new WorkspaceSetupRule("org.jboss.tools.ws.jaxrs.tests.sampleproject");
	
	@Rule
	public TestProjectMonitor sampleProject = new TestProjectMonitor("org.jboss.tools.ws.jaxrs.tests.sampleproject");

	@Test
	public void shouldVerifyProjectNatureIsNotInstalled() throws Exception {
		Assert.assertFalse("Wrong result", ProjectNatureUtils.isProjectNatureInstalled(sampleProject.getProject(), ProjectNatureUtils.JAXRS_NATURE_ID));
	}
	
	@Test
	public void shouldVerifyProjectNatureIsInstalled() throws Exception {
		Assert.assertTrue("Wrong result", ProjectNatureUtils.isProjectNatureInstalled(sampleProject.getProject(), "org.eclipse.jdt.core.javanature"));
	}
	
	@Test
	public void shouldInstallAndUninstallProjectNature() throws Exception {
		Assert.assertFalse("Wrong result", ProjectNatureUtils.isProjectNatureInstalled(sampleProject.getProject(), ProjectNatureUtils.JAXRS_NATURE_ID));
		Assert.assertTrue("Wrong result", ProjectNatureUtils.installProjectNature(sampleProject.getProject(), ProjectNatureUtils.JAXRS_NATURE_ID));
		Assert.assertFalse("Wrong result", ProjectNatureUtils.installProjectNature(sampleProject.getProject(), ProjectNatureUtils.JAXRS_NATURE_ID));
		Assert.assertTrue("Wrong result", ProjectNatureUtils.isProjectNatureInstalled(sampleProject.getProject(), ProjectNatureUtils.JAXRS_NATURE_ID));
		Assert.assertTrue("Wrong result", ProjectNatureUtils.uninstallProjectNature(sampleProject.getProject(), ProjectNatureUtils.JAXRS_NATURE_ID));
		Assert.assertFalse("Wrong result", ProjectNatureUtils.uninstallProjectNature(sampleProject.getProject(), ProjectNatureUtils.JAXRS_NATURE_ID));
		Assert.assertFalse("Wrong result", ProjectNatureUtils.isProjectNatureInstalled(sampleProject.getProject(), ProjectNatureUtils.JAXRS_NATURE_ID));
	}
	
	@Test
	public void shouldInstallAndUninstallProjectNatureFromActions() throws Exception {
		// pre-condition
		final StructuredSelection selection = new StructuredSelection(sampleProject.getProject());
		final AddNatureAction addNatureAction = new AddNatureAction();
		addNatureAction.selectionChanged(null, selection);
		final RemoveNatureAction removeNatureAction = new RemoveNatureAction();
		removeNatureAction.selectionChanged(null, selection);
		Assert.assertFalse("Wrong result", ProjectNatureUtils.isProjectNatureInstalled(sampleProject.getProject(), ProjectNatureUtils.JAXRS_NATURE_ID));
		// operations and verifications
		addNatureAction.run(null);
		Assert.assertTrue("Wrong result", ProjectNatureUtils.isProjectNatureInstalled(sampleProject.getProject(), ProjectNatureUtils.JAXRS_NATURE_ID));
		removeNatureAction.run(null);
		Assert.assertFalse("Wrong result", ProjectNatureUtils.isProjectNatureInstalled(sampleProject.getProject(), ProjectNatureUtils.JAXRS_NATURE_ID));
	}

}
