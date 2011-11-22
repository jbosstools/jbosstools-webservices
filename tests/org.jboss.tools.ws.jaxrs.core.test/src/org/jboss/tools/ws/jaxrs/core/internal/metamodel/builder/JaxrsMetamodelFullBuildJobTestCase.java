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

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.jboss.tools.ws.jaxrs.core.WorkbenchUtils.getMethod;
import static org.jboss.tools.ws.jaxrs.core.WorkbenchUtils.getType;
import static org.junit.Assert.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.builder.AbstractMetamodelBuilderTestCase;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsResource;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsResourceMethod;
import org.junit.Assert;
import org.junit.Test;

/** @author xcoulon */
public class JaxrsMetamodelFullBuildJobTestCase extends AbstractMetamodelBuilderTestCase {

	@Test
	public void shouldAssertHTTPMethods() throws CoreException {
		// 6 HttpMethods in the jax-rs API (@GET, etc.) + 1 in the project
		// (@FOO)
		Assert.assertEquals(1 * 7, metamodel.getAllHttpMethods().size());
		Set<IJaxrsHttpMethod> jaxrsHttpMethods = new HashSet<IJaxrsHttpMethod>();
		for (IJaxrsHttpMethod httpMethod : metamodel.getAllHttpMethods()) {
			// toString() called for code coverage
			Assert.assertNotNull(httpMethod.toString());
			Assert.assertTrue(jaxrsHttpMethods.add(httpMethod));
		}
	}

	@Test
	public void shouldAssertResourcesAndMethods() throws CoreException {
		// for now, the result excludes the (binary) AsynchronousDispatcher, and
		// hence, its (sub)resources
		Assert.assertEquals(5, metamodel.getAllResources().size());
		for (IJaxrsResource jaxrsResource : metamodel.getAllResources()) {
			assertThat(jaxrsResource.getJavaElement(), notNullValue());
			assertThat(jaxrsResource.getKind(), notNullValue());
			assertThat(jaxrsResource.getAllMethods().size(), greaterThan(0));
		}
	}

	@Test
	public void shouldAssertResolvedEndpoints() throws CoreException {
		List<IJaxrsEndpoint> endpoints = metamodel.getAllEndpoints();
		Assert.assertEquals("Wrong result", 11, endpoints.size());
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
	public void shouldRetrieveCustomerResource() throws CoreException {
		IType customerType = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final IJaxrsResource customerResource = (IJaxrsResource) metamodel.getElement(customerType);
		Assert.assertNotNull("CustomerResource not found", customerType);
		Assert.assertEquals("Wrong number of resource resourceMethods", 6, customerResource.getResourceMethods().size());
	}

	@Test
	public void shouldRetrieveCustomerResourceMethodProposals() throws CoreException {
		IType customerType = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		IMethod customerMethod = getMethod(customerType, "getCustomers");
		final IJaxrsResourceMethod customerResourceMethod = (IJaxrsResourceMethod) metamodel.getElement(customerMethod);
		Assert.assertThat(customerResourceMethod, notNullValue());
		Assert.assertThat(customerResourceMethod.getPathParamValueProposals().size(), equalTo(0));
	}

	@Test
	public void shouldRetrieveCustomerSubresourceMethodProposals() throws CoreException {
		IType customerType = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		IMethod customerMethod = getMethod(customerType, "getCustomer");
		final IJaxrsResourceMethod customerResourceMethod = (IJaxrsResourceMethod) metamodel.getElement(customerMethod);
		Assert.assertThat(customerResourceMethod, notNullValue());
		Assert.assertThat(customerResourceMethod.getPathParamValueProposals(), containsInAnyOrder("id"));
	}

	@Test
	public void shouldRetrieveBookResourceMethodProposals() throws CoreException {
		IType customerType = getType("org.jboss.tools.ws.jaxrs.sample.services.BookResource", javaProject);
		IMethod customerMethod = getMethod(customerType, "getAllProducts");
		final IJaxrsResourceMethod customerResourceMethod = (IJaxrsResourceMethod) metamodel.getElement(customerMethod);
		Assert.assertThat(customerResourceMethod, notNullValue());
		Assert.assertThat(customerResourceMethod.getPathParamValueProposals().size(), equalTo(0));
	}

	@Test
	public void shouldRetrieveBooksubresourceMethodProposals() throws CoreException {
		IType customerType = getType("org.jboss.tools.ws.jaxrs.sample.services.BookResource", javaProject);
		IMethod customerMethod = getMethod(customerType, "getProduct");
		final IJaxrsResourceMethod customerResourceMethod = (IJaxrsResourceMethod) metamodel.getElement(customerMethod);
		Assert.assertThat(customerResourceMethod, notNullValue());
		Assert.assertThat(customerResourceMethod.getPathParamValueProposals(), containsInAnyOrder("id"));
	}

}
