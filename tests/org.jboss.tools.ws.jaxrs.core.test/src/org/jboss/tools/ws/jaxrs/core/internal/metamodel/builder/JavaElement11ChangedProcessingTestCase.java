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
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.IJavaElementDeltaFlag.F_SIGNATURE;
import static org.jboss.tools.ws.jaxrs.core.internal.utils.HamcrestExtras.flagMatches;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils.createAnnotation;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils.createMethod;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils.getAnnotation;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils.removeFirstOccurrenceOfCode;
import static org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils.replaceAllOccurrencesOfCode;
import static org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils.replaceFirstOccurrenceOfCode;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_CONSUMES_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_ELEMENT_KIND;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_HTTP_METHOD_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_METHOD_PARAMETERS;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_METHOD_RETURN_TYPE;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_PATH_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_PRODUCES_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_PROVIDER_HIERARCHY;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_TARGET_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.APPLICATION_PATH;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.CONSUMES;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.CONTEXT;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.GET;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.HTTP_METHOD;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.MATRIX_PARAM;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PATH;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PATH_PARAM;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.POST;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PRODUCES;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PROVIDER;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.QUERY_PARAM;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.hamcrest.Matchers;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsJavaApplication;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsProvider;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceField;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementCategory;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta;
import org.jboss.tools.ws.jaxrs.core.utils.Annotation;
import org.jboss.tools.ws.jaxrs.core.utils.JavaMethodParameter;
import org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames;
import org.jboss.tools.ws.jaxrs.core.utils.JdtUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class JavaElement11ChangedProcessingTestCase {

	private final static int ANY_EVENT_TYPE = 0;

	private final static int NO_FLAG = 0;

	@ClassRule
	public static WorkspaceSetupRule workspaceSetupRule = new WorkspaceSetupRule("org.jboss.tools.ws.jaxrs.tests.sampleproject");
	
	@Rule
	public JaxrsMetamodelMonitor metamodelMonitor = new JaxrsMetamodelMonitor(
			"org.jboss.tools.ws.jaxrs.tests.sampleproject", false);

	private JaxrsMetamodel metamodel = null;

	@Before
	public void setup() {
		metamodel = metamodelMonitor.getMetamodel();
		assertThat(metamodel, notNullValue());
	}

	private void processEvent(Annotation annotation, int deltaKind) throws CoreException {
		final JavaElementChangedEvent delta = new JavaElementChangedEvent(annotation.getJavaAnnotation(), deltaKind, ANY_EVENT_TYPE,
				JdtUtils.parse(((IMember) annotation.getJavaParent()), new NullProgressMonitor()), NO_FLAG);
		metamodel.processJavaElementChange(delta, new NullProgressMonitor());
	}

	private void processEvent(IMember element, int deltaKind) throws CoreException {
		final JavaElementChangedEvent delta = new JavaElementChangedEvent(element, deltaKind, ANY_EVENT_TYPE, JdtUtils.parse(element,
				new NullProgressMonitor()), NO_FLAG);
		metamodel.processJavaElementChange(delta, new NullProgressMonitor());
	}

	private void processEvent(IMember element, int deltaKind, int flags) throws CoreException {
		final JavaElementChangedEvent delta = new JavaElementChangedEvent(element, deltaKind, ANY_EVENT_TYPE, JdtUtils.parse(element,
				new NullProgressMonitor()), flags);
		metamodel.processJavaElementChange(delta, new NullProgressMonitor());
	}

	private void processEvent(ICompilationUnit element, int deltaKind) throws CoreException {
		final JavaElementChangedEvent delta = new JavaElementChangedEvent(element, deltaKind, ANY_EVENT_TYPE, JdtUtils.parse(element,
				new NullProgressMonitor()), NO_FLAG);
		metamodel.processJavaElementChange(delta, new NullProgressMonitor());
	}

	private void processEvent(IPackageFragmentRoot element, int deltaKind) throws CoreException {
		final JavaElementChangedEvent delta = new JavaElementChangedEvent(element, deltaKind, ANY_EVENT_TYPE, null, NO_FLAG);
		metamodel.processJavaElementChange(delta, new NullProgressMonitor());
	}

	@Test
	public void shouldAddElementsWhenAddingSourceFolder() throws CoreException {
		// pre-conditions
		final IPackageFragmentRoot sourceFolder = metamodelMonitor.resolvePackageFragmentRoot("src/main/java");
		// 6 Built-in HTTP Methods
		assertThat(metamodel.findAllElements().size(), equalTo(6));
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(sourceFolder, ADDED);
		// verifications
		// 37 elements: 1 Application + 2 custom HTTP
		// Method + 2 providers + 5 RootResources + 2 Subresources + all
		// their methods and fields..
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(38));
		assertThat(metamodelMonitor.getElementChanges(),
				everyItem(Matchers.<JaxrsElementDelta> hasProperty("deltaKind", equalTo(ADDED))));
		// 6 built-in HTTP Methods + all added items
		assertThat(metamodel.findAllElements().size(), equalTo(44));
	}

	@Test
	public void shouldAdd6HttpMethodsAnd0ResourceWhenAddingBinaryLib() throws CoreException {
		// pre-conditions
		final IPackageFragmentRoot lib = metamodelMonitor.resolvePackageFragmentRoot("lib/jaxrs-api-2.0.1.GA.jar");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(lib, ADDED);
		// verifications. Damned : none in the jar...
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(0));
		// 6 built-in HTTP Methods only
		assertThat(metamodel.findAllElements().size(), equalTo(6));
	}

	@Test
	public void shouldAddApplicationWhenAddingSourceCompilationUnit() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(type.getCompilationUnit(), ADDED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.APPLICATION));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(
				((JaxrsJavaApplication) metamodelMonitor.getElementChanges().get(0).getElement()).getApplicationPath(),
				equalTo("/app"));
		// 6 Built-in HTTP Methods + 1 Application
		assertThat(metamodel.findAllElements().size(), equalTo(7));
	}

	@Test
	public void shouldAddApplicationWhenAddingSourceType() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		// operation
		processEvent(type, ADDED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.APPLICATION));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(
				((JaxrsJavaApplication) metamodelMonitor.getElementChanges().get(0).getElement()).getApplicationPath(),
				equalTo("/app"));
		// 6 Built-in HTTP Methods + 1 Application
		assertThat(metamodel.findAllElements().size(), equalTo(7));
	}

	@Test
	public void shouldAddApplicationWhenAddingApplicationPathAnnotation() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final Annotation pathAnnotation = getAnnotation(type, APPLICATION_PATH);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(pathAnnotation, ADDED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.APPLICATION));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(
				((JaxrsJavaApplication) metamodelMonitor.getElementChanges().get(0).getElement()).getApplicationPath(),
				equalTo("/app"));
		// 6 Built-in HTTP Methods + 1 Application
		assertThat(metamodel.findAllElements().size(), equalTo(7));
	}

	@Test
	public void shouldDoNothingWhenAddingUnrelatedAnnotationOnApplication() throws CoreException {
		// pre-conditions
		final JaxrsJavaApplication application = metamodelMonitor
				.createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final Annotation suppressWarningAnnotation = getAnnotation(application.getJavaElement(),
				SuppressWarnings.class.getName());
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(suppressWarningAnnotation, ADDED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(0));
		// 6 Built-in HTTP Methods + 1 Application
		assertThat(metamodel.findAllElements().size(), equalTo(7));
	}

	@Test
	public void shouldUpdateApplicationWhenChangingAnnotationValue() throws CoreException {
		// pre-conditions
		final JaxrsJavaApplication application = metamodelMonitor.createJavaApplication(
				"org.jboss.tools.ws.jaxrs.sample.services.RestApplication", "/bar");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(application.getAnnotation(APPLICATION_PATH), CHANGED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.APPLICATION));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(
				((JaxrsJavaApplication) metamodelMonitor.getElementChanges().get(0).getElement()).getApplicationPath(),
				equalTo("/app"));
		// 6 Built-in HTTP Methods + 1 Application
		assertThat(metamodel.findAllElements().size(), equalTo(7));
	}

	@Test
	public void shouldDoNothingWhenAnnotationValueRemainsSameOnApplication() throws CoreException {
		// pre-conditions
		final JaxrsJavaApplication application = metamodelMonitor
				.createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(application.getAnnotation(APPLICATION_PATH), CHANGED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(0));
		// 6 Built-in HTTP Methods + 1 Application
		assertThat(metamodel.findAllElements().size(), equalTo(7));
	}

	@Test
	public void shouldDoNothingWhenChangingUnrelatedApplicationAnnotationValue() throws CoreException {
		// pre-conditions
		final JaxrsJavaApplication application = metamodelMonitor
				.createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final Annotation suppressWarningsAnnotation = getAnnotation(application.getJavaElement(),
				SuppressWarnings.class.getName());
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(suppressWarningsAnnotation, CHANGED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(0));
		// 6 Built-in HTTP Methods + 1 Application
		assertThat(metamodel.findAllElements().size(), equalTo(7));
	}

	@Test
	public void shouldRemoveApplicationWhenRemovingCompilationUnit() throws CoreException {
		// pre-conditions
		final JaxrsJavaApplication application = metamodelMonitor
				.createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(application.getJavaElement().getCompilationUnit(), REMOVED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.APPLICATION));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((JaxrsJavaApplication) metamodelMonitor.getElementChanges().get(0).getElement()),
				equalTo(application));
		// 6 Built-in HTTP Methods
		assertThat(metamodel.findAllElements().size(), equalTo(6));
	}

	@Test
	public void shouldRemoveApplicationWhenRemovingSourceType() throws CoreException {
		// pre-conditions
		final JaxrsJavaApplication application = metamodelMonitor
				.createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(application.getJavaElement(), REMOVED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.APPLICATION));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(
				((JaxrsJavaApplication) metamodelMonitor.getElementChanges().get(0).getElement()).getApplicationPath(),
				equalTo("/app"));
		// 6 Built-in HTTP Methods
		assertThat(metamodel.findAllElements().size(), equalTo(6));
	}

	@Test
	public void shouldNotRemoveApplicationWhenRemovingAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsJavaApplication application = metamodelMonitor
				.createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(application.getAnnotation(APPLICATION_PATH), REMOVED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.APPLICATION));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement(), is(notNullValue()));
		// 6 Built-in HTTP Methods + 1 Application
		assertThat(metamodel.findAllElements().size(), equalTo(7));
		assertThat(application.getApplicationPath(), nullValue());
	}

	@Test
	public void shouldRemoveApplicationWhenRemovingAnnotationAndHierarchyAlreadyMissing() throws CoreException {
		// pre-conditions
		/*
		 * final IType type = metamodelMonitor.resolveType(
		 * "org.jboss.tools.ws.jaxrs.sample.services.RestApplication" ); final
		 * Annotation appPathAnnotation = resolveAnnotation(type,
		 * APPLICATION_PATH); final JaxrsJavaApplication
		 * application = new JaxrsJavaApplication(type, appPathAnnotation,
		 * false); metamodel.add(application);
		 */
		final IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		replaceFirstOccurrenceOfCode(type, "extends Application", "", false);
		final JaxrsJavaApplication application = metamodelMonitor.createJavaApplication(
				"org.jboss.tools.ws.jaxrs.sample.services.RestApplication", false);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(application.getAnnotation(APPLICATION_PATH), REMOVED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement(), equalTo((IJaxrsElement) application));
		// 6 Built-in HTTP Methods
		assertThat(metamodel.findAllElements().size(), equalTo(6));

	}

	@Test
	public void shouldRemoveApplicationWhenRemovingHierarchyAndAnnotationAlreadyMissing() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		replaceFirstOccurrenceOfCode(type, "@ApplicationPath(\"/app\")", "", false);
		metamodelMonitor.createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		replaceFirstOccurrenceOfCode(type, "extends Application", "", false);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(type, CHANGED, IJavaElementDeltaFlag.F_SUPER_TYPES);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.APPLICATION));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement(), is(notNullValue()));
		// 6 Built-in HTTP Methods
		assertThat(metamodel.findAllElements().size(), equalTo(6));
	}

	@Test
	public void shouldDoNothingWhenRemovingUnrelatedAnnotationOnApplication() throws CoreException {
		// pre-conditions
		final JaxrsJavaApplication application = metamodelMonitor
				.createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(application.getJavaElement(), SuppressWarnings.class.getName()), REMOVED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(0));
		// 6 Built-in HTTP Methods + 1 Application
		assertThat(metamodel.findAllElements().size(), equalTo(7));
	}

	@Test
	public void shouldRemoveApplicationWhenRemovingSourceFolder() throws CoreException {
		// pre-conditions
		metamodelMonitor.createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final IPackageFragmentRoot sourceFolder = metamodelMonitor.resolvePackageFragmentRoot("src/main/java");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(sourceFolder, REMOVED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.APPLICATION));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement(), is(notNullValue()));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		// 6 Built-in HTTP Methods
		assertThat(metamodel.findAllElements().size(), equalTo(6));
	}

	@Test
	public void shouldAddHttpMethodWhenAddingSourceCompilationUnit() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BAR");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(type.getCompilationUnit(), ADDED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.HTTP_METHOD));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((JaxrsHttpMethod) metamodelMonitor.getElementChanges().get(0).getElement()).getHttpVerb(),
				equalTo("BAR"));
		// 6 Built-in HTTP Methods + 1 custom one
		assertThat(metamodel.findAllElements().size(), equalTo(7));
	}

	@Test
	public void shouldAddHttpMethodWhenAddingSourceType() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BAR");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(type, ADDED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.HTTP_METHOD));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((JaxrsHttpMethod) metamodelMonitor.getElementChanges().get(0).getElement()).getHttpVerb(),
				equalTo("BAR"));
		// 6 Built-in HTTP Methods + 1 custom one
		assertThat(metamodel.findAllElements().size(), equalTo(7));
	}

	@Test
	public void shouldAddHttpMethodWhenAddingHttpMethodAnnotation() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BAR");
		final Annotation httpMethodAnnotation = getAnnotation(type, HTTP_METHOD);
		metamodelMonitor.resetElementChangesNotifications();
		
		// operation
		processEvent(httpMethodAnnotation, ADDED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.HTTP_METHOD));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((JaxrsHttpMethod) metamodelMonitor.getElementChanges().get(0).getElement()).getHttpVerb(),
				equalTo("BAR"));
		// 6 Built-in HTTP Methods + 1 custom one
		assertThat(metamodel.findAllElements().size(), equalTo(7));
	}

	@Test
	public void shouldDoNothingWhenAddingUnrelatedAnnotationOnHttpMethod() throws CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = metamodelMonitor
				.createHttpMethod("org.jboss.tools.ws.jaxrs.sample.services.BAR");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(httpMethod.getJavaElement(), Target.class.getName()), ADDED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_TARGET_ANNOTATION));
		// one call, during pre-conditions
		// 6 Built-in HTTP Methods + 1 custom one
		assertThat(metamodel.findAllElements().size(), equalTo(7));
	}

	@Test
	public void shouldUpdateHttpMethodWhenChangingAnnotationValue() throws CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = metamodelMonitor.createHttpMethod(
				"org.jboss.tools.ws.jaxrs.sample.services.BAR", "BAZ");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(httpMethod.getJavaElement(), HTTP_METHOD), CHANGED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.HTTP_METHOD));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(((JaxrsHttpMethod) metamodelMonitor.getElementChanges().get(0).getElement()).getHttpVerb(),
				equalTo("BAR"));
		// 6 Built-in HTTP Methods + 1 custom one
		assertThat(metamodel.findAllElements().size(), equalTo(7));
	}

	@Test
	public void shouldDoNothingWhenAnnotationValueRemainsSameOnHttpMethod() throws CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = metamodelMonitor
				.createHttpMethod("org.jboss.tools.ws.jaxrs.sample.services.BAR");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(httpMethod.getJavaElement(), HTTP_METHOD), CHANGED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(0));
		// 6 Built-in HTTP Methods + 1 custom one
		assertThat(metamodel.findAllElements().size(), equalTo(7));
	}

	@Test
	public void shouldDoNothingWhenChangingUnrelatedHttpMethodAnnotationValue() throws CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = metamodelMonitor
				.createHttpMethod("org.jboss.tools.ws.jaxrs.sample.services.BAR");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final Annotation targetAnnotation = getAnnotation(httpMethod.getJavaElement(), Target.class.getName());
		processEvent(targetAnnotation, CHANGED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(0));
		// 6 Built-in HTTP Methods + 1 custom one
		assertThat(metamodel.findAllElements().size(), equalTo(7));
	}

	@Test
	public void shouldRemoveHttpMethodWhenRemovingCompilationUnit() throws CoreException {
		final JaxrsHttpMethod httpMethod = metamodelMonitor
				.createHttpMethod("org.jboss.tools.ws.jaxrs.sample.services.BAR");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(httpMethod.getJavaElement().getCompilationUnit(), REMOVED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.HTTP_METHOD));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((JaxrsHttpMethod) metamodelMonitor.getElementChanges().get(0).getElement()), equalTo(httpMethod));
		// 6 Built-in HTTP Methods
		assertThat(metamodel.findAllElements().size(), equalTo(6));
	}

	@Test
	public void shouldRemoveHttpMethodWhenRemovingSourceType() throws CoreException {
		final JaxrsHttpMethod httpMethod = metamodelMonitor
				.createHttpMethod("org.jboss.tools.ws.jaxrs.sample.services.BAR");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(httpMethod.getJavaElement(), REMOVED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.HTTP_METHOD));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((JaxrsHttpMethod) metamodelMonitor.getElementChanges().get(0).getElement()).getHttpVerb(),
				equalTo("BAR"));
		// 6 Built-in HTTP Methods
		assertThat(metamodel.findAllElements().size(), equalTo(6));
	}

	@Test
	public void shouldRemoveHttpMethodWhenRemovingAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = metamodelMonitor
				.createHttpMethod("org.jboss.tools.ws.jaxrs.sample.services.BAR");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(httpMethod.getJavaElement(), HTTP_METHOD), REMOVED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement(), equalTo((IJaxrsElement) httpMethod));
		// 6 Built-in HTTP Methods
		assertThat(metamodel.findAllElements().size(), equalTo(6));
	}

	@Test
	public void shouldDoNothingWhenRemovingUnrelatedAnnotationOnHttpMethod() throws CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = metamodelMonitor
				.createHttpMethod("org.jboss.tools.ws.jaxrs.sample.services.BAR");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(httpMethod.getJavaElement(), SuppressWarnings.class.getName()), REMOVED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(0));
		// 6 Built-in HTTP Methods + 1 custom one
		assertThat(metamodel.findAllElements().size(), equalTo(7));
	}

	@Test
	public void shouldRemoveHttpMethodWhenRemovingSourceFolder() throws CoreException {
		// pre-conditions
		metamodelMonitor.createHttpMethod("org.jboss.tools.ws.jaxrs.sample.services.BAR");
		final IPackageFragmentRoot sourceFolder = metamodelMonitor.resolvePackageFragmentRoot("src/main/java");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(sourceFolder, REMOVED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.HTTP_METHOD));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement(), is(notNullValue()));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		// 6 Built-in HTTP Methods
		assertThat(metamodel.findAllElements().size(), equalTo(6));
	}

	@Test
	public void shouldNotRemoveHttpMethodWhenRemovingBinaryLib() throws CoreException {
		// pre-conditions
		final IPackageFragmentRoot lib = metamodelMonitor.resolvePackageFragmentRoot("lib/jaxrs-api-2.0.1.GA.jar");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(lib, REMOVED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(0));
		assertThat(metamodel.findAllElements().size(), equalTo(6));
	}

	@Test
	public void shouldAddResourceWhenAddingSourceCompilationUnit() throws CoreException {
		// pre-conditions
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		processEvent(type.getCompilationUnit(), ADDED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(7));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((JaxrsResource) metamodelMonitor.getElementChanges().get(6).getElement()).getPathTemplate(),
				equalTo("/customers"));
		// includes 6 built-in HTTP Methods + 1 Resource and its
		// ResourceMethods/Fields
		assertThat(metamodel.findAllElements().size(), equalTo(13));
	}

	@Test
	public void shouldAddSubresourceWhenAddingSourceCompilationUnit() throws CoreException {
		// pre-conditions
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		processEvent(type.getCompilationUnit(), ADDED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(4));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((JaxrsResource) metamodelMonitor.getElementChanges().get(3).getElement()).getAllMethods().size(),
				equalTo(3));
		assertThat(((JaxrsResource) metamodelMonitor.getElementChanges().get(3).getElement()).getPathTemplate(),
				nullValue());
		// includes 6 Built-in HTTP Methods + Resource, ResourceMethods and
		// ResourceFields
		assertThat(metamodel.findAllElements().size(), equalTo(10));
	}

	@Test
	public void shouldAddSubresourceLocatorWhenAddingSourceCompilationUnit() throws CoreException {
		// pre-conditions
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		processEvent(type.getCompilationUnit(), ADDED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(5));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((JaxrsResource) metamodelMonitor.getElementChanges().get(4).getElement()).getAllMethods().size(),
				equalTo(1));
		assertThat(((JaxrsResource) metamodelMonitor.getElementChanges().get(4).getElement()).getPathTemplate(),
				equalTo("/products"));
		// includes 6 Built-in HTTP Methods + Resource, ResourceMethods and
		// ResourceFields
		assertThat(metamodel.findAllElements().size(), equalTo(11));
	}

	@Test
	public void shouldAddResourceWhenAddingSourceType() throws CoreException {
		// pre-conditions
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		processEvent(type, ADDED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(7));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((JaxrsResource) metamodelMonitor.getElementChanges().get(6).getElement()).getPathTemplate(),
				equalTo("/customers"));
		// includes 6 Built-in HTTP Methods + Resource, ResourceMethods and
		// ResourceFields
		assertThat(metamodel.findAllElements().size(), equalTo(13));
	}

	@Test
	public void shouldAddResourceWhenAddingPathAnnotation() throws CoreException {
		// pre-conditions
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final Annotation pathAnnotation = getAnnotation(type, PATH);
		processEvent(pathAnnotation, ADDED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(7));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(ADDED));
		final JaxrsResource resource = (JaxrsResource) metamodelMonitor.getElementChanges().get(6).getElement();
		assertThat(resource.getPathTemplate(), equalTo("/customers"));
		assertThat(resource.getConsumedMediaTypes(), equalTo(Arrays.asList("application/xml")));
		assertThat(resource.getProducedMediaTypes(), equalTo(Arrays.asList("application/xml", "application/json")));
		// includes 6 Built-in HTTP Methods + Resource, ResourceMethods and
		// ResourceFields
		assertThat(metamodel.findAllElements().size(), equalTo(13));
	}

	@Test
	public void shouldBecomeRootResourceWhenAddingPathAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		resource.removeAnnotation(getAnnotation(resource.getJavaElement(), PATH).getJavaAnnotation());
		assertThat(resource.isSubresource(), equalTo(true));
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final Annotation pathAnnotation = getAnnotation(resource.getJavaElement(), PATH);
		processEvent(pathAnnotation, ADDED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_ELEMENT_KIND
				+ F_PATH_ANNOTATION));
	}

	@Test
	public void shouldNotAddResourceWhenAddingUnrelatedAnnotation() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FooResource");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final Annotation consumesAnnotation = getAnnotation(type, CONSUMES);
		processEvent(consumesAnnotation, ADDED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(0));
		// includes 6 Built-in HTTP Methods
		assertThat(metamodel.findAllElements().size(), equalTo(6));
	}

	@Test
	public void shouldUpdateResourceWhenChangingPathAnnotationValue() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final Annotation pathAnnotation = createAnnotation(
				getAnnotation(resource.getJavaElement(), PATH), "/bar");
		resource.updateAnnotation(pathAnnotation);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(pathAnnotation, CHANGED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_PATH_ANNOTATION));
		assertThat((JaxrsResource) metamodelMonitor.getElementChanges().get(0).getElement(), equalTo(resource));
		// 6 built-in HTTP Methods + 1 Resource + 6 Resource Methods
		assertThat(metamodel.findAllElements().size(), equalTo(13));
	}

	@Test
	public void shouldUpdateResourceWhenAddingConsumesAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		resource.removeAnnotation(getAnnotation(resource.getJavaElement(), JaxrsClassnames.CONSUMES)
				.getJavaAnnotation());
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final Annotation consumesAnnotation = getAnnotation(resource.getJavaElement(), CONSUMES);
		processEvent(consumesAnnotation, ADDED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_CONSUMES_ANNOTATION));
		// 6 built-in HTTP Methods + 1 Resource + 6 Resource Methods
		assertThat(metamodel.findAllElements().size(), equalTo(13));
	}

	@Test
	public void shouldUpdateResourceWhenChangingConsumesAnnotationValue() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final Annotation consumesAnnotation = createAnnotation(
				getAnnotation(resource.getJavaElement(), CONSUMES), "application/foo");
		resource.updateAnnotation(consumesAnnotation);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(consumesAnnotation, CHANGED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_CONSUMES_ANNOTATION));
		// 6 built-in HTTP Methods + 1 Resource + 6 Resource Methods
		assertThat(metamodel.findAllElements().size(), equalTo(13));
	}

	@Test
	public void shouldUpdateResourceWhenRemovingConsumesAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(resource.getJavaElement(), CONSUMES), REMOVED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_CONSUMES_ANNOTATION));
		// 6 built-in HTTP Methods + 1 Resource + 6 Resource Methods
		assertThat(metamodel.findAllElements().size(), equalTo(13));
	}

	@Test
	public void shouldUpdateResourceWhenAddingProducesAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		resource.removeAnnotation(getAnnotation(resource.getJavaElement(), JaxrsClassnames.PRODUCES)
				.getJavaAnnotation());
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final Annotation producesAnnotation = getAnnotation(resource.getJavaElement(), PRODUCES);
		processEvent(producesAnnotation, ADDED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_PRODUCES_ANNOTATION));
		// 6 built-in HTTP Methods + 1 Resource + 6 Resource Methods
		assertThat(metamodel.findAllElements().size(), equalTo(13));
	}

	@Test
	public void shouldUpdateResourceWhenChangingProducesAnnotationValue() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final Annotation producesAnnotation = createAnnotation(
				getAnnotation(resource.getJavaElement(), PRODUCES), "application/foo");
		resource.updateAnnotation(producesAnnotation);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(producesAnnotation, CHANGED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_PRODUCES_ANNOTATION));
		// 6 built-in HTTP Methods + 1 Resource + 6 Resource Methods
		assertThat(metamodel.findAllElements().size(), equalTo(13));
	}

	@Test
	public void shouldUpdateResourceWhenRemovingProducesAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(resource.getJavaElement(), PRODUCES), REMOVED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_PRODUCES_ANNOTATION));
		// 6 built-in HTTP Methods + 1 Resource + 6 Resource Methods
		assertThat(metamodel.findAllElements().size(), equalTo(13));
	}

	@Test
	public void shouldAddResourceFieldWhenAddingPathParamAnnotationOnField() throws CoreException {
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		resource.getField("_pType").remove();
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final Annotation fieldAnnotation = getAnnotation(resource.getJavaElement().getField("_pType"),
				PATH_PARAM);
		processEvent(fieldAnnotation, ADDED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1)); // field
																				// only
		assertThat(metamodelMonitor.getElementChanges(),
				everyItem(Matchers.<JaxrsElementDelta> hasProperty("deltaKind", equalTo(ADDED))));
		// 6 built-in HTTP Methods + 1 resource + 1 method + 3 fields
		assertThat(metamodel.findAllElements().size(), equalTo(11));
	}

	@Test
	@Ignore
	public void shouldAddResourceFieldWhenAddingImportPathParam() throws CoreException {
		/*
		 * // pre-conditions final IType type = metamodelMonitor.resolveType(
		 * "org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator" );
		 * final JaxrsResource resource = new JaxrsResource(type, annotations,
		 * metamodel).build(); //metamodel.add(resource); final
		 * IImportDeclaration importDeclaration = getImportDeclaration(type,
		 * PathParam.getName()); // operation final JavaElementChangedEvent
		 * event = createEvent(importDeclaration, ADDED); final
		 * List<JaxrsElementChangedEvent> jaxrsElementDelta =
		 * processEvent(event, new NullProgressMonitor()); // verifications
		 * assertThat(jaxrsElementDelta.size(), equalTo(1));
		 * assertThat(jaxrsElementDelta.get(0).getDeltaKind(), equalTo(ADDED));
		 */
	}

	@Test
	public void shouldAddResourceFieldWhenAddingQueryParamAnnotationOnField() throws CoreException {
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		resource.getField("_foo").remove();
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final Annotation fieldAnnotation = getAnnotation(resource.getJavaElement().getField("_foo"),
				QUERY_PARAM);
		processEvent(fieldAnnotation, ADDED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1)); // field
																				// only
		assertThat(metamodelMonitor.getElementChanges(),
				everyItem(Matchers.<JaxrsElementDelta> hasProperty("deltaKind", equalTo(ADDED))));
		// 6 built-in HTTP Methods + 1 resource + 1 method + 3 fields
		assertThat(metamodel.findAllElements().size(), equalTo(11));
	}

	@Test
	public void shouldAddResourceFieldWhenAddingMatrixParamAnnotationOnField() throws CoreException {
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		resource.getField("_bar").remove();
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final Annotation fieldAnnotation = getAnnotation(resource.getJavaElement().getField("_bar"),
				MATRIX_PARAM);
		processEvent(fieldAnnotation, ADDED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1)); // field
																				// only
		assertThat(metamodelMonitor.getElementChanges(),
				everyItem(Matchers.<JaxrsElementDelta> hasProperty("deltaKind", equalTo(ADDED))));
		// 6 built-in HTTP Methods + 1 resource + 1 method + 3 fields
		assertThat(metamodel.findAllElements().size(), equalTo(11));
	}

	@Test
	public void shouldAddResourceFieldWhenAddingFieldAnnotatedWithPathParam() throws CoreException {
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		resource.getField("_pType").remove();
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final IField field = resource.getJavaElement().getField("_pType");
		processEvent(field, ADDED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1)); // field
																				// only
		assertThat(metamodelMonitor.getElementChanges(),
				everyItem(Matchers.<JaxrsElementDelta> hasProperty("deltaKind", equalTo(ADDED))));
		// 6 built-in HTTP Methods + 1 resource + 1 method + 3 fields
		assertThat(metamodel.findAllElements().size(), equalTo(11));
	}

	@Test
	public void shouldAddResourceFieldWhenAddingFieldAnnotatedWithQueryParam() throws CoreException {
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		resource.getField("_foo").remove();
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final IField field = resource.getJavaElement().getField("_foo");
		processEvent(field, ADDED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1)); // field
																				// only
		assertThat(metamodelMonitor.getElementChanges(),
				everyItem(Matchers.<JaxrsElementDelta> hasProperty("deltaKind", equalTo(ADDED))));
		// 6 built-in HTTP Methods + 1 resource + 1 method + 3 fields
		assertThat(metamodel.findAllElements().size(), equalTo(11));
	}

	@Test
	public void shouldAddResourceFieldWhenAddingFieldAnnotatedWithMatrixParam() throws CoreException {
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		resource.getField("_bar").remove();
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final IField field = resource.getJavaElement().getField("_bar");
		processEvent(field, ADDED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1)); // field
																				// only
		assertThat(metamodelMonitor.getElementChanges(),
				everyItem(Matchers.<JaxrsElementDelta> hasProperty("deltaKind", equalTo(ADDED))));
		// 6 built-in HTTP Methods + 1 resource + 1 method + 3 fields
		assertThat(metamodel.findAllElements().size(), equalTo(11));
	}

	@Test
	public void shouldDoNothingWhenAddingFieldWithAnyAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IField field = resource.getJavaElement().getField("entityManager");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(field, ADDED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(0));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findAllElements().size(), equalTo(13));
	}

	@Test
	public void shouldDoNothingWhenAddingUnrelatedAnnotationOnField() throws CoreException {
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IField field = resource.getJavaElement().getField("entityManager");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(field, "javax.persistence.PersistenceContext"), ADDED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(0));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findAllElements().size(), equalTo(13));
	}

	@Test
	public void shouldUpdateResourceFieldWhenChangingPathParamAnnotationValueOnField() throws CoreException {
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final Annotation fieldAnnotation = resource.getField("_pType").getAnnotation(PATH_PARAM);
		fieldAnnotation.update(createAnnotation(PATH_PARAM, "foobar"));
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(fieldAnnotation, CHANGED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		// 6 built-in HTTP Methods + 1 resource + 1 method + 3 fields
		assertThat(metamodel.findAllElements().size(), equalTo(11));
	}

	@Test
	public void shouldUpdateResourceFieldWhenChangingQueryParamAnnotationValueOnField() throws CoreException {
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final Annotation fieldAnnotation = resource.getField("_foo").getAnnotation(QUERY_PARAM);
		fieldAnnotation.update(createAnnotation(QUERY_PARAM, "foobar"));
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(fieldAnnotation, CHANGED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		// 6 built-in HTTP Methods + 1 resource + 1 method + 3 fields
		assertThat(metamodel.findAllElements().size(), equalTo(11));
	}

	@Test
	public void shouldUpdateResourceFieldWhenChangingMatrixParamAnnotationValueOnField() throws CoreException {
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final Annotation fieldAnnotation = resource.getField("_bar").getAnnotation(MATRIX_PARAM);
		fieldAnnotation.update(createAnnotation(MATRIX_PARAM, "foobar"));
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(fieldAnnotation, CHANGED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		// 6 built-in HTTP Methods + 1 resource + 1 method + 3 fields
		assertThat(metamodel.findAllElements().size(), equalTo(11));
	}

	@Test
	public void shouldDoNothingWhenChangingUnrelatedResourceFieldAnnotationValue() throws CoreException {
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final Annotation fieldAnnotation = getAnnotation(resource.getField("_bar").getJavaElement(),
				SuppressWarnings.class.getName());
		createAnnotation(fieldAnnotation, "foobar");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(fieldAnnotation, CHANGED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(0));
		// 6 built-in HTTP Methods + 1 resource + 1 method + 3 fields
		assertThat(metamodel.findAllElements().size(), equalTo(11));
	}

	@Test
	public void shouldRemoveResourceFieldWhenRemovingPathParamAnnotatedOnField() throws CoreException {
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final Annotation fieldAnnotation = resource.getField("_pType").getAnnotation(PATH_PARAM);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(fieldAnnotation, REMOVED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		// 6 built-in HTTP Methods + 1 resource + 1 method + 2 fields
		assertThat(metamodel.findAllElements().size(), equalTo(10));
	}

	@Test
	public void shouldRemoveResourceFieldWhenRemovingQueryParamAnnotationOnField() throws CoreException {
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final Annotation fieldAnnotation = resource.getField("_foo").getAnnotation(QUERY_PARAM);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(fieldAnnotation, REMOVED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		// 6 built-in HTTP Methods + 1 resource + 1 method + 2 fields
		assertThat(metamodel.findAllElements().size(), equalTo(10));
	}

	@Test
	public void shouldRemoveResourceFieldWhenRemovingMatrixParamAnnotationOnField() throws CoreException {
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final Annotation fieldAnnotation = resource.getField("_bar").getAnnotation(MATRIX_PARAM);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(fieldAnnotation, REMOVED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		// 6 built-in HTTP Methods + 1 resource + 1 method + 2 fields
		assertThat(metamodel.findAllElements().size(), equalTo(10));
	}

	@Test
	public void shouldRemoveResourceFieldWhenRemovingFieldAnnotatedWithPathParam() throws CoreException {
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final JaxrsResourceField field = resource.getField("_pType");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(field.getJavaElement(), REMOVED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		// 6 built-in HTTP Methods + 1 resource + 1 method + 2 fields
		assertThat(metamodel.findAllElements().size(), equalTo(10));
	}

	@Test
	public void shouldRemoveResourceFieldWhenRemovingFieldAnnotatedWithQueryParam() throws CoreException {
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final JaxrsResourceField field = resource.getField("_foo");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(field.getJavaElement(), REMOVED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		// 6 built-in HTTP Methods + 1 resource + 1 method + 2 fields
		assertThat(metamodel.findAllElements().size(), equalTo(10));
	}

	@Test
	public void shouldRemoveResourceFieldWhenRemovingFieldAnnotatedWithMatrixParam() throws CoreException {
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final JaxrsResourceField field = resource.getField("_bar");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(field.getJavaElement(), REMOVED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		// 6 built-in HTTP Methods + 1 resource + 1 method + 2 fields
		assertThat(metamodel.findAllElements().size(), equalTo(10));
	}

	@Test
	public void shouldDoNothingWhenRemovingUnrelatedAnnotationOnField() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IField field = resource.getJavaElement().getField("entityManager");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(field, REMOVED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(0));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findAllElements().size(), equalTo(13));
	}

	@Test
	public void shouldDoNothingWhenAnnotationValueRemainsSameOnResource() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(resource.getJavaElement(), PATH), CHANGED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(0));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findAllElements().size(), equalTo(13));
	}

	@Test
	public void shouldDoNothingWhenChangingUnrelatedResourceAnnotationValue() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final Annotation suppressWarningsAnnotation = getAnnotation(resource.getJavaElement(),
				SuppressWarnings.class.getName());
		createAnnotation(suppressWarningsAnnotation, "foobar");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(suppressWarningsAnnotation, CHANGED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(0));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findAllElements().size(), equalTo(13));
	}

	@Test
	public void shouldRemoveResourceWhenRemovingCompilationUnit() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(resource.getJavaElement().getCompilationUnit(), REMOVED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(7));
		assertThat(metamodelMonitor.getElementChanges(),
				everyItem(Matchers.<JaxrsElementDelta> hasProperty("deltaKind", equalTo(REMOVED))));
		// 6 built-in HTTP Methods
		assertThat(metamodel.findAllElements().size(), equalTo(6));
	}

	@Test
	public void shouldRemoveResourceWhenRemovingSourceType() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(resource.getJavaElement(), REMOVED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(7));
		assertThat(metamodelMonitor.getElementChanges(),
				everyItem(Matchers.<JaxrsElementDelta> hasProperty("deltaKind", equalTo(REMOVED))));
		// 6 built-in HTTP Methods
		assertThat(metamodel.findAllElements().size(), equalTo(6));
	}

	@Test
	public void shouldRemoveResourceWhenNoMethodAndRemovingPathAnnotations() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final List<JaxrsResourceMethod> resourceMethods = new ArrayList<JaxrsResourceMethod>(resource.getMethods()
				.values());
		for (JaxrsResourceMethod resourceMethod : resourceMethods) {
			resource.removeMethod(resourceMethod);
		}
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(resource.getJavaElement(), PATH), REMOVED);
		// verifications : resource removed, since it has no field nor method
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((JaxrsResource) metamodelMonitor.getElementChanges().get(0).getElement()), equalTo(resource));
		// 6 built-in HTTP Methods
		assertThat(metamodel.findAllElements().size(), equalTo(6));
	}

	@Test
	public void shouldRemoveResourceMethodWhenRemovingJavaMethod() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod resourceMethod = metamodelMonitor.resolveResourceMethod(resource, "createCustomer");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(resourceMethod.getJavaElement(), REMOVED);
		// verifications : resource removed, since it has no field nor method
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findAllElements().size(), equalTo(12));
	}

	@Test
	public void shouldBecomeSubresourceWhenRemovingPathAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final Annotation pathAnnotation = getAnnotation(resource.getJavaElement(), PATH);
		processEvent(pathAnnotation, REMOVED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_ELEMENT_KIND
				+ F_PATH_ANNOTATION));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findAllElements().size(), equalTo(13));
	}

	@Test
	public void shouldDoNothingWhenRemovingUnrelatedAnnotationOnResource() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(resource.getJavaElement(), SuppressWarnings.class.getName()), REMOVED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(0));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findAllElements().size(), equalTo(13));
	}

	@Test
	public void shouldRemoveResourceWhenRemovingSourceFolder() throws CoreException {
		// pre-conditions
		metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final IPackageFragmentRoot sourceFolder = metamodelMonitor.resolvePackageFragmentRoot("src/main/java");
		processEvent(sourceFolder, REMOVED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(7));
		// 6 built-in HTTP Methods
		assertThat(metamodel.findAllElements().size(), equalTo(6));
	}

	@Test
	@Ignore
	public void shouldRemoveResourceWhenRemovingBinaryLib() throws CoreException {
		// need to package a JAX-RS resource into a jar...
	}

	@Test
	public void shouldAddResourceMethodWhenAddingBuiltinHttpMethodAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		metamodelMonitor.removeResourceMethod(resource, "createCustomer");
		final IMethod method = metamodelMonitor.resolveMethod(resource.getJavaElement(), "createCustomer");
		final Annotation postAnnotation = getAnnotation(method, POST);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(postAnnotation, ADDED);
		// verifications
		// method only
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1)); 
		assertThat(metamodelMonitor.getElementChanges(),
				everyItem(Matchers.<JaxrsElementDelta> hasProperty("deltaKind", equalTo(ADDED))));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findAllElements().size(), equalTo(13));
	}
	
	@Test
	public void shouldAddResourceMethodWhenAnnotationBecomesHttpMethod() throws CoreException {
		// precondition: @BAR exists but is not an HTTP Method yet
		final IType fooType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BAR");
		ResourcesUtils.replaceAllOccurrencesOfCode(fooType.getCompilationUnit(), "@HttpMethod(\"BAR\")", "", false);
		// precondition: annotate a method with @BAR, the method is not a JAX-RS Resource Method yet
		final IType customerType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		ResourcesUtils.replaceAllOccurrencesOfCode(customerType.getCompilationUnit(), "@POST", "@BAR", false);
		final JaxrsResource resource = metamodelMonitor.createResource(customerType);
		final IMethod method = metamodelMonitor.resolveMethod(resource.getJavaElement(), "createCustomer");
		assertThat(resource.getAllMethods().size(), equalTo(5));
		assertThat(resource.getMethods().get(method.getHandleIdentifier()), nullValue());
		// operation: add @HttpMethod annotation on BAR
		ResourcesUtils.replaceAllOccurrencesOfCode(fooType.getCompilationUnit(), "public @interface BAR", "@HttpMethod(\"BAR\") public @interface BAR", false);
		processEvent(fooType, CHANGED);
		// verification: method became a JAX-RS Resource Method
		assertThat(resource.getAllMethods().size(), equalTo(6));
		assertThat(resource.getMethods().get(method.getHandleIdentifier()), notNullValue());
	}

	@Test
	public void shouldRemoveResourceMethodWhenHttpMethodRemovedOnAnnotation() throws CoreException {
		// precondition: @BAR exists and is already an HTTP Method
		final IType fooType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		metamodelMonitor.createHttpMethod(fooType);
		// precondition: annotate a method with @BAR, the method is a JAX-RS Resource Method
		final IType customerType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		ResourcesUtils.replaceAllOccurrencesOfCode(customerType.getCompilationUnit(), "@POST", "@FOO", false);
		final JaxrsResource resource = metamodelMonitor.createResource(customerType);
		final IMethod method = metamodelMonitor.resolveMethod(resource.getJavaElement(), "createCustomer");
		assertThat(resource.getAllMethods().size(), equalTo(6));
		assertThat(resource.getMethods().get(method.getHandleIdentifier()), notNullValue());
		// operation: remove @HttpMethod annotation on FOO
		ResourcesUtils.replaceAllOccurrencesOfCode(fooType.getCompilationUnit(), "@HttpMethod(\"FOO\")", "", false);
		processEvent(fooType, CHANGED);
		// verification: method is not a JAX-RS Resource Method anymore
		assertThat(resource.getAllMethods().size(), equalTo(5));
		assertThat(resource.getMethods().get(method.getHandleIdentifier()), nullValue());
	}
	
	@Test
	public void shouldAddResourceMethodWhenAddingAnnotatedMethod() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		metamodelMonitor.removeResourceMethod(resource, "createCustomer");
		final IMethod method = metamodelMonitor.resolveMethod(resource.getJavaElement(), "createCustomer");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(method, ADDED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1)); 
		assertThat(metamodelMonitor.getElementChanges(),
				everyItem(Matchers.<JaxrsElementDelta> hasProperty("deltaKind", equalTo(ADDED))));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findAllElements().size(), equalTo(13));
	}

	@Test
	public void shouldAddSubresourceLocatorWhenAddingPathAnnotation() throws CoreException {
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		metamodelMonitor.removeResourceMethod(resource, "getProductResourceLocator");
		final IMethod method = metamodelMonitor.resolveMethod(resource.getJavaElement(), "getProductResourceLocator");
		final Annotation pathAnnotation = getAnnotation(method, PATH);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(pathAnnotation, ADDED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1)); // field
																				// only
		assertThat(metamodelMonitor.getElementChanges(),
				everyItem(Matchers.<JaxrsElementDelta> hasProperty("deltaKind", equalTo(ADDED))));
		// 6 built-in HTTP Methods + 1 resource + 1 method + 3 fields
		assertThat(metamodel.findAllElements().size(), equalTo(11));
	}

	@Test
	public void shouldRemoveResourceMethodWhenRemovingHttpMethodAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod resourceMethod = metamodelMonitor.resolveResourceMethod(resource, "createCustomer");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(resourceMethod.getJavaElement(), POST), REMOVED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		// 6 built-in HTTP Methods + 1 resource + 5 methods
		assertThat(metamodel.findAllElements().size(), equalTo(12));
	}

	@Test
	public void shouldConvertIntoSubresourceMethodWhenAddingPathAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod resourceMethod = metamodelMonitor.resolveResourceMethod(resource, "getCustomer");
		resourceMethod.removeAnnotation(resourceMethod.getAnnotation(PATH).getJavaAnnotation());
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final Annotation pathAnnotation = getAnnotation(resourceMethod.getJavaElement(), PATH);
		processEvent(pathAnnotation, ADDED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_PATH_ANNOTATION
				+ F_ELEMENT_KIND));
		// 6 built-in HTTP Methods + 1 resource + 5 methods
		assertThat(metamodel.findAllElements().size(), equalTo(13));
	}

	@Test
	public void shouldConvertIntoResourceMethodWhenRemovingPathAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod resourceMethod = metamodelMonitor.resolveResourceMethod(resource, "getCustomer");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(resourceMethod.getJavaElement(), PATH), REMOVED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_PATH_ANNOTATION
				+ F_ELEMENT_KIND));
		// 6 built-in HTTP Methods + 1 resource + 5 methods
		assertThat(metamodel.findAllElements().size(), equalTo(13));
	}

	@Test
	public void shouldConvertIntoSubresourceLocatorWhenRemovingHttpMethodAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod resourceMethod = metamodelMonitor.resolveResourceMethod(resource, "getCustomer");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(resourceMethod.getJavaElement(), GET), REMOVED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_HTTP_METHOD_ANNOTATION
				+ F_ELEMENT_KIND));
		// 6 built-in HTTP Methods + 1 resource + 5 methods
		assertThat(metamodel.findAllElements().size(), equalTo(13));
	}

	@Test
	public void shouldRemoveSubresourceLocatorWhenRemovingPathAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final IMethod getProductResourceLocatorMethod = metamodelMonitor.resolveMethod(resource.getJavaElement(),
				"getProductResourceLocator");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(getProductResourceLocatorMethod, PATH), REMOVED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		// 6 built-in HTTP Methods + 1 resource + 3 fields
		assertThat(metamodel.findAllElements().size(), equalTo(10));
	}

	@Test
	public void shouldUpdateSubresourceMethodWhenChangingPathAnnotationValue() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod getCustomerMethod = metamodelMonitor.resolveMethod(resource.getJavaElement(), "getCustomer");
		replaceFirstOccurrenceOfCode(getCustomerMethod, "@Path(\"{id}\")", "@Path(\"foo\")", true);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(getCustomerMethod, PATH), CHANGED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_PATH_ANNOTATION));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findAllElements().size(), equalTo(13));
	}

	@Test
	public void shouldUpdateResourceMethodWhenAddingConsumesAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod resourceMethod = metamodelMonitor.resolveResourceMethod(resource, "createCustomer");
		resourceMethod.removeAnnotation(resourceMethod.getAnnotation(CONSUMES).getJavaAnnotation());
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final Annotation consumesAnnotation = getAnnotation(resourceMethod.getJavaElement(), CONSUMES);
		processEvent(consumesAnnotation, ADDED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_CONSUMES_ANNOTATION));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findAllElements().size(), equalTo(13));
	}

	@Test
	public void shouldUpdateResourceMethodWhenChangingConsumesAnnotationValue() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod createCustomerMethod = metamodelMonitor.resolveMethod(resource.getJavaElement(), "createCustomer");
		replaceFirstOccurrenceOfCode(createCustomerMethod, "@Consumes(MediaType.APPLICATION_XML)",
				"@Consumes(\"application/foo\")", true);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(createCustomerMethod, CONSUMES), CHANGED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_CONSUMES_ANNOTATION));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findAllElements().size(), equalTo(13));
	}

	@Test
	public void shouldUpdateResourceMethodMethodWhenRemovingConsumesAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod resourceMethod = metamodelMonitor.resolveResourceMethod(resource, "createCustomer");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(resourceMethod.getJavaElement(), CONSUMES), REMOVED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_CONSUMES_ANNOTATION));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findAllElements().size(), equalTo(13));
	}

	@Test
	public void shouldUpdateResourceMethodWhenAddingProducesAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod resourceMethod = metamodelMonitor.resolveResourceMethod(resource, "getCustomerAsVCard");
		final Annotation producesAnnotation = getAnnotation(resourceMethod.getJavaElement(), PRODUCES);
		resourceMethod.removeAnnotation(producesAnnotation.getJavaAnnotation());
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(producesAnnotation, ADDED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_PRODUCES_ANNOTATION));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findAllElements().size(), equalTo(13));
	}

	@Test
	public void shouldUpdateResourceMethodWhenChangingProducesAnnotationValue() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod getCustomerAsVCardMethod = metamodelMonitor.resolveMethod(resource.getJavaElement(), "getCustomerAsVCard");
		replaceFirstOccurrenceOfCode(getCustomerAsVCardMethod, "@Produces({ \"text/x-vcard\" })",
				"@Produces(\"text/foo\")", true);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(getCustomerAsVCardMethod, PRODUCES), CHANGED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_PRODUCES_ANNOTATION));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findAllElements().size(), equalTo(13));
	}

	@Test
	public void shouldUpdateResourceMethodWhenRemovingProducesAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod resourceMethod = metamodelMonitor.resolveResourceMethod(resource, "getCustomerAsVCard");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final Annotation producesAnnotation = getAnnotation(resourceMethod.getJavaElement(), PRODUCES);
		processEvent(producesAnnotation, REMOVED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_PRODUCES_ANNOTATION));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findAllElements().size(), equalTo(13));
	}

	@Test
	public void shouldUpdateResourceMethodWhenAddingParameterWithPathParamAnnotation() throws CoreException {
		// test is not relevant : the old method is replaced by a new one
	}

	@Test
	public void shouldUpdateResourceMethodWhenAddingPathParamAnnotationOnParameter() throws CoreException {
		// the method signature is changed
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod resourceMethod = metamodelMonitor.resolveResourceMethod(resource, "getCustomer");
		final JavaMethodParameter javaMethodParameter = resourceMethod.getJavaMethodParameterByName("id");
		final Annotation annotation = javaMethodParameter.getAnnotation(PATH_PARAM);
		javaMethodParameter.removeAnnotation(annotation);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(resourceMethod.getJavaElement(), CHANGED, F_SIGNATURE);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_METHOD_PARAMETERS));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findAllElements().size(), equalTo(13));
	}

	@Test
	public void shouldUpdateResourceMethodWhenChangingPathParamAnnotationOnParameter() throws CoreException {
		// the method signature is changed
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod resourceMethod = metamodelMonitor.resolveResourceMethod(resource, "getCustomer");
		final JavaMethodParameter javaMethodParameter = resourceMethod.getJavaMethodParameterByName("id");
		final Annotation annotation = javaMethodParameter.getAnnotation(PATH_PARAM);
		annotation.update(createAnnotation(annotation, "foo"));
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(resourceMethod.getJavaElement(), CHANGED, F_SIGNATURE);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_METHOD_PARAMETERS));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findAllElements().size(), equalTo(13));
	}

	@Test
	public void shouldUpdateResourceMethodWhenRemovingPathParamAnnotationOnParameter() throws CoreException {
		// precondition: the method signature is changed
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		// JAX-RS Resource Method with an extra @PathParam annotation on the
		// 'update' parameter.
		final JaxrsResourceMethod resourceMethod = metamodelMonitor.resolveResourceMethod(resource, "updateCustomer");
		resourceMethod.getJavaMethodParameterByName("update")
				.addAnnotation(createAnnotation(PATH_PARAM, "foo"));
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(resourceMethod.getJavaElement(), CHANGED, F_SIGNATURE);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_METHOD_PARAMETERS));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findAllElements().size(), equalTo(13));
	}

	@Test
	public void shouldUpdateResourceMethodWhenAddingParameterWithQueryParamAnnotation() throws CoreException {
		// the method signature is changed
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		// JAX-RS Resource Method (size param is not declared)
		final JaxrsResourceMethod resourceMethod = metamodelMonitor.resolveResourceMethod(resource, "getCustomers");
		resourceMethod.removeJavaMethodParameter("size");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(resourceMethod.getJavaElement(), CHANGED, F_SIGNATURE);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_METHOD_PARAMETERS));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.RESOURCE_METHOD));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findAllElements().size(), equalTo(13));
	}

	@Test
	public void shouldUpdateResourceMethodWhenAddingParameterWithMatrixParamAnnotation() throws CoreException {
		// the method signature is changed
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource"
			);
		final JaxrsResourceMethod resourceMethod = metamodelMonitor.resolveResourceMethod(resource, "getPicture");
		resourceMethod.removeJavaMethodParameter("c");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(resourceMethod.getJavaElement(), CHANGED, F_SIGNATURE);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(),
				flagMatches(F_METHOD_PARAMETERS));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind()
				.getCategory(), equalTo(EnumElementCategory.RESOURCE_METHOD));
		// 6 built-in HTTP Methods + 1 resource + 3 methods
		assertThat(metamodel.findAllElements().size(), equalTo(10));
	}

	@Test
	public void shouldUpdateResourceMethodWhenAddingQueryParamAnnotationOnParameter() throws CoreException {
		// the method signature is changed
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod resourceMethod = metamodelMonitor.resolveResourceMethod(resource, "getCustomers");
		resourceMethod.getJavaMethodParameterByName("size").removeAnnotation(QUERY_PARAM);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(resourceMethod.getJavaElement(), CHANGED, F_SIGNATURE);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_METHOD_PARAMETERS));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findAllElements().size(), equalTo(13));
	}

	@Test
	public void shouldUpdateResourceMethodWhenChangingQueryParamAnnotationOnParameter() throws CoreException {
		// the method signature is changed
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		// JAX-RS Resource Method (QueryParam value is different: "length" vs
		// "size" on second param)
		final JaxrsResourceMethod resourceMethod = metamodelMonitor.resolveResourceMethod(resource, "getCustomers");
		resourceMethod.getJavaMethodParameterByName("size").getAnnotation(QUERY_PARAM)
				.update(createAnnotation(QUERY_PARAM, "length"));
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(resourceMethod.getJavaElement(), CHANGED, F_SIGNATURE);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_METHOD_PARAMETERS));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findAllElements().size(), equalTo(13));
	}

	@Test
	public void shouldUpdateResourceMethodWhenRemovingQueryParamAnnotationOnParameter() throws CoreException {
		// the method signature is changed
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		// JAX-RS Resource Method (with an extra @Queryparam annotation on
		// 'uriInfo' param)
		final JaxrsResourceMethod resourceMethod = metamodelMonitor.resolveResourceMethod(resource, "getCustomers");
		resourceMethod.getJavaMethodParameterByName("uriInfo").addAnnotation(
				createAnnotation(QUERY_PARAM, "foo"));
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(resourceMethod.getJavaElement(), CHANGED, F_SIGNATURE);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_METHOD_PARAMETERS));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findAllElements().size(), equalTo(13));
	}

	@Test
	public void shouldUpdateResourceMethodWhenAddingMatrixParamAnnotationOnParameter() throws CoreException {
		// the method signature is changed
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource"
			);
		final JaxrsResourceMethod resourceMethod = metamodelMonitor.resolveResourceMethod(resource, "getPicture");
		resourceMethod.getJavaMethodParameterByName("c").removeAnnotation(MATRIX_PARAM);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(resourceMethod.getJavaElement(), CHANGED, F_SIGNATURE);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(),
				flagMatches(F_METHOD_PARAMETERS));
		// 6 built-in HTTP Methods + 1 resource + 3 methods
		assertThat(metamodel.findAllElements().size(), equalTo(10));
	}

	@Test
	public void shouldUpdateResourceMethodWhenChangingMatrixParamAnnotationOnParameter() throws CoreException {
		// the method signature is changed
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource"
			);
		// JAX-RS Resource Method (MATRIX_PARAM value is
		// different: "foo" vs "color" on second param)
		final JaxrsResourceMethod resourceMethod = metamodelMonitor.resolveResourceMethod(resource, "getPicture");
		resourceMethod.getJavaMethodParameterByName("c").getAnnotation(MATRIX_PARAM)
				.update(createAnnotation(MATRIX_PARAM, "foo"));
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(resourceMethod.getJavaElement(), CHANGED, F_SIGNATURE);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(),
				flagMatches(F_METHOD_PARAMETERS));
		// 6 built-in HTTP Methods + 1 resource + 3 methods
		assertThat(metamodel.findAllElements().size(), equalTo(10));
	}

	@Test
	public void shouldUpdateResourceMethodWhenRemovingMatrixParamAnnotationOnParameter() throws CoreException {
		// the method signature is changed
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource"
			);
		// JAX-RS Resource Method (an extra MATRIX_PARAM
		// annotation on 'id' param)
		final JaxrsResourceMethod resourceMethod = metamodelMonitor.resolveResourceMethod(resource, "getProduct");
		resourceMethod.getJavaMethodParameterByName("id").addAnnotation(createAnnotation(MATRIX_PARAM, "foo"));
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(resourceMethod.getJavaElement(), CHANGED, F_SIGNATURE);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(),
				flagMatches(F_METHOD_PARAMETERS));
		// 6 built-in HTTP Methods + 1 resource + 3 methods
		assertThat(metamodel.findAllElements().size(), equalTo(10));
	}

	@Test
	public void shouldUpdateResourceMethodWhenChangingReturnType() throws CoreException {
		// pre-conditions
		// the method signature is changed
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		// JAX-RS Resource Method (declared return type is
		// 'javax.ws.rs.Response')
		final JaxrsResourceMethod resourceMethod = metamodelMonitor.resolveResourceMethod(resource, "getCustomer");
		resourceMethod.setReturnedType(metamodelMonitor.resolveType("java.lang.Object"));
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(resourceMethod.getJavaElement(), CHANGED, F_SIGNATURE);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		// annotations were not provided but are retrieved...
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_METHOD_RETURN_TYPE));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findAllElements().size(), equalTo(13));
	}

	@Test
	public void shouldDoNothingMethodWhenAddingParamWithUnrelatedAnnotation() throws CoreException {
		// test is not relevant : the old method is replaced by a new one
	}

	@Test
	@Ignore("For now, accept all annotations")
	public void shouldDoNothingMethodWhenAddingUnrelatedAnnotationOnParameter() throws CoreException {
		// pre-conditions
		// the method signature is changed: @Context is not declared on
		// 'uriInfo' param
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod resourceMethod = metamodelMonitor.resolveResourceMethod(resource, "getCustomers");
		resourceMethod.getJavaMethodParameterByName("uriInfo").removeAnnotation(CONTEXT);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(resourceMethod.getJavaElement(), CHANGED, F_SIGNATURE);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(0));
		assertThat(metamodel.findAllElements().size(), equalTo(1));
	}

	@Test
	@Ignore("For now, accept all annotations")
	public void shouldDoNothingMethodWhenChangingUnrelatedAnnotationOnParameter() throws CoreException {
		fail("Not implemented yet");
	}

	@Test
	@Ignore("For now, accept all annotations")
	public void shouldDoNothingMethodWhenRemovingUnrelatedAnnotationOnParameter() throws CoreException {
		fail("Not implemented yet");
	}

	@Test
	public void shouldDoNothingWhenAddingBasicJavaMethod() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod method = metamodelMonitor.resolveMethod(resource.getJavaElement(), "getEntityManager");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(method, ADDED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(0));
	}

	@Test
	public void shouldDoNothingWhenChangingBasicJavaMethod() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod method = metamodelMonitor.resolveMethod(resource.getJavaElement(), "getEntityManager");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(method, CHANGED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(0));
	}

	@Test
	public void shouldDoNothingWhenRemovingBasicJavaMethod() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod method = metamodelMonitor.resolveMethod(resource.getJavaElement(), "getEntityManager");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(method, REMOVED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(0));
	}

	@Test
	public void shouldDoFailWhenDuplicatingJavaMethod() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod method = metamodelMonitor.resolveMethod(resource.getJavaElement(), "getCustomer");
		metamodelMonitor.resetElementChangesNotifications();
		// before operation: metamodel has 6 built-in HTTP Methods + 1 resource
		// + 6 methods
		assertThat(metamodel.findAllElements().size(), equalTo(13));
		// operation
		final IMethod addedMethod = createMethod(method.getDeclaringType(), method.getSource(), true);
		processEvent(addedMethod, ADDED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(0));
		// after operation: metamodel has still 6 built-in HTTP Methods + 1
		// resource + 6 methods
		assertThat(metamodel.findAllElements().size(), equalTo(13));
	}

	@Test
	@Ignore
	public void shouldUpdateResourceMethodWhenAddingThrowsException() throws CoreException {
		fail("Not implemented yet - postponed along with support for providers");
	}

	@Test
	@Ignore
	public void shouldUpdateResourceMethodWhenChangingThrowsException() throws CoreException {
		fail("Not implemented yet - postponed along with support for providers");
	}

	@Test
	@Ignore
	public void shouldUpdateResourceMethodWhenRemovingThrowsException() throws CoreException {
		fail("Not implemented yet - postponed along with support for providers");
	}

	@Test
	public void shouldAddProviderWhenAddingSourceCompilationUnit() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(type.getCompilationUnit(), ADDED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.PROVIDER));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(ADDED));
		// 6 built-in HTTP Methods + 1 Provider
		assertThat(metamodel.findAllElements().size(), equalTo(7));
	}

	@Test
	public void shouldAddProviderWhenAddingSourceType() throws CoreException {
		// pre-conditions
		final IType providerType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(providerType, ADDED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		final JaxrsElementDelta delta = metamodelMonitor.getElementChanges().get(0);
		assertThat(delta.getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.PROVIDER));
		assertThat(delta.getDeltaKind(), equalTo(ADDED));
		assertThat(((JaxrsProvider) delta.getElement()).getProvidedType(EnumElementKind.EXCEPTION_MAPPER),
				equalTo(metamodelMonitor.resolveType("javax.persistence.EntityNotFoundException")));
		// 6 built-in HTTP Methods + 1 Provider
		assertThat(metamodel.findAllElements().size(), equalTo(7));
	}

	@Test
	public void shouldAddProviderWhenAddingProviderAnnotationAndHierarchyIsMissing() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		removeFirstOccurrenceOfCode(type, "implements ExceptionMapper<EntityNotFoundException>", false);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final Annotation providerAnnotation = getAnnotation(type, PROVIDER);
		processEvent(providerAnnotation, ADDED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		final JaxrsElementDelta delta = metamodelMonitor.getElementChanges().get(0);
		assertThat(delta.getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.PROVIDER));
		assertThat(delta.getDeltaKind(), equalTo(ADDED));
		assertThat(((JaxrsProvider) delta.getElement()).getProvidedType(EnumElementKind.EXCEPTION_MAPPER), nullValue());
		// 6 built-in HTTP Methods + 1 Provider
		assertThat(metamodel.findAllElements().size(), equalTo(7));
	}

	@Test
	public void shouldAddProviderWhenHierarchyExistsAndProviderAnnotationIsMissing() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		removeFirstOccurrenceOfCode(type, "@Provider", false);
		metamodelMonitor.resetElementChangesNotifications();
		// operation: adding a type
		processEvent(type, ADDED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		final JaxrsElementDelta delta = metamodelMonitor.getElementChanges().get(0);
		assertThat(delta.getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.PROVIDER));
		assertThat(delta.getDeltaKind(), equalTo(ADDED));
		assertThat(((JaxrsProvider) delta.getElement()).getProvidedType(EnumElementKind.EXCEPTION_MAPPER),
				equalTo(metamodelMonitor.resolveType("javax.persistence.EntityNotFoundException")));
		// 6 built-in HTTP Methods + 1 Provider
		assertThat(metamodel.findAllElements().size(), equalTo(7));
	}

	@Test
	public void shouldAddExceptionMapperProviderWhenAddingHierarchyAndProviderAnnotationIsMissing() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		removeFirstOccurrenceOfCode(type, "@Provider", false);
		metamodelMonitor.resetElementChangesNotifications();
		// operation: change in existing type
		processEvent(type, CHANGED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		final JaxrsElementDelta delta = metamodelMonitor.getElementChanges().get(0);
		assertThat(delta.getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.PROVIDER));
		assertThat(delta.getDeltaKind(), equalTo(ADDED));
		assertThat(((JaxrsProvider) delta.getElement()).getProvidedType(EnumElementKind.EXCEPTION_MAPPER),
				equalTo(metamodelMonitor.resolveType("javax.persistence.EntityNotFoundException")));
		// 6 built-in HTTP Methods + 1 Provider
		assertThat(metamodel.findAllElements().size(), equalTo(7));
	}

	@Test
	public void shouldDoNothingWhenAddingUnrelatedAnnotationOnProvider() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final JaxrsProvider provider = metamodelMonitor.createProvider(type);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final Annotation suppressWarningAnnotation = getAnnotation(provider.getJavaElement(),
				SuppressWarnings.class.getName());
		processEvent(suppressWarningAnnotation, ADDED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(0));
		// 6 built-in HTTP Methods + 1 Provider
		assertThat(metamodel.findAllElements().size(), equalTo(7));
	}

	@Test
	public void shouldDoNothingWhenAnnotationValueRemainsSameOnProvider() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final JaxrsProvider provider = metamodelMonitor.createProvider(type);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(provider.getAnnotation(PROVIDER), CHANGED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(0));
		// 6 built-in HTTP Methods + 1 Provider
		assertThat(metamodel.findAllElements().size(), equalTo(7));
	}

	@Test
	public void shouldDoNothingWhenChangingUnrelatedProviderAnnotationValue() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final JaxrsProvider provider = metamodelMonitor.createProvider(type);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final Annotation suppressWarningsAnnotation = getAnnotation(provider.getJavaElement(),
				SuppressWarnings.class.getName());
		processEvent(suppressWarningsAnnotation, CHANGED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(0));
		// 6 built-in HTTP Methods + 1 Provider
		assertThat(metamodel.findAllElements().size(), equalTo(7));
	}

	@Test
	public void shouldRemoveProviderWhenRemovingCompilationUnit() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final JaxrsProvider provider = metamodelMonitor.createProvider(type);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(provider.getJavaElement().getCompilationUnit(), REMOVED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.PROVIDER));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((JaxrsProvider) metamodelMonitor.getElementChanges().get(0).getElement()), equalTo(provider));
		// 6 built-in HTTP Methods
		assertThat(metamodel.findAllElements().size(), equalTo(6));
	}

	@Test
	public void shouldRemoveProviderWhenRemovingSourceType() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final JaxrsProvider provider = metamodelMonitor.createProvider(type);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(provider.getJavaElement(), REMOVED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.PROVIDER));
		// 6 built-in HTTP Methods
		assertThat(metamodel.findAllElements().size(), equalTo(6));
	}

	@Test
	public void shouldNotRemoveProviderWhenRemovingAnnotation() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final JaxrsProvider provider = metamodelMonitor.createProvider(type);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(provider.getAnnotation(PROVIDER), REMOVED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.PROVIDER));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement(), is(notNullValue()));
		// 6 built-in HTTP Methods + 1 Provider
		assertThat(metamodel.findAllElements().size(), equalTo(7));
	}

	@Test
	public void shouldRemoveProviderWhenRemovingAnnotationAndHierarchyAlreadyMissing() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		removeFirstOccurrenceOfCode(type, "implements ExceptionMapper<EntityNotFoundException>", false);
		final JaxrsProvider provider = metamodelMonitor.createProvider(type);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(provider.getAnnotation(PROVIDER), REMOVED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.PROVIDER));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement(), is(notNullValue()));
		// 6 built-in HTTP Methods
		assertThat(metamodel.findAllElements().size(), equalTo(6));
	}

	@Test
	public void shouldRemoveProviderWhenRemovingHierarchyAndAnnotationAlreadyMissing() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		replaceFirstOccurrenceOfCode(type, "@Provider", "", false);
		metamodelMonitor.createProvider(type);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		replaceFirstOccurrenceOfCode(type, "implements ExceptionMapper<EntityNotFoundException>", "", false);
		processEvent(type, CHANGED, IJavaElementDeltaFlag.F_SUPER_TYPES);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.PROVIDER));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		// 6 built-in HTTP Methods
		assertThat(metamodel.findAllElements().size(), equalTo(6));
	}

	@Test
	public void shouldDoNothingWhenRemovingUnrelatedAnnotationOnProvider() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final JaxrsProvider provider = metamodelMonitor.createProvider(type);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(provider.getJavaElement(), SuppressWarnings.class.getName()), REMOVED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(0));
		// 6 built-in HTTP Methods + 1 Provider
		assertThat(metamodel.findAllElements().size(), equalTo(7));
	}

	@Test
	public void shouldRemoveProviderWhenRemovingSourceFolder() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		metamodelMonitor.createProvider(type);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final IPackageFragmentRoot sourceFolder = metamodelMonitor.resolvePackageFragmentRoot("src/main/java");
		processEvent(sourceFolder, REMOVED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.PROVIDER));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement(), is(notNullValue()));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		// 6 built-in HTTP Methods
		assertThat(metamodel.findAllElements().size(), equalTo(6));
	}

	@Test
	public void shouldUpdateProviderWhenAddingConsumesAnnotation() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final JaxrsProvider provider = metamodelMonitor.createProvider(type);
		provider.removeAnnotation(provider.getAnnotation(CONSUMES).getJavaAnnotation());
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final Annotation consumesAnnotation = getAnnotation(provider.getJavaElement(), CONSUMES);
		processEvent(consumesAnnotation, ADDED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_CONSUMES_ANNOTATION));
		// 6 built-in HTTP Methods + 1 Provider
		assertThat(metamodel.findAllElements().size(), equalTo(7));
	}

	@Test
	public void shouldUpdateProviderWhenChangingConsumesAnnotationValue() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final JaxrsProvider provider = metamodelMonitor.createProvider(type);
		replaceFirstOccurrenceOfCode(type, "@Consumes(\"application/json\")", "@Consumes(\"application/foo\")", false);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(provider.getAnnotation(CONSUMES), CHANGED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_CONSUMES_ANNOTATION));
		// 6 built-in HTTP Methods + 1 Provider
		assertThat(metamodel.findAllElements().size(), equalTo(7));
	}

	@Test
	public void shouldUpdateProviderWhenRemovingConsumesAnnotation() throws CoreException {
		final IType type = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final JaxrsProvider provider = metamodelMonitor.createProvider(type);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(provider.getAnnotation(CONSUMES), REMOVED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_CONSUMES_ANNOTATION));
		// 6 built-in HTTP Methods + 1 Provider
		assertThat(metamodel.findAllElements().size(), equalTo(7));
	}

	@Test
	public void shouldUpdateProviderWhenAddingProducesAnnotation() throws CoreException {
		// pre-condition
		final IType type = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final JaxrsProvider provider = metamodelMonitor.createProvider(type);
		provider.removeAnnotation(provider.getAnnotation(PRODUCES).getJavaAnnotation());
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(provider.getJavaElement(), PRODUCES), ADDED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_PRODUCES_ANNOTATION));
		// 6 built-in HTTP Methods + 1 Provider
		assertThat(metamodel.findAllElements().size(), equalTo(7));
	}

	@Test
	public void shouldUpdateProviderWhenChangingProducesAnnotationValue() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final JaxrsProvider provider = metamodelMonitor.createProvider(type);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		replaceFirstOccurrenceOfCode(type, "@Produces(\"application/json\")", "@Produces(\"application/foo\")", false);
		processEvent(provider.getAnnotation(PRODUCES), CHANGED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_PRODUCES_ANNOTATION));
		// 6 built-in HTTP Methods + 1 Provider
		assertThat(metamodel.findAllElements().size(), equalTo(7));
	}

	@Test
	public void shouldUpdateProviderWhenAProducesAnnotation() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final JaxrsProvider provider = metamodelMonitor.createProvider(type);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(provider.getAnnotation(PRODUCES), REMOVED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_PRODUCES_ANNOTATION));
		// 6 built-in HTTP Methods + 1 Provider
		assertThat(metamodel.findAllElements().size(), equalTo(7));
	}

	@Test
	public void shouldUpdateProviderWhenRemovingProducesAnnotation() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final JaxrsProvider provider = metamodelMonitor.createProvider(type);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processEvent(provider.getAnnotation(PRODUCES), REMOVED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_PRODUCES_ANNOTATION));
		// 6 built-in HTTP Methods + 1 Provider
		assertThat(metamodel.findAllElements().size(), equalTo(7));
	}

	@Test
	public void shouldUpdateProviderWhenProvidedTypeChanged() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final JaxrsProvider provider = metamodelMonitor.createProvider(type);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		replaceAllOccurrencesOfCode(type.getCompilationUnit(), "<EntityNotFoundException>", "<NoResultException>",
				false);
		replaceAllOccurrencesOfCode(type.getCompilationUnit(), "import javax.persistence.EntityNotFoundException;",
				"import javax.persistence.NoResultException;", false);
		processEvent(provider.getJavaElement(), CHANGED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_PROVIDER_HIERARCHY));
		assertThat(provider.getProvidedType(EnumElementKind.EXCEPTION_MAPPER), notNullValue());
		assertThat(provider.getProvidedType(EnumElementKind.EXCEPTION_MAPPER).getFullyQualifiedName(),
				equalTo("javax.persistence.NoResultException"));
		// 6 built-in HTTP Methods + 1 Provider
		assertThat(metamodel.findAllElements().size(), equalTo(7));
	}

	@Test
	public void shouldUpdateProviderWhenProvidedTypeChangedWithInterfacesInheritance() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.extra.DummyProvider");
		final JaxrsProvider provider = metamodelMonitor.createProvider(type);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		replaceAllOccurrencesOfCode(type.getCompilationUnit(), "<String, Number>", "<Number, String>", false);
		processEvent(provider.getJavaElement(), CHANGED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_PROVIDER_HIERARCHY));
		assertThat(provider.getProvidedType(EnumElementKind.MESSAGE_BODY_READER).getFullyQualifiedName(),
				equalTo("java.lang.Number"));
		assertThat(provider.getProvidedType(EnumElementKind.MESSAGE_BODY_WRITER).getFullyQualifiedName(),
				equalTo("java.lang.String"));
		// 6 built-in HTTP Methods + 1 Provider
		assertThat(metamodel.findAllElements().size(), equalTo(7));
	}

	@Test
	public void shouldNotRemoveMessageBodyReaderWhenProviderAnnotationExistButProvidedTypeDoesNotExist()
			throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.extra.DummyProvider");
		final JaxrsProvider provider = metamodelMonitor.createProvider(type);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		replaceAllOccurrencesOfCode(provider.getJavaElement(), "AbstractEntityProvider<String, Number>",
				"AbstractEntityProvider<Foo, Number>", false);
		processEvent(provider.getJavaElement(), CHANGED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_PROVIDER_HIERARCHY));
		assertThat(provider.getProvidedType(EnumElementKind.MESSAGE_BODY_READER), nullValue());
		assertThat(provider.getProvidedType(EnumElementKind.MESSAGE_BODY_WRITER).getFullyQualifiedName(),
				equalTo("java.lang.Number"));
		// 6 built-in HTTP Methods + 1 Provider
		assertThat(metamodel.findAllElements().size(), equalTo(7));
	}

	@Test
	public void shouldNotRemoveProviderWhenProviderAnnotationExistButProvidedTypesDoNotExist() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.extra.DummyProvider");
		final JaxrsProvider provider = metamodelMonitor.createProvider(type);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		replaceAllOccurrencesOfCode(provider.getJavaElement(), "AbstractEntityProvider<String, Number>",
				"AbstractEntityProvider<Foo, Bar>", false);
		processEvent(provider.getJavaElement(), CHANGED);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(provider.getProvidedType(EnumElementKind.MESSAGE_BODY_READER), nullValue());
		assertThat(provider.getProvidedType(EnumElementKind.MESSAGE_BODY_WRITER), nullValue());
		// 6 built-in HTTP Methods + 1 Provider
		assertThat(metamodel.findAllElements().size(), equalTo(7));
	}
	
}
