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
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.validation;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.eclipse.wst.validation.ReporterHelper;
import org.eclipse.wst.validation.internal.core.ValidationException;
import org.eclipse.wst.validation.internal.provisional.core.IReporter;
import org.jboss.tools.common.validation.ContextValidationHelper;
import org.jboss.tools.common.validation.IProjectValidationContext;
import org.jboss.tools.common.validation.ValidationErrorManager;
import org.jboss.tools.common.validation.ValidatorManager;
import org.jboss.tools.common.validation.internal.ProjectValidationContext;
import org.jboss.tools.ws.jaxrs.core.WorkbenchUtils;
import org.jboss.tools.ws.jaxrs.core.builder.AbstractMetamodelBuilderTestCase;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsBaseElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.preferences.JaxrsPreferences;
import org.junit.Test;

/**
 * @author Xi
 * 
 */
@SuppressWarnings("restriction")
public class JaxrsResourceValidatorTestCase extends AbstractMetamodelBuilderTestCase {

	private final IReporter reporter = new ReporterHelper(new NullProgressMonitor());
	private final ContextValidationHelper validationHelper = new ContextValidationHelper();
	private final IProjectValidationContext context = new ProjectValidationContext();
	private final ValidatorManager validatorManager = new ValidatorManager();

	private Set<IFile> toSet(IResource resource) {
		final Set<IFile> changedFiles = new HashSet<IFile>();
		changedFiles.add((IFile) resource);
		return changedFiles;
	}

	@Test
	public void shouldValidateCustomerResourceMethod() throws CoreException, ValidationException {
		// preconditions
		final IType customerJavaType = WorkbenchUtils.getType(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final JaxrsBaseElement customerResource = (JaxrsBaseElement) metamodel.getElement(customerJavaType);
		MarkerUtils.deleteJaxrsMarkers(customerResource);
		// operation
		new JaxrsMetamodelValidator().validate(toSet(customerResource.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		// validation
		assertThat(MarkerUtils.findJaxrsMarkers(customerResource).length, equalTo(0));
	}

	@Test
	public void shouldReportProblemsOnBarResourceMethods() throws CoreException, ValidationException {
		// preconditions
		final IType barJavaType = WorkbenchUtils.getType("org.jboss.tools.ws.jaxrs.sample.services.BarResource",
				javaProject);
		final JaxrsResource barResource = metamodel.getElement(barJavaType, JaxrsResource.class);
		MarkerUtils.deleteJaxrsMarkers(barResource);
		// operation
		new JaxrsMetamodelValidator().validate(toSet(barResource.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		final IMarker[] markers = MarkerUtils.findJaxrsMarkers(barResource);
		assertThat(markers.length, equalTo(8));
		final Map<String, JaxrsResourceMethod> resourceMethods = barResource.getMethods();
		for (Entry<String, JaxrsResourceMethod> entry : resourceMethods.entrySet()) {
			final IMarker[] methodMarkers = MarkerUtils.findJaxrsMarkers(entry.getValue());
			if (entry.getKey().contains("getContent1")) {
				assertThat(entry.getValue().hasErrors(), is(true));
				assertThat(methodMarkers.length, equalTo(1));
				assertThat(methodMarkers[0].getType(), equalTo(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE));
				assertThat(methodMarkers[0].getAttribute(ValidationErrorManager.PREFERENCE_KEY_ATTRIBUTE_NAME, ""),
						equalTo(JaxrsPreferences.RESOURCE_METHOD_UNBOUND_PATH_ANNOTATION_TEMPLATE_PARAMETER));
			} else if (entry.getKey().contains("getContent2")) {
				assertThat(entry.getValue().hasErrors(), is(true));
				assertThat(methodMarkers.length, equalTo(3));
			} else if (entry.getKey().contains("update1")) {
				assertThat(entry.getValue().hasErrors(), is(true));
				assertThat(methodMarkers.length, equalTo(2));
			} else if (entry.getKey().contains("update2")) {
				assertThat(entry.getValue().hasErrors(), is(true));
				assertThat(methodMarkers.length, equalTo(1));
			} else if (entry.getKey().contains("update3")) {
				assertThat(entry.getValue().hasErrors(), is(true));
				assertThat(methodMarkers.length, equalTo(1));
			} else {
				fail("Unexpected method " + entry.getKey());
			}
		}
	}

	@Test
	public void shouldReportProblemsOnBazResourceMethods() throws CoreException, ValidationException {
		// preconditions
		final IType barJavaType = WorkbenchUtils.getType("org.jboss.tools.ws.jaxrs.sample.services.BazResource",
				javaProject);
		final JaxrsResource barResource = metamodel.getElement(barJavaType, JaxrsResource.class);
		MarkerUtils.deleteJaxrsMarkers(barResource);
		// operation
		new JaxrsMetamodelValidator().validate(toSet(barResource.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		final IMarker[] markers = MarkerUtils.findJaxrsMarkers(barResource);
		assertThat(markers.length, equalTo(6));
		final Map<String, JaxrsResourceMethod> resourceMethods = barResource.getMethods();
		for (Entry<String, JaxrsResourceMethod> entry : resourceMethods.entrySet()) {
			final IMarker[] methodMarkers = MarkerUtils.findJaxrsMarkers(entry.getValue());
			if (entry.getKey().contains("getContent1")) {
				assertThat(entry.getValue().hasErrors(), is(true));
				assertThat(methodMarkers.length, equalTo(1));
				assertThat(methodMarkers[0].getType(), equalTo(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE));
				assertThat(methodMarkers[0].getAttribute(ValidationErrorManager.PREFERENCE_KEY_ATTRIBUTE_NAME, ""),
						equalTo(JaxrsPreferences.RESOURCE_METHOD_UNBOUND_PATH_ANNOTATION_TEMPLATE_PARAMETER));
			} else if (entry.getKey().contains("getContent2")) {
				assertThat(entry.getValue().hasErrors(), is(true));
				assertThat(methodMarkers.length, equalTo(2));
			} else if (entry.getKey().contains("update1")) {
				assertThat(entry.getValue().hasErrors(), is(true));
				assertThat(methodMarkers.length, equalTo(2));
			} else if (entry.getKey().contains("update2")) {
				assertThat(entry.getValue().hasErrors(), is(false));
				assertThat(methodMarkers.length, equalTo(0));
			} else if (entry.getKey().contains("update3")) {
				assertThat(entry.getValue().hasErrors(), is(true));
				assertThat(methodMarkers.length, equalTo(1));
			} else {
				fail("Unexpected method " + entry.getKey());
			}
		}
	}
}
