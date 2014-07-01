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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils.replaceFirstOccurrenceOfCode;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_MATRIX_PARAM_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_PATH_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_QUERY_PARAM_ANNOTATION;
import static org.junit.Assert.assertThat;

import java.util.Collection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.Flags;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils;
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsEndpoint;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author xcoulon
 * 
 */
public class JaxrsEndpointTestCase {

	private static final boolean PRIMARY_COPY = false;

	private static final boolean WORKING_COPY = true;

	@ClassRule
	public static WorkspaceSetupRule workspaceSetupRule = new WorkspaceSetupRule(
			"org.jboss.tools.ws.jaxrs.tests.sampleproject");

	@Rule
	public JaxrsMetamodelMonitor metamodelMonitor = new JaxrsMetamodelMonitor(
			"org.jboss.tools.ws.jaxrs.tests.sampleproject", true);

	private JaxrsMetamodel metamodel = null;

	@Before
	public void setup() throws CoreException {
		metamodel = metamodelMonitor.getMetamodel();
	}

	private JaxrsResourceMethod getModifiedResourceMethod(final boolean useWorkingCopy) throws CoreException {
		final String typeName = "org.jboss.tools.ws.jaxrs.sample.services.CustomerResource";
		final IType resourceType = metamodelMonitor.resolveType(typeName);
		final IMethod method = metamodelMonitor.resolveMethod(resourceType, "getCustomer");
		final String oldContent = "@PathParam(\"id\") Integer id, @Context UriInfo uriInfo";
		final String newContent = "@PathParam(\"id\") Integer id, @QueryParam(\"queryParam1\") Integer queryParam1, @QueryParam(\"queryParam2\") String queryParam2, @MatrixParam(\"matrixParam1\") Integer matrixParam1, @MatrixParam(\"matrixParam2\") List<String> matrixParam2";
		replaceFirstOccurrenceOfCode(method, oldContent, newContent, useWorkingCopy);
		final JaxrsResource resource = metamodel.findResource(resourceType);
		return resource.getMethods().get(metamodelMonitor.resolveMethod(resourceType, "getCustomer").getHandleIdentifier());
	}

	private IMethod modifyJavaMethodSignature(final JaxrsResourceMethod resourceMethod, final boolean useWorkingCopy)
			throws CoreException {
		final IMethod javaMethod = resourceMethod.getJavaElement();
		final String oldAnnotationContent = "@Path(\"{id}\")";
		final String newAnnotationContent = "@Path(\"{foo:\\\\d+}/{bar:[a-z]+}\")";
		replaceFirstOccurrenceOfCode(javaMethod, oldAnnotationContent, newAnnotationContent, useWorkingCopy);
		final String oldArgsContent = "@PathParam(\"id\") Integer id, @QueryParam(\"queryParam1\") Integer queryParam1, @QueryParam(\"queryParam2\") String queryParam2, @MatrixParam(\"matrixParam1\") Integer matrixParam1, @MatrixParam(\"matrixParam2\") List<String> matrixParam2";
		final String newArgsContent = "@PathParam(\"foo\") int ident, @PathParam(\"bar\") Char bar, @QueryParam(\"queryParam1\") long queryParam1, @MatrixParam(\"matrixParam1\") short matrixParam1, @MatrixParam(\"matrixParam2\") List<String> matrixParam2, @MatrixParam(\"matrixParam3\") String matrixParam3";
		return replaceFirstOccurrenceOfCode(javaMethod, oldArgsContent, newArgsContent, useWorkingCopy);
	}
	
	@Test
	public void shouldDisplayEndpointParametersInOrderAtCreationInWorkingCopy() throws CoreException {
		// pre-conditions
		final JaxrsResourceMethod resourceMethod = getModifiedResourceMethod(WORKING_COPY);
		final IJaxrsEndpoint endpoint = metamodel.findEndpoints(resourceMethod).iterator().next();
		// operation
		final String uriPathTemplate = endpoint.getUriPathTemplate();
		// verifications
		assertThat(
				uriPathTemplate,
				equalTo("/hello/customers/{id:Integer};matrixParam1={Integer};matrixParam2={List<String>}?queryParam1={Integer}&queryParam2={String}"));
	}

	@Test
	public void shouldDisplayEndpointParametersInOrderAtCreationInPrimaryCopy() throws CoreException {
		// pre-conditions
		final JaxrsResourceMethod resourceMethod = getModifiedResourceMethod(PRIMARY_COPY);
		final IJaxrsEndpoint endpoint = metamodel.findEndpoints(resourceMethod).iterator().next();
		// operation
		final String uriPathTemplate = endpoint.getUriPathTemplate();
		// verifications
		assertThat(
				uriPathTemplate,
				equalTo("/hello/customers/{id:Integer};matrixParam1={Integer};matrixParam2={List<String>}?queryParam1={Integer}&queryParam2={String}"));
	}
	
