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
import org.junit.Assert;
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
		Assert.assertEquals("Wrong number of resourceMethods", 6, resource.getAllMethods().size());
		// operation
		WorkbenchUtils.removeMethod(resource.getJavaElement(), "createCustomer");
		// post-condition
		Assert.assertEquals("Wrong number of resourceMethods : should have decreased:", 5, resource.getAllMethods()
				.size());
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
	}

	@Test
	public void shouldBecomeSubresourceMethodWhenAddingPathAnnotation() throws CoreException {
		// pre-conditions
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		Assert.assertEquals("Wrong number of resourceMethods", 6, resource.getAllMethods().size());
		Assert.assertEquals("Wrong number of resource resourceMethods", 2, resource.getResourceMethods().size());
		Assert.assertEquals("Wrong number of subresource resourceMethods", 4, resource.getSubresourceMethods().size());
		Assert.assertEquals("Wrong number of subresource locators", 0, resource.getSubresourceLocators().size());
		// operation
		HTTPMethod httpMethod = metamodel.getHttpMethods().getByVerb("POST");
		ResourceMethod resourceMethod = resource.getByURIMapping(httpMethod, null, null, null);
		WorkbenchUtils.addMethodAnnotion(resourceMethod.getJavaElement(), "@Path(\"/foo\")");
		// post-conditions
		Assert.assertEquals("Wrong number of resource resourceMethods", 1, resource.getResourceMethods().size());
		Assert.assertEquals("Wrong number of subresource resourceMethods", 5, resource.getSubresourceMethods().size());
		Assert.assertEquals("Wrong number of subresource locators", 0, resource.getSubresourceLocators().size());
	}

	@Test
	public void shouldBecomeSubresourceLocatorWhenAddingPathAnnotation() throws CoreException {
		// pre-conditions
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		IMethod method = WorkbenchUtils.addMethod(resource.getJavaElement(),
				"public Object fooLocator() { return null; }");
		Assert.assertEquals("Wrong number of resourceMethods", 6, resource.getAllMethods().size());
		Assert.assertEquals("Wrong number of resource resourceMethods", 2, resource.getResourceMethods().size());
		Assert.assertEquals("Wrong number of subresource resourceMethods", 4, resource.getSubresourceMethods().size());
		List<ResourceMethod> subresourceLocators = resource.getSubresourceLocators();
		Assert.assertEquals("Wrong number of subresource locators", 0, subresourceLocators.size());
		// operation
		WorkbenchUtils.addMethodAnnotion(method, "@Path(\"/foo\")");
		// post-conditions
		subresourceLocators = resource.getSubresourceLocators();
		Assert.assertEquals("Wrong number of resource resourceMethods", 2, resource.getResourceMethods().size());
		Assert.assertEquals("Wrong number of subresource resourceMethods", 4, resource.getSubresourceMethods().size());
		Assert.assertEquals("Wrong number of subresource locators", 1, subresourceLocators.size());

		Assert.assertNotNull("PathParam mapping not found", subresourceLocators.get(0).getUriMapping().getPathParams());
		Assert.assertNull("HTTP Method mapping not expected", subresourceLocators.get(0).getUriMapping()
				.getHTTPMethod());
	}

	@Test
	public void shouldDecreaseWhenRemovingPathAnnotation() throws CoreException {
		// pre-conditions
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		Assert.assertEquals("Wrong number of resource resourceMethods", 0, resource.getResourceMethods().size());
		Assert.assertEquals("Wrong number of subresource resourceMethods", 0, resource.getSubresourceMethods().size());
		Assert.assertEquals("Wrong number of subresource locators", 1, resource.getSubresourceLocators().size());
		// operation
		WorkbenchUtils.removeFirstOccurrenceOfCode(resource.getJavaElement(), "@Path(\"/{type}\")");
		// post-conditions
		Assert.assertEquals("Wrong number of resource resourceMethods", 0, resource.getResourceMethods().size());
		Assert.assertEquals("Wrong number of subresource resourceMethods", 0, resource.getSubresourceMethods().size());
		Assert.assertEquals("Wrong number of subresource locators", 0, resource.getSubresourceLocators().size());
	}

	@Test
	public void shouldBecomeResourceMethodWhenRemovingPathAnnotation() throws CoreException {
		// pre-conditions
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		Assert.assertEquals("Wrong number of resourceMethods", 6, resource.getAllMethods().size());
		Assert.assertEquals("Wrong number of resource resourceMethods", 2, resource.getResourceMethods().size());
		Assert.assertEquals("Wrong number of subresource resourceMethods", 4, resource.getSubresourceMethods().size());
		Assert.assertEquals("Wrong number of subresource locators", 0, resource.getSubresourceLocators().size());
		// operation
		WorkbenchUtils.removeMethodAnnotation(resource.getJavaElement(), "getCustomerAsVCard", "@Path(\"{id}\")");
		// post-conditions
		Assert.assertEquals("Wrong number of resource resourceMethods", 3, resource.getResourceMethods().size());
		Assert.assertEquals("Wrong number of subresource resourceMethods", 3, resource.getSubresourceMethods().size());
		Assert.assertEquals("Wrong number of subresource locators", 0, resource.getSubresourceLocators().size());
	}

	@Test
	public void shouldBecomeResourceMethodWhenAddingHTTPMethodAnnotation() throws CoreException {
		// pre-conditions
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		Assert.assertEquals("Wrong number of resourceMethods", 6, resource.getAllMethods().size());
		Assert.assertEquals("Wrong number of resource resourceMethods", 2, resource.getResourceMethods().size());
		Assert.assertEquals("Wrong number of subresource resourceMethods", 4, resource.getSubresourceMethods().size());
		Assert.assertEquals("Wrong number of subresource locators", 0, resource.getSubresourceLocators().size());
		// operation
		WorkbenchUtils.addMethodAnnotion(resource.getJavaElement().getMethod("getEntityManager", null), "@GET");
		// post-conditions
		Assert.assertEquals("Wrong number of resourceMethods", 7, resource.getAllMethods().size());
		Assert.assertEquals("Wrong number of resource resourceMethods", 3, resource.getResourceMethods().size());
		Assert.assertEquals("Wrong number of subresource resourceMethods", 4, resource.getSubresourceMethods().size());
		Assert.assertEquals("Wrong number of subresource locators", 0, resource.getSubresourceLocators().size());
	}

	@Test
	public void shouldBecomeSubresourceMethodWhenAddingHTTPMethodAnnotation() throws CoreException {
		// pre-conditions
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		Assert.assertEquals("Wrong number of resource resourceMethods", 0, resource.getResourceMethods().size());
		Assert.assertEquals("Wrong number of subresource resourceMethods", 0, resource.getSubresourceMethods().size());
		Assert.assertEquals("Wrong number of subresource locators", 1, resource.getSubresourceLocators().size());
		ResourceMethod resourceMethod = resource.getByURIMapping(null, "/{type}", null, null);
		Assert.assertNotNull("ResourceMethod not found", resourceMethod);
		// operation
		WorkbenchUtils.addImport(resourceMethod.getJavaElement().getCompilationUnit(), GET.class.getName());
		WorkbenchUtils.addMethodAnnotion(resourceMethod.getJavaElement(), "@GET");
		// post-conditions
		Assert.assertEquals("Wrong number of resource resourceMethods", 0, resource.getResourceMethods().size());
		Assert.assertEquals("Wrong number of subresource resourceMethods", 1, resource.getSubresourceMethods().size());
		Assert.assertEquals("Wrong number of subresource locators", 0, resource.getSubresourceLocators().size());
	}

	@Test
	public void shouldBecomeSubresourceLocatorWhenRemovingHTTPMethodAnnotation() throws CoreException {
		// pre-conditions
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		Assert.assertEquals("Wrong number of resourceMethods", 6, resource.getAllMethods().size());
		Assert.assertEquals("Wrong number of resource resourceMethods", 2, resource.getResourceMethods().size());
		Assert.assertEquals("Wrong number of subresource resourceMethods", 4, resource.getSubresourceMethods().size());
		Assert.assertEquals("Wrong number of subresource locators", 0, resource.getSubresourceLocators().size());
		// operation
		WorkbenchUtils.removeMethodAnnotation(resource.getJavaElement(), "getCustomerAsVCard", "@GET");
		// post-conditions
		Assert.assertEquals("Wrong number of resource resourceMethods", 2, resource.getResourceMethods().size());
		Assert.assertEquals("Wrong number of subresource resourceMethods", 3, resource.getSubresourceMethods().size());
		Assert.assertEquals("Wrong number of subresource locators", 1, resource.getSubresourceLocators().size());
	}

	@Test
	public void shouldDecreaseWhenRemovingHTTPMethodAnnotation() throws CoreException {
		// pre-conditions
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		Assert.assertEquals("Wrong number of resourceMethods", 6, resource.getAllMethods().size());
		Assert.assertEquals("Wrong number of resource resourceMethods", 2, resource.getResourceMethods().size());
		Assert.assertEquals("Wrong number of subresource resourceMethods", 4, resource.getSubresourceMethods().size());
		Assert.assertEquals("Wrong number of subresource locators", 0, resource.getSubresourceLocators().size());
		// operation
		WorkbenchUtils.removeMethodAnnotation(resource.getJavaElement(), "createCustomer", "@POST");
		// post-conditions
		Assert.assertEquals("Wrong number of resource resourceMethods", 1, resource.getResourceMethods().size());
		Assert.assertEquals("Wrong number of subresource resourceMethods", 4, resource.getSubresourceMethods().size());
		Assert.assertEquals("Wrong number of subresource locators", 0, resource.getSubresourceLocators().size());
	}

	public void shouldChangeWhenModifyingPathAnnotationValue() throws CoreException {
		// pre-conditions
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		Assert.assertEquals("Wrong number of resourceMethods", 6, resource.getAllMethods().size());
		HTTPMethod httpMethod = metamodel.getHttpMethods().getByVerb("GET");
		ResourceMethod resourceMethod = resource.getByURIMapping(httpMethod, "{id}", null, null);
		Assert.assertNotNull("ResourceMethod not found", resourceMethod);
		// operation
		WorkbenchUtils.replaceFirstOccurrenceOfCode(resourceMethod.getJavaElement(), "@Path(\"{id}\")",
				"@Path(\"{idCustomer}\")");
		// post-conditions
		Assert.assertNull("No result expected", resource.getByURIMapping(httpMethod, "{id}", null, null));
		Assert.assertNotNull("No result found", resource.getByURIMapping(httpMethod, "{idCustomer}", null, null));
	}

	@Test
	public void shouldChangeWhenAddingProducesAnnotation() throws CoreException {
		// pre-conditions
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		Assert.assertEquals("Wrong number of resourceMethods", 6, resource.getAllMethods().size());
		HTTPMethod httpMethod = metamodel.getHttpMethods().getByVerb("GET");
		ResourceMethod resourceMethod = resource.getByURIMapping(httpMethod, "{id}", null, null);
		Assert.assertNotNull("ResourceMethod not found", resourceMethod);
		Assert.assertTrue("No result expected", resourceMethod.getUriMapping().getMediaTypeCapabilities()
				.getProducedMimeTypes().isEmpty());
		// operation
		WorkbenchUtils.addMethodAnnotion(resourceMethod.getJavaElement(), "@Produces(\"foo/bar\")");
		// post-conditions
		Assert.assertEquals("No result found", "foo/bar", resourceMethod.getUriMapping().getMediaTypeCapabilities()
				.getProducedMimeTypes().get(0));
	}

	@Test
	public void shouldChangeWhenModifyingProducesAnnotation() throws CoreException {
		// pre-conditions
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		Assert.assertEquals("Wrong number of resourceMethods", 6, resource.getAllMethods().size());
		HTTPMethod httpMethod = metamodel.getHttpMethods().getByVerb("GET");
		ResourceMethod resourceMethod = resource.getByURIMapping(httpMethod, "{id}", null, "text/x-vcard");
		Assert.assertNotNull("ResourceMethod not found", resourceMethod);
		Assert.assertEquals("Wrong result:", "text/x-vcard", resourceMethod.getUriMapping().getMediaTypeCapabilities()
				.getProducedMimeTypes().get(0));
		// operation
		WorkbenchUtils.replaceFirstOccurrenceOfCode(resourceMethod.getJavaElement(), "@Produces({ \"text/x-vcard\" })",
				"@Produces(\"foo/bar\")");
		// post-conditions
		Assert.assertEquals("Wrong result:", "foo/bar", resourceMethod.getUriMapping().getMediaTypeCapabilities()
				.getProducedMimeTypes().get(0));
	}

	@Test
	public void shouldChangeWhenRemovingProducesAnnotation() throws CoreException {
		// pre-conditions
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		Assert.assertEquals("Wrong number of resourceMethods", 6, resource.getAllMethods().size());
		HTTPMethod httpMethod = metamodel.getHttpMethods().getByVerb("GET");
		ResourceMethod resourceMethod = resource.getByURIMapping(httpMethod, "{id}", null, "text/x-vcard");
		Assert.assertNotNull("ResourceMethod not found", resourceMethod);
		List<String> producedMimeTypes = resourceMethod.getUriMapping().getMediaTypeCapabilities()
				.getProducedMimeTypes();
		Assert.assertEquals("No result found", "text/x-vcard", producedMimeTypes.get(0));
		// operation
		WorkbenchUtils.removeMethodAnnotation(resourceMethod.getJavaElement(), "@Produces({ \"text/x-vcard\" })");
		// post-conditions
		Assert.assertTrue("No result expected:" + producedMimeTypes, producedMimeTypes.isEmpty());
	}

	@Test
	public void shouldChangeWhenAddingConsumesAnnotation() throws CoreException {
		// pre-conditions
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		HTTPMethod httpMethod = metamodel.getHttpMethods().getByVerb("GET");
		ResourceMethod resourceMethod = resource.getByURIMapping(httpMethod, "/{id}", null, null);
		Assert.assertNotNull("ResourceMethod not found", resourceMethod);
		Assert.assertTrue("No result expected", resourceMethod.getUriMapping().getMediaTypeCapabilities()
				.getConsumedMimeTypes().isEmpty());
		// operation
		WorkbenchUtils.addMethodAnnotion(resourceMethod.getJavaElement(), "@Consumes(\"foo/bar\")");
		// post-conditions
		Assert.assertEquals("No result found", "foo/bar", resourceMethod.getUriMapping().getMediaTypeCapabilities()
				.getConsumedMimeTypes().get(0));
	}

	@Test
	public void shouldChangeWhenModifyingConsumesAnnotation() throws CoreException {
		// pre-conditions
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		HTTPMethod httpMethod = metamodel.getHttpMethods().getByVerb("POST");
		ResourceMethod resourceMethod = resource.getByURIMapping(httpMethod, null, null, null);
		Assert.assertNotNull("ResourceMethod not found", resourceMethod);
		MediaTypeCapabilities mediaTypeCapabilities = resourceMethod.getUriMapping().getMediaTypeCapabilities();
		Assert.assertEquals("No result found", "application/vnd.bytesparadise.game+xml", mediaTypeCapabilities
				.getConsumedMimeTypes().get(0));
		Assert.assertEquals("No result found", "application/xml", mediaTypeCapabilities.getConsumedMimeTypes().get(1));
		Assert.assertEquals("No result found", "application/json", mediaTypeCapabilities.getConsumedMimeTypes().get(2));
		// operation
		WorkbenchUtils.replaceFirstOccurrenceOfCode(resourceMethod.getJavaElement(),
				"@Consumes({ \"application/vnd.bytesparadise.game+xml\", \"application/xml\", \"application/json\" })",
				"@Consumes(\"foo/bar\")");
		// post-conditions
		Assert.assertEquals("No result found", 1, mediaTypeCapabilities.getConsumedMimeTypes().size());
		Assert.assertEquals("No result found", "foo/bar", mediaTypeCapabilities.getConsumedMimeTypes().get(0));
	}

	@Test
	public void shouldChangeWhenRemovingConsumesAnnotation() throws CoreException {
		// pre-conditions
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		HTTPMethod httpMethod = metamodel.getHttpMethods().getByVerb("POST");
		ResourceMethod resourceMethod = resource.getByURIMapping(httpMethod, null, null, null);
		Assert.assertNotNull("ResourceMethod not found", resourceMethod);
		MediaTypeCapabilities mediaTypeCapabilities = resourceMethod.getUriMapping().getMediaTypeCapabilities();
		Assert.assertEquals("No result found", "application/vnd.bytesparadise.game+xml", mediaTypeCapabilities
				.getConsumedMimeTypes().get(0));
		Assert.assertEquals("No result found", "application/xml", mediaTypeCapabilities.getConsumedMimeTypes().get(1));
		Assert.assertEquals("No result found", "application/json", mediaTypeCapabilities.getConsumedMimeTypes().get(2));
		// operation
		WorkbenchUtils.removeMethodAnnotation(resourceMethod.getJavaElement(),
				"@Consumes({ \"application/vnd.bytesparadise.game+xml\", \"application/xml\", \"application/json\" })");
		// post-conditions
		Assert.assertEquals("No result found", 0, mediaTypeCapabilities.getConsumedMimeTypes().size());
	}

	@Test
	public void shouldNotChangeWhenRenamingMethodName() throws CoreException {
		// pre-conditions
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		Assert.assertEquals("Wrong number of resourceMethods", 2, resource.getAllMethods().size());
		HTTPMethod httpMethod = metamodel.getHttpMethods().getByVerb("POST");
		ResourceMethod resourceMethod = resource.getByURIMapping(httpMethod, null, null, null);
		Assert.assertNotNull("ResourceMethod not found", resourceMethod);
		// operation
		WorkbenchUtils.rename(resourceMethod.getJavaElement(), "createProductPOST");
		// post-conditions
		Assert.assertEquals("Wrong number of resourceMethods", 2, resource.getAllMethods().size());
		resourceMethod = resource.getByURIMapping(httpMethod, null, null, null);
		Assert.assertNotNull("ResourceMethod not found", resourceMethod);
		Assert.assertEquals("Wrong java method name", "createProductPOST", resourceMethod.getJavaElement()
				.getElementName());
	}

	@Test
	public void shouldNotChangeWhenChangingParameters() throws CoreException {
		// pre-conditions
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		Assert.assertEquals("Wrong number of resourceMethods", 2, resource.getAllMethods().size());
		HTTPMethod httpMethod = metamodel.getHttpMethods().getByVerb("GET");
		ResourceMethod resourceMethod = resource.getByURIMapping(httpMethod, "/{id}", null, null);
		Assert.assertNotNull("ResourceMethod not found", resourceMethod);
		Assert.assertTrue("No query param expected", resourceMethod.getUriMapping().getQueryParams().isEmpty());
		// operation
		WorkbenchUtils.addImport(resourceMethod.getJavaElement().getCompilationUnit(), QueryParam.class.getName());
		WorkbenchUtils.addMethodParameter(resourceMethod.getJavaElement(), "@QueryParam(\"start\") int start");
		// post-conditions
		Assert.assertEquals("Wrong number of resourceMethods", 2, resource.getAllMethods().size());
		resourceMethod = resource.getByURIMapping(httpMethod, "/{id}", null, null);
		Assert.assertNotNull("ResourceMethod not found", resourceMethod);
		Assert.assertEquals("No query param found", 1, resourceMethod.getUriMapping().getQueryParams().size());
	}

	@Test
	public void shouldReportErrorWhenRemovingImport() throws JavaModelException {
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		List<ResourceMethod> allMethods = resource.getAllMethods();
		Assert.assertEquals("Wrong number of resourceMethods", 2, allMethods.size());
		HTTPMethod httpMethod = metamodel.getHttpMethods().getByVerb("GET");
		ResourceMethod resourceMethod = resource.getByURIMapping(httpMethod, "/{id}", null, null);
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
	}

	@Test
	public void shouldUpdateResourceMethodModelWhenChangingPathParamAnnotation() throws CoreException {
		Resource resource = metamodel.getResources().getByTypeName(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		Assert.assertEquals("Wrong number of resourceMethods", 6, resource.getAllMethods().size());
		HTTPMethod httpMethod = metamodel.getHttpMethods().getByVerb("GET");
		ResourceMethod resourceMethod = resource.getByURIMapping(httpMethod, "{id}", null, "text/x-vcard");
		Assert.assertNotNull("ResourceMethod not found", resourceMethod);
		Assert.assertEquals("Invalid PathParam", "id", resourceMethod.getUriMapping().getPathParams().get(0).getAnnotationValue());

		// operation
		WorkbenchUtils.replaceAllOccurrencesOfCode(resource.getJavaElement().getCompilationUnit(),
				"@PathParam(\"id\")", "@PathParam(\"ide\")");
		// post-conditions
		Assert.assertEquals("Invalid PathParam", "ide", resourceMethod.getUriMapping().getPathParams().get(0).getAnnotationValue());
	}

}
