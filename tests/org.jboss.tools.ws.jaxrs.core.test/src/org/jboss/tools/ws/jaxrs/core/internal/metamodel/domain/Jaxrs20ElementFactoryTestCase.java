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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsNameBinding;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsParamConverterProvider;
import org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames;
import org.jboss.tools.ws.jaxrs.core.utils.JdtUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

public class Jaxrs20ElementFactoryTestCase {
	
	@ClassRule
	public static WorkspaceSetupRule workspaceSetupRule = new WorkspaceSetupRule(
			"org.jboss.tools.ws.jaxrs.tests.sampleproject2");
	@Rule
	public JaxrsMetamodelMonitor metamodelMonitor = new JaxrsMetamodelMonitor(
			"org.jboss.tools.ws.jaxrs.tests.sampleproject2", false);
	private JaxrsMetamodel metamodel = null;

	@Before
	public void setup() throws CoreException {
		metamodel = metamodelMonitor.getMetamodel();
	}

	@Test
	public void shouldCreateContainerRequestFilterWithPreMatchingAnnotationFromType() throws CoreException {
		// pre-conditions
		final IType providerType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomRequestFilter");
		// operation
		final List<IJaxrsElement> elements = JaxrsElementFactory.createElements(providerType,
				JdtUtils.parse(providerType, new NullProgressMonitor()), metamodel, new NullProgressMonitor());
		// verifications
		assertThat(elements.size(), equalTo(1));
		final JaxrsProvider provider = (JaxrsProvider) (elements.get(0));
		assertThat(provider.getAnnotations().size(), equalTo(2));
		assertThat(provider.getElementKind(), equalTo(EnumElementKind.CONTAINER_REQUEST_FILTER));
		assertThat(provider.getAnnotation(JaxrsClassnames.PRE_MATCHING), notNullValue());
		assertNull(provider.getProvidedType(EnumElementKind.CONTAINER_REQUEST_FILTER));
	}

	@Test
	public void shouldCreateContainerResponseFilterFromType() throws CoreException {
		// pre-conditions
		final IType providerType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilter");
		// operation
		final List<IJaxrsElement> elements = JaxrsElementFactory.createElements(providerType,
				JdtUtils.parse(providerType, new NullProgressMonitor()), metamodel, new NullProgressMonitor());
		// verifications
		assertThat(elements.size(), equalTo(1));
		final JaxrsProvider provider = (JaxrsProvider) (elements.get(0));
		assertThat(provider.getAnnotations().size(), equalTo(1));
		assertThat(provider.getElementKind(), equalTo(EnumElementKind.CONTAINER_RESPONSE_FILTER));
		assertNull(provider.getProvidedType(EnumElementKind.CONTAINER_RESPONSE_FILTER));
	}

	@Test
	public void shouldCreateContainerResponseFilterWithBindingFromType() throws CoreException {
		// pre-conditions
		final IType nameBindingType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding");
		JaxrsElementFactory.createElements(nameBindingType,
				JdtUtils.parse(nameBindingType, new NullProgressMonitor()), metamodel, new NullProgressMonitor());
		final IType providerType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding");
		// operation
		final List<IJaxrsElement> elements = JaxrsElementFactory.createElements(providerType,
				JdtUtils.parse(providerType, new NullProgressMonitor()), metamodel, new NullProgressMonitor());
		// verifications
		assertThat(elements.size(), equalTo(1));
		final JaxrsProvider provider = (JaxrsProvider) (elements.get(0));
		assertThat(provider.getAnnotations().size(), equalTo(2));
		assertThat(provider.getElementKind(), equalTo(EnumElementKind.CONTAINER_RESPONSE_FILTER));
		assertNull(provider.getProvidedType(EnumElementKind.CONTAINER_RESPONSE_FILTER));
		assertThat(provider.getNameBindingAnnotations().size(), equalTo(1));
		assertThat(provider.getNameBindingAnnotations().keySet().iterator().next(), equalTo("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding"));
	}
	
	@Test
	public void shouldCreateReaderInterceptorFromType() throws CoreException {
		// pre-conditions
		final IType providerType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomReaderInterceptor");
		// operation
		final List<IJaxrsElement> elements = JaxrsElementFactory.createElements(providerType,
				JdtUtils.parse(providerType, new NullProgressMonitor()), metamodel, new NullProgressMonitor());
		// verifications
		assertThat(elements.size(), equalTo(1));
		final JaxrsProvider provider = (JaxrsProvider) (elements.get(0));
		assertThat(provider.getAnnotations().size(), equalTo(1));
		assertThat(provider.getElementKind(), equalTo(EnumElementKind.ENTITY_READER_INTERCEPTOR));
		assertNull(provider.getProvidedType(EnumElementKind.ENTITY_READER_INTERCEPTOR));
	}
	
	@Test
	public void shouldCreateWriterInterceptorFromType() throws CoreException {
		// pre-conditions
		final IType providerType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomWriterInterceptor");
		// operation
		final List<IJaxrsElement> elements = JaxrsElementFactory.createElements(providerType,
				JdtUtils.parse(providerType, new NullProgressMonitor()), metamodel, new NullProgressMonitor());
		// verifications
		assertThat(elements.size(), equalTo(1));
		final JaxrsProvider provider = (JaxrsProvider) (elements.get(0));
		assertThat(provider.getAnnotations().size(), equalTo(1));
		assertThat(provider.getElementKind(), equalTo(EnumElementKind.ENTITY_WRITER_INTERCEPTOR));
		assertNull(provider.getProvidedType(EnumElementKind.ENTITY_WRITER_INTERCEPTOR));
	}

	@Test
	public void shouldCreateNameBindingAnnotationFromType() throws CoreException {
		// pre-conditions
		final IType nameBindingType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding");
		// operation
		final List<IJaxrsElement> elements = JaxrsElementFactory.createElements(nameBindingType,
				JdtUtils.parse(nameBindingType, new NullProgressMonitor()), metamodel, new NullProgressMonitor());
		// verifications
		assertThat(elements.size(), equalTo(1));
		final JaxrsNameBinding nameBinding = (JaxrsNameBinding) (elements.get(0));
		assertThat(nameBinding.getAnnotations().size(), equalTo(3));
		assertThat(nameBinding.getElementKind(), equalTo(EnumElementKind.NAME_BINDING));
		assertThat(metamodel.findAllNameBindings().size(), equalTo(1));
		assertThat(metamodel.findAllNameBindings().get(0), equalTo((IJaxrsNameBinding)nameBinding));
	}

	@Test
	public void shouldCreateParamConverterProviderFromType() throws CoreException {
		// pre-conditions
		final IType providerType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CarParamConverterProvider");
		// operation
		final List<IJaxrsElement> elements = JaxrsElementFactory.createElements(providerType,
				JdtUtils.parse(providerType, new NullProgressMonitor()), metamodel, new NullProgressMonitor());
		// verifications
		assertThat(elements.size(), equalTo(1));
		final JaxrsParamConverterProvider paramConverterProvider = (JaxrsParamConverterProvider) (elements.get(0));
		assertThat(paramConverterProvider.getAnnotations().size(), equalTo(1));
		assertThat(paramConverterProvider.getElementKind(), equalTo(EnumElementKind.PARAM_CONVERTER_PROVIDER));
		assertThat(metamodel.findAllParamConverterProviders().size(), equalTo(1));
		assertThat(metamodel.findAllParamConverterProviders().get(0), equalTo((IJaxrsParamConverterProvider)paramConverterProvider));
	}
	
	
	
}
