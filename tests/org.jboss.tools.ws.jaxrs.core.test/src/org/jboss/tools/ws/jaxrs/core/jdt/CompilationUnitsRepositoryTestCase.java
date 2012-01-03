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
package org.jboss.tools.ws.jaxrs.core.jdt;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.tools.ws.jaxrs.core.AbstractCommonTestCase;
import org.jboss.tools.ws.jaxrs.core.WorkbenchUtils;
import org.junit.Before;
import org.junit.Test;

public class CompilationUnitsRepositoryTestCase extends AbstractCommonTestCase {

	protected static final IProgressMonitor progressMonitor = new NullProgressMonitor();

	private CompilationUnitsRepository repository = null;

	@Before
	public void setup() {
		repository = CompilationUnitsRepository.getInstance();
		repository.clear();
	}

	private IType getType(String typeName) throws CoreException {
		return JdtUtils.resolveType(typeName, javaProject, progressMonitor);
	}

	private IMethod getMethod(IType parentType, String methodName) throws JavaModelException {
		return WorkbenchUtils.getMethod(parentType, methodName);
	}

	@Test
	public void shouldGetASTByCompilationUnit() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final ICompilationUnit compilationUnit = type.getCompilationUnit();
		// operation
		final CompilationUnit ast = repository.getAST(compilationUnit);
		// verification
		assertThat(ast, notNullValue());
	}

	@Test
	public void shouldGetASTByResource() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		// operation
		final CompilationUnit ast = repository.getAST(type.getResource());
		// verification
		assertThat(ast, notNullValue());
	}

	@Test
	public void shouldMergeASTWithDiffComputation() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod method = getMethod(type, "getCustomer");
		final ICompilationUnit compilationUnit = type.getCompilationUnit();
		// record the previous version
		repository.getAST(compilationUnit);
		// operation
		WorkbenchUtils.replaceFirstOccurrenceOfCode(method, "@PathParam(\"id\") Integer id",
				"@PathParam(\"ide\") Integer id", true);
		final CompilationUnit ast = JdtUtils.parse(compilationUnit, progressMonitor);
		final List<JavaMethodSignature> diffs = repository.mergeAST(compilationUnit, ast, true);
		// verification
		assertThat(diffs.size(), equalTo(1));
	}

	@Test
	public void shouldMergeASTWithoutDiffComputation() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod method = getMethod(type, "getCustomer");
		final ICompilationUnit compilationUnit = type.getCompilationUnit();
		// record the previous version
		repository.getAST(compilationUnit);
		// operation
		WorkbenchUtils.replaceFirstOccurrenceOfCode(method, "@PathParam(\"id\") Integer id",
				"@PathParam(\"ide\") Integer id", true);
		final CompilationUnit ast = JdtUtils.parse(compilationUnit, progressMonitor);
		final List<JavaMethodSignature> diffs = repository.mergeAST(compilationUnit, ast, false);
		// verification
		assertThat(diffs.size(), equalTo(0));
	}

	@Test
	public void shouldRemoveAST() throws CoreException {
		// pre-conditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final ICompilationUnit compilationUnit = type.getCompilationUnit();
		// record the previous version
		repository.getAST(compilationUnit);
		// operation
		repository.removeAST(compilationUnit);
		// verification
		// assertThat(diffs.size(), equalTo(0));
	}

	@Test
	public void shouldMergeProblemsAndReturnSomeFixedOnes() throws CoreException {
		// preconditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final ICompilationUnit compilationUnit = type.getCompilationUnit();
		final IAnnotation annotation = type.getAnnotations()[0];
		IProblem problem1 = createProblem(1, annotation);
		IProblem problem2 = createProblem(2, annotation);
		IProblem problem3 = createProblem(3, annotation);
		IProblem problem4 = createProblem(4, annotation);
		IProblem problem5 = createProblem(5, annotation);
		repository.mergeProblems(compilationUnit, new IProblem[] { problem1, problem2, problem3 });
		// operation
		Map<IProblem, IJavaElement> fixedProblems = repository.mergeProblems(compilationUnit, new IProblem[] {
				problem3, problem4, problem5 });
		// verification
		assertThat(fixedProblems.size(), equalTo(2));
		assertThat(fixedProblems.keySet(), containsInAnyOrder(problem1, problem2));
	}

	private IProblem createProblem(int id, final IAnnotation annotation) throws JavaModelException {
		IProblem mockProblem = mock(IProblem.class);
		when(mockProblem.getID()).thenReturn(id);
		when(mockProblem.getSourceStart()).thenReturn(annotation.getSourceRange().getOffset());
		return mockProblem;
	}

	@Test
	public void shouldMergeProblemsWhenNoneBefore() throws JavaModelException {
		// preconditions
		final ICompilationUnit mockCompilationUnit = mock(ICompilationUnit.class);
		IProblem problem = when(mock(IProblem.class).getID()).thenReturn(1).getMock();
		// operation
		Map<IProblem, IJavaElement> fixedProblems = repository.mergeProblems(mockCompilationUnit,
				new IProblem[] { problem });
		// verification
		assertThat(fixedProblems.size(), equalTo(0));
	}
}
