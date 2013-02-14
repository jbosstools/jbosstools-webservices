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
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.nullValue;
import static org.jboss.tools.ws.jaxrs.core.WorkbenchUtils.replaceFirstOccurrenceOfCode;
import static org.jboss.tools.ws.jaxrs.core.WorkbenchUtils.resolveAnnotation;
import static org.jboss.tools.ws.jaxrs.core.WorkbenchUtils.resolveAnnotations;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.APPLICATION_PATH;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.GET;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.HTTP_METHOD;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.PATH;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.QUERY_PARAM;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.spy;

import java.util.Map;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.AbstractCommonTestCase;
import org.jboss.tools.ws.jaxrs.core.WorkbenchUtils;
import org.jboss.tools.ws.jaxrs.core.internal.utils.WtpUtils;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsApplication;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsResource;
import org.junit.Before;
import org.junit.Test;

public class JaxrsElementFactoryTestCase extends AbstractCommonTestCase {

	private static final IProgressMonitor progressMonitor = new NullProgressMonitor();

	private JaxrsMetamodel metamodel;

	@Before
	public void setup() throws CoreException {
		metamodel = spy(JaxrsMetamodel.create(javaProject));
	}

	@Test
	public void shouldCreateRootResourceFromPathAnnotation() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final Annotation annotation = resolveAnnotation(type, PATH.qualifiedName);
		// operation
		final JaxrsJavaElement<?> element = JaxrsElementFactory.createElement(annotation.getJavaAnnotation(),
				JdtUtils.parse(type, progressMonitor), metamodel, new NullProgressMonitor());
		// verifications
		assertNotNull(element);
		final IJaxrsResource resource = (IJaxrsResource) element;
		// result contains a mix of resource methods and subresource methods since http methods are built-in the metamodel
		assertThat(resource.getAllMethods().size(), equalTo(6));
	}

	@Test
	public void shouldCreateRootResourceFromType() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		// operation
		final JaxrsResource element = JaxrsElementFactory.createResource(type, JdtUtils.parse(type, progressMonitor), metamodel);
		// verifications
		assertThat(element.getAllMethods().size(), greaterThan(0));
	}

	@Test
	public void shouldCreateSubresourceFromType() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		// operation
		final JaxrsResource element = JaxrsElementFactory.createResource(type, JdtUtils.parse(type, progressMonitor), metamodel);
		// verifications
		assertThat(element.getAllMethods().size(), greaterThan(0));
	}

	@Test
	public void shouldCreateHttpMethodFromAnnotation() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final Annotation annotation = resolveAnnotation(type, HTTP_METHOD.qualifiedName);
		// operation
		final JaxrsJavaElement<?> element = JaxrsElementFactory.createElement(annotation.getJavaAnnotation(),
				JdtUtils.parse(type, progressMonitor), metamodel, new NullProgressMonitor());
		// verifications
		assertNotNull(element);
		final IJaxrsHttpMethod httpMethod = (IJaxrsHttpMethod)element ;
		assertThat(httpMethod.getHttpVerb(), equalTo("FOO"));
	}

	@Test
	public void shouldNotCreateElementFromOtherType() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.domain.Customer");
		// operation
		final JaxrsResource resource = JaxrsElementFactory.createResource(type, JdtUtils.parse(type, progressMonitor), metamodel);
		// verifications
		assertThat(resource, nullValue());
	}

	@Test
	public void shouldCreateMethodInRootResourceFromAnnotation() throws CoreException {
		// pre-conditions
		final IType httpType = getType(GET.qualifiedName);
		final Map<String, Annotation> annotations = resolveAnnotations(httpType, HTTP_METHOD.qualifiedName);
		final JaxrsHttpMethod httpMethod = new JaxrsHttpMethod(httpType, annotations, metamodel);
		metamodel.add(httpMethod);
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod method = getMethod(type, "getCustomerAsVCard");
		final Annotation annotation = resolveAnnotation(method, PATH.qualifiedName);
		// operation
		JaxrsResourceMethod element = JaxrsElementFactory.createResourceMethod(annotation,
				JdtUtils.parse(method, progressMonitor), metamodel);
		// verifications
		assertThat(element.getAnnotations().size(), equalTo(3));
		assertThat(element.getJavaMethodParameters().size(), equalTo(2));
	}

	@Test
	public void shouldCreateMethodInSubresourceFromAnnotation() throws CoreException {
		// pre-conditions
		final IType httpType = getType(GET.qualifiedName);
		final Map<String, Annotation> annotations = resolveAnnotations(httpType, HTTP_METHOD.qualifiedName);
		final JaxrsHttpMethod httpMethod = new JaxrsHttpMethod(httpType, annotations, metamodel);
		metamodel.add(httpMethod);
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		final IMethod method = getMethod(type, "getProduct");
		final Annotation annotation = resolveAnnotation(method, PATH.qualifiedName);
		// operation
		JaxrsResourceMethod element = JaxrsElementFactory.createResourceMethod(annotation,
				JdtUtils.parse(method, progressMonitor), metamodel);
		// verifications
		assertThat(element.getAnnotations().size(), equalTo(3));
		assertThat(element.getJavaMethodParameters().size(), equalTo(1));
	}

	@Test
	public void shouldCreateFieldFromAnnotation() throws CoreException {
		// pre-conditions
		final IType httpType = getType(GET.qualifiedName);
		final Map<String, Annotation> annotations = resolveAnnotations(httpType, HTTP_METHOD.qualifiedName);
		final JaxrsHttpMethod httpMethod = new JaxrsHttpMethod(httpType, annotations, metamodel);
		metamodel.add(httpMethod);
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		IField field = type.getField("foo");
		final Annotation annotation = resolveAnnotation(field, QUERY_PARAM.qualifiedName);
		// operation
		JaxrsResourceField element = JaxrsElementFactory.createField(annotation, JdtUtils.parse(field, progressMonitor), metamodel);
		// verifications
		assertThat(element.getAnnotations().size(), equalTo(2));
		assertThat(element.getPathParamAnnotation(), nullValue());
		assertThat(element.getQueryParamAnnotation().getValue("value"), equalTo("foo"));
		assertThat(element.getDefaultValueAnnotation().getValue("value"), equalTo("foo!"));
	}
	
	@Test
	public void shouldCreateMessageBodyWriterProviderFromType() throws CoreException {
		// pre-conditions
		final IType providerType = getType("org.jboss.tools.ws.jaxrs.sample.services.providers.CustomerVCardMessageBodyWriter");
		// operation
		JaxrsProvider element = JaxrsElementFactory.createProvider(providerType, JdtUtils.parse(providerType, progressMonitor), metamodel, progressMonitor);
		// verifications
		assertNotNull(element);
		assertThat(element.getAnnotations().size(), equalTo(2));
		assertThat(element.getElementKind(), equalTo(EnumElementKind.MESSAGE_BODY_WRITER));
		assertNull(element.getProvidedType(EnumElementKind.MESSAGE_BODY_READER));
		assertThat(element.getProvidedType(EnumElementKind.MESSAGE_BODY_WRITER).getFullyQualifiedName(), equalTo("org.jboss.tools.ws.jaxrs.sample.domain.Customer"));
		assertNull(element.getProvidedType(EnumElementKind.EXCEPTION_MAPPER));
	}
	
	
	@Test
	public void shouldCreateEntityProviderFromType() throws CoreException {
		// pre-conditions
		final IType providerType = getType("org.jboss.tools.ws.jaxrs.sample.extra.DummyProvider");
		// operation
		JaxrsProvider element = JaxrsElementFactory.createProvider(providerType, JdtUtils.parse(providerType, progressMonitor), metamodel, progressMonitor);
		// verifications
		assertNotNull(element);
		assertThat(element.getAnnotations().size(), equalTo(3));
		assertThat(element.getElementKind(), equalTo(EnumElementKind.ENTITY_MAPPER));
		assertThat(element.getProvidedType(EnumElementKind.MESSAGE_BODY_READER).getFullyQualifiedName(), equalTo(String.class.getName()));
		assertThat(element.getProvidedType(EnumElementKind.MESSAGE_BODY_WRITER).getFullyQualifiedName(), equalTo(Number.class.getName()));
		assertNull(element.getProvidedType(EnumElementKind.EXCEPTION_MAPPER));
	}

	@Test
	public void shouldCreateExceptionMapperProviderFromType() throws CoreException {
		// pre-conditions
		final IType providerType = getType("org.jboss.tools.ws.jaxrs.sample.extra.TestQualifiedExceptionMapper");
		// operation
		JaxrsProvider element = JaxrsElementFactory.createProvider(providerType, JdtUtils.parse(providerType, progressMonitor), metamodel, progressMonitor);
		// verifications
		assertNotNull(element);
		assertThat(element.getAnnotations().size(), equalTo(1));
		assertThat(element.getElementKind(), equalTo(EnumElementKind.EXCEPTION_MAPPER));
		assertNull(element.getProvidedType(EnumElementKind.MESSAGE_BODY_READER));
		assertNull(element.getProvidedType(EnumElementKind.MESSAGE_BODY_WRITER));
		assertThat(element.getProvidedType(EnumElementKind.EXCEPTION_MAPPER).getFullyQualifiedName(), equalTo("org.jboss.tools.ws.jaxrs.sample.extra.TestQualifiedException$TestException"));
	}

	@Test
	public void shouldNotCreateProviderFromType() throws CoreException {
		// pre-conditions
		final IType providerType = getType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		// operation
		JaxrsProvider element = JaxrsElementFactory.createProvider(providerType, JdtUtils.parse(providerType, progressMonitor), metamodel, progressMonitor);
		// verifications
		assertNull(element);
	}
	
	@Test
	public void shouldCreateApplicationFromApplicationAnnotationAndApplicationSubclass() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final Annotation annotation = resolveAnnotation(type, APPLICATION_PATH.qualifiedName);
		// operation
		final JaxrsJavaElement<?> element = JaxrsElementFactory.createApplication(annotation,
				JdtUtils.parse(type, progressMonitor), metamodel);
		// verifications
		assertNotNull(element);
		final IJaxrsApplication application = (IJaxrsApplication) element;
		// result contains a mix of resource methods and subresource methods since http methods are built-in the metamodel
		assertThat(application.getApplicationPath(), equalTo("/app"));
	}

	@Test
	public void shouldCreateApplicationFromApplicationSubclassOnly() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final Annotation annotation = resolveAnnotation(type, APPLICATION_PATH.qualifiedName);
		WorkbenchUtils.delete(annotation.getJavaAnnotation(), false);
		// operation
		final JaxrsJavaElement<?> element = JaxrsElementFactory.createApplication(type,
				JdtUtils.parse(type, progressMonitor), metamodel);
		// verifications
		assertNotNull(element);
		final IJaxrsApplication application = (IJaxrsApplication) element;
		// result contains a mix of resource methods and subresource methods since http methods are built-in the metamodel
		assertNull(application.getApplicationPath());
	}
	
	@Test
	public void shouldCreateApplicationFromApplicationAnnotationOnly() throws CoreException {
		// pre-conditions
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.RestApplication", javaProject, "RestApplication extends Application", "RestApplication", false);
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final IType applicationType = resolveType(EnumJaxrsClassname.APPLICATION.qualifiedName);
		assertFalse(JdtUtils.isTypeOrSuperType(applicationType, type));
		final Annotation annotation = resolveAnnotation(type, APPLICATION_PATH.qualifiedName);
		// operation
		final JaxrsJavaElement<?> element = JaxrsElementFactory.createApplication(annotation,
				JdtUtils.parse(type, progressMonitor), metamodel);
		// verifications
		assertNotNull(element);
		final IJaxrsApplication application = (IJaxrsApplication) element;
		// result contains a mix of resource methods and subresource methods since http methods are built-in the metamodel
		assertThat(application.getApplicationPath(), equalTo("/app"));
	}
	
	@Test
	public void shouldCreateApplicationFromApplicationSubclassInWebxml() throws CoreException {
		// pre-conditions
		final IType appType = getType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		IFolder webInfFolder = WtpUtils.getWebInfFolder(javaProject.getProject());
		IResource webxmlResource = webInfFolder.findMember("web.xml");
		final JaxrsJavaApplication javaApplication = JaxrsElementFactory.createApplication(appType, JdtUtils.parse(appType, progressMonitor), metamodel);
		metamodel.add(javaApplication);
		// operation
		final JaxrsWebxmlApplication webxmlApplication = JaxrsElementFactory.createApplication(
				appType.getFullyQualifiedName(), "/foo", webxmlResource, metamodel);
		// verifications
		assertNotNull(webxmlApplication);
		assertThat(webxmlApplication.getApplicationPath(), equalTo("/foo"));
		assertThat(webxmlApplication.isOverride(), equalTo(true));
		assertThat(webxmlApplication.getOverridenJaxrsJavaApplication(), equalTo(javaApplication));
	}
	
	@Test
	public void shouldCreateApplicationFromApplicationClassInWebxml() throws CoreException {
		// pre-conditions
		IFolder webInfFolder = WtpUtils.getWebInfFolder(javaProject.getProject());
		IResource webxmlResource = webInfFolder.findMember("web.xml");
		// operation
		final JaxrsWebxmlApplication webxmlApplication = JaxrsElementFactory.createApplication(
				EnumJaxrsClassname.APPLICATION.qualifiedName, "/foo", webxmlResource, metamodel);
		// verifications
		assertNotNull(webxmlApplication);
		assertThat(webxmlApplication.getApplicationPath(), equalTo("/foo"));
		assertThat(webxmlApplication.isOverride(), equalTo(false));
		assertThat(webxmlApplication.getOverridenJaxrsJavaApplication(), equalTo(null));
	}
}
