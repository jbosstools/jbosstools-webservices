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
package org.jboss.tools.ws.jaxws.core.test.command;

import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.jboss.tools.test.util.JobUtils;
import org.jboss.tools.test.util.ResourcesUtils;
import org.jboss.tools.test.util.TestProjectProvider;
import org.jboss.tools.ws.jaxws.core.data.ServiceModel;
import org.jboss.tools.ws.jaxws.ui.utils.JBossWSCreationUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import junit.framework.TestCase;

public class JBossWSClientSampleCreationCommandTest extends TestCase {
	static String BUNDLE = "org.jboss.tools.ws.jaxws.core.test";
	IProject prj;

	@Before
	public void setUp() throws Exception {
		TestProjectProvider provider = new TestProjectProvider(BUNDLE,
				"/projects/" + "WebTest", "WebTest", true);
		prj = provider.getProject();
		JobUtils.delay(3000);
	}

	@Test
	public void testJBIDE6175() throws ExecutionException{
		IResource src = prj.findMember("src");
		assertTrue("src is there",src.exists());
		ServiceModel model = new ServiceModel();
		model.setCustomPackage("");
		model.setWebProjectName("WebTest");
		List<ICompilationUnit> list = JBossWSCreationUtils.findJavaUnitsByAnnotation(JavaCore.create(prj), "WebService", "");	
		assertTrue("No java files in src!",list.isEmpty());
	}

	@After
	public void tearDown() throws Exception {
		boolean oldAutoBuilding = ResourcesUtils.setBuildAutomatically(false);
		Exception last = null;
		try {
			JobUtils.delay(500);
			try {
				prj.delete(true, null);
				JobUtils.delay(500);
			} catch (Exception e) {
				e.printStackTrace();
				last = e;
			}
		} finally {
			ResourcesUtils.setBuildAutomatically(oldAutoBuilding);
		}
		if (last != null)
			throw last;
	}
}
