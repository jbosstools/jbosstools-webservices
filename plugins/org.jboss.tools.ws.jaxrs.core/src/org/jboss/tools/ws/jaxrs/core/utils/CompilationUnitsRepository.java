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
package org.jboss.tools.ws.jaxrs.core.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionUtils;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;

public class CompilationUnitsRepository {

	private static final CompilationUnitsRepository instance = new CompilationUnitsRepository();

	private final Map<ICompilationUnit, Map<String, JavaMethodSignature>> methodDeclarationsMap = new HashMap<ICompilationUnit, Map<String, JavaMethodSignature>>();

	private final Map<IPath, CompilationUnit> astMap = new HashMap<IPath, CompilationUnit>();

	private final Map<ICompilationUnit, Map<Integer, Problem>> problemsMap = new HashMap<ICompilationUnit, Map<Integer, Problem>>();

	/** Singleton constructor */
	private CompilationUnitsRepository() {
		super();
	}

	public static CompilationUnitsRepository getInstance() {
		return instance;
	}

	public void clear() {
		methodDeclarationsMap.clear();
		astMap.clear();
		problemsMap.clear();
	}

	/**
	 * @param compilationUnit
	 * @param methodsVisitor
	 * @return
	 * @throws JavaModelException
	 */
	public CompilationUnit getAST(final ICompilationUnit compilationUnit) throws JavaModelException {
		if (compilationUnit == null || compilationUnit.getResource() == null) {
			return null;
		}
		final IResource resource = compilationUnit.getResource();
		final IPath resourcePath = resource.getFullPath();
		if (!astMap.containsKey(resourcePath)) {
			Logger.trace("Adding {}'s AST in CompilationUnitsRepository cache.", compilationUnit.getElementName());
			recordAST(compilationUnit);
		} else {
			Logger.trace("CompilationUnitsRepository cache contains {}'s AST.", compilationUnit.getElementName());
		}

		return astMap.get(resourcePath);

	}

	/**
	 * @param compilationUnit
	 * @param methodsVisitor
	 * @return
	 * @throws JavaModelException
	 */
	public CompilationUnit getAST(final IResource resource) throws JavaModelException {
		final ICompilationUnit compilationUnit = JdtUtils.getCompilationUnit(resource);
		return getAST(compilationUnit);
	}

	/**
	 * Parse and stores the AST associated with the given compilation unit. If
	 * an AST already existed in the repository for the given Compilation Unit,
	 * it is overridden.
	 * 
	 * @param compilationUnit
	 * @return
	 * @throws JavaModelException
	 */
	public CompilationUnit recordAST(final ICompilationUnit compilationUnit) throws JavaModelException {
		// compute the AST by parsing the compilation unit...
		if (compilationUnit == null || !compilationUnit.exists()) {
			return null;
		}
		CompilationUnit compilationUnitAST = JdtUtils.parse(compilationUnit, new NullProgressMonitor());
		astMap.put(compilationUnit.getResource().getFullPath(), compilationUnitAST);
		final Map<String, JavaMethodSignature> methodSignatures = JdtUtils.resolveMethodSignatures(compilationUnit.findPrimaryType(), compilationUnitAST);
		methodDeclarationsMap.put(compilationUnit, methodSignatures);
		return compilationUnitAST;
	}

