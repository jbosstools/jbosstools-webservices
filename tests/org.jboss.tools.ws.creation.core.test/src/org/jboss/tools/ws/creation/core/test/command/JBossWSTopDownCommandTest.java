/*******************************************************************************
 * Copyright (c) 2012 - 2014 Red Hat, Inc. and others.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/ 
package org.jboss.tools.ws.creation.core.test.command;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;

import javax.wsdl.Definition;
import javax.wsdl.Service;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.j2ee.model.IModelProvider;
import org.eclipse.jst.j2ee.model.ModelProviderManager;
import org.eclipse.jst.j2ee.webapplication.ServletType;
import org.eclipse.jst.javaee.web.Servlet;
import org.eclipse.jst.javaee.web.ServletMapping;
import org.eclipse.jst.javaee.web.WebApp;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.ws.internal.wsrt.IWebService;
import org.eclipse.wst.ws.internal.wsrt.WebServiceInfo;
import org.eclipse.wst.ws.internal.wsrt.WebServiceScenario;
import org.jboss.tools.test.util.JobUtils;
import org.jboss.tools.ws.creation.core.commands.ImplementationClassCreationCommand;
import org.jboss.tools.ws.creation.core.commands.InitialCommand;
import org.jboss.tools.ws.creation.core.commands.MergeWebXMLCommand;
import org.jboss.tools.ws.creation.core.commands.WSDL2JavaCommand;
import org.jboss.tools.ws.creation.ui.wsrt.JBossWebService;

@SuppressWarnings("restriction")
public class JBossWSTopDownCommandTest extends AbstractJBossWSGenerationTest {

	public JBossWSTopDownCommandTest() {
	}
	
	public void setUp() throws Exception {
		super.setUp();

		//create jbossws web project
		fproject = createJBossWSProject("JBossWSTestProject");
		wsdlFile = fproject.getProject().getFile(wsdlFileName);
		model = createServiceModel();
		assertTrue(wsdlFile.exists());
	}
	
	public void testDeployResult() throws ExecutionException, CoreException, IOException{
		doInitialCommand();		
		doCodeGenerationCommand();
		doMergeWebXMLCommand();
		
		fproject.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
		fproject.getProject().build(IncrementalProjectBuilder.FULL_BUILD, null);
		
		publishWebProject(fproject.getProject());
		JobUtils.delay(10000);
		startup(currentServer);
		JobUtils.delay(10000);
		
		assertTrue(currentServer.getModules().length > 0);
		String webServiceUrl = "http://127.0.0.1:8080/JBossWSTestProject/Greeter?wsdl";
		URL url = new URL(webServiceUrl);
		URLConnection conn =  url.openConnection();
		assertEquals("unable to start JBoss server",IServer.STATE_STARTED, currentServer.getServerState());
		conn.connect();
		assertFalse("The url connection's status is "+ ((HttpURLConnection) conn).getResponseMessage(), "Ok".equals(((HttpURLConnection) conn).getResponseMessage()));
		conn.getContent();
	}

	public void doInitialCommand() throws CoreException, ExecutionException{		
		WebServiceInfo info = new WebServiceInfo();
		info.setWsdlURL(wsdlFile.getLocationURI().toString());
		IWebService ws = new JBossWebService(info); 
		
		//test initial command
		InitialCommand cmdInitial = new InitialCommand(model, ws, WebServiceScenario.TOPDOWN);
		IStatus status = cmdInitial.execute(null, null);
		assertNotNull(model.getWsdlDefinition());
		Definition def = model.getWsdlDefinition();
		Iterator<?> iter = def.getServices().values().iterator();
		model.setService((Service) iter.next());
		assertTrue(status.getMessage(), status.isOK());
		assertEquals(wsdlFile.getLocationURI().toString(), model.getWsdlURI());
		assertEquals("", model.getCustomPackage());		
	}
	
	
	public void doCodeGenerationCommand() throws ExecutionException{
		IProject project = fproject.getProject();
		
		model.setJavaSourceFolder("//JBossWSTestProject//src");
		
		//test wsdl2Javacommand
		WSDL2JavaCommand cmdW2j = new WSDL2JavaCommand(model);
		IStatus status = cmdW2j.execute(null, null);
		assertTrue(status.getMessage(), status.getSeverity() != Status.ERROR);	
		assertTrue(project.getFile("src/org/apache/hello_world_soap_http/Greeter.java").exists());
		
		// test ImplementationClassCreationCommand
		model.setGenerateImplementatoin(false);
		ImplementationClassCreationCommand cmdImpl = new ImplementationClassCreationCommand(model);
		status = cmdImpl.execute(null, null);
		assertTrue(status.getMessage(), status.getSeverity() != Status.ERROR);
		assertFalse(project.getFile("src/org/apache/hello_world_soap_http.impl/GreeterImpl.java").exists());
		
		model.setGenerateImplementatoin(true);
		cmdImpl = new ImplementationClassCreationCommand(model);
		status = cmdImpl.execute(null, null);
		assertTrue(status.getMessage(), status.getSeverity() != Status.ERROR);
		assertTrue("failed to generate implemenatation class", project.getFile("src/org/apache/hello_world_soap_http/impl/GreeterImpl.java").exists());		
	}
	
	public void doMergeWebXMLCommand() throws ExecutionException{
		model.setGenerateImplementatoin(true);
		model.addServiceClasses("org.apache.hello_world_soap_http.impl.GreeterImpl");
		
		MergeWebXMLCommand cmdweb = new MergeWebXMLCommand(model);
		IStatus  status = cmdweb.execute(null, null);
		assertTrue(status.getMessage(), status.isOK());
		
		IProject project = fproject.getProject();
		IFile webxml = project.getFile("WebContent/WEB-INF/web.xml");
		assertTrue(webxml.exists());
		IModelProvider provider = ModelProviderManager.getModelProvider(project);
		Object object = provider.getModelObject();
		if (object instanceof WebApp) {
			WebApp webApp = (WebApp) object;
			assertTrue("failed to update web.xml ", webApp.getServlets().size() > 0);
			Servlet servlet = (Servlet)webApp.getServlets().get(0);
			assertEquals("the servlet with the name 'Greeter' was not created", servlet.getServletName(), "Greeter");
			assertEquals("org.apache.hello_world_soap_http.impl.GreeterImpl", servlet.getServletClass());
			
			ServletMapping mapping = (ServletMapping)webApp.getServletMappings().get(0);
			assertEquals("Greeter", mapping.getServletName());
		}else if (object instanceof org.eclipse.jst.j2ee.webapplication.WebApp) { 
			org.eclipse.jst.j2ee.webapplication.WebApp webApp = (org.eclipse.jst.j2ee.webapplication.WebApp) object;
			assertTrue("failed to update web.xml ", webApp.getServlets().size() > 0);
			org.eclipse.jst.j2ee.webapplication.Servlet servlet = (org.eclipse.jst.j2ee.webapplication.Servlet)webApp.getServlets().get(0);
			assertEquals("a servlet with the name 'Greeger' should be created", servlet.getServletName(), "Greeter");
			assertEquals("servlet display name:","Greeter", servlet.getDisplayName());
			if(servlet.getWebType() instanceof ServletType){
				ServletType webtype = (ServletType)servlet.getWebType();
				assertEquals("org.apache.hello_world_soap_http.impl.GreeterImpl", webtype.getClassName());
			}			
			org.eclipse.jst.j2ee.webapplication.ServletMapping mapping = (org.eclipse.jst.j2ee.webapplication.ServletMapping)webApp.getServletMappings().get(0);
			assertEquals("url pattern: ","/Greeter", mapping.getUrlPattern());
			assertEquals("Greeter", mapping.getServlet().getServletName());
		}	
	}
	
	public void tearDown() throws Exception{
		undeployWebProject();
		super.tearDown();
	}
}
