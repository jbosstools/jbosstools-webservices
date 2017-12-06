/******************************************************************************* 
 * Copyright (c) 2008 - 2014 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/

package org.jboss.tools.ws.jaxrs.core.internal.metamodel.indexation;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.lucene.index.CorruptIndexException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsJavaApplication;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsProvider;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsWebxmlApplication;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.search.JaxrsElementsIndexationDelegate;
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsJavaApplication;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsProvider;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResource;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author xcoulon
 * 
 */
public class JaxrsElementsIndexationDelegateTestCase {

	private JaxrsElementsIndexationDelegate indexationService;

	@ClassRule
	public static WorkspaceSetupRule workspaceSetupRule = new WorkspaceSetupRule("org.jboss.tools.ws.jaxrs.tests.sampleproject");
	
	@Rule
	public JaxrsMetamodelMonitor metamodelMonitor = new JaxrsMetamodelMonitor("org.jboss.tools.ws.jaxrs.tests.sampleproject", false);
	
	private JaxrsMetamodel metamodel = null;

	@Before
	public void setup() throws CoreException {
		metamodel = metamodelMonitor.getMetamodel();
		indexationService = new JaxrsElementsIndexationDelegate(metamodel);
	}

	@After
	public void cleanIndex() throws CorruptIndexException, IOException, CoreException {
		indexationService.dispose();
		metamodel.remove();
	}

	@Test
	public void shouldIndexAndRetrieveEndpointWithSingleResourceMethod() throws CoreException {
		// pre-condition: to make it even funnier, we'll use a custom HTTP Method here
		final IType httpMethodType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		JaxrsHttpMethod.from(httpMethodType).withMetamodel(metamodel).build();
		final IType bazResourceType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BazResource");
		final JaxrsResource bazResource = JaxrsResource.from(bazResourceType, metamodel.findAllHttpMethodNames()).withMetamodel(metamodel).build();
		final JaxrsResourceMethod bazResourceMethod = metamodelMonitor.resolveResourceMethod(bazResource, "update3");
		// operation
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(bazResourceMethod);
		// verifications
		assertThat(endpoints.size(), equalTo(1));
	}

	@Test
	public void shouldIndexAndRetrieveEndpointWithCustomHttpMethod() throws CoreException {
		// pre-condition: to make it even funnier, we'll use a custom HTTP Method here
		final IType httpMethodType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final JaxrsHttpMethod fooHttpMethod = JaxrsHttpMethod.from(httpMethodType).withMetamodel(metamodel).build();
		final IType bazResourceType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BazResource");
		JaxrsResource.from(bazResourceType, metamodel.findAllHttpMethodNames()).withMetamodel(metamodel).build();
		// operation
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(fooHttpMethod);
		// verifications
		assertThat(endpoints.size(), equalTo(1));
	}

	@Test
	public void shouldIndexAndRetrieveSingleEndpointWithMultipleMethods() throws CoreException {
		// pre-condition
		final IType productResourceLocatorType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		JaxrsResource.from(productResourceLocatorType, metamodel.findAllHttpMethodNames()).withMetamodel(metamodel).build();
		final IType bookResourceType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		final JaxrsResource bookResource = JaxrsResource.from(bookResourceType, metamodel.findAllHttpMethodNames()).withMetamodel(metamodel).build();
		final JaxrsResourceMethod bookResourceMethod = metamodelMonitor.resolveResourceMethod(bookResource, "getProduct");
		// operation
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(bookResourceMethod);
		// verifications
		assertThat(endpoints.size(), equalTo(1));
	}

	@Test
	public void shouldIndexAndRetrieveMultipleEndpointsFromHttpMethod() throws CoreException {
		// pre-condition
		final IType productResourceLocatorType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		JaxrsResource.from(productResourceLocatorType, metamodel.findAllHttpMethodNames()).withMetamodel(metamodel).build();
		final IType bookResourceType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		JaxrsResource.from(bookResourceType, metamodel.findAllHttpMethodNames()).withMetamodel(metamodel).build();
		final JaxrsHttpMethod httpMethod = metamodel.findHttpMethodByTypeName("javax.ws.rs.GET");
		// operation
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(httpMethod);
		// verifications
		assertThat(endpoints.size(), equalTo(3));
	}

