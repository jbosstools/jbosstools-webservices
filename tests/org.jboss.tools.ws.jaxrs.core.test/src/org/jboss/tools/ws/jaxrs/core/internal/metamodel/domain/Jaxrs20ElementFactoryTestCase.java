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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils.replaceAllOccurrencesOfCode;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils;
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsNameBinding;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsParamConverterProvider;
import org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames;
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
		final JaxrsProvider provider = (JaxrsProvider) (elements.iterator().next());
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
		final JaxrsProvider provider = (JaxrsProvider) (elements.iterator().next());
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
		final JaxrsProvider provider = (JaxrsProvider) (elements.iterator().next());
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
		final JaxrsProvider provider = (JaxrsProvider) (elements.iterator().next());
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
		final JaxrsProvider provider = (JaxrsProvider) (elements.iterator().next());
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
		final JaxrsNameBinding nameBinding = (JaxrsNameBinding) (elements.iterator().next());
		assertThat(nameBinding.getAnnotations().size(), equalTo(3));
		assertThat(nameBinding.getElementKind(), equalTo(EnumElementKind.NAME_BINDING));
		assertThat(metamodel.findAllNameBindings().size(), equalTo(1));
		assertThat(metamodel.findAllNameBindings().iterator().next(), equalTo((IJaxrsNameBinding)nameBinding));
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
		final JaxrsParamConverterProvider paramConverterProvider = (JaxrsParamConverterProvider) (elements.iterator().next());
		assertThat(paramConverterProvider.getAnnotations().size(), equalTo(1));
		assertThat(paramConverterProvider.getElementKind(), equalTo(EnumElementKind.PARAM_CONVERTER_PROVIDER));
		assertThat(metamodel.findAllParamConverterProviders().size(), equalTo(1));
		assertThat(metamodel.findAllParamConverterProviders().iterator().next(), equalTo((IJaxrsParamConverterProvider)paramConverterProvider));
	}
	
	@Test
	public void shouldCreateResourceMethodWithBeanParamAnnotatedParameter() throws JavaModelException, CoreException {
		// pre-conditions
		final IType carType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CarResource");
		replaceAllOccurrencesOfCode(carType, "update(final CarParameterAggregator car)", "update(@BeanParam final CarParameterAggregator car)", false);
		assertThat(carType, notNullValue());
		final IMethod carMethod = JavaElementsUtils.getMethod(carType, "update");
		assertThat(carMethod, notNullValue());
		// operation
		final List<IJaxrsElement> elements = JaxrsElementFactory.createElements(carType,
				JdtUtils.parse(carType, new NullProgressMonitor()), metamodel, new NullProgressMonitor());
		// verifications
		assertThat(elements.size(), equalTo(3));
		final JaxrsResource carResource = metamodel.findResource(carType);
		final JaxrsResourceMethod resourceMethod = carResource.getMethods().get(carMethod.getHandleIdentifier());
		assertThat(resourceMethod.getJavaMethodParameters().size(), equalTo(1));
		assertThat(resourceMethod.getJavaMethodParameters().iterator().next().getAnnotation(JaxrsClassnames.BEAN_PARAM), notNullValue());
	}
	
	@Test
	public void shouldCreateParameterAggregatorFromTypeWithAnnotatedMethod() throws CoreException {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("ParameterAggregator.txt", "org.jboss.tools.ws.jaxrs.sample.services",
				"ParameterAggregator.java");
		final IType parameterAggregatorType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ParameterAggregator");
		assertThat(parameterAggregatorType, notNullValue());
		replaceAllOccurrencesOfCode(parameterAggregatorType, "// PLACEHOLDER",
				"@PathParam(\"id2\") public void setId2(String id2) {}", false);
		// operation
		final List<IJaxrsElement> elements = JaxrsElementFactory.createElements(parameterAggregatorType,
				JdtUtils.parse(parameterAggregatorType, new NullProgressMonitor()), metamodel,
				new NullProgressMonitor());
		// verification: parameter aggregator with child method created
		assertThat(elements.size(), equalTo(2));
		assertThat(elements.iterator().next().getElementKind(), equalTo(EnumElementKind.PARAMETER_AGGREGATOR));
		assertThat(elements.get(1).getElementKind(), equalTo(EnumElementKind.PARAMETER_AGGREGATOR_PROPERTY));
	}

	@Test
	public void shouldCreateParameterAggregatorFromTypeWithAnnotatedField() throws CoreException {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("ParameterAggregator.txt", "org.jboss.tools.ws.jaxrs.sample.services",
				"ParameterAggregator.java");
		final IType parameterAggregatorType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ParameterAggregator");
		assertThat(parameterAggregatorType, notNullValue());
		replaceAllOccurrencesOfCode(parameterAggregatorType, "// PLACEHOLDER",
				"@PathParam(\"id2\") public String id2;", false);
		// operation
		final List<IJaxrsElement> elements = JaxrsElementFactory.createElements(parameterAggregatorType,
				JdtUtils.parse(parameterAggregatorType, new NullProgressMonitor()), metamodel,
				new NullProgressMonitor());
		// verification: parameter aggregator with child method created
		assertThat(elements.size(), equalTo(2));
		assertThat(elements.iterator().next().getElementKind(), equalTo(EnumElementKind.PARAMETER_AGGREGATOR));
		assertThat(elements.get(1).getElementKind(), equalTo(EnumElementKind.PARAMETER_AGGREGATOR_FIELD));
	}

	@Test
	public void shouldNotCreateParameterAggregatorFromTypeWithUnannotatedField() throws CoreException {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("ParameterAggregator.txt", "org.jboss.tools.ws.jaxrs.sample.services",
				"ParameterAggregator.java");
		final IType parameterAggregatorType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ParameterAggregator");
		assertThat(parameterAggregatorType, notNullValue());
		replaceAllOccurrencesOfCode(parameterAggregatorType, "// PLACEHOLDER",
				"public String id2;", false);
		// operation
		final List<IJaxrsElement> elements = JaxrsElementFactory.createElements(parameterAggregatorType,
				JdtUtils.parse(parameterAggregatorType, new NullProgressMonitor()), metamodel,
				new NullProgressMonitor());
		// verification: parameter aggregator with child method created
		assertThat(elements.size(), equalTo(0));
	}

	@Test
	public void shouldNotCreateParameterAggregatorFromTypeWithUnannotatedMethod() throws CoreException {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("ParameterAggregator.txt", "org.jboss.tools.ws.jaxrs.sample.services",
				"ParameterAggregator.java");
		final IType parameterAggregatorType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ParameterAggregator");
		assertThat(parameterAggregatorType, notNullValue());
		replaceAllOccurrencesOfCode(parameterAggregatorType, "// PLACEHOLDER",
				"public void setId2(String id2) {}", false);
		// operation
		final List<IJaxrsElement> elements = JaxrsElementFactory.createElements(parameterAggregatorType,
				JdtUtils.parse(parameterAggregatorType, new NullProgressMonitor()), metamodel,
				new NullProgressMonitor());
		// verification: parameter aggregator with child method created
		assertThat(elements.size(), equalTo(0));
	}
	
	
}
