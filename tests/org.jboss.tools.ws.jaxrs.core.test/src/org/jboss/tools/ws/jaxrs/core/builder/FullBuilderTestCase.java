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

package org.jboss.tools.ws.jaxrs.core.builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.jboss.tools.ws.jaxrs.core.metamodel.BaseElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.HTTPMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.Metamodel;
import org.jboss.tools.ws.jaxrs.core.metamodel.Provider;
import org.jboss.tools.ws.jaxrs.core.metamodel.Provider.EnumProviderKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.ResolvedUriMapping;
import org.jboss.tools.ws.jaxrs.core.metamodel.Resource;
import org.jboss.tools.ws.jaxrs.core.metamodel.ResourceMethod;
import org.jboss.tools.ws.jaxrs.core.utils.JdtUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author xcoulon
 * 
 */
public class FullBuilderTestCase extends AbstractMetamodelBuilderTestCase {

	private Metamodel metamodel = null;

	@Before
	public void setupMetamodel() throws CoreException {
		metamodel = buildMetamodel(IncrementalProjectBuilder.FULL_BUILD);
	}

	@Test
	public void shouldAssertHTTPMethods() throws CoreException {
		// resourceMethods are stored twice in the underlying map: once by fully
		// qualified name, once by simple name
		// check that there is no duplicate Http Methods in the returned
		// collection
		Assert.assertEquals(1 * 6, metamodel.getHttpMethods().size());
		Set<HTTPMethod> httpMethods = new HashSet<HTTPMethod>();
		for (Entry<String, HTTPMethod> entry : metamodel.getHttpMethods()) {
			// toString() called for code coverage
			HTTPMethod httpMethod = entry.getValue();
			Assert.assertNotNull(httpMethod.toString());
			Assert.assertTrue(httpMethods.add(httpMethod));
		}
	}

	@Test
	public void shouldAssertProviders() throws CoreException {
		// FIXME : check no abstract class is registered
		Assert.assertEquals(2, metamodel.getProviders().getConsumers().size());
		for (Provider p : metamodel.getProviders().getConsumers()) {
			Assert.assertNotNull("Missing provided type", p.getProvidedKinds().containsKey(EnumProviderKind.CONSUMER));
			/*Assert.assertNotNull("Missing mime-type: " + p.getJavaType().getFullyQualifiedName(),
					p.getMediaTypes(EnumProviderKind.CONSUMER));*/
			Assert.assertFalse("Provider type shouldn't be abstract: " + p.getJavaElement().getFullyQualifiedName(),
					JdtUtils.isAbstractType(p.getJavaElement()));
		}

		Assert.assertEquals(3, metamodel.getProviders().getProducers().size());
		for (Provider p : metamodel.getProviders().getProducers()) {
			Assert.assertNotNull("Missing provided type", p.getProvidedKinds().containsKey(EnumProviderKind.PRODUCER));
			/*Assert.assertNotNull("Missing mime-type: " + p.getJavaType().getFullyQualifiedName(),
					p.getMediaTypes(EnumProviderKind.PRODUCER));*/
			Assert.assertFalse("Provider type shouldn't be abstract: " + p.getJavaElement().getFullyQualifiedName(),
					JdtUtils.isAbstractType(p.getJavaElement()));
		}

		Assert.assertEquals(3, metamodel.getProviders().getExceptionMappers().size());
		for (Provider p : metamodel.getProviders().getExceptionMappers()) {
			Assert.assertNotNull("Missing provided type",
					p.getProvidedKinds().containsKey(EnumProviderKind.EXCEPTION_MAPPER));
			Assert.assertNull("Unexpected mime-type: " + p.getJavaElement().getFullyQualifiedName(),
					p.getMediaTypeCapabilities(EnumProviderKind.EXCEPTION_MAPPER));
			Assert.assertFalse("Provider type shouldn't be abstract: " + p.getJavaElement().getFullyQualifiedName(),
					JdtUtils.isAbstractType(p.getJavaElement()));
		}
	}

