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
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.jboss.tools.common.test.util.TestProjectProvider;
import org.jboss.tools.test.util.JUnitUtils;
import org.jboss.tools.test.util.xpl.EditorTestHelper;
import org.jboss.tools.ws.core.classpath.JbossWSRuntime;
import org.jboss.tools.ws.core.classpath.JbossWSRuntimeManager;
import org.jboss.tools.ws.core.facet.delegate.IJBossWSFacetDataModelProperties;
import org.jboss.tools.ws.core.facet.delegate.JBossWSFacetInstallDataModelProvider;
import org.jboss.tools.ws.creation.core.data.ServiceModel;

abstract public class AbstractJBossWSCommandTest extends TestCase {
	protected static final IWorkspace ws = ResourcesPlugin.getWorkspace();
	protected static final IWorkbench wb = PlatformUI.getWorkbench();
	
 

	protected static final String JBOSSWS_HOME = "jbosstools.test.jbossws.eap.home";
	protected static final String JBOSSWS_HOME_DEFAULT = "F:/jbdevstudio-ga/jboss-eap/seam";
	
	
	protected final Set<IResource> resourcesToCleanup = new HashSet<IResource>();

	protected static final IProjectFacetVersion dynamicWebVersion;
	protected static final IProjectFacetVersion javaVersion;
	protected static final IProjectFacetVersion jbosswsFacetVersion;
	private static final IProjectFacet jbosswsFacet;
	private static final String RuntimeName;
	private static final String RuntimeLocation;
	private static final boolean isDeployed;

	static String wsdlFileName = "HelloWorld.wsdl";
	static String BUNDLE = "org.jboss.tools.ws.core.test";
	
	IProject project;
	TestProjectProvider provider;
	
	static {
		javaVersion = ProjectFacetsManager.getProjectFacet("jst.java").getVersion("5.0");
		dynamicWebVersion = ProjectFacetsManager.getProjectFacet("jst.web").getVersion("2.5");
		jbosswsFacet = ProjectFacetsManager.getProjectFacet("jbossws.core");
		jbosswsFacetVersion = jbosswsFacet.getVersion("1.0");
		RuntimeName = "testjbosswsruntime";
		RuntimeLocation = "";
		isDeployed = false;
		
		
	}

	public AbstractJBossWSCommandTest() {
	}
	
	protected void setUp() throws Exception {
		super.setUp();
		JbossWSRuntimeManager.getInstance().addRuntime(RuntimeName, getJBossWSHomeFolder().toString(), "", true);
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
		super.tearDown();
	}

	public void testInitialCommand() throws CoreException{
		IFacetedProject fproject = createJBossWSProject("JBossWSTestProject", false);
		IFile wsdlFile = fproject.getProject().getFile(wsdlFileName);
		ServiceModel model = new ServiceModel();
		model.setWsdlURI(wsdlFile.getFullPath().toOSString());
		
		
	}
	
	protected IFacetedProject createJBossWSProject(String baseProjectName, boolean isServerSupplied) throws CoreException {
		IProject project = createProject("JBossWSTestProject");
		final IFacetedProject fproj = ProjectFacetsManager.create(project);
	
		installDependentFacets(fproj);
		fproj.installProjectFacet(jbosswsFacetVersion, createJBossWSDataModel(isServerSupplied), null);
		
		assertNotNull(project);
		assertTrue(project.exists());
		this.addResourceToCleanup(project);	
		
		
		return fproj;
	}
	
	protected void installDependentFacets(final IFacetedProject fproj) throws CoreException {
		fproj.installProjectFacet(javaVersion, null, null);
		fproj.installProjectFacet(dynamicWebVersion, null, null);
		fproj.installProjectFacet(jbosswsFacetVersion, null, null);
	}
	
	
	
	
	
	
	abstract protected IProject getProject();

	abstract void setUpSeamProjects();

	abstract void assertProjectsAreCreated();

	
	
	protected IDataModel createJBossWSDataModel( boolean isServerSupplied) {
		IDataModel config = (IDataModel) new JBossWSFacetInstallDataModelProvider().create();
		if(isServerSupplied){
			config.setBooleanProperty(IJBossWSFacetDataModelProperties.JBOSS_WS_RUNTIME_IS_SERVER_SUPPLIED, true);
		}else{
			config.setBooleanProperty(IJBossWSFacetDataModelProperties.JBOSS_WS_DEPLOY, isDeployed);
			config.setStringProperty(IJBossWSFacetDataModelProperties.JBOSS_WS_RUNTIME_ID, RuntimeName);
			config.setStringProperty(IJBossWSFacetDataModelProperties.JBOSS_WS_RUNTIME_HOME, RuntimeLocation);
		}
		return config;
	}

	
	
	protected final void addResourceToCleanup(final IResource resource) {
		this.resourcesToCleanup.add(resource);
	}



	
	protected File getJBossWSHomeFolder() {
		return new File(System.getProperty(JBOSSWS_HOME, JBOSSWS_HOME_DEFAULT));
	}



	protected String getPackagePath(String packageName) {
		return (packageName == null ? "" : packageName.replace('.', '/'));
	}
	

 
}
