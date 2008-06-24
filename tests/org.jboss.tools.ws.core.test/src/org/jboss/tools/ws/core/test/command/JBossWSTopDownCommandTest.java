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
package org.jboss.tools.ws.core.test.command;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.jst.j2ee.model.IModelProvider;
import org.eclipse.jst.j2ee.model.ModelProviderManager;
import org.eclipse.jst.j2ee.webapplication.ServletType;
import org.eclipse.jst.javaee.web.Servlet;
import org.eclipse.jst.javaee.web.ServletMapping;
import org.eclipse.jst.javaee.web.WebApp;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.common.frameworks.datamodel.AbstractDataModelOperation;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.common.project.facet.core.util.internal.StatusWrapper;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeType;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerListener;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.ServerEvent;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.IServer.IOperationListener;
import org.eclipse.wst.server.core.internal.ServerWorkingCopy;
import org.eclipse.wst.ws.internal.wsrt.IWebService;
import org.eclipse.wst.ws.internal.wsrt.WebServiceInfo;
import org.eclipse.wst.ws.internal.wsrt.WebServiceScenario;
import org.jboss.ide.eclipse.as.core.server.internal.DeployableServer;
import org.jboss.ide.eclipse.as.core.server.internal.JBossServerBehavior;
import org.jboss.tools.common.test.util.TestProjectProvider;
import org.jboss.tools.test.util.JUnitUtils;
import org.jboss.tools.test.util.xpl.EditorTestHelper;
import org.jboss.tools.ws.core.classpath.JbossWSRuntime;
import org.jboss.tools.ws.core.classpath.JbossWSRuntimeManager;
import org.jboss.tools.ws.core.facet.delegate.IJBossWSFacetDataModelProperties;
import org.jboss.tools.ws.core.facet.delegate.JBossWSFacetInstallDataModelProvider;
import org.jboss.tools.ws.creation.core.commands.ImplementationClassCreationCommand;
import org.jboss.tools.ws.creation.core.commands.InitialCommand;
import org.jboss.tools.ws.creation.core.commands.MergeWebXMLCommand;
import org.jboss.tools.ws.creation.core.commands.WSDL2JavaCommand;
import org.jboss.tools.ws.creation.core.data.ServiceModel;
import org.jboss.tools.ws.creation.ui.wsrt.JBossWebService;

public class JBossWSTopDownCommandTest extends TestCase {
	protected static final IWorkspace ws = ResourcesPlugin.getWorkspace();
	protected static final IWorkbench wb = PlatformUI.getWorkbench();
	
	protected static final int DEFAULT_STARTUP_TIME = 150000;
	protected static final int DEFAULT_SHUTDOWN_TIME = 90000;

	protected static final String JBOSSWS_HOME_DEFAULT = "/home/fugang/jboss-all/jboss-4.2.2.GA";
	public static final String JBOSS_RUNTIME_42 = "org.jboss.ide.eclipse.as.runtime.42";
	public static final String JBOSS_AS_42_HOME = System.getProperty(JBOSS_RUNTIME_42, JBOSSWS_HOME_DEFAULT);
	public static final String JBOSS_SERVER_42 = "org.jboss.ide.eclipse.as.42";
	
	protected final Set<IResource> resourcesToCleanup = new HashSet<IResource>();

	protected static final IProjectFacetVersion dynamicWebVersion;
	protected static final IProjectFacetVersion javaVersion;
	protected static final IProjectFacetVersion jbosswsFacetVersion;
	private static final IProjectFacet jbosswsFacet;
	private static final String RuntimeName;
	private static final boolean isDeployed;

	static String wsdlFileName = "hello_world.wsdl";
	static String BUNDLE = "org.jboss.tools.ws.core.test";
	
	IFacetedProject fproject;
	TestProjectProvider provider;
	private IRuntime currentRuntime;
	private IServer currentServer;
	
	static {
		javaVersion = ProjectFacetsManager.getProjectFacet("jst.java").getVersion("5.0");
		dynamicWebVersion = ProjectFacetsManager.getProjectFacet("jst.web").getVersion("2.5");
		jbosswsFacet = ProjectFacetsManager.getProjectFacet("jbossws.core");
		jbosswsFacetVersion = jbosswsFacet.getVersion("2.0.1");
		RuntimeName = "testjbosswsruntime";
		isDeployed = false;
		
		
	}

	public JBossWSTopDownCommandTest() {
	}
	
