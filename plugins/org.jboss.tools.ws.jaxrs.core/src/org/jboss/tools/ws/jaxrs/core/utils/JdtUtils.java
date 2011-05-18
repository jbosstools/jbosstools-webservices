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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.core.BinaryType;
import org.eclipse.jdt.internal.core.CreateTypeHierarchyOperation;
import org.eclipse.jdt.internal.core.SourceType;
import org.eclipse.jface.text.ITypedRegion;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.internal.utils.MemberAnnotationBindingVisitor;
import org.jboss.tools.ws.jaxrs.core.internal.utils.MemberAnnotationBindingsVisitor;
import org.jboss.tools.ws.jaxrs.core.internal.utils.MethodBindingVisitor;

/**
 * A JDT wrapper that provides utility methods to manipulate the Java Model.
 * 
 * @author xcoulon
 * 
 */
@SuppressWarnings("restriction")
public final class JdtUtils {

	/**
	 * Hidden constructor of the utility method. Prevents instantiation.
	 */
	private JdtUtils() {
		super();
	}

	/**
	 * Return the first IType that matches the QualifiedName in the javaProject
	 * (anyway, there shouldn't be more than one, unless there are duplicate
	 * jars in the classpath, should it ?).
	 * 
	 * @param qName
	 *            the fully qualified name of the searched type
	 * @param javaProject
	 *            the java project in which the type should be resolved
	 * @param progressMonitor
	 *            a progress monitor (or null)
	 * @return the first IType found
	 * @throws CoreException
	 *             the underlying CoreException thrown by the manipulated JDT
	 *             APIs
	 */
	public static IType resolveType(final String qName, final IJavaProject javaProject,
			final IProgressMonitor progressMonitor) throws CoreException {
		if (qName == null) {
			return null;
		}
		IType findType = javaProject.findType(qName);
		if (findType == null) {
			Logger.warn("Unable to find type with fully qualified name '" + qName + "' in Java Project '"
					+ javaProject.getProject().getName() + "'");
		}
		return findType;
	}

	/**
	 * Returns the hierarchy for the given type.
	 * 
	 * @param baseType
	 *            the base type for the hierarchy
	 * @param includeLibraries
	 *            should the hierarchy include type from libraries
	 * @param progressMonitor
	 *            a progress monitor (or null)
	 * @return the Type Hierarchy for the base type
	 * @throws CoreException
	 *             the underlying CoreException thrown by the manipulated JDT
	 *             APIs
	 */
	public static ITypeHierarchy resolveTypeHierarchy(final IType baseType, final boolean includeLibraries,
			final IProgressMonitor progressMonitor) throws CoreException {
		// create type hierarchy
		// FIXME : restrict operation scope to sources only, exclude application
		// libraries.
		int appLibs = 0;
		if (includeLibraries) {
			appLibs = IJavaSearchScope.APPLICATION_LIBRARIES;
		}
		IJavaSearchScope searchScope = SearchEngine.createJavaSearchScope(
				new IJavaElement[] { baseType.getJavaProject() }, IJavaSearchScope.SOURCES | appLibs
						| IJavaSearchScope.REFERENCED_PROJECTS);
		CreateTypeHierarchyOperation operation = new CreateTypeHierarchyOperation(baseType, null, searchScope, true);
		ITypeHierarchy hierarchy = operation.getResult();
		if (hierarchy.exists()) {
			hierarchy.refresh(progressMonitor);
			return hierarchy;
		}
		Logger.warn("No type hierarchy found for " + baseType.getFullyQualifiedName());
		return null;
	}

