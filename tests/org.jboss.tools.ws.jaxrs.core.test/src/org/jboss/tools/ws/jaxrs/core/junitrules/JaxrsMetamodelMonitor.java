/**
 * 
 */
package org.jboss.tools.ws.jaxrs.core.junitrules;

import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.APPLICATION_PATH;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.HTTP_METHOD;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.jboss.tools.ws.jaxrs.core.configuration.ProjectNatureUtils;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JavaElementDelta;
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
import org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElementChangedListener;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsEndpointChangedListener;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsEndpointDelta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author xcoulon
 *
 */
public class JaxrsMetamodelMonitor extends TestProjectMonitor implements IJaxrsElementChangedListener,
		IJaxrsEndpointChangedListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(JaxrsMetamodelMonitor.class);

	private JaxrsMetamodel metamodel;

	private final List<JaxrsElementDelta> elementChanges = new ArrayList<JaxrsElementDelta>();

	private final List<JaxrsEndpointDelta> endpointChanges = new ArrayList<JaxrsEndpointDelta>();

	private final List<IJaxrsEndpoint> endpointProblemLevelChanges = new ArrayList<IJaxrsEndpoint>();

	private final List<IJaxrsMetamodel> metamodelProblemLevelChanges = new ArrayList<IJaxrsMetamodel>();

	private final boolean buildMetamodel;
	
	public JaxrsMetamodelMonitor(final String projectName, final boolean buildMetamodel) {
		super(projectName);
		this.buildMetamodel = buildMetamodel;
	}
	
	@Override
	protected void before() throws Throwable {
		LOGGER.debug("***********************************************");
		LOGGER.debug("* Setting up test project (with metamodel: {})...", buildMetamodel);
		LOGGER.debug("***********************************************");
		long startTime = new Date().getTime();
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
			this.metamodel.addJaxrsEndpointChangedListener(this);
			
		} catch (CoreException e) {
			fail(e.getMessage());
		} finally {
			long endTime = new Date().getTime();
			LOGGER.debug("***********************************************");
			LOGGER.debug("* Test project setup in " + (endTime - startTime) + "ms. ***");
			LOGGER.debug("***********************************************");
		}
	}

	@Override
	protected void after() {
		LOGGER.debug("***********************************************");
		LOGGER.debug("* Tearing down project (with metamodel: {}) after test run...", buildMetamodel);
		LOGGER.debug("***********************************************");
		
		if(this.metamodel == null) {
			return;
		}
		long startTime = new Date().getTime();
		try {
			LOGGER.info("Destroying metamodel...");
			// remove listener before sync' to avoid desync...
			if (metamodel != null) {
				metamodel.removeListener((IJaxrsElementChangedListener) this);
				metamodel.removeListener((IJaxrsEndpointChangedListener) this);
				metamodel.remove();
			}
		} catch (CoreException e) {
			e.printStackTrace();
			fail("Failed to remove metamodel: " + e.getMessage());
		} finally {
			long endTime = new Date().getTime();
			LOGGER.info("Test Workspace sync'd in " + (endTime - startTime) + "ms.");
			super.after();
		}
		
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

	public List<JaxrsElementDelta> getElementChanges() {
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

	public JaxrsMetamodel getMetamodel() {
		return metamodel;
	}

	public void resetElementChangesNotifications() {
		this.elementChanges.clear();
		this.endpointChanges.clear();
		this.endpointProblemLevelChanges.clear();
		this.metamodelProblemLevelChanges.clear();
	}

	public void processJavaElementChange(final JavaElementDelta delta, final IProgressMonitor progressMonitor) throws CoreException {
		metamodel.processJavaElementChange(delta, progressMonitor);
		
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
		buildProject();		
		return this.metamodel;
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
	public JaxrsJavaApplication createJavaApplication(final String typeName, final String applicationPath) throws CoreException {
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
	public JaxrsWebxmlApplication createWebxmlApplication(final String applicationClassName,
			final String applicationPath) throws CoreException, IOException {
		final IResource webDeploymentDescriptor = WtpUtils.getWebDeploymentDescriptor(metamodel.getProject());
		ResourcesUtils.replaceContent(webDeploymentDescriptor, "javax.ws.rs.core.Application", applicationClassName);
		ResourcesUtils.replaceContent(webDeploymentDescriptor, "/hello/*", applicationPath);
		return JaxrsWebxmlApplication.from(webDeploymentDescriptor).inMetamodel(metamodel).build();
	}

	/**
	 * @return
	 * @throws CoreException
	 * @throws JavaModelException
	 */
	public JaxrsHttpMethod createHttpMethod(final EnumJaxrsClassname httpMethodElement) throws CoreException,
			JavaModelException {
		return createHttpMethod(httpMethodElement.qualifiedName);
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
		final Annotation httpMethodAnnotation = httpMethod.getAnnotation(HTTP_METHOD.qualifiedName);
		httpMethod.addOrUpdateAnnotation(createAnnotation(httpMethodAnnotation, httpVerb));
		return httpMethod;
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
		return JaxrsResource.from(type, metamodel.findAllHttpMethods()).withMetamodel(metamodel).build();
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
				resource.removeMethod(resourceMethod);
				break;
			}
		}
	}

	public JaxrsResourceMethod resolveResourceMethod(final JaxrsResource resource, final String methodName) {
		for(IJaxrsResourceMethod resourceMethod : resource.getAllMethods()) {
			if(resourceMethod.getJavaElement().getElementName().equals(methodName)) {
				return (JaxrsResourceMethod) resourceMethod;
			}
		}
		return null;
	}

}