	protected void setUp() throws Exception {
		super.setUp();
		JbossWSRuntimeManager.getInstance().addRuntime(RuntimeName, getJBossWSHomeFolder().toString(), "", true);

		//create jbossws web project
		fproject = createJBossWSProject("JBossWSTestProject", isServerSupplied());
		IFile wsdlFile = fproject.getProject().getFile(wsdlFileName);
		
		
		createServer(JBOSS_RUNTIME_42, JBOSS_SERVER_42, JBOSS_AS_42_HOME, "default");
		
		assertTrue(wsdlFile.exists());
		try { EditorTestHelper.joinBackgroundActivities(); } 
		catch (Exception e) { JUnitUtils.fail(e.getMessage(), e); }
		EditorTestHelper.runEventQueue(3000);
	}
	
	private IProject createProject(String prjName) throws CoreException {
		provider = new TestProjectProvider(BUNDLE,"/projects/"+prjName , prjName, true);
		IProject prj = provider.getProject();
		EditorTestHelper.joinBackgroundActivities();
		return prj;
	}
	
	

	protected void tearDown() throws Exception {
		// Wait until all jobs is finished to avoid delete project problems
		EditorTestHelper.joinBackgroundActivities();
		EditorTestHelper.runEventQueue(3000);
		Exception last = null;
		for (IResource r : this.resourcesToCleanup) {
			try {
				System.out.println("Deleting " + r);
				r.delete(true, null);
			} catch(Exception e) {
				System.out.println("Error deleting " + r);
				e.printStackTrace();
				last = e;
			}
		}

		if(last!=null) throw last;

		resourcesToCleanup.clear();
		JbossWSRuntime runtime = JbossWSRuntimeManager.getInstance().findRuntimeByName(RuntimeName);
		JbossWSRuntimeManager.getInstance().removeRuntime(runtime);
		
		//cleanProjectFromServer() ;
		undeployWebProject();
		currentServer.stop(true);
		currentServer.delete();
		
		super.tearDown();
	}

	
	public void testInitialCommand() throws CoreException, ExecutionException{
		
		IFile wsdlFile = fproject.getProject().getFile(wsdlFileName);
		ServiceModel model = new ServiceModel();
		model.setWebProjectName(fproject.getProject().getName());
		//model.setWsdlURI(wsdlFile.getLocation().toOSString());

		
		WebServiceInfo info = new WebServiceInfo();
		info.setWsdlURL(wsdlFile.getLocation().toOSString());
		IWebService ws = new JBossWebService(info); 
		
		//test initial command
		InitialCommand cmdInitial = new InitialCommand(model, ws, WebServiceScenario.TOPDOWN);
		IStatus status = cmdInitial.execute(null, null);
		assertTrue(status.getMessage(), status.isOK());
		
		assertTrue(model.getServiceNames().contains("SOAPService"));
		assertEquals(wsdlFile.getLocation().toOSString(), model.getWsdlURI());
		assertTrue(model.getPortTypes().contains("Greeter"));
		assertEquals("org.apache.hello_world_soap_http", model.getCustomPackage());
		
	}
	
	
	public void testCodeGenerationCommand() throws ExecutionException{

		ServiceModel model = createServiceModel();
		IProject project = fproject.getProject();
		//test wsdl2Javacommand
		WSDL2JavaCommand cmdW2j = new WSDL2JavaCommand(model);
		IStatus status = cmdW2j.execute(null, null);
		assertTrue(status.getMessage(), status.isOK());
		
		assertTrue(project.getFile("src/org/apache/hello_world_soap_http/Greeter.java").exists());
		
		// test ImplementationClassCreationCommand
		model.setGenerateImplementatoin(false);
		ImplementationClassCreationCommand cmdImpl = new ImplementationClassCreationCommand(model);
		status = cmdImpl.execute(null, null);
		assertTrue(status.getMessage(), status.isOK());
		assertFalse(project.getFile("src/org/apache/hello_world_soap_http/GreeterImpl.java").exists());
		
		model.setGenerateImplementatoin(true);
		cmdImpl = new ImplementationClassCreationCommand(model);
		status = cmdImpl.execute(null, null);
		assertTrue(status.getMessage(), status.isOK());
		assertTrue("failed to generate implemenatation class", project.getFile("src/org/apache/hello_world_soap_http/GreeterImpl.java").exists());
		
		
	}
	
