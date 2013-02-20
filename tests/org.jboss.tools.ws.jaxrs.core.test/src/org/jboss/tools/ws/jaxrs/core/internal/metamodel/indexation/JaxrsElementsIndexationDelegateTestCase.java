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

package org.jboss.tools.ws.jaxrs.core.internal.metamodel.indexation;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.index.CorruptIndexException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.jboss.tools.ws.jaxrs.core.AbstractCommonTestCase;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsJavaApplication;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsProvider;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsWebxmlApplication;
import org.jboss.tools.ws.jaxrs.core.internal.utils.WtpUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsProvider;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResource;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResourceMethod;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author xcoulon
 * 
 */
public class JaxrsElementsIndexationDelegateTestCase extends AbstractCommonTestCase {

	private JaxrsElementsIndexationDelegate indexationService;

	@Before
	public void setupIndex() throws CoreException {
		indexationService = new JaxrsElementsIndexationDelegate();
	}

	@After
	public void cleanIndex() throws CorruptIndexException, IOException {
		indexationService.dispose();
	}

	@Test
	public void shouldIndexAndRetrieveEndpointWithSingleResourceMethod() throws CoreException {
		// pre-condition: to make it even funnier, we'll use a custom HTTP Method here
		final IType httpMethodType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		JaxrsHttpMethod.from(httpMethodType).withMetamodel(metamodel).build();
		final IType bazResourceType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.BazResource");
		final JaxrsResource bazResource = JaxrsResource.from(bazResourceType, metamodel.findAllHttpMethods()).withMetamodel(metamodel).build();
		final JaxrsResourceMethod bazResourceMethod = getResourceMethod(bazResource, "update3");
		// operation
		final List<JaxrsEndpoint> endpoints = metamodel.findEndpoints(bazResourceMethod);
		// verifications
		assertThat(endpoints.size(), equalTo(1));
	}

	@Test
	public void shouldIndexAndRetrieveEndpointWithCustomHttpMethod() throws CoreException {
		// pre-condition: to make it even funnier, we'll use a custom HTTP Method here
		final IType httpMethodType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final JaxrsHttpMethod fooHttpMethod = JaxrsHttpMethod.from(httpMethodType).withMetamodel(metamodel).build();
		final IType bazResourceType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.BazResource");
		JaxrsResource.from(bazResourceType, metamodel.findAllHttpMethods()).withMetamodel(metamodel).build();
		// operation
		final List<JaxrsEndpoint> endpoints = metamodel.findEndpoints(fooHttpMethod);
		// verifications
		assertThat(endpoints.size(), equalTo(1));
	}

	@Test
	public void shouldIndexAndRetrieveSingleEndpointWithMultipleMethods() throws CoreException {
		// pre-condition
		final IType productResourceLocatorType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		JaxrsResource.from(productResourceLocatorType, metamodel.findAllHttpMethods()).withMetamodel(metamodel).build();
		final IType bookResourceType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		final JaxrsResource bookResource = JaxrsResource.from(bookResourceType, metamodel.findAllHttpMethods()).withMetamodel(metamodel).build();
		final JaxrsResourceMethod bookResourceMethod = getResourceMethod(bookResource, "getProduct");
		// operation
		final List<JaxrsEndpoint> endpoints = metamodel.findEndpoints(bookResourceMethod);
		// verifications
		assertThat(endpoints.size(), equalTo(1));
	}

	@Test
	public void shouldIndexAndRetrieveMultipleEndpointsFromHttpMethod() throws CoreException {
		// pre-condition
		final IType productResourceLocatorType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		JaxrsResource.from(productResourceLocatorType, metamodel.findAllHttpMethods()).withMetamodel(metamodel).build();
		final IType bookResourceType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		JaxrsResource.from(bookResourceType, metamodel.findAllHttpMethods()).withMetamodel(metamodel).build();
		final JaxrsHttpMethod httpMethod = metamodel.findHttpMethodByTypeName("javax.ws.rs.GET");
		// operation
		final List<JaxrsEndpoint> endpoints = metamodel.findEndpoints(httpMethod);
		// verifications
		assertThat(endpoints.size(), equalTo(3));
	}

	@Test
	public void shouldIndexAndRetrieveMultipleEndpointsWithMultipleMethods() throws CoreException {
		final IType productResourceLocatorType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final JaxrsResource productResourceLocator = JaxrsResource.from(productResourceLocatorType, metamodel.findAllHttpMethods()).withMetamodel(metamodel).build();
		final IType bookResourceType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		JaxrsResource.from(bookResourceType, metamodel.findAllHttpMethods()).withMetamodel(metamodel).build();
		final IType gameResourceType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		JaxrsResource.from(gameResourceType, metamodel.findAllHttpMethods()).withMetamodel(metamodel).build();
		// operation
		final List<JaxrsEndpoint> endpoints = metamodel.findEndpoints(getResourceMethod(productResourceLocator, "getProductResourceLocator"));
		// verifications
		assertThat(endpoints.size(), equalTo(5));
	}

