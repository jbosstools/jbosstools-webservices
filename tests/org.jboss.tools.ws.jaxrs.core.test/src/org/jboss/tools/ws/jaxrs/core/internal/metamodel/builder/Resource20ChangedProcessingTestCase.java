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
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder;

import static org.eclipse.jdt.core.IJavaElementDelta.ADDED;
import static org.eclipse.jdt.core.IJavaElementDelta.CHANGED;
import static org.eclipse.jdt.core.IJavaElementDelta.REMOVED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils.delete;
import static org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils.replaceFirstOccurrenceOfCode;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.DEFAULT_VALUE;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.QUERY_PARAM;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsParameterAggregator;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsParameterAggregatorField;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsParameterAggregatorProperty;
import org.jboss.tools.ws.jaxrs.core.jdt.Flags;
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementCategory;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

public class Resource20ChangedProcessingTestCase {

	@ClassRule
	public static WorkspaceSetupRule workspaceSetupRule = new WorkspaceSetupRule("org.jboss.tools.ws.jaxrs.tests.sampleproject2");
	
	@Rule
	public JaxrsMetamodelMonitor metamodelMonitor = new JaxrsMetamodelMonitor("org.jboss.tools.ws.jaxrs.tests.sampleproject2", false);
	
	private JaxrsMetamodel metamodel = null;

	private IJavaProject javaProject = null;

	@Before
	public void setup() throws CoreException {
		metamodel = metamodelMonitor.getMetamodel();
		javaProject = metamodel.getJavaProject();
	}
	
	protected ResourceDelta createResourceDelta(final IResource resource, final int deltaKind) {
		return new ResourceDelta(resource, deltaKind, Flags.NONE);
	}

	protected void processAffectedResources(final ResourceDelta event) throws CoreException {
		metamodel.processAffectedResources(Arrays.asList(event), new NullProgressMonitor());
	}

	protected void processProject() throws CoreException {
		metamodel.processProject(new NullProgressMonitor());
	}