	@Test
	public void shouldIndexAndRetrieveMultipleEndpointsWithMultipleMethods() throws CoreException {
		final IType productResourceLocatorType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final JaxrsResource productResourceLocator = JaxrsResource.from(productResourceLocatorType, metamodel.findAllHttpMethodNames()).withMetamodel(metamodel).build();
		final IType bookResourceType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		JaxrsResource.from(bookResourceType, metamodel.findAllHttpMethodNames()).withMetamodel(metamodel).build();
		final IType gameResourceType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		JaxrsResource.from(gameResourceType, metamodel.findAllHttpMethodNames()).withMetamodel(metamodel).build();
		// operation
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(metamodelMonitor.resolveResourceMethod(productResourceLocator, "getProductResourceLocator"));
		// verifications
		assertThat(endpoints.size(), equalTo(5));
	}

	@Test
	public void shouldIndexAndRetrieveMultipleEndpointsFromJavaApplication() throws CoreException {
		final IType applicationType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final JaxrsJavaApplication application = JaxrsJavaApplication.from(applicationType).withMetamodel(metamodel).build();
		final IType productResourceLocatorType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		JaxrsResource.from(productResourceLocatorType, metamodel.findAllHttpMethodNames()).withMetamodel(metamodel).build();
		final IType bookResourceType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		JaxrsResource.from(bookResourceType, metamodel.findAllHttpMethodNames()).withMetamodel(metamodel).build();
		final IType gameResourceType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		JaxrsResource.from(gameResourceType, metamodel.findAllHttpMethodNames()).withMetamodel(metamodel).build();
		// operation
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(application);
		// verifications
		assertThat(endpoints.size(), equalTo(5));
	}

	@Test
	public void shouldIndexAndRetrieveResourceMethod() throws CoreException {
		// pre-condition
		final IType bazResourceType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BazResource");
		final JaxrsResource bazResource = JaxrsResource.from(bazResourceType, metamodel.findAllHttpMethodNames()).withMetamodel(metamodel).build();
		final JaxrsResourceMethod bazResourceMethod = metamodelMonitor.resolveResourceMethod(bazResource, "update3");
		// operation
		Collection<IJaxrsResourceMethod> resourceMethods = metamodel.findResourceMethodsByReturnedType(bazResourceMethod.getReturnedType().getErasureType());
		// verification
		assertThat(resourceMethods.size(), equalTo(5));
	}

	@Test
	public void shouldNotRetrieveResourceMethodFromNull() throws CoreException {
		// operation
		Collection<IJaxrsResourceMethod> resourceMethods = metamodel.findResourceMethodsByReturnedType(null);
		// verification
		assertThat(resourceMethods.size(), equalTo(0));
	}

	@Test
	public void shouldIndexAndRetrieveProviderByProviderType() throws JavaModelException, CoreException {
		// pre-condition
		final IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		JaxrsProvider.from(type).withMetamodel(metamodel).build();
		// operation
		final IJaxrsProvider provider = metamodel.findProvider(type);
		// verifications
		assertThat(provider, notNullValue());
	}

	@Test
	public void shouldNotIndexAndRetrieveNullProvider() throws JavaModelException, CoreException {
		// pre-condition
		final IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		JaxrsProvider.from(type).withMetamodel(metamodel).build();
		// operation
		final IJaxrsProvider foundProvider = metamodel.findProvider((IType)null);
		// verifications
		assertThat(foundProvider, nullValue());
	}

	@Test
	public void shouldIndexAndRetrieveResourceByAnnotation() throws JavaModelException, CoreException {
		// pre-condition
		final IType resourceType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BazResource");
		final IJaxrsResource resource = JaxrsResource.from(resourceType, metamodel.findAllHttpMethodNames()).withMetamodel(metamodel).build();
		// operation
		final Collection<IJaxrsResource> foundResources = metamodel.findResourcesByAnnotation(JaxrsClassnames.PATH);
		// verifications
		assertThat(foundResources.size(), equalTo(1));
		assertThat(foundResources.iterator().next(), equalTo(resource));
	}
	
	@Test
	public void shouldIndexAndRetrieveRootResourceByType() throws JavaModelException, CoreException {
		// pre-condition
		final IType resourceType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BazResource");
		final IJaxrsResource resource = JaxrsResource.from(resourceType, metamodel.findAllHttpMethodNames()).withMetamodel(metamodel).build();
		// operation
		final IJaxrsResource foundResource = metamodel.findResource(resourceType);
		// verifications
		assertThat(foundResource, equalTo(resource));
	}

	@Test
	public void shouldIndexAndRetrieveRootResourceByUnderlyingResource() throws JavaModelException, CoreException {
		// pre-condition
		final IType resourceType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BazResource");
		final IJaxrsResource resource = JaxrsResource.from(resourceType, metamodel.findAllHttpMethodNames()).withMetamodel(metamodel).build();
		// operation
		final IJaxrsResource foundResource = (IJaxrsResource) metamodel.findElement(resourceType.getResource());
		// verifications
		assertThat(foundResource, equalTo(resource));
	}

