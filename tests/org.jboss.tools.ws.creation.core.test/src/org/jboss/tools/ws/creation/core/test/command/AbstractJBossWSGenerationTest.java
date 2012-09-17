package org.jboss.tools.ws.creation.core.test.command;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.model.ServerDelegate;
import org.jboss.ide.eclipse.as.core.server.IDeployableServer;
import org.jboss.ide.eclipse.as.core.util.IJBossToolingConstants;
import org.jboss.ide.eclipse.as.test.ASTest;
import org.jboss.ide.eclipse.as.test.util.ServerRuntimeUtils;
import org.jboss.ide.eclipse.as.test.util.jdt.JREUtils;
import org.jboss.tools.test.util.JobUtils;
import org.jboss.tools.test.util.ResourcesUtils;
import org.jboss.tools.test.util.TestProjectProvider;
import org.jboss.tools.ws.core.classpath.JBossWSRuntime;
import org.jboss.tools.ws.core.classpath.JBossWSRuntimeManager;
import org.jboss.tools.ws.core.facet.delegate.IJBossWSFacetDataModelProperties;
import org.jboss.tools.ws.core.facet.delegate.JBossWSFacetInstallDataModelProvider;
import org.jboss.tools.ws.creation.core.data.ServiceModel;

public class AbstractJBossWSGenerationTest extends ServerRuntimeUtils {
	protected IServer currentServer;
	protected final Set<IResource> resourcesToCleanup = new HashSet<IResource>();
	static String BUNDLE = "org.jboss.tools.ws.creation.core.test";
	private String RuntimeName = "testjbosswsruntime";
	public String wsdlFileName = "hello_world.wsdl";
	public ServiceModel model;
	private String JBOSS_AS_42_HOME = ASTest.JBOSS_AS_42_HOME;
	private String JBOSS_WS_HOME = JBOSS_AS_42_HOME;
	public String wsHomePath;
	IFacetedProject fproject;
	public IFile wsdlFile;
	
	public void setUp() throws Exception{
		super.setUp();
		assertNotNull(ASTest.JRE_5_HOME, "No JRE5 property in System");
		assertTrue("The JRE5 location is not right", new Path(ASTest.JRE_5_HOME).toFile().exists());
		createWSServer();
		wsHomePath = getJBossWSHomeFolder().toString();
		JBossWSRuntimeManager.getInstance().addRuntime(RuntimeName,wsHomePath, "", true);
	}
	
	public void createWSServer() throws Exception {
		currentServer = createServer(IJBossToolingConstants.AS_42, IJBossToolingConstants.SERVER_AS_42, ASTest.JBOSS_AS_42_HOME, DEFAULT_CONFIG,JREUtils.createJRE());		
	}

	public IProject createProject(String prjName) throws CoreException {
		TestProjectProvider provider = new TestProjectProvider(BUNDLE, "/projects/" + prjName,
				prjName, true);
		IProject prj = provider.getProject();
		return prj;
	}
	
	public IFacetedProject createJBossWSProject(String baseProjectName) throws CoreException {
		IProject project = createProject(baseProjectName);
		final IFacetedProject fproj = ProjectFacetsManager.create(project);
		fproj.installProjectFacet(getJBossWSFacetVersion(),createJBossWSDataModel(isServerSupplied()), null);
		assertNotNull(project);
		this.addResourceToCleanup(project);

		return fproj;
	}
	
	public IProjectFacetVersion getJBossWSFacetVersion(){
		IProjectFacet jbosswsFacet = ProjectFacetsManager.getProjectFacet("jbossws.core");
		IProjectFacetVersion jbosswsFacetVersion = jbosswsFacet.getVersion("3.0");
		return jbosswsFacetVersion;
	}
	
	protected ServiceModel createServiceModel() {
		model = new ServiceModel();
		model.setWebProjectName(fproject.getProject().getName());
		if(wsdlFile != null) {
		    model.setWsdlURI(wsdlFile.getLocationURI().toString());
		}
		model.setCustomPackage("org.apache.hello_world_soap_http");
		model.setUpdateWebxml(true);
		return model;
	}
	
