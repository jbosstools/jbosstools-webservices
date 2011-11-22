package org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder;

import static org.eclipse.jdt.core.IJavaElementDelta.ADDED;
import static org.eclipse.jdt.core.IJavaElementDelta.CHANGED;
import static org.eclipse.jdt.core.IJavaElementDelta.REMOVED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isOneOf;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementChangedEvent.F_ELEMENT_KIND;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementChangedEvent.F_PATH_VALUE;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.spy;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.jboss.tools.ws.jaxrs.core.AbstractCommonTestCase;
import org.jboss.tools.ws.jaxrs.core.WorkbenchUtils;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod.Builder;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.JavaMethodParameter;
import org.jboss.tools.ws.jaxrs.core.jdt.JavaMethodSignature;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsEndpointChangedEvent;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsResource;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsResourceMethod;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.jboss.tools.ws.jaxrs.core.WorkbenchUtils.*;

public class JaxrsElementChangedProcessorTestCase extends AbstractCommonTestCase {

	private JaxrsMetamodel metamodel;

	private final JaxrsElementChangedProcessor delegate = new JaxrsElementChangedProcessor();

	private static final IProgressMonitor progressMonitor = new NullProgressMonitor();

	@Before
	public void setup() throws CoreException {
		metamodel = spy(JaxrsMetamodel.create(javaProject));
	}

	private JaxrsResource createResource(String typeName) throws CoreException, JavaModelException {
		final IType resourceType = getType(typeName, javaProject);
		final JaxrsResource customerResource = new JaxrsResource.Builder(resourceType, metamodel).pathTemplate(
				getAnnotation(resourceType, Path.class)).build();
		metamodel.add(customerResource);
		return customerResource;
	}

	private JaxrsResourceMethod createResourceMethod(String methodName, IJaxrsResource parentResource,
			Class<?> httpMethod) throws CoreException, JavaModelException {
		final IType javaType = parentResource.getJavaElement();
		final ICompilationUnit compilationUnit = javaType.getCompilationUnit();
		final IMethod javaMethod = getMethod(javaType, methodName);
		final JavaMethodSignature methodSignature = JdtUtils.resolveMethodSignature(javaMethod,
				JdtUtils.parse(compilationUnit, progressMonitor));

		final Builder builder = new JaxrsResourceMethod.Builder(javaMethod, (JaxrsResource) parentResource, metamodel)
				.httpMethod(getAnnotation(javaMethod, httpMethod)).pathTemplate(getAnnotation(javaMethod, Path.class))
				.returnType(methodSignature.getReturnedType());
		for (JavaMethodParameter methodParam : methodSignature.getMethodParameters()) {
			builder.methodParameter(methodParam);
		}
		final JaxrsResourceMethod resourceMethod = builder.build();
		metamodel.add(resourceMethod);
		return resourceMethod;
	}

	private JaxrsHttpMethod createHttpMethod(Class<?> annotationClass) throws JavaModelException, CoreException {
		final IType type = getType(annotationClass.getName(), javaProject);
		final Annotation httpAnnotation = getAnnotation(type, HttpMethod.class);
		final JaxrsHttpMethod httpMethod = new JaxrsHttpMethod(type, httpAnnotation, metamodel);
		metamodel.add(httpMethod);
		return httpMethod;
	}