	/**
	 * Resolves the Type Argument for the given parameterizedType against the
	 * given matchGenericType that is part of the parameterizedTypeHierarchy.
	 * Binding information is obtained from the Java model. This means that the
	 * compilation unit must be located relative to the Java model. This happens
	 * automatically when the source code comes from either
	 * setSource(ICompilationUnit) or setSource(IClassFile). When source is
	 * supplied by setSource(char[]), the location must be established
	 * explicitly by calling setProject(IJavaProject) and setUnitName(String).
	 * Note that the compiler options that affect doc comment checking may also
	 * affect whether any bindings are resolved for nodes within doc comments.
	 * 
	 * Note : the binding resolution on IClassFile requires the
	 * 'org.eclipse.jdt.launching' bundle, but Eclipse PDE detects it as an
	 * unused dependency.
	 * 
	 * @param parameterizedType
	 *            the parameterized type
	 * @param compilationUnit
	 *            the DOM CompilationUnit returned by the parse() method. This
	 *            operation is expensive and should be performed only once for
	 *            each type.
	 * @param matchGenericType
	 *            the super type
	 * @param parameterizedTypeHierarchy
	 *            the parameterized type hierarchy
	 * @param progressMonitor
	 *            a progress monitor (or null)
	 * @return a list of fully qualified type names
	 * @throws CoreException
	 *             the underlying CoreException thrown by the manipulated JDT
	 *             APIs
	 */
	@SuppressWarnings("unchecked")
	public static List<IType> resolveTypeArguments(final IType parameterizedType,
			final CompilationUnit compilationUnit, final IType matchGenericType,
			final ITypeHierarchy parameterizedTypeHierarchy, final IProgressMonitor progressMonitor)
			throws CoreException {
		if (compilationUnit == null) {
			Logger.warn("Unable to retrieve the Compilation Unit for type '" + parameterizedType
					+ "'. Check the library sources attachement.");
			return null;
		}
		// find path to the matchGenericType (class or interface)
		// ITypeHierarchy parameterizedTypeHierarchy =
		// getTypeHierarchy(parameterizedType, false, progressMonitor);
		List<IType> pathToParameterizedType = new ArrayList<IType>(Arrays.asList(parameterizedTypeHierarchy
				.getAllSubtypes(matchGenericType)));
		// skip the last values as they are the parameterized type and its
		// optionally sub types
		int index = pathToParameterizedType.indexOf(parameterizedType);
		// the generic type does not belong to the parameterized type's
		// hierarchy
		if (index < 0) {
			return null;
		}
		pathToParameterizedType = pathToParameterizedType.subList(0, index);
		// add match/target generic type, as by default it is not included
		// in
		// the result
		pathToParameterizedType.add(0, matchGenericType);
		// reverse the path, for easier comprehension of the code below
		Collections.reverse(pathToParameterizedType);
		List<IType> arguments = null;
		for (TypeDeclaration typeDeclaration : (List<TypeDeclaration>) compilationUnit.types()) {
			// ohoh, everything is resolved with bindings :-)
			ITypeBinding typeBinding = typeDeclaration.resolveBinding();
			if (typeBinding.getJavaElement().equals(parameterizedType)) {
				// locate the matchGenericType declaration...
				for (int i = 0; i < pathToParameterizedType.size(); i++) {
					IType superType = pathToParameterizedType.get(i);
					// lookup in the type's interfaces
					if (superType.isInterface()) {
						for (ITypeBinding superInterfaceBinding : typeBinding.getInterfaces()) {
							String superInterfaceErasureQName = superInterfaceBinding.getErasure().getQualifiedName();
							if (superInterfaceErasureQName.equals(superType.getFullyQualifiedName())) {
								typeBinding = superInterfaceBinding;
								break;
							}
						}
					} else {
						// lookup in type's super class
						typeBinding = typeBinding.getSuperclass();
					}
				}
				// ... then resolve the type parameters using its bindings
				// resolve in the parameterized type's interfaces
				ITypeBinding[] typeArgBindings = typeBinding.getTypeArguments();
				arguments = new ArrayList<IType>(typeArgBindings.length);
				for (ITypeBinding typeArgBinding : typeArgBindings) {
					Logger.debug("Resolving Java Element for type argument '" + typeArgBinding.getName() +"'");
					IJavaElement javaElement = typeArgBinding.getJavaElement();
					if (javaElement.getElementType() == IJavaElement.TYPE && javaElement.exists()) {
						arguments.add((IType) javaElement);
					}
				}
				// FIXME : path for a sample result with the help of
				// bindings
				// superClassBinding.getSuperclass().getInterfaces()[0].getInterfaces()[0].getTypeArguments()[0].getQualifiedName();
			}
		}

		return arguments;
	}

