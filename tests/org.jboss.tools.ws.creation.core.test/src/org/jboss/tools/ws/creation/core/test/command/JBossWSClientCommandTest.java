/*******************************************************************************
 * Copyright (c) 2008 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.ws.creation.core.test.command;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.wst.ws.internal.wsrt.IWebServiceClient;
import org.eclipse.wst.ws.internal.wsrt.WebServiceClientInfo;
import org.eclipse.wst.ws.internal.wsrt.WebServiceScenario;
import org.jboss.tools.ws.core.classpath.JBossWSRuntimeClassPathInitializer.JBossWSRuntimeClasspathContainer;
import org.jboss.tools.ws.creation.core.JBossWSCreationCorePlugin;
import org.jboss.tools.ws.creation.core.commands.ClientSampleCreationCommand;
import org.jboss.tools.ws.creation.core.commands.InitialClientCommand;
import org.jboss.tools.ws.creation.core.commands.RemoveClientJarsCommand;
import org.jboss.tools.ws.creation.core.commands.WSDL2JavaCommand;
import org.jboss.tools.ws.creation.core.test.util.JBossWSCreationCoreTestUtils;
import org.jboss.tools.ws.creation.ui.wsrt.JBossWebServiceClient;

/**
 * @author Grid Qian
 */
@SuppressWarnings("restriction")
public class JBossWSClientCommandTest extends AbstractJBossWSGenerationTest {

	public JBossWSClientCommandTest() {
	}

	public void setUp() throws Exception {
		super.setUp();

		// create jbossws web project
		fproject = createJBossWSProject("JBossWSTestProject");
		wsdlFile = fproject.getProject().getFile(wsdlFileName);
		model = createServiceModel();
		assertTrue(wsdlFile.exists());
	}

	public void testInitialClientCommand() throws CoreException,
			ExecutionException {
		WebServiceClientInfo info = new WebServiceClientInfo();
		info.setWsdlURL(wsdlFile.getLocation().toOSString());
		IWebServiceClient ws = new JBossWebServiceClient(info);

		// test initial command
		InitialClientCommand cmdInitial = new InitialClientCommand(model, ws,
				WebServiceScenario.CLIENT);
		IStatus status = cmdInitial.execute(null, null);
		assertTrue(status.getMessage(), status.isOK());
		assertEquals(wsdlFile.getLocation().toOSString(), model.getWsdlURI());
		assertEquals("", model.getCustomPackage());
	}

	public void testClientCodeGenerationCommand() throws ExecutionException {
		IProject project = fproject.getProject();
		model.setJavaProject(JavaCore.create(project));
		// test wsdl2Javacommand
		model.setJavaSourceFolder("//JBossWSTestProject//src");
		WSDL2JavaCommand cmdW2j = new WSDL2JavaCommand(model);
		IStatus status = cmdW2j.execute(null, null);
		assertFalse(status.getMessage(), Status.ERROR == status.getSeverity());
		assertTrue(project.getFile("src/org/apache/hello_world_soap_http/Greeter.java").exists());

		// test ClientSampleCreationCommand
		ClientSampleCreationCommand cmdImpl = new ClientSampleCreationCommand(model);
		status = cmdImpl.execute(null, null);
		assertTrue(status.getMessage(), status.isOK());
		assertTrue("failed to generate sample class",project.getFile("src/org/apache/hello_world_soap_http/clientsample/ClientSample.java").exists());
	}

	public void testRemoveClientJarsCommand() throws ExecutionException {
		RemoveClientJarsCommand command = new RemoveClientJarsCommand(model);
		model.setJavaProject(JavaCore.create(fproject.getProject()));
		IStatus status = command.execute(null, null);
		assertTrue(status.getMessage(), status.isOK());
		try {
			IClasspathEntry[] entries = JBossWSCreationCoreTestUtils.getJavaProjectByName(fproject.getProject().getName()).getRawClasspath();
			for (IClasspathEntry entry : entries) {
				IClasspathContainer container = JavaCore.getClasspathContainer(entry.getPath(), JBossWSCreationCoreTestUtils.getJavaProjectByName(fproject.getProject().getName()));
				if (container instanceof JBossWSRuntimeClasspathContainer) {
					boolean nojar = true;
					for (IClasspathEntry jar : ((JBossWSRuntimeClasspathContainer) container).getClasspathEntries()) {
						if (jar.getPath().toString().contains("jaxws-rt.jar")) {
							nojar = false;
						}
					}
					assertTrue(nojar);
				}
			}
		} catch (JavaModelException e) {
			JBossWSCreationCorePlugin.getDefault().logError(e);
		}
	}
}
