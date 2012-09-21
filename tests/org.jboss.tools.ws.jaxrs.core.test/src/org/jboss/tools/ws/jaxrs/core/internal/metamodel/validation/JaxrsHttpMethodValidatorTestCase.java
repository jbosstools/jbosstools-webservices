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
import static org.jboss.tools.ws.jaxrs.core.WorkbenchUtils.getAnnotation;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.HTTP_METHOD;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.TARGET;
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
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
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
		assertThat(MarkerUtils.findJaxrsMarkers(httpMethod).length, equalTo(0));
		MarkerUtils.deleteJaxrsMarkers(httpMethod);
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		assertThat(MarkerUtils.findJaxrsMarkers(httpMethod).length, equalTo(0));
	}

	@Test
	public void shouldReportProblemWhenHttpMethodVerbIsEmpty() throws CoreException, ValidationException {
		// preconditions
		final IType fooType = WorkbenchUtils.getType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject);
		final JaxrsHttpMethod httpMethod = metamodel.getElement(fooType, JaxrsHttpMethod.class);
		final Annotation httpAnnotation = changeAnnotation(fooType, HTTP_METHOD.qualifiedName, new String[0]);
		httpMethod.addOrUpdateAnnotation(httpAnnotation);
		MarkerUtils.deleteJaxrsMarkers(httpMethod);
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		final IMarker[] markers = MarkerUtils.findJaxrsMarkers(httpMethod);
		assertThat(markers.length, equalTo(1));
	}

	@Test
	public void shouldReportProblemWhenHttpMethodVerbIsNull() throws CoreException, ValidationException {
		// preconditions
		final IType fooType = WorkbenchUtils.getType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject);
		final JaxrsHttpMethod httpMethod = metamodel.getElement(fooType, JaxrsHttpMethod.class);
		final Annotation httpAnnotation = changeAnnotation(fooType, HTTP_METHOD.qualifiedName, (String) null);
		httpMethod.addOrUpdateAnnotation(httpAnnotation);
		MarkerUtils.deleteJaxrsMarkers(httpMethod);
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		assertThat(MarkerUtils.findJaxrsMarkers(httpMethod).length, equalTo(1));
	}

	@Test
	public void shouldReportProblemWhenHttpMethodTypeMissesTargetAnnotation() throws CoreException, ValidationException {
		// preconditions
		final IType fooType = WorkbenchUtils.getType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject);
		final JaxrsHttpMethod httpMethod = metamodel.getElement(fooType, JaxrsHttpMethod.class);
		final Annotation targetAnnotation = getAnnotation(fooType, TARGET.qualifiedName);
		httpMethod.removeAnnotation(targetAnnotation);
		MarkerUtils.deleteJaxrsMarkers(httpMethod);
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		assertThat(MarkerUtils.findJaxrsMarkers(httpMethod).length, equalTo(1));
	}

	@Test
	public void shouldReportProblemWhenHttpMethodTypeTargetAnnotationHasNullValue() throws CoreException,
			ValidationException {
		// preconditions
		final IType fooType = WorkbenchUtils.getType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject);
		final JaxrsHttpMethod httpMethod = metamodel.getElement(fooType, JaxrsHttpMethod.class);
		final Annotation targetAnnotation = changeAnnotation(fooType, TARGET.qualifiedName, (String) null);
		httpMethod.addOrUpdateAnnotation(targetAnnotation);
		MarkerUtils.deleteJaxrsMarkers(httpMethod);
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		assertThat(MarkerUtils.findJaxrsMarkers(httpMethod).length, equalTo(1));

	}

	@Test
	public void shouldReportProblemWhenHttpMethodTypeTargetAnnotationHasWrongValue() throws CoreException,
			ValidationException {
		// preconditions
		final IType fooType = WorkbenchUtils.getType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject);
		final JaxrsHttpMethod httpMethod = metamodel.getElement(fooType, JaxrsHttpMethod.class);
		final Annotation targetAnnotation = changeAnnotation(fooType, TARGET.qualifiedName, "FOO");
		httpMethod.addOrUpdateAnnotation(targetAnnotation);
		MarkerUtils.deleteJaxrsMarkers(httpMethod);
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		assertThat(MarkerUtils.findJaxrsMarkers(httpMethod).length, equalTo(1));
	}

}
