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
import static org.jboss.tools.ws.jaxrs.core.WorkbenchUtils.changeAnnotation;
import static org.jboss.tools.ws.jaxrs.core.WorkbenchUtils.resolveAnnotation;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.validation.MarkerUtils.deleteJaxrsMarkers;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.validation.MarkerUtils.findJaxrsMarkers;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.validation.MarkerUtils.hasPreferenceKey;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.HTTP_METHOD;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.RETENTION;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.TARGET;
import static org.jboss.tools.ws.jaxrs.core.preferences.JaxrsPreferences.HTTP_METHOD_INVALID_HTTP_METHOD_ANNOTATION_VALUE;
import static org.jboss.tools.ws.jaxrs.core.preferences.JaxrsPreferences.HTTP_METHOD_INVALID_RETENTION_ANNOTATION_VALUE;
import static org.jboss.tools.ws.jaxrs.core.preferences.JaxrsPreferences.HTTP_METHOD_INVALID_TARGET_ANNOTATION_VALUE;
import static org.jboss.tools.ws.jaxrs.core.preferences.JaxrsPreferences.HTTP_METHOD_MISSING_RETENTION_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.preferences.JaxrsPreferences.HTTP_METHOD_MISSING_TARGET_ANNOTATION;
import static org.junit.Assert.assertThat;

import java.util.HashSet;
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
import org.jboss.tools.common.validation.ValidatorManager;
import org.jboss.tools.common.validation.internal.ProjectValidationContext;
import org.jboss.tools.ws.jaxrs.core.WorkbenchUtils;
import org.jboss.tools.ws.jaxrs.core.builder.AbstractMetamodelBuilderTestCase;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsBaseElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsElementFactory;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.junit.Test;

/**
 * @author Xi
 * 
 */
