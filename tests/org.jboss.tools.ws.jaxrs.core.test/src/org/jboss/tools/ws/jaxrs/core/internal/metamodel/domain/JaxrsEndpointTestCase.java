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
import static org.junit.Assert.assertThat;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.WorkbenchUtils;
import org.jboss.tools.ws.jaxrs.core.builder.AbstractMetamodelBuilderTestCase;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta;
import org.junit.Test;

/**
 * @author xcoulon
 * 
 */
public class JaxrsEndpointTestCase extends AbstractMetamodelBuilderTestCase {

	private static final boolean PRIMARY_COPY = false;

	private static final boolean WORKING_COPY = true;

	private JaxrsResourceMethod getModifiedResourceMethod(final boolean useWorkingCopy) throws CoreException {
		final String typeName = "org.jboss.tools.ws.jaxrs.sample.services.CustomerResource";
		final IType resourceType = getType(typeName);
		final IMethod method = getJavaMethod(resourceType, "getCustomer");
		final String oldContent = "@PathParam(\"id\") Integer id, @Context UriInfo uriInfo";
		final String newContent = "@PathParam(\"id\") Integer id, @QueryParam(\"queryParam1\") Integer queryParam1, @QueryParam(\"queryParam2\") String queryParam2, @MatrixParam(\"matrixParam1\") Integer matrixParam1, @MatrixParam(\"matrixParam2\") List<String> matrixParam2";
		WorkbenchUtils.replaceFirstOccurrenceOfCode(method, oldContent, newContent, useWorkingCopy);
		final JaxrsResource resource = metamodel.findResource(resourceType);
		return resource.getMethods().get(getJavaMethod(resourceType, "getCustomer").getHandleIdentifier());
	}

	private IMethod modifyGetCustomerMethodSignature(final JaxrsResourceMethod resourceMethod, final boolean useWorkingCopy) throws CoreException {
		IMethod javaMethod = resourceMethod.getJavaElement();
		final String oldAnnotationContent = "@Path(\"{id}\")";
		final String newAnnotationContent = "@Path(\"{foo:\\\\d+}/{bar:[a-z]+}\")";
		WorkbenchUtils.replaceFirstOccurrenceOfCode(javaMethod, oldAnnotationContent, newAnnotationContent, useWorkingCopy);
		final String oldArgsContent = "@PathParam(\"id\") Integer id, @QueryParam(\"queryParam1\") Integer queryParam1, @QueryParam(\"queryParam2\") String queryParam2, @MatrixParam(\"matrixParam1\") Integer matrixParam1, @MatrixParam(\"matrixParam2\") List<String> matrixParam2";
		final String newArgsContent = "@PathParam(\"foo\") int ident, @PathParam(\"bar\") Char bar, @QueryParam(\"queryParam1\") long queryParam1, @MatrixParam(\"matrixParam1\") short matrixParam1, @MatrixParam(\"matrixParam2\") List<String> matrixParam2, @MatrixParam(\"matrixParam3\") String matrixParam3";
		return WorkbenchUtils.replaceFirstOccurrenceOfCode(javaMethod, oldArgsContent, newArgsContent, useWorkingCopy);
	}

	private IMethod modifyGetProductResourceLocatorMethodSignature(final IMethod javaMethod, final boolean useWorkingCopy) throws CoreException {
		final String oldArgsContent = "getProductResourceLocator()";
		final String newArgsContent = "getProductResourceLocator(@QueryParam(\"queryParam1\") Integer queryParam1, @MatrixParam(\"matrixParam1\") Integer matrixParam1)";
		return WorkbenchUtils.replaceFirstOccurrenceOfCode(javaMethod, oldArgsContent, newArgsContent, useWorkingCopy);
	}
	
	@Test
	public void shouldDisplayEndpointParametersInOrderAtCreationInWorkingCopy() throws CoreException {
		// pre-conditions
		//metamodel.findHttpMethodByTypeName("javax.ws.rs.GET");
		final JaxrsResourceMethod resourceMethod = getModifiedResourceMethod(WORKING_COPY);
		final IJaxrsEndpoint endpoint = metamodel.findEndpoints(resourceMethod).get(0);
		// operation
		String uriPathTemplate = endpoint.getUriPathTemplate();
		// verifications
		assertThat(
				uriPathTemplate,
				equalTo("/hello/customers/{id:Integer};matrixParam1={Integer};matrixParam2={List<String>}?queryParam1={Integer}&queryParam2={String}"));
	}

