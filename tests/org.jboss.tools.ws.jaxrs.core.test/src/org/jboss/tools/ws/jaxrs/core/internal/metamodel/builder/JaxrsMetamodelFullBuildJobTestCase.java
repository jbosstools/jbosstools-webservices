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

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.jboss.tools.ws.jaxrs.core.builder.AbstractMetamodelBuilderTestCase;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsResource;
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

	/** @Test
	 * @Ignore
	 *         public void shouldAssertProviders() throws CoreException {
	 *         // FIXME : check no abstract class is registered
	 *         Assert.assertEquals(2,
	 *         metamodel.getProviders(EnumKind.CONSUMER).size());
	 *         for (IJaxrsProvider p :
	 *         metamodel.getProviders(EnumKind.CONSUMER)) {
	 *         Assert.assertNotNull("Missing provided type",
	 *         p.getProvidedKinds().containsKey(EnumKind.CONSUMER));
	 *         /
	 *         Assert.assertNotNull("Missing mime-type: " +
	 *         p.getJavaType().getFullyQualifiedName(),
	 *         p.getMediaTypes(EnumKind.CONSUMER));
	 *         /
	 *         Assert.assertFalse("Provider type shouldn't be abstract: " +
	 *         p.getJavaElement().getFullyQualifiedName(),
	 *         JdtUtils.isAbstractType(p.getJavaElement()));
	 *         }
	 * 
	 *         Assert.assertEquals(3,
	 *         metamodel.getProviders(EnumKind.PRODUCER).size());
	 *         for (IJaxrsProvider p :
	 *         metamodel.getProviders(EnumKind.PRODUCER)) {
	 *         Assert.assertNotNull("Missing provided type",
	 *         p.getProvidedKinds().containsKey(EnumKind.PRODUCER));
	 *         /
	 *         Assert.assertNotNull("Missing mime-type: " +
	 *         p.getJavaType().getFullyQualifiedName(),
	 *         p.getMediaTypes(EnumKind.PRODUCER));
	 *         /
	 *         Assert.assertFalse("Provider type shouldn't be abstract: " +
	 *         p.getJavaElement().getFullyQualifiedName(),
	 *         JdtUtils.isAbstractType(p.getJavaElement()));
	 *         }
	 * 
	 *         Assert.assertEquals(3,
	 *         metamodel.getProviders(EnumKind.EXCEPTION_MAPPER).size());
	 *         for (IJaxrsProvider p :
	 *         metamodel.getProviders(EnumKind.EXCEPTION_MAPPER)) {
	 *         Assert.assertNotNull("Missing provided type",
	 *         p.getProvidedKinds().containsKey(EnumKind.EXCEPTION_MAPPER));
	 *         Assert.assertNull("Unexpected mime-type: " +
	 *         p.getJavaElement().getFullyQualifiedName(),
	 *         p.getMediaTypeCapabilities(EnumKind.EXCEPTION_MAPPER));
	 *         Assert.assertFalse("Provider type shouldn't be abstract: " +
	 *         p.getJavaElement().getFullyQualifiedName(),
	 *         JdtUtils.isAbstractType(p.getJavaElement()));
	 *         }
	 *         } */

	@Test
	public void shouldAssertResourcesAndMethods() throws CoreException {
		// for now, the result excludes the (binary) AsynchronousDispatcher, and
		// hence, its (sub)resources
		// FIXME : should this include the subresource locator (method) ???
		Assert.assertEquals(5, metamodel.getAllResources().size());
		for (IJaxrsResource jaxrsResource : metamodel.getAllResources()) {
			assertThat(jaxrsResource.getJavaElement(), notNullValue());
			assertThat(jaxrsResource.getKind(), notNullValue());
			assertThat(jaxrsResource.getAllMethods().size(), greaterThan(0));
		}
		/*
		 * for (IJaxrsResource resource : metamodel.getAllResources()) {
		 * Assert.assertNotNull("JavaType not found",
		 * resource.getJavaElement());
		 * Assert.assertEquals("Wrong kind", EnumKind.ROOT_RESOURCE,
		 * resource.getKind());
		 * }
		 */
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
		/*
		 * List<Route> uriMappings = new
		 * ArrayList<Route>(resolveUriMappings.keySet());
		 * Collections.sort(uriMappings); Assert.assertEquals("Wrong result",
		 * "/customers?start={int}&size={int}", uriMappings.get(0)
		 * .getFullUriPathTemplate()); Assert.assertEquals("Wrong result",
		 * "GET", uriMappings.get(0).getHTTPMethod().getHttpVerb());
		 * Assert.assertEquals("Wrong result", "/products/{type}/{id}",
		 * uriMappings.get(9).getFullUriPathTemplate());
		 */
	}

	@Test
	public void shouldFullyAssertCustomerResource() throws CoreException {
		IJaxrsResource customerResource = (IJaxrsResource) metamodel.getElement(JdtUtils.resolveType(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject, new NullProgressMonitor()));
		Assert.assertNotNull("CustomerResource not found", customerResource);
		Assert.assertEquals("Wrong number of resource resourceMethods", 6, customerResource.getResourceMethods().size());
	}

}
