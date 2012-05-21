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

import static org.eclipse.core.resources.IResourceDelta.CONTENT;
import static org.eclipse.core.resources.IResourceDelta.MARKERS;
import static org.eclipse.jdt.core.ElementChangedEvent.POST_CHANGE;
import static org.eclipse.jdt.core.ElementChangedEvent.POST_RECONCILE;
import static org.eclipse.jdt.core.IJavaElementDelta.ADDED;
import static org.eclipse.jdt.core.IJavaElementDelta.CHANGED;
import static org.eclipse.jdt.core.IJavaElementDelta.F_CONTENT;
import static org.eclipse.jdt.core.IJavaElementDelta.F_REMOVED_FROM_CLASSPATH;
import static org.eclipse.jdt.core.IJavaElementDelta.F_SUPER_TYPES;
import static org.eclipse.jdt.core.IJavaElementDelta.REMOVED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.IJavaElementDeltaFlag.F_MARKER_ADDED;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.IJavaElementDeltaFlag.F_MARKER_REMOVED;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.IJavaElementDeltaFlag.F_SIGNATURE;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JavaElementDelta.NO_FLAG;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.EventObject;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.ui.IEditorPart;
import org.jboss.tools.ws.jaxrs.core.AbstractCommonTestCase;
import org.jboss.tools.ws.jaxrs.core.JBossJaxrsCorePlugin;
import org.jboss.tools.ws.jaxrs.core.WorkbenchTasks;
import org.jboss.tools.ws.jaxrs.core.WorkbenchUtils;
import org.jboss.tools.ws.jaxrs.core.jdt.CompilationUnitsRepository;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaElementDeltaScannerTestCase extends AbstractCommonTestCase {

	private static final Logger LOGGER = LoggerFactory.getLogger(JavaElementDeltaScannerTestCase.class);

	private final CompilationUnitsRepository astRepository = CompilationUnitsRepository.getInstance();

	private static final boolean PRIMARY_COPY = false;

	private static final boolean WORKING_COPY = true;

	private final ElementChangeListener elementChangeListener = new ElementChangeListener();

	private final ResourceChangeListener resourceChangeListener = new ResourceChangeListener();

	private List<JavaElementDelta> javaElementEvents = null;

	private List<ResourceDelta> resourceEvents = null;

	private final class ElementChangeListener implements IElementChangedListener {
		@Override
		public void elementChanged(ElementChangedEvent event) {
			try {
				final JavaElementDeltaScanner scanner = new JavaElementDeltaScanner();
				final List<? extends EventObject> events = scanner.scanAndFilterEvent(event, new NullProgressMonitor());
				for (EventObject e : events) {
					if (e instanceof JavaElementDelta) {
						javaElementEvents.add((JavaElementDelta) e);
					} else {
						fail("Unexpected event type:" + e);
					}
				}
			} catch (CoreException e) {
				LOGGER.error("Failed to scan event {}", event, e);
			}
		}
	}

	private final class ResourceChangeListener implements IResourceChangeListener {

		@Override
		public void resourceChanged(IResourceChangeEvent event) {
			try {
				final ResourceDeltaScanner scanner = new ResourceDeltaScanner();
				final List<ResourceDelta> affectedResources = scanner.scanAndFilterEvent(event.getDelta(),
						new NullProgressMonitor());
				// must explicitly call the add() method to perform verify() on the mocks at the end of the test.
				for (ResourceDelta resourceDelta : affectedResources) {
					resourceEvents.add(resourceDelta);
				}
			} catch (CoreException e) {
				LOGGER.error("Failed to scan event {}", event, e);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Before
	public void setup() throws CoreException {
		JBossJaxrsCorePlugin.getDefault().unregisterListeners();
		Assert.assertNotNull("JavaProject not set", javaProject);
		// ElementChangedEvent.POST_RECONCILE is the only case where the
		// CompilationUnitAST is retrieved
		astRepository.clear();
		// JBossJaxrsCorePlugin.getDefault().unregisterListeners();
		JavaCore.addElementChangedListener(elementChangeListener);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceChangeListener);
		javaElementEvents = Mockito.mock(List.class);
		resourceEvents = Mockito.mock(List.class);
	}

	@After
	public void removeAndRestoreListeners() {
		JavaCore.removeElementChangedListener(elementChangeListener);
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener);
		// JBossJaxrsCorePlugin.getDefault().registerListeners();
	}

	private void verifyEventNotification(IJavaElement element, int deltaKind, int eventType, int flags,
			VerificationMode numberOfTimes) throws JavaModelException {
		LOGGER.info("Verifying method calls..");
		if (element == null) {
			verify(javaElementEvents, numberOfTimes).add(null);
		} else {
			// verify(scanner,
			// numberOfTimes).notifyJavaElementChanged(eq(element),
			// eq(elementKind), eq(deltaKind),
			// any(CompilationUnit.class), eq(flags));
			ICompilationUnit compilationUnit = JdtUtils.getCompilationUnit(element);
			CompilationUnit ast = JdtUtils.parse(compilationUnit, new NullProgressMonitor());
			verify(javaElementEvents, numberOfTimes).add(
					new JavaElementDelta(element, deltaKind, eventType, ast, flags));
		}
	}

	private void verifyEventNotification(IResource resource, int deltaKind, int eventType, int flags,
			VerificationMode numberOfTimes) throws JavaModelException {
		LOGGER.info("Verifying method calls..");
		// verify(scanner,
		// numberOfTimes).notifyJavaElementChanged(eq(element),
		// eq(elementKind), eq(deltaKind),
		// any(CompilationUnit.class), eq(flags));
		verify(resourceEvents, numberOfTimes).add(new ResourceDelta(resource, deltaKind, flags));
	}

	@Test
	public void shouldNotifyWhenEmptyCompilationUnitAdded() throws JavaModelException {
		// operation
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject,
				"EmptyCompilationUnit.txt", "org.jboss.tools.ws.jaxrs.sample.services", "FOO2.java", bundle);
		// verifications:
		verifyEventNotification(compilationUnit.getResource(), ADDED, POST_CHANGE, NO_FLAG, times(1));
	}

	@Test
	public void shouldNotNotifyWhenCompilationUnitChangedInWorkingCopy() throws CoreException {
		// pre-condition
		ICompilationUnit compilationUnit = JdtUtils.resolveType(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject, null).getCompilationUnit();
		// operation
		WorkbenchUtils.appendCompilationUnitType(compilationUnit, "FooBarHTTPMethodMember.txt", bundle, WORKING_COPY);
		// verifications
		verifyEventNotification(compilationUnit, CHANGED, POST_RECONCILE, NO_FLAG, never());
	}

	@Test
	public void shouldNotifyWhenCompilationUnitChangedInPrimaryCopy() throws CoreException {
		// pre-condition
		ICompilationUnit compilationUnit = JdtUtils.resolveType(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject, null).getCompilationUnit();
		// operation
		WorkbenchUtils.appendCompilationUnitType(compilationUnit, "FooBarHTTPMethodMember.txt", bundle, PRIMARY_COPY);
		// verifications
		verifyEventNotification(compilationUnit.getResource(), CHANGED, POST_CHANGE, NO_FLAG, atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenCompilationUnitAddedInPrimaryCopy() throws CoreException {
		// operation
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject, "FooResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "FooResource.java", bundle);
		// verifications: 1 times
		verifyEventNotification(compilationUnit.getResource(), CHANGED, POST_CHANGE, CONTENT, atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenCompilationUnitRemovedInPrimaryCopy() throws CoreException {
		// pre-condition
		ICompilationUnit compilationUnit = JdtUtils.resolveType(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject, null).getCompilationUnit();
		// operation
		WorkbenchUtils.delete(compilationUnit);
		// verifications:
		verifyEventNotification(compilationUnit.getResource(), REMOVED, POST_CHANGE, MARKERS, times(1));
	}

	@Test
	@Ignore("can't produce the right test conditions here")
	public void shouldNotifyWhenResourceRemovedInWorkingCopy() throws CoreException {
		// pre-condition
		ICompilationUnit compilationUnit = JdtUtils
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject, null)
				.getCompilationUnit().getWorkingCopy(null);
		compilationUnit.open(null);
		final CompilationUnit ast = CompilationUnitsRepository.getInstance().getAST(compilationUnit);
		assertThat(ast, notNullValue());
		IEditorPart editorPart = JavaUI.openInEditor(compilationUnit);
		JavaUI.revealInEditor(editorPart, (IJavaElement) compilationUnit);
		// operation
		compilationUnit.getResource().delete(true, null);
		// verifications: 1 time
		verifyEventNotification(compilationUnit, REMOVED, POST_RECONCILE, NO_FLAG, times(2));
	}

	@Test
	public void shouldNotifyWhenResourceRemovedInPrimaryCopy() throws CoreException {
		// pre-condition
		ICompilationUnit compilationUnit = JdtUtils.resolveType(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject, null).getCompilationUnit();
		// operation
		compilationUnit.getResource().delete(true, null);
		// verifications: 1 time
		verifyEventNotification(compilationUnit.getResource(), REMOVED, POST_CHANGE, NO_FLAG, times(1));
	}

	@Test
	public void shouldNotifyWhenTypeAddedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		ICompilationUnit compilationUnit = JdtUtils.resolveType(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject, new NullProgressMonitor())
				.getCompilationUnit();
		// operations
		IType addedType = WorkbenchUtils.appendCompilationUnitType(compilationUnit, "FooResourceMember.txt", bundle,
				WORKING_COPY);
		// verifications:
		verifyEventNotification(addedType, ADDED, POST_RECONCILE, NO_FLAG, times(1));
	}

	@Test
	public void shouldNotifyWhenTypeAddedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		ICompilationUnit compilationUnit = JdtUtils.resolveType(
				"org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject, new NullProgressMonitor())
				.getCompilationUnit();
		// operations
		IType addedType = WorkbenchUtils.appendCompilationUnitType(compilationUnit, "FooResourceMember.txt", bundle,
				PRIMARY_COPY);
		// verifications: one call PostReconcile + one call on PostChange
		verifyEventNotification(addedType.getResource(), CHANGED, POST_CHANGE, CONTENT, atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenTypeRemovedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		// operation
		WorkbenchUtils.removeType(type, WORKING_COPY);
		// verifications
		verifyEventNotification(type, REMOVED, POST_RECONCILE, NO_FLAG, times(1));
	}

	@Test
	public void shouldNotifyWhenTypeRemovedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		// operation
		WorkbenchUtils.removeType(type, PRIMARY_COPY);
		// verifications
		verifyEventNotification(type.getResource(), CHANGED, POST_CHANGE, CONTENT, times(1));
	}

	@Test
	public void shouldNotifyWhenTypeParameterAddedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject,
				"PersistenceExceptionMapperEmptyParameter.txt", "org.jboss.tools.ws.jaxrs.sample.services.providers",
				"PersistenceExceptionMapper.java", bundle);
		// operation
		LOGGER.info("Performing Test Operation(s)...");
		WorkbenchUtils.replaceAllOccurrencesOfCode(compilationUnit, "ExceptionMapper<>",
				"ExceptionMapper<FooException>", WORKING_COPY);
		// verifications
		verifyEventNotification(compilationUnit.findPrimaryType(), CHANGED, POST_RECONCILE, F_SUPER_TYPES, times(1));
	}

	@Test
	public void shouldNotifyWhenTypeParameterAddedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject,
				"PersistenceExceptionMapperEmptyParameter.txt", "org.jboss.tools.ws.jaxrs.sample.services.providers",
				"PersistenceExceptionMapper.java", bundle);
		// operation
		LOGGER.info("Performing Test Operation(s)...");
		WorkbenchUtils.replaceAllOccurrencesOfCode(compilationUnit, "ExceptionMapper<>",
				"ExceptionMapper<FooException>", PRIMARY_COPY);
		// verifications
		verifyEventNotification(compilationUnit.getResource(), CHANGED, POST_CHANGE, CONTENT, atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenTypeInterfaceAddedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType(
				"org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper", javaProject, null);
		// operation
		type = WorkbenchUtils.replaceFirstOccurrenceOfCode(type, "implements", "implements Serializable, ",
				WORKING_COPY);
		// verifications
		verifyEventNotification(type, CHANGED, POST_RECONCILE, F_SUPER_TYPES, times(1));
	}

	@Test
	public void shouldNotifyWhenTypeInterfaceAddedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType(
				"org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper", javaProject, null);
		// operation
		type = WorkbenchUtils.replaceFirstOccurrenceOfCode(type, "implements", "implements Serializable, ",
				PRIMARY_COPY);
		// verifications
		verifyEventNotification(type.getResource(), CHANGED, POST_CHANGE, CONTENT, atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenTypeInterfaceRemovedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType(
				"org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper", javaProject, null);
		// operation
		type = WorkbenchUtils.replaceFirstOccurrenceOfCode(type, "implements ExceptionMapper<EntityNotFoundException>",
				"", WORKING_COPY);
		// verifications
		verifyEventNotification(type, CHANGED, POST_RECONCILE, F_SUPER_TYPES, times(1));
	}

	@Test
	public void shouldNotifyWhenTypeInterfaceRemovedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType(
				"org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper", javaProject, null);
		// operation
		type = WorkbenchUtils.replaceFirstOccurrenceOfCode(type, "implements ExceptionMapper<EntityNotFoundException>",
				"", PRIMARY_COPY);
		// verifications
		verifyEventNotification(type.getResource(), CHANGED, POST_CHANGE, CONTENT, atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenTypeSuperclassAddedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType(
				"org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper", javaProject, null);
		// operation
		type = WorkbenchUtils.replaceFirstOccurrenceOfCode(type, "implements", "extends Object implements",
				WORKING_COPY);
		// verifications
		verifyEventNotification(type, CHANGED, POST_RECONCILE, F_SUPER_TYPES, times(1));
	}

	@Test
	public void shouldNotifyWhenTypeSuperclassAddedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType(
				"org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper", javaProject, null);
		// operation
		type = WorkbenchUtils.replaceFirstOccurrenceOfCode(type, "implements", "extends Object implements",
				PRIMARY_COPY);
		// verifications
		verifyEventNotification(type.getResource(), CHANGED, POST_CHANGE, CONTENT, atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenTypeSuperclassRemovedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.domain.Game", javaProject, null);
		// operation
		type = WorkbenchUtils.replaceFirstOccurrenceOfCode(type, "extends Product", "", WORKING_COPY);
		// verifications
		verifyEventNotification(type, CHANGED, POST_RECONCILE, F_SUPER_TYPES, times(1));
	}

	@Test
	public void shouldNotifyWhenTypeSuperclassRemovedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.domain.Game", javaProject, null);
		// operation
		type = WorkbenchUtils.replaceFirstOccurrenceOfCode(type, "extends Product", "", PRIMARY_COPY);
		// verifications
		verifyEventNotification(type.getResource(), CHANGED, POST_CHANGE, CONTENT, atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenTypeParameterChangedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject,
				"PersistenceExceptionMapper.txt", "org.jboss.tools.ws.jaxrs.sample.services.providers",
				"PersistenceExceptionMapper.java", bundle);
		// operation
		LOGGER.info("Performing Test Operation(s)...");
		WorkbenchUtils.replaceAllOccurrencesOfCode(compilationUnit, "<PersistenceException>", "<FooException>",
				WORKING_COPY);
		// verifications
		verifyEventNotification(compilationUnit.findPrimaryType(), CHANGED, POST_RECONCILE, F_SUPER_TYPES, times(1));
	}

	@Test
	public void shouldNotifyWhenTypeParameterChangedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject,
				"PersistenceExceptionMapper.txt", "org.jboss.tools.ws.jaxrs.sample.services.providers",
				"PersistenceExceptionMapper.java", bundle);
		// operation
		LOGGER.info("Performing Test Operation(s)...");
		WorkbenchUtils.replaceAllOccurrencesOfCode(compilationUnit, "<PersistenceException>", "<FooException>",
				PRIMARY_COPY);
		// verifications
		verifyEventNotification(compilationUnit.getResource(), CHANGED, POST_CHANGE, CONTENT, atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenTypeParameterRemovedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject,
				"PersistenceExceptionMapper.txt", "org.jboss.tools.ws.jaxrs.sample.services.providers",
				"PersistenceExceptionMapper.java", bundle);
		// operation
		LOGGER.info("Performing Test Operation(s)...");
		WorkbenchUtils.replaceAllOccurrencesOfCode(compilationUnit, "<PersistenceException>", "<>", WORKING_COPY);
		// verifications
		verifyEventNotification(compilationUnit.findPrimaryType(), CHANGED, POST_RECONCILE, F_SUPER_TYPES, times(1));
	}

	@Test
	public void shouldNotifyWhenTypeParameterRemovedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject,
				"PersistenceExceptionMapper.txt", "org.jboss.tools.ws.jaxrs.sample.services.providers",
				"PersistenceExceptionMapper.java", bundle);
		// operation
		LOGGER.info("Performing Test Operation(s)...");
		WorkbenchUtils.replaceAllOccurrencesOfCode(compilationUnit, "<PersistenceException>", "<>", PRIMARY_COPY);
		// verifications
		verifyEventNotification(compilationUnit.getResource(), CHANGED, POST_CHANGE, CONTENT, atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenTypeAnnotationAddedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.PurchaseOrderResource",
				javaProject, null);
		// operation
		IAnnotation addedAnnotation = WorkbenchUtils.addTypeAnnotation(type,
				"import javax.ws.rs.Consumes;\n@Consumes(\"foo/bar\")", WORKING_COPY);
		// verifications
		verifyEventNotification(addedAnnotation, ADDED, POST_RECONCILE, NO_FLAG, times(1));
	}

	@Test
	public void shouldNotifyWhenTypeAnnotationAddedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.PurchaseOrderResource",
				javaProject, null);
		// operation
		WorkbenchUtils.addTypeAnnotation(type, "import javax.ws.rs.Consumes;\n@Consumes(\"foo/bar\")", PRIMARY_COPY);
		// verifications
		verifyEventNotification(type.getResource(), CHANGED, POST_CHANGE, CONTENT, atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenTypeAnnotationChangedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		// operation
		type = WorkbenchUtils.replaceFirstOccurrenceOfCode(type, "@Path(CustomerResource.URI_BASE)", "@Path(\"/foo\")",
				WORKING_COPY);
		IAnnotation annotation = type.getAnnotation("Path");
		// verifications
		verifyEventNotification(annotation, CHANGED, POST_RECONCILE, F_CONTENT, times(1));
	}

	@Test
	public void shouldNotifyWhenTypeAnnotationChangedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		// operation
		type = WorkbenchUtils.replaceFirstOccurrenceOfCode(type, "@Path(CustomerResource.URI_BASE)", "@Path(\"/foo\")",
				PRIMARY_COPY);
		// verifications
		verifyEventNotification(type.getResource(), CHANGED, POST_CHANGE, CONTENT, atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenTypeAnnotationRemovedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		// operation
		IAnnotation annotation = type.getAnnotation("Path");
		WorkbenchUtils.removeFirstOccurrenceOfCode(type, "@Path(CustomerResource.URI_BASE)", WORKING_COPY);
		// verifications
		verifyEventNotification(annotation, REMOVED, POST_RECONCILE, NO_FLAG, times(1));
	}

	@Test
	public void shouldNotifyWhenTypeAnnotationRemovedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		// operation
		WorkbenchUtils.removeFirstOccurrenceOfCode(type, "@Path(CustomerResource.URI_BASE)", PRIMARY_COPY);
		// verifications
		verifyEventNotification(type.getResource(), CHANGED, POST_CHANGE, CONTENT, atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenLibraryAddedInClasspath() throws CoreException, InterruptedException {
		// operation
		IPackageFragmentRoot addedEntry = WorkbenchTasks.addClasspathEntry(javaProject, "slf4j-api-1.5.2.jar",
				new NullProgressMonitor());
		// verifications
		verifyEventNotification(addedEntry, ADDED, POST_CHANGE, NO_FLAG, times(1));
	}

	@Test
	public void shouldNotNotifyWhenUnexistingLibraryAddedInClasspath() throws CoreException, InterruptedException {
		// operation
		IPackageFragmentRoot addedEntry = WorkbenchTasks.addClasspathEntry(javaProject, "slf4j-api-1.5.xyz.jar",
				new NullProgressMonitor());
		// verifications
		verifyEventNotification(addedEntry, ADDED, POST_CHANGE, NO_FLAG, times(0));
	}

	@Test
	public void shouldNotifyWhenLibraryRemovedFromClasspath() throws CoreException, InterruptedException {
		// operation
		List<IPackageFragmentRoot> removedEntries = WorkbenchUtils.removeClasspathEntry(javaProject,
				"jaxrs-api-2.0.1.GA.jar", null);
		// verifications
		for (IPackageFragmentRoot removedEntry : removedEntries) {
			verifyEventNotification(removedEntry, REMOVED, POST_CHANGE, F_REMOVED_FROM_CLASSPATH, times(1));
		}
	}

	@Test
	public void shouldNotifyWhenFieldAddedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		// operation
		IField addedField = WorkbenchUtils.createField(type, "private int i", WORKING_COPY);
		// verifications
		verifyEventNotification(addedField, ADDED, POST_RECONCILE, NO_FLAG, times(1));
	}

	@Test
	public void shouldNotifyWhenFieldAddedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		// operation
		WorkbenchUtils.createField(type, "private int i", PRIMARY_COPY);
		// verifications
		verifyEventNotification(type.getResource(), CHANGED, POST_CHANGE, CONTENT, atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenAnnotatedFieldAddedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		// operation
		IField addedField = WorkbenchUtils.createField(type, "@PathParam() private int i", WORKING_COPY);
		// verifications
		verifyEventNotification(addedField, ADDED, POST_RECONCILE, NO_FLAG, times(1));
	}

	@Test
	public void shouldNotifyWhenAnnotatedFieldAddedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		// operation
		IField addedField = WorkbenchUtils.createField(type, "@PathParam() private int i", PRIMARY_COPY);
		// verifications
		verifyEventNotification(addedField.getResource(), CHANGED, POST_CHANGE, CONTENT, atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenFieldNameChangedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		IField oldField = type.getField("entityManager");
		// operation
		WorkbenchUtils.replaceAllOccurrencesOfCode(type.getCompilationUnit(), "entityManager", "em", WORKING_COPY);
		IField newField = type.getField("em");
		// verifications
		verifyEventNotification(oldField, REMOVED, POST_RECONCILE, NO_FLAG, times(1));
		verifyEventNotification(newField, ADDED, POST_RECONCILE, NO_FLAG, times(1));
	}

	@Test
	public void shouldNotifyWhenFieldNameChangedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		// operation
		WorkbenchUtils.replaceAllOccurrencesOfCode(type.getCompilationUnit(), "entityManager", "em", PRIMARY_COPY);
		// verifications
		verifyEventNotification(type.getResource(), CHANGED, POST_CHANGE, CONTENT, atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenFieldTypeChangedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		IField field = type.getField("entityManager");
		// operation
		WorkbenchUtils.replaceAllOccurrencesOfCode(type.getCompilationUnit(), "private EntityManager",
				"private HibernateEntityManager", WORKING_COPY);
		// verifications
		verifyEventNotification(field, CHANGED, POST_RECONCILE, F_CONTENT, times(1));
	}

	@Test
	public void shouldNotifyWhenFieldTypeChangedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		// operation
		WorkbenchUtils.replaceAllOccurrencesOfCode(type.getCompilationUnit(), "private EntityManager",
				"private HibernateEntityManager", PRIMARY_COPY);
		// verifications
		verifyEventNotification(type.getResource(), CHANGED, POST_CHANGE, CONTENT, atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenFieldRemovedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		IField field = type.getField("entityManager");
		// operation
		WorkbenchUtils.removeField(field, WORKING_COPY);
		// verifications
		verifyEventNotification(field, REMOVED, POST_RECONCILE, NO_FLAG, times(1));
	}

	@Test
	public void shouldNotifyWhenFieldRemovedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		IField field = type.getField("entityManager");
		// operation
		WorkbenchUtils.removeField(field, PRIMARY_COPY);
		// verifications
		verifyEventNotification(type.getResource(), CHANGED, POST_CHANGE, CONTENT, atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenFieldAnnotationAddedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		IField field = type.getField("entityManager");
		// operation
		IAnnotation addedAnnotation = WorkbenchUtils.addFieldAnnotation(field, "@PathParam()", WORKING_COPY);
		// verifications
		verifyEventNotification(addedAnnotation, ADDED, POST_RECONCILE, NO_FLAG, times(1));
	}

	@Test
	public void shouldNotifyWhenFieldAnnotationAddedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		IField field = type.getField("entityManager");
		// operation
		WorkbenchUtils.addFieldAnnotation(field, "@PathParam()", PRIMARY_COPY);
		// verifications
		verifyEventNotification(type.getResource(), CHANGED, POST_CHANGE, CONTENT, atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenFieldAnnotationChangedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		IField field = type.getField("entityManager");
		// operation
		field = WorkbenchUtils.replaceFirstOccurrenceOfCode(field, "@PersistenceContext",
				"@PersistenceContext(value=\"foo\")", WORKING_COPY);
		// verifications
		IAnnotation annotation = field.getAnnotation("PersistenceContext");
		verifyEventNotification(annotation, CHANGED, POST_RECONCILE, F_CONTENT, times(1));
	}

	@Test
	public void shouldNotifyWhenFieldAnnotationChangedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		IField field = type.getField("entityManager");
		// operation
		field = WorkbenchUtils.replaceFirstOccurrenceOfCode(field, "@PersistenceContext",
				"@PersistenceContext(value=\"foo\")", PRIMARY_COPY);
		// verifications
		IAnnotation annotation = field.getAnnotation("PersistenceContext");
		verifyEventNotification(annotation.getResource(), CHANGED, POST_CHANGE, CONTENT, atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenFieldAnnotationRemovedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		IField field = type.getField("entityManager");
		IAnnotation annotation = field.getAnnotation("PersistenceContext");
		// operation
		WorkbenchUtils.removeFieldAnnotation(field, "@PersistenceContext", WORKING_COPY);
		// verifications
		verifyEventNotification(annotation, REMOVED, POST_RECONCILE, NO_FLAG, times(1));
	}

	@Test
	public void shouldNotifyWhenFieldAnnotationRemovedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		IField field = type.getField("entityManager");
		IAnnotation annotation = field.getAnnotation("PersistenceContext");
		// operation
		WorkbenchUtils.removeFieldAnnotation(field, "@PersistenceContext", PRIMARY_COPY);
		// verifications
		verifyEventNotification(annotation.getResource(), CHANGED, POST_CHANGE, CONTENT, atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenMethodAddedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		// operation
		IMethod addedMethod = WorkbenchUtils.createMethod(type, "public Object fooLocator() { return null; }",
				WORKING_COPY);
		// verifications
		verifyEventNotification(addedMethod, ADDED, POST_RECONCILE, NO_FLAG, times(1));
	}

	@Test
	public void shouldNotifyWhenMethodAddedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		// operation
		IMethod addedMethod = WorkbenchUtils.createMethod(type, "public Object fooLocator() { return null; }",
				PRIMARY_COPY);
		// verifications
		verifyEventNotification(addedMethod.getResource(), CHANGED, POST_CHANGE, CONTENT, atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenMethodRemovedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		// operation
		IMethod method = WorkbenchUtils.removeMethod(type.getCompilationUnit(), "createCustomer", WORKING_COPY);
		// verifications
		verifyEventNotification(method, REMOVED, POST_RECONCILE, NO_FLAG, times(1));
	}

	@Test
	public void shouldNotifyWhenMethodRemovedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		// operation
		IMethod method = WorkbenchUtils.removeMethod(type.getCompilationUnit(), "createCustomer", PRIMARY_COPY);
		// verifications
		verifyEventNotification(method.getResource(), CHANGED, POST_CHANGE, CONTENT, atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenMethodRenamedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		IMethod oldMethod = WorkbenchUtils.getMethod(type, "getEntityManager");
		// operation
		WorkbenchUtils.renameMethod(type.getCompilationUnit(), "getEntityManager", "getEM", WORKING_COPY);
		// verifications
		IMethod newMethod = WorkbenchUtils.getMethod(type, "getEM");
		verifyEventNotification(oldMethod, REMOVED, POST_RECONCILE, NO_FLAG, times(1));
		verifyEventNotification(newMethod, ADDED, POST_RECONCILE, NO_FLAG, times(1));
	}

	@Test
	public void shouldNotifyWhenMethodRenamedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		// operation
		IMethod oldMethod = WorkbenchUtils.renameMethod(type.getCompilationUnit(), "getEntityManager", "getEM",
				PRIMARY_COPY);
		// verifications
		IMethod newMethod = WorkbenchUtils.getMethod(type, "getEM");
		verifyEventNotification(oldMethod.getResource(), CHANGED, POST_CHANGE, CONTENT, atLeastOnce());
		verifyEventNotification(newMethod.getResource(), CHANGED, POST_CHANGE, CONTENT, atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenMethodParameterAddedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		IMethod oldMethod = WorkbenchUtils.getMethod(type, "createCustomer");
		// operation
		IMethod newMethod = WorkbenchUtils.addMethodParameter(oldMethod, "int i", WORKING_COPY);
		// verifications
		verifyEventNotification(oldMethod, REMOVED, POST_RECONCILE, NO_FLAG, times(1));
		verifyEventNotification(newMethod, ADDED, POST_RECONCILE, NO_FLAG, times(1));
	}

	@Test
	public void shouldNotifyWhenMethodParameterAddedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		IMethod oldMethod = WorkbenchUtils.getMethod(type, "createCustomer");
		// operation
		WorkbenchUtils.addMethodParameter(oldMethod, "int i", PRIMARY_COPY);
		// verifications
		verifyEventNotification(type.getResource(), CHANGED, POST_CHANGE, CONTENT, atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenMethodParameterTypeChangedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		IMethod oldMethod = WorkbenchUtils.getMethod(type, "createCustomer");
		// operation
		IMethod newMethod = WorkbenchUtils.replaceFirstOccurrenceOfCode(oldMethod, "Customer customer",
				"String customer", WORKING_COPY);
		// verifications
		verifyEventNotification(oldMethod, REMOVED, POST_RECONCILE, NO_FLAG, times(1));
		verifyEventNotification(newMethod, ADDED, POST_RECONCILE, NO_FLAG, times(1));
	}

	@Test
	public void shouldNotifyWhenMethodParameterTypeChangedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		IMethod oldMethod = WorkbenchUtils.getMethod(type, "createCustomer");
		// operation
		WorkbenchUtils.replaceFirstOccurrenceOfCode(oldMethod, "Customer customer", "String customer", PRIMARY_COPY);
		// verifications
		// 1 invocation for both the old method removal and the new method
		// addition
		verifyEventNotification(oldMethod.getResource(), CHANGED, POST_CHANGE, CONTENT, atLeastOnce());
	}

	@Test
	public void shouldNotNotifyWhenMethodParameterNameChangedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);

		IMethod method = WorkbenchUtils.getMethod(type, "createCustomer");
		// operation
		method = WorkbenchUtils
				.replaceFirstOccurrenceOfCode(method, "Customer customer", "Customer cust", WORKING_COPY);
		// verifications
		verifyEventNotification(method, CHANGED, POST_RECONCILE, F_SIGNATURE, times(0));
	}

	@Test
	public void shouldNotNotifyWhenMethodParameterNameChangedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);

		IMethod method = WorkbenchUtils.getMethod(type, "createCustomer");
		// operation
		method = WorkbenchUtils
				.replaceFirstOccurrenceOfCode(method, "Customer customer", "Customer cust", PRIMARY_COPY);
		// verifications
		verifyEventNotification(method, CHANGED, POST_RECONCILE, F_SIGNATURE, times(0));
	}

	@Test
	public void shouldNotifyWhenMethodParametersReversedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		IMethod oldMethod = WorkbenchUtils.getMethod(type, "getCustomer");
		// operation
		IMethod newMethod = WorkbenchUtils.replaceFirstOccurrenceOfCode(oldMethod,
				"@PathParam(\"id\") Integer id, @Context UriInfo uriInfo",
				"@Context UriInfo uriInfo, @PathParam(\"id\") Integer id", WORKING_COPY);
		// verifications
		verifyEventNotification(oldMethod, REMOVED, POST_RECONCILE, NO_FLAG, times(1));
		verifyEventNotification(newMethod, ADDED, POST_RECONCILE, NO_FLAG, times(1));
	}

	@Test
	public void shouldNotifyWhenMethodParametersReversedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		IMethod oldMethod = WorkbenchUtils.getMethod(type, "getCustomer");
		// operation
		WorkbenchUtils.replaceFirstOccurrenceOfCode(oldMethod,
				"@PathParam(\"id\") Integer id, @Context UriInfo uriInfo",
				"@Context UriInfo uriInfo, @PathParam(\"id\") Integer id", PRIMARY_COPY);
		// verifications
		verifyEventNotification(oldMethod.getResource(), CHANGED, POST_CHANGE, CONTENT, atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenMethodParameterRemovedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		IMethod oldMethod = WorkbenchUtils.getMethod(type, "getCustomer");
		LOGGER.info("Method signature: " + oldMethod.getSignature());
		// operation
		IMethod newMethod = WorkbenchUtils.replaceFirstOccurrenceOfCode(oldMethod,
				"@PathParam(\"id\") Integer id, @Context UriInfo uriInfo", "@PathParam(\"id\") Integer id",
				WORKING_COPY);
		LOGGER.info("Method signature: " + newMethod.getSignature());
		// verifications
		verifyEventNotification(oldMethod, REMOVED, POST_RECONCILE, NO_FLAG, times(1));
		verifyEventNotification(newMethod, ADDED, POST_RECONCILE, NO_FLAG, times(1));
	}

	@Test
	public void shouldNotifyWhenMethodParameterRemovedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		IMethod oldMethod = WorkbenchUtils.getMethod(type, "getCustomer");
		LOGGER.info("Method signature: " + oldMethod.getSignature());
		// operation
		IMethod newMethod = WorkbenchUtils.replaceFirstOccurrenceOfCode(oldMethod,
				"@PathParam(\"id\") Integer id, @Context UriInfo uriInfo", "@PathParam(\"id\") Integer id",
				PRIMARY_COPY);
		LOGGER.info("Method signature: " + newMethod.getSignature());
		// verifications
		verifyEventNotification(oldMethod.getResource(), CHANGED, POST_CHANGE, CONTENT, atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenMethodParameterAnnotationAddedInWorkingCopy() throws CoreException,
			InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		IMethod method = WorkbenchUtils.getMethod(type, "createCustomer");
		// operation
		method = WorkbenchUtils.replaceFirstOccurrenceOfCode(method, "Customer customer",
				"@PathParam(\"id\") Customer customer", WORKING_COPY);
		// verifications
		verifyEventNotification(method, CHANGED, POST_RECONCILE, F_SIGNATURE, times(1));
	}

	@Test
	public void shouldNotifyWhenMethodParameterAnnotationAddedInPrimaryCopy() throws CoreException,
			InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		IMethod method = WorkbenchUtils.getMethod(type, "createCustomer");
		// operation
		method = WorkbenchUtils.replaceFirstOccurrenceOfCode(method, "Customer customer",
				"@PathParam(\"id\") Customer customer", PRIMARY_COPY);
		// verifications
		verifyEventNotification(method.getResource(), CHANGED, POST_CHANGE, CONTENT, atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenMethodParameterAnnotationChangedInWorkingCopy() throws CoreException,
			InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		IMethod method = WorkbenchUtils.getMethod(type, "getCustomer");
		// operation
		method = WorkbenchUtils.replaceFirstOccurrenceOfCode(method, "@PathParam(\"id\")", "@PathParam(\"bar\")",
				WORKING_COPY);
		// verifications
		verifyEventNotification(method, CHANGED, POST_RECONCILE, F_SIGNATURE, times(1));
	}

	@Test
	public void shouldNotifyWhenMethodParameterAnnotationChangedInPrimaryCopy() throws CoreException,
			InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		IMethod method = WorkbenchUtils.getMethod(type, "getCustomer");
		// operation
		method = WorkbenchUtils.replaceFirstOccurrenceOfCode(method, "@PathParam(\"id\")", "@PathParam(\"bar\")",
				PRIMARY_COPY);
		// verifications
		verifyEventNotification(method.getResource(), CHANGED, POST_CHANGE, CONTENT, atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenMethodParameterAnnotationRemovedInWorkingCopy() throws CoreException,
			InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		IMethod method = WorkbenchUtils.getMethod(type, "getCustomer");
		// operation
		method = WorkbenchUtils.replaceFirstOccurrenceOfCode(method, "@PathParam(\"id\") Integer id", "Integer id",
				WORKING_COPY);
		// verifications
		verifyEventNotification(method, CHANGED, POST_RECONCILE, F_SIGNATURE, times(1));
	}

	@Test
	public void shouldNotifyWhenMethodParameterAnnotationRemovedInPrimaryCopy() throws CoreException,
			InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		IMethod method = WorkbenchUtils.getMethod(type, "getCustomer");
		// operation
		method = WorkbenchUtils.replaceFirstOccurrenceOfCode(method, "@PathParam(\"id\") Integer id", "Integer id",
				PRIMARY_COPY);
		// verifications
		verifyEventNotification(method.getResource(), CHANGED, POST_CHANGE, CONTENT, atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenMethodAnnotationAddedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		IMethod method = WorkbenchUtils.getMethod(type, "createCustomer");
		// operation
		IAnnotation addedAnnotation = WorkbenchUtils.addMethodAnnotation(method, "@Path(\"/foo\")", WORKING_COPY);
		// verifications
		verifyEventNotification(addedAnnotation, ADDED, POST_RECONCILE, NO_FLAG, times(1));
	}

	@Test
	public void shouldNotifyWhenMethodAnnotationAddedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		IMethod method = WorkbenchUtils.getMethod(type, "createCustomer");
		// operation
		WorkbenchUtils.addMethodAnnotation(method, "@Path(\"/foo\")", PRIMARY_COPY);
		// verifications
		verifyEventNotification(method.getResource(), CHANGED, POST_CHANGE, CONTENT, atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenMethodAnnotationChangedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		IMethod method = WorkbenchUtils.getMethod(type, "createCustomer");
		IAnnotation annotation = method.getAnnotation("Path");
		// operation
		method = WorkbenchUtils.replaceFirstOccurrenceOfCode(method, "@Path(\"{id}\")", "@Path(\"{foo}\")",
				WORKING_COPY);
		// verifications
		verifyEventNotification(annotation, CHANGED, POST_RECONCILE, F_CONTENT, times(1));
	}

	@Test
	public void shouldNotifyWhenMethodAnnotationChangedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		IMethod method = WorkbenchUtils.getMethod(type, "createCustomer");
		IAnnotation annotation = method.getAnnotation("Path");
		// operation
		method = WorkbenchUtils.replaceFirstOccurrenceOfCode(method, "@Path(\"{id}\")", "@Path(\"{foo}\")",
				PRIMARY_COPY);
		// verifications
		verifyEventNotification(annotation.getResource(), CHANGED, POST_CHANGE, CONTENT, atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenMethodAnnotationRemovedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		IMethod method = WorkbenchUtils.getMethod(type, "createCustomer");
		IAnnotation annotation = method.getAnnotation("POST");
		// operation
		WorkbenchUtils.removeMethodAnnotation(method, annotation, WORKING_COPY);
		// verifications
		verifyEventNotification(annotation, REMOVED, POST_RECONCILE, NO_FLAG, times(1));
	}

	@Test
	public void shouldNotifyWhenMethodAnnotationRemovedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		IMethod method = WorkbenchUtils.getMethod(type, "createCustomer");
		IAnnotation annotation = method.getAnnotation("POST");
		// operation
		WorkbenchUtils.removeMethodAnnotation(method, annotation, PRIMARY_COPY);
		// verifications
		verifyEventNotification(method.getResource(), CHANGED, POST_CHANGE, CONTENT, atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenResourceMarkerAddedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		IField field = type.getField("entityManager");
		// operation
		WorkbenchUtils.removeField(field, PRIMARY_COPY);
		// verifications
		final IMethod method = WorkbenchUtils.getMethod(type, "deleteCustomer");
		verifyEventNotification(method.getResource(), CHANGED, POST_RECONCILE, F_MARKER_ADDED, atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenResourceMarkerRemovedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		// ResourcesPlugin.getWorkspace().addResourceChangeListener(listener);
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				null);
		// operations
		IField field = type.getField("entityManager");
		WorkbenchUtils.removeField(field, PRIMARY_COPY);
		WorkbenchUtils.createField(type, "private EntityManager entityManager;", PRIMARY_COPY);
		// verifications
		for (IMethod method : type.getMethods()) {
			verifyEventNotification(method.getResource(), CHANGED, POST_CHANGE, F_MARKER_ADDED, atLeastOnce());
			verifyEventNotification(method.getResource(), CHANGED, POST_CHANGE, F_MARKER_REMOVED, atLeastOnce());
		}
	}
}
