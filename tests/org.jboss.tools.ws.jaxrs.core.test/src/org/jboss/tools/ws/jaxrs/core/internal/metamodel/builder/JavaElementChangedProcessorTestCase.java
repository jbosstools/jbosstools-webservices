package org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder;

import static org.eclipse.jdt.core.IJavaElementDelta.ADDED;
import static org.eclipse.jdt.core.IJavaElementDelta.CHANGED;
import static org.eclipse.jdt.core.IJavaElementDelta.REMOVED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.jboss.tools.ws.jaxrs.core.WorkbenchUtils.getAnnotation;
import static org.jboss.tools.ws.jaxrs.core.WorkbenchUtils.getMethod;
import static org.jboss.tools.ws.jaxrs.core.WorkbenchUtils.getType;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.IJavaElementDeltaFlag.F_SIGNATURE;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementChangedEvent.F_CONSUMED_MEDIATYPES_VALUE;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementChangedEvent.F_ELEMENT_KIND;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementChangedEvent.F_HTTP_METHOD_VALUE;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementChangedEvent.F_METHOD_PARAMETERS;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementChangedEvent.F_METHOD_RETURN_TYPE;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementChangedEvent.F_PATH_VALUE;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementChangedEvent.F_PRODUCED_MEDIATYPES_VALUE;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.hamcrest.Matchers;
import org.jboss.tools.ws.jaxrs.core.AbstractCommonTestCase;
import org.jboss.tools.ws.jaxrs.core.JBossJaxrsCorePlugin;
import org.jboss.tools.ws.jaxrs.core.WorkbenchUtils;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsParamField;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.JavaMethodParameter;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsResource;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsResourceMethod;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class JavaElementChangedProcessorTestCase extends AbstractCommonTestCase {

	private JaxrsMetamodel metamodel;

	private final JavaElementChangedProcessor delegate = new JavaElementChangedProcessor();

	private static final IProgressMonitor progressMonitor = new NullProgressMonitor();

	@Before
	public void setup() throws CoreException {
		JBossJaxrsCorePlugin.getDefault().unregisterListeners();
		// metamodel = Mockito.mock(JaxrsMetamodel.class);
		// in case an element was attempted to be removed, some impact would be
		// retrieved
		// when(metamodel.remove(any(IJaxrsElement.class))).thenReturn(true);
		metamodel = spy(JaxrsMetamodel.create(javaProject));
		// replace the normal metamodel instance with the one spied by Mockito
		javaProject.getProject().setSessionProperty(JaxrsMetamodel.METAMODEL_QUALIFIED_NAME, metamodel);
	}

	/** @return
	 * @throws CoreException
	 * @throws JavaModelException */
	private JaxrsHttpMethod createHttpMethod(Class<?> httpClass) throws CoreException, JavaModelException {
		final IType httpMethodType = JdtUtils.resolveType(httpClass.getName(), javaProject, progressMonitor);
		final Annotation httpMethodAnnotation = getAnnotation(httpMethodType, HttpMethod.class);
		final JaxrsHttpMethod httpMethod = new JaxrsHttpMethod(httpMethodType, httpMethodAnnotation, metamodel);
		return httpMethod;
	}

	
	private Annotation createAnnotation(IAnnotation annotation, String name) {
		return createAnnotation(annotation, name, null);
	}

	private Annotation createAnnotation(Class<?> clazz, String value) {
		return createAnnotation(null, clazz.getName(), value);
	}
	
	private Annotation createAnnotation(IAnnotation annotation, String name, String value) {
		Map<String, List<String>> values = new HashMap<String, List<String>>();
		values.put("value", Arrays.asList(value));
		return new Annotation(annotation, name, values, null);
	}
	private List<JaxrsElementChangedEvent> processEvent(JavaElementChangedEvent event, IProgressMonitor progressmonitor) {
		return delegate.processEvents(Arrays.asList(event), progressmonitor);
	}

	private static JavaElementChangedEvent createEvent(Annotation annotation, int deltaKind) throws JavaModelException {
		return new JavaElementChangedEvent(annotation.getJavaAnnotation(), deltaKind, JdtUtils.parse(
				((IMember) annotation.getJavaParent()), progressMonitor), new int[0]);
	}

	private static JavaElementChangedEvent createEvent(IMember element, int deltaKind) throws JavaModelException {
		return createEvent(element, deltaKind, new int[0]);
	}

	private static JavaElementChangedEvent createEvent(IMember element, int deltaKind, int... flags)
			throws JavaModelException {
		return new JavaElementChangedEvent(element, deltaKind, JdtUtils.parse(element, progressMonitor), flags);
	}

	private static JavaElementChangedEvent createEvent(ICompilationUnit element, int deltaKind)
			throws JavaModelException {
		return new JavaElementChangedEvent(element, deltaKind, JdtUtils.parse(element, progressMonitor), new int[0]);
	}

	private static JavaElementChangedEvent createEvent(IPackageFragmentRoot element, int deltaKind)
			throws JavaModelException {
		return new JavaElementChangedEvent(element, deltaKind, null, new int[0]);
	}

	public static List<JavaElementChangedEvent> asList(JavaElementChangedEvent event) {
		return Arrays.asList(new JavaElementChangedEvent[] { event });
	}

	/** Because sometimes, generics are painful...
	 * 
	 * @param elements
	 * @return private List<IJaxrsElement<?>> asList(IJaxrsElement<?>...
	 *         elements) { final List<IJaxrsElement<?>> result = new
	 *         ArrayList<IJaxrsElement<?>>();
	 *         result.addAll(Arrays.asList(elements)); return result; } */

	@Test
	public void shouldAdd1HttpMethodAnd3ResourcesWhenAddingSourceFolder() throws CoreException {
		// pre-conditions
		createHttpMethod(GET.class);
		final IPackageFragmentRoot sourceFolder = WorkbenchUtils.getPackageFragmentRoot(javaProject, "src/main/java",
				progressMonitor);
		// operation
		final JavaElementChangedEvent event = createEvent(sourceFolder, ADDED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(6)); // 1 HttpMethod + 3
												// RootResources + 2
												// Subresources
		assertThat(impacts, everyItem(Matchers.<JaxrsElementChangedEvent> hasProperty("deltaKind", equalTo(ADDED))));
	}

	@Test
	public void shouldAdd6HttpMethodsAnd0ResourceWhenAddingBinaryLib() throws CoreException {
		// pre-conditions
		final IPackageFragmentRoot lib = WorkbenchUtils.getPackageFragmentRoot(javaProject,
				"lib/jaxrs-api-2.0.1.GA.jar", progressMonitor);
		// operation
		final JavaElementChangedEvent event = createEvent(lib, ADDED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications. Damned : none in the jar...
		assertThat(impacts.size(), equalTo(6));
		assertThat(impacts, everyItem(Matchers.<JaxrsElementChangedEvent> hasProperty("deltaKind", equalTo(ADDED))));
		verify(metamodel, times(6)).add(any(IJaxrsHttpMethod.class));
	}

	@Test
	public void shouldAddHttpMethodWhenAddingSourceCompilationUnit() throws CoreException {
		// pre-conditions
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject,
				progressMonitor);
		// operation
		final JavaElementChangedEvent event = createEvent(type.getCompilationUnit(), ADDED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((IJaxrsHttpMethod) impacts.get(0).getElement()).getHttpVerb(), equalTo("FOO"));
		verify(metamodel, times(1)).add(any(IJaxrsHttpMethod.class));
	}

	@Test
	public void shouldAddHttpMethodWhenAddingSourceType() throws CoreException {
		// pre-conditions
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject,
				progressMonitor);
		// operation
		final JavaElementChangedEvent event = createEvent(type, ADDED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((IJaxrsHttpMethod) impacts.get(0).getElement()).getHttpVerb(), equalTo("FOO"));
		verify(metamodel, times(1)).add(any(IJaxrsHttpMethod.class));
	}

	@Test
	public void shouldAddHttpMethodWhenAddingHttpMethodAnnotation() throws CoreException {
		// pre-conditions
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject,
				progressMonitor);
		final Annotation annotation = getAnnotation(type, HttpMethod.class);
		// operation
		final JavaElementChangedEvent event = createEvent(annotation, ADDED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((IJaxrsHttpMethod) impacts.get(0).getElement()).getHttpVerb(), equalTo("FOO"));
		verify(metamodel, times(1)).add(any(IJaxrsHttpMethod.class));
	}

	@Test
	public void shouldDoNothingWhenAddingAnotherAnnotation() throws CoreException {
		// pre-conditions
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject,
				progressMonitor);
		final Annotation annotation = getAnnotation(type, Target.class);
		// operation
		final JavaElementChangedEvent event = createEvent(annotation, ADDED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(0));
		verify(metamodel, times(0)).add(any(IJaxrsHttpMethod.class));
	}

	@Test
	public void shouldUpdateHttpMethodWhenChangingAnnotationValue() throws CoreException {
		// pre-conditions
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject,
				progressMonitor);
		final Annotation annotation = getAnnotation(type, HttpMethod.class, "BAR");
		final IJaxrsHttpMethod httpMethod = new JaxrsHttpMethod(type, annotation, metamodel);
		metamodel.add(httpMethod);
		// operation
		final JavaElementChangedEvent event = createEvent(annotation, CHANGED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(((IJaxrsHttpMethod) impacts.get(0).getElement()).getHttpVerb(), equalTo("FOO"));
	}

	@Test
	public void shouldDoNothingWhenAnnotationValueRemainsSameOnHttpMethod() throws CoreException {
		// pre-conditions
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject,
				progressMonitor);
		final Annotation annotation = getAnnotation(type, HttpMethod.class);
		final IJaxrsHttpMethod httpMethod = new JaxrsHttpMethod(type, annotation, metamodel);
		metamodel.add(httpMethod);
		// operation
		final JavaElementChangedEvent event = createEvent(annotation, CHANGED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(0));
	}

	@Test
	public void shouldDoNothingWhenChangingAnotherAnnotationValue() throws CoreException {
		// pre-conditions
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject,
				progressMonitor);
		final Annotation httpMethodAnnotation = getAnnotation(type, HttpMethod.class);
		final IJaxrsHttpMethod httpMethod = new JaxrsHttpMethod(type, httpMethodAnnotation, metamodel);
		metamodel.add(httpMethod);
		final Annotation targetAnnotation = getAnnotation(type, Target.class);
		// operation
		final JavaElementChangedEvent event = createEvent(targetAnnotation, CHANGED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(0));
	}

	@Test
	public void shouldRemoveHttpMethodWhenRemovingCompilationUnit() throws CoreException {
		// pre-conditions
		// JaxrsMetamodel metamodel = new JaxrsMetamodel(javaProject);
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject,
				progressMonitor);
		final Annotation annotation = getAnnotation(type, HttpMethod.class);
		final IJaxrsHttpMethod httpMethod = new JaxrsHttpMethod(type, annotation, metamodel);
		metamodel.add(httpMethod);
		// operation
		final JavaElementChangedEvent event = createEvent(type.getCompilationUnit(), REMOVED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((IJaxrsHttpMethod) impacts.get(0).getElement()), equalTo(httpMethod));
	}

	@Test
	public void shouldRemoveHttpMethodWhenRemovingSourceType() throws CoreException {
		// pre-conditions
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject,
				progressMonitor);
		final Annotation annotation = getAnnotation(type, HttpMethod.class);
		final IJaxrsHttpMethod httpMethod = new JaxrsHttpMethod(type, annotation, metamodel);
		metamodel.add(httpMethod);
		// operation
		final JavaElementChangedEvent event = createEvent(type, REMOVED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((IJaxrsHttpMethod) impacts.get(0).getElement()).getHttpVerb(), equalTo("FOO"));
	}

	@Test
	public void shouldRemoveHttpMethodWhenRemovingAnnotation() throws CoreException {
		// pre-conditions
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject,
				progressMonitor);
		final Annotation annotation = getAnnotation(type, HttpMethod.class);
		final JaxrsHttpMethod httpMethod = new JaxrsHttpMethod(type, annotation, metamodel);
		metamodel.add(httpMethod);
		// operation
		final JavaElementChangedEvent event = createEvent(annotation, REMOVED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(impacts.get(0).getElement(), is(notNullValue()));
	}

	@Test
	public void shouldDoNothingWhenRemovingAnotherAnnotationOnHttpMethod() throws CoreException {
		// pre-conditions
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject,
				progressMonitor);
		final JaxrsHttpMethod httpMethod = new JaxrsHttpMethod(type, getAnnotation(type, HttpMethod.class), metamodel);
		metamodel.add(httpMethod);
		// operation
		final JavaElementChangedEvent event = createEvent(getAnnotation(type, Target.class), REMOVED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(0));
	}

	@Test
	public void shouldRemoveHttpMethodWhenRemovingSourceFolder() throws CoreException {
		// pre-conditions
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject,
				progressMonitor);
		final Annotation annotation = getAnnotation(type, HttpMethod.class);
		final JaxrsHttpMethod httpMethod = new JaxrsHttpMethod(type, annotation, metamodel);
		metamodel.add(httpMethod);
		final IPackageFragmentRoot sourceFolder = WorkbenchUtils.getPackageFragmentRoot(javaProject, "src/main/java",
				progressMonitor);
		// operation
		final JavaElementChangedEvent event = createEvent(sourceFolder, REMOVED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getElement(), is(notNullValue()));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
	}

	@Test
	public void shouldRemoveHttpMethodWhenRemovingBinaryLib() throws CoreException {
		// pre-conditions
		final IPackageFragmentRoot lib = WorkbenchUtils.getPackageFragmentRoot(javaProject,
				"lib/jaxrs-api-2.0.1.GA.jar", progressMonitor);
		// let's suppose that this jar only contains 1 HTTP Methods ;-)
		final IType type = JdtUtils.resolveType("javax.ws.rs.GET", javaProject, progressMonitor);
		final Annotation annotation = getAnnotation(type, HttpMethod.class);
		final JaxrsHttpMethod httpMethod = new JaxrsHttpMethod(type, annotation, metamodel);
		metamodel.add(httpMethod);
		// operation
		final JavaElementChangedEvent event = createEvent(lib, REMOVED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts, everyItem(Matchers.<JaxrsElementChangedEvent> hasProperty("deltaKind", equalTo(REMOVED))));
	}

	@Test
	public void shouldAddResourceWhenAddingSourceCompilationUnit() throws CoreException {
		// pre-conditions
		IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		// operation
		final JavaElementChangedEvent event = createEvent(type.getCompilationUnit(), ADDED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((IJaxrsResource) impacts.get(0).getElement()).getPathTemplate(), equalTo("/customers"));
	}

	@Test
	public void shouldAddSubresourceWhenAddingSourceCompilationUnit() throws CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = createHttpMethod(GET.class);
		metamodel.add(httpMethod);
		IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.BookResource", javaProject);
		// operation
		final JavaElementChangedEvent event = createEvent(type.getCompilationUnit(), ADDED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((IJaxrsResource) impacts.get(0).getElement()).getAllMethods().size(), equalTo(2));
		assertThat(((IJaxrsResource) impacts.get(0).getElement()).getPathTemplate(), nullValue());
	}

	@Test
	public void shouldAddSubresourceLocatorWhenAddingSourceCompilationUnit() throws CoreException {
		// pre-conditions
		IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator", javaProject);
		// operation
		final JavaElementChangedEvent event = createEvent(type.getCompilationUnit(), ADDED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((IJaxrsResource) impacts.get(0).getElement()).getAllMethods().size(), equalTo(1));
		assertThat(((IJaxrsResource) impacts.get(0).getElement()).getPathTemplate(), equalTo("/products"));
	}

	@Test
	public void shouldAddResourceWhenAddingSourceType() throws CoreException {
		// pre-conditions
		IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		// operation
		final JavaElementChangedEvent event = createEvent(type, ADDED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((IJaxrsResource) impacts.get(0).getElement()).getPathTemplate(), equalTo("/customers"));
		verify(metamodel, times(5)).add(any(IJaxrsResource.class)); // includes
																	// resource
																	// methods..
	}

	@Test
	public void shouldAddResourceWhenAddingPathAnnotation() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final Annotation annotation = getAnnotation(type, Path.class);
		// operation
		final JavaElementChangedEvent event = createEvent(annotation, ADDED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(ADDED));
		final IJaxrsResource resource = (IJaxrsResource) impacts.get(0).getElement();
		assertThat(resource.getPathTemplate(), equalTo("/customers"));
		assertThat(resource.getConsumedMediaTypes(), equalTo(Arrays.asList(MediaType.APPLICATION_XML)));
		assertThat(resource.getProducedMediaTypes(),
				equalTo(Arrays.asList(MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON)));
		verify(metamodel, times(5)).add(any(IJaxrsResource.class)); // includes
																	// resource
																	// methods...
	}

	@Test
	public void shouldBecomeRootResourceWhenAddingPathAnnotation() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final Annotation annotation = getAnnotation(type, Path.class);
		final IJaxrsResource resource = new JaxrsResource.Builder(type, metamodel).build();
		metamodel.add(resource);
		// operation
		final JavaElementChangedEvent event = createEvent(annotation, ADDED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_ELEMENT_KIND + F_PATH_VALUE));
	}

	@Test
	public void shouldNotAddResourceWhenAddingAnotherAnnotation() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final Annotation annotation = getAnnotation(type, Consumes.class);
		// operation
		final JavaElementChangedEvent event = createEvent(annotation, ADDED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(0));
		verify(metamodel, times(0)).add(any(IJaxrsResource.class));
	}

	@Test
	public void shouldUpdateResourceWhenChangingPathAnnotationValue() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final Annotation annotation = getAnnotation(type, Path.class, "/bar");
		final IJaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(annotation).build();
		metamodel.add(resource);
		// operation
		final JavaElementChangedEvent event = createEvent(annotation, CHANGED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_PATH_VALUE));
		assertThat((IJaxrsResource) impacts.get(0).getElement(), equalTo(resource));
	}

	@Test
	public void shouldUpdateResourceWhenAddingConsumesAnnotation() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final Annotation annotation = getAnnotation(type, Consumes.class);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		// operation
		final JavaElementChangedEvent event = createEvent(annotation, ADDED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_CONSUMED_MEDIATYPES_VALUE));
	}

	@Test
	public void shouldUpdateResourceWhenChangingConsumesAnnotationValue() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final Annotation consumesAnnotation = getAnnotation(type, Consumes.class, "application/foo");
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation)
				.consumes(consumesAnnotation).build();
		metamodel.add(resource);
		// operation
		final JavaElementChangedEvent event = createEvent(consumesAnnotation, CHANGED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_CONSUMED_MEDIATYPES_VALUE));
	}

	@Test
	public void shouldUpdateResourceWhenRemovingConsumesAnnotation() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final Annotation consumesAnnotation = getAnnotation(type, Consumes.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation)
				.consumes(consumesAnnotation).build();
		metamodel.add(resource);
		// operation
		final JavaElementChangedEvent event = createEvent(consumesAnnotation, REMOVED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_CONSUMED_MEDIATYPES_VALUE));
	}

	@Test
	public void shouldUpdateResourceWhenAddingProducesAnnotation() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		final Annotation producesAnnotation = getAnnotation(type, Produces.class);
		// operation
		final JavaElementChangedEvent event = createEvent(producesAnnotation, ADDED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_PRODUCED_MEDIATYPES_VALUE));
	}

	@Test
	public void shouldUpdateResourceWhenChangingProducesAnnotationValue() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final Annotation annotation = getAnnotation(type, Produces.class, "application/foo");
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation)
				.produces(annotation).build();
		metamodel.add(resource);
		// operation
		final JavaElementChangedEvent event = createEvent(annotation, CHANGED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_PRODUCED_MEDIATYPES_VALUE));
	}

	@Test
	public void shouldUpdateResourceWhenRemovingProducesAnnotation() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final Annotation annotation = getAnnotation(type, Produces.class);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation)
				.produces(annotation).build();
		metamodel.add(resource);
		// operation
		final JavaElementChangedEvent event = createEvent(annotation, REMOVED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_PRODUCED_MEDIATYPES_VALUE));
	}

	@Test
	public void shouldAddResourceFieldWhenAddingPathParamAnnotationOnField() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		final Annotation annotation = getAnnotation(type.getField("productType"), PathParam.class);
		// operation
		final JavaElementChangedEvent event = createEvent(annotation, ADDED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(ADDED));
	}

	@Test
	@Ignore
	public void shouldAddResourceFieldWhenAddingImportPathParam() throws CoreException {
		/*
		 * // pre-conditions final IType type =
		 * getType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator"
		 * ); final JaxrsResource resource = new JaxrsResource.Builder(type,
		 * metamodel).build(); //metamodel.add(resource); final
		 * IImportDeclaration importDeclaration = getImportDeclaration(type,
		 * PathParam.class.getName()); // operation final
		 * JavaElementChangedEvent event = createEvent(importDeclaration,
		 * ADDED); final List<JaxrsElementChangedEvent> impacts =
		 * processEvent(event, progressMonitor); // verifications
		 * assertThat(impacts.size(), equalTo(1));
		 * assertThat(impacts.get(0).getDeltaKind(), equalTo(ADDED));
		 */
	}

	@Test
	public void shouldAddResourceFieldWhenAddingQueryParamAnnotationOnField() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		final Annotation annotation = getAnnotation(type.getField("foo"), QueryParam.class);
		// operation
		final JavaElementChangedEvent event = createEvent(annotation, ADDED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(ADDED));
	}

	@Test
	public void shouldAddResourceFieldWhenAddingMatrixParamAnnotationOnField() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		final Annotation annotation = getAnnotation(type.getField("bar"), MatrixParam.class);
		// operation
		final JavaElementChangedEvent event = createEvent(annotation, ADDED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(ADDED));
	}

	@Test
	public void shouldAddResourceFieldWhenAddingFieldAnnotatedWithPathParam() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		final IField field = type.getField("productType");
		// operation
		final JavaElementChangedEvent event = createEvent(field, ADDED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(ADDED));
	}

	@Test
	public void shouldAddResourceFieldWhenAddingFieldAnnotatedWithQueryParam() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		final IField field = type.getField("foo");
		// operation
		final JavaElementChangedEvent event = createEvent(field, ADDED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(ADDED));
	}

	@Test
	public void shouldAddResourceFieldWhenAddingFieldAnnotatedWithMatrixParam() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		final IField field = type.getField("bar");
		// operation
		final JavaElementChangedEvent event = createEvent(field, ADDED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(ADDED));
	}

	@Test
	public void shouldDoNothingWhenAddingFieldWithAnyAnnotation() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		final IField field = type.getField("entityManager");
		// operation
		final JavaElementChangedEvent event = createEvent(field, ADDED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(0));
	}

	@Test
	public void shouldDoNothingWhenAddingAnotherAnnotationOnField() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		final IField field = type.getField("entityManager");
		// operation
		final JavaElementChangedEvent event = createEvent(field, ADDED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(0));
	}

	@Test
	public void shouldUpdateResourceFieldWhenChangingPathParamAnnotationValueOnField() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		final IField field = type.getField("productType");
		final Annotation annotation = getAnnotation(field, PathParam.class, "foo");
		final JaxrsParamField resourceField = new JaxrsParamField(field, annotation, resource, metamodel);
		metamodel.add(resourceField);
		// operation
		final JavaElementChangedEvent event = createEvent(annotation, CHANGED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
	}

	@Test
	public void shouldUpdateResourceFieldWhenChangingQueryParamAnnotationValueOnField() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		final IField field = type.getField("foo");
		final Annotation annotation = getAnnotation(field, QueryParam.class, "foo!");
		final JaxrsParamField resourceField = new JaxrsParamField(field, annotation, resource, metamodel);
		metamodel.add(resourceField);
		// operation
		final JavaElementChangedEvent event = createEvent(annotation, CHANGED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
	}

	@Test
	public void shouldUpdateResourceFieldWhenChangingMatrixParamAnnotationValueOnField() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		final IField field = type.getField("bar");
		final Annotation annotation = getAnnotation(field, MatrixParam.class, "bar!");
		final JaxrsParamField resourceField = new JaxrsParamField(field, annotation, resource, metamodel);
		metamodel.add(resourceField);
		// operation
		final JavaElementChangedEvent event = createEvent(annotation, CHANGED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
	}

	@Test
	public void shouldDoNothingWhenChangingAnotherAnnotationValueOnField() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		final IField field = type.getField("foo");
		final Annotation annotation = getAnnotation(field, DefaultValue.class);
		final JaxrsParamField resourceField = new JaxrsParamField(field, annotation, resource, metamodel);
		metamodel.add(resourceField);
		// operation
		final JavaElementChangedEvent event = createEvent(annotation, CHANGED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(0));
	}

	@Test
	public void shouldRemoveResourceFieldWhenRemovingPathParamAnnotatedOnField() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		final IField field = type.getField("productType");
		final Annotation annotation = getAnnotation(field, PathParam.class);
		final JaxrsParamField resourceField = new JaxrsParamField(field, annotation, resource, metamodel);
		metamodel.add(resourceField);
		// operation
		final JavaElementChangedEvent event = createEvent(annotation, REMOVED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
	}

	@Test
	public void shouldRemoveResourceFieldWhenRemovingQueryParamAnnotationOnField() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		final IField field = type.getField("foo");
		final Annotation annotation = getAnnotation(field, QueryParam.class);
		final JaxrsParamField resourceField = new JaxrsParamField(field, annotation, resource, metamodel);
		metamodel.add(resourceField);
		// operation
		final JavaElementChangedEvent event = createEvent(annotation, REMOVED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
	}

	@Test
	public void shouldRemoveResourceFieldWhenRemovingMatrixParamAnnotationOnField() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		final IField field = type.getField("bar");
		final Annotation annotation = getAnnotation(field, MatrixParam.class);
		final JaxrsParamField resourceField = new JaxrsParamField(field, annotation, resource, metamodel);
		metamodel.add(resourceField);
		// operation
		final JavaElementChangedEvent event = createEvent(annotation, REMOVED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
	}

	@Test
	public void shouldRemoveResourceFieldWhenRemovingFieldAnnotatedWithQueryParam() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		IField field = type.getField("foo");
		final Annotation annotation = getAnnotation(field, QueryParam.class);
		final JaxrsParamField resourceField = new JaxrsParamField(field, annotation, resource, metamodel);
		metamodel.add(resourceField);
		// operation
		final JavaElementChangedEvent event = createEvent(field, REMOVED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
	}

	@Test
	public void shouldRemoveResourceFieldWhenRemovingFieldAnnotatedWithPathParam() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		final IField field = type.getField("productType");
		final Annotation annotation = getAnnotation(field, PathParam.class);
		final JaxrsParamField resourceField = new JaxrsParamField(field, annotation, resource, metamodel);
		metamodel.add(resourceField);
		// operation
		final JavaElementChangedEvent event = createEvent(field, REMOVED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));

	}

	@Test
	public void shouldRemoveResourceFieldWhenRemovingFieldAnnotatedWithMatrixParam() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		final IField field = type.getField("bar");
		final Annotation annotation = getAnnotation(field, MatrixParam.class);
		final JaxrsParamField resourceField = new JaxrsParamField(field, annotation, resource, metamodel);
		metamodel.add(resourceField);
		// operation
		final JavaElementChangedEvent event = createEvent(field, REMOVED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
	}

	@Test
	public void shouldDoNothingWhenRemovingAnotherAnnotationOnField() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		final IField field = type.getField("entityManager");
		// operation
		final JavaElementChangedEvent event = createEvent(field, REMOVED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(0));
	}

	@Test
	public void shouldDoNothingWhenAnnotationValueRemainsSameOnResource() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final Annotation annotation = JdtUtils.resolveAnnotation(type, JdtUtils.parse(type, progressMonitor),
				Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(annotation).build();
		metamodel.add(resource);
		// operation
		final JavaElementChangedEvent event = createEvent(annotation, CHANGED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(0));
	}

	@Test
	public void shouldDoNothingResourceWhenChangingAnotherAnnotationValue() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		final Annotation annotation = getAnnotation(type, Consumes.class);
		// operation
		final JavaElementChangedEvent event = createEvent(annotation, CHANGED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(0));
	}

	@Test
	public void shouldRemoveResourceWhenRemovingCompilationUnit() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final IJaxrsResource resource = new JaxrsResource.Builder(type, metamodel).build();
		metamodel.add(resource);
		// operation
		final JavaElementChangedEvent event = createEvent(type.getCompilationUnit(), REMOVED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((IJaxrsResource) impacts.get(0).getElement()), equalTo(resource));
		verify(metamodel, times(1)).remove(any(IJaxrsResource.class));
	}

	@Test
	public void shouldRemoveResourceWhenRemovingSourceType() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final IJaxrsResource resource = new JaxrsResource.Builder(type, metamodel).build();
		metamodel.add(resource);
		// operation
		final JavaElementChangedEvent event = createEvent(type, REMOVED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((IJaxrsResource) impacts.get(0).getElement()), equalTo(resource));
		verify(metamodel, times(1)).remove(any(IJaxrsResource.class));
	}

	@Test
	public void shouldRemoveResourceWhenRemovingAnnotation() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final Annotation annotation = getAnnotation(type, Path.class);
		final IJaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(annotation).build();
		metamodel.add(resource);
		for (IJaxrsResourceMethod resourceMethod : resource.getAllMethods()) {
			metamodel.remove(resourceMethod);
		}
		// operation
		final JavaElementChangedEvent event = createEvent(annotation, REMOVED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications : resource removed, since it has no field nor method
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((IJaxrsResource) impacts.get(0).getElement()), equalTo(resource));
		verify(metamodel, times(1)).remove(any(IJaxrsResource.class));
	}

	@Test
	public void shouldRemoveResourceMethodWhenRemovingJavaMethod() throws CoreException {
		// JAX-RS HttpMethod
		final JaxrsHttpMethod httpMethod = createHttpMethod(POST.class);
		metamodel.add(httpMethod);
		// Parent JAX-RS Resource
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		// JAX-RS Resource Method
		final IMethod method = getMethod(type, "createCustomer");
		final Annotation annotation = getAnnotation(method, POST.class);
		final IJaxrsResourceMethod resourceMethod = new JaxrsResourceMethod.Builder(method, resource, metamodel)
				.httpMethod(annotation).build();
		metamodel.add(resourceMethod);
		// operation
		final JavaElementChangedEvent event = createEvent(method, REMOVED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications : resource removed, since it has no field nor method
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
	}

	@Test
	public void shouldBecomeSubresourceWhenRemovingPathAnnotation() throws CoreException {
		// pre-conditions
		// JAX-RS HttpMethod
		final JaxrsHttpMethod httpMethod = createHttpMethod(POST.class);
		metamodel.add(httpMethod);
		// JAX-RS Resource
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		// JAX-RS Resource Method
		final IMethod method = getMethod(type, "createCustomer");
		final Annotation httpMethodAnnotation = getAnnotation(method, POST.class);
		final IJaxrsResourceMethod resourceMethod = new JaxrsResourceMethod.Builder(method, resource, metamodel)
				.httpMethod(httpMethodAnnotation).build();
		metamodel.add(resourceMethod);
		// operation
		final JavaElementChangedEvent event = createEvent(pathAnnotation, REMOVED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_ELEMENT_KIND + F_PATH_VALUE));
	}

	@Test
	public void shouldDoNothingWhenRemovingAnotherAnnotationOnResource() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final IJaxrsResource resource = new JaxrsResource.Builder(type, metamodel).build();
		metamodel.add(resource);
		// in case it would be removed
		final Annotation annotation = JdtUtils.resolveAnnotation(type, JdtUtils.parse(type, progressMonitor),
				Encoded.class);
		// operation
		final JavaElementChangedEvent event = createEvent(annotation, REMOVED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(0));
	}

	@Test
	public void shouldRemoveResourceWhenRemovingSourceFolder() throws CoreException {
		// pre-conditions
		final IPackageFragmentRoot sourceFolder = WorkbenchUtils.getPackageFragmentRoot(javaProject, "src/main/java",
				progressMonitor);
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final Annotation annotation = getAnnotation(type, Consumes.class);
		final IJaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(annotation).build();
		metamodel.add(resource);
		// operation
		final JavaElementChangedEvent event = createEvent(sourceFolder, REMOVED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		verify(metamodel, times(1)).remove(any(IJaxrsResource.class));
	}

	@Test
	@Ignore
	public void shouldRemoveResourceWhenRemovingBinaryLib() throws CoreException {
		// need to package a JAX-RS resource into a jar...
	}

	@Test
	public void shouldAddResourceMethodWhenAddingHttpMethodAnnotation() throws CoreException {
		// pre-conditions
		// JAX-RS HttpMethod
		metamodel.add(createHttpMethod(POST.class));
		// Parent JAX-RS Resource
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		final IMethod method = getMethod(type, "createCustomer");
		final Annotation annotation = getAnnotation(method, POST.class);
		// operation
		final JavaElementChangedEvent event = createEvent(annotation, ADDED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(ADDED));
	}

	@Test
	public void shouldAddResourceMethodWhenAddingAnnotatedMethod() throws CoreException {
		// pre-conditions
		// JAX-RS HttpMethod
		metamodel.add(createHttpMethod(POST.class));
		// Parent JAX-RS Resource
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		final IMethod method = getMethod(type, "createCustomer");
		// operation
		final JavaElementChangedEvent event = createEvent(method, ADDED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(ADDED));
	}

	@Test
	public void shouldAddSubresourceLocatorWhenAddingPathAnnotation() throws CoreException {
		// pre-conditions
		// Parent JAX-RS Resource
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		final IMethod method = getMethod(type, "getProductResourceLocator");
		final Annotation annotation = getAnnotation(method, Path.class);
		// operation
		final JavaElementChangedEvent event = createEvent(annotation, ADDED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(ADDED));
	}

	@Test
	public void shouldRemoveResourceMethodWhenRemovingHttpMethodAnnotation() throws CoreException {
		// pre-conditions
		// JAX-RS HttpMethod
		final JaxrsHttpMethod httpMethod = createHttpMethod(POST.class);
		metamodel.add(httpMethod);
		// Parent JAX-RS Resource
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		// JAX-RS Resource Method
		final IMethod method = getMethod(type, "createCustomer");
		final Annotation annotation = getAnnotation(method, POST.class);
		final IJaxrsResourceMethod resourceMethod = new JaxrsResourceMethod.Builder(method, resource, metamodel)
				.httpMethod(annotation).build();
		metamodel.add(resourceMethod);
		// operation
		final JavaElementChangedEvent event = createEvent(annotation, REMOVED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		verify(metamodel, times(1)).remove(any(IJaxrsResourceMethod.class));
	}

	@Test
	public void shouldConvertIntoSubresourceMethodWhenAddingPathAnnotation() throws CoreException {
		final JaxrsHttpMethod httpMethod = createHttpMethod(GET.class);
		metamodel.add(httpMethod);
		// Parent JAX-RS Resource
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(
				getAnnotation(type, Path.class)).build();
		metamodel.add(resource);
		// JAX-RS Resource Method
		final IMethod method = getMethod(type, "getCustomer");
		final Annotation httpAnnotation = getAnnotation(method, GET.class);
		final IJaxrsResourceMethod resourceMethod = new JaxrsResourceMethod.Builder(method, resource, metamodel)
				.httpMethod(httpAnnotation).build();
		metamodel.add(resourceMethod);
		final Annotation pathAnnotation = getAnnotation(method, Path.class);
		// operation
		final JavaElementChangedEvent event = createEvent(pathAnnotation, ADDED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_PATH_VALUE + F_ELEMENT_KIND));
	}

	@Test
	public void shouldConvertIntoResourceMethodWhenRemovingPathAnnotation() throws CoreException {
		// pre-conditions
		// JAX-RS HttpMethod
		final JaxrsHttpMethod httpMethod = createHttpMethod(GET.class);
		metamodel.add(httpMethod);
		// Parent JAX-RS Resource
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(
				getAnnotation(type, Path.class)).build();
		metamodel.add(resource);
		// JAX-RS Resource Method
		final IMethod method = getMethod(type, "getCustomer");
		final Annotation httpAnnotation = getAnnotation(method, GET.class);
		final Annotation pathAnnotation = getAnnotation(method, Path.class);
		final IJaxrsResourceMethod resourceMethod = new JaxrsResourceMethod.Builder(method, resource, metamodel)
				.httpMethod(httpAnnotation).pathTemplate(pathAnnotation).build();
		metamodel.add(resourceMethod);
		// operation
		final JavaElementChangedEvent event = createEvent(pathAnnotation, REMOVED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_PATH_VALUE + F_ELEMENT_KIND));
	}

	@Test
	public void shouldConvertIntoSubresourceLocatorWhenRemovingHttpMethodAnnotation() throws CoreException {
		// pre-conditions
		// JAX-RS HttpMethod
		final JaxrsHttpMethod httpMethod = createHttpMethod(GET.class);
		metamodel.add(httpMethod);
		// Parent JAX-RS Resource
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(
				getAnnotation(type, Path.class)).build();
		metamodel.add(resource);
		// JAX-RS Resource Method
		final IMethod method = getMethod(type, "getCustomer");
		final Annotation pathAnnotation = getAnnotation(method, Path.class);
		final Annotation httpAnnotation = getAnnotation(method, GET.class);
		final IJaxrsResourceMethod resourceMethod = new JaxrsResourceMethod.Builder(method, resource, metamodel)
				.httpMethod(httpAnnotation).pathTemplate(pathAnnotation).build();
		metamodel.add(resourceMethod);
		// operation
		final JavaElementChangedEvent event = createEvent(httpAnnotation, REMOVED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_HTTP_METHOD_VALUE + F_ELEMENT_KIND));
	}

	@Test
	public void shouldRemoveSubresourceLocatorWhenRemovingPathAnnotation() throws CoreException {
		// pre-conditions
		// Parent JAX-RS Resource
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator", javaProject);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(
				getAnnotation(type, Path.class)).build();
		metamodel.add(resource);
		// JAX-RS Resource Method
		final IMethod method = getMethod(type, "getProductResourceLocator");
		final Annotation annotation = getAnnotation(method, Path.class);
		final IJaxrsResourceMethod resourceMethod = new JaxrsResourceMethod.Builder(method, resource, metamodel)
				.pathTemplate(annotation).build();
		metamodel.add(resourceMethod);
		// operation
		final JavaElementChangedEvent event = createEvent(annotation, REMOVED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
	}

	@Test
	public void shouldUpdateSubresourceMethodWhenChangingPathAnnotationValue() throws CoreException {
		// pre-conditions
		// JAX-RS HttpMethod
		final JaxrsHttpMethod httpMethod = createHttpMethod(GET.class);
		metamodel.add(httpMethod);
		// Parent JAX-RS Resource
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(
				getAnnotation(type, Path.class)).build();
		metamodel.add(resource);
		// JAX-RS Resource Method
		final IMethod method = getMethod(type, "getCustomer");
		final Annotation pathAnnotation = getAnnotation(method, Path.class, "/foo");
		final Annotation httpAnnotation = getAnnotation(method, GET.class);
		final IJaxrsResourceMethod resourceMethod = new JaxrsResourceMethod.Builder(method, resource, metamodel)
				.httpMethod(httpAnnotation).pathTemplate(pathAnnotation).build();
		metamodel.add(resourceMethod);
		// operation
		final JavaElementChangedEvent event = createEvent(pathAnnotation, CHANGED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_PATH_VALUE));
	}

	@Test
	public void shouldUpdateResourceMethodWhenAddingConsumesAnnotation() throws CoreException {
		// pre-conditions
		// JAX-RS HttpMethod
		final JaxrsHttpMethod httpMethod = createHttpMethod(POST.class);
		metamodel.add(httpMethod);
		// Parent JAX-RS Resource
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		// JAX-RS Resource Method
		final IMethod method = getMethod(type, "createCustomer");
		final Annotation consumesAnnotation = getAnnotation(method, Consumes.class);
		final Annotation httpAnnotation = getAnnotation(method, POST.class);
		final IJaxrsResourceMethod resourceMethod = new JaxrsResourceMethod.Builder(method, resource, metamodel)
				.httpMethod(httpAnnotation).build();
		metamodel.add(resourceMethod);
		// operation
		final JavaElementChangedEvent event = createEvent(consumesAnnotation, ADDED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_CONSUMED_MEDIATYPES_VALUE));
	}

	@Test
	public void shouldUpdateResourceMethodWhenChangingConsumesAnnotationValue() throws CoreException {
		// pre-conditions
		// JAX-RS HttpMethod
		final JaxrsHttpMethod httpMethod = createHttpMethod(POST.class);
		metamodel.add(httpMethod);
		// Parent JAX-RS Resource
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		// JAX-RS Resource Method
		final IMethod method = getMethod(type, "createCustomer");
		final Annotation consumesAnnotation = getAnnotation(method, Consumes.class, "application/foo");
		final Annotation httpAnnotation = getAnnotation(method, POST.class);
		final IJaxrsResourceMethod resourceMethod = new JaxrsResourceMethod.Builder(method, resource, metamodel)
				.httpMethod(httpAnnotation).consumes(consumesAnnotation).build();
		metamodel.add(resourceMethod);
		// operation
		final JavaElementChangedEvent event = createEvent(consumesAnnotation, CHANGED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_CONSUMED_MEDIATYPES_VALUE));
	}

	@Test
	public void shouldUpdateResourceMethodMethodWhenRemovingConsumesAnnotation() throws CoreException {
		// pre-conditions
		// JAX-RS HttpMethod
		final JaxrsHttpMethod httpMethod = createHttpMethod(POST.class);
		metamodel.add(httpMethod);
		// Parent JAX-RS Resource
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		// JAX-RS Resource Method
		final IMethod method = getMethod(type, "createCustomer");
		final Annotation httpAnnotation = getAnnotation(method, POST.class);
		final Annotation consumesAnnotation = getAnnotation(method, Consumes.class);
		final IJaxrsResourceMethod resourceMethod = new JaxrsResourceMethod.Builder(method, resource, metamodel)
				.httpMethod(httpAnnotation).consumes(consumesAnnotation).build();
		metamodel.add(resourceMethod);
		// operation
		final JavaElementChangedEvent event = createEvent(consumesAnnotation, REMOVED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_CONSUMED_MEDIATYPES_VALUE));
	}

	@Test
	public void shouldUpdateResourceMethodWhenAddingProducesAnnotation() throws CoreException {
		// pre-conditions
		// JAX-RS HttpMethod
		final JaxrsHttpMethod httpMethod = createHttpMethod(POST.class);
		metamodel.add(httpMethod);
		// Parent JAX-RS Resource
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		// JAX-RS Resource Method
		final IMethod method = getMethod(type, "getCustomerAsVCard");
		final Annotation producesAnnotation = getAnnotation(method, Produces.class);
		final Annotation httpAnnotation = getAnnotation(method, GET.class);
		final IJaxrsResourceMethod resourceMethod = new JaxrsResourceMethod.Builder(method, resource, metamodel)
				.httpMethod(httpAnnotation).build();
		metamodel.add(resourceMethod);
		// operation
		final JavaElementChangedEvent event = createEvent(producesAnnotation, ADDED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_PRODUCED_MEDIATYPES_VALUE));
	}

	@Test
	public void shouldUpdateResourceMethodWhenChangingProducesAnnotationValue() throws CoreException {
		// pre-conditions
		// JAX-RS HttpMethod
		final JaxrsHttpMethod httpMethod = createHttpMethod(POST.class);
		metamodel.add(httpMethod);
		// Parent JAX-RS Resource
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		// JAX-RS Resource Method
		final IMethod method = getMethod(type, "getCustomerAsVCard");
		final Annotation httpAnnotation = getAnnotation(method, GET.class);
		final Annotation producesAnnotation = getAnnotation(method, Produces.class, "application/foo");
		final IJaxrsResourceMethod resourceMethod = new JaxrsResourceMethod.Builder(method, resource, metamodel)
				.httpMethod(httpAnnotation).produces(producesAnnotation).build();
		metamodel.add(resourceMethod);
		// operation
		final JavaElementChangedEvent event = createEvent(producesAnnotation, CHANGED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_PRODUCED_MEDIATYPES_VALUE));
	}

	@Test
	public void shouldUpdateResourceMethodWhenRemovingProducesAnnotation() throws CoreException {
		// pre-conditions
		// JAX-RS HttpMethod
		final JaxrsHttpMethod httpMethod = createHttpMethod(GET.class);
		metamodel.add(httpMethod);
		// Parent JAX-RS Resource
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		// JAX-RS Resource Method
		final IMethod method = getMethod(type, "getCustomerAsVCard");
		final Annotation httpAnnotation = getAnnotation(method, GET.class);
		final Annotation producesAnnotation = getAnnotation(method, Produces.class);
		final IJaxrsResourceMethod resourceMethod = new JaxrsResourceMethod.Builder(method, resource, metamodel)
				.httpMethod(httpAnnotation).produces(producesAnnotation).build();
		metamodel.add(resourceMethod);
		// operation
		final JavaElementChangedEvent event = createEvent(producesAnnotation, REMOVED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_PRODUCED_MEDIATYPES_VALUE));
	}

	@Test
	public void shouldUpdateResourceMethodWhenAddingParameterWithPathParamAnnotation() throws CoreException {
		// test is not relevant : the old method is replaced by a new one
	}

	@Test
	public void shouldUpdateResourceMethodWhenAddingPathParamAnnotationOnParameter() throws CoreException {
		// the method signature is changed
		// pre-conditions
		// Parent JAX-RS Resource
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		// JAX-RS Resource Method
		final IMethod method = getMethod(type, "getCustomer");
		final JavaMethodParameter pathParameter = new JavaMethodParameter("id", Integer.class.getName(), null);
		final JavaMethodParameter contextParameter = new JavaMethodParameter("uriInfo", UriInfo.class.getName(),
				Arrays.asList(createAnnotation(Context.class, null)));
		final IJaxrsResourceMethod resourceMethod = new JaxrsResourceMethod.Builder(method, resource, metamodel)
				.httpMethod(getAnnotation(method, GET.class)).returnType(getType(Response.class.getName(), javaProject))
				.methodParameter(pathParameter).methodParameter(contextParameter).build();
		metamodel.add(resourceMethod);
		// operation
		final JavaElementChangedEvent event = createEvent(method, CHANGED, F_SIGNATURE);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_METHOD_PARAMETERS));

	}

	@Test
	public void shouldUpdateResourceMethodWhenChangingPathParamAnnotationOnParameter() throws CoreException {
		// the method signature is changed
		// pre-conditions
		// Parent JAX-RS Resource
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		// JAX-RS Resource Method
		final IMethod method = getMethod(type, "getCustomer");
		final JavaMethodParameter pathParameter = new JavaMethodParameter("id", Integer.class.getName(),
				Arrays.asList(createAnnotation(PathParam.class, "foo!")));
		final JavaMethodParameter contextParameter = new JavaMethodParameter("uriInfo", UriInfo.class.getName(),
				Arrays.asList(createAnnotation(Context.class, null)));
		final IJaxrsResourceMethod resourceMethod = new JaxrsResourceMethod.Builder(method, resource, metamodel)
				.httpMethod(getAnnotation(method, GET.class)).returnType(getType(Response.class.getName(), javaProject))
				.methodParameter(pathParameter).methodParameter(contextParameter).build();
		metamodel.add(resourceMethod);
		// operation
		final JavaElementChangedEvent event = createEvent(method, CHANGED, F_SIGNATURE);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_METHOD_PARAMETERS));
	}

	@Test
	public void shouldUpdateResourceMethodWhenRemovingPathParamAnnotationOnParameter() throws CoreException {
		// the method signature is changed
		// pre-conditions
		// Parent JAX-RS Resource
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		// JAX-RS Resource Method
		final IMethod method = getMethod(type, "updateCustomer");
		final JavaMethodParameter pathParameter = new JavaMethodParameter("id", Integer.class.getName(),
				Arrays.asList(createAnnotation(PathParam.class, "id")));
		final JavaMethodParameter customerParameter = new JavaMethodParameter("update",
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", Arrays.asList(createAnnotation(
						PathParam.class, "foo")));
		final IJaxrsResourceMethod resourceMethod = new JaxrsResourceMethod.Builder(method, resource, metamodel)
				.httpMethod(getAnnotation(method, PUT.class)).returnType(getType("void", javaProject))
				.methodParameter(pathParameter).methodParameter(customerParameter).build();
		metamodel.add(resourceMethod);
		// operation
		final JavaElementChangedEvent event = createEvent(method, CHANGED, F_SIGNATURE);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_METHOD_PARAMETERS));
	}

	@Test
	public void shouldUpdateResourceMethodWhenAddingParameterWithQueryParamAnnotation() throws CoreException {
		// the method signature is changed
		// pre-conditions
		// Parent JAX-RS Resource
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		// JAX-RS Resource Method (size param is not declared)
		final IMethod method = getMethod(type, "getCustomers");
		final JavaMethodParameter startParameter = new JavaMethodParameter("start", "int",
				Arrays.asList(createAnnotation(QueryParam.class, "start")));
		final JavaMethodParameter uriInfoParameter = new JavaMethodParameter("uriInfo", UriInfo.class.getName(),
				Arrays.asList(createAnnotation(Context.class, null)));
		final IJaxrsResourceMethod jaxrsMethod = new JaxrsResourceMethod.Builder(method, resource, metamodel)
				.httpMethod(getAnnotation(method, GET.class)).returnType(getType("java.util.List", javaProject))
				.methodParameter(startParameter).methodParameter(uriInfoParameter).build();
		metamodel.add(jaxrsMethod);
		// operation
		final JavaElementChangedEvent event = createEvent(method, CHANGED, F_SIGNATURE);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_METHOD_PARAMETERS));
	}

	@Test
	public void shouldUpdateResourceMethodWhenAddingQueryParamAnnotationOnParameter() throws CoreException {
		// the method signature is changed
		// pre-conditions
		// Parent JAX-RS Resource
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		// JAX-RS Resource Method (@QueryParam on 'size' param is not declared)
		final IMethod method = getMethod(type, "getCustomers");
		final JavaMethodParameter startParameter = new JavaMethodParameter("start", "int",
				Arrays.asList(createAnnotation(QueryParam.class, "start")));
		final JavaMethodParameter sizeParameter = new JavaMethodParameter("size", "int", Arrays.asList(createAnnotation(
				DefaultValue.class, "2")));
		final JavaMethodParameter uriInfoParameter = new JavaMethodParameter("uriInfo", UriInfo.class.getName(),
				Arrays.asList(createAnnotation(Context.class, null)));
		final IJaxrsResourceMethod jaxrsMethod = new JaxrsResourceMethod.Builder(method, resource, metamodel)
				.httpMethod(getAnnotation(method, GET.class)).returnType(getType("java.util.List", javaProject))
				.methodParameter(startParameter).methodParameter(sizeParameter).methodParameter(uriInfoParameter)
				.build();
		metamodel.add(jaxrsMethod);
		// operation
		final JavaElementChangedEvent event = createEvent(method, CHANGED, F_SIGNATURE);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_METHOD_PARAMETERS));
	}

	@Test
	public void shouldUpdateResourceMethodWhenChangingQueryParamAnnotationOnParameter() throws CoreException {
		// the method signature is changed
		// pre-conditions
		// Parent JAX-RS Resource
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		// JAX-RS Resource Method (QueryParam value is different: "length" vs
		// "size" on second param)
		final IMethod method = getMethod(type, "getCustomers");
		final JavaMethodParameter startParameter = new JavaMethodParameter("start", "int",
				Arrays.asList(createAnnotation(QueryParam.class, "start")));
		final JavaMethodParameter sizeParameter = new JavaMethodParameter("size", "int", Arrays.asList(createAnnotation(
				QueryParam.class, "length"), createAnnotation(DefaultValue.class, "2")));
		final JavaMethodParameter uriInfoParameter = new JavaMethodParameter("uriInfo", UriInfo.class.getName(),
				Arrays.asList(createAnnotation(Context.class, null)));
		final IJaxrsResourceMethod jaxrsMethod = new JaxrsResourceMethod.Builder(method, resource, metamodel)
				.httpMethod(getAnnotation(method, GET.class)).returnType(getType("java.util.List", javaProject))
				.methodParameter(startParameter).methodParameter(sizeParameter).methodParameter(uriInfoParameter)
				.build();
		metamodel.add(jaxrsMethod);
		// operation
		final JavaElementChangedEvent event = createEvent(method, CHANGED, F_SIGNATURE);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_METHOD_PARAMETERS));
	}

	@Test
	public void shouldUpdateResourceMethodWhenRemovingQueryParamAnnotationOnParameter() throws CoreException {
		// the method signature is changed
		// pre-conditions
		// Parent JAX-RS Resource
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		// JAX-RS Resource Method (an extra annotation on 'start' param)
		final IMethod method = getMethod(type, "getCustomers");
		final JavaMethodParameter startParameter = new JavaMethodParameter("start", "int", Arrays.asList(
				createAnnotation(QueryParam.class, "start"),
				createAnnotation(DefaultValue.class, "0")));
		final JavaMethodParameter sizeParameter = new JavaMethodParameter("size", "int", Arrays.asList(createAnnotation(
				QueryParam.class, "size"), createAnnotation(DefaultValue.class, "2")));
		final JavaMethodParameter uriInfoParameter = new JavaMethodParameter("uriInfo", UriInfo.class.getName(),
				Arrays.asList(createAnnotation(Context.class, null)));
		final IJaxrsResourceMethod jaxrsMethod = new JaxrsResourceMethod.Builder(method, resource, metamodel)
				.httpMethod(getAnnotation(method, GET.class)).returnType(getType("java.util.List", javaProject))
				.methodParameter(startParameter).methodParameter(sizeParameter).methodParameter(uriInfoParameter)
				.build();
		metamodel.add(jaxrsMethod);
		// operation
		final JavaElementChangedEvent event = createEvent(method, CHANGED, F_SIGNATURE);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_METHOD_PARAMETERS));
	}

	@Test
	@Ignore("deferred for now")
	public void shouldUpdateResourceMethodWhenAddingMatrixParamAnnotationOnParameter() throws CoreException {
		fail("Not implemented yet");
	}

	@Test
	@Ignore("deferred for now")
	public void shouldUpdateResourceMethodWhenChangingMatrixParamAnnotationOnParameter() throws CoreException {
		fail("Not implemented yet");
	}

	@Test
	@Ignore("deferred for now")
	public void shouldUpdateResourceMethodWhenRemovingMatrixParamAnnotationOnParameter() throws CoreException {
		fail("Not implemented yet");
	}

	@Test
	public void shouldUpdateResourceMethodWhenChangingReturnType() throws CoreException {
		// the method signature is changed
		// pre-conditions
		// Parent JAX-RS Resource
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		// JAX-RS Resource Method (declared return type is
		// 'javax.ws.rs.Response')
		final IMethod method = getMethod(type, "getCustomers");
		final JavaMethodParameter startParameter = new JavaMethodParameter("start", "int",
				Arrays.asList(createAnnotation(QueryParam.class, "start")));
		final JavaMethodParameter sizeParameter = new JavaMethodParameter("size", "int", Arrays.asList(createAnnotation(
				QueryParam.class, "size"), createAnnotation(DefaultValue.class, "2")));
		final JavaMethodParameter uriInfoParameter = new JavaMethodParameter("uriInfo", UriInfo.class.getName(),
				Arrays.asList(createAnnotation(Context.class, null)));
		final IJaxrsResourceMethod jaxrsMethod = new JaxrsResourceMethod.Builder(method, resource, metamodel)
				.httpMethod(getAnnotation(method, GET.class)).returnType(getType(Response.class.getName(), javaProject))
				.methodParameter(startParameter).methodParameter(sizeParameter).methodParameter(uriInfoParameter)
				.build();
		metamodel.add(jaxrsMethod);
		// operation
		final JavaElementChangedEvent event = createEvent(method, CHANGED, F_SIGNATURE);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		// annotations were not provided but are retrieved... 
		assertThat(impacts.get(0).getFlags(), equalTo(F_METHOD_RETURN_TYPE + F_METHOD_PARAMETERS));
	}

	@Test
	public void shouldDoNothingMethodWhenAddingParamWithAnotherAnnotation() throws CoreException {
		// test is not relevant : the old method is replaced by a new one
	}

	@Test
	@Ignore("For now, accept all annotations")
	public void shouldDoNothingMethodWhenAddingAnotherAnnotationOnParameter() throws CoreException {
		// the method signature is changed
		// pre-conditions
		// Parent JAX-RS Resource
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		// JAX-RS Resource Method (@Context is not declared on 'uriInfo' param)
		final IMethod method = getMethod(type, "getCustomers");
		final JavaMethodParameter startParameter = new JavaMethodParameter("start", "int",
				Arrays.asList(createAnnotation(QueryParam.class, "start")));
		final JavaMethodParameter sizeParameter = new JavaMethodParameter("size", "int", Arrays.asList(createAnnotation(
				QueryParam.class, "length"), createAnnotation(DefaultValue.class, "2")));
		final JavaMethodParameter uriInfoParameter = new JavaMethodParameter("uriInfo", UriInfo.class.getName(), null);
		final IJaxrsResourceMethod jaxrsMethod = new JaxrsResourceMethod.Builder(method, resource, metamodel)
				.methodParameter(startParameter).methodParameter(sizeParameter).methodParameter(uriInfoParameter)
				.build();
		metamodel.add(jaxrsMethod);
		// operation
		final JavaElementChangedEvent event = createEvent(method, CHANGED, F_SIGNATURE);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(0));
	}

	@Test
	@Ignore("For now, accept all annotations")
	public void shouldDoNothingMethodWhenChangingAnotherAnnotationOnParameter() throws CoreException {
		fail("Not implemented yet");
	}

	@Test
	@Ignore("For now, accept all annotations")
	public void shouldDoNothingMethodWhenRemovingAnotherAnnotationOnParameter() throws CoreException {
		fail("Not implemented yet");
	}

	@Test
	public void shouldDoNothingWhenAddingBasicJavaMethod() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		// JAX-RS Resource Method (an extra annotation on 'start' param)
		final IMethod method = getMethod(type, "getEntityManager");
		// operation
		final JavaElementChangedEvent event = createEvent(method, ADDED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(0));
	}

	@Test
	public void shouldDoNothingWhenChangingBasicJavaMethod() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		// JAX-RS Resource Method (an extra annotation on 'start' param)
		final IMethod method = getMethod(type, "getEntityManager");
		// operation
		final JavaElementChangedEvent event = createEvent(method, CHANGED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(0));
	}

	@Test
	public void shouldDoNothingWhenRemovingBasicJavaMethod() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final Annotation pathAnnotation = getAnnotation(type, Path.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation).build();
		metamodel.add(resource);
		// JAX-RS Resource Method (an extra annotation on 'start' param)
		final IMethod method = getMethod(type, "getEntityManager");
		// operation
		final JavaElementChangedEvent event = createEvent(method, REMOVED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(0));
	}

	@Test
	@Ignore
	public void shouldUpdateResourceMethodWhenAddingThrowsException() throws CoreException {
		fail("Not implemented yet - postponed along with support for providers");
	}

	@Test
	@Ignore
	public void shouldUpdateResourceMethodWhenChangingThrowsException() throws CoreException {
		fail("Not implemented yet - postponed along with support for providers");
	}

	@Test
	@Ignore
	public void shouldUpdateResourceMethodWhenRemovingThrowsException() throws CoreException {
		fail("Not implemented yet - postponed along with support for providers");
	}

}
