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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.spy;

import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.jboss.tools.ws.jaxrs.core.AbstractCommonTestCase;
import org.jboss.tools.ws.jaxrs.core.WorkbenchUtils;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsResource;
import org.junit.Before;
import org.junit.Test;

public class JaxrsElementFactoryTestCase extends AbstractCommonTestCase {

	private final JaxrsElementFactory factory = new JaxrsElementFactory();

	private static final IProgressMonitor progressMonitor = new NullProgressMonitor();

	private JaxrsMetamodel metamodel;

	@Before
	public void setup() throws CoreException {
		metamodel = spy(JaxrsMetamodel.create(javaProject));
	}

	private IType getType(String typeName) throws CoreException {
		return JdtUtils.resolveType(typeName, javaProject, progressMonitor);
	}

	private IMethod getMethod(IType parentType, String methodName) throws JavaModelException {
		return WorkbenchUtils.getMethod(parentType, methodName);
	}

	private Annotation getAnnotation(final IMember member, final Class<?> annotationClass) throws JavaModelException {
		return JdtUtils.resolveAnnotation(member, JdtUtils.parse(member, progressMonitor), annotationClass);
	}

	@Test
	public void shouldCreateRootResourceFromPathAnnotation() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final Annotation annotation = getAnnotation(type, Path.class);
		// operation
		final JaxrsJavaElement<?> element = factory.createElement(annotation.getJavaAnnotation(),
				JdtUtils.parse(type, progressMonitor), metamodel);
		// verifications
		assertNotNull(element);
		final IJaxrsResource resource = (IJaxrsResource) element;
		// only @Path annotation is known by the metamodel, so pure resource
		// methods with @GET, etc. are not created here.
		assertThat(resource.getAllMethods().size(), equalTo(4));
	}

	@Test
	public void shouldCreateRootResourceFromType() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		// operation
		final JaxrsResource element = factory.createResource(type, JdtUtils.parse(type, progressMonitor), metamodel);
		// verifications
		assertThat(element.getAllMethods().size(), greaterThan(0));
	}

	@Test
	public void shouldCreateSubesourceFromType() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		// operation
		final JaxrsResource element = factory.createResource(type, JdtUtils.parse(type, progressMonitor), metamodel);
		// verifications
		assertThat(element.getAllMethods().size(), greaterThan(0));
	}

	@Test
	public void shouldCreateHttpMethodFromAnnotation() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final Annotation annotation = getAnnotation(type, HttpMethod.class);
		// operation
		final JaxrsJavaElement<?> element = factory.createElement(annotation.getJavaAnnotation(),
				JdtUtils.parse(type, progressMonitor), metamodel);
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
		final JaxrsResource resource = factory.createResource(type, JdtUtils.parse(type, progressMonitor), metamodel);
		// verifications
		assertThat(resource, nullValue());
	}

	@Test
	public void shouldCreateMethodInRootResourceFromAnnotation() throws CoreException {
		// pre-conditions
		final IType httpType = getType(GET.class.getName());
		final Annotation httpAnnotation = getAnnotation(httpType, HttpMethod.class);
		JaxrsHttpMethod httpMethod = new JaxrsHttpMethod(httpType, httpAnnotation, metamodel);
		metamodel.add(httpMethod);
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod method = getMethod(type, "getCustomerAsVCard");
		final Annotation annotation = getAnnotation(method, Path.class);
		// operation
		JaxrsResourceMethod element = factory.createResourceMethod(annotation,
				JdtUtils.parse(method, progressMonitor), metamodel);
		// verifications
		assertThat(element.getAnnotations().size(), equalTo(3));
		assertThat(element.getJavaMethodParameters().size(), equalTo(2));
	}

	@Test
	public void shouldCreateMethodInSubresourceFromAnnotation() throws CoreException {
		// pre-conditions
		final IType httpType = getType(GET.class.getName());
		final Annotation httpAnnotation = getAnnotation(httpType, HttpMethod.class);
		JaxrsHttpMethod httpMethod = new JaxrsHttpMethod(httpType, httpAnnotation, metamodel);
		metamodel.add(httpMethod);
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		final IMethod method = getMethod(type, "getProduct");
		final Annotation annotation = getAnnotation(method, Path.class);
		// operation
		JaxrsResourceMethod element = factory.createResourceMethod(annotation,
				JdtUtils.parse(method, progressMonitor), metamodel);
		// verifications
		assertThat(element.getAnnotations().size(), equalTo(3));
		assertThat(element.getJavaMethodParameters().size(), equalTo(1));
	}

	@Test
	public void shouldCreateFieldFromAnnotation() throws CoreException {
		// pre-conditions
		final IType httpType = getType(GET.class.getName());
		final Annotation httpAnnotation = getAnnotation(httpType, HttpMethod.class);
		JaxrsHttpMethod httpMethod = new JaxrsHttpMethod(httpType, httpAnnotation, metamodel);
		metamodel.add(httpMethod);
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		IField field = type.getField("foo");

		final Annotation annotation = getAnnotation(field, QueryParam.class);
		// operation
		JaxrsResourceField element = factory.createField(annotation, JdtUtils.parse(field, progressMonitor), metamodel);
		// verifications
		assertThat(element.getAnnotations().size(), equalTo(2));
		assertThat(element.getPathParamAnnotation(), nullValue());
		assertThat(element.getQueryParamAnnotation().getValue("value"), equalTo("foo"));
		assertThat(element.getDefaultValueAnnotation().getValue("value"), equalTo("foo!"));
	}

}