	@Test
	public void shouldDisplayEndpointParametersInOrderAfterHttpMethodUpdateInPrimaryCopy() throws CoreException {
		// pre-conditions
		final JaxrsResourceMethod resourceMethod = getModifiedResourceMethod(PRIMARY_COPY);
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(resourceMethod).iterator().next();
		// operation
		replaceFirstOccurrenceOfCode(resourceMethod.getJavaElement(), "@GET", "@POST", false);
		// verifications
		assertThat(endpoint.getHttpMethod().getHttpVerb(), equalTo("POST"));
	}

	@Test
	public void shouldDisplayEndpointParametersInOrderAfterHttpMethodUpdateInWorkingCopy() throws CoreException {
		// pre-conditions
		final JaxrsResourceMethod resourceMethod = getModifiedResourceMethod(WORKING_COPY);
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(resourceMethod).iterator().next();
		// operation
		replaceFirstOccurrenceOfCode(resourceMethod.getJavaElement(), "@GET", "@POST", false);
		// verifications
		assertThat(endpoint.getHttpMethod().getHttpVerb(), equalTo("POST"));
	}

	@Test
	public void shouldDisplayEndpointParametersInOrderAfterMethodParametersUpdateInWorkingCopy() throws CoreException {
		// pre-conditions
		final JaxrsResourceMethod resourceMethod = getModifiedResourceMethod(WORKING_COPY);
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(resourceMethod).iterator().next();
		// operation
		final IMethod modifiedMethod  = modifyJavaMethodSignature(resourceMethod, WORKING_COPY);
		resourceMethod.update(modifiedMethod, JdtUtils.parse(modifiedMethod, null));
		endpoint.update(new Flags(F_PATH_ANNOTATION + F_QUERY_PARAM_ANNOTATION + F_MATRIX_PARAM_ANNOTATION));
		final String uriPathTemplate = endpoint.getUriPathTemplate();
		// verifications
		assertThat(
				uriPathTemplate,
				equalTo("/hello/customers/{foo:\\d+}/{bar:[a-z]+};matrixParam1={short};matrixParam2={List<String>};matrixParam3={String}?queryParam1={long}"));
	}

	@Test
	public void shouldDisplayEndpointParametersInOrderAfterMethodParametersUpdateInPrimaryCopy() throws CoreException {
		// pre-conditions
		final JaxrsResourceMethod resourceMethod = getModifiedResourceMethod(PRIMARY_COPY);
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(resourceMethod).iterator().next();
		// operation
		final IMethod modifiedMethod  = modifyJavaMethodSignature(resourceMethod, PRIMARY_COPY);
		resourceMethod.update(modifiedMethod, JdtUtils.parse(modifiedMethod, null));
		endpoint.update(new Flags(F_PATH_ANNOTATION + F_QUERY_PARAM_ANNOTATION + F_MATRIX_PARAM_ANNOTATION));
		// verifications
		final String uriPathTemplate = endpoint.getUriPathTemplate();
		assertThat(
				uriPathTemplate,
				equalTo("/hello/customers/{foo:\\d+}/{bar:[a-z]+};matrixParam1={short};matrixParam2={List<String>};matrixParam3={String}?queryParam1={long}"));
	}

	@Test
	public void shouldDisplayEndpointWithTypePathBoundOnField() throws CoreException {
		// pre-conditions
		final String typeName = "org.jboss.tools.ws.jaxrs.sample.services.BookResource";
		final IType resourceType = metamodelMonitor.resolveType(typeName);
		final IMethod method = metamodelMonitor.resolveMethod(resourceType, "getPicture");
		final IJaxrsElement resourceMethod = (IJaxrsElement) metamodel.findElement(method);
		// operation
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(resourceMethod).iterator().next();
		final String uriPathTemplate = endpoint.getUriPathTemplate();
		// verifications
		assertThat(uriPathTemplate, equalTo("/hello/products/{productType:String};bar={String};qux2={String:\"qux2!\"}/{id:Integer};color={String}?foo={String:\"foo!\"}&qux1={String:\"qux1!\"}"));
	}

	@Test
	// see JBIDE-16476
	public void shouldDisplayEndpointWithMatrixParamsOnly() throws CoreException {
		// pre-conditions
		final String typeName = "org.jboss.tools.ws.jaxrs.sample.services.PurchaseOrderResource";
		final IType resourceType = metamodelMonitor.resolveType(typeName);
		ResourcesUtils.replaceFirstOccurrenceOfCode(resourceType, "//PLACEHOLDER", "@GET public void getAll(@MatrixParam(\"author\") java.lang.Long param1, @MatrixParam(\"country\") java.lang.Integer param2) {}", PRIMARY_COPY);
		final IMethod method = metamodelMonitor.resolveMethod(resourceType, "getAll");
		final IJaxrsElement resourceMethod = (IJaxrsElement) metamodel.findElement(method);
		// operation
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(resourceMethod).iterator().next();
		final String uriPathTemplate = endpoint.getUriPathTemplate();
		// verifications
		assertThat(uriPathTemplate, equalTo("/hello/orders;author={Long};country={Integer}"));
	}
	
