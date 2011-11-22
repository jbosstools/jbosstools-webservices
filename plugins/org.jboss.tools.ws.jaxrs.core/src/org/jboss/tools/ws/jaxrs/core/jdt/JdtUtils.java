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

import static org.eclipse.jdt.core.IJavaElement.PACKAGE_FRAGMENT_ROOT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IAnnotatable;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
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
import org.eclipse.jdt.internal.core.CreateTypeHierarchyOperation;
import org.eclipse.jface.text.ITypedRegion;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;

/** A JDT wrapper that provides utility methods to manipulate the Java Model.
 * 
 * @author xcoulon */
@SuppressWarnings("restriction")
public final class JdtUtils {

	/** Hidden constructor of the utility method. Prevents instantiation. */
	private JdtUtils() {
		super();
	}

	/** Returns the compilation unit associated with the given resource.
	 * 
	 * @param resource
	 *            the resource
	 * @return the compilation unit or null if the resource is not a compilation
	 *         unit. */
	public static ICompilationUnit getCompilationUnit(final IResource resource) {
		IJavaElement element = JavaCore.create(resource);
		if (element instanceof ICompilationUnit) {
			return (ICompilationUnit) element;
		}
		return null;
	}

	/** Returns the compilation unit associated with the given resource.
	 * 
	 * @param resource
	 *            the resource
	 * @return the compilation unit or null if the resource is not a compilation
	 *         unit. */
	public static ICompilationUnit getCompilationUnit(final IJavaElement element) {
		if (element instanceof IMember) {
			return ((IMember) element).getCompilationUnit();
		} else if (element instanceof IAnnotation) {
			return ((IMember) ((IAnnotation) element).getParent()).getCompilationUnit();
		} else if (element instanceof ICompilationUnit) {
			return (ICompilationUnit) element;
		}
		return null;
	}

	/** Checks whether the given type is abstract or not.
	 * 
	 * @param type
	 *            the type to check
	 * @return true if the type is abstract, false otherwise
	 * @throws JavaModelException
	 *             the underlying JavaModelException thrown by the manipulated
	 *             JDT APIs */
	public static boolean isAbstractType(final IType type) throws JavaModelException {
		return Flags.isAbstract(type.getFlags());
	}

	/** Returns the toplevel type of the given compilation unit.
	 * 
	 * @param compilationUnit
	 *            the DOM CompilationUnit returned by the parse() method. This
	 *            operation is expensive and should be performed only once for
	 *            each type.
	 * @return the top level type
	 * @throws JavaModelException
	 *             in case of exception */
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

	/** Checks if the given type is a top-level type in its own compilation unit.
	 * 
	 * @param type
	 *            the given type
	 * @return if the given type is a top-level type, false otherwise */
	public static boolean isTopLevelType(final IType type) {
		return (type.equals(type.getTypeRoot().findPrimaryType()));
	}

	/** Returns true if the given Java Element is a ICompilationUnit element or
	 * an IMember, and is in working copy state.
	 * 
	 * @param element
	 * @return true if the enclosing compilation unit is a working copy, false
	 *         otherwise */
	public static boolean isWorkingCopy(IJavaElement element) {
		ICompilationUnit compilationUnit = getCompilationUnit(element);
		if (compilationUnit != null) {
			return compilationUnit.isWorkingCopy();
		}
		return false;
	}

	public static IPackageFragmentRoot getPackageFragmentRoot(final IJavaElement element) {
		IJavaElement e = element;
		while (e.getElementType() != PACKAGE_FRAGMENT_ROOT) {
			e = e.getParent();
		}
		return (IPackageFragmentRoot) e;

	}

