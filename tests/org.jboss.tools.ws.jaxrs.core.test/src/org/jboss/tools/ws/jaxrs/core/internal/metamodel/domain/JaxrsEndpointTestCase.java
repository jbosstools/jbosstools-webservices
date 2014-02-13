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
import static org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils.replaceFirstOccurrenceOfCode;
import static org.junit.Assert.assertThat;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.Flags;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta;
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
	public static WorkspaceSetupRule workspaceSetupRule = new WorkspaceSetupRule("org.jboss.tools.ws.jaxrs.tests.sampleproject");
	
	@Rule
	public JaxrsMetamodelMonitor metamodelMonitor = new JaxrsMetamodelMonitor("org.jboss.tools.ws.jaxrs.tests.sampleproject", true);
	
	private JaxrsMetamodel metamodel = null;

	private IJavaProject javaProject = null;

	@Before
	public void setup() throws CoreException {
		metamodel = metamodelMonitor.getMetamodel();
		javaProject = metamodel.getJavaProject();
	}
	
	private JaxrsResourceMethod getModifiedResourceMethod(final boolean useWorkingCopy) throws CoreException {
		final String typeName = "org.jboss.tools.ws.jaxrs.sample.services.CustomerResource";
		final String oldContent = "@PathParam(\"id\") Integer id, @Context UriInfo uriInfo";
		final String newContent = "@QueryParam(\"queryParam1\") String queryParam1, @QueryParam(\"queryParam2\") String queryParam2, @MatrixParam(\"matrixParam1\") String matrixParam1, @MatrixParam(\"matrixParam2\") String matrixParam2";
		replaceFirstOccurrenceOfCode(typeName, javaProject, oldContent, newContent, useWorkingCopy);
		final IType resourceType = metamodelMonitor.resolveType(typeName);
		final IMethod method = metamodelMonitor.resolveMethod(resourceType, "getCustomer");
		final JaxrsResource resource = metamodel.findResource(resourceType);
		return resource.getMethods().get(method.getHandleIdentifier());
	}

	private IMethod modifyJavaMethodSignature(final JaxrsResourceMethod resourceMethod, final boolean useWorkingCopy) throws CoreException {
		IMethod javaMethod = resourceMethod.getJavaElement();
		final String oldContent = "@QueryParam(\"queryParam1\") String queryParam1, @QueryParam(\"queryParam2\") String queryParam2, @MatrixParam(\"matrixParam1\") String matrixParam1, @MatrixParam(\"matrixParam2\") String matrixParam2";
		final String newContent = "@QueryParam(\"queryParam1\") String queryParam1, @MatrixParam(\"matrixParam1\") String matrixParam1, @MatrixParam(\"matrixParam2\") String matrixParam2, @MatrixParam(\"matrixParam3\") String matrixParam3";
		return replaceFirstOccurrenceOfCode(javaMethod, oldContent, newContent, useWorkingCopy);
	}

	@Test
	public void shouldDisplayEndpointParametersInOrderAtCreationInWorkingCopy() throws CoreException {
		// pre-conditions
		metamodel.findHttpMethodByTypeName("javax.ws.rs.GET");
		final JaxrsResourceMethod resourceMethod = getModifiedResourceMethod(WORKING_COPY);
		final IJaxrsEndpoint endpoint = metamodel.findEndpoints(resourceMethod).get(0);
		// operation
		String uriPathTemplate = endpoint.getUriPathTemplate();
		// verifications
		assertThat(
				uriPathTemplate,
				equalTo("/hello/customers/{id};matrixParam1={java.lang.String};matrixParam2={java.lang.String}?queryParam1={queryParam1:java.lang.String}&queryParam2={queryParam2:java.lang.String}"));
	}

	@Test
	public void shouldDisplayEndpointParametersInOrderAtCreationInPrimaryCopy() throws CoreException {
		// pre-conditions
		metamodel.findHttpMethodByTypeName("javax.ws.rs.GET");
		final JaxrsResourceMethod resourceMethod = getModifiedResourceMethod(PRIMARY_COPY);
		final IJaxrsEndpoint endpoint = metamodel.findEndpoints(resourceMethod).get(0);
		// operation
		String uriPathTemplate = endpoint.getUriPathTemplate();
		// verifications
		assertThat(
				uriPathTemplate,
				equalTo("/hello/customers/{id};matrixParam1={java.lang.String};matrixParam2={java.lang.String}?queryParam1={queryParam1:java.lang.String}&queryParam2={queryParam2:java.lang.String}"));
	}
	
	@Test
	public void shouldDisplayEndpointParametersInOrderAfterHttpMethodUpdateInPrimaryCopy() throws CoreException {
		// pre-conditions
		metamodel.findHttpMethodByTypeName("javax.ws.rs.GET");
		final JaxrsResourceMethod resourceMethod = getModifiedResourceMethod(PRIMARY_COPY);
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(resourceMethod).get(0);
		// operation
		replaceFirstOccurrenceOfCode(resourceMethod.getJavaElement(), "@GET", "@POST", false);
		// verifications
		assertThat(endpoint.getHttpMethod().getHttpVerb(), equalTo("POST"));
	}

	@Test
	public void shouldDisplayEndpointParametersInOrderAfterHttpMethodUpdateInWorkingCopy() throws CoreException {
		// pre-conditions
		metamodel.findHttpMethodByTypeName("javax.ws.rs.GET");
		final JaxrsResourceMethod resourceMethod = getModifiedResourceMethod(WORKING_COPY);
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(resourceMethod).get(0);
		// operation
		replaceFirstOccurrenceOfCode(resourceMethod.getJavaElement(), "@GET", "@POST", false);
		// verifications
		assertThat(endpoint.getHttpMethod().getHttpVerb(), equalTo("POST"));
	}

	@Test
	public void shouldDisplayEndpointParametersInOrderAfterMethodParametersUpdateInWorkingCopy() throws CoreException {
		// pre-conditions
		metamodel.findHttpMethodByTypeName("javax.ws.rs.GET");
		final JaxrsResourceMethod resourceMethod = getModifiedResourceMethod(WORKING_COPY);
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(resourceMethod).get(0);
		// operation
		final IMethod modifiedResource  = modifyJavaMethodSignature(resourceMethod, WORKING_COPY);
		resourceMethod.update(modifiedResource, JdtUtils.parse(modifiedResource, null));
		endpoint.update(new Flags(JaxrsElementDelta.F_QUERY_PARAM_ANNOTATION
				+ JaxrsElementDelta.F_MATRIX_PARAM_ANNOTATION));
		String uriPathTemplate = endpoint.getUriPathTemplate();
		// verifications
		assertThat(
				uriPathTemplate,
				equalTo("/hello/customers/{id};matrixParam1={java.lang.String};matrixParam2={java.lang.String};matrixParam3={java.lang.String}?queryParam1={queryParam1:java.lang.String}"));
	}

	@Test
	public void shouldDisplayEndpointParametersInOrderAfterMethodParametersUpdateInPrimaryCopy() throws CoreException {
		// pre-conditions
		metamodel.findHttpMethodByTypeName("javax.ws.rs.GET");
		final JaxrsResourceMethod resourceMethod = getModifiedResourceMethod(PRIMARY_COPY);
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(resourceMethod).get(0);
		// operation
		final IMethod modifiedResource  = modifyJavaMethodSignature(resourceMethod, PRIMARY_COPY);
		resourceMethod.update(modifiedResource, JdtUtils.parse(modifiedResource, null));
		endpoint.update(new Flags(JaxrsElementDelta.F_QUERY_PARAM_ANNOTATION
				+ JaxrsElementDelta.F_MATRIX_PARAM_ANNOTATION));
		String uriPathTemplate = endpoint.getUriPathTemplate();
		// verifications
		assertThat(
				uriPathTemplate,
				equalTo("/hello/customers/{id};matrixParam1={java.lang.String};matrixParam2={java.lang.String};matrixParam3={java.lang.String}?queryParam1={queryParam1:java.lang.String}"));
	}

}
