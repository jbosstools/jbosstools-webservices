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
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.jboss.tools.ws.jaxrs.core.WorkbenchUtils.replaceFirstOccurrenceOfCode;
import static org.jboss.tools.ws.jaxrs.core.WorkbenchUtils.resolveAnnotation;
import static org.jboss.tools.ws.jaxrs.core.WorkbenchUtils.resolveAnnotations;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.APPLICATION_PATH;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.CONTEXT;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.DELETE;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.GET;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.HTTP_METHOD;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.PATH;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.PATH_PARAM;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.POST;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.PUT;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.TARGET;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.hamcrest.Matchers;
import org.jboss.tools.ws.jaxrs.core.AbstractCommonTestCase;
import org.jboss.tools.ws.jaxrs.core.WorkbenchUtils;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsJavaApplication;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceField;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsWebxmlApplication;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.CompilationUnitsRepository;
import org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementCategory;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsApplication;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.metamodel.JaxrsMetamodelDelta;
import org.junit.Ignore;
import org.junit.Test;

public class ResourceChangedProcessorTestCase extends AbstractCommonTestCase {

	public final static int NO_FLAG = 0;

	public final ResourceChangedProcessor processor = new ResourceChangedProcessor();

	public static final IProgressMonitor progressMonitor = new NullProgressMonitor();

	protected ResourceDelta createFullResourceDelta(IResource resource, int deltaKind) {
		return new ResourceDelta(resource, deltaKind, NO_FLAG);
	}

	protected List<JaxrsElementDelta> processResourceChanges(ResourceDelta event, IProgressMonitor progressmonitor)
			throws CoreException {
		final JaxrsMetamodelDelta affectedMetamodel = processor.processAffectedResources(project, false,
				Arrays.asList(event), progressmonitor);
		return affectedMetamodel.getAffectedElements();
	}

	@Test
	public void shouldAddHttpMethodsAndResourcesWhenAddingSourceFolderWithExistingMetamodel() throws CoreException {
		// pre-conditions
		// operation
		final IPackageFragmentRoot sourceFolder = getPackageFragmentRoot("src/main/java");
		final ResourceDelta event = createFullResourceDelta(sourceFolder.getResource(), ADDED);
		final JaxrsMetamodelDelta affectedMetamodel = processor.processAffectedResources(project, false,
				Arrays.asList(event), progressMonitor);
		// verifications
		assertThat(affectedMetamodel.getDeltaKind(), equalTo(CHANGED));
		assertThat(affectedMetamodel.getMetamodel(), equalTo((IJaxrsMetamodel) metamodel));
		final List<JaxrsElementDelta> affectedElements = affectedMetamodel.getAffectedElements();
		// 1 application + 1 HttpMethod + 7 Resources
		assertThat(affectedElements.size(), equalTo(9));
		assertThat(affectedElements, everyItem(Matchers.<JaxrsElementDelta> hasProperty("deltaKind", equalTo(ADDED))));
		// all HttpMethods, Resources, ResourceMethods and ResourceFields. only
		// application is available: the java-based
		// one found in src/main/java
		assertThat(metamodel.getElements(javaProject).size(), equalTo(35));
	}

	@Test
	public void shouldAddHttpMethodsAndResourcesWhenAddingSourceFolderWithExistingMetamodelWithReset()
			throws CoreException {
		// pre-conditions
		// operation
		final IPackageFragmentRoot sourceFolder = getPackageFragmentRoot("src/main/java");
		final ResourceDelta event = createFullResourceDelta(sourceFolder.getResource(), ADDED);
		final JaxrsMetamodelDelta affectedMetamodel = processor.processAffectedResources(project, true,
				Arrays.asList(event), progressMonitor);
		// verifications
		assertThat(affectedMetamodel.getDeltaKind(), equalTo(CHANGED));
		metamodel = (JaxrsMetamodel) affectedMetamodel.getMetamodel();
		assertThat(metamodel, equalTo((IJaxrsMetamodel) metamodel));
		final List<JaxrsElementDelta> affectedElements = affectedMetamodel.getAffectedElements();
		// 1 application + 1 HttpMethod + 7 Resources
		assertThat(affectedElements.size(), equalTo(9));
		assertThat(affectedElements, everyItem(Matchers.<JaxrsElementDelta> hasProperty("deltaKind", equalTo(ADDED))));
		// all project-specific Applications, HttpMethods, Resources,
		// ResourceMethods and ResourceFields (built-in
		// HttpMethods are not bound to a project)
		// 2 applications are available: the java-based and the web.xml since a
		// full build was performed
		assertThat(metamodel.getElements(javaProject).size(), equalTo(36));
	}

	@Test
	public void shouldAddHttpMethodsAndResourcesWhenAddingSourceFolderWithoutExistingMetamodel() throws CoreException {
		// pre-conditions
		// remove the metamodel
		metamodel.remove();
		// operation
		final IPackageFragmentRoot sourceFolder = getPackageFragmentRoot("src/main/java");
		final ResourceDelta event = createFullResourceDelta(sourceFolder.getResource(), ADDED);
		final JaxrsMetamodelDelta affectedMetamodel = processor.processAffectedResources(project, false,
				Arrays.asList(event), progressMonitor);
		// verifications
		// 1 application + 1 HttpMethod + 3 RootResources + 2 Subresources
		assertThat(affectedMetamodel.getDeltaKind(), equalTo(ADDED));
		metamodel = (JaxrsMetamodel) affectedMetamodel.getMetamodel();
		assertThat(metamodel, notNullValue());
		final List<JaxrsElementDelta> affectedElements = affectedMetamodel.getAffectedElements();
		assertThat(affectedElements.size(), equalTo(9));
		// all Applications, HttpMethods, Resources, ResourceMethods and
		// ResourceFields specific to the project
		assertThat(metamodel.getElements(javaProject).size(), equalTo(36));

	}