	@Test
	public void shouldDisplayEndpointWithTypePathUnboundOnMethodParam() throws CoreException {
		// pre-conditions
		final String typeName = "org.jboss.tools.ws.jaxrs.sample.services.BarResource";
		final IType resourceType = metamodelMonitor.resolveType(typeName);
		final IMethod method = metamodelMonitor.resolveMethod(resourceType, "getContent2");
		final IJaxrsElement resourceMethod = (IJaxrsElement) metamodel.findElement(method);
		// operation
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(resourceMethod).iterator().next();
		final String uriPathTemplate = endpoint.getUriPathTemplate();
		// verifications
		assertThat(uriPathTemplate, equalTo("/hello/foo/bar/{param1:.*}/user/{id:.*}/{format:(/format/[^/]+?)?}/{encoding:(/encoding/[^/]+?)?}?start={int}"));
	}

	@Test
	public void shouldDisplayEndpointWithTypePathBoundOnMethodParam() throws CoreException {
		// pre-conditions
		final String typeName = "org.jboss.tools.ws.jaxrs.sample.services.BarResource";
		final IType resourceType = metamodelMonitor.resolveType(typeName);
		final IMethod method = metamodelMonitor.resolveMethod(resourceType, "update3");
		final IJaxrsElement resourceMethod = (IJaxrsElement) metamodel.findElement(method);
		// operation
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(resourceMethod).iterator().next();
		final String uriPathTemplate = endpoint.getUriPathTemplate();
		// verifications
		assertThat(uriPathTemplate, equalTo("/hello/foo/bar/{param1:String}/{param2:String}"));
	}

