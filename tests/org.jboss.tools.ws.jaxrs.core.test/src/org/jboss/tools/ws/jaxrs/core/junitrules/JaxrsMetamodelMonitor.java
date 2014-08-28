/**
 * 
 */
package org.jboss.tools.ws.jaxrs.core.junitrules;

import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.APPLICATION_PATH;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.HTTP_METHOD;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.jboss.tools.ws.jaxrs.core.JBossJaxrsCorePlugin;
import org.jboss.tools.ws.jaxrs.core.configuration.ProjectBuilderUtils;
import org.jboss.tools.ws.jaxrs.core.configuration.ProjectNatureUtils;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JavaElementChangedEvent;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsElementFactory;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsJavaApplication;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsNameBinding;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsParamConverterProvider;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsParameterAggregator;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsProvider;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceProperty;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsWebxmlApplication;
import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionUtils;
import org.jboss.tools.ws.jaxrs.core.internal.utils.TestLogger;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.Flags;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElementChangedListener;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsMetamodelChangedListener;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsNameBinding;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResourceProperty;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsEndpointDelta;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsMetamodelDelta;
import org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames;
import org.jboss.tools.ws.jaxrs.core.wtp.WtpUtils;

/**
 * @author xcoulon
 *
 */
public class JaxrsMetamodelMonitor extends TestProjectMonitor implements IJaxrsMetamodelChangedListener, IJaxrsElementChangedListener {
	
	public final static int ANY_EVENT_TYPE = 0;

	public final static int NO_FLAG = 0;

	private JaxrsMetamodel metamodel;

	private final List<JaxrsElementDelta> elementChanges = new ArrayList<JaxrsElementDelta>();

	private final List<JaxrsEndpointDelta> endpointChanges = new ArrayList<JaxrsEndpointDelta>();

	private final List<IJaxrsEndpoint> endpointProblemLevelChanges = new ArrayList<IJaxrsEndpoint>();

	private final List<JaxrsMetamodelDelta> metamodelChanges = new ArrayList<JaxrsMetamodelDelta>();
	
	private final List<IJaxrsMetamodel> metamodelProblemLevelChanges = new ArrayList<IJaxrsMetamodel>();

	private final boolean buildMetamodel;
	
	private long testStartTime;
	
	public JaxrsMetamodelMonitor(final String projectName, final boolean buildMetamodel) {
		super(projectName);
		this.buildMetamodel = buildMetamodel;
	}
	
	@Override
	protected void before() throws Throwable {
		TestLogger.debug("***********************************************");
		TestLogger.debug("* Setting up test project (with metamodel: {})...", buildMetamodel);
		TestLogger.debug("***********************************************");
		long startTime = System.currentTimeMillis();
		try {
			super.setupProject();
			this.metamodel = JaxrsMetamodel.create(super.getJavaProject());
			if(buildMetamodel) {
				buildMetamodel();
			}
			// clear listener list
			this.elementChanges.clear();
			this.endpointChanges.clear();
			this.endpointProblemLevelChanges.clear();
			this.metamodelProblemLevelChanges.clear();
			// register listeners:
			this.metamodel.addJaxrsElementChangedListener(this);
			JBossJaxrsCorePlugin.addJaxrsMetamodelChangedListener(this);
		} catch (CoreException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			long endTime = System.currentTimeMillis();
			TestLogger.debug("***********************************************");
			TestLogger.debug("* Test project setup in " + (endTime - startTime) + "ms. ***");
			TestLogger.debug("***********************************************");
			testStartTime = System.currentTimeMillis();
		}
	}

	@Override
	protected void after() {
		TestLogger.debug("*********************************************************************");
		TestLogger.debug("* Tearing down project (with metamodel: {}) after test run ({}ms)", buildMetamodel, (System.currentTimeMillis() - testStartTime));
		TestLogger.debug("*********************************************************************");
		
		if(this.metamodel == null) {
			return;
		}
		long startTime = System.currentTimeMillis();
		try {
			TestLogger.info("Destroying metamodel...");
			// remove listener before sync' to avoid desync...
			metamodel.removeListener((IJaxrsElementChangedListener) this);
			JBossJaxrsCorePlugin.removeListener((IJaxrsMetamodelChangedListener) this);
			metamodel.remove();
		} catch (CoreException e) {
			e.printStackTrace();
			fail("Failed to remove metamodel: " + e.getMessage());
		} finally {
			long endTime = System.currentTimeMillis();
			TestLogger.debug("Test Workspace sync'd in " + (endTime - startTime) + "ms.");
			super.after();
		}
		
	}