	@Test
	public void shouldIndexAndRetrieveSubresourceByType() throws JavaModelException, CoreException {
		// pre-condition
		final IType resourceType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		final IJaxrsResource gameResource = JaxrsResource.from(resourceType, metamodel.findAllHttpMethodNames()).withMetamodel(metamodel).build();
		// operation
		final IJaxrsResource foundResource = metamodel.findResource(resourceType);
		// verifications
		assertThat(foundResource, equalTo(gameResource));
	}
	
	@Test
	public void shouldIndexAndRetrieveSubresourceByUnderlyingResource() throws JavaModelException, CoreException {
		// pre-condition
		final IType resourceType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		final IJaxrsResource gameResource = JaxrsResource.from(resourceType, metamodel.findAllHttpMethodNames()).withMetamodel(metamodel).build();
		// operation
		final IJaxrsResource foundResource = (IJaxrsResource) metamodel.findElement(resourceType.getResource());
		// verifications
		assertThat(foundResource, equalTo(gameResource));
	}
	
	@Test
	public void shouldIndexAndRetrieveProviderByExceptionTypeName() throws JavaModelException, CoreException {
		// pre-condition
		final IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final IJaxrsProvider provider = JaxrsProvider.from(type).withMetamodel(metamodel).build();
		// operation
		final IJaxrsProvider foundProvider = metamodel.findProviders(EnumElementKind.EXCEPTION_MAPPER,
				"javax.persistence.EntityNotFoundException").iterator().next();
		// verifications
		assertThat(foundProvider, equalTo(provider));
	}

	@Test
	public void shouldIndexAndRetrieveProviderByUnderlyingResource() throws JavaModelException, CoreException {
		// pre-condition
		final IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final IJaxrsProvider provider = JaxrsProvider.from(type).withMetamodel(metamodel).build();
		// operation
		final IJaxrsProvider foundProvider = (IJaxrsProvider) metamodel.findElement(provider.getResource());
		// verifications
		assertThat(foundProvider, equalTo(provider));
	}

	@Test
	public void shouldIndexAndRetrieveProviderByAnnotation() throws JavaModelException, CoreException {
		// pre-condition
		final IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final IJaxrsProvider provider = JaxrsProvider.from(type).withMetamodel(metamodel).build();
		// operation
		final Collection<IJaxrsProvider> foundProviders = metamodel.findProvidersByAnnotation(JaxrsClassnames.PROVIDER);
		// verifications
		assertThat(foundProviders.size(), equalTo(1));
		assertThat(foundProviders.iterator().next(), equalTo(provider));
	}

	@Test
	public void shouldNotRetrieveProviderByNullExceptionTypeName() throws JavaModelException, CoreException {
		// operation
		final Collection<JaxrsProvider> providers = metamodel.findProviders(EnumElementKind.EXCEPTION_MAPPER,
				null);
		// verifications
		assertThat(providers.size(), equalTo(0));
	}
	
	@Test
	public void shouldIndexAndRetrieveCustomHttpMethodByTypeName() throws CoreException {
		// pre-condition
		final IType httpMethodType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final JaxrsHttpMethod httpMethod = JaxrsHttpMethod.from(httpMethodType).withMetamodel(metamodel).build();
		// operation
		final JaxrsHttpMethod foundHttpMethod = metamodel.findHttpMethodByTypeName(httpMethodType.getFullyQualifiedName());
		// verifications
		assertThat(foundHttpMethod, equalTo(httpMethod));
	}

	@Test
	public void shouldIndexAndRetrieveCustomHttpMethodByUnderlyingResource() throws CoreException {
		// pre-condition
		final IType httpMethodType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final JaxrsHttpMethod httpMethod = JaxrsHttpMethod.from(httpMethodType).withMetamodel(metamodel).build();
		// operation
		final JaxrsHttpMethod foundHttpMethod = (JaxrsHttpMethod) metamodel.findElement(httpMethodType.getResource());
		// verifications
		assertThat(foundHttpMethod, equalTo(httpMethod));
	}

	@Test
	public void shouldIndexAndRetrieveBuiltinHttpMethodByTypeName() throws CoreException {
		// pre-condition
		final IType type = metamodelMonitor.resolveType("javax.ws.rs.GET");
		// operation
		final JaxrsHttpMethod httpMethod = metamodel.findHttpMethodByTypeName(type.getFullyQualifiedName());
		// verifications
		assertThat(httpMethod, notNullValue());
	}

