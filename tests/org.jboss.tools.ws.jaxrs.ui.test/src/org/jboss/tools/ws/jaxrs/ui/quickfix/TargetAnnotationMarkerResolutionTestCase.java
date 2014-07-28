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

package org.jboss.tools.ws.jaxrs.ui.quickfix;

import static org.hamcrest.Matchers.equalTo;
import static org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils.replaceFirstOccurrenceOfCode;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.findJavaProblems;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.findJaxrsMarkers;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.hasPreferenceKey;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.toSet;
import static org.jboss.tools.ws.jaxrs.ui.preferences.JaxrsPreferences.HTTP_METHOD_MISSING_TARGET_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.ui.preferences.JaxrsPreferences.NAME_BINDING_MISSING_TARGET_ANNOTATION;
import static org.junit.Assert.assertThat;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.wst.validation.ReporterHelper;
import org.eclipse.wst.validation.internal.core.ValidationException;
import org.eclipse.wst.validation.internal.provisional.core.IReporter;
import org.jboss.tools.common.validation.ContextValidationHelper;
import org.jboss.tools.common.validation.IProjectValidationContext;
import org.jboss.tools.common.validation.ValidatorManager;
import org.jboss.tools.common.validation.internal.ProjectValidationContext;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsNameBinding;
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementCategory;
import org.jboss.tools.ws.jaxrs.ui.internal.validation.JaxrsMetamodelValidator;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author xcoulon
 *
 */
@SuppressWarnings("restriction")
public class TargetAnnotationMarkerResolutionTestCase {

	private final IReporter reporter = new ReporterHelper(new NullProgressMonitor());
	private final ContextValidationHelper validationHelper = new ContextValidationHelper();
	private final IProjectValidationContext context = new ProjectValidationContext();
	private final ValidatorManager validatorManager = new ValidatorManager();

	@ClassRule
	public static WorkspaceSetupRule workspaceSetupRule = new WorkspaceSetupRule("org.jboss.tools.ws.jaxrs.tests.sampleproject2");
	
	@Rule
	public JaxrsMetamodelMonitor metamodelMonitor = new JaxrsMetamodelMonitor("org.jboss.tools.ws.jaxrs.tests.sampleproject2", false);
	
	private JaxrsMetamodel metamodel = null;

	private IProject project = null;

	private IJavaProject javaProject = null;

	@Before
	public void setup() throws CoreException {
		this.metamodel = metamodelMonitor.getMetamodel();
		this.project = metamodel.getProject();
		this.javaProject = metamodel.getJavaProject();
	}
	