	@Test
	public void shouldAssertResourcesAndMethods() throws CoreException {
		// for now, the result excludes the (binary) AsynchronousDispatcher, and
		// hence, its (sub)resources
		// FIXME : should this include the subresource locator (method) ???
		Assert.assertEquals(5, metamodel.getResources().getAll().size());
		for (Resource resource : metamodel.getResources().getRootResources()) {
			Assert.assertNotNull("JavaType not found", resource.getJavaElement());
			Assert.assertTrue("Wrong kind", resource.isRootResource());
			Assert.assertEquals("Wrong kind", BaseElement.EnumType.ROOT_RESOURCE, resource.getKind());
			Assert.assertNotNull("UriPathTemplate not found", resource.getUriPathTemplate());
			Assert.assertFalse("Wrong UriPathTemplate format", resource.getUriPathTemplate().contains("null"));
			Assert.assertFalse("Wrong UriPathTemplate format", resource.getUriPathTemplate().contains("*"));
			Assert.assertNotNull("MediaTypeCapabilities not found", resource.getMediaTypeCapabilities());
		}

		for (Resource resource : metamodel.getResources().getRootResources()) {
			Assert.assertNotNull("JavaType not found", resource.getJavaElement());
			Assert.assertTrue("Wrong kind", resource.isRootResource());
			Assert.assertEquals("Wrong kind", BaseElement.EnumType.ROOT_RESOURCE, resource.getKind());
		}

		Map<ResolvedUriMapping, Stack<ResourceMethod>> resolveUriMappings = metamodel.getResources().resolveUriMappings(new NullProgressMonitor());
		Assert.assertEquals("Wrong result", 10, resolveUriMappings.size());
		for(Entry<ResolvedUriMapping, Stack<ResourceMethod>> entry : resolveUriMappings.entrySet()) {
			ResolvedUriMapping key = entry.getKey();
			Stack<ResourceMethod> value = entry.getValue();
			Assert.assertFalse("Found some '//' in the uri path template in " + key, key.getFullUriPathTemplate().contains("//"));
			//Assert.assertFalse("Missing consumed media types in " + key, key.getMediaTypeCapabilities().getConsumedMimeTypes().isEmpty());
			//Assert.assertFalse("Missing produced media types in " + key, key.getMediaTypeCapabilities().getProducedMimeTypes().isEmpty());
			Assert.assertTrue("Empty list of stacks of resourceMethods", !value.isEmpty());
			Assert.assertFalse("Empty stack of resourceMethods", value.isEmpty());
		}
		List<ResolvedUriMapping> uriMappings = new ArrayList<ResolvedUriMapping>(resolveUriMappings.keySet());
		Collections.sort(uriMappings);
		Assert.assertEquals("Wrong result", "/customers?start={int}&size={int}", uriMappings.get(0).getFullUriPathTemplate());
		Assert.assertEquals("Wrong result", "GET", uriMappings.get(0).getHTTPMethod().getHttpVerb());
		Assert.assertEquals("Wrong result", "/products/{type}/{id}", uriMappings.get(9).getFullUriPathTemplate());
	}

	@Test
	public void shouldFullyAssertCustomerResource() throws CoreException {
		Resource customerResource = metamodel.getResources().getByType(
				JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
						new NullProgressMonitor()));
		Assert.assertNotNull("CustomerResource not found", customerResource);
		Assert.assertEquals("Wrong result", "/customers", customerResource.getUriPathTemplate());
		Assert.assertEquals("Wrong result", customerResource, metamodel.getResources().getByPath("/customers"));
		Assert.assertArrayEquals("Wrong mediatype capabilities", new String[] { "application/xml" }, customerResource
				.getMediaTypeCapabilities().getConsumedMimeTypes().toArray());
		Assert.assertArrayEquals("Wrong mediatype capabilities", new String[] {
				"application/vnd.bytesparadise.customer+xml", "application/xml", "application/json" }, customerResource
				.getMediaTypeCapabilities().getProducedMimeTypes().toArray());
		Assert.assertEquals("Wrong number of resource resourceMethods", 2, customerResource.getResourceMethods().size());
		Assert.assertEquals("Wrong number of resource resourceMethods", 4, customerResource.getSubresourceMethods().size());
		Assert.assertEquals("Wrong number of resource resourceMethods", 0, customerResource.getSubresourceLocators().size());
		HTTPMethod httpMethod = metamodel.getHttpMethods().getByVerb("GET");
		ResourceMethod vcardMethod = customerResource.getByURIMapping(httpMethod, "{id}", null, "text/x-vcard");
		Assert.assertNotNull("ResourceMethod not found", vcardMethod);
	}

}
