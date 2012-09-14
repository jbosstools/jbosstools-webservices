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

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.jboss.tools.ws.jaxrs.core.WorkbenchUtils.getAnnotation;
import static org.jboss.tools.ws.jaxrs.core.WorkbenchUtils.getMethod;
import static org.jboss.tools.ws.jaxrs.core.WorkbenchUtils.getType;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsElements.HTTP_METHOD;
import static org.junit.Assert.assertThat;

import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.WorkbenchUtils;
import org.jboss.tools.ws.jaxrs.core.builder.AbstractMetamodelBuilderTestCase;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsResource;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsResourceMethod;
import org.junit.Assert;
import org.junit.Test;

public class JaxrsMetamodelTestCase extends AbstractMetamodelBuilderTestCase {

	final IProgressMonitor progressMonitor = new NullProgressMonitor();

	@Test
	public void shouldGetHttpMethodByType() throws CoreException {
		IType javaType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject,
				progressMonitor);
		assertThat(metamodel.getElement(javaType, JaxrsHttpMethod.class), notNullValue());
	}

	@Test
	public void shouldNotGetHttpMethodByType() throws CoreException {
		IType javaType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				progressMonitor);
		assertThat(metamodel.getElement(javaType, JaxrsHttpMethod.class), nullValue());
	}

	@Test
	public void shouldGetHttpMethodByAnnotation() throws CoreException {
		IType javaType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject,
				progressMonitor);
		final Annotation annotation = getAnnotation(javaType, HTTP_METHOD.qualifiedName);
		assertThat((JaxrsHttpMethod) metamodel.getElement(annotation), notNullValue());
	}

	@Test
	public void shouldNotGetHttpMethodByAnnotation() throws CoreException {
		IType javaType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject,
				progressMonitor);
		final Annotation annotation = JdtUtils.resolveAnnotation(javaType, JdtUtils.parse(javaType, progressMonitor),
				Target.class);
		assertThat(metamodel.getElement(annotation), nullValue());
	}

	@Test
	public void shouldGetHttpMethodByCompilationUnit() throws CoreException {
		IType javaType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject,
				progressMonitor);
		final List<JaxrsHttpMethod> httpMethods = metamodel.getElements(javaType.getCompilationUnit(),
				JaxrsHttpMethod.class);
		assertThat(httpMethods.size(), equalTo(1));
	}

	@Test
	public void shouldNotGetHttpMethodByCompilationUnit() throws CoreException {
		IType javaType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				progressMonitor);
		final List<JaxrsHttpMethod> httpMethods = metamodel.getElements(javaType.getCompilationUnit(),
				JaxrsHttpMethod.class);
		assertThat(httpMethods.size(), equalTo(0));
	}

	@Test
	public void shouldGetHttpMethodByPackageFragmentRoot() throws CoreException {
		IPackageFragmentRoot src = WorkbenchUtils.getPackageFragmentRoot(javaProject, "src/main/java",
				new NullProgressMonitor());
		final List<JaxrsHttpMethod> httpMethods = metamodel.getElements(src, JaxrsHttpMethod.class);
		assertThat(httpMethods.size(), equalTo(1));
	}

	@Test
	public void shouldNotGetHttpMethodByPackageFragmentRoot() throws CoreException {
		IPackageFragmentRoot src = WorkbenchUtils.getPackageFragmentRoot(javaProject, "src/test/java",
				new NullProgressMonitor());
		final List<JaxrsHttpMethod> httpMethods = metamodel.getElements(src, JaxrsHttpMethod.class);
		assertThat(httpMethods.size(), equalTo(0));
	}
	
	@Test
	public void shouldAssertHTTPMethods() throws CoreException {
		// 6 fixed HttpMethods as part of the jax-rs API (@GET, etc.) + 1 in the project
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
		Assert.assertEquals(7, metamodel.getAllResources().size());
		for (IJaxrsResource jaxrsResource : metamodel.getAllResources()) {
			assertThat(((JaxrsResource) jaxrsResource).getJavaElement(), notNullValue());
			assertThat(((JaxrsResource) jaxrsResource).getKind(), notNullValue());
			assertThat(jaxrsResource.getAllMethods().size(), greaterThan(0));
		}
	}

	@Test
	public void shouldAssertResolvedEndpoints() throws CoreException {
		List<IJaxrsEndpoint> endpoints = metamodel.getAllEndpoints();
		Assert.assertEquals("Wrong result", 14, endpoints.size());
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
	public void shouldRetrieveApplicationPath() throws CoreException {
		assertThat(metamodel.getApplication().getKind(), equalTo(EnumKind.APPLICATION_WEBXML));
		assertThat(metamodel.getApplication().getApplicationPath(), equalTo("/hello"));
	}
	
	@Test
	public void shouldRetrieveCustomerResource() throws CoreException {
		IType customerType = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final IJaxrsResource customerResource = (IJaxrsResource) metamodel.getElement(customerType);
		Assert.assertNotNull("CustomerResource not found", customerType);
		Assert.assertEquals("Wrong number of resource resourceMethods", 6, customerResource.getAllMethods().size());
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
	public void shouldRetrieveBookSubresourceMethodProposals() throws CoreException {
		IType customerType = getType("org.jboss.tools.ws.jaxrs.sample.services.BookResource", javaProject);
		IMethod customerMethod = getMethod(customerType, "getProduct");
		final IJaxrsResourceMethod customerResourceMethod = (IJaxrsResourceMethod) metamodel.getElement(customerMethod);
		Assert.assertThat(customerResourceMethod, notNullValue());
		Assert.assertThat(customerResourceMethod.getPathParamValueProposals(), containsInAnyOrder("id"));
	}
	
	@Test
	public void shouldRetrieveBarResourceMethodProposals() throws CoreException {
		IType bazType = getType("org.jboss.tools.ws.jaxrs.sample.services.BazResource", javaProject);
		IMethod bazMethod = getMethod(bazType, "getContent2");
		final IJaxrsResourceMethod customerResourceMethod = (IJaxrsResourceMethod) metamodel.getElement(bazMethod);
		Assert.assertThat(customerResourceMethod, notNullValue());
		final List<String> pathParamValueProposals = customerResourceMethod.getPathParamValueProposals();
		Assert.assertThat(pathParamValueProposals, hasSize(3));
		Assert.assertThat(pathParamValueProposals, containsInAnyOrder("id", "format", "encoding"));
	}
	
	@Test
	public void shouldSortHttpMethods() {
		final List<IJaxrsHttpMethod> httpMethods = new ArrayList<IJaxrsHttpMethod>(metamodel.getAllHttpMethods());
		Collections.sort(httpMethods);
		assertThat(httpMethods.get(0).getHttpVerb(), equalTo("GET"));
		assertThat(httpMethods.get(5).getHttpVerb(), equalTo("OPTIONS"));
		assertThat(httpMethods.get(6).getHttpVerb(), equalTo("FOO"));
		
	}
	

}