	/** Returns the closest Java Element that surrounds the given location in the
	 * given compilationUnit. This method can return SimpleAnnotation, which the
	 * default JDT ICompilationUnit implementation does not support.
	 * 
	 * @param sourceRange
	 * @param location
	 * @return
	 * @throws JavaModelException */
	public static IJavaElement getElementAt(ICompilationUnit compilationUnit, int location) throws JavaModelException {
		final IJavaElement element = compilationUnit.getElementAt(location);
		if (element instanceof IAnnotatable) {
			for (IAnnotation annotation : ((IAnnotatable) element).getAnnotations()) {
				final int length = annotation.getSourceRange().getLength();
				final int offset = annotation.getSourceRange().getOffset();
				if (offset <= location && location < (offset + length)) {
					return annotation;
				}
			}
		}
		return element;
	}

	/** Parse the DOM of the given member, and resolve bindings. If the given
	 * member is not a type, then its declaring type is used by the parser.
	 * 
	 * @param member
	 *            the type to parse
	 * @param progressMonitor
	 *            the progress monitor
	 * @return compilationUnit the DOM CompilationUnit returned by the parse()
	 *         method. This operation is expensive and should be performed only
	 *         once for each type. Returns null if the given member was null.
	 * @throws JavaModelException
	 *             in case of exception underneath... */
	public static CompilationUnit parse(final IMember member, final IProgressMonitor progressMonitor)
			throws JavaModelException {
		if (member == null) {
			return null;
		}
		return parse(member.getCompilationUnit(), progressMonitor);
	}

