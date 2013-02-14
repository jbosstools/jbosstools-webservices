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
import static org.jboss.tools.ws.jaxrs.core.WorkbenchUtils.resolveAnnotations;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.PATH;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.builder.AbstractMetamodelBuilderTestCase;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementDelta;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname;
import org.jboss.tools.ws.jaxrs.core.jdt.JavaMethodParameter;
import org.jboss.tools.ws.jaxrs.core.jdt.JavaMethodSignature;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsEndpoint;
import org.junit.Test;

/**
 * @author xcoulon
 * 
 */
public class JaxrsEndpointTestCase extends AbstractMetamodelBuilderTestCase {

	private Annotation createAnnotation(EnumJaxrsClassname clazz, String value) {
		Map<String, List<String>> values = new HashMap<String, List<String>>();
		values.put("value", Arrays.asList(value));
		return new Annotation(null, clazz.qualifiedName, values);
	}
	
	private JavaMethodParameter createMethodParameter(final String paramName, final EnumJaxrsClassname annotationClassName) {
		Annotation annotation = createAnnotation(annotationClassName, paramName);
		return new JavaMethodParameter(paramName, String.class.getName(), Arrays.asList(annotation));
	}

	private JaxrsResourceMethod createResourceMethod() throws CoreException {
		final IType resourceType = getType(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final Map<String, Annotation> annotations = resolveAnnotations(resourceType, PATH.qualifiedName);
		final JaxrsResource resource = new JaxrsResource(resourceType, annotations, metamodel);
		final IMethod method = getMethod(resourceType, "getCustomer");
		final List<JavaMethodParameter> methodParameters = new ArrayList<JavaMethodParameter>();
		methodParameters.add(createMethodParameter("queryParam1", EnumJaxrsClassname.QUERY_PARAM));
		methodParameters.add(createMethodParameter("queryParam2", EnumJaxrsClassname.QUERY_PARAM));
		methodParameters.add(createMethodParameter("matrixParam1", EnumJaxrsClassname.MATRIX_PARAM));
		methodParameters.add(createMethodParameter("matrixParam2", EnumJaxrsClassname.MATRIX_PARAM));
		return new JaxrsResourceMethod(method, methodParameters, null, null, resource, metamodel);
	}
	
	private JavaMethodSignature modifyJavaMethodSignature(JaxrsResourceMethod resourceMethod) {
		IMethod javaMethod = resourceMethod.getJavaElement();
		IType returnedType = resourceMethod.getReturnedType();
		final JavaMethodParameter queryParam1 = createMethodParameter("queryParam1", EnumJaxrsClassname.QUERY_PARAM);
		final JavaMethodParameter matrixParam1 = createMethodParameter("matrixParam1", EnumJaxrsClassname.MATRIX_PARAM);
		final JavaMethodParameter matrixParam2 = createMethodParameter("matrixParam2", EnumJaxrsClassname.MATRIX_PARAM);
		final JavaMethodParameter matrixParam3 = createMethodParameter("matrixParam3", EnumJaxrsClassname.MATRIX_PARAM);
		
		JavaMethodSignature methodSignature = new JavaMethodSignature(javaMethod, returnedType, Arrays.asList(matrixParam1, matrixParam2, matrixParam3, queryParam1));
		return methodSignature;
	}
	
	@Test
	public void shouldDisplayEndpointParametersInOrderAtCreation() throws CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.GET;
		final JaxrsResourceMethod resourceMethod = createResourceMethod();
		final IJaxrsEndpoint endpoint = new JaxrsEndpoint(metamodel,
				httpMethod, resourceMethod);
		// operation
		String uriPathTemplate = endpoint.getUriPathTemplate();
		// verifications
		assertThat(uriPathTemplate, equalTo("/hello/customers;matrixParam1={java.lang.String};matrixParam2={java.lang.String}?queryParam1={queryParam1:java.lang.String}&queryParam2={queryParam2:java.lang.String}"));
	}

	@Test
	public void shouldDisplayEndpointParametersInOrderAfterHttpMethodUpdate() throws CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.GET;
		final JaxrsResourceMethod resourceMethod = createResourceMethod();
		final JaxrsEndpoint endpoint = new JaxrsEndpoint(metamodel,
				httpMethod, resourceMethod);
		// operation
		final Map<String, Annotation> annotations = new HashMap<String, Annotation>();
		final Annotation postAnnotation = createAnnotation(EnumJaxrsClassname.POST, null);
		annotations.put(postAnnotation.getFullyQualifiedName(),postAnnotation);
		resourceMethod.updateAnnotations(annotations);
		endpoint.refresh(resourceMethod, JaxrsElementDelta.F_HTTP_METHOD_ANNOTATION);
		// verifications
		assertThat(endpoint.getHttpMethod().getHttpVerb(), equalTo("POST"));
	}
	
	@Test
	public void shouldDisplayEndpointParametersInOrderAfterMethodParametersUpdate() throws CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = JaxrsBuiltinHttpMethod.GET;
		final JaxrsResourceMethod resourceMethod = createResourceMethod();
		final JaxrsEndpoint endpoint = new JaxrsEndpoint(metamodel,
				httpMethod, resourceMethod);
		// operation
		JavaMethodSignature methodSignature = modifyJavaMethodSignature(resourceMethod);
		resourceMethod.update(methodSignature);
		endpoint.refresh(resourceMethod, JaxrsElementDelta.F_QUERY_PARAM_ANNOTATION + JaxrsElementDelta.F_MATRIX_PARAM_ANNOTATION);
		String uriPathTemplate = endpoint.getUriPathTemplate();
		// verifications
		assertThat(uriPathTemplate, equalTo("/hello/customers;matrixParam1={java.lang.String};matrixParam2={java.lang.String};matrixParam3={java.lang.String}?queryParam1={queryParam1:java.lang.String}"));
	}

	

}
