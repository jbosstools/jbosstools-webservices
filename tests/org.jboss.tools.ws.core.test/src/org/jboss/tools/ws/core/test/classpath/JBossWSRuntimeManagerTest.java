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

package org.jboss.tools.ws.core.test.classpath;

import org.jboss.tools.ws.core.classpath.JBossWSRuntime;
import org.jboss.tools.ws.core.classpath.JBossWSRuntimeManager;

import junit.framework.TestCase;

/**
 * @author Grid Qian
 */
public class JBossWSRuntimeManagerTest extends TestCase {

	JBossWSRuntimeManager manager;

	protected void setUp() throws Exception {
		super.setUp();
		manager = JBossWSRuntimeManager.getInstance();
		assertNotNull("Cannot obtainJBossWSRuntimeManager instance", manager);
		if (manager.findRuntimeByName("JBossWS Runtime 4.2") != null)
			return;
		manager.addRuntime("JBossWS Runtime 4.2", "runtimelocation", "4.2",
				true);
	}

	public void testGetRuntimes() {
		JBossWSRuntime[] rtms = manager.getRuntimes();
		assertTrue("JBossWS runtime 'JBossWS Runtime 4.2' is not created",
				rtms.length == 1);
		assertTrue("JBossWS runtime 'JBossWS Runtime 4.2' is not created",
				rtms[0].getName().equals("JBossWS Runtime 4.2"));
	}

	public void testFindRuntimeByName() {
		JBossWSRuntime srt = manager.findRuntimeByName("JBossWS Runtime 4.2");
		assertNotNull("Cannot find runtime 'JBossWS Runtime 4.2'", srt);
	}

	public void testGetDefaultRuntime() {
		assertNotNull("Cannot obtain default runtime 'JBossWS Runtime 4.2'",
				manager.getDefaultRuntime());
	}
}
