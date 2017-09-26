/******************************************************************************* 
 * Copyright (c) 2008 - 2014 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.ws.core.test.project.facet;

import junit.framework.TestCase;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

public class JBossWSProjectFacetTest extends TestCase {
	protected static final IWorkspace ws = ResourcesPlugin.getWorkspace();
	private static final IProjectFacet wsFacet;
	private static final IProjectFacetVersion wsVersion;
	private IFacetedProject wsProj;
	static {
		wsFacet = ProjectFacetsManager.getProjectFacet("jbossws.core");
		wsVersion = wsFacet.getVersion("3.0");
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		wsProj = createFacetedProject("wsFacetTestProject");
	}

	public void testWSFacet() throws CoreException{
		wsProj.installProjectFacet(JavaFacet.VERSION_1_8, null, null);
		wsProj.installProjectFacet(ProjectFacetsManager.getProjectFacet("jst.web").getVersion("3.1"), null, null);
		wsProj.installProjectFacet(wsVersion, null, null);
		assertTrue(wsProj.hasProjectFacet(wsFacet));
		wsProj.uninstallProjectFacet(wsVersion, null, null);
		assertFalse(wsProj.hasProjectFacet(wsVersion));
	}
	
	protected void tearDown() throws Exception {
		wsProj.getProject().delete(true, null);
		super.tearDown();
	}

	protected IFacetedProject createFacetedProject(String name)
			throws CoreException

	{
		assertFalse(ws.getRoot().getProject(name).exists());
		final IFacetedProject fpj = ProjectFacetsManager.create(name, null,
				null);
		final IProject pj = fpj.getProject();
		assertTrue(pj.exists());
		return fpj;
	}
}
