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

package org.jboss.tools.ws.jaxrs.ui.facet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProjectWorkingCopy;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.jboss.tools.ws.jaxrs.core.configuration.ProjectNatureUtils;
import org.jboss.tools.ws.jaxrs.core.junitrules.TestProjectMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author xcoulon
 *
 */
public class JaxrsFacetedProjectListenerTestCase {

	@ClassRule
	public static WorkspaceSetupRule workspaceSetupRule = new WorkspaceSetupRule(
			"org.jboss.tools.ws.jaxrs.tests.sampleproject2");

	@Rule
	public TestProjectMonitor projectMonitor = new TestProjectMonitor("org.jboss.tools.ws.jaxrs.tests.sampleproject2");

	private IProject project = null;

	@Before
	public void setup() {
		this.project = projectMonitor.getProject();
	}

	/**
	 * @param facetedProject
	 * @throws CoreException
	 */
	private void addFacet(final IFacetedProject facetedProject, final String id, final String version) throws CoreException {
		final IFacetedProjectWorkingCopy workingCopy = facetedProject.createWorkingCopy();
		final IProjectFacet javaFacet = ProjectFacetsManager.getProjectFacet(id);
		final IProjectFacetVersion defaultJavaFacet = javaFacet.getVersion(version);
		workingCopy.addProjectFacet(defaultJavaFacet);
		workingCopy.commitChanges(null);
	}
	
	private boolean hasFacet(final IFacetedProject facetedProject, final String id) {
		for(IProjectFacetVersion projectFacet : facetedProject.getProjectFacets()) {
			if(projectFacet.getProjectFacet().getId().equals(id)) {
				return true;
			}
		}
		return false;
	}

	@Test
	public void shouldInstallProjectNatureWhenJAXRSFacet11isInstalled() throws Exception {
		// pre-condition
		final IFacetedProject facetedProject = ProjectFacetsManager.create(project, true, null);
		assertFalse(ProjectNatureUtils.isProjectNatureInstalled(project, ProjectNatureUtils.JAXRS_NATURE_ID));
		assertFalse(hasFacet(facetedProject, "jst.jaxrs"));
		// operation
		addFacet(facetedProject, "jst.jaxrs", "1.1");
		// verification
		assertTrue(ProjectNatureUtils.isProjectNatureInstalled(project, ProjectNatureUtils.JAXRS_NATURE_ID));
	}

	@Test
	public void shouldInstallProjectNatureWhenJAXRSFacet20isInstalled() throws Exception {
		// pre-condition
		final IFacetedProject facetedProject = ProjectFacetsManager.create(project, true, null);
		assertFalse(ProjectNatureUtils.isProjectNatureInstalled(project, ProjectNatureUtils.JAXRS_NATURE_ID));
		assertFalse(hasFacet(facetedProject, "jst.jaxrs"));
		// operation
		addFacet(facetedProject, "jst.jaxrs", "2.0");
		// verification
		assertTrue(ProjectNatureUtils.isProjectNatureInstalled(project, ProjectNatureUtils.JAXRS_NATURE_ID));
	}

}
