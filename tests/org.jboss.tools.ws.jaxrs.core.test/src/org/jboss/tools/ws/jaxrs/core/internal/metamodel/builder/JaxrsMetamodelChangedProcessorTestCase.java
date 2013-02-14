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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isOneOf;
import static org.jboss.tools.ws.jaxrs.core.WorkbenchUtils.changeAnnotationValue;
import static org.jboss.tools.ws.jaxrs.core.WorkbenchUtils.resolveAnnotation;
import static org.jboss.tools.ws.jaxrs.core.WorkbenchUtils.resolveAnnotations;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementDelta.F_ELEMENT_KIND;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementDelta.F_PATH_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.APPLICATION_PATH;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.CONSUMES;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.GET;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.HTTP_METHOD;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.PATH;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.POST;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.PRODUCES;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.jboss.tools.ws.jaxrs.core.AbstractCommonTestCase;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsBaseElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsBuiltinHttpMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsJavaApplication;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsWebxmlApplication;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname;
import org.jboss.tools.ws.jaxrs.core.jdt.JavaMethodSignature;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.JaxrsEndpointDelta;
import org.jboss.tools.ws.jaxrs.core.metamodel.JaxrsMetamodelDelta;
import org.junit.Ignore;
import org.junit.Test;

public class JaxrsMetamodelChangedProcessorTestCase extends AbstractCommonTestCase {

	private final JaxrsMetamodelChangedProcessor delegate = new JaxrsMetamodelChangedProcessor();

	/*
	private JaxrsJavaApplication createJavaApplication(String typeName) throws CoreException, JavaModelException {
		final IType applicationType = getType(typeName, javaProject);
		final Annotation appPathAnnotation = resolveAnnotation(applicationType, APPLICATION_PATH.qualifiedName);
		final JaxrsJavaApplication application = new JaxrsJavaApplication(applicationType,
				appPathAnnotation, true, metamodel);
		metamodel.add(application);
		return application;
	}

	private JaxrsWebxmlApplication createWebxmlApplication(String typeName, String applicationPath) throws CoreException, JavaModelException {
		final JaxrsWebxmlApplication application = new JaxrsWebxmlApplication(typeName,
				applicationPath, WtpUtils.getWebDeploymentDescriptor(project), metamodel);
		metamodel.add(application);
		return application;
	}
	*/
	

	private JaxrsEndpoint createEndpoint(JaxrsMetamodel metamodel, JaxrsHttpMethod httpMethod, JaxrsResourceMethod... resourceMethods) {
		JaxrsEndpoint endpoint = new JaxrsEndpoint(metamodel, httpMethod, new LinkedList<JaxrsResourceMethod>(
				Arrays.asList(resourceMethods)));
		metamodel.add(endpoint);
		return endpoint;
	}

	private JaxrsEndpoint createEndpoint(JaxrsHttpMethod httpMethod, JaxrsResourceMethod... resourceMethods) {
		JaxrsEndpoint endpoint = new JaxrsEndpoint(this.metamodel, httpMethod, new LinkedList<JaxrsResourceMethod>(
				Arrays.asList(resourceMethods)));
		metamodel.add(endpoint);
		return endpoint;
	}

	private List<JaxrsEndpointDelta> processEvent(JaxrsElementDelta affectedElement) throws CoreException {
		JaxrsMetamodelDelta affectedMetamodel = new JaxrsMetamodelDelta(metamodel, CHANGED);
		affectedMetamodel.add(affectedElement);
		delegate.processAffectedMetamodel(affectedMetamodel, new NullProgressMonitor());
		return affectedMetamodel.getAffectedEndpoints();
	}

