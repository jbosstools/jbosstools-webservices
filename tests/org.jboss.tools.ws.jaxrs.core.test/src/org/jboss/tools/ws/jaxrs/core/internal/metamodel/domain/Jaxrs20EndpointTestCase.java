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

import static org.eclipse.jdt.core.IJavaElementDelta.ADDED;
import static org.eclipse.jdt.core.IJavaElementDelta.CHANGED;
import static org.eclipse.jdt.core.IJavaElementDelta.REMOVED;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Collection;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils;
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementCategory;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsEndpointDelta;
import org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author xcoulon
 * 
 */
public class Jaxrs20EndpointTestCase {

	private static final boolean WORKING_COPY = true;

	@ClassRule
	public static WorkspaceSetupRule workspaceSetupRule = new WorkspaceSetupRule(
			"org.jboss.tools.ws.jaxrs.tests.sampleproject2");

	@Rule
	public JaxrsMetamodelMonitor metamodelMonitor = new JaxrsMetamodelMonitor(
			"org.jboss.tools.ws.jaxrs.tests.sampleproject2", false);

	private JaxrsMetamodel metamodel = null;
	
	private IJavaProject javaProject = null;

	@Before
	public void setup() throws CoreException {
		metamodel = metamodelMonitor.getMetamodel();
		javaProject = metamodel.getJavaProject();
	}

