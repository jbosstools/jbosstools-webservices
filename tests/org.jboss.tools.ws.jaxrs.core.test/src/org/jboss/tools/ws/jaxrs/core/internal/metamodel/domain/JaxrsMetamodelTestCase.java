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
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain;

import static org.eclipse.jdt.core.IJavaElementDelta.CHANGED;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.builder.AbstractMetamodelBuilderTestCase;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementCategory;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResource;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta;
import org.junit.Assert;
import org.junit.Test;

public class JaxrsMetamodelTestCase extends AbstractMetamodelBuilderTestCase {

	final IProgressMonitor progressMonitor = new NullProgressMonitor();

	@Test
	public void shouldFindHttpMethodByType() throws CoreException {
		IType javaType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		assertThat(metamodel.findHttpMethodByTypeName(javaType.getFullyQualifiedName()), notNullValue());
	}

	@Test
	public void shouldNotFindHttpMethodByType() throws CoreException {
		IType javaType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		assertThat(metamodel.findHttpMethodByTypeName(javaType.getFullyQualifiedName()), nullValue());
	}

	@Test
	public void shouldAssertHTTPMethods() throws CoreException {
		// 6 fixed HttpMethods as part of the JAX-RS API (@GET, etc.) + 1 in the
		// project
		// (@FOO)
		Assert.assertEquals(1 * 7, metamodel.findAllHttpMethods().size());
		Set<IJaxrsHttpMethod> jaxrsHttpMethods = new HashSet<IJaxrsHttpMethod>();
		for (IJaxrsHttpMethod httpMethod : metamodel.findAllHttpMethods()) {
			// toString() called for code coverage
			Assert.assertNotNull(httpMethod.toString());
			Assert.assertTrue(jaxrsHttpMethods.add(httpMethod));
		}
	}

	@Test
	public void shouldAssertResourcesAndMethods() throws CoreException {
		// for now, the result excludes the (binary) AsynchronousDispatcher, and
		// hence, its (sub)resources
		final List<IJaxrsResource> resources = metamodel.getAllResources();
		Assert.assertEquals(7, resources.size());
		for (IJaxrsResource jaxrsResource : resources) {
			assertThat(((JaxrsResource) jaxrsResource).getJavaElement(), notNullValue());
			assertThat(((JaxrsResource) jaxrsResource).getElementKind().getCategory(), equalTo(EnumElementCategory.RESOURCE));
			assertThat(jaxrsResource.getAllMethods().size(), greaterThan(0));
		}
	}

	@Test
	public void shouldAssertResolvedEndpoints() throws CoreException {
		Collection<IJaxrsEndpoint> endpoints = metamodel.getAllEndpoints();
		Collections.sort(new ArrayList<IJaxrsEndpoint>(endpoints), new Comparator<IJaxrsEndpoint>() {

			@Override
			public int compare(IJaxrsEndpoint o1, IJaxrsEndpoint o2) {
				return o1.getResourceMethods().getLast().toString().compareTo(o2.getResourceMethods().getLast().toString());
			}
		});
		Assert.assertEquals("Wrong result", 22, endpoints.size());
		for (IJaxrsEndpoint endpoint : endpoints) {
			Assert.assertFalse("Empty list of resourceMethods", endpoint.getResourceMethods().isEmpty());
			Assert.assertNotNull("No URI Path template", endpoint.getUriPathTemplate());
			Assert.assertFalse("No URI Path template:" + endpoint.getUriPathTemplate(), endpoint.getUriPathTemplate()
					.contains("null"));
			Assert.assertFalse("Found some '//' in the uri path template in " + endpoint.getUriPathTemplate(), endpoint
					.getUriPathTemplate().contains("//"));
			Assert.assertNotNull("No Http Method", endpoint.getHttpMethod());
			Assert.assertFalse("No consumed media types", endpoint.getConsumedMediaTypes().isEmpty());
			Assert.assertFalse("No produced media types", endpoint.getProducedMediaTypes().isEmpty());
		}
	}

	@Test
	public void shouldRetrieveAllApplicationPaths() throws CoreException {
		assertThat(metamodel.getAllApplications().size(), equalTo(2));
		assertThat(metamodel.findWebxmlApplications().size(), equalTo(1));
		assertThat(metamodel.getJavaApplications().size(), equalTo(1));
		assertThat(metamodel.getApplication().getApplicationPath(), equalTo("/hello"));
	}

	@Test
	public void shouldRetrieveCustomerResource() throws CoreException {
		IType customerType = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IJaxrsResource customerResource = (IJaxrsResource) metamodel.findElement(customerType);
		Assert.assertNotNull("CustomerResource not found", customerType);
		Assert.assertEquals("Wrong number of resource resourceMethods", 6, customerResource.getAllMethods().size());
	}