	@Override
	public void notifyElementChanged(final JaxrsElementDelta delta) {
		this.elementChanges.add(delta);
	}

	@Override
	public void notifyEndpointChanged(final JaxrsEndpointDelta delta) {
		this.endpointChanges.add(delta);
	}

	@Override
	public void notifyEndpointProblemLevelChanged(final IJaxrsEndpoint endpoint) {
		this.endpointProblemLevelChanges.add(endpoint);

	}

	@Override
	public void notifyMetamodelChanged(final JaxrsMetamodelDelta delta) {
		this.metamodelChanges.add(delta);
	}
	
	@Override
	public void notifyMetamodelProblemLevelChanged(final IJaxrsMetamodel metamodel) {
		this.metamodelProblemLevelChanges.add(metamodel);

	}

	public List<JaxrsElementDelta> getElementChanges() {
		Collections.sort(elementChanges, new JaxrsElementDeltaComparator());
		return elementChanges;
	}

	public List<JaxrsEndpointDelta> getEndpointChanges() {
		return endpointChanges;
	}

	public List<IJaxrsEndpoint> getEndpointProblemLevelChanges() {
		return endpointProblemLevelChanges;
	}

	public List<IJaxrsMetamodel> getMetamodelProblemLevelChanges() {
		return metamodelProblemLevelChanges;
	}
	
	public List<JaxrsMetamodelDelta> getMetamodelChanges() {
		return metamodelChanges;
	}

	public JaxrsMetamodel getMetamodel() {
		return metamodel;
	}

	public void resetElementChangesNotifications() {
		this.elementChanges.clear();
		this.endpointChanges.clear();
		this.endpointProblemLevelChanges.clear();
		this.metamodelProblemLevelChanges.clear();
	}

	public void processEvent(final Annotation annotation, final int deltaKind) throws CoreException {
		final JavaElementChangedEvent delta = new JavaElementChangedEvent(annotation.getJavaAnnotation(), deltaKind, ANY_EVENT_TYPE,
				JdtUtils.parse(((IMember) annotation.getJavaParent()), new NullProgressMonitor()), Flags.NONE);
		metamodel.processJavaElementChange(delta, new NullProgressMonitor());
	}
	
	public void processEvent(final IJavaElement element, final int deltaKind) throws CoreException {
		processEvent(element, deltaKind, Flags.NONE);
	}
	
	public void processEvent(final IJavaElement element, final int deltaKind, final Flags flags) throws CoreException {
		final JavaElementChangedEvent delta = new JavaElementChangedEvent(element, deltaKind, ANY_EVENT_TYPE, JdtUtils.parse(element,
				new NullProgressMonitor()), flags);
		metamodel.processJavaElementChange(delta, new NullProgressMonitor());
	}
	

	/********************************************************************************************
	 * 
	 * JAX-RS Metamodel manipulation utility methods (a.k.a., Helpers)
	 * 
	 ********************************************************************************************/
	/**
	 * 
	 * @return 
	 * @throws CoreException
	 * @throws InterruptedException 
	 * @throws OperationCanceledException 
	 */
	public JaxrsMetamodel buildMetamodel() throws CoreException, OperationCanceledException, InterruptedException {
		ProjectNatureUtils.installProjectNature(getProject(), ProjectNatureUtils.JAXRS_NATURE_ID);
		// remove the validation builder to avoid blocking during tests
		ProjectBuilderUtils.uninstallProjectBuilder(getProject(), ProjectBuilderUtils.VALIDATOR_BUILDER_ID);
		buildProject();		
		return this.metamodel;
	}

	
	public Set<IJaxrsElement> createElements(final String... classNames) throws CoreException {
		final Set<IJaxrsElement> elements = new HashSet<IJaxrsElement>();
		for(String className : classNames) {
			final IType javaType = JdtUtils.resolveType(className, getJavaProject(), null);
			elements.addAll(JaxrsElementFactory.createElements(javaType, JdtUtils.parse(javaType, null), metamodel, null));
		}
		return elements;
	}