	/**
	 * Parse the DOM of the given member, and resolve bindings. If the given
	 * member is not a type, then its declaring type is used by the parser.
	 * 
	 * @param member
	 *            the type to parse
	 * @param progressMonitor
	 *            the progress monitor
	 * @return compilationUnit the DOM CompilationUnit returned by the parse()
	 *         method. This operation is expensive and should be performed only
	 *         once for each type.
	 * @throws JavaModelException
	 *             in case of exception underneath...
	 */
	public static CompilationUnit parse(final IMember member, final IProgressMonitor progressMonitor)
			throws JavaModelException {
		IType type = null;
		if (member.getElementType() == IMember.TYPE) {
			type = (IType) member;
		} else {
			type = member.getDeclaringType();
		}
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		if (type instanceof BinaryType) {
			IClassFile classFile = (IClassFile) type.getParent();
			if (classFile.getSource() == null) {
				Logger.warn("No source attachment is available for type '" + type
						+ "'. Unable to resolve type arguments.");
				return null;
			}
			parser.setKind(ASTParser.K_COMPILATION_UNIT);
			parser.setSource(classFile);
		} else if (type instanceof SourceType) {
			parser.setSource(type.getCompilationUnit());
		}

		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);
		// FIXME : parser.createAST throws an IllegalStateException on binary
		// parameterizedType if source code is not available.
		CompilationUnit node = (CompilationUnit) parser.createAST(progressMonitor);
		return node;
	}

	/**
	 * Return the annotation binding (AST3/DOM) matching the fully qualified
	 * name (or matching the simple name) on the given element. Checks that the
	 * annotation really exists (not a fake handle) or returns null
	 * 
	 * @param member
	 *            the element to scan (can be a type or a method)
	 * @param compilationUnit
	 *            the DOM CompilationUnit returned by the parse() method. This
	 *            operation is expensive and should be performed only once for
	 *            each type.
	 * @param annotationQualifiedName
	 *            the fully qualified name of the annotation to retrieve
	 * @return the annotation if it exists, null otherwise
	 * @throws JavaModelException
	 *             the underlying Exception thrown by the manipulated JDT APIs
	 */
	public static IAnnotationBinding resolveAnnotationBinding(final IMember member,
			final CompilationUnit compilationUnit, final Class<?> annotationClass) throws JavaModelException {
		return resolveAnnotationBinding(member, compilationUnit, annotationClass.getName());
	}

	public static IAnnotationBinding resolveAnnotationBinding(final IMember member,
			final CompilationUnit compilationUnit, final String annotationName) {
		if (compilationUnit == null) {
			return null;
		}
		MemberAnnotationBindingVisitor visitor = new MemberAnnotationBindingVisitor(member.getElementName(),
				member.getElementType(), annotationName);
		compilationUnit.accept(visitor);
		return visitor.getAnnotationBinding();
	}

	/**
	 * Resolves the parameters of the given annotation on the given method.
	 * 
	 * @param methodBinding
	 *            the binding of the method
	 * @param annotationNameFilter
	 *            the fully qualified name of the annotation
	 * @return a map in which each 'key' is the value of an annotation and
	 *         'value' is the literal of the type ('int', 'long', etc.)
	 * @throws JavaModelException
	 *             in case of underlying exception
	 */
	// TODO : merge with JdtUtils ?
	public static Map<IAnnotationBinding, ITypedRegion> resolveMethodParamBindings(final IMethod javaMethod,
			CompilationUnit compilationUnit, final Class<?> annotationTypeFilter) throws JavaModelException {
		MemberAnnotationBindingsVisitor visitor = new MemberAnnotationBindingsVisitor(javaMethod,
				annotationTypeFilter.getName());
		compilationUnit.accept(visitor);
		return visitor.getAnnotationBindings();
	}

	/**
	 * Resolves the bindings for the given method in the given Compilation Unit
	 * (AST3/DOM).
	 * 
	 * @param method
	 *            the method
	 * @param compilationUnit
	 *            the compilation unit
	 * @return the method binding or null if not found.
	 */
	public static IMethodBinding resolveMethodBinding(final IMethod method, final CompilationUnit compilationUnit) {
		MethodBindingVisitor visitor = new MethodBindingVisitor(method);
		compilationUnit.accept(visitor);
		return visitor.getMethodBinding();
	}

	/**
	 * Returns the compilation unit associated with the given resource.
	 * 
	 * @param resource
	 *            the resource
	 * @return the compilation unit or null if the resource is not a compilation
	 *         unit.
	 */
	public static ICompilationUnit getCompilationUnit(final IResource resource) {
		IJavaElement element = JavaCore.create(resource);
		if (element instanceof ICompilationUnit) {
			return (ICompilationUnit) element;
		}
		return null;
	}

	/**
	 * Returns the value set for the given attribute from the given annotation
	 * binding.
	 * 
	 * @param annotationBinding
	 *            the annotation binding
	 * @param attributeName
	 *            the attribute name to look up
	 * @return
	 * @return the value of the attribute, or null if the attribute is not
	 *         defined. The value can also be an Array of objects if the
	 *         attribute is multi-valued.
	 * @throws CoreException
	 *             the underlying CoreException thrown by the manipulated JDT
	 *             APIs
	 */
	public static Object resolveAnnotationAttributeValue(final IAnnotationBinding annotationBinding,
			final String attributeName) throws CoreException {
		if (annotationBinding != null) {
			for (IMemberValuePairBinding binding : annotationBinding.getAllMemberValuePairs()) {
				if (binding.getName().equals(attributeName)) {
					return binding.getValue();
				}
			}
		}
		return null;
	}

	/**
	 * Resolves the fully qualified name of the annotation given its binding.
	 * 
	 * @param annotationBinding
	 *            the annotation binding
	 * @return the fully qualified name, or null if it could not be resolved
	 * @throws JavaModelException
	 *             the exception thrown underneath
	 */
	public static String resolveAnnotationFullyQualifiedName(final IAnnotationBinding annotationBinding)
			throws JavaModelException {
		if (annotationBinding != null) {
			return annotationBinding.getAnnotationType().getQualifiedName();
		}
		return null;
	}

	/**
	 * Checks whether the given type is abstract or not.
	 * 
	 * @param type
	 *            the type to check
	 * @return true if the type is abstract, false otherwise
	 * @throws JavaModelException
	 *             the underlying JavaModelException thrown by the manipulated
	 *             JDT APIs
	 */
	public static boolean isAbstractType(final IType type) throws JavaModelException {
		return Flags.isAbstract(type.getFlags());
	}

	/**
	 * Returns the toplevel type of the given compilation unit.
	 * 
	 * @param compilationUnit
	 *            the DOM CompilationUnit returned by the parse() method. This
	 *            operation is expensive and should be performed only once for
	 *            each type.
	 * @return the top level type
	 * @throws JavaModelException
	 *             in case of exception
	 */
	public static IType resolveTopLevelType(final ICompilationUnit compilationUnit) throws JavaModelException {

		if (compilationUnit != null && compilationUnit.exists() && compilationUnit.getTypes() != null
				&& compilationUnit.getTypes().length > 0) {
			if (compilationUnit.getTypes()[0].getDeclaringType() != null) {
				return compilationUnit.getTypes()[0].getDeclaringType();
			}
			return compilationUnit.getTypes()[0];
		}
		return null;
	}

	/**
	 * Checks if the given type is a top-level type in its own compilation unit.
	 * 
	 * @param type
	 *            the given type
	 * @return if the given type is a top-level type, false otherwise
	 */
	public static boolean isTopLevelType(final IType type) {
		return (type.equals(type.getTypeRoot().findPrimaryType()));
	}

	/**
	 * Retrieve error problems from the given source member in the given
	 * compilation unit.
	 * 
	 * @param member
	 *            the member
	 * @param compilationUnit
	 *            the compilation unit
	 * @return the compilation errors
	 * @throws JavaModelException
	 *             in case of underlying exception
	 */
	public static Set<IProblem> resolveErrors(final ISourceReference member, final CompilationUnit compilationUnit)
			throws JavaModelException {
		Set<IProblem> problems = new HashSet<IProblem>();
		ISourceRange sourceRange = member.getSourceRange();
		int typeSourceStart = sourceRange.getOffset();
		int typeSourceEnd = typeSourceStart + sourceRange.getLength();
		for (IProblem problem : compilationUnit.getProblems()) {
			if (problem.isError() && typeSourceStart < problem.getSourceStart()
					&& problem.getSourceStart() < typeSourceEnd) {
				problems.add(problem);
			}
		}
		return problems;
	}

}
