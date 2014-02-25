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
import static org.jboss.tools.ws.jaxrs.core.WorkbenchUtils.getAnnotation;
import static org.jboss.tools.ws.jaxrs.core.WorkbenchUtils.getAnnotations;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.IJavaElementDeltaFlag.F_SIGNATURE;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.APPLICATION_PATH;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.CONSUMES;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.CONTEXT;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.GET;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.HTTP_METHOD;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.MATRIX_PARAM;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.PATH;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.PATH_PARAM;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.POST;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.PRODUCES;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.PROVIDER;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.QUERY_PARAM;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_CONSUMES_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_ELEMENT_KIND;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_HTTP_METHOD_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_METHOD_PARAMETERS;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_METHOD_RETURN_TYPE;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_PATH_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_PRODUCES_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_PROVIDER_HIERARCHY;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_TARGET_ANNOTATION;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.hamcrest.Matchers;
import org.jboss.tools.ws.jaxrs.core.AbstractCommonTestCase;
import org.jboss.tools.ws.jaxrs.core.WorkbenchUtils;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsJavaApplication;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsProvider;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceField;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname;
import org.jboss.tools.ws.jaxrs.core.jdt.JavaMethodParameter;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementCategory;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta;
import org.junit.Ignore;
import org.junit.Test;

public class JavaElementChangedProcessingTestCase extends AbstractCommonTestCase {

	private final static int ANY_EVENT_TYPE = 0;

	private final static int NO_FLAG = 0;

	public static final IProgressMonitor progressMonitor = new NullProgressMonitor();

	private void processEvent(Annotation annotation, int deltaKind) throws CoreException {
		final JavaElementDelta delta = new JavaElementDelta(annotation.getJavaAnnotation(), deltaKind, ANY_EVENT_TYPE,
				JdtUtils.parse(((IMember) annotation.getJavaParent()), progressMonitor), NO_FLAG);
		metamodel.processJavaElementChange(delta, progressMonitor);
	}

	private void processEvent(IMember element, int deltaKind) throws CoreException {
		final JavaElementDelta delta = new JavaElementDelta(element, deltaKind, ANY_EVENT_TYPE, JdtUtils.parse(element,
				progressMonitor), NO_FLAG);
		metamodel.processJavaElementChange(delta, progressMonitor);
	}

	private void processEvent(IMember element, int deltaKind, int flags) throws CoreException {
		final JavaElementDelta delta = new JavaElementDelta(element, deltaKind, ANY_EVENT_TYPE, JdtUtils.parse(element,
				progressMonitor), flags);
		metamodel.processJavaElementChange(delta, progressMonitor);
	}

	private void processEvent(ICompilationUnit element, int deltaKind) throws CoreException {
		final JavaElementDelta delta = new JavaElementDelta(element, deltaKind, ANY_EVENT_TYPE, JdtUtils.parse(element,
				progressMonitor), NO_FLAG);
		metamodel.processJavaElementChange(delta, progressMonitor);
	}

	private void processEvent(IPackageFragmentRoot element, int deltaKind) throws CoreException {
		final JavaElementDelta delta = new JavaElementDelta(element, deltaKind, ANY_EVENT_TYPE, null, NO_FLAG);
		metamodel.processJavaElementChange(delta, progressMonitor);
	}

	@Test
	public void shouldAddElementsWhenAddingSourceFolder() throws CoreException {
		// pre-conditions
		final IPackageFragmentRoot sourceFolder = getPackageFragmentRoot("src/main/java");
		// 6 Built-in HTTP Methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
		resetElementChangesNotifications();
		// operation
		processEvent(sourceFolder, ADDED);
		// verifications
		// 37 elements: 1 Application + 1 custom HTTP
		// Method + 2 providers + 5 RootResources + 2 Subresources + all
		// their methods and fields..
		assertThat(elementChanges.size(), equalTo(37));
		assertThat(elementChanges,
				everyItem(Matchers.<JaxrsElementDelta> hasProperty("deltaKind", equalTo(ADDED))));
		// 6 built-in HTTP Methods + all added items
		assertThat(metamodel.findElements(javaProject).size(), equalTo(43));
	}