	public IStatus publishWebProject(IProject project) throws CoreException {
		IModule mod = ServerUtil.getModule(project);
		IDeployableServer ds = (IDeployableServer)currentServer.loadAdapter(IDeployableServer.class, new NullProgressMonitor());
		IServerWorkingCopy copy = currentServer.createWorkingCopy();
		copy.modifyModules(new IModule[]{mod}, new IModule[0], new NullProgressMonitor());
		currentServer = copy.save(true, new NullProgressMonitor());
		ds = (IDeployableServer)currentServer.loadAdapter(IDeployableServer.class, new NullProgressMonitor());
		ds.setDeployLocationType("server");
		IStatus status = ServerRuntimeUtils.publish(IServer.PUBLISH_INCREMENTAL,currentServer);
		return status;
	}
	
	private boolean isServerSupplied() {
		return false;
	}
	
	public void tearDown() throws Exception{
		cleanResouces();
		JBossWSRuntime runtime = JBossWSRuntimeManager.getInstance().findRuntimeByName(RuntimeName);
        JBossWSRuntimeManager.getInstance().removeRuntime(runtime);
		shutdown(currentServer);
		try {
			if( currentServer != null )
				currentServer.delete();
		} catch( CoreException ce ) {
			// report
		}
		super.tearDown();
	}
	
	private void cleanResouces() throws Exception {
		boolean oldAutoBuilding = ResourcesUtils.setBuildAutomatically(false);
		Exception last = null;
		try {
			JobUtils.delay(500);
			for (IResource r : this.resourcesToCleanup) {
				try {
					r.delete(true, null);
					JobUtils.delay(500);
				} catch (Exception e) {
					e.printStackTrace();
					last = e;
				}
			}
		} finally {
			ResourcesUtils.setBuildAutomatically(oldAutoBuilding);
		}
		if (last != null)
			throw last;
		resourcesToCleanup.clear();
	}

	protected void undeployWebProject() throws CoreException {
		IModule[] modules = ServerUtil.getModules(currentServer.getServerType()
				.getRuntimeType().getModuleTypes());
		ServerDelegate ds = (ServerDelegate)currentServer.loadAdapter(ServerDelegate.class, new NullProgressMonitor());
		IServerWorkingCopy serverWC = ds.getServerWorkingCopy();
		serverWC.modifyModules(null, modules, null);
		serverWC.save(true, null).publish(IServer.PUBLISH_FULL, null);
	}
	
	private final void addResourceToCleanup(final IResource resource) {
		this.resourcesToCleanup.add(resource);
	}
	
	private IDataModel createJBossWSDataModel( boolean isServerSupplied) {
		IDataModel config = (IDataModel) new JBossWSFacetInstallDataModelProvider().create();
		if(isServerSupplied) {
			config.setBooleanProperty(IJBossWSFacetDataModelProperties.JBOSS_WS_RUNTIME_IS_SERVER_SUPPLIED, true);
		} else {
			config.setBooleanProperty(IJBossWSFacetDataModelProperties.JBOSS_WS_DEPLOY, false);
			config.setStringProperty(IJBossWSFacetDataModelProperties.JBOSS_WS_RUNTIME_ID, RuntimeName);
			config.setStringProperty(IJBossWSFacetDataModelProperties.JBOSS_WS_RUNTIME_HOME, wsHomePath);
		}
		return config;
	}
	
	private File getJBossWSHomeFolder() {
		assertTrue("No system property for the WS Home",JBOSS_WS_HOME != null);
		File runtimelocation = new File(JBOSS_WS_HOME);
		assertTrue("The system WS Home doesn't exist",runtimelocation.exists());
		String cmdFileLocation = JBOSS_WS_HOME + File.separator + "bin" + File.separator + "wsconsume.sh";
		assertTrue(JBOSS_WS_HOME + " is not a valid jboss AS home", new File(cmdFileLocation).exists());
		return runtimelocation;
	}
}
