/******************************************************************************* 
 * Copyright (c) 2008 - 2014 Red Hat, Inc. and others. 
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
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.IJavaElementDeltaFlag.F_MARKER_ADDED;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.IJavaElementDeltaFlag.F_MARKER_REMOVED;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.IJavaElementDeltaFlag.F_SIGNATURE;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils.addFieldAnnotation;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils.addMethodAnnotation;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils.addMethodParameter;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils.addTypeAnnotation;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils.appendCompilationUnitType;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils.createField;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils.createMethod;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils.delete;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils.removeField;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils.removeFieldAnnotation;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils.removeFirstOccurrenceOfCode;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils.removeMethod;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils.removeMethodAnnotation;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils.removeType;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils.renameMethod;
import static org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils.replaceAllOccurrencesOfCode;
import static org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils.replaceFirstOccurrenceOfCode;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.EventObject;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.ui.IEditorPart;
import org.jboss.tools.test.util.JobUtils;
import org.jboss.tools.ws.jaxrs.core.JBossJaxrsCorePlugin;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.utils.TestLogger;
import org.jboss.tools.ws.jaxrs.core.jdt.Flags;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;

public class JavaElementDeltaScannerTestCase {

	@ClassRule
	public static WorkspaceSetupRule workspaceSetupRule = new WorkspaceSetupRule("org.jboss.tools.ws.jaxrs.tests.sampleproject");
	
	@Rule
	public JaxrsMetamodelMonitor metamodelMonitor = new JaxrsMetamodelMonitor("org.jboss.tools.ws.jaxrs.tests.sampleproject", false);
	
	private JaxrsMetamodel metamodel = null;
	
	private IProject project = null;
	
	private IJavaProject javaProject = null;
	
	@SuppressWarnings("unchecked")
	@Before
	public void setup() throws CoreException {
		metamodel = metamodelMonitor.getMetamodel();
		javaProject = metamodel.getJavaProject();
		project = metamodel.getProject();
		JBossJaxrsCorePlugin.getDefault().pauseListeners();
		Assert.assertNotNull("JavaProject not set");
		JavaCore.addElementChangedListener(elementChangeListener);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceChangeListener);
		javaElementEvents = Mockito.mock(List.class);
		resourceEvents = Mockito.mock(List.class);
		JobUtils.waitForIdle();
	}
	
	@After
	public void removeAndRestoreListeners() throws CoreException, OperationCanceledException, InterruptedException {
		project.open(new NullProgressMonitor());
		javaProject.open(new NullProgressMonitor());
		JavaCore.removeElementChangedListener(elementChangeListener);
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener);
	}
	
	private static final boolean PRIMARY_COPY = false;

	private static final boolean WORKING_COPY = true;

	private final ElementChangeListener elementChangeListener = new ElementChangeListener();

	private final ResourceChangeListener resourceChangeListener = new ResourceChangeListener();

	private List<JavaElementChangedEvent> javaElementEvents = null;

	private List<ResourceDelta> resourceEvents = null;

	private final class ElementChangeListener implements IElementChangedListener {
		@Override
		public void elementChanged(ElementChangedEvent event) {
			try {
				final JavaElementDeltaScanner scanner = new JavaElementDeltaScanner();
				final List<? extends EventObject> events = scanner.scanAndFilterEvent(event, new NullProgressMonitor());
				for (EventObject e : events) {
					if (e instanceof JavaElementChangedEvent) {
						javaElementEvents.add((JavaElementChangedEvent) e);
					} else {
						fail("Unexpected event type:" + e);
					}
				}
			} catch (CoreException e) {
				TestLogger.error("Failed to scan event {}", e, event);
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
				// must explicitly call the add() method to perform verify() on
				// the mocks at the end of the test.
				for (ResourceDelta resourceDelta : affectedResources) {
					resourceEvents.add(resourceDelta);
				}
			} catch (CoreException e) {
				TestLogger.error("Failed to scan event {}", e, event);
			}
		}
	}

	private void verifyEventNotification(final IJavaElement element, final int deltaKind, final int eventType, final int flags,
			final VerificationMode numberOfTimes) throws JavaModelException {
		verifyEventNotification(element, deltaKind, eventType, new Flags(flags), numberOfTimes);
	}
	
	private void verifyEventNotification(final IJavaElement element, final int deltaKind, final int eventType, final Flags flags,
			final VerificationMode numberOfTimes) throws JavaModelException {
		TestLogger.info("Verifying method calls..");
		if (element == null) {
			verify(javaElementEvents, numberOfTimes).add(null);
		} else {
			final ICompilationUnit compilationUnit = JdtUtils.getCompilationUnit(element);
			final CompilationUnit ast = JdtUtils.parse(compilationUnit, new NullProgressMonitor());
			verify(javaElementEvents, numberOfTimes).add(
					new JavaElementChangedEvent(element, deltaKind, eventType, ast, flags));
		}
	}

	private void verifyEventNotification(final IResource resource, final int deltaKind, final int eventType, final int flags,
			final VerificationMode numberOfTimes) throws JavaModelException {
		verifyEventNotification(resource, deltaKind, eventType, new Flags(flags), numberOfTimes);
	}

	private void verifyEventNotification(final IResource resource, final int deltaKind, final int eventType, final Flags flags,
			final VerificationMode numberOfTimes) throws JavaModelException {
		TestLogger.info("Verifying method calls..");
		verify(resourceEvents, numberOfTimes).add(new ResourceDelta(resource, deltaKind, flags));
	}

	@Test
	public void shouldNotifyWhenEmptyCompilationUnitAdded() throws JavaModelException {
		// operation
		ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit(
				"EmptyCompilationUnit.txt", "org.jboss.tools.ws.jaxrs.sample.services", "FOO2.java");
		// verifications:
		verifyEventNotification(compilationUnit.getResource(), ADDED, POST_CHANGE, Flags.NONE, times(1));
	}

	@Test
	public void shouldNotNotifyWhenCompilationUnitChangedInWorkingCopy() throws CoreException {
		// pre-condition
		ICompilationUnit compilationUnit = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource")
				.getCompilationUnit();
		// operation
		appendCompilationUnitType(compilationUnit, "FooBarHTTPMethodMember.txt", WORKING_COPY);
		// verifications
		verifyEventNotification(compilationUnit, CHANGED, POST_RECONCILE, Flags.NONE, never());
	}

	@Test
	public void shouldNotifyWhenCompilationUnitChangedInPrimaryCopy() throws CoreException {
		// pre-condition
		ICompilationUnit compilationUnit = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource")
				.getCompilationUnit();
		// operation
		appendCompilationUnitType(compilationUnit, "FooBarHTTPMethodMember.txt", PRIMARY_COPY);
		// verifications
		verifyEventNotification(compilationUnit.getResource(), CHANGED, POST_CHANGE, Flags.NONE, atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenCompilationUnitAddedInPrimaryCopy() throws CoreException {
		// operation
		ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit( "FooResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "FooResource.java");
		// verifications: 1 times
		verifyEventNotification(compilationUnit.getResource(), CHANGED, POST_CHANGE, new Flags(CONTENT), atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenCompilationUnitRemovedInPrimaryCopy() throws CoreException {
		// pre-condition
		ICompilationUnit compilationUnit = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource")
				.getCompilationUnit();
		// operation
		delete(compilationUnit);
		// verifications:
		verifyEventNotification(compilationUnit.getResource(), REMOVED, POST_CHANGE, MARKERS, times(1));
	}

	@Test
	@Ignore("can't produce the right test conditions here")
	public void shouldNotifyWhenResourceRemovedInWorkingCopy() throws CoreException {
		// pre-condition
		final ICompilationUnit compilationUnit = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource")
				.getCompilationUnit().getWorkingCopy(new NullProgressMonitor());
		compilationUnit.open(new NullProgressMonitor());
		final IEditorPart editorPart = JavaUI.openInEditor(compilationUnit);
		JavaUI.revealInEditor(editorPart, (IJavaElement) compilationUnit);
		// operation
		ResourcesUtils.delete(compilationUnit.getResource());
		// verifications: 1 time
		verifyEventNotification(compilationUnit, REMOVED, POST_RECONCILE, Flags.NONE, times(2));
	}

	@Test
	public void shouldNotifyWhenResourceRemovedInPrimaryCopy() throws CoreException {
		// pre-condition
		ICompilationUnit compilationUnit = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource")
				.getCompilationUnit();
		// operation
		ResourcesUtils.delete(compilationUnit.getResource());
		// verifications: 1 time
		verifyEventNotification(compilationUnit.getResource(), REMOVED, POST_CHANGE, Flags.NONE, times(1));
	}

	@Test
	public void shouldNotifyWhenTypeAddedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		ICompilationUnit compilationUnit = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource")
				.getCompilationUnit();
		// operations
		IType addedType = appendCompilationUnitType(compilationUnit, "FooResourceMember.txt",
				WORKING_COPY);
		// verifications:
		verifyEventNotification(addedType, ADDED, POST_RECONCILE, Flags.NONE, times(1));
	}

	@Test
	public void shouldNotifyWhenTypeAddedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		ICompilationUnit compilationUnit = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource")
				.getCompilationUnit();
		// operations
		IType addedType = appendCompilationUnitType(compilationUnit, "FooResourceMember.txt",
				PRIMARY_COPY);
		// verifications: one call PostReconcile + one call on PostChange
		verifyEventNotification(addedType.getResource(), CHANGED, POST_CHANGE, new Flags(CONTENT), atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenTypeRemovedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		// operation
		removeType(type, WORKING_COPY);
		// verifications
		verifyEventNotification(type, REMOVED, POST_RECONCILE, Flags.NONE, times(1));
	}

	@Test
	public void shouldNotifyWhenTypeRemovedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		// operation
		removeType(type, PRIMARY_COPY);
		// verifications
		verifyEventNotification(type.getResource(), CHANGED, POST_CHANGE, new Flags(CONTENT), times(1));
	}

	@Test
	public void shouldNotifyWhenTypeParameterAddedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit(
				"PersistenceExceptionMapperEmptyParameter.txt", "org.jboss.tools.ws.jaxrs.sample.services.providers",
				"PersistenceExceptionMapper.java");
		// operation
		TestLogger.info("Performing Test Operation(s)...");
		replaceAllOccurrencesOfCode(compilationUnit, "ExceptionMapper<>",
				"ExceptionMapper<FooException>", WORKING_COPY);
		// verifications
		verifyEventNotification(compilationUnit.findPrimaryType(), CHANGED, POST_RECONCILE, new Flags(F_SUPER_TYPES), times(1));
	}

	@Test
	public void shouldNotifyWhenTypeParameterAddedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit(
				"PersistenceExceptionMapperEmptyParameter.txt", "org.jboss.tools.ws.jaxrs.sample.services.providers",
				"PersistenceExceptionMapper.java");
		// operation
		TestLogger.info("Performing Test Operation(s)...");
		replaceAllOccurrencesOfCode(compilationUnit, "ExceptionMapper<>",
				"ExceptionMapper<FooException>", PRIMARY_COPY);
		// verifications
		verifyEventNotification(compilationUnit.getResource(), CHANGED, POST_CHANGE, new Flags(CONTENT), atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenTypeInterfaceAddedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		// operation
		type = replaceFirstOccurrenceOfCode(type, "implements", "implements Serializable, ",
				WORKING_COPY);
		// verifications
		verifyEventNotification(type, CHANGED, POST_RECONCILE, F_SUPER_TYPES, times(1));
	}

	@Test
	public void shouldNotifyWhenTypeInterfaceAddedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		// operation
		type = replaceFirstOccurrenceOfCode(type, "implements", "implements Serializable, ",
				PRIMARY_COPY);
		// verifications
		verifyEventNotification(type.getResource(), CHANGED, POST_CHANGE, new Flags(CONTENT), atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenTypeInterfaceRemovedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		// operation
		type = replaceFirstOccurrenceOfCode(type, "implements ExceptionMapper<EntityNotFoundException>",
				"", WORKING_COPY);
		// verifications
		verifyEventNotification(type, CHANGED, POST_RECONCILE, F_SUPER_TYPES, times(1));
	}

	@Test
	public void shouldNotifyWhenTypeInterfaceRemovedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		// operation
		type = replaceFirstOccurrenceOfCode(type, "implements ExceptionMapper<EntityNotFoundException>",
				"", PRIMARY_COPY);
		// verifications
		verifyEventNotification(type.getResource(), CHANGED, POST_CHANGE, new Flags(CONTENT), atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenTypeSuperclassAddedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		// operation
		type = replaceFirstOccurrenceOfCode(type, "implements", "extends Object implements",
				WORKING_COPY);
		// verifications
		verifyEventNotification(type, CHANGED, POST_RECONCILE, F_SUPER_TYPES, times(1));
	}

	@Test
	public void shouldNotifyWhenTypeSuperclassAddedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		// operation
		type = replaceFirstOccurrenceOfCode(type, "implements", "extends Object implements",
				PRIMARY_COPY);
		// verifications
		verifyEventNotification(type.getResource(), CHANGED, POST_CHANGE, new Flags(CONTENT), atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenTypeSuperclassRemovedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.domain.Game");
		// operation
		type = replaceFirstOccurrenceOfCode(type, "extends Product", "", WORKING_COPY);
		// verifications
		verifyEventNotification(type, CHANGED, POST_RECONCILE, F_SUPER_TYPES, times(1));
	}

	@Test
	public void shouldNotifyWhenTypeSuperclassRemovedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.domain.Game");
		// operation
		type = replaceFirstOccurrenceOfCode(type, "extends Product", "", PRIMARY_COPY);
		// verifications
		verifyEventNotification(type.getResource(), CHANGED, POST_CHANGE, new Flags(CONTENT), atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenTypeParameterChangedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit(
				"PersistenceExceptionMapper.txt", "org.jboss.tools.ws.jaxrs.sample.services.providers",
				"PersistenceExceptionMapper.java");
		// operation
		TestLogger.info("Performing Test Operation(s)...");
		replaceAllOccurrencesOfCode(compilationUnit, "<PersistenceException>", "<FooException>",
				WORKING_COPY);
		// verifications
		verifyEventNotification(compilationUnit.findPrimaryType(), CHANGED, POST_RECONCILE, F_SUPER_TYPES, times(1));
	}

	@Test
	public void shouldNotifyWhenTypeParameterChangedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit(
				"PersistenceExceptionMapper.txt", "org.jboss.tools.ws.jaxrs.sample.services.providers",
				"PersistenceExceptionMapper.java");
		// operation
		TestLogger.info("Performing Test Operation(s)...");
		replaceAllOccurrencesOfCode(compilationUnit, "<PersistenceException>", "<FooException>",
				PRIMARY_COPY);
		// verifications
		verifyEventNotification(compilationUnit.getResource(), CHANGED, POST_CHANGE, new Flags(CONTENT), atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenTypeParameterRemovedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit(
				"PersistenceExceptionMapper.txt", "org.jboss.tools.ws.jaxrs.sample.services.providers",
				"PersistenceExceptionMapper.java");
		// operation
		TestLogger.info("Performing Test Operation(s)...");
		replaceAllOccurrencesOfCode(compilationUnit, "<PersistenceException>", "<>", WORKING_COPY);
		// verifications
		verifyEventNotification(compilationUnit.findPrimaryType(), CHANGED, POST_RECONCILE, F_SUPER_TYPES, times(1));
	}

	@Test
	public void shouldNotifyWhenTypeParameterRemovedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit(
				"PersistenceExceptionMapper.txt", "org.jboss.tools.ws.jaxrs.sample.services.providers",
				"PersistenceExceptionMapper.java");
		// operation
		TestLogger.info("Performing Test Operation(s)...");
		replaceAllOccurrencesOfCode(compilationUnit, "<PersistenceException>", "<>", PRIMARY_COPY);
		// verifications
		verifyEventNotification(compilationUnit.getResource(), CHANGED, POST_CHANGE, new Flags(CONTENT), atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenTypeAnnotationAddedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.PurchaseOrderResource");
		// operation
		IAnnotation addedAnnotation = addTypeAnnotation(type,
				"import javax.ws.rs.Consumes;\n@Consumes(\"foo/bar\")", WORKING_COPY);
		// verifications
		verifyEventNotification(addedAnnotation, ADDED, POST_RECONCILE, Flags.NONE, times(1));
	}

	@Test
	public void shouldNotifyWhenTypeAnnotationAddedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.PurchaseOrderResource");
		// operation
		addTypeAnnotation(type, "import javax.ws.rs.Consumes;\n@Consumes(\"foo/bar\")", PRIMARY_COPY);
		// verifications
		verifyEventNotification(type.getResource(), CHANGED, POST_CHANGE, new Flags(CONTENT), atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenTypeAnnotationChangedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		// operation
		type = replaceFirstOccurrenceOfCode(type, "@Path(value=CustomerResource.URI_BASE)",
				"@Path(\"/foo\")", WORKING_COPY);
		IAnnotation annotation = type.getAnnotation("Path");
		// verifications
		verifyEventNotification(annotation, CHANGED, POST_RECONCILE, F_CONTENT, times(1));
	}

	@Test
	public void shouldNotifyWhenTypeAnnotationChangedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		// operation
		type = replaceFirstOccurrenceOfCode(type, "@Path(value=CustomerResource.URI_BASE)",
				"@Path(\"/foo\")", PRIMARY_COPY);
		// verifications
		verifyEventNotification(type.getResource(), CHANGED, POST_CHANGE, new Flags(CONTENT), atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenTypeAnnotationRemovedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		final IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		// operation
		final IAnnotation annotation = type.getAnnotation("Path");
		removeFirstOccurrenceOfCode(type, "@Path(value=CustomerResource.URI_BASE)", WORKING_COPY);
		// verifications
		verifyEventNotification(annotation, REMOVED, POST_RECONCILE, Flags.NONE, times(1));
	}

	@Test
	public void shouldNotifyWhenTypeAnnotationRemovedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		final IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		// operation
		removeFirstOccurrenceOfCode(type, "@Path(value=CustomerResource.URI_BASE)", PRIMARY_COPY);
		// verifications
		verifyEventNotification(type.getResource(), CHANGED, POST_CHANGE, new Flags(CONTENT), atLeastOnce());
	}

	@Test
	public void shouldNotNotifyWhenLibraryAddedInClasspath() throws CoreException, InterruptedException {
		// operation
		IPackageFragmentRoot addedEntry = metamodelMonitor.addClasspathEntry("slf4j-api-1.5.2.jar");
		// verifications
		verifyEventNotification(addedEntry, ADDED, POST_CHANGE, Flags.NONE, times(0));
	}

	@Test
	public void shouldNotNotifyWhenUnexistingLibraryAddedInClasspath() throws CoreException, InterruptedException {
		// operation
		IPackageFragmentRoot addedEntry = metamodelMonitor.addClasspathEntry("slf4j-api-1.5.xyz.jar");
		// verifications
		verifyEventNotification(addedEntry, ADDED, POST_CHANGE, Flags.NONE, times(0));
	}

	@Test
	public void shouldNotNotifyWhenLibraryRemovedFromClasspath() throws CoreException, InterruptedException {
		// operation
		List<IPackageFragmentRoot> removedEntries = metamodelMonitor.removeClasspathEntry(
				"jaxrs-api-2.0.1.GA.jar");
		// verifications
		for (IPackageFragmentRoot removedEntry : removedEntries) {
			verifyEventNotification(removedEntry, REMOVED, POST_CHANGE, F_REMOVED_FROM_CLASSPATH, times(0));
		}
	}

	@Test
	public void shouldNotifyWhenFieldAddedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		// operation
		IField addedField = createField(type, "private int i;", WORKING_COPY);
		// verifications
		verifyEventNotification(addedField, ADDED, POST_RECONCILE, Flags.NONE, times(1));
	}

	@Test
	public void shouldNotifyWhenFieldAddedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		// operation
		createField(type, "private int i;", PRIMARY_COPY);
		// verifications
		verifyEventNotification(type.getResource(), CHANGED, POST_CHANGE, new Flags(CONTENT), atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenAnnotatedFieldAddedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		// operation
		IField addedField = createField(type, "@PathParam() private int i;", WORKING_COPY);
		// verifications
		verifyEventNotification(addedField, ADDED, POST_RECONCILE, Flags.NONE, times(1));
	}

	@Test
	public void shouldNotifyWhenAnnotatedFieldAddedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		// operation
		IField addedField = createField(type, "@PathParam() private int i;", PRIMARY_COPY);
		// verifications
		verifyEventNotification(addedField.getResource(), CHANGED, POST_CHANGE, new Flags(CONTENT), atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenFieldNameChangedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		IField oldField = type.getField("entityManager");
		// operation
		replaceAllOccurrencesOfCode(type.getCompilationUnit(), "entityManager", "em", WORKING_COPY);
		IField newField = type.getField("em");
		// verifications
		verifyEventNotification(oldField, REMOVED, POST_RECONCILE, Flags.NONE, times(1));
		verifyEventNotification(newField, ADDED, POST_RECONCILE, Flags.NONE, times(1));
	}

	@Test
	public void shouldNotifyWhenFieldNameChangedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		// operation
		replaceAllOccurrencesOfCode(type.getCompilationUnit(), "entityManager", "em", PRIMARY_COPY);
		// verifications
		verifyEventNotification(type.getResource(), CHANGED, POST_CHANGE, new Flags(CONTENT), atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenFieldTypeChangedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		IField field = type.getField("entityManager");
		// operation
		replaceAllOccurrencesOfCode(type.getCompilationUnit(), "private EntityManager",
				"private HibernateEntityManager", WORKING_COPY);
		// verifications
		verifyEventNotification(field, CHANGED, POST_RECONCILE, F_CONTENT, times(1));
	}

	@Test
	public void shouldNotifyWhenFieldTypeChangedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		// operation
		replaceAllOccurrencesOfCode(type.getCompilationUnit(), "private EntityManager",
				"private HibernateEntityManager", PRIMARY_COPY);
		// verifications
		verifyEventNotification(type.getResource(), CHANGED, POST_CHANGE, new Flags(CONTENT), atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenFieldRemovedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		IField field = type.getField("entityManager");
		// operation
		removeField(field, WORKING_COPY);
		// verifications
		verifyEventNotification(field, REMOVED, POST_RECONCILE, Flags.NONE, times(1));
	}

	@Test
	public void shouldNotifyWhenFieldRemovedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		IField field = type.getField("entityManager");
		// operation
		removeField(field, PRIMARY_COPY);
		// verifications
		verifyEventNotification(type.getResource(), CHANGED, POST_CHANGE, new Flags(CONTENT), atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenFieldAnnotationAddedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		IField field = type.getField("entityManager");
		// operation
		IAnnotation addedAnnotation = addFieldAnnotation(field, "@PathParam()", WORKING_COPY);
		// verifications
		verifyEventNotification(addedAnnotation, ADDED, POST_RECONCILE, Flags.NONE, times(1));
	}

	@Test
	public void shouldNotifyWhenFieldAnnotationAddedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		IField field = type.getField("entityManager");
		// operation
		addFieldAnnotation(field, "@PathParam()", PRIMARY_COPY);
		// verifications
		verifyEventNotification(type.getResource(), CHANGED, POST_CHANGE, new Flags(CONTENT), atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenFieldAnnotationChangedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		IField field = type.getField("entityManager");
		// operation
		field = replaceFirstOccurrenceOfCode(field, "@PersistenceContext",
				"@PersistenceContext(value=\"foo\")", WORKING_COPY);
		// verifications
		IAnnotation annotation = field.getAnnotation("PersistenceContext");
		verifyEventNotification(annotation, CHANGED, POST_RECONCILE, F_CONTENT, times(1));
	}

	@Test
	public void shouldNotifyWhenFieldAnnotationChangedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		IField field = type.getField("entityManager");
		// operation
		field = replaceFirstOccurrenceOfCode(field, "@PersistenceContext",
				"@PersistenceContext(value=\"foo\")", PRIMARY_COPY);
		// verifications
		IAnnotation annotation = field.getAnnotation("PersistenceContext");
		verifyEventNotification(annotation.getResource(), CHANGED, POST_CHANGE, new Flags(CONTENT), atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenFieldAnnotationRemovedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		IField field = type.getField("entityManager");
		IAnnotation annotation = field.getAnnotation("PersistenceContext");
		// operation
		removeFieldAnnotation(field, "@PersistenceContext", WORKING_COPY);
		// verifications
		verifyEventNotification(annotation, REMOVED, POST_RECONCILE, Flags.NONE, times(1));
	}

	@Test
	public void shouldNotifyWhenFieldAnnotationRemovedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		IField field = type.getField("entityManager");
		IAnnotation annotation = field.getAnnotation("PersistenceContext");
		// operation
		removeFieldAnnotation(field, "@PersistenceContext", PRIMARY_COPY);
		// verifications
		verifyEventNotification(annotation.getResource(), CHANGED, POST_CHANGE, new Flags(CONTENT), atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenMethodAddedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		// operation
		IMethod addedMethod = createMethod(type, "public Object fooLocator() { return null; }",
				WORKING_COPY);
		// verifications
		verifyEventNotification(addedMethod, ADDED, POST_RECONCILE, Flags.NONE, times(1));
	}

	@Test
	public void shouldNotifyWhenMethodAddedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		// operation
		IMethod addedMethod = createMethod(type, "public Object fooLocator() { return null; }",
				PRIMARY_COPY);
		// verifications
		verifyEventNotification(addedMethod.getResource(), CHANGED, POST_CHANGE, new Flags(CONTENT), atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenMethodRemovedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		// operation
		IMethod method = removeMethod(type.getCompilationUnit(), "createCustomer", WORKING_COPY);
		// verifications
		verifyEventNotification(method, REMOVED, POST_RECONCILE, Flags.NONE, times(1));
	}

	@Test
	public void shouldNotifyWhenMethodRemovedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		// operation
		IMethod method = removeMethod(type.getCompilationUnit(), "createCustomer", PRIMARY_COPY);
		// verifications
		verifyEventNotification(method.getResource(), CHANGED, POST_CHANGE, new Flags(CONTENT), atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenMethodRenamedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		IMethod oldMethod = metamodelMonitor.resolveMethod(type, "getEntityManager");
		// operation
		renameMethod(type.getCompilationUnit(), "getEntityManager", "getEM", WORKING_COPY);
		// verifications
		IMethod newMethod = metamodelMonitor.resolveMethod(type, "getEM");
		verifyEventNotification(oldMethod, REMOVED, POST_RECONCILE, Flags.NONE, times(1));
		verifyEventNotification(newMethod, ADDED, POST_RECONCILE, Flags.NONE, times(1));
	}

	@Test
	public void shouldNotifyWhenMethodRenamedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		// operation
		IMethod oldMethod = renameMethod(type.getCompilationUnit(), "getEntityManager", "getEM",
				PRIMARY_COPY);
		// verifications
		IMethod newMethod = metamodelMonitor.resolveMethod(type, "getEM");
		verifyEventNotification(oldMethod.getResource(), CHANGED, POST_CHANGE, new Flags(CONTENT), atLeastOnce());
		verifyEventNotification(newMethod.getResource(), CHANGED, POST_CHANGE, new Flags(CONTENT), atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenMethodParameterAddedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		IMethod oldMethod = metamodelMonitor.resolveMethod(type, "createCustomer");
		// operation
		IMethod newMethod = addMethodParameter(oldMethod, "int i", WORKING_COPY);
		// verifications
		verifyEventNotification(oldMethod, REMOVED, POST_RECONCILE, Flags.NONE, times(1));
		verifyEventNotification(newMethod, ADDED, POST_RECONCILE, Flags.NONE, times(1));
	}

	@Test
	public void shouldNotifyWhenMethodParameterAddedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		IMethod oldMethod = metamodelMonitor.resolveMethod(type, "createCustomer");
		// operation
		addMethodParameter(oldMethod, "int i", PRIMARY_COPY);
		// verifications
		verifyEventNotification(type.getResource(), CHANGED, POST_CHANGE, new Flags(CONTENT), atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenMethodParameterTypeChangedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		IMethod oldMethod = metamodelMonitor.resolveMethod(type, "createCustomer");
		// operation
		IMethod newMethod = replaceFirstOccurrenceOfCode(oldMethod, "Customer customer",
				"String customer", WORKING_COPY);
		// verifications
		verifyEventNotification(oldMethod, REMOVED, POST_RECONCILE, Flags.NONE, times(1));
		verifyEventNotification(newMethod, ADDED, POST_RECONCILE, Flags.NONE, times(1));
	}

	@Test
	public void shouldNotifyWhenMethodParameterTypeChangedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		IMethod oldMethod = metamodelMonitor.resolveMethod(type, "createCustomer");
		// operation
		replaceFirstOccurrenceOfCode(oldMethod, "Customer customer", "String customer", PRIMARY_COPY);
		// verifications
		// 1 invocation for both the old method removal and the new method
		// addition
		verifyEventNotification(oldMethod.getResource(), CHANGED, POST_CHANGE, new Flags(CONTENT), atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenMethodParameterNameChangedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");

		IMethod method = metamodelMonitor.resolveMethod(type, "createCustomer");
		// operation
		method = replaceFirstOccurrenceOfCode(method, "Customer customer", "Customer cust", WORKING_COPY);
		// verifications
		verifyEventNotification(method, CHANGED, POST_RECONCILE, F_SIGNATURE, times(1));
	}

	@Test
	public void shouldNotNotifyWhenMethodParameterNameChangedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");

		IMethod method = metamodelMonitor.resolveMethod(type, "createCustomer");
		// operation
		method = replaceFirstOccurrenceOfCode(method, "Customer customer", "Customer cust", PRIMARY_COPY);
		// verifications
		verifyEventNotification(method, CHANGED, POST_RECONCILE, F_SIGNATURE, times(0));
	}

	@Test
	public void shouldNotifyWhenMethodParametersReversedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		IMethod oldMethod = metamodelMonitor.resolveMethod(type, "getCustomer");
		// operation
		IMethod newMethod = replaceFirstOccurrenceOfCode(oldMethod,
				"@PathParam(\"id\") Integer id, @Context UriInfo uriInfo",
				"@Context UriInfo uriInfo, @PathParam(\"id\") Integer id", WORKING_COPY);
		// verifications
		verifyEventNotification(oldMethod, REMOVED, POST_RECONCILE, Flags.NONE, times(1));
		verifyEventNotification(newMethod, ADDED, POST_RECONCILE, Flags.NONE, times(1));
	}

	@Test
	public void shouldNotifyWhenMethodParametersReversedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		IMethod oldMethod = metamodelMonitor.resolveMethod(type, "getCustomer");
		// operation
		replaceFirstOccurrenceOfCode(oldMethod,
				"@PathParam(\"id\") Integer id, @Context UriInfo uriInfo",
				"@Context UriInfo uriInfo, @PathParam(\"id\") Integer id", PRIMARY_COPY);
		// verifications
		verifyEventNotification(oldMethod.getResource(), CHANGED, POST_CHANGE, new Flags(CONTENT), atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenMethodParameterRemovedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		IMethod oldMethod = metamodelMonitor.resolveMethod(type, "getCustomer");
		TestLogger.info("Method signature: " + oldMethod.getSignature());
		// operation
		IMethod newMethod = replaceFirstOccurrenceOfCode(oldMethod,
				"@PathParam(\"id\") Integer id, @Context UriInfo uriInfo", "@PathParam(\"id\") Integer id",
				WORKING_COPY);
		TestLogger.info("Method signature: " + newMethod.getSignature());
		// verifications
		verifyEventNotification(oldMethod, REMOVED, POST_RECONCILE, Flags.NONE, times(1));
		verifyEventNotification(newMethod, ADDED, POST_RECONCILE, Flags.NONE, times(1));
	}

	@Test
	public void shouldNotifyWhenMethodParameterRemovedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		IMethod oldMethod = metamodelMonitor.resolveMethod(type, "getCustomer");
		TestLogger.info("Method signature: " + oldMethod.getSignature());
		// operation
		IMethod newMethod = replaceFirstOccurrenceOfCode(oldMethod,
				"@PathParam(\"id\") Integer id, @Context UriInfo uriInfo", "@PathParam(\"id\") Integer id",
				PRIMARY_COPY);
		TestLogger.info("Method signature: " + newMethod.getSignature());
		// verifications
		verifyEventNotification(oldMethod.getResource(), CHANGED, POST_CHANGE, new Flags(CONTENT), atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenMethodParameterAnnotationAddedInWorkingCopy() throws CoreException,
			InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		IMethod method = metamodelMonitor.resolveMethod(type, "createCustomer");
		// operation
		method = replaceFirstOccurrenceOfCode(method, "Customer customer",
				"@PathParam(\"id\") Customer customer", WORKING_COPY);
		// verifications
		verifyEventNotification(method, CHANGED, POST_RECONCILE, F_SIGNATURE, times(1));
	}

	@Test
	public void shouldNotifyWhenMethodParameterAnnotationAddedInPrimaryCopy() throws CoreException,
			InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		IMethod method = metamodelMonitor.resolveMethod(type, "createCustomer");
		// operation
		method = replaceFirstOccurrenceOfCode(method, "Customer customer",
				"@PathParam(\"id\") Customer customer", PRIMARY_COPY);
		// verifications
		verifyEventNotification(method.getResource(), CHANGED, POST_CHANGE, new Flags(CONTENT), atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenMethodParameterAnnotationChangedInWorkingCopy() throws CoreException,
			InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		IMethod method = metamodelMonitor.resolveMethod(type, "getCustomer");
		// operation
		method = replaceFirstOccurrenceOfCode(method, "@PathParam(\"id\")", "@PathParam(\"bar\")",
				WORKING_COPY);
		// verifications
		verifyEventNotification(method, CHANGED, POST_RECONCILE, F_SIGNATURE, times(1));
	}

	@Test
	public void shouldNotifyWhenMethodParameterAnnotationChangedInPrimaryCopy() throws CoreException,
			InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		IMethod method = metamodelMonitor.resolveMethod(type, "getCustomer");
		// operation
		method = replaceFirstOccurrenceOfCode(method, "@PathParam(\"id\")", "@PathParam(\"bar\")",
				PRIMARY_COPY);
		// verifications
		verifyEventNotification(method.getResource(), CHANGED, POST_CHANGE, new Flags(CONTENT), atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenMethodParameterAnnotationRemovedInWorkingCopy() throws CoreException,
			InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		IMethod method = metamodelMonitor.resolveMethod(type, "getCustomer");
		// operation
		method = replaceFirstOccurrenceOfCode(method, "@PathParam(\"id\") Integer id", "Integer id",
				WORKING_COPY);
		// verifications
		verifyEventNotification(method, CHANGED, POST_RECONCILE, F_SIGNATURE, times(1));
	}

	@Test
	public void shouldNotifyWhenMethodParameterAnnotationRemovedInPrimaryCopy() throws CoreException,
			InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		IMethod method = metamodelMonitor.resolveMethod(type, "getCustomer");
		// operation
		method = replaceFirstOccurrenceOfCode(method, "@PathParam(\"id\") Integer id", "Integer id",
				PRIMARY_COPY);
		// verifications
		verifyEventNotification(method.getResource(), CHANGED, POST_CHANGE, new Flags(CONTENT), atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenMethodAnnotationAddedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		IMethod method = metamodelMonitor.resolveMethod(type, "createCustomer");
		// operation
		IAnnotation addedAnnotation = addMethodAnnotation(method, "@Path(\"/foo\")", WORKING_COPY);
		// verifications
		verifyEventNotification(addedAnnotation, ADDED, POST_RECONCILE, Flags.NONE, times(1));
	}

	@Test
	public void shouldNotifyWhenMethodAnnotationAddedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		IMethod method = metamodelMonitor.resolveMethod(type, "createCustomer");
		// operation
		addMethodAnnotation(method, "@Path(\"/foo\")", PRIMARY_COPY);
		// verifications
		verifyEventNotification(method.getResource(), CHANGED, POST_CHANGE, new Flags(CONTENT), atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenMethodAnnotationChangedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		IMethod method = metamodelMonitor.resolveMethod(type, "createCustomer");
		IAnnotation annotation = method.getAnnotation("Path");
		// operation
		replaceFirstOccurrenceOfCode(method, "@Path(\"{id}\")", "@Path(\"{foo}\")", WORKING_COPY);
		// verifications
		verifyEventNotification(annotation, CHANGED, POST_RECONCILE, F_CONTENT, times(1));
	}

	@Test
	public void shouldNotifyWhenMethodAnnotationChangedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		IMethod method = metamodelMonitor.resolveMethod(type, "createCustomer");
		IAnnotation annotation = method.getAnnotation("Path");
		// operation
		replaceFirstOccurrenceOfCode(method, "@Path(\"{id}\")", "@Path(\"{foo}\")", PRIMARY_COPY);
		// verifications
		verifyEventNotification(annotation.getResource(), CHANGED, POST_CHANGE, new Flags(CONTENT), atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenMethodAnnotationRemovedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		IMethod method = metamodelMonitor.resolveMethod(type, "createCustomer");
		IAnnotation annotation = method.getAnnotation("POST");
		// operation
		removeMethodAnnotation(method, annotation, WORKING_COPY);
		// verifications
		verifyEventNotification(annotation, REMOVED, POST_RECONCILE, Flags.NONE, times(1));
	}

	@Test
	public void shouldNotifyWhenMethodAnnotationRemovedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		IMethod method = metamodelMonitor.resolveMethod(type, "createCustomer");
		IAnnotation annotation = method.getAnnotation("POST");
		// operation
		removeMethodAnnotation(method, annotation, PRIMARY_COPY);
		// verifications
		verifyEventNotification(method.getResource(), CHANGED, POST_CHANGE, new Flags(CONTENT), atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenResourceMarkerAddedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		IField field = type.getField("entityManager");
		// operation
		removeField(field, PRIMARY_COPY);
		// verifications
		final IMethod method = metamodelMonitor.resolveMethod(type, "deleteCustomer");
		verifyEventNotification(method.getResource(), CHANGED, POST_RECONCILE, F_MARKER_ADDED, atLeastOnce());
	}

	@Test
	public void shouldNotifyWhenResourceMarkerRemovedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		// operations
		IField field = type.getField("entityManager");
		removeField(field, PRIMARY_COPY);
		createField(type, "private EntityManager entityManager;", PRIMARY_COPY);
		// verifications
		for (IMethod method : type.getMethods()) {
			verifyEventNotification(method.getResource(), CHANGED, POST_CHANGE, F_MARKER_ADDED, atLeastOnce());
			verifyEventNotification(method.getResource(), CHANGED, POST_CHANGE, F_MARKER_REMOVED, atLeastOnce());
		}
	}
	
	@Test
	public void shouldNotifyWhenParameterTypeChangedInPrimaryCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		// operations
		replaceAllOccurrencesOfCode(type, "import javax.persistence.EntityNotFoundException;", "import javax.persistence.NoResultException;", false);
		replaceAllOccurrencesOfCode(type, "ExceptionMapper<EntityNotFoundException>", "ExceptionMapper<NoResultException>", false);
		// verifications
		for (IMethod method : type.getMethods()) {
			verifyEventNotification(method.getResource(), CHANGED, POST_CHANGE, F_SIGNATURE, atLeastOnce());
		}
	}

	@Test
	public void shouldNotifyWhenParameterTypeChangedInWorkingCopy() throws CoreException, InterruptedException {
		// pre-condition
		IType type = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		// operations
		replaceAllOccurrencesOfCode(type, "import javax.persistence.EntityNotFoundException;", "import javax.persistence.NoResultException;", true);
		replaceAllOccurrencesOfCode(type, "ExceptionMapper<EntityNotFoundException>", "ExceptionMapper<NoResultException>", true);
		// verifications
		for (IMethod method : type.getMethods()) {
			verifyEventNotification(method.getResource(), CHANGED, POST_CHANGE, F_SIGNATURE, atLeastOnce());
		}
	}
	
	@Test
	public void shoudIgnoreEventOnCloseProject() throws CoreException {
		// pre-condition
		// operation
		project.close(new NullProgressMonitor());
		// verification
		verify(resourceEvents, never()).add(new ResourceDelta(project, CHANGED, Flags.NONE));
	}

	@Test
	public void shoudIgnoreEventOnCloseJavaProject() throws CoreException {
		// pre-condition
		// operation
		javaProject.close();
		// verification
		verify(resourceEvents, never()).add(new ResourceDelta(project, CHANGED, Flags.NONE));
	}
	
}