	@Test
	public void shouldRetrieveCustomerResourceMethodProposals() throws CoreException {
		IType customerType = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		IMethod customerMethod = getJavaMethod(customerType, "getCustomers");
		final IJaxrsResourceMethod customerResourceMethod = (IJaxrsResourceMethod) metamodel.findElement(customerMethod);
		Assert.assertThat(customerResourceMethod, notNullValue());
		Assert.assertThat(customerResourceMethod.getPathParamValueProposals().size(), equalTo(0));
	}

	@Test
	public void shouldRetrieveCustomerSubresourceMethodProposals() throws CoreException {
		IType customerType = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		IMethod customerMethod = getJavaMethod(customerType, "getCustomer");
		final IJaxrsResourceMethod customerResourceMethod = (IJaxrsResourceMethod) metamodel.findElement(customerMethod);
		Assert.assertThat(customerResourceMethod, notNullValue());
		Assert.assertThat(customerResourceMethod.getPathParamValueProposals().keySet(), containsInAnyOrder("id"));
	}

	@Test
	public void shouldRetrieveBookResourceMethodProposals() throws CoreException {
		IType customerType = getType("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		IMethod customerMethod = getJavaMethod(customerType, "getAllProducts");
		final IJaxrsResourceMethod customerResourceMethod = (IJaxrsResourceMethod) metamodel.findElement(customerMethod);
		Assert.assertThat(customerResourceMethod, notNullValue());
		Assert.assertThat(customerResourceMethod.getPathParamValueProposals().size(), equalTo(0));
	}

	@Test
	public void shouldRetrieveBookSubresourceMethodProposals() throws CoreException {
		IType customerType = getType("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		IMethod customerMethod = getJavaMethod(customerType, "getProduct");
		final IJaxrsResourceMethod customerResourceMethod = (IJaxrsResourceMethod) metamodel.findElement(customerMethod);
		Assert.assertThat(customerResourceMethod, notNullValue());
		Assert.assertThat(customerResourceMethod.getPathParamValueProposals().keySet(), containsInAnyOrder("id"));
	}

	@Test
	public void shouldRetrieveBarResourceMethodProposals() throws CoreException {
		IType bazType = getType("org.jboss.tools.ws.jaxrs.sample.services.BazResource");
		IMethod bazMethod = getJavaMethod(bazType, "getContent2");
		final IJaxrsResourceMethod customerResourceMethod = (IJaxrsResourceMethod) metamodel.findElement(bazMethod);
		Assert.assertThat(customerResourceMethod, notNullValue());
		final Set<String> pathParamValueProposals = customerResourceMethod.getPathParamValueProposals().keySet();
		Assert.assertThat(pathParamValueProposals, hasSize(3));
		Assert.assertThat(pathParamValueProposals, containsInAnyOrder("id", "format", "encoding"));
	}

	@Test
	public void shouldSortHttpMethods() {
		final List<IJaxrsHttpMethod> httpMethods = new ArrayList<IJaxrsHttpMethod>(metamodel.findAllHttpMethods());
		Collections.sort(httpMethods);
		assertThat(httpMethods.get(0).getHttpVerb(), equalTo("GET"));
		assertThat(httpMethods.get(5).getHttpVerb(), equalTo("OPTIONS"));
		assertThat(httpMethods.get(6).getHttpVerb(), equalTo("FOO"));
	}
	
	@Test
	public void shouldDoNothingWhenAddingNullElement() throws CoreException {
		// pre-conditions
		resetElementChangesNotifications();
		// operation
		metamodel.add((JaxrsBaseElement)null);
		// verifications
		assertThat(elementChanges.size(), equalTo(0));
	}

	@Test
	public void shouldDoNothingWhenUpdatingNullElement() throws CoreException {
		// pre-conditions
		resetElementChangesNotifications();
		// operation
		metamodel.update(new JaxrsElementDelta(null, 0));
		// verifications
		assertThat(elementChanges.size(), equalTo(0));
	}
	
	@Test
	public void shouldDoNothingWhenUpdatingUnchangedElement() throws CoreException {
		// pre-conditions
		IType bazType = getType("org.jboss.tools.ws.jaxrs.sample.services.BazResource");
		IMethod bazMethod = getJavaMethod(bazType, "getContent2");
		final IJaxrsResourceMethod bazResourceMethod = (IJaxrsResourceMethod) metamodel.findElement(bazMethod);
		resetElementChangesNotifications();
		// operation
		metamodel.update(new JaxrsElementDelta(bazResourceMethod, CHANGED, JaxrsElementDelta.F_NONE));
		// verifications
		assertThat(elementChanges.size(), equalTo(0));
	}
	
	@Test
	public void shouldDoNothingWhenAddingNullEndpoint() throws CoreException {
		// pre-conditions
		resetElementChangesNotifications();
		// operation
		metamodel.add((JaxrsEndpoint)null);
		// verifications
		assertThat(endpointChanges.size(), equalTo(0));
	}
	
	@Test
	public void shouldDoNothingWhenUpdatingNullEndpoint() throws CoreException {
		// pre-conditions
		resetElementChangesNotifications();
		// operation
		metamodel.update((JaxrsEndpoint)null);
		// verifications
		assertThat(elementChanges.size(), equalTo(0));
	}

}
