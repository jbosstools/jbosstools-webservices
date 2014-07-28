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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.jdt.Flags;
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.TestWatcher;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementCategory;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResource;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

public class Jaxrs11MetamodelTestCase {

	final IProgressMonitor progressMonitor = new NullProgressMonitor();
	
	@ClassRule
	public static WorkspaceSetupRule workspaceSetupRule = new WorkspaceSetupRule("org.jboss.tools.ws.jaxrs.tests.sampleproject");
	
	@Rule
	public JaxrsMetamodelMonitor metamodelMonitor = new JaxrsMetamodelMonitor("org.jboss.tools.ws.jaxrs.tests.sampleproject", true);
	
	@Rule
	public TestWatcher watcher = new TestWatcher();
	
	private JaxrsMetamodel metamodel = null;
	
	@Before
	public void setup() throws CoreException {
		metamodel = metamodelMonitor.getMetamodel();
	}
	
	@Test
	public void shouldFindHttpMethodByType() throws CoreException {
		final IType javaType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		assertThat(metamodel.findHttpMethodByTypeName(javaType.getFullyQualifiedName()), notNullValue());
	}

	@Test
	public void shouldNotFindHttpMethodByNullType() throws CoreException {
		assertThat(metamodel.findHttpMethodByTypeName(null), nullValue());
	}
	
	@Test
	public void shouldNotFindHttpMethodByType() throws CoreException {
		final IType javaType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		assertThat(metamodel.findHttpMethodByTypeName(javaType.getFullyQualifiedName()), nullValue());
	}

	@Test
	public void shouldAssertHTTPMethods() throws CoreException {
		// 6 fixed HttpMethods as part of the JAX-RS API (@GET, etc.) + 2 in the
		// project
		// (@FOO)
		Assert.assertEquals(8, metamodel.findAllHttpMethods().size());
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
		final Collection<IJaxrsResource> resources = metamodel.findAllResources();
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
		assertThat(metamodel.findAllApplications().size(), equalTo(2));
		assertThat(metamodel.findWebxmlApplications().size(), equalTo(1));
		assertThat(metamodel.findJavaApplications().size(), equalTo(1));
		assertThat(metamodel.findApplication().getApplicationPath(), equalTo("/hello"));
		assertThat(metamodel.findWebxmlApplicationByClassName(null), nullValue());
		assertThat(metamodel.findJavaApplicationByTypeName(null), nullValue());
		assertThat(metamodel.findJavaApplicationByTypeName(null), nullValue());
	}

	@Test
	public void shouldNotRetrieveElementFromNull() throws CoreException {
		final IJaxrsElement element = metamodel.findElement((IType)null);
		assertThat(element, nullValue());
	}

	@Test
	public void shouldNotRetrieveElementsFromNull() throws CoreException {
		final Collection<IJaxrsElement> elements = metamodel.findElements((IType)null);
		assertThat(elements.size(), equalTo(0));
	}

	@Test
	public void shouldNotRetrieveEndpointsFromNull() throws CoreException {
		final Collection<JaxrsEndpoint> elements = metamodel.findEndpoints((IJaxrsElement)null);
		assertThat(elements.size(), equalTo(0));
	}
	@Test
	public void shouldRetrieveCustomerResource() throws CoreException {
		final IType customerType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IJaxrsResource customerResource = (IJaxrsResource) metamodel.findElement(customerType);
		Assert.assertNotNull("CustomerResource not found", customerType);
		Assert.assertEquals("Wrong number of resource resourceMethods", 6, customerResource.getAllMethods().size());
	}
	
	@Test
	public void shouldRetrieveCustomerResourceMethodProposals() throws CoreException {
		final IType customerType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod customerMethod = metamodelMonitor.resolveMethod(customerType, "getCustomers");
		final IJaxrsResourceMethod customerResourceMethod = (IJaxrsResourceMethod) metamodel.findElement(customerMethod);
		Assert.assertThat(customerResourceMethod, notNullValue());
		Assert.assertThat(customerResourceMethod.getPathTemplateParameters().size(), equalTo(0));
		Assert.assertThat(customerResourceMethod.getParentResource().getPathTemplateParameters().size(), equalTo(0));
	}

	@Test
	public void shouldRetrieveCustomerSubresourceMethodProposals() throws CoreException {
		final IType customerType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod customerMethod = metamodelMonitor.resolveMethod(customerType, "getCustomer");
		final IJaxrsResourceMethod customerResourceMethod = (IJaxrsResourceMethod) metamodel.findElement(customerMethod);
		Assert.assertThat(customerResourceMethod, notNullValue());
		Assert.assertThat(customerResourceMethod.getPathTemplateParameters().keySet(), containsInAnyOrder("id"));
	}

