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

import java.io.File;

import junit.framework.TestCase;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.JavaCore;
import org.jboss.tools.test.util.JobUtils;
import org.jboss.tools.test.util.ResourcesUtils;
import org.jboss.tools.test.util.TestProjectProvider;
import org.jboss.tools.ws.jaxws.core.data.ServiceModel;
import org.jboss.tools.ws.jaxws.ui.commands.MergeWebXMLCommand;
import org.jboss.tools.ws.jaxws.ui.utils.JBossWSCreationUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JBossWSMergeWebXMLCommandTest extends TestCase {

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
	public void testMergeWebXMLCommand() throws ExecutionException{
		File file = JBossWSCreationUtils.findFileByPath("web.xml", prj.getLocation().toOSString());
		assertTrue("For now, no web.xml",file == null);
		ServiceModel model = new ServiceModel();
		model.setUpdateWebxml(true);
		model.setWebProjectName("WebTest");
		model.setJavaProject(JavaCore.create(prj));
		
		MergeWebXMLCommand command = new MergeWebXMLCommand(model);
		command.execute(null, null);
		file = JBossWSCreationUtils.findFileByPath("web.xml", prj.getLocation().toOSString());
		assertTrue("For now, web.xml should be there",file != null);
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