	public void testMergeWebXMLCommand() throws ExecutionException{
		ServiceModel model = createServiceModel();
		model.setGenerateImplementatoin(true);
		model.setUpdateWebxml(true);
		model.setWebProjectName(fproject.getProject().getName());
		model.addServiceClasses("org.apache.hello_world_soap_http.GreeterImpl");
		MergeWebXMLCommand cmdweb = new MergeWebXMLCommand(model);
		IStatus  status = cmdweb.execute(null, null);
		assertTrue(status.getMessage(), status.isOK());
		
		IProject project = fproject.getProject();
		IFile webxml = project.getFile("WebContent/WEB-INF/web.xml");
		assertTrue(webxml.exists());
		IModelProvider provider = ModelProviderManager
		.getModelProvider(project);
		Object object = provider.getModelObject();
		if (object instanceof WebApp) {
			WebApp webApp = (WebApp) object;
			assertTrue("failed to update web.xml ", webApp.getServlets().size() > 0);
			Servlet servlet = (Servlet)webApp.getServlets().get(0);
			assertEquals("the servlet with the name 'Greeger' was not created", servlet.getServletName(), "Greeter");
			assertTrue("the servlet display names should contain 'Greeter'", servlet.getDisplayNames().contains("Greeter"));
			assertEquals("org.apache.hello_world_soap_http.GreeterImpl", servlet.getServletClass());
			
			ServletMapping mapping = (ServletMapping)webApp.getServletMappings().get(0);
			assertTrue("url patterns should contain '/Greeter'", mapping.getUrlPatterns().contains("/Greeter"));
			assertEquals("Greeter", mapping.getServletName());
		}else if(object instanceof org.eclipse.jst.j2ee.webapplication.WebApp){
			org.eclipse.jst.j2ee.webapplication.WebApp webApp = (org.eclipse.jst.j2ee.webapplication.WebApp) object;
			assertTrue("failed to update web.xml ", webApp.getServlets().size() > 0);
			org.eclipse.jst.j2ee.webapplication.Servlet servlet = (org.eclipse.jst.j2ee.webapplication.Servlet)webApp.getServlets().get(0);
			assertEquals("a servlet with the name 'Greeger' should be created", servlet.getServletName(), "Greeter");
			assertEquals("servlet display name:","Greeter", servlet.getDisplayName());
			if(servlet.getWebType() instanceof ServletType){
				ServletType webtype = (ServletType)servlet.getWebType();
				assertEquals("org.apache.hello_world_soap_http.GreeterImpl", webtype.getClassName());
			}
			
			org.eclipse.jst.j2ee.webapplication.ServletMapping mapping = (org.eclipse.jst.j2ee.webapplication.ServletMapping)webApp.getServletMappings().get(0);
			assertEquals("url pattern: ","/Greeter", mapping.getUrlPattern());
			assertEquals("Greeter", mapping.getServlet().getServletName());
		}
		//ServerType d; d.createServer(id, file, monitor)
		
	}
	
	public void testDeployResult() throws ExecutionException, CoreException, IOException{
		
		//currentServer.start(ILaunchManager.RUN_MODE, new NullProgressMonitor());
		IFile wsdlFile = fproject.getProject().getFile(wsdlFileName);
		ServiceModel model = new ServiceModel();
		model.setWebProjectName(fproject.getProject().getName());
		//model.setWsdlURI(wsdlFile.getLocation().toOSString());

		
		WebServiceInfo info = new WebServiceInfo();
		info.setWsdlURL(wsdlFile.getLocation().toOSString());
		IWebService ws = new JBossWebService(info); 
		
		//test initial command
		AbstractDataModelOperation cmd = new InitialCommand(model, ws, WebServiceScenario.TOPDOWN);
		IStatus status = cmd.execute(null, null);
		assertTrue(status.getMessage(), status.isOK());
		
		cmd = new WSDL2JavaCommand(model);
		status = cmd.execute(null, null);
		assertTrue(status.getMessage(), status.isOK());
		
		cmd = new ImplementationClassCreationCommand(model);
		status = cmd.execute(null, null);
		assertTrue(status.getMessage(), status.isOK());
		
		cmd = new MergeWebXMLCommand(model);
		status = cmd.execute(null, null);
		assertTrue(status.getMessage(), status.isOK());
		
		fproject.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
		fproject.getProject().build(IncrementalProjectBuilder.FULL_BUILD, null);
		publishWebProject();
		
		assertTrue(currentServer.getModules().length > 0);
		String webServiceUrl = "http://localhost:8080/JBossWSTestProject/Greeter?wsdl";
		URL url = new URL(webServiceUrl);
		URLConnection conn =  url.openConnection();
		
		startup();
		
		assertEquals("unable to start JBoss server",IServer.STATE_STARTED, currentServer.getServerState());

		conn.connect();
		conn.getContent();
		
		
		
	}
	
	
	protected void createServer(String runtimeID, String serverID,
			String location, String configuration) throws CoreException {
		// if file doesnt exist, abort immediately.
		assertTrue(new Path(location).toFile().exists());

		currentRuntime = createRuntime(runtimeID, location, configuration);
		IServerType serverType = ServerCore.findServerType(serverID);
		IServerWorkingCopy serverWC = serverType.createServer(null, null,
				new NullProgressMonitor());
		serverWC.setRuntime(currentRuntime);
		serverWC.setName(serverID);
		serverWC.setServerConfiguration(null);
		IPath path = new Path(location).append("server").append("default").append("deploy");
		((ServerWorkingCopy)serverWC).setAttribute(DeployableServer.DEPLOY_DIRECTORY, path.toOSString() );
		currentServer = serverWC.save(true, new NullProgressMonitor());
		
	}

