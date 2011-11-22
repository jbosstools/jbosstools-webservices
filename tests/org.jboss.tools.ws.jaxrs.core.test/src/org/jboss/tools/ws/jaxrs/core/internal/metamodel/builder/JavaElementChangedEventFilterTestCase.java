package org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder;

import static org.eclipse.jdt.core.IJavaElement.COMPILATION_UNIT;
import static org.eclipse.jdt.core.IJavaElement.JAVA_PROJECT;
import static org.eclipse.jdt.core.IJavaElement.METHOD;
import static org.eclipse.jdt.core.IJavaElement.TYPE;
import static org.eclipse.jdt.core.IJavaElementDelta.ADDED;
import static org.eclipse.jdt.core.IJavaElementDelta.CHANGED;
import static org.eclipse.jdt.core.IJavaElementDelta.F_CONTENT;
import static org.eclipse.jdt.core.IJavaElementDelta.F_PRIMARY_RESOURCE;
import static org.eclipse.jdt.core.IJavaElementDelta.REMOVED;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import junit.framework.Assert;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.junit.Before;
import org.junit.Test;

public class JavaElementChangedEventFilterTestCase {

	private static final int[] NO_FLAG = new int[0];
	private final JavaElementChangedEventFilter filter = new JavaElementChangedEventFilter();
	private ICompilationUnit workingCopy;
	private ICompilationUnit primaryCopy;

	private static JavaElementChangedEvent createEvent(IJavaElement element, int deltaKind) {
		return new JavaElementChangedEvent(element, deltaKind, null, NO_FLAG);
	}

	private static JavaElementChangedEvent createEvent(IJavaElement element, int deltaKind, int... flags) {
		return new JavaElementChangedEvent(element, deltaKind, null, flags);
	}

	private static <T extends IMember> T createMock(Class<T> type, int elementType, ICompilationUnit compilationUnit) {
		final T mock = mock(type);
		when(mock.getCompilationUnit()).thenReturn(compilationUnit);
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
	public void shouldAcceptInAnyCompilationUnitCopyContext() throws JavaModelException {
		IJavaElement element = createMock(IType.class, TYPE, workingCopy);
		assertTrue("Wrong result", filter.apply(createEvent(element, ADDED)));
	}

	@Test
	public void shouldNotAcceptInAnyCompilationUnitCopyContext() throws JavaModelException {
		IJavaElement element = createMock(IType.class, TYPE, workingCopy);
		assertFalse("Wrong result", filter.apply(createEvent(element, CHANGED)));
	}

	@Test
	public void shouldAcceptInCompilationUnitWorkingCopyContext() throws JavaModelException {
		IJavaElement element = createMock(IMethod.class, METHOD, workingCopy);
		assertTrue("Wrong result", filter.apply(createEvent(element, ADDED)));
	}

	@Test
	public void shouldNotAcceptInCompilationUnitWorkingCopyContext() throws JavaModelException {
		IJavaElement element = createMock(IMethod.class, METHOD);
		assertFalse("Wrong result", filter.apply(createEvent(element, CHANGED)));
	}

	@Test
	public void shouldAcceptInCompilationUnitPrimaryCopyContext() throws JavaModelException {
		IJavaElement element = primaryCopy; // , COMPILATION_UNIT
		assertTrue("Wrong result", filter.apply(createEvent(element, REMOVED)));
	}

	@Test
	public void shouldNotAcceptInCompilationUnitPrimaryCopyContext() throws JavaModelException {
		IJavaElement element = workingCopy;// , COMPILATION_UNIT
		assertFalse("Wrong result", filter.apply(createEvent(element, ADDED)));
	}

	@Test
	public void shouldNotAcceptUnsupportedElementKind() throws JavaModelException {
		IJavaElement element = createMock(IJavaProject.class, JAVA_PROJECT);
		assertFalse("Wrong result", filter.apply(createEvent(element, CHANGED)));
	}

	@Test
	public void shouldAcceptWithValidFlags() throws JavaModelException {
		IJavaElement element = createMock(ICompilationUnit.class, COMPILATION_UNIT);
		assertTrue("Wrong result", filter.apply(createEvent(element, CHANGED, F_CONTENT, F_PRIMARY_RESOURCE)));
	}

	@Test
	public void shouldNotAcceptWithIncompleteFlags() throws JavaModelException {
		IJavaElement element = createMock(ICompilationUnit.class, COMPILATION_UNIT);
		assertFalse("Wrong result", filter.apply(createEvent(element, CHANGED, F_CONTENT)));
	}

	@Test
	public void shouldNotAcceptWithMissingFlags() throws JavaModelException {
		IJavaElement element = createMock(IMethod.class, METHOD);
		assertFalse("Wrong result", filter.apply(createEvent(element, CHANGED)));
	}

	@Test
	public void shouldMatchHashCodesAndEquals() {
		JavaElementChangedEventFilter filter1 = new JavaElementChangedEventFilter();
		JavaElementChangedEventFilter filter2 = new JavaElementChangedEventFilter();
		Assert.assertEquals(filter1, filter2);
		Assert.assertEquals(filter1.hashCode(), filter2.hashCode());
	}

}