	@Test
	public void shouldIndexAndRetrieveMultipleEndpointsFromJavaApplication() throws CoreException {
		final IType applicationType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final JaxrsJavaApplication application = JaxrsJavaApplication.from(applicationType).withMetamodel(metamodel).build();
		final IType productResourceLocatorType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		JaxrsResource.from(productResourceLocatorType, metamodel.findAllHttpMethods()).withMetamodel(metamodel).build();
		final IType bookResourceType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		JaxrsResource.from(bookResourceType, metamodel.findAllHttpMethods()).withMetamodel(metamodel).build();
		final IType gameResourceType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		JaxrsResource.from(gameResourceType, metamodel.findAllHttpMethods()).withMetamodel(metamodel).build();
		// operation
		final List<JaxrsEndpoint> endpoints = metamodel.findEndpoints(application);
		// verifications
		assertThat(endpoints.size(), equalTo(5));
	}

	@Test
	public void shouldIndexAndRetrieveResourceMethod() throws CoreException {
		// pre-condition
		final IType bazResourceType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.BazResource");
		final JaxrsResource bazResource = JaxrsResource.from(bazResourceType, metamodel.findAllHttpMethods()).withMetamodel(metamodel).build();
		final JaxrsResourceMethod bazResourceMethod = getResourceMethod(bazResource, "update3");
		// operation
		List<IJaxrsResourceMethod> resourceMethods = metamodel.findResourceMethodsByReturnedType(bazResourceMethod.getReturnedType());
		// verification
		assertThat(resourceMethods.size(), equalTo(5));
	}

	@Test
	public void shouldIndexAndRetrieveProviderByProviderType() throws JavaModelException, CoreException {
		// pre-condition
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		JaxrsProvider.from(type).withMetamodel(metamodel).build();
		// operation
		final IJaxrsProvider provider = metamodel.findProvider(type);
		// verifications
		assertThat(provider, notNullValue());
	}

	@Test
	public void shouldIndexAndRetrieveRootResourceByType() throws JavaModelException, CoreException {
		// pre-condition
		final IType resourceType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.BazResource");
		JaxrsResource.from(resourceType, metamodel.findAllHttpMethods()).withMetamodel(metamodel).build();
		// operation
		final IJaxrsResource resource = metamodel.findResource(resourceType);
		// verifications
		assertThat(resource, notNullValue());
	}

	@Test
	public void shouldIndexAndRetrieveSubresourceByType() throws JavaModelException, CoreException {
		// pre-condition
		final IType resourceType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		JaxrsResource.from(resourceType, metamodel.findAllHttpMethods()).withMetamodel(metamodel).build();
		// operation
		final IJaxrsResource resource = metamodel.findResource(resourceType);
		// verifications
		assertThat(resource, notNullValue());
	}
	
	@Test
	public void shouldIndexAndRetrieveProviderByExceptionTypeName() throws JavaModelException, CoreException {
		// pre-condition
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		JaxrsProvider.from(type).withMetamodel(metamodel).build();
		// operation
		final IJaxrsProvider provider = metamodel.findProviders(EnumElementKind.EXCEPTION_MAPPER,
				"javax.persistence.EntityNotFoundException").get(0);
		// verifications
		assertThat(provider, notNullValue());
	}

	@Test
	public void shouldIndexAndRetrieveCustomHttpMethodByTypeName() throws CoreException {
		// pre-condition
		final IType httpMethodType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		JaxrsHttpMethod.from(httpMethodType).withMetamodel(metamodel).build();
		// operation
		final JaxrsHttpMethod httpMethod = metamodel.findHttpMethodByTypeName(httpMethodType.getFullyQualifiedName());
		// verifications
		assertThat(httpMethod, notNullValue());
	}

	@Test
	public void shouldIndexAndRetrieveBuiltinHttpMethodByTypeName() throws CoreException {
		// pre-condition
		final IType type = resolveType("javax.ws.rs.GET");
		// operation
		final JaxrsHttpMethod httpMethod = metamodel.findHttpMethodByTypeName(type.getFullyQualifiedName());
		// verifications
		assertThat(httpMethod, notNullValue());
	}

	@Test
	public void shouldIndexAndRetrieveDefaultWebxmlApplication() throws CoreException {
		// pre-condition
		final IResource webDeploymentDescriptor = WtpUtils.getWebDeploymentDescriptor(project);
		JaxrsWebxmlApplication.from(webDeploymentDescriptor).inMetamodel(metamodel).build();
		// operation
		final JaxrsWebxmlApplication webxmlApplication = metamodel.findWebxmlApplication();
		// verifications
		assertThat(webxmlApplication, notNullValue());
	}

	@Test
	public void shouldIndexAndRetrieveWebxmlApplicationByClassName() throws CoreException {
		// pre-condition
		final IResource webDeploymentDescriptor = WtpUtils.getWebDeploymentDescriptor(project);
		JaxrsWebxmlApplication.from(webDeploymentDescriptor).inMetamodel(metamodel).build();
		// operation
		final JaxrsWebxmlApplication webxmlApplication = metamodel
				.findWebxmlApplicationByClassName("javax.ws.rs.core.Application");
		// verifications
		assertThat(webxmlApplication, notNullValue());
	}

	@Test
	public void shouldIndexAndRetrieveJavaApplication() throws JavaModelException, CoreException {
		// pre-condition
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		JaxrsJavaApplication.from(type).withMetamodel(metamodel).build();
		// operation
		final JaxrsJavaApplication javaApplication = metamodel.findJavaApplicationByTypeName(type.getFullyQualifiedName());
		// verifications
		assertThat(javaApplication, notNullValue());
	}

}