	@Test
	public void shouldAddTargetAnnotationAndAllImportsOnHttpMethod() throws CoreException, ValidationException {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("BAZ.txt", "org.jboss.tools.ws.jaxrs.sample.services", "BAZ.java");
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.BAZ", javaProject, "import java.lang.annotation.Target;", "//import java.lang.annotation.Target;", false);
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.BAZ", javaProject, "import java.lang.annotation.ElementType;", "//import java.lang.annotation.ElementType;", false);
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.BAZ", javaProject, "@Target(value=ElementType.METHOD)", "//@Target(value=ElementType.METHOD)", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.BAZ");
		final JaxrsHttpMethod bazMethod = (JaxrsHttpMethod) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.BAZ", EnumElementCategory.HTTP_METHOD);
		
		// operation 1: validate the HTTP Method
		new JaxrsMetamodelValidator().validate(toSet(bazMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		
		// verification 1: there should be 1 error: missing @Target annotation
		final IMarker[] markers = findJaxrsMarkers(bazMethod);
		assertThat(markers.length, equalTo(1));
		assertThat(markers, hasPreferenceKey(HTTP_METHOD_MISSING_TARGET_ANNOTATION));
		
		// operation 2: now, use the quickfix to fix the problem
		final ICompilationUnit compilationUnit = bazMethod.getJavaElement().getCompilationUnit();
		JavaCompletionProposalUtils.applyMarkerResolution(new AddHttpMethodTargetAnnotationMarkerResolution(bazMethod.getJavaElement()), bazMethod.getResource());
		metamodelMonitor.processEvent(compilationUnit, IJavaElementDelta.CHANGED);
		
		// verification 2: revalidate, there should be 0 JAX-RS/Java error
		new JaxrsMetamodelValidator().validate(toSet(bazMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		assertThat(findJaxrsMarkers(bazMethod).length, equalTo(0));
		assertThat(findJavaProblems(bazMethod.getResource()).length, equalTo(0));
	}

	@Test
	public void shouldAddTargetAnnotationWhenImportsExistOnHttpMethod() throws CoreException, ValidationException {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("BAZ.txt", "org.jboss.tools.ws.jaxrs.sample.services", "BAZ.java");
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.BAZ", javaProject, "@Target(value=ElementType.METHOD)", "//@Target(value=ElementType.METHOD)", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.BAZ");
		final JaxrsHttpMethod bazMethod = (JaxrsHttpMethod) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.BAZ", EnumElementCategory.HTTP_METHOD);
		
		// operation 1: validate the HTTP Method
		new JaxrsMetamodelValidator().validate(toSet(bazMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		
		// verification 1: there should be 1 error: missing @Target annotation
		final IMarker[] markers = findJaxrsMarkers(bazMethod);
		assertThat(markers.length, equalTo(1));
		assertThat(markers, hasPreferenceKey(HTTP_METHOD_MISSING_TARGET_ANNOTATION));
		
		// operation 2: now, use the quickfix to fix the problem
		final ICompilationUnit compilationUnit = bazMethod.getJavaElement().getCompilationUnit();
		JavaCompletionProposalUtils.applyMarkerResolution(new AddHttpMethodTargetAnnotationMarkerResolution(bazMethod.getJavaElement()), bazMethod.getResource());
		metamodelMonitor.processEvent(compilationUnit, IJavaElementDelta.CHANGED);
		
		// verification 2: revalidate, there should be 0 JAX-RS/Java error
		new JaxrsMetamodelValidator().validate(toSet(bazMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		assertThat(findJaxrsMarkers(bazMethod).length, equalTo(0));
		assertThat(findJavaProblems(bazMethod.getResource()).length, equalTo(0));
	}
	
	@Test
	public void shouldAddTargetAnnotationValueAndImportOnHttpMethod() throws CoreException, ValidationException {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("BAZ.txt", "org.jboss.tools.ws.jaxrs.sample.services", "BAZ.java");
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.BAZ", javaProject, "import java.lang.annotation.ElementType;", "//import java.lang.annotation.ElementType;", false);
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.BAZ", javaProject, "@Target(value=ElementType.METHOD)", "@Target()", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.BAZ");
		final JaxrsHttpMethod bazMethod = (JaxrsHttpMethod) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.BAZ", EnumElementCategory.HTTP_METHOD);
		
		// operation 1: validate the HTTP Method
		final IResource resource = bazMethod.getResource();
		new JaxrsMetamodelValidator().validate(toSet(resource), project, validationHelper, context,
				validatorManager, reporter);
		
		// verification 1: there should be 1 error: missing @Target annotation value
		final IMarker[] markers = findJaxrsMarkers(bazMethod);
		assertThat(markers.length, equalTo(0));
		final IProblem[] javaProblems = findJavaProblems(resource);
		
		// operation 2: now, use the quickfix to fix the problem
		final ICompilationUnit compilationUnit = bazMethod.getJavaElement().getCompilationUnit();
		final IJavaCompletionProposal completionProposal = new AddHttpMethodTargetValuesCompletionProposal(compilationUnit,
				JaxrsMarkerResolutionGenerator.findEffectiveSourceRange(compilationUnit, new ProblemLocation(javaProblems[0])));
		JavaCompletionProposalUtils.applyCompletionProposal(completionProposal, bazMethod);
		
		// verification 2: revalidate, there should be 0 JAX-RS/Java error
		new JaxrsMetamodelValidator().validate(toSet(resource), project, validationHelper, context,
				validatorManager, reporter);
		assertThat(findJaxrsMarkers(bazMethod).length, equalTo(0));
		assertThat(findJavaProblems(resource).length, equalTo(0));
	}

	@Test
	public void shouldAddTargetAnnotationValueOnlyWhenImportsExistOnHttpMethod() throws CoreException, ValidationException {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("BAZ.txt", "org.jboss.tools.ws.jaxrs.sample.services", "BAZ.java");
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.BAZ", javaProject, "@Target(value=ElementType.METHOD)", "@Target", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.BAZ");
		final JaxrsHttpMethod bazMethod = (JaxrsHttpMethod) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.BAZ", EnumElementCategory.HTTP_METHOD);
		
		// operation 1: validate the HTTP Method
		final IResource resource = bazMethod.getResource();
		new JaxrsMetamodelValidator().validate(toSet(resource), project, validationHelper, context,
				validatorManager, reporter);
		
		// verification 1: there should be 1 error: missing @Target annotation value
		final IMarker[] markers = findJaxrsMarkers(bazMethod);
		assertThat(markers.length, equalTo(0));
		final IProblem[] javaProblems = findJavaProblems(resource);
		
		// operation 2: now, use the quickfix to fix the problem
		final ICompilationUnit compilationUnit = bazMethod.getJavaElement().getCompilationUnit();
		final IJavaCompletionProposal completionProposal = new AddHttpMethodTargetValuesCompletionProposal(compilationUnit,
				JaxrsMarkerResolutionGenerator.findEffectiveSourceRange(compilationUnit, new ProblemLocation(javaProblems[0])));
		JavaCompletionProposalUtils.applyCompletionProposal(completionProposal, bazMethod);
		
		// verification 2: revalidate, there should be 0 JAX-RS/Java error
		new JaxrsMetamodelValidator().validate(toSet(resource), project, validationHelper, context,
				validatorManager, reporter);
		assertThat(findJaxrsMarkers(bazMethod).length, equalTo(0));
		assertThat(findJavaProblems(resource).length, equalTo(0));
	}
	
	@Test
	public void shouldUpdateRetentionAnnotationValueWhenInvalidValueOnHttpMethod() throws CoreException, ValidationException  {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("BAZ.txt", "org.jboss.tools.ws.jaxrs.sample.services", "BAZ.java");
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.BAZ", javaProject, "@Target(value=ElementType.METHOD)", "@Target(value=ElementType.TYPE)", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.BAZ");
		final JaxrsHttpMethod bazMethod = (JaxrsHttpMethod) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.BAZ", EnumElementCategory.HTTP_METHOD);
		
		// operation 1: validate the HTTP Method
		final IResource resource = bazMethod.getResource();
		new JaxrsMetamodelValidator().validate(toSet(resource), project, validationHelper, context,
				validatorManager, reporter);
		
		// verification 1: there should be 1 error: missing @Retention annotation value
		assertThat(findJavaProblems(resource).length, equalTo(0));
		final IMarker[] markers = findJaxrsMarkers(bazMethod);
		assertThat(markers.length, equalTo(1));
		
		// operation 2: now, use the quickfix to fix the problem
		final IJavaCompletionProposal completionProposal = new UpdateHttpMethodTargetAnnotationValueMarkerResolution(bazMethod.getJavaElement());
		JavaCompletionProposalUtils.applyCompletionProposal(completionProposal, bazMethod);
		metamodelMonitor.processEvent(bazMethod.getJavaElement().getCompilationUnit(), IJavaElementDelta.CHANGED);
		
		// verification 2: revalidate, there should be 0 JAX-RS/Java error
		new JaxrsMetamodelValidator().validate(toSet(resource), project, validationHelper, context,
				validatorManager, reporter);
		assertThat(findJaxrsMarkers(bazMethod).length, equalTo(0));
		assertThat(findJavaProblems(resource).length, equalTo(0));
	}
	
	@Test
	public void shouldAddTargetAnnotationAndAllImportsOnNameBinding() throws CoreException, ValidationException {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("AnotherNameBinding.txt", "org.jboss.tools.ws.jaxrs.sample.services", "AnotherNameBinding.java");
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.AnotherNameBinding", javaProject, "import java.lang.annotation.Target;", "//import java.lang.annotation.Target;", false);
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.AnotherNameBinding", javaProject, "import java.lang.annotation.ElementType;", "//import java.lang.annotation.ElementType;", false);
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.AnotherNameBinding", javaProject, "@Target({ ElementType.TYPE, ElementType.METHOD })", "//@Target({ ElementType.TYPE, ElementType.METHOD })", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.AnotherNameBinding");
		final JaxrsNameBinding nameBinding = (JaxrsNameBinding) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.AnotherNameBinding", EnumElementCategory.NAME_BINDING);
		
		// operation 1: validate the Name Binding
		new JaxrsMetamodelValidator().validate(toSet(nameBinding.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		
		// verification 1: there should be 1 error: missing @Target annotation
		final IMarker[] markers = findJaxrsMarkers(nameBinding);
		assertThat(markers.length, equalTo(1));
		assertThat(markers, hasPreferenceKey(NAME_BINDING_MISSING_TARGET_ANNOTATION));
		
		// operation 2: now, use the quickfix to fix the problem
		final ICompilationUnit compilationUnit = nameBinding.getJavaElement().getCompilationUnit();
		JavaCompletionProposalUtils.applyMarkerResolution(new AddNameBindingTargetAnnotationMarkerResolution(nameBinding.getJavaElement()), nameBinding.getResource());
		metamodelMonitor.processEvent(compilationUnit, IJavaElementDelta.CHANGED);
		
		// verification 2: revalidate, there should be 0 JAX-RS/Java error
		new JaxrsMetamodelValidator().validate(toSet(nameBinding.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		assertThat(findJaxrsMarkers(nameBinding).length, equalTo(0));
		assertThat(findJavaProblems(nameBinding.getResource()).length, equalTo(0));
	}

	@Test
	public void shouldAddTargetAnnotationWhenImportsExistOnNameBinding() throws CoreException, ValidationException {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("AnotherNameBinding.txt", "org.jboss.tools.ws.jaxrs.sample.services", "AnotherNameBinding.java");
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.AnotherNameBinding", javaProject, "@Target({ ElementType.TYPE, ElementType.METHOD })", "//@Target({ ElementType.TYPE, ElementType.METHOD })", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.AnotherNameBinding");
		final JaxrsNameBinding nameBinding = (JaxrsNameBinding) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.AnotherNameBinding", EnumElementCategory.NAME_BINDING);
		
		// operation 1: validate the Name Binding
		new JaxrsMetamodelValidator().validate(toSet(nameBinding.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		
		// verification 1: there should be 1 error: missing @Target annotation
		final IMarker[] markers = findJaxrsMarkers(nameBinding);
		assertThat(markers.length, equalTo(1));
		assertThat(markers, hasPreferenceKey(NAME_BINDING_MISSING_TARGET_ANNOTATION));
		
		// operation 2: now, use the quickfix to fix the problem
		final ICompilationUnit compilationUnit = nameBinding.getJavaElement().getCompilationUnit();
		JavaCompletionProposalUtils.applyMarkerResolution(new AddNameBindingTargetAnnotationMarkerResolution(nameBinding.getJavaElement()), nameBinding.getResource());
		metamodelMonitor.processEvent(compilationUnit, IJavaElementDelta.CHANGED);
		
		// verification 2: revalidate, there should be 0 JAX-RS/Java error
		new JaxrsMetamodelValidator().validate(toSet(nameBinding.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		assertThat(findJaxrsMarkers(nameBinding).length, equalTo(0));
		assertThat(findJavaProblems(nameBinding.getResource()).length, equalTo(0));
	}
	
	@Test
	public void shouldAddTargetAnnotationValueAndImportOnNameBinding() throws CoreException, ValidationException {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("AnotherNameBinding.txt", "org.jboss.tools.ws.jaxrs.sample.services", "AnotherNameBinding.java");
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.AnotherNameBinding", javaProject, "import java.lang.annotation.ElementType;", "//import java.lang.annotation.ElementType;", false);
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.AnotherNameBinding", javaProject, "@Target({ ElementType.TYPE, ElementType.METHOD })", "@Target()", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.AnotherNameBinding");
		final JaxrsNameBinding nameBinding = (JaxrsNameBinding) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.AnotherNameBinding", EnumElementCategory.NAME_BINDING);
		
		// operation 1: validate the Name Binding
		final IResource resource = nameBinding.getResource();
		new JaxrsMetamodelValidator().validate(toSet(resource), project, validationHelper, context,
				validatorManager, reporter);
		
		// verification 1: there should be 1 error: missing @Target annotation value
		final IMarker[] markers = findJaxrsMarkers(nameBinding);
		assertThat(markers.length, equalTo(0));
		final IProblem[] javaProblems = findJavaProblems(resource);
		
		// operation 2: now, use the quickfix to fix the problem
		final ICompilationUnit compilationUnit = nameBinding.getJavaElement().getCompilationUnit();
		final IJavaCompletionProposal completionProposal = new AddNameBindingTargetValuesCompletionProposal(compilationUnit,
				JaxrsMarkerResolutionGenerator.findEffectiveSourceRange(compilationUnit, new ProblemLocation(javaProblems[0])));
		JavaCompletionProposalUtils.applyCompletionProposal(completionProposal, nameBinding);
		
		// verification 2: revalidate, there should be 0 JAX-RS/Java error
		new JaxrsMetamodelValidator().validate(toSet(resource), project, validationHelper, context,
				validatorManager, reporter);
		assertThat(findJaxrsMarkers(nameBinding).length, equalTo(0));
		assertThat(findJavaProblems(resource).length, equalTo(0));
	}

	@Test
	public void shouldAddTargetAnnotationValueOnlyWhenImportsExistOnNameBinding() throws CoreException, ValidationException {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("AnotherNameBinding.txt", "org.jboss.tools.ws.jaxrs.sample.services", "AnotherNameBinding.java");
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.AnotherNameBinding", javaProject, "@Target({ ElementType.TYPE, ElementType.METHOD })", "@Target", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.AnotherNameBinding");
		final JaxrsNameBinding nameBinding = (JaxrsNameBinding) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.AnotherNameBinding", EnumElementCategory.NAME_BINDING);
		
		// operation 1: validate the Name Binding
		final IResource resource = nameBinding.getResource();
		new JaxrsMetamodelValidator().validate(toSet(resource), project, validationHelper, context,
				validatorManager, reporter);
		
		// verification 1: there should be 1 error: missing @Target annotation value
		final IMarker[] markers = findJaxrsMarkers(nameBinding);
		assertThat(markers.length, equalTo(0));
		final IProblem[] javaProblems = findJavaProblems(resource);
		
		// operation 2: now, use the quickfix to fix the problem
		final ICompilationUnit compilationUnit = nameBinding.getJavaElement().getCompilationUnit();
		final IJavaCompletionProposal completionProposal = new AddNameBindingTargetValuesCompletionProposal(compilationUnit, 
				JaxrsMarkerResolutionGenerator.findEffectiveSourceRange(compilationUnit, new ProblemLocation(javaProblems[0])));
		JavaCompletionProposalUtils.applyCompletionProposal(completionProposal, nameBinding);
		
		// verification 2: revalidate, there should be 0 JAX-RS/Java error
		new JaxrsMetamodelValidator().validate(toSet(resource), project, validationHelper, context,
				validatorManager, reporter);
		assertThat(findJaxrsMarkers(nameBinding).length, equalTo(0));
		assertThat(findJavaProblems(resource).length, equalTo(0));
	}
	
	@Test
	public void shouldUpdateRetentionAnnotationValueWhenMissingValueOnNameBinding() throws CoreException, ValidationException  {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("AnotherNameBinding.txt", "org.jboss.tools.ws.jaxrs.sample.services", "AnotherNameBinding.java");
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.AnotherNameBinding", javaProject, "@Target({ ElementType.TYPE, ElementType.METHOD })", "@Target(value=ElementType.TYPE)", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.AnotherNameBinding");
		final JaxrsNameBinding nameBinding = (JaxrsNameBinding) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.AnotherNameBinding", EnumElementCategory.NAME_BINDING);
		
		// operation 1: validate the Name Binding
		final IResource resource = nameBinding.getResource();
		new JaxrsMetamodelValidator().validate(toSet(resource), project, validationHelper, context,
				validatorManager, reporter);
		
		// verification 1: there should be 1 error: missing @Retention annotation value
		assertThat(findJavaProblems(resource).length, equalTo(0));
		final IMarker[] markers = findJaxrsMarkers(nameBinding);
		assertThat(markers.length, equalTo(1));
		
		// operation 2: now, use the quickfix to fix the problem
		final IJavaCompletionProposal completionProposal = new UpdateNameBindingTargetAnnotationValueMarkerResolution(nameBinding.getJavaElement());
		JavaCompletionProposalUtils.applyCompletionProposal(completionProposal, nameBinding);
		metamodelMonitor.processEvent(nameBinding.getJavaElement().getCompilationUnit(), IJavaElementDelta.CHANGED);
		
		// verification 2: revalidate, there should be 0 JAX-RS/Java error
		new JaxrsMetamodelValidator().validate(toSet(resource), project, validationHelper, context,
				validatorManager, reporter);
		assertThat(findJaxrsMarkers(nameBinding).length, equalTo(0));
		assertThat(findJavaProblems(resource).length, equalTo(0));
	}

	@Test
	public void shouldUpdateRetentionAnnotationValueWhenInvalidValueOnNameBinding() throws CoreException, ValidationException  {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("AnotherNameBinding.txt", "org.jboss.tools.ws.jaxrs.sample.services", "AnotherNameBinding.java");
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.AnotherNameBinding", javaProject, "@Target({ ElementType.TYPE, ElementType.METHOD })", "@Target(value=ElementType.FIELD)", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.AnotherNameBinding");
		final JaxrsNameBinding nameBinding = (JaxrsNameBinding) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.AnotherNameBinding", EnumElementCategory.NAME_BINDING);
		
		// operation 1: validate the Name Binding
		final IResource resource = nameBinding.getResource();
		new JaxrsMetamodelValidator().validate(toSet(resource), project, validationHelper, context,
				validatorManager, reporter);
		
		// verification 1: there should be 1 error: missing @Retention annotation value
		assertThat(findJavaProblems(resource).length, equalTo(0));
		final IMarker[] markers = findJaxrsMarkers(nameBinding);
		assertThat(markers.length, equalTo(1));
		
		// operation 2: now, use the quickfix to fix the problem
		final IJavaCompletionProposal completionProposal = new UpdateNameBindingTargetAnnotationValueMarkerResolution(nameBinding.getJavaElement());
		JavaCompletionProposalUtils.applyCompletionProposal(completionProposal, nameBinding);
		metamodelMonitor.processEvent(nameBinding.getJavaElement().getCompilationUnit(), IJavaElementDelta.CHANGED);
		
		// verification 2: revalidate, there should be 0 JAX-RS/Java error
		new JaxrsMetamodelValidator().validate(toSet(resource), project, validationHelper, context,
				validatorManager, reporter);
		assertThat(findJaxrsMarkers(nameBinding).length, equalTo(0));
		assertThat(findJavaProblems(resource).length, equalTo(0));
	}

	@Test
	public void shouldUpdateRetentionAnnotationValueWhenInvalidAndMissingValueOnNameBinding() throws CoreException, ValidationException  {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("AnotherNameBinding.txt", "org.jboss.tools.ws.jaxrs.sample.services", "AnotherNameBinding.java");
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.AnotherNameBinding", javaProject, "@Target({ ElementType.TYPE, ElementType.METHOD })", "@Target(value={ElementType.TYPE, ElementType.FIELD})", false);
		metamodelMonitor.createElements("org.jboss.tools.ws.jaxrs.sample.services.AnotherNameBinding");
		final JaxrsNameBinding nameBinding = (JaxrsNameBinding) metamodel.findElement("org.jboss.tools.ws.jaxrs.sample.services.AnotherNameBinding", EnumElementCategory.NAME_BINDING);
		
		// operation 1: validate the Name Binding
		final IResource resource = nameBinding.getResource();
		new JaxrsMetamodelValidator().validate(toSet(resource), project, validationHelper, context,
				validatorManager, reporter);
		
		// verification 1: there should be 1 error: missing @Retention annotation value
		assertThat(findJavaProblems(resource).length, equalTo(0));
		final IMarker[] markers = findJaxrsMarkers(nameBinding);
		assertThat(markers.length, equalTo(1));
		
		// operation 2: now, use the quickfix to fix the problem
		final IJavaCompletionProposal completionProposal = new UpdateNameBindingTargetAnnotationValueMarkerResolution(nameBinding.getJavaElement());
		JavaCompletionProposalUtils.applyCompletionProposal(completionProposal, nameBinding);
		metamodelMonitor.processEvent(nameBinding.getJavaElement().getCompilationUnit(), IJavaElementDelta.CHANGED);
		
		// verification 2: revalidate, there should be 0 JAX-RS/Java error
		new JaxrsMetamodelValidator().validate(toSet(resource), project, validationHelper, context,
				validatorManager, reporter);
		assertThat(findJaxrsMarkers(nameBinding).length, equalTo(0));
		assertThat(findJavaProblems(resource).length, equalTo(0));
	}

}