@SuppressWarnings("restriction")
public class JaxrsHttpMethodValidatorTestCase extends AbstractMetamodelBuilderTestCase {

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
	public void shouldValidateHttpMethod() throws CoreException, ValidationException {
		// preconditions
		final IType fooType = WorkbenchUtils.getType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject);
		final JaxrsBaseElement httpMethod = (JaxrsBaseElement) metamodel.getElement(fooType);
		assertThat(findJaxrsMarkers(httpMethod).length, equalTo(0));
		deleteJaxrsMarkers(httpMethod);
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		assertThat(findJaxrsMarkers(httpMethod).length, equalTo(0));
	}

	@Test
	public void shouldSkipValidationOnBinaryHttpMethod() throws CoreException, ValidationException {
		// preconditions: create an HttpMethod from the binary annotation, then try to validate
		final IType getType = WorkbenchUtils.getType("javax.ws.rs.GET", javaProject);
		final JaxrsHttpMethod httpMethod = new JaxrsElementFactory().createHttpMethod(getType, JdtUtils.parse(getType, null), metamodel);
		metamodel.add(httpMethod);
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
		assertThat(markers, hasPreferenceKey(HTTP_METHOD_INVALID_HTTP_METHOD_ANNOTATION_VALUE));
	}

	@Test
	public void shouldReportProblemWhenHttpMethodVerbIsNull() throws CoreException, ValidationException {
		// preconditions
		final IType fooType = WorkbenchUtils.getType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject);
		final JaxrsHttpMethod httpMethod = metamodel.getElement(fooType, JaxrsHttpMethod.class);
		final Annotation httpAnnotation = changeAnnotation(fooType, HTTP_METHOD.qualifiedName, (String) null);
		httpMethod.addOrUpdateAnnotation(httpAnnotation);
		WorkbenchUtils.replaceFirstOccurrenceOfCode(fooType.getCompilationUnit(), "@HttpMethod(\"FOO\")", "@HttpMethod", true);
		deleteJaxrsMarkers(httpMethod);
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(httpMethod);
		assertThat(markers.length, equalTo(1));
		assertThat(markers, hasPreferenceKey(HTTP_METHOD_INVALID_HTTP_METHOD_ANNOTATION_VALUE));
	}

	@Test
	public void shouldReportProblemWhenHttpMethodTypeMissesTargetAnnotation() throws CoreException, ValidationException {
		// preconditions
		final IType fooType = WorkbenchUtils.getType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject);
		final JaxrsHttpMethod httpMethod = metamodel.getElement(fooType, JaxrsHttpMethod.class);
		final Annotation targetAnnotation = resolveAnnotation(fooType, TARGET.qualifiedName);
		httpMethod.removeAnnotation(targetAnnotation);
		WorkbenchUtils.replaceFirstOccurrenceOfCode(fooType.getCompilationUnit(), "@Target(value=ElementType.METHOD)", "", true);
		deleteJaxrsMarkers(httpMethod);
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(httpMethod);
		assertThat(markers.length, equalTo(1));
		assertThat(markers, hasPreferenceKey(HTTP_METHOD_MISSING_TARGET_ANNOTATION));
	}

	@Test
	public void shouldReportProblemWhenHttpMethodTypeTargetAnnotationHasNullValue() throws CoreException,
			ValidationException {
		// preconditions
		final IType fooType = WorkbenchUtils.getType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject);
		final JaxrsHttpMethod httpMethod = metamodel.getElement(fooType, JaxrsHttpMethod.class);
		final Annotation targetAnnotation = changeAnnotation(fooType, TARGET.qualifiedName, (String) null);
		httpMethod.addOrUpdateAnnotation(targetAnnotation);
		WorkbenchUtils.replaceFirstOccurrenceOfCode(fooType.getCompilationUnit(), "@Target(value=ElementType.METHOD)", "@Target", true);
		deleteJaxrsMarkers(httpMethod);
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(httpMethod);
		assertThat(markers.length, equalTo(1));
		assertThat(markers, hasPreferenceKey(HTTP_METHOD_INVALID_TARGET_ANNOTATION_VALUE));
	}

	@Test
	public void shouldReportProblemWhenHttpMethodTypeTargetAnnotationHasWrongValue() throws CoreException,
			ValidationException {
		// preconditions
		final IType fooType = WorkbenchUtils.getType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject);
		final JaxrsHttpMethod httpMethod = metamodel.getElement(fooType, JaxrsHttpMethod.class);
		final Annotation targetAnnotation = changeAnnotation(fooType, TARGET.qualifiedName, "FOO");
		httpMethod.addOrUpdateAnnotation(targetAnnotation);
		WorkbenchUtils.replaceFirstOccurrenceOfCode(fooType.getCompilationUnit(), "@Target(value=ElementType.METHOD)", "@Target(value=ElementType.FIELD)", true);
		deleteJaxrsMarkers(httpMethod);
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(httpMethod);
		assertThat(markers.length, equalTo(1));
		assertThat(markers, hasPreferenceKey(HTTP_METHOD_INVALID_TARGET_ANNOTATION_VALUE));
	}

	@Test
	public void shouldReportProblemWhenHttpMethodTypeMissesRetentionAnnotation() throws CoreException, ValidationException {
		// preconditions
		final IType fooType = WorkbenchUtils.getType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject);
		final JaxrsHttpMethod httpMethod = metamodel.getElement(fooType, JaxrsHttpMethod.class);
		final Annotation targetAnnotation = resolveAnnotation(fooType, RETENTION.qualifiedName);
		httpMethod.removeAnnotation(targetAnnotation);
		WorkbenchUtils.replaceFirstOccurrenceOfCode(fooType.getCompilationUnit(), "@Retention(value=RetentionPolicy.RUNTIME)", "", true);
		deleteJaxrsMarkers(httpMethod);
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(httpMethod);
		assertThat(markers.length, equalTo(1));
		assertThat(markers, hasPreferenceKey(HTTP_METHOD_MISSING_RETENTION_ANNOTATION));
	}
	
	@Test
	public void shouldReportProblemWhenHttpMethodTypeRetentionAnnotationHasNullValue() throws CoreException,
	ValidationException {
		// preconditions
		final IType fooType = WorkbenchUtils.getType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject);
		final JaxrsHttpMethod httpMethod = metamodel.getElement(fooType, JaxrsHttpMethod.class);
		final Annotation targetAnnotation = changeAnnotation(fooType, TARGET.qualifiedName, (String) null);
		httpMethod.addOrUpdateAnnotation(targetAnnotation);
		WorkbenchUtils.replaceFirstOccurrenceOfCode(fooType.getCompilationUnit(), "@Retention(value=RetentionPolicy.RUNTIME)", "@Retention", true);
		deleteJaxrsMarkers(httpMethod);
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(httpMethod);
		assertThat(markers.length, equalTo(1));
		assertThat(markers, hasPreferenceKey(HTTP_METHOD_INVALID_RETENTION_ANNOTATION_VALUE));
	}
	
	@Test
	public void shouldReportProblemWhenHttpMethodTypeRetentionAnnotationHasWrongValue() throws CoreException,
	ValidationException {
		// preconditions
		final IType fooType = WorkbenchUtils.getType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject);
		final JaxrsHttpMethod httpMethod = metamodel.getElement(fooType, JaxrsHttpMethod.class);
		final Annotation targetAnnotation = changeAnnotation(fooType, RETENTION.qualifiedName, "FOO");
		httpMethod.addOrUpdateAnnotation(targetAnnotation);
		WorkbenchUtils.replaceFirstOccurrenceOfCode(fooType.getCompilationUnit(), "@Retention(value=RetentionPolicy.RUNTIME)", "@Retention(value=RetentionPolicy.SOURCE)", true);
		deleteJaxrsMarkers(httpMethod);
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(httpMethod);
		assertThat(markers.length, equalTo(1));
		assertThat(markers, hasPreferenceKey(HTTP_METHOD_INVALID_RETENTION_ANNOTATION_VALUE));
	}

}
