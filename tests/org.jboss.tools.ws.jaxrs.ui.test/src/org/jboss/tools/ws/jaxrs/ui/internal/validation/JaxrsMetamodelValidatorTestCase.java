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
package org.jboss.tools.ws.jaxrs.ui.internal.validation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.deleteJaxrsMarkers;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.findJaxrsMarkers;
import static org.junit.Assert.assertThat;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
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
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Xi
 * 
 */
@SuppressWarnings("restriction")
public class JaxrsMetamodelValidatorTestCase {

	private final IReporter reporter = new ReporterHelper(new NullProgressMonitor());
	private final IProjectValidationContext projectValidationContext = new ProjectValidationContext();
	private EditorValidationContext editorValidationContext = null;
	private final IValidationContext validationContext = new WorkbenchContext();
	private final ValidatorManager validatorManager = new ValidatorManager();
	private JaxrsMetamodelValidator metamodelValidator;
	
	@ClassRule
	public static WorkspaceSetupRule workspaceSetupRule = new WorkspaceSetupRule("org.jboss.tools.ws.jaxrs.tests.sampleproject");
	
	@Rule
	public JaxrsMetamodelMonitor metamodelMonitor = new JaxrsMetamodelMonitor("org.jboss.tools.ws.jaxrs.tests.sampleproject", true);
	
	private JaxrsMetamodel metamodel = null;

	private IProject project = null;

	@Before
	public void setup() throws CoreException {
		metamodel = metamodelMonitor.getMetamodel();
		project = metamodel.getProject();
		metamodelValidator = new JaxrsMetamodelValidator();
	}

	@Test
	public void shouldValidateMetamodel() throws CoreException, ValidationException {
		// preconditions
		final IFile changedFile = (IFile)(metamodel.findJavaApplications().get(0).getResource());
		IDocument document = new Document();
		editorValidationContext = new EditorValidationContext(project, document);
		deleteJaxrsMarkers(project);
		// operation
		metamodelValidator.shouldValidate(project);
		metamodelValidator.validate(validatorManager, project, null, validationContext, reporter, editorValidationContext, projectValidationContext, changedFile);
		// validation
		final IMarker[] markers = findJaxrsMarkers(project);
		assertThat(markers.length, equalTo(0));
		assertThat(metamodelMonitor.getMetamodelProblemLevelChanges().size(), is(0));
	}
	
}
