/******************************************************************************* 
 * Copyright (c) 2008 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/

package org.jboss.tools.ws.core.test.classpath;

import org.jboss.tools.ws.core.classpath.JbossWSRuntime;
import org.jboss.tools.ws.core.classpath.JbossWSRuntimeManager;

import junit.framework.TestCase;

/**
 * @author Grid Qian
 */
public class JbossWSRuntimeManagerTest extends TestCase {

	JbossWSRuntimeManager manager;

	protected void setUp() throws Exception {
		super.setUp();
		manager = JbossWSRuntimeManager.getInstance();
		assertNotNull("Cannot obtainJbossWSRuntimeManager instance", manager);
		if (manager.findRuntimeByName("JBossWS Runtime 4.2") != null)
			return;
		manager.addRuntime("JBossWS Runtime 4.2", "runtimelocation", "4.2",
				true);
	}

	public void testGetRuntimes() {
		JbossWSRuntime[] rtms = manager.getRuntimes();
		assertTrue("JbossWS runtime 'JBossWS Runtime 4.2' is not created",
				rtms.length == 1);
		assertTrue("JbossWS runtime 'JBossWS Runtime 4.2' is not created",
				rtms[0].getName().equals("JBossWS Runtime 4.2"));
	}

	public void testFindRuntimeByName() {
		JbossWSRuntime srt = manager.findRuntimeByName("JBossWS Runtime 4.2");
		assertNotNull("Cannot find runtime 'JBossWS Runtime 4.2'", srt);
	}

	public void testGetDefaultRuntime() {
		assertNotNull("Cannot obtain default runtime 'JBossWS Runtime 4.2'",
				manager.getDefaultRuntime());
	}
}