	@Test
	public void shouldAddQueryParamInAllEndpointsWhenAddingAnnotatedFieldInRootResource() throws CoreException {
		// pre-condition
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		// operation
		JavaElementsUtils.createField(resource.getJavaElement(), "@QueryParam(\"start\") private int start;", WORKING_COPY);
		// verifications
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(resource);
		assertThat(endpoints.size(), equalTo(6));
		for(JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("start={int}"));
			assertThat(endpoint.getUriPathTemplate(), not(containsString("start={int}&start={int}")));
		}
	}

	@Test
	public void shouldAddQueryParamInAllEndpointsWhenAddingAnnotationOnFieldInRootResource() throws CoreException {
		// pre-condition
		final IType resourceType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		ResourcesUtils.replaceFirstOccurrenceOfCode(resourceType, "//PlaceHolder", "private int start; //PlaceHolder", PRIMARY_COPY);
		final IField queryParamField = resourceType.getField("start");
		final JaxrsResource resource = metamodelMonitor.createResource(resourceType);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		JavaElementsUtils.addFieldAnnotation(queryParamField, "@QueryParam(\"start\")", WORKING_COPY);
		final IType modifiedType = (IType) queryParamField.getAncestor(IJavaElement.TYPE);
		resource.update(modifiedType, JdtUtils.parse(modifiedType, null));
		// verifications
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(resource);
		assertThat(endpoints.size(), equalTo(6));
		for(JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("start={int}"));
			assertThat(endpoint.getUriPathTemplate(), not(containsString("start={int}&start={int}")));
		}
	}

	@Test
	public void shouldAddQueryParamInAllEndpointsWhenAddingAnnotatedFieldInSubresource() throws CoreException {
		// pre-condition
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		// operation
		JavaElementsUtils.createField(resource.getJavaElement(), "@QueryParam(\"start\") private int start;", WORKING_COPY);
		// verifications
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(resource);
		assertThat(endpoints.size(), equalTo(3));
		for(JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("start={int}"));
		}
	}

	@Test
	public void shouldAddQueryParamInAllEndpointsWhenAddingAnnotationOnFieldInSubresource() throws CoreException {
		// pre-condition
		final IType resourceType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		ResourcesUtils.replaceFirstOccurrenceOfCode(resourceType, "//PlaceHolder", "private int start; //PlaceHolder", PRIMARY_COPY);
		final IField queryParamField = resourceType.getField("start");
		final JaxrsResource resource = metamodelMonitor.createResource(resourceType);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		JavaElementsUtils.addFieldAnnotation(queryParamField, "@QueryParam(\"start\")", WORKING_COPY);
		final IType modifiedType = (IType) queryParamField.getAncestor(IJavaElement.TYPE);
		resource.update(modifiedType, JdtUtils.parse(modifiedType, null));
		// verifications
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(resource);
		assertThat(endpoints.size(), equalTo(3));
		for(JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("start={int}"));
		}
	}
	
	@Test
	public void shouldAddQueryParamInAllEndpointsWhenAddingAnnotatedFieldInSubresourceLocator() throws CoreException {
		// pre-condition
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		// operation
		JavaElementsUtils.createField(resource.getJavaElement(), "@QueryParam(\"start\") private int start;", WORKING_COPY);
		// verifications
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(resource);
		assertThat(endpoints.size(), equalTo(5));
		for(JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("start={int}"));
		}
	}
	
	@Test
	public void shouldAddQueryParamInAllEndpointsWhenAddingAnnotationOnFieldInSubresourceLocator() throws CoreException {
		// pre-condition
		final IType resourceType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		ResourcesUtils.replaceFirstOccurrenceOfCode(resourceType, "//PlaceHolder", "private int start; //PlaceHolder", PRIMARY_COPY);
		final IField queryParamField = resourceType.getField("start");
		final JaxrsResource resource = metamodelMonitor.createResource(resourceType);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		JavaElementsUtils.addFieldAnnotation(queryParamField, "@QueryParam(\"start\")", WORKING_COPY);
		final IType modifiedType = (IType) queryParamField.getAncestor(IJavaElement.TYPE);
		resource.update(modifiedType, JdtUtils.parse(modifiedType, null));
		// verifications
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(resource);
		assertThat(endpoints.size(), equalTo(5));
		for(JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("start={int}"));
		}
	}
	
	@Test
	public void shouldAddMatrixParamInAllEndpointsWhenAddingAnnotatedFieldInRootResource() throws CoreException {
		// pre-condition
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		// operation
		JavaElementsUtils.createField(resource.getJavaElement(), "@MatrixParam(\"lang\") private String l;", WORKING_COPY);
		// verifications
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(resource);
		assertThat(endpoints.size(), equalTo(6));
		for(JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString(";lang={String}"));
		}
	}
	
	@Test
	public void shouldAddMatrixParamInAllEndpointsWhenAddingAnnotationOnFieldInRootResource() throws CoreException {
		// pre-condition
		final IType resourceType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		ResourcesUtils.replaceFirstOccurrenceOfCode(resourceType, "//PlaceHolder", "private String l; //PlaceHolder", PRIMARY_COPY);
		final IField queryParamField = resourceType.getField("l");
		final JaxrsResource resource = metamodelMonitor.createResource(resourceType);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		JavaElementsUtils.addFieldAnnotation(queryParamField, "@MatrixParam(\"lang\")", WORKING_COPY);
		final IType modifiedType = (IType) queryParamField.getAncestor(IJavaElement.TYPE);
		resource.update(modifiedType, JdtUtils.parse(modifiedType, null));
		// verifications
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(resource);
		assertThat(endpoints.size(), equalTo(6));
		for(JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString(";lang={String}"));
		}
	}
	
	@Test
	public void shouldAddMatrixParamInAllEndpointsWhenAddingAnnotatedFieldInSubresource() throws CoreException {
		// pre-condition
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		// operation
		JavaElementsUtils.createField(resource.getJavaElement(), "@MatrixParam(\"lang\") private String l;", WORKING_COPY);
		// verifications
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(resource);
		assertThat(endpoints.size(), equalTo(3));
		for(JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString(";lang={String}"));
		}
	}
	
	@Test
	public void shouldAddMatrixParamInAllEndpointsWhenAddingAnnotationOnFieldInSubresource() throws CoreException {
		// pre-condition
		final IType resourceType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		ResourcesUtils.replaceFirstOccurrenceOfCode(resourceType, "//PlaceHolder", "private String l; //PlaceHolder", PRIMARY_COPY);
		final IField queryParamField = resourceType.getField("l");
		final JaxrsResource resource = metamodelMonitor.createResource(resourceType);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		JavaElementsUtils.addFieldAnnotation(queryParamField, "@MatrixParam(\"lang\")", WORKING_COPY);
		final IType modifiedType = (IType) queryParamField.getAncestor(IJavaElement.TYPE);
		resource.update(modifiedType, JdtUtils.parse(modifiedType, null));
		// verifications
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(resource);
		assertThat(endpoints.size(), equalTo(3));
		for(JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString(";lang={String}"));
		}
	}
	
	@Test
	public void shouldAddMatrixParamInAllEndpointsWhenAddingAnnotatedFieldInSubresourceLocator() throws CoreException {
		// pre-condition
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		// operation
		JavaElementsUtils.createField(resource.getJavaElement(), "@MatrixParam(\"lang\") private String l;", WORKING_COPY);
		// verifications
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(resource);
		assertThat(endpoints.size(), equalTo(5));
		for(JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString(";lang={String}"));
		}
	}
	
	@Test
	public void shouldAddMatrixParamInAllEndpointsWhenAddingAnnotationOnFieldInSubresourceLocator() throws CoreException {
		// pre-condition
		final IType resourceType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		ResourcesUtils.replaceFirstOccurrenceOfCode(resourceType, "//PlaceHolder", "private String l; //PlaceHolder", PRIMARY_COPY);
		final IField queryParamField = resourceType.getField("l");
		final JaxrsResource resource = metamodelMonitor.createResource(resourceType);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		JavaElementsUtils.addFieldAnnotation(queryParamField, "@MatrixParam(\"lang\")", WORKING_COPY);
		final IType modifiedType = (IType) queryParamField.getAncestor(IJavaElement.TYPE);
		resource.update(modifiedType, JdtUtils.parse(modifiedType, null));
		// verifications
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(resource);
		assertThat(endpoints.size(), equalTo(5));
		for(JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString(";lang={String}"));
		}
	}
	
	@Test
	public void shouldChangeQueryParamInAllEndpointsWhenUpdatingFieldAnnotationInRootResource() throws CoreException {
		// pre-condition
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		JavaElementsUtils.createField(resource.getJavaElement(), "@QueryParam(\"start\") private int foo;", WORKING_COPY);
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(resource);
		assertThat(endpoints.size(), equalTo(6));
		for(JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("start={int}"));
		}
		// now change the field annotation
		replaceFirstOccurrenceOfCode(resource.getJavaElement(), "@QueryParam(\"start\") private int foo;", "@QueryParam(\"begin\") private int foo;", WORKING_COPY);
		// verifications
		final Collection<JaxrsEndpoint> modifiedEndpoints = metamodel.findEndpoints(resource);
		assertThat(modifiedEndpoints.size(), equalTo(6));
		for(JaxrsEndpoint endpoint : modifiedEndpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("begin={int}"));
		}
	}
	
	@Test
	public void shouldChangeQueryParamInAllEndpointsWhenUpdatingFieldAnnotationInSubresource() throws CoreException {
		// pre-condition
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		JavaElementsUtils.createField(resource.getJavaElement(), "@QueryParam(\"start\") private int foo;", WORKING_COPY);
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(resource);
		assertThat(endpoints.size(), equalTo(3));
		for(JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("start={int}"));
		}
		// now change the field annotation
		replaceFirstOccurrenceOfCode(resource.getJavaElement(), "@QueryParam(\"start\") private int foo;", "@QueryParam(\"size\") private int foo;", WORKING_COPY);
		// verifications
		final Collection<JaxrsEndpoint> modifiedEndpoints = metamodel.findEndpoints(resource);
		assertThat(modifiedEndpoints.size(), equalTo(3));
		for(JaxrsEndpoint endpoint : modifiedEndpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("size={int}"));
		}
	}
	
	@Test
	public void shouldChangeQueryParamInAllEndpointsWhenUpdatingFieldAnnotationInSubresourceLocator() throws CoreException {
		// pre-condition
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		JavaElementsUtils.createField(resource.getJavaElement(), "@QueryParam(\"start\") private int foo;", WORKING_COPY);
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(resource);
		assertThat(endpoints.size(), equalTo(5));
		for(JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("start={int}"));
		}
		// now change the field annotation
		replaceFirstOccurrenceOfCode(resource.getJavaElement(), "@QueryParam(\"start\") private int foo;", "@QueryParam(\"size\") private int foo;", WORKING_COPY);
		// verifications
		final Collection<JaxrsEndpoint> modifiedEndpoints = metamodel.findEndpoints(resource);
		assertThat(modifiedEndpoints.size(), equalTo(5));
		for(JaxrsEndpoint endpoint : modifiedEndpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("size={int}"));
		}
	}
	
	@Test
	public void shouldChangeMatrixParamInAllEndpointsWhenUpdatingFieldAnnotationInRootRresource() throws CoreException {
		// pre-condition
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		JavaElementsUtils.createField(resource.getJavaElement(), "@MatrixParam(\"lang\") private String l;", WORKING_COPY);
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(resource);
		assertThat(endpoints.size(), equalTo(6));
		for(JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("lang={String}"));
		}
		// now change the field annotation
		replaceFirstOccurrenceOfCode(resource.getJavaElement(), "@MatrixParam(\"lang\") private String l;", "@MatrixParam(\"language\") private String l;", WORKING_COPY);
		// verifications
		final Collection<JaxrsEndpoint> modifiedEndpoints = metamodel.findEndpoints(resource);
		assertThat(modifiedEndpoints.size(), equalTo(6));
		for(JaxrsEndpoint endpoint : modifiedEndpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("language={String}"));
		}
	}
	
	@Test
	public void shouldChangeMatrixParamInAllEndpointsWhenUpdatingFieldAnnotationInSubresource() throws CoreException {
		// pre-condition
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		JavaElementsUtils.createField(resource.getJavaElement(), "@MatrixParam(\"lang\") private String l;",
				WORKING_COPY);
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(resource);
		assertThat(endpoints.size(), equalTo(3));
		for (JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("lang={String}"));
		}
		// now change the field annotation
		replaceFirstOccurrenceOfCode(resource.getJavaElement(), "@MatrixParam(\"lang\") private String l;",
				"@MatrixParam(\"language\") private String l;", WORKING_COPY);
		// verifications
		final Collection<JaxrsEndpoint> modifiedEndpoints = metamodel.findEndpoints(resource);
		assertThat(modifiedEndpoints.size(), equalTo(3));
		for (JaxrsEndpoint endpoint : modifiedEndpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("language={String}"));
		}
	}
	
	@Test
	public void shouldChangeMatrixParamInAllEndpointsWhenUpdatingFieldAnnotationInSubresourceLocator() throws CoreException {
		// pre-condition
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		JavaElementsUtils.createField(resource.getJavaElement(), "@MatrixParam(\"lang\") private String l;", WORKING_COPY);
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(resource);
		assertThat(endpoints.size(), equalTo(5));
		for(JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("lang={String}"));
		}
		// now change the field annotation
		replaceFirstOccurrenceOfCode(resource.getJavaElement(), "@MatrixParam(\"lang\") private String l;", "@MatrixParam(\"language\") private String l;", WORKING_COPY);
		// verifications
		final Collection<JaxrsEndpoint> modifiedEndpoints = metamodel.findEndpoints(resource);
		assertThat(modifiedEndpoints.size(), equalTo(5));
		for(JaxrsEndpoint endpoint : modifiedEndpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("language={String}"));
		}
	}
	
	@Test
	public void shouldRemoveQueryParamInAllEndpointsWhenRemovingAnnotatedFieldInRootResource() throws CoreException {
		// pre-condition
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		metamodelMonitor.resetElementChangesNotifications();
		final IField queryParamField = JavaElementsUtils.createField(resource.getJavaElement(), "@QueryParam(\"foo\") private int start;", WORKING_COPY);
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(resource);
		assertThat(endpoints.size(), equalTo(6));
		for(JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("foo={int}"));
		}
		// operation: now, remove field
		JavaElementsUtils.removeField(queryParamField, WORKING_COPY);
		// verifications
		final Collection<JaxrsEndpoint> modifiedEndpoints = metamodel.findEndpoints(resource);
		assertThat(modifiedEndpoints.size(), equalTo(6));
		for(JaxrsEndpoint endpoint : modifiedEndpoints) {
			assertThat(endpoint.getUriPathTemplate(), not(containsString("foo={int}")));
		}
	}
	
	@Test
	public void shouldRemoveQueryParamInAllEndpointsWhenRemovingAnnotationOnFieldInRootResource() throws CoreException {
		// pre-condition
		final IType resourceType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		ResourcesUtils.replaceFirstOccurrenceOfCode(resourceType, "//PlaceHolder", "private int start; //PlaceHolder", PRIMARY_COPY);
		final IField queryParamField = resourceType.getField("start");
		final JaxrsResource resource = metamodelMonitor.createResource(resourceType);
		metamodelMonitor.resetElementChangesNotifications();
		JavaElementsUtils.addFieldAnnotation(queryParamField, "@QueryParam(\"foo\")", WORKING_COPY);
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(resource);
		assertThat(endpoints.size(), equalTo(6));
		for(JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("foo={int}"));
		}
		// operation: now, remove the annotation
		JavaElementsUtils.removeFieldAnnotation(queryParamField, "@QueryParam(\"foo\")", WORKING_COPY);
		// verifications
		final Collection<JaxrsEndpoint> modifiedEndpoints = metamodel.findEndpoints(resource);
		assertThat(modifiedEndpoints.size(), equalTo(6));
		for(JaxrsEndpoint endpoint : modifiedEndpoints) {
			assertThat(endpoint.getUriPathTemplate(), not(containsString("foo={int}")));
		}
	}
	
	@Test
	public void shouldRemoveQueryParamInAllEndpointsWhenRemovingAnnotatedFieldInSubresource() throws CoreException {
		// pre-condition
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		metamodelMonitor.resetElementChangesNotifications();
		final IField queryParamField = JavaElementsUtils.createField(resource.getJavaElement(), "@QueryParam(\"foo\") private int start;", WORKING_COPY);
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(resource);
		assertThat(endpoints.size(), equalTo(3));
		for(JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("foo={int}"));
		}
		// operation: now, remove field
		JavaElementsUtils.removeField(queryParamField, WORKING_COPY);
		// verifications
		final Collection<JaxrsEndpoint> modifiedEndpoints = metamodel.findEndpoints(resource);
		assertThat(modifiedEndpoints.size(), equalTo(3));
		for(JaxrsEndpoint endpoint : modifiedEndpoints) {
			assertThat(endpoint.getUriPathTemplate(), not(containsString("foo={int}")));
		}
	}

	@Test
	public void shouldRemoveQueryParamInAllEndpointsWhenRemovingAnnotationOnFieldInSubresource() throws CoreException {
		// pre-condition
		final IType resourceType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		ResourcesUtils.replaceFirstOccurrenceOfCode(resourceType, "//PlaceHolder", "private int start; //PlaceHolder", PRIMARY_COPY);
		final IField queryParamField = resourceType.getField("start");
		final JaxrsResource resource = metamodelMonitor.createResource(resourceType);
		metamodelMonitor.resetElementChangesNotifications();
		JavaElementsUtils.addFieldAnnotation(queryParamField, "@QueryParam(\"foo\")", WORKING_COPY);
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(resource);
		assertThat(endpoints.size(), equalTo(3));
		for(JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("foo={int}"));
		}
		// operation: now, remove the annotation
		JavaElementsUtils.removeFieldAnnotation(queryParamField, "@QueryParam(\"foo\")", WORKING_COPY);
		// verifications
		final Collection<JaxrsEndpoint> modifiedEndpoints = metamodel.findEndpoints(resource);
		assertThat(modifiedEndpoints.size(), equalTo(3));
		for(JaxrsEndpoint endpoint : modifiedEndpoints) {
			assertThat(endpoint.getUriPathTemplate(), not(containsString("foo={int}")));
		}
	}

	@Test
	public void shouldRemoveQueryParamInAllEndpointsWhenRemovingAnnotatedFieldInSubresourceLocator() throws CoreException {
		// pre-condition
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		metamodelMonitor.resetElementChangesNotifications();
		final IField queryParamField = JavaElementsUtils.createField(resource.getJavaElement(), "@QueryParam(\"start\") private int start;", WORKING_COPY);
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(resource);
		assertThat(endpoints.size(), equalTo(5));
		for(JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("start={int}"));
		}
		// operation: now, remove field
		JavaElementsUtils.removeField(queryParamField, WORKING_COPY);
		// verifications
		final Collection<JaxrsEndpoint> modifiedEndpoints = metamodel.findEndpoints(resource);
		assertThat(modifiedEndpoints.size(), equalTo(5));
		for(JaxrsEndpoint endpoint : modifiedEndpoints) {
			assertThat(endpoint.getUriPathTemplate(), not(containsString("start={int}")));
		}
	}
	
	@Test
	public void shouldRemoveQueryParamInAllEndpointsWhenRemovingAnnotationOnFieldInSubresourceLocator() throws CoreException {
		// pre-condition
		final IType resourceType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		ResourcesUtils.replaceFirstOccurrenceOfCode(resourceType, "//PlaceHolder", "private int start; //PlaceHolder", PRIMARY_COPY);
		final IField queryParamField = resourceType.getField("start");
		final JaxrsResource resource = metamodelMonitor.createResource(resourceType);
		metamodelMonitor.resetElementChangesNotifications();
		JavaElementsUtils.addFieldAnnotation(queryParamField, "@QueryParam(\"start\")", WORKING_COPY);
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(resource);
		assertThat(endpoints.size(), equalTo(5));
		for(JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("start={int}"));
		}
		// operation: now, remove the annotation
		JavaElementsUtils.removeFieldAnnotation(queryParamField, "@QueryParam(\"start\")", WORKING_COPY);
		// verifications
		final Collection<JaxrsEndpoint> modifiedEndpoints = metamodel.findEndpoints(resource);
		assertThat(modifiedEndpoints.size(), equalTo(5));
		for(JaxrsEndpoint endpoint : modifiedEndpoints) {
			assertThat(endpoint.getUriPathTemplate(), not(containsString("start={int}")));
		}
	}
	
	@Test
	public void shouldRemoveMatrixParamInAllEndpointsWhenRemovingAnnotatedFieldInRootResource() throws CoreException {
		// pre-condition
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		metamodelMonitor.resetElementChangesNotifications();
		final IField queryParamField = JavaElementsUtils.createField(resource.getJavaElement(), "@MatrixParam(\"lang\") private String l;", WORKING_COPY);
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(resource);
		assertThat(endpoints.size(), equalTo(6));
		for(JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("lang={String}"));
		}
		// operation: now, remove field
		JavaElementsUtils.removeField(queryParamField, WORKING_COPY);
		// verifications
		final Collection<JaxrsEndpoint> modifiedEndpoints = metamodel.findEndpoints(resource);
		assertThat(modifiedEndpoints.size(), equalTo(6));
		for(JaxrsEndpoint endpoint : modifiedEndpoints) {
			assertThat(endpoint.getUriPathTemplate(), not(containsString("lang={String}")));
		}
	}
	
	@Test
	public void shouldRemoveMatrixParamInAllEndpointsWhenRemovingAnnotationOnFieldInRootResource() throws CoreException {
		// pre-condition
		final IType resourceType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		ResourcesUtils.replaceFirstOccurrenceOfCode(resourceType, "//PlaceHolder", "private String l; //PlaceHolder", PRIMARY_COPY);
		final IField queryParamField = resourceType.getField("l");
		final JaxrsResource resource = metamodelMonitor.createResource(resourceType);
		metamodelMonitor.resetElementChangesNotifications();
		JavaElementsUtils.addFieldAnnotation(queryParamField, "@MatrixParam(\"lang\")", WORKING_COPY);
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(resource);
		assertThat(endpoints.size(), equalTo(6));
		for(JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("lang={String}"));
		}
		// operation: now, remove the annotation
		JavaElementsUtils.removeFieldAnnotation(queryParamField, "@MatrixParam(\"lang\")", WORKING_COPY);
		// verifications
		final Collection<JaxrsEndpoint> modifiedEndpoints = metamodel.findEndpoints(resource);
		assertThat(modifiedEndpoints.size(), equalTo(6));
		for(JaxrsEndpoint endpoint : modifiedEndpoints) {
			assertThat(endpoint.getUriPathTemplate(), not(containsString("lang={String}")));
		}
	}
	
	@Test
	public void shouldRemoveMatrixParamInAllEndpointsWhenRemovingAnnotatedFieldInSubresource() throws CoreException {
		// pre-condition
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		metamodelMonitor.resetElementChangesNotifications();
		final IField queryParamField = JavaElementsUtils.createField(resource.getJavaElement(), "@MatrixParam(\"lang\") private String l;", WORKING_COPY);
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(resource);
		assertThat(endpoints.size(), equalTo(3));
		for(JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("lang={String}"));
		}
		// operation: now, remove field
		JavaElementsUtils.removeField(queryParamField, WORKING_COPY);
		// verifications
		final Collection<JaxrsEndpoint> modifiedEndpoints = metamodel.findEndpoints(resource);
		assertThat(modifiedEndpoints.size(), equalTo(3));
		for(JaxrsEndpoint endpoint : modifiedEndpoints) {
			assertThat(endpoint.getUriPathTemplate(), not(containsString("lang={String}")));
		}
	}

	@Test
	public void shouldRemoveMatrixParamInAllEndpointsWhenRemovingAnnotationOnFieldInSubresource() throws CoreException {
		// pre-condition
		final IType resourceType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		ResourcesUtils.replaceFirstOccurrenceOfCode(resourceType, "//PlaceHolder", "private String l; //PlaceHolder", PRIMARY_COPY);
		final IField queryParamField = resourceType.getField("l");
		final JaxrsResource resource = metamodelMonitor.createResource(resourceType);
		metamodelMonitor.resetElementChangesNotifications();
		JavaElementsUtils.addFieldAnnotation(queryParamField, "@MatrixParam(\"lang\")", WORKING_COPY);
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(resource);
		assertThat(endpoints.size(), equalTo(3));
		for(JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("lang={String}"));
		}
		// operation: now, remove the annotation
		JavaElementsUtils.removeFieldAnnotation(queryParamField, "@MatrixParam(\"lang\")", WORKING_COPY);
		// verifications
		final Collection<JaxrsEndpoint> modifiedEndpoints = metamodel.findEndpoints(resource);
		assertThat(modifiedEndpoints.size(), equalTo(3));
		for(JaxrsEndpoint endpoint : modifiedEndpoints) {
			assertThat(endpoint.getUriPathTemplate(), not(containsString("lang={String}")));
		}
	}

	@Test
	public void shouldRemoveMatrixParamInAllEndpointsWhenRemovingAnnotatedFieldInSubresourceLocator() throws CoreException {
		// pre-condition
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		metamodelMonitor.resetElementChangesNotifications();
		final IField queryParamField = JavaElementsUtils.createField(resource.getJavaElement(), "@MatrixParam(\"lang\") private String l;", WORKING_COPY);
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(resource);
		assertThat(endpoints.size(), equalTo(5));
		for(JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("lang={String}"));
		}
		// operation: now, remove field
		JavaElementsUtils.removeField(queryParamField, WORKING_COPY);
		// verifications
		final Collection<JaxrsEndpoint> modifiedEndpoints = metamodel.findEndpoints(resource);
		assertThat(modifiedEndpoints.size(), equalTo(5));
		for(JaxrsEndpoint endpoint : modifiedEndpoints) {
			assertThat(endpoint.getUriPathTemplate(), not(containsString("lang={String}")));
		}
	}
	
	@Test
	public void shouldRemoveMatrixParamInAllEndpointsWhenRemovingAnnotationOnFieldInSubresourceLocator() throws CoreException {
		// pre-condition
		final IType resourceType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		ResourcesUtils.replaceFirstOccurrenceOfCode(resourceType, "//PlaceHolder", "private String l; //PlaceHolder", PRIMARY_COPY);
		final IField queryParamField = resourceType.getField("l");
		final JaxrsResource resource = metamodelMonitor.createResource(resourceType);
		metamodelMonitor.resetElementChangesNotifications();
		JavaElementsUtils.addFieldAnnotation(queryParamField, "@MatrixParam(\"lang\")", WORKING_COPY);
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(resource);
		assertThat(endpoints.size(), equalTo(5));
		for(JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("lang={String}"));
		}
		// operation: now, remove the annotation
		JavaElementsUtils.removeFieldAnnotation(queryParamField, "@MatrixParam(\"lang\")", WORKING_COPY);
		// verifications
		final Collection<JaxrsEndpoint> modifiedEndpoints = metamodel.findEndpoints(resource);
		assertThat(modifiedEndpoints.size(), equalTo(5));
		for(JaxrsEndpoint endpoint : modifiedEndpoints) {
			assertThat(endpoint.getUriPathTemplate(), not(containsString("lang={String}")));
		}
	}
	
}
