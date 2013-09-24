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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.jboss.tools.ws.jaxrs.core.WorkbenchUtils.getAnnotation;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.APPLICATION_PATH;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.GET;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.HTTP_METHOD;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.PATH;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.PROVIDER;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.QUERY_PARAM;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.AbstractCommonTestCase;
import org.jboss.tools.ws.jaxrs.core.WorkbenchUtils;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementCategory;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsProvider;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResource;
import org.junit.Test;

public class JaxrsElementFactoryTestCase extends AbstractCommonTestCase {

	private static final IProgressMonitor progressMonitor = new NullProgressMonitor();

	@Test
	public void shouldCreateRootResourceFromPathAnnotation() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final Annotation annotation = getAnnotation(type, PATH.qualifiedName);
		// operation
		final List<IJaxrsElement> elements = JaxrsElementFactory.createElements(annotation.getJavaAnnotation(),
				JdtUtils.parse(type, progressMonitor), metamodel, new NullProgressMonitor());
		// verifications
		assertThat(elements.size(), equalTo(7));
		final IJaxrsResource resource = (IJaxrsResource) (elements.get(0));
		// result contains a mix of resource methods and subresource methods
		// since http methods are built-in the metamodel
		assertThat(resource.getAllMethods().size(), equalTo(6));
		assertThat(metamodel.getAllElements().size(), equalTo(13));
	}

	@Test
	public void shouldCreateRootResourceAndChildElementsFromType() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		// operation
		final List<IJaxrsElement> elements = JaxrsElementFactory.createElements(type,
				JdtUtils.parse(type, progressMonitor), metamodel, new NullProgressMonitor());
		// verifications
		assertThat(elements.size(), equalTo(7));
		final IJaxrsResource resource = (IJaxrsResource) (elements.get(0));
		// result contains a mix of resource methods and subresource methods
		// since http methods are built-in the metamodel
		assertThat(resource.getAllMethods().size(), equalTo(6));
		assertThat(metamodel.getAllElements().size(), equalTo(13));
	}

	@Test
	public void shouldCreateSubresourceWithChildElementsFromType() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		// operation
		final List<IJaxrsElement> elements = JaxrsElementFactory.createElements(type,
				JdtUtils.parse(type, progressMonitor), metamodel, new NullProgressMonitor());
		// verifications
		assertThat(elements.size(), equalTo(4));
		final IJaxrsResource resource = (IJaxrsResource) (elements.get(0));
		// result contains a mix of resource methods and subresource methods
		// since http methods are built-in the metamodel
		assertThat(resource.getAllMethods().size(), equalTo(3));
		assertThat(metamodel.getAllElements().size(), equalTo(10));
	}

	@Test
	public void shouldCreateHttpMethodFromAnnotation() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final Annotation annotation = getAnnotation(type, HTTP_METHOD.qualifiedName);
		// operation
		final List<IJaxrsElement> elements = JaxrsElementFactory.createElements(annotation.getJavaAnnotation(),
				JdtUtils.parse(type, progressMonitor), metamodel, new NullProgressMonitor());
		// verifications
		assertThat(elements.size(), equalTo(1));
		final IJaxrsHttpMethod httpMethod = (IJaxrsHttpMethod) (elements.get(0));
		assertThat(httpMethod.getHttpVerb(), equalTo("FOO"));
	}

	@Test
	public void shouldCreateHttpMethodFromType() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		// operation
		final List<IJaxrsElement> elements = JaxrsElementFactory.createElements(type,
				JdtUtils.parse(type, progressMonitor), metamodel, new NullProgressMonitor());
		// verifications
		assertThat(elements.size(), equalTo(1));
		final IJaxrsHttpMethod httpMethod = (IJaxrsHttpMethod) (elements.get(0));
		assertThat(httpMethod.getHttpVerb(), equalTo("FOO"));
	}

	@Test
	public void shouldNotCreateHttpMethodFromType() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomCDIQualifier");
		// operation
		final List<IJaxrsElement> elements = JaxrsElementFactory.createElements(type,
				JdtUtils.parse(type, progressMonitor), metamodel, new NullProgressMonitor());
		// verifications
		assertThat(elements.size(), equalTo(0));
	}

	@Test
	public void shouldNotCreateElementFromOtherType() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.domain.Customer");
		// operation
		final List<IJaxrsElement> resource = JaxrsElementFactory.createElements(type,
				JdtUtils.parse(type, progressMonitor), metamodel, new NullProgressMonitor());
		// verifications
		assertThat(resource, notNullValue());
		assertThat(resource.size(), equalTo(0));
	}

	@Test
	public void shouldCreateRootResourceAndResourceMethodFromJavaMethod() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.PurchaseOrderResource");
		final IMethod javaMethod = getJavaMethod(type, "getOrder");
		// operation
		final List<IJaxrsElement> elements = JaxrsElementFactory.createElements(javaMethod,
				JdtUtils.parse(type, progressMonitor), metamodel, new NullProgressMonitor());
		// verifications
		assertThat(elements.size(), equalTo(2));
		assertThat(elements.get(0).getElementKind().getCategory(), equalTo(EnumElementCategory.RESOURCE));
		assertThat(elements.get(1).getElementKind().getCategory(), equalTo(EnumElementCategory.RESOURCE_METHOD));
		final JaxrsResourceMethod element = (JaxrsResourceMethod) elements.get(1);
		assertThat(element.getAnnotations().size(), equalTo(3));
		assertThat(element.getAnnotations().size(), equalTo(3));
		assertThat(element.getJavaMethodParameters().size(), equalTo(2));
		assertThat(element.getParentResource(), notNullValue());
	}

	@Test
	public void shouldCreateSubesourceAndResourceMethodsFromJavaMethod() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		final IMethod javaMethod = getJavaMethod(type, "getProduct");
		// operation
		final List<IJaxrsElement> elements = JaxrsElementFactory.createElements(javaMethod,
				JdtUtils.parse(type, progressMonitor), metamodel, new NullProgressMonitor());
		// verifications
		assertThat(elements.size(), equalTo(4));
		assertThat(elements.get(0).getElementKind().getCategory(), equalTo(EnumElementCategory.RESOURCE));
		assertThat(elements.get(1).getElementKind().getCategory(), equalTo(EnumElementCategory.RESOURCE_METHOD));
		assertThat(elements.get(2).getElementKind().getCategory(), equalTo(EnumElementCategory.RESOURCE_METHOD));
		assertThat(elements.get(3).getElementKind().getCategory(), equalTo(EnumElementCategory.RESOURCE_METHOD));
		final JaxrsResourceMethod element = (JaxrsResourceMethod) elements.get(1);
		assertThat(element.getAnnotations().size(), equalTo(3));
		assertThat(element.getAnnotations().size(), equalTo(3));
		assertThat(element.getJavaMethodParameters().size(), equalTo(2));
		assertThat(element.getParentResource(), notNullValue());

	}

	@Test
	public void shouldCreateResourceLocatorAndMethodAndFieldsFromAnnotation() throws CoreException {
		// pre-conditions
		final IType httpType = getType(GET.qualifiedName);
		JaxrsHttpMethod.from(httpType).withMetamodel(metamodel).build();
		// metamodel.add(httpMethod);
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		IField field = type.getField("foo");
		final Annotation annotation = getAnnotation(field, QUERY_PARAM.qualifiedName);
		// operation
		final List<IJaxrsElement> elements = JaxrsElementFactory.createElements(annotation.getJavaAnnotation(),
				JdtUtils.parse(type, progressMonitor), metamodel, new NullProgressMonitor());
		// verifications
		assertThat(elements.size(), equalTo(5));
		assertThat(elements.get(0).getElementKind().getCategory(), equalTo(EnumElementCategory.RESOURCE));
		assertThat(elements.get(1).getElementKind().getCategory(), equalTo(EnumElementCategory.RESOURCE_METHOD));
		assertThat(elements.get(2).getElementKind().getCategory(), equalTo(EnumElementCategory.RESOURCE_FIELD));
		assertThat(elements.get(3).getElementKind().getCategory(), equalTo(EnumElementCategory.RESOURCE_FIELD));
		assertThat(elements.get(4).getElementKind().getCategory(), equalTo(EnumElementCategory.RESOURCE_FIELD));
		final JaxrsResourceField element = ((JaxrsResource) elements.get(0)).getField("foo");
		assertThat(element.getAnnotations().size(), equalTo(2));
		assertThat(element.getPathParamAnnotation(), nullValue());
		assertThat(element.getQueryParamAnnotation().getValue("value"), equalTo("foo"));
		assertThat(element.getDefaultValueAnnotation().getValue("value"), equalTo("foo!"));
	}

	@Test
	public void shouldCreateApplicationFromApplicationAnnotationAndApplicationSubclass() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final Annotation annotation = getAnnotation(type, APPLICATION_PATH.qualifiedName);
		// operation
		final List<IJaxrsElement> elements = JaxrsElementFactory.createElements(annotation.getJavaAnnotation(),
				JdtUtils.parse(type, progressMonitor), metamodel, new NullProgressMonitor());
		// verifications
		assertThat(elements.size(), equalTo(1));
		final JaxrsJavaApplication element = (JaxrsJavaApplication) elements.get(0);
		// result contains a mix of resource methods and subresource methods
		// since http methods are built-in the metamodel
		assertThat(element.getApplicationPath(), equalTo("/app"));
	}

	@Test
	public void shouldCreateApplicationFromApplicationSubclassOnly() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final Annotation annotation = getAnnotation(type, APPLICATION_PATH.qualifiedName);
		WorkbenchUtils.delete(annotation.getJavaAnnotation(), false);
		// operation
		final List<IJaxrsElement> elements = JaxrsElementFactory.createElements(annotation.getJavaAnnotation(),
				JdtUtils.parse(type, progressMonitor), metamodel, new NullProgressMonitor());
		// verifications
		assertThat(elements.size(), equalTo(1));
		final JaxrsJavaApplication element = (JaxrsJavaApplication) elements.get(0);
		// result contains a mix of resource methods and subresource methods
		// since http methods are built-in the metamodel
		assertThat(element.getApplicationPath(), nullValue());
	}

	@Test
	public void shouldCreateApplicationFromApplicationAnnotationOnly() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		WorkbenchUtils.removeFirstOccurrenceOfCode(type, "extends Application", false);
		final IType applicationType = resolveType(EnumJaxrsClassname.APPLICATION.qualifiedName);
		assertFalse(JdtUtils.isTypeOrSuperType(applicationType, type));
		final Annotation annotation = getAnnotation(type, APPLICATION_PATH.qualifiedName);
		// operation
		final List<IJaxrsElement> elements = JaxrsElementFactory.createElements(annotation.getJavaAnnotation(),
				JdtUtils.parse(type, progressMonitor), metamodel, new NullProgressMonitor());
		// verifications
		assertThat(elements.size(), equalTo(1));
		final JaxrsJavaApplication element = (JaxrsJavaApplication) elements.get(0);
		// result contains a mix of resource methods and subresource methods
		// since http methods are built-in the metamodel
		assertThat(element.getApplicationPath(), equalTo("/app"));
	}

	@Test
	public void shouldCreateApplicationFromApplicationSubclassInWebxml() throws CoreException, IOException {
		// pre-conditions
		final IType appType = getType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final JaxrsJavaApplication javaApplication = JaxrsJavaApplication.from(appType).withMetamodel(metamodel)
				.build();
		// operation
		final JaxrsWebxmlApplication webxmlApplication = createWebxmlApplication(appType.getFullyQualifiedName(),
				"/foo");
		// verifications
		assertNotNull(webxmlApplication);
		assertThat(webxmlApplication.getApplicationPath(), equalTo("/foo"));
		assertThat(webxmlApplication.isOverride(), equalTo(true));
		assertThat(webxmlApplication.getOverridenJaxrsJavaApplication(), equalTo(javaApplication));
	}

	@Test
	public void shouldCreateApplicationFromApplicationClassInWebxml() throws CoreException, IOException {
		// pre-conditions
		// operation
		final JaxrsWebxmlApplication webxmlApplication = createWebxmlApplication(
				EnumJaxrsClassname.APPLICATION.qualifiedName, "/foo");
		
		// verifications
		assertNotNull(webxmlApplication);
		assertThat(webxmlApplication.getApplicationPath(), equalTo("/foo"));
		assertThat(webxmlApplication.isOverride(), equalTo(false));
		assertThat(webxmlApplication.getOverridenJaxrsJavaApplication(), equalTo(null));
	}

	@Test
	public void shouldCreateProviderFromAnnotation() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final Annotation annotation = getAnnotation(type, PROVIDER.qualifiedName);
		// operation
		final List<IJaxrsElement> elements = JaxrsElementFactory.createElements(annotation.getJavaAnnotation(),
				JdtUtils.parse(type, progressMonitor), metamodel, new NullProgressMonitor());
		// verifications
		assertThat(elements.size(), equalTo(1));
		final IJaxrsProvider provider = (IJaxrsProvider) (elements.get(0));
		assertThat(provider.getConsumedMediaTypes().size(), equalTo(1));
		assertThat(provider.getConsumedMediaTypes().get(0), equalTo("application/json"));
		assertThat(provider.getProducedMediaTypes().size(), equalTo(1));
		assertThat(provider.getProducedMediaTypes().get(0), equalTo("application/json"));
		assertThat(provider.getProvidedType(EnumElementKind.EXCEPTION_MAPPER).getFullyQualifiedName(),
				equalTo("javax.persistence.EntityNotFoundException"));
	}

	@Test
	public void shouldCreateProviderFromType() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		// operation
		final List<IJaxrsElement> elements = JaxrsElementFactory.createElements(type,
				JdtUtils.parse(type, progressMonitor), metamodel, new NullProgressMonitor());
		// verifications
		assertThat(elements.size(), equalTo(1));
		final IJaxrsProvider provider = (IJaxrsProvider) (elements.get(0));
		assertThat(provider.getConsumedMediaTypes().size(), equalTo(1));
		assertThat(provider.getConsumedMediaTypes().get(0), equalTo("application/json"));
		assertThat(provider.getProducedMediaTypes().size(), equalTo(1));
		assertThat(provider.getProducedMediaTypes().get(0), equalTo("application/json"));
		assertThat(provider.getProvidedType(EnumElementKind.EXCEPTION_MAPPER).getFullyQualifiedName(),
				equalTo("javax.persistence.EntityNotFoundException"));
	}

	@Test
	public void shouldCreateProviderWithoutHierarchyFromType() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		// operation
		WorkbenchUtils.removeFirstOccurrenceOfCode(type, "implements ExceptionMapper<EntityNotFoundException>", false);
		final List<IJaxrsElement> elements = JaxrsElementFactory.createElements(type,
				JdtUtils.parse(type, progressMonitor), metamodel, new NullProgressMonitor());
		// verifications
		assertThat(elements.size(), equalTo(1));
		final JaxrsProvider provider = (JaxrsProvider) (elements.get(0));
		assertThat(provider.getAnnotation(PROVIDER.qualifiedName), notNullValue());
		assertThat(provider.getConsumedMediaTypes().size(), equalTo(1));
		assertThat(provider.getConsumedMediaTypes().get(0), equalTo("application/json"));
		assertThat(provider.getProducedMediaTypes().size(), equalTo(1));
		assertThat(provider.getProducedMediaTypes().get(0), equalTo("application/json"));
		assertThat(provider.getProvidedType(EnumElementKind.EXCEPTION_MAPPER), nullValue());
	}

	@Test
	public void shouldCreateProviderWithoutAnnotationFromType() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		WorkbenchUtils.removeFirstOccurrenceOfCode(type, "@Provider", false);
		// operation
		final List<IJaxrsElement> elements = JaxrsElementFactory.createElements(type,
				JdtUtils.parse(type, progressMonitor), metamodel, new NullProgressMonitor());
		// verifications
		assertThat(elements.size(), equalTo(1));
		final JaxrsProvider provider = (JaxrsProvider) (elements.get(0));
		assertThat(provider.getAnnotation(PROVIDER.qualifiedName), nullValue());
		assertThat(provider.getConsumedMediaTypes().size(), equalTo(1));
		assertThat(provider.getConsumedMediaTypes().get(0), equalTo("application/json"));
		assertThat(provider.getProducedMediaTypes().size(), equalTo(1));
		assertThat(provider.getProducedMediaTypes().get(0), equalTo("application/json"));
		assertThat(provider.getProvidedType(EnumElementKind.EXCEPTION_MAPPER).getFullyQualifiedName(),
				equalTo("javax.persistence.EntityNotFoundException"));
	}

	@Test
	public void shouldCreateMessageBodyWriterProviderFromType() throws CoreException {
		// pre-conditions
		final IType providerType = getType("org.jboss.tools.ws.jaxrs.sample.services.providers.CustomerVCardMessageBodyWriter");
		// operation
		final List<IJaxrsElement> elements = JaxrsElementFactory.createElements(providerType,
				JdtUtils.parse(providerType, progressMonitor), metamodel, new NullProgressMonitor());
		// verifications
		assertThat(elements.size(), equalTo(1));
		final JaxrsProvider provider = (JaxrsProvider) (elements.get(0));
		assertThat(provider.getAnnotations().size(), equalTo(2));
		assertThat(provider.getElementKind(), equalTo(EnumElementKind.MESSAGE_BODY_WRITER));
		assertThat(provider.getProvidedType(EnumElementKind.MESSAGE_BODY_READER), nullValue());
		assertThat(provider.getProvidedType(EnumElementKind.MESSAGE_BODY_WRITER).getFullyQualifiedName(),
				equalTo("org.jboss.tools.ws.jaxrs.sample.domain.Customer"));
		assertThat(provider.getProvidedType(EnumElementKind.EXCEPTION_MAPPER), nullValue());
	}

	@Test
	public void shouldCreateEntityProviderFromType() throws CoreException {
		// pre-conditions
		final IType providerType = getType("org.jboss.tools.ws.jaxrs.sample.extra.DummyProvider");
		// operation
		final List<IJaxrsElement> elements = JaxrsElementFactory.createElements(providerType,
				JdtUtils.parse(providerType, progressMonitor), metamodel, new NullProgressMonitor());
		// verifications
		assertThat(elements.size(), equalTo(1));
		final JaxrsProvider provider = (JaxrsProvider) (elements.get(0));
		assertThat(provider.getAnnotations().size(), equalTo(3));
		assertThat(provider.getElementKind(), equalTo(EnumElementKind.ENTITY_MAPPER));
		assertThat(provider.getProvidedType(EnumElementKind.MESSAGE_BODY_READER).getFullyQualifiedName(),
				equalTo(String.class.getName()));
		assertThat(provider.getProvidedType(EnumElementKind.MESSAGE_BODY_WRITER).getFullyQualifiedName(),
				equalTo(Number.class.getName()));
		assertNull(provider.getProvidedType(EnumElementKind.EXCEPTION_MAPPER));
	}

	@Test
	public void shouldCreateExceptionMapperProviderFromType() throws CoreException {
		// pre-conditions
		final IType providerType = getType("org.jboss.tools.ws.jaxrs.sample.extra.TestQualifiedExceptionMapper");
		// operation
		final List<IJaxrsElement> elements = JaxrsElementFactory.createElements(providerType,
				JdtUtils.parse(providerType, progressMonitor), metamodel, new NullProgressMonitor());
		// verifications
		assertThat(elements.size(), equalTo(1));
		final JaxrsProvider provider = (JaxrsProvider) (elements.get(0));
		assertThat(provider.getAnnotations().size(), equalTo(1));
		assertThat(provider.getElementKind(), equalTo(EnumElementKind.EXCEPTION_MAPPER));
		assertNull(provider.getProvidedType(EnumElementKind.MESSAGE_BODY_READER));
		assertNull(provider.getProvidedType(EnumElementKind.MESSAGE_BODY_WRITER));
		assertThat(provider.getProvidedType(EnumElementKind.EXCEPTION_MAPPER).getFullyQualifiedName(),
				equalTo("org.jboss.tools.ws.jaxrs.sample.extra.TestQualifiedException$TestException"));
	}

	@Test
	public void shouldNotCreateProviderFromOtherType() throws CoreException {
		// pre-conditions
		final IType providerType = getType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		// operation
		final JaxrsProvider provider = JaxrsProvider.from(providerType).build();
		// verifications
		assertNull(provider);
	}

}