	/**
	 * Creates a java annotated type based JAX-RS Application element
	 * @param isApplicationSubtype
	 * @param type
	 * @param applicationPath
	 * 
	 * @return
	 * @throws CoreException
	 */
	public JaxrsJavaApplication createJavaApplication(final String typeName) throws CoreException {
		return createJavaApplication(typeName, true);
		
	}
	/**
	 * Creates a java annotated type based JAX-RS Application element
	 * @param isApplicationSubtype
	 * @param type
	 * @param applicationPath
	 * 
	 * @return
	 * @throws CoreException
	 */
	public JaxrsJavaApplication createJavaApplication(final String typeName, boolean isApplicationSubtype) throws CoreException {
		final IType type = JdtUtils.resolveType(typeName, metamodel.getJavaProject(), new NullProgressMonitor());
		//final Map<String, Annotation> annotations = resolveAnnotations(type, APPLICATION_PATH, SuppressWarnings.class.getName());
		//final JaxrsJavaApplication application = new JaxrsJavaApplication(type, annotations.get(APPLICATION_PATH), isApplicationSubtype);
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
	public JaxrsJavaApplication createJavaApplication(final String typeName, final String applicationPath) throws CoreException {
		final JaxrsJavaApplication application = createJavaApplication(typeName, true);
		final Annotation applicationPathAnnotation = application.getAnnotation(APPLICATION_PATH);
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
	public JaxrsWebxmlApplication createWebxmlApplication(final String applicationClassName,
			final String applicationPath) throws CoreException, IOException {
		final IResource webDeploymentDescriptor = WtpUtils.getWebDeploymentDescriptor(metamodel.getProject());
		ResourcesUtils.replaceContent(webDeploymentDescriptor, "javax.ws.rs.core.Application", applicationClassName);
		ResourcesUtils.replaceContent(webDeploymentDescriptor, "/hello/*", applicationPath);
		return JaxrsWebxmlApplication.from(webDeploymentDescriptor).inMetamodel(metamodel).build();
	}

	/**
	 * Creates a web.xml based JAX-RS Application element
	 * 
	 * @param applicationPath
	 * @return
	 * @throws CoreException 
	 * @throws IOException 
	 */
	public JaxrsWebxmlApplication createWebxmlApplication() throws CoreException, IOException {
		final IResource webDeploymentDescriptor = WtpUtils.getWebDeploymentDescriptor(metamodel.getProject());
		return JaxrsWebxmlApplication.from(webDeploymentDescriptor).inMetamodel(metamodel).build();
	}

	public JaxrsHttpMethod createHttpMethod(final String typeName) throws CoreException, JavaModelException {
		final IType type = JdtUtils.resolveType(typeName, metamodel.getJavaProject(), new NullProgressMonitor());
		return createHttpMethod(type);
	}

	public JaxrsHttpMethod createHttpMethod(final String typeName, final String httpVerb) throws CoreException, JavaModelException {
		final IType type = JdtUtils.resolveType(typeName, metamodel.getJavaProject(), new NullProgressMonitor());
		return createHttpMethod(type, httpVerb);
	}

	public JaxrsHttpMethod createHttpMethod(final IType type) throws CoreException {
		final JaxrsHttpMethod httpMethod = JaxrsHttpMethod.from(type).withMetamodel(metamodel).build();
		return httpMethod;
	}

	public JaxrsHttpMethod createHttpMethod(final IType type, final String httpVerb) throws CoreException {
		final JaxrsHttpMethod httpMethod = JaxrsHttpMethod.from(type).withMetamodel(metamodel).build();
		final Annotation httpMethodAnnotation = httpMethod.getAnnotation(HTTP_METHOD);
		httpMethod.addOrUpdateAnnotation(createAnnotation(httpMethodAnnotation, httpVerb));
		return httpMethod;
	}
	
	public IJaxrsNameBinding createNameBinding(IType nameBindingType) throws CoreException {
		return JaxrsNameBinding.from(nameBindingType).withMetamodel(metamodel).build();
	}
	
	public JaxrsProvider createProvider(final String typeName) throws JavaModelException, CoreException {
		final IType type = JdtUtils.resolveType(typeName, metamodel.getJavaProject(), new NullProgressMonitor());
		return createProvider(type);
	}

	public JaxrsProvider createProvider(final IType type) throws JavaModelException, CoreException {
		return JaxrsProvider.from(type).withMetamodel(metamodel).build();
	}

	public JaxrsResource createResource(final String typeName) throws CoreException, JavaModelException {
		return createResource(JdtUtils.resolveType(typeName, metamodel.getJavaProject(), new NullProgressMonitor()));
	}

	public JaxrsResource createResource(final IType type) throws CoreException, JavaModelException {
		return JaxrsResource.from(type, metamodel.findAllHttpMethodNames()).withMetamodel(metamodel).build();
	}
	
	public JaxrsParameterAggregator createParameterAggregator(final String typeName) throws JavaModelException, CoreException {
		return createParameterAggregator(JdtUtils.resolveType(typeName, metamodel.getJavaProject(), new NullProgressMonitor()));
	}
	
	public JaxrsParameterAggregator createParameterAggregator(final IType type) throws JavaModelException, CoreException {
		return JaxrsParameterAggregator.from(type).buildInMetamodel(metamodel);
	}

	public JaxrsParamConverterProvider createParameterConverterProvider(final String typeName) throws JavaModelException, CoreException {
		return createParameterConverterProvider(JdtUtils.resolveType(typeName, metamodel.getJavaProject(), new NullProgressMonitor()));
	}
	
	public JaxrsParamConverterProvider createParameterConverterProvider(final IType type) throws JavaModelException, CoreException {
		return JaxrsParamConverterProvider.from(type).withMetamodel(metamodel).build();
	}

	/**
	 * Returns a <strong>new annotation</strong> built from the given one, with overriden values
	 * @param annotation
	 * @param values
	 * @return
	 * @throws JavaModelException
	 */
	private static Annotation createAnnotation(final Annotation annotation, final String... values)
			throws JavaModelException {
		Map<String, List<String>> elements = CollectionUtils.toMap("value", Arrays.asList(values));
		return new Annotation(annotation.getJavaAnnotation(), annotation.getFullyQualifiedName(), elements);
	}

	public void removeResourceMethod(final JaxrsResource resource, final String methodName) throws CoreException {
		final List<IJaxrsResourceMethod> allMethods = new ArrayList<IJaxrsResourceMethod>(resource.getAllMethods());
		for(IJaxrsResourceMethod resourceMethod : allMethods) {
			if(resourceMethod.getJavaElement().getElementName().equals(methodName)) {
				((JaxrsResourceMethod)resourceMethod).remove(Flags.NONE);
				return;
			}
		}
		fail("Failed to remove Resource Method '" + methodName + "'");
	}

	public JaxrsResourceMethod resolveResourceMethod(final JaxrsResource resource, final String methodName) {
		final List<IJaxrsResourceMethod> allMethods = resource.getAllMethods();
		for(IJaxrsResourceMethod resourceMethod : allMethods) {
			if(resourceMethod.getJavaElement().getElementName().equals(methodName)) {
				return (JaxrsResourceMethod) resourceMethod;
			}
		}
		fail("Failed to resolve Resource Method '" + methodName + "'");
		return null;
	}

	public void removeResourceProperty(final JaxrsResource resource, final String propertyName) throws CoreException {
		final List<IJaxrsResourceProperty> allProperties = new ArrayList<IJaxrsResourceProperty>(resource.getAllProperties());
		for(IJaxrsResourceProperty resourceProperty : allProperties) {
			if(resourceProperty.getJavaElement().getElementName().equals(propertyName)) {
				((JaxrsResourceProperty)resourceProperty).remove(Flags.NONE);
				return;
			}
		}
		fail("Failed to remove Resource Property '" + propertyName + "'");
	}

	public JaxrsResourceProperty resolveResourceProperty(final JaxrsResource resource, final String propertyName) {
		final List<JaxrsResourceProperty> allProperties = resource.getAllProperties();
		for(JaxrsResourceProperty resourceMethod : allProperties) {
			if(resourceMethod.getJavaElement().getElementName().equals(propertyName)) {
				return (JaxrsResourceProperty) resourceMethod;
			}
		}
		fail("Failed to resolve Resource Property '" + propertyName + "'");
		return null;
	}

}
