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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.jboss.tools.ws.jaxrs.core.WorkbenchUtils.changeAnnotation;
import static org.jboss.tools.ws.jaxrs.core.WorkbenchUtils.getAnnotation;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.HTTP_METHOD;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.TARGET;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ISourceRange;
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
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsApplication;
import org.jboss.tools.ws.jaxrs.core.preferences.JaxrsPreferences;
import org.junit.Test;

/**
 * @author Xi
 * 
 */
@SuppressWarnings("restriction")
public class JaxrsMetamodelValidatorTestCase extends AbstractMetamodelBuilderTestCase {

	private final IReporter reporter = new ReporterHelper(new NullProgressMonitor());
	private final ContextValidationHelper validationHelper = new ContextValidationHelper();
	private final IProjectValidationContext context = new ProjectValidationContext();
	private final ValidatorManager validatorManager = new ValidatorManager();

	private Set<IFile> toSet(IResource resource) {
		final Set<IFile> changedFiles = new HashSet<IFile>();
		changedFiles.add((IFile) resource);
		return changedFiles;
	}

	/**
	 * @param element
	 * @return
	 * @throws CoreException
	 */
	private IMarker[] findJaxrsMarkers(final JaxrsBaseElement element) throws CoreException {
		switch (element.getElementCategory()) {
		case HTTP_METHOD:
		case RESOURCE:
			return element.getResource().findMarkers(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, true,
					IResource.DEPTH_INFINITE);
		case RESOURCE_METHOD:
			final IMarker[] markers = element.getResource().findMarkers(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE,
					true, IResource.DEPTH_INFINITE);
			final List<IMarker> resourceMethodMarkers = new ArrayList<IMarker>();
			final ISourceRange methodSourceRange = ((JaxrsResourceMethod) element).getJavaElement().getSourceRange();

			for (IMarker marker : markers) {
				final int markerCharStart = marker.getAttribute(IMarker.CHAR_START, -1);
				if (markerCharStart >= methodSourceRange.getOffset()
						&& markerCharStart <= (methodSourceRange.getOffset() + methodSourceRange.getLength())) {
					resourceMethodMarkers.add(marker);
				}
			}
			return resourceMethodMarkers.toArray(new IMarker[resourceMethodMarkers.size()]);
		default:
			return new IMarker[0];
		}
	}

	/**
	 * @param element
	 * @throws CoreException
	 */
	private void deleteJaxrsMarkers(final JaxrsBaseElement element) throws CoreException {
		element.getResource()
				.deleteMarkers(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, false, IResource.DEPTH_INFINITE);
	}

	/**
	 * @param element
	 * @throws CoreException
	 */
	private void deleteJaxrsMarkers(final IProject project) throws CoreException {
		project.deleteMarkers(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, false, IResource.DEPTH_INFINITE);
	}

