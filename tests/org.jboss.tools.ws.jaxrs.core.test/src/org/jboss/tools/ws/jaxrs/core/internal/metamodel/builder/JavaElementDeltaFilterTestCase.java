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

import static org.eclipse.core.resources.IResourceDelta.REMOVED;
import static org.eclipse.jdt.core.ElementChangedEvent.POST_CHANGE;
import static org.eclipse.jdt.core.ElementChangedEvent.POST_RECONCILE;
import static org.eclipse.jdt.core.IJavaElement.ANNOTATION;
import static org.eclipse.jdt.core.IJavaElement.COMPILATION_UNIT;
import static org.eclipse.jdt.core.IJavaElement.METHOD;
import static org.eclipse.jdt.core.IJavaElement.PACKAGE_FRAGMENT_ROOT;
import static org.eclipse.jdt.core.IJavaElement.TYPE;
import static org.eclipse.jdt.core.IJavaElementDelta.ADDED;
import static org.eclipse.jdt.core.IJavaElementDelta.CHANGED;
import static org.eclipse.jdt.core.IJavaElementDelta.F_CONTENT;
import static org.eclipse.jdt.core.IJavaElementDelta.F_PRIMARY_RESOURCE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.jboss.tools.ws.jaxrs.core.jdt.Flags;
import org.junit.Before;
import org.junit.Test;

public class JavaElementDeltaFilterTestCase {

	private final JavaElementDeltaFilter filter = new JavaElementDeltaFilter();
	private ICompilationUnit workingCopy;
	private ICompilationUnit primaryCopy;

	private static JavaElementChangedEvent createEvent(IJavaElement element, int deltaKind, int eventType, Flags flags) {
		return new JavaElementChangedEvent(element, deltaKind, eventType, null, flags);
	}

	private static <T extends IJavaElement> T createMock(Class<T> type, int elementType, ICompilationUnit compilationUnit) {
		final T mock = mock(type);
		when(mock.getElementType()).thenReturn(elementType);
		return mock;
	}

	private static <T extends IJavaElement> T createMock(Class<T> type, int elementType) {
		return when(mock(type).getElementType()).thenReturn(elementType).getMock();
	}

	@Before
	public void setup() {
		workingCopy = when(createMock(ICompilationUnit.class, COMPILATION_UNIT).isWorkingCopy()).thenReturn(true)
				.getMock();
		primaryCopy = when(createMock(ICompilationUnit.class, COMPILATION_UNIT).isWorkingCopy()).thenReturn(false)
				.getMock();
	}

	@Test
	public void shouldAcceptAnyChangeEvent() throws JavaModelException {
		assertTrue("Wrong result", filter.apply(createEvent(workingCopy, REMOVED, POST_RECONCILE, Flags.NONE)));
		assertTrue("Wrong result", filter.apply(createEvent(primaryCopy, REMOVED, POST_RECONCILE, Flags.NONE)));
	}
	
	@Test
	public void shouldAcceptPostChangeAndPostReconcileEvents() throws JavaModelException {
		final IJavaElement element = createMock(IType.class, TYPE, workingCopy);
		assertTrue("Wrong result", filter.apply(createEvent(element, ADDED, POST_CHANGE, Flags.NONE)));
		assertTrue("Wrong result", filter.apply(createEvent(element, ADDED, POST_RECONCILE, Flags.NONE)));
	}

	@Test
	public void shouldAcceptWithValidFlags() throws JavaModelException {
		final IJavaElement element = createMock(ICompilationUnit.class, COMPILATION_UNIT);
		assertTrue("Wrong result", filter.apply(createEvent(element, CHANGED, POST_RECONCILE, new Flags(F_PRIMARY_RESOURCE + F_CONTENT))));
	}

	@Test
	public void shouldNotAcceptWithIncompleteFlags() throws JavaModelException {
		final IJavaElement element = createMock(ICompilationUnit.class, COMPILATION_UNIT);
		assertFalse("Wrong result", filter.apply(createEvent(element, CHANGED, POST_RECONCILE, new Flags(F_CONTENT))));
	}

	@Test
	public void shouldNotAcceptWithMissingFlags() throws JavaModelException {
		final IJavaElement element = createMock(IMethod.class, METHOD);
		assertFalse("Wrong result", filter.apply(createEvent(element, CHANGED, POST_RECONCILE, Flags.NONE)));
	}
	
	@Test
	public void shouldNotAcceptChangesInPackageInfoFile() {
		final IJavaElement element = createMock(IType.class, ANNOTATION, workingCopy);
		final IResource resource = mock(IResource.class);
		when(element.getResource()).thenReturn(resource);
		when(resource.getType()).thenReturn(IResource.FILE);
		when(resource.getName()).thenReturn("package-info.java");
		assertFalse("Wrong result", filter.apply(createEvent(element, ADDED, POST_RECONCILE, Flags.NONE)));
		assertFalse("Wrong result", filter.apply(createEvent(element, ADDED, POST_CHANGE, Flags.NONE)));
		assertFalse("Wrong result", filter.apply(createEvent(element, CHANGED, POST_RECONCILE, Flags.NONE)));
		assertFalse("Wrong result", filter.apply(createEvent(element, CHANGED, POST_CHANGE, Flags.NONE)));
		assertFalse("Wrong result", filter.apply(createEvent(element, REMOVED, POST_RECONCILE, Flags.NONE)));
		assertFalse("Wrong result", filter.apply(createEvent(element, REMOVED, POST_CHANGE, Flags.NONE)));
	}

	@Test
	public void shouldNotAcceptChangesInJarFile() {
		final IPackageFragmentRoot element = createMock(IPackageFragmentRoot.class, PACKAGE_FRAGMENT_ROOT);
		final IResource resource = mock(IResource.class);
		when(element.getResource()).thenReturn(resource);
		when(element.isArchive()).thenReturn(true);
		when(resource.getType()).thenReturn(IResource.FILE);
		when(resource.getName()).thenReturn("somearchive.jar");
		assertFalse("Wrong result", filter.apply(createEvent(element, ADDED, POST_RECONCILE, Flags.NONE)));
		assertFalse("Wrong result", filter.apply(createEvent(element, ADDED, POST_CHANGE, Flags.NONE)));
		assertFalse("Wrong result", filter.apply(createEvent(element, CHANGED, POST_RECONCILE, Flags.NONE)));
		assertFalse("Wrong result", filter.apply(createEvent(element, CHANGED, POST_CHANGE, Flags.NONE)));
		assertFalse("Wrong result", filter.apply(createEvent(element, REMOVED, POST_RECONCILE, Flags.NONE)));
		assertFalse("Wrong result", filter.apply(createEvent(element, REMOVED, POST_CHANGE, Flags.NONE)));
	}
}