	@Test
	public void shouldAddQueryParamInAllEndpointsWhenAddingAnnotatedParameterAggregatorField() throws CoreException {
		// pre-condition
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.CarResource", "org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		final JaxrsResource carResource = (JaxrsResource) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.CarResource", EnumElementCategory.RESOURCE);
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(carResource);
		assertThat(endpoints.size(), equalTo(2));
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation: now, add an annotated field
		final IField beanParamField = JavaElementsUtils.createField(carResource.getJavaElement(), "@BeanParam private CarParameterAggregator carDTO;\n", WORKING_COPY);
		metamodelMonitor.processEvent(beanParamField, ADDED);
		
		// verifications
		final Collection<JaxrsEndpoint> modifiedEndpoints = metamodel.findEndpoints(carResource);
		assertThat(modifiedEndpoints.size(), equalTo(2));
		for(JaxrsEndpoint endpoint : modifiedEndpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("color={String:\"color!\"}"));
			assertThat(endpoint.getUriPathTemplate(), containsString("shape={String:\"shape!\"}"));
		}
	}

	@Test
	public void shouldAddQueryParamInAllEndpointsWhenAddingBeanParamAnnotationOnParameterAggregatorField() throws CoreException {
		// pre-condition
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.CarResource", "org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		final JaxrsResource carResource = (JaxrsResource) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.CarResource", EnumElementCategory.RESOURCE);
		final IField beanParamField = JavaElementsUtils.createField(carResource.getJavaElement(), "private CarParameterAggregator carDTO;\n", WORKING_COPY);
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(carResource);
		assertThat(endpoints.size(), equalTo(2));
		for (JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), not(containsString("color={String:\"color!\"}")));
			assertThat(endpoint.getUriPathTemplate(), not(containsString("shape={String:\"shape!\"}")));
		}
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation: add an annotation on the existing field
		JavaElementsUtils.addFieldAnnotation(beanParamField, "@BeanParam", WORKING_COPY);
		metamodelMonitor.processEvent(beanParamField, CHANGED);
		
		// verifications
		final Collection<JaxrsEndpoint> modifiedEndpoints = metamodel.findEndpoints(beanParamField);
		assertThat(modifiedEndpoints.size(), equalTo(2));
		for (JaxrsEndpoint endpoint : modifiedEndpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("color={String:\"color!\"}"));
			assertThat(endpoint.getUriPathTemplate(), containsString("shape={String:\"shape!\"}"));
		}
	}

	@Test
	public void shouldAddQueryParamInAllEndpointsWhenAddingQueryParamFieldInParameterAggregator() throws CoreException {
		// pre-condition
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.CarResource", "org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		final JaxrsResource carResource = (JaxrsResource) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.CarResource", EnumElementCategory.RESOURCE);
		final IField beanParamField = JavaElementsUtils.createField(carResource.getJavaElement(), "@BeanParam private CarParameterAggregator carDTO;\n", WORKING_COPY);
		metamodelMonitor.processEvent(beanParamField, ADDED);
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(beanParamField);
		assertThat(endpoints.size(), equalTo(2));
		for(JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("color={String:\"color!\"}"));
			assertThat(endpoint.getUriPathTemplate(), containsString("shape={String:\"shape!\"}"));
		}
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation: modify the field annotation in the parameter aggregator type
		final IType parameterAggregatorType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		final IField modelField = JavaElementsUtils.createField(parameterAggregatorType, "@QueryParam(\"model\") private String model;\n",true);
		metamodelMonitor.processEvent(modelField, ADDED);
		
		// verifications
		final Collection<JaxrsEndpoint> modifiedEndpoints = metamodel.findEndpoints(beanParamField);
		assertThat(modifiedEndpoints.size(), equalTo(2));
		for(JaxrsEndpoint endpoint : modifiedEndpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("color={String:\"color!\"}"));
			assertThat(endpoint.getUriPathTemplate(), containsString("shape={String:\"shape!\"}"));
			assertThat(endpoint.getUriPathTemplate(), containsString("model={String}"));
		}
	}
	
	@Test
	public void shouldChangeQueryParamInAllEndpointsWhenChangingAnnotatedParameterAggregatorField() throws CoreException, IOException {
		// pre-condition
		final IResource resource = JavaElementsUtils.getResource("org.jboss.tools.ws.jaxrs.sample.services.CarResource", javaProject);
		ResourcesUtils.replaceContent(resource, "//PlaceHolder", "@BeanParam private CarParameterAggregator carDTO;\n");
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.CarResource", "org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		final JaxrsResource carResource = (JaxrsResource) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.CarResource", EnumElementCategory.RESOURCE);
		final IField beanParamField = JavaElementsUtils.getField(carResource.getJavaElement(), "carDTO");
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(beanParamField);
		assertThat(endpoints.size(), equalTo(2));
		for(JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("color={String:\"color!\"}"));
			assertThat(endpoint.getUriPathTemplate(), containsString("shape={String:\"shape!\"}"));
		}
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation: modify the field annotation in the parameter aggregator type
		final IType parameterAggregatorType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		JavaElementsUtils.replaceFirstOccurrenceOfCode(parameterAggregatorType.getCompilationUnit(), "@QueryParam(\"color\")", "@QueryParam(\"model\")", true);
		metamodelMonitor.processEvent(JavaElementsUtils.getField(parameterAggregatorType, "color"), CHANGED);
		
		// verifications
		final Collection<JaxrsEndpoint> modifiedEndpoints = metamodel.findEndpoints(beanParamField);
		assertThat(modifiedEndpoints.size(), equalTo(2));
		for(JaxrsEndpoint endpoint : modifiedEndpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("model={String:\"color!\"}"));
			assertThat(endpoint.getUriPathTemplate(), containsString("shape={String:\"shape!\"}"));
		}
	}
	
	@Test
	public void shouldRemoveQueryParamInAllEndpointsWhenRemovingQueryParamFieldInParameterAggregator() throws CoreException, IOException {
		// pre-condition
		final IResource resource = JavaElementsUtils.getResource("org.jboss.tools.ws.jaxrs.sample.services.CarResource", javaProject);
		ResourcesUtils.replaceContent(resource, "//PlaceHolder", "@BeanParam private CarParameterAggregator carDTO;\n");
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.CarResource", "org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		final JaxrsResource carResource = (JaxrsResource) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.CarResource", EnumElementCategory.RESOURCE);
		final IField beanParamField = JavaElementsUtils.getField(carResource.getJavaElement(), "carDTO");
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(beanParamField);
		assertThat(endpoints.size(), equalTo(2));
		for(JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("color={String:\"color!\"}"));
			assertThat(endpoint.getUriPathTemplate(), containsString("shape={String:\"shape!\"}"));
		}
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation: modify the field annotation in the parameter aggregator type
		final IType parameterAggregatorType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		final IField parameterField = JavaElementsUtils.getField(parameterAggregatorType, "color");
		final IField removedField = JavaElementsUtils.removeField(parameterField, WORKING_COPY);
		metamodelMonitor.processEvent(removedField, REMOVED);
		
		// verifications
		final Collection<JaxrsEndpoint> modifiedEndpoints = metamodel.findEndpoints(beanParamField);
		assertThat(modifiedEndpoints.size(), equalTo(2));
		for(JaxrsEndpoint endpoint : modifiedEndpoints) {
			assertThat(endpoint.getUriPathTemplate(), not(containsString("color={String:\"color!\"}")));
			assertThat(endpoint.getUriPathTemplate(), containsString("shape={String:\"shape!\"}"));
		}
	}
	
	@Test
	public void shouldRemoveQueryParamInAllEndpointsWhenRemovingAnnotatedParameterAggregatorField() throws CoreException, IOException {
		// pre-condition
		final IResource resource = JavaElementsUtils.getResource("org.jboss.tools.ws.jaxrs.sample.services.CarResource", javaProject);
		ResourcesUtils.replaceContent(resource, "//PlaceHolder", "@BeanParam private CarParameterAggregator carDTO;\n");
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.CarResource", "org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		final JaxrsResource carResource = (JaxrsResource) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.CarResource", EnumElementCategory.RESOURCE);
		final IField beanParamField = JavaElementsUtils.getField(carResource.getJavaElement(), "carDTO");
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(beanParamField);
		assertThat(endpoints.size(), equalTo(2));
		for (JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("color={String:\"color!\"}"));
			assertThat(endpoint.getUriPathTemplate(), containsString("shape={String:\"shape!\"}"));
		}
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation: remove the parameter aggregator field 
		final IField removedField = JavaElementsUtils.removeField(beanParamField, WORKING_COPY);
		metamodelMonitor.processEvent(removedField, REMOVED);
		
		// verifications
		final Collection<JaxrsEndpoint> modifiedEndpoints = metamodel.findEndpoints(carResource);
		assertThat(modifiedEndpoints.size(), equalTo(2));
		for (JaxrsEndpoint endpoint : modifiedEndpoints) {
			assertThat(endpoint.getUriPathTemplate(), not(containsString("color={String:\"color!\"}")));
			assertThat(endpoint.getUriPathTemplate(), not(containsString("shape={String:\"shape!\"}")));
		}
	}

	@Test
	public void shouldRemoveQueryParamInAllEndpointsWhenRemovingBeanParamAnnotationOnResourceField() throws CoreException, IOException {
		// pre-condition
		final IResource resource = JavaElementsUtils.getResource("org.jboss.tools.ws.jaxrs.sample.services.CarResource", javaProject);
		ResourcesUtils.replaceContent(resource, "//PlaceHolder", "@BeanParam private CarParameterAggregator carDTO;\n");
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.CarResource", "org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		final JaxrsResource carResource = (JaxrsResource) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.CarResource", EnumElementCategory.RESOURCE);
		final IField beanParamField = JavaElementsUtils.getField(carResource.getJavaElement(), "carDTO");
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(beanParamField);
		assertThat(endpoints.size(), equalTo(2));
		for (JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("color={String:\"color!\"}"));
			assertThat(endpoint.getUriPathTemplate(), containsString("shape={String:\"shape!\"}"));
		}
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation: remove the @BeanParam annotation on the parameter aggregator field (ie, JaxrsResourceField gets removed)
		JavaElementsUtils.removeFieldAnnotation(beanParamField, "@BeanParam", WORKING_COPY);
		metamodelMonitor.processEvent(beanParamField, CHANGED);

		// verifications
		final Collection<JaxrsEndpoint> modifiedEndpoints = metamodel.findEndpoints(carResource);
		assertThat(modifiedEndpoints.size(), equalTo(2));
		for (JaxrsEndpoint endpoint : modifiedEndpoints) {
			assertThat(endpoint.getUriPathTemplate(), not(containsString("color={String:\"color!\"}")));
			assertThat(endpoint.getUriPathTemplate(), not(containsString("shape={String:\"shape!\"}")));
		}
	}
	
	@Test
	public void shouldAddQueryParamInAllEndpointsWhenAddingAnnotatedParameterAggregatorProperty() throws CoreException {
		// pre-condition
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.CarResource", "org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		final JaxrsResource carResource = (JaxrsResource) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.CarResource", EnumElementCategory.RESOURCE);
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(carResource);
		assertThat(endpoints.size(), equalTo(2));
		for (JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), not(containsString("color={String:\"color!\"}")));
			assertThat(endpoint.getUriPathTemplate(), not(containsString("shape={String:\"shape!\"}")));
		}
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation: add an annotated method in type
		final IMethod createdMethod = JavaElementsUtils.createMethod(carResource.getJavaElement(),
				"@BeanParam public void setCarParameterAggregator(CarParameterAggregator carDTO) {}\n", WORKING_COPY);
		metamodelMonitor.processEvent(createdMethod, ADDED);

		// verifications
		final Collection<JaxrsEndpoint> modifiedEndpoints = metamodel.findEndpoints(createdMethod);
		assertThat(modifiedEndpoints.size(), equalTo(2));
		for (JaxrsEndpoint endpoint : modifiedEndpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("color={String:\"color!\"}"));
			assertThat(endpoint.getUriPathTemplate(), containsString("shape={String:\"shape!\"}"));
		}
	}

	@Test
	public void shouldAddQueryParamInAllEndpointsWhenAddingBeanParamAnnotationOnParameterAggregatorProperty() throws CoreException {
		// pre-condition
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.CarResource", "org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		final JaxrsResource carResource = (JaxrsResource) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.CarResource", EnumElementCategory.RESOURCE);
		final IMethod beanParamMethod = JavaElementsUtils.createMethod(carResource.getJavaElement(), "public void setCarParameterAggregator(CarParameterAggregator carDTO) {}\n", WORKING_COPY);
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(carResource);
		assertThat(endpoints.size(), equalTo(2));
		for (JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), not(containsString("color={String:\"color!\"}")));
			assertThat(endpoint.getUriPathTemplate(), not(containsString("shape={String:\"shape!\"}")));
		}
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation: add an annotation on the existing property method
		JavaElementsUtils.addMethodAnnotation(beanParamMethod, "@BeanParam", WORKING_COPY);
		metamodelMonitor.processEvent(beanParamMethod, CHANGED);

		// verifications
		final Collection<JaxrsEndpoint> modifiedEndpoints = metamodel.findEndpoints(beanParamMethod);
		assertThat(modifiedEndpoints.size(), equalTo(2));
		for (JaxrsEndpoint endpoint : modifiedEndpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("color={String:\"color!\"}"));
			assertThat(endpoint.getUriPathTemplate(), containsString("shape={String:\"shape!\"}"));
		}
	}
	
	@Test
	public void shouldChangeQueryParamInAllEndpointsWhenChangingParameterAggregatorProperty() throws CoreException {
		// pre-condition
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.CarResource", "org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		final JaxrsResource carResource = (JaxrsResource) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.CarResource", EnumElementCategory.RESOURCE);
		final IMethod beanParamMethod = JavaElementsUtils.createMethod(carResource.getJavaElement(), "@BeanParam public void setCarParameterAggregator(CarParameterAggregator carDTO) {}\n", WORKING_COPY);
		metamodelMonitor.processEvent(beanParamMethod, ADDED);
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(beanParamMethod);
		assertThat(endpoints.size(), equalTo(2));
		for(JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("color={String:\"color!\"}"));
			assertThat(endpoint.getUriPathTemplate(), containsString("shape={String:\"shape!\"}"));
		}
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation: modify the annotation in the parameter aggregator property
		final IType parameterAggregatorType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		JavaElementsUtils.replaceFirstOccurrenceOfCode(parameterAggregatorType.getCompilationUnit(), "@QueryParam(\"shape\")", "@QueryParam(\"model\")", true);
		metamodelMonitor.processEvent(JavaElementsUtils.getMethod(parameterAggregatorType, "setShape"), CHANGED);

		// verifications
		final Collection<JaxrsEndpoint> modifiedEndpoints = metamodel.findEndpoints(beanParamMethod);
		assertThat(modifiedEndpoints.size(), equalTo(2));
		for(JaxrsEndpoint endpoint : modifiedEndpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("color={String:\"color!\"}"));
			assertThat(endpoint.getUriPathTemplate(), containsString("model={String:\"shape!\"}"));
		}
	}
	
	@Test
	public void shouldRemoveQueryParamInAllEndpointsWhenRemovingAnnotatedParameterAggregatorProperty() throws CoreException {
		// pre-condition
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.CarResource", "org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		final JaxrsResource carResource = (JaxrsResource) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.CarResource", EnumElementCategory.RESOURCE);
		final IMethod beanParamMethod = JavaElementsUtils.createMethod(carResource.getJavaElement(), "@BeanParam public void setCarParameterAggregator(CarParameterAggregator carDTO) {}\n", WORKING_COPY);
		metamodelMonitor.processEvent(beanParamMethod, ADDED);
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(beanParamMethod);
		assertThat(endpoints.size(), equalTo(2));
		for (JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("color={String:\"color!\"}"));
			assertThat(endpoint.getUriPathTemplate(), containsString("shape={String:\"shape!\"}"));
		}
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation: remove the @BeanParam annotation on the parameter aggregator field 
		JavaElementsUtils.replaceFirstOccurrenceOfCode(carResource.getJavaElement().getCompilationUnit(),
				"@BeanParam public void setCarParameterAggregator", "public void setCarParameterAggregator", true);
		metamodelMonitor.processEvent(beanParamMethod, REMOVED);

		// verifications
		final Collection<JaxrsEndpoint> modifiedEndpoints = metamodel.findEndpoints(carResource);
		assertThat(modifiedEndpoints.size(), equalTo(2));
		for (JaxrsEndpoint endpoint : modifiedEndpoints) {
			assertThat(endpoint.getUriPathTemplate(), not(containsString("color={String:\"color!\"}")));
			assertThat(endpoint.getUriPathTemplate(), not(containsString("shape={String:\"shape!\"}")));
		}
	}

	@Test
	public void shouldRemoveQueryParamInAllEndpointsWhenRemovingBeanParamAnnotationOnParameterResourceProperty() throws CoreException {
		// pre-condition
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.CarResource", "org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		final JaxrsResource carResource = (JaxrsResource) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.CarResource", EnumElementCategory.RESOURCE);
		final IMethod beanParamMethod = JavaElementsUtils.createMethod(carResource.getJavaElement(), "@BeanParam public void setCarParameterAggregator(CarParameterAggregator carDTO) {}\n", WORKING_COPY);
		metamodelMonitor.processEvent(beanParamMethod, ADDED);
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(beanParamMethod);
		assertThat(endpoints.size(), equalTo(2));
		for (JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("color={String:\"color!\"}"));
			assertThat(endpoint.getUriPathTemplate(), containsString("shape={String:\"shape!\"}"));
		}
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation: remove the @BeanParam annotation on the parameter aggregator field 
		JavaElementsUtils.replaceFirstOccurrenceOfCode(carResource.getJavaElement().getCompilationUnit(),
				"@BeanParam public void setCarParameterAggregator(CarParameterAggregator carDTO) {}", "", true);
		metamodelMonitor.processEvent(beanParamMethod, CHANGED);

		// verifications
		final Collection<JaxrsEndpoint> modifiedEndpoints = metamodel.findEndpoints(carResource);
		assertThat(modifiedEndpoints.size(), equalTo(2));
		for (JaxrsEndpoint endpoint : modifiedEndpoints) {
			assertThat(endpoint.getUriPathTemplate(), not(containsString("color={String:\"color!\"}")));
			assertThat(endpoint.getUriPathTemplate(), not(containsString("shape={String:\"shape!\"}")));
		}
	}

	@Test
	public void shouldAddQueryParamInSingleEndpointWhenAddingAnnotatedParameterAggregatorPropertyArgument() throws CoreException {
		// pre-condition
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.CarResource", "org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		final JaxrsResource carResource = (JaxrsResource) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.CarResource", EnumElementCategory.RESOURCE);
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(carResource);
		assertThat(endpoints.size(), equalTo(2));
		for (JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), not(containsString("color={String:\"color!\"}")));
			assertThat(endpoint.getUriPathTemplate(), not(containsString("shape={String:\"shape!\"}")));
		}
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation: add an annotated method parameter
		final IMethod javaMethod = JavaElementsUtils.getMethod(carResource.getJavaElement(), "update");
		metamodelMonitor.processEvent(javaMethod, REMOVED);
		final IMethod replacementJavaMethod = JavaElementsUtils.addMethodParameter(javaMethod, "@BeanParam CarParameterAggregator carDTO", WORKING_COPY);
		metamodelMonitor.processEvent(replacementJavaMethod, ADDED);

		// verifications
		final Collection<JaxrsEndpoint> modifiedEndpoints = metamodel.findEndpoints(replacementJavaMethod);
		assertThat(modifiedEndpoints.size(), equalTo(1));
		for (JaxrsEndpoint endpoint : modifiedEndpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("color={String:\"color!\"}"));
			assertThat(endpoint.getUriPathTemplate(), containsString("shape={String:\"shape!\"}"));
		}
		final Collection<JaxrsEndpoint> unmodifiedEndpoints = metamodel.findEndpoints(JavaElementsUtils.getMethod(carResource.getJavaElement(), "findById"));
		assertThat(unmodifiedEndpoints.size(), equalTo(1));
		for (JaxrsEndpoint endpoint : unmodifiedEndpoints) {
			assertThat(endpoint.getUriPathTemplate(), not(containsString("color={String:\"color!\"}")));
			assertThat(endpoint.getUriPathTemplate(), not(containsString("shape={String:\"shape!\"}")));
		}
	}

	@Test
	public void shouldAddQueryParamInSingleEndpointWhenAddingBeanParamAnnotationOnParameterAggregatorPropertyArgument() throws CoreException {
		// pre-condition
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.CarResource", "org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		final JaxrsResource carResource = (JaxrsResource) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.CarResource", EnumElementCategory.RESOURCE);
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(carResource);
		assertThat(endpoints.size(), equalTo(2));
		for (JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), not(containsString("color={String:\"color!\"}")));
			assertThat(endpoint.getUriPathTemplate(), not(containsString("shape={String:\"shape!\"}")));
		}
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation: add an annotated method argument in 'update()'
		final IMethod javaMethod = JavaElementsUtils.getMethod(carResource.getJavaElement(), "update");
		metamodelMonitor.processEvent(javaMethod, REMOVED);
		JavaElementsUtils.replaceFirstOccurrenceOfCode(carResource.getJavaElement().getCompilationUnit(), "update(final CarParameterAggregator car)", "update(@BeanParam final CarParameterAggregator carDTO)", WORKING_COPY);
		final IMethod replacementJavaMethod = JavaElementsUtils.getMethod(carResource.getJavaElement(), "update");
		metamodelMonitor.processEvent(replacementJavaMethod, ADDED);
		
		// verifications: 1 endpoint removed and another one added
		final Collection<JaxrsEndpointDelta> modifiedEndpoints = metamodelMonitor.getEndpointChanges();
		assertThat(modifiedEndpoints.size(), equalTo(2));
		for (JaxrsEndpointDelta endpointDelta : modifiedEndpoints) {
			if(endpointDelta.getKind() == ADDED) {
				assertThat(endpointDelta.getEndpoint().getUriPathTemplate(), containsString("color={String:\"color!\"}"));
				assertThat(endpointDelta.getEndpoint().getUriPathTemplate(), containsString("shape={String:\"shape!\"}"));
			}
		}
		final Collection<JaxrsEndpoint> unmodifiedEndpoints = metamodel.findEndpoints(JavaElementsUtils.getMethod(carResource.getJavaElement(), "findById"));
		assertThat(unmodifiedEndpoints.size(), equalTo(1));
		for (JaxrsEndpoint endpoint : unmodifiedEndpoints) {
			assertThat(endpoint.getUriPathTemplate(), not(containsString("color={String:\"color!\"}")));
			assertThat(endpoint.getUriPathTemplate(), not(containsString("shape={String:\"shape!\"}")));
		}
	}
	
	@Test
	public void shouldChangeQueryParamInSingleEndpointWhenChangingParameterAggregatorPropertyArgument() throws CoreException {
		// pre-condition
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.CarResource", "org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		final JaxrsResource carResource = (JaxrsResource) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.CarResource", EnumElementCategory.RESOURCE);
		final IMethod javaMethod = JavaElementsUtils.getMethod(carResource.getJavaElement(), "update");
		metamodelMonitor.processEvent(javaMethod, REMOVED);
		final IMethod replacedJavaMethod = JavaElementsUtils.addMethodParameter(javaMethod, "@BeanParam CarParameterAggregator carDTO", WORKING_COPY);
		metamodelMonitor.processEvent(replacedJavaMethod, ADDED);
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(replacedJavaMethod);
		assertThat(endpoints.size(), equalTo(1));
		for(JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("color={String:\"color!\"}"));
			assertThat(endpoint.getUriPathTemplate(), containsString("shape={String:\"shape!\"}"));
		}
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation: modify the method annotation in the parameter aggregator type
		final IType parameterAggregatorType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		JavaElementsUtils.replaceFirstOccurrenceOfCode(parameterAggregatorType.getCompilationUnit(), "@QueryParam(\"shape\")", "@QueryParam(\"model\")", true);
		metamodelMonitor.processEvent(JavaElementsUtils.getMethod(parameterAggregatorType, "setShape"), CHANGED);

		// verifications
		final Collection<JaxrsEndpoint> modifiedEndpoints = metamodel.findEndpoints(replacedJavaMethod);
		assertThat(modifiedEndpoints.size(), equalTo(1));
		for(JaxrsEndpoint endpoint : modifiedEndpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("color={String:\"color!\"}"));
			assertThat(endpoint.getUriPathTemplate(), containsString("model={String:\"shape!\"}"));
		}
		final Collection<JaxrsEndpoint> unmodifiedEndpoints = metamodel.findEndpoints(JavaElementsUtils.getMethod(carResource.getJavaElement(), "findById"));
		assertThat(unmodifiedEndpoints.size(), equalTo(1));
		for (JaxrsEndpoint endpoint : unmodifiedEndpoints) {
			assertThat(endpoint.getUriPathTemplate(), not(containsString("color={String:\"color!\"}")));
			assertThat(endpoint.getUriPathTemplate(), not(containsString("shape={String:\"shape!\"}")));
		}
	}
	
	@Test
	public void shouldRemoveQueryParamInSingleEndpointWhenRemovingAnnotatedParameterAggregatorPropertyArgument() throws CoreException {
		// pre-condition
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.CarResource", "org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		final JaxrsResource carResource = (JaxrsResource) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.CarResource", EnumElementCategory.RESOURCE);
		final IMethod javaMethod = JavaElementsUtils.getMethod(carResource.getJavaElement(), "update");
		metamodelMonitor.processEvent(javaMethod, REMOVED);
		final IMethod replacedJavaMethod = JavaElementsUtils.addMethodParameter(javaMethod, "@BeanParam CarParameterAggregator carDTO", WORKING_COPY);
		metamodelMonitor.processEvent(replacedJavaMethod, ADDED);
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(replacedJavaMethod);
		assertThat(endpoints.size(), equalTo(1));
		for (JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("color={String:\"color!\"}"));
			assertThat(endpoint.getUriPathTemplate(), containsString("shape={String:\"shape!\"}"));
		}
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation: remove the @BeanParam annotation on the parameter aggregator field 
		metamodelMonitor.processEvent(replacedJavaMethod, REMOVED);
		JavaElementsUtils.replaceFirstOccurrenceOfCode(carResource.getJavaElement().getCompilationUnit(),
				"@BeanParam CarParameterAggregator carDTO", "", true);
		final IMethod restoredJavaMethod = JavaElementsUtils.getMethod(carResource.getJavaElement(), "update");
		metamodelMonitor.processEvent(restoredJavaMethod, CHANGED);
		
		// verifications
		final Collection<JaxrsEndpoint> modifiedEndpoints = metamodel.findEndpoints(restoredJavaMethod);
		assertThat(modifiedEndpoints.size(), equalTo(1));
		for (JaxrsEndpoint endpoint : modifiedEndpoints) {
			assertThat(endpoint.getUriPathTemplate(), not(containsString("color={String:\"color!\"}")));
			assertThat(endpoint.getUriPathTemplate(), not(containsString("shape={String:\"shape!\"}")));
		}
		final IJaxrsElement otherJavaMethod = metamodel.findElement(JavaElementsUtils.getMethod(carResource.getJavaElement(), "findById"));
		final Collection<JaxrsEndpoint> unmodifiedEndpoints = metamodel.findEndpoints(otherJavaMethod);
		assertThat(unmodifiedEndpoints.size(), equalTo(1));
		for (JaxrsEndpoint endpoint : unmodifiedEndpoints) {
			assertThat(endpoint.getUriPathTemplate(), not(containsString("color={String:\"color!\"}")));
			assertThat(endpoint.getUriPathTemplate(), not(containsString("shape={String:\"shape!\"}")));
		}
	}
	
	@Test
	public void shouldRemoveQueryParamSingleEndpointWhenRemovingBeanParamAnnotationOnParameterAggregatorPropertyArgument() throws CoreException, IOException {
		// pre-condition
		final IResource resource = JavaElementsUtils.getResource("org.jboss.tools.ws.jaxrs.sample.services.CarResource", javaProject);
		ResourcesUtils.replaceContent(resource, "findById(", "findById(@BeanParam CarParameterAggregator carDTO, ");
		// the order of the elements does not matter: the resource methods will be updated when the ParameterAggregator will be created in the metamodel.
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.CarResource", "org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		final JaxrsResource carResource = (JaxrsResource) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.CarResource", EnumElementCategory.RESOURCE);
		final IMethod modifiedMethod = JavaElementsUtils.getMethod(carResource.getJavaElement(), "findById");
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(modifiedMethod);
		assertThat(endpoints.size(), equalTo(1));
		for (JaxrsEndpoint endpoint : endpoints) {
			assertThat(endpoint.getUriPathTemplate(), containsString("color={String:\"color!\"}"));
			assertThat(endpoint.getUriPathTemplate(), containsString("shape={String:\"shape!\"}"));
		}
		metamodelMonitor.resetElementChangesNotifications();

		// operation: remove the @BeanParam annotation on the parameter aggregator argument of the 'findById' method. 
		JavaElementsUtils.replaceFirstOccurrenceOfCode(carResource.getJavaElement().getCompilationUnit(),
				"@BeanParam CarParameterAggregator carDTO", "CarParameterAggregator carDTO", true);
		metamodelMonitor.processEvent(modifiedMethod, CHANGED);

		// verifications
		final Collection<JaxrsEndpoint> modifiedEndpoints = metamodel.findEndpoints(modifiedMethod);
		assertThat(modifiedEndpoints.size(), equalTo(1));
		for (JaxrsEndpoint endpoint : modifiedEndpoints) {
			assertThat(endpoint.getUriPathTemplate(), not(containsString("color={String:\"color!\"}")));
			assertThat(endpoint.getUriPathTemplate(), not(containsString("shape={String:\"shape!\"}")));
		}
		final Collection<JaxrsEndpoint> unmodifiedEndpoints = metamodel.findEndpoints(modifiedMethod);
		assertThat(unmodifiedEndpoints.size(), equalTo(1));
		for (JaxrsEndpoint endpoint : unmodifiedEndpoints) {
			assertThat(endpoint.getUriPathTemplate(), not(containsString("color={String:\"color!\"}")));
			assertThat(endpoint.getUriPathTemplate(), not(containsString("shape={String:\"shape!\"}")));
		}
	}
	
	@Test
	// @see JBIDE-17711
	public void shouldRetrievePathParamTypeInParameterAggregatorField() throws CoreException {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("MyPathParams.txt", "org.jboss.tools.ws.jaxrs.sample.services", "MyPathParams.java");
		metamodelMonitor.createCompilationUnit("MyPathParamsResource.txt", "org.jboss.tools.ws.jaxrs.sample.services", "MyPathParamsResource.java");
		metamodelMonitor.createParameterAggregator("org.jboss.tools.ws.jaxrs.sample.services.MyPathParams");
		final JaxrsResource resource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.MyPathParamsResource");
		// operation: retrieve the endpoint for the Resource Method
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(resource).iterator().next();
		// verification: check the URI template
		assertThat(endpoint.getUriPathTemplate(), equalTo("/test/{path:String}?query={String}"));
	}
	
	@Test
	// @see JBIDE-17711
	public void shouldRetrievePathParamTypeChangeInParameterAggregatorField() throws CoreException {
		// pre-conditions
		final ICompilationUnit paramAggregatorUnit = metamodelMonitor.createCompilationUnit("MyPathParams.txt", "org.jboss.tools.ws.jaxrs.sample.services", "MyPathParams.java");
		metamodelMonitor.createCompilationUnit("MyPathParamsResource.txt", "org.jboss.tools.ws.jaxrs.sample.services", "MyPathParamsResource.java");
		final JaxrsParameterAggregator parameterAggregator = metamodelMonitor.createParameterAggregator("org.jboss.tools.ws.jaxrs.sample.services.MyPathParams");
		final JaxrsResource resource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.MyPathParamsResource");
		// operation 1: retrieve the endpoint for the Resource Method
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(resource).iterator().next();
		// verification 1: check the URI template
		assertThat(endpoint.getUriPathTemplate(), equalTo("/test/{path:String}?query={String}"));
		// operation 2: update the field annotated with @PathParam in the Parameter Aggregator
		final IField removedField = JavaElementsUtils.getField(parameterAggregator.getJavaElement(), "path");
		ResourcesUtils.replaceAllOccurrencesOfCode(paramAggregatorUnit, "String path", "Integer path", true);
		final IField addedField = JavaElementsUtils.getField(parameterAggregator.getJavaElement(), "path");
		metamodelMonitor.processEvent(removedField, REMOVED);
		metamodelMonitor.processEvent(addedField, ADDED);
		final JaxrsEndpoint updatedEndpoint = metamodel.findEndpoints(resource).iterator().next();
		// verification 1: check the URI template
		assertThat(updatedEndpoint.getUriPathTemplate(), equalTo("/test/{path:Integer}?query={String}"));
	}

	@Test
	// @see JBIDE-17711
	public void shouldRetrievePathParamTypeInParameterAggregatorProperty() throws CoreException {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("MyPathParams.txt", "org.jboss.tools.ws.jaxrs.sample.services", "MyPathParams.java");
		metamodelMonitor.createCompilationUnit("MyPathParamsResource.txt", "org.jboss.tools.ws.jaxrs.sample.services", "MyPathParamsResource.java");
		metamodelMonitor.createParameterAggregator("org.jboss.tools.ws.jaxrs.sample.services.MyPathParams");
		final JaxrsResource resource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.MyPathParamsResource");
		// operation: retrieve the endpoint for the Resource Method
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(resource).iterator().next();
		// verification: check the URI template
		assertThat(endpoint.getUriPathTemplate(), equalTo("/test/{path:String}?query={String}"));
	}
	
	@Test
	// @see JBIDE-17711
	public void shouldRetrievePathParamTypeChangeInParameterAggregatorProperty() throws CoreException {
		// pre-conditions
		final ICompilationUnit paramAggregatorUnit = metamodelMonitor.createCompilationUnit("MyPathParams.txt", "org.jboss.tools.ws.jaxrs.sample.services", "MyPathParams.java");
		ResourcesUtils.replaceAllOccurrencesOfCode(paramAggregatorUnit, "@PathParam(\"path\")", "", true);
		ResourcesUtils.replaceAllOccurrencesOfCode(paramAggregatorUnit, "// PLACEHOLDER", "@PathParam(\"path\")", true);
		metamodelMonitor.createCompilationUnit("MyPathParamsResource.txt", "org.jboss.tools.ws.jaxrs.sample.services", "MyPathParamsResource.java");
		final JaxrsParameterAggregator parameterAggregator = metamodelMonitor.createParameterAggregator("org.jboss.tools.ws.jaxrs.sample.services.MyPathParams");
		final JaxrsResource resource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.MyPathParamsResource");
		// operation 1: retrieve the endpoint for the Resource Method
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(resource).iterator().next();
		// verification 1: check the URI template
		assertThat(endpoint.getUriPathTemplate(), equalTo("/test/{path:String}?query={String}"));
		// operation 2: update the field annotated with @PathParam in the Parameter Aggregator
		final IMethod removedProperty = JavaElementsUtils.getMethod(parameterAggregator.getJavaElement(), "setPath");
		ResourcesUtils.replaceAllOccurrencesOfCode(paramAggregatorUnit, "setPath(String ", "setPath(Integer ", true);
		final IMethod addedProperty = JavaElementsUtils.getMethod(parameterAggregator.getJavaElement(), "setPath");
		metamodelMonitor.processEvent(removedProperty, REMOVED);
		metamodelMonitor.processEvent(addedProperty, ADDED);
		final JaxrsEndpoint updatedEndpoint = metamodel.findEndpoints(resource).iterator().next();
		// verification 1: check the URI template
		assertThat(updatedEndpoint.getUriPathTemplate(), equalTo("/test/{path:Integer}?query={String}"));
	}
	
	@Test
	// @see JBIDE-17712
	public void shouldRetrieveMatrixParamTypeInParameterAggregatorField() throws CoreException {
		// pre-conditions
		final ICompilationUnit paramAggregatorUnit = metamodelMonitor.createCompilationUnit("MyPathParams.txt", "org.jboss.tools.ws.jaxrs.sample.services", "MyPathParams.java");
		ResourcesUtils.replaceAllOccurrencesOfCode(paramAggregatorUnit, "Query", "Matrix", true);
		ResourcesUtils.replaceAllOccurrencesOfCode(paramAggregatorUnit, "query", "matrix", true);
		metamodelMonitor.createCompilationUnit("MyPathParamsResource.txt", "org.jboss.tools.ws.jaxrs.sample.services", "MyPathParamsResource.java");
		metamodelMonitor.createParameterAggregator("org.jboss.tools.ws.jaxrs.sample.services.MyPathParams");
		final JaxrsResource resource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.MyPathParamsResource");
		// operation: retrieve the endpoint for the Resource Method
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(resource).iterator().next();
		// verification: check the URI template
		assertThat(endpoint.getUriPathTemplate(), equalTo("/test/{path:String};matrix={String}"));
	}
	
	@Test
	// @see JBIDE-17712
	public void shouldRetrieveMatrixParamTypeChangeInParameterAggregatorField() throws CoreException {
		// pre-conditions
		final ICompilationUnit paramAggregatorUnit = metamodelMonitor.createCompilationUnit("MyPathParams.txt", "org.jboss.tools.ws.jaxrs.sample.services", "MyPathParams.java");
		ResourcesUtils.replaceAllOccurrencesOfCode(paramAggregatorUnit, "Query", "Matrix", true);
		ResourcesUtils.replaceAllOccurrencesOfCode(paramAggregatorUnit, "query", "matrix", true);
		metamodelMonitor.createCompilationUnit("MyPathParamsResource.txt", "org.jboss.tools.ws.jaxrs.sample.services", "MyPathParamsResource.java");
		final JaxrsParameterAggregator parameterAggregator = metamodelMonitor.createParameterAggregator("org.jboss.tools.ws.jaxrs.sample.services.MyPathParams");
		final JaxrsResource resource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.MyPathParamsResource");
		// operation 1: retrieve the endpoint for the Resource Method
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(resource).iterator().next();
		// verification 1: check the URI template
		assertThat(endpoint.getUriPathTemplate(), equalTo("/test/{path:String};matrix={String}"));
		// operation 2: update the field annotated with @PathParam in the Parameter Aggregator
		final IField removedField = JavaElementsUtils.getField(parameterAggregator.getJavaElement(), "matrix");
		ResourcesUtils.replaceAllOccurrencesOfCode(paramAggregatorUnit, "String matrix", "Integer matrix", true);
		final IField addedField = JavaElementsUtils.getField(parameterAggregator.getJavaElement(), "matrix");
		metamodelMonitor.processEvent(removedField, REMOVED);
		metamodelMonitor.processEvent(addedField, ADDED);
		for(JaxrsParameterAggregatorField aggregatorField : parameterAggregator.getAllFields()) {
			metamodelMonitor.processEvent(aggregatorField.getJavaElement(), CHANGED);
		}
		final JaxrsEndpoint updatedEndpoint = metamodel.findEndpoints(resource).iterator().next();
		// verification 1: check the URI template
		assertThat(updatedEndpoint.getUriPathTemplate(), equalTo("/test/{path:String};matrix={Integer}"));
	}
	
	@Test
	// @see JBIDE-17712
	public void shouldRetrieveMatrixParamTypeChangeInParameterAggregatorProperty() throws CoreException {
		// pre-conditions
		final ICompilationUnit paramAggregatorUnit = metamodelMonitor.createCompilationUnit("MyPathParams.txt", "org.jboss.tools.ws.jaxrs.sample.services", "MyPathParams.java");
		ResourcesUtils.replaceAllOccurrencesOfCode(paramAggregatorUnit, "@QueryParam(\"query\")", "", true);
		ResourcesUtils.replaceAllOccurrencesOfCode(paramAggregatorUnit, "public void setQuery", "@MatrixParam(\"matrix\") public void setMatrix", true);
		metamodelMonitor.createCompilationUnit("MyPathParamsResource.txt", "org.jboss.tools.ws.jaxrs.sample.services", "MyPathParamsResource.java");
		final JaxrsParameterAggregator parameterAggregator = metamodelMonitor.createParameterAggregator("org.jboss.tools.ws.jaxrs.sample.services.MyPathParams");
		final JaxrsResource resource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.MyPathParamsResource");
		// operation 1: retrieve the endpoint for the Resource Method
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(resource).iterator().next();
		// verification 1: check the URI template
		assertThat(endpoint.getUriPathTemplate(), equalTo("/test/{path:String};matrix={String}"));
		// operation 2: update the field annotated with @PathParam in the Parameter Aggregator
		final IMethod removedProperty = JavaElementsUtils.getMethod(parameterAggregator.getJavaElement(), "setMatrix");
		ResourcesUtils.replaceAllOccurrencesOfCode(paramAggregatorUnit, "setMatrix(String ", "setMatrix(Integer ", true);
		final IMethod addedProperty = JavaElementsUtils.getMethod(parameterAggregator.getJavaElement(), "setMatrix");
		metamodelMonitor.processEvent(removedProperty, REMOVED);
		metamodelMonitor.processEvent(addedProperty, ADDED);
		// verification 2: check the URI template
		final JaxrsEndpoint updatedEndpoint = metamodel.findEndpoints(resource).iterator().next();
		assertThat(updatedEndpoint.getUriPathTemplate(), equalTo("/test/{path:String};matrix={Integer}"));
	}
	


	@Test
	// @see JBIDE-17712
	public void shouldRetrieveMatrixParamTypeInParameterAggregatorProperty() throws CoreException {
		// pre-conditions
		final ICompilationUnit paramAggregatorUnit = metamodelMonitor.createCompilationUnit("MyPathParams.txt", "org.jboss.tools.ws.jaxrs.sample.services", "MyPathParams.java");
		ResourcesUtils.replaceAllOccurrencesOfCode(paramAggregatorUnit, "Query", "Matrix", true);
		ResourcesUtils.replaceAllOccurrencesOfCode(paramAggregatorUnit, "query", "matrix", true);
		metamodelMonitor.createCompilationUnit("MyPathParamsResource.txt", "org.jboss.tools.ws.jaxrs.sample.services", "MyPathParamsResource.java");
		metamodelMonitor.createParameterAggregator("org.jboss.tools.ws.jaxrs.sample.services.MyPathParams");
		final JaxrsResource resource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.MyPathParamsResource");
		// operation: retrieve the endpoint for the Resource Method
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(resource).iterator().next();
		// verification: check the URI template
		assertThat(endpoint.getUriPathTemplate(), equalTo("/test/{path:String};matrix={String}"));
	}
	
	@Test
	// @see JBIDE-17712
	public void shouldRetrieveQueryParamTypeChangeInParameterAggregatorField() throws CoreException {
		// pre-conditions
		final ICompilationUnit paramAggregatorUnit = metamodelMonitor.createCompilationUnit("MyPathParams.txt", "org.jboss.tools.ws.jaxrs.sample.services", "MyPathParams.java");
		metamodelMonitor.createCompilationUnit("MyPathParamsResource.txt", "org.jboss.tools.ws.jaxrs.sample.services", "MyPathParamsResource.java");
		final JaxrsParameterAggregator parameterAggregator = metamodelMonitor.createParameterAggregator("org.jboss.tools.ws.jaxrs.sample.services.MyPathParams");
		final JaxrsResource resource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.MyPathParamsResource");
		// operation 1: retrieve the endpoint for the Resource Method
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(resource).iterator().next();
		// verification 1: check the URI template
		assertThat(endpoint.getUriPathTemplate(), equalTo("/test/{path:String}?query={String}"));
		// operation 2: update the field annotated with @PathParam in the Parameter Aggregator
		final IField removedField = JavaElementsUtils.getField(parameterAggregator.getJavaElement(), "query");
		ResourcesUtils.replaceAllOccurrencesOfCode(paramAggregatorUnit, "String query", "Integer query", true);
		final IField addedField = JavaElementsUtils.getField(parameterAggregator.getJavaElement(), "query");
		metamodelMonitor.processEvent(removedField, REMOVED);
		metamodelMonitor.processEvent(addedField, ADDED);
		// verification 2: check the URI template
		final JaxrsEndpoint updatedEndpoint = metamodel.findEndpoints(resource).iterator().next();
		assertThat(updatedEndpoint.getUriPathTemplate(), equalTo("/test/{path:String}?query={Integer}"));
	}

	@Test
	// @see JBIDE-17712
	public void shouldRetrieveQueryParamTypeChangeInParameterAggregatorProperty() throws CoreException {
		// pre-conditions
		final ICompilationUnit paramAggregatorUnit = metamodelMonitor.createCompilationUnit("MyPathParams.txt", "org.jboss.tools.ws.jaxrs.sample.services", "MyPathParams.java");
		ResourcesUtils.replaceAllOccurrencesOfCode(paramAggregatorUnit, "@QueryParam(\"query\")", "", true);
		ResourcesUtils.replaceAllOccurrencesOfCode(paramAggregatorUnit, "public void setQuery", "@QueryParam(\"query\") public void setQuery", true);
		metamodelMonitor.createCompilationUnit("MyPathParamsResource.txt", "org.jboss.tools.ws.jaxrs.sample.services", "MyPathParamsResource.java");
		final JaxrsParameterAggregator parameterAggregator = metamodelMonitor.createParameterAggregator("org.jboss.tools.ws.jaxrs.sample.services.MyPathParams");
		final JaxrsResource resource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.MyPathParamsResource");
		// operation 1: retrieve the endpoint for the Resource Method
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(resource).iterator().next();
		// verification 1: check the URI template
		assertThat(endpoint.getUriPathTemplate(), equalTo("/test/{path:String}?query={String}"));
		// operation 2: update the field annotated with @PathParam in the Parameter Aggregator
		final IMethod removedProperty = JavaElementsUtils.getMethod(parameterAggregator.getJavaElement(), "setQuery");
		ResourcesUtils.replaceAllOccurrencesOfCode(paramAggregatorUnit, "setQuery(String ", "setQuery(Integer ", true);
		final IMethod addedProperty = JavaElementsUtils.getMethod(parameterAggregator.getJavaElement(), "setQuery");
		metamodelMonitor.processEvent(removedProperty, REMOVED);
		metamodelMonitor.processEvent(addedProperty, ADDED);
		// verification 2: check the URI template
		final JaxrsEndpoint updatedEndpoint = metamodel.findEndpoints(resource).iterator().next();
		assertThat(updatedEndpoint.getUriPathTemplate(), equalTo("/test/{path:String}?query={Integer}"));
	}

	@Test
	// @see JBIDE-17663
	public void shouldAddParameterTypeInEndpointWhenAddingPathParamAnnotationOnField() throws CoreException {
		// pre-conditions
		final ICompilationUnit paramResourceUnit = metamodelMonitor.createCompilationUnit("MyPathParamsResource.txt", "org.jboss.tools.ws.jaxrs.sample.services",
				"MyPathParamsResource.java");
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.MyPathParamsResource");
		// operation 1: retrieve the endpoint for the Resource Method
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(resource).iterator().next();
		// verification 1: check the URI template
		assertThat(endpoint.getUriPathTemplate(), equalTo("/test/{path:.*}"));
		// operation 2: uncomment the @PathParam annotation on the 'path' field in the Parameter Aggregator
		ResourcesUtils.replaceAllOccurrencesOfCode(paramResourceUnit, "//PLACEHOLDER", "@PathParam(\"path\") private String path;", true);
		final IField pathField = JavaElementsUtils.getField(paramResourceUnit.findPrimaryType(), "path");
		metamodelMonitor.processEvent(JavaElementsUtils.getAnnotation(pathField, JaxrsClassnames.PATH_PARAM), ADDED);
		final JaxrsEndpoint updatedEndpoint = metamodel.findEndpoints(resource).iterator().next();
		// verification 2: check the URI template
		assertThat(updatedEndpoint.getUriPathTemplate(), equalTo("/test/{path:String}"));
	}
	
	@Test
	// @see JBIDE-17663
	public void shouldRemoveParameterTypeInEndpointWhenRemovingPathParamAnnotationOnField() throws CoreException {
		// pre-conditions
		final ICompilationUnit paramResourceUnit = metamodelMonitor.createCompilationUnit("MyPathParamsResource.txt", "org.jboss.tools.ws.jaxrs.sample.services",
				"MyPathParamsResource.java");
		ResourcesUtils.replaceAllOccurrencesOfCode(paramResourceUnit, "//PLACEHOLDER", "@PathParam(\"path\") private String path;", true);
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.MyPathParamsResource");
		// operation 1: retrieve the endpoint for the Resource Method
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(resource).iterator().next();
		// verification 1: check the URI template
		assertThat(endpoint.getUriPathTemplate(), equalTo("/test/{path:String}"));
		// operation 2: uncomment the @PathParam annotation on the 'path' field in the Parameter Aggregator
		final IField pathField = JavaElementsUtils.getField(paramResourceUnit.findPrimaryType(), "path");
		final Annotation pathParamAnnotation = JavaElementsUtils.getAnnotation(pathField, JaxrsClassnames.PATH_PARAM);
		ResourcesUtils.replaceAllOccurrencesOfCode(paramResourceUnit, "@PathParam(\"path\") private String path;", "private String path;", true);
		metamodelMonitor.processEvent(pathParamAnnotation, REMOVED);
		final JaxrsEndpoint updatedEndpoint = metamodel.findEndpoints(resource).iterator().next();
		// verification 2: check the URI template
		assertThat(updatedEndpoint.getUriPathTemplate(), equalTo("/test/{path:.*}"));
	}
	
	@Test
	// @see JBIDE-17663
	public void shouldAddParameterTypeInEndpointWhenAddingPathParamAnnotationOnBeanParamField() throws CoreException {
		// pre-conditions
		final ICompilationUnit paramAggregatorUnit = metamodelMonitor.createCompilationUnit("MyPathParams.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "MyPathParams.java");
		ResourcesUtils.replaceAllOccurrencesOfCode(paramAggregatorUnit, "@PathParam(\"path\")", "//@PathParam(\"path\")", true);
		ResourcesUtils.replaceAllOccurrencesOfCode(paramAggregatorUnit, "@QueryParam(\"query\")", "", true);
		metamodelMonitor.createCompilationUnit("MyPathParamsResource.txt", "org.jboss.tools.ws.jaxrs.sample.services",
				"MyPathParamsResource.java");
		metamodelMonitor.createParameterAggregator("org.jboss.tools.ws.jaxrs.sample.services.MyPathParams");
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.MyPathParamsResource");
		// operation 1: retrieve the endpoint for the Resource Method
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(resource).iterator().next();
		// verification 1: check the URI template
		assertThat(endpoint.getUriPathTemplate(), equalTo("/test/{path:.*}"));
		// operation 2: uncomment the @PathParam annotation on the 'path' field in the Parameter Aggregator
		ResourcesUtils.replaceAllOccurrencesOfCode(paramAggregatorUnit, "//@PathParam(\"path\")", "@PathParam(\"path\")", true);
		final IField pathField = JavaElementsUtils.getField(paramAggregatorUnit.findPrimaryType(), "path");
		metamodelMonitor.processEvent(JavaElementsUtils.getAnnotation(pathField, JaxrsClassnames.PATH_PARAM), ADDED);
		final JaxrsEndpoint updatedEndpoint = metamodel.findEndpoints(resource).iterator().next();
		// verification 2: check the URI template
		assertThat(updatedEndpoint.getUriPathTemplate(), equalTo("/test/{path:String}"));
	}
	
	@Test
	// @see JBIDE-17663
	public void shouldRemoveParameterTypeInEndpointWhenRemovingPathParamAnnotationOnBeanParamField() throws CoreException {
		// pre-conditions
		final ICompilationUnit paramAggregatorUnit = metamodelMonitor.createCompilationUnit("MyPathParams.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "MyPathParams.java");
		ResourcesUtils.replaceAllOccurrencesOfCode(paramAggregatorUnit, "@QueryParam(\"query\")", "", true);
		metamodelMonitor.createCompilationUnit("MyPathParamsResource.txt", "org.jboss.tools.ws.jaxrs.sample.services",
				"MyPathParamsResource.java");
		metamodelMonitor.createParameterAggregator("org.jboss.tools.ws.jaxrs.sample.services.MyPathParams");
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.MyPathParamsResource");
		// operation 1: retrieve the endpoint for the Resource Method
		final JaxrsEndpoint endpoint = metamodel.findEndpoints(resource).iterator().next();
		// verification 1: check the URI template
		assertThat(endpoint.getUriPathTemplate(), equalTo("/test/{path:String}"));
		// operation 2: comment out the @PathParam annotation on the 'path' field in the Parameter Aggregator
		final IField pathField = JavaElementsUtils.getField(paramAggregatorUnit.findPrimaryType(), "path");
		final Annotation annotation = JavaElementsUtils.getAnnotation(pathField, JaxrsClassnames.PATH_PARAM);
		ResourcesUtils.replaceAllOccurrencesOfCode(paramAggregatorUnit, "@PathParam(\"path\")", "//@PathParam(\"path\")", true);
		metamodelMonitor.processEvent(annotation, REMOVED);
		final JaxrsEndpoint updatedEndpoint = metamodel.findEndpoints(resource).iterator().next();
		// verification 2: check the URI template
		assertThat(updatedEndpoint.getUriPathTemplate(), equalTo("/test/{path:.*}"));
	}
	
}