	@Test
	public void shouldDisplayEndpointParametersInOrderAtCreationInPrimaryCopy() throws CoreException {
		// pre-conditions
		//metamodel.findHttpMethodByTypeName("javax.ws.rs.GET");
		final JaxrsResourceMethod resourceMethod = getModifiedResourceMethod(PRIMARY_COPY);
		final IJaxrsEndpoint endpoint = metamodel.findEndpoints(resourceMethod).get(0);
		// operation
		String uriPathTemplate = endpoint.getUriPathTemplate();
		// verifications
		assertThat(
				uriPathTemplate,
				equalTo("/hello/customers/{id:Integer};matrixParam1={Integer};matrixParam2={List<String>}?queryParam1={Integer}&queryParam2={String}"));
	}
	
	@Test
	public void shouldDisplayEndpointParametersInOrderAfterHttpMethodUpdateInPrimaryCopy() throws CoreException {
		// pre-conditions
		//metamodel.findHttpMethodByTypeName("javax.ws.rs.GET");
		final JaxrsResourceMethod resourceMethod = getModifiedResourceMethod(PRIMARY_COPY);
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(resourceMethod).get(0);
		// operation
		WorkbenchUtils.replaceFirstOccurrenceOfCode(resourceMethod.getJavaElement(), "@GET", "@POST", false);
		// verifications
		assertThat(endpoint.getHttpMethod().getHttpVerb(), equalTo("POST"));
	}

	@Test
	public void shouldDisplayEndpointParametersInOrderAfterHttpMethodUpdateInWorkingCopy() throws CoreException {
		// pre-conditions
		//metamodel.findHttpMethodByTypeName("javax.ws.rs.GET");
		final JaxrsResourceMethod resourceMethod = getModifiedResourceMethod(WORKING_COPY);
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(resourceMethod).get(0);
		// operation
		WorkbenchUtils.replaceFirstOccurrenceOfCode(resourceMethod.getJavaElement(), "@GET", "@POST", false);
		// verifications
		assertThat(endpoint.getHttpMethod().getHttpVerb(), equalTo("POST"));
	}

	@Test
	public void shouldDisplayEndpointParametersInOrderAfterMethodParametersUpdateInWorkingCopy() throws CoreException {
		// pre-conditions
		//metamodel.findHttpMethodByTypeName("javax.ws.rs.GET");
		final JaxrsResourceMethod resourceMethod = getModifiedResourceMethod(WORKING_COPY);
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(resourceMethod).get(0);
		// operation
		final IMethod modifiedMethod  = modifyGetCustomerMethodSignature(resourceMethod, WORKING_COPY);
		resourceMethod.update(modifiedMethod, JdtUtils.parse(modifiedMethod, null));
		endpoint.update(JaxrsElementDelta.F_PATH_ANNOTATION + JaxrsElementDelta.F_QUERY_PARAM_ANNOTATION
				+ JaxrsElementDelta.F_MATRIX_PARAM_ANNOTATION);
		String uriPathTemplate = endpoint.getUriPathTemplate();
		// verifications
		assertThat(
				uriPathTemplate,
				equalTo("/hello/customers/{foo:\\d+}/{bar:[a-z]+};matrixParam1={short};matrixParam2={List<String>};matrixParam3={String}?queryParam1={long}"));
	}

	@Test
	public void shouldDisplayEndpointParametersInOrderAfterMethodParametersUpdateInPrimaryCopy() throws CoreException {
		// pre-conditions
		//metamodel.findHttpMethodByTypeName("javax.ws.rs.GET");
		final JaxrsResourceMethod resourceMethod = getModifiedResourceMethod(PRIMARY_COPY);
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(resourceMethod).get(0);
		// operation
		final IMethod modifiedMethod  = modifyGetCustomerMethodSignature(resourceMethod, PRIMARY_COPY);
		resourceMethod.update(modifiedMethod, JdtUtils.parse(modifiedMethod, null));
		endpoint.update(JaxrsElementDelta.F_PATH_ANNOTATION + JaxrsElementDelta.F_QUERY_PARAM_ANNOTATION
				+ JaxrsElementDelta.F_MATRIX_PARAM_ANNOTATION);
		// verifications
		String uriPathTemplate = endpoint.getUriPathTemplate();
		assertThat(
				uriPathTemplate,
				equalTo("/hello/customers/{foo:\\d+}/{bar:[a-z]+};matrixParam1={short};matrixParam2={List<String>};matrixParam3={String}?queryParam1={long}"));
	}