	public Map<String, JavaMethodSignature> mergeAST(final ICompilationUnit compilationUnit,
			final CompilationUnit compilationUnitAST, final boolean computeDiffs) throws JavaModelException {
		final Map<String, JavaMethodSignature> methodSignatures = JdtUtils.resolveMethodSignatures(compilationUnit.findPrimaryType(), compilationUnitAST);
		Map<String, JavaMethodSignature> diffs = null;
		// FIXME: must make sure that the methodDeclarationsMap remains in sync
		// with the working copy after each change.
		if (computeDiffs) {
			Map<String, JavaMethodSignature> workingCopyDeclarations = methodSignatures;
			Map<String, JavaMethodSignature> controlDeclarations = methodDeclarationsMap.get(compilationUnit);
			diffs = CollectionUtils.difference(workingCopyDeclarations, controlDeclarations);
			if (diffs.size() > 0) {
				Logger.trace("Found diffs in method signatures:", diffs);
			}
		} else {
			diffs = new HashMap<String, JavaMethodSignature>();
		}
		// replace old values in "cache"
		astMap.put(compilationUnit.getResource().getFullPath(), compilationUnitAST);
		// TODO : improve performances here : do not override all method
		// declaration, but only those that changed, because reparsing method
		// signatures (annotated parameters, etc.) may be expensive.
		methodDeclarationsMap.put(compilationUnit, methodSignatures);
		return diffs;
	}

	/**
	 * Returns the known {@link JavaMethodSignature} for the given {@link IMethod} in this repository, or null if it is unknown.
	 * @param javaMethod the given Java Method
	 * @return the known JavaMethodSignature or null
	 * @throws JavaModelException
	 */
	public JavaMethodSignature getMethodSignature(final IMethod javaMethod) throws JavaModelException {
		final ICompilationUnit compilationUnit = javaMethod.getCompilationUnit();
		if (!methodDeclarationsMap.containsKey(compilationUnit)) {
			recordAST(compilationUnit);
		}
		final Map<String, JavaMethodSignature> methodSignatures = methodDeclarationsMap.get(compilationUnit);
		return methodSignatures.get(javaMethod.getHandleIdentifier());
	}

	public void removeAST(final ICompilationUnit compilationUnit) {
		final IPath fullPath = compilationUnit.getResource().getFullPath();
		Logger.trace("Removing {}'s AST from CompilationUnitsRepository (path={})", compilationUnit, fullPath);
		methodDeclarationsMap.remove(compilationUnit);
		astMap.remove(fullPath);
		problemsMap.remove(compilationUnit);
	}

	/**
	 * Stores the given new/current problems for the given compilationUnit and
	 * returns the ones that were fixed, ie, the ones that were previously
	 * stored by not currently reported. This method assumes that a given
	 * problem has the same Id between two successive calls.
	 * 
	 * @param compilationUnit
	 *            the compilation unit
	 * @param problems
	 *            the new/current problems
	 * @return the problems that were fixed
	 * @throws JavaModelException
	 */
	public Map<IProblem, IJavaElement> mergeProblems(final ICompilationUnit compilationUnit, final IProblem[] problems)
			throws JavaModelException {
		final Map<Integer, Problem> lastProblems = problemsMap.get(compilationUnit);
		final Map<Integer, Problem> newProblems = new HashMap<Integer, Problem>();
		// convert array into map
		for (IProblem p : problems) {
			final int problemLocation = p.getSourceStart();
			final IJavaElement element = JdtUtils.getElementAt(compilationUnit, problemLocation);
			if (element instanceof IAnnotation) {
				newProblems.put(p.getID(), new Problem(p, element));
			}
		}
		// computes diffs between last and new problems
		final Map<IProblem, IJavaElement> fixedProblems = new HashMap<IProblem, IJavaElement>();
		if (lastProblems != null) {
			for (Entry<Integer, Problem> entry : lastProblems.entrySet()) {
				if (!newProblems.containsKey(entry.getKey())) {
					fixedProblems.put(entry.getValue().getProblem(), entry.getValue().getJavaElement());
				}
			}
		}
		// store new problems
		problemsMap.put(compilationUnit, newProblems);
		return fixedProblems;
	}

	static class Problem {

		private final IProblem problem;

		private final IJavaElement javaElement;

		public Problem(IProblem problem, IJavaElement javaElement) {
			this.problem = problem;
			this.javaElement = javaElement;
		}

		/** @return the problem */
		public IProblem getProblem() {
			return problem;
		}

		/** @return the javaElement */
		public IJavaElement getJavaElement() {
			return javaElement;
		}

	}

}