	@Test
	public void shouldAddParameterAggregatorWhenAddingSourceCompilationUnit() throws CoreException {
		// pre-conditions
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		final ResourceDelta event = createResourceDelta(type.getResource(), ADDED);
		processAffectedResources(event);
		// verifications: 1 aggregator + 2 fields + 2 methods added
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(5)); 
		// 6 built-in HttpMethods + 1 parameter aggregator (including its 2 fields and 2 properties)
		assertThat(metamodel.findElements(javaProject).size(), equalTo(11));
	}

	@Test
	public void shouldRemoveParameterAggregatorWhenRemovingUnderlyingResource() throws CoreException {
		// pre-conditions
		final JaxrsParameterAggregator parameterAggregator = metamodelMonitor.createParameterAggregator("org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final ResourceDelta event = createResourceDelta(parameterAggregator.getResource(), REMOVED);
		processAffectedResources(event);
		// verifications: 1 aggregator + 2 fields + 2 methods removed
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(5)); 
		// 6 built-in HttpMethods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldRemoveParameterAggregatorWhenRemovingJavaType() throws CoreException {
		// pre-conditions
		final JaxrsParameterAggregator parameterAggregator = metamodelMonitor.createParameterAggregator("org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		delete(parameterAggregator.getJavaElement());
		final ResourceDelta event = createResourceDelta(parameterAggregator.getResource(), CHANGED);
		processAffectedResources(event);
		// verifications: 1 aggregator + 2 fields + 2 methods removed
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(5)); 
		// 6 built-in HttpMethods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}
	
	@Test
	public void shouldAddParameterAggregatorFieldWhenChangingResource() throws CoreException {
		// pre-conditions
		final JaxrsParameterAggregator parameterAggregator = metamodelMonitor.createParameterAggregator("org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		parameterAggregator.getAllFields().get(0).remove(Flags.NONE);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final ResourceDelta event = createResourceDelta(parameterAggregator.getJavaElement().getResource(), CHANGED);
		processAffectedResources(event);
		// verifications: 1 field added (the one removed before)
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1)); 
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.PARAMETER_AGGREGATOR_FIELD));
		// 6 built-in HttpMethods + 1 parameter aggregator (including its 2 fields and 2 properties)
		assertThat(metamodel.findElements(javaProject).size(), equalTo(11));
	}

	@Test
	public void shouldChangeParameterAggregatorFieldWhenChangingResource() throws CoreException {
		// pre-conditions
		final JaxrsParameterAggregator parameterAggregator = metamodelMonitor.createParameterAggregator("org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		for (Iterator<JaxrsParameterAggregatorField> iterator = parameterAggregator.getAllFields().iterator(); iterator
				.hasNext();) {
			JaxrsParameterAggregatorField aggregatorField = iterator.next();
			if (aggregatorField.getAnnotation(DEFAULT_VALUE) != null) {
				replaceFirstOccurrenceOfCode(aggregatorField.getJavaElement(), "@DefaultValue(\"color!\")",
						"@DefaultValue(\"bar\")", false);
			}
		}
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final ResourceDelta event = createResourceDelta(parameterAggregator.getJavaElement().getResource(), CHANGED);
		processAffectedResources(event);
		// verifications: 1 field removed (matching the java field those annotation was removed before)
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1)); 
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.PARAMETER_AGGREGATOR_FIELD));
		// 6 built-in HttpMethods + 1 parameter aggregator (including its 2 fields and 2 properties)
		assertThat(metamodel.findElements(javaProject).size(), equalTo(11));
	}

	@Test
	public void shouldRemoveParameterAggregatorFieldWhenChangingResource() throws CoreException {
		// pre-conditions
		final JaxrsParameterAggregator parameterAggregator = metamodelMonitor.createParameterAggregator("org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		for (Iterator<JaxrsParameterAggregatorField> iterator = parameterAggregator.getAllFields().iterator(); iterator
				.hasNext();) {
			JaxrsParameterAggregatorField aggregatorField = iterator.next();
			if (aggregatorField.getAnnotation(QUERY_PARAM) != null) {
				delete(aggregatorField.getAnnotation(QUERY_PARAM).getJavaAnnotation(), false);
			}
		}
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final ResourceDelta event = createResourceDelta(parameterAggregator.getJavaElement().getResource(), CHANGED);
		processAffectedResources(event);
		// verifications: 1 field removed (matching the java field those annotation was removed before)
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1)); 
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.PARAMETER_AGGREGATOR_FIELD));
		// 6 built-in HttpMethods + 1 parameter aggregator (including its 1 field and 2 properties)
		assertThat(metamodel.findElements(javaProject).size(), equalTo(10));
	}

	@Test
	public void shouldAddParameterAggregatorPropertyWhenChangingResource() throws CoreException {
		// pre-conditions
		final JaxrsParameterAggregator parameterAggregator = metamodelMonitor.createParameterAggregator("org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		parameterAggregator.getAllProperties().get(0).remove(Flags.NONE);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final ResourceDelta event = createResourceDelta(parameterAggregator.getJavaElement().getResource(), CHANGED);
		processAffectedResources(event);
		// verifications: 1 property added (the one removed before)
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1)); 
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.PARAMETER_AGGREGATOR_PROPERTY));
		// 6 built-in HttpMethods + 1 parameter aggregator (including its 2 fields and 2 properties)
		assertThat(metamodel.findElements(javaProject).size(), equalTo(11));
	}
	
	@Test
	public void shouldChangeParameterAggregatorPropertyWhenChangingResource() throws CoreException {
		// pre-conditions
		final JaxrsParameterAggregator parameterAggregator = metamodelMonitor.createParameterAggregator("org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		for (Iterator<JaxrsParameterAggregatorProperty> iterator = parameterAggregator.getAllProperties().iterator(); iterator
				.hasNext();) {
			JaxrsParameterAggregatorProperty aggregatorProperty = iterator.next();
			if (aggregatorProperty.getAnnotation(DEFAULT_VALUE) != null) {
				replaceFirstOccurrenceOfCode(aggregatorProperty.getJavaElement(), "@DefaultValue(\"shape!\")",
						"@DefaultValue(\"bar\")", false);
			}
		}
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final ResourceDelta event = createResourceDelta(parameterAggregator.getJavaElement().getResource(), CHANGED);
		processAffectedResources(event);
		// verifications: 1 field removed (matching the java field those annotation was removed before)
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1)); 
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.PARAMETER_AGGREGATOR_PROPERTY));
		// 6 built-in HttpMethods + 1 parameter aggregator (including its 2 fields and 2 properties)
		assertThat(metamodel.findElements(javaProject).size(), equalTo(11));
	}

	@Test
	public void shouldRemoveParameterAggregatorPropertyWhenChangingResource() throws CoreException {
		// pre-conditions
		final JaxrsParameterAggregator parameterAggregator = metamodelMonitor.createParameterAggregator("org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		for (Iterator<JaxrsParameterAggregatorProperty> iterator = parameterAggregator.getAllProperties().iterator(); iterator
				.hasNext();) {
			JaxrsParameterAggregatorProperty aggregatorProperty = iterator.next();
			if (aggregatorProperty.getAnnotation(QUERY_PARAM) != null) {
				delete(aggregatorProperty.getAnnotation(QUERY_PARAM).getJavaAnnotation(), false);
			}
		}
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final ResourceDelta event = createResourceDelta(parameterAggregator.getJavaElement().getResource(), CHANGED);
		processAffectedResources(event);
		// verifications: 1 field removed (matching the java field those annotation was removed before)
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1)); 
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.PARAMETER_AGGREGATOR_PROPERTY));
		// 6 built-in HttpMethods + 1 parameter aggregator (including its 1 field and 2 properties)
		assertThat(metamodel.findElements(javaProject).size(), equalTo(10));
	}
}
