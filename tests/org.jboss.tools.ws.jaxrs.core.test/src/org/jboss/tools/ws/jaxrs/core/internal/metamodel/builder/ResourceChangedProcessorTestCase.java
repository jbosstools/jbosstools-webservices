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
import static org.jboss.tools.ws.jaxrs.core.WorkbenchUtils.changeAnnotation;
import static org.jboss.tools.ws.jaxrs.core.WorkbenchUtils.getAnnotation;
import static org.jboss.tools.ws.jaxrs.core.WorkbenchUtils.getType;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.APPLICATION_PATH;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.CONSUMES;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.DELETE;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.GET;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.HTTP_METHOD;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.POST;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.PUT;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.RETENTION;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.TARGET;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.hamcrest.Matchers;
import org.jboss.tools.ws.jaxrs.core.AbstractCommonTestCase;
import org.jboss.tools.ws.jaxrs.core.JBossJaxrsCorePlugin;
import org.jboss.tools.ws.jaxrs.core.WorkbenchUtils;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsElementFactory;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsJavaApplication;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceField;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsWebxmlApplication;
import org.jboss.tools.ws.jaxrs.core.internal.utils.WtpUtils;
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
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class ResourceChangedProcessorTestCase extends AbstractCommonTestCase {

	private final static int NO_FLAG = 0;

	private JaxrsMetamodel metamodel;

	private final ResourceChangedProcessor processor = new ResourceChangedProcessor();

	private static final IProgressMonitor progressMonitor = new NullProgressMonitor();

	@Before
	public void setup() throws CoreException {
		JBossJaxrsCorePlugin.getDefault().unregisterListeners();
		// metamodel = Mockito.mock(JaxrsMetamodel.class);
		// in case an element was attempted to be removed, some impact would be
		// retrieved
		// when(metamodel.remove(any(IJaxrsElement.class))).thenReturn(true);
		metamodel = spy(JaxrsMetamodel.create(javaProject));
		// replace the normal metamodel instance with the one spied by Mockito
		javaProject.getProject().setSessionProperty(JaxrsMetamodel.METAMODEL_QUALIFIED_NAME, metamodel);
		CompilationUnitsRepository.getInstance().clear();
	}

	/**
	 * Creates a java annotated type based JAX-RS Application element
	 * 
	 * @param type
	 * @return
	 * @throws JavaModelException
	 */
	private JaxrsJavaApplication createJavaApplication(IType type) throws JavaModelException {
		final Annotation appPathAnnotation = getAnnotation(type, APPLICATION_PATH.qualifiedName);
		return new JaxrsJavaApplication(type, appPathAnnotation, true, metamodel);
	}

	/**
	 * Creates a java annotated type based JAX-RS Application element
	 * 
	 * @param type
	 * @param applicationPath
	 * @return
	 * @throws JavaModelException
	 */
	private JaxrsJavaApplication createJavaApplication(IType type, String applicationPath) throws JavaModelException {
		final Annotation appPathAnnotation = changeAnnotation(type, APPLICATION_PATH.qualifiedName, applicationPath);
		return new JaxrsJavaApplication(type, appPathAnnotation, true, metamodel);
	}

	/**
	 * Creates a web.xml based JAX-RS Application element
	 * 
	 * @param applicationPath
	 * @return
	 * @throws JavaModelException
	 */
	private JaxrsWebxmlApplication createWebxmlApplication(final String applicationClassName,
			final String applicationPath) throws JavaModelException {
		final IResource webDeploymentDescriptor = WtpUtils.getWebDeploymentDescriptor(project);
		return new JaxrsWebxmlApplication(applicationClassName, applicationPath, webDeploymentDescriptor, metamodel);
	}

	/**
	 * @return
	 * @throws CoreException
	 * @throws JavaModelException
	 */
	private JaxrsHttpMethod createHttpMethod(EnumJaxrsClassname httpMethodElement) throws CoreException,
			JavaModelException {
		final IType httpMethodType = JdtUtils
				.resolveType(httpMethodElement.qualifiedName, javaProject, progressMonitor);
		final Annotation httpMethodAnnotation = getAnnotation(httpMethodType, HTTP_METHOD.qualifiedName);
		final JaxrsHttpMethod httpMethod = new JaxrsHttpMethod.Builder(httpMethodType, metamodel).httpMethod(
				httpMethodAnnotation).build();

		return httpMethod;
	}

	private JaxrsHttpMethod createHttpMethod(IType type) throws JavaModelException {
		final Annotation httpMethodAnnotation = getAnnotation(type, HTTP_METHOD.qualifiedName);
		final Annotation targetAnnotation = getAnnotation(type, TARGET.qualifiedName);
		final Annotation retentionAnnotation = getAnnotation(type, RETENTION.qualifiedName);
		final JaxrsHttpMethod httpMethod = new JaxrsHttpMethod.Builder(type, metamodel)
				.httpMethod(httpMethodAnnotation).target(targetAnnotation).retention(retentionAnnotation).build();
		return httpMethod;
	}

	private JaxrsHttpMethod createHttpMethod(IType type, String httpVerb) throws JavaModelException {
		final Annotation annotation = changeAnnotation(type, HTTP_METHOD.qualifiedName, httpVerb);
		final JaxrsHttpMethod httpMethod = new JaxrsHttpMethod.Builder(type, metamodel).httpMethod(annotation).build();
		return httpMethod;
	}

	private JaxrsResource createResource(String fileName) throws CoreException {
		final IType type = getType(fileName, javaProject);
		final JaxrsResource resource = new JaxrsElementFactory().createResource(type, JdtUtils.parse(type, null),
				metamodel);
		return resource;
	}

	private ResourceDelta createEvent(IResource resource, int deltaKind) {
		return new ResourceDelta(resource, deltaKind, NO_FLAG);
	}

	private List<JaxrsElementDelta> processResourceChanges(ResourceDelta event, IProgressMonitor progressmonitor)
			throws CoreException {
		final JaxrsMetamodelDelta affectedMetamodel = processor.processAffectedResources(project, false,
				Arrays.asList(event), progressmonitor);
		return affectedMetamodel.getAffectedElements();
	}

	@Test
	public void shouldAddHttpMethodsAndResourcesWhenAddingSourceFolderWithExistingMetamodel() throws CoreException {
		// pre-conditions
		// operation
		final IPackageFragmentRoot sourceFolder = WorkbenchUtils.getPackageFragmentRoot(javaProject, "src/main/java",
				progressMonitor);
		final ResourceDelta event = createEvent(sourceFolder.getResource(), ADDED);
		final JaxrsMetamodelDelta affectedMetamodel = processor.processAffectedResources(project, false,
				Arrays.asList(event), progressMonitor);
		// verifications
		assertThat(affectedMetamodel.getDeltaKind(), equalTo(CHANGED));
		assertThat(affectedMetamodel.getMetamodel(), equalTo((IJaxrsMetamodel) metamodel));
		final List<JaxrsElementDelta> affectedElements = affectedMetamodel.getAffectedElements();
		// 1 application + 1 HttpMethod + 7 Resources
		assertThat(affectedElements.size(), equalTo(9));
		assertThat(affectedElements, everyItem(Matchers.<JaxrsElementDelta> hasProperty("deltaKind", equalTo(ADDED))));
		// all HttpMethods, Resources, ResourceMethods and ResourceFields. only application is available: the java-based
		// one found in src/main/java
		assertThat(metamodel.getElements(javaProject).size(), equalTo(35));
	}

	@Test
	public void shouldAddHttpMethodsAndResourcesWhenAddingSourceFolderWithExistingMetamodelWithReset()
			throws CoreException {
		// pre-conditions
		// operation
		final IPackageFragmentRoot sourceFolder = WorkbenchUtils.getPackageFragmentRoot(javaProject, "src/main/java",
				progressMonitor);
		final ResourceDelta event = createEvent(sourceFolder.getResource(), ADDED);
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
		// all project-specific Applications, HttpMethods, Resources, ResourceMethods and ResourceFields (built-in
		// HttpMethods are not bound to a project)
		// 2 applications are available: the java-based and the web.xml since a full build was performed
		assertThat(metamodel.getElements(javaProject).size(), equalTo(36));
	}

	/**
	 * Because sometimes, generics are painful...
	 * 
	 * @param elements
	 * @return private List<IJaxrsElement<?>> asList(IJaxrsElement<?>... elements) { final List<IJaxrsElement<?>> result
	 *         = new ArrayList<IJaxrsElement<?>>(); result.addAll(Arrays.asList(elements)); return result; }
	 */

	@Test
	public void shouldAddHttpMethodsAndResourcesWhenAddingSourceFolderWithoutExistingMetamodel() throws CoreException {
		// pre-conditions
		// remove the metamodel
		this.metamodel.remove();
		this.metamodel = null;
		// operation
		final IPackageFragmentRoot sourceFolder = WorkbenchUtils.getPackageFragmentRoot(javaProject, "src/main/java",
				progressMonitor);
		final ResourceDelta event = createEvent(sourceFolder.getResource(), ADDED);
		final JaxrsMetamodelDelta affectedMetamodel = processor.processAffectedResources(project, false,
				Arrays.asList(event), progressMonitor);
		// verifications
		// 1 application + 1 HttpMethod + 3 RootResources + 2 Subresources
		assertThat(affectedMetamodel.getDeltaKind(), equalTo(ADDED));
		metamodel = (JaxrsMetamodel) affectedMetamodel.getMetamodel();
		assertThat(metamodel, notNullValue());
		final List<JaxrsElementDelta> affectedElements = affectedMetamodel.getAffectedElements();
		assertThat(affectedElements.size(), equalTo(9));
		// all Applications, HttpMethods, Resources, ResourceMethods and ResourceFields specific to the project
		assertThat(metamodel.getElements(javaProject).size(), equalTo(36));

	}

	@Test
	public void shouldNotAddAnythingAddingBinaryLib() throws CoreException {
		// pre-conditions
		final IPackageFragmentRoot lib = WorkbenchUtils.getPackageFragmentRoot(javaProject,
				"lib/jaxrs-api-2.0.1.GA.jar", progressMonitor);
		// operation
		final ResourceDelta event = createEvent(lib.getResource(), ADDED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications: jar should not be taken into account, even if if it contains matching elements...
		assertThat(affectedElements.size(), equalTo(0));
	}

	@Test
	public void shouldAddHttpMethodWhenAddingSourceCompilationUnit() throws CoreException {
		// pre-conditions
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject,
				progressMonitor);
		// operation
		final ResourceDelta event = createEvent(type.getCompilationUnit().getResource(), ADDED);
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
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication",
				javaProject, progressMonitor);
		final Annotation annotation = getAnnotation(type, APPLICATION_PATH.qualifiedName);
		// operation
		final ResourceDelta event = createEvent(annotation.getJavaParent().getResource(), CHANGED);
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
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication",
				javaProject, progressMonitor);
		metamodel.add(createJavaApplication(type, "/bar"));
		// operation
		final ResourceDelta event = createEvent(type.getResource(), CHANGED);
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
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication",
				javaProject, progressMonitor);
		metamodel.add(createJavaApplication(type));
		final Annotation annotation = getAnnotation(type, APPLICATION_PATH.qualifiedName);
		// operation
		WorkbenchUtils.delete(annotation.getJavaAnnotation(), false);
		final ResourceDelta event = createEvent(annotation.getJavaParent().getResource(), CHANGED);
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
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication",
				javaProject, progressMonitor);
		metamodel.add(createJavaApplication(type));
		// operation
		WorkbenchUtils.replaceAllOccurrencesOfCode(type.getCompilationUnit(), "extends Application", "", false);
		final ResourceDelta event = createEvent(type.getResource(), CHANGED);
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
		// JaxrsMetamodel metamodel = new JaxrsMetamodel(javaProject);
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication",
				javaProject, progressMonitor);
		final JaxrsJavaApplication application = createJavaApplication(type);
		metamodel.add(application);
		// operation
		final ResourceDelta event = createEvent(type.getResource(), REMOVED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1));
		assertThat(affectedElements.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((JaxrsJavaApplication) affectedElements.get(0).getElement()), equalTo(application));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(0));
	}

	@Test
	public void shouldRemoveJavaApplicationWhenRemovingSourceType() throws CoreException {
		// pre-conditions
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject,
				progressMonitor);
		final Annotation annotation = getAnnotation(type, HTTP_METHOD.qualifiedName);
		final JaxrsHttpMethod httpMethod = new JaxrsHttpMethod.Builder(type, metamodel).httpMethod(annotation).build();
		metamodel.add(httpMethod);
		// operation
		WorkbenchUtils.delete(type);
		final ResourceDelta event = createEvent(type.getResource(), CHANGED);
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
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication",
				javaProject, progressMonitor);
		final JaxrsJavaApplication application = createJavaApplication(type);
		metamodel.add(application);
		final IPackageFragmentRoot sourceFolder = WorkbenchUtils.getPackageFragmentRoot(javaProject, "src/main/java",
				progressMonitor);
		// operation
		final ResourceDelta event = createEvent(sourceFolder.getResource(), REMOVED);
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
				"web-3_0-with-default-servlet-mapping.xml", bundle);
		// operation
		final ResourceDelta event = createEvent(webxmlResource, ADDED);
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
				"web-3_0-without-servlet-mapping.xml", bundle);
		// operation
		final ResourceDelta event = createEvent(webxmlResource, ADDED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(0));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(0));
	}

	@Test
	public void shouldAddWebxmlApplicationWhenChangingWebxml() throws Exception {
		// pre-conditions
		final IResource webxmlResource = WorkbenchUtils.replaceDeploymentDescriptorWith(javaProject,
				"web-3_0-with-default-servlet-mapping.xml", bundle);
		// operation
		final ResourceDelta event = createEvent(webxmlResource, CHANGED);
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
		// in this test, the java-application exists first, and then a web.xml application is added -> it should
		// immediately override the java-one
		// pre-conditions
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication",
				javaProject, progressMonitor);
		final JaxrsJavaApplication application = createJavaApplication(type);
		metamodel.add(application);
		final IResource webxmlResource = WorkbenchUtils.replaceDeploymentDescriptorWith(javaProject,
				"web-3_0-with-custom-servlet-mapping.xml", bundle);
		// operation
		final ResourceDelta event = createEvent(webxmlResource, ADDED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications: the JAVA Application is the sole element to be really changed
		assertThat(affectedElements.size(), equalTo(2));
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(affectedElements.get(0).getElement().getElementKind(), equalTo(EnumElementKind.APPLICATION_WEBXML));
		final JaxrsWebxmlApplication webxmlApplication = (JaxrsWebxmlApplication) affectedElements.get(0).getElement();
		assertThat(webxmlApplication.getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(webxmlApplication.getApplicationPath(), equalTo("/hello"));

		assertThat(affectedElements.get(1).getElement().getElementKind(), equalTo(EnumElementKind.APPLICATION_JAVA));
		assertThat(affectedElements.get(1).getDeltaKind(), equalTo(CHANGED));
		final JaxrsJavaApplication javaApplication = (JaxrsJavaApplication) affectedElements.get(1).getElement();
		assertThat(metamodel.getApplication(), equalTo((IJaxrsApplication) javaApplication)); // custom web.xml override
																								// DOES NOT precede the
																								// java based JAX-RS
																								// Application element
		assertThat(javaApplication.getApplicationPath(), equalTo("/hello")); // Java-based application configuration
																				// should not be changed
		assertThat(metamodel.getElements(javaProject).size(), equalTo(2)); // old application (java) + new one (web.xml)
		verify(metamodel, times(1)).add(any(JaxrsWebxmlApplication.class));
	}

	/**
	 * in this test, the webxml exists first, and then an annotated Java Application is added -> it should be
	 * immediately overriden
	 */
	@Test
	public void shouldOverrideJavaApplicationWhenAddingAnnotatedJavaApplication() throws Exception {
		// precondition
		final JaxrsWebxmlApplication webxmlApplication = createWebxmlApplication(
				"org.jboss.tools.ws.jaxrs.sample.services.RestApplication", "/hello");
		metamodel.add(webxmlApplication);
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication",
				javaProject, progressMonitor);
		// operation
		final ResourceDelta event = createEvent(type.getResource(), ADDED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications: the JAVA Application is the sole element to be really changed
		assertThat(affectedElements.size(), equalTo(1));
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(affectedElements.get(0).getElement().getElementKind(), equalTo(EnumElementKind.APPLICATION_JAVA));
		final JaxrsJavaApplication javaApplication = (JaxrsJavaApplication) affectedElements.get(0).getElement();
		assertThat(javaApplication.getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(javaApplication.getApplicationPath(), equalTo("/hello"));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(2)); // old application (web.xml) + new one (java)
		verify(metamodel, times(1)).add(any(JaxrsJavaApplication.class));
	}

	/**
	 * in this test, the webxml exists first, and then a NOT annotated Java Application is added -> it should be
	 * immediately overriden
	 * 
	 * @throws Exception
	 */
	@Test
	public void shouldOverrideJavaApplicationWhenAddingUnannotatedJavaApplication() throws Exception {
		// precondition
		final JaxrsWebxmlApplication webxmlApplication = createWebxmlApplication(
				"org.jboss.tools.ws.jaxrs.sample.services.RestApplication", "/hello");
		metamodel.add(webxmlApplication);
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication",
				javaProject, progressMonitor);
		WorkbenchUtils.replaceFirstOccurrenceOfCode(type.getCompilationUnit(), "@ApplicationPath(\"/app\")", "", false);
		// operation
		final ResourceDelta event = createEvent(type.getResource(), ADDED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications: the JAVA Application is the sole element to be really changed
		assertThat(affectedElements.size(), equalTo(1));
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(affectedElements.get(0).getElement().getElementKind(), equalTo(EnumElementKind.APPLICATION_JAVA));
		final JaxrsJavaApplication javaApplication = (JaxrsJavaApplication) affectedElements.get(0).getElement();
		assertThat(javaApplication.getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(javaApplication.getApplicationPath(), equalTo("/hello"));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(2)); // old application (web.xml) + new one (java)
		verify(metamodel, times(1)).add(any(JaxrsJavaApplication.class));
	}

	/**
	 * In this test, the java application path override should be removed when the web.xml application is removed from
	 * the web.xml file
	 * 
	 * @throws Exception
	 */
	@Test
	public void shouldUnoverrideAnnotatedJavaApplicationWhenRemovingCustomWebxml() throws Exception {
		// precondition
		final JaxrsWebxmlApplication webxmlApplication = createWebxmlApplication(
				"org.jboss.tools.ws.jaxrs.sample.services.RestApplication", "/hello");
		metamodel.add(webxmlApplication);
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication",
				javaProject, progressMonitor);
		final JaxrsJavaApplication javaApplication = createJavaApplication(type);
		metamodel.add(javaApplication);
		assertThat(javaApplication.isOverriden(), equalTo(true));
		// operation
		final ResourceDelta event = createEvent(webxmlApplication.getResource(), REMOVED);
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
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1)); // one (java)
	}

	@Test
	public void shouldUnoverrideUnannotatedJavaApplicationWhenRemovingCustomWebxml() throws Exception {
		// precondition
		final JaxrsWebxmlApplication webxmlApplication = createWebxmlApplication(
				"org.jboss.tools.ws.jaxrs.sample.services.RestApplication", "/hello");
		metamodel.add(webxmlApplication);
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication",
				javaProject, progressMonitor);
		WorkbenchUtils.replaceFirstOccurrenceOfCode(type.getCompilationUnit(), "@ApplicationPath(\"/app\")", "", false);
		final JaxrsJavaApplication javaApplication = createJavaApplication(type);
		metamodel.add(javaApplication);
		assertThat(javaApplication.isOverriden(), equalTo(true));
		// operation
		final ResourceDelta event = createEvent(webxmlApplication.getResource(), REMOVED);
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
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1)); // one (java)
	}

	@Test
	public void shouldNotOverrideJavaApplicationWhenAddingDefaultServletMapping() throws Exception {
		// pre-conditions
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication",
				javaProject, progressMonitor);
		final JaxrsJavaApplication application = createJavaApplication(type);
		metamodel.add(application);
		final IResource webxmlResource = WorkbenchUtils.replaceDeploymentDescriptorWith(javaProject,
				"web-3_0-with-default-servlet-mapping.xml", bundle);
		// operation
		final ResourceDelta event = createEvent(webxmlResource, CHANGED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1));
		final JaxrsWebxmlApplication webxmlApplication = (JaxrsWebxmlApplication) affectedElements.get(0).getElement();
		assertThat(webxmlApplication.getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(webxmlApplication.getApplicationPath(), equalTo("/hello"));
		verify(metamodel, times(1)).add(any(JaxrsWebxmlApplication.class));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(2)); // old application (java) + new one (web.xml)
		assertThat(metamodel.getApplication(), equalTo((IJaxrsApplication) webxmlApplication)); // web.xml based
																								// application precedes
																								// any other java based
																								// JAX-RS Application
																								// element
		assertThat(metamodel.getJavaApplications().get(0).getApplicationPath(), equalTo("/app")); // Java-based
																									// application
																									// configuration
																									// should not be
																									// changed
	}

	/**
	 * In this test, the existing Java Application is not modified when adding a web.xml with default application
	 * configuration, but the resulting webxmlApplication becomes the primary one in the metamodel
	 * 
	 * @throws Exception
	 */
	@Test
	public void shouldPreceedJavaApplicationWhenAddingDefaultWebxmlMapping() throws Exception {
		// in this test, the java-application exists first, and then a web.xml application is added -> it should
		// immediately override the java-one
		// pre-conditions
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication",
				javaProject, progressMonitor);
		final JaxrsJavaApplication javaApplication = createJavaApplication(type);
		metamodel.add(javaApplication);
		final IResource webxmlResource = WorkbenchUtils.replaceDeploymentDescriptorWith(javaProject,
				"web-3_0-with-default-servlet-mapping.xml", bundle);
		// operation
		final ResourceDelta event = createEvent(webxmlResource, ADDED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1));
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(affectedElements.get(0).getElement().getElementKind(), equalTo(EnumElementKind.APPLICATION_WEBXML));
		final JaxrsWebxmlApplication webxmlApplication = (JaxrsWebxmlApplication) affectedElements.get(0).getElement();
		assertThat(webxmlApplication.getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(webxmlApplication.getApplicationPath(), equalTo("/hello"));
		assertThat(javaApplication.getApplicationPath(), equalTo("/app")); // Java-based application configuration
																			// should not be changed
		assertThat(metamodel.getElements(javaProject).size(), equalTo(2)); // old application (java) + new one (web.xml)
		assertThat(metamodel.getApplication(), equalTo((IJaxrsApplication) webxmlApplication)); // old application
																								// (java) + new one
																								// (web.xml)
		verify(metamodel, times(1)).add(any(JaxrsWebxmlApplication.class));
	}

	@Test
	public void shouldRestoreJavaApplicationWhenRemovingDefaultWebxmlMapping() throws Exception {
		// in this test, the java-application exists first, and then a web.xml application is added -> it should
		// immediately override the java-one
		// pre-conditions
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication",
				javaProject, progressMonitor);
		final JaxrsJavaApplication javaApplication = createJavaApplication(type);
		metamodel.add(javaApplication);
		final JaxrsWebxmlApplication webxmlApplication = createWebxmlApplication(
				EnumJaxrsClassname.APPLICATION.qualifiedName, "/hello");
		metamodel.add(webxmlApplication);
		// operation
		final ResourceDelta event = createEvent(webxmlApplication.getResource(), REMOVED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1));
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(affectedElements.get(0).getElement().getElementKind(), equalTo(EnumElementKind.APPLICATION_WEBXML));
		assertThat(javaApplication.getApplicationPath(), equalTo("/app")); // Java-based application configuration
																			// should not be changed
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1)); // java application
		assertThat(metamodel.getApplication(), equalTo((IJaxrsApplication) javaApplication)); // old application (java)
																								// + new one (web.xml)
		verify(metamodel, times(1)).add(any(JaxrsWebxmlApplication.class));
	}

	@Test
	public void shouldRemoveApplicationWhenRemovingAnnotationAndHierarchyAlreadyMissing() throws CoreException {
		// pre-conditions
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication",
				javaProject, progressMonitor);
		WorkbenchUtils.replaceFirstOccurrenceOfCode(type.getCompilationUnit(), "extends Application", "", false);
		metamodel.add(createJavaApplication(type, "/bar"));
		// operation
		WorkbenchUtils.replaceFirstOccurrenceOfCode(type.getCompilationUnit(), "@ApplicationPath(\"/app\")", "", false);
		final ResourceDelta event = createEvent(type.getResource(), CHANGED);
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
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication",
				javaProject, progressMonitor);
		WorkbenchUtils.replaceFirstOccurrenceOfCode(type.getCompilationUnit(), "@ApplicationPath(\"/app\")", "", false);
		metamodel.add(createJavaApplication(type));
		// operation
		WorkbenchUtils.replaceFirstOccurrenceOfCode(type.getCompilationUnit(), "extends Application", "", false);
		final ResourceDelta event = createEvent(type.getResource(), CHANGED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1));
		assertThat(affectedElements.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.APPLICATION));
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(REMOVED));
		verify(metamodel, times(1)).remove(any(JaxrsJavaApplication.class));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(0));

	}

	/**
	 * In this test, the webxml application is changed when the application path is changed, too.
	 * 
	 * @throws Exception
	 */
	@Test
	public void shouldReplaceWebxmlApplicationWhenChangingApplicationClassName() throws Exception {
		// pre-conditions
		final IResource webxmlResource = WorkbenchUtils.replaceDeploymentDescriptorWith(javaProject,
				"web-3_0-with-default-servlet-mapping.xml", bundle);
		metamodel.add(createWebxmlApplication("bar.foo", "/foo"));
		// operation
		final ResourceDelta event = createEvent(webxmlResource, CHANGED);
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
		verify(metamodel, times(2)).add(any(JaxrsWebxmlApplication.class)); // initial app added + new app added, too
		verify(metamodel, times(1)).remove(any(JaxrsWebxmlApplication.class)); // initial app removed
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	/**
	 * In this test, the webxml application is changed when the application path is changed, too.
	 * 
	 * @throws Exception
	 */
	@Test
	public void shouldChangeWebxmlApplicationWhenChangingApplicationPathValue() throws Exception {
		// pre-conditions
		final IResource webxmlResource = WorkbenchUtils.replaceDeploymentDescriptorWith(javaProject,
				"web-3_0-with-default-servlet-mapping.xml", bundle);
		metamodel.add(createWebxmlApplication(EnumJaxrsClassname.APPLICATION.qualifiedName, "/foo"));
		// operation
		// operation
		final ResourceDelta event = createEvent(webxmlResource, CHANGED);
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
				"web-3_0-with-invalid-servlet-mapping.xml", bundle);
		// metamodel.add(createApplication("/foo"));
		// operation
		// operation
		final ResourceDelta event = createEvent(webxmlResource, CHANGED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(0));
	}

	@Test
	public void shouldRemoveWebxmlApplicationWhenChangingWebxml() throws Exception {
		// pre-conditions
		metamodel.add(createWebxmlApplication(EnumJaxrsClassname.APPLICATION.qualifiedName, "/hello"));
		final IResource webxmlResource = WorkbenchUtils.replaceDeploymentDescriptorWith(javaProject,
				"web-3_0-without-servlet-mapping.xml", bundle);
		// operation
		final ResourceDelta event = createEvent(webxmlResource, CHANGED);
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
		metamodel.add(application);
		final IResource webxmlResource = WorkbenchUtils.replaceDeploymentDescriptorWith(javaProject,
				"web-3_0-with-default-servlet-mapping.xml", bundle);
		// operation
		webxmlResource.delete(true, progressMonitor);
		final ResourceDelta event = createEvent(webxmlResource, REMOVED);
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
				"web-3_0-with-servlet-mapping.xml", bundle);
		final JaxrsWebxmlApplication application = createWebxmlApplication(
				EnumJaxrsClassname.APPLICATION.qualifiedName, "/hello");
		metamodel.add(application);

		// operation
		final IContainer webInfFolder = webxmlResource.getParent();
		webInfFolder.delete(IResource.FORCE, progressMonitor);
		final ResourceDelta event = createEvent(webInfFolder, REMOVED);
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
		final IPackageFragmentRoot lib = WorkbenchUtils.getPackageFragmentRoot(javaProject,
				"lib/jaxrs-api-2.0.1.GA.jar", progressMonitor);
		// let's suppose that this jar only contains 1 HTTP Methods ;-)
		final IType type = JdtUtils.resolveType("javax.ws.rs.GET", javaProject, progressMonitor);
		final Annotation annotation = getAnnotation(type, HTTP_METHOD.qualifiedName);
		final JaxrsHttpMethod httpMethod = new JaxrsHttpMethod.Builder(type, metamodel).httpMethod(annotation).build();
		metamodel.add(httpMethod);
		// operation
		final ResourceDelta event = createEvent(lib.getResource(), REMOVED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(0));
	}

	@Test
	public void shouldAddHttpMethodWhenChangingResource() throws CoreException {
		// pre-conditions
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject,
				progressMonitor);
		final Annotation annotation = getAnnotation(type, HTTP_METHOD.qualifiedName);
		// operation
		final ResourceDelta event = createEvent(annotation.getJavaParent().getResource(), CHANGED);
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
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject,
				progressMonitor);
		final JaxrsHttpMethod httpMethod = createHttpMethod(type, "bar");
		metamodel.add(httpMethod);
		// operation
		final ResourceDelta event = createEvent(type.getResource(), CHANGED);
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
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject,
				progressMonitor);
		final JaxrsHttpMethod httpMethod = createHttpMethod(type);
		metamodel.add(httpMethod);
		final Annotation annotation = getAnnotation(type, TARGET.qualifiedName);
		// operation
		WorkbenchUtils.delete(annotation.getJavaAnnotation(), false);
		final ResourceDelta event = createEvent(annotation.getJavaParent().getResource(), CHANGED);
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
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject,
				progressMonitor);
		final JaxrsHttpMethod httpMethod = createHttpMethod(type);
		metamodel.add(httpMethod);
		final Annotation annotation = getAnnotation(type, TARGET.qualifiedName);
		// operation
		WorkbenchUtils.addTypeAnnotation(type, "@Deprecated", false);
		final ResourceDelta event = createEvent(annotation.getJavaParent().getResource(), CHANGED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(0));
	}

	@Test
	public void shouldRemoveHttpMethodWhenChangingResource() throws CoreException {
		// pre-conditions
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject,
				progressMonitor);
		metamodel.add(createHttpMethod(type));
		final Annotation annotation = getAnnotation(type, HTTP_METHOD.qualifiedName);
		// operation
		WorkbenchUtils.delete(annotation.getJavaAnnotation(), false);
		final ResourceDelta event = createEvent(annotation.getJavaParent().getResource(), CHANGED);
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
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject,
				progressMonitor);
		final Annotation annotation = getAnnotation(type, HTTP_METHOD.qualifiedName);
		final JaxrsHttpMethod httpMethod = new JaxrsHttpMethod.Builder(type, metamodel).httpMethod(annotation).build();
		metamodel.add(httpMethod);
		// operation
		final ResourceDelta event = createEvent(type.getResource(), REMOVED);
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
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject,
				progressMonitor);
		final Annotation annotation = getAnnotation(type, HTTP_METHOD.qualifiedName);
		final JaxrsHttpMethod httpMethod = new JaxrsHttpMethod.Builder(type, metamodel).httpMethod(annotation).build();
		metamodel.add(httpMethod);
		// operation
		WorkbenchUtils.delete(type);
		final ResourceDelta event = createEvent(type.getResource(), CHANGED);
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
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject,
				progressMonitor);
		final Annotation annotation = getAnnotation(type, HTTP_METHOD.qualifiedName);
		final JaxrsHttpMethod httpMethod = new JaxrsHttpMethod.Builder(type, metamodel).httpMethod(annotation).build();
		metamodel.add(httpMethod);
		final IPackageFragmentRoot sourceFolder = WorkbenchUtils.getPackageFragmentRoot(javaProject, "src/main/java",
				progressMonitor);
		// operation
		final ResourceDelta event = createEvent(sourceFolder.getResource(), REMOVED);
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
		metamodel.add(createHttpMethod(GET));
		metamodel.add(createHttpMethod(POST));
		metamodel.add(createHttpMethod(PUT));
		metamodel.add(createHttpMethod(DELETE));
		// operation
		IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final ResourceDelta event = createEvent(type.getResource(), ADDED);
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
		metamodel.add(createHttpMethod(GET));
		metamodel.add(createHttpMethod(POST));
		metamodel.add(createHttpMethod(PUT));
		metamodel.add(createHttpMethod(DELETE));
		metamodel.add(createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource"));
		final IType customerType = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		// operation
		final ResourceDelta event = createEvent(customerType.getResource(), CHANGED);
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
		metamodel.add(createHttpMethod(GET));
		metamodel.add(createHttpMethod(POST));
		metamodel.add(createHttpMethod(PUT));
		metamodel.add(createHttpMethod(DELETE));
		metamodel.add(createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource"));
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		metamodel.add(resource);
		resource.removeAnnotation(resource.getProducesAnnotation());
		// operation
		final ResourceDelta event = createEvent(resource.getJavaElement().getResource(), CHANGED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1)); // 1 resource
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(((JaxrsResource) affectedElements.get(0).getElement()), equalTo(resource));
		// 4 HttpMethods + 2 resources (including their methods and fields)
		assertThat(metamodel.getElements(javaProject).size(), equalTo(15));
	}

	@Test
	public void shouldAddResourceMethodWhenChangingResource() throws CoreException {
		// pre-conditions
		metamodel.add(createHttpMethod(GET));
		metamodel.add(createHttpMethod(POST));
		metamodel.add(createHttpMethod(PUT));
		metamodel.add(createHttpMethod(DELETE));
		final JaxrsResource bookResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		bookResource.removeMethod(bookResource.getAllMethods().get(0));
		metamodel.add(bookResource);
		// operation
		final ResourceDelta event = createEvent(bookResource.getJavaElement().getResource(), CHANGED);
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
		metamodel.add(createHttpMethod(GET));
		metamodel.add(createHttpMethod(POST));
		metamodel.add(createHttpMethod(PUT));
		metamodel.add(createHttpMethod(DELETE));
		final JaxrsResource bookResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		metamodel.add(bookResource);
		// operation
		// operation
		for (Iterator<JaxrsResourceMethod> iterator = bookResource.getMethods().values().iterator(); iterator.hasNext();) {
			JaxrsResourceMethod resourceMethod = iterator.next();
			if (resourceMethod.getElementKind() == EnumElementKind.SUBRESOURCE_METHOD) {
				WorkbenchUtils.replaceFirstOccurrenceOfCode(resourceMethod.getJavaElement(), "@Path(\"/{id}\")",
						"@Path(\"/{foo}\")", false);
				WorkbenchUtils.delete(resourceMethod.getHttpMethodAnnotation().getJavaAnnotation(), false);
			}
		}
		final ResourceDelta event = createEvent(bookResource.getJavaElement().getResource(), CHANGED);
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
		metamodel.add(createHttpMethod(GET));
		metamodel.add(createHttpMethod(POST));
		metamodel.add(createHttpMethod(PUT));
		metamodel.add(createHttpMethod(DELETE));
		final JaxrsResource bookResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		metamodel.add(bookResource);
		// operation
		for (Iterator<JaxrsResourceMethod> iterator = bookResource.getMethods().values().iterator(); iterator.hasNext();) {
			JaxrsResourceMethod resourceMethod = iterator.next();
			if (resourceMethod.getElementKind() == EnumElementKind.RESOURCE_METHOD) {
				WorkbenchUtils.delete(resourceMethod.getHttpMethodAnnotation().getJavaAnnotation(), false);
			}
		}
		final ResourceDelta event = createEvent(bookResource.getJavaElement().getResource(), CHANGED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1)); // 1 resource method
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(affectedElements.get(0).getElement().getElementCategory(),
				equalTo(EnumElementCategory.RESOURCE_METHOD));
		// 4 HttpMethods + 1 resource (including their remaining methods and fields)
		assertThat(metamodel.getElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldAddResourceFieldWhenChangingResource() throws CoreException {
		// pre-conditions
		metamodel.add(createHttpMethod(GET));
		metamodel.add(createHttpMethod(POST));
		metamodel.add(createHttpMethod(PUT));
		metamodel.add(createHttpMethod(DELETE));
		final JaxrsResource productResourceLocator = createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		productResourceLocator.removeField(productResourceLocator.getAllFields().get(0));
		metamodel.add(productResourceLocator);
		// operation
		final ResourceDelta event = createEvent(productResourceLocator.getJavaElement().getResource(), CHANGED);
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
		metamodel.add(createHttpMethod(GET));
		metamodel.add(createHttpMethod(POST));
		metamodel.add(createHttpMethod(PUT));
		metamodel.add(createHttpMethod(DELETE));
		final JaxrsResource productResourceLocator = createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		metamodel.add(productResourceLocator);
		// operation
		// operation
		for (Iterator<JaxrsResourceField> iterator = productResourceLocator.getFields().values().iterator(); iterator
				.hasNext();) {
			JaxrsResourceField resourceField = iterator.next();
			if (resourceField.getDefaultValueAnnotation() != null) {
				WorkbenchUtils.replaceFirstOccurrenceOfCode(resourceField.getJavaElement(), "@DefaultValue(\"foo!\")",
						"@DefaultValue(\"bar\")", false);
			}
		}
		final ResourceDelta event = createEvent(productResourceLocator.getJavaElement().getResource(), CHANGED);
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
		metamodel.add(createHttpMethod(GET));
		metamodel.add(createHttpMethod(POST));
		metamodel.add(createHttpMethod(PUT));
		metamodel.add(createHttpMethod(DELETE));
		final JaxrsResource productResourceLocator = createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		metamodel.add(productResourceLocator);
		// operation
		for (Iterator<JaxrsResourceField> iterator = productResourceLocator.getFields().values().iterator(); iterator
				.hasNext();) {
			JaxrsResourceField resourceField = iterator.next();
			if (resourceField.getQueryParamAnnotation() != null) {
				WorkbenchUtils.delete(resourceField.getQueryParamAnnotation().getJavaAnnotation(), false);
			}
		}
		final ResourceDelta event = createEvent(productResourceLocator.getJavaElement().getResource(), CHANGED);
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
		metamodel.add(createHttpMethod(GET));
		metamodel.add(createHttpMethod(POST));
		metamodel.add(createHttpMethod(PUT));
		metamodel.add(createHttpMethod(DELETE));
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		metamodel.add(resource);
		// operation
		for (IMethod method : resource.getJavaElement().getMethods()) {
			WorkbenchUtils.delete(method);
		}
		final ResourceDelta event = createEvent(resource.getJavaElement().getResource(), CHANGED);
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
		metamodel.add(createHttpMethod(GET));
		metamodel.add(createHttpMethod(POST));
		metamodel.add(createHttpMethod(PUT));
		metamodel.add(createHttpMethod(DELETE));
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		metamodel.add(resource);
		// operation
		final ResourceDelta event = createEvent(resource.getJavaElement().getResource(), REMOVED);
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
		metamodel.add(createHttpMethod(GET));
		metamodel.add(createHttpMethod(POST));
		metamodel.add(createHttpMethod(PUT));
		metamodel.add(createHttpMethod(DELETE));
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		metamodel.add(resource);
		// operation
		WorkbenchUtils.delete(resource.getJavaElement());
		final ResourceDelta event = createEvent(resource.getJavaElement().getResource(), CHANGED);
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
		final IPackageFragmentRoot sourceFolder = WorkbenchUtils.getPackageFragmentRoot(javaProject, "src/main/java",
				progressMonitor);
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final Annotation annotation = getAnnotation(type, CONSUMES.qualifiedName);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(annotation).build();
		metamodel.add(resource);
		// operation
		final ResourceDelta event = createEvent(sourceFolder.getResource(), REMOVED);
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
		metamodel.add(createHttpMethod(GET));
		metamodel.add(createHttpMethod(POST));
		metamodel.add(createHttpMethod(PUT));
		metamodel.add(createHttpMethod(DELETE));
		final JaxrsResource resourceLocator = createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		metamodel.add(resourceLocator);
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
		final ResourceDelta event = createEvent(resourceLocator.getJavaElement().getResource(), CHANGED);
		final List<JaxrsElementDelta> affectedElements = processResourceChanges(event, progressMonitor);
		// verifications
		assertThat(affectedElements.size(), equalTo(1)); // 1 resource method
		assertThat(affectedElements.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(affectedElements.get(0).getElement().getElementCategory(), equalTo(EnumElementCategory.RESOURCE));
		// 4 HttpMethods left only
		assertThat(metamodel.getElements(javaProject).size(), equalTo(4));
	}

}
