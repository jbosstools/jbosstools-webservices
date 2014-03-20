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
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.jboss.tools.ws.jaxrs.core.internal.utils.HamcrestExtras.flagMatches;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils.addTypeAnnotation;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils.delete;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils.getAnnotation;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils.removeFirstOccurrenceOfCode;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils.removeTypeAnnotation;
import static org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils.delete;
import static org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils.replaceAllOccurrencesOfCode;
import static org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils.replaceContent;
import static org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils.replaceFirstOccurrenceOfCode;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_CONSUMES_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_PRODUCES_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_PROVIDER_HIERARCHY;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.APPLICATION_PATH;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.CONSUMES;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.CONTEXT;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.HTTP_METHOD;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PATH;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PATH_PARAM;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.POST;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PRODUCES;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PROVIDER;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.TARGET;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.hamcrest.Matchers;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsJavaApplication;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsProvider;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceField;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsWebxmlApplication;
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementCategory;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsApplication;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta;
import org.jboss.tools.ws.jaxrs.core.utils.Annotation;
import org.jboss.tools.ws.jaxrs.core.utils.CompilationUnitsRepository;
import org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames;
import org.jboss.tools.ws.jaxrs.core.utils.JdtUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class ResourceChangedProcessingTestCase {

	public final static int NO_FLAG = 0;

	@ClassRule
	public static WorkspaceSetupRule workspaceSetupRule = new WorkspaceSetupRule("org.jboss.tools.ws.jaxrs.tests.sampleproject");
	
	@Rule
	public JaxrsMetamodelMonitor metamodelMonitor = new JaxrsMetamodelMonitor("org.jboss.tools.ws.jaxrs.tests.sampleproject", false);
	
	private JaxrsMetamodel metamodel = null;

	private IJavaProject javaProject = null;

	@Before
	public void setup() throws CoreException {
		metamodel = metamodelMonitor.getMetamodel();
		javaProject = metamodel.getJavaProject();
	}
	
	protected ResourceDelta createResourceDelta(IResource resource, int deltaKind) {
		return new ResourceDelta(resource, deltaKind, NO_FLAG);
	}

	protected void processAffectedResources(ResourceDelta event) throws CoreException {
		metamodel.processAffectedResources(Arrays.asList(event), new NullProgressMonitor());
	}

	protected void processProject() throws CoreException {
		metamodel.processProject(new NullProgressMonitor());
	}

	@Test
	public void shouldAddApplicationHttpMethodsResourcesAndProvidersWhenAddingSourceFolderWithExistingMetamodel()
			throws CoreException {
		// pre-conditions
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final IPackageFragmentRoot sourceFolder = metamodelMonitor.resolvePackageFragmentRoot("src/main/java");
		final ResourceDelta event = createResourceDelta(sourceFolder.getResource(), ADDED);
		processAffectedResources(event);
		// verifications
		// 1 application + 1 HttpMethod + 7 Resources and their methods + 2
		// Providers
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(38));
		assertThat(metamodelMonitor.getElementChanges(), everyItem(Matchers.<JaxrsElementDelta> hasProperty("deltaKind", equalTo(ADDED))));
		// all HttpMethods, Resources, ResourceMethods and ResourceFields. only
		// application is available: the java-based
		// one found in src/main/java
		assertThat(metamodel.findElements(javaProject).size(), equalTo(44));
	}

	@Test
	public void shouldAddApplicationHttpMethodsResourcesAndProvidersWhenAddingSourceFolderWithExistingMetamodelWithReset()
			throws CoreException {
		// pre-conditions
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processProject();
		// verifications
		// 2 applications (java/webxml) + 6 built-in HttpMethods + 2 custom
		// HttpMethod + 7 Resources and their methods + 5 Providers: the whole
		// project is used to build the metamodel.
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(48));
		assertThat(metamodelMonitor.getElementChanges(), everyItem(Matchers.<JaxrsElementDelta> hasProperty("deltaKind", equalTo(ADDED))));
		// all project-specific Applications, HttpMethods, Resources,
		// ResourceMethods and ResourceFields (built-in
		// HttpMethods are not bound to a project)
		// 2 applications are available: the java-based and the web.xml since a
		// full build was performed
		assertThat(metamodel.findElements(javaProject).size(), equalTo(48));
	}

	@Test
	@Ignore("Ignoring for now: removing and creating a new metamodel should be tested in JaxrsMetamodelTestCase")
	public void shouldAddHttpMethodsAndResourcesWhenAddingSourceFolderWithoutExistingMetamodel() throws CoreException,
			IOException {
		// pre-conditions
		// remove the metamodel
		metamodel.remove();
		metamodel = JaxrsMetamodel.create(javaProject);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final IPackageFragmentRoot sourceFolder = metamodelMonitor.resolvePackageFragmentRoot("src/main/java");
		final ResourceDelta event = createResourceDelta(sourceFolder.getResource(), ADDED);
		processAffectedResources(event);
		// verifications
		// 1 application + 1 HttpMethod + 3 RootResources + 2 Subresources + 5
		// Providers: the whole project is used to build the metamodel.
		assertThat(metamodel, notNullValue());
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(14));
		// all Applications, HttpMethods, Resources, ResourceMethods and
		// ResourceFields specific to the project
		assertThat(metamodel.findElements(javaProject).size(), equalTo(41));
	}

	@Test
	public void shouldNotAddAnythingAddingBinaryLib() throws CoreException {
		// pre-conditions
		final IPackageFragmentRoot lib = metamodelMonitor.resolvePackageFragmentRoot("lib/jaxrs-api-2.0.1.GA.jar");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final ResourceDelta event = createResourceDelta(lib.getResource(), ADDED);
		processAffectedResources(event);
		// verifications: jar should not be taken into account, even if if it
		// contains matching elements...
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(0));
	}

	@Test
	public void shouldAddHttpMethodWhenAddingSourceCompilationUnit() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BAR");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final ResourceDelta event = createResourceDelta(type.getCompilationUnit().getResource(), ADDED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.HTTP_METHOD));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(ADDED));
		// 6 built-in HTTP Methods + 1 custom one
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldAddJavaApplicationWhenChangingResource() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final Annotation annotation = getAnnotation(type, APPLICATION_PATH);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final ResourceDelta event = createResourceDelta(annotation.getJavaParent().getResource(), CHANGED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((IJaxrsApplication) metamodelMonitor.getElementChanges().get(0).getElement()).getApplicationPath(), equalTo("/app"));
		// 6 built-in HTTP Methods + 1 app
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldChangeJavaApplicationWhenChangingResource() throws CoreException {
		// pre-conditions
		final JaxrsJavaApplication javaApplication = metamodelMonitor.createJavaApplication(
				"org.jboss.tools.ws.jaxrs.sample.services.RestApplication", "/bar");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final ResourceDelta event = createResourceDelta(javaApplication.getResource(), CHANGED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(((IJaxrsApplication) metamodelMonitor.getElementChanges().get(0).getElement()).getApplicationPath(), equalTo("/app"));
		// 6 built-in HTTP Methods + 1 app
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldChangeJavaApplicationWhenRemovingAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsJavaApplication javaApplication = metamodelMonitor.createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final Annotation annotation = javaApplication.getAnnotation(APPLICATION_PATH);
		delete(annotation.getJavaAnnotation(), false);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final ResourceDelta event = createResourceDelta(annotation.getJavaParent().getResource(), CHANGED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement(), instanceOf(JaxrsJavaApplication.class));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(((IJaxrsApplication) metamodelMonitor.getElementChanges().get(0).getElement()).getApplicationPath(), nullValue());
		// 6 built-in HTTP Methods + 1 app
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldChangeJavaApplicationWhenRemovingSupertype() throws CoreException {
		// pre-conditions
		final JaxrsJavaApplication javaApplication = metamodelMonitor.createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		replaceAllOccurrencesOfCode(javaApplication.getJavaElement().getCompilationUnit(),
				"extends Application", "", false);
		final ResourceDelta event = createResourceDelta(javaApplication.getResource(), CHANGED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement(), instanceOf(JaxrsJavaApplication.class));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(((IJaxrsApplication) metamodelMonitor.getElementChanges().get(0).getElement()).getApplicationPath(), equalTo("/app"));
		// 6 built-in HTTP Methods + 1 app
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldRemoveJavaApplicationWhenRemovingCompilationUnit() throws CoreException {
		// pre-conditions
		final JaxrsJavaApplication javaApplication = metamodelMonitor.createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final ResourceDelta event = createResourceDelta(javaApplication.getResource(), REMOVED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((JaxrsJavaApplication) metamodelMonitor.getElementChanges().get(0).getElement()), equalTo(javaApplication));
		// 6 built-in HTTP Methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldRemoveJavaApplicationWhenRemovingSourceType() throws CoreException {
		// pre-conditions
		final JaxrsJavaApplication javaApplication = metamodelMonitor.createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		delete(javaApplication.getJavaElement());
		final ResourceDelta event = createResourceDelta(javaApplication.getResource(), CHANGED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		// 6 built-in HTTP Methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldRemoveJavaApplicationWhenRemovingSourceFolder() throws CoreException {
		// pre-conditions
		metamodelMonitor.createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final IPackageFragmentRoot sourceFolder = metamodelMonitor.resolvePackageFragmentRoot("src/main/java");
		final ResourceDelta event = createResourceDelta(sourceFolder.getResource(), REMOVED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement(), is(notNullValue()));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		// 6 built-in HTTP Methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldAddWebxmlApplicationWhenAddingWebxml() throws Exception {
		// pre-conditions
		final IResource webxmlResource = metamodelMonitor.replaceDeploymentDescriptorWith(
				"web-3_0-with-default-servlet-mapping.xml");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final ResourceDelta event = createResourceDelta(webxmlResource, ADDED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((JaxrsWebxmlApplication) metamodelMonitor.getElementChanges().get(0).getElement()).getApplicationPath(),
				equalTo("/hello"));
		// 6 built-in HTTP Methods + 1 app
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldNotAddWebxmlApplicationWhenAddingEmptyWebxml() throws Exception {
		// pre-conditions
		final IResource webxmlResource = metamodelMonitor.replaceDeploymentDescriptorWith(
				"web-3_0-without-servlet-mapping.xml");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final ResourceDelta event = createResourceDelta(webxmlResource, ADDED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(0));
		// 6 built-in HTTP Methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldAddWebxmlApplicationWhenChangingWebxml() throws Exception {
		// pre-conditions
		final IResource webxmlResource = metamodelMonitor.replaceDeploymentDescriptorWith(
				"web-3_0-with-default-servlet-mapping.xml");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final ResourceDelta event = createResourceDelta(webxmlResource, CHANGED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(((IJaxrsApplication) metamodelMonitor.getElementChanges().get(0).getElement()).isWebXmlApplication(), equalTo(true));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((JaxrsWebxmlApplication) metamodelMonitor.getElementChanges().get(0).getElement()).getApplicationPath(),
				equalTo("/hello"));
		// 6 built-in HTTP Methods + 1 app
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldOverrideJavaApplicationWhenAddingCustomServletMapping() throws Exception {
		// in this test, the java-application exists first, and then a web.xml
		// application is added -> it should immediately override the java-one
		// pre-conditions
		metamodelMonitor.createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final IResource webxmlResource = metamodelMonitor.replaceDeploymentDescriptorWith(
				"web-3_0-with-custom-servlet-mapping.xml");
		final ResourceDelta event = createResourceDelta(webxmlResource, ADDED);
		processAffectedResources(event);
		// verifications: the Web Application is created and the Java
		// Application is impacted
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(2));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement(), instanceOf(JaxrsWebxmlApplication.class));
		final JaxrsWebxmlApplication webxmlApplication = (JaxrsWebxmlApplication) metamodelMonitor.getElementChanges().get(0).getElement();
		assertThat(webxmlApplication.getElementKind().getCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(webxmlApplication.getApplicationPath(), equalTo("/hello"));
		// custom web.xml override DOES override the java based JAX-RS
		// Application element
		assertThat(metamodel.findApplication().getApplicationPath(), equalTo("/hello"));
		// 6 built-in HTTP Methods + 2 apps (java + xml)
		assertThat(metamodel.findElements(javaProject).size(), equalTo(8));
	}

	/**
	 * in this test, the webxml exists first, and then an annotated Java
	 * Application is added -> it should be immediately overriden
	 */
	@Test
	public void shouldOverrideJavaApplicationWhenAddingAnnotatedJavaApplication() throws Exception {
		// precondition
		metamodelMonitor.createWebxmlApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication", "/hello");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final ResourceDelta event = createResourceDelta(type.getResource(), ADDED);
		processAffectedResources(event);
		// verifications: the JAVA Application is the sole element to be really
		// changed
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement(), instanceOf(JaxrsJavaApplication.class));
		final JaxrsJavaApplication javaApplication = (JaxrsJavaApplication) metamodelMonitor.getElementChanges().get(0).getElement();
		assertThat(javaApplication.getElementKind().getCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(javaApplication.getApplicationPath(), equalTo("/hello"));
		// 6 built-in HTTP Methods + 2 apps (java + xml)
		assertThat(metamodel.findElements(javaProject).size(), equalTo(8));
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
		metamodelMonitor.createWebxmlApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication", "/hello");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		IType type = replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.RestApplication",
				javaProject, "@ApplicationPath(\"/app\")", "", false);
		final ResourceDelta event = createResourceDelta(type.getResource(), ADDED);
		processAffectedResources(event);
		// verifications: the JAVA Application is the sole element to be really
		// changed
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement(), instanceOf(JaxrsJavaApplication.class));
		final JaxrsJavaApplication javaApplication = (JaxrsJavaApplication) metamodelMonitor.getElementChanges().get(0).getElement();
		assertThat(javaApplication.getElementKind().getCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(javaApplication.getApplicationPath(), equalTo("/hello"));
		// 6 built-in HTTP Methods + 2 apps (java + xml)
		assertThat(metamodel.findElements(javaProject).size(), equalTo(8));
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
		final JaxrsWebxmlApplication webxmlApplication = metamodelMonitor.createWebxmlApplication(
				"org.jboss.tools.ws.jaxrs.sample.services.RestApplication", "/hello");
		final JaxrsJavaApplication javaApplication = metamodelMonitor.createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		assertThat(javaApplication.isOverriden(), equalTo(true));
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final ResourceDelta event = createResourceDelta(webxmlApplication.getResource(), REMOVED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(2));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement(), instanceOf(JaxrsJavaApplication.class));
		assertThat(metamodelMonitor.getElementChanges().get(1).getDeltaKind(), equalTo(REMOVED));
		assertThat(metamodelMonitor.getElementChanges().get(1).getElement(), instanceOf(JaxrsWebxmlApplication.class));
		final JaxrsJavaApplication application = (JaxrsJavaApplication) metamodelMonitor.getElementChanges().get(0).getElement();
		assertThat(application.getElementKind().getCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(application.getApplicationPath(), equalTo("/app"));
		// 6 built-in HTTP Methods + 1 app (java)
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldUnoverrideUnannotatedJavaApplicationWhenRemovingCustomWebxml() throws Exception {
		// precondition
		final JaxrsWebxmlApplication webxmlApplication = metamodelMonitor.createWebxmlApplication(
				"org.jboss.tools.ws.jaxrs.sample.services.RestApplication", "/hello");
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.RestApplication", javaProject,
				"@ApplicationPath(\"/app\")", "", false);
		final JaxrsJavaApplication javaApplication = metamodelMonitor.createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		assertThat(javaApplication.isOverriden(), equalTo(true));
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final ResourceDelta event = createResourceDelta(webxmlApplication.getResource(), REMOVED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(2));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement(), instanceOf(JaxrsJavaApplication.class));
		assertThat(metamodelMonitor.getElementChanges().get(1).getDeltaKind(), equalTo(REMOVED));
		assertThat(metamodelMonitor.getElementChanges().get(1).getElement(), instanceOf(JaxrsWebxmlApplication.class));
		final JaxrsJavaApplication application = (JaxrsJavaApplication) metamodelMonitor.getElementChanges().get(0).getElement();
		assertThat(application.getElementKind().getCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(application.getApplicationPath(), nullValue());
		// 6 built-in HTTP Methods + 1 app (java)
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldNotOverrideJavaApplicationWhenAddingDefaultServletMapping() throws Exception {
		// pre-conditions
		metamodelMonitor.createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final IResource webxmlResource = metamodelMonitor.replaceDeploymentDescriptorWith(
				"web-3_0-with-default-servlet-mapping.xml");
		final ResourceDelta event = createResourceDelta(webxmlResource, CHANGED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		final JaxrsWebxmlApplication webxmlApplication = (JaxrsWebxmlApplication) metamodelMonitor.getElementChanges().get(0).getElement();
		assertThat(webxmlApplication.getElementKind().getCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(webxmlApplication.getApplicationPath(), equalTo("/hello"));
		// web.xml based application precedes any other java based JAX-RS
		// Application element
		assertThat(metamodel.findApplication(), equalTo((IJaxrsApplication) webxmlApplication));
		// Java-based application configuration should not be changed
		assertThat(metamodel.findJavaApplications().get(0).getApplicationPath(), equalTo("/app"));
		// 6 built-in HTTP Methods + 2 apps (java + xml)
		assertThat(metamodel.findElements(javaProject).size(), equalTo(8));
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
		final JaxrsJavaApplication javaApplication = metamodelMonitor.createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final IResource webxmlResource = metamodelMonitor.replaceDeploymentDescriptorWith(
				"web-3_0-with-default-servlet-mapping.xml");
		final ResourceDelta event = createResourceDelta(webxmlResource, ADDED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((IJaxrsApplication)metamodelMonitor.getElementChanges().get(0).getElement()).isWebXmlApplication(), equalTo(true));
		final JaxrsWebxmlApplication webxmlApplication = (JaxrsWebxmlApplication) metamodelMonitor.getElementChanges().get(0).getElement();
		assertThat(webxmlApplication.getElementKind().getCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(webxmlApplication.getApplicationPath(), equalTo("/hello"));
		// Java-based application configuration should not be changed
		assertThat(javaApplication.getApplicationPath(), equalTo("/app"));
		assertThat(metamodel.findApplication(), equalTo((IJaxrsApplication) webxmlApplication));
		// 6 built-in HTTP Methods + 2 apps (java + xml)
		assertThat(metamodel.findElements(javaProject).size(), equalTo(8));
	}

	@Test
	public void shouldRestoreJavaApplicationWhenRemovingDefaultWebxmlMapping() throws Exception {
		// in this test, the java-application exists first, and then a web.xml
		// application is added -> it should
		// immediately override the java-one
		// pre-conditions
		final JaxrsJavaApplication javaApplication = metamodelMonitor.createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final JaxrsWebxmlApplication webxmlApplication = metamodelMonitor.createWebxmlApplication(
				JaxrsClassnames.APPLICATION, "/hello");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final ResourceDelta event = createResourceDelta(webxmlApplication.getResource(), REMOVED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((IJaxrsApplication)metamodelMonitor.getElementChanges().get(0).getElement()).isWebXmlApplication(), equalTo(true));
		// Java-based application configuration should not be changed
		assertThat(javaApplication.getApplicationPath(), equalTo("/app"));
		assertThat(metamodel.findApplication(), equalTo((IJaxrsApplication) javaApplication));
		// 6 built-in HTTP Methods + 1 app (java)
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldRemoveApplicationWhenRemovingAnnotationAndHierarchyAlreadyMissing() throws CoreException {
		// pre-conditions
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.RestApplication", javaProject,
				"extends Application", "", false);
		metamodelMonitor.createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication", "/bar");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		IType type = replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.RestApplication",
				javaProject, "@ApplicationPath(\"/app\")", "", false);
		final ResourceDelta event = createResourceDelta(type.getResource(), CHANGED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		// 6 built-in HTTP Methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldRemoveApplicationWhenRemovingHierarchyAndAnnotationAlreadyMissing() throws CoreException {
		// pre-conditions
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.RestApplication", javaProject,
				"@ApplicationPath(\"/app\")", "", false);
		metamodelMonitor.createJavaApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		IType type = replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.RestApplication",
				javaProject, "extends Application", "", false);
		final ResourceDelta event = createResourceDelta(type.getResource(), CHANGED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		// 6 built-in HTTP Methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldChangeWebxmlApplicationWhenChangingApplicationClassName() throws Exception {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("RestApplication2.txt", "org.jboss.tools.ws.jaxrs.sample.services", "RestApplication2.java");
		final IResource webxmlResource = metamodelMonitor.replaceDeploymentDescriptorWith(
				"web-3_0-with-default-servlet-mapping.xml");
		metamodelMonitor.createWebxmlApplication("org.jboss.tools.ws.jaxrs.sample.services.RestApplication2", "/hello/*");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		replaceContent(webxmlResource, "org.jboss.tools.ws.jaxrs.sample.services.RestApplication2", "org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		final ResourceDelta event = createResourceDelta(webxmlResource, CHANGED);
		processAffectedResources(event);
		// verifications: 1 webxml app added and the old one (with
		// "org..RestApplication"
		// classname) removed.
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		final IJaxrsApplication app = (IJaxrsApplication) metamodelMonitor.getElementChanges().get(0).getElement();
		assertThat(app.isWebXmlApplication(), equalTo(true));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(app.getApplicationPath(), equalTo("/hello"));
		// 6 built-in HTTP Methods + 1 app (java)
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
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
		final IResource webxmlResource = metamodelMonitor.replaceDeploymentDescriptorWith(
				"web-3_0-with-default-servlet-mapping.xml");
		metamodelMonitor.createWebxmlApplication(JaxrsClassnames.APPLICATION, "/foo");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		metamodelMonitor.replaceDeploymentDescriptorWith( "web-3_0-with-default-servlet-mapping.xml");
		final ResourceDelta event = createResourceDelta(webxmlResource, CHANGED);
		processAffectedResources(event);
		// verifications
		final IJaxrsApplication app = (IJaxrsApplication) metamodelMonitor.getElementChanges().get(0).getElement();
		assertThat(app.isWebXmlApplication(), equalTo(true));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(app.getApplicationPath(), equalTo("/hello"));

		// 6 built-in HTTP Methods + 1 app (java)
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldNotFailWhenWebxmlWithUnknownServletClass() throws Exception {
		// pre-conditions
		List<IPackageFragmentRoot> removedEntries = metamodelMonitor.removeClasspathEntry(
				"jaxrs-api-2.0.1.GA.jar");
		assertFalse(removedEntries.isEmpty());
		final IResource webxmlResource = metamodelMonitor.replaceDeploymentDescriptorWith(
				"web-3_0-with-invalid-servlet-mapping.xml");
		// //metamodel.add(createApplication("/foo"));
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final ResourceDelta event = createResourceDelta(webxmlResource, CHANGED);
		processAffectedResources(event);
		// verifications
		// 6 built-in HTTP Methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldRemoveWebxmlApplicationWhenChangingWebxml() throws Exception {
		// pre-conditions
		metamodelMonitor.createWebxmlApplication(JaxrsClassnames.APPLICATION, "/hello");
		final IResource webxmlResource = metamodelMonitor.replaceDeploymentDescriptorWith(
				"web-3_0-without-servlet-mapping.xml");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final ResourceDelta event = createResourceDelta(webxmlResource, CHANGED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((JaxrsWebxmlApplication) metamodelMonitor.getElementChanges().get(0).getElement()).getApplicationPath(),
				equalTo("/hello"));
		// 6 built-in HTTP Methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldRemoveWebxmlApplicationWhenRemovingWebxml() throws Exception {
		// pre-conditions
		// JaxrsMetamodel metamodel = new JaxrsMetamodel(javaProject);
		final JaxrsWebxmlApplication application = metamodelMonitor.createWebxmlApplication(
				JaxrsClassnames.APPLICATION, "/hello");
		final IResource webxmlResource = metamodelMonitor.replaceDeploymentDescriptorWith(
				"web-3_0-with-default-servlet-mapping.xml");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		webxmlResource.delete(true, new NullProgressMonitor());
		final ResourceDelta event = createResourceDelta(webxmlResource, REMOVED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((JaxrsWebxmlApplication) metamodelMonitor.getElementChanges().get(0).getElement()), equalTo(application));
		// 6 built-in HTTP Methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	@Ignore()
	public void shouldRemoveWebxmlApplicationWhenRemovingWebInfFolder() throws Exception {
		// pre-conditions
		final IResource webxmlResource = metamodelMonitor.replaceDeploymentDescriptorWith(
				"web-3_0-with-servlet-mapping.xml");
		metamodelMonitor.createWebxmlApplication(JaxrsClassnames.APPLICATION, "/hello");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final IContainer webInfFolder = webxmlResource.getParent();
		webInfFolder.delete(IResource.FORCE, new NullProgressMonitor());
		final ResourceDelta event = createResourceDelta(webInfFolder, REMOVED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement(), is(notNullValue()));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		// 6 built-in HTTP Methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldNotRemoveHttpMethodWhenRemovingBinaryLib() throws CoreException {
		// pre-conditions
		// this jar also contains the 6 built-in HTTP Method, but its removal
		// should have no effect
		final IPackageFragmentRoot lib = metamodelMonitor.resolvePackageFragmentRoot("lib/jaxrs-api-2.0.1.GA.jar");
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final ResourceDelta event = createResourceDelta(lib.getResource(), REMOVED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(0));
		// 6 built-in HTTP Methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldAddHttpMethodWhenChangingResource() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BAR");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processAffectedResources(createResourceDelta(type.getResource(), CHANGED));
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.HTTP_METHOD));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((IJaxrsHttpMethod) metamodelMonitor.getElementChanges().get(0).getElement()).getHttpVerb(), equalTo("BAR"));
		// 6 built-in HTTP Methods + 1 custom one
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldChangeHttpMethodWhenChangingResource() throws CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = metamodelMonitor.createHttpMethod("org.jboss.tools.ws.jaxrs.sample.services.BAR", "bar");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		processAffectedResources(createResourceDelta(httpMethod.getResource(), CHANGED));
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.HTTP_METHOD));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(((IJaxrsHttpMethod) metamodelMonitor.getElementChanges().get(0).getElement()).getHttpVerb(), equalTo("BAR"));
		assertThat(httpMethod.getHttpVerb(), equalTo("BAR"));
		// 6 built-in HTTP Methods + 1 custom one
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldChangeHttpMethodWhenRemovingTargetAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = metamodelMonitor.createHttpMethod("org.jboss.tools.ws.jaxrs.sample.services.BAR");
		final Annotation annotation = httpMethod.getAnnotation(TARGET);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		delete(annotation.getJavaAnnotation(), false);
		final ResourceDelta event = createResourceDelta(annotation.getJavaParent().getResource(), CHANGED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.HTTP_METHOD));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(((IJaxrsHttpMethod) metamodelMonitor.getElementChanges().get(0).getElement()).getHttpVerb(), equalTo("BAR"));
		assertNull(httpMethod.getAnnotations().get(TARGET));
		// 6 built-in HTTP Methods + 1 custom one
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldNotChangeHttpMethodWhenAddingDeprecatedAnnotation() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BAR");
		metamodelMonitor.createHttpMethod(type);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		addTypeAnnotation(type, "@Deprecated", false);
		final ResourceDelta event = createResourceDelta(type.getResource(), CHANGED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(0));
		// 6 built-in HTTP Methods + 1 custom one
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldRemoveHttpMethodWhenChangingResource() throws CoreException {
		// pre-conditions
		final JaxrsHttpMethod httpMethod = metamodelMonitor.createHttpMethod("org.jboss.tools.ws.jaxrs.sample.services.BAR");
		final Annotation annotation = httpMethod.getAnnotation(HTTP_METHOD);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		delete(annotation.getJavaAnnotation(), false);
		final ResourceDelta event = createResourceDelta(annotation.getJavaParent().getResource(), CHANGED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.HTTP_METHOD));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		// assertThat(((IJaxrsHttpMethod)
		// metamodelMonitor.getElementChanges().get(0).getElement()).getHttpVerb(),
		// equalTo("BAR")); <- verb was removed
		// 6 built-in HTTP Methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldRemoveHttpMethodWhenRemovingCompilationUnit() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BAR");
		final JaxrsHttpMethod httpMethod = metamodelMonitor.createHttpMethod(type);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final ResourceDelta event = createResourceDelta(type.getResource(), REMOVED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.HTTP_METHOD));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((JaxrsHttpMethod) metamodelMonitor.getElementChanges().get(0).getElement()), equalTo(httpMethod));
		// 6 built-in HTTP Methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldRemoveHttpMethodWhenRemovingSourceType() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BAR");
		final JaxrsHttpMethod httpMethod = metamodelMonitor.createHttpMethod(type);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		delete(type);
		final ResourceDelta event = createResourceDelta(type.getResource(), CHANGED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.HTTP_METHOD));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((IJaxrsHttpMethod) metamodelMonitor.getElementChanges().get(0).getElement()).getHttpVerb(), equalTo("BAR"));
		assertThat(((JaxrsHttpMethod) metamodelMonitor.getElementChanges().get(0).getElement()), equalTo(httpMethod));

		// 6 built-in HTTP Methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldRemoveHttpMethodWhenRemovingSourceFolder() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BAR");
		JaxrsHttpMethod.from(type).withMetamodel(metamodel).build();
		// metamodel.add(httpMethod);
		final IPackageFragmentRoot sourceFolder = metamodelMonitor.resolvePackageFragmentRoot("src/main/java");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final ResourceDelta event = createResourceDelta(sourceFolder.getResource(), REMOVED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.HTTP_METHOD));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement(), is(notNullValue()));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		// 6 built-in HTTP Methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldAddResourceWhenAddingSourceCompilationUnit() throws CoreException {
		// pre-conditions
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final ResourceDelta event = createResourceDelta(type.getResource(), ADDED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(7)); // 1 resource + 6
														// methods
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(metamodelMonitor.getElementChanges().get(6).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.RESOURCE));
		// HttpMethods, Resource, ResourceMethods and ResourceFields
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(13));
	}

	@Test
	public void shouldAddResourceWhenChangingResource() throws CoreException {
		// pre-conditions
		metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		// metamodel.add();
		final IType customerType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final ResourceDelta event = createResourceDelta(customerType.getResource(), CHANGED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(7)); // 1 resource + 6
														// methods
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(ADDED));
		// 6 built-in HTTP Methods + 2 resource + (3 + 6) methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(17));
	}

	@Test
	public void shouldChangeExistingResourceWhenChangingResource() throws CoreException {
		// pre-conditions
		metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		// metamodel.add();
		final JaxrsResource customerResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		customerResource.removeAnnotation(customerResource.getProducesAnnotation().getJavaAnnotation());
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final ResourceDelta event = createResourceDelta(customerResource.getJavaElement().getResource(), CHANGED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1)); // 1 resource
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(((JaxrsResource) metamodelMonitor.getElementChanges().get(0).getElement()), equalTo(customerResource));
		// 6 built-in HTTP Methods + 1 resource + 6 methods
		assertThat(metamodel.findElements(javaProject).size(), equalTo(17));
	}

	@Test
	public void shouldAddResourceMethodWhenChangingResource() throws CoreException {
		// pre-conditions
		final JaxrsResource bookResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		bookResource.removeMethod(bookResource.getAllMethods().get(0));
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final ResourceDelta event = createResourceDelta(bookResource.getJavaElement().getResource(), CHANGED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1)); // 1 resource method
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.RESOURCE_METHOD));
		// 6 built-in HttpMethods + 1 resource (including its methods and
		// fields)
		assertThat(metamodel.findElements(javaProject).size(), equalTo(10));
	}

	@Test
	public void shouldChangeResourceMethodWhenChangingResource() throws CoreException {
		// pre-conditions
		final JaxrsResource bookResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		for (Iterator<JaxrsResourceMethod> iterator = bookResource.getMethods().values().iterator(); iterator.hasNext();) {
			JaxrsResourceMethod resourceMethod = iterator.next();
			if (resourceMethod.getElementKind() == EnumElementKind.SUBRESOURCE_METHOD) {
				replaceFirstOccurrenceOfCode(resourceMethod.getJavaElement(), "@Path(\"/{id}\")", "@Path(\"/{foo}\")",
						false);
				delete(resourceMethod.getHttpMethodAnnotation().getJavaAnnotation(), false);
			}
		}
		final ResourceDelta event = createResourceDelta(bookResource.getJavaElement().getResource(), CHANGED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(2)); // 2 resource
														// methods
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.RESOURCE_METHOD));
		assertThat(metamodelMonitor.getElementChanges().get(1).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(1).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.RESOURCE_METHOD));
		// 6 built-in HttpMethods + 1 resources (including its methods and
		// fields)
		assertThat(metamodel.findElements(javaProject).size(), equalTo(10));
	}

	@Test
	public void shouldRemoveResourceMethodWhenChangingResource() throws CoreException {
		// pre-conditions
		final JaxrsResource bookResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		for (Iterator<JaxrsResourceMethod> iterator = bookResource.getMethods().values().iterator(); iterator.hasNext();) {
			JaxrsResourceMethod resourceMethod = iterator.next();
			if (resourceMethod.getElementKind() == EnumElementKind.RESOURCE_METHOD) {
				delete(resourceMethod.getHttpMethodAnnotation().getJavaAnnotation(), false);
			}
		}
		final ResourceDelta event = createResourceDelta(bookResource.getJavaElement().getResource(), CHANGED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1)); // 1 resource method
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.RESOURCE_METHOD));
		// 6 HttpMethods + 1 resource (including its remaining 2 methods and
		// fields)
		assertThat(metamodel.findElements(javaProject).size(), equalTo(9));
	}

	@Test
	public void shouldAddResourceFieldWhenChangingResource() throws CoreException {
		// pre-conditions
		final JaxrsResource productResourceLocator = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		productResourceLocator.removeField(productResourceLocator.getAllFields().get(0));
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final ResourceDelta event = createResourceDelta(productResourceLocator.getJavaElement().getResource(), CHANGED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1)); // 1 resource field
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.RESOURCE_FIELD));
		// 6 built-in HttpMethods + 1 resource (including its 1 method and 3
		// fields)
		assertThat(metamodel.findElements(javaProject).size(), equalTo(11));
	}

	@Test
	public void shouldChangeResourceFieldWhenChangingResource() throws CoreException {
		// pre-conditions
		final JaxrsResource productResourceLocator = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		for (Iterator<JaxrsResourceField> iterator = productResourceLocator.getFields().values().iterator(); iterator
				.hasNext();) {
			JaxrsResourceField resourceField = iterator.next();
			if (resourceField.getDefaultValueAnnotation() != null) {
				replaceFirstOccurrenceOfCode(resourceField.getJavaElement(), "@DefaultValue(\"foo!\")",
						"@DefaultValue(\"bar\")", false);
			}
		}
		final ResourceDelta event = createResourceDelta(productResourceLocator.getJavaElement().getResource(), CHANGED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1)); // 1 resource field
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.RESOURCE_FIELD));
		// 6 built-in HttpMethods + 1 resource (including its 1 method and 3
		// fields)
		assertThat(metamodel.findElements(javaProject).size(), equalTo(11));
	}

	@Test
	public void shouldRemoveResourceFieldWhenChangingResource() throws CoreException {
		// pre-conditions
		final JaxrsResource productResourceLocator = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		for (Iterator<JaxrsResourceField> iterator = productResourceLocator.getFields().values().iterator(); iterator
				.hasNext();) {
			JaxrsResourceField resourceField = iterator.next();
			if (resourceField.getQueryParamAnnotation() != null) {
				delete(resourceField.getQueryParamAnnotation().getJavaAnnotation(), false);
			}
		}
		final ResourceDelta event = createResourceDelta(productResourceLocator.getJavaElement().getResource(), CHANGED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1)); // 1 resource field
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.RESOURCE_FIELD));
		// 6 built-in HttpMethods + 1 resource (including its 1 method and 2
		// fields)
		assertThat(metamodel.findElements(javaProject).size(), equalTo(10));
	}

	@Test
	public void shouldRemoveExistingResourceWhenChangingResource() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		for (IMethod method : resource.getJavaElement().getMethods()) {
			delete(method);
		}
		final ResourceDelta event = createResourceDelta(resource.getJavaElement().getResource(), CHANGED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(3)); // 1 resource + 2
														// methods
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.RESOURCE));
		// 6 HttpMethods left only
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldRemoveResourceWhenRemovingCompilationUnit() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		delete(resource.getJavaElement().getResource());
		final ResourceDelta event = createResourceDelta(resource.getJavaElement().getResource(), REMOVED);
		processAffectedResources(event);
		// verifications: 1 resource and its 2 methods removed
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(3));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.RESOURCE_METHOD));
		assertThat(metamodelMonitor.getElementChanges().get(1).getDeltaKind(), equalTo(REMOVED));
		assertThat(metamodelMonitor.getElementChanges().get(1).getElement().getElementKind().getCategory(),
				equalTo(EnumElementCategory.RESOURCE_METHOD));
		assertThat(metamodelMonitor.getElementChanges().get(2).getDeltaKind(), equalTo(REMOVED));
		assertThat(metamodelMonitor.getElementChanges().get(2).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.RESOURCE));
		assertThat(((JaxrsResource) metamodelMonitor.getElementChanges().get(2).getElement()), equalTo(resource));
		// 6 built-in HttpMethods left only
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldRemoveResourceWhenRemovingSourceType() throws CoreException {
		// pre-conditions
		final JaxrsResource resource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		delete(resource.getJavaElement());
		final ResourceDelta event = createResourceDelta(resource.getJavaElement().getResource(), CHANGED);
		processAffectedResources(event);
		// verifications: 1 resource and its 2 methods removed
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(3));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.RESOURCE));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((JaxrsResource) metamodelMonitor.getElementChanges().get(0).getElement()), equalTo(resource));
		// 6 built-in HttpMethods left only
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldRemoveResourceWhenRemovingSourceFolder() throws CoreException {
		// pre-conditions
		final IPackageFragmentRoot sourceFolder = metamodelMonitor.resolvePackageFragmentRoot("src/main/java");
		metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final ResourceDelta event = createResourceDelta(sourceFolder.getResource(), REMOVED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(7));
		// only built-in HTTP Methods left
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	@Ignore
	public void shouldRemoveResourceWhenRemovingBinaryLib() throws CoreException {
		// need to package a JAX-RS resource into a jar...
	}

	@Test
	public void shouldRemoveResourceWhenRemovingMethodsFieldsAndAnnotations() throws CoreException {
		// pre-conditions
		final JaxrsResource resourceLocator = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		for (Iterator<JaxrsResourceMethod> iterator = resourceLocator.getMethods().values().iterator(); iterator
				.hasNext();) {
			JaxrsResourceMethod resourceMethod = iterator.next();
			delete(resourceMethod.getJavaElement());
		}
		for (Iterator<JaxrsResourceField> iterator = resourceLocator.getFields().values().iterator(); iterator
				.hasNext();) {
			JaxrsResourceField resourceField = iterator.next();
			delete(resourceField.getJavaElement());
		}
		for (Iterator<Annotation> iterator = resourceLocator.getAnnotations().values().iterator(); iterator.hasNext();) {
			Annotation annotation = iterator.next();
			delete(annotation.getJavaAnnotation(), false);
		}
		final ResourceDelta event = createResourceDelta(resourceLocator.getJavaElement().getResource(), CHANGED);
		processAffectedResources(event);
		// verifications: 1 resource, its 1 method and 3 fields removed
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(5));
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.RESOURCE));
		// 6 built-in HttpMethods left only
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	/**
	 * Test in relation with https://issues.jboss.org/browse/JBIDE-12806
	 * 
	 * @throws CoreException
	 */
	@Test
	public void shouldUpdateTypeAnnotationLocationAfterCodeChangeAbove() throws CoreException {
		// pre-condition: using the CustomerResource type
		final JaxrsResource customerResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final Annotation pathAnnotation = customerResource.getAnnotation(PATH);
		final ISourceRange beforeChangeSourceRange = JdtUtils.resolveMemberPairValueRange(
				pathAnnotation.getJavaAnnotation(), "value");
		final int length = beforeChangeSourceRange.getLength();
		final int offset = beforeChangeSourceRange.getOffset();
		metamodelMonitor.resetElementChangesNotifications();
		// operation: removing @Encoded *before* the CustomerResource type
		replaceFirstOccurrenceOfCode(customerResource.getJavaElement(), "@Encoded", "", false);
		CompilationUnitsRepository.getInstance().mergeAST(customerResource.getJavaElement().getCompilationUnit(),
				JdtUtils.parse(customerResource.getJavaElement().getCompilationUnit(), null), true);
		final ResourceDelta event = createResourceDelta(customerResource.getJavaElement().getResource(), CHANGED);
		processAffectedResources(event);
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
		final JaxrsResource customerResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod resourceMethod = metamodelMonitor.resolveResourceMethod(customerResource, "createCustomer");
		final ISourceRange beforeChangeSourceRange = resourceMethod.getAnnotation(POST)
				.getJavaAnnotation().getSourceRange();
		final int length = beforeChangeSourceRange.getLength();
		final int offset = beforeChangeSourceRange.getOffset();
		metamodelMonitor.resetElementChangesNotifications();
		// operation: removing @Encoded *before* the createCustomer() method
		replaceFirstOccurrenceOfCode(customerResource.getJavaElement(), "@Encoded", "", false);
		CompilationUnitsRepository.getInstance().mergeAST(customerResource.getJavaElement().getCompilationUnit(),
				JdtUtils.parse(customerResource.getJavaElement().getCompilationUnit(), null), true);
		final ResourceDelta event = createResourceDelta(customerResource.getJavaElement().getResource(), CHANGED);
		processAffectedResources(event);
		// verifications
		final ISourceRange afterChangeSourceRange = resourceMethod.getAnnotation(POST)
				.getJavaAnnotation().getSourceRange();
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
		final JaxrsResource customerResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod javaMethod = metamodelMonitor.resolveMethod(customerResource.getJavaElement(), "getCustomer");
		final JaxrsResourceMethod resourceMethod = customerResource.getMethods().get(javaMethod.getHandleIdentifier());
		Annotation pathParamAnnotation = resourceMethod.getJavaMethodParameters().get(0).getAnnotations()
				.get(PATH_PARAM);
		final ISourceRange beforeChangeSourceRange = JdtUtils.resolveMemberPairValueRange(
				pathParamAnnotation.getJavaAnnotation(), "value");
		final int beforeLength = beforeChangeSourceRange.getLength();
		final int beforeOffset = beforeChangeSourceRange.getOffset();
		metamodelMonitor.resetElementChangesNotifications();
		// operation: removing @Encoded *before* the getCustomer() method
		replaceFirstOccurrenceOfCode(customerResource.getJavaElement(), "@Encoded", "", false);
		CompilationUnitsRepository.getInstance().mergeAST(customerResource.getJavaElement().getCompilationUnit(),
				JdtUtils.parse(customerResource.getJavaElement().getCompilationUnit(), null), true);
		final ResourceDelta event = createResourceDelta(customerResource.getJavaElement().getResource(), CHANGED);
		processAffectedResources(event);
		// verifications
		// reference has changed (local variable)
		pathParamAnnotation = resourceMethod.getJavaMethodParameters().get(0).getAnnotations()
				.get(PATH_PARAM);
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
		final JaxrsResource customerResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		IMethod javaMethod = metamodelMonitor.resolveMethod(customerResource.getJavaElement(), "getCustomer");
		JaxrsResourceMethod resourceMethod = customerResource.getMethods().get(javaMethod.getHandleIdentifier());
		Annotation contextAnnotation = resourceMethod.getJavaMethodParameters().get(1).getAnnotations()
				.get(CONTEXT);
		final ISourceRange beforeChangeSourceRange = contextAnnotation.getJavaAnnotation().getSourceRange();
		final int beforeLength = beforeChangeSourceRange.getLength();
		final int beforeOffset = beforeChangeSourceRange.getOffset();
		metamodelMonitor.resetElementChangesNotifications();
		// operation: removing "@PathParam("id") Integer id" parameter in the
		// getCustomer() method
		replaceFirstOccurrenceOfCode(customerResource.getJavaElement(), "@PathParam(\"id\") Integer id, ", "", false);
		CompilationUnitsRepository.getInstance().mergeAST(customerResource.getJavaElement().getCompilationUnit(),
				JdtUtils.parse(customerResource.getJavaElement().getCompilationUnit(), null), true);
		final ResourceDelta event = createResourceDelta(customerResource.getJavaElement().getResource(), CHANGED);
		processAffectedResources(event);
		// verifications: java method has changed, so all references must be
		// looked-up again
		javaMethod = metamodelMonitor.resolveMethod(customerResource.getJavaElement(), "getCustomer");
		resourceMethod = customerResource.getMethods().get(javaMethod.getHandleIdentifier());
		contextAnnotation = resourceMethod.getJavaMethodParameters().get(0).getAnnotations().get(CONTEXT);
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
		final JaxrsResource customerResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final Annotation pathAnnotation = customerResource.getAnnotation(PATH);
		final ISourceRange beforeChangeSourceRange = JdtUtils.resolveMemberPairValueRange(
				pathAnnotation.getJavaAnnotation(), "value");
		final int length = beforeChangeSourceRange.getLength();
		final int offset = beforeChangeSourceRange.getOffset();
		metamodelMonitor.resetElementChangesNotifications();
		// operation: removing @DELETE *after* the CustomerResource type
		replaceFirstOccurrenceOfCode(customerResource.getJavaElement(), "@DELETE", "", false);
		CompilationUnitsRepository.getInstance().mergeAST(customerResource.getJavaElement().getCompilationUnit(),
				JdtUtils.parse(customerResource.getJavaElement().getCompilationUnit(), null), true);
		final ResourceDelta event = createResourceDelta(customerResource.getJavaElement().getResource(), CHANGED);
		processAffectedResources(event);
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
		final JaxrsResource customerResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResourceMethod resourceMethod = metamodelMonitor.resolveResourceMethod(customerResource, "createCustomer");
		final Annotation postAnnotation = resourceMethod.getAnnotation(POST);
		final ISourceRange beforeChangeSourceRange = postAnnotation.getJavaAnnotation().getSourceRange();
		final int length = beforeChangeSourceRange.getLength();
		final int offset = beforeChangeSourceRange.getOffset();
		metamodelMonitor.resetElementChangesNotifications();
		// operation: removing @DELETE, after the createCustomer() method
		replaceFirstOccurrenceOfCode(customerResource.getJavaElement(), "@DELETE", "", false);
		CompilationUnitsRepository.getInstance().mergeAST(customerResource.getJavaElement().getCompilationUnit(),
				JdtUtils.parse(customerResource.getJavaElement().getCompilationUnit(), null), true);
		final ResourceDelta event = createResourceDelta(customerResource.getJavaElement().getResource(), CHANGED);
		processAffectedResources(event);
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
		final JaxrsResource customerResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod javaMethod = metamodelMonitor.resolveMethod(customerResource.getJavaElement(), "getCustomer");
		final JaxrsResourceMethod resourceMethod = customerResource.getMethods().get(javaMethod.getHandleIdentifier());
		final Annotation pathParamAnnotation = resourceMethod.getJavaMethodParameters().get(0).getAnnotations()
				.get(PATH_PARAM);
		final ISourceRange beforeChangeSourceRange = JdtUtils.resolveMemberPairValueRange(
				pathParamAnnotation.getJavaAnnotation(), "value");
		final int length = beforeChangeSourceRange.getLength();
		final int offset = beforeChangeSourceRange.getOffset();
		metamodelMonitor.resetElementChangesNotifications();
		// operation: removing @DELETE, *after* the getCustomer() method
		replaceFirstOccurrenceOfCode(customerResource.getJavaElement(), "@DELETE", "", false);
		CompilationUnitsRepository.getInstance().mergeAST(customerResource.getJavaElement().getCompilationUnit(),
				JdtUtils.parse(customerResource.getJavaElement().getCompilationUnit(), null), true);
		final ResourceDelta event = createResourceDelta(customerResource.getJavaElement().getResource(), CHANGED);
		processAffectedResources(event);
		// verifications
		final ISourceRange afterChangeSourceRange = JdtUtils.resolveMemberPairValueRange(
				pathParamAnnotation.getJavaAnnotation(), "value");
		assertThat(afterChangeSourceRange.getOffset(), equalTo(offset));
		assertThat(afterChangeSourceRange.getLength(), equalTo(length));
	}

	@Test
	public void shouldAddProviderWhenAddingSourceCompilationUnit() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final ResourceDelta event = createResourceDelta(type.getResource(), ADDED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1)); // 1 provider
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.PROVIDER));
		// 6 built-in HTTP Methods and 1 Provider
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldAddProviderWhenChangingResource() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final ResourceDelta event = createResourceDelta(type.getResource(), CHANGED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1)); // 1 provider
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.PROVIDER));
		// 6 built-in HTTP Methods and 1 Provider
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldNotChangeProviderWhenAddingDeprecatedAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsProvider provider = metamodelMonitor.createProvider("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		addTypeAnnotation(provider.getJavaElement(), "@Deprecated", false);
		final ResourceDelta event = createResourceDelta(provider.getResource(), CHANGED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(0));
	}

	@Test
	public void shouldDoNothingWhenChangingUnrelatedProviderAnnotationValue() throws CoreException {
		// pre-conditions
		final JaxrsProvider provider = metamodelMonitor.createProvider("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		replaceAllOccurrencesOfCode(provider.getJavaElement().getCompilationUnit(),
				"@SuppressWarnings(\"testing\")", "@SuppressWarnings(\"test\")", false);
		final ResourceDelta event = createResourceDelta(provider.getResource(), CHANGED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(0));
	}

	@Test
	public void shouldRemoveProviderWhenRemovingResource() throws CoreException {
		// pre-conditions
		final JaxrsProvider provider = metamodelMonitor.createProvider("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		delete(provider.getResource());
		final ResourceDelta event = createResourceDelta(provider.getResource(), REMOVED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1)); // 1 provider
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.PROVIDER));
		// 6 built-in HTTP Methods only
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldRemoveProviderWhenCompilationUnit() throws CoreException {
		// pre-conditions
		final JaxrsProvider provider = metamodelMonitor.createProvider("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		delete(provider.getJavaElement().getCompilationUnit());
		final ResourceDelta event = createResourceDelta(provider.getResource(), REMOVED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1)); // 1 provider
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.PROVIDER));
		// 6 built-in HTTP Methods only
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldRemoveProviderWhenRemovingSourceType() throws CoreException {
		// pre-conditions
		final JaxrsProvider provider = metamodelMonitor.createProvider("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		delete(provider.getJavaElement());
		final ResourceDelta event = createResourceDelta(provider.getResource(), CHANGED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1)); // 1 provider
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.PROVIDER));
		// 6 built-in HTTP Methods only
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldNotRemoveProviderWhenRemovingAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsProvider provider = metamodelMonitor.createProvider("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final IType type = provider.getJavaElement();
		final IAnnotation annotation = provider.getAnnotation(PROVIDER).getJavaAnnotation();
		removeTypeAnnotation(type, annotation, false);
		final ResourceDelta event = createResourceDelta(provider.getResource(), CHANGED);
		processAffectedResources(event);
		// verifications
		// no change (validation warning may occur, though)
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
	}

	@Test
	public void shouldCreateProviderEvenIfHierarchyIsMissing() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		replaceFirstOccurrenceOfCode(type, "implements ExceptionMapper<EntityNotFoundException>", "",
				false);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		final ResourceDelta event = createResourceDelta(type.getResource(), CHANGED);
		processAffectedResources(event);
		// verifications: 1 provider should be created
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1));
		assertThat(((JaxrsProvider) metamodelMonitor.getElementChanges().get(0).getElement()).getProvidedTypes().size(), equalTo(0));
	}

	@Test
	public void shouldRemoveProviderWhenRemovingHierarchyAndAnnotationAlreadyMissing() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		replaceFirstOccurrenceOfCode(type, "@Provider", "", false);
		final JaxrsProvider provider = metamodelMonitor.createProvider(type);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		replaceFirstOccurrenceOfCode(type, "implements ExceptionMapper<EntityNotFoundException>", "",
				false);
		final ResourceDelta event = createResourceDelta(provider.getResource(), CHANGED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1)); // 1 provider
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.PROVIDER));
		// 6 built-in HTTP Methods only
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldDoNothingWhenRemovingUnrelatedAnnotationOnProvider() throws CoreException {
		// pre-conditions
		final IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final JaxrsProvider provider = metamodelMonitor.createProvider(type);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		replaceFirstOccurrenceOfCode(type, "@SuppressWarnings(\"testing\")", "", false);
		final ResourceDelta event = createResourceDelta(provider.getResource(), CHANGED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(0)); // no change
	}

	@Test
	public void shouldRemoveProviderWhenRemovingSourceFolder() throws CoreException {
		// pre-conditions
		metamodelMonitor.createProvider("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		final IPackageFragmentRoot sourceFolder = metamodelMonitor.resolvePackageFragmentRoot("src/main/java");
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		delete(sourceFolder.getResource());
		final ResourceDelta event = createResourceDelta(sourceFolder.getResource(), REMOVED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1)); // 1 provider
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getElement().getElementKind().getCategory(), equalTo(EnumElementCategory.PROVIDER));
		// 6 built-in HTTP Methods only
		assertThat(metamodel.findElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldUpdateProviderWhenAddingConsumesAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsProvider provider = metamodelMonitor.createProvider("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		provider.removeAnnotation(provider.getAnnotation(CONSUMES).getJavaAnnotation());
		metamodelMonitor.resetElementChangesNotifications();
		// operation: should see the @Consumes annotation that was just removed
		// from the model entity
		final ResourceDelta event = createResourceDelta(provider.getResource(), CHANGED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1)); // 1 provider
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_CONSUMES_ANNOTATION));
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
		assertThat(provider.getAnnotation(CONSUMES), notNullValue());
	}

	@Test
	public void shouldUpdateProviderWhenChangingConsumesAnnotationValue() throws CoreException {
		// pre-conditions
		final JaxrsProvider provider = metamodelMonitor.createProvider("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		metamodelMonitor.resetElementChangesNotifications();
		// operation: should see the @Consumes annotation that was just removed
		// from the model entity
		replaceFirstOccurrenceOfCode(provider.getJavaElement(), "@Consumes(\"application/json\")",
				"@Consumes(\"application/foo\")", false);
		final ResourceDelta event = createResourceDelta(provider.getResource(), CHANGED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1)); // 1 provider
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_CONSUMES_ANNOTATION));
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
		assertThat(provider.getAnnotation(CONSUMES).getValue(), equalTo("application/foo"));
	}

	@Test
	public void shouldUpdateProviderWhenRemovingConsumesAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsProvider provider = metamodelMonitor.createProvider("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		metamodelMonitor.resetElementChangesNotifications();
		// operation: should see the @Consumes annotation that was just removed
		// from the model entity
		removeFirstOccurrenceOfCode(provider.getJavaElement(), "@Consumes(\"application/json\")", false);
		final ResourceDelta event = createResourceDelta(provider.getResource(), CHANGED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1)); // 1 provider
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_CONSUMES_ANNOTATION));
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
		assertThat(provider.getAnnotation(CONSUMES), nullValue());
	}

	@Test
	public void shouldUpdateProviderWhenAddingProducesAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsProvider provider = metamodelMonitor.createProvider("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		provider.removeAnnotation(provider.getAnnotation(PRODUCES).getJavaAnnotation());
		metamodelMonitor.resetElementChangesNotifications();
		// operation: should see the @Consumes annotation that was just removed
		// from the model entity
		final ResourceDelta event = createResourceDelta(provider.getResource(), CHANGED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1)); // 1 provider
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_PRODUCES_ANNOTATION));
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
		assertThat(provider.getAnnotation(PRODUCES), notNullValue());
	}

	@Test
	public void shouldUpdateProviderWhenChangingProducesAnnotationValue() throws CoreException {
		// pre-conditions
		final JaxrsProvider provider = metamodelMonitor.createProvider("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		metamodelMonitor.resetElementChangesNotifications();
		// operation: should see the @Produces annotation that was just changed
		// from the model entity
		replaceFirstOccurrenceOfCode(provider.getJavaElement(), "@Produces(\"application/json\")",
				"@Produces(\"application/foo\")", false);
		final ResourceDelta event = createResourceDelta(provider.getResource(), CHANGED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1)); // 1 provider
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_PRODUCES_ANNOTATION));
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
		assertThat(provider.getAnnotation(PRODUCES).getValue(), equalTo("application/foo"));
	}

	@Test
	public void shouldUpdateProviderWhenRemovingProducesAnnotation() throws CoreException {
		// pre-conditions
		final JaxrsProvider provider = metamodelMonitor.createProvider("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		metamodelMonitor.resetElementChangesNotifications();
		// operation: should see the @Produces annotation that was just removed
		// from the model entity
		removeFirstOccurrenceOfCode(provider.getJavaElement(), "@Produces(\"application/json\")", false);
		final ResourceDelta event = createResourceDelta(provider.getResource(), CHANGED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1)); // 1 provider
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_PRODUCES_ANNOTATION));
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
		assertThat(provider.getAnnotation(PRODUCES), nullValue());
	}

	@Test
	public void shouldUpdateProviderWhenProvidedTypeChanged() throws CoreException {
		// pre-conditions
		final JaxrsProvider provider = metamodelMonitor.createProvider("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		metamodelMonitor.resetElementChangesNotifications();
		// operation: should see the provider type that was just changed
		// from the model entity
		replaceAllOccurrencesOfCode(provider.getJavaElement(),
				"import javax.persistence.EntityNotFoundException;", "import javax.persistence.NoResultException;",
				false);
		replaceAllOccurrencesOfCode(provider.getJavaElement(),
				"ExceptionMapper<EntityNotFoundException>", "ExceptionMapper<NoResultException>", false);
		// replaceAllOccurrencesOfCode(provider.getJavaElement(),
		// "(EntityNotFoundException exception)",
		// "(NoResultException exception)", false);
		final ResourceDelta event = createResourceDelta(provider.getResource(), CHANGED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1)); // 1 provider
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_PROVIDER_HIERARCHY));
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
		assertThat(provider.getProvidedType(EnumElementKind.EXCEPTION_MAPPER), notNullValue());
		assertThat(provider.getProvidedType(EnumElementKind.EXCEPTION_MAPPER).getFullyQualifiedName(),
				equalTo("javax.persistence.NoResultException"));
	}

	@Test
	public void shouldUpdateProviderWhenProvidedTypeChangedWithInterfacesInheritance() throws CoreException {
		// pre-conditions
		final JaxrsProvider provider = metamodelMonitor.createProvider("org.jboss.tools.ws.jaxrs.sample.extra.DummyProvider");
		metamodelMonitor.resetElementChangesNotifications();
		// operation: should see the @Consumes annotation that was just removed
		// from the model entity
		replaceAllOccurrencesOfCode(provider.getJavaElement(), "AbstractEntityProvider<String, Number>",
				"AbstractEntityProvider<Integer, Number>", false);
		final ResourceDelta event = createResourceDelta(provider.getResource(), CHANGED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1)); // 1 provider
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_PROVIDER_HIERARCHY));
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
		assertThat(provider.getProvidedType(EnumElementKind.MESSAGE_BODY_READER).getFullyQualifiedName(),
				equalTo("java.lang.Integer"));
		assertThat(provider.getProvidedType(EnumElementKind.MESSAGE_BODY_WRITER).getFullyQualifiedName(),
				equalTo("java.lang.Number"));
	}

	@Test
	public void shouldRemoveMessageBodyReaderWhenProvidedTypeDoesNotExist() throws CoreException {
		// pre-conditions
		final JaxrsProvider provider = metamodelMonitor.createProvider("org.jboss.tools.ws.jaxrs.sample.extra.DummyProvider");
		metamodelMonitor.resetElementChangesNotifications();
		// operation: should see the @Consumes annotation that was just removed
		// from the model entity
		replaceAllOccurrencesOfCode(provider.getJavaElement(), "AbstractEntityProvider<String, Number>",
				"AbstractEntityProvider<Foo, Number>", false);
		final ResourceDelta event = createResourceDelta(provider.getResource(), CHANGED);
		processAffectedResources(event);
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1)); // 1 provider
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodelMonitor.getElementChanges().get(0).getFlags(), flagMatches(F_PROVIDER_HIERARCHY));
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
		assertThat(provider.getProvidedType(EnumElementKind.MESSAGE_BODY_READER), nullValue());
		assertThat(provider.getProvidedType(EnumElementKind.MESSAGE_BODY_WRITER).getFullyQualifiedName(),
				equalTo("java.lang.Number"));
	}

	@Test
	public void shouldNotRemoveProviderWhenProvidedTypesDoNotExist() throws CoreException {
		// pre-conditions
		final JaxrsProvider provider = metamodelMonitor.createProvider("org.jboss.tools.ws.jaxrs.sample.extra.DummyProvider");
		metamodelMonitor.resetElementChangesNotifications();
		// operation: should see the @Consumes annotation that was just removed
		// from the model entity
		replaceAllOccurrencesOfCode(provider.getJavaElement(), "AbstractEntityProvider<String, Number>",
				"AbstractEntityProvider<Foo, Bar>", false);
		processAffectedResources(createResourceDelta(provider.getResource(), CHANGED));
		// verifications
		assertThat(metamodelMonitor.getElementChanges().size(), equalTo(1)); // 1 provider
		assertThat(metamodelMonitor.getElementChanges().get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(metamodel.findElements(javaProject).size(), equalTo(7));
		assertThat(provider.getProvidedType(EnumElementKind.MESSAGE_BODY_READER), nullValue());
		assertThat(provider.getProvidedType(EnumElementKind.MESSAGE_BODY_WRITER), nullValue());

	}
}
