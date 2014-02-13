/******************************************************************************* 
 * Copyright (c) 2008 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/

package org.jboss.tools.ws.jaxrs.core;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.JavaProject;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.jdt.CompilationUnitsRepository;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElementChangedListener;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsEndpointChangedListener;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsEndpointDelta;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Made abstract, so won't be automatically picked up as test (since intended to
 * be subclassed).
 * 
 * Based on
 * http://dev.eclipse.org/viewcvs/index.cgi/incubator/sourceediting/tests
 * /org.eclipse
 * .wst.xsl.ui.tests/src/org/eclipse/wst/xsl/ui/tests/AbstractXSLUITest
 * .java?revision=1.2&root=WebTools_Project&view=markup
 */
@RunWithProject("org.jboss.tools.ws.jaxrs.tests.sampleproject")
@SuppressWarnings("restriction")
@Deprecated
public abstract class AbstractCommonTestCase implements IJaxrsElementChangedListener, IJaxrsEndpointChangedListener {

	public static final Logger LOGGER = LoggerFactory.getLogger(AbstractCommonTestCase.class);

	protected String projectName = null;

	protected IJavaProject javaProject = null;

	protected IProject project = null;

	protected List<JaxrsElementDelta> elementChanges = null;
	
	protected List<JaxrsEndpointDelta> endpointChanges = null;
	
	protected List<IJaxrsEndpoint> endpointProblemLevelChanges = null;

	protected List<IJaxrsMetamodel> metamodelProblemLevelChanges = null;
	
	public final static String DEFAULT_SAMPLE_PROJECT_NAME = WorkbenchUtils
			.retrieveSampleProjectName(AbstractCommonTestCase.class);

	private TestProjectSynchronizator synchronizor;

	protected JaxrsMetamodel metamodel;
	
	@Rule
	public TestRule watchman = new TestWatcher() {
		@Override
		public void starting(Description description) {
			LOGGER.info("**********************************************************************************");
			LOGGER.info("Starting test '{}'...", description.getMethodName());
			LOGGER.info("**********************************************************************************");
		}

		@Override
		public void finished(Description description) {
			LOGGER.info("**********************************************************************************");
			LOGGER.info("Finished test '{}'.", description.getMethodName());
			LOGGER.info("**********************************************************************************");
		}
	};

	@BeforeClass
	public static void setupWorkspace() throws Exception {
		// org.eclipse.jdt.core.JavaCore.getPlugin().start(bundle.getBundleContext());
		long startTime = new Date().getTime();
		try {
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			if (workspace.isAutoBuilding()) {
				IWorkspaceDescription description = workspace.getDescription();
				description.setAutoBuilding(false);
				workspace.setDescription(description);
			}
			workspace.getRoot().refreshLocal(IResource.DEPTH_INFINITE, null);
			LOGGER.info("Initial Synchronization (@BeforeClass)");
		} finally {
			long endTime = new Date().getTime();
			LOGGER.info("Initial Workspace setup in " + (endTime - startTime) + "ms.");
		}
		JBossJaxrsCorePlugin.getDefault().pauseListeners();
	}

	@Before
	public void setup() throws Exception {
		long startTime = new Date().getTime();
		try {
			JBossJaxrsCorePlugin.getDefault().pauseListeners();
			projectName = WorkbenchUtils.retrieveSampleProjectName(this.getClass());
			project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
			project.open(new NullProgressMonitor());
			javaProject = JavaCore.create(project);
			javaProject.open(new NullProgressMonitor());
			Assert.assertNotNull("JavaProject not found", javaProject.exists());
			Assert.assertNotNull("Project not found", javaProject.getProject().exists());
			Assert.assertTrue("Project is not a JavaProject", JavaProject.hasJavaNature(javaProject.getProject()));
			synchronizor = new TestProjectSynchronizator(DEFAULT_SAMPLE_PROJECT_NAME);
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			workspace.addResourceChangeListener(synchronizor);
			// clear CompilationUnit repository
			CompilationUnitsRepository.getInstance().clear();
			
			//metamodel = spy(JaxrsMetamodel.create(javaProject));
			// replace the normal metamodel instance with the one spied by Mockito
			//javaProject.getProject().setSessionProperty(JaxrsMetamodel.METAMODEL_QUALIFIED_NAME, metamodel);
			metamodel = JaxrsMetamodel.create(javaProject);
			this.elementChanges = new ArrayList<JaxrsElementDelta>();
			this.endpointChanges = new ArrayList<JaxrsEndpointDelta>();
			this.endpointProblemLevelChanges = new ArrayList<IJaxrsEndpoint>();
			this.metamodelProblemLevelChanges = new ArrayList<IJaxrsMetamodel>();
			metamodel.addJaxrsElementChangedListener((IJaxrsElementChangedListener)this);
			metamodel.addJaxrsEndpointChangedListener((IJaxrsEndpointChangedListener)this);
		} finally {
			long endTime = new Date().getTime();
			LOGGER.info("Test Workspace setup in " + (endTime - startTime) + "ms.");
		}
	}

	@After
	public void removeResourceChangeListener() throws CoreException, InvocationTargetException, InterruptedException {
		long startTime = new Date().getTime();
		try {
			LOGGER.info("Synchronizing the workspace back to its initial state...");
			// remove listener before sync' to avoid desync...
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			workspace.removeResourceChangeListener(synchronizor);
			synchronizor.resync();
		} finally {
			long endTime = new Date().getTime();
			LOGGER.info("Test Workspace sync'd in " + (endTime - startTime) + "ms.");
		}
	}

	@After
	public void removeMetamodelListener() {
		if(metamodel != null) {
			metamodel.removeListener((IJaxrsElementChangedListener)this);
			metamodel.removeListener((IJaxrsEndpointChangedListener)this);
		}
	}
	
	void resetProblemLevelChangeNotifications() {
		this.endpointProblemLevelChanges.clear();
		this.metamodelProblemLevelChanges.clear();
	}

	
	

	@Override
	public void notifyElementChanged(final JaxrsElementDelta delta) {
		elementChanges.add(delta);
	}

	@Override
	public void notifyEndpointChanged(final JaxrsEndpointDelta delta) {
		endpointChanges.add(delta);
	}

	@Override
	public void notifyEndpointProblemLevelChanged(IJaxrsEndpoint endpoint) {
		endpointProblemLevelChanges.add(endpoint);
		
	}

	@Override
	public void notifyMetamodelProblemLevelChanged(IJaxrsMetamodel metamodel) {
		metamodelProblemLevelChanges.add(metamodel);
		
	}
	
	

}