	private IRuntime createRuntime(String runtimeId, String homeDir,
			String config) throws CoreException {
		IRuntimeType[] runtimeTypes = ServerUtil.getRuntimeTypes(null, null,
				runtimeId);
		assertEquals("expects only one runtime type", runtimeTypes.length, 1);
		IRuntimeType runtimeType = runtimeTypes[0];
		IRuntimeWorkingCopy runtimeWC = runtimeType.createRuntime(null,
				new NullProgressMonitor());
		runtimeWC.setName(runtimeId);
		runtimeWC.setLocation(new Path(homeDir));
		IRuntime savedRuntime = runtimeWC.save(true, new NullProgressMonitor());
		return savedRuntime;
	}
	
	protected ServiceModel createServiceModel(){
		ServiceModel model = new ServiceModel();
		model.setWebProjectName(fproject.getProject().getName());
		IFile wsdlFile = fproject.getProject().getFile(wsdlFileName);
		model.setWsdlURI(wsdlFile.getLocation().toOSString());
		model.addServiceName("SOAPService");
		model.addPortTypes("Greeter");
		model.setCustomPackage("org.apache.hello_world_soap_http");
		
		return model;
		
	}
	
	protected void publishWebProject() throws CoreException{
		IModule[] modules = ServerUtil.getModules(currentServer.getServerType().getRuntimeType().getModuleTypes());
		IServerWorkingCopy serverWC = currentServer.createWorkingCopy();
		serverWC.modifyModules(modules, null, null);
		serverWC.save(true, null).publish(0, null);
		currentServer.publish(IServer.PUBLISH_FULL, null);
		
	}
	
	protected void undeployWebProject() throws CoreException{
		IModule[] modules = ServerUtil.getModules(currentServer.getServerType().getRuntimeType().getModuleTypes());
		IServerWorkingCopy serverWC = currentServer.createWorkingCopy();
		serverWC.modifyModules( null,modules, null);
		serverWC.save(true, null).publish(0, null);
		currentServer.publish(IServer.PUBLISH_FULL, null);
		
	}
	
	protected void cleanProjectFromServer() throws CoreException{
		IModule[] modules = ServerUtil.getModules(currentServer.getServerType().getRuntimeType().getModuleTypes());
		IServerWorkingCopy serverWC = currentServer.createWorkingCopy();
		serverWC.modifyModules(null, modules, null);
		currentServer.publish(0, null);
		currentServer.stop(true);
		
	}
	
	protected boolean isServerSupplied(){
		return false;
	}
	
	
	protected IFacetedProject createJBossWSProject(String baseProjectName, boolean isServerSupplied) throws CoreException {
		IProject project = createProject("JBossWSTestProject");
		final IFacetedProject fproj = ProjectFacetsManager.create(project);
	
		//installDependentFacets(fproj);
		fproj.installProjectFacet(jbosswsFacetVersion, createJBossWSDataModel(isServerSupplied), null);
		
		assertNotNull(project);
		assertTrue(project.exists());
		this.addResourceToCleanup(project);	
		
		
		return fproj;
	}
	
	protected void installDependentFacets(final IFacetedProject fproj) throws CoreException {
		fproj.installProjectFacet(javaVersion, null, null);
		fproj.installProjectFacet(dynamicWebVersion, null, null);
		//fproj.installProjectFacet(jbosswsFacetVersion, null, null);
	}
	

	
	
