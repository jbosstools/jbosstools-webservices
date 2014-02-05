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

import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.APPLICATION_PATH;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.HTTP_METHOD;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaProject;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsJavaApplication;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsProvider;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsWebxmlApplication;
import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionUtils;
import org.jboss.tools.ws.jaxrs.core.internal.utils.WtpUtils;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.CompilationUnitsRepository;
import org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElementChangedListener;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsEndpointChangedListener;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResourceMethod;
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
public abstract class AbstractCommonTestCase implements IJaxrsElementChangedListener, IJaxrsEndpointChangedListener {

	@SuppressWarnings("unused")
	private static final String M2_REPO = "M2_REPO";

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

	private ProjectSynchronizator synchronizor;

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
			WorkbenchTasks.syncSampleProject(DEFAULT_SAMPLE_PROJECT_NAME);
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
			synchronizor = new ProjectSynchronizator();
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
			metamodel.addListener((IJaxrsElementChangedListener)this);
			metamodel.addListener((IJaxrsEndpointChangedListener)this);
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

	
	protected IType resolveType(String typeName) throws CoreException {
		return JdtUtils.resolveType(typeName, javaProject, new NullProgressMonitor());
	}
	
	protected IMethod resolveMethod(final String typeName, final String methodName) throws CoreException {
		final IType type = resolveType(typeName);
		return resolveMethod(type, methodName);
	}

	protected IMethod resolveMethod(final IType type, final String methodName) throws CoreException {
		for(IMethod method : type.getMethods()) {
			if(method.getElementName().equals(methodName)) {
				return method;
			}
		}
		return null;
	}

	/**
	 * Creates a java annotated type based JAX-RS Application element
	 * @param isApplicationSubtype TODO
	 * @param type
	 * @param applicationPath
	 * 
	 * @return
	 * @throws CoreException
	 */
	protected JaxrsJavaApplication createJavaApplication(String typeName) throws CoreException {
		return createJavaApplication(typeName, true);
		
	}
	/**
	 * Creates a java annotated type based JAX-RS Application element
	 * @param isApplicationSubtype TODO
	 * @param type
	 * @param applicationPath
	 * 
	 * @return
	 * @throws CoreException
	 */
	protected JaxrsJavaApplication createJavaApplication(String typeName, boolean isApplicationSubtype) throws CoreException {
		final IType type = resolveType(typeName);
		//final Map<String, Annotation> annotations = resolveAnnotations(type, APPLICATION_PATH.qualifiedName, SuppressWarnings.class.getName());
		//final JaxrsJavaApplication application = new JaxrsJavaApplication(type, annotations.get(APPLICATION_PATH.qualifiedName), isApplicationSubtype);
		final JaxrsJavaApplication application = JaxrsJavaApplication.from(type).withMetamodel(metamodel).build();
		// clear notifications as this method should be called during the test initialization, and thus should not count in the verifications phase
		application.setJaxrsCoreApplicationSubclass(isApplicationSubtype);
		return application;

	}

	/**
	 * Creates a java annotated type based JAX-RS Application element
	 * 
	 * @param type
	 * @param applicationPath
	 * @return
	 * @throws CoreException
	 */
	protected JaxrsJavaApplication createJavaApplication(String typeName, String applicationPath) throws CoreException {
		final JaxrsJavaApplication application = createJavaApplication(typeName, true);
		final Annotation applicationPathAnnotation = application.getAnnotation(APPLICATION_PATH.qualifiedName);
		application.addOrUpdateAnnotation(createAnnotation(applicationPathAnnotation, applicationPath));
		return application;
	}

	/**
	 * Creates a web.xml based JAX-RS Application element
	 * 
	 * @param applicationPath
	 * @return
	 * @throws CoreException 
	 * @throws IOException 
	 */
	protected JaxrsWebxmlApplication createWebxmlApplication(final String applicationClassName,
			final String applicationPath) throws CoreException, IOException {
		final IResource webDeploymentDescriptor = WtpUtils.getWebDeploymentDescriptor(project);
		WorkbenchUtils.replaceContent(webDeploymentDescriptor, "javax.ws.rs.core.Application", applicationClassName);
		WorkbenchUtils.replaceContent(webDeploymentDescriptor, "/hello/*", applicationPath);
		return JaxrsWebxmlApplication.from(webDeploymentDescriptor).inMetamodel(metamodel).build();
	}

	/**
	 * 
	 */
	protected void resetElementChangesNotifications() {
		LOGGER.info("Reseting Changes Notifications before test operation");
		elementChanges.clear();
		endpointChanges.clear();
	}
	