	private IJaxrsEndpoint createEndpoint(IJaxrsHttpMethod httpMethod, IJaxrsResourceMethod... resourceMethods) {
		IJaxrsEndpoint endpoint = new JaxrsEndpoint(httpMethod, new LinkedList<IJaxrsResourceMethod>(
				Arrays.asList(resourceMethods)));
		metamodel.add(endpoint);
		return endpoint;
	}

	
	@Test
	public void shouldConstructSimpleEndpoint() throws JavaModelException, CoreException {
		// pre-conditions
		final IJaxrsHttpMethod httpMethod = createHttpMethod(GET.class);
		final JaxrsResource customerResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		customerResource.addOrUpdateAnnotation(getAnnotation(customerResource.getJavaElement(), Consumes.class));
		customerResource.addOrUpdateAnnotation(getAnnotation(customerResource.getJavaElement(), Produces.class));
		final IJaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomerAsVCard",
				customerResource, GET.class);
		customerResourceMethod.addOrUpdateAnnotation(getAnnotation(customerResourceMethod.getJavaElement(),
				Produces.class));
		// operation
		IJaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		// verifications
		assertThat(endpoint.getHttpMethod(), equalTo(httpMethod));
		assertThat(endpoint.getUriPathTemplate(), equalTo("/customers/{id}"));
		// @produces and @consumes annotations were explicitly declared
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("application/xml")));
		assertThat(endpoint.getProducedMediaTypes(), equalTo(Arrays.asList("text/x-vcard")));
	}

	@Test
	public void shouldConstructEndpointFromSubresource() throws CoreException {
		// pre-conditions
		final IJaxrsHttpMethod httpMethod = createHttpMethod(GET.class);
		final JaxrsResource producLocatorResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final IJaxrsResourceMethod productLocatorMethod = createResourceMethod("getProductResourceLocator",
				producLocatorResource, GET.class);
		final JaxrsResource bookResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		final IJaxrsResourceMethod subresourceMethod = createResourceMethod("getProduct", bookResource, GET.class);
		// operation
		IJaxrsEndpoint endpoint = createEndpoint(httpMethod, productLocatorMethod, subresourceMethod);
		// verifications
		assertThat(endpoint.getHttpMethod(), equalTo(httpMethod));
		assertThat(endpoint.getUriPathTemplate(), equalTo("/products/{productType}/{id}"));
		// @produces and @consumes annotations were not declared in the setup(),
		// default values should be set
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("*/*")));
		assertThat(endpoint.getProducedMediaTypes(), equalTo(Arrays.asList("*/*")));
	}

	@Test
	public void shouldConstructEndpointWithQueryParams() throws CoreException {
		// pre-conditions
		final IJaxrsHttpMethod httpMethod = createHttpMethod(GET.class);
		final JaxrsResource customerResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IJaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomers", customerResource,
				GET.class);
		// operation
		IJaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		// verifications
		assertThat(endpoint.getHttpMethod(), equalTo(httpMethod));
		assertThat(endpoint.getUriPathTemplate(), equalTo("/customers?start={int}&size={int:2}"));
	}

	@Test
	public void shoudCreateEndpointWhenAddingResourceMethodInRootResource() throws CoreException {
		// pre-conditions
		createHttpMethod(GET.class);
		final JaxrsResource customerResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IJaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomers", customerResource,
				GET.class);
		// operation
		JaxrsElementChangedEvent event = new JaxrsElementChangedEvent(customerResourceMethod, ADDED);
		final List<JaxrsEndpointChangedEvent> changes = delegate.processEvents(Arrays.asList(event), progressMonitor);
		// verifications
		assertThat(changes.size(), equalTo(1));
		assertThat(changes.get(0).getDeltaKind(), equalTo(ADDED));
	}

	@Test
	public void shoudCreateEndpointWhenAddingSubresourceMethodInRootResource() throws JavaModelException, CoreException {
		// pre-conditions
		createHttpMethod(GET.class);
		final JaxrsResource customerResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IJaxrsResourceMethod customerSubresourceMethod = createResourceMethod("getCustomer", customerResource,
				GET.class);
		// operation
		JaxrsElementChangedEvent event = new JaxrsElementChangedEvent(customerSubresourceMethod, ADDED);
		final List<JaxrsEndpointChangedEvent> changes = delegate.processEvents(Arrays.asList(event), progressMonitor);
		// verifications
		assertThat(changes.size(), equalTo(1));
		assertThat(changes.get(0).getDeltaKind(), equalTo(ADDED));
	}

	@Test
	public void shoudCreateEndpointWhenAddingSubresourceLocatorMethodInRootResource() throws JavaModelException,
			CoreException {
		// pre-conditions
		final IJaxrsHttpMethod httpMethod = createHttpMethod(GET.class);
		final IJaxrsResource bookResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		final IJaxrsResourceMethod bookResourceMethod = createResourceMethod("getProduct", bookResource, GET.class);
		// createEndpoint(httpMethod, bookResourceMethod);
		final JaxrsResource gameResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		final IJaxrsResourceMethod gameResourceMethod = createResourceMethod("getProduct", gameResource, GET.class);
		// createEndpoint(httpMethod, gameResourceMethod);

		final JaxrsResource productResourceLocator = createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final IJaxrsResourceMethod productResourceLocatorMethod = createResourceMethod("getProductResourceLocator",
				productResourceLocator, null);
		// operation
		JaxrsElementChangedEvent event = new JaxrsElementChangedEvent(productResourceLocatorMethod, ADDED);
		final List<JaxrsEndpointChangedEvent> changes = delegate.processEvents(Arrays.asList(event), progressMonitor);
		// verifications
		assertThat(changes.size(), equalTo(2));
		assertThat(changes.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(changes.get(1).getDeltaKind(), equalTo(ADDED));
	}

	@Test
	public void shoudCreateEndpointWhenAddingResourceMethodInSubresource() throws JavaModelException, CoreException {
		// pre-conditions
		createHttpMethod(GET.class);
		final JaxrsResource productResourceLocator = createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		createResourceMethod("getProductResourceLocator", productResourceLocator, null);
		final IJaxrsResource bookResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		final IJaxrsResourceMethod bookResourceMethod = createResourceMethod("getAllProducts", bookResource, GET.class);
		// operation
		final JaxrsElementChangedEvent event = new JaxrsElementChangedEvent(bookResourceMethod, ADDED);
		final List<JaxrsEndpointChangedEvent> changes = delegate.processEvents(Arrays.asList(event), progressMonitor);
		// verifications
		assertThat(changes.size(), equalTo(1));
		assertThat(changes.get(0).getDeltaKind(), equalTo(ADDED));
	}

	@Test
	public void shoudCreateEndpointWhenChangingSubresourceLocatorMethodIntoSubresourceMethod()
			throws JavaModelException, CoreException {
		// pre-conditions
		createHttpMethod(GET.class);
		final JaxrsResource customerResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IJaxrsResourceMethod customerSubresourceMethod = createResourceMethod("getCustomer", customerResource,
				null);
		assertThat(customerSubresourceMethod.getKind(), equalTo(EnumKind.SUBRESOURCE_LOCATOR));
		// operation
		Annotation httpAnnotation = getAnnotation(customerSubresourceMethod.getJavaElement(), GET.class);
		final int flags = customerSubresourceMethod.addOrUpdateAnnotation(httpAnnotation);
		JaxrsElementChangedEvent event = new JaxrsElementChangedEvent(customerSubresourceMethod, CHANGED, flags);
		final List<JaxrsEndpointChangedEvent> changes = delegate.processEvents(Arrays.asList(event), progressMonitor);
		// verifications
		assertThat(changes.size(), equalTo(1));
		assertThat(changes.get(0).getDeltaKind(), equalTo(ADDED));

	}

	@Test
	public void shoudCreateEndpointWhenAddingSubresourceMethodInSubresource() throws JavaModelException, CoreException {
		// pre-conditions
		createHttpMethod(GET.class);
		final JaxrsResource productResourceLocator = createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		createResourceMethod("getProductResourceLocator", productResourceLocator, null);
		final IJaxrsResource bookResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		final IJaxrsResourceMethod bookResourceMethod = createResourceMethod("getProduct", bookResource, GET.class);
		// operation
		final JaxrsElementChangedEvent event = new JaxrsElementChangedEvent(bookResourceMethod, ADDED);
		final List<JaxrsEndpointChangedEvent> changes = delegate.processEvents(Arrays.asList(event), progressMonitor);
		// verifications
		assertThat(changes.size(), equalTo(1));
		assertThat(changes.get(0).getDeltaKind(), equalTo(ADDED));
	}

	@Test
	@Ignore("deferred for now")
	public void shoudCreateEndpointWhenAddingSubresourceLocatorMethodInSubresource() {
	}

	@Test
	public void shoudChangeUriPathTemplateWhenAddingResourcePathAnnotation() throws JavaModelException, CoreException {
		// the subresource becomes a root resource !
		// pre-conditions
		final JaxrsHttpMethod httpMethod = createHttpMethod(GET.class);
		final JaxrsResource customerResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IJaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomers", customerResource,
				GET.class);
		final IJaxrsEndpoint fakeEndpoint = createEndpoint(httpMethod, customerResourceMethod);
		// operation
		final JaxrsElementChangedEvent event = new JaxrsElementChangedEvent(customerResource, CHANGED, F_ELEMENT_KIND
				+ F_PATH_VALUE);
		final List<JaxrsEndpointChangedEvent> changes = delegate.processEvents(Arrays.asList(event), progressMonitor);
		// verifications
		assertThat(changes.size(), equalTo(2));
		assertThat(changes.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(changes.get(0).getEndpoint(), equalTo(fakeEndpoint));
		assertThat(changes.get(1).getDeltaKind(), equalTo(ADDED));
	}

	@Test
	public void shoudChangeUriPathTemplateWhenAddingMethodPathAnnotation() throws JavaModelException, CoreException {
		// pre-conditions
		final IJaxrsHttpMethod httpMethod = createHttpMethod(GET.class);
		final JaxrsResource customerResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IJaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomer", customerResource,
				GET.class);
		final Annotation annotation = getAnnotation(customerResourceMethod.getJavaElement(), Path.class);
		customerResourceMethod.removeAnnotation(annotation.getJavaAnnotation().getHandleIdentifier());
		final IJaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		assertThat(endpoint.getUriPathTemplate(), equalTo("/customers"));
		// operation
		customerResourceMethod.addOrUpdateAnnotation(annotation);
		final JaxrsElementChangedEvent event = new JaxrsElementChangedEvent(customerResourceMethod, CHANGED,
				F_PATH_VALUE);
		final List<JaxrsEndpointChangedEvent> changes = delegate.processEvents(Arrays.asList(event), progressMonitor);
		// verifications
		assertThat(changes.size(), equalTo(1));
		final IJaxrsEndpointChangedEvent change = changes.get(0);
		assertThat(change.getDeltaKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint(), equalTo(endpoint));
		assertThat(change.getEndpoint().getHttpMethod(), equalTo(httpMethod));
		assertThat(change.getEndpoint().getUriPathTemplate(), equalTo("/customers/{id}"));
	}

	@Test
	public void shoudChangeUriPathTemplateWhenChangingResourcePathAnnotation() throws JavaModelException, CoreException {
		// pre-conditions
		final IJaxrsHttpMethod httpMethod = createHttpMethod(GET.class);
		final JaxrsResource customerResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IJaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomer", customerResource,
				GET.class);
		final IJaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		assertThat(endpoint.getUriPathTemplate(), equalTo("/customers/{id}"));
		// operation
		final Annotation annotation = getAnnotation(customerResource.getJavaElement(), Path.class, "/foo");
		customerResource.addOrUpdateAnnotation(annotation);
		final JaxrsElementChangedEvent event = new JaxrsElementChangedEvent(customerResource, CHANGED, F_PATH_VALUE);
		final List<JaxrsEndpointChangedEvent> changes = delegate.processEvents(Arrays.asList(event), progressMonitor);
		// verifications
		assertThat(changes.size(), equalTo(1));
		final IJaxrsEndpointChangedEvent change = changes.get(0);
		assertThat(change.getDeltaKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint(), equalTo(endpoint));
		assertThat(change.getEndpoint().getHttpMethod(), equalTo(httpMethod));
		assertThat(change.getEndpoint().getUriPathTemplate(), equalTo("/foo/{id}"));
	}

	@Test
	public void shoudChangeUriPathTemplateWhenChangingMethodPathAnnotation() throws JavaModelException, CoreException {
		// pre-conditions
		final IJaxrsHttpMethod httpMethod = createHttpMethod(GET.class);
		final JaxrsResource customerResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IJaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomer", customerResource,
				GET.class);
		final IJaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		assertThat(endpoint.getUriPathTemplate(), equalTo("/customers/{id}"));
		// operation
		final Annotation annotation = getAnnotation(customerResourceMethod.getJavaElement(), Path.class, "{foo}");
		final int flags = customerResourceMethod.addOrUpdateAnnotation(annotation);
		final JaxrsElementChangedEvent event = new JaxrsElementChangedEvent(customerResourceMethod, CHANGED, flags);
		final List<JaxrsEndpointChangedEvent> changes = delegate.processEvents(Arrays.asList(event), progressMonitor);
		// verifications
		assertThat(changes.size(), equalTo(1));
		final IJaxrsEndpointChangedEvent change = changes.get(0);
		assertThat(change.getDeltaKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint(), equalTo(endpoint));
		assertThat(change.getEndpoint().getHttpMethod(), equalTo(httpMethod));
		assertThat(change.getEndpoint().getUriPathTemplate(), equalTo("/customers/{foo}"));
	}

	@Test
	public void shoudChangeUriPathTemplateWhenRemovingResourcePathAnnotationAndMatchingSubresourceLocatorFound()
			throws JavaModelException, CoreException {
		// pre-conditions
		final IJaxrsHttpMethod httpMethod = createHttpMethod(GET.class);
		// the subresource locator that will match the resourcemethod when the
		// rootresource becomes a subresource
		final JaxrsResource productResourceLocator = createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final JaxrsResourceMethod productResourceLocatorMethod = createResourceMethod("getProductResourceLocator",
				productResourceLocator, null);
		// the root resource that will become a subresource
		final JaxrsResource customerResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IJaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomer", customerResource,
				GET.class);
		final IJaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		assertThat(endpoint.getUriPathTemplate(), equalTo("/customers/{id}"));
		// operation
		final Annotation annotation = getAnnotation(customerResource.getJavaElement(), Path.class);
		final int flags = customerResource.removeAnnotation(annotation.getJavaAnnotation().getHandleIdentifier());
		final JaxrsElementChangedEvent event = new JaxrsElementChangedEvent(customerResource, CHANGED, flags);
		final List<JaxrsEndpointChangedEvent> changes = delegate.processEvents(Arrays.asList(event), progressMonitor);
		// verifications
		assertThat(changes.size(), equalTo(2));
		final IJaxrsEndpointChangedEvent change1 = changes.get(0);
		assertThat(change1.getDeltaKind(), equalTo(REMOVED));
		assertThat(change1.getEndpoint(), equalTo(endpoint));
		final IJaxrsEndpointChangedEvent change2 = changes.get(1);
		assertThat(change2.getEndpoint().getHttpMethod(), equalTo(httpMethod));
		assertThat(change2.getEndpoint().getUriPathTemplate(), equalTo("/products/{productType}/{id}"));
	}

	@Test
	public void shoudRemoveEndpointWhenRemovingResourcePathAnnotationAndMatchingSubresourceLocatorNotFound()
			throws JavaModelException, CoreException {
		// pre-conditions
		final IJaxrsHttpMethod httpMethod = createHttpMethod(GET.class);
		final JaxrsResource customerResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IJaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomer", customerResource,
				GET.class);

		final IJaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		assertThat(endpoint.getUriPathTemplate(), equalTo("/customers/{id}"));
		// operation
		final Annotation annotation = getAnnotation(customerResource.getJavaElement(), Path.class);
		final int flags = customerResource.removeAnnotation(annotation.getJavaAnnotation().getHandleIdentifier());
		final JaxrsElementChangedEvent event = new JaxrsElementChangedEvent(customerResource, CHANGED, flags);
		final List<JaxrsEndpointChangedEvent> changes = delegate.processEvents(Arrays.asList(event), progressMonitor);
		// verifications
		assertThat(changes.size(), equalTo(1));
		final IJaxrsEndpointChangedEvent change = changes.get(0);
		assertThat(change.getDeltaKind(), equalTo(REMOVED));
		assertThat(change.getEndpoint(), equalTo(endpoint));
	}

	@Test
	public void shoudChangeUriPathTemplateWhenRemovingMethodPathAnnotation() throws JavaModelException, CoreException {
		// pre-conditions
		final IJaxrsHttpMethod httpMethod = createHttpMethod(GET.class);
		final JaxrsResource customerResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IJaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomer", customerResource,
				GET.class);
		final IJaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		assertThat(endpoint.getUriPathTemplate(), equalTo("/customers/{id}"));
		// operation
		final Annotation annotation = getAnnotation(customerResourceMethod.getJavaElement(), Path.class);
		final int flags = customerResourceMethod.removeAnnotation(annotation.getJavaAnnotation().getHandleIdentifier());
		final JaxrsElementChangedEvent event = new JaxrsElementChangedEvent(customerResourceMethod, CHANGED, flags);
		final List<JaxrsEndpointChangedEvent> changes = delegate.processEvents(Arrays.asList(event), progressMonitor);
		// verifications
		assertThat(changes.size(), equalTo(2));
		assertThat(changes.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(changes.get(0).getEndpoint(), equalTo(endpoint));
		assertThat(changes.get(1).getDeltaKind(), equalTo(ADDED));
		assertThat(changes.get(1).getEndpoint().getHttpMethod(), equalTo(httpMethod));
		assertThat(changes.get(1).getEndpoint().getUriPathTemplate(), equalTo("/customers"));
	}

	@Test
	public void shoudChangeConsumedMediatypesWhenAddingResourceAnnotation() throws JavaModelException, CoreException {
		// pre-conditions
		final IJaxrsHttpMethod httpMethod = createHttpMethod(POST.class);
		final JaxrsResource customerResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IJaxrsResourceMethod customerResourceMethod = createResourceMethod("createCustomer", customerResource,
				POST.class);
		final IJaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("*/*")));
		// operation
		final int flags = customerResource.addOrUpdateAnnotation(getAnnotation(customerResource.getJavaElement(),
				Consumes.class));
		final JaxrsElementChangedEvent event = new JaxrsElementChangedEvent(customerResource, CHANGED, flags);
		final List<JaxrsEndpointChangedEvent> changes = delegate.processEvents(Arrays.asList(event), progressMonitor);
		// verifications
		assertThat(changes.size(), equalTo(1));
		final IJaxrsEndpointChangedEvent change = changes.get(0);
		assertThat(change.getDeltaKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint(), equalTo(endpoint));
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("application/xml")));
	}

	@Test
	public void shoudChangeConsumedMediatypesWhenAddingResourceMethodAnnotation() throws JavaModelException,
			CoreException {
		// pre-conditions
		final IJaxrsHttpMethod httpMethod = createHttpMethod(POST.class);
		final JaxrsResource customerResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IJaxrsResourceMethod customerResourceMethod = createResourceMethod("createCustomer", customerResource,
				POST.class);
		final IJaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("*/*")));
		// operation
		final int flags = customerResourceMethod.addOrUpdateAnnotation(getAnnotation(
				customerResourceMethod.getJavaElement(), Consumes.class));
		final JaxrsElementChangedEvent event = new JaxrsElementChangedEvent(customerResourceMethod, CHANGED, flags);
		final List<JaxrsEndpointChangedEvent> changes = delegate.processEvents(Arrays.asList(event), progressMonitor);
		// verifications
		assertThat(changes.size(), equalTo(1));
		final IJaxrsEndpointChangedEvent change = changes.get(0);
		assertThat(change.getDeltaKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint(), equalTo(endpoint));
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("application/xml")));
	}

	@Test
	public void shoudChangeConsumedMediatypesWhenChangingResourceMethodAnnotation() throws JavaModelException,
			CoreException {
		// pre-conditions
		final IJaxrsHttpMethod httpMethod = createHttpMethod(POST.class);
		final JaxrsResource customerResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IJaxrsResourceMethod customerResourceMethod = createResourceMethod("createCustomer", customerResource,
				POST.class);
		final Annotation annotation = getAnnotation(customerResourceMethod.getJavaElement(), Consumes.class,
				"application/foo");
		customerResourceMethod.addOrUpdateAnnotation(annotation);
		final IJaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("application/foo")));
		// operation
		int flags = customerResourceMethod.addOrUpdateAnnotation(getAnnotation(customerResourceMethod.getJavaElement(),
				Consumes.class));
		final JaxrsElementChangedEvent event = new JaxrsElementChangedEvent(customerResourceMethod, CHANGED, flags);
		final List<JaxrsEndpointChangedEvent> changes = delegate.processEvents(Arrays.asList(event), progressMonitor);
		// verifications
		assertThat(changes.size(), equalTo(1));
		final IJaxrsEndpointChangedEvent change = changes.get(0);
		assertThat(change.getDeltaKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint(), equalTo(endpoint));
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("application/xml")));
	}

	@Test
	public void shoudChangeConsumedMediatypesWhenChangingResourceAnnotation() throws JavaModelException, CoreException {
		// pre-conditions
		final IJaxrsHttpMethod httpMethod = createHttpMethod(POST.class);
		final JaxrsResource customerResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final Annotation annotation = getAnnotation(customerResource.getJavaElement(), Consumes.class,
				"application/foo");
		customerResource.addOrUpdateAnnotation(annotation);
		final IJaxrsResourceMethod customerResourceMethod = createResourceMethod("createCustomer", customerResource,
				POST.class);
		final IJaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("application/foo")));
		// operation
		int flags = customerResource.addOrUpdateAnnotation(getAnnotation(customerResource.getJavaElement(),
				Consumes.class));
		final JaxrsElementChangedEvent event = new JaxrsElementChangedEvent(customerResourceMethod, CHANGED, flags);
		final List<JaxrsEndpointChangedEvent> changes = delegate.processEvents(Arrays.asList(event), progressMonitor);
		// verifications
		assertThat(changes.size(), equalTo(1));
		final IJaxrsEndpointChangedEvent change = changes.get(0);
		assertThat(change.getDeltaKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint(), equalTo(endpoint));
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("application/xml")));
	}

	@Test
	public void shoudChangeConsumedMediatypesWhenRemovingMethodAnnotationWithResourceDefault()
			throws JavaModelException, CoreException {
		// pre-conditions
		final IJaxrsHttpMethod httpMethod = createHttpMethod(POST.class);
		final JaxrsResource customerResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		customerResource.addOrUpdateAnnotation(getAnnotation(customerResource.getJavaElement(), Consumes.class,
				"application/xml"));
		final IJaxrsResourceMethod customerResourceMethod = createResourceMethod("createCustomer", customerResource,
				POST.class);
		final Annotation annotation = getAnnotation(customerResourceMethod.getJavaElement(), Consumes.class,
				"application/foo");
		customerResourceMethod.addOrUpdateAnnotation(annotation);
		final IJaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("application/foo")));
		// operation
		int flags = customerResourceMethod.removeAnnotation(annotation.getJavaAnnotation().getHandleIdentifier());
		final JaxrsElementChangedEvent event = new JaxrsElementChangedEvent(customerResourceMethod, CHANGED, flags);
		final List<JaxrsEndpointChangedEvent> changes = delegate.processEvents(Arrays.asList(event), progressMonitor);
		// verifications
		assertThat(changes.size(), equalTo(1));
		final IJaxrsEndpointChangedEvent change = changes.get(0);
		assertThat(change.getDeltaKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint(), equalTo(endpoint));
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("application/xml")));
	}

	@Test
	public void shoudChangeConsumedMediatypesWhenRemovingMethodAnnotationWithoutResourceDefault()
			throws JavaModelException, CoreException {
		// pre-conditions
		final IJaxrsHttpMethod httpMethod = createHttpMethod(POST.class);
		final JaxrsResource customerResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IJaxrsResourceMethod customerResourceMethod = createResourceMethod("createCustomer", customerResource,
				POST.class);
		final Annotation annotation = getAnnotation(customerResourceMethod.getJavaElement(), Consumes.class,
				"application/foo");
		customerResourceMethod.addOrUpdateAnnotation(annotation);
		final IJaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("application/foo")));
		// operation
		int flags = customerResourceMethod.removeAnnotation(annotation.getJavaAnnotation().getHandleIdentifier());
		final JaxrsElementChangedEvent event = new JaxrsElementChangedEvent(customerResourceMethod, CHANGED, flags);
		final List<JaxrsEndpointChangedEvent> changes = delegate.processEvents(Arrays.asList(event), progressMonitor);
		// verifications
		assertThat(changes.size(), equalTo(1));
		final IJaxrsEndpointChangedEvent change = changes.get(0);
		assertThat(change.getDeltaKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint(), equalTo(endpoint));
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("*/*")));
	}

	@Test
	public void shoudChangeProducedMediatypesWhenAddingResourceAnnotation() throws JavaModelException, CoreException {
		// pre-conditions
		final IJaxrsHttpMethod httpMethod = createHttpMethod(GET.class);
		final JaxrsResource customerResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IJaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomerAsVCard",
				customerResource, GET.class);
		final IJaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		assertThat(endpoint.getProducedMediaTypes(), equalTo(Arrays.asList("*/*")));
		// operation
		final int flags = customerResource.addOrUpdateAnnotation(getAnnotation(customerResource.getJavaElement(),
				Produces.class, "application/xml"));
		final JaxrsElementChangedEvent event = new JaxrsElementChangedEvent(customerResource, CHANGED, flags);
		final List<JaxrsEndpointChangedEvent> changes = delegate.processEvents(Arrays.asList(event), progressMonitor);
		// verifications
		assertThat(changes.size(), equalTo(1));
		final IJaxrsEndpointChangedEvent change = changes.get(0);
		assertThat(change.getDeltaKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint(), equalTo(endpoint));
		assertThat(endpoint.getProducedMediaTypes(), equalTo(Arrays.asList("application/xml")));
	}

	@Test
	public void shoudChangeProducedMediatypesWhenAddingResourceMethodAnnotation() throws JavaModelException,
			CoreException {
		// pre-conditions
		final IJaxrsHttpMethod httpMethod = createHttpMethod(GET.class);
		final JaxrsResource customerResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IJaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomerAsVCard",
				customerResource, GET.class);
		final IJaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		assertThat(endpoint.getProducedMediaTypes(), equalTo(Arrays.asList("*/*")));
		// operation
		final int flags = customerResourceMethod.addOrUpdateAnnotation(getAnnotation(
				customerResourceMethod.getJavaElement(), Produces.class));
		final JaxrsElementChangedEvent event = new JaxrsElementChangedEvent(customerResourceMethod, CHANGED, flags);
		final List<JaxrsEndpointChangedEvent> changes = delegate.processEvents(Arrays.asList(event), progressMonitor);
		// verifications
		assertThat(changes.size(), equalTo(1));
		final IJaxrsEndpointChangedEvent change = changes.get(0);
		assertThat(change.getDeltaKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint(), equalTo(endpoint));
		assertThat(endpoint.getProducedMediaTypes(), equalTo(Arrays.asList("text/x-vcard")));
	}

	@Test
	public void shoudChangeProducedMediatypesWhenChangingResourceAnnotation() throws JavaModelException, CoreException {
		// pre-conditions
		final IJaxrsHttpMethod httpMethod = createHttpMethod(GET.class);
		final JaxrsResource customerResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final Annotation annotation = getAnnotation(customerResource.getJavaElement(), Produces.class,
				"application/foo");
		customerResource.addOrUpdateAnnotation(annotation);
		final IJaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomerAsVCard",
				customerResource, GET.class);
		final IJaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		assertThat(endpoint.getProducedMediaTypes(), equalTo(Arrays.asList("application/foo")));
		// operation
		int flags = customerResource.addOrUpdateAnnotation(getAnnotation(customerResource.getJavaElement(),
				Produces.class, "application/xml"));
		final JaxrsElementChangedEvent event = new JaxrsElementChangedEvent(customerResource, CHANGED, flags);
		final List<JaxrsEndpointChangedEvent> changes = delegate.processEvents(Arrays.asList(event), progressMonitor);
		// verifications
		assertThat(changes.size(), equalTo(1));
		final IJaxrsEndpointChangedEvent change = changes.get(0);
		assertThat(change.getDeltaKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint(), equalTo(endpoint));
		assertThat(endpoint.getProducedMediaTypes(), equalTo(Arrays.asList("application/xml")));
	}

	@Test
	public void shoudChangeProducedMediatypesWhenChangingResourceMethodAnnotation() throws JavaModelException,
			CoreException {
		// pre-conditions
		final IJaxrsHttpMethod httpMethod = createHttpMethod(POST.class);
		final JaxrsResource customerResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IJaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomerAsVCard",
				customerResource, POST.class);
		final Annotation annotation = getAnnotation(customerResourceMethod.getJavaElement(), Produces.class,
				"application/foo");
		customerResourceMethod.addOrUpdateAnnotation(annotation);
		final IJaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		assertThat(endpoint.getProducedMediaTypes(), equalTo(Arrays.asList("application/foo")));
		// operation
		int flags = customerResourceMethod.addOrUpdateAnnotation(getAnnotation(customerResourceMethod.getJavaElement(),
				Produces.class));
		final JaxrsElementChangedEvent event = new JaxrsElementChangedEvent(customerResourceMethod, CHANGED, flags);
		final List<JaxrsEndpointChangedEvent> changes = delegate.processEvents(Arrays.asList(event), progressMonitor);
		// verifications
		assertThat(changes.size(), equalTo(1));
		final IJaxrsEndpointChangedEvent change = changes.get(0);
		assertThat(change.getDeltaKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint(), equalTo(endpoint));
		assertThat(endpoint.getProducedMediaTypes(), equalTo(Arrays.asList("text/x-vcard")));
	}

	@Test
	public void shoudChangeProducedMediatypesWhenRemovingMethodAnnotationWithResourceDefault()
			throws JavaModelException, CoreException {
		// pre-conditions
		final IJaxrsHttpMethod httpMethod = createHttpMethod(GET.class);
		final JaxrsResource customerResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		customerResource.addOrUpdateAnnotation(getAnnotation(customerResource.getJavaElement(), Produces.class,
				"application/xml"));
		final IJaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomerAsVCard",
				customerResource, POST.class);
		final Annotation annotation = getAnnotation(customerResourceMethod.getJavaElement(), Produces.class,
				"application/foo");
		customerResourceMethod.addOrUpdateAnnotation(annotation);
		final IJaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		assertThat(endpoint.getProducedMediaTypes(), equalTo(Arrays.asList("application/foo")));
		// operation
		int flags = customerResourceMethod.removeAnnotation(annotation.getJavaAnnotation().getHandleIdentifier());
		final JaxrsElementChangedEvent event = new JaxrsElementChangedEvent(customerResourceMethod, CHANGED, flags);
		final List<JaxrsEndpointChangedEvent> changes = delegate.processEvents(Arrays.asList(event), progressMonitor);
		// verifications
		assertThat(changes.size(), equalTo(1));
		final IJaxrsEndpointChangedEvent change = changes.get(0);
		assertThat(change.getDeltaKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint(), equalTo(endpoint));
		assertThat(endpoint.getProducedMediaTypes(), equalTo(Arrays.asList("application/xml")));
	}

	@Test
	public void shoudChangeProducedMediatypesWhenRemovingMethodAnnotationWithoutResourceDefault()
			throws JavaModelException, CoreException {
		// pre-conditions
		final IJaxrsHttpMethod httpMethod = createHttpMethod(POST.class);
		final JaxrsResource customerResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IJaxrsResourceMethod customerResourceMethod = createResourceMethod("createCustomer", customerResource,
				POST.class);
		final Annotation annotation = getAnnotation(customerResourceMethod.getJavaElement(), Consumes.class,
				"application/foo");
		customerResourceMethod.addOrUpdateAnnotation(annotation);
		final IJaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("application/foo")));
		// operation
		int flags = customerResourceMethod.removeAnnotation(annotation.getJavaAnnotation().getHandleIdentifier());
		final JaxrsElementChangedEvent event = new JaxrsElementChangedEvent(customerResourceMethod, CHANGED, flags);
		final List<JaxrsEndpointChangedEvent> changes = delegate.processEvents(Arrays.asList(event), progressMonitor);
		// verifications
		assertThat(changes.size(), equalTo(1));
		final IJaxrsEndpointChangedEvent change = changes.get(0);
		assertThat(change.getDeltaKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint(), equalTo(endpoint));
		assertThat(endpoint.getProducedMediaTypes(), equalTo(Arrays.asList("*/*")));
	}

	@Test
	public void shoudRemoveEndpointsWhenRemovingRootResource() throws JavaModelException, CoreException {
		// pre-conditions
		final IJaxrsHttpMethod httpMethod = createHttpMethod(GET.class);
		final JaxrsResource customerResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IJaxrsResourceMethod customerResourceMethod1 = createResourceMethod("getCustomer", customerResource,
				GET.class);
		final IJaxrsEndpoint endpoint1 = createEndpoint(httpMethod, customerResourceMethod1);
		final IJaxrsResourceMethod customerResourceMethod2 = createResourceMethod("getCustomers", customerResource,
				GET.class);
		final IJaxrsEndpoint endpoint2 = createEndpoint(httpMethod, customerResourceMethod2);
		// operation
		final JaxrsElementChangedEvent event = new JaxrsElementChangedEvent(customerResource, REMOVED);
		final List<JaxrsEndpointChangedEvent> changes = delegate.processEvents(Arrays.asList(event), progressMonitor);
		// verifications
		assertThat(changes.size(), equalTo(2));
		for (IJaxrsEndpointChangedEvent change : changes) {
			assertThat(change.getDeltaKind(), equalTo(REMOVED));
			assertThat(change.getEndpoint(), isOneOf(endpoint1, endpoint2));
		}
	}

	@Test
	public void shoudRemoveEndpointsWhenRemovingSubresource() throws JavaModelException, CoreException {
		// pre-conditions
		IJaxrsHttpMethod httpMethod = createHttpMethod(GET.class);
		final JaxrsResource productResourceLocator = createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final JaxrsResourceMethod productResourceLocatorMethod = createResourceMethod("getProductResourceLocator",
				productResourceLocator, null);
		final IJaxrsResource bookResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		final IJaxrsResourceMethod bookResourceMethod = createResourceMethod("getProduct", bookResource, GET.class);
		final IJaxrsEndpoint bookEndpoint = createEndpoint(httpMethod, productResourceLocatorMethod, bookResourceMethod);
		// adding an extra endpoint that shouldn't be affected
		final IJaxrsResource gameResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		final IJaxrsResourceMethod gameResourceMethod = createResourceMethod("getProduct", gameResource, GET.class);
		createEndpoint(httpMethod, gameResourceMethod);
		// operation
		final JaxrsElementChangedEvent event = new JaxrsElementChangedEvent(bookResource, REMOVED);
		final List<JaxrsEndpointChangedEvent> changes = delegate.processEvents(Arrays.asList(event), progressMonitor);
		// verifications
		assertThat(changes.size(), equalTo(1));
		assertThat(changes.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(changes.get(0).getEndpoint(), equalTo(bookEndpoint));
	}

	@Test
	public void shoudRemoveEndpointsWhenRemovingHttpMethod() throws JavaModelException, CoreException {
		// pre-conditions
		IJaxrsHttpMethod httpMethod = createHttpMethod(GET.class);
		final JaxrsResource productResourceLocator = createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		createResourceMethod("getProductResourceLocator", productResourceLocator, null);
		final IJaxrsResource bookResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		final IJaxrsResourceMethod bookResourceMethod = createResourceMethod("getProduct", bookResource, GET.class);
		final IJaxrsEndpoint bookEndpoint = createEndpoint(httpMethod, bookResourceMethod);
		// adding an extra endpoint that shouldn't be affected
		final IJaxrsResource gameResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		final IJaxrsResourceMethod gameResourceMethod = createResourceMethod("getProduct", gameResource, GET.class);
		createEndpoint(httpMethod, gameResourceMethod);
		final Annotation httpAnnotation = bookResourceMethod.getHttpMethodAnnotation();
		final int flags = bookResourceMethod.removeAnnotation(httpAnnotation.getJavaAnnotation().getHandleIdentifier());
		// operation
		final JaxrsElementChangedEvent event = new JaxrsElementChangedEvent(bookResourceMethod, CHANGED, flags);
		final List<JaxrsEndpointChangedEvent> changes = delegate.processEvents(Arrays.asList(event), progressMonitor);
		// verifications
		assertThat(changes.size(), equalTo(1));
		assertThat(changes.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(changes.get(0).getEndpoint(), equalTo(bookEndpoint));
	}

	@Test
	public void shoudAddEndpointsWhenChangingSubresourceLocatorReturnType() throws JavaModelException, CoreException {
		// pre-conditions
		IJaxrsHttpMethod httpMethod = createHttpMethod(GET.class);
		final JaxrsResource productResourceLocator = createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final JaxrsResourceMethod productResourceLocatorMethod = createResourceMethod("getProductResourceLocator",
				productResourceLocator, null);
		final IJaxrsResource bookResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		final JaxrsResourceMethod bookResourceMethod = createResourceMethod("getProduct", bookResource, GET.class);
		productResourceLocatorMethod.update(new JavaMethodSignature(productResourceLocatorMethod.getJavaElement(),
				bookResource.getJavaElement(), productResourceLocatorMethod.getJavaMethodParameters()));
		createEndpoint(httpMethod, productResourceLocatorMethod, bookResourceMethod);
		// adding an extra subresource that should be affected later
		final IJaxrsResource gameResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		final IJaxrsResourceMethod gameResourceMethod = createResourceMethod("getProduct", gameResource, GET.class);
		assertThat(metamodel.getAllEndpoints().size(), equalTo(1));
		// operation
		final IType objectType = JdtUtils.resolveType(Object.class.getName(), javaProject, progressMonitor);
		int flags = productResourceLocatorMethod.update(new JavaMethodSignature(productResourceLocatorMethod
				.getJavaElement(), objectType, productResourceLocatorMethod.getJavaMethodParameters()));
		final JaxrsElementChangedEvent event = new JaxrsElementChangedEvent(productResourceLocatorMethod, CHANGED,
				flags);
		final List<JaxrsEndpointChangedEvent> changes = delegate.processEvents(Arrays.asList(event), progressMonitor);
		// verifications
		assertThat(changes.size(), equalTo(1));
		assertThat(changes.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(changes.get(0).getEndpoint().getResourceMethods().contains(gameResourceMethod), is(true));
		assertThat(metamodel.getAllEndpoints().size(), equalTo(2));
	}

	@Test
	public void shoudRemoveEndpointsWhenChangingSubresourceLocatorReturnType() throws JavaModelException, CoreException {
		// pre-conditions
		IJaxrsHttpMethod httpMethod = createHttpMethod(GET.class);
		final JaxrsResource productResourceLocator = createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final JaxrsResourceMethod productResourceLocatorMethod = createResourceMethod("getProductResourceLocator",
				productResourceLocator, null);
		final IJaxrsResource bookResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		final JaxrsResourceMethod bookResourceMethod = createResourceMethod("getProduct", bookResource, GET.class);
		createEndpoint(httpMethod, productResourceLocatorMethod, bookResourceMethod);
		// adding an extra subresource that should be affected later
		final IJaxrsResource gameResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		final IJaxrsResourceMethod gameResourceMethod = createResourceMethod("getProduct", gameResource, GET.class);
		createEndpoint(httpMethod, productResourceLocatorMethod, gameResourceMethod);
		assertThat(metamodel.getAllEndpoints().size(), equalTo(2));
		// operation
		final IType bookResourceType = bookResource.getJavaElement();
		int flags = productResourceLocatorMethod.update(new JavaMethodSignature(productResourceLocatorMethod
				.getJavaElement(), bookResourceType, productResourceLocatorMethod.getJavaMethodParameters()));
		final JaxrsElementChangedEvent event = new JaxrsElementChangedEvent(productResourceLocatorMethod, CHANGED,
				flags);
		final List<JaxrsEndpointChangedEvent> changes = delegate.processEvents(Arrays.asList(event), progressMonitor);
		// verifications
		assertThat(changes.size(), equalTo(1));
		assertThat(changes.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(changes.get(0).getEndpoint().getResourceMethods().contains(gameResourceMethod), is(true));
		assertThat(metamodel.getAllEndpoints().size(), equalTo(1));

	}

	@Test
	public void shoudRemoveEndpointsWhenRemovingSubresourceLocatorResource() throws JavaModelException, CoreException {
		// pre-conditions
		IJaxrsHttpMethod httpMethod = createHttpMethod(GET.class);
		final JaxrsResource productResourceLocator = createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final JaxrsResourceMethod productResourceLocatorMethod = createResourceMethod("getProductResourceLocator",
				productResourceLocator, null);
		final IJaxrsResource bookResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		final IJaxrsResourceMethod bookResourceMethod = createResourceMethod("getProduct", bookResource, GET.class);
		final IJaxrsEndpoint bookEndpoint = createEndpoint(httpMethod, productResourceLocatorMethod, bookResourceMethod);
		final IJaxrsResource gameResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		final IJaxrsResourceMethod gameResourceMethod = createResourceMethod("getProduct", gameResource, GET.class);
		final IJaxrsEndpoint gameEndpoint = createEndpoint(httpMethod, productResourceLocatorMethod, gameResourceMethod);
		// operation
		final JaxrsElementChangedEvent event = new JaxrsElementChangedEvent(productResourceLocator, REMOVED);
		final List<JaxrsEndpointChangedEvent> changes = delegate.processEvents(Arrays.asList(event), progressMonitor);
		// verifications
		assertThat(changes.size(), equalTo(2));
		for (IJaxrsEndpointChangedEvent change : changes) {
			assertThat(change.getDeltaKind(), equalTo(REMOVED));
			assertThat(change.getEndpoint(), isOneOf(bookEndpoint, gameEndpoint));
		}
	}

	@Test
	public void shoudRemoveEndpointWhenRemovingResourceMethodInRootResource() throws JavaModelException, CoreException {
		// pre-conditions
		final IJaxrsHttpMethod httpMethod = createHttpMethod(GET.class);
		final JaxrsResource customerResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IJaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomers", customerResource,
				GET.class);
		final IJaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		// operation
		final JaxrsElementChangedEvent event = new JaxrsElementChangedEvent(customerResourceMethod, REMOVED);
		final List<JaxrsEndpointChangedEvent> changes = delegate.processEvents(Arrays.asList(event), progressMonitor);
		// verifications
		assertThat(changes.size(), equalTo(1));
		final IJaxrsEndpointChangedEvent change = changes.get(0);
		assertThat(change.getDeltaKind(), equalTo(REMOVED));
		assertThat(change.getEndpoint(), equalTo(endpoint));
	}

	@Test
	public void shoudRemoveEndpointWhenRemovingSubresourceMethodInRootResource() throws JavaModelException,
			CoreException {
		// pre-conditions
		final IJaxrsHttpMethod httpMethod = createHttpMethod(GET.class);
		final JaxrsResource customerResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IJaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomer", customerResource,
				GET.class);
		final IJaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		// operation
		final Annotation annotation = getAnnotation(customerResourceMethod.getJavaElement(), Path.class);
		final int flags = customerResourceMethod.removeAnnotation(annotation.getJavaAnnotation().getHandleIdentifier());
		final JaxrsElementChangedEvent event = new JaxrsElementChangedEvent(customerResourceMethod, REMOVED, flags);
		final List<JaxrsEndpointChangedEvent> changes = delegate.processEvents(Arrays.asList(event), progressMonitor);
		// verifications
		assertThat(changes.size(), equalTo(1));
		final IJaxrsEndpointChangedEvent change = changes.get(0);
		assertThat(change.getDeltaKind(), equalTo(REMOVED));
		assertThat(change.getEndpoint(), equalTo(endpoint));
	}

	@Test
	public void shoudRemoveEndpointWhenRemovingSubresourceLocatorMethod() throws JavaModelException, CoreException {
		// pre-conditions
		IJaxrsHttpMethod httpMethod = createHttpMethod(GET.class);
		final JaxrsResource productResourceLocator = createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final JaxrsResourceMethod productResourceLocatorMethod = createResourceMethod("getProductResourceLocator",
				productResourceLocator, null);
		final IJaxrsResource bookResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		final IJaxrsResourceMethod bookResourceMethod = createResourceMethod("getProduct", bookResource, GET.class);
		final IJaxrsEndpoint bookEndpoint = createEndpoint(httpMethod, productResourceLocatorMethod, bookResourceMethod);
		final IJaxrsResource gameResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		final IJaxrsResourceMethod gameResourceMethod = createResourceMethod("getProduct", gameResource, GET.class);
		final IJaxrsEndpoint gameEndpoint = createEndpoint(httpMethod, productResourceLocatorMethod, gameResourceMethod);
		// operation
		final JaxrsElementChangedEvent event = new JaxrsElementChangedEvent(productResourceLocatorMethod, REMOVED);
		final List<JaxrsEndpointChangedEvent> changes = delegate.processEvents(Arrays.asList(event), progressMonitor);
		// verifications
		assertThat(changes.size(), equalTo(2));
		for (IJaxrsEndpointChangedEvent change : changes) {
			assertThat(change.getDeltaKind(), equalTo(REMOVED));
			assertThat(change.getEndpoint(), isOneOf(bookEndpoint, gameEndpoint));
		}
	}

	@Test
	public void shoudRemoveEndpointWhenSubresourceLocatorRootResourceBecomesSubresource() throws JavaModelException,
			CoreException {
		// pre-conditions
		IJaxrsHttpMethod httpMethod = createHttpMethod(GET.class);
		final JaxrsResource productResourceLocator = createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final JaxrsResourceMethod productResourceLocatorMethod = createResourceMethod("getProductResourceLocator",
				productResourceLocator, null);
		final IJaxrsResource bookResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		final IJaxrsResourceMethod bookResourceMethod = createResourceMethod("getProduct", bookResource, GET.class);
		final IJaxrsEndpoint bookEndpoint = createEndpoint(httpMethod, productResourceLocatorMethod, bookResourceMethod);
		final IJaxrsResource gameResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		final IJaxrsResourceMethod gameResourceMethod = createResourceMethod("getProduct", gameResource, GET.class);
		final IJaxrsEndpoint gameEndpoint = createEndpoint(httpMethod, productResourceLocatorMethod, gameResourceMethod);
		final Annotation productResourceLocatorPathAnnotation = getAnnotation(productResourceLocator.getJavaElement(),
				Path.class);
		final int flags = productResourceLocator.removeAnnotation(productResourceLocatorPathAnnotation
				.getJavaAnnotation().getHandleIdentifier());
		// operation
		final JaxrsElementChangedEvent event = new JaxrsElementChangedEvent(productResourceLocator, CHANGED, flags);
		final List<JaxrsEndpointChangedEvent> changes = delegate.processEvents(Arrays.asList(event), progressMonitor);
		// verifications
		assertThat(changes.size(), equalTo(2));
		for (IJaxrsEndpointChangedEvent change : changes) {
			assertThat(change.getDeltaKind(), equalTo(REMOVED));
			assertThat(change.getEndpoint(), isOneOf(bookEndpoint, gameEndpoint));
		}
	}
}