	@Test
	public void shouldAdd6HttpMethodsAnd0ResourceWhenAddingBinaryLib() throws CoreException {
		// pre-conditions
		final IPackageFragmentRoot lib = getPackageFragmentRoot("lib/jaxrs-api-2.0.1.GA.jar");
		resetElementChangesNotifications();
		// operation
		processEvent(lib, ADDED);
		// verifications. Damned : none in the jar...
		assertThat(elementChanges.size(), equalTo(0));
		// 6 built-in HTTP Methods only
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldAddApplicationWhenAddingSourceCompilationUnit() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		resetElementChangesNotifications();
		// operation
		processEvent(type.getCompilationUnit(), ADDED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.APPLICATION));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((JaxrsJavaApplication) elementChanges.get(0).getElement()).getApplicationPath(),
				equalTo("/app"));
		// 6 Built-in HTTP Methods + 1 Application
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldAddApplicationWhenAddingSourceType() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		// operation
		processEvent(type, ADDED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.APPLICATION));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((JaxrsJavaApplication) elementChanges.get(0).getElement()).getApplicationPath(),
				equalTo("/app"));
		// 6 Built-in HTTP Methods + 1 Application
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldAddApplicationWhenAddingApplicationPathAnnotation() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final Annotation pathAnnotation = getAnnotation(type, APPLICATION_PATH.qualifiedName);
		resetElementChangesNotifications();
		// operation
		processEvent(pathAnnotation, ADDED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.APPLICATION));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((JaxrsJavaApplication) elementChanges.get(0).getElement()).getApplicationPath(),
				equalTo("/app"));
		// 6 Built-in HTTP Methods + 1 Application
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldDoNothingWhenAddingUnrelatedAnnotationOnApplication() throws CoreException {
		// pre-conditions
		final JaxrsJavaApplication application = createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final Annotation suppressWarningAnnotation = getAnnotation(application.getJavaElement(),
				SuppressWarnings.class.getName());
		resetElementChangesNotifications();
		// operation
		processEvent(suppressWarningAnnotation, ADDED);
		// verifications
		assertThat(elementChanges.size(), equalTo(0));
		// 6 Built-in HTTP Methods + 1 Application
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldUpdateApplicationWhenChangingAnnotationValue() throws CoreException {
		// pre-conditions
		final JaxrsJavaApplication application = createJavaApplication(
				"org.jboss.tools.ws.jaxrs.sample.services.RestApplication", "/bar");
		resetElementChangesNotifications();
		// operation
		processEvent(application.getAnnotation(APPLICATION_PATH.qualifiedName), CHANGED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.APPLICATION));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(((JaxrsJavaApplication) elementChanges.get(0).getElement()).getApplicationPath(),
				equalTo("/app"));
		// 6 Built-in HTTP Methods + 1 Application
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldDoNothingWhenAnnotationValueRemainsSameOnApplication() throws CoreException {
		// pre-conditions
		final JaxrsJavaApplication application = createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		resetElementChangesNotifications();
		// operation
		processEvent(application.getAnnotation(APPLICATION_PATH.qualifiedName), CHANGED);
		// verifications
		assertThat(elementChanges.size(), equalTo(0));
		// 6 Built-in HTTP Methods + 1 Application
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldDoNothingWhenChangingUnrelatedApplicationAnnotationValue() throws CoreException {
		// pre-conditions
		final JaxrsJavaApplication application = createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final Annotation suppressWarningsAnnotation = getAnnotation(application.getJavaElement(),
				SuppressWarnings.class.getName());
		resetElementChangesNotifications();
		// operation
		processEvent(suppressWarningsAnnotation, CHANGED);
		// verifications
		assertThat(elementChanges.size(), equalTo(0));
		// 6 Built-in HTTP Methods + 1 Application
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldRemoveApplicationWhenRemovingCompilationUnit() throws CoreException {
		// pre-conditions
		final JaxrsJavaApplication application = createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		resetElementChangesNotifications();
		// operation
		processEvent(application.getJavaElement().getCompilationUnit(), REMOVED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.APPLICATION));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((JaxrsJavaApplication) elementChanges.get(0).getElement()), equalTo(application));
		// 6 Built-in HTTP Methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldRemoveApplicationWhenRemovingSourceType() throws CoreException {
		// pre-conditions
		final JaxrsJavaApplication application = createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		resetElementChangesNotifications();
		// operation
		processEvent(application.getJavaElement(), REMOVED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.APPLICATION));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((JaxrsJavaApplication) elementChanges.get(0).getElement()).getApplicationPath(),
				equalTo("/app"));
		// 6 Built-in HTTP Methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldNotRemoveApplicationWhenRemovingAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsJavaApplication application = createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		resetElementChangesNotifications();
		// operation
		processEvent(application.getAnnotation(APPLICATION_PATH.qualifiedName), REMOVED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.APPLICATION));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getElement(), is(notNullValue()));
		// 6 Built-in HTTP Methods + 1 Application
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
		assertThat(application.getApplicationPath(), nullValue());
	}

	@Test
	public void shouldRemoveApplicationWhenRemovingAnnotationAndHierarchyAlreadyMissing() throws CoreException {
		// pre-conditions
		/*
		 * final IType type =
		 * resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication"
		 * ); final Annotation appPathAnnotation = resolveAnnotation(type,
		 * APPLICATION_PATH.qualifiedName); final JaxrsJavaApplication
		 * application = new JaxrsJavaApplication(type, appPathAnnotation,
		 * false, metamodel); metamodel.add(application);
		 */
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		WorkbenchUtils.replaceFirstOccurrenceOfCode(type, "extends Application", "", false);
		final JaxrsJavaApplication application = createJavaApplication(
				"org.jboss.tools.ws.jaxrs.sample.services.RestApplication", false);
		resetElementChangesNotifications();
		// operation
		processEvent(application.getAnnotation(APPLICATION_PATH.qualifiedName), REMOVED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(elementChanges.get(0).getElement(), equalTo((IJaxrsElement) application));
		// 6 Built-in HTTP Methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));

	}

	@Test
	public void shouldRemoveApplicationWhenRemovingHierarchyAndAnnotationAlreadyMissing() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		WorkbenchUtils.replaceFirstOccurrenceOfCode(type, "@ApplicationPath(\"/app\")", "", false);
		createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		WorkbenchUtils.replaceFirstOccurrenceOfCode(type, "extends Application", "", false);
		resetElementChangesNotifications();
		// operation
		processEvent(type, CHANGED, IJavaElementDeltaFlag.F_SUPER_TYPES);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.APPLICATION));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(elementChanges.get(0).getElement(), is(notNullValue()));
		// 6 Built-in HTTP Methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldDoNothingWhenRemovingUnrelatedAnnotationOnApplication() throws CoreException {
		// pre-conditions
		final JaxrsJavaApplication application = createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(application.getJavaElement(), SuppressWarnings.class.getName()), REMOVED);
		// verifications
		assertThat(elementChanges.size(), equalTo(0));
		// 6 Built-in HTTP Methods + 1 Application
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldRemoveApplicationWhenRemovingSourceFolder() throws CoreException {
		// pre-conditions
		createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final IPackageFragmentRoot sourceFolder = getPackageFragmentRoot("src/main/java");
		resetElementChangesNotifications();
		// operation
		processEvent(sourceFolder, REMOVED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.APPLICATION));
		assertThat(elementChanges.get(0).getElement(), is(notNullValue()));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(REMOVED));
		// 6 Built-in HTTP Methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldAddHttpMethodWhenAddingSourceCompilationUnit() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		resetElementChangesNotifications();
		// operation
		processEvent(type.getCompilationUnit(), ADDED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.HTTP_METHOD));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((JaxrsHttpMethod) elementChanges.get(0).getElement()).getHttpVerb(), equalTo("FOO"));
		// 6 Built-in HTTP Methods + 1 custom one
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldAddHttpMethodWhenAddingSourceType() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		resetElementChangesNotifications();
		// operation
		processEvent(type, ADDED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.HTTP_METHOD));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((JaxrsHttpMethod) elementChanges.get(0).getElement()).getHttpVerb(), equalTo("FOO"));
		// 6 Built-in HTTP Methods + 1 custom one
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldAddHttpMethodWhenAddingHttpMethodAnnotation() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final Annotation httpMethodAnnotation = getAnnotation(type, HTTP_METHOD.qualifiedName);
		resetElementChangesNotifications();
		// operation
		processEvent(httpMethodAnnotation, ADDED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.HTTP_METHOD));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((JaxrsHttpMethod) elementChanges.get(0).getElement()).getHttpVerb(), equalTo("FOO"));
		// 6 Built-in HTTP Methods + 1 custom one
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldDoNothingWhenAddingUnrelatedAnnotationOnHttpMethod() throws CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = createHttpMethod("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(httpMethod.getJavaElement(), Target.class.getName()), ADDED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getFlags(), equalTo(F_TARGET_ANNOTATION));
		// one call, during pre-conditions
		// 6 Built-in HTTP Methods + 1 custom one
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldUpdateHttpMethodWhenChangingAnnotationValue() throws CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = createHttpMethod("org.jboss.tools.ws.jaxrs.sample.services.FOO", "BAR");
		resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(httpMethod.getJavaElement(), HTTP_METHOD.qualifiedName), CHANGED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.HTTP_METHOD));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(((JaxrsHttpMethod) elementChanges.get(0).getElement()).getHttpVerb(), equalTo("FOO"));
		// 6 Built-in HTTP Methods + 1 custom one
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldDoNothingWhenAnnotationValueRemainsSameOnHttpMethod() throws CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = createHttpMethod("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(httpMethod.getJavaElement(), HTTP_METHOD.qualifiedName), CHANGED);
		// verifications
		assertThat(elementChanges.size(), equalTo(0));
		// 6 Built-in HTTP Methods + 1 custom one
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldDoNothingWhenChangingUnrelatedHttpMethodAnnotationValue() throws CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = createHttpMethod("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		resetElementChangesNotifications();
		// operation
		final Annotation targetAnnotation = getAnnotation(httpMethod.getJavaElement(), Target.class.getName());
		processEvent(targetAnnotation, CHANGED);
		// verifications
		assertThat(elementChanges.size(), equalTo(0));
		// 6 Built-in HTTP Methods + 1 custom one
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldRemoveHttpMethodWhenRemovingCompilationUnit() throws CoreException {
		final JaxrsHttpMethod httpMethod = createHttpMethod("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		resetElementChangesNotifications();
		// operation
		processEvent(httpMethod.getJavaElement().getCompilationUnit(), REMOVED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.HTTP_METHOD));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((JaxrsHttpMethod) elementChanges.get(0).getElement()), equalTo(httpMethod));
		// 6 Built-in HTTP Methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldRemoveHttpMethodWhenRemovingSourceType() throws CoreException {
		final JaxrsHttpMethod httpMethod = createHttpMethod("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		resetElementChangesNotifications();
		// operation
		processEvent(httpMethod.getJavaElement(), REMOVED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.HTTP_METHOD));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((JaxrsHttpMethod) elementChanges.get(0).getElement()).getHttpVerb(), equalTo("FOO"));
		// 6 Built-in HTTP Methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldRemoveHttpMethodWhenRemovingAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = createHttpMethod("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(httpMethod.getJavaElement(), HTTP_METHOD.qualifiedName), REMOVED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(elementChanges.get(0).getElement(), equalTo((IJaxrsElement) httpMethod));
		// 6 Built-in HTTP Methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldDoNothingWhenRemovingUnrelatedAnnotationOnHttpMethod() throws CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = createHttpMethod("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(httpMethod.getJavaElement(), SuppressWarnings.class.getName()), REMOVED);
		// verifications
		assertThat(elementChanges.size(), equalTo(0));
		// 6 Built-in HTTP Methods + 1 custom one
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldRemoveHttpMethodWhenRemovingSourceFolder() throws CoreException {
		// pre-conditions
		createHttpMethod("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final IPackageFragmentRoot sourceFolder = getPackageFragmentRoot("src/main/java");
		resetElementChangesNotifications();
		// operation
		processEvent(sourceFolder, REMOVED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.HTTP_METHOD));
		assertThat(elementChanges.get(0).getElement(), is(notNullValue()));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(REMOVED));
		// 6 Built-in HTTP Methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldNotRemoveHttpMethodWhenRemovingBinaryLib() throws CoreException {
		// pre-conditions
		final IPackageFragmentRoot lib = getPackageFragmentRoot("lib/jaxrs-api-2.0.1.GA.jar");
		resetElementChangesNotifications();
		// operation
		processEvent(lib, REMOVED);
		// verifications
		assertThat(elementChanges.size(), equalTo(0));
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldAddResourceWhenAddingSourceCompilationUnit() throws CoreException {
		// pre-conditions
		resetElementChangesNotifications();
		// operation
		IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		processEvent(type.getCompilationUnit(), ADDED);
		// verifications
		assertThat(elementChanges.size(), equalTo(7));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((JaxrsResource) elementChanges.get(6).getElement()).getPathTemplate(), equalTo("/customers"));
		// includes 6 built-in HTTP Methods + 1 Resource and its
		// ResourceMethods/Fields
		assertThat(metamodel.findElements(javaProject).size(), equalTo(13));
	}

	@Test
	public void shouldAddSubresourceWhenAddingSourceCompilationUnit() throws CoreException {
		// pre-conditions
		resetElementChangesNotifications();
		// operation
		IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		processEvent(type.getCompilationUnit(), ADDED);
		// verifications
		assertThat(elementChanges.size(), equalTo(4));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((JaxrsResource) elementChanges.get(3).getElement()).getAllMethods().size(), equalTo(3));
		assertThat(((JaxrsResource) elementChanges.get(3).getElement()).getPathTemplate(), nullValue());
		// includes 6 Built-in HTTP Methods + Resource, ResourceMethods and
		// ResourceFields
		assertThat(metamodel.findElements(javaProject).size(), equalTo(10));
	}

	@Test
	public void shouldAddSubresourceLocatorWhenAddingSourceCompilationUnit() throws CoreException {
		// pre-conditions
		resetElementChangesNotifications();
		// operation
		IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		processEvent(type.getCompilationUnit(), ADDED);
		// verifications
		assertThat(elementChanges.size(), equalTo(5));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((JaxrsResource) elementChanges.get(4).getElement()).getAllMethods().size(), equalTo(1));
		assertThat(((JaxrsResource) elementChanges.get(4).getElement()).getPathTemplate(), equalTo("/products"));
		// includes 6 Built-in HTTP Methods + Resource, ResourceMethods and
		// ResourceFields
		assertThat(metamodel.findElements(javaProject).size(), equalTo(11));
	}

	@Test
	public void shouldAddResourceWhenAddingSourceType() throws CoreException {
		// pre-conditions
		resetElementChangesNotifications();
		// operation
		IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		processEvent(type, ADDED);
		// verifications
		assertThat(elementChanges.size(), equalTo(7));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((JaxrsResource) elementChanges.get(6).getElement()).getPathTemplate(), equalTo("/customers"));
		// includes 6 Built-in HTTP Methods + Resource, ResourceMethods and
		// ResourceFields
		assertThat(metamodel.findElements(javaProject).size(), equalTo(13));
	}

	@Test
	public void shouldAddResourceWhenAddingPathAnnotation() throws CoreException {
		// pre-conditions
		resetElementChangesNotifications();
		// operation
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final Annotation pathAnnotation = getAnnotation(type, PATH.qualifiedName);
		processEvent(pathAnnotation, ADDED);
		// verifications
		assertThat(elementChanges.size(), equalTo(7));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(ADDED));
		final JaxrsResource resource = (JaxrsResource) elementChanges.get(6).getElement();
		assertThat(resource.getPathTemplate(), equalTo("/customers"));
		assertThat(resource.getConsumedMediaTypes(), equalTo(Arrays.asList("application/xml")));
		assertThat(resource.getProducedMediaTypes(), equalTo(Arrays.asList("application/xml", "application/json")));
		// includes 6 Built-in HTTP Methods + Resource, ResourceMethods and
		// ResourceFields
		assertThat(metamodel.findElements(javaProject).size(), equalTo(13));
	}

	@Test
	public void shouldBecomeRootResourceWhenAddingPathAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		resource.removeAnnotation(getAnnotation(resource.getJavaElement(), PATH.qualifiedName).getJavaAnnotation());
		assertThat(resource.isSubresource(), equalTo(true));
		resetElementChangesNotifications();
		// operation
		final Annotation pathAnnotation = getAnnotation(resource.getJavaElement(), PATH.qualifiedName);
		processEvent(pathAnnotation, ADDED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getFlags(), equalTo(F_ELEMENT_KIND + F_PATH_ANNOTATION));
	}

	@Test
	public void shouldNotAddResourceWhenAddingUnrelatedAnnotation() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.FooResource");
		resetElementChangesNotifications();
		// operation
		final Annotation consumesAnnotation = getAnnotation(type, CONSUMES.qualifiedName);
		processEvent(consumesAnnotation, ADDED);
		// verifications
		assertThat(elementChanges.size(), equalTo(0));
		// includes 6 Built-in HTTP Methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldUpdateResourceWhenChangingPathAnnotationValue() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final Annotation pathAnnotation = createAnnotation(
				getAnnotation(resource.getJavaElement(), PATH.qualifiedName), "/bar");
		resource.updateAnnotation(pathAnnotation);
		resetElementChangesNotifications();
		// operation
		processEvent(pathAnnotation, CHANGED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getFlags(), equalTo(F_PATH_ANNOTATION));
		assertThat((JaxrsResource) elementChanges.get(0).getElement(), equalTo(resource));
		// 6 built-in HTTP Methods + 1 Resource + 6 Resource Methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(13));
	}

	@Test
	public void shouldUpdateResourceWhenAddingConsumesAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		resource.removeAnnotation(getAnnotation(resource.getJavaElement(), EnumJaxrsClassname.CONSUMES.qualifiedName).getJavaAnnotation());
		resetElementChangesNotifications();
		// operation
		final Annotation consumesAnnotation = getAnnotation(resource.getJavaElement(), CONSUMES.qualifiedName);
		processEvent(consumesAnnotation, ADDED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getFlags(), equalTo(F_CONSUMES_ANNOTATION));
		// 6 built-in HTTP Methods + 1 Resource + 6 Resource Methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(13));
	}

	@Test
	public void shouldUpdateResourceWhenChangingConsumesAnnotationValue() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final Annotation consumesAnnotation = createAnnotation(
				getAnnotation(resource.getJavaElement(), CONSUMES.qualifiedName), "application/foo");
		resource.updateAnnotation(consumesAnnotation);
		resetElementChangesNotifications();
		// operation
		processEvent(consumesAnnotation, CHANGED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getFlags(), equalTo(F_CONSUMES_ANNOTATION));
		// 6 built-in HTTP Methods + 1 Resource + 6 Resource Methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(13));
	}

	@Test
	public void shouldUpdateResourceWhenRemovingConsumesAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(resource.getJavaElement(), CONSUMES.qualifiedName), REMOVED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getFlags(), equalTo(F_CONSUMES_ANNOTATION));
		// 6 built-in HTTP Methods + 1 Resource + 6 Resource Methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(13));
	}

	@Test
	public void shouldUpdateResourceWhenAddingProducesAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		resource.removeAnnotation(getAnnotation(resource.getJavaElement(), EnumJaxrsClassname.PRODUCES.qualifiedName).getJavaAnnotation());
		resetElementChangesNotifications();
		// operation
		final Annotation producesAnnotation = getAnnotation(resource.getJavaElement(), PRODUCES.qualifiedName);
		processEvent(producesAnnotation, ADDED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getFlags(), equalTo(F_PRODUCES_ANNOTATION));
		// 6 built-in HTTP Methods + 1 Resource + 6 Resource Methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(13));
	}

	@Test
	public void shouldUpdateResourceWhenChangingProducesAnnotationValue() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final Annotation producesAnnotation = createAnnotation(
				getAnnotation(resource.getJavaElement(), PRODUCES.qualifiedName), "application/foo");
		resource.updateAnnotation(producesAnnotation);
		resetElementChangesNotifications();
		// operation
		processEvent(producesAnnotation, CHANGED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getFlags(), equalTo(F_PRODUCES_ANNOTATION));
		// 6 built-in HTTP Methods + 1 Resource + 6 Resource Methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(13));
	}

	@Test
	public void shouldUpdateResourceWhenRemovingProducesAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(resource.getJavaElement(), PRODUCES.qualifiedName), REMOVED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getFlags(), equalTo(F_PRODUCES_ANNOTATION));
		// 6 built-in HTTP Methods + 1 Resource + 6 Resource Methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(13));
	}

	@Test
	public void shouldAddResourceFieldWhenAddingPathParamAnnotationOnField() throws CoreException {
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		resource.getField("_pType").remove();
		resetElementChangesNotifications();
		// operation
		final Annotation fieldAnnotation = getAnnotation(resource.getJavaElement().getField("_pType"),
				PATH_PARAM.qualifiedName);
		processEvent(fieldAnnotation, ADDED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1)); // field only
		assertThat(elementChanges,
				everyItem(Matchers.<JaxrsElementDelta> hasProperty("deltaKind", equalTo(ADDED))));
		// 6 built-in HTTP Methods + 1 resource + 1 method + 3 fields
		assertThat(metamodel.findElements(javaProject).size(), equalTo(11));
	}

	@Test
	@Ignore
	public void shouldAddResourceFieldWhenAddingImportPathParam() throws CoreException {
		/*
		 * // pre-conditions final IType type =
		 * getType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator"
		 * ); final JaxrsResource resource = new JaxrsResource(type,
		 * annotations, metamodel).build(); //metamodel.add(resource); final
		 * IImportDeclaration importDeclaration = getImportDeclaration(type,
		 * PathParam.getName()); // operation final JavaElementChangedEvent
		 * event = createEvent(importDeclaration, ADDED); final
		 * List<JaxrsElementChangedEvent> jaxrsElementDelta =
		 * processEvent(event, progressMonitor); // verifications
		 * assertThat(jaxrsElementDelta.size(), equalTo(1));
		 * assertThat(jaxrsElementDelta.get(0).getDeltaKind(), equalTo(ADDED));
		 */
	}

	@Test
	public void shouldAddResourceFieldWhenAddingQueryParamAnnotationOnField() throws CoreException {
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		resource.getField("_foo").remove();
		resetElementChangesNotifications();
		// operation
		final Annotation fieldAnnotation = getAnnotation(resource.getJavaElement().getField("_foo"),
				QUERY_PARAM.qualifiedName);
		processEvent(fieldAnnotation, ADDED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1)); // field only
		assertThat(elementChanges,
				everyItem(Matchers.<JaxrsElementDelta> hasProperty("deltaKind", equalTo(ADDED))));
		// 6 built-in HTTP Methods + 1 resource + 1 method + 3 fields
		assertThat(metamodel.findElements(javaProject).size(), equalTo(11));
	}

	@Test
	public void shouldAddResourceFieldWhenAddingMatrixParamAnnotationOnField() throws CoreException {
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		resource.getField("_bar").remove();
		resetElementChangesNotifications();
		// operation
		final Annotation fieldAnnotation = getAnnotation(resource.getJavaElement().getField("_bar"),
				MATRIX_PARAM.qualifiedName);
		processEvent(fieldAnnotation, ADDED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1)); // field only
		assertThat(elementChanges,
				everyItem(Matchers.<JaxrsElementDelta> hasProperty("deltaKind", equalTo(ADDED))));
		// 6 built-in HTTP Methods + 1 resource + 1 method + 3 fields
		assertThat(metamodel.findElements(javaProject).size(), equalTo(11));
	}

	@Test
	public void shouldAddResourceFieldWhenAddingFieldAnnotatedWithPathParam() throws CoreException {
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		resource.getField("_pType").remove();
		resetElementChangesNotifications();
		// operation
		final IField field = resource.getJavaElement().getField("_pType");
		processEvent(field, ADDED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1)); // field only
		assertThat(elementChanges,
				everyItem(Matchers.<JaxrsElementDelta> hasProperty("deltaKind", equalTo(ADDED))));
		// 6 built-in HTTP Methods + 1 resource + 1 method + 3 fields
		assertThat(metamodel.findElements(javaProject).size(), equalTo(11));
	}

	@Test
	public void shouldAddResourceFieldWhenAddingFieldAnnotatedWithQueryParam() throws CoreException {
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		resource.getField("_foo").remove();
		resetElementChangesNotifications();
		// operation
		final IField field = resource.getJavaElement().getField("_foo");
		processEvent(field, ADDED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1)); // field only
		assertThat(elementChanges,
				everyItem(Matchers.<JaxrsElementDelta> hasProperty("deltaKind", equalTo(ADDED))));
		// 6 built-in HTTP Methods + 1 resource + 1 method + 3 fields
		assertThat(metamodel.findElements(javaProject).size(), equalTo(11));
	}

	@Test
	public void shouldAddResourceFieldWhenAddingFieldAnnotatedWithMatrixParam() throws CoreException {
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		resource.getField("_bar").remove();
		resetElementChangesNotifications();
		// operation
		final IField field = resource.getJavaElement().getField("_bar");
		processEvent(field, ADDED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1)); // field only
		assertThat(elementChanges,
				everyItem(Matchers.<JaxrsElementDelta> hasProperty("deltaKind", equalTo(ADDED))));
		// 6 built-in HTTP Methods + 1 resource + 1 method + 3 fields
		assertThat(metamodel.findElements(javaProject).size(), equalTo(11));
	}

	@Test
	public void shouldDoNothingWhenAddingFieldWithAnyAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IField field = resource.getJavaElement().getField("entityManager");
		resetElementChangesNotifications();
		// operation
		processEvent(field, ADDED);
		// verifications
		assertThat(elementChanges.size(), equalTo(0));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(13));
	}

	@Test
	public void shouldDoNothingWhenAddingUnrelatedAnnotationOnField() throws CoreException {
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IField field = resource.getJavaElement().getField("entityManager");
		resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(field, "javax.persistence.PersistenceContext"), ADDED);
		// verifications
		assertThat(elementChanges.size(), equalTo(0));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(13));
	}

	@Test
	public void shouldUpdateResourceFieldWhenChangingPathParamAnnotationValueOnField() throws CoreException {
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final Annotation fieldAnnotation = resource.getField("_pType").getAnnotation(PATH_PARAM.qualifiedName);
		fieldAnnotation.update(createAnnotation(PATH_PARAM.qualifiedName, "foobar"));
		resetElementChangesNotifications();
		// operation
		processEvent(fieldAnnotation, CHANGED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		// 6 built-in HTTP Methods + 1 resource + 1 method + 3 fields
		assertThat(metamodel.findElements(javaProject).size(), equalTo(11));
	}

	@Test
	public void shouldUpdateResourceFieldWhenChangingQueryParamAnnotationValueOnField() throws CoreException {
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final Annotation fieldAnnotation = resource.getField("_foo").getAnnotation(QUERY_PARAM.qualifiedName);
		fieldAnnotation.update(createAnnotation(QUERY_PARAM.qualifiedName, "foobar"));
		resetElementChangesNotifications();
		// operation
		processEvent(fieldAnnotation, CHANGED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		// 6 built-in HTTP Methods + 1 resource + 1 method + 3 fields
		assertThat(metamodel.findElements(javaProject).size(), equalTo(11));
	}

	@Test
	public void shouldUpdateResourceFieldWhenChangingMatrixParamAnnotationValueOnField() throws CoreException {
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final Annotation fieldAnnotation = resource.getField("_bar").getAnnotation(MATRIX_PARAM.qualifiedName);
		fieldAnnotation.update(createAnnotation(MATRIX_PARAM.qualifiedName, "foobar"));
		resetElementChangesNotifications();
		// operation
		processEvent(fieldAnnotation, CHANGED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		// 6 built-in HTTP Methods + 1 resource + 1 method + 3 fields
		assertThat(metamodel.findElements(javaProject).size(), equalTo(11));
	}

	@Test
	public void shouldDoNothingWhenChangingUnrelatedResourceFieldAnnotationValue() throws CoreException {
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final Annotation fieldAnnotation = getAnnotation(resource.getField("_bar").getJavaElement(),
				SuppressWarnings.class.getName());
		createAnnotation(fieldAnnotation, "foobar");
		resetElementChangesNotifications();
		// operation
		processEvent(fieldAnnotation, CHANGED);
		// verifications
		assertThat(elementChanges.size(), equalTo(0));
		// 6 built-in HTTP Methods + 1 resource + 1 method + 3 fields
		assertThat(metamodel.findElements(javaProject).size(), equalTo(11));
	}

	@Test
	public void shouldRemoveResourceFieldWhenRemovingPathParamAnnotatedOnField() throws CoreException {
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final Annotation fieldAnnotation = resource.getField("_pType").getAnnotation(PATH_PARAM.qualifiedName);
		resetElementChangesNotifications();
		// operation
		processEvent(fieldAnnotation, REMOVED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(REMOVED));
		// 6 built-in HTTP Methods + 1 resource + 1 method + 2 fields
		assertThat(metamodel.findElements(javaProject).size(), equalTo(10));
	}

	@Test
	public void shouldRemoveResourceFieldWhenRemovingQueryParamAnnotationOnField() throws CoreException {
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final Annotation fieldAnnotation = resource.getField("_foo").getAnnotation(QUERY_PARAM.qualifiedName);
		resetElementChangesNotifications();
		// operation
		processEvent(fieldAnnotation, REMOVED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(REMOVED));
		// 6 built-in HTTP Methods + 1 resource + 1 method + 2 fields
		assertThat(metamodel.findElements(javaProject).size(), equalTo(10));
	}

	@Test
	public void shouldRemoveResourceFieldWhenRemovingMatrixParamAnnotationOnField() throws CoreException {
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final Annotation fieldAnnotation = resource.getField("_bar").getAnnotation(MATRIX_PARAM.qualifiedName);
		resetElementChangesNotifications();
		// operation
		processEvent(fieldAnnotation, REMOVED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(REMOVED));
		// 6 built-in HTTP Methods + 1 resource + 1 method + 2 fields
		assertThat(metamodel.findElements(javaProject).size(), equalTo(10));
	}

	@Test
	public void shouldRemoveResourceFieldWhenRemovingFieldAnnotatedWithPathParam() throws CoreException {
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final JaxrsResourceField field = resource.getField("_pType");
		resetElementChangesNotifications();
		// operation
		processEvent(field.getJavaElement(), REMOVED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(REMOVED));
		// 6 built-in HTTP Methods + 1 resource + 1 method + 2 fields
		assertThat(metamodel.findElements(javaProject).size(), equalTo(10));
	}

	@Test
	public void shouldRemoveResourceFieldWhenRemovingFieldAnnotatedWithQueryParam() throws CoreException {
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final JaxrsResourceField field = resource.getField("_foo");
		resetElementChangesNotifications();
		// operation
		processEvent(field.getJavaElement(), REMOVED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(REMOVED));
		// 6 built-in HTTP Methods + 1 resource + 1 method + 2 fields
		assertThat(metamodel.findElements(javaProject).size(), equalTo(10));
	}

	@Test
	public void shouldRemoveResourceFieldWhenRemovingFieldAnnotatedWithMatrixParam() throws CoreException {
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final JaxrsResourceField field = resource.getField("_bar");
		resetElementChangesNotifications();
		// operation
		processEvent(field.getJavaElement(), REMOVED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(REMOVED));
		// 6 built-in HTTP Methods + 1 resource + 1 method + 2 fields
		assertThat(metamodel.findElements(javaProject).size(), equalTo(10));
	}

	@Test
	public void shouldDoNothingWhenRemovingUnrelatedAnnotationOnField() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IField field = resource.getJavaElement().getField("entityManager");
		resetElementChangesNotifications();
		// operation
		processEvent(field, REMOVED);
		// verifications
		assertThat(elementChanges.size(), equalTo(0));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(13));
	}

	@Test
	public void shouldDoNothingWhenAnnotationValueRemainsSameOnResource() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(resource.getJavaElement(), PATH.qualifiedName), CHANGED);
		// verifications
		assertThat(elementChanges.size(), equalTo(0));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(13));
	}

	@Test
	public void shouldDoNothingWhenChangingUnrelatedResourceAnnotationValue() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final Annotation suppressWarningsAnnotation = getAnnotation(resource.getJavaElement(),
				SuppressWarnings.class.getName());
		createAnnotation(suppressWarningsAnnotation, "foobar");
		resetElementChangesNotifications();
		// operation
		processEvent(suppressWarningsAnnotation, CHANGED);
		// verifications
		assertThat(elementChanges.size(), equalTo(0));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(13));
	}

	@Test
	public void shouldRemoveResourceWhenRemovingCompilationUnit() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		resetElementChangesNotifications();
		// operation
		processEvent(resource.getJavaElement().getCompilationUnit(), REMOVED);
		// verifications
		assertThat(elementChanges.size(), equalTo(7));
		assertThat(elementChanges,
				everyItem(Matchers.<JaxrsElementDelta> hasProperty("deltaKind", equalTo(REMOVED))));
		// 6 built-in HTTP Methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldRemoveResourceWhenRemovingSourceType() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		resetElementChangesNotifications();
		// operation
		processEvent(resource.getJavaElement(), REMOVED);
		// verifications
		assertThat(elementChanges.size(), equalTo(7));
		assertThat(elementChanges,
				everyItem(Matchers.<JaxrsElementDelta> hasProperty("deltaKind", equalTo(REMOVED))));
		// 6 built-in HTTP Methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldRemoveResourceWhenNoMethodAndRemovingPathAnnotations() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final List<JaxrsResourceMethod> resourceMethods = new ArrayList<JaxrsResourceMethod>(resource
				.getMethods().values());
		for (JaxrsResourceMethod resourceMethod : resourceMethods) {
			resource.removeMethod(resourceMethod);
		}
		resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(resource.getJavaElement(), PATH.qualifiedName), REMOVED);
		// verifications : resource removed, since it has no field nor method
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((JaxrsResource) elementChanges.get(0).getElement()), equalTo(resource));
		// 6 built-in HTTP Methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldRemoveResourceMethodWhenRemovingJavaMethod() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod resourceMethod = getResourceMethod(resource, "createCustomer");
		resetElementChangesNotifications();
		// operation
		processEvent(resourceMethod.getJavaElement(), REMOVED);
		// verifications : resource removed, since it has no field nor method
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(REMOVED));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(12));
	}

	@Test
	public void shouldBecomeSubresourceWhenRemovingPathAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		resetElementChangesNotifications();
		// operation
		final Annotation pathAnnotation = getAnnotation(resource.getJavaElement(), PATH.qualifiedName);
		processEvent(pathAnnotation, REMOVED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getFlags(), equalTo(F_ELEMENT_KIND + F_PATH_ANNOTATION));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(13));
	}

	@Test
	public void shouldDoNothingWhenRemovingUnrelatedAnnotationOnResource() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(resource.getJavaElement(), SuppressWarnings.class.getName()), REMOVED);
		// verifications
		assertThat(elementChanges.size(), equalTo(0));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(13));
	}

	@Test
	public void shouldRemoveResourceWhenRemovingSourceFolder() throws CoreException {
		// pre-conditions
		createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		resetElementChangesNotifications();
		// operation
		final IPackageFragmentRoot sourceFolder = getPackageFragmentRoot("src/main/java");
		processEvent(sourceFolder, REMOVED);
		// verifications
		assertThat(elementChanges.size(), equalTo(7));
		// 6 built-in HTTP Methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	@Ignore
	public void shouldRemoveResourceWhenRemovingBinaryLib() throws CoreException {
		// need to package a JAX-RS resource into a jar...
	}

	@Test
	public void shouldAddResourceMethodWhenAddingHttpMethodAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		removeResourceMethod(resource, "createCustomer");
		final IMethod method = getJavaMethod(resource.getJavaElement(), "createCustomer");
		final Annotation postAnnotation = getAnnotation(method, POST.qualifiedName);
		resetElementChangesNotifications();
		// operation
		processEvent(postAnnotation, ADDED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1)); // method only
		assertThat(elementChanges,
				everyItem(Matchers.<JaxrsElementDelta> hasProperty("deltaKind", equalTo(ADDED))));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(13));
	}

	@Test
	public void shouldAddResourceMethodWhenAddingAnnotatedMethod() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		removeResourceMethod(resource, "createCustomer");
		final IMethod method = getJavaMethod(resource.getJavaElement(), "createCustomer");
		resetElementChangesNotifications();
		// operation
		processEvent(method, ADDED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1)); // field only
		assertThat(elementChanges,
				everyItem(Matchers.<JaxrsElementDelta> hasProperty("deltaKind", equalTo(ADDED))));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(13));
	}

	@Test
	public void shouldAddSubresourceLocatorWhenAddingPathAnnotation() throws CoreException {
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		removeResourceMethod(resource, "getProductResourceLocator");
		final IMethod method = getJavaMethod(resource.getJavaElement(), "getProductResourceLocator");
		final Map<String, Annotation> methodAnnotations = getAnnotations(method, PATH.qualifiedName);
		resetElementChangesNotifications();
		// operation
		processEvent(methodAnnotations.get(PATH.qualifiedName), ADDED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1)); // field only
		assertThat(elementChanges,
				everyItem(Matchers.<JaxrsElementDelta> hasProperty("deltaKind", equalTo(ADDED))));
		// 6 built-in HTTP Methods + 1 resource + 1 method + 3 fields
		assertThat(metamodel.findElements(javaProject).size(), equalTo(11));
	}

	@Test
	public void shouldRemoveResourceMethodWhenRemovingHttpMethodAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod resourceMethod = getResourceMethod(resource, "createCustomer");
		resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(resourceMethod.getJavaElement(), POST.qualifiedName), REMOVED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(REMOVED));
		// 6 built-in HTTP Methods + 1 resource + 5 methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(12));
	}

	@Test
	public void shouldConvertIntoSubresourceMethodWhenAddingPathAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod resourceMethod = getResourceMethod(resource, "getCustomer");
		resourceMethod.removeAnnotation(resourceMethod.getAnnotation(PATH.qualifiedName).getJavaAnnotation());
		resetElementChangesNotifications();
		// operation
		final Annotation pathAnnotation = getAnnotation(resourceMethod.getJavaElement(), PATH.qualifiedName);
		processEvent(pathAnnotation, ADDED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getFlags(), equalTo(F_PATH_ANNOTATION + F_ELEMENT_KIND));
		// 6 built-in HTTP Methods + 1 resource + 5 methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(13));
	}

	@Test
	public void shouldConvertIntoResourceMethodWhenRemovingPathAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod resourceMethod = getResourceMethod(resource, "getCustomer");
		resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(resourceMethod.getJavaElement(), PATH.qualifiedName), REMOVED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getFlags(), equalTo(F_PATH_ANNOTATION + F_ELEMENT_KIND));
		// 6 built-in HTTP Methods + 1 resource + 5 methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(13));
	}

	@Test
	public void shouldConvertIntoSubresourceLocatorWhenRemovingHttpMethodAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod resourceMethod = getResourceMethod(resource, "getCustomer");
		resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(resourceMethod.getJavaElement(), GET.qualifiedName), REMOVED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getFlags(), equalTo(F_HTTP_METHOD_ANNOTATION + F_ELEMENT_KIND));
		// 6 built-in HTTP Methods + 1 resource + 5 methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(13));
	}

	@Test
	public void shouldRemoveSubresourceLocatorWhenRemovingPathAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final IMethod getProductResourceLocatorMethod = getJavaMethod(resource.getJavaElement(),
				"getProductResourceLocator");
		resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(getProductResourceLocatorMethod, PATH.qualifiedName), REMOVED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(REMOVED));
		// 6 built-in HTTP Methods + 1 resource + 3 fields
		assertThat(metamodel.findElements(javaProject).size(), equalTo(10));
	}

	@Test
	public void shouldUpdateSubresourceMethodWhenChangingPathAnnotationValue() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod getCustomerMethod = getJavaMethod(resource.getJavaElement(), "getCustomer");
		WorkbenchUtils.replaceFirstOccurrenceOfCode(getCustomerMethod, "@Path(\"{id}\")", "@Path(\"foo\")", true);
		resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(getCustomerMethod, PATH.qualifiedName), CHANGED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getFlags(), equalTo(F_PATH_ANNOTATION));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(13));
	}

	@Test
	public void shouldUpdateResourceMethodWhenAddingConsumesAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod resourceMethod = getResourceMethod(resource, "createCustomer");
		resourceMethod.removeAnnotation(resourceMethod.getAnnotation(CONSUMES.qualifiedName).getJavaAnnotation());
		resetElementChangesNotifications();
		// operation
		final Annotation consumesAnnotation = getAnnotation(resourceMethod.getJavaElement(), CONSUMES.qualifiedName);
		processEvent(consumesAnnotation, ADDED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getFlags(), equalTo(F_CONSUMES_ANNOTATION));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(13));
	}

	@Test
	public void shouldUpdateResourceMethodWhenChangingConsumesAnnotationValue() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod createCustomerMethod = getJavaMethod(resource.getJavaElement(), "createCustomer");
		WorkbenchUtils.replaceFirstOccurrenceOfCode(createCustomerMethod, "@Consumes(MediaType.APPLICATION_XML)",
				"@Consumes(\"application/foo\")", true);
		resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(createCustomerMethod, CONSUMES.qualifiedName), CHANGED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getFlags(), equalTo(F_CONSUMES_ANNOTATION));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(13));
	}

	@Test
	public void shouldUpdateResourceMethodMethodWhenRemovingConsumesAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod resourceMethod = getResourceMethod(resource, "createCustomer");
		resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(resourceMethod.getJavaElement(), CONSUMES.qualifiedName), REMOVED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getFlags(), equalTo(F_CONSUMES_ANNOTATION));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(13));
	}

	@Test
	public void shouldUpdateResourceMethodWhenAddingProducesAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod resourceMethod = getResourceMethod(resource, "getCustomerAsVCard");
		final Annotation producesAnnotation = getAnnotation(resourceMethod.getJavaElement(), PRODUCES.qualifiedName);
		resourceMethod.removeAnnotation(producesAnnotation.getJavaAnnotation());
		resetElementChangesNotifications();
		// operation
		processEvent(producesAnnotation, ADDED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getFlags(), equalTo(F_PRODUCES_ANNOTATION));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(13));
	}

	@Test
	public void shouldUpdateResourceMethodWhenChangingProducesAnnotationValue() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod getCustomerAsVCardMethod = getJavaMethod(resource.getJavaElement(), "getCustomerAsVCard");
		WorkbenchUtils.replaceFirstOccurrenceOfCode(getCustomerAsVCardMethod, "@Produces({ \"text/x-vcard\" })",
				"@Produces(\"text/foo\")", true);
		resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(getCustomerAsVCardMethod, PRODUCES.qualifiedName), CHANGED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getFlags(), equalTo(F_PRODUCES_ANNOTATION));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(13));
	}

	@Test
	public void shouldUpdateResourceMethodWhenRemovingProducesAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod resourceMethod = getResourceMethod(resource, "getCustomerAsVCard");
		resetElementChangesNotifications();
		// operation
		final Annotation producesAnnotation = getAnnotation(resourceMethod.getJavaElement(), PRODUCES.qualifiedName);
		processEvent(producesAnnotation, REMOVED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getFlags(), equalTo(F_PRODUCES_ANNOTATION));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(13));
	}

	@Test
	public void shouldUpdateResourceMethodWhenAddingParameterWithPathParamAnnotation() throws CoreException {
		// test is not relevant : the old method is replaced by a new one
	}

	@Test
	public void shouldUpdateResourceMethodWhenAddingPathParamAnnotationOnParameter() throws CoreException {
		// the method signature is changed
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod resourceMethod = getResourceMethod(resource, "getCustomer");
		final JavaMethodParameter javaMethodParameter = resourceMethod.getJavaMethodParameter("id");
		final Annotation annotation = javaMethodParameter.getAnnotation(PATH_PARAM.qualifiedName);
		javaMethodParameter.removeAnnotation(annotation);
		resetElementChangesNotifications();
		// operation
		processEvent(resourceMethod.getJavaElement(), CHANGED, F_SIGNATURE);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getFlags(), equalTo(F_METHOD_PARAMETERS));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(13));
	}

	@Test
	public void shouldUpdateResourceMethodWhenChangingPathParamAnnotationOnParameter() throws CoreException {
		// the method signature is changed
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod resourceMethod = getResourceMethod(resource, "getCustomer");
		final JavaMethodParameter javaMethodParameter = resourceMethod.getJavaMethodParameter("id");
		final Annotation annotation = javaMethodParameter.getAnnotation(PATH_PARAM.qualifiedName);
		annotation.update(createAnnotation(annotation, "foo"));
		resetElementChangesNotifications();
		// operation
		processEvent(resourceMethod.getJavaElement(), CHANGED, F_SIGNATURE);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getFlags(), equalTo(F_METHOD_PARAMETERS));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(13));
	}

	@Test
	public void shouldUpdateResourceMethodWhenRemovingPathParamAnnotationOnParameter() throws CoreException {
		// precondition: the method signature is changed
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		// JAX-RS Resource Method with an extra @PathParam annotation on the
		// 'update' parameter.
		final JaxrsResourceMethod resourceMethod = getResourceMethod(resource, "updateCustomer");
		resourceMethod.getJavaMethodParameter("update")
				.addAnnotation(createAnnotation(PATH_PARAM.qualifiedName, "foo"));
		resetElementChangesNotifications();
		// operation
		processEvent(resourceMethod.getJavaElement(), CHANGED, F_SIGNATURE);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getFlags(), equalTo(F_METHOD_PARAMETERS));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(13));
	}

	@Test
	public void shouldUpdateResourceMethodWhenAddingParameterWithQueryParamAnnotation() throws CoreException {
		// the method signature is changed
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		// JAX-RS Resource Method (size param is not declared)
		final JaxrsResourceMethod resourceMethod = getResourceMethod(resource, "getCustomers");
		resourceMethod.removeJavaMethodParameter("size");
		resetElementChangesNotifications();
		// operation
		processEvent(resourceMethod.getJavaElement(), CHANGED, F_SIGNATURE);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getFlags(), equalTo(F_METHOD_PARAMETERS));
		assertThat(elementChanges.get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.RESOURCE_METHOD));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(13));
	}

	@Test
	public void shouldUpdateResourceMethodWhenAddingParameterWithMatrixParamAnnotation() throws CoreException {
		// the method signature is changed
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		final JaxrsResourceMethod resourceMethod = getResourceMethod(resource, "getPicture");
		resourceMethod.removeJavaMethodParameter("c");
		resetElementChangesNotifications();
		// operation
		processEvent(resourceMethod.getJavaElement(), CHANGED, F_SIGNATURE);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getFlags(), equalTo(F_METHOD_PARAMETERS));
		assertThat(elementChanges.get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.RESOURCE_METHOD));
		// 6 built-in HTTP Methods + 1 resource + 3 methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(10));
	}

	@Test
	public void shouldUpdateResourceMethodWhenAddingQueryParamAnnotationOnParameter() throws CoreException {
		// the method signature is changed
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod resourceMethod = getResourceMethod(resource, "getCustomers");
		resourceMethod.getJavaMethodParameter("size").removeAnnotation(QUERY_PARAM.qualifiedName);
		resetElementChangesNotifications();
		// operation
		processEvent(resourceMethod.getJavaElement(), CHANGED, F_SIGNATURE);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getFlags(), equalTo(F_METHOD_PARAMETERS));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(13));
	}

	@Test
	public void shouldUpdateResourceMethodWhenChangingQueryParamAnnotationOnParameter() throws CoreException {
		// the method signature is changed
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		// JAX-RS Resource Method (QueryParam value is different: "length" vs
		// "size" on second param)
		final JaxrsResourceMethod resourceMethod = getResourceMethod(resource, "getCustomers");
		resourceMethod.getJavaMethodParameter("size").getAnnotation(QUERY_PARAM.qualifiedName)
				.update(createAnnotation(QUERY_PARAM.qualifiedName, "length"));
		resetElementChangesNotifications();
		// operation
		processEvent(resourceMethod.getJavaElement(), CHANGED, F_SIGNATURE);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getFlags(), equalTo(F_METHOD_PARAMETERS));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(13));
	}

	@Test
	public void shouldUpdateResourceMethodWhenRemovingQueryParamAnnotationOnParameter() throws CoreException {
		// the method signature is changed
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		// JAX-RS Resource Method (with an extra @Queryparam annotation on
		// 'uriInfo' param)
		final JaxrsResourceMethod resourceMethod = getResourceMethod(resource, "getCustomers");
		resourceMethod.getJavaMethodParameter("uriInfo").addAnnotation(
				createAnnotation(QUERY_PARAM.qualifiedName, "foo"));
		resetElementChangesNotifications();
		// operation
		processEvent(resourceMethod.getJavaElement(), CHANGED, F_SIGNATURE);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getFlags(), equalTo(F_METHOD_PARAMETERS));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(13));
	}

	@Test
	public void shouldUpdateResourceMethodWhenAddingMatrixParamAnnotationOnParameter() throws CoreException {
		// the method signature is changed
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		final JaxrsResourceMethod resourceMethod = getResourceMethod(resource, "getPicture");
		resourceMethod.getJavaMethodParameter("c").removeAnnotation(MATRIX_PARAM.qualifiedName);
		resetElementChangesNotifications();
		// operation
		processEvent(resourceMethod.getJavaElement(), CHANGED, F_SIGNATURE);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getFlags(), equalTo(F_METHOD_PARAMETERS));
		// 6 built-in HTTP Methods + 1 resource + 3 methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(10));
	}

	@Test
	public void shouldUpdateResourceMethodWhenChangingMatrixParamAnnotationOnParameter() throws CoreException {
		// the method signature is changed
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		// JAX-RS Resource Method (MATRIX_PARAM.qualifiedName value is
		// different: "foo" vs "color" on second param)
		final JaxrsResourceMethod resourceMethod = getResourceMethod(resource, "getPicture");
		resourceMethod.getJavaMethodParameter("c").getAnnotation(MATRIX_PARAM.qualifiedName)
				.update(createAnnotation(MATRIX_PARAM.qualifiedName, "foo"));
		resetElementChangesNotifications();
		// operation
		processEvent(resourceMethod.getJavaElement(), CHANGED, F_SIGNATURE);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getFlags(), equalTo(F_METHOD_PARAMETERS));
		// 6 built-in HTTP Methods + 1 resource + 3 methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(10));
	}

	@Test
	public void shouldUpdateResourceMethodWhenRemovingMatrixParamAnnotationOnParameter() throws CoreException {
		// the method signature is changed
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		// JAX-RS Resource Method (an extra MATRIX_PARAM.qualifiedName
		// annotation on 'id' param)
		final JaxrsResourceMethod resourceMethod = getResourceMethod(resource, "getProduct");
		resourceMethod.getJavaMethodParameter("id").addAnnotation(createAnnotation(MATRIX_PARAM.qualifiedName, "foo"));
		resetElementChangesNotifications();
		// operation
		processEvent(resourceMethod.getJavaElement(), CHANGED, F_SIGNATURE);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getFlags(), equalTo(F_METHOD_PARAMETERS));
		// 6 built-in HTTP Methods + 1 resource + 3 methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(10));
	}

	@Test
	public void shouldUpdateResourceMethodWhenChangingReturnType() throws CoreException {
		// pre-conditions
		// the method signature is changed
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		// JAX-RS Resource Method (declared return type is
		// 'javax.ws.rs.Response')
		final JaxrsResourceMethod resourceMethod = getResourceMethod(resource, "getCustomer");
		resourceMethod.setReturnedType(resolveType("java.lang.Object"));
		resetElementChangesNotifications();
		// operation
		processEvent(resourceMethod.getJavaElement(), CHANGED, F_SIGNATURE);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		// annotations were not provided but are retrieved...
		assertThat(elementChanges.get(0).getFlags(), equalTo(F_METHOD_RETURN_TYPE));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(13));
	}

	@Test
	public void shouldDoNothingMethodWhenAddingParamWithUnrelatedAnnotation() throws CoreException {
		// test is not relevant : the old method is replaced by a new one
	}

	@Test
	@Ignore("For now, accept all annotations")
	public void shouldDoNothingMethodWhenAddingUnrelatedAnnotationOnParameter() throws CoreException {
		// pre-conditions
		// the method signature is changed: @Context is not declared on 'uriInfo' param
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod resourceMethod = getResourceMethod(resource, "getCustomers");
		resourceMethod.getJavaMethodParameter("uriInfo").removeAnnotation(CONTEXT.qualifiedName);
		resetElementChangesNotifications();
		// operation
		processEvent(resourceMethod.getJavaElement(), CHANGED, F_SIGNATURE);
		// verifications
		assertThat(elementChanges.size(), equalTo(0));
		assertThat(metamodel.findElements(javaProject).size(), equalTo(1));
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
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod method = getJavaMethod(resource.getJavaElement(), "getEntityManager");
		resetElementChangesNotifications();
		// operation
		processEvent(method, ADDED);
		// verifications
		assertThat(elementChanges.size(), equalTo(0));
	}

	@Test
	public void shouldDoNothingWhenChangingBasicJavaMethod() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod method = getJavaMethod(resource.getJavaElement(), "getEntityManager");
		resetElementChangesNotifications();
		// operation
		processEvent(method, CHANGED);
		// verifications
		assertThat(elementChanges.size(), equalTo(0));
	}

	@Test
	public void shouldDoNothingWhenRemovingBasicJavaMethod() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod method = getJavaMethod(resource.getJavaElement(), "getEntityManager");
		resetElementChangesNotifications();
		// operation
		processEvent(method, REMOVED);
		// verifications
		assertThat(elementChanges.size(), equalTo(0));
	}

	@Test
	public void shouldDoFailWhenDuplicatingJavaMethod() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod method = getJavaMethod(resource.getJavaElement(), "getCustomer");
		resetElementChangesNotifications();
		// before operation: metamodel has 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(13));
		// operation
		final IMethod addedMethod = WorkbenchUtils.createMethod(method.getDeclaringType(), method.getSource(), true);
		processEvent(addedMethod, ADDED);
		// verifications
		assertThat(elementChanges.size(), equalTo(0));
		// after operation: metamodel has still 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(13));
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
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		resetElementChangesNotifications();
		// operation
		processEvent(type.getCompilationUnit(), ADDED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.PROVIDER));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(ADDED));
		// 6 built-in HTTP Methods + 1 Provider
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldAddProviderWhenAddingSourceType() throws CoreException {
		// pre-conditions
		final IType providerType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		resetElementChangesNotifications();
		// operation
		processEvent(providerType, ADDED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		final JaxrsElementDelta delta = elementChanges.get(0);
		assertThat(delta.getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.PROVIDER));
		assertThat(delta.getDeltaKind(), equalTo(ADDED));
		assertThat(((JaxrsProvider) delta.getElement()).getProvidedType(EnumElementKind.EXCEPTION_MAPPER),
				equalTo(resolveType("javax.persistence.EntityNotFoundException")));
		// 6 built-in HTTP Methods + 1 Provider
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldAddProviderWhenAddingProviderAnnotationAndHierarchyIsMissing() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		WorkbenchUtils.removeFirstOccurrenceOfCode(type, "implements ExceptionMapper<EntityNotFoundException>", false);
		resetElementChangesNotifications();
		// operation
		final Annotation providerAnnotation = getAnnotation(type, PROVIDER.qualifiedName);
		processEvent(providerAnnotation, ADDED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		final JaxrsElementDelta delta = elementChanges.get(0);
		assertThat(delta.getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.PROVIDER));
		assertThat(delta.getDeltaKind(), equalTo(ADDED));
		assertThat(((JaxrsProvider) delta.getElement()).getProvidedType(EnumElementKind.EXCEPTION_MAPPER), nullValue());
		// 6 built-in HTTP Methods + 1 Provider
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldAddProviderWhenHierarchyExistsAndProviderAnnotationIsMissing() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		WorkbenchUtils.removeFirstOccurrenceOfCode(type, "@Provider", false);
		resetElementChangesNotifications();
		// operation: adding a type
		processEvent(type, ADDED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		final JaxrsElementDelta delta = elementChanges.get(0);
		assertThat(delta.getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.PROVIDER));
		assertThat(delta.getDeltaKind(), equalTo(ADDED));
		assertThat(((JaxrsProvider) delta.getElement()).getProvidedType(EnumElementKind.EXCEPTION_MAPPER),
				equalTo(resolveType("javax.persistence.EntityNotFoundException")));
		// 6 built-in HTTP Methods + 1 Provider
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldAddProviderWhenAddingHierarchyAndProviderAnnotationIsMissing() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		WorkbenchUtils.removeFirstOccurrenceOfCode(type, "@Provider", false);
		resetElementChangesNotifications();
		// operation: change in existing type
		processEvent(type, CHANGED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		final JaxrsElementDelta delta = elementChanges.get(0);
		assertThat(delta.getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.PROVIDER));
		assertThat(delta.getDeltaKind(), equalTo(ADDED));
		assertThat(((JaxrsProvider) delta.getElement()).getProvidedType(EnumElementKind.EXCEPTION_MAPPER),
				equalTo(resolveType("javax.persistence.EntityNotFoundException")));
		// 6 built-in HTTP Methods + 1 Provider
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldDoNothingWhenAddingUnrelatedAnnotationOnProvider() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final JaxrsProvider provider = createProvider(type);
		resetElementChangesNotifications();
		// operation
		final Annotation suppressWarningAnnotation = getAnnotation(provider.getJavaElement(),
				SuppressWarnings.class.getName());
		processEvent(suppressWarningAnnotation, ADDED);
		// verifications
		assertThat(elementChanges.size(), equalTo(0));
		// 6 built-in HTTP Methods + 1 Provider
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldDoNothingWhenAnnotationValueRemainsSameOnProvider() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final JaxrsProvider provider = createProvider(type);
		resetElementChangesNotifications();
		// operation
		processEvent(provider.getAnnotation(PROVIDER.qualifiedName), CHANGED);
		// verifications
		assertThat(elementChanges.size(), equalTo(0));
		// 6 built-in HTTP Methods + 1 Provider
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldDoNothingWhenChangingUnrelatedProviderAnnotationValue() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final JaxrsProvider provider = createProvider(type);
		resetElementChangesNotifications();
		// operation
		final Annotation suppressWarningsAnnotation = getAnnotation(provider.getJavaElement(),
				SuppressWarnings.class.getName());
		processEvent(suppressWarningsAnnotation, CHANGED);
		// verifications
		assertThat(elementChanges.size(), equalTo(0));
		// 6 built-in HTTP Methods + 1 Provider
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldRemoveProviderWhenRemovingCompilationUnit() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final JaxrsProvider provider = createProvider(type);
		resetElementChangesNotifications();
		// operation
		processEvent(provider.getJavaElement().getCompilationUnit(), REMOVED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.PROVIDER));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((JaxrsProvider) elementChanges.get(0).getElement()), equalTo(provider));
		// 6 built-in HTTP Methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldRemoveProviderWhenRemovingSourceType() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final JaxrsProvider provider = createProvider(type);
		resetElementChangesNotifications();
		// operation
		processEvent(provider.getJavaElement(), REMOVED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.PROVIDER));
		// 6 built-in HTTP Methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldNotRemoveProviderWhenRemovingAnnotation() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final JaxrsProvider provider = createProvider(type);
		resetElementChangesNotifications();
		// operation
		processEvent(provider.getAnnotation(PROVIDER.qualifiedName), REMOVED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.PROVIDER));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getElement(), is(notNullValue()));
		// 6 built-in HTTP Methods + 1 Provider
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldRemoveProviderWhenRemovingAnnotationAndHierarchyAlreadyMissing() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		WorkbenchUtils.removeFirstOccurrenceOfCode(type, "implements ExceptionMapper<EntityNotFoundException>", false);
		final JaxrsProvider provider = createProvider(type);
		resetElementChangesNotifications();
		// operation
		processEvent(provider.getAnnotation(PROVIDER.qualifiedName), REMOVED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.PROVIDER));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(elementChanges.get(0).getElement(), is(notNullValue()));
		// 6 built-in HTTP Methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldRemoveProviderWhenRemovingHierarchyAndAnnotationAlreadyMissing() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		WorkbenchUtils.replaceFirstOccurrenceOfCode(type, "@Provider", "", false);
		createProvider(type);
		resetElementChangesNotifications();
		// operation
		WorkbenchUtils.replaceFirstOccurrenceOfCode(type, "implements ExceptionMapper<EntityNotFoundException>", "",
				false);
		processEvent(type, CHANGED, IJavaElementDeltaFlag.F_SUPER_TYPES);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.PROVIDER));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(REMOVED));
		// 6 built-in HTTP Methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldDoNothingWhenRemovingUnrelatedAnnotationOnProvider() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final JaxrsProvider provider = createProvider(type);
		resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(provider.getJavaElement(), SuppressWarnings.class.getName()), REMOVED);
		// verifications
		assertThat(elementChanges.size(), equalTo(0));
		// 6 built-in HTTP Methods + 1 Provider
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldRemoveProviderWhenRemovingSourceFolder() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		createProvider(type);
		resetElementChangesNotifications();
		// operation
		final IPackageFragmentRoot sourceFolder = getPackageFragmentRoot("src/main/java");
		processEvent(sourceFolder, REMOVED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.PROVIDER));
		assertThat(elementChanges.get(0).getElement(), is(notNullValue()));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(REMOVED));
		// 6 built-in HTTP Methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldUpdateProviderWhenAddingConsumesAnnotation() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final JaxrsProvider provider = createProvider(type);
		provider.removeAnnotation(provider.getAnnotation(CONSUMES.qualifiedName).getJavaAnnotation());
		resetElementChangesNotifications();
		// operation
		final Annotation consumesAnnotation = getAnnotation(provider.getJavaElement(), CONSUMES.qualifiedName);
		processEvent(consumesAnnotation, ADDED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getFlags(), equalTo(F_CONSUMES_ANNOTATION));
		// 6 built-in HTTP Methods + 1 Provider
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldUpdateProviderWhenChangingConsumesAnnotationValue() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final JaxrsProvider provider = createProvider(type);
		WorkbenchUtils.replaceFirstOccurrenceOfCode(type, "@Consumes(\"application/json\")",
				"@Consumes(\"application/foo\")", false);
		resetElementChangesNotifications();
		// operation
		processEvent(provider.getAnnotation(CONSUMES.qualifiedName), CHANGED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getFlags(), equalTo(F_CONSUMES_ANNOTATION));
		// 6 built-in HTTP Methods + 1 Provider
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldUpdateProviderWhenRemovingConsumesAnnotation() throws CoreException {
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final JaxrsProvider provider = createProvider(type);
		resetElementChangesNotifications();
		// operation
		processEvent(provider.getAnnotation(CONSUMES.qualifiedName), REMOVED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getFlags(), equalTo(F_CONSUMES_ANNOTATION));
		// 6 built-in HTTP Methods + 1 Provider
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldUpdateProviderWhenAddingProducesAnnotation() throws CoreException {
		// pre-condition
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final JaxrsProvider provider = createProvider(type);
		provider.removeAnnotation(provider.getAnnotation(PRODUCES.qualifiedName).getJavaAnnotation());
		resetElementChangesNotifications();
		// operation
		processEvent(getAnnotation(provider.getJavaElement(), PRODUCES.qualifiedName), ADDED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getFlags(), equalTo(F_PRODUCES_ANNOTATION));
		// 6 built-in HTTP Methods + 1 Provider
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldUpdateProviderWhenChangingProducesAnnotationValue() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final JaxrsProvider provider = createProvider(type);
		resetElementChangesNotifications();
		// operation
		WorkbenchUtils.replaceFirstOccurrenceOfCode(type, "@Produces(\"application/json\")",
				"@Produces(\"application/foo\")", false);
		processEvent(provider.getAnnotation(PRODUCES.qualifiedName), CHANGED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getFlags(), equalTo(F_PRODUCES_ANNOTATION));
		// 6 built-in HTTP Methods + 1 Provider
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldUpdateProviderWhenAProducesAnnotation() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final JaxrsProvider provider = createProvider(type);
		resetElementChangesNotifications();
		// operation
		processEvent(provider.getAnnotation(PRODUCES.qualifiedName), REMOVED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getFlags(), equalTo(F_PRODUCES_ANNOTATION));
		// 6 built-in HTTP Methods + 1 Provider
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldUpdateProviderWhenRemovingProducesAnnotation() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final JaxrsProvider provider = createProvider(type);

		resetElementChangesNotifications();
		// operation
		processEvent(provider.getAnnotation(PRODUCES.qualifiedName), REMOVED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getFlags(), equalTo(F_PRODUCES_ANNOTATION));
		// 6 built-in HTTP Methods + 1 Provider
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldUpdateProviderWhenProvidedTypeChanged() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final JaxrsProvider provider = createProvider(type);
		resetElementChangesNotifications();
		// operation
		WorkbenchUtils.replaceAllOccurrencesOfCode(type.getCompilationUnit(), "<EntityNotFoundException>",
				"<NoResultException>", false);
		WorkbenchUtils.replaceAllOccurrencesOfCode(type.getCompilationUnit(),
				"import javax.persistence.EntityNotFoundException;", "import javax.persistence.NoResultException;",
				false);
		processEvent(provider.getJavaElement(), CHANGED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getFlags(), equalTo(F_PROVIDER_HIERARCHY));
		assertThat(provider.getProvidedType(EnumElementKind.EXCEPTION_MAPPER), notNullValue());
		assertThat(provider.getProvidedType(EnumElementKind.EXCEPTION_MAPPER).getFullyQualifiedName(),
				equalTo("javax.persistence.NoResultException"));
		// 6 built-in HTTP Methods + 1 Provider
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldUpdateProviderWhenProvidedTypeChangedWithInterfacesInheritance() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.extra.DummyProvider");
		final JaxrsProvider provider = createProvider(type);
		resetElementChangesNotifications();
		// operation
		WorkbenchUtils.replaceAllOccurrencesOfCode(type.getCompilationUnit(), "<String, Number>", "<Number, String>",
				false);
		processEvent(provider.getJavaElement(), CHANGED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getFlags(), equalTo(F_PROVIDER_HIERARCHY));
		assertThat(provider.getProvidedType(EnumElementKind.MESSAGE_BODY_READER).getFullyQualifiedName(),
				equalTo("java.lang.Number"));
		assertThat(provider.getProvidedType(EnumElementKind.MESSAGE_BODY_WRITER).getFullyQualifiedName(),
				equalTo("java.lang.String"));
		// 6 built-in HTTP Methods + 1 Provider
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldNotRemoveMessageBodyReaderWhenProviderAnnotationExistButProvidedTypeDoesNotExist()
			throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.extra.DummyProvider");
		final JaxrsProvider provider = createProvider(type);
		resetElementChangesNotifications();
		// operation
		WorkbenchUtils.replaceAllOccurrencesOfCode(provider.getJavaElement(), "AbstractEntityProvider<String, Number>",
				"AbstractEntityProvider<Foo, Number>", false);
		processEvent(provider.getJavaElement(), CHANGED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(elementChanges.get(0).getFlags(), equalTo(F_PROVIDER_HIERARCHY));
		assertThat(provider.getProvidedType(EnumElementKind.MESSAGE_BODY_READER), nullValue());
		assertThat(provider.getProvidedType(EnumElementKind.MESSAGE_BODY_WRITER).getFullyQualifiedName(),
				equalTo("java.lang.Number"));
		// 6 built-in HTTP Methods + 1 Provider
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldNotRemoveProviderWhenProviderAnnotationExistButProvidedTypesDoNotExist() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.extra.DummyProvider");
		final JaxrsProvider provider = createProvider(type);
		resetElementChangesNotifications();
		// operation
		WorkbenchUtils.replaceAllOccurrencesOfCode(provider.getJavaElement(), "AbstractEntityProvider<String, Number>",
				"AbstractEntityProvider<Foo, Bar>", false);
		processEvent(provider.getJavaElement(), CHANGED);
		// verifications
		assertThat(elementChanges.size(), equalTo(1));
		assertThat(elementChanges.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(provider.getProvidedType(EnumElementKind.MESSAGE_BODY_READER), nullValue());
		assertThat(provider.getProvidedType(EnumElementKind.MESSAGE_BODY_WRITER), nullValue());
		// 6 built-in HTTP Methods + 1 Provider
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}
}