	/**
	 * @return
	 * @throws CoreException
	 * @throws JavaModelException
	 */
	protected JaxrsHttpMethod createHttpMethod(EnumJaxrsClassname httpMethodElement) throws CoreException,
			JavaModelException {
		return createHttpMethod(httpMethodElement.qualifiedName);
	}
	
	protected JaxrsHttpMethod createHttpMethod(String typeName) throws CoreException, JavaModelException {
		final IType type = resolveType(typeName);
		return createHttpMethod(type);
	}

	protected JaxrsHttpMethod createHttpMethod(String typeName, String httpVerb) throws CoreException, JavaModelException {
		final IType type = resolveType(typeName);
		return createHttpMethod(type, httpVerb);
	}

	protected JaxrsHttpMethod createHttpMethod(IType type) throws CoreException {
		final JaxrsHttpMethod httpMethod = JaxrsHttpMethod.from(type).withMetamodel(metamodel).build();
		return httpMethod;
	}

	protected JaxrsHttpMethod createHttpMethod(IType type, String httpVerb) throws CoreException {
		final JaxrsHttpMethod httpMethod = JaxrsHttpMethod.from(type).withMetamodel(metamodel).build();
		final Annotation httpMethodAnnotation = httpMethod.getAnnotation(HTTP_METHOD.qualifiedName);
		httpMethod.addOrUpdateAnnotation(createAnnotation(httpMethodAnnotation, httpVerb));
		return httpMethod;
	}
	
	protected JaxrsProvider createProvider(final String typeName) throws JavaModelException, CoreException {
		final IType type = resolveType(typeName);
		return createProvider(type);
	}

	protected JaxrsProvider createProvider(final IType type) throws JavaModelException, CoreException {
		return JaxrsProvider.from(type).withMetamodel(metamodel).build();
	}

	protected JaxrsResource createResource(String typeName) throws CoreException, JavaModelException {
		return createResource(JdtUtils.resolveType(typeName, javaProject, new NullProgressMonitor()));
	}

	protected JaxrsResource createResource(IType type) throws CoreException, JavaModelException {
		return JaxrsResource.from(type, metamodel.findAllHttpMethods()).withMetamodel(metamodel).build();
	}
	
	protected Annotation createAnnotation(String className) {
		return createAnnotation(null, className, null);
	}

	protected Annotation createAnnotation(String className, String value) {
		return createAnnotation(null, className, value);
	}

	protected Annotation createAnnotation(IAnnotation annotation, String name, String value) {
		Map<String, List<String>> values = new HashMap<String, List<String>>();
		values.put("value", Arrays.asList(value));
		return new Annotation(annotation, name, values);
	}
	
	/**
	 * Returns a <strong>new annotation</strong> built from the given one, with overriden values
	 * @param annotation
	 * @param values
	 * @return
	 * @throws JavaModelException
	 */
	protected Annotation createAnnotation(final Annotation annotation, final String... values)
			throws JavaModelException {
		Map<String, List<String>> elements = CollectionUtils.toMap("value", Arrays.asList(values));
		return new Annotation(annotation.getJavaAnnotation(), annotation.getFullyQualifiedName(), elements);
	}

	protected IType getType(String typeName) throws CoreException {
		return JdtUtils.resolveType(typeName, javaProject, null);
	}

	/**
	 * @param type
	 * @return
	 * @throws JavaModelException
	 */
	protected IMethod getJavaMethod(IType type, String name) throws JavaModelException {
		for (IMethod method : type.getMethods()) {
			if (method.getElementName().equals(name)) {
				return method;
			}
		}
		Assert.fail("Failed to locate method named '" + name + "'");
		return null;
	}

	protected IPackageFragmentRoot getPackageFragmentRoot(String path) throws JavaModelException {
		return WorkbenchUtils.getPackageFragmentRoot(javaProject, path,
				new NullProgressMonitor());
	}
	
	protected static void removeResourceMethod(final JaxrsResource resource, final String methodName) throws CoreException {
		final List<IJaxrsResourceMethod> allMethods = new ArrayList<IJaxrsResourceMethod>(resource.getAllMethods());
		for(IJaxrsResourceMethod resourceMethod : allMethods) {
			if(resourceMethod.getJavaElement().getElementName().equals(methodName)) {
				resource.removeMethod(resourceMethod);
				break;
			}
		}
	}

	protected static JaxrsResourceMethod getResourceMethod(final JaxrsResource resource, final String methodName) {
		for(IJaxrsResourceMethod resourceMethod : resource.getAllMethods()) {
			if(resourceMethod.getJavaElement().getElementName().equals(methodName)) {
				return (JaxrsResourceMethod) resourceMethod;
			}
		}
		return null;
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