	@Test
	public void shouldRetrieveBookResourceMethodProposals() throws CoreException {
		final IType bookType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		final IMethod bookMethod = metamodelMonitor.resolveMethod(bookType, "getAllProducts");
		final IJaxrsResourceMethod bookResourceMethod = (IJaxrsResourceMethod) metamodel.findElement(bookMethod);
		Assert.assertThat(bookResourceMethod, notNullValue());
		Assert.assertThat(bookResourceMethod.getPathTemplateParameters().size(), equalTo(0));
		Assert.assertThat(bookResourceMethod.getParentResource().getPathTemplateParameters().size(), equalTo(0));
	}

	@Test
	public void shouldRetrieveBookSubresourceMethodProposals() throws CoreException {
		final IType bookType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		final IMethod bookMethod = metamodelMonitor.resolveMethod(bookType, "getProduct");
		final IJaxrsResourceMethod bookResourceMethod = (IJaxrsResourceMethod) metamodel.findElement(bookMethod);
		Assert.assertThat(bookResourceMethod, notNullValue());
		Assert.assertThat(bookResourceMethod.getPathTemplateParameters().keySet(), containsInAnyOrder("id"));
	}

	@Test
	public void shouldRetrieveBarResourceMethodProposals() throws CoreException {
		final IType bazType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BazResource");
		final IMethod bazMethod = metamodelMonitor.resolveMethod(bazType, "getContent2");
		final IJaxrsResourceMethod bazResourceMethod = (IJaxrsResourceMethod) metamodel.findElement(bazMethod);
		Assert.assertThat(bazResourceMethod, notNullValue());
		final List<String> proposals = new ArrayList<String>(bazResourceMethod.getPathTemplateParameters().keySet());
		proposals.addAll(bazResourceMethod.getParentResource().getPathTemplateParameters().keySet());
		Assert.assertThat(proposals, hasSize(3));
		Assert.assertThat(proposals, containsInAnyOrder("id", "format", "encoding"));
	}

	@Test
	public void shouldSortHttpMethods() {
		final List<IJaxrsHttpMethod> httpMethods = new ArrayList<IJaxrsHttpMethod>(metamodel.findAllHttpMethods());
		Collections.sort(httpMethods);
		assertThat(httpMethods.get(0).getHttpVerb(), equalTo("GET"));
		assertThat(httpMethods.get(5).getHttpVerb(), equalTo("OPTIONS"));
		assertThat(httpMethods.get(6).getHttpVerb(), equalTo("BAR"));
		assertThat(httpMethods.get(7).getHttpVerb(), equalTo("FOO"));
	}
	
	@Test
	public void shouldDoNothingWhenAddingNullElement() throws CoreException {
		// pre-conditions
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		metamodel.add((JaxrsBaseElement)null);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(0));
	}

	@Test
	public void shouldDoNothingWhenUpdatingNullElement() throws CoreException {
		// pre-conditions
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		metamodel.update(new JaxrsElementDelta(null, 0, Flags.NONE));
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(0));
	}
	
	@Test
	public void shouldDoNothingWhenUpdatingUnchangedElement() throws CoreException {
		// pre-conditions
		final IType bazType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BazResource");
		final IMethod bazMethod = metamodelMonitor.resolveMethod(bazType, "getContent2");
		final IJaxrsResourceMethod bazResourceMethod = (IJaxrsResourceMethod) metamodel.findElement(bazMethod);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		metamodel.update(new JaxrsElementDelta(bazResourceMethod, CHANGED, Flags.NONE));
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(0));
	}
	
	@Test
	public void shouldDoNothingWhenAddingNullEndpoint() throws CoreException {
		// pre-conditions
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		metamodel.add((JaxrsEndpoint)null);
		// verifications
		assertThat(metamodelMonitor.getEndpointChanges().size(), equalTo(0));
	}
	
	@Test
	public void shouldDoNothingWhenUpdatingNullEndpoint() throws CoreException {
		// pre-conditions
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		metamodel.update((JaxrsEndpoint)null);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(0));
	}
	
	@Test
	public void shouldGetElementsFromResource() throws CoreException {
		// pre-condition
		final IType customerType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IResource customerResource = customerType.getResource();
		// operation
		final Collection<IJaxrsElement> elements = metamodel.findElements(customerResource);
		// verification: 1 Resource only (children resource methods and resource fields are not indexed by the underlying resource) 
		assertThat(elements.size(), equalTo(1));
	}
	
}