	protected IDataModel createJBossWSDataModel( boolean isServerSupplied) {
		IDataModel config = (IDataModel) new JBossWSFacetInstallDataModelProvider().create();
		if(isServerSupplied){
			config.setBooleanProperty(IJBossWSFacetDataModelProperties.JBOSS_WS_RUNTIME_IS_SERVER_SUPPLIED, true);
		}else{
			config.setBooleanProperty(IJBossWSFacetDataModelProperties.JBOSS_WS_DEPLOY, isDeployed);
			config.setStringProperty(IJBossWSFacetDataModelProperties.JBOSS_WS_RUNTIME_ID, RuntimeName);
			config.setStringProperty(IJBossWSFacetDataModelProperties.JBOSS_WS_RUNTIME_HOME, getJBossWSHomeFolder().toString());
		}
		return config;
	}

	
	
	protected final void addResourceToCleanup(final IResource resource) {
		this.resourcesToCleanup.add(resource);
	}



	
	protected File getJBossWSHomeFolder() {
		
		String jbosshome = System.getProperty(JBOSS_RUNTIME_42, JBOSSWS_HOME_DEFAULT);
		File runtimelocation = new File(jbosshome);
		assertTrue("Please set Jboss EAP Home in system property:" + JBOSS_RUNTIME_42, runtimelocation.exists());
		
		String cmdFileLocation = jbosshome + File.separator + "bin" + File.separator + "wsconsume.sh";
		assertTrue(jbosshome + " is not a valid jboss EAP home", new File(cmdFileLocation).exists());
		return runtimelocation;
	}



	 
	protected class ServerStateListener implements IServerListener {
		private ArrayList stateChanges;
		public ServerStateListener() {
			this.stateChanges = new ArrayList();
		}
		public ArrayList getStateChanges() {
			return stateChanges;
		}
		public void serverChanged(ServerEvent event) {
			if((event.getKind() & ServerEvent.SERVER_CHANGE) != 0)  {
				if((event.getKind() & ServerEvent.STATE_CHANGE) != 0) {
					if( event.getState() != IServer.STATE_STOPPED)
						stateChanges.add(new Integer(event.getState()));
				}
			}
		}
	}
	
	protected class ErrorStreamListener implements IStreamListener {
		protected boolean errorFound = false;
		String entireLog = "";
		public void streamAppended(String text, IStreamMonitor monitor) {
			entireLog += text;
		} 
		
		// will need to be fixed or decided how to figure out errors
		public boolean hasError() {
			return errorFound;
		}
	}

	protected void startup() { startup(DEFAULT_STARTUP_TIME); }
	protected void startup(int maxWait) {
		long finishTime = new Date().getTime() + maxWait;
		
		// operation listener, which is only alerted when the startup is *done*
		final StatusWrapper opWrapper = new StatusWrapper();
		final IOperationListener listener = new IOperationListener() {
			public void done(IStatus result) {
				opWrapper.setStatus(result);
			} };
			
			
		// a stream listener to listen for errors
		ErrorStreamListener streamListener = new ErrorStreamListener();
		
		// the thread to actually start the server
		Thread startThread = new Thread() { 
			public void run() {
				currentServer.start(ILaunchManager.RUN_MODE, listener);
			}
		};
		
		startThread.start();
		
		boolean addedStream = false;
		while( finishTime > new Date().getTime() && opWrapper.getStatus() == null) {
			// we're waiting for startup to finish
			if( !addedStream ) {
				IStreamMonitor mon = getStreamMonitor();
				if( mon != null ) {
					mon.addListener(streamListener);
					addedStream = true;
				}
			}
			try {
				Display.getDefault().readAndDispatch();
			} catch( SWTException swte ) {}
		}
		
		try {
			assertTrue("Startup has taken longer than what is expected for a default startup", finishTime >= new Date().getTime());
			assertNotNull("Startup never finished", opWrapper.getStatus());
			assertFalse("Startup had System.error output", streamListener.hasError());
		} catch( AssertionFailedError afe ) {
			// cleanup
			currentServer.stop(true);
			// rethrow
			throw afe;
		}
		getStreamMonitor().removeListener(streamListener);
	}
 
		
	protected IStreamMonitor getStreamMonitor() {
		JBossServerBehavior behavior = 
			(JBossServerBehavior)currentServer.loadAdapter(JBossServerBehavior.class, null);
		if( behavior != null ) {
			if( behavior.getProcess() != null ) {
				return behavior.getProcess().getStreamsProxy().getOutputStreamMonitor();
			}
		}
		return null;
	}
	
	public class StatusWrapper {
		protected IStatus status;
		public IStatus getStatus() { return this.status; }
		public void setStatus(IStatus s) { this.status = s; }
	}

}