	@Test
	public void shouldNotAddAnythingAddingBinaryLib() throws CoreException {
		// pre-conditions
		final IPackageFragmentRoot lib = getPackageFragmentRoot("lib/jaxrs-api-2.0.1.GA.jar");
		// operation
		final ResourceDelta event = createFullResourceDelta(lib.getResource(), ADDED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications: jar should not be taken into account, even if if it
		// contains matching elements...
		assertThat(affectedElements.size(), equalTo(0));
	}

	@Test
	public void shouldAddHttpMethodWhenAddingSourceCompilationUnit() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		// operation
		final ResourceDelta event = createFullResourceDelta(type.getCompilationUnit().getResource(), ADDED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1));
		assertThat(affectedElements.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.HTTP_METHOD));
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldAddJavaApplicationWhenChangingResource() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final Annotation annotation = resolveAnnotation(type, APPLICATION_PATH.qualifiedName);
		// operation
		final ResourceDelta event = createFullResourceDelta(annotation.getJavaParent().getResource(), CHANGED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1));
		assertThat(affectedElements.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((IJaxrsApplication) affectedElements.get(0).getElement()).getApplicationPath(), equalTo("/app"));
		verify(metamodel, times(1)).add(any(JaxrsJavaApplication.class));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldChangeJavaApplicationWhenChangingResource() throws CoreException {
		// pre-conditions
		final JaxrsJavaApplication javaApplication = createJavaApplication(
				"org.jboss.tools.ws.jaxrs.sample.services.RestApplication", "/bar");
		// operation
		final ResourceDelta event = createFullResourceDelta(javaApplication.getResource(), CHANGED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1));
		assertThat(affectedElements.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(((IJaxrsApplication) affectedElements.get(0).getElement()).getApplicationPath(), equalTo("/app"));
		verify(metamodel, times(1)).add(any(JaxrsJavaApplication.class));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldChangeJavaApplicationWhenRemovingAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsJavaApplication javaApplication = createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		// operation
		final Annotation annotation = javaApplication.getAnnotation(APPLICATION_PATH.qualifiedName);
		WorkbenchUtils.delete(annotation.getJavaAnnotation(), false);
		final ResourceDelta event = createFullResourceDelta(annotation.getJavaParent().getResource(), CHANGED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1));
		assertThat(affectedElements.get(0).getElement().getElementKind(), equalTo(EnumElementKind.APPLICATION_JAVA));
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(((IJaxrsApplication) affectedElements.get(0).getElement()).getApplicationPath(), nullValue());
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldChangeJavaApplicationWhenRemovingSupertype() throws CoreException {
		// pre-conditions
		final JaxrsJavaApplication javaApplication = createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		// operation
		WorkbenchUtils.replaceAllOccurrencesOfCode(javaApplication.getJavaElement().getCompilationUnit(),
				"extends Application", "", false);
		final ResourceDelta event = createFullResourceDelta(javaApplication.getResource(), CHANGED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1));
		assertThat(affectedElements.get(0).getElement().getElementKind(), equalTo(EnumElementKind.APPLICATION_JAVA));
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(((IJaxrsApplication) affectedElements.get(0).getElement()).getApplicationPath(), equalTo("/app"));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldRemoveJavaApplicationWhenRemovingCompilationUnit() throws CoreException {
		// pre-conditions
		final JaxrsJavaApplication javaApplication = createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		// operation
		final ResourceDelta event = createFullResourceDelta(javaApplication.getResource(), REMOVED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1));
		assertThat(affectedElements.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((JaxrsJavaApplication) affectedElements.get(0).getElement()), equalTo(javaApplication));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(0));
	}

	@Test
	public void shouldRemoveJavaApplicationWhenRemovingSourceType() throws CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = createHttpMethod("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		// operation
		WorkbenchUtils.delete(httpMethod.getJavaElement());
		final ResourceDelta event = createFullResourceDelta(httpMethod.getResource(), CHANGED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1));
		assertThat(affectedElements.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.HTTP_METHOD));
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((IJaxrsHttpMethod) affectedElements.get(0).getElement()).getHttpVerb(), equalTo("FOO"));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(0));
	}

	@Test
	public void shouldRemoveJavaApplicationWhenRemovingSourceFolder() throws CoreException {
		// pre-conditions
		createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		// operation
		final IPackageFragmentRoot sourceFolder = getPackageFragmentRoot("src/main/java");
		final ResourceDelta event = createFullResourceDelta(sourceFolder.getResource(), REMOVED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1));
		assertThat(affectedElements.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(affectedElements.get(0).getElement(), is(notNullValue()));
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(0));
	}

	@Test
	public void shouldAddWebxmlApplicationWhenAddingWebxml() throws Exception {
		// pre-conditions
		final IResource webxmlResource = WorkbenchUtils.replaceDeploymentDescriptorWith(javaProject,
				"web-3_0-with-default-servlet-mapping.xml");
		// operation
		final ResourceDelta event = createFullResourceDelta(webxmlResource, ADDED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1));
		assertThat(affectedElements.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((JaxrsWebxmlApplication) affectedElements.get(0).getElement()).getApplicationPath(),
				equalTo("/hello"));
		verify(metamodel, times(1)).add(any(JaxrsWebxmlApplication.class));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldNotAddWebxmlApplicationWhenAddingEmptyWebxml() throws Exception {
		// pre-conditions
		final IResource webxmlResource = WorkbenchUtils.replaceDeploymentDescriptorWith(javaProject,
				"web-3_0-without-servlet-mapping.xml");
		// operation
		final ResourceDelta event = createFullResourceDelta(webxmlResource, ADDED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(0));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(0));
	}

	@Test
	public void shouldAddWebxmlApplicationWhenChangingWebxml() throws Exception {
		// pre-conditions
		final IResource webxmlResource = WorkbenchUtils.replaceDeploymentDescriptorWith(javaProject,
				"web-3_0-with-default-servlet-mapping.xml");
		// operation
		final ResourceDelta event = createFullResourceDelta(webxmlResource, CHANGED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1));
		assertThat(affectedElements.get(0).getElement().getElementKind(), equalTo(EnumElementKind.APPLICATION_WEBXML));
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((JaxrsWebxmlApplication) affectedElements.get(0).getElement()).getApplicationPath(),
				equalTo("/hello"));
		verify(metamodel, times(1)).add(any(JaxrsWebxmlApplication.class));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldOverrideJavaApplicationWhenAddingCustomServletMapping() throws Exception {
		// in this test, the java-application exists first, and then a web.xml
		// application is added -> it should immediately override the java-one
		// pre-conditions
		createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		// operation
		final IResource webxmlResource = WorkbenchUtils.replaceDeploymentDescriptorWith(javaProject,
				"web-3_0-with-custom-servlet-mapping.xml");
		final ResourceDelta event = createFullResourceDelta(webxmlResource, ADDED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications: the JAVA Application is the sole element to be really
		// changed
		assertThat(affectedElements.size(), equalTo(2));
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(affectedElements.get(0).getElement().getElementKind(), equalTo(EnumElementKind.APPLICATION_WEBXML));
		final JaxrsWebxmlApplication webxmlApplication = (JaxrsWebxmlApplication) affectedElements.get(0).getElement();
		assertThat(webxmlApplication.getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(webxmlApplication.getApplicationPath(), equalTo("/hello"));

		assertThat(affectedElements.get(1).getElement().getElementKind(), equalTo(EnumElementKind.APPLICATION_JAVA));
		assertThat(affectedElements.get(1).getDeltaKind(), equalTo(CHANGED));
		final JaxrsJavaApplication javaApplication = (JaxrsJavaApplication) affectedElements.get(1).getElement();
		// custom web.xml override DOES NOT precede the java based JAX-RS
		// Application element
		assertThat(metamodel.getApplication(), equalTo((IJaxrsApplication) javaApplication));
		// Java-based application configuration should not be changed
		assertThat(javaApplication.getApplicationPath(), equalTo("/hello"));
		// old application (java) + new one (web.xml)
		assertThat(metamodel.getElements(javaProject).size(), equalTo(2)); 
		verify(metamodel, times(1)).add(any(JaxrsWebxmlApplication.class));
	}

	/**
	 * in this test, the webxml exists first, and then an annotated Java
	 * Application is added -> it should be immediately overriden
	 */
	@Test
	public void shouldOverrideJavaApplicationWhenAddingAnnotatedJavaApplication() throws Exception {
		// precondition
		createWebxmlApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication", "/hello");
		// operation
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final ResourceDelta event = createFullResourceDelta(type.getResource(), ADDED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications: the JAVA Application is the sole element to be really
		// changed
		assertThat(affectedElements.size(), equalTo(1));
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(affectedElements.get(0).getElement().getElementKind(), equalTo(EnumElementKind.APPLICATION_JAVA));
		final JaxrsJavaApplication javaApplication = (JaxrsJavaApplication) affectedElements.get(0).getElement();
		assertThat(javaApplication.getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(javaApplication.getApplicationPath(), equalTo("/hello"));
		// old application (web.xml) + new one (java)
		assertThat(metamodel.getElements(javaProject).size(), equalTo(2)); 
		verify(metamodel, times(1)).add(any(JaxrsJavaApplication.class));
	}

	/**
	 * in this test, the webxml exists first, and then a NOT annotated Java
	 * Application is added -> it should be immediately overriden
	 * 
	 * @throws Exception
	 */
	@Test
	public void shouldOverrideJavaApplicationWhenAddingUnannotatedJavaApplication() throws Exception {
		// precondition
		createWebxmlApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication", "/hello");
		// operation
		IType type = replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.RestApplication",
				javaProject, "@ApplicationPath(\"/app\")", "", false);
		final ResourceDelta event = createFullResourceDelta(type.getResource(), ADDED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications: the JAVA Application is the sole element to be really
		// changed
		assertThat(affectedElements.size(), equalTo(1));
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(affectedElements.get(0).getElement().getElementKind(), equalTo(EnumElementKind.APPLICATION_JAVA));
		final JaxrsJavaApplication javaApplication = (JaxrsJavaApplication) affectedElements.get(0).getElement();
		assertThat(javaApplication.getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(javaApplication.getApplicationPath(), equalTo("/hello"));
		// old application (web.xml) + new one (java)
		assertThat(metamodel.getElements(javaProject).size(), equalTo(2)); 
		verify(metamodel, times(1)).add(any(JaxrsJavaApplication.class));
	}

	/**
	 * In this test, the java application path override should be removed when
	 * the web.xml application is removed from the web.xml file
	 * 
	 * @throws Exception
	 */
	@Test
	public void shouldUnoverrideAnnotatedJavaApplicationWhenRemovingCustomWebxml() throws Exception {
		// precondition
		final JaxrsWebxmlApplication webxmlApplication = createWebxmlApplication(
				"org.jboss.tools.ws.jaxrs.sample.services.RestApplication", "/hello");
		final JaxrsJavaApplication javaApplication = createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		assertThat(javaApplication.isOverriden(), equalTo(true));
		// operation
		final ResourceDelta event = createFullResourceDelta(webxmlApplication.getResource(), REMOVED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(2));
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(affectedElements.get(0).getElement().getElementKind(), equalTo(EnumElementKind.APPLICATION_WEBXML));
		assertThat(affectedElements.get(1).getDeltaKind(), equalTo(CHANGED));
		assertThat(affectedElements.get(1).getElement().getElementKind(), equalTo(EnumElementKind.APPLICATION_JAVA));
		final JaxrsJavaApplication application = (JaxrsJavaApplication) affectedElements.get(1).getElement();
		assertThat(application.getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(application.getApplicationPath(), equalTo("/app"));
		// one (java)
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1)); 
	}

	@Test
	public void shouldUnoverrideUnannotatedJavaApplicationWhenRemovingCustomWebxml() throws Exception {
		// precondition
		final JaxrsWebxmlApplication webxmlApplication = createWebxmlApplication(
				"org.jboss.tools.ws.jaxrs.sample.services.RestApplication", "/hello");
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.RestApplication", javaProject,
				"@ApplicationPath(\"/app\")", "", false);
		final JaxrsJavaApplication javaApplication = createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		assertThat(javaApplication.isOverriden(), equalTo(true));
		// operation
		final ResourceDelta event = createFullResourceDelta(webxmlApplication.getResource(), REMOVED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(2));
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(affectedElements.get(0).getElement().getElementKind(), equalTo(EnumElementKind.APPLICATION_WEBXML));
		assertThat(affectedElements.get(1).getDeltaKind(), equalTo(CHANGED));
		assertThat(affectedElements.get(1).getElement().getElementKind(), equalTo(EnumElementKind.APPLICATION_JAVA));
		final JaxrsJavaApplication application = (JaxrsJavaApplication) affectedElements.get(1).getElement();
		assertThat(application.getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(application.getApplicationPath(), nullValue());
		// one (java)
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1)); 
	}

	@Test
	public void shouldNotOverrideJavaApplicationWhenAddingDefaultServletMapping() throws Exception {
		// pre-conditions
		createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		// operation
		final IResource webxmlResource = WorkbenchUtils.replaceDeploymentDescriptorWith(javaProject,
				"web-3_0-with-default-servlet-mapping.xml");
		final ResourceDelta event = createFullResourceDelta(webxmlResource, CHANGED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1));
		final JaxrsWebxmlApplication webxmlApplication = (JaxrsWebxmlApplication) affectedElements.get(0).getElement();
		assertThat(webxmlApplication.getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(webxmlApplication.getApplicationPath(), equalTo("/hello"));
		verify(metamodel, times(1)).add(any(JaxrsWebxmlApplication.class));
		// old application (java) + new one (web.xml)
		assertThat(metamodel.getElements(javaProject).size(), equalTo(2)); 
		// web.xml based application precedes any other java based JAX-RS Application element
		assertThat(metamodel.getApplication(), equalTo((IJaxrsApplication) webxmlApplication)); 
		// Java-based application configuration should not be changed
		assertThat(metamodel.getJavaApplications().get(0).getApplicationPath(), equalTo("/app")); 
	}

	/**
	 * In this test, the existing Java Application is not modified when adding a
	 * web.xml with default application configuration, but the resulting
	 * webxmlApplication becomes the primary one in the metamodel
	 * 
	 * @throws Exception
	 */
	@Test
	public void shouldPreceedJavaApplicationWhenAddingDefaultWebxmlMapping() throws Exception {
		// in this test, the java-application exists first, and then a web.xml
		// application is added -> it should
		// immediately override the java-one
		// pre-conditions
		final JaxrsJavaApplication javaApplication = createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		// operation
		final IResource webxmlResource = WorkbenchUtils.replaceDeploymentDescriptorWith(javaProject,
				"web-3_0-with-default-servlet-mapping.xml");
		final ResourceDelta event = createFullResourceDelta(webxmlResource, ADDED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1));
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(affectedElements.get(0).getElement().getElementKind(), equalTo(EnumElementKind.APPLICATION_WEBXML));
		final JaxrsWebxmlApplication webxmlApplication = (JaxrsWebxmlApplication) affectedElements.get(0).getElement();
		assertThat(webxmlApplication.getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(webxmlApplication.getApplicationPath(), equalTo("/hello"));
		// Java-based application configuration should not be changed
		assertThat(javaApplication.getApplicationPath(), equalTo("/app")); 
		// old application (java) + new one (web.xml)
		assertThat(metamodel.getElements(javaProject).size(), equalTo(2)); 
		// old application (java) + new one (web.xml)
		assertThat(metamodel.getApplication(), equalTo((IJaxrsApplication) webxmlApplication)); 
		verify(metamodel, times(1)).add(any(JaxrsWebxmlApplication.class));
	}

	@Test
	public void shouldRestoreJavaApplicationWhenRemovingDefaultWebxmlMapping() throws Exception {
		// in this test, the java-application exists first, and then a web.xml
		// application is added -> it should
		// immediately override the java-one
		// pre-conditions
		final JaxrsJavaApplication javaApplication = createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final JaxrsWebxmlApplication webxmlApplication = createWebxmlApplication(
				EnumJaxrsClassname.APPLICATION.qualifiedName, "/hello");
		// operation
		final ResourceDelta event = createFullResourceDelta(webxmlApplication.getResource(), REMOVED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1));
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(affectedElements.get(0).getElement().getElementKind(), equalTo(EnumElementKind.APPLICATION_WEBXML));
		// Java-based application configuration should not be changed
		assertThat(javaApplication.getApplicationPath(), equalTo("/app"));
		// java application
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1)); 
		// old application (java) + new one (web.xml)
		assertThat(metamodel.getApplication(), equalTo((IJaxrsApplication) javaApplication)); 
		verify(metamodel, times(1)).add(any(JaxrsWebxmlApplication.class));
	}

	@Test
	public void shouldRemoveApplicationWhenRemovingAnnotationAndHierarchyAlreadyMissing() throws CoreException {
		// pre-conditions
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.RestApplication", javaProject,
				"extends Application", "", false);
		createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication", "/bar");
		// operation
		IType type = replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.RestApplication",
				javaProject, "@ApplicationPath(\"/app\")", "", false);
		final ResourceDelta event = createFullResourceDelta(type.getResource(), CHANGED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1));
		assertThat(affectedElements.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(REMOVED));
		verify(metamodel, times(1)).remove(any(JaxrsJavaApplication.class));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(0));
	}

	@Test
	public void shouldRemoveApplicationWhenRemovingHierarchyAndAnnotationAlreadyMissing() throws CoreException {
		// pre-conditions
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.RestApplication", javaProject,
				"@ApplicationPath(\"/app\")", "", false);
		createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		// operation
		IType type = replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.RestApplication",
				javaProject, "extends Application", "", false);
		final ResourceDelta event = createFullResourceDelta(type.getResource(), CHANGED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1));
		assertThat(affectedElements.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(REMOVED));
		verify(metamodel, times(1)).remove(any(JaxrsJavaApplication.class));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(0));
	}

	/**
	 * In this test, the webxml application is changed when the application path
	 * is changed, too.
	 * 
	 * @throws Exception
	 */
	@Test
	public void shouldReplaceWebxmlApplicationWhenChangingApplicationClassName() throws Exception {
		// pre-conditions
		final IResource webxmlResource = WorkbenchUtils.replaceDeploymentDescriptorWith(javaProject,
				"web-3_0-with-default-servlet-mapping.xml");
		createWebxmlApplication("bar.foo", "/foo");
		// operation
		final ResourceDelta event = createFullResourceDelta(webxmlResource, CHANGED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(2));
		assertThat(affectedElements.get(0).getElement().getElementKind(), equalTo(EnumElementKind.APPLICATION_WEBXML));
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(affectedElements.get(1).getElement().getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(affectedElements.get(1).getElement().getElementKind(), equalTo(EnumElementKind.APPLICATION_WEBXML));
		assertThat(affectedElements.get(1).getDeltaKind(), equalTo(REMOVED));
		assertThat(((JaxrsWebxmlApplication) affectedElements.get(0).getElement()).getApplicationPath(),
				equalTo("/hello"));
		// initial app added + new app added, too
		verify(metamodel, times(2)).add(any(JaxrsWebxmlApplication.class)); 
		// initial app removed
		verify(metamodel, times(1)).remove(any(JaxrsWebxmlApplication.class)); 
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	/**
	 * In this test, the webxml application is changed when the application path
	 * is changed, too.
	 * 
	 * @throws Exception
	 */
	@Test
	public void shouldChangeWebxmlApplicationWhenChangingApplicationPathValue() throws Exception {
		// pre-conditions
		final IResource webxmlResource = WorkbenchUtils.replaceDeploymentDescriptorWith(javaProject,
				"web-3_0-with-default-servlet-mapping.xml");
		createWebxmlApplication(EnumJaxrsClassname.APPLICATION.qualifiedName, "/foo");
		// operation
		final ResourceDelta event = createFullResourceDelta(webxmlResource, CHANGED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1));
		assertThat(affectedElements.get(0).getElement().getElementKind(), equalTo(EnumElementKind.APPLICATION_WEBXML));
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(((JaxrsWebxmlApplication) affectedElements.get(0).getElement()).getApplicationPath(),
				equalTo("/hello"));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldNotFailWhenWebxmlWithUnknownServletClass() throws Exception {
		// pre-conditions
		List<IPackageFragmentRoot> removedEntries = WorkbenchUtils.removeClasspathEntry(javaProject,
				"jaxrs-api-2.0.1.GA.jar", null);
		assertFalse(removedEntries.isEmpty());
		final IResource webxmlResource = WorkbenchUtils.replaceDeploymentDescriptorWith(javaProject,
				"web-3_0-with-invalid-servlet-mapping.xml");
		// metamodel.add(createApplication("/foo"));
		// operation
		// operation
		final ResourceDelta event = createFullResourceDelta(webxmlResource, CHANGED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(0));
	}

	@Test
	public void shouldRemoveWebxmlApplicationWhenChangingWebxml() throws Exception {
		// pre-conditions
		createWebxmlApplication(EnumJaxrsClassname.APPLICATION.qualifiedName, "/hello");
		final IResource webxmlResource = WorkbenchUtils.replaceDeploymentDescriptorWith(javaProject,
				"web-3_0-without-servlet-mapping.xml");
		// operation
		final ResourceDelta event = createFullResourceDelta(webxmlResource, CHANGED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1));
		assertThat(affectedElements.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((JaxrsWebxmlApplication) affectedElements.get(0).getElement()).getApplicationPath(),
				equalTo("/hello"));
		verify(metamodel, times(1)).remove(any(JaxrsWebxmlApplication.class));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(0));
	}

	@Test
	public void shouldRemoveWebxmlApplicationWhenRemovingWebxml() throws Exception {
		// pre-conditions
		// JaxrsMetamodel metamodel = new JaxrsMetamodel(javaProject);
		final JaxrsWebxmlApplication application = createWebxmlApplication(
				EnumJaxrsClassname.APPLICATION.qualifiedName, "/hello");
		final IResource webxmlResource = WorkbenchUtils.replaceDeploymentDescriptorWith(javaProject,
				"web-3_0-with-default-servlet-mapping.xml");
		// operation
		webxmlResource.delete(true, progressMonitor);
		final ResourceDelta event = createFullResourceDelta(webxmlResource, REMOVED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1));
		assertThat(affectedElements.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((JaxrsWebxmlApplication) affectedElements.get(0).getElement()), equalTo(application));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(0));
	}

	@Test
	@Ignore()
	public void shouldRemoveWebxmlApplicationWhenRemovingWebInfFolder() throws Exception {
		// pre-conditions
		final IResource webxmlResource = WorkbenchUtils.replaceDeploymentDescriptorWith(javaProject,
				"web-3_0-with-servlet-mapping.xml");
		createWebxmlApplication(EnumJaxrsClassname.APPLICATION.qualifiedName, "/hello");
		// operation
		final IContainer webInfFolder = webxmlResource.getParent();
		webInfFolder.delete(IResource.FORCE, progressMonitor);
		final ResourceDelta event = createFullResourceDelta(webInfFolder, REMOVED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1));
		assertThat(affectedElements.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(affectedElements.get(0).getElement(), is(notNullValue()));
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(0));
	}

	@Test
	public void shouldNotRemoveHttpMethodWhenRemovingBinaryLib() throws CoreException {
		// pre-conditions
		final IPackageFragmentRoot lib = getPackageFragmentRoot("lib/jaxrs-api-2.0.1.GA.jar");
		// let's suppose that this jar only contains 1 HTTP Methods ;-)
		final IType type = resolveType("javax.ws.rs.GET");
		final Map<String, Annotation> annotations = resolveAnnotations(type, HTTP_METHOD.qualifiedName);
		final JaxrsHttpMethod httpMethod = new JaxrsHttpMethod(type, annotations, metamodel);
		metamodel.add(httpMethod);
		// operation
		final ResourceDelta event = createFullResourceDelta(lib.getResource(), REMOVED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(0));
	}

	@Test
	public void shouldAddHttpMethodWhenChangingResource() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final Annotation annotation = resolveAnnotation(type, HTTP_METHOD.qualifiedName);
		// operation
		final ResourceDelta event = createFullResourceDelta(annotation.getJavaParent().getResource(), CHANGED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1));
		assertThat(affectedElements.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.HTTP_METHOD));
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((IJaxrsHttpMethod) affectedElements.get(0).getElement()).getHttpVerb(), equalTo("FOO"));
		verify(metamodel, times(1)).add(any(JaxrsHttpMethod.class));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldChangeHttpMethodWhenChangingResource() throws CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = createHttpMethod("org.jboss.tools.ws.jaxrs.sample.services.FOO", "bar");
		// operation
		final ResourceDelta event = createFullResourceDelta(httpMethod.getResource(), CHANGED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1));
		assertThat(affectedElements.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.HTTP_METHOD));
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(((IJaxrsHttpMethod) affectedElements.get(0).getElement()).getHttpVerb(), equalTo("FOO"));
		verify(metamodel, times(1)).add(any(JaxrsHttpMethod.class));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
		assertThat(httpMethod.getHttpVerb(), equalTo("FOO"));
	}

	@Test
	public void shouldChangeHttpMethodWhenRemovingTargetAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = createHttpMethod("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final Annotation annotation = httpMethod.getAnnotation(TARGET.qualifiedName);
		// operation
		WorkbenchUtils.delete(annotation.getJavaAnnotation(), false);
		final ResourceDelta event = createFullResourceDelta(annotation.getJavaParent().getResource(), CHANGED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1));
		assertThat(affectedElements.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.HTTP_METHOD));
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(((IJaxrsHttpMethod) affectedElements.get(0).getElement()).getHttpVerb(), equalTo("FOO"));
		verify(metamodel, times(1)).add(any(JaxrsHttpMethod.class));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
		assertNull(httpMethod.getAnnotations().get(TARGET.qualifiedName));
	}

	@Test
	public void shouldNotChangeHttpMethodWhenAddingDeprecatedAnnotation() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final JaxrsHttpMethod httpMethod = createHttpMethod(type);
		metamodel.add(httpMethod);
		final Annotation annotation = resolveAnnotation(type, TARGET.qualifiedName);
		// operation
		WorkbenchUtils.addTypeAnnotation(type, "@Deprecated", false);
		final ResourceDelta event = createFullResourceDelta(annotation.getJavaParent().getResource(), CHANGED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(0));
	}

	@Test
	public void shouldRemoveHttpMethodWhenChangingResource() throws CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = createHttpMethod("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final Annotation annotation = httpMethod.getAnnotation(HTTP_METHOD.qualifiedName);
		// operation
		WorkbenchUtils.delete(annotation.getJavaAnnotation(), false);
		final ResourceDelta event = createFullResourceDelta(annotation.getJavaParent().getResource(), CHANGED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1));
		assertThat(affectedElements.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.HTTP_METHOD));
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((IJaxrsHttpMethod) affectedElements.get(0).getElement()).getHttpVerb(), equalTo("FOO"));
		verify(metamodel, times(1)).add(any(JaxrsHttpMethod.class));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(0));
	}

	@Test
	public void shouldRemoveHttpMethodWhenRemovingCompilationUnit() throws CoreException {
		// pre-conditions
		// JaxrsMetamodel metamodel = new JaxrsMetamodel(javaProject);
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final Map<String, Annotation> annotations = resolveAnnotations(type, HTTP_METHOD.qualifiedName);
		final JaxrsHttpMethod httpMethod = new JaxrsHttpMethod(type, annotations, metamodel);
		metamodel.add(httpMethod);
		// operation
		final ResourceDelta event = createFullResourceDelta(type.getResource(), REMOVED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1));
		assertThat(affectedElements.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.HTTP_METHOD));
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((JaxrsHttpMethod) affectedElements.get(0).getElement()), equalTo(httpMethod));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(0));
	}

	@Test
	public void shouldRemoveHttpMethodWhenRemovingSourceType() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final Map<String, Annotation> annotations = resolveAnnotations(type, HTTP_METHOD.qualifiedName);
		final JaxrsHttpMethod httpMethod = new JaxrsHttpMethod(type, annotations, metamodel);
		metamodel.add(httpMethod);
		// operation
		WorkbenchUtils.delete(type);
		final ResourceDelta event = createFullResourceDelta(type.getResource(), CHANGED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1));
		assertThat(affectedElements.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.HTTP_METHOD));
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((IJaxrsHttpMethod) affectedElements.get(0).getElement()).getHttpVerb(), equalTo("FOO"));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(0));
	}

	@Test
	public void shouldRemoveHttpMethodWhenRemovingSourceFolder() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final Map<String, Annotation> annotations = resolveAnnotations(type, HTTP_METHOD.qualifiedName);
		final JaxrsHttpMethod httpMethod = new JaxrsHttpMethod(type, annotations, metamodel);
		metamodel.add(httpMethod);
		final IPackageFragmentRoot sourceFolder = getPackageFragmentRoot("src/main/java");
		// operation
		final ResourceDelta event = createFullResourceDelta(sourceFolder.getResource(), REMOVED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1));
		assertThat(affectedElements.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.HTTP_METHOD));
		assertThat(affectedElements.get(0).getElement(), is(notNullValue()));
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(REMOVED));
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
		final ResourceDelta event = createFullResourceDelta(type.getResource(), ADDED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1)); // 1 resource
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(affectedElements.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.RESOURCE));
		// HttpMethods, Resource, ResourceMethods and ResourceFields
		assertThat(metamodel.getElements(javaProject).size(), equalTo(11));
	}

	@Test
	public void shouldAddResourceWhenChangingResource() throws CoreException {
		// pre-conditions
		createHttpMethod(GET);
		createHttpMethod(POST);
		createHttpMethod(PUT);
		createHttpMethod(DELETE);
		metamodel.add(createFullResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource"));
		final IType customerType = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		// operation
		final ResourceDelta event = createFullResourceDelta(customerType.getResource(), CHANGED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1)); // 1 resource
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(ADDED));
		// 4 HttpMethods + 2 resources (including their methods and fields)
		assertThat(metamodel.getElements(javaProject).size(), equalTo(15));
	}

	@Test
	public void shouldChangeExistingResourceWhenChangingResource() throws CoreException {
		// pre-conditions
		createHttpMethod(GET);
		createHttpMethod(POST);
		createHttpMethod(PUT);
		createHttpMethod(DELETE);
		metamodel.add(createFullResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource"));
		final JaxrsResource customerResource = createFullResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		customerResource.removeAnnotation(customerResource.getProducesAnnotation());
		// operation
		final ResourceDelta event = createFullResourceDelta(customerResource.getJavaElement().getResource(), CHANGED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1)); // 1 resource
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(((JaxrsResource) affectedElements.get(0).getElement()), equalTo(customerResource));
		// 4 HttpMethods + 2 resources (including their methods and fields)
		assertThat(metamodel.getElements(javaProject).size(), equalTo(15));
	}

	@Test
	public void shouldAddResourceMethodWhenChangingResource() throws CoreException {
		// pre-conditions
		createHttpMethod(GET);
		createHttpMethod(POST);
		createHttpMethod(PUT);
		createHttpMethod(DELETE);
		final JaxrsResource bookResource = createFullResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		bookResource.removeMethod(bookResource.getAllMethods().get(0));
		// operation
		final ResourceDelta event = createFullResourceDelta(bookResource.getJavaElement().getResource(), CHANGED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1)); // 1 resource method
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(affectedElements.get(0).getElement().getElementCategory(),
				equalTo(EnumElementCategory.RESOURCE_METHOD));
		// 4 HttpMethods + 2 resources (including their methods and fields)
		assertThat(metamodel.getElements(javaProject).size(), equalTo(8));
	}

	@Test
	public void shouldChangeResourceMethodWhenChangingResource() throws CoreException {
		// pre-conditions
		createHttpMethod(GET);
		createHttpMethod(POST);
		createHttpMethod(PUT);
		createHttpMethod(DELETE);
		final JaxrsResource bookResource = createFullResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		// operation
		// operation
		for (Iterator<JaxrsResourceMethod> iterator = bookResource.getMethods().values().iterator(); iterator.hasNext();) {
			JaxrsResourceMethod resourceMethod = iterator.next();
			if (resourceMethod.getElementKind() == EnumElementKind.SUBRESOURCE_METHOD) {
				replaceFirstOccurrenceOfCode(resourceMethod.getJavaElement(), "@Path(\"/{id}\")", "@Path(\"/{foo}\")",
						false);
				WorkbenchUtils.delete(resourceMethod.getHttpMethodAnnotation().getJavaAnnotation(), false);
			}
		}
		final ResourceDelta event = createFullResourceDelta(bookResource.getJavaElement().getResource(), CHANGED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(2)); // 2 resource methods
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(affectedElements.get(0).getElement().getElementCategory(),
				equalTo(EnumElementCategory.RESOURCE_METHOD));
		assertThat(affectedElements.get(1).getDeltaKind(), equalTo(CHANGED));
		assertThat(affectedElements.get(1).getElement().getElementCategory(),
				equalTo(EnumElementCategory.RESOURCE_METHOD));
		// 4 HttpMethods + 2 resources (including their methods and fields)
		assertThat(metamodel.getElements(javaProject).size(), equalTo(8));
	}

	@Test
	public void shouldRemoveResourceMethodWhenChangingResource() throws CoreException {
		// pre-conditions
		createHttpMethod(GET);
		createHttpMethod(POST);
		createHttpMethod(PUT);
		createHttpMethod(DELETE);
		final JaxrsResource bookResource = createFullResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		// operation
		for (Iterator<JaxrsResourceMethod> iterator = bookResource.getMethods().values().iterator(); iterator.hasNext();) {
			JaxrsResourceMethod resourceMethod = iterator.next();
			if (resourceMethod.getElementKind() == EnumElementKind.RESOURCE_METHOD) {
				WorkbenchUtils.delete(resourceMethod.getHttpMethodAnnotation().getJavaAnnotation(), false);
			}
		}
		final ResourceDelta event = createFullResourceDelta(bookResource.getJavaElement().getResource(), CHANGED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1)); // 1 resource method
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(affectedElements.get(0).getElement().getElementCategory(),
				equalTo(EnumElementCategory.RESOURCE_METHOD));
		// 4 HttpMethods + 1 resource (including their remaining methods and
		// fields)
		assertThat(metamodel.getElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldAddResourceFieldWhenChangingResource() throws CoreException {
		// pre-conditions
		createHttpMethod(GET);
		createHttpMethod(POST);
		createHttpMethod(PUT);
		createHttpMethod(DELETE);
		final JaxrsResource productResourceLocator = createFullResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		productResourceLocator.removeField(productResourceLocator.getAllFields().get(0));
		// operation
		final ResourceDelta event = createFullResourceDelta(productResourceLocator.getJavaElement().getResource(),
				CHANGED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1)); // 1 resource field
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(affectedElements.get(0).getElement().getElementCategory(),
				equalTo(EnumElementCategory.RESOURCE_FIELD));
		// 4 HttpMethods + 2 resources (including their methods and fields)
		assertThat(metamodel.getElements(javaProject).size(), equalTo(9));
	}

	@Test
	public void shouldChangeResourceFieldWhenChangingResource() throws CoreException {
		// pre-conditions
		createHttpMethod(GET);
		createHttpMethod(POST);
		createHttpMethod(PUT);
		createHttpMethod(DELETE);
		final JaxrsResource productResourceLocator = createFullResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		// operation
		for (Iterator<JaxrsResourceField> iterator = productResourceLocator.getFields().values().iterator(); iterator
				.hasNext();) {
			JaxrsResourceField resourceField = iterator.next();
			if (resourceField.getDefaultValueAnnotation() != null) {
				replaceFirstOccurrenceOfCode(resourceField.getJavaElement(), "@DefaultValue(\"foo!\")",
						"@DefaultValue(\"bar\")", false);
			}
		}
		final ResourceDelta event = createFullResourceDelta(productResourceLocator.getJavaElement().getResource(),
				CHANGED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1)); // 1 resource field
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(affectedElements.get(0).getElement().getElementCategory(),
				equalTo(EnumElementCategory.RESOURCE_FIELD));
		// 4 HttpMethods + 2 resources (including their methods and fields)
		assertThat(metamodel.getElements(javaProject).size(), equalTo(9));
	}

	@Test
	public void shouldRemoveResourceFieldWhenChangingResource() throws CoreException {
		// pre-conditions
		createHttpMethod(GET);
		createHttpMethod(POST);
		createHttpMethod(PUT);
		createHttpMethod(DELETE);
		final JaxrsResource productResourceLocator = createFullResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		// operation
		for (Iterator<JaxrsResourceField> iterator = productResourceLocator.getFields().values().iterator(); iterator
				.hasNext();) {
			JaxrsResourceField resourceField = iterator.next();
			if (resourceField.getQueryParamAnnotation() != null) {
				WorkbenchUtils.delete(resourceField.getQueryParamAnnotation().getJavaAnnotation(), false);
			}
		}
		final ResourceDelta event = createFullResourceDelta(productResourceLocator.getJavaElement().getResource(),
				CHANGED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1)); // 1 resource field
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(affectedElements.get(0).getElement().getElementCategory(),
				equalTo(EnumElementCategory.RESOURCE_FIELD));
		// 4 HttpMethods + 2 resources (including their methods and fields)
		assertThat(metamodel.getElements(javaProject).size(), equalTo(8));
	}

	@Test
	public void shouldRemoveExistingResourceWhenChangingResource() throws CoreException {
		// pre-conditions
		createHttpMethod(GET);
		createHttpMethod(POST);
		createHttpMethod(PUT);
		createHttpMethod(DELETE);
		final JaxrsResource resource = createFullResource("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		// operation
		for (IMethod method : resource.getJavaElement().getMethods()) {
			WorkbenchUtils.delete(method);
		}
		final ResourceDelta event = createFullResourceDelta(resource.getJavaElement().getResource(), CHANGED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1)); // 1 resource
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(affectedElements.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.RESOURCE));
		// 4 HttpMethods left only
		assertThat(metamodel.getElements(javaProject).size(), equalTo(4));
	}

	@Test
	public void shouldRemoveResourceWhenRemovingCompilationUnit() throws CoreException {
		// pre-conditions
		createHttpMethod(GET);
		createHttpMethod(POST);
		createHttpMethod(PUT);
		createHttpMethod(DELETE);
		final JaxrsResource resource = createFullResource("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		// operation
		final ResourceDelta event = createFullResourceDelta(resource.getJavaElement().getResource(), REMOVED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1));
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(affectedElements.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.RESOURCE));
		assertThat(((JaxrsResource) affectedElements.get(0).getElement()), equalTo(resource));
		// 4 HttpMethods left only
		assertThat(metamodel.getElements(javaProject).size(), equalTo(4));
	}

	@Test
	public void shouldRemoveResourceWhenRemovingSourceType() throws CoreException {
		// pre-conditions
		createHttpMethod(GET);
		createHttpMethod(POST);
		createHttpMethod(PUT);
		createHttpMethod(DELETE);
		final JaxrsResource resource = createFullResource("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		// operation
		WorkbenchUtils.delete(resource.getJavaElement());
		final ResourceDelta event = createFullResourceDelta(resource.getJavaElement().getResource(), CHANGED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1));
		assertThat(affectedElements.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.RESOURCE));
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((JaxrsResource) affectedElements.get(0).getElement()), equalTo(resource));
		// 4 HttpMethods left only
		assertThat(metamodel.getElements(javaProject).size(), equalTo(4));
	}

	@Test
	public void shouldRemoveResourceWhenRemovingSourceFolder() throws CoreException {
		// pre-conditions
		final IPackageFragmentRoot sourceFolder = getPackageFragmentRoot("src/main/java");
		createSimpleResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		// operation
		final ResourceDelta event = createFullResourceDelta(sourceFolder.getResource(), REMOVED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1));
		assertThat(affectedElements.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.RESOURCE));
		verify(metamodel, times(1)).remove(any(JaxrsResource.class));
		// nothing left
		assertThat(metamodel.getElements(javaProject).size(), equalTo(0));
	}

	@Test
	@Ignore
	public void shouldRemoveResourceWhenRemovingBinaryLib() throws CoreException {
		// need to package a JAX-RS resource into a jar...
	}

	@Test
	public void shouldRemoveResourceWhenRemovingMethodsFieldsAndAnnotations() throws CoreException {
		// pre-conditions
		createHttpMethod(GET);
		createHttpMethod(POST);
		createHttpMethod(PUT);
		createHttpMethod(DELETE);
		final JaxrsResource resourceLocator = createFullResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		// operation
		for (Iterator<JaxrsResourceMethod> iterator = resourceLocator.getMethods().values().iterator(); iterator
				.hasNext();) {
			JaxrsResourceMethod resourceMethod = iterator.next();
			WorkbenchUtils.delete(resourceMethod.getJavaElement());
		}
		for (Iterator<JaxrsResourceField> iterator = resourceLocator.getFields().values().iterator(); iterator
				.hasNext();) {
			JaxrsResourceField resourceField = iterator.next();
			WorkbenchUtils.delete(resourceField.getJavaElement());
		}
		for (Iterator<Annotation> iterator = resourceLocator.getAnnotations().values().iterator(); iterator.hasNext();) {
			Annotation annotation = iterator.next();
			WorkbenchUtils.delete(annotation.getJavaAnnotation(), false);
		}
		final ResourceDelta event = createFullResourceDelta(resourceLocator.getJavaElement().getResource(), CHANGED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1)); // 1 resource method
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(affectedElements.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.RESOURCE));
		// 4 HttpMethods left only
		assertThat(metamodel.getElements(javaProject).size(), equalTo(4));
	}

	/**
	 * Test in relation with https://issues.jboss.org/browse/JBIDE-12806
	 * 
	 * @throws CoreException
	 */
	@Test
	public void shouldUpdateTypeAnnotationLocationAfterCodeChangeAbove() throws CoreException {
		// pre-condition: using the CustomerResource type
		final JaxrsResource customerResource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		final Annotation pathAnnotation = customerResource.getAnnotation(PATH.qualifiedName);
		final ISourceRange beforeChangeSourceRange = JdtUtils.resolveMemberPairValueRange(
				pathAnnotation.getJavaAnnotation(), "value");
		final int length = beforeChangeSourceRange.getLength();
		final int offset = beforeChangeSourceRange.getOffset();
		// operation: removing @Encoded *before* the CustomerResource type
		replaceFirstOccurrenceOfCode(customerResource.getJavaElement(), "@Encoded", "", false);
		CompilationUnitsRepository.getInstance().mergeAST(customerResource.getJavaElement().getCompilationUnit(),
				JdtUtils.parse(customerResource.getJavaElement().getCompilationUnit(), null), true);
		final ResourceDelta event = createFullResourceDelta(customerResource.getJavaElement().getResource(), CHANGED);
		processResourceChanges(event, progressMonitor);
		// verifications
		final ISourceRange afterChangeSourceRange = JdtUtils.resolveMemberPairValueRange(
				pathAnnotation.getJavaAnnotation(), "value");
		assertThat(afterChangeSourceRange.getOffset(), lessThan(offset));
		assertThat(afterChangeSourceRange.getLength(), equalTo(length));
	}

	/**
	 * Test in relation with https://issues.jboss.org/browse/JBIDE-12806
	 * 
	 * @throws CoreException
	 */
	@Test
	public void shouldUpdateMethodAnnotationLocationAfterCodeChangeAbove() throws CoreException {
		// pre-condition: using the CustomerResource type
		final JaxrsResource customerResource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		final JaxrsResourceMethod resourceMethod = createResourceMethod("createCustomer", customerResource, POST.qualifiedName);
		final ISourceRange beforeChangeSourceRange = resourceMethod.getAnnotation(POST.qualifiedName).getJavaAnnotation()
				.getSourceRange();
		final int length = beforeChangeSourceRange.getLength();
		final int offset = beforeChangeSourceRange.getOffset();
		// operation: removing @Encoded *before* the createCustomer() method
		replaceFirstOccurrenceOfCode(customerResource.getJavaElement(), "@Encoded", "", false);
		CompilationUnitsRepository.getInstance().mergeAST(customerResource.getJavaElement().getCompilationUnit(),
				JdtUtils.parse(customerResource.getJavaElement().getCompilationUnit(), null), true);
		final ResourceDelta event = createFullResourceDelta(customerResource.getJavaElement().getResource(), CHANGED);
		processResourceChanges(event, progressMonitor);
		// verifications
		final ISourceRange afterChangeSourceRange = resourceMethod.getAnnotation(POST.qualifiedName).getJavaAnnotation()
				.getSourceRange();
		assertThat(afterChangeSourceRange.getOffset(), lessThan(offset));
		assertThat(afterChangeSourceRange.getLength(), equalTo(length));
	}

	/**
	 * Test in relation with https://issues.jboss.org/browse/JBIDE-12806
	 * 
	 * @throws CoreException
	 */
	@Test
	public void shouldUpdateMethodParameterAnnotationLocationAfterCodeChangeAbove() throws CoreException {
		// pre-condition: using the CustomerResource type
		final JaxrsResource customerResource = createFullResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod javaMethod = getMethod(customerResource.getJavaElement(), "getCustomer");
		final JaxrsResourceMethod resourceMethod = customerResource.getMethods().get(javaMethod.getHandleIdentifier());
		Annotation pathParamAnnotation = resourceMethod.getJavaMethodParameters().get(0).getAnnotations()
				.get(PATH_PARAM.qualifiedName);
		final ISourceRange beforeChangeSourceRange = JdtUtils.resolveMemberPairValueRange(
				pathParamAnnotation.getJavaAnnotation(), "value");
		final int beforeLength = beforeChangeSourceRange.getLength();
		final int beforeOffset = beforeChangeSourceRange.getOffset();
		// operation: removing @Encoded *before* the getCustomer() method
		replaceFirstOccurrenceOfCode(customerResource.getJavaElement(), "@Encoded", "", false);
		CompilationUnitsRepository.getInstance().mergeAST(customerResource.getJavaElement().getCompilationUnit(),
				JdtUtils.parse(customerResource.getJavaElement().getCompilationUnit(), null), true);
		final ResourceDelta event = createFullResourceDelta(customerResource.getJavaElement().getResource(), CHANGED);
		processResourceChanges(event, progressMonitor);
		// verifications
		// reference has changed (local variable)
		pathParamAnnotation = resourceMethod.getJavaMethodParameters().get(0).getAnnotations()
				.get(PATH_PARAM.qualifiedName);
		final ISourceRange afterChangeSourceRange = JdtUtils.resolveMemberPairValueRange(
				pathParamAnnotation.getJavaAnnotation(), "value");
		final int afterLength = afterChangeSourceRange.getLength();
		final int afterOffset = afterChangeSourceRange.getOffset();
		assertThat(afterOffset, lessThan(beforeOffset));
		assertThat(afterLength, equalTo(beforeLength));
	}

	/**
	 * Test in relation with https://issues.jboss.org/browse/JBIDE-12806
	 * 
	 * @throws CoreException
	 */
	@Test
	public void shouldUpdateMethodParameterAnnotationLocationAfterPreviousMethodParamRemoved() throws CoreException {
		// pre-condition: using the CustomerResource type
		final JaxrsResource customerResource = createFullResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		IMethod javaMethod = getMethod(customerResource.getJavaElement(), "getCustomer");
		JaxrsResourceMethod resourceMethod = customerResource.getMethods().get(javaMethod.getHandleIdentifier());
		Annotation contextAnnotation = resourceMethod.getJavaMethodParameters().get(1).getAnnotations()
				.get(CONTEXT.qualifiedName);
		final ISourceRange beforeChangeSourceRange = contextAnnotation.getJavaAnnotation().getSourceRange();
		final int beforeLength = beforeChangeSourceRange.getLength();
		final int beforeOffset = beforeChangeSourceRange.getOffset();
		// operation: removing "@PathParam("id") Integer id" parameter in the
		// getCustomer() method
		replaceFirstOccurrenceOfCode(customerResource.getJavaElement(), "@PathParam(\"id\") Integer id, ", "", false);
		CompilationUnitsRepository.getInstance().mergeAST(customerResource.getJavaElement().getCompilationUnit(),
				JdtUtils.parse(customerResource.getJavaElement().getCompilationUnit(), null), true);
		final ResourceDelta event = createFullResourceDelta(customerResource.getJavaElement().getResource(), CHANGED);
		processResourceChanges(event, progressMonitor);
		// verifications: java method has changed, so all references must be
		// looked-up again
		javaMethod = getMethod(customerResource.getJavaElement(), "getCustomer");
		resourceMethod = customerResource.getMethods().get(javaMethod.getHandleIdentifier());
		contextAnnotation = resourceMethod.getJavaMethodParameters().get(0).getAnnotations().get(CONTEXT.qualifiedName);
		final ISourceRange afterChangeSourceRange = contextAnnotation.getJavaAnnotation().getSourceRange();
		final int afterLength = afterChangeSourceRange.getLength();
		final int afterOffset = afterChangeSourceRange.getOffset();
		assertThat(afterOffset, lessThan(beforeOffset));
		assertThat(afterLength, equalTo(beforeLength));
	}

	/**
	 * Test in relation with https://issues.jboss.org/browse/JBIDE-12806
	 * 
	 * @throws CoreException
	 */
	@Test
	public void shouldNotUpdateTypeAnnotationLocationAfterCodeChangeBelow() throws CoreException {
		// pre-condition: using the CustomerResource type
		final JaxrsResource customerResource = createFullResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final Annotation pathAnnotation = customerResource.getAnnotation(PATH.qualifiedName);
		final ISourceRange beforeChangeSourceRange = JdtUtils.resolveMemberPairValueRange(
				pathAnnotation.getJavaAnnotation(), "value");
		final int length = beforeChangeSourceRange.getLength();
		final int offset = beforeChangeSourceRange.getOffset();
		// operation: removing @DELETE *after* the CustomerResource type
		replaceFirstOccurrenceOfCode(customerResource.getJavaElement(), "@DELETE", "", false);
		CompilationUnitsRepository.getInstance().mergeAST(customerResource.getJavaElement().getCompilationUnit(),
				JdtUtils.parse(customerResource.getJavaElement().getCompilationUnit(), null), true);
		final ResourceDelta event = createFullResourceDelta(customerResource.getJavaElement().getResource(), CHANGED);
		processResourceChanges(event, progressMonitor);
		// verifications
		final ISourceRange afterChangeSourceRange = JdtUtils.resolveMemberPairValueRange(
				pathAnnotation.getJavaAnnotation(), "value");
		assertThat(afterChangeSourceRange.getOffset(), equalTo(offset));
		assertThat(afterChangeSourceRange.getLength(), equalTo(length));
	}

	/**
	 * Test in relation with https://issues.jboss.org/browse/JBIDE-12806
	 * 
	 * @throws CoreException
	 */
	@Test
	public void shouldNotUpdateMethodAnnotationLocationAfterCodeChangeBelow() throws CoreException {
		// pre-condition: using the CustomerResource type
		final JaxrsResource customerResource = createSimpleResource(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", PATH.qualifiedName);
		final JaxrsResourceMethod resourceMethod = createResourceMethod("createCustomer", customerResource, POST.qualifiedName);
		final Annotation postAnnotation = resourceMethod.getAnnotation(POST.qualifiedName);
		final ISourceRange beforeChangeSourceRange = postAnnotation.getJavaAnnotation().getSourceRange();
		final int length = beforeChangeSourceRange.getLength();
		final int offset = beforeChangeSourceRange.getOffset();
		// operation: removing @DELETE, after the createCustomer() method
		replaceFirstOccurrenceOfCode(customerResource.getJavaElement(), "@DELETE", "", false);
		CompilationUnitsRepository.getInstance().mergeAST(customerResource.getJavaElement().getCompilationUnit(),
				JdtUtils.parse(customerResource.getJavaElement().getCompilationUnit(), null), true);
		final ResourceDelta event = createFullResourceDelta(customerResource.getJavaElement().getResource(), CHANGED);
		processResourceChanges(event, progressMonitor);
		// verifications
		final ISourceRange afterChangeSourceRange = postAnnotation.getJavaAnnotation().getSourceRange();
		assertThat(afterChangeSourceRange.getOffset(), equalTo(offset));
		assertThat(afterChangeSourceRange.getLength(), equalTo(length));
	}

	/**
	 * Test in relation with https://issues.jboss.org/browse/JBIDE-12806
	 * 
	 * @throws CoreException
	 */
	@Test
	public void shouldNotUpdateMethodParameterAnnotationLocationAfterCodeChangeBelow() throws CoreException {
		// pre-condition: using the CustomerResource type
		final JaxrsResource customerResource = createFullResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod javaMethod = getMethod(customerResource.getJavaElement(), "getCustomer");
		final JaxrsResourceMethod resourceMethod = customerResource.getMethods().get(javaMethod.getHandleIdentifier());
		final Annotation pathParamAnnotation = resourceMethod.getJavaMethodParameters().get(0).getAnnotations()
				.get(PATH_PARAM.qualifiedName);
		final ISourceRange beforeChangeSourceRange = JdtUtils.resolveMemberPairValueRange(
				pathParamAnnotation.getJavaAnnotation(), "value");
		final int length = beforeChangeSourceRange.getLength();
		final int offset = beforeChangeSourceRange.getOffset();
		// operation: removing @DELETE, *after* the getCustomer() method
		replaceFirstOccurrenceOfCode(customerResource.getJavaElement(), "@DELETE", "", false);
		CompilationUnitsRepository.getInstance().mergeAST(customerResource.getJavaElement().getCompilationUnit(),
				JdtUtils.parse(customerResource.getJavaElement().getCompilationUnit(), null), true);
		final ResourceDelta event = createFullResourceDelta(customerResource.getJavaElement().getResource(), CHANGED);
		processResourceChanges(event, progressMonitor);
		// verifications
		final ISourceRange afterChangeSourceRange = JdtUtils.resolveMemberPairValueRange(
				pathParamAnnotation.getJavaAnnotation(), "value");
		assertThat(afterChangeSourceRange.getOffset(), equalTo(offset));
		assertThat(afterChangeSourceRange.getLength(), equalTo(length));
	}
}
