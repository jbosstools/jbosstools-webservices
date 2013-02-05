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
import static org.jboss.tools.ws.jaxrs.core.WorkbenchUtils.changeAnnotationValue;
import static org.jboss.tools.ws.jaxrs.core.WorkbenchUtils.resolveAnnotation;
import static org.jboss.tools.ws.jaxrs.core.WorkbenchUtils.resolveAnnotations;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.IJavaElementDeltaFlag.F_SIGNATURE;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementDelta.F_CONSUMED_MEDIATYPES_VALUE;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementDelta.F_ELEMENT_KIND;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementDelta.F_HTTP_METHOD_VALUE;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementDelta.F_METHOD_PARAMETERS;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementDelta.F_METHOD_RETURN_TYPE;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementDelta.F_PATH_VALUE;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementDelta.F_PRODUCED_MEDIATYPES_VALUE;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.APPLICATION_PATH;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.CONSUMES;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.CONTEXT;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.DEFAULT_VALUE;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.DELETE;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.ENCODED;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.GET;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.HTTP_METHOD;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.MATRIX_PARAM;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.PATH;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.PATH_PARAM;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.POST;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.PRODUCES;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.PUT;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.QUERY_PARAM;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.RESPONSE;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.URI_INFO;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Collections;
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
import org.eclipse.jdt.core.JavaModelException;
import org.hamcrest.Matchers;
import org.jboss.tools.ws.jaxrs.core.AbstractCommonTestCase;
import org.jboss.tools.ws.jaxrs.core.WorkbenchUtils;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsBaseElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsJavaApplication;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsProvider;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceField;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionUtils;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.JavaMethodParameter;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementCategory;
import org.jboss.tools.ws.jaxrs.core.metamodel.JaxrsMetamodelDelta;
import org.junit.Ignore;
import org.junit.Test;

public class JavaElementChangedProcessorTestCase extends AbstractCommonTestCase {

	private final static int ANY_EVENT_TYPE = 0;

	private final static int NO_FLAG = 0;

	private final JavaElementChangedProcessor processor = new JavaElementChangedProcessor();

	public static final IProgressMonitor progressMonitor = new NullProgressMonitor();

	private List<JaxrsElementDelta> processEvent(JavaElementDelta event, IProgressMonitor progressmonitor) {
		final List<JaxrsMetamodelDelta> affectedMetamodels = processor.processAffectedJavaElements(
				Arrays.asList(event), progressmonitor);
		if (affectedMetamodels.isEmpty()) {
			return Collections.emptyList();
		}
		return affectedMetamodels.get(0).getAffectedElements();
	}

	private static JavaElementDelta createEvent(Annotation annotation, int deltaKind) throws JavaModelException {
		return new JavaElementDelta(annotation.getJavaAnnotation(), deltaKind, ANY_EVENT_TYPE, JdtUtils.parse(
				((IMember) annotation.getJavaParent()), progressMonitor), NO_FLAG);
	}

	private static JavaElementDelta createEvent(IMember element, int deltaKind) throws JavaModelException {
		return createEvent(element, deltaKind, NO_FLAG);
	}

	private static JavaElementDelta createEvent(IMember element, int deltaKind, int flags) throws JavaModelException {
		return new JavaElementDelta(element, deltaKind, ANY_EVENT_TYPE, JdtUtils.parse(element, progressMonitor), flags);
	}

	private static JavaElementDelta createEvent(ICompilationUnit element, int deltaKind) throws JavaModelException {
		return new JavaElementDelta(element, deltaKind, ANY_EVENT_TYPE, JdtUtils.parse(element, progressMonitor),
				NO_FLAG);
	}

	private static JavaElementDelta createEvent(IPackageFragmentRoot element, int deltaKind) throws JavaModelException {
		return new JavaElementDelta(element, deltaKind, ANY_EVENT_TYPE, null, NO_FLAG);
	}

	/**
	 * Because sometimes, generics are painful...
	 * 
	 * @param elements
	 * @return private List<JaxrsElement<?>> asList(JaxrsElement<?>... elements)
	 *         { final List<JaxrsElement<?>> result = new
	 *         ArrayList<JaxrsElement<?>>();
	 *         result.addAll(Arrays.asList(elements)); return result; }
	 */

