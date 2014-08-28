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
package org.jboss.tools.ws.jaxrs.ui.internal.validation;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.deleteJaxrsMarkers;
import static org.junit.Assert.assertThat;

import java.util.Arrays;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.wst.validation.ReporterHelper;
import org.eclipse.wst.validation.internal.core.ValidationException;
import org.eclipse.wst.validation.internal.operations.WorkbenchContext;
import org.eclipse.wst.validation.internal.provisional.core.IReporter;
import org.eclipse.wst.validation.internal.provisional.core.IValidationContext;
import org.jboss.tools.common.validation.EditorValidationContext;
import org.jboss.tools.common.validation.IProjectValidationContext;
import org.jboss.tools.common.validation.ValidatorManager;
import org.jboss.tools.common.validation.internal.ProjectValidationContext;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils;
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJavaMethodParameter;
import org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Xi
 * 
 */
@SuppressWarnings("restriction")
public class JaxrsMetamodelValidatorAsYouTypeTestCase {

	private final IReporter reporter = new ReporterHelper(new NullProgressMonitor());
	private final IProjectValidationContext projectValidationContext = new ProjectValidationContext();
	private final IValidationContext validationContext = new WorkbenchContext();
	private final ValidatorManager validatorManager = new ValidatorManager();
	private JaxrsMetamodelValidator metamodelValidator;

	@ClassRule
	public static WorkspaceSetupRule workspaceSetupRule = new WorkspaceSetupRule(
			"org.jboss.tools.ws.jaxrs.tests.sampleproject");

	@Rule
	public JaxrsMetamodelMonitor metamodelMonitor = new JaxrsMetamodelMonitor(
			"org.jboss.tools.ws.jaxrs.tests.sampleproject", false);

	private JaxrsMetamodel metamodel = null;

	private IProject project = null;

	private IJavaProject javaProject = null;

	@Before
	public void setup() throws CoreException {
		metamodel = metamodelMonitor.getMetamodel();
		project = metamodel.getProject();
		javaProject = metamodel.getJavaProject();
		metamodelValidator = new JaxrsMetamodelValidator();
	}

	@Test
	public void shouldValidateMetamodel() throws CoreException, ValidationException {
		// preconditions
		ResourcesUtils.replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.BarResource", javaProject, "getContent1(@PathParam(\"param1\") int id)", "getContent1(@PathParam(\"param3\") int id)", true);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.BarResource");
		final IType barType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BarResource");
		final JaxrsResource barResource = metamodel.findResource(barType);
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();
		final IMethod barMethod = JavaElementsUtils.getMethod(barType, "getContent1");
		final JaxrsResourceMethod barResourceMethod = barResource.getMethods().get(barMethod.getHandleIdentifier());
		final IJavaMethodParameter javaMethodParameter = barResourceMethod.getJavaMethodParameters().get(0);
		final Annotation pathParamAnnotation = javaMethodParameter.getAnnotation(JaxrsClassnames.PATH_PARAM);
		final ISourceRange annotationRange = pathParamAnnotation.getJavaAnnotation().getSourceRange();
		final IRegion dirtyRegion = new Region(annotationRange.getOffset() + "@PathParam(".length(), "\"param3\"".length());
		final IDocument document = new Document(barType.getCompilationUnit().getSource());
		final EditorValidationContext editorValidationContext = new EditorValidationContext(project, document);
		// operation
		metamodelValidator.validate(validatorManager, project, Arrays.asList(dirtyRegion), validationContext, reporter,
				editorValidationContext, projectValidationContext, (IFile) barResourceMethod.getResource());
		// validation
		final IMarker[] markers = ValidationUtils.findJaxrsMarkers(project);
		assertThat(markers.length, equalTo(0));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(1));
	}


}
