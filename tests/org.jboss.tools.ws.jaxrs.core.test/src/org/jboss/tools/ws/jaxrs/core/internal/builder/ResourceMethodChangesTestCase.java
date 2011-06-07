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

package org.jboss.tools.ws.jaxrs.core.internal.builder;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.jboss.tools.ws.jaxrs.core.WorkbenchUtils;
import org.jboss.tools.ws.jaxrs.core.builder.AbstractMetamodelBuilderTestCase;
import org.jboss.tools.ws.jaxrs.core.metamodel.HTTPMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.MediaTypeCapabilities;
import org.jboss.tools.ws.jaxrs.core.metamodel.Resource;
import org.jboss.tools.ws.jaxrs.core.metamodel.ResourceMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.Resources;
import org.jboss.tools.ws.jaxrs.core.metamodel.Route;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author xcoulon
 * 
 */
public class ResourceMethodChangesTestCase extends AbstractMetamodelBuilderTestCase {

	@Test
	public void shouldDecreaseWhenRemovingMethod() throws CoreException {
		// pre-condition
		Resources resources = metamodel.getResources();
		Resource resource = resources.getByTypeName("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		Assert.assertNotNull("Resource not found");
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
		// operation
		WorkbenchUtils.removeMethod(resource.getJavaElement(), "createCustomer");
		// post-condition
		Assert.assertEquals("Wrong number of resourceMethods : should have decreased:", 5, resource.getAllMethods()
				.size());
		Assert.assertEquals("Wrong number of routes", 9, metamodel.getRoutes().getAll().size());
	}

	@Test
	public void shouldIncreaseWhenAddingJaxrsMethod() throws CoreException {
		// pre-conditions
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		Assert.assertEquals("Wrong number of resourceMethods", 6, resource.getAllMethods().size());
		// operation
		String contents = WorkbenchUtils.getResourceContent("FooResourceMethod.txt", bundle);
		WorkbenchUtils.addMethod(resource.getJavaElement(), contents);
		// post-conditions
		Assert.assertEquals("Wrong number of resourceMethods", 7, resource.getAllMethods().size());
		Assert.assertEquals("Wrong number of routes", 11, metamodel.getRoutes().getAll().size());
	}

	@Test
	public void shouldNotIncreaseWhenAddingOtherMethod() throws CoreException {
		// pre-conditions
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		Assert.assertEquals("Wrong number of resourceMethods", 6, resource.getAllMethods().size());
		// operation
		String contents = "public void foo() { }";
		WorkbenchUtils.addMethod(resource.getJavaElement(), contents);
		// post-conditions
		Assert.assertEquals("Wrong number of resourceMethods", 6, resource.getAllMethods().size());
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
	}

	@Test
	public void shouldNotDecreaseWhenRemovingOtherMethod() throws CoreException {
		// pre-conditions
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		Assert.assertEquals("Wrong number of resourceMethods", 6, resource.getAllMethods().size());
		// operation
		WorkbenchUtils.removeMethod(resource.getJavaElement().getCompilationUnit(), "getEntityManager");
		// post-conditions
		Assert.assertEquals("Wrong number of resourceMethods", 6, resource.getAllMethods().size());
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
	}

	@Test
	public void shouldBecomeSubresourceMethodWhenAddingPathAnnotation() throws CoreException {
		// pre-conditions
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
		Assert.assertEquals("Wrong number of resource resourceMethods", 2, resource.getResourceMethods().size());
		Assert.assertEquals("Wrong number of subresource resourceMethods", 4, resource.getSubresourceMethods().size());
		Assert.assertEquals("Wrong number of subresource locators", 0, resource.getSubresourceLocators().size());
		// operation
		HTTPMethod httpMethod = metamodel.getHttpMethods().getByVerb("POST");
		ResourceMethod resourceMethod = resource.getByMapping(httpMethod, null, null, null);
		WorkbenchUtils.addMethodAnnotation(resourceMethod.getJavaElement(), "@Path(\"/foo\")");
		// post-conditions
		Assert.assertEquals("Wrong number of resource resourceMethods", 1, resource.getResourceMethods().size());
		Assert.assertEquals("Wrong number of subresource resourceMethods", 5, resource.getSubresourceMethods().size());
		Assert.assertEquals("Wrong number of subresource locators", 0, resource.getSubresourceLocators().size());
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
	}

	@Test
	public void shouldBecomeSubresourceLocatorWhenAddingPathAnnotation() throws CoreException {
		// pre-conditions
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		IMethod method = WorkbenchUtils.addMethod(resource.getJavaElement(),
				"public Object fooLocator() { return null; }");
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
		Assert.assertEquals("Wrong number of resource resourceMethods", 2, resource.getResourceMethods().size());
		Assert.assertEquals("Wrong number of subresource resourceMethods", 4, resource.getSubresourceMethods().size());
		List<ResourceMethod> subresourceLocators = resource.getSubresourceLocators();
		Assert.assertEquals("Wrong number of subresource locators", 0, subresourceLocators.size());
		// operation
		WorkbenchUtils.addMethodAnnotation(method, "@Path(\"/foo\")");
		// post-conditions
		subresourceLocators = resource.getSubresourceLocators();
		Assert.assertEquals("Wrong number of resource resourceMethods", 2, resource.getResourceMethods().size());
		Assert.assertEquals("Wrong number of subresource resourceMethods", 4, resource.getSubresourceMethods().size());
		Assert.assertEquals("Wrong number of subresource locators", 1, subresourceLocators.size());

		Assert.assertNotNull("PathParam mapping not found", subresourceLocators.get(0).getMapping().getPathParams());
		Assert.assertNull("HTTP Method mapping not expected", subresourceLocators.get(0).getMapping().getHTTPMethod());
		Assert.assertEquals("Wrong number of routes", 13, metamodel.getRoutes().getAll().size());
		Resource gameResource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		Assert.assertEquals("Wrong number", 2,
				metamodel.getRoutes().getByResourceMethod(gameResource.getAllMethods().get(0)).size());
	}

	@Test
	public void shouldDecreaseWhenRemovingPathAnnotation() throws CoreException {
		// pre-conditions
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
		Assert.assertEquals("Wrong number of resource resourceMethods", 0, resource.getResourceMethods().size());
		Assert.assertEquals("Wrong number of subresource resourceMethods", 0, resource.getSubresourceMethods().size());
		Assert.assertEquals("Wrong number of subresource locators", 1, resource.getSubresourceLocators().size());
		// operation
		WorkbenchUtils.removeFirstOccurrenceOfCode(resource.getJavaElement(), "@Path(\"/{type}\")");
		// post-conditions
		Assert.assertEquals("Wrong number of resource resourceMethods", 0, resource.getResourceMethods().size());
		Assert.assertEquals("Wrong number of subresource resourceMethods", 0, resource.getSubresourceMethods().size());
		Assert.assertEquals("Wrong number of subresource locators", 0, resource.getSubresourceLocators().size());
		Assert.assertEquals("Wrong number of routes", 7, metamodel.getRoutes().getAll().size());
	}

	@Test
	public void shouldBecomeResourceMethodWhenRemovingPathAnnotation() throws CoreException {
		// pre-conditions
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
		Assert.assertEquals("Wrong number of resource resourceMethods", 2, resource.getResourceMethods().size());
		Assert.assertEquals("Wrong number of subresource resourceMethods", 4, resource.getSubresourceMethods().size());
		Assert.assertEquals("Wrong number of subresource locators", 0, resource.getSubresourceLocators().size());
		// operation
		WorkbenchUtils.removeMethodAnnotation(resource.getJavaElement(), "getCustomerAsVCard", "@Path(\"{id}\")");
		// post-conditions
		Assert.assertEquals("Wrong number of resource resourceMethods", 3, resource.getResourceMethods().size());
		Assert.assertEquals("Wrong number of subresource resourceMethods", 3, resource.getSubresourceMethods().size());
		Assert.assertEquals("Wrong number of subresource locators", 0, resource.getSubresourceLocators().size());
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
	}

	@Test
	public void shouldBecomeResourceMethodWhenAddingHTTPMethodAnnotation() throws CoreException {
		// pre-conditions
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
		Assert.assertEquals("Wrong number of resource resourceMethods", 2, resource.getResourceMethods().size());
		Assert.assertEquals("Wrong number of subresource resourceMethods", 4, resource.getSubresourceMethods().size());
		Assert.assertEquals("Wrong number of subresource locators", 0, resource.getSubresourceLocators().size());
		// operation
		WorkbenchUtils.addMethodAnnotation(resource.getJavaElement().getMethod("getEntityManager", null), "@GET");
		// post-conditions
		Assert.assertEquals("Wrong number of resource resourceMethods", 3, resource.getResourceMethods().size());
		Assert.assertEquals("Wrong number of subresource resourceMethods", 4, resource.getSubresourceMethods().size());
		Assert.assertEquals("Wrong number of subresource locators", 0, resource.getSubresourceLocators().size());
		Assert.assertEquals("Wrong number of routes", 11, metamodel.getRoutes().getAll().size());
	}

	@Test
	public void shouldChangeWhenModifyingHTTPMethodAnnotation() throws CoreException {
		// pre-conditions
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
		Assert.assertEquals("Wrong number of resource resourceMethods", 2, resource.getResourceMethods().size());
		Assert.assertEquals("Wrong number of subresource resourceMethods", 4, resource.getSubresourceMethods().size());
		Assert.assertEquals("Wrong number of subresource locators", 0, resource.getSubresourceLocators().size());
		ResourceMethod resourceMethod = null;
		for (ResourceMethod r : resource.getResourceMethods()) {
			if (r.getMapping().getHTTPMethod().getHttpVerb().equals("POST")) {
				resourceMethod = r;
			}
		}
		Route route = metamodel.getRoutes().getByResourceMethod(resourceMethod).get(0);
		Assert.assertEquals("Wrong HttpMethod", "POST", route.getEndpoint().getHttpMethod().getHttpVerb());
		// operation
		WorkbenchUtils.replaceFirstOccurrenceOfCode(resourceMethod.getJavaElement(), "@POST", "@DELETE");
		// post-conditions
		Assert.assertEquals("Wrong number of resource resourceMethods", 2, resource.getResourceMethods().size());
		Assert.assertEquals("Wrong number of subresource resourceMethods", 4, resource.getSubresourceMethods().size());
		Assert.assertEquals("Wrong number of subresource locators", 0, resource.getSubresourceLocators().size());
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
		Assert.assertEquals("Wrong HttpMethod", "DELETE", route.getEndpoint().getHttpMethod().getHttpVerb());
	}

	@Test
	public void shouldBecomeSubresourceMethodWhenAddingHTTPMethodAnnotation() throws CoreException {
		// pre-conditions
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
		Assert.assertEquals("Wrong number of resource resourceMethods", 0, resource.getResourceMethods().size());
		Assert.assertEquals("Wrong number of subresource resourceMethods", 0, resource.getSubresourceMethods().size());
		Assert.assertEquals("Wrong number of subresource locators", 1, resource.getSubresourceLocators().size());
		ResourceMethod resourceMethod = resource.getByMapping(null, "/{type}", null, null);
		Assert.assertNotNull("ResourceMethod not found", resourceMethod);
		// operation
		WorkbenchUtils.addImport(resourceMethod.getJavaElement().getCompilationUnit(), GET.class.getName());
		WorkbenchUtils.addMethodAnnotation(resourceMethod.getJavaElement(), "@GET");
		// post-conditions
		Assert.assertEquals("Wrong number of resource resourceMethods", 0, resource.getResourceMethods().size());
		Assert.assertEquals("Wrong number of subresource resourceMethods", 1, resource.getSubresourceMethods().size());
		Assert.assertEquals("Wrong number of subresource locators", 0, resource.getSubresourceLocators().size());
		// -3, +1
		Assert.assertEquals("Wrong number of routes", 8, metamodel.getRoutes().getAll().size());
	}

	@Test
	@Ignore
	public void shouldBecomeSubresourceLocatorWhenRemovingHTTPMethodAnnotation() throws CoreException {
		// pre-conditions
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		Assert.assertEquals("Wrong number of resource resourceMethods", 2, resource.getResourceMethods().size());
		Assert.assertEquals("Wrong number of subresource resourceMethods", 4, resource.getSubresourceMethods().size());
		Assert.assertEquals("Wrong number of subresource locators", 0, resource.getSubresourceLocators().size());
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
		// operation
		WorkbenchUtils.removeMethodAnnotation(resource.getJavaElement(), "getCustomerAsVCard", "@GET");
		// post-conditions
		Assert.assertEquals("Wrong number of resource resourceMethods", 2, resource.getResourceMethods().size());
		Assert.assertEquals("Wrong number of subresource resourceMethods", 3, resource.getSubresourceMethods().size());
		Assert.assertEquals("Wrong number of subresource locators", 1, resource.getSubresourceLocators().size());
		// -1, but nothing more as the subresource locator does not match with
		// any resource (returning Response type!)
		LOGGER.debug("Remaing routes:\n{}", metamodel.getRoutes().getAll());
		Assert.assertEquals("Wrong number of routes", 9, metamodel.getRoutes().getAll().size());
	}

	@Test
	public void shouldDecreaseWhenRemovingHTTPMethodAnnotation() throws CoreException {
		// pre-conditions
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
		Assert.assertEquals("Wrong number of resource resourceMethods", 2, resource.getResourceMethods().size());
		Assert.assertEquals("Wrong number of subresource resourceMethods", 4, resource.getSubresourceMethods().size());
		Assert.assertEquals("Wrong number of subresource locators", 0, resource.getSubresourceLocators().size());
		// operation
		WorkbenchUtils.removeMethodAnnotation(resource.getJavaElement(), "createCustomer", "@POST");
		// post-conditions
		Assert.assertEquals("Wrong number of resource resourceMethods", 1, resource.getResourceMethods().size());
		Assert.assertEquals("Wrong number of subresource resourceMethods", 4, resource.getSubresourceMethods().size());
		Assert.assertEquals("Wrong number of subresource locators", 0, resource.getSubresourceLocators().size());
		Assert.assertEquals("Wrong number of routes", 9, metamodel.getRoutes().getAll().size());
	}

	@Test
	public void shouldChangeWhenAddingPathAnnotationValueAtTypeLevel() throws CoreException {
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
		Route route = metamodel.getRoutes().getByResourceMethod(resource.getSubresourceMethods().get(0)).get(0);
		Assert.assertEquals("Wrong mediatypes", "/products/{type}/{id}", route.getEndpoint().getUriPathTemplate());
		// operation
		WorkbenchUtils.addTypeAnnotation(resource.getJavaElement(), "@Path(\"/foo\")");
		// post-conditions
		Assert.assertTrue(resource.isRootResource());
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
		route = metamodel.getRoutes().getByResourceMethod(resource.getSubresourceMethods().get(0)).get(0);
		Assert.assertEquals("Wrong URI Path Template", "/foo/{id}", route.getEndpoint().getUriPathTemplate());
	}

	@Test
	public void shouldChangeWhenModifyingPathAnnotationValueAtTypeLevel() throws CoreException {
		// pre-conditions
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
		HTTPMethod httpMethod = metamodel.getHttpMethods().getByVerb("GET");
		ResourceMethod resourceMethod = resource.getByMapping(httpMethod, "{id}", null, null);
		Assert.assertNotNull("ResourceMethod not found", resourceMethod);
		Route route = resourceMethod.getMetamodel().getRoutes().getByResourceMethod(resourceMethod).get(0);
		Assert.assertEquals("Wrong template", "/customers/{id}", route.getEndpoint().getUriPathTemplate());
		// operation
		WorkbenchUtils.replaceFirstOccurrenceOfCode(resourceMethod.getParentResource().getJavaElement(),
				"@Path(CustomerResource.URI_BASE)", "@Path(\"/foo\")");
		// post-conditions
		Assert.assertNotNull("No result expected", resource.getByMapping(httpMethod, "{id}", null, null));
		Assert.assertEquals("Wrong template", "/foo/{id}", route.getEndpoint().getUriPathTemplate());
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
	}

	@Test
	public void shouldChangeWhenRemovingPathAnnotationValueAtTypeLevel() throws CoreException {
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.PurchaseOrderResource");
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
		Route route = metamodel.getRoutes().getByResourceMethod(resource.getSubresourceMethods().get(0)).get(0);
		Assert.assertEquals("Wrong mediatypes", "/orders/{id}", route.getEndpoint().getUriPathTemplate());
		// operation
		WorkbenchUtils.removeFirstOccurrenceOfCode(resource.getJavaElement(), "@Path(\"/orders\")");
		// post-conditions
		Assert.assertFalse(resource.isRootResource());
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
		route = metamodel.getRoutes().getByResourceMethod(resource.getSubresourceMethods().get(0)).get(0);
		Assert.assertEquals("Wrong URI Path Template", "/products/{type}/{id}", route.getEndpoint()
				.getUriPathTemplate());
	}

	@Test
	public void shouldChangeWhenAddingPathAnnotationAtMethodLevel() throws CoreException {
		// pre-conditions
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
		HTTPMethod httpMethod = metamodel.getHttpMethods().getByVerb("POST");
		ResourceMethod resourceMethod = resource.getByMapping(httpMethod, null, null, null);
		Assert.assertNotNull("ResourceMethod not found", resourceMethod);
		Route route = resourceMethod.getMetamodel().getRoutes().getByResourceMethod(resourceMethod).get(0);
		Assert.assertEquals("Wrong template", "/customers", route.getEndpoint().getUriPathTemplate());
		// operation
		WorkbenchUtils.addMethodAnnotation(resourceMethod.getJavaElement(), "@Path(\"{id}\")");
		// post-conditions
		Assert.assertNotNull("No result expected", resource.getByMapping(httpMethod, "{id}", null, null));
		Assert.assertEquals("Wrong template", "/customers/{id}", route.getEndpoint().getUriPathTemplate());
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
	}

	public void shouldChangeWhenModifyingPathAnnotationValueAtMethodLevel() throws CoreException {
		// pre-conditions
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
		HTTPMethod httpMethod = metamodel.getHttpMethods().getByVerb("GET");
		ResourceMethod resourceMethod = resource.getByMapping(httpMethod, "{id}", null, null);
		Assert.assertNotNull("ResourceMethod not found", resourceMethod);
		Route route = resourceMethod.getMetamodel().getRoutes().getByResourceMethod(resourceMethod).get(0);
		Assert.assertEquals("Wrong template", "/customers/{id}", route.getEndpoint().getUriPathTemplate());
		// operation
		WorkbenchUtils.replaceFirstOccurrenceOfCode(resourceMethod.getJavaElement(), "@Path(\"{id}\")",
				"@Path(\"{idCustomer}\")");
		// post-conditions
		Assert.assertNull("No result expected", resource.getByMapping(httpMethod, "{id}", null, null));
		Assert.assertNotNull("No result found", resource.getByMapping(httpMethod, "{idCustomer}", null, null));
		Assert.assertEquals("Wrong template", "/customers/{idCustomer}", route.getEndpoint().getUriPathTemplate());
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
	}

	@Test
	public void shouldChangeWhenRemovingPathAnnotationAtMethodLevel() throws CoreException {
		// pre-conditions
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
		HTTPMethod httpMethod = metamodel.getHttpMethods().getByVerb("DELETE");
		ResourceMethod resourceMethod = resource.getByMapping(httpMethod, "{id}", null, null);
		Assert.assertNotNull("ResourceMethod not found", resourceMethod);
		Route route = resourceMethod.getMetamodel().getRoutes().getByResourceMethod(resourceMethod).get(0);
		Assert.assertEquals("Wrong template", "/customers/{id}", route.getEndpoint().getUriPathTemplate());
		// operation
		WorkbenchUtils.removeMethodAnnotation(resourceMethod.getJavaElement(), "@Path(\"{id}\")");
		// post-conditions
		Assert.assertNull("No result expected", resource.getByMapping(httpMethod, "{id}", null, null));
		Assert.assertEquals("Wrong template", "/customers", route.getEndpoint().getUriPathTemplate());
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
	}

	@Test
	public void shouldChangeWhenAddingProducesAnnotationAtMethodLevel() throws CoreException {
		// pre-conditions
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
		HTTPMethod httpMethod = metamodel.getHttpMethods().getByVerb("GET");
		ResourceMethod resourceMethod = resource.getByMapping(httpMethod, "{id}", null, null);
		Assert.assertNotNull("ResourceMethod not found", resourceMethod);
		MediaTypeCapabilities methodMediaTypes = resourceMethod.getMapping().getProcucedMediaTypes();
		Assert.assertTrue("Wrong result", methodMediaTypes.isEmpty());
		Route route = resourceMethod.getMetamodel().getRoutes().getByResourceMethod(resourceMethod).get(0);
		Assert.assertEquals("Wrong mediatypes", 3, route.getEndpoint().getProducedMediaTypes().size());
		// operation
		WorkbenchUtils.addMethodAnnotation(resourceMethod.getJavaElement(), "@Produces(\"foo/bar\")");
		// post-conditions
		Assert.assertEquals("No result found", "foo/bar", methodMediaTypes.get(0));
		Assert.assertEquals("Wrong mediatypes", "foo/bar", route.getEndpoint().getProducedMediaTypes().get(0));
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
	}

	@Test
	public void shouldChangeWhenAddingProducesAnnotationAtTypeLevel() throws CoreException {
		// pre-conditions
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.PurchaseOrderResource");
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
		String contents = WorkbenchUtils.getResourceContent("FooResourceMethod.txt", bundle);
		WorkbenchUtils.addImport(resource.getJavaElement(), "javax.ws.rs.POST");
		WorkbenchUtils.addImport(resource.getJavaElement(), "javax.ws.rs.core.Response");
		WorkbenchUtils.addMethod(resource.getJavaElement(), contents);
		HTTPMethod httpMethod = metamodel.getHttpMethods().getByVerb("POST");
		ResourceMethod resourceMethod = resource.getByMapping(httpMethod, null, null, null);
		Assert.assertNotNull("ResourceMethod not found", resourceMethod);
		MediaTypeCapabilities methodMediaTypes = resourceMethod.getMapping().getProcucedMediaTypes();
		Assert.assertTrue("Wrong result", methodMediaTypes.isEmpty());
		Route route = resourceMethod.getMetamodel().getRoutes().getByResourceMethod(resourceMethod).get(0);
		MediaTypeCapabilities resolvedMediaTypes = route.getEndpoint().getProducedMediaTypes();
		Assert.assertEquals("Wrong mediatypes", "*/*", resolvedMediaTypes.get(0));
		// operation
		WorkbenchUtils.addTypeAnnotation(resourceMethod.getParentResource().getJavaElement(), "@Produces(\"foo/bar\")");
		// post-conditions
		Assert.assertTrue("Wrong result", methodMediaTypes.isEmpty());
		route = resourceMethod.getMetamodel().getRoutes().getByResourceMethod(resourceMethod).get(0);
		resolvedMediaTypes = route.getEndpoint().getProducedMediaTypes();
		Assert.assertEquals("Wrong mediatypes", "foo/bar", resolvedMediaTypes.get(0));
		Assert.assertEquals("Wrong number of routes", 11, metamodel.getRoutes().getAll().size());
	}

	@Test
	public void shouldNotChangeWhenModifyingProducesAnnotationAtTypeLevel() throws CoreException {
		// pre-conditions
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
		Assert.assertEquals("Wrong number of resourceMethods", 6, resource.getAllMethods().size());
		HTTPMethod httpMethod = metamodel.getHttpMethods().getByVerb("GET");
		ResourceMethod resourceMethod = resource.getByMapping(httpMethod, "{id}", null, "text/x-vcard");
		Assert.assertNotNull("ResourceMethod not found", resourceMethod);
		MediaTypeCapabilities methodMediaTypes = resourceMethod.getMapping().getProcucedMediaTypes();
		Assert.assertEquals("Wrong result:", "text/x-vcard", methodMediaTypes.get(0));
		Route route = resourceMethod.getMetamodel().getRoutes().getByResourceMethod(resourceMethod).get(0);
		MediaTypeCapabilities resolvedMediaTypes = route.getEndpoint().getProducedMediaTypes();
		Assert.assertEquals("Wrong mediatypes", "text/x-vcard", resolvedMediaTypes.get(0));

		// operation
		WorkbenchUtils
				.replaceFirstOccurrenceOfCode(
						resourceMethod.getJavaElement().getDeclaringType(),
						"@Produces({ \"application/vnd.bytesparadise.customer+xml\", MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })",
						"@Produces(\"foo/bar\")");
		// post-conditions
		Assert.assertEquals("Wrong result:", "text/x-vcard", methodMediaTypes.get(0));
		Assert.assertEquals("Wrong mediatypes", "text/x-vcard", resolvedMediaTypes.get(0));
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
	}

	@Test
	public void shouldChangeWhenModifyingProducesAnnotationAtMethodLevel() throws CoreException {
		// pre-conditions
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
		HTTPMethod httpMethod = metamodel.getHttpMethods().getByVerb("GET");
		ResourceMethod resourceMethod = resource.getByMapping(httpMethod, "{id}", null, "text/x-vcard");
		Assert.assertNotNull("ResourceMethod not found", resourceMethod);
		MediaTypeCapabilities methodMediaTypes = resourceMethod.getMapping().getProcucedMediaTypes();
		Assert.assertEquals("Wrong result:", "text/x-vcard", methodMediaTypes.get(0));
		Route route = resourceMethod.getMetamodel().getRoutes().getByResourceMethod(resourceMethod).get(0);
		MediaTypeCapabilities resolvedMediaTypes = route.getEndpoint().getProducedMediaTypes();
		Assert.assertEquals("Wrong mediatypes", "text/x-vcard", resolvedMediaTypes.get(0));
		// operation
		WorkbenchUtils.replaceFirstOccurrenceOfCode(resourceMethod.getJavaElement(), "@Produces({ \"text/x-vcard\" })",
				"@Produces(\"foo/bar\")");
		// post-conditions
		Assert.assertEquals("Wrong result:", "foo/bar", methodMediaTypes.get(0));
		Assert.assertEquals("Wrong mediatypes", "foo/bar", resolvedMediaTypes.get(0));
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
	}

	@Test
	public void shouldChangeWhenRemovingProducesAnnotationAtMethodLevel() throws CoreException {
		// pre-conditions
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
		HTTPMethod httpMethod = metamodel.getHttpMethods().getByVerb("GET");
		ResourceMethod resourceMethod = resource.getByMapping(httpMethod, "{id}", null, "text/x-vcard");
		Assert.assertNotNull("ResourceMethod not found", resourceMethod);
		List<String> producedMimeTypes = resourceMethod.getMapping().getProcucedMediaTypes().getMediatypes();
		Assert.assertEquals("Wrong result", "text/x-vcard", producedMimeTypes.get(0));
		Route route = resourceMethod.getMetamodel().getRoutes().getByResourceMethod(resourceMethod).get(0);
		MediaTypeCapabilities resolvedMediaTypes = route.getEndpoint().getProducedMediaTypes();
		Assert.assertEquals("Wrong mediatypes", "text/x-vcard", resolvedMediaTypes.get(0));
		// operation
		WorkbenchUtils.removeMethodAnnotation(resourceMethod.getJavaElement(), "@Produces({ \"text/x-vcard\" })");
		// post-conditions: type-level annotation value applies
		Assert.assertTrue("Wrong mediatype capabilities", producedMimeTypes.isEmpty());
		Assert.assertEquals("Wrong mediatypes", 3, resolvedMediaTypes.size());
		Assert.assertEquals("Wrong mediatypes", "application/vnd.bytesparadise.customer+xml", resolvedMediaTypes.get(0));
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
	}

	@Test
	public void shouldChangeWhenAddingConsumesAnnotationAtMethodLevel() throws CoreException {
		// pre-conditions
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		HTTPMethod httpMethod = metamodel.getHttpMethods().getByVerb("GET");
		ResourceMethod resourceMethod = resource.getByMapping(httpMethod, "/{id}", null, null);
		Assert.assertNotNull("ResourceMethod not found", resourceMethod);
		MediaTypeCapabilities consumedMediaTypes = resourceMethod.getMapping().getConsumedMediaTypes();
		Assert.assertTrue("Wrong result", consumedMediaTypes.isEmpty());
		Route route = resourceMethod.getMetamodel().getRoutes().getByResourceMethod(resourceMethod).get(0);
		MediaTypeCapabilities resolvedMediaTypes = route.getEndpoint().getConsumedMediaTypes();
		Assert.assertEquals("Wrong mediatypes", "application/xml", resolvedMediaTypes.get(0));
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
		// operation
		WorkbenchUtils.addMethodAnnotation(resourceMethod.getJavaElement(), "@Consumes(\"foo/bar\")");
		// post-conditions
		Assert.assertEquals("No result found", "foo/bar", consumedMediaTypes.get(0));
		Assert.assertEquals("Wrong mediatypes", "foo/bar", resolvedMediaTypes.get(0));
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
	}

	@Test
	public void shouldChangeWhenAddingConsumesAnnotationAtTypeLevel() throws CoreException {
		// pre-conditions
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		HTTPMethod httpMethod = metamodel.getHttpMethods().getByVerb("GET");
		ResourceMethod resourceMethod = resource.getByMapping(httpMethod, "/{id}", null, null);
		Assert.assertNotNull("ResourceMethod not found", resourceMethod);
		MediaTypeCapabilities consumedMediaTypes = resourceMethod.getMapping().getConsumedMediaTypes();
		Assert.assertTrue("Wrong result", consumedMediaTypes.isEmpty());
		Route route = resourceMethod.getMetamodel().getRoutes().getByResourceMethod(resourceMethod).get(0);
		MediaTypeCapabilities resolvedMediaTypes = route.getEndpoint().getConsumedMediaTypes();
		Assert.assertEquals("Wrong mediatypes", "*/*", resolvedMediaTypes.get(0));
		// operation
		WorkbenchUtils.addImport(resourceMethod.getJavaElement().getDeclaringType(), "javax.ws.rs.Consumes");
		WorkbenchUtils.addTypeAnnotation(resourceMethod.getJavaElement().getDeclaringType(), "@Consumes(\"foo/bar\")");
		// post-conditions
		Assert.assertTrue("Wrong result", consumedMediaTypes.isEmpty());
		Assert.assertEquals("Wrong mediatypes", "foo/bar", resolvedMediaTypes.get(0));
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
	}

	@Test
	public void shouldChangeWhenModifyingConsumesAnnotationAtMethodLevel() throws CoreException {
		// pre-conditions
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		HTTPMethod httpMethod = metamodel.getHttpMethods().getByVerb("POST");
		ResourceMethod resourceMethod = resource.getByMapping(httpMethod, null, null, null);
		Assert.assertNotNull("ResourceMethod not found", resourceMethod);
		MediaTypeCapabilities mediaTypeCapabilities = resourceMethod.getMapping().getConsumedMediaTypes();
		Assert.assertEquals("No result found", "application/vnd.bytesparadise.game+xml", mediaTypeCapabilities.get(0));
		Assert.assertEquals("No result found", "application/xml", mediaTypeCapabilities.get(1));
		Assert.assertEquals("No result found", "application/json", mediaTypeCapabilities.get(2));
		Route route = resourceMethod.getMetamodel().getRoutes().getByResourceMethod(resourceMethod).get(0);
		MediaTypeCapabilities resolvedMediaTypes = route.getEndpoint().getConsumedMediaTypes();
		Assert.assertEquals("Wrong mediatypes", 3, resolvedMediaTypes.size());
		// operation
		WorkbenchUtils.replaceFirstOccurrenceOfCode(resourceMethod.getJavaElement(),
				"@Consumes({ \"application/vnd.bytesparadise.game+xml\", \"application/xml\", \"application/json\" })",
				"@Consumes(\"foo/bar\")");
		// post-conditions
		Assert.assertEquals("No result found", 1, mediaTypeCapabilities.size());
		Assert.assertEquals("No result found", "foo/bar", mediaTypeCapabilities.get(0));
		Assert.assertEquals("Wrong mediatypes", "foo/bar", resolvedMediaTypes.get(0));
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
	}

	@Test
	public void shouldChangeWhenRemovingConsumesAnnotationAtMethodLevel() throws CoreException {
		// pre-conditions
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		HTTPMethod httpMethod = metamodel.getHttpMethods().getByVerb("POST");
		ResourceMethod resourceMethod = resource.getByMapping(httpMethod, null, null, null);
		Assert.assertNotNull("ResourceMethod not found", resourceMethod);
		MediaTypeCapabilities mediaTypeCapabilities = resourceMethod.getMapping().getConsumedMediaTypes();
		Assert.assertEquals("No result found", "application/vnd.bytesparadise.game+xml", mediaTypeCapabilities.get(0));
		Assert.assertEquals("No result found", "application/xml", mediaTypeCapabilities.get(1));
		Assert.assertEquals("No result found", "application/json", mediaTypeCapabilities.get(2));
		Route route = resourceMethod.getMetamodel().getRoutes().getByResourceMethod(resourceMethod).get(0);
		MediaTypeCapabilities resolvedMediaTypes = route.getEndpoint().getConsumedMediaTypes();
		Assert.assertEquals("Wrong mediatypes", 3, resolvedMediaTypes.size());
		// operation
		WorkbenchUtils.removeMethodAnnotation(resourceMethod.getJavaElement(),
				"@Consumes({ \"application/vnd.bytesparadise.game+xml\", \"application/xml\", \"application/json\" })");
		// post-conditions
		Assert.assertTrue("No result found", mediaTypeCapabilities.isEmpty());
		Assert.assertEquals("Wrong mediatypes", "application/xml", resolvedMediaTypes.get(0));
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
	}

	@Test
	public void shouldNotChangeWhenRenamingMethodName() throws CoreException {
		// pre-conditions
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		Assert.assertEquals("Wrong number of resourceMethods", 2, resource.getAllMethods().size());
		HTTPMethod httpMethod = metamodel.getHttpMethods().getByVerb("POST");
		ResourceMethod resourceMethod = resource.getByMapping(httpMethod, null, null, null);
		Assert.assertNotNull("ResourceMethod not found", resourceMethod);
		// operation
		WorkbenchUtils.rename(resourceMethod.getJavaElement(), "createProductPOST");
		// post-conditions
		Assert.assertEquals("Wrong number of resourceMethods", 2, resource.getAllMethods().size());
		resourceMethod = resource.getByMapping(httpMethod, null, null, null);
		Assert.assertNotNull("ResourceMethod not found", resourceMethod);
		Assert.assertEquals("Wrong java method name", "createProductPOST", resourceMethod.getJavaElement()
				.getElementName());
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
	}

	@Test
	public void shouldNotChangeWhenChangingParameters() throws CoreException {
		// pre-conditions
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		Assert.assertEquals("Wrong number of resourceMethods", 2, resource.getAllMethods().size());
		HTTPMethod httpMethod = metamodel.getHttpMethods().getByVerb("GET");
		ResourceMethod resourceMethod = resource.getByMapping(httpMethod, "/{id}", null, null);
		Assert.assertNotNull("ResourceMethod not found", resourceMethod);
		Assert.assertTrue("No query param expected", resourceMethod.getMapping().getQueryParams().isEmpty());
		// operation
		WorkbenchUtils.addImport(resourceMethod.getJavaElement().getCompilationUnit(), QueryParam.class.getName());
		WorkbenchUtils.addMethodParameter(resourceMethod.getJavaElement(), "@QueryParam(\"start\") int start");
		// post-conditions
		Assert.assertEquals("Wrong number of resourceMethods", 2, resource.getAllMethods().size());
		resourceMethod = resource.getByMapping(httpMethod, "/{id}", null, null);
		Assert.assertNotNull("ResourceMethod not found", resourceMethod);
		Assert.assertEquals("No query param found", 1, resourceMethod.getMapping().getQueryParams().size());
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
	}

	@Test
	public void shouldReportErrorWhenRemovingImport() throws JavaModelException {
		// preconditions
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		List<ResourceMethod> allMethods = resource.getAllMethods();
		Assert.assertEquals("Wrong number of resourceMethods", 2, allMethods.size());
		HTTPMethod httpMethod = metamodel.getHttpMethods().getByVerb("GET");
		ResourceMethod resourceMethod = resource.getByMapping(httpMethod, "/{id}", null, null);
		// operation 1
		WorkbenchUtils.removeImport(resourceMethod.getJavaElement().getCompilationUnit(), Path.class.getName());
		// post-conditions 1 : errors reported (import is missing)
		Assert.assertEquals("Wrong number of resourceMethods", 2, allMethods.size());
		Assert.assertTrue("Error not reported", resourceMethod.hasErrors());
		// operation 2
		WorkbenchUtils.addImport(resourceMethod.getJavaElement().getCompilationUnit(), Path.class.getName());
		// post-conditions 2 : no error reported (missing import was restored)
		Assert.assertEquals("Wrong number of resourceMethods", 2, allMethods.size());
		Assert.assertFalse("Error still reported", resourceMethod.hasErrors());
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
	}

	@Test
	public void shouldUpdateResourceMethodModelWhenChangingPathParamAnnotation() throws CoreException {
		// preconditions
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		Assert.assertEquals("Wrong number of resourceMethods", 6, resource.getAllMethods().size());
		HTTPMethod httpMethod = metamodel.getHttpMethods().getByVerb("GET");
		ResourceMethod resourceMethod = resource.getByMapping(httpMethod, "{id}", null, "text/x-vcard");
		Assert.assertNotNull("ResourceMethod not found", resourceMethod);
		Assert.assertEquals("Invalid PathParam", "id", resourceMethod.getMapping().getPathParams().get(0)
				.getAnnotationValue());

		// operation
		WorkbenchUtils.replaceAllOccurrencesOfCode(resource.getJavaElement().getCompilationUnit(),
				"@PathParam(\"id\")", "@PathParam(\"ide\")");
		// post-conditions
		Assert.assertEquals("Invalid PathParam", "ide", resourceMethod.getMapping().getPathParams().get(0)
				.getAnnotationValue());
		Assert.assertEquals("Wrong number of routes", 10, metamodel.getRoutes().getAll().size());
	}

}
