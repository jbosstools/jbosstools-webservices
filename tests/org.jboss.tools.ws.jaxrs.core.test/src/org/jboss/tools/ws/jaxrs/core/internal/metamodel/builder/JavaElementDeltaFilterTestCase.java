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

import static org.eclipse.core.resources.IResourceDelta.REMOVED;
import static org.eclipse.jdt.core.ElementChangedEvent.POST_CHANGE;
import static org.eclipse.jdt.core.ElementChangedEvent.POST_RECONCILE;
import static org.eclipse.jdt.core.IJavaElement.*;
import static org.eclipse.jdt.core.IJavaElement.COMPILATION_UNIT;
import static org.eclipse.jdt.core.IJavaElement.METHOD;
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
import org.junit.Before;
import org.junit.Test;

public class JavaElementDeltaFilterTestCase {

	private final int NO_FLAG = 0;
	private final JavaElementDeltaFilter filter = new JavaElementDeltaFilter();
	private ICompilationUnit workingCopy;
	private ICompilationUnit primaryCopy;

	private static JavaElementDelta createEvent(IJavaElement element, int deltaKind, int eventType, int flags) {
		return new JavaElementDelta(element, deltaKind, eventType, null, flags);
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
		assertTrue("Wrong result", filter.apply(createEvent(workingCopy, REMOVED, POST_RECONCILE, NO_FLAG)));
		assertTrue("Wrong result", filter.apply(createEvent(primaryCopy, REMOVED, POST_RECONCILE, NO_FLAG)));
	}
	
	@Test
	public void shouldAcceptPostChangeEventOnly() throws JavaModelException {
		IJavaElement element = createMock(IType.class, TYPE, workingCopy);
		assertFalse("Wrong result", filter.apply(createEvent(element, ADDED, POST_CHANGE, NO_FLAG)));
		assertTrue("Wrong result", filter.apply(createEvent(element, ADDED, POST_RECONCILE, NO_FLAG)));
	}

	@Test
	public void shouldAcceptPostReconcileEventOnly() throws JavaModelException {
		IJavaElement element = createMock(IType.class, ANNOTATION, workingCopy);
		assertTrue("Wrong result", filter.apply(createEvent(element, ADDED, POST_RECONCILE, NO_FLAG)));
		assertFalse("Wrong result", filter.apply(createEvent(element, ADDED, POST_CHANGE, NO_FLAG)));
	}

	@Test
	public void shouldAcceptWithValidFlags() throws JavaModelException {
		IJavaElement element = createMock(ICompilationUnit.class, COMPILATION_UNIT);
		assertTrue("Wrong result", filter.apply(createEvent(element, CHANGED, POST_RECONCILE, F_PRIMARY_RESOURCE + F_CONTENT)));
	}

	@Test
	public void shouldNotAcceptWithIncompleteFlags() throws JavaModelException {
		IJavaElement element = createMock(ICompilationUnit.class, COMPILATION_UNIT);
		assertFalse("Wrong result", filter.apply(createEvent(element, CHANGED, POST_RECONCILE, F_CONTENT)));
	}

	@Test
	public void shouldNotAcceptWithMissingFlags() throws JavaModelException {
		IJavaElement element = createMock(IMethod.class, METHOD);
		assertFalse("Wrong result", filter.apply(createEvent(element, CHANGED, POST_RECONCILE, NO_FLAG)));
	}
	
	@Test
	public void shouldNotAcceptChangesInPackageInfoFile() {
		IJavaElement element = createMock(IType.class, ANNOTATION, workingCopy);
		IResource resource = mock(IResource.class);
		when(element.getResource()).thenReturn(resource);
		when(resource.getType()).thenReturn(IResource.FILE);
		when(resource.getName()).thenReturn("package-info.java");
		assertFalse("Wrong result", filter.apply(createEvent(element, ADDED, POST_RECONCILE, NO_FLAG)));
		assertFalse("Wrong result", filter.apply(createEvent(element, ADDED, POST_CHANGE, NO_FLAG)));
		assertFalse("Wrong result", filter.apply(createEvent(element, CHANGED, POST_RECONCILE, NO_FLAG)));
		assertFalse("Wrong result", filter.apply(createEvent(element, CHANGED, POST_CHANGE, NO_FLAG)));
		assertFalse("Wrong result", filter.apply(createEvent(element, REMOVED, POST_RECONCILE, NO_FLAG)));
		assertFalse("Wrong result", filter.apply(createEvent(element, REMOVED, POST_CHANGE, NO_FLAG)));
	}

	@Test
	public void shouldNotAcceptChangesInJarFile() {
		IPackageFragmentRoot element = createMock(IPackageFragmentRoot.class, PACKAGE_FRAGMENT_ROOT);
		IResource resource = mock(IResource.class);
		when(element.getResource()).thenReturn(resource);
		when(element.isArchive()).thenReturn(true);
		when(resource.getType()).thenReturn(IResource.FILE);
		assertFalse("Wrong result", filter.apply(createEvent(element, ADDED, POST_RECONCILE, NO_FLAG)));
		assertFalse("Wrong result", filter.apply(createEvent(element, ADDED, POST_CHANGE, NO_FLAG)));
		assertFalse("Wrong result", filter.apply(createEvent(element, CHANGED, POST_RECONCILE, NO_FLAG)));
		assertFalse("Wrong result", filter.apply(createEvent(element, CHANGED, POST_CHANGE, NO_FLAG)));
		assertFalse("Wrong result", filter.apply(createEvent(element, REMOVED, POST_RECONCILE, NO_FLAG)));
		assertFalse("Wrong result", filter.apply(createEvent(element, REMOVED, POST_CHANGE, NO_FLAG)));
	}
}