	@Test
	public void shouldDisplayEndpointWithTypePathBoundOnField() throws CoreException {
		// pre-conditions
		final String typeName = "org.jboss.tools.ws.jaxrs.sample.services.BookResource";
		final IType resourceType = getType(typeName);
		final IMethod method = getJavaMethod(resourceType, "getPicture");
		final IJaxrsElement resourceMethod = (IJaxrsElement) metamodel.findElement(method);
		// operation
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(resourceMethod).get(0);
		String uriPathTemplate = endpoint.getUriPathTemplate();
		// verifications
		assertThat(uriPathTemplate, equalTo("/hello/products;bar={String}/{productType:String}/{id:Integer};color={String}?foo={String}"));
	}

	@Test
	public void shouldDisplayEndpointWithTypePathUnboundOnMethodParam() throws CoreException {
		// pre-conditions
		final String typeName = "org.jboss.tools.ws.jaxrs.sample.services.BarResource";
		final IType resourceType = getType(typeName);
		final IMethod method = getJavaMethod(resourceType, "getContent2");
		final IJaxrsElement resourceMethod = (IJaxrsElement) metamodel.findElement(method);
		// operation
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(resourceMethod).get(0);
		String uriPathTemplate = endpoint.getUriPathTemplate();
		// verifications
		assertThat(uriPathTemplate, equalTo("/hello/foo/bar/{param1:.*}/user/{id:.*}/{format:(/format/[^/]+?)?}/{encoding:(/encoding/[^/]+?)?}?start={int}"));
	}

	@Test
	public void shouldDisplayEndpointWithTypePathBoundOnMethodParam() throws CoreException {
		// pre-conditions
		final String typeName = "org.jboss.tools.ws.jaxrs.sample.services.BarResource";
		final IType resourceType = getType(typeName);
		final IMethod method = getJavaMethod(resourceType, "update3");
		final IJaxrsElement resourceMethod = (IJaxrsElement) metamodel.findElement(method);
		// operation
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(resourceMethod).get(0);
		String uriPathTemplate = endpoint.getUriPathTemplate();
		// verifications
		assertThat(uriPathTemplate, equalTo("/hello/foo/bar/{param1:String}/{param2:String}"));
	}

	@Test
	public void shouldDisplayEndpointWithFieldBindingAndQueryAndMatrixParams() throws CoreException {
		// pre-conditions
		final String typeName = "org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator";
		final IType resourceType = getType(typeName);
		final IMethod method = getJavaMethod(resourceType, "getProductResourceLocator");
		final IMethod modifiedMethod = modifyGetProductResourceLocatorMethodSignature(method, false);
		final JaxrsResource resource = metamodel.findResource(resourceType);
		final JaxrsResourceMethod resourceMethod = resource.getMethods().get(modifiedMethod.getHandleIdentifier());
		resourceMethod.update(modifiedMethod, JdtUtils.parse(modifiedMethod, null));
		final String bookTypeName = "org.jboss.tools.ws.jaxrs.sample.services.BookResource";
		final IType bookResourceType = getType(bookTypeName);
		final JaxrsResource bookResource = metamodel.findResource(bookResourceType);
		final IMethod bookMethod = getJavaMethod(bookResourceType, "getAllProducts");
		final JaxrsResourceMethod bookResourceMethod = bookResource.getMethods().get(bookMethod.getHandleIdentifier());
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(bookResourceMethod).get(0);
		endpoint.update(JaxrsElementDelta.F_PATH_ANNOTATION + JaxrsElementDelta.F_QUERY_PARAM_ANNOTATION
				+ JaxrsElementDelta.F_MATRIX_PARAM_ANNOTATION);
		// operation
		String uriPathTemplate = endpoint.getUriPathTemplate();
		// verifications
		assertThat(uriPathTemplate, equalTo("/hello/products;bar={String}/{productType:String};matrixParam1={Integer}?foo={String}&queryParam1={Integer}"));
	}
	
}
