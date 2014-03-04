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
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder;

import static org.eclipse.jdt.core.IJavaElementDelta.ADDED;
import static org.eclipse.jdt.core.IJavaElementDelta.CHANGED;
import static org.eclipse.jdt.core.IJavaElementDelta.REMOVED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.APPLICATION_PATH;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.CONSUMES;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.GET;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.HTTP_METHOD;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.PATH;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.PATH_PARAM;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.PRODUCES;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils.createAnnotation;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils.getAnnotation;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils.getAnnotations;
import static org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils.replaceFirstOccurrenceOfCode;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsJavaApplication;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsWebxmlApplication;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsEndpointDelta;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class JaxrsMetamodelChangedProcessorTestCase {

	@ClassRule
	public static WorkspaceSetupRule workspaceSetupRule = new WorkspaceSetupRule("org.jboss.tools.ws.jaxrs.tests.sampleproject");
	
	@Rule
	public JaxrsMetamodelMonitor metamodelMonitor = new JaxrsMetamodelMonitor("org.jboss.tools.ws.jaxrs.tests.sampleproject", false);
	
	private JaxrsMetamodel metamodel = null;

	private IJavaProject javaProject = null;

	@Before
	public void setup() throws CoreException {
		metamodel = metamodelMonitor.getMetamodel();
		javaProject = metamodel.getJavaProject();
	}

	@Test
	public void shouldConstructSimpleEndpoint() throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = metamodel.findHttpMethodByTypeName("javax.ws.rs.GET");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final JaxrsResource customerResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		// verifications
		final JaxrsResourceMethod customerResourceMethod = metamodelMonitor.resolveResourceMethod(customerResource, "getCustomerAsVCard");
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(customerResourceMethod).get(0);
		assertThat(endpoint.getHttpMethod(), equalTo((IJaxrsHttpMethod) httpMethod));
		assertThat(endpoint.getUriPathTemplate(), equalTo("/customers/{id:Integer}"));
		// @produces and @consumes annotations were explicitly declared
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("application/xml")));
		assertThat(endpoint.getProducedMediaTypes(), equalTo(Arrays.asList("text/x-vcard")));
	}

	@Test
	public void shouldConstructEndpointFromSubresource() throws CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = metamodel.findHttpMethodByTypeName("javax.ws.rs.GET");
		metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final JaxrsResource bookResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		// verifications
		final JaxrsResourceMethod subresourceMethod = metamodelMonitor.resolveResourceMethod(bookResource, "getProduct");
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(subresourceMethod).get(0);
		assertThat(endpoint.getHttpMethod(), equalTo((IJaxrsHttpMethod) httpMethod));
		assertThat(endpoint.getUriPathTemplate(), equalTo("/products/{productType:String};bar={String}/{id:Integer}?foo={String:foo!}"));
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("*/*")));
		assertThat(endpoint.getProducedMediaTypes(), equalTo(Arrays.asList("application/xml", "application/json")));
	}

	@Test
	public void shouldConstructEndpointWithQueryParams() throws CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = metamodel.findHttpMethodByTypeName("javax.ws.rs.GET");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final JaxrsResource customerResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		// verifications
		final JaxrsResourceMethod customerResourceMethod = metamodelMonitor.resolveResourceMethod(customerResource, "getCustomers");
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(customerResourceMethod).get(0);
		assertThat(endpoint.getHttpMethod(), equalTo((IJaxrsHttpMethod) httpMethod));
		assertThat(endpoint.getUriPathTemplate(), equalTo("/customers?start={int}&size={int:2}"));
	}

	@Test
	public void shouldCreateEndpointWhenAddingRootResourceWithMethods() throws CoreException {
		// pre-conditions
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		// verifications
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(6));
	}

	@Test
	public void shouldCreateEndpointWhenAddingResourceMethodInRootResource() throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsResource customerResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		metamodelMonitor.resolveResourceMethod(customerResource, "getCustomers").remove();
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final IMethod javaMethod = metamodelMonitor.resolveMethod(customerResource.getJavaElement(), "getCustomers");
		JaxrsResourceMethod.from(javaMethod, metamodel.findAllHttpMethods()).withParentResource(customerResource)
				.withMetamodel(metamodel).build();
		// verifications
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getEndpointChanges().get(0).getKind(), equalTo(ADDED));
	}

	@Test
	public void shouldCreateEndpointWhenAddingSubresourceMethodInRootResource() throws JavaModelException,
			CoreException {
		// pre-conditions
		final JaxrsResource customerResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		metamodelMonitor.resolveResourceMethod(customerResource, "getCustomer").remove();
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final IMethod javaMethod = metamodelMonitor.resolveMethod(customerResource.getJavaElement(), "getCustomer");
		JaxrsResourceMethod.from(javaMethod, metamodel.findAllHttpMethods()).withParentResource(customerResource)
				.withMetamodel(metamodel).build();
		// verifications
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getEndpointChanges().get(0).getKind(), equalTo(ADDED));
	}

	@Test
	public void shouldCreateEndpointsWhenAddingSubresourceLocatorMethodInRootResource() throws JavaModelException,
			CoreException {
		// pre-conditions
		metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		final JaxrsResource productResourceLocator = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		metamodelMonitor.resolveResourceMethod(productResourceLocator, "getProductResourceLocator").remove();
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final IMethod javaMethod = metamodelMonitor.resolveMethod(productResourceLocator.getJavaElement(), "getProductResourceLocator");
		JaxrsResourceMethod.from(javaMethod, metamodel.findAllHttpMethods()).withParentResource(productResourceLocator)
				.withMetamodel(metamodel).build();
		// verifications
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(5));
		assertThat(metamodelMonitor.getEndpointChanges().get(0).getKind(), equalTo(ADDED));
		assertThat(metamodelMonitor.getEndpointChanges().get(1).getKind(), equalTo(ADDED));
		assertThat(metamodelMonitor.getEndpointChanges().get(2).getKind(), equalTo(ADDED));
		assertThat(metamodelMonitor.getEndpointChanges().get(3).getKind(), equalTo(ADDED));
		assertThat(metamodelMonitor.getEndpointChanges().get(4).getKind(), equalTo(ADDED));
	}

	@Test
	public void shouldCreateEndpointWhenAddingResourceMethodInSubresource() throws JavaModelException, CoreException {
		// pre-conditions
		metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final JaxrsResource bookResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		metamodelMonitor.resolveResourceMethod(bookResource, "getProduct").remove();
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final IMethod javaMethod = metamodelMonitor.resolveMethod(bookResource.getJavaElement(), "getProduct");
		JaxrsResourceMethod.from(javaMethod, metamodel.findAllHttpMethods()).withParentResource(bookResource)
				.withMetamodel(metamodel).build();
		// verifications
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getEndpointChanges().get(0).getKind(), equalTo(ADDED));
	}

	@Test
	public void shouldCreateEndpointWhenChangingSubresourceLocatorMethodIntoSubresourceMethod()
			throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsResource productResourceLocator = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final JaxrsResourceMethod subresourceMethod = metamodelMonitor.resolveResourceMethod(productResourceLocator,
				"getProductResourceLocator");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		Annotation httpAnnotation = createAnnotation(GET.qualifiedName);
		subresourceMethod.addOrUpdateAnnotation(httpAnnotation);
		// verifications
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getEndpointChanges().get(0).getKind(), equalTo(ADDED));
	}

	@Test
	public void shouldCreateEndpointWhenAddingSubresourceMethodInSubresource() throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsResource productResourceLocator = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		metamodelMonitor.resolveResourceMethod(productResourceLocator, "getProductResourceLocator");
		final JaxrsResource bookResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		final JaxrsResourceMethod bookResourceMethod = metamodelMonitor.resolveResourceMethod(bookResource, "getProduct");
		bookResourceMethod.remove();
		metamodelMonitor.resetElementChangesNotifications();
		assertThat(metamodel.getAllEndpoints().size(), equalTo(2));
		// operation
		JaxrsResourceMethod.from(bookResourceMethod.getJavaElement(), metamodel.findAllHttpMethods()).withParentResource(bookResource).withMetamodel(metamodel).build();
		// verifications
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getEndpointChanges().get(0).getKind(), equalTo(ADDED));
		assertThat(metamodel.getAllEndpoints().size(), equalTo(3));
	}

	@Test
	@Ignore("deferred for now")
	public void shouldCreateEndpointWhenAddingSubresourceLocatorMethodInSubresource() {
	}

	@Test
	public void shouldChangeEndpointEndpointUriPathTemplateWhenAddingApplication() throws JavaModelException,
			CoreException {
		// pre-conditions
		metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		metamodelMonitor.createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		// verifications
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(6));
	}

	@Test
	public void shouldChangeEndpointUriPathTemplateWhenRemovingApplicationType() throws JavaModelException,
			CoreException {
		// pre-conditions
		metamodel.findHttpMethodByTypeName("javax.ws.rs.GET");
		final JaxrsJavaApplication application = metamodelMonitor.createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		application.remove();
		// verifications
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(6));
		for (int i = 0; i < 6; i++) {
			assertThat(metamodelMonitor.getEndpointChanges().get(i).getKind(), equalTo(CHANGED));
		}
	}

	@Test
	public void shouldChangeEndpointUriPathTemplateWhenRemovingApplicationPathAnnotation() throws JavaModelException,
			CoreException {
		// pre-conditions
		final JaxrsJavaApplication application = metamodelMonitor.createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final Annotation appPathAnnotation = application
				.getAnnotation(EnumJaxrsClassname.APPLICATION_PATH.qualifiedName);
		application.removeAnnotation(appPathAnnotation.getJavaAnnotation());
		// verifications
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(6));
		assertThat(metamodelMonitor.getEndpointChanges().get(0).getKind(), equalTo(CHANGED));
	}

	@Test
	public void shouldChangeEndpointUriPathTemplateWhenAddingResourcePathAnnotation() throws JavaModelException,
			CoreException {
		// pre-conditions
		final JaxrsResource customerResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = metamodelMonitor.resolveResourceMethod(customerResource, "getCustomers");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		customerResourceMethod.addAnnotation(createAnnotation(PATH.qualifiedName, "/{id}"));
		// verifications
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(2));
		assertThat(metamodelMonitor.getEndpointChanges().get(0).getKind(), equalTo(REMOVED));
		assertThat(metamodelMonitor.getEndpointChanges().get(1).getKind(), equalTo(ADDED));
	}

	@Test
	public void shouldChangeEndpointUriPathTemplateWhenAddingMethodPathAnnotation() throws JavaModelException,
			CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = metamodel.findHttpMethodByTypeName("javax.ws.rs.GET");
		final JaxrsResource customerResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = metamodelMonitor.resolveResourceMethod(customerResource, "getCustomer");
		final Annotation annotation = getAnnotation(customerResourceMethod.getJavaElement(), PATH.qualifiedName);
		customerResourceMethod.removeAnnotation(annotation.getJavaAnnotation());
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		customerResourceMethod.addOrUpdateAnnotation(annotation);
		// verifications
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(2));
		JaxrsEndpointDelta change = metamodelMonitor.getEndpointChanges().get(0);
		assertThat(change.getKind(), equalTo(REMOVED));
		change = metamodelMonitor.getEndpointChanges().get(1);
		assertThat(change.getKind(), equalTo(ADDED));
		assertThat(change.getEndpoint().getHttpMethod(), equalTo((IJaxrsHttpMethod) httpMethod));
		assertThat(change.getEndpoint().getUriPathTemplate(), equalTo("/customers/{id:Integer}"));
	}

	@Test
	public void shouldChangeEndpointUriPathTemplateWhenChangingApplicationPathAnnotation() throws JavaModelException,
			CoreException {
		// pre-conditions
		final JaxrsJavaApplication application = metamodelMonitor.createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final Map<String, Annotation> annotations = getAnnotations(application.getJavaElement(),
				APPLICATION_PATH.qualifiedName);
		application.addOrUpdateAnnotation(createAnnotation(annotations.get(APPLICATION_PATH.qualifiedName), "/foo"));
		// verifications: all 6 methods changed
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(6));
		for (int i = 0; i < 6; i++) {
			final JaxrsEndpointDelta change = metamodelMonitor.getEndpointChanges().get(i);
			assertThat(change.getKind(), equalTo(CHANGED));
			assertThat(change.getEndpoint().getUriPathTemplate(), startsWith("/foo/customers"));
		}
	}

	@Test
	public void shouldChangeEndpointUriPathTemplateWhenSwitchingToWebxmlCoreApplication() throws JavaModelException,
			CoreException, IOException {
		// pre-conditions
		metamodelMonitor.createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final JaxrsResource customerResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = metamodelMonitor.resolveResourceMethod(customerResource, "getCustomer");
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(customerResourceMethod).get(0);
		assertThat(endpoint.getUriPathTemplate(), equalTo("/app/customers/{id:Integer}"));
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		metamodelMonitor.createWebxmlApplication("javax.ws.rs.core.Application", "/foo");
		// verifications: all endpoints changed
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(6));
		for (int i = 0; i < 6; i++) {
			final JaxrsEndpointDelta change = metamodelMonitor.getEndpointChanges().get(i);
			assertThat(change.getKind(), equalTo(CHANGED));
			assertThat(change.getEndpoint().getUriPathTemplate(), startsWith("/foo/customers"));
		}
	}

	@Test
	public void shouldChangeEndpointUriPathTemplateWhenSwitchingBackToJavaApplication() throws JavaModelException,
			CoreException, IOException {
		// pre-conditions
		metamodelMonitor.createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final JaxrsResource customerResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = metamodelMonitor.resolveResourceMethod(customerResource, "getCustomer");
		final JaxrsWebxmlApplication webxmlApplication = metamodelMonitor.createWebxmlApplication("javax.ws.rs.core.Application", "/foo");
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(customerResourceMethod).get(0);
		assertThat(endpoint.getUriPathTemplate(), equalTo("/foo/customers/{id:Integer}"));
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		webxmlApplication.remove();
		// verifications
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(6));
		for(int i = 0; i < 6; i++) {
			final JaxrsEndpointDelta change = metamodelMonitor.getEndpointChanges().get(i);
			assertThat(change.getKind(), equalTo(CHANGED));
			assertThat(change.getEndpoint().getUriPathTemplate(), startsWith("/app/customers"));
		}
	}

	@Test
	public void shouldChangeEndpointUriPathTemplateWhenOverridingApplication() throws JavaModelException,
			CoreException, IOException {
		// pre-conditions
		final JaxrsJavaApplication javaApplication = metamodelMonitor.createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final JaxrsResource customerResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = metamodelMonitor.resolveResourceMethod(customerResource, "getCustomer");
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(customerResourceMethod).get(0);
		assertThat(endpoint.getUriPathTemplate(), equalTo("/app/customers/{id:Integer}"));
		metamodelMonitor.resetElementChangesNotifications();
		// operation: the JavaApplication is overridden
		metamodelMonitor.createWebxmlApplication(javaApplication.getJavaClassName(), "/foo");
		// verifications:
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(6));
		final JaxrsEndpointDelta change = metamodelMonitor.getEndpointChanges().get(0);
		assertThat(change.getKind(), equalTo(CHANGED));
		final JaxrsEndpoint changedEndpoint = metamodel.findEndpoints(customerResourceMethod).get(0);
		assertThat(changedEndpoint.getUriPathTemplate(), equalTo("/foo/customers/{id:Integer}"));
	}

	@Test
	public void shouldChangeEndpointUriPathTemplateWhenUnoverridingApplication() throws JavaModelException,
			CoreException, IOException {
		// pre-conditions
		final JaxrsJavaApplication javaApplication = metamodelMonitor.createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final JaxrsResource customerResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = metamodelMonitor.resolveResourceMethod(customerResource, "getCustomer");
		final JaxrsWebxmlApplication webxmlApplication = metamodelMonitor.createWebxmlApplication(javaApplication.getJavaClassName(),
				"/foo");
		javaApplication.setApplicationPathOverride("/foo");
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(customerResourceMethod).get(0);
		assertThat(endpoint.getUriPathTemplate(), equalTo("/foo/customers/{id:Integer}"));
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		webxmlApplication.remove();
		// verifications
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(6));
		final JaxrsEndpointDelta change = metamodelMonitor.getEndpointChanges().get(0);
		assertThat(change.getKind(), equalTo(CHANGED));
		final JaxrsEndpoint changedEndpoint = metamodel.findEndpoints(customerResourceMethod).get(0);
		assertThat(changedEndpoint.getUriPathTemplate(), equalTo("/app/customers/{id:Integer}"));
	}

	@Test
	public void shouldChangeEndpointUriPathTemplateWhenChangingResourcePathAnnotation() throws JavaModelException,
			CoreException {
		// pre-conditions
		final JaxrsResource customerResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = metamodelMonitor.resolveResourceMethod(customerResource, "getCustomer");
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(customerResourceMethod).get(0);
		assertThat(endpoint.getUriPathTemplate(), equalTo("/customers/{id:Integer}"));
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final Annotation pathAnnotation = getAnnotation(customerResourceMethod.getJavaElement(), PATH.qualifiedName);
		customerResource.addOrUpdateAnnotation(createAnnotation(pathAnnotation, "/foo"));
		// verifications: all 6 methods changed
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(6));
		for (int i = 0; i < 6; i++) {
			final JaxrsEndpointDelta change = metamodelMonitor.getEndpointChanges().get(i);
			assertThat(change.getKind(), equalTo(CHANGED));
			assertThat(change.getEndpoint().getUriPathTemplate(), startsWith("/foo"));
		}
	}

	@Test
	public void shouldChangeEndpointUriPathTemplateWhenChangingMethodPathAnnotation() throws JavaModelException,
			CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = metamodel.findHttpMethodByTypeName("javax.ws.rs.GET");
		final JaxrsResource customerResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = metamodelMonitor.resolveResourceMethod(customerResource, "getCustomer");
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(customerResourceMethod).get(0);
		assertThat(endpoint.getUriPathTemplate(), equalTo("/customers/{id:Integer}"));
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		// FIXME: path param annotation must happen first, because no event will be triggered here
		final Annotation pathParamAnnotation = customerResourceMethod.getJavaMethodParameterByName("id").getAnnotation(PATH_PARAM.qualifiedName);
		pathParamAnnotation.update(createAnnotation(pathParamAnnotation, "foo"));
		final Annotation pathAnnotation = getAnnotation(customerResourceMethod.getJavaElement(), PATH.qualifiedName);
		customerResourceMethod.addOrUpdateAnnotation(createAnnotation(pathAnnotation, "{foo}"));
		// verifications
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(1));
		final JaxrsEndpointDelta change = metamodelMonitor.getEndpointChanges().get(0);
		assertThat(change.getKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
		assertThat(change.getEndpoint().getHttpMethod(), equalTo((IJaxrsHttpMethod) httpMethod));
		assertThat(change.getEndpoint().getUriPathTemplate(), equalTo("/customers/{foo:Integer}"));
	}

	@Test
	public void shouldChangeEndpointUriPathTemplateWithWildcardWhenChangingMethodPathAnnotation() throws JavaModelException,
	CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = metamodel.findHttpMethodByTypeName("javax.ws.rs.GET");
		final JaxrsResource customerResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = metamodelMonitor.resolveResourceMethod(customerResource, "getCustomer");
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(customerResourceMethod).get(0);
		assertThat(endpoint.getUriPathTemplate(), equalTo("/customers/{id:Integer}"));
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final Annotation pathAnnotation = getAnnotation(customerResourceMethod.getJavaElement(), PATH.qualifiedName);
		customerResourceMethod.addOrUpdateAnnotation(createAnnotation(pathAnnotation, "{foo}"));
		// verifications
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(1));
		final JaxrsEndpointDelta change = metamodelMonitor.getEndpointChanges().get(0);
		assertThat(change.getKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
		assertThat(change.getEndpoint().getHttpMethod(), equalTo((IJaxrsHttpMethod) httpMethod));
		assertThat(change.getEndpoint().getUriPathTemplate(), equalTo("/customers/{foo:.*}"));
	}

	@Test
	public void shouldChangeEndpointUriPathTemplateWhenRemovingResourcePathAnnotationAndMatchingSubresourceLocatorFound()
			throws JavaModelException, CoreException {
		// pre-conditions
		// the subresource locator that will match the resourcemethod when the
		// rootresource becomes a subresource
		metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		// the root resource that will become a subresource
		final JaxrsResource customerResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = metamodelMonitor.resolveResourceMethod(customerResource, "getCustomer");
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(customerResourceMethod).get(0);
		assertThat(endpoint.getUriPathTemplate(), equalTo("/customers/{id:Integer}"));
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final Annotation annotation = getAnnotation(customerResource.getJavaElement(), PATH.qualifiedName);
		customerResource.removeAnnotation(annotation.getJavaAnnotation());
		// verifications
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(12));
		final JaxrsEndpointDelta change1 = metamodelMonitor.getEndpointChanges().get(0);
		assertThat(change1.getKind(), equalTo(REMOVED));
		final JaxrsEndpointDelta change2 = metamodelMonitor.getEndpointChanges().get(1);
		assertThat(change2.getKind(), equalTo(ADDED));
	}

	@Test
	public void shouldChangeEndpointHttpVerbWhenChangingHttpMethodAnnotation() throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = metamodelMonitor.createHttpMethod("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final JaxrsResource bazResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.BazResource");
		final JaxrsResourceMethod bazResourceMethod = metamodelMonitor.resolveResourceMethod(bazResource, "update3");
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(bazResourceMethod).get(0);
		assertThat(endpoint.getUriPathTemplate(), equalTo("/foo/baz/{param3:String}"));
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final Annotation httpMethodAnnotation = getAnnotation(httpMethod.getJavaElement(), HTTP_METHOD.qualifiedName);
		httpMethod.addOrUpdateAnnotation(createAnnotation(httpMethodAnnotation, "BAR"));
		// verifications
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getEndpointChanges().get(0).getKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getEndpointChanges().get(0).getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
		assertThat(metamodelMonitor.getEndpointChanges().get(0).getEndpoint().getHttpMethod().getHttpVerb(), equalTo("BAR"));
	}

	@Test
	public void shouldChangeEndpointHttpVerbWhenReplacingHttpMethodAnnotation() throws JavaModelException,
			CoreException {
		// pre-conditions
		final JaxrsHttpMethod fooHttpMethod = metamodelMonitor.createHttpMethod("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final JaxrsResource bazResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.BazResource");
		final JaxrsResourceMethod bazResourceMethod = metamodelMonitor.resolveResourceMethod(bazResource, "update3");
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(bazResourceMethod).get(0);
		assertThat(endpoint.getUriPathTemplate(), equalTo("/foo/baz/{param3:String}"));
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final Annotation fooAnnotation = getAnnotation(bazResourceMethod.getJavaElement(),
				fooHttpMethod.getJavaClassName());
		bazResourceMethod.removeAnnotation(fooAnnotation.getJavaAnnotation());
		bazResourceMethod.addAnnotation(createAnnotation("javax.ws.rs.GET", "GET"));
		// verifications: old endpoint added (but none recreated just after),
		// then a new endpoint added once the GET annotation was added
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(2));
		assertThat(metamodelMonitor.getEndpointChanges().get(0).getKind(), equalTo(REMOVED));
		assertThat(metamodelMonitor.getEndpointChanges().get(1).getKind(), equalTo(ADDED));
		assertThat(metamodelMonitor.getEndpointChanges().get(0).getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
		assertThat(metamodelMonitor.getEndpointChanges().get(1).getEndpoint().getHttpMethod().getHttpVerb(), equalTo("GET"));
	}

	@Test
	public void shouldChangeEndpointUriPathTemplateWhenRemovingMetamodelApplication() throws JavaModelException,
			CoreException {
		// pre-conditions
		final JaxrsJavaApplication application = metamodelMonitor.createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final JaxrsResource customerResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = metamodelMonitor.resolveResourceMethod(customerResource, "getCustomer");
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(customerResourceMethod).get(0);
		assertThat(endpoint.getUriPathTemplate(), equalTo("/app/customers/{id:Integer}"));
		metamodelMonitor.resetElementChangesNotifications();
		// operation : no 'application' left in the metamodel
		application.remove();
		// verifications: all 6 methods changed
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(6));
		for (int i = 0; i < 6; i++) {
			final JaxrsEndpointDelta change = metamodelMonitor.getEndpointChanges().get(i);
			assertThat(change.getKind(), equalTo(CHANGED));
			assertThat(change.getEndpoint().getUriPathTemplate(), startsWith("/customers"));
		}
	}

	@Test
	public void shouldChangeEndpointUriPathTemplateWhenRemovingMethodPathAnnotation() throws JavaModelException,
			CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = metamodel.findHttpMethodByTypeName("javax.ws.rs.GET");
		final JaxrsResource customerResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = metamodelMonitor.resolveResourceMethod(customerResource, "getCustomer");
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(customerResourceMethod).get(0);
		assertThat(endpoint.getUriPathTemplate(), equalTo("/customers/{id:Integer}"));
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final Annotation annotation = getAnnotation(customerResourceMethod.getJavaElement(), PATH.qualifiedName);
		customerResourceMethod.removeAnnotation(annotation.getJavaAnnotation());
		// verifications
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(2));
		assertThat(metamodelMonitor.getEndpointChanges().get(0).getKind(), equalTo(REMOVED));
		assertThat(metamodelMonitor.getEndpointChanges().get(0).getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
		assertThat(metamodelMonitor.getEndpointChanges().get(1).getKind(), equalTo(ADDED));
		assertThat(metamodelMonitor.getEndpointChanges().get(1).getEndpoint().getHttpMethod(), equalTo((IJaxrsHttpMethod) httpMethod));
		assertThat(metamodelMonitor.getEndpointChanges().get(1).getEndpoint().getUriPathTemplate(), equalTo("/customers"));
	}

	@Test
	public void shouldChangeEndpointConsumedMediatypesWhenAddingResourceAnnotation() throws JavaModelException,
			CoreException {
		// pre-conditions
		final JaxrsResource customerResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final Annotation consumesAnnotation = getAnnotation(customerResource.getJavaElement(), CONSUMES.qualifiedName);
		customerResource.removeAnnotation(consumesAnnotation.getJavaAnnotation());
		final JaxrsResourceMethod customerResourceMethod = metamodelMonitor.resolveResourceMethod(customerResource, "getCustomer");
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(customerResourceMethod).get(0);
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("*/*")));
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		customerResource.addOrUpdateAnnotation(consumesAnnotation);
		// verifications: 5 endpoints changed (last one remains unchanged)
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(5));
		final JaxrsEndpointDelta change = metamodelMonitor.getEndpointChanges().get(0);
		assertThat(change.getKind(), equalTo(CHANGED));
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("application/xml")));
	}

	@Test
	public void shouldChangeEndpointConsumedMediatypesWhenAddingResourceMethodAnnotation() throws JavaModelException,
			CoreException {
		// pre-conditions
		final JaxrsResource customerResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = metamodelMonitor.resolveResourceMethod(customerResource, "createCustomer");
		customerResource.removeAnnotation(customerResource.getAnnotation(CONSUMES.qualifiedName).getJavaAnnotation());
		customerResourceMethod.removeAnnotation(customerResourceMethod.getAnnotation(CONSUMES.qualifiedName).getJavaAnnotation());
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(customerResourceMethod).get(0);
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("*/*")));
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		customerResourceMethod.addOrUpdateAnnotation(getAnnotation(customerResourceMethod.getJavaElement(),
				CONSUMES.qualifiedName));
		// verifications
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(1));
		final JaxrsEndpointDelta change = metamodelMonitor.getEndpointChanges().get(0);
		assertThat(change.getKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("application/xml")));
	}

	@Test
	public void shouldChangeEndpointConsumedMediatypesWhenChangingResourceMethodAnnotation() throws JavaModelException,
			CoreException {
		// pre-conditions
		final JaxrsResource customerResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = metamodelMonitor.resolveResourceMethod(customerResource, "createCustomer");
		final Annotation consumesAnnotation = getAnnotation(customerResourceMethod.getJavaElement(),
				CONSUMES.qualifiedName);
		customerResourceMethod.addOrUpdateAnnotation(createAnnotation(consumesAnnotation, "application/foo"));
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(customerResourceMethod).get(0);
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("application/foo")));
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		customerResourceMethod.addOrUpdateAnnotation(getAnnotation(customerResourceMethod.getJavaElement(),
				CONSUMES.qualifiedName));
		// verifications
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(1));
		final JaxrsEndpointDelta change = metamodelMonitor.getEndpointChanges().get(0);
		assertThat(change.getKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("application/xml")));
	}

	@Test
	public void shouldChangeEndpointConsumedMediatypesWhenChangingResourceAnnotation() throws JavaModelException,
			CoreException {
		// pre-conditions
		final JaxrsResource customerResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final Annotation consumesAnnotation = getAnnotation(customerResource.getJavaElement(), CONSUMES.qualifiedName);
		customerResource.addOrUpdateAnnotation(createAnnotation(consumesAnnotation, "application/foo"));
		final JaxrsResourceMethod customerResourceMethod = metamodelMonitor.resolveResourceMethod(customerResource, "getCustomer");
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(customerResourceMethod).get(0);
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("application/foo")));
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		customerResource
				.addOrUpdateAnnotation(getAnnotation(customerResource.getJavaElement(), CONSUMES.qualifiedName));
		// verifications: 5 endpoints changed when changing resource annotation
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(5));
		final JaxrsEndpointDelta change = metamodelMonitor.getEndpointChanges().get(0);
		assertThat(change.getKind(), equalTo(CHANGED));
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("application/xml")));
	}

	@Test
	public void shouldChangeEndpointConsumedMediatypesWhenRemovingMethodAnnotationWithResourceDefault()
			throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsResource customerResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final Annotation consumesTypeAnnotation = getAnnotation(customerResource.getJavaElement(),
				CONSUMES.qualifiedName);
		customerResource.addOrUpdateAnnotation(createAnnotation(consumesTypeAnnotation, "application/xml"));
		final JaxrsResourceMethod customerResourceMethod = metamodelMonitor.resolveResourceMethod(customerResource, "createCustomer");
		final Annotation consumesMethodAnnotation = getAnnotation(customerResourceMethod.getJavaElement(),
				CONSUMES.qualifiedName);
		customerResourceMethod.addOrUpdateAnnotation(createAnnotation(consumesMethodAnnotation, "application/foo"));
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(customerResourceMethod).get(0);
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("application/foo")));
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		customerResourceMethod.removeAnnotation(consumesMethodAnnotation.getJavaAnnotation());
		// verifications
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(1));
		final JaxrsEndpointDelta change = metamodelMonitor.getEndpointChanges().get(0);
		assertThat(change.getKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("application/xml")));
	}

	@Test
	public void shouldChangeEndpointConsumedMediatypesWhenRemovingMethodAnnotationWithoutResourceDefault()
			throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsResource customerResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		customerResource.removeAnnotation(customerResource.getAnnotation(CONSUMES.qualifiedName).getJavaAnnotation());
		final JaxrsResourceMethod customerResourceMethod = metamodelMonitor.resolveResourceMethod(customerResource, "createCustomer");
		final Annotation consumesAnnotation = createAnnotation(
				getAnnotation(customerResourceMethod.getJavaElement(), CONSUMES.qualifiedName), "application/foo");
		customerResourceMethod.addOrUpdateAnnotation(consumesAnnotation);
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(customerResourceMethod).get(0);
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("application/foo")));
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		customerResourceMethod.removeAnnotation(consumesAnnotation.getJavaAnnotation());
		// verifications
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(1));
		final JaxrsEndpointDelta change = metamodelMonitor.getEndpointChanges().get(0);
		assertThat(change.getKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("*/*")));
	}

	@Test
	public void shouldChangeEndpointProducedMediatypesWhenAddingResourceAnnotation() throws JavaModelException,
			CoreException {
		// pre-conditions
		final JaxrsResource customerResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		customerResource.removeAnnotation(customerResource.getAnnotation(PRODUCES.qualifiedName).getJavaAnnotation());
		final JaxrsResourceMethod customerResourceMethod = metamodelMonitor.resolveResourceMethod(customerResource, "getCustomerAsVCard");
		customerResourceMethod.removeAnnotation(customerResourceMethod.getAnnotation(PRODUCES.qualifiedName).getJavaAnnotation());
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(customerResourceMethod).get(0);
		assertThat(endpoint.getProducedMediaTypes(), equalTo(Arrays.asList("*/*")));
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		customerResource.addOrUpdateAnnotation(createAnnotation(PRODUCES.qualifiedName, "application/xml"));
		// verifications: all 6 methods changed
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(6));
		for (int i = 0; i < 6; i++) {
			final JaxrsEndpointDelta change = metamodelMonitor.getEndpointChanges().get(i);
			assertThat(change.getKind(), equalTo(CHANGED));
			assertThat(endpoint.getProducedMediaTypes(), equalTo(Arrays.asList("application/xml")));
		}
	}

	@Test
	public void shouldChangeEndpointProducedMediatypesWhenAddingResourceMethodAnnotation() throws JavaModelException,
			CoreException {
		// pre-conditions
		final JaxrsResource customerResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		customerResource.removeAnnotation(customerResource.getAnnotation(PRODUCES.qualifiedName).getJavaAnnotation());
		final JaxrsResourceMethod customerResourceMethod = metamodelMonitor.resolveResourceMethod(customerResource, "getCustomerAsVCard");
		customerResourceMethod.removeAnnotation(customerResourceMethod.getAnnotation(PRODUCES.qualifiedName).getJavaAnnotation());
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(customerResourceMethod).get(0);
		assertThat(endpoint.getProducedMediaTypes(), equalTo(Arrays.asList("*/*")));
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		customerResourceMethod.addOrUpdateAnnotation(getAnnotation(customerResourceMethod.getJavaElement(),
				PRODUCES.qualifiedName));
		// verifications
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(1));
		final JaxrsEndpointDelta change = metamodelMonitor.getEndpointChanges().get(0);
		assertThat(change.getKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
		assertThat(endpoint.getProducedMediaTypes(), equalTo(Arrays.asList("text/x-vcard")));
	}

	@Test
	public void shouldChangeEndpointProducedMediatypesWhenChangingResourceAnnotation() throws JavaModelException,
			CoreException {
		// pre-conditions
		final JaxrsResource customerResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final Annotation producesResourceAnnotation = getAnnotation(customerResource.getJavaElement(),
				PRODUCES.qualifiedName);
		customerResource.addOrUpdateAnnotation(createAnnotation(producesResourceAnnotation, "application/foo"));
		final JaxrsResourceMethod customerResourceMethod = metamodelMonitor.resolveResourceMethod(customerResource, "getCustomer");
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(customerResourceMethod).get(0);
		assertThat(endpoint.getProducedMediaTypes(), equalTo(Arrays.asList("application/foo")));
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		customerResource.addOrUpdateAnnotation(createAnnotation(producesResourceAnnotation, "application/xml"));
		// verifications: 5 changes after resource annotation was changed
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(5));
		final JaxrsEndpointDelta change = metamodelMonitor.getEndpointChanges().get(0);
		assertThat(change.getKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint().getProducedMediaTypes(), equalTo(Arrays.asList("application/xml")));
	}

	@Test
	public void shouldChangeEndpointProducedMediatypesWhenChangingResourceMethodAnnotation() throws JavaModelException,
			CoreException {
		// pre-conditions
		final JaxrsResource customerResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = metamodelMonitor.resolveResourceMethod(customerResource, "getCustomerAsVCard");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final Annotation producesMethodAnnotation = getAnnotation(customerResourceMethod.getJavaElement(),
				PRODUCES.qualifiedName);
		customerResourceMethod.addOrUpdateAnnotation(createAnnotation(producesMethodAnnotation, "text/foo"));
		// verifications
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(1));
		final JaxrsEndpointDelta change = metamodelMonitor.getEndpointChanges().get(0);
		assertThat(change.getKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint().getProducedMediaTypes(), equalTo(Arrays.asList("text/foo")));
	}

	@Test
	public void shouldChangeEndpointProducedMediatypesWhenRemovingMethodAnnotationWithResourceDefault()
			throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsResource customerResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = metamodelMonitor.resolveResourceMethod(customerResource, "getCustomerAsVCard");
		final Annotation producesAnnotation = createAnnotation(
				customerResourceMethod.getAnnotation(PRODUCES.qualifiedName), "application/foo");
		customerResourceMethod.addOrUpdateAnnotation(producesAnnotation);
		// customerResourceMethod.addOrUpdateAnnotation(producesAnnotation);
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(customerResourceMethod).get(0);
		assertThat(endpoint.getProducedMediaTypes(), equalTo(Arrays.asList("application/foo")));
		metamodelMonitor.resetElementChangesNotifications();
		final int numberOfEndpoints = metamodel.getAllEndpoints().size();
		// operation
		customerResourceMethod.removeAnnotation(producesAnnotation.getJavaAnnotation());
		// verifications
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(1));
		final JaxrsEndpointDelta change = metamodelMonitor.getEndpointChanges().get(0);
		assertThat(change.getKind(), equalTo(CHANGED));
		assertThat(metamodel.getAllEndpoints().size(), equalTo(numberOfEndpoints));
		assertThat(change.getEndpoint().getProducedMediaTypes(),
				equalTo(Arrays.asList("application/xml", "application/json")));
		assertThat(change.getEndpoint().getIdentifier(), equalTo(endpoint.getIdentifier()));
	}

	@Test
	public void shouldChangeEndpointProducedMediatypesWhenRemovingMethodAnnotationWithoutResourceDefault()
			throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsResource customerResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = metamodelMonitor.resolveResourceMethod(customerResource, "createCustomer");
		final Annotation consumesAnnotation = customerResourceMethod.getAnnotation(CONSUMES.qualifiedName);
		customerResourceMethod.addOrUpdateAnnotation(createAnnotation(consumesAnnotation, "application/foo"));
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		customerResourceMethod.removeAnnotation(consumesAnnotation.getJavaAnnotation());
		// verifications
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(1));
		final JaxrsEndpointDelta change = metamodelMonitor.getEndpointChanges().get(0);
		assertThat(change.getKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint().getProducedMediaTypes(),
				equalTo(Arrays.asList("application/xml", "application/json")));
	}

	@Test
	public void shouldRemoveEndpointWhenRemovingBuiltinHttpMethodAnnotation() throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = metamodel.findHttpMethodByTypeName("javax.ws.rs.DELETE");
		final JaxrsResource customerResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = metamodelMonitor.resolveResourceMethod(customerResource, "deleteCustomer");
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(customerResourceMethod).get(0);
		assertThat(endpoint.getUriPathTemplate(), equalTo("/customers/{id:Integer}"));
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		httpMethod.remove();
		// verifications
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(1));
		final JaxrsEndpointDelta change = metamodelMonitor.getEndpointChanges().get(0);
		assertThat(change.getKind(), equalTo(REMOVED));
		assertThat(change.getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
	}

	@Test
	public void shouldRemoveEndpointWhenRemovingCustomHttpMethodAnnotation() throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = metamodelMonitor.createHttpMethod("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final JaxrsResource bazResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.BazResource");
		final JaxrsResourceMethod bazResourceMethod = metamodelMonitor.resolveResourceMethod(bazResource, "update3");
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(bazResourceMethod).get(0);
		assertThat(endpoint.getUriPathTemplate(), equalTo("/foo/baz/{param3:String}"));
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		httpMethod.remove();
		// verifications
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(1));
		final JaxrsEndpointDelta change = metamodelMonitor.getEndpointChanges().get(0);
		assertThat(change.getKind(), equalTo(REMOVED));
		assertThat(change.getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
	}

	@Test
	public void shouldRemoveEndpointWhenRemovingResourcePathAnnotationAndMatchingSubresourceLocatorNotFound()
			throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsResource customerResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final Annotation annotation = getAnnotation(customerResource.getJavaElement(), PATH.qualifiedName);
		customerResource.removeAnnotation(annotation.getJavaAnnotation());
		// verifications
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(6));
		final JaxrsEndpointDelta change = metamodelMonitor.getEndpointChanges().get(0);
		assertThat(change.getKind(), equalTo(REMOVED));
	}

	@Test
	public void shouldRemoveEndpointsWhenRemovingRootResource() throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsResource customerResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		customerResource.remove();
		// verifications
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(6));
		for (JaxrsEndpointDelta change : metamodelMonitor.getEndpointChanges()) {
			assertThat(change.getKind(), equalTo(REMOVED));
		}
	}

	@Test
	public void shouldRemoveEndpointsWhenRemovingSubresource() throws JavaModelException, CoreException {
		// pre-conditions
		metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final JaxrsResource bookResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		// adding an extra endpoint that shouldn't be affected
		metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		bookResource.remove();
		// verifications
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(3));
		assertThat(metamodelMonitor.getEndpointChanges().get(0).getKind(), equalTo(REMOVED));
	}

	@Test
	public void shouldRemoveEndpointsWhenRemovingHttpMethod() throws JavaModelException, CoreException {
		// pre-conditions
		metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final JaxrsResource bookResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		final JaxrsResourceMethod bookResourceMethod = metamodelMonitor.resolveResourceMethod(bookResource, "getProduct");
		// adding an extra endpoint that shouldn't be affected
		metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final Annotation httpAnnotation = bookResourceMethod.getHttpMethodAnnotation();
		bookResourceMethod.removeAnnotation(httpAnnotation.getJavaAnnotation());
		// verifications
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getEndpointChanges().get(0).getKind(), equalTo(REMOVED));
	}

	@Test
	public void shouldAddEndpointsWhenChangingSubresourceLocatorReturnType() throws JavaModelException, CoreException {
		// pre-conditions
		IType productResourceLocatorType = replaceFirstOccurrenceOfCode(
				"org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator", javaProject,
				"public Object getProductResourceLocator()", "public BookResource getProductResourceLocator()", false);
		final JaxrsResource productResourceLocator = metamodelMonitor.createResource(productResourceLocatorType);
		metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		// adding an extra subresource that should be affected later
		final JaxrsResource gameResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		metamodelMonitor.resolveResourceMethod(gameResource, "getProduct");
		// endpoints created from BookResource only
		assertThat(metamodel.getAllEndpoints().size(), equalTo(3));
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		productResourceLocatorType = replaceFirstOccurrenceOfCode(
				"org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator", javaProject,
				"public BookResource getProductResourceLocator()", "public Object getProductResourceLocator()", false);
		final JaxrsResourceMethod productResourceLocatorMethod = metamodelMonitor.resolveResourceMethod(productResourceLocator,
				"getProductResourceLocator");
		productResourceLocatorMethod.update(metamodelMonitor.resolveMethod(productResourceLocatorType, "getProductResourceLocator"),
				JdtUtils.parse(productResourceLocatorType, null));
		// verifications: 3 removed, 3+2 added
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(8));
		for (int i = 0; i < 8; i++) {
			if (i < 3) {
				assertThat(metamodelMonitor.getEndpointChanges().get(i).getKind(), equalTo(REMOVED));
			} else {
				assertThat(metamodelMonitor.getEndpointChanges().get(i).getKind(), equalTo(ADDED));
			}
		}
		assertThat(metamodel.getAllEndpoints().size(), equalTo(5));
	}

	@Test
	public void shouldRemoveEndpointsWhenChangingSubresourceLocatorReturnType() throws JavaModelException,
			CoreException {
		// pre-conditions
		final IType productResourceLocatorType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final JaxrsResourceMethod productResourceLocatorMethod = metamodelMonitor.resolveResourceMethod(metamodelMonitor.createResource(productResourceLocatorType),
				"getProductResourceLocator");
		final JaxrsResource bookResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		// adding an extra subresource that should be affected later
		final JaxrsResource gameResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final IType changedType = replaceFirstOccurrenceOfCode(productResourceLocatorType,
				"public Object getProductResourceLocator", "public BookResource getProductResourceLocator", false);
		// method changed, we need to look it up again, without changing the Resource associated with the IType, though...
		final IMethod changedMethod = metamodelMonitor.resolveMethod(changedType, "getProductResourceLocator");
		productResourceLocatorMethod.update(changedMethod, JdtUtils.parse(productResourceLocatorType, null));
		// verifications: 5 removed and then 3 added
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(8));
		assertThat(metamodel.findEndpoints(gameResource).size(), equalTo(0));
		assertThat(metamodel.findEndpoints(bookResource).size(), equalTo(3));
		assertThat(metamodel.getAllEndpoints().size(), equalTo(3));

	}

	@Test
	public void shouldRemoveEndpointsWhenRemovingSubresourceLocatorResource() throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsResource productResourceLocator = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		productResourceLocator.remove();
		// verifications
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(5));
		for (JaxrsEndpointDelta change : metamodelMonitor.getEndpointChanges()) {
			assertThat(change.getKind(), equalTo(REMOVED));
		}
	}

	@Test
	public void shouldRemoveEndpointWhenRemovingResourceMethodInRootResource() throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsResource customerResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		metamodelMonitor.resetElementChangesNotifications();
		assertThat(metamodel.getAllEndpoints().size(), equalTo(6));
		// operation
		final JaxrsResourceMethod customerResourceMethod = metamodelMonitor.resolveResourceMethod(customerResource, "getCustomers");
		customerResourceMethod.remove();
		// verifications
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getEndpointChanges().get(0).getKind(), equalTo(REMOVED));
	}

	@Test
	public void shouldRemoveEndpointWhenRemovingSubresourceMethodInRootResource() throws JavaModelException,
			CoreException {
		// pre-conditions
		final JaxrsResource customerResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = metamodelMonitor.resolveResourceMethod(customerResource, "getCustomer");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final Annotation annotation = getAnnotation(customerResourceMethod.getJavaElement(), PATH.qualifiedName);
		customerResourceMethod.removeAnnotation(annotation.getJavaAnnotation());
		// verifications: 2 events: remove/add
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(2));
		assertThat(metamodelMonitor.getEndpointChanges().get(0).getKind(), equalTo(REMOVED));
		assertThat(metamodelMonitor.getEndpointChanges().get(1).getKind(), equalTo(ADDED));
	}

	@Test
	public void shouldRemoveEndpointWhenRemovingSubresourceLocatorMethod() throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsResource productResourceLocator = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final JaxrsResourceMethod productResourceLocatorMethod = metamodelMonitor.resolveResourceMethod(productResourceLocator,
				"getProductResourceLocator");
		metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		productResourceLocatorMethod.remove();
		// verifications
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(5));
	}

	@Test
	public void shouldRemoveEndpointWhenSubresourceLocatorRootResourceBecomesSubresource() throws JavaModelException,
			CoreException {
		// pre-conditions
		final JaxrsResource productResourceLocator = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		final Annotation productResourceLocatorPathAnnotation = getAnnotation(productResourceLocator.getJavaElement(),
				PATH.qualifiedName);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		productResourceLocator.removeAnnotation(productResourceLocatorPathAnnotation.getJavaAnnotation());
		// verifications
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(5));
		for (JaxrsEndpointDelta change : metamodelMonitor.getEndpointChanges()) {
			assertThat(change.getKind(), equalTo(REMOVED));
		}
	}

}
