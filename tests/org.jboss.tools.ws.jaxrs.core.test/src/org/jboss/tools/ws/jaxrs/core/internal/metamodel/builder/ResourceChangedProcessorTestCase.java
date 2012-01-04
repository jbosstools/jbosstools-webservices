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
import static org.jboss.tools.ws.jaxrs.core.WorkbenchUtils.getAnnotation;
import static org.jboss.tools.ws.jaxrs.core.WorkbenchUtils.getType;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;

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
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceField;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsHttpMethod;
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
	}

	/**
	 * @return
	 * @throws CoreException
	 * @throws JavaModelException
	 */
	private JaxrsHttpMethod createHttpMethod(Class<?> httpClass) throws CoreException, JavaModelException {
		final IType httpMethodType = JdtUtils.resolveType(httpClass.getName(), javaProject, progressMonitor);
		final Annotation httpMethodAnnotation = getAnnotation(httpMethodType, HttpMethod.class);
		final JaxrsHttpMethod httpMethod = new JaxrsHttpMethod(httpMethodType, httpMethodAnnotation, metamodel);
		return httpMethod;
	}

	private JaxrsHttpMethod createHttpMethod(IType type) throws JavaModelException {
		final Annotation annotation = getAnnotation(type, HttpMethod.class);
		final JaxrsHttpMethod httpMethod = new JaxrsHttpMethod(type, annotation, metamodel);
		return httpMethod;
	}

	private JaxrsHttpMethod createHttpMethod(IType type, String httpVerb) throws JavaModelException {
		final Annotation annotation = getAnnotation(type, HttpMethod.class, httpVerb);
		final JaxrsHttpMethod httpMethod = new JaxrsHttpMethod(type, annotation, metamodel);
		return httpMethod;
	}

	private JaxrsResource createResource(String fileName) throws CoreException {
		final IType type = getType(fileName, javaProject);
		final JaxrsResource resource = new JaxrsElementFactory().createResource(type, JdtUtils.parse(type, null),
				metamodel);
		return resource;
	}

	private ResourceChangedEvent createEvent(IResource resource, int deltaKind) {
		return new ResourceChangedEvent(resource, deltaKind, NO_FLAG);
	}

	private List<JaxrsElementChangedEvent> processEvent(ResourceChangedEvent event, IProgressMonitor progressmonitor) {
		return processor.processEvents(Arrays.asList(event), progressmonitor);
	}

	/**
	 * Because sometimes, generics are painful...
	 * 
	 * @param elements
	 * @return private List<IJaxrsElement<?>> asList(IJaxrsElement<?>...
	 *         elements) { final List<IJaxrsElement<?>> result = new
	 *         ArrayList<IJaxrsElement<?>>();
	 *         result.addAll(Arrays.asList(elements)); return result; }
	 */

	@Test
	public void shouldAddHttpMethodsAndResourcesWhenAddingSourceFolder() throws CoreException {
		// pre-conditions
		metamodel.add(createHttpMethod(GET.class));
		metamodel.add(createHttpMethod(POST.class));
		metamodel.add(createHttpMethod(PUT.class));
		metamodel.add(createHttpMethod(DELETE.class));
		// operation
		final IPackageFragmentRoot sourceFolder = WorkbenchUtils.getPackageFragmentRoot(javaProject, "src/main/java",
				progressMonitor);
		final ResourceChangedEvent event = createEvent(sourceFolder.getResource(), ADDED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		// 1 HttpMethod + 3 RootResources + 2 Subresources
		assertThat(impacts.size(), equalTo(6));
		assertThat(impacts, everyItem(Matchers.<JaxrsElementChangedEvent> hasProperty("deltaKind", equalTo(ADDED))));
		// all HttpMethods, Resources, ResourceMethods and ResourceFields
		assertThat(metamodel.getElements(javaProject).size(), equalTo(26));
	}

	@Test
	public void shouldAdd6HttpMethodsAnd0ResourceWhenAddingBinaryLib() throws CoreException {
		// pre-conditions
		final IPackageFragmentRoot lib = WorkbenchUtils.getPackageFragmentRoot(javaProject,
				"lib/jaxrs-api-2.0.1.GA.jar", progressMonitor);
		// operation
		final ResourceChangedEvent event = createEvent(lib.getResource(), ADDED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications. Damned : none in the jar...
		assertThat(impacts.size(), equalTo(6));
		assertThat(impacts, everyItem(Matchers.<JaxrsElementChangedEvent> hasProperty("deltaKind", equalTo(ADDED))));
		verify(metamodel, times(6)).add(any(JaxrsHttpMethod.class));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(6));
	}

	@Test
	public void shouldAddHttpMethodWhenAddingSourceCompilationUnit() throws CoreException {
		// pre-conditions
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject,
				progressMonitor);
		// operation
		final ResourceChangedEvent event = createEvent(type.getCompilationUnit().getResource(), ADDED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getElement().getElementKind(), equalTo(EnumElementKind.HTTP_METHOD));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldAddHttpMethodWhenChangingResource() throws CoreException {
		// pre-conditions
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject,
				progressMonitor);
		final Annotation annotation = getAnnotation(type, HttpMethod.class);
		// operation
		final ResourceChangedEvent event = createEvent(annotation.getJavaParent().getResource(), CHANGED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getElement().getElementKind(), equalTo(EnumElementKind.HTTP_METHOD));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(((IJaxrsHttpMethod) impacts.get(0).getElement()).getHttpVerb(), equalTo("FOO"));
		verify(metamodel, times(1)).add(any(JaxrsHttpMethod.class));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldChangeHttpMethodWhenChangingResource() throws CoreException {
		// pre-conditions
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject,
				progressMonitor);
		metamodel.add(createHttpMethod(type, "bar"));
		// operation
		final ResourceChangedEvent event = createEvent(type.getResource(), CHANGED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getElement().getElementKind(), equalTo(EnumElementKind.HTTP_METHOD));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(((IJaxrsHttpMethod) impacts.get(0).getElement()).getHttpVerb(), equalTo("FOO"));
		verify(metamodel, times(1)).add(any(JaxrsHttpMethod.class));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(1));
	}

	@Test
	public void shouldRemoveHttpMethodWhenChangingResource() throws CoreException {
		// pre-conditions
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject,
				progressMonitor);
		metamodel.add(createHttpMethod(type));
		final Annotation annotation = getAnnotation(type, HttpMethod.class);
		// operation
		WorkbenchUtils.delete(annotation.getJavaAnnotation(), false);
		final ResourceChangedEvent event = createEvent(annotation.getJavaParent().getResource(), CHANGED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getElement().getElementKind(), equalTo(EnumElementKind.HTTP_METHOD));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((IJaxrsHttpMethod) impacts.get(0).getElement()).getHttpVerb(), equalTo("FOO"));
		verify(metamodel, times(1)).add(any(JaxrsHttpMethod.class));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(0));
	}

	@Test
	public void shouldRemoveHttpMethodWhenRemovingCompilationUnit() throws CoreException {
		// pre-conditions
		// JaxrsMetamodel metamodel = new JaxrsMetamodel(javaProject);
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject,
				progressMonitor);
		final Annotation annotation = getAnnotation(type, HttpMethod.class);
		final JaxrsHttpMethod httpMethod = new JaxrsHttpMethod(type, annotation, metamodel);
		metamodel.add(httpMethod);
		// operation
		final ResourceChangedEvent event = createEvent(type.getResource(), REMOVED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getElement().getElementKind(), equalTo(EnumElementKind.HTTP_METHOD));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((JaxrsHttpMethod) impacts.get(0).getElement()), equalTo(httpMethod));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(0));
	}

	@Test
	public void shouldRemoveHttpMethodWhenRemovingSourceType() throws CoreException {
		// pre-conditions
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject,
				progressMonitor);
		final Annotation annotation = getAnnotation(type, HttpMethod.class);
		final JaxrsHttpMethod httpMethod = new JaxrsHttpMethod(type, annotation, metamodel);
		metamodel.add(httpMethod);
		// operation
		WorkbenchUtils.delete(type);
		final ResourceChangedEvent event = createEvent(type.getResource(), CHANGED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getElement().getElementKind(), equalTo(EnumElementKind.HTTP_METHOD));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((IJaxrsHttpMethod) impacts.get(0).getElement()).getHttpVerb(), equalTo("FOO"));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(0));
	}

	@Test
	public void shouldRemoveHttpMethodWhenRemovingSourceFolder() throws CoreException {
		// pre-conditions
		final IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject,
				progressMonitor);
		final Annotation annotation = getAnnotation(type, HttpMethod.class);
		final JaxrsHttpMethod httpMethod = new JaxrsHttpMethod(type, annotation, metamodel);
		metamodel.add(httpMethod);
		final IPackageFragmentRoot sourceFolder = WorkbenchUtils.getPackageFragmentRoot(javaProject, "src/main/java",
				progressMonitor);
		// operation
		final ResourceChangedEvent event = createEvent(sourceFolder.getResource(), REMOVED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getElement().getElementKind(), equalTo(EnumElementKind.HTTP_METHOD));
		assertThat(impacts.get(0).getElement(), is(notNullValue()));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(0));
	}

	@Test
	public void shouldRemoveHttpMethodWhenRemovingBinaryLib() throws CoreException {
		// pre-conditions
		final IPackageFragmentRoot lib = WorkbenchUtils.getPackageFragmentRoot(javaProject,
				"lib/jaxrs-api-2.0.1.GA.jar", progressMonitor);
		// let's suppose that this jar only contains 1 HTTP Methods ;-)
		final IType type = JdtUtils.resolveType("javax.ws.rs.GET", javaProject, progressMonitor);
		final Annotation annotation = getAnnotation(type, HttpMethod.class);
		final JaxrsHttpMethod httpMethod = new JaxrsHttpMethod(type, annotation, metamodel);
		metamodel.add(httpMethod);
		// operation
		final ResourceChangedEvent event = createEvent(lib.getResource(), REMOVED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getElement().getElementKind(), equalTo(EnumElementKind.HTTP_METHOD));
		assertThat(impacts, everyItem(Matchers.<JaxrsElementChangedEvent> hasProperty("deltaKind", equalTo(REMOVED))));
		assertThat(metamodel.getElements(javaProject).size(), equalTo(0));
	}

	@Test
	public void shouldAddResourceWhenAddingSourceCompilationUnit() throws CoreException {
		// pre-conditions
		metamodel.add(createHttpMethod(GET.class));
		metamodel.add(createHttpMethod(POST.class));
		metamodel.add(createHttpMethod(PUT.class));
		metamodel.add(createHttpMethod(DELETE.class));
		// operation
		IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final ResourceChangedEvent event = createEvent(type.getResource(), ADDED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1)); // 1 resource
		assertThat(impacts.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(impacts.get(0).getElement().getElementKind(), equalTo(EnumElementKind.RESOURCE));
		// HttpMethods, Resource, ResourceMethods and ResourceFields
		assertThat(metamodel.getElements(javaProject).size(), equalTo(11));
	}

	@Test
	public void shouldAddResourceWhenChangingResource() throws CoreException {
		// pre-conditions
		metamodel.add(createHttpMethod(GET.class));
		metamodel.add(createHttpMethod(POST.class));
		metamodel.add(createHttpMethod(PUT.class));
		metamodel.add(createHttpMethod(DELETE.class));
		metamodel.add(createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource"));
		final IType customerType = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		// operation
		final ResourceChangedEvent event = createEvent(customerType.getResource(), CHANGED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1)); // 1 resource
		assertThat(impacts.get(0).getDeltaKind(), equalTo(ADDED));
		// 4 HttpMethods + 2 resources (including their methods and fields)
		assertThat(metamodel.getElements(javaProject).size(), equalTo(15));
	}

	@Test
	public void shouldChangeExistingResourceWhenChangingResource() throws CoreException {
		// pre-conditions
		metamodel.add(createHttpMethod(GET.class));
		metamodel.add(createHttpMethod(POST.class));
		metamodel.add(createHttpMethod(PUT.class));
		metamodel.add(createHttpMethod(DELETE.class));
		metamodel.add(createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource"));
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		metamodel.add(resource);
		resource.removeAnnotation(resource.getProducesAnnotation());
		// operation
		final ResourceChangedEvent event = createEvent(resource.getJavaElement().getResource(), CHANGED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1)); // 1 resource
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(((JaxrsResource) impacts.get(0).getElement()), equalTo(resource));
		// 4 HttpMethods + 2 resources (including their methods and fields)
		assertThat(metamodel.getElements(javaProject).size(), equalTo(15));
	}

	@Test
	public void shouldAddResourceMethodWhenChangingResource() throws CoreException {
		// pre-conditions
		metamodel.add(createHttpMethod(GET.class));
		metamodel.add(createHttpMethod(POST.class));
		metamodel.add(createHttpMethod(PUT.class));
		metamodel.add(createHttpMethod(DELETE.class));
		final JaxrsResource bookResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		bookResource.removeMethod(bookResource.getAllMethods().get(0));
		metamodel.add(bookResource);
		// operation
		final ResourceChangedEvent event = createEvent(bookResource.getJavaElement().getResource(), CHANGED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1)); // 1 resource method
		assertThat(impacts.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(impacts.get(0).getElement().getElementKind(), equalTo(EnumElementKind.RESOURCE_METHOD));
		// 4 HttpMethods + 2 resources (including their methods and fields)
		assertThat(metamodel.getElements(javaProject).size(), equalTo(8));
	}

	@Test
	public void shouldChangeResourceMethodWhenChangingResource() throws CoreException {
		// pre-conditions
		metamodel.add(createHttpMethod(GET.class));
		metamodel.add(createHttpMethod(POST.class));
		metamodel.add(createHttpMethod(PUT.class));
		metamodel.add(createHttpMethod(DELETE.class));
		final JaxrsResource bookResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		metamodel.add(bookResource);
		// operation
		// operation
		for (Iterator<JaxrsResourceMethod> iterator = bookResource.getMethods().values().iterator(); iterator.hasNext();) {
			JaxrsResourceMethod resourceMethod = iterator.next();
			if (resourceMethod.getKind() == EnumKind.SUBRESOURCE_METHOD) {
				WorkbenchUtils.replaceFirstOccurrenceOfCode(resourceMethod.getJavaElement(), "@Path(\"/{id}\")",
						"@Path(\"/{foo}\")", false);
				WorkbenchUtils.delete(resourceMethod.getHttpMethodAnnotation().getJavaAnnotation(), false);
			}
		}
		final ResourceChangedEvent event = createEvent(bookResource.getJavaElement().getResource(), CHANGED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(2)); // 2 resource methods
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getElement().getElementKind(), equalTo(EnumElementKind.RESOURCE_METHOD));
		assertThat(impacts.get(1).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(1).getElement().getElementKind(), equalTo(EnumElementKind.RESOURCE_METHOD));
		// 4 HttpMethods + 2 resources (including their methods and fields)
		assertThat(metamodel.getElements(javaProject).size(), equalTo(8));
	}

	@Test
	public void shouldRemoveResourceMethodWhenChangingResource() throws CoreException {
		// pre-conditions
		metamodel.add(createHttpMethod(GET.class));
		metamodel.add(createHttpMethod(POST.class));
		metamodel.add(createHttpMethod(PUT.class));
		metamodel.add(createHttpMethod(DELETE.class));
		final JaxrsResource bookResource = createResource("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		metamodel.add(bookResource);
		// operation
		for (Iterator<JaxrsResourceMethod> iterator = bookResource.getMethods().values().iterator(); iterator.hasNext();) {
			JaxrsResourceMethod resourceMethod = iterator.next();
			if (resourceMethod.getKind() == EnumKind.RESOURCE_METHOD) {
				WorkbenchUtils.delete(resourceMethod.getHttpMethodAnnotation().getJavaAnnotation(), false);
			}
		}
		final ResourceChangedEvent event = createEvent(bookResource.getJavaElement().getResource(), CHANGED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1)); // 1 resource method
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(impacts.get(0).getElement().getElementKind(), equalTo(EnumElementKind.RESOURCE_METHOD));
		// 4 HttpMethods + 1 resource (including their remaining methods and fields)
		assertThat(metamodel.getElements(javaProject).size(), equalTo(7));
	}

	@Test
	public void shouldAddResourceFieldWhenChangingResource() throws CoreException {
		// pre-conditions
		metamodel.add(createHttpMethod(GET.class));
		metamodel.add(createHttpMethod(POST.class));
		metamodel.add(createHttpMethod(PUT.class));
		metamodel.add(createHttpMethod(DELETE.class));
		final JaxrsResource productResourceLocator = createResource("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		productResourceLocator.removeField(productResourceLocator.getAllFields().get(0));
		metamodel.add(productResourceLocator);
		// operation
		final ResourceChangedEvent event = createEvent(productResourceLocator.getJavaElement().getResource(), CHANGED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1)); // 1 resource field
		assertThat(impacts.get(0).getDeltaKind(), equalTo(ADDED));
		assertThat(impacts.get(0).getElement().getElementKind(), equalTo(EnumElementKind.RESOURCE_FIELD));
		// 4 HttpMethods + 2 resources (including their methods and fields)
		assertThat(metamodel.getElements(javaProject).size(), equalTo(9));
	}

	@Test
	public void shouldChangeResourceFieldWhenChangingResource() throws CoreException {
		// pre-conditions
		metamodel.add(createHttpMethod(GET.class));
		metamodel.add(createHttpMethod(POST.class));
		metamodel.add(createHttpMethod(PUT.class));
		metamodel.add(createHttpMethod(DELETE.class));
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
		final ResourceChangedEvent event = createEvent(productResourceLocator.getJavaElement().getResource(), CHANGED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1)); // 1 resource field
		assertThat(impacts.get(0).getDeltaKind(), equalTo(CHANGED));
		assertThat(impacts.get(0).getElement().getElementKind(), equalTo(EnumElementKind.RESOURCE_FIELD));
		// 4 HttpMethods + 2 resources (including their methods and fields)
		assertThat(metamodel.getElements(javaProject).size(), equalTo(9));
	}

	@Test
	public void shouldRemoveResourceFieldWhenChangingResource() throws CoreException {
		// pre-conditions
		metamodel.add(createHttpMethod(GET.class));
		metamodel.add(createHttpMethod(POST.class));
		metamodel.add(createHttpMethod(PUT.class));
		metamodel.add(createHttpMethod(DELETE.class));
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
		final ResourceChangedEvent event = createEvent(productResourceLocator.getJavaElement().getResource(), CHANGED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1)); // 1 resource field
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(impacts.get(0).getElement().getElementKind(), equalTo(EnumElementKind.RESOURCE_FIELD));
		// 4 HttpMethods + 2 resources (including their methods and fields)
		assertThat(metamodel.getElements(javaProject).size(), equalTo(8));
	}

	@Test
	public void shouldRemoveExistingResourceWhenChangingResource() throws CoreException {
		// pre-conditions
		metamodel.add(createHttpMethod(GET.class));
		metamodel.add(createHttpMethod(POST.class));
		metamodel.add(createHttpMethod(PUT.class));
		metamodel.add(createHttpMethod(DELETE.class));
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		metamodel.add(resource);
		// operation
		for (IMethod method : resource.getJavaElement().getMethods()) {
			WorkbenchUtils.delete(method);
		}
		final ResourceChangedEvent event = createEvent(resource.getJavaElement().getResource(), CHANGED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1)); // 1 resource
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(impacts.get(0).getElement().getElementKind(), equalTo(EnumElementKind.RESOURCE));
		// 4 HttpMethods left only
		assertThat(metamodel.getElements(javaProject).size(), equalTo(4));
	}

	@Test
	public void shouldRemoveResourceWhenRemovingCompilationUnit() throws CoreException {
		// pre-conditions
		metamodel.add(createHttpMethod(GET.class));
		metamodel.add(createHttpMethod(POST.class));
		metamodel.add(createHttpMethod(PUT.class));
		metamodel.add(createHttpMethod(DELETE.class));
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		metamodel.add(resource);
		// operation
		final ResourceChangedEvent event = createEvent(resource.getJavaElement().getResource(), REMOVED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(impacts.get(0).getElement().getElementKind(), equalTo(EnumElementKind.RESOURCE));
		assertThat(((JaxrsResource) impacts.get(0).getElement()), equalTo(resource));
		// 4 HttpMethods left only
		assertThat(metamodel.getElements(javaProject).size(), equalTo(4));
	}

	@Test
	public void shouldRemoveResourceWhenRemovingSourceType() throws CoreException {
		// pre-conditions
		metamodel.add(createHttpMethod(GET.class));
		metamodel.add(createHttpMethod(POST.class));
		metamodel.add(createHttpMethod(PUT.class));
		metamodel.add(createHttpMethod(DELETE.class));
		final JaxrsResource resource = createResource("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		metamodel.add(resource);
		// operation
		WorkbenchUtils.delete(resource.getJavaElement());
		final ResourceChangedEvent event = createEvent(resource.getJavaElement().getResource(), CHANGED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getElement().getElementKind(), equalTo(EnumElementKind.RESOURCE));
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(((JaxrsResource) impacts.get(0).getElement()), equalTo(resource));
		// 4 HttpMethods left only
		assertThat(metamodel.getElements(javaProject).size(), equalTo(4));
	}

	@Test
	public void shouldRemoveResourceWhenRemovingSourceFolder() throws CoreException {
		// pre-conditions
		final IPackageFragmentRoot sourceFolder = WorkbenchUtils.getPackageFragmentRoot(javaProject, "src/main/java",
				progressMonitor);
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject);
		final Annotation annotation = getAnnotation(type, Consumes.class);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(annotation).build();
		metamodel.add(resource);
		// operation
		final ResourceChangedEvent event = createEvent(sourceFolder.getResource(), REMOVED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1));
		assertThat(impacts.get(0).getElement().getElementKind(), equalTo(EnumElementKind.RESOURCE));
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
		metamodel.add(createHttpMethod(GET.class));
		metamodel.add(createHttpMethod(POST.class));
		metamodel.add(createHttpMethod(PUT.class));
		metamodel.add(createHttpMethod(DELETE.class));
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
		final ResourceChangedEvent event = createEvent(resourceLocator.getJavaElement().getResource(), CHANGED);
		final List<JaxrsElementChangedEvent> impacts = processEvent(event, progressMonitor);
		// verifications
		assertThat(impacts.size(), equalTo(1)); // 1 resource method
		assertThat(impacts.get(0).getDeltaKind(), equalTo(REMOVED));
		assertThat(impacts.get(0).getElement().getElementKind(), equalTo(EnumElementKind.RESOURCE));
		// 4 HttpMethods left only
		assertThat(metamodel.getElements(javaProject).size(), equalTo(4));
	}

}
