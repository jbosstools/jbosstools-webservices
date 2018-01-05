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

package org.jboss.tools.ws.jaxws.core.test;

import org.jboss.tools.ws.jaxws.core.test.command.JBossWSClientCommandTest;
import org.jboss.tools.ws.jaxws.core.test.command.JBossWSClientSampleCreationCommandTest;
import org.jboss.tools.ws.jaxws.core.test.command.JBossWSJavaFirstCommandTest;
import org.jboss.tools.ws.jaxws.core.test.command.JBossWSMergeWebXMLCommandTest;
import org.jboss.tools.ws.jaxws.core.test.command.JBossWSTopDownCommandTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	JBossWSTopDownCommandTest.class,
	JBossWSJavaFirstCommandTest.class,
	JBossWSClientCommandTest.class,
	JBossWSMergeWebXMLCommandTest.class,
	JBossWSClientSampleCreationCommandTest.class
})
public class JBossJAXWSCoreTestSuite {
}