	@Test
	public void shouldValidateHttpMethod() throws CoreException, ValidationException {
		// preconditions
		final IType fooType = WorkbenchUtils.getType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject);
		final JaxrsBaseElement httpMethod = metamodel.getElement(fooType);
		assertThat(findJaxrsMarkers(httpMethod).length, equalTo(0));
		deleteJaxrsMarkers(httpMethod);
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		assertThat(findJaxrsMarkers(httpMethod).length, equalTo(0));
	}

	@Test
	public void shouldReportProblemWhenHttpMethodVerbIsEmpty() throws CoreException, ValidationException {
		// preconditions
		final IType fooType = WorkbenchUtils.getType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject);
		final JaxrsHttpMethod httpMethod = metamodel.getElement(fooType, JaxrsHttpMethod.class);
		final Annotation httpAnnotation = changeAnnotation(fooType, HTTP_METHOD.qualifiedName, new String[0]);
		httpMethod.addOrUpdateAnnotation(httpAnnotation);
		deleteJaxrsMarkers(httpMethod);
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(httpMethod);
		assertThat(markers.length, equalTo(1));
	}

	@Test
	public void shouldReportProblemWhenHttpMethodVerbIsNull() throws CoreException, ValidationException {
		// preconditions
		final IType fooType = WorkbenchUtils.getType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject);
		final JaxrsHttpMethod httpMethod = metamodel.getElement(fooType, JaxrsHttpMethod.class);
		final Annotation httpAnnotation = changeAnnotation(fooType, HTTP_METHOD.qualifiedName, (String) null);
		httpMethod.addOrUpdateAnnotation(httpAnnotation);
		deleteJaxrsMarkers(httpMethod);
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		assertThat(findJaxrsMarkers(httpMethod).length, equalTo(1));
	}

	@Test
	public void shouldReportProblemWhenHttpMethodTypeMissesTargetAnnotation() throws CoreException, ValidationException {
		// preconditions
		final IType fooType = WorkbenchUtils.getType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject);
		final JaxrsHttpMethod httpMethod = metamodel.getElement(fooType, JaxrsHttpMethod.class);
		final Annotation targetAnnotation = getAnnotation(fooType, TARGET.qualifiedName);
		httpMethod.removeAnnotation(targetAnnotation);
		deleteJaxrsMarkers(httpMethod);
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		assertThat(findJaxrsMarkers(httpMethod).length, equalTo(1));
	}

	@Test
	public void shouldReportProblemWhenHttpMethodTypeTargetAnnotationHasNullValue() throws CoreException,
			ValidationException {
		// preconditions
		final IType fooType = WorkbenchUtils.getType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject);
		final JaxrsHttpMethod httpMethod = metamodel.getElement(fooType, JaxrsHttpMethod.class);
		final Annotation targetAnnotation = changeAnnotation(fooType, TARGET.qualifiedName, (String) null);
		httpMethod.addOrUpdateAnnotation(targetAnnotation);
		deleteJaxrsMarkers(httpMethod);
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		assertThat(findJaxrsMarkers(httpMethod).length, equalTo(1));

	}

	@Test
	public void shouldReportProblemWhenHttpMethodTypeTargetAnnotationHasWrongValue() throws CoreException,
			ValidationException {
		// preconditions
		final IType fooType = WorkbenchUtils.getType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject);
		final JaxrsHttpMethod httpMethod = metamodel.getElement(fooType, JaxrsHttpMethod.class);
		final Annotation targetAnnotation = changeAnnotation(fooType, TARGET.qualifiedName, "FOO");
		httpMethod.addOrUpdateAnnotation(targetAnnotation);
		deleteJaxrsMarkers(httpMethod);
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		assertThat(findJaxrsMarkers(httpMethod).length, equalTo(1));
	}

	@Test
	public void shouldValidateCustomerResourceMethod() throws CoreException, ValidationException {
		// preconditions
		final IType customerJavaType = WorkbenchUtils.getType(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final JaxrsBaseElement customerResource = metamodel.getElement(customerJavaType);
		deleteJaxrsMarkers(customerResource);
		// operation
		new JaxrsMetamodelValidator().validate(toSet(customerResource.getResource()), project, validationHelper,
				context, validatorManager, reporter);
		// validation
		assertThat(findJaxrsMarkers(customerResource).length, equalTo(0));
	}

	@Test
	public void shouldReportProblemsOnBarResourceMethods() throws CoreException, ValidationException {
		// preconditions
		final IType barJavaType = WorkbenchUtils.getType("org.jboss.tools.ws.jaxrs.sample.services.BarResource",
				javaProject);
		final JaxrsResource barResource = metamodel.getElement(barJavaType, JaxrsResource.class);
		deleteJaxrsMarkers(barResource);
		// operation
		new JaxrsMetamodelValidator().validate(toSet(barResource.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(barResource);
		assertThat(markers.length, equalTo(8));
		final Map<String, JaxrsResourceMethod> resourceMethods = barResource.getMethods();
		for (Entry<String, JaxrsResourceMethod> entry : resourceMethods.entrySet()) {
			final IMarker[] methodMarkers = findJaxrsMarkers(entry.getValue());
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
		deleteJaxrsMarkers(barResource);
		// operation
		new JaxrsMetamodelValidator().validate(toSet(barResource.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(barResource);
		assertThat(markers.length, equalTo(6));
		final Map<String, JaxrsResourceMethod> resourceMethods = barResource.getMethods();
		for (Entry<String, JaxrsResourceMethod> entry : resourceMethods.entrySet()) {
			final IMarker[] methodMarkers = findJaxrsMarkers(entry.getValue());
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

	@Test
	public void shouldNotWarnIfOneApplicationExists() throws CoreException, ValidationException {
		// preconditions
		final List<IJaxrsApplication> applications = metamodel.getAllApplications();
		for (IJaxrsApplication application : applications) {
			if (application.getElementKind() == EnumElementKind.APPLICATION_WEBXML) {
				metamodel.remove((JaxrsBaseElement) application);
			}
		}
		deleteJaxrsMarkers(project);
		// operation
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation
		assertThat(project.findMarkers(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, false, 0).length, equalTo(0));
	}

	@Test
	public void shouldWarnOnProjectIfNoApplicationExists() throws CoreException, ValidationException {
		// preconditions
		final List<IJaxrsApplication> applications = metamodel.getAllApplications();
		for (IJaxrsApplication application : applications) {
			metamodel.remove((JaxrsBaseElement) application);
		}
		deleteJaxrsMarkers(project);
		// operation
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation
		final IMarker[] projectMarkers = project.findMarkers(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, false, 0);
		assertThat(projectMarkers.length, equalTo(1));
	}

	@Test
	public void shouldWarnOnProjectIfMultipleApplicationsExist() throws CoreException, ValidationException {
		// preconditions
		final List<IJaxrsApplication> applications = metamodel.getAllApplications();
		assertThat(applications, hasSize(2));
		deleteJaxrsMarkers(project);
		// operation
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation
		final IMarker[] projectMarkers = project.findMarkers(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, false, 0);
		assertThat(projectMarkers.length, equalTo(1));
	}
}