	@Test
	public void shouldConstructSimpleEndpoint() throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.GET;
		final JaxrsResource customerResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomerAsVCard", customerResource,
				PATH.qualifiedName, GET.qualifiedName);
		customerResourceMethod.addOrUpdateAnnotation(resolveAnnotation(customerResourceMethod.getJavaElement(),
				PRODUCES.qualifiedName));
		// operation
		JaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		// verifications
		assertThat(endpoint.getHttpMethod(), equalTo((IJaxrsHttpMethod) httpMethod));
		assertThat(endpoint.getUriPathTemplate(), equalTo("/customers/{id}"));
		// @produces and @consumes annotations were explicitly declared
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("application/xml")));
		assertThat(endpoint.getProducedMediaTypes(), equalTo(Arrays.asList("text/x-vcard")));
	}

	@Test
	public void shouldConstructEndpointFromSubresource() throws CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.GET;
		final JaxrsResource producLocatorResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final JaxrsResourceMethod productLocatorMethod = createResourceMethod("getProductResourceLocator",
				producLocatorResource, PATH.qualifiedName,GET.qualifiedName);
		final JaxrsResource bookResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		final JaxrsResourceMethod subresourceMethod = createResourceMethod("getProduct", bookResource, PATH.qualifiedName,GET.qualifiedName);
		// operation
		JaxrsEndpoint endpoint = createEndpoint(httpMethod, productLocatorMethod, subresourceMethod);
		// verifications
		assertThat(endpoint.getHttpMethod(), equalTo((IJaxrsHttpMethod) httpMethod));
		assertThat(endpoint.getUriPathTemplate(), equalTo("/products/{productType}/{id}"));
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("*/*")));
		assertThat(endpoint.getProducedMediaTypes(), equalTo(Arrays.asList("application/xml", "application/json")));
	}

	@Test
	public void shouldConstructEndpointWithQueryParams() throws CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.GET;
		final JaxrsResource customerResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomers", customerResource,
				GET.qualifiedName);
		// operation
		JaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		// verifications
		assertThat(endpoint.getHttpMethod(), equalTo((IJaxrsHttpMethod) httpMethod));
		assertThat(endpoint.getUriPathTemplate(), equalTo("/customers?start={start:int}&size={size:int=2}"));
	}

	@Test
	public void shoudCreateEndpointWhenAddingResourceMethodInRootResource() throws CoreException {
		// pre-conditions
		final JaxrsResource customerResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsBaseElement customerResourceMethod = createResourceMethod("getCustomers", customerResource,
				GET.qualifiedName);
		// operation
		JaxrsElementDelta event = new JaxrsElementDelta(customerResourceMethod, ADDED);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(1));
		assertThat(changes.get(0).getDeltaKind(), equalTo(ADDED));
	}

	@Test
	public void shoudCreateEndpointWhenAddingSubresourceMethodInRootResource() throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsResource customerResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsBaseElement customerSubresourceMethod = createResourceMethod("getCustomer", customerResource,
				GET.qualifiedName);
		// operation
		JaxrsElementDelta event = new JaxrsElementDelta(customerSubresourceMethod, ADDED);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(1));
		assertThat(changes.get(0).getDeltaKind(), equalTo(ADDED));
	}

	@Test
	public void shoudCreateEndpointWhenAddingSubresourceLocatorMethodInRootResource() throws JavaModelException,
			CoreException {
		// pre-conditions
		final JaxrsResource bookResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		createResourceMethod("getProduct", bookResource, GET.qualifiedName);
		// createEndpoint(httpMethod, bookResourceMethod);
		final JaxrsResource gameResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		createResourceMethod("getProduct", gameResource, GET.qualifiedName);
		// createEndpoint(httpMethod, gameResourceMethod);

		final JaxrsResource productResourceLocator = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final JaxrsBaseElement productResourceLocatorMethod = createResourceMethod("getProductResourceLocator",
				productResourceLocator);
		// operation
		JaxrsElementDelta event = new JaxrsElementDelta(productResourceLocatorMethod, ADDED);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(2));
		assertThat(changes.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(changes.get(1).getDeltaKind(), equalTo(ADDED));
	}

	@Test
	public void shoudCreateEndpointWhenAddingResourceMethodInSubresource() throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsResource productResourceLocator = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		createResourceMethod("getProductResourceLocator", productResourceLocator);
		final JaxrsResource bookResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		final JaxrsBaseElement bookResourceMethod = createResourceMethod("getAllProducts", bookResource, GET.qualifiedName);
		// operation
		final JaxrsElementDelta event = new JaxrsElementDelta(bookResourceMethod, ADDED);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(1));
		assertThat(changes.get(0).getDeltaKind(), equalTo(ADDED));
	}

	@Test
	public void shoudCreateEndpointWhenChangingSubresourceLocatorMethodIntoSubresourceMethod()
			throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsResource customerResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerSubresourceMethod = createResourceMethod("getCustomer", customerResource);
		assertThat(customerSubresourceMethod.getElementKind(), equalTo(EnumElementKind.SUBRESOURCE_LOCATOR));
		// operation
		Annotation httpAnnotation = resolveAnnotation(customerSubresourceMethod.getJavaElement(), GET.qualifiedName);
		final int flags = customerSubresourceMethod.addOrUpdateAnnotation(httpAnnotation);
		JaxrsElementDelta event = new JaxrsElementDelta(customerSubresourceMethod, CHANGED, flags);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(1));
		assertThat(changes.get(0).getDeltaKind(), equalTo(ADDED));
	}

	@Test
	public void shoudCreateEndpointWhenAddingSubresourceMethodInSubresource() throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsResource productResourceLocator = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		createResourceMethod("getProductResourceLocator", productResourceLocator);
		final JaxrsResource bookResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		final JaxrsBaseElement bookResourceMethod = createResourceMethod("getProduct", bookResource, GET.qualifiedName);
		// operation
		final JaxrsElementDelta event = new JaxrsElementDelta(bookResourceMethod, ADDED);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(1));
		assertThat(changes.get(0).getDeltaKind(), equalTo(ADDED));
	}

	@Test
	@Ignore("deferred for now")
	public void shoudCreateEndpointWhenAddingSubresourceLocatorMethodInSubresource() {
	}

	@Test
	public void shoudChangeUriPathTemplateWhenAddingApplication() throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.GET;
		final JaxrsResource customerResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomer", customerResource,
				PATH.qualifiedName,GET.qualifiedName);
		final JaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		assertThat(endpoint.getUriPathTemplate(), equalTo("/customers/{id}"));
		// operation
		final JaxrsJavaApplication application = createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final JaxrsElementDelta event = new JaxrsElementDelta(application, ADDED);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(1));
		assertThat(changes.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(changes.get(0).getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
		assertThat(changes.get(0).getEndpoint().getUriPathTemplate(), equalTo("/app/customers/{id}"));
	}
	
	@Test
	public void shoudChangeUriPathTemplateWhenRemovingApplicationType() throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.GET;
		final JaxrsJavaApplication application = createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final JaxrsResource customerResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomer", customerResource,
				PATH.qualifiedName,GET.qualifiedName);
		final JaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		assertThat(endpoint.getUriPathTemplate(), equalTo("/app/customers/{id}"));
		// operation
		metamodel.remove(application);
		final JaxrsElementDelta event = new JaxrsElementDelta(application, REMOVED);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(1));
		assertThat(changes.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(changes.get(0).getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
		assertThat(changes.get(0).getEndpoint().getUriPathTemplate(), equalTo("/customers/{id}"));
	}

	@Test
	public void shoudChangeUriPathTemplateWhenRemovingApplicationPathAnnotation() throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.GET;
		final JaxrsJavaApplication application = createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final JaxrsResource customerResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomer", customerResource,
				PATH.qualifiedName,GET.qualifiedName);
		final JaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		assertThat(endpoint.getUriPathTemplate(), equalTo("/app/customers/{id}"));
		// operation
		final Annotation appPathAnnotation = application.getAnnotation(EnumJaxrsClassname.APPLICATION_PATH.qualifiedName);
		application.removeAnnotation(appPathAnnotation);
		final JaxrsElementDelta event = new JaxrsElementDelta(application, CHANGED);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(1));
		assertThat(changes.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(changes.get(0).getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
		assertThat(changes.get(0).getEndpoint().getUriPathTemplate(), equalTo("/customers/{id}"));
	}
	
	@Test
	public void shoudChangeUriPathTemplateWhenAddingResourcePathAnnotation() throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.GET;
		final JaxrsResource customerResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomers", customerResource,
				GET.qualifiedName);
		final JaxrsEndpoint fakeEndpoint = createEndpoint(httpMethod, customerResourceMethod);
		// operation
		final JaxrsElementDelta event = new JaxrsElementDelta(customerResource, CHANGED, F_ELEMENT_KIND
				+ F_PATH_ANNOTATION);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(2));
		assertThat(changes.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(changes.get(0).getEndpoint(), equalTo((IJaxrsEndpoint) fakeEndpoint));
		assertThat(changes.get(1).getDeltaKind(), equalTo(ADDED));
	}

	@Test
	public void shoudChangeUriPathTemplateWhenAddingMethodPathAnnotation() throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.GET;
		final JaxrsResource customerResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomer", customerResource,
				GET.qualifiedName);
		final Annotation annotation = resolveAnnotation(customerResourceMethod.getJavaElement(), PATH.qualifiedName);
		customerResourceMethod.removeAnnotation(annotation.getJavaAnnotation().getHandleIdentifier());
		final JaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		assertThat(endpoint.getUriPathTemplate(), equalTo("/customers"));
		// operation
		customerResourceMethod.addOrUpdateAnnotation(annotation);
		final JaxrsElementDelta event = new JaxrsElementDelta(customerResourceMethod, CHANGED,
				F_PATH_ANNOTATION);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(1));
		final JaxrsEndpointDelta change = changes.get(0);
		assertThat(change.getDeltaKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
		assertThat(change.getEndpoint().getHttpMethod(), equalTo((IJaxrsHttpMethod) httpMethod));
		assertThat(change.getEndpoint().getUriPathTemplate(), equalTo("/customers/{id}"));
	}

	@Test
	public void shoudChangeUriPathTemplateWhenChangingApplicationPathAnnotation() throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsJavaApplication application = createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.GET;
		final JaxrsResource customerResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomer", customerResource, PATH.qualifiedName,GET.qualifiedName);
		final JaxrsEndpoint endpoint = createEndpoint(metamodel, httpMethod, customerResourceMethod);
		assertThat(endpoint.getUriPathTemplate(), equalTo("/app/customers/{id}"));
		// operation
		final Map<String, Annotation> annotations = resolveAnnotations(application.getJavaElement(), APPLICATION_PATH.qualifiedName);
		int flags = application.addOrUpdateAnnotation(changeAnnotationValue(annotations.get(APPLICATION_PATH.qualifiedName), "/foo"));
		final JaxrsElementDelta event = new JaxrsElementDelta(application, CHANGED, flags);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(1));
		final JaxrsEndpointDelta change = changes.get(0);
		assertThat(change.getDeltaKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
		assertThat(change.getEndpoint().getHttpMethod(), equalTo((IJaxrsHttpMethod) httpMethod));
		assertThat(change.getEndpoint().getUriPathTemplate(), equalTo("/foo/customers/{id}"));
	}

	@Test
	public void shoudChangeUriPathTemplateWhenSwitchingToWebxmlCoreApplication() throws JavaModelException, CoreException {
		// pre-conditions
		createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.GET;
		final JaxrsResource customerResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomer", customerResource,
				PATH.qualifiedName,GET.qualifiedName);
		final JaxrsEndpoint endpoint = createEndpoint(metamodel, httpMethod, customerResourceMethod);
		assertThat(endpoint.getUriPathTemplate(), equalTo("/app/customers/{id}"));
		// operation
		final JaxrsWebxmlApplication webxmlApplication = createWebxmlApplication("javax.ws.rs.core.Application", "/foo");
		final JaxrsElementDelta event = new JaxrsElementDelta(webxmlApplication, ADDED);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(1));
		final JaxrsEndpointDelta change = changes.get(0);
		assertThat(change.getDeltaKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
		assertThat(change.getEndpoint().getHttpMethod(), equalTo((IJaxrsHttpMethod) httpMethod));
		assertThat(change.getEndpoint().getUriPathTemplate(), equalTo("/foo/customers/{id}"));
	}
	
	@Test
	public void shoudChangeUriPathTemplateWhenSwitchingBackToJavaApplication() throws JavaModelException, CoreException {
		// pre-conditions
		createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.GET;
		final JaxrsResource customerResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomer", customerResource,
				PATH.qualifiedName,GET.qualifiedName);
		final JaxrsWebxmlApplication webxmlApplication = createWebxmlApplication("javax.ws.rs.core.Application", "/foo");
		final JaxrsEndpoint endpoint = createEndpoint(metamodel, httpMethod, customerResourceMethod);
		assertThat(endpoint.getUriPathTemplate(), equalTo("/foo/customers/{id}"));
		// operation
		metamodel.remove(webxmlApplication);
		final JaxrsElementDelta event = new JaxrsElementDelta(webxmlApplication, REMOVED);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(1));
		final JaxrsEndpointDelta change = changes.get(0);
		assertThat(change.getDeltaKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
		assertThat(change.getEndpoint().getHttpMethod(), equalTo((IJaxrsHttpMethod) httpMethod));
		assertThat(change.getEndpoint().getUriPathTemplate(), equalTo("/app/customers/{id}"));
	}
	
	@Test
	public void shoudChangeUriPathTemplateWhenOverridingApplication() throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsJavaApplication javaApplication = createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.GET;
		final JaxrsResource customerResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomer", customerResource,
				PATH.qualifiedName,GET.qualifiedName);
		final JaxrsEndpoint endpoint = createEndpoint(metamodel, httpMethod, customerResourceMethod);
		assertThat(endpoint.getUriPathTemplate(), equalTo("/app/customers/{id}"));
		// operation
		final JaxrsWebxmlApplication webxmlApplication = createWebxmlApplication(javaApplication.getJavaClassName(), "/foo");
		List<JaxrsEndpointDelta> changes = processEvent(new JaxrsElementDelta(webxmlApplication, ADDED));
		assertThat(changes.size(), equalTo(0));
		// (at the same time, the JavaApplication is changed since this is an override)
		javaApplication.setApplicationPathOverride("/foo");
		changes = processEvent(new JaxrsElementDelta(javaApplication, CHANGED));
		// verifications
		assertThat(changes.size(), equalTo(1));
		final JaxrsEndpointDelta change = changes.get(0);
		assertThat(change.getDeltaKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
		assertThat(change.getEndpoint().getHttpMethod(), equalTo((IJaxrsHttpMethod) httpMethod));
		assertThat(change.getEndpoint().getUriPathTemplate(), equalTo("/foo/customers/{id}"));
	}

	@Test
	public void shoudChangeUriPathTemplateWhenUnoverridingApplication() throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsJavaApplication javaApplication = createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.GET;
		final JaxrsResource customerResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomer", customerResource,
				PATH.qualifiedName,GET.qualifiedName);
		final JaxrsWebxmlApplication webxmlApplication = createWebxmlApplication(javaApplication.getJavaClassName(), "/foo");
		javaApplication.setApplicationPathOverride("/foo");
		final JaxrsEndpoint endpoint = createEndpoint(metamodel, httpMethod, customerResourceMethod);
		assertThat(endpoint.getUriPathTemplate(), equalTo("/foo/customers/{id}"));
		// operation
		List<JaxrsEndpointDelta> changes = processEvent(new JaxrsElementDelta(webxmlApplication, REMOVED));
		assertThat(changes.size(), equalTo(1)); // FAKE change...
		// (at the same time, the JavaApplication is changed since this is an override)
		javaApplication.unsetApplicationPathOverride();
		changes = processEvent(new JaxrsElementDelta(javaApplication, CHANGED));
		// verifications
		assertThat(changes.size(), equalTo(1));
		final JaxrsEndpointDelta change = changes.get(0);
		assertThat(change.getDeltaKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
		assertThat(change.getEndpoint().getHttpMethod(), equalTo((IJaxrsHttpMethod) httpMethod));
		assertThat(change.getEndpoint().getUriPathTemplate(), equalTo("/app/customers/{id}"));
	}
	
	@Test
	public void shoudChangeUriPathTemplateWhenChangingResourcePathAnnotation() throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.GET;
		final JaxrsResource customerResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomer", customerResource,
				PATH.qualifiedName,GET.qualifiedName);
		final JaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		assertThat(endpoint.getUriPathTemplate(), equalTo("/customers/{id}"));
		// operation
		final Annotation pathAnnotation = resolveAnnotation(customerResourceMethod.getJavaElement(), PATH.qualifiedName);	
		customerResource.addOrUpdateAnnotation(changeAnnotationValue(pathAnnotation, "/foo"));
		final JaxrsElementDelta event = new JaxrsElementDelta(customerResource, CHANGED, F_PATH_ANNOTATION);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(1));
		final JaxrsEndpointDelta change = changes.get(0);
		assertThat(change.getDeltaKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
		assertThat(change.getEndpoint().getHttpMethod(), equalTo((IJaxrsHttpMethod) httpMethod));
		assertThat(change.getEndpoint().getUriPathTemplate(), equalTo("/foo/{id}"));
	}

	@Test
	public void shoudChangeUriPathTemplateWhenChangingMethodPathAnnotation() throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.GET;
		final JaxrsResource customerResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomer", customerResource,
				PATH.qualifiedName,GET.qualifiedName);
		final JaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		assertThat(endpoint.getUriPathTemplate(), equalTo("/customers/{id}"));
		// operation
		final Annotation pathAnnotation = resolveAnnotation(customerResourceMethod.getJavaElement(), PATH.qualifiedName);	
		final int flags = customerResourceMethod.addOrUpdateAnnotation(changeAnnotationValue(pathAnnotation, "{foo}"));
		final JaxrsElementDelta event = new JaxrsElementDelta(customerResourceMethod, CHANGED, flags);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(1));
		final JaxrsEndpointDelta change = changes.get(0);
		assertThat(change.getDeltaKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
		assertThat(change.getEndpoint().getHttpMethod(), equalTo((IJaxrsHttpMethod) httpMethod));
		assertThat(change.getEndpoint().getUriPathTemplate(), equalTo("/customers/{foo}"));
	}

	@Test
	public void shoudChangeUriPathTemplateWhenRemovingResourcePathAnnotationAndMatchingSubresourceLocatorFound()
			throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.GET;
		// the subresource locator that will match the resourcemethod when the
		// rootresource becomes a subresource
		final JaxrsResource productResourceLocator = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		createResourceMethod("getProductResourceLocator", productResourceLocator);
		// the root resource that will become a subresource
		final JaxrsResource customerResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomer", customerResource,
				PATH.qualifiedName,GET.qualifiedName);
		final JaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		assertThat(endpoint.getUriPathTemplate(), equalTo("/customers/{id}"));
		// operation
		final Annotation annotation = resolveAnnotation(customerResource.getJavaElement(), PATH.qualifiedName);
		final int flags = customerResource.removeAnnotation(annotation.getJavaAnnotation().getHandleIdentifier());
		final JaxrsElementDelta event = new JaxrsElementDelta(customerResource, CHANGED, flags);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(2));
		final JaxrsEndpointDelta change1 = changes.get(0);
		assertThat(change1.getDeltaKind(), equalTo(REMOVED));
		assertThat(change1.getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
		final JaxrsEndpointDelta change2 = changes.get(1);
		assertThat(change2.getEndpoint().getHttpMethod(), equalTo((IJaxrsHttpMethod) httpMethod));
		assertThat(change2.getEndpoint().getUriPathTemplate(), equalTo("/products/{productType}/{id}"));
	}

	@Test
	public void shoudChangeHttpVerbWhenChangingHttpMethodAnnotation() throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = createHttpMethod("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final JaxrsResource customerResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomer", customerResource,
				PATH.qualifiedName,GET.qualifiedName);
		final JaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		assertThat(endpoint.getUriPathTemplate(), equalTo("/customers/{id}"));
		// operation
		final Annotation httpMethodAnnotation = resolveAnnotation(httpMethod.getJavaElement(), HTTP_METHOD.qualifiedName);	
		int flags = httpMethod.addOrUpdateAnnotation(changeAnnotationValue(httpMethodAnnotation, "BAR"));
		final JaxrsElementDelta event = new JaxrsElementDelta(httpMethod, CHANGED, flags);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(1));
		assertThat(changes.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(changes.get(0).getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
		assertThat(changes.get(0).getEndpoint().getHttpMethod().getHttpVerb(), equalTo("BAR"));
	}

	@Test
	public void shoudChangeUriPathTemplateWhenRemovingMetamodelApplication() throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsJavaApplication application = createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.GET;
		final JaxrsResource customerResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomer", customerResource,
				PATH.qualifiedName,GET.qualifiedName);
		final JaxrsEndpoint endpoint = createEndpoint(metamodel, httpMethod, customerResourceMethod);
		assertThat(endpoint.getUriPathTemplate(), equalTo("/app/customers/{id}"));
		// operation : no 'application' left in the metamodel
		metamodel.remove(application);
		final JaxrsElementDelta event = new JaxrsElementDelta(application, REMOVED);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(1));
		assertThat(changes.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(changes.get(0).getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
		assertThat(changes.get(0).getEndpoint().getHttpMethod(), equalTo((IJaxrsHttpMethod) httpMethod));
		assertThat(changes.get(0).getEndpoint().getUriPathTemplate(), equalTo("/customers/{id}"));
		assertThat(metamodel.getAllApplications().size(), equalTo(0));
	}

	@Test
	public void shoudChangeUriPathTemplateWhenRemovingMethodPathAnnotation() throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.GET;
		final JaxrsResource customerResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomer", customerResource,
				PATH.qualifiedName,GET.qualifiedName);
		final JaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		assertThat(endpoint.getUriPathTemplate(), equalTo("/customers/{id}"));
		// operation
		final Annotation annotation = resolveAnnotation(customerResourceMethod.getJavaElement(), PATH.qualifiedName);
		final int flags = customerResourceMethod.removeAnnotation(annotation.getJavaAnnotation().getHandleIdentifier());
		final JaxrsElementDelta event = new JaxrsElementDelta(customerResourceMethod, CHANGED, flags);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(2));
		assertThat(changes.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(changes.get(0).getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
		assertThat(changes.get(1).getDeltaKind(), equalTo(ADDED));
		assertThat(changes.get(1).getEndpoint().getHttpMethod(), equalTo((IJaxrsHttpMethod) httpMethod));
		assertThat(changes.get(1).getEndpoint().getUriPathTemplate(), equalTo("/customers"));
	}

	@Test
	public void shoudChangeConsumedMediatypesWhenAddingResourceAnnotation() throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.POST;
		final JaxrsResource customerResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		final JaxrsResourceMethod customerResourceMethod = createResourceMethod("createCustomer", customerResource,
				POST.qualifiedName);
		final JaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("*/*")));
		// operation
		final int flags = customerResource.addOrUpdateAnnotation(resolveAnnotation(customerResource.getJavaElement(),
				CONSUMES.qualifiedName));
		final JaxrsElementDelta event = new JaxrsElementDelta(customerResource, CHANGED, flags);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(1));
		final JaxrsEndpointDelta change = changes.get(0);
		assertThat(change.getDeltaKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("application/xml")));
	}

	@Test
	public void shoudChangeConsumedMediatypesWhenAddingResourceMethodAnnotation() throws JavaModelException,
			CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.POST;
		final JaxrsResource customerResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		final JaxrsResourceMethod customerResourceMethod = createResourceMethod("createCustomer", customerResource,
				POST.qualifiedName);
		final JaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("*/*")));
		// operation
		final int flags = customerResourceMethod.addOrUpdateAnnotation(resolveAnnotation(
				customerResourceMethod.getJavaElement(), CONSUMES.qualifiedName));
		final JaxrsElementDelta event = new JaxrsElementDelta(customerResourceMethod, CHANGED, flags);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(1));
		final JaxrsEndpointDelta change = changes.get(0);
		assertThat(change.getDeltaKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("application/xml")));
	}

	@Test
	public void shoudChangeConsumedMediatypesWhenChangingResourceMethodAnnotation() throws JavaModelException,
			CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.POST;
		final JaxrsResource customerResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = resourceMethodBuilder(customerResource, "createCustomer")
				.annotation(POST.qualifiedName).annotation(CONSUMES.qualifiedName, "application/foo")
				.build();
		final JaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("application/foo")));
		// operation
		int flags = customerResourceMethod.addOrUpdateAnnotation(resolveAnnotation(customerResourceMethod.getJavaElement(),
				CONSUMES.qualifiedName));
		final JaxrsElementDelta event = new JaxrsElementDelta(customerResourceMethod, CHANGED, flags);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(1));
		final JaxrsEndpointDelta change = changes.get(0);
		assertThat(change.getDeltaKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("application/xml")));
	}

	@Test
	public void shoudChangeConsumedMediatypesWhenChangingResourceAnnotation() throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.POST;
		final JaxrsResource customerResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final Annotation consumesAnnotation = resolveAnnotation(customerResource.getJavaElement(), CONSUMES.qualifiedName);	
		customerResource.addOrUpdateAnnotation(changeAnnotationValue(consumesAnnotation, "application/foo"));
		final JaxrsResourceMethod customerResourceMethod = createResourceMethod("createCustomer", customerResource,
				POST.qualifiedName);
		final JaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("application/foo")));
		// operation
		int flags = customerResource.addOrUpdateAnnotation(resolveAnnotation(customerResource.getJavaElement(),
				CONSUMES.qualifiedName));
		final JaxrsElementDelta event = new JaxrsElementDelta(customerResourceMethod, CHANGED, flags);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(1));
		final JaxrsEndpointDelta change = changes.get(0);
		assertThat(change.getDeltaKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("application/xml")));
	}

	@Test
	public void shoudChangeConsumedMediatypesWhenRemovingMethodAnnotationWithResourceDefault()
			throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.POST;
		final JaxrsResource customerResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final Annotation consumesTypeAnnotation = resolveAnnotation(customerResource.getJavaElement(), CONSUMES.qualifiedName);	
		customerResource.addOrUpdateAnnotation(changeAnnotationValue(consumesTypeAnnotation, "application/xml"));
		final JaxrsResourceMethod customerResourceMethod = createResourceMethod("createCustomer", customerResource,
				POST.qualifiedName);
		final Annotation consumesMethodAnnotation = resolveAnnotation(customerResourceMethod.getJavaElement(), CONSUMES.qualifiedName);	
		customerResourceMethod.addOrUpdateAnnotation(changeAnnotationValue(consumesMethodAnnotation, "application/foo"));
		final JaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("application/foo")));
		// operation
		int flags = customerResourceMethod.removeAnnotation(consumesMethodAnnotation.getJavaAnnotation().getHandleIdentifier());
		final JaxrsElementDelta event = new JaxrsElementDelta(customerResourceMethod, CHANGED, flags);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(1));
		final JaxrsEndpointDelta change = changes.get(0);
		assertThat(change.getDeltaKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("application/xml")));
	}

	@Test
	public void shoudChangeConsumedMediatypesWhenRemovingMethodAnnotationWithoutResourceDefault()
			throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.POST;
		final JaxrsResource customerResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		final JaxrsResourceMethod customerResourceMethod = createResourceMethod("createCustomer", customerResource,
				POST.qualifiedName);
		final Annotation consumesAnnotation = changeAnnotationValue(resolveAnnotation(customerResourceMethod.getJavaElement(), CONSUMES.qualifiedName), "application/foo");
		customerResourceMethod.addOrUpdateAnnotation(consumesAnnotation);
		final JaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("application/foo")));
		// operation
		int flags = customerResourceMethod.removeAnnotation(consumesAnnotation.getJavaAnnotation().getHandleIdentifier());
		final JaxrsElementDelta event = new JaxrsElementDelta(customerResourceMethod, CHANGED, flags);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(1));
		final JaxrsEndpointDelta change = changes.get(0);
		assertThat(change.getDeltaKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("*/*")));
	}

	@Test
	public void shoudChangeProducedMediatypesWhenAddingResourceAnnotation() throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.GET;
		final JaxrsResource customerResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		final JaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomerAsVCard", customerResource,
				GET.qualifiedName);
		final JaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		assertThat(endpoint.getProducedMediaTypes(), equalTo(Arrays.asList("*/*")));
		// operation
		final Annotation producesAnnotation = resolveAnnotation(customerResource.getJavaElement(), PRODUCES.qualifiedName);
		final int flags = customerResource.addOrUpdateAnnotation(changeAnnotationValue(producesAnnotation, "application/xml"));
		final JaxrsElementDelta event = new JaxrsElementDelta(customerResource, CHANGED, flags);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(1));
		final JaxrsEndpointDelta change = changes.get(0);
		assertThat(change.getDeltaKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
		assertThat(endpoint.getProducedMediaTypes(), equalTo(Arrays.asList("application/xml")));
	}

	@Test
	public void shoudChangeProducedMediatypesWhenAddingResourceMethodAnnotation() throws JavaModelException,
			CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.GET;
		final JaxrsResource customerResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		final JaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomerAsVCard", customerResource,
				GET.qualifiedName);
		final JaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		assertThat(endpoint.getProducedMediaTypes(), equalTo(Arrays.asList("*/*")));
		// operation
		final int flags = customerResourceMethod.addOrUpdateAnnotation(resolveAnnotation(
				customerResourceMethod.getJavaElement(), PRODUCES.qualifiedName));
		final JaxrsElementDelta event = new JaxrsElementDelta(customerResourceMethod, CHANGED, flags);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(1));
		final JaxrsEndpointDelta change = changes.get(0);
		assertThat(change.getDeltaKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
		assertThat(endpoint.getProducedMediaTypes(), equalTo(Arrays.asList("text/x-vcard")));
	}

	@Test
	public void shoudChangeProducedMediatypesWhenChangingResourceAnnotation() throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.GET;
		final JaxrsResource customerResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		final Annotation producesResourceAnnotation = resolveAnnotation(customerResource.getJavaElement(), PRODUCES.qualifiedName);
		customerResource.addOrUpdateAnnotation(changeAnnotationValue(producesResourceAnnotation, "application/foo"));
		final JaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomerAsVCard", customerResource,
				PATH.qualifiedName,GET.qualifiedName);
		final JaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		assertThat(endpoint.getProducedMediaTypes(), equalTo(Arrays.asList("application/foo")));
		// operation
		int flags = customerResource.addOrUpdateAnnotation(changeAnnotationValue(producesResourceAnnotation, "application/xml"));
		final JaxrsElementDelta event = new JaxrsElementDelta(customerResource, CHANGED, flags);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(1));
		final JaxrsEndpointDelta change = changes.get(0);
		assertThat(change.getDeltaKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
		assertThat(endpoint.getProducedMediaTypes(), equalTo(Arrays.asList("application/xml")));
	}

	@Test
	public void shoudChangeProducedMediatypesWhenChangingResourceMethodAnnotation() throws JavaModelException,
			CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.POST;
		final JaxrsResource customerResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomerAsVCard", customerResource,
				GET.qualifiedName, PATH.qualifiedName);
		final JaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		assertThat(endpoint.getProducedMediaTypes(), equalTo(Arrays.asList("application/xml", "application/json")));
		// operation
		final Annotation producesMethodAnnotation = resolveAnnotation(customerResourceMethod.getJavaElement(), PRODUCES.qualifiedName);
		int flags = customerResourceMethod.addOrUpdateAnnotation(changeAnnotationValue(producesMethodAnnotation, "text/x-vcard"));
		final JaxrsElementDelta event = new JaxrsElementDelta(customerResourceMethod, CHANGED, flags);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(1));
		final JaxrsEndpointDelta change = changes.get(0);
		assertThat(change.getDeltaKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
		assertThat(endpoint.getProducedMediaTypes(), equalTo(Arrays.asList("text/x-vcard")));
	}

	@Test
	public void shoudChangeProducedMediatypesWhenRemovingMethodAnnotationWithResourceDefault()
			throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.GET;
		final JaxrsResource customerResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomerAsVCard", customerResource,
				PATH.qualifiedName,POST.qualifiedName, PRODUCES.qualifiedName);
		final Annotation producesAnnotation = changeAnnotationValue(customerResourceMethod.getAnnotation(PRODUCES.qualifiedName), "application/foo");
		customerResourceMethod.addOrUpdateAnnotation(producesAnnotation);
		//customerResourceMethod.addOrUpdateAnnotation(producesAnnotation);
		final JaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		assertThat(endpoint.getProducedMediaTypes(), equalTo(Arrays.asList("application/foo")));
		// operation
		int flags = customerResourceMethod.removeAnnotation(producesAnnotation.getJavaAnnotation().getHandleIdentifier());
		final JaxrsElementDelta event = new JaxrsElementDelta(customerResourceMethod, CHANGED, flags);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(1));
		final JaxrsEndpointDelta change = changes.get(0);
		assertThat(change.getDeltaKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
		assertThat(endpoint.getProducedMediaTypes(), equalTo(Arrays.asList("application/xml", "application/json")));
	}

	@Test
	public void shoudChangeProducedMediatypesWhenRemovingMethodAnnotationWithoutResourceDefault()
			throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.POST;
		final JaxrsResource customerResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = createResourceMethod("createCustomer", customerResource,
				PATH.qualifiedName,POST.qualifiedName);
		final Annotation consumesAnnotation = customerResource.getAnnotation(CONSUMES.qualifiedName);
		customerResourceMethod.addOrUpdateAnnotation(changeAnnotationValue(consumesAnnotation, "application/foo"));
		final JaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		assertThat(endpoint.getConsumedMediaTypes(), equalTo(Arrays.asList("application/foo")));
		// operation
		int flags = customerResourceMethod.removeAnnotation(consumesAnnotation.getJavaAnnotation().getHandleIdentifier());
		final JaxrsElementDelta event = new JaxrsElementDelta(customerResourceMethod, CHANGED, flags);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(1));
		final JaxrsEndpointDelta change = changes.get(0);
		assertThat(change.getDeltaKind(), equalTo(CHANGED));
		assertThat(change.getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
		assertThat(endpoint.getProducedMediaTypes(), equalTo(Arrays.asList("application/xml", "application/json")));
	}

	@Test
	public void shoudRemoveEndpointWhenRemovingHttpMethodAnnotation() throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = createHttpMethod("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final JaxrsResource customerResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomer", customerResource,
				PATH.qualifiedName,GET.qualifiedName);
		final JaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		assertThat(endpoint.getUriPathTemplate(), equalTo("/customers/{id}"));
		// operation
		final JaxrsElementDelta event = new JaxrsElementDelta(httpMethod, REMOVED);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(1));
		final JaxrsEndpointDelta change = changes.get(0);
		assertThat(change.getDeltaKind(), equalTo(REMOVED));
		assertThat(change.getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
	}

	@Test
	public void shoudRemoveEndpointWhenRemovingResourcePathAnnotationAndMatchingSubresourceLocatorNotFound()
			throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.GET;
		final JaxrsResource customerResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomer", customerResource,
				PATH.qualifiedName,GET.qualifiedName);

		final JaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		assertThat(endpoint.getUriPathTemplate(), equalTo("/customers/{id}"));
		// operation
		final Annotation annotation = resolveAnnotation(customerResource.getJavaElement(), PATH.qualifiedName);
		final int flags = customerResource.removeAnnotation(annotation.getJavaAnnotation().getHandleIdentifier());
		final JaxrsElementDelta event = new JaxrsElementDelta(customerResource, CHANGED, flags);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(1));
		final JaxrsEndpointDelta change = changes.get(0);
		assertThat(change.getDeltaKind(), equalTo(REMOVED));
		assertThat(change.getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
	}

	@Test
	public void shoudRemoveEndpointsWhenRemovingRootResource() throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.GET;
		final JaxrsResource customerResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod1 = createResourceMethod("getCustomer", customerResource,
				GET.qualifiedName);
		final JaxrsEndpoint endpoint1 = createEndpoint(httpMethod, customerResourceMethod1);
		final JaxrsResourceMethod customerResourceMethod2 = createResourceMethod("getCustomers", customerResource,
				GET.qualifiedName);
		final JaxrsEndpoint endpoint2 = createEndpoint(httpMethod, customerResourceMethod2);
		// operation
		final JaxrsElementDelta event = new JaxrsElementDelta(customerResource, REMOVED);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(2));
		for (JaxrsEndpointDelta change : changes) {
			assertThat(change.getDeltaKind(), equalTo(REMOVED));
			assertThat(change.getEndpoint(), isOneOf((IJaxrsEndpoint) endpoint1, (IJaxrsEndpoint) endpoint2));
		}
	}

	@Test
	public void shoudRemoveEndpointsWhenRemovingSubresource() throws JavaModelException, CoreException {
		// pre-conditions
		JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.GET;
		final JaxrsResource productResourceLocator = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final JaxrsResourceMethod productResourceLocatorMethod = createResourceMethod("getProductResourceLocator",
				productResourceLocator);
		final JaxrsResource bookResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		final JaxrsResourceMethod bookResourceMethod = createResourceMethod("getProduct", bookResource, GET.qualifiedName);
		final JaxrsEndpoint bookEndpoint = createEndpoint(httpMethod, productResourceLocatorMethod, bookResourceMethod);
		// adding an extra endpoint that shouldn't be affected
		final JaxrsResource gameResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		final JaxrsResourceMethod gameResourceMethod = createResourceMethod("getProduct", gameResource, GET.qualifiedName);
		createEndpoint(httpMethod, gameResourceMethod);
		// operation
		final JaxrsElementDelta event = new JaxrsElementDelta(bookResource, REMOVED);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(1));
		assertThat(changes.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(changes.get(0).getEndpoint(), equalTo((IJaxrsEndpoint) bookEndpoint));
	}

	@Test
	public void shoudRemoveEndpointsWhenRemovingHttpMethod() throws JavaModelException, CoreException {
		// pre-conditions
		JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.GET;
		final JaxrsResource productResourceLocator = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		createResourceMethod("getProductResourceLocator", productResourceLocator);
		final JaxrsResource bookResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		final JaxrsResourceMethod bookResourceMethod = createResourceMethod("getProduct", bookResource, GET.qualifiedName);
		final JaxrsEndpoint bookEndpoint = createEndpoint(httpMethod, bookResourceMethod);
		// adding an extra endpoint that shouldn't be affected
		final JaxrsResource gameResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		final JaxrsResourceMethod gameResourceMethod = createResourceMethod("getProduct", gameResource, GET.qualifiedName);
		createEndpoint(httpMethod, gameResourceMethod);
		final Annotation httpAnnotation = bookResourceMethod.getHttpMethodAnnotation();
		final int flags = bookResourceMethod.removeAnnotation(httpAnnotation.getJavaAnnotation().getHandleIdentifier());
		// operation
		final JaxrsElementDelta event = new JaxrsElementDelta(bookResourceMethod, REMOVED, flags);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(1));
		assertThat(changes.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(changes.get(0).getEndpoint(), equalTo((IJaxrsEndpoint) bookEndpoint));
	}

	@Test
	public void shoudAddEndpointsWhenChangingSubresourceLocatorReturnType() throws JavaModelException, CoreException {
		// pre-conditions
		JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.GET;
		final JaxrsResource productResourceLocator = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final JaxrsResourceMethod productResourceLocatorMethod = createResourceMethod("getProductResourceLocator",
				productResourceLocator);
		final JaxrsResource bookResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		final JaxrsResourceMethod bookResourceMethod = createResourceMethod("getProduct", bookResource, GET.qualifiedName);
		productResourceLocatorMethod.update(new JavaMethodSignature(productResourceLocatorMethod.getJavaElement(),
				bookResource.getJavaElement(), productResourceLocatorMethod.getJavaMethodParameters()));
		createEndpoint(httpMethod, productResourceLocatorMethod, bookResourceMethod);
		// adding an extra subresource that should be affected later
		final JaxrsResource gameResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		final JaxrsBaseElement gameResourceMethod = createResourceMethod("getProduct", gameResource, GET.qualifiedName);
		assertThat(metamodel.getAllEndpoints().size(), equalTo(1));
		// operation
		final IType objectType = resolveType(Object.class.getName());
		int flags = productResourceLocatorMethod.update(new JavaMethodSignature(productResourceLocatorMethod
				.getJavaElement(), objectType, productResourceLocatorMethod.getJavaMethodParameters()));
		final JaxrsElementDelta event = new JaxrsElementDelta(productResourceLocatorMethod, CHANGED,
				flags);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(1));
		assertThat(changes.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(changes.get(0).getEndpoint().getResourceMethods().contains(gameResourceMethod), is(true));
		assertThat(metamodel.getAllEndpoints().size(), equalTo(2));
	}

	@Test
	public void shoudRemoveEndpointsWhenChangingSubresourceLocatorReturnType() throws JavaModelException, CoreException {
		// pre-conditions
		JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.GET;
		final JaxrsResource productResourceLocator = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final JaxrsResourceMethod productResourceLocatorMethod = createResourceMethod("getProductResourceLocator",
				productResourceLocator);
		final JaxrsResource bookResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		final JaxrsResourceMethod bookResourceMethod = createResourceMethod("getProduct", bookResource, GET.qualifiedName);
		createEndpoint(httpMethod, productResourceLocatorMethod, bookResourceMethod);
		// adding an extra subresource that should be affected later
		final JaxrsResource gameResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		final JaxrsResourceMethod gameResourceMethod = createResourceMethod("getProduct", gameResource, GET.qualifiedName);
		createEndpoint(httpMethod, productResourceLocatorMethod, gameResourceMethod);
		assertThat(metamodel.getAllEndpoints().size(), equalTo(2));
		// operation
		final IType bookResourceType = bookResource.getJavaElement();
		int flags = productResourceLocatorMethod.update(new JavaMethodSignature(productResourceLocatorMethod
				.getJavaElement(), bookResourceType, productResourceLocatorMethod.getJavaMethodParameters()));
		final JaxrsElementDelta event = new JaxrsElementDelta(productResourceLocatorMethod, CHANGED,
				flags);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(1));
		assertThat(changes.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(changes.get(0).getEndpoint().getResourceMethods().contains(gameResourceMethod), is(true));
		assertThat(metamodel.getAllEndpoints().size(), equalTo(1));

	}

	@Test
	public void shoudRemoveEndpointsWhenRemovingSubresourceLocatorResource() throws JavaModelException, CoreException {
		// pre-conditions
		JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.GET;
		final JaxrsResource productResourceLocator = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final JaxrsResourceMethod productResourceLocatorMethod = createResourceMethod("getProductResourceLocator",
				productResourceLocator);
		final JaxrsResource bookResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		final JaxrsResourceMethod bookResourceMethod = createResourceMethod("getProduct", bookResource, GET.qualifiedName);
		final JaxrsEndpoint bookEndpoint = createEndpoint(httpMethod, productResourceLocatorMethod, bookResourceMethod);
		final JaxrsResource gameResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		final JaxrsResourceMethod gameResourceMethod = createResourceMethod("getProduct", gameResource, GET.qualifiedName);
		final JaxrsEndpoint gameEndpoint = createEndpoint(httpMethod, productResourceLocatorMethod, gameResourceMethod);
		// operation
		final JaxrsElementDelta event = new JaxrsElementDelta(productResourceLocator, REMOVED);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(2));
		for (JaxrsEndpointDelta change : changes) {
			assertThat(change.getDeltaKind(), equalTo(REMOVED));
			assertThat(change.getEndpoint(), isOneOf((IJaxrsEndpoint) bookEndpoint, (IJaxrsEndpoint) gameEndpoint));
		}
	}

	@Test
	public void shoudRemoveEndpointWhenRemovingResourceMethodInRootResource() throws JavaModelException, CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.GET;
		final JaxrsResource customerResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomers", customerResource,
				GET.qualifiedName);
		final JaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		// operation
		final JaxrsElementDelta event = new JaxrsElementDelta(customerResourceMethod, REMOVED);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(1));
		final JaxrsEndpointDelta change = changes.get(0);
		assertThat(change.getDeltaKind(), equalTo(REMOVED));
		assertThat(change.getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
	}

	@Test
	public void shoudRemoveEndpointWhenRemovingSubresourceMethodInRootResource() throws JavaModelException,
			CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.GET;
		final JaxrsResource customerResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod customerResourceMethod = createResourceMethod("getCustomer", customerResource,
				GET.qualifiedName);
		final JaxrsEndpoint endpoint = createEndpoint(httpMethod, customerResourceMethod);
		// operation
		final Annotation annotation = resolveAnnotation(customerResourceMethod.getJavaElement(), PATH.qualifiedName);
		final int flags = customerResourceMethod.removeAnnotation(annotation.getJavaAnnotation().getHandleIdentifier());
		final JaxrsElementDelta event = new JaxrsElementDelta(customerResourceMethod, REMOVED, flags);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(1));
		final JaxrsEndpointDelta change = changes.get(0);
		assertThat(change.getDeltaKind(), equalTo(REMOVED));
		assertThat(change.getEndpoint(), equalTo((IJaxrsEndpoint) endpoint));
	}

	@Test
	public void shoudRemoveEndpointWhenRemovingSubresourceLocatorMethod() throws JavaModelException, CoreException {
		// pre-conditions
		JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.GET;
		final JaxrsResource productResourceLocator = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final JaxrsResourceMethod productResourceLocatorMethod = createResourceMethod("getProductResourceLocator",
				productResourceLocator);
		final JaxrsResource bookResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		final JaxrsResourceMethod bookResourceMethod = createResourceMethod("getProduct", bookResource, GET.qualifiedName);
		final JaxrsEndpoint bookEndpoint = createEndpoint(httpMethod, productResourceLocatorMethod, bookResourceMethod);
		final JaxrsResource gameResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		final JaxrsResourceMethod gameResourceMethod = createResourceMethod("getProduct", gameResource, GET.qualifiedName);
		final JaxrsEndpoint gameEndpoint = createEndpoint(httpMethod, productResourceLocatorMethod, gameResourceMethod);
		// operation
		final JaxrsElementDelta event = new JaxrsElementDelta(productResourceLocatorMethod, REMOVED);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(2));
		for (JaxrsEndpointDelta change : changes) {
			assertThat(change.getDeltaKind(), equalTo(REMOVED));
			assertThat(change.getEndpoint(), isOneOf((IJaxrsEndpoint) bookEndpoint, (IJaxrsEndpoint) gameEndpoint));
		}
	}

	@Test
	public void shoudRemoveEndpointWhenSubresourceLocatorRootResourceBecomesSubresource() throws JavaModelException,
			CoreException {
		// pre-conditions
		JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.GET;
		final JaxrsResource productResourceLocator = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final JaxrsResourceMethod productResourceLocatorMethod = createResourceMethod("getProductResourceLocator",
				productResourceLocator);
		final JaxrsResource bookResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		final JaxrsResourceMethod bookResourceMethod = createResourceMethod("getProduct", bookResource, GET.qualifiedName);
		final JaxrsEndpoint bookEndpoint = createEndpoint(httpMethod, productResourceLocatorMethod, bookResourceMethod);
		final JaxrsResource gameResource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		final JaxrsResourceMethod gameResourceMethod = createResourceMethod("getProduct", gameResource, GET.qualifiedName);
		final JaxrsEndpoint gameEndpoint = createEndpoint(httpMethod, productResourceLocatorMethod, gameResourceMethod);
		final Annotation productResourceLocatorPathAnnotation = resolveAnnotation(productResourceLocator.getJavaElement(),
				PATH.qualifiedName);
		final int flags = productResourceLocator.removeAnnotation(productResourceLocatorPathAnnotation
				.getJavaAnnotation().getHandleIdentifier());
		// operation
		final JaxrsElementDelta event = new JaxrsElementDelta(productResourceLocator, CHANGED, flags);
		final List<JaxrsEndpointDelta> changes = processEvent(event);
		// verifications
		assertThat(changes.size(), equalTo(2));
		for (JaxrsEndpointDelta change : changes) {
			assertThat(change.getDeltaKind(), equalTo(REMOVED));
			assertThat(change.getEndpoint(), isOneOf((IJaxrsEndpoint) bookEndpoint, (IJaxrsEndpoint) gameEndpoint));
		}
	}
	
	
}