	/** Parse the DOM of the given member, and resolve bindings. If the given
	 * member is not a type, then its declaring type is used by the parser.
	 * 
	 * @param member
	 *            the type to parse
	 * @param progressMonitor
	 *            the progress monitor
	 * @return compilationUnit the DOM CompilationUnit returned by the parse()
	 *         method. This operation is expensive and should be performed only
	 *         once for each type. Returns null if the given member was null.
	 * @throws JavaModelException
	 *             in case of exception underneath... */
	public static CompilationUnit parse(final ICompilationUnit compilationUnit, final IProgressMonitor progressMonitor)
			throws JavaModelException {
		if (compilationUnit == null || !compilationUnit.exists()) {
			return null;
		}

		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setSource(compilationUnit);
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);
		// FIXME : parser.createAST throws an IllegalStateException on binary
		// parameterizedType if source code is not available.
		return (CompilationUnit) parser.createAST(progressMonitor);
	}

	/** Resolves the annotation given its type.
	 * 
	 * @param type
	 * @param ast
	 * @param annotationClass
	 * @return
	 * @throws JavaModelException */
	public static Annotation resolveAnnotation(IMember member, CompilationUnit ast, String annotationName)
			throws JavaModelException {
		if (member.isBinary()) {
			IAnnotatable javaElement = (IAnnotatable) member;
			final IAnnotation annotation = javaElement.getAnnotation(annotationName);
			if (annotation.exists()) {
				return new Annotation(annotation, annotation.getElementName(), resolveAnnotationElements(annotation));
			}
			return null;
		}
		// when the compilation is being created, the AST may not be available
		if (ast == null) {
			return null;
		}
		// TODO : do we really need to resolve the annotation binding ?
		JavaAnnotationsVisitor visitor = new JavaAnnotationsVisitor(member, annotationName);
		ast.accept(visitor);
		return visitor.getResolvedAnnotation();
	}

	/** Resolves the annotation given its type.
	 * 
	 * @param type
	 * @param ast
	 * @param annotationClass
	 * @return
	 * @throws JavaModelException */
	public static Map<String, Annotation> resolveAnnotations(IMember member, CompilationUnit ast,
			String... annotationNames) throws JavaModelException {
		return resolveAnnotations(member, ast, Arrays.asList(annotationNames));

	}

	/** Resolves the annotation given its type.
	 * 
	 * @param type
	 * @param ast
	 * @param annotationClass
	 * @return
	 * @throws JavaModelException */
	public static Map<String, Annotation> resolveAnnotations(IMember member, CompilationUnit ast,
			List<String> annotationNames) throws JavaModelException {
		if (member.isBinary()) {
			IAnnotatable javaElement = (IAnnotatable) member;
			final Map<String, Annotation> annotations = new HashMap<String, Annotation>();
			for (String annotationName : annotationNames) {
				final IAnnotation annotation = javaElement.getAnnotation(annotationName);
				if (annotation.exists()) {
					annotations.put(annotationName, new Annotation(annotation, annotation.getElementName(),
							resolveAnnotationElements(annotation)));
				}
			}
			return annotations;
		}
		// TODO : do we really need to resolve the annotation binding ?
		JavaAnnotationsVisitor visitor = new JavaAnnotationsVisitor(member, annotationNames);
		ast.accept(visitor);
		return visitor.getResolvedAnnotations();
	}

	/** Resolves the annotation given its type.
	 * 
	 * @param type
	 * @param ast
	 * @param annotationClass
	 * @return
	 * @throws JavaModelException */
	public static Annotation resolveAnnotation(IMember member, CompilationUnit ast, Class<?> annotationClass)
			throws JavaModelException {
		return resolveAnnotation(member, ast, annotationClass.getName());
	}

	/** Resolves the annotation given its type.
	 * 
	 * @param type
	 * @param ast
	 * @param annotationClass
	 * @return
	 * @throws JavaModelException */
	public static Annotation resolveAnnotation(IAnnotation javaAnnotation, CompilationUnit ast)
			throws JavaModelException {
		return resolveAnnotation((IMember) javaAnnotation.getParent(), ast, javaAnnotation.getElementName());
	}

	private static Map<String, List<String>> resolveAnnotationElements(IAnnotation annotation)
			throws JavaModelException {
		final Map<String, List<String>> annotationElements = new HashMap<String, List<String>>();
		for (IMemberValuePair element : annotation.getMemberValuePairs()) {
			final List<String> values = new ArrayList<String>();
			if (element.getValue() instanceof Collection<?>) {
				for (Object v : (Collection<?>) element.getValue()) {
					values.add(v.toString());
				}
			} else {
				values.add(element.getValue().toString());
			}
			annotationElements.put(element.getMemberName(), values);
		}
		return annotationElements;
	}

	/** Return the annotation binding (AST3/DOM) matching the fully qualified
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
	 *             the underlying Exception thrown by the manipulated JDT APIs */
	@Deprecated
	public static IAnnotationBinding resolveAnnotationBinding(final IMember member,
			final CompilationUnit compilationUnit, final Class<?> annotationClass) throws JavaModelException {
		return resolveAnnotationBinding(member, compilationUnit, annotationClass.getName());
	}

	@Deprecated
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

	@SuppressWarnings("unchecked")
	@Deprecated
	public static <T> T resolveAnnotationAttributeValue(IAnnotation annotation, CompilationUnit ast,
			String attributeName) throws JavaModelException {
		if (annotation == null) {
			return null;
		}
		if (((IMember) annotation.getParent()).isBinary()) {
			for (IMemberValuePair mvp : annotation.getMemberValuePairs()) {
				if (mvp.getMemberName().equals(attributeName)) {
					return (T) mvp.getValue();
				}
			}
			return null;
		}
		IAnnotationBinding annotationBinding = JdtUtils.resolveAnnotationBinding((IMember) annotation.getParent(), ast,
				annotation.getElementName());
		return (T) resolveAnnotationAttributeValue(annotationBinding, attributeName);
	}

	/** Returns the value set for the given attribute from the given annotation
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
	 *             APIs */
	@SuppressWarnings("unchecked")
	@Deprecated
	public static <T> T resolveAnnotationAttributeValue(final IAnnotationBinding annotationBinding,
			final String attributeName) {
		if (annotationBinding != null) {
			for (IMemberValuePairBinding binding : annotationBinding.getAllMemberValuePairs()) {
				if (binding.getName().equals(attributeName)) {
					return (T) binding.getValue();
				}
			}
		}
		return null;
	}

	/** Returns the value set for the given attribute from the given annotation
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
	 * @throws JavaModelException
	 * @throws CoreException
	 *             the underlying CoreException thrown by the manipulated JDT
	 *             APIs */
	@Deprecated
	public static Object resolveAnnotationAttributeValue(final IMember member, final CompilationUnit compilationUnit,
			final Class<?> annotationClass, final String attributeName) throws JavaModelException {
		if (member.isBinary()) {
			final IMemberValuePair[] memberValuePairs = ((IAnnotatable) member)
					.getAnnotation(annotationClass.getName()).getMemberValuePairs();
			for (IMemberValuePair valuePair : memberValuePairs) {
				if (valuePair.getMemberName().equals(attributeName)) {
					return valuePair.getValue();
				}
			}
			return null;
		}
		CompilationUnit ast = compilationUnit != null ? compilationUnit : parse(member, new NullProgressMonitor());
		IAnnotationBinding annotationBinding = JdtUtils.resolveAnnotationBinding(member, ast, annotationClass);
		return resolveAnnotationAttributeValue(annotationBinding, attributeName);
	}

	/** Resolves the fully qualified name of the annotation given its binding.
	 * 
	 * @param annotationBinding
	 *            the annotation binding
	 * @return the fully qualified name, or null if it could not be resolved
	 * @throws JavaModelException
	 *             the exception thrown underneath */
	@Deprecated
	public static String resolveAnnotationFullyQualifiedName(final IAnnotationBinding annotationBinding)
			throws JavaModelException {
		if (annotationBinding != null) {
			return annotationBinding.getAnnotationType().getQualifiedName();
		}
		return null;
	}

	/** Resolves the fully qualified name of the annotation given its binding.
	 * 
	 * @param annotationBinding
	 *            the annotation binding
	 * @return the fully qualified name, or null if it could not be resolved
	 * @throws JavaModelException
	 *             the exception thrown underneath */
	@Deprecated
	public static String resolveAnnotationFullyQualifiedName(IAnnotation annotation, CompilationUnit ast)
			throws JavaModelException {
		IAnnotationBinding annotationBinding = JdtUtils.resolveAnnotationBinding((IMember) annotation.getParent(), ast,
				annotation.getElementName());
		return resolveAnnotationFullyQualifiedName(annotationBinding);
	}

	/** Resolves the parameters of the given annotation on the given method.
	 * 
	 * @param methodBinding
	 *            the binding of the method
	 * @param annotationNameFilter
	 *            the fully qualified name of the annotation
	 * @return a map in which each 'key' is the value of an annotation and
	 *         'value' is the literal of the type ('int', 'long', etc.)
	 * @throws JavaModelException
	 *             in case of underlying exception */
	public static Map<IAnnotationBinding, ITypedRegion> resolveMethodParamBindings(final IMethod javaMethod,
			CompilationUnit compilationUnit, final Class<?> annotationTypeFilter) throws JavaModelException {
		MemberAnnotationBindingsVisitor visitor = new MemberAnnotationBindingsVisitor(javaMethod,
				annotationTypeFilter.getName());
		compilationUnit.accept(visitor);
		return visitor.getAnnotationBindings();
	}

	/** Resolves the bindings for the given method in the given Compilation Unit
	 * (AST3/DOM).
	 * 
	 * @param method
	 *            the method
	 * @param compilationUnit
	 *            the compilation unit
	 * @return the method binding or null if not found. */
	public static IMethodBinding resolveMethodBinding(final IMethod method, final CompilationUnit compilationUnit) {
		MethodBindingVisitor visitor = new MethodBindingVisitor(method);
		compilationUnit.accept(visitor);
		return visitor.getMethodBinding();
	}

	/** Retrieve error problems from the given source member in the given
	 * compilation unit.
	 * 
	 * @param member
	 *            the member
	 * @param compilationUnit
	 *            the compilation unit
	 * @return the compilation errors
	 * @throws JavaModelException
	 *             in case of underlying exception */
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

	/** Resolves the method parameters.
	 * 
	 * @param javaMethod
	 * @param compilationUnit
	 * @param annotationType
	 * @return the method parameters
	 * @throws JavaModelException */
	@Deprecated
	public static List<JavaMethodParameter> resolveMethodParameters(IMethod javaMethod,
			CompilationUnit compilationUnit, Class<?> annotationType) throws JavaModelException {
		List<JavaMethodParameter> parameters = new ArrayList<JavaMethodParameter>();
		Map<IAnnotationBinding, ITypedRegion> bindings = JdtUtils.resolveMethodParamBindings(javaMethod,
				compilationUnit, annotationType);
		for (Entry<IAnnotationBinding, ITypedRegion> entry : bindings.entrySet()) {
			IAnnotationBinding binding = entry.getKey();
			ITypedRegion region = entry.getValue();
			IMemberValuePairBinding[] allMemberValuePairs = binding.getAllMemberValuePairs();
			IMemberValuePairBinding memberValuePair = allMemberValuePairs[0];
			String annotationValue = (String) memberValuePair.getValue();
			int lineNumber = compilationUnit.getLineNumber(region.getOffset());
			/*
			 * parameters.add(new JavaMethodParameter(region.getType(),
			 * annotationType.getName(), annotationValue, region.getOffset(),
			 * region.getOffset() + region.getLength(), lineNumber));
			 */
		}
		// Collections.sort(parameters);
		return parameters;
	}

	@Deprecated
	public static List<MethodParameter> resolveMethodParameters(IMethod javaMethod, CompilationUnit ast)
			throws JavaModelException {
		MethodParametersVisitor visitor = new MethodParametersVisitor(javaMethod);
		ast.accept(visitor);
		return visitor.getMethodParameters();
	}

	/** Return the first IType that matches the QualifiedName in the javaProject
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
	 *             APIs */
	public static IType resolveType(final String qName, final IJavaProject javaProject,
			final IProgressMonitor progressMonitor) throws CoreException {
		if (qName == null) {
			return null;
		}
		IType findType = javaProject.findType(qName, progressMonitor);
		if (findType == null) {
			Logger.warn("Unable to find type with fully qualified name '" + qName + "' in Java Project '"
					+ javaProject.getProject().getName() + "'");
		}
		return findType;
	}

	/** Returns the hierarchy for the given type.
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
	 *             APIs */
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

	/** Resolves the Type Argument for the given parameterizedType against the
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
	 *             APIs */
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
					Logger.debug("Resolving Java ElementKind for type argument '" + typeArgBinding.getName() + "'");
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

	public static List<JavaMethodSignature> resolveMethodSignatures(IType type, CompilationUnit ast) {
		JavaMethodSignaturesVisitor methodsVisitor = new JavaMethodSignaturesVisitor(type.getCompilationUnit());
		ast.accept(methodsVisitor);
		return methodsVisitor.getMethodSignatures();
	}

	public static JavaMethodSignature resolveMethodSignature(IMethod method, CompilationUnit ast) {
		JavaMethodSignatureVisitor methodsVisitor = new JavaMethodSignatureVisitor(method);
		ast.accept(methodsVisitor);
		return methodsVisitor.getMethodSignature();
	}

	/** Return true if the given superType parameter is actually a super type of
	 * the given subType parameter, ie, the superType belongs to the supertypes
	 * in the subtype's hierarchy.
	 * 
	 * @param superType
	 *            the suspected super type
	 * @param subType
	 *            the suspected sub type
	 * @return true or false
	 * @throws CoreException */
	public static boolean isTypeOrSuperType(IType superType, IType subType) throws CoreException {
		if (subType == null || superType == null) {
			return false;
		}
		if (superType.getHandleIdentifier().equals(subType.getHandleIdentifier())) {
			return true;
		}
		final ITypeHierarchy hierarchy = JdtUtils.resolveTypeHierarchy(subType, true, new NullProgressMonitor());
		final List<IType> allSuperclasses = Arrays.asList(hierarchy.getAllSuperclasses(subType));
		for (IType type : allSuperclasses) {
			if (type.getHandleIdentifier().equals(superType.getHandleIdentifier())) {
				return true;
			}
		}
		return false;

	}

}
