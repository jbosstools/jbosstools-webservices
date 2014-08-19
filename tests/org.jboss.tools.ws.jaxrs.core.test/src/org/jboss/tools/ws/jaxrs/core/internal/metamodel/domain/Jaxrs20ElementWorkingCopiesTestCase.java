/******************************************************************************* 
 * Copyright (c) 2008 - 2014 Red Hat, Inc. and others. 
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils;
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

public class Jaxrs20ElementWorkingCopiesTestCase {

	@ClassRule
	public static WorkspaceSetupRule workspaceSetupRule = new WorkspaceSetupRule(
			"org.jboss.tools.ws.jaxrs.tests.sampleproject2");
	@Rule
	public JaxrsMetamodelMonitor metamodelMonitor = new JaxrsMetamodelMonitor(
			"org.jboss.tools.ws.jaxrs.tests.sampleproject2", false);

	/**
	 * Checks that the given {@code workingCopy} is associated with the given {@code primaryCopy}
	 * @param workingCopy
	 * @param primaryCopy
	 */
	private void compare(final Object workingCopy, final Object primaryCopy) {
		if(workingCopy instanceof JaxrsBaseElement) {
			compare((JaxrsBaseElement)workingCopy, (JaxrsBaseElement)primaryCopy);
		} else if(workingCopy instanceof Annotation) {
			compare((Annotation)workingCopy, (Annotation)primaryCopy);
		}
	}
	/**
	 * Checks that the given {@code workingCopy} is associated with the given {@code primaryCopy}
	 * @param workingCopy
	 * @param primaryCopy
	 */
	private void compare(final Annotation workingCopy, final Annotation primaryCopy) {
		assertNotNull(workingCopy);
		assertNotNull(primaryCopy);
		assertEquals(workingCopy.getPrimaryCopy(), primaryCopy);
		assertNull(primaryCopy.getPrimaryCopy());
		assertTrue(workingCopy.isWorkingCopy());
		assertFalse(primaryCopy.isWorkingCopy());
	}
	
	/**
	 * Checks that the given {@code workingCopy} is associated with the given {@code primaryCopy}
	 * @param workingCopy
	 * @param primaryCopy
	 */
	private void compare(final JaxrsBaseElement workingCopy, JaxrsBaseElement primaryCopy) {
		assertNotNull(workingCopy);
		assertNotNull(primaryCopy);
		assertTrue(workingCopy.isWorkingCopy());
		assertEquals(workingCopy.getPrimaryCopy(), primaryCopy);
		assertNull(primaryCopy.getPrimaryCopy());
		assertFalse(primaryCopy.isWorkingCopy());
	}

	/**
	 * Compares the two given {@link Map}. They should contain the same entries
	 * and the same values (ie: {@code equals()} but not {@code ==})
	 * 
	 * @param workingCopyElements
	 * @param primaryCopyElements
	 */
	private <K, V> void compare(final Map<K, V> workingCopyElements,
			final Map<K, V> primaryCopyElements) {
		assertFalse(workingCopyElements == primaryCopyElements);
		assertThat(workingCopyElements.size(), equalTo(primaryCopyElements.size()));
		for (Entry<K, V> entry : workingCopyElements.entrySet()) {
			assertTrue(primaryCopyElements.containsKey(entry.getKey()));
			assertEquals(entry.getValue(), primaryCopyElements.get(entry.getKey()));
			if(entry.getValue() == null && primaryCopyElements.get(entry.getKey()) == null) {
				continue;
			}
			compare(entry.getValue(), primaryCopyElements.get(entry.getKey()));
		}
	}
	
	/**
	 * Compares the two given {@link List}. They should contain the same entries
	 * and the same values (ie: {@code equals()} but not {@code ==})

	 * @param workingCopyElements
	 * @param primaryCopyElements
	 */
	private <T extends JaxrsBaseElement> void compare(final List<T> workingCopyElements,
			final List<T> primaryCopyElements) {
		assertNotNull(workingCopyElements);
		assertNotNull(primaryCopyElements);
		assertThat(workingCopyElements.size(), equalTo(primaryCopyElements.size()));
		assertFalse(workingCopyElements == primaryCopyElements);
		for (T workingCopyElement : workingCopyElements) {
			assertTrue(primaryCopyElements.contains(workingCopyElement));
			compare(workingCopyElement, workingCopyElement.getPrimaryCopy());
		}
	}


	@Test
	public void shouldCreateWorkingCopyForJaxrsResource() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor.createCompilationUnit("BoatResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "BoatResource.java").findPrimaryType();
		final JaxrsResource primaryCopy = metamodelMonitor.createResource(type);
		// operation
		final JaxrsResource workingCopy = primaryCopy.getWorkingCopy();
		// verifications
		compare(workingCopy, primaryCopy);
		compare(workingCopy.getAnnotations(), primaryCopy.getAnnotations());
		compare(workingCopy.getFields(), primaryCopy.getFields());
		compare(workingCopy.getProperties(), primaryCopy.getProperties());
		compare(workingCopy.getMethods(), primaryCopy.getMethods());
	}

	@Test
	public void shouldCreateWorkingCopyForJaxrsResourceMethod() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor.createCompilationUnit("BoatResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "BoatResource.java").findPrimaryType();
		final JaxrsResource parentResource = metamodelMonitor.createResource(type);
		final IMethod javaMethod = JavaElementsUtils.getMethod(type, "getBoat");
		final JaxrsResourceMethod primaryCopy = parentResource.getMethods().get(javaMethod.getHandleIdentifier());
		// operation
		final JaxrsResourceMethod workingCopy = primaryCopy.getWorkingCopy();
		// verifications
		compare(workingCopy, primaryCopy);
		compare(workingCopy.getAnnotations(), primaryCopy.getAnnotations());
		compare(workingCopy.getParentResource(), primaryCopy.getParentResource());
		compare(workingCopy.getParentResource().getFields(), primaryCopy.getParentResource().getFields());
		compare(workingCopy.getParentResource().getProperties(), primaryCopy.getParentResource().getProperties());
		compare(workingCopy.getParentResource().getMethods(), primaryCopy.getParentResource().getMethods());
	}

	@Test
	public void shouldCreateWorkingCopyForJaxrsParamConverterProvider() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CarParamConverterProvider");
		final JaxrsParamConverterProvider primaryCopy = metamodelMonitor.createParameterConverterProvider(type);
		// operation
		final JaxrsParamConverterProvider workingCopy = primaryCopy.getWorkingCopy();
		// verifications
		compare(workingCopy, primaryCopy);
		compare(workingCopy.getAnnotations(), primaryCopy.getAnnotations());
		compare(workingCopy.getNameBindingAnnotations(), primaryCopy.getNameBindingAnnotations());
	}

	@Test
	public void shouldCreateWorkingCopyForJaxrsParameterAggregator() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		final JaxrsParameterAggregator primaryCopy = metamodelMonitor.createParameterAggregator(type);
		// operation
		final JaxrsParameterAggregator workingCopy = primaryCopy.getWorkingCopy();
		// verifications
		compare(workingCopy, primaryCopy);
		compare(workingCopy.getAnnotations(), primaryCopy.getAnnotations());
		compare(workingCopy.getAllFields(), primaryCopy.getAllFields());
		compare(workingCopy.getAllProperties(), primaryCopy.getAllProperties());
	}

	@Test
	public void shouldCreateWorkingCopyForJaxrsJavaApplication() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final JaxrsJavaApplication primaryCopy = metamodelMonitor.createJavaApplication(type.getFullyQualifiedName());
		// operation
		final JaxrsJavaApplication workingCopy = primaryCopy.getWorkingCopy();
		// verifications
		compare(workingCopy, primaryCopy);
		compare(workingCopy.getAnnotations(), primaryCopy.getAnnotations());
		compare(workingCopy.getNameBindingAnnotations(), primaryCopy.getNameBindingAnnotations());
	}

	@Test
	public void shouldCreateWorkingCopyForJaxrsProvider() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomRequestFilter");
		final JaxrsProvider primaryCopy = metamodelMonitor.createProvider(type);
		// operation
		final JaxrsProvider workingCopy = primaryCopy.getWorkingCopy();
		// verifications
		compare(workingCopy, primaryCopy);
		compare(workingCopy.getAnnotations(), primaryCopy.getAnnotations());
		compare(workingCopy.getNameBindingAnnotations(), primaryCopy.getNameBindingAnnotations());
		compare(workingCopy.getProvidedTypes(), primaryCopy.getProvidedTypes());
	}

}