	@Test
	public void shouldAdd1HttpMethodAnd3ResourcesWhenAddingSourceFolder() throws CoreException {
		// pre-conditions
		createHttpMethod(GET);
		createHttpMethod(POST);
		createHttpMethod(PUT);
		createHttpMethod(DELETE);
		final IPackageFragmentRoot sourceFolder = getPackageFragmentRoot("src/main/java");
		// operation
		final JavaElementDelta event = createEvent(sourceFolder, ADDED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		// 1 Application + 1 HttpMethod + 6 RootResources + 2 Subresources + all
		// their methods and fields..
		assertThat(impacts.size(), equalTo(35));
		// 4 previous HttpMethods + all added items
		assertThat(metamodel.getElements(javaProject).size(), equalTo(39));
		assertThat(impacts, everyItem(Matchers.<JaxrsElementDelta> hasProperty("deltaKind", equalTo(ADDED))));
	}

	@Test
	public void shouldAdd6HttpMethodsAnd0ResourceWhenAddingBinaryLib() throws CoreException {
		// pre-conditions
		final IPackageFragmentRoot lib = getPackageFragmentRoot("lib/jaxrs-api-2.0.1.GA.jar");
		// operation
		final JavaElementDelta event = createEvent(lib, ADDED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications. Damned : none in the jar...
		assertThat(impacts.size(), equalTo(6));
		assertThat(impacts, everyItem(Matchers.<JaxrsElementDelta> hasProperty("deltaKind", equalTo(ADDED))));
		verify(metamodel, times(6)).add(any(JaxrsHttpMethod.class));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldAddApplicationWhenAddingSourceCompilationUnit() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		// operation
		final JavaElementDelta event = createEvent(type.getCompilationUnit(), ADDED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((JaxrsJavaApplication) impacts.get(0).getElement()).getApplicationPath(), equalTo("/app"));
		verify(metamodel, times(1)).add(any(JaxrsJavaApplication.class));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldAddApplicationWhenAddingSourceType() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		// operation
		final JavaElementDelta event = createEvent(type, ADDED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((JaxrsJavaApplication) impacts.get(0).getElement()).getApplicationPath(), equalTo("/app"));
		verify(metamodel, times(1)).add(any(JaxrsJavaApplication.class));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldAddApplicationWhenAddingApplicationPathAnnotation() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final Annotation pathAnnotation = resolveAnnotation(type, APPLICATION_PATH.qualifiedName);
		// operation
		final JavaElementDelta event = createEvent(pathAnnotation, ADDED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((JaxrsJavaApplication) impacts.get(0).getElement()).getApplicationPath(), equalTo("/app"));
		verify(metamodel, times(1)).add(any(JaxrsJavaApplication.class));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldDoNothingWhenAddingUnrelatedAnnotationOnApplication() throws CoreException {
		// pre-conditions
		final JaxrsJavaApplication application = createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final Annotation suppressWarningAnnotation = resolveAnnotation(application.getJavaElement(),
				SuppressWarnings.class.getName());
		// operation
		final JavaElementDelta event = createEvent(suppressWarningAnnotation, ADDED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(0));
		// one call, during pre-conditions
		verify(metamodel, times(1)).add(any(JaxrsJavaApplication.class));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldUpdateApplicationWhenChangingAnnotationValue() throws CoreException {
		// pre-conditions
		final JaxrsJavaApplication application = createJavaApplication(
				"org.jboss.tools.ws.jaxrs.sample.services.RestApplication", "/bar");
		// operation
		final JavaElementDelta event = createEvent(application.getAnnotation(APPLICATION_PATH.qualifiedName), CHANGED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(((JaxrsJavaApplication) impacts.get(0).getElement()).getApplicationPath(), equalTo("/app"));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldDoNothingWhenAnnotationValueRemainsSameOnApplication() throws CoreException {
		// pre-conditions
		final JaxrsJavaApplication application = createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		// operation
		final JavaElementDelta event = createEvent(application.getAnnotation(APPLICATION_PATH.qualifiedName), CHANGED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(0));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldDoNothingWhenChangingUnrelatedApplicationAnnotationValue() throws CoreException {
		// pre-conditions
		final JaxrsJavaApplication application = createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final Annotation suppressWarningsAnnotation = resolveAnnotation(application.getJavaElement(),
				SuppressWarnings.class.getName());
		// operation
		final JavaElementDelta event = createEvent(suppressWarningsAnnotation, CHANGED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(0));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldRemoveApplicationWhenRemovingCompilationUnit() throws CoreException {
		// pre-conditions
		final JaxrsJavaApplication application = createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		// operation
		final JavaElementDelta event = createEvent(application.getJavaElement().getCompilationUnit(), REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((JaxrsJavaApplication) impacts.get(0).getElement()), equalTo(application));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(0));
	}

	@Test
	public void shouldRemoveApplicationWhenRemovingSourceType() throws CoreException {
		// pre-conditions
		final JaxrsJavaApplication application = createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		// operation
		final JavaElementDelta event = createEvent(application.getJavaElement(), REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((JaxrsJavaApplication) impacts.get(0).getElement()).getApplicationPath(), equalTo("/app"));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(0));
	}

	@Test
	public void shouldNotRemoveApplicationWhenRemovingAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsJavaApplication application = createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		// operation
		final JavaElementDelta event = createEvent(application.getAnnotation(APPLICATION_PATH.qualifiedName), REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getElement(), is(notNullValue()));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
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
		final JaxrsJavaApplication application = createJavaApplication(
				"org.jboss.tools.ws.jaxrs.sample.services.RestApplication", false);
		// operation
		final JavaElementDelta event = createEvent(application.getAnnotation(APPLICATION_PATH.qualifiedName), REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(impacts.get(0).getElement(), is(notNullValue()));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(0));
	}

	@Test
	public void shouldRemoveApplicationWhenRemovingHierarchyAndAnnotationAlreadyMissing() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		WorkbenchUtils.replaceFirstOccurrenceOfCode(type, "@ApplicationPath(\"/app\")", "", false);
		WorkbenchUtils.replaceFirstOccurrenceOfCode(type, "extends Application", "", false);
		createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		// operation
		final JavaElementDelta event = createEvent(type, CHANGED, IJavaElementDeltaFlag.F_SUPER_TYPES);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(impacts.get(0).getElement(), is(notNullValue()));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(0));
	}

	@Test
	public void shouldDoNothingWhenRemovingUnrelatedAnnotationOnApplication() throws CoreException {
		// pre-conditions
		final JaxrsJavaApplication application = createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		// operation
		final JavaElementDelta event = createEvent(application.getAnnotation(SuppressWarnings.class.getName()), REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(0));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldRemoveApplicationWhenRemovingSourceFolder() throws CoreException {
		// pre-conditions
		createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final IPackageFragmentRoot sourceFolder = getPackageFragmentRoot("src/main/java");
		// operation
		final JavaElementDelta event = createEvent(sourceFolder, REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(impacts.get(0).getElement(), is(notNullValue()));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(0));
	}

	@Test
	public void shouldAddHttpMethodWhenAddingSourceCompilationUnit() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		// operation
		final JavaElementDelta event = createEvent(type.getCompilationUnit(), ADDED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.HTTP_METHOD));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((JaxrsHttpMethod) impacts.get(0).getElement()).getHttpVerb(), equalTo("FOO"));
		verify(metamodel, times(1)).add(any(JaxrsHttpMethod.class));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldAddHttpMethodWhenAddingSourceType() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		// operation
		final JavaElementDelta event = createEvent(type, ADDED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.HTTP_METHOD));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((JaxrsHttpMethod) impacts.get(0).getElement()).getHttpVerb(), equalTo("FOO"));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldAddHttpMethodWhenAddingHttpMethodAnnotation() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final Annotation httpMethodAnnotation = resolveAnnotation(type, HTTP_METHOD.qualifiedName);
		// operation
		final JavaElementDelta event = createEvent(httpMethodAnnotation, ADDED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.HTTP_METHOD));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((JaxrsHttpMethod) impacts.get(0).getElement()).getHttpVerb(), equalTo("FOO"));
		verify(metamodel, times(1)).add(any(JaxrsHttpMethod.class));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldDoNothingWhenAddingUnrelatedAnnotationOnHttpMethod() throws CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = createHttpMethod("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		// operation
		final JavaElementDelta event = createEvent(httpMethod.getAnnotation(Target.class.getName()), ADDED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(0));
		// one call, during pre-conditions
		verify(metamodel, times(1)).add(any(JaxrsHttpMethod.class));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldUpdateHttpMethodWhenChangingAnnotationValue() throws CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = createHttpMethod("org.jboss.tools.ws.jaxrs.sample.services.FOO", "BAR");
		// operation
		final JavaElementDelta event = createEvent(httpMethod.getAnnotation(HTTP_METHOD.qualifiedName), CHANGED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.HTTP_METHOD));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(((JaxrsHttpMethod) impacts.get(0).getElement()).getHttpVerb(), equalTo("FOO"));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldDoNothingWhenAnnotationValueRemainsSameOnHttpMethod() throws CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = createHttpMethod("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		// operation
		final JavaElementDelta event = createEvent(httpMethod.getAnnotation(HTTP_METHOD.qualifiedName), CHANGED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(0));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldDoNothingWhenChangingUnrelatedHttpMethodAnnotationValue() throws CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = createHttpMethod("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		// operation
		final Annotation targetAnnotation = httpMethod.getAnnotation(Target.class.getName());
		final JavaElementDelta event = createEvent(targetAnnotation, CHANGED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(0));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldRemoveHttpMethodWhenRemovingCompilationUnit() throws CoreException {
		final JaxrsHttpMethod httpMethod = createHttpMethod("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		// operation
		final JavaElementDelta event = createEvent(httpMethod.getJavaElement().getCompilationUnit(), REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.HTTP_METHOD));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((JaxrsHttpMethod) impacts.get(0).getElement()), equalTo(httpMethod));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(0));
	}

	@Test
	public void shouldRemoveHttpMethodWhenRemovingSourceType() throws CoreException {
		final JaxrsHttpMethod httpMethod = createHttpMethod("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		// operation
		final JavaElementDelta event = createEvent(httpMethod.getJavaElement(), REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.HTTP_METHOD));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((JaxrsHttpMethod) impacts.get(0).getElement()).getHttpVerb(), equalTo("FOO"));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(0));
	}

	@Test
	public void shouldRemoveHttpMethodWhenRemovingAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = createHttpMethod("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		// operation
		final JavaElementDelta event = createEvent(httpMethod.getAnnotation(HTTP_METHOD.qualifiedName), REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.HTTP_METHOD));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(impacts.get(0).getElement(), is(notNullValue()));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(0));
	}

	@Test
	public void shouldDoNothingWhenRemovingUnrelatedAnnotationOnHttpMethod() throws CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = createHttpMethod("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		// operation
		final JavaElementDelta event = createEvent(httpMethod.getAnnotation(SuppressWarnings.class.getName()), REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(0));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldRemoveHttpMethodWhenRemovingSourceFolder() throws CoreException {
		// pre-conditions
		createHttpMethod("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final IPackageFragmentRoot sourceFolder = getPackageFragmentRoot("src/main/java");
		// operation
		final JavaElementDelta event = createEvent(sourceFolder, REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.HTTP_METHOD));
		assertThat(impacts.get(0).getElement(), is(notNullValue()));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(0));
	}

	@Test
	public void shouldRemoveHttpMethodWhenRemovingBinaryLib() throws CoreException {
		// pre-conditions
		final IPackageFragmentRoot lib = getPackageFragmentRoot("lib/jaxrs-api-2.0.1.GA.jar");
		// let's suppose that this jar only contains 1 HTTP Methods ;-)
		createHttpMethod("javax.ws.rs.GET");
		// operation
		final JavaElementDelta event = createEvent(lib, REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.HTTP_METHOD));
		assertThat(impacts, everyItem(Matchers.<JaxrsElementDelta> hasProperty("deltaKind", equalTo(REMOVED))));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(0));
	}

	@Test
	public void shouldAddResourceWhenAddingSourceCompilationUnit() throws CoreException {
		// pre-conditions
		createHttpMethod(GET);
		createHttpMethod(POST);
		createHttpMethod(PUT);
		createHttpMethod(DELETE);
		// operation
		IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JavaElementDelta event = createEvent(type.getCompilationUnit(), ADDED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((JaxrsResource) impacts.get(0).getElement()).getPathTemplate(), equalTo("/customers"));
		// includes HttpMethods, Resource and ResourceMethods
		assertThat(metamodel.getElements(javaProject).size(), equalTo(11));
	}

	@Test
	public void shouldAddSubresourceWhenAddingSourceCompilationUnit() throws CoreException {
		// pre-conditions
		createHttpMethod(GET);
		// operation
		IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		final JavaElementDelta event = createEvent(type.getCompilationUnit(), ADDED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((JaxrsResource) impacts.get(0).getElement()).getAllMethods().size(), equalTo(3));
		assertThat(((JaxrsResource) impacts.get(0).getElement()).getPathTemplate(), nullValue());
		// includes Resource, ResourceMethods and ResourceFields
		assertThat(metamodel.getElements(javaProject).size(), equalTo(5));
	}

	@Test
	public void shouldAddSubresourceLocatorWhenAddingSourceCompilationUnit() throws CoreException {
		// pre-conditions
		createHttpMethod(GET);
		// operation
		IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final JavaElementDelta event = createEvent(type.getCompilationUnit(), ADDED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((JaxrsResource) impacts.get(0).getElement()).getAllMethods().size(), equalTo(1));
		assertThat(((JaxrsResource) impacts.get(0).getElement()).getPathTemplate(), equalTo("/products"));
		// includes HttpMethods, Resource, ResourceMethods and ResourceFields
		assertThat(metamodel.getElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldAddResourceWhenAddingSourceType() throws CoreException {
		// pre-conditions
		createHttpMethod(GET);
		createHttpMethod(POST);
		createHttpMethod(PUT);
		createHttpMethod(DELETE);
		// operation
		IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JavaElementDelta event = createEvent(type, ADDED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((JaxrsResource) impacts.get(0).getElement()).getPathTemplate(), equalTo("/customers"));
		// includes HttpMethods, Resource, ResourceMethods and ResourceFields
		assertThat(metamodel.getElements(javaProject).size(), equalTo(11));
	}

	@Test
	public void shouldAddResourceWhenAddingPathAnnotation() throws CoreException {
		// pre-conditions
		createHttpMethod(GET);
		createHttpMethod(POST);
		createHttpMethod(PUT);
		createHttpMethod(DELETE);
		// operation
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final Annotation pathAnnotation = resolveAnnotation(type, PATH.qualifiedName);
		final JavaElementDelta event = createEvent(pathAnnotation, ADDED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(ADDED));
		final JaxrsResource resource = (JaxrsResource) impacts.get(0).getElement();
		assertThat(resource.getPathTemplate(), equalTo("/customers"));
		assertThat(resource.getConsumedMediaTypes(), equalTo(Arrays.asList("application/xml")));
		assertThat(resource.getProducedMediaTypes(), equalTo(Arrays.asList("application/xml", "application/json")));
		// includes HttpMethods, Resource, ResourceMethods and ResourceFields
		assertThat(metamodel.getElements(javaProject).size(), equalTo(11));
	}

	@Test
	public void shouldBecomeRootResourceWhenAddingPathAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PRODUCES.qualifiedName,
				CONSUMES.qualifiedName);
		// operation
		final Annotation pathAnnotation = resolveAnnotation(resource.getJavaElement(), PATH.qualifiedName);
		final JavaElementDelta event = createEvent(pathAnnotation, ADDED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_ELEMENT_KIND + F_PATH_VALUE));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldNotAddResourceWhenAddingUnrelatedAnnotation() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.FooResource");
		// operation
		final Annotation consumesAnnotation = resolveAnnotation(type, CONSUMES.qualifiedName);
		final JavaElementDelta event = createEvent(consumesAnnotation, ADDED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(0));
		verify(metamodel, times(0)).add(any(JaxrsResource.class));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(0));
	}

	@Test
	public void shouldUpdateResourceWhenChangingPathAnnotationValue() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		final Annotation pathAnnotation = changeAnnotationValue(resource.getAnnotation(PATH.qualifiedName), "/bar");
		resource.updateAnnotations(CollectionUtils.toMap(PATH.qualifiedName, pathAnnotation));
		// operation
		final JavaElementDelta event = createEvent(pathAnnotation, CHANGED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_PATH_VALUE));
		assertThat((JaxrsResource) impacts.get(0).getElement(), equalTo(resource));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldUpdateResourceWhenAddingConsumesAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		// operation
		final Annotation consumesAnnotation = resolveAnnotation(resource.getJavaElement(), CONSUMES.qualifiedName);
		final JavaElementDelta event = createEvent(consumesAnnotation, ADDED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_CONSUMED_MEDIATYPES_VALUE));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldUpdateResourceWhenChangingConsumesAnnotationValue() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName, CONSUMES.qualifiedName);
		final Annotation consumesAnnotation = changeAnnotationValue(resource.getAnnotation(CONSUMES.qualifiedName),
				"application/foo");
		resource.updateAnnotations(CollectionUtils.toMap(CONSUMES.qualifiedName, consumesAnnotation));
		// operation
		final JavaElementDelta event = createEvent(consumesAnnotation, CHANGED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_CONSUMED_MEDIATYPES_VALUE));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldUpdateResourceWhenRemovingConsumesAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName, CONSUMES.qualifiedName);
		// operation
		final JavaElementDelta event = createEvent(resource.getAnnotation(CONSUMES.qualifiedName), REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_CONSUMED_MEDIATYPES_VALUE));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldUpdateResourceWhenAddingProducesAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		// operation
		final Annotation producesAnnotation = resolveAnnotation(resource.getJavaElement(), PRODUCES.qualifiedName);
		final JavaElementDelta event = createEvent(producesAnnotation, ADDED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_PRODUCED_MEDIATYPES_VALUE));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldUpdateResourceWhenChangingProducesAnnotationValue() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName, PRODUCES.qualifiedName);
		final Annotation producesAnnotation = changeAnnotationValue(resource.getAnnotation(PRODUCES.qualifiedName),
				"application/foo");
		resource.updateAnnotations(CollectionUtils.toMap(PRODUCES.qualifiedName, producesAnnotation));
		// operation
		final JavaElementDelta event = createEvent(producesAnnotation, CHANGED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_PRODUCED_MEDIATYPES_VALUE));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldUpdateResourceWhenRemovingProducesAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName, PRODUCES.qualifiedName);
		// operation
		final JavaElementDelta event = createEvent(resource.getAnnotation(PRODUCES.qualifiedName), REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_PRODUCED_MEDIATYPES_VALUE));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldAddResourceFieldWhenAddingPathParamAnnotationOnField() throws CoreException {
		final JaxrsResource resource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final Annotation fieldAnnotation = resolveAnnotation(resource.getJavaElement().getField("productType"),
				PATH_PARAM.qualifiedName);
		// operation
		final JavaElementDelta event = createEvent(fieldAnnotation, ADDED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1)); // field only
		assertThat(impacts, everyItem(Matchers.<JaxrsElementDelta> hasProperty("deltaKind", equalTo(ADDED))));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(2));
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
		 * List<JaxrsElementChangedEvent> impacts = processEvent(event,
		 * progressMonitor); // verifications assertThat(impacts.size(),
		 * equalTo(1)); assertThat(impacts.get(0).getDeltaKind(),
		 * equalTo(ADDED));
		 */
	}

	@Test
	public void shouldAddResourceFieldWhenAddingQueryParamAnnotationOnField() throws CoreException {
		final JaxrsResource resource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final Annotation fieldAnnotation = resolveAnnotation(resource.getJavaElement().getField("foo"),
				QUERY_PARAM.qualifiedName);
		// operation
		final JavaElementDelta event = createEvent(fieldAnnotation, ADDED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1)); // field only
		assertThat(impacts, everyItem(Matchers.<JaxrsElementDelta> hasProperty("deltaKind", equalTo(ADDED))));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(2));
	}

	@Test
	public void shouldAddResourceFieldWhenAddingMatrixParamAnnotationOnField() throws CoreException {
		final JaxrsResource resource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final Annotation annotation = resolveAnnotation(resource.getJavaElement().getField("bar"),
				MATRIX_PARAM.qualifiedName);
		// operation
		final JavaElementDelta event = createEvent(annotation, ADDED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1)); // field only
		assertThat(impacts, everyItem(Matchers.<JaxrsElementDelta> hasProperty("deltaKind", equalTo(ADDED))));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(2));
	}

	@Test
	public void shouldAddResourceFieldWhenAddingFieldAnnotatedWithPathParam() throws CoreException {
		final JaxrsResource resource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final IField field = resource.getJavaElement().getField("productType");
		// operation
		final JavaElementDelta event = createEvent(field, ADDED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1)); // field + only
		assertThat(impacts, everyItem(Matchers.<JaxrsElementDelta> hasProperty("deltaKind", equalTo(ADDED))));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(2));
	}

	@Test
	public void shouldAddResourceFieldWhenAddingFieldAnnotatedWithQueryParam() throws CoreException {
		final JaxrsResource resource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final IField field = resource.getJavaElement().getField("foo");
		// operation
		final JavaElementDelta event = createEvent(field, ADDED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1)); // field only
		assertThat(impacts, everyItem(Matchers.<JaxrsElementDelta> hasProperty("deltaKind", equalTo(ADDED))));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(2));
	}

	@Test
	public void shouldAddResourceFieldWhenAddingFieldAnnotatedWithMatrixParam() throws CoreException {
		final JaxrsResource resource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final IField field = resource.getJavaElement().getField("bar");
		// operation
		final JavaElementDelta event = createEvent(field, ADDED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1)); // field only
		assertThat(impacts, everyItem(Matchers.<JaxrsElementDelta> hasProperty("deltaKind", equalTo(ADDED))));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(2));
	}

	@Test
	public void shouldDoNothingWhenAddingFieldWithAnyAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		final IField field = resource.getJavaElement().getField("entityManager");
		// operation
		final JavaElementDelta event = createEvent(field, ADDED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(0));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldDoNothingWhenAddingUnrelatedAnnotationOnField() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		final IField field = resource.getJavaElement().getField("entityManager");
		// operation
		final JavaElementDelta event = createEvent(field, ADDED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(0));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldUpdateResourceFieldWhenChangingPathParamAnnotationValueOnField() throws CoreException {
		final JaxrsResource resource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final JaxrsResourceField field = createField(resource, "productType", PATH_PARAM.qualifiedName, "foo");
		// operation
		final JavaElementDelta event = createEvent(field.getAnnotation(PATH_PARAM.qualifiedName), CHANGED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(2));
	}

	@Test
	public void shouldUpdateResourceFieldWhenChangingQueryParamAnnotationValueOnField() throws CoreException {
		final JaxrsResource resource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final JaxrsResourceField field = createField(resource, "foo", QUERY_PARAM.qualifiedName, "foo!");
		// operation
		final JavaElementDelta event = createEvent(field.getAnnotation(QUERY_PARAM.qualifiedName), CHANGED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(2));
	}

	@Test
	public void shouldUpdateResourceFieldWhenChangingMatrixParamAnnotationValueOnField() throws CoreException {
		final JaxrsResource resource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final JaxrsResourceField field = createField(resource, "bar", MATRIX_PARAM.qualifiedName, "bar!");
		// operation
		final JavaElementDelta event = createEvent(field.getAnnotation(MATRIX_PARAM.qualifiedName), CHANGED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(2));
	}

	@Test
	public void shouldDoNothingWhenChangingUnrelatedResourceFieldAnnotationValue() throws CoreException {
		final JaxrsResource resource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final JaxrsResourceField field = createField(resource, "bar", SuppressWarnings.class.getName(), "foobar");
		// operation
		final JavaElementDelta event = createEvent(field.getAnnotation(SuppressWarnings.class.getName()), CHANGED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(0));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(2));
	}

	@Test
	public void shouldRemoveResourceFieldWhenRemovingPathParamAnnotatedOnField() throws CoreException {
		final JaxrsResource resource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final JaxrsResourceField field = createField(resource, "productType", PATH_PARAM.qualifiedName);
		// operation
		final JavaElementDelta event = createEvent(field.getAnnotation(PATH_PARAM.qualifiedName), REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldRemoveResourceFieldWhenRemovingQueryParamAnnotationOnField() throws CoreException {
		final JaxrsResource resource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final JaxrsResourceField field = createField(resource, "foo", QUERY_PARAM.qualifiedName);
		// operation
		final JavaElementDelta event = createEvent(field.getAnnotation(QUERY_PARAM.qualifiedName), REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldRemoveResourceFieldWhenRemovingMatrixParamAnnotationOnField() throws CoreException {
		final JaxrsResource resource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final JaxrsResourceField field = createField(resource, "bar", MATRIX_PARAM.qualifiedName);
		// operation
		final JavaElementDelta event = createEvent(field.getAnnotation(MATRIX_PARAM.qualifiedName), REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldRemoveResourceFieldWhenRemovingFieldAnnotatedWithQueryParam() throws CoreException {
		final JaxrsResource resource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final JaxrsResourceField field = createField(resource, "foo", QUERY_PARAM.qualifiedName);
		// operation
		final JavaElementDelta event = createEvent(field.getAnnotation(QUERY_PARAM.qualifiedName), REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldRemoveResourceFieldWhenRemovingFieldAnnotatedWithPathParam() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final JaxrsResourceField field = createField(resource, "productType", PATH_PARAM.qualifiedName, "foo");
		// operation
		final JavaElementDelta event = createEvent(field.getJavaElement(), REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldRemoveResourceFieldWhenRemovingFieldAnnotatedWithMatrixParam() throws CoreException {
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator", PATH.qualifiedName);
		final JaxrsResourceField field = createField(resource, "bar", MATRIX_PARAM.qualifiedName);
		// operation
		final JavaElementDelta event = createEvent(field.getJavaElement(), REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldDoNothingWhenRemovingUnrelatedAnnotationOnField() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		final IField field = resource.getJavaElement().getField("entityManager");
		// operation
		final JavaElementDelta event = createEvent(field, REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(0));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldDoNothingWhenAnnotationValueRemainsSameOnResource() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		// operation
		final JavaElementDelta event = createEvent(resource.getAnnotation(PATH.qualifiedName), CHANGED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(0));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldDoNothingWhenChangingUnrelatedResourceAnnotationValue() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName, CONSUMES.qualifiedName);
		final Annotation consumesAnnotation = resource.getAnnotation(CONSUMES.qualifiedName);
		// operation
		final JavaElementDelta event = createEvent(consumesAnnotation, CHANGED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(0));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldRemoveResourceWhenRemovingCompilationUnit() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		// operation
		final JavaElementDelta event = createEvent(resource.getJavaElement().getCompilationUnit(), REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((JaxrsResource) impacts.get(0).getElement()), equalTo(resource));
		verify(metamodel, times(1)).remove(any(JaxrsResource.class));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(0));
	}

	@Test
	public void shouldRemoveResourceWhenRemovingSourceType() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		// operation
		final JavaElementDelta event = createEvent(resource.getJavaElement(), REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((JaxrsResource) impacts.get(0).getElement()), equalTo(resource));
		verify(metamodel, times(1)).remove(any(JaxrsResource.class));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(0));
	}

	@Test
	public void shouldRemoveResourceWhenRemovingAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		for (JaxrsBaseElement resourceMethod : resource.getMethods().values()) {
			metamodel.remove(resourceMethod);
		}
		// operation
		final JavaElementDelta event = createEvent(resource.getAnnotation(PATH.qualifiedName), REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications : resource removed, since it has no field nor method
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((JaxrsResource) impacts.get(0).getElement()), equalTo(resource));
		verify(metamodel, times(1)).remove(any(JaxrsResource.class));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(0));
	}

	@Test
	public void shouldRemoveResourceMethodWhenRemovingJavaMethod() throws CoreException {
		// JAX-RS HttpMethod
		createHttpMethod(POST);
		// Parent JAX-RS Resource
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		final JaxrsResourceMethod resourceMethod = createResourceMethod("createCustomer", resource, POST.qualifiedName);
		// operation
		final JavaElementDelta event = createEvent(resourceMethod.getJavaElement(), REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications : resource removed, since it has no field nor method
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		// @POST + resource
		assertThat(metamodel.getElements(javaProject).size(), equalTo(2));
	}

	@Test
	public void shouldBecomeSubresourceWhenRemovingPathAnnotation() throws CoreException {
		// pre-conditions
		// JAX-RS HttpMethod
		createHttpMethod(POST);
		// JAX-RS Resource
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		createResourceMethod("createCustomer", resource, CONSUMES.qualifiedName, POST.qualifiedName);
		// operation
		final Annotation pathAnnotation = resource.getAnnotation(PATH.qualifiedName);
		final JavaElementDelta event = createEvent(pathAnnotation, REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_ELEMENT_KIND + F_PATH_VALUE));
		// @HTTP + resource + resourceMethod
		assertThat(metamodel.getElements(javaProject).size(), equalTo(3));
	}

	@Test
	public void shouldDoNothingWhenRemovingUnrelatedAnnotationOnResource() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName, ENCODED.qualifiedName);
		// operation
		final JavaElementDelta event = createEvent(resource.getAnnotation(ENCODED.qualifiedName), REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(0));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldRemoveResourceWhenRemovingSourceFolder() throws CoreException {
		// pre-conditions
		final IPackageFragmentRoot sourceFolder = getPackageFragmentRoot("src/main/java");
		createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		// operation
		final JavaElementDelta event = createEvent(sourceFolder, REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		verify(metamodel, times(1)).remove(any(JaxrsResource.class));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(0));
	}

	@Test
	@Ignore
	public void shouldRemoveResourceWhenRemovingBinaryLib() throws CoreException {
		// need to package a JAX-RS resource into a jar...
	}

	@Test
	public void shouldAddResourceMethodWhenAddingHttpMethodAnnotation() throws CoreException {
		// pre-conditions
		// JAX-RS HttpMethod
		createHttpMethod(POST);
		// Parent JAX-RS Resource
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		final IMethod method = getMethod(resource.getJavaElement(), "createCustomer");
		final Map<String, Annotation> methodAnnotations = resolveAnnotations(method, POST.qualifiedName);
		// operation
		final JavaElementDelta event = createEvent(methodAnnotations.get(POST.qualifiedName), ADDED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1)); // field only
		assertThat(impacts, everyItem(Matchers.<JaxrsElementDelta> hasProperty("deltaKind", equalTo(ADDED))));
		// @HTTP + resource + resourceMethod
		assertThat(metamodel.getElements(javaProject).size(), equalTo(3));
	}

	@Test
	public void shouldAddResourceMethodWhenAddingAnnotatedMethod() throws CoreException {
		// pre-conditions
		// JAX-RS HttpMethod
		createHttpMethod(POST);
		// Parent JAX-RS Resource
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		final IMethod method = getMethod(resource.getJavaElement(), "createCustomer");
		// operation
		final JavaElementDelta event = createEvent(method, ADDED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(ADDED));
		// @HTTP + resource + resourceMethod
		assertThat(metamodel.getElements(javaProject).size(), equalTo(3));
	}

	@Test
	public void shouldAddSubresourceLocatorWhenAddingPathAnnotation() throws CoreException {
		final JaxrsResource resource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final IMethod method = getMethod(resource.getJavaElement(), "getProductResourceLocator");
		final Map<String, Annotation> methodAnnotations = resolveAnnotations(method, PATH.qualifiedName);
		// operation
		final JavaElementDelta event = createEvent(methodAnnotations.get(PATH.qualifiedName), ADDED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1)); // field only
		assertThat(impacts, everyItem(Matchers.<JaxrsElementDelta> hasProperty("deltaKind", equalTo(ADDED))));
		// resource + resourceMethod
		assertThat(metamodel.getElements(javaProject).size(), equalTo(2));
	}

	@Test
	public void shouldRemoveResourceMethodWhenRemovingHttpMethodAnnotation() throws CoreException {
		// pre-conditions
		// JAX-RS HttpMethod
		createHttpMethod(POST);
		// Parent JAX-RS Resource
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		final JaxrsResourceMethod resourceMethod = createResourceMethod("createCustomer", resource, POST.qualifiedName);
		// operation
		final JavaElementDelta event = createEvent(resourceMethod.getAnnotation(POST.qualifiedName), REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		verify(metamodel, times(1)).remove(any(JaxrsResourceMethod.class));
		// @HTTP + resource
		assertThat(metamodel.getElements(javaProject).size(), equalTo(2));
	}

	@Test
	public void shouldConvertIntoSubresourceMethodWhenAddingPathAnnotation() throws CoreException {
		createHttpMethod(GET);
		// Parent JAX-RS Resource
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		final JaxrsResourceMethod resourceMethod = createResourceMethod("getCustomer", resource, GET.qualifiedName);
		// operation
		final Annotation pathAnnotation = resolveAnnotation(resourceMethod.getJavaElement(), PATH.qualifiedName);
		final JavaElementDelta event = createEvent(pathAnnotation, ADDED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_PATH_VALUE + F_ELEMENT_KIND));
		// @HTTP + resource + resourceMethod
		assertThat(metamodel.getElements(javaProject).size(), equalTo(3));
	}

	@Test
	public void shouldConvertIntoResourceMethodWhenRemovingPathAnnotation() throws CoreException {
		// pre-conditions
		// JAX-RS HttpMethod
		createHttpMethod(GET);
		// Parent JAX-RS Resource
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		final JaxrsResourceMethod resourceMethod = createResourceMethod("getCustomer", resource, GET.qualifiedName,
				PATH.qualifiedName);
		// operation
		final JavaElementDelta event = createEvent(resourceMethod.getAnnotation(PATH.qualifiedName), REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_PATH_VALUE + F_ELEMENT_KIND));
		// @HTTP + resource + resourceMethod
		assertThat(metamodel.getElements(javaProject).size(), equalTo(3));
	}

	@Test
	public void shouldConvertIntoSubresourceLocatorWhenRemovingHttpMethodAnnotation() throws CoreException {
		// pre-conditions
		// JAX-RS HttpMethod
		createHttpMethod(GET);
		// Parent JAX-RS Resource
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		final JaxrsResourceMethod resourceMethod = createResourceMethod("getCustomer", resource, GET.qualifiedName,
				PATH.qualifiedName);
		// operation
		final JavaElementDelta event = createEvent(resourceMethod.getAnnotation(GET.qualifiedName), REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_HTTP_METHOD_VALUE + F_ELEMENT_KIND));
		// @HTTP + resource + resourceMethod
		assertThat(metamodel.getElements(javaProject).size(), equalTo(3));
	}

	@Test
	public void shouldRemoveSubresourceLocatorWhenRemovingPathAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final JaxrsResourceMethod resourceMethod = createResourceMethod("getProductResourceLocator", resource,
				GET.qualifiedName, PATH.qualifiedName);
		// operation
		final JavaElementDelta event = createEvent(resourceMethod.getAnnotation(PATH.qualifiedName), REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		// resource only
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldUpdateSubresourceMethodWhenChangingPathAnnotationValue() throws CoreException {
		// pre-conditions
		// JAX-RS HttpMethod
		createHttpMethod(GET);
		// Parent JAX-RS Resource
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		final JaxrsResourceMethod resourceMethod = resourceMethodBuilder(resource, "getCustomer").annotation(
				PATH.qualifiedName, "/foo").build();
		// operation
		final JavaElementDelta event = createEvent(resourceMethod.getAnnotation(PATH.qualifiedName), CHANGED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_PATH_VALUE));
		// @HTTP + resource + resourceMethod
		assertThat(metamodel.getElements(javaProject).size(), equalTo(3));
	}

	@Test
	public void shouldUpdateResourceMethodWhenAddingConsumesAnnotation() throws CoreException {
		// pre-conditions
		// JAX-RS HttpMethod
		createHttpMethod(POST);
		// Parent JAX-RS Resource
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		final JaxrsResourceMethod resourceMethod = createResourceMethod("createCustomer", resource, POST.qualifiedName);
		// operation
		final Annotation consumesAnnotation = resolveAnnotation(resourceMethod.getJavaElement(), CONSUMES.qualifiedName);
		final JavaElementDelta event = createEvent(consumesAnnotation, ADDED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_CONSUMED_MEDIATYPES_VALUE));
		// @HTTP + resource + resourceMethod
		assertThat(metamodel.getElements(javaProject).size(), equalTo(3));
	}

	@Test
	public void shouldUpdateResourceMethodWhenChangingConsumesAnnotationValue() throws CoreException {
		// pre-conditions
		// JAX-RS HttpMethod
		createHttpMethod(POST);
		// Parent JAX-RS Resource
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		final JaxrsResourceMethod resourceMethod = resourceMethodBuilder(resource, "createCustomer")
				.annotation(POST.qualifiedName).annotation(CONSUMES.qualifiedName, "application/foo").build();
		// operation
		final JavaElementDelta event = createEvent(resourceMethod.getAnnotation(CONSUMES.qualifiedName), CHANGED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_CONSUMED_MEDIATYPES_VALUE));
		// @HTTP + resource + resourceMethod
		assertThat(metamodel.getElements(javaProject).size(), equalTo(3));
	}

	@Test
	public void shouldUpdateResourceMethodMethodWhenRemovingConsumesAnnotation() throws CoreException {
		// pre-conditions
		// JAX-RS HttpMethod
		createHttpMethod(POST);
		// Parent JAX-RS Resource
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		final JaxrsResourceMethod resourceMethod = createResourceMethod("createCustomer", resource, POST.qualifiedName,
				CONSUMES.qualifiedName);
		// operation
		final JavaElementDelta event = createEvent(resourceMethod.getAnnotation(CONSUMES.qualifiedName), REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_CONSUMED_MEDIATYPES_VALUE));
		// @HTTP + resource + resourceMethod
		assertThat(metamodel.getElements(javaProject).size(), equalTo(3));
	}

	@Test
	public void shouldUpdateResourceMethodWhenAddingProducesAnnotation() throws CoreException {
		// pre-conditions
		// JAX-RS HttpMethod
		createHttpMethod(POST);
		// Parent JAX-RS Resource
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		final JaxrsResourceMethod resourceMethod = createResourceMethod("getCustomerAsVCard", resource,
				GET.qualifiedName);
		// operation
		final Annotation producesAnnotation = resolveAnnotation(resourceMethod.getJavaElement(), PRODUCES.qualifiedName);
		final JavaElementDelta event = createEvent(producesAnnotation, ADDED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_PRODUCED_MEDIATYPES_VALUE));
		// @HTTP + resource + resourceMethod
		assertThat(metamodel.getElements(javaProject).size(), equalTo(3));
	}

	@Test
	public void shouldUpdateResourceMethodWhenChangingProducesAnnotationValue() throws CoreException {
		// pre-conditions
		// JAX-RS HttpMethod
		createHttpMethod(POST);
		final JaxrsResource resource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod resourceMethod = resourceMethodBuilder(resource, "getCustomerAsVCard")
				.annotation(GET.qualifiedName).annotation(PRODUCES.qualifiedName, "application/foo").build();
		// operation
		final JavaElementDelta event = createEvent(resourceMethod.getAnnotation(PRODUCES.qualifiedName), CHANGED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_PRODUCED_MEDIATYPES_VALUE));
		// @HTTP + resource + resourceMethod
		assertThat(metamodel.getElements(javaProject).size(), equalTo(3));
	}

	@Test
	public void shouldUpdateResourceMethodWhenRemovingProducesAnnotation() throws CoreException {
		// pre-conditions
		createHttpMethod(GET);
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		final JaxrsResourceMethod resourceMethod = createResourceMethod("getCustomerAsVCard", resource,
				GET.qualifiedName, PRODUCES.qualifiedName);
		// operation
		final Annotation producesAnnotation = resourceMethod.getAnnotation(PRODUCES.qualifiedName);
		final JavaElementDelta event = createEvent(producesAnnotation, REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_PRODUCED_MEDIATYPES_VALUE));
		// @HTTP + resource + resourceMethod
		assertThat(metamodel.getElements(javaProject).size(), equalTo(3));
	}

	@Test
	public void shouldUpdateResourceMethodWhenAddingParameterWithPathParamAnnotation() throws CoreException {
		// test is not relevant : the old method is replaced by a new one
	}

	@Test
	public void shouldUpdateResourceMethodWhenAddingPathParamAnnotationOnParameter() throws CoreException {
		// the method signature is changed
		// pre-conditions
		createHttpMethod(GET);
		// Parent JAX-RS Resource
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		final JaxrsResourceMethod resourceMethod = resourceMethodBuilder(resource, "getCustomer")
				.annotation(GET.qualifiedName).methodParameter("id", Integer.class.getName())
				.methodParameter("uriInfo", URI_INFO.qualifiedName, createAnnotation(CONTEXT.qualifiedName))
				.returnedType(RESPONSE.qualifiedName).build();
		// operation
		final JavaElementDelta event = createEvent(resourceMethod.getJavaElement(), CHANGED, F_SIGNATURE);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_METHOD_PARAMETERS));
		// @HTTP + resource + resourceMethod
		assertThat(metamodel.getElements(javaProject).size(), equalTo(3));
	}

	@Test
	public void shouldUpdateResourceMethodWhenChangingPathParamAnnotationOnParameter() throws CoreException {
		// the method signature is changed
		// pre-conditions
		// JAX-RS HttpMethod
		createHttpMethod(GET);
		// Parent JAX-RS Resource
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		final JaxrsResourceMethod resourceMethod = resourceMethodBuilder(resource, "getCustomer")
				.annotation(GET.qualifiedName)
				.methodParameter("id", Integer.class.getName(), createAnnotation(PATH_PARAM.qualifiedName, "foo!"))
				.methodParameter("uriInfo", URI_INFO.qualifiedName, createAnnotation(CONTEXT.qualifiedName))
				.returnedType(RESPONSE.qualifiedName).build();
		// operation
		final JavaElementDelta event = createEvent(resourceMethod.getJavaElement(), CHANGED, F_SIGNATURE);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_METHOD_PARAMETERS));
		// @HTTP + resource + resourceMethod
		assertThat(metamodel.getElements(javaProject).size(), equalTo(3));
	}

	@Test
	public void shouldUpdateResourceMethodWhenRemovingPathParamAnnotationOnParameter() throws CoreException {
		// the method signature is changed
		// pre-conditions
		// JAX-RS HttpMethod
		createHttpMethod(PUT);
		// Parent JAX-RS Resource
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		// JAX-RS Resource Method with an extra @PathParam annotation on the
		// 'update' parameter.
		final JaxrsResourceMethod resourceMethod = resourceMethodBuilder(resource, "updateCustomer")
				.methodParameter("id", Integer.class.getName(), createAnnotation(PATH_PARAM.qualifiedName, "id"))
				.methodParameter("update", "org.jboss.tools.ws.jaxrs.sample.services.CustomerResource",
						createAnnotation(PATH_PARAM.qualifiedName, "foo")).returnedType(void.class.getName()).build();
		// operation
		final JavaElementDelta event = createEvent(resourceMethod.getJavaElement(), CHANGED, F_SIGNATURE);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_METHOD_PARAMETERS));
		// @HTTP + resource + resourceMethod
		assertThat(metamodel.getElements(javaProject).size(), equalTo(3));
	}

	@Test
	public void shouldUpdateResourceMethodWhenAddingParameterWithQueryParamAnnotation() throws CoreException {
		// the method signature is changed
		// pre-conditions
		// JAX-RS HttpMethod
		createHttpMethod(GET);
		// Parent JAX-RS Resource
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		// JAX-RS Resource Method (size param is not declared)
		final JaxrsResourceMethod resourceMethod = resourceMethodBuilder(resource, "getCustomers")
				.annotation(GET.qualifiedName)
				.methodParameter("start", "int", createAnnotation(QUERY_PARAM.qualifiedName, "start"))
				.methodParameter("uriInfo", URI_INFO.qualifiedName, createAnnotation(CONTEXT.qualifiedName))
				.returnedType("java.util.List").build();
		// operation
		final JavaElementDelta event = createEvent(resourceMethod.getJavaElement(), CHANGED, F_SIGNATURE);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_METHOD_PARAMETERS));
		assertThat(impacts.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.RESOURCE_METHOD));
		// @HTTP + resource + resourceMethod
		assertThat(metamodel.getElements(javaProject).size(), equalTo(3));
	}

	@Test
	public void shouldUpdateResourceMethodWhenAddingParameterWithMatrixParamAnnotation() throws CoreException {
		// the method signature is changed
		// pre-conditions
		// JAX-RS HttpMethod
		createHttpMethod(GET);
		// Parent JAX-RS Resource
		final JaxrsResource resource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource",
				PATH.qualifiedName);
		final IMethod method = createMethod(resource, "getPicture");
		// operation
		final JavaElementDelta event = createEvent(method, CHANGED, F_SIGNATURE);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_METHOD_PARAMETERS));
		assertThat(impacts.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.RESOURCE_METHOD));
		// @HTTP + resource + resourceMethod
		assertThat(metamodel.getElements(javaProject).size(), equalTo(3));
	}

	protected IMethod createMethod(final JaxrsResource resource, String methodName) throws JavaModelException,
			CoreException {
		final IMethod method = getMethod(resource.getJavaElement(), methodName);
		final JavaMethodParameter pathParameter = new JavaMethodParameter("id", String.class.getName(),
				Arrays.asList(createAnnotation(PATH_PARAM.qualifiedName, null)));
		final List<JavaMethodParameter> methodParameters = Arrays.asList(pathParameter);
		final Map<String, Annotation> methodAnnotations = resolveAnnotations(method, PATH.qualifiedName);
		final IType returnedType = getType("java.lang.Object");
		final JaxrsResourceMethod resourceMethod = new JaxrsResourceMethod(method, resource, methodParameters,
				returnedType, methodAnnotations, metamodel);
		metamodel.add(resourceMethod);
		return method;
	}

	@Test
	public void shouldUpdateResourceMethodWhenAddingQueryParamAnnotationOnParameter() throws CoreException {
		// the method signature is changed
		// pre-conditions
		// JAX-RS HttpMethod
		createHttpMethod(GET);
		// Parent JAX-RS Resource
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		// JAX-RS Resource Method (@QueryParam on 'size' param is not declared)
		final JaxrsResourceMethod resourceMethod = resourceMethodBuilder(resource, "getCustomers")
				.annotation(GET.qualifiedName)
				.methodParameter("start", "int", createAnnotation(QUERY_PARAM.qualifiedName, "start"))
				.methodParameter("size", "int", createAnnotation(DEFAULT_VALUE.qualifiedName, "2"))
				.methodParameter("uriInfo", URI_INFO.qualifiedName, createAnnotation(CONTEXT.qualifiedName))
				.returnedType("java.util.List").build();
		// operation
		final JavaElementDelta event = createEvent(resourceMethod.getJavaElement(), CHANGED, F_SIGNATURE);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_METHOD_PARAMETERS));
		// @HTTP + resource + resourceMethod
		assertThat(metamodel.getElements(javaProject).size(), equalTo(3));
	}

	@Test
	public void shouldUpdateResourceMethodWhenChangingQueryParamAnnotationOnParameter() throws CoreException {
		// the method signature is changed
		// pre-conditions
		// JAX-RS HttpMethod
		createHttpMethod(GET);
		// Parent JAX-RS Resource
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		// JAX-RS Resource Method (QueryParam value is different: "length" vs
		// "size" on second param)
		final JaxrsResourceMethod resourceMethod = resourceMethodBuilder(resource, "getCustomers")
				.annotation(GET.qualifiedName)
				.methodParameter("start", "int", createAnnotation(QUERY_PARAM.qualifiedName, "start"))
				.methodParameter("size", "int", createAnnotation(QUERY_PARAM.qualifiedName, "length"),
						createAnnotation(DEFAULT_VALUE.qualifiedName, "2"))
				.methodParameter("uriInfo", URI_INFO.qualifiedName, createAnnotation(CONTEXT.qualifiedName))
				.returnedType("java.util.List").build();
		// operation
		final JavaElementDelta event = createEvent(resourceMethod.getJavaElement(), CHANGED, F_SIGNATURE);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_METHOD_PARAMETERS));
		// @HTTP + resource + resourceMethod
		assertThat(metamodel.getElements(javaProject).size(), equalTo(3));
	}

	@Test
	public void shouldUpdateResourceMethodWhenRemovingQueryParamAnnotationOnParameter() throws CoreException {
		// the method signature is changed
		// pre-conditions
		// JAX-RS HttpMethod
		createHttpMethod(GET);
		// Parent JAX-RS Resource
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		// JAX-RS Resource Method (with an extra @Queryparam annotation on 'uriInfo' param)
		final JaxrsResourceMethod resourceMethod = resourceMethodBuilder(resource, "getCustomers")
				.annotation(GET.qualifiedName)
				.methodParameter("start", "int", createAnnotation(QUERY_PARAM.qualifiedName, "start"))
				.methodParameter("size", "int", createAnnotation(QUERY_PARAM.qualifiedName, "length"),
						createAnnotation(DEFAULT_VALUE.qualifiedName, "2"))
				.methodParameter("uriInfo", URI_INFO.qualifiedName, createAnnotation(CONTEXT.qualifiedName), createAnnotation(QUERY_PARAM.qualifiedName, "foo"))
				.returnedType("java.util.List").build();
		// operation
		final JavaElementDelta event = createEvent(resourceMethod.getJavaElement(), CHANGED, F_SIGNATURE);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_METHOD_PARAMETERS));
		// @HTTP + resource + resourceMethod
		assertThat(metamodel.getElements(javaProject).size(), equalTo(3)); 
	}

	@Test
	public void shouldUpdateResourceMethodWhenAddingMatrixParamAnnotationOnParameter() throws CoreException {
		// the method signature is changed
		// pre-conditions
		// JAX-RS HttpMethod
		createHttpMethod(GET);
		// Parent JAX-RS Resource
		final JaxrsResource resource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource",
				PATH.qualifiedName);
		final JaxrsResourceMethod resourceMethod = resourceMethodBuilder(resource, "getPicture")
				.annotation(GET.qualifiedName)
				.methodParameter("id", Integer.class.getName(), createAnnotation(PATH_PARAM.qualifiedName, "id"))
				.methodParameter("color", String.class.getName())
				.returnedType("java.lang.Object").build();
		// operation
		final JavaElementDelta event = createEvent(resourceMethod.getJavaElement(), CHANGED, F_SIGNATURE);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_METHOD_PARAMETERS));
		// @HTTP + resource + resourceMethod
		assertThat(metamodel.getElements(javaProject).size(), equalTo(3));
	}

	@Test
	public void shouldUpdateResourceMethodWhenChangingMatrixParamAnnotationOnParameter() throws CoreException {
		// the method signature is changed
		// pre-conditions
		// JAX-RS HttpMethod
		createHttpMethod(GET);
		// Parent JAX-RS Resource
		final JaxrsResource resource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource",
				PATH.qualifiedName);
		// JAX-RS Resource Method (MATRIX_PARAM.qualifiedName value is
		// different: "foo" vs "color" on second param)
		final JaxrsResourceMethod resourceMethod = resourceMethodBuilder(resource, "getPicture")
				.annotation(GET.qualifiedName)
				.methodParameter("id", Integer.class.getName(), createAnnotation(PATH_PARAM.qualifiedName, "id"))
				.methodParameter("color", String.class.getName(), createAnnotation(MATRIX_PARAM.qualifiedName, "foo"))
				.returnedType("java.lang.Object").build();
		// operation
		final JavaElementDelta event = createEvent(resourceMethod.getJavaElement(), CHANGED, F_SIGNATURE);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_METHOD_PARAMETERS));
		// @HTTP + resource + resourceMethod
		assertThat(metamodel.getElements(javaProject).size(), equalTo(3));
	}

	@Test
	public void shouldUpdateResourceMethodWhenRemovingMatrixParamAnnotationOnParameter() throws CoreException {
		// the method signature is changed
		// pre-conditions
		// JAX-RS HttpMethod
		createHttpMethod(GET);
		// Parent JAX-RS Resource
		final JaxrsResource resource = createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource",
				PATH.qualifiedName);
		// JAX-RS Resource Method (an extra MATRIX_PARAM.qualifiedName
		// annotation on 'id' param)
		final JaxrsResourceMethod resourceMethod = resourceMethodBuilder(resource, "getProduct")
				.annotation(GET.qualifiedName)
				.methodParameter("id", Integer.class.getName(), createAnnotation(MATRIX_PARAM.qualifiedName, "foo"), createAnnotation(PATH_PARAM.qualifiedName, "id"))
				.returnedType("org.jboss.tools.ws.jaxrs.sample.domain.Book").build();
		// operation
		final JavaElementDelta event = createEvent(resourceMethod.getJavaElement(), CHANGED, F_SIGNATURE);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_METHOD_PARAMETERS));
		// @HTTP + resource + resourceMethod
		assertThat(metamodel.getElements(javaProject).size(), equalTo(3));
	}

	@Test
	public void shouldUpdateResourceMethodWhenChangingReturnType() throws CoreException {
		// the method signature is changed
		// pre-conditions
		// JAX-RS HttpMethod
		createHttpMethod(GET);
		// Parent JAX-RS Resource
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		// JAX-RS Resource Method (declared return type is
		// 'javax.ws.rs.Response')
		final IMethod method = getMethod(resource.getJavaElement(), "getCustomers");
		final JavaMethodParameter startParameter = new JavaMethodParameter("start", "int",
				Arrays.asList(createAnnotation(QUERY_PARAM.qualifiedName, "start")));
		final JavaMethodParameter sizeParameter = new JavaMethodParameter("size", "int",
				Arrays.asList(createAnnotation(QUERY_PARAM.qualifiedName, "size"),
						createAnnotation(DEFAULT_VALUE.qualifiedName, "2")));
		final JavaMethodParameter uriInfoParameter = new JavaMethodParameter("uriInfo", URI_INFO.qualifiedName,
				Arrays.asList(createAnnotation(CONTEXT.qualifiedName, null)));
		final List<JavaMethodParameter> methodParameters = Arrays.asList(startParameter, sizeParameter,
				uriInfoParameter);
		final Map<String, Annotation> methodAnnotations = resolveAnnotations(method, GET.qualifiedName);
		final IType returnedType = getType(RESPONSE.qualifiedName);
		final JaxrsResourceMethod resourceMethod = new JaxrsResourceMethod(method, resource, methodParameters,
				returnedType, methodAnnotations, metamodel);
		metamodel.add(resourceMethod);
		// operation
		final JavaElementDelta event = createEvent(method, CHANGED, F_SIGNATURE);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		// annotations were not provided but are retrieved...
		assertThat(impacts.get(0).getFlags(), equalTo(F_METHOD_RETURN_TYPE + F_METHOD_PARAMETERS));
		// @HTTP + resource + resourceMethod
		assertThat(metamodel.getElements(javaProject).size(), equalTo(3));
	}

	@Test
	public void shouldDoNothingMethodWhenAddingParamWithUnrelatedAnnotation() throws CoreException {
		// test is not relevant : the old method is replaced by a new one
	}

	@Test
	@Ignore("For now, accept all annotations")
	public void shouldDoNothingMethodWhenAddingUnrelatedAnnotationOnParameter() throws CoreException {
		// the method signature is changed
		// pre-conditions
		// Parent JAX-RS Resource
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		// JAX-RS Resource Method (@Context is not declared on 'uriInfo' param)
		final JaxrsResourceMethod resourceMethod = resourceMethodBuilder(resource, "getCustomers")
				.methodParameter("start", "int", createAnnotation(QUERY_PARAM.qualifiedName, "start"))
				.methodParameter("size", "int", createAnnotation(QUERY_PARAM.qualifiedName, "length"),
						createAnnotation(DEFAULT_VALUE.qualifiedName, "2"))
				.methodParameter("uriInfo", URI_INFO.qualifiedName).build();
		// operation
		final JavaElementDelta event = createEvent(resourceMethod.getJavaElement(), CHANGED, F_SIGNATURE);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(0));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
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
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		final IMethod method = getMethod(resource.getJavaElement(), "getEntityManager");
		// operation
		final JavaElementDelta event = createEvent(method, ADDED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(0));
	}

	@Test
	public void shouldDoNothingWhenChangingBasicJavaMethod() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		final IMethod method = getMethod(resource.getJavaElement(), "getEntityManager");
		// operation
		final JavaElementDelta event = createEvent(method, CHANGED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(0));
	}

	@Test
	public void shouldDoNothingWhenRemovingBasicJavaMethod() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		final IMethod method = getMethod(resource.getJavaElement(), "getEntityManager");
		// operation
		final JavaElementDelta event = createEvent(method, REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(0));
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
	
	
	
	@Test @Ignore
	public void shouldAddProviderWhenAddingSourceCompilationUnit() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		// operation
		final JavaElementDelta event = createEvent(type.getCompilationUnit(), ADDED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.PROVIDER));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(ADDED));
		verify(metamodel, times(1)).add(any(JaxrsProvider.class));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test @Ignore
	public void shouldAddProviderWhenAddingSourceType() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		// operation
		final JavaElementDelta event = createEvent(type, ADDED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((JaxrsJavaApplication) impacts.get(0).getElement()).getApplicationPath(), equalTo("/app"));
		verify(metamodel, times(1)).add(any(JaxrsJavaApplication.class));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test @Ignore
	public void shouldAddProviderWhenAddingProviderAnnotation() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final Annotation pathAnnotation = resolveAnnotation(type, APPLICATION_PATH.qualifiedName);
		// operation
		final JavaElementDelta event = createEvent(pathAnnotation, ADDED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((JaxrsJavaApplication) impacts.get(0).getElement()).getApplicationPath(), equalTo("/app"));
		verify(metamodel, times(1)).add(any(JaxrsJavaApplication.class));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test @Ignore
	public void shouldDoNothingWhenAddingUnrelatedAnnotationOnProvider() throws CoreException {
		// pre-conditions
		final JaxrsJavaApplication application = createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final Annotation suppressWarningAnnotation = resolveAnnotation(application.getJavaElement(),
				SuppressWarnings.class.getName());
		// operation
		final JavaElementDelta event = createEvent(suppressWarningAnnotation, ADDED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(0));
		// one call, during pre-conditions
		verify(metamodel, times(1)).add(any(JaxrsJavaApplication.class));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test @Ignore
	public void shouldDoNothingWhenAnnotationValueRemainsSameOnProvider() throws CoreException {
		// pre-conditions
		final JaxrsJavaApplication application = createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		// operation
		final JavaElementDelta event = createEvent(application.getAnnotation(APPLICATION_PATH.qualifiedName), CHANGED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(0));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test @Ignore
	public void shouldDoNothingWhenChangingUnrelatedProviderAnnotationValue() throws CoreException {
		// pre-conditions
		final JaxrsJavaApplication application = createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final Annotation suppressWarningsAnnotation = resolveAnnotation(application.getJavaElement(),
				SuppressWarnings.class.getName());
		// operation
		final JavaElementDelta event = createEvent(suppressWarningsAnnotation, CHANGED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(0));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test @Ignore
	public void shouldRemoveProviderWhenRemovingCompilationUnit() throws CoreException {
		// pre-conditions
		final JaxrsJavaApplication application = createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		// operation
		final JavaElementDelta event = createEvent(application.getJavaElement().getCompilationUnit(), REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((JaxrsJavaApplication) impacts.get(0).getElement()), equalTo(application));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(0));
	}

	@Test @Ignore
	public void shouldRemoveProviderWhenRemovingSourceType() throws CoreException {
		// pre-conditions
		final JaxrsJavaApplication application = createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		// operation
		final JavaElementDelta event = createEvent(application.getJavaElement(), REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((JaxrsJavaApplication) impacts.get(0).getElement()).getApplicationPath(), equalTo("/app"));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(0));
	}

	@Test @Ignore
	public void shouldNotRemoveProviderWhenRemovingAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsJavaApplication application = createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		// operation
		final JavaElementDelta event = createEvent(application.getAnnotation(APPLICATION_PATH.qualifiedName), REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getElement(), is(notNullValue()));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
		assertThat(application.getApplicationPath(), nullValue());
	}

	@Test @Ignore
	public void shouldRemoveProviderWhenRemovingAnnotationAndHierarchyAlreadyMissing() throws CoreException {
		// pre-conditions
		/*
		 * final IType type =
		 * resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper"
		 * ); final Annotation appPathAnnotation = resolveAnnotation(type,
		 * APPLICATION_PATH.qualifiedName); final JaxrsJavaApplication
		 * application = new JaxrsJavaApplication(type, appPathAnnotation,
		 * false, metamodel); metamodel.add(application);
		 */
		final JaxrsJavaApplication application = createJavaApplication(
				"org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper", false);
		// operation
		final JavaElementDelta event = createEvent(application.getAnnotation(APPLICATION_PATH.qualifiedName), REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(impacts.get(0).getElement(), is(notNullValue()));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(0));
	}

	@Test @Ignore
	public void shouldRemoveProviderWhenRemovingHierarchyAndAnnotationAlreadyMissing() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		WorkbenchUtils.replaceFirstOccurrenceOfCode(type, "@ApplicationPath(\"/app\")", "", false);
		WorkbenchUtils.replaceFirstOccurrenceOfCode(type, "extends Application", "", false);
		createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		// operation
		final JavaElementDelta event = createEvent(type, CHANGED, IJavaElementDeltaFlag.F_SUPER_TYPES);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(impacts.get(0).getElement(), is(notNullValue()));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(0));
	}

	@Test @Ignore
	public void shouldDoNothingWhenRemovingUnrelatedAnnotationOnProvider() throws CoreException {
		// pre-conditions
		final JaxrsJavaApplication application = createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		// operation
		final JavaElementDelta event = createEvent(application.getAnnotation(SuppressWarnings.class.getName()), REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(0));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test @Ignore
	public void shouldRemoveProviderWhenRemovingSourceFolder() throws CoreException {
		// pre-conditions
		createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final IPackageFragmentRoot sourceFolder = getPackageFragmentRoot("src/main/java");
		// operation
		final JavaElementDelta event = createEvent(sourceFolder, REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(impacts.get(0).getElement(), is(notNullValue()));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(0));
	}
	
	@Test @Ignore
	public void shouldUpdateProviderWhenAddingConsumesAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		// operation
		final Annotation consumesAnnotation = resolveAnnotation(resource.getJavaElement(), CONSUMES.qualifiedName);
		final JavaElementDelta event = createEvent(consumesAnnotation, ADDED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_CONSUMED_MEDIATYPES_VALUE));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test @Ignore
	public void shouldUpdateProviderWhenChangingConsumesAnnotationValue() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName, CONSUMES.qualifiedName);
		final Annotation consumesAnnotation = changeAnnotationValue(resource.getAnnotation(CONSUMES.qualifiedName),
				"application/foo");
		resource.updateAnnotations(CollectionUtils.toMap(CONSUMES.qualifiedName, consumesAnnotation));
		// operation
		final JavaElementDelta event = createEvent(consumesAnnotation, CHANGED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_CONSUMED_MEDIATYPES_VALUE));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test @Ignore
	public void shouldUpdateProviderWhenRemovingConsumesAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName, CONSUMES.qualifiedName);
		// operation
		final JavaElementDelta event = createEvent(resource.getAnnotation(CONSUMES.qualifiedName), REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_CONSUMED_MEDIATYPES_VALUE));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test @Ignore
	public void shouldUpdateProviderWhenAddingProducesAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		// operation
		final Annotation producesAnnotation = resolveAnnotation(resource.getJavaElement(), PRODUCES.qualifiedName);
		final JavaElementDelta event = createEvent(producesAnnotation, ADDED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_PRODUCED_MEDIATYPES_VALUE));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test @Ignore
	public void shouldUpdateProviderWhenChangingProducesAnnotationValue() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName, PRODUCES.qualifiedName);
		final Annotation producesAnnotation = changeAnnotationValue(resource.getAnnotation(PRODUCES.qualifiedName),
				"application/foo");
		resource.updateAnnotations(CollectionUtils.toMap(PRODUCES.qualifiedName, producesAnnotation));
		// operation
		final JavaElementDelta event = createEvent(producesAnnotation, CHANGED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_PRODUCED_MEDIATYPES_VALUE));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test @Ignore
	public void shouldUpdateProviderWhenRemovingProducesAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName, PRODUCES.qualifiedName);
		// operation
		final JavaElementDelta event = createEvent(resource.getAnnotation(PRODUCES.qualifiedName), REMOVED);
		final List<JaxrsElementDelta> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getFlags(), equalTo(F_PRODUCED_MEDIATYPES_VALUE));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

}
