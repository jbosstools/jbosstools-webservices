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

package org.jboss.tools.ws.creation.core.test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.jboss.tools.ws.creation.core.test.command.JBossWSClientCommandTest;
import org.jboss.tools.ws.creation.core.test.command.JBossWSClientSampleCreationCommandTest;
import org.jboss.tools.ws.creation.core.test.command.JBossWSJavaFirstCommandTest;
import org.jboss.tools.ws.creation.core.test.command.JBossWSMergeWebXMLCommandTest;
import org.jboss.tools.ws.creation.core.test.command.JBossWSTopDownCommandTest;

public class JBossWSCreationCoreTestSuite extends TestCase {
	public static final String PLUGIN_ID = "org.jboss.tools.ws.creation.core.test";
	public static Test suite ()
	{
		TestSuite suite = new TestSuite(JBossWSCreationCoreTestSuite.class.getName());
		suite.addTestSuite(JBossWSTopDownCommandTest.class);
		suite.addTestSuite(JBossWSJavaFirstCommandTest.class);
		suite.addTestSuite(JBossWSClientCommandTest.class);
		suite.addTestSuite(JBossWSMergeWebXMLCommandTest.class);
		suite.addTestSuite(JBossWSClientSampleCreationCommandTest.class);

		return suite;
	}
}