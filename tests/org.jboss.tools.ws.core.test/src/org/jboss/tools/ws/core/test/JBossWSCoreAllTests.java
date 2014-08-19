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

package org.jboss.tools.ws.core.test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.jboss.tools.ws.core.test.classpath.JBossWSRuntimeManagerTest;
import org.jboss.tools.ws.core.test.project.facet.JBossWSProjectFacetTest;

public class JBossWSCoreAllTests extends TestCase {
	public static final String PLUGIN_ID = "org.jboss.tools.ws.creation.core.test";
	public static Test suite ()
	{
		TestSuite suite = new TestSuite(JBossWSCoreAllTests.class.getName());
		suite.addTestSuite(JBossWSRuntimeManagerTest.class);
		suite.addTestSuite(JBossWSProjectFacetTest.class);
		return suite;
	}
}