	@Test
	public void shouldIndexAndRetrieveDefaultWebxmlApplication() throws CoreException {
		// pre-condition
		final IResource webDeploymentDescriptor = metamodelMonitor.getWebDeploymentDescriptor();
		final JaxrsWebxmlApplication webxmlApplication = JaxrsWebxmlApplication.from(webDeploymentDescriptor).inMetamodel(metamodel).build();
		// operation
		final JaxrsWebxmlApplication foundWebxmlApplication = metamodel.findWebxmlApplication();
		// verifications
		assertThat(foundWebxmlApplication, equalTo(webxmlApplication));
	}

	@Test
	public void shouldIndexAndRetrieveWebxmlApplicationByClassName() throws CoreException {
		// pre-condition
		final IResource webDeploymentDescriptor = metamodelMonitor.getWebDeploymentDescriptor();
		final JaxrsWebxmlApplication webxmlApplication = JaxrsWebxmlApplication.from(webDeploymentDescriptor).inMetamodel(metamodel).build();
		// operation
		final JaxrsWebxmlApplication foundWebxmlApplication = metamodel
				.findWebxmlApplicationByClassName("javax.ws.rs.core.Application");
		// verifications
		assertThat(foundWebxmlApplication, equalTo(webxmlApplication));
	}
	
	@Test
	public void shouldIndexAndRetrieveWebxmlApplicationByUnderlyingResource() throws JavaModelException, CoreException {
		// pre-condition
		final IResource webDeploymentDescriptor = metamodelMonitor.getWebDeploymentDescriptor();
		final JaxrsWebxmlApplication webxmlApplication = JaxrsWebxmlApplication.from(webDeploymentDescriptor).inMetamodel(metamodel).build();
		// operation
		final JaxrsWebxmlApplication foundWebxmlApplication = (JaxrsWebxmlApplication) metamodel
				.findElement(webxmlApplication.getResource());
		// verifications
		assertThat(foundWebxmlApplication, equalTo(webxmlApplication));
	}

	@Test
	public void shouldIndexAndRetrieveJavaApplicationByTypeName() throws JavaModelException, CoreException {
		// pre-condition
		final IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final JaxrsJavaApplication application = JaxrsJavaApplication.from(type).withMetamodel(metamodel).build();
		// operation
		final JaxrsJavaApplication foundApplication = metamodel.findJavaApplicationByTypeName(type.getFullyQualifiedName());
		// verifications
		assertThat(foundApplication, equalTo(application));
	}
	
	@Test
	public void shouldIndexAndRetrieveJavaApplicationByUnderlyingResource() throws JavaModelException, CoreException {
		// pre-condition
		final IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final JaxrsJavaApplication application = JaxrsJavaApplication.from(type).withMetamodel(metamodel).build();
		// operation
		final JaxrsJavaApplication element = (JaxrsJavaApplication) metamodel.findElement(type.getResource());
		// verifications
		assertThat(element, notNullValue());
		assertThat(element, equalTo(application));
	}
	
	@Test
	public void shouldIndexAndRetrieveApplicationByAnnotation() throws JavaModelException, CoreException {
		// pre-condition
		final IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final IJaxrsJavaApplication application = JaxrsJavaApplication.from(type).withMetamodel(metamodel).build();
		// operation
		final Collection<IJaxrsJavaApplication> elements = metamodel.findApplicationsByAnnotation(JaxrsClassnames.APPLICATION_PATH);
		// verifications
		assertThat(elements, notNullValue());
		assertThat(elements.size(), equalTo(1));
		assertThat(elements, contains(application));
	}

	@Test
	public void shouldIndexAndRetrieveElementsByAnnotation() throws JavaModelException, CoreException {
		// pre-condition
		final IType barResourceType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BarResource");
		final IJaxrsResource barResource = JaxrsResource.from(barResourceType, metamodel.findAllHttpMethodNames()).withMetamodel(metamodel).build();
		final IType bazResourceType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BazResource");
		final IJaxrsResource bazResource = JaxrsResource.from(bazResourceType, metamodel.findAllHttpMethodNames()).withMetamodel(metamodel).build();
		// operation
		final Collection<IJaxrsElement> foundElements = metamodel.findElementsByAnnotation(JaxrsClassnames.PATH);
		// verifications: 2 resources + 5 resource methods *each*
		assertThat(foundElements.size(), equalTo(12));
		final List<IJaxrsElement> expectedMatches = new ArrayList<IJaxrsElement>();
		expectedMatches.add(barResource);
		expectedMatches.addAll(barResource.getAllMethods());
		expectedMatches.add(bazResource);
		expectedMatches.addAll(bazResource.getAllMethods());
		// thanks, Hamcrest and generics, for making the syntax below so complicated...
		assertThat(foundElements, containsInAnyOrder(expectedMatches.toArray(new IJaxrsElement[expectedMatches.size()])));
	}
	
	
}
