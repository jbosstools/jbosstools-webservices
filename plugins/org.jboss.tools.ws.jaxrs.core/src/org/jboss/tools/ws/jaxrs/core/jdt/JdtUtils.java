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

package org.jboss.tools.ws.jaxrs.core.jdt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IAnnotatable;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.SourceRange;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.core.CreateTypeHierarchyOperation;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JavaMethodParameter;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JavaMethodSignature;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJavaMethodParameter;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJavaMethodSignature;

/**
 * A JDT wrapper that provides utility methods to manipulate the Java Model.
 * 
 * @author xcoulon
 */
@SuppressWarnings("restriction")
public final class JdtUtils {

	/** Hidden constructor of the utility method. Prevents instantiation. */
	private JdtUtils() {
		super();
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
	 * Returns the compilation unit associated with the given resource.
	 * 
	 * @param resource
	 *            the resource
	 * @return the compilation unit or null if the resource is not a compilation
	 *         unit.
	 */
	public static ICompilationUnit getCompilationUnit(final IJavaElement element) {
		if (element instanceof IMember) {
			return ((IMember) element).getCompilationUnit();
		} else if (element instanceof IAnnotation
		// ignore annotations on PackageDeclaration, such as in
		// package-info.java
				&& element.getParent() instanceof IMember) {
			return ((IMember) (element.getParent())).getCompilationUnit();
		} else if (element instanceof ICompilationUnit) {
			return (ICompilationUnit) element;
		}
		return null;
	}

	/**
	 * @return {@code true} if the given {@link IJavaElement} is an Archive (ie,
	 *         its type equals {@code IJavaElement#PACKAGE_FRAGMENT_ROOT} and
	 *         call to {@code IPackageFragmentRoot#isArchive()} return true. Otherwise,
	 *         returns false.
	 * @param javaElement
	 *            the {@link IJavaElement} to analyze.
	 */
	public static boolean isArchive(final IJavaElement javaElement) {
		return (javaElement.getElementType() == IJavaElement.PACKAGE_FRAGMENT_ROOT && ((IPackageFragmentRoot) javaElement)
				.isArchive());
	}

	/**
	 * Checks whether the given type is abstract or not.
	 * 
	 * @param type
	 *            the type to check
	 * @return {@code true} if the type is abstract or {@code null}, {@code false} otherwise
	 * @throws JavaModelException
	 *             the underlying JavaModelException thrown by the manipulated
	 *             JDT APIs
	 */
	public static boolean isAbstractType(final IType type)
			throws JavaModelException {
		if(type == null) {
			return true;
		}
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
	public static IType resolveTopLevelType(
			final ICompilationUnit compilationUnit) throws JavaModelException {

		if (compilationUnit != null && compilationUnit.exists()
				&& compilationUnit.getTypes() != null
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
	 * Returns true if the given Java Element is a ICompilationUnit element or
	 * an IMember, and is in working copy state.
	 * 
	 * @param element
	 * @return true if the enclosing compilation unit is a working copy, false
	 *         otherwise
	 */
	public static boolean isWorkingCopy(IJavaElement element) {
		ICompilationUnit compilationUnit = getCompilationUnit(element);
		if (compilationUnit != null) {
			return compilationUnit.isWorkingCopy();
		}
		return false;
	}

	/**
	 * Returns the closest Java Element that surrounds the given location in the
	 * given compilationUnit. This method can return SimpleAnnotation, which the
	 * default JDT ICompilationUnit implementation does not support.
	 * 
	 * @param sourceRange
	 * @param location
	 * @return
	 * @throws JavaModelException
	 */
	public static IJavaElement getElementAt(ICompilationUnit compilationUnit,
			int location) throws JavaModelException {
		if (compilationUnit == null) {
			return null;
		}
		final IJavaElement element = compilationUnit.getElementAt(location);
		if (element instanceof IAnnotatable) {
			for (IAnnotation annotation : ((IAnnotatable) element)
					.getAnnotations()) {
				final int length = annotation.getSourceRange().getLength();
				final int offset = annotation.getSourceRange().getOffset();
				if (offset <= location && location < (offset + length)) {
					return annotation;
				}
			}
		}
		return element;
	}

	/**
	 * Returns the closest Java Element of the expected type that surrounds the
	 * given location in the given compilationUnit. This method can return
	 * SimpleAnnotation, which the default JDT ICompilationUnit implementation
	 * does not support.
	 * 
	 * @param sourceRange
	 * @param location
	 * @param type
	 * @return
	 * @throws JavaModelException
	 */
	public static IJavaElement getElementAt(ICompilationUnit compilationUnit,
			int location, int type) throws JavaModelException {
		final IJavaElement element = getElementAt(compilationUnit, location);
		if(element != null && element.exists()) {
			return element.getAncestor(type);
		}
		return null;
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
	 *         once for each type. Returns null if the given member was null.
	 * @throws JavaModelException
	 *             in case of exception underneath...
	 */
	public static CompilationUnit parse(final ICompilationUnit compilationUnit,
			final IProgressMonitor progressMonitor) throws JavaModelException {
		if (compilationUnit == null || !compilationUnit.exists()) {
			return null;
		}

		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(compilationUnit);
		parser.setResolveBindings(true);
		parser.setEnvironment(null, null, null, true);
		parser.setBindingsRecovery(true);
		final CompilationUnit ast = (CompilationUnit) parser
				.createAST(progressMonitor);
		return ast;
	}

	/**
	 * Parse the compilation unit of the given {@link IJavaElement} if it is a
	 * {@link IMember} or a {@link ICompilationUnit}, null otherwise.
	 * 
	 * @param javaElement
	 * @param progressMonitor
	 * @return the compilation unit or null if the given java element has no
	 *         compilation unit (ex: package fragment root).
	 * @throws JavaModelException
	 */
	public static CompilationUnit parse(final IJavaElement javaElement,
			IProgressMonitor progressMonitor) throws JavaModelException {
		if (javaElement instanceof IMember) {
			return parse(((IMember) javaElement).getCompilationUnit(),
					progressMonitor);
		} else if(javaElement instanceof IAnnotation) {
			return parse(((IAnnotation) javaElement).getAncestor(IJavaElement.COMPILATION_UNIT),
					progressMonitor);
		
		} else if (javaElement instanceof ICompilationUnit) {
			return parse((ICompilationUnit) javaElement, progressMonitor);
		}
		return null;
	}

	/**
	 * Resolves the annotation on the given member and its associated AST given its fully qualified name.
	 * 
	 * @param member the annotated member to analyze 
	 * @param ast the AST associated with the member to analyze
	 * @param annotationName the name of the annotation to resolve 
	 * @return the resolved {@link Annotation} or {@code null} if it could not be resolved.
	 * @throws JavaModelException
	 */
	public static Annotation resolveAnnotation(final IMember member,
			final CompilationUnit ast, final String annotationName)
			throws JavaModelException {
		if (member.isBinary()) {
			IAnnotatable javaElement = (IAnnotatable) member;
			final IAnnotation javaAnnotation = javaElement
					.getAnnotation(annotationName);
			if (javaAnnotation != null && javaAnnotation.exists()) {
				return new Annotation(javaAnnotation,
						javaAnnotation.getElementName(),
						resolveAnnotationElements(javaAnnotation));
			}
			return null;
		}
		// when the compilation is being created, the AST may not be available
		if (ast == null) {
			return null;
		}
		final ASTNode memberNode = findDeclaringNode(member, ast);
		if(memberNode == null) {
			return null;
		}
		final List<?> memberModifiers = getNodeModifiers(memberNode);
		return findAnnotation(memberModifiers, annotationName);
	}

	/**
	 * Resolves *all* annotations on the given member.
	 * 
	 * @param member the annotated member
	 * @param ast its associated {@link CompilationUnit}
	 * @return a map with all annotations index by their fully qualified name, or empty map if none was found
	 * @throws JavaModelException
	 */
	public static Map<String, Annotation> resolveAllAnnotations(final IMember member,
			final CompilationUnit ast)
			throws JavaModelException {
		// only applies to binary element because the annotation names are the fully qualified names. 
		if (member.isBinary()) {
			final IAnnotatable javaElement = (IAnnotatable) member;
			final Map<String, Annotation> annotations = new HashMap<String, Annotation>();
			for(IAnnotation javaAnnotation: javaElement.getAnnotations()) {
				if (javaAnnotation.exists()) {
					final Annotation annotation = new Annotation(
							javaAnnotation, javaAnnotation.getElementName(),
							resolveAnnotationElements(javaAnnotation));
					annotations.put(annotation.getFullyQualifiedName(), annotation);
				}
			}
			return annotations;
		}
		// for source elements, we need to pass by bindings to resolve annotations fully qualified names.
		final ASTNode memberNode = findDeclaringNode(member, ast);
		if(memberNode == null) {
			return Collections.emptyMap();
		}
		final List<?> memberModifiers = getNodeModifiers(memberNode);
		return findAllAnnotations(memberModifiers);

	}

	/**
	 * Resolves the annotation given its type.
	 * 
	 * @param type
	 * @param ast
	 * @param annotationClass
	 * @return
	 * @throws JavaModelException
	 */
	public static Annotation resolveAnnotation(
			final IAnnotation javaAnnotation, final CompilationUnit ast)
			throws JavaModelException {
		if (javaAnnotation.getParent() instanceof IMember) {
			return resolveAnnotation((IMember) javaAnnotation.getParent(), ast,
					javaAnnotation.getElementName());
		}
		return null;
	}

	/**
	 * Locates the annotation located at the given position in the compilation
	 * unit, with a hint on the search scope provided by the given eponym
	 * parameter.
	 * 
	 * @param location
	 * @param compilationUnit
	 * @return the {@link Annotation} or null if the element at the given
	 *         location is not an IJavaAnnotation
	 * @throws JavaModelException
	 */
	public static Annotation resolveAnnotationAt(final int location,
			final ICompilationUnit compilationUnit) throws JavaModelException {
		final CompilationUnit ast = parse(compilationUnit, new NullProgressMonitor());
		if (ast != null) {
			return findAnnotation(NodeFinder.perform(ast, location, 1), location);
		}
		return null;
	}

	private static List<?> getNodeModifiers(final ASTNode node) {
		if (node == null) {
			return null;
		}
		switch (node.getNodeType()) {
		case ASTNode.TYPE_DECLARATION:
			return (List<?>) node
					.getStructuralProperty(TypeDeclaration.MODIFIERS2_PROPERTY);
		case ASTNode.ANNOTATION_TYPE_DECLARATION:
			return (List<?>) node
					.getStructuralProperty(AnnotationTypeDeclaration.MODIFIERS2_PROPERTY);
		case ASTNode.METHOD_DECLARATION:
			return (List<?>) node
					.getStructuralProperty(MethodDeclaration.MODIFIERS2_PROPERTY);
		case ASTNode.FIELD_DECLARATION:
			return (List<?>) node
					.getStructuralProperty(FieldDeclaration.MODIFIERS2_PROPERTY);
		default:
			return null;
		}
	}

	/**
	 * Finds the declaring {@link ASTNode} for the given {@link IMember}, using
	 * the {@link NodeFinder} if the member was not resolved.
	 * 
	 * @param member
	 *            the member to find
	 * @param ast
	 *            the Compilation Unit
	 * @return the associated declaring node
	 * @throws JavaModelException
	 */
	private static ASTNode findDeclaringNode(final IMember member,
			final CompilationUnit ast) throws JavaModelException {
		switch (member.getElementType()) {
		case IJavaElement.TYPE:
			final IType type = (IType) member;
			if (type.isResolved()) {
				final ASTNode typeNode = ast.findDeclaringNode(type.getKey());
				// return if match found
				if(typeNode != null) {
					return typeNode;
				}
			}
			break;
		case IJavaElement.METHOD:
			final IMethod method = (IMethod) member;
			if (method.isResolved()) {
				final ASTNode methodNode = ast.findDeclaringNode(method.getKey());
				// return if match found
				if(methodNode != null) {
					return methodNode;
				}
			}
			break;
		case IJavaElement.FIELD:
			final IField field = (IField) member;
			if (field.isResolved()) {
				// in the case of a Field, the
				// CompilationUnit#findDeclaringNode(String key) method returns
				// a VariableDeclarationFragment in a FieldDeclaration
				final ASTNode variableDeclarationFragment = ast
						.findDeclaringNode(field.getKey());
				if (variableDeclarationFragment != null) {
					final ASTNode fieldNode = variableDeclarationFragment.getParent();
					if(fieldNode != null) {
						// return if match found
						return fieldNode;
					}
				}
			}
			break;
		default:
		}
		// fallback approach if everything above failed.
		if(member.exists()) {
			final NodeFinder finder = new NodeFinder(ast, member.getSourceRange().getOffset(),
					member.getSourceRange().getLength());
			switch (member.getElementType()) {
			case IJavaElement.TYPE:
				return NodeFinder.perform(ast, member.getSourceRange());
			default:
				return finder.getCoveredNode();
			}
		}
		// assume that the element does not exist anymore
		return null;
	}

	/**
	 * @return an {@link Annotation} from the given node if it is an
	 *         {@link org.eclipse.jdt.core.dom.Annotation}, or recursively calls
	 *         with the given node's parent until match, or return null
	 * @param node
	 *            the current node
	 * @param location the location in the Root {@link ASTNode}
	 * @throws JavaModelException 
	 */
	private static Annotation findAnnotation(final ASTNode node, final int location) throws JavaModelException {
		if(node == null) {
			return null;
		} else if (!(node instanceof org.eclipse.jdt.core.dom.Annotation)) {
			return findAnnotation(node.getParent(), location);
		} 
		final IAnnotationBinding annotationBinding = ((org.eclipse.jdt.core.dom.Annotation) node).resolveAnnotationBinding();
		if(annotationBinding.getJavaElement() != null && annotationBinding.getJavaElement().getElementType() == IJavaElement.ANNOTATION) {
			return toAnnotation(annotationBinding, (IAnnotation) annotationBinding.getJavaElement());
		}
		if(node.getParent().getNodeType() == ASTNode.SINGLE_VARIABLE_DECLARATION) {
			final SingleVariableDeclaration variableDeclaration = (SingleVariableDeclaration) node.getParent();
			final IVariableBinding variableDeclarationBinding = variableDeclaration.resolveBinding();
			final IAnnotationBinding[] annotationBindings = variableDeclarationBinding.getAnnotations();
			// retrieve the parameter index in the parent method
			final IMethod parentMethod = (IMethod) variableDeclarationBinding.getDeclaringMethod().getJavaElement();
			final ILocalVariable localVariable = getLocalVariable(variableDeclarationBinding, parentMethod);
			if (localVariable != null) {
				final IAnnotation[] variableAnnotations = localVariable.getAnnotations();
				for (int j = 0; j < annotationBindings.length; j++) {
					final IAnnotation javaAnnotation = variableAnnotations[j];
					if (RangeUtils.matches(javaAnnotation.getSourceRange(), location)) {
						final IAnnotationBinding javaAnnotationBinding = annotationBindings[j];
						return toAnnotation(javaAnnotationBinding, javaAnnotation);
					}
				}
			}
	
		}
		return null;
	}

	/**
	 * Visits the modifiers and find the annotation whose name is the given annotationName
	 * 
	 * @param modifiers
	 *            the modifiers
	 * @param annotationName the annotation name to look for
	 * @return the {@link Annotation}
	 */
	private static Annotation findAnnotation(final List<?> modifiers,
			final String annotationName) {
		if (modifiers == null) {
			return null;
		}
		for (Object modifier : modifiers) {
			if (modifier instanceof org.eclipse.jdt.core.dom.Annotation) {
				final IAnnotationBinding annotationBinding = ((org.eclipse.jdt.core.dom.Annotation) modifier)
						.resolveAnnotationBinding();
				if (annotationBinding != null) {
					final String qualifiedName = annotationBinding
							.getAnnotationType().getQualifiedName();
					final String name = annotationBinding.getAnnotationType()
							.getName();
					if (annotationName.equals(qualifiedName)
							|| annotationName.equals(name)) {
						final Annotation annotation = toAnnotation(annotationBinding, (IAnnotation) annotationBinding.getJavaElement());
						// returned Annotation may be null in case of
						// compilation error in the source code, for example.
						if (annotation != null) {
							return annotation;
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Visits the modifiers and find all the annotations
	 * 
	 * @param modifiers
	 *            the modifiers
	 * @return a map of the found {@link Annotation}, indexed by their fully qualified names.
	 */
	private static Map<String, Annotation> findAllAnnotations(final List<?> modifiers) {
		final Map<String, Annotation> annotations = new HashMap<String, Annotation>();
		if (modifiers == null) {
			return annotations;
		}
		for (Object modifier : modifiers) {
			if (modifier instanceof org.eclipse.jdt.core.dom.Annotation) {
				final IAnnotationBinding annotationBinding = ((org.eclipse.jdt.core.dom.Annotation) modifier)
						.resolveAnnotationBinding();
				if (annotationBinding != null) {
					final String qualifiedName = annotationBinding
							.getAnnotationType().getQualifiedName();
					final Annotation annotation = toAnnotation(annotationBinding, (IAnnotation) annotationBinding.getJavaElement());
					// returned Annotation may be null in case of
					// compilation error in the source code, for example.
					if (annotation != null) {
						annotations.put(qualifiedName, annotation);
					}
				}
			}
		}
		return annotations;
	}

	/**
	 * Returns the localVariable associated with the given variable declaration binding, or null if it could not be found
	 * @param variableDeclarationBinding
	 * @return
	 * @throws JavaModelException
	 */
	private static ILocalVariable getLocalVariable(final IVariableBinding variableDeclarationBinding, final IMethod parentMethod)
			throws JavaModelException {
		int i = -1;
		for (String paramName : parentMethod.getParameterNames()) {
			i++;
			if (paramName.equals(variableDeclarationBinding.getName())) {
				break;
			}
		}
		if(i>=0) {
			return parentMethod.getParameters()[i];
		}
		return null;
	}
	
	/**
	 * Creates a instance of {@link Annotation} from the given annotation binding, specifically using the given javaAnnotation instead of the one that could be retrieved from the binding.
	 * @param annotationBinding
	 * @param javaAnnotation
	 * @return
	 */
	private static Annotation toAnnotation(final IAnnotationBinding annotationBinding, final IAnnotation javaAnnotation) {
		// return null if underlying java annotation does not exists or is not found (eg: compilation error in the compilation unit)
		if(javaAnnotation == null) {
			return null;
		}
		final String annotationName = annotationBinding.getAnnotationType().getQualifiedName();
		final Map<String, List<String>> annotationElements = resolveAnnotationElements(annotationBinding);
		return new Annotation(javaAnnotation, annotationName, annotationElements);
	}

	
	private static Map<String, List<String>> resolveAnnotationElements(IAnnotationBinding annotationBinding) {
		final Map<String, List<String>> annotationElements = new HashMap<String, List<String>>();
		try {
			for (IMemberValuePairBinding binding : annotationBinding.getAllMemberValuePairs()) {
				final List<String> values = new ArrayList<String>();
				if(binding.getValue() != null) {
					if (binding.getValue() instanceof Object[]) {
					for (Object v : (Object[]) binding.getValue()) {
						values.add(toString(v));
					}
				} else {
					values.add(toString(binding.getValue()));
				}
				}
				annotationElements.put(binding.getName(), values);
			}
			// if the code is not valid, the underlying DefaultValuePairBinding
			// may throw a NPE:
			// at
			// org.eclipse.jdt.core.dom.DefaultValuePairBinding.<init>(DefaultValuePairBinding.java:31)
			// at
			// org.eclipse.jdt.core.dom.AnnotationBinding.getAllMemberValuePairs(AnnotationBinding.java:98)
		} catch (RuntimeException e) {
			// silently ignore
		}
		return annotationElements;
	}

	/**
	 * Converts the given value into String. The actual types that are supported are:
	 * java.lang.Class - the ITypeBinding for the class object
	 * java.lang.String - the string value itself
	 * enum type - the IVariableBinding for the enum constant
	 * annotation type - an IAnnotationBinding
	 * for other types, the <code>java.lang.Object{@link #toString()}</code> method is used.
	 * @param value the binding value to read
	 * @return litteral value
	 */
	public static String toString(Object value) {
		if(value instanceof ITypeBinding) {
			return ((ITypeBinding)value).getQualifiedName();
		} else if(value instanceof IVariableBinding) {
			return ((IVariableBinding)value).getName();
		} else if(value instanceof IAnnotationBinding) {
			return ((IAnnotationBinding)value).getName();
		} 
		return value.toString();
	}
	
	/**
	 * Returns the source range for the MemberValuePair whose name is the given
	 * memberName, in the given annotation.
	 * 
	 * @param annotation
	 * @param memberName
	 * @param ast
	 * @return the sourceRange or null if it could not be evaluated.
	 * @throws JavaModelException
	 */
	public static ISourceRange resolveMemberPairValueRange(
			final IAnnotation annotation, final String memberName, final CompilationUnit ast)
			throws JavaModelException {
		final IType ancestor = (IType) annotation.getAncestor(IJavaElement.TYPE);
		if (ancestor != null && ancestor.exists()) {
			final ASTNode node = findMemberValuePair(annotation, memberName, ast);
			if(node != null) {
				return new SourceRange(node.getStartPosition(), node.getLength());
			}
		}
		return null;
	}

	/**
	 * @return the {@link ASTNode} (an instance of {@link MemberValuePair} or {@link QualifiedName}) in the given 
	 * annotation with the given name, or null if none was found.
	 * 
	 * @param annotation the annotation to analyze
	 * @param memberName the member name to find
	 * @param ast the associated AST
	 * @throws JavaModelException
	 */
	public static ASTNode findMemberValuePair(final IAnnotation annotation, final String memberName, final CompilationUnit ast) throws JavaModelException {
		if (annotation != null && ast != null) {
			final ASTNode node = NodeFinder.perform(ast, annotation.getSourceRange());
			if (node instanceof NormalAnnotation) {
				@SuppressWarnings("unchecked")
				final List<MemberValuePair> allMemberValuePairs = (List<MemberValuePair>) node
						.getStructuralProperty(NormalAnnotation.VALUES_PROPERTY);
				for (MemberValuePair memberValuePair : allMemberValuePairs) {
					if (memberValuePair.getName().getFullyQualifiedName().equals(memberName)) {
						return memberValuePair;
					}
				}
			} else if (node instanceof SingleMemberAnnotation) {
				return ((SingleMemberAnnotation)node).getValue();
			} 
		}
		return null;
	}

	/**
	 * @return a {@link Map} of annotation values (in a {@link List}), indexed
	 *         by annotation their associated name, or empty map is none was
	 *         found.
	 * @param annotation the annotation to analyze
	 * @throws JavaModelException
	 */
	private static Map<String, List<String>> resolveAnnotationElements(
			IAnnotation annotation) throws JavaModelException {
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
	public static IType resolveType(final String qName,
			final IJavaProject javaProject,
			final IProgressMonitor progressMonitor) throws CoreException {
		if (qName == null) {
			return null;
		}
		IType findType = javaProject.findType(qName);
		if (findType == null) {
			Logger.debug("Unable to find type with fully qualified name '"
					+ qName + "' in Java Project '"
					+ javaProject.getProject().getName() + "'");
		}
		return findType;
	}
	
	/**
	 * @param compilationUnit the compilation unit to analyze
	 * @param handleIdentifier the target type identifier
	 * @return the found {@link IType} or {@code null} if none was found.
	 * @throws JavaModelException
	 */
	public static IType resolveType(final ICompilationUnit compilationUnit, final String handleIdentifier) throws JavaModelException {
		final IType[] allTypes = compilationUnit.getAllTypes();
		for(IType type: allTypes) {
			if(type.getHandleIdentifier().equals(handleIdentifier)) {
				return type;
			}
		}
		return null;
	}


	/**
	 * Returns the hierarchy for the given type, or null if it could not be
	 * 'computed'.
	 * 
	 * @param baseType
	 *            the base type for the hierarchy
	 * @param includeLibraries
	 *            should the hierarchy include type from libraries
	 * @param progressMonitor
	 *            a progress monitor (or null)
	 * @return the SourceType Hierarchy for the base type
	 * @throws CoreException
	 *             the underlying CoreException thrown by the manipulated JDT
	 *             APIs
	 */
	public static ITypeHierarchy resolveTypeHierarchy(final IType baseType,
			final IJavaElement scope, final boolean includeLibraries,
			final IProgressMonitor progressMonitor) throws CoreException {
		// create type hierarchy
		// FIXME : restrict operation scope to sources only, exclude application
		// libraries.
		int appLibs = 0;
		if (includeLibraries) {
			appLibs = IJavaSearchScope.APPLICATION_LIBRARIES;
		}
		IJavaSearchScope searchScope = SearchEngine.createJavaSearchScope(
				new IJavaElement[] { scope }, IJavaSearchScope.SOURCES
						| appLibs | IJavaSearchScope.REFERENCED_PROJECTS);
		CreateTypeHierarchyOperation operation = new CreateTypeHierarchyOperation(
				baseType, null, searchScope, true);
		ITypeHierarchy hierarchy = operation.getResult();
		if (hierarchy != null && hierarchy.exists()) {
			hierarchy.refresh(progressMonitor);
			return hierarchy;
		}
		Logger.warn("No type hierarchy found for "
				+ baseType.getFullyQualifiedName());
		return null;
	}

	/**
	 * Searches and returns all the subtypes of the given superType that do not
	 * have themselves other subtypes
	 * 
	 * @param scope
	 * @param progressMonitor
	 * @param searchScope
	 * @return a list of subtypes or an empty list if none was found.
	 * @throws CoreException
	 * @throws JavaModelException
	 */
	public static List<IType> findSubtypes(final IJavaElement scope,
			final IType superType, final IProgressMonitor progressMonitor)
			throws CoreException, JavaModelException {
		final List<IType> types = new ArrayList<IType>();
		if (superType != null) {
			final ITypeHierarchy hierarchy = JdtUtils.resolveTypeHierarchy(
					superType, scope, false, progressMonitor);
			final IType[] allSubtypes = hierarchy.getAllSubtypes(superType);
			for (IType subtype : allSubtypes) {
				if (subtype.isStructureKnown()
						&& subtype.getJavaProject().equals(
								scope.getJavaProject())
						&& hierarchy.getAllSubtypes(subtype).length == 0) {
					types.add(subtype);
				}
			}
		}
		return types;
	}

	/**
	 * Returns a list containing the given type and all its subtypes in the
	 * current project
	 * 
	 * @param progressMonitor
	 * @param type
	 * @return the given type and its subtypes
	 * @throws CoreException
	 */
	public static List<IType> findSubtypes(final IType type)
			throws CoreException {
		final long start = System.currentTimeMillis();
		try {
			final List<IType> types = new ArrayList<IType>();
			final ITypeHierarchy returnTypeHierarchy = JdtUtils.resolveTypeHierarchy(type, type.getJavaProject(),
					false, new NullProgressMonitor());
			if (returnTypeHierarchy != null) {
				types.addAll(Arrays.asList(returnTypeHierarchy.getAllSubtypes(type)));
			}
			types.add(type);
			return types;
		} finally {
			Logger.tracePerf("Found subtypes of {} in {}ms", type.getFullyQualifiedName(),
					(System.currentTimeMillis() - start));
		}
	}

	/**
	 * Returns a list containing the given type and all its supertypes in the
	 * current project
	 * 
	 * @param progressMonitor
	 * @param type
	 * @return the given type and its subtypes
	 * @throws CoreException
	 */
	public static List<IType> findSupertypes(final IType type)
			throws CoreException {
		if(type == null) {
			return null;
		}
		final List<IType> types = new ArrayList<IType>();
		types.add(type);
		final ITypeHierarchy returnTypeHierarchy = JdtUtils
				.resolveTypeHierarchy(type, type.getJavaProject(), false,
						new NullProgressMonitor());
		types.addAll(Arrays.asList(returnTypeHierarchy.getAllSupertypes(type)));
		return types;
	}
	
	/**
	 * Resolves the SourceType Argument for the given parameterizedType against the
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
	public static List<IType> resolveTypeArguments(
			final IType parameterizedType,
			final CompilationUnit compilationUnit,
			final IType matchGenericType,
			final ITypeHierarchy parameterizedTypeHierarchy,
			final IProgressMonitor progressMonitor) throws CoreException {
		if (compilationUnit == null) {
			Logger.warn("Unable to retrieve the Compilation Unit for type '"
					+ parameterizedType
					+ "'. Check the library sources attachement.");
			return null;
		}
		// find path to the matchGenericType (class or interface)
		// ITypeHierarchy parameterizedTypeHierarchy =
		// getTypeHierarchy(parameterizedType, false, progressMonitor);
		List<IType> pathToParameterizedType = new ArrayList<IType>(
				Arrays.asList(parameterizedTypeHierarchy
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
		for (TypeDeclaration typeDeclaration : (List<TypeDeclaration>) compilationUnit
				.types()) {
			// ohoh, everything is resolved with bindings :-)
			ITypeBinding typeBinding = typeDeclaration.resolveBinding();
			if (typeBinding != null && typeBinding.getJavaElement().equals(parameterizedType)) {
				// locate the matchGenericType declaration...
				for (int i = 0; i < pathToParameterizedType.size(); i++) {
					IType superType = pathToParameterizedType.get(i);
					// lookup in the type's interfaces
					if (superType.isInterface()) {
						for (ITypeBinding superInterfaceBinding : typeBinding
								.getInterfaces()) {
							String superInterfaceErasureQName = superInterfaceBinding
									.getErasure().getQualifiedName();
							if (superInterfaceErasureQName.equals(superType
									.getFullyQualifiedName())) {
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
					Logger.trace("Resolving Java ElementKind for type argument '"
							+ typeArgBinding.getName() + "'");
					IJavaElement javaElement = typeArgBinding.getJavaElement();
					if (javaElement.getElementType() == IJavaElement.TYPE
							&& javaElement.exists()) {
						arguments.add((IType) javaElement);
					}
				}
			}
		}

		return arguments;
	}

	/**
	 * Returns the method signatures for <strong> all the methods within the
	 * given type</strong>, indexed by their handleIdentifier.
	 * 
	 * @param type the type containing methods to analyze.
	 * @param ast
	 *            the Compilation Unit AST.
	 * @return the JavaMethodSignatures or empty map if the given type or AST is null or
	 *         has no method.
	 * @throws JavaModelException 
	 */
	public static Map<String, JavaMethodSignature> resolveMethodSignatures(
			final IType type, final CompilationUnit ast) throws JavaModelException {
 		if(type == null || ast == null) {
			return Collections.emptyMap();
		}
		final Map<String, JavaMethodSignature> signatures = new HashMap<String, JavaMethodSignature>();
		for(IMethod method : type.getMethods()) {
			final JavaMethodSignature resolvedMethodSignature= resolveMethodSignature(method, ast);
			if(resolvedMethodSignature != null) {
				signatures.put(method.getHandleIdentifier(), resolvedMethodSignature);
			}
		}
		return signatures;
	}

	/**
	 * Returns the method signature for the given method with the given AST.
	 * 
	 * @param method
	 *            the java method
	 * @param ast
	 *            the associated Compilation Unit AST
	 * @return the JavaMethodSignature or null if the given {@code ast} is null or if the given {@code method} does not exist (anymore).
	 * @throws JavaModelException 
	 */
	public static JavaMethodSignature resolveMethodSignature(
			final IMethod method, final CompilationUnit ast) throws JavaModelException {
		if (ast == null || !method.exists()) {
			return null;
		}
		final ASTNode matchNode = NodeFinder.perform(ast, method.getNameRange());
		if(matchNode == null || matchNode.getParent() == null || matchNode.getParent().getNodeType() != ASTNode.METHOD_DECLARATION) {
			return null;
		}
		final MethodDeclaration methodDeclaration = ((MethodDeclaration)matchNode.getParent());
		final IMethodBinding methodBinding = methodDeclaration.resolveBinding();
		// method bindings may not be resolved (eg : duplicate method - see JBIDE-13580)
		if(methodBinding == null) { 
			return null; 
		}
		final SourceType returnedType = getReturnType(methodBinding, methodDeclaration);
		// .getReturnType().getJavaElement() : null;
		final List<IJavaMethodParameter> methodParameters = new ArrayList<IJavaMethodParameter>();
		@SuppressWarnings("unchecked")
		final List<SingleVariableDeclaration> parameters = methodDeclaration.parameters();
		for (int i = 0; i < parameters.size(); i++) {
			final SingleVariableDeclaration parameter = parameters.get(i);
			final ILocalVariable localVariable = method.getParameters()[i];
			final String paramName = parameter.getName().getFullyQualifiedName();
			final SourceType paramType = SourceType.from(parameter);
			final IVariableBinding paramBinding = parameter.resolveBinding();
			final List<Annotation> paramAnnotations = resolveParameterAnnotations(
					localVariable, paramBinding);
			
			methodParameters.add(new JavaMethodParameter(paramName, paramType, paramAnnotations, method.getResource()));
		}
		return new JavaMethodSignature(method, returnedType, methodParameters);
	}

	/**
	 * Resolves the fully qualified name of the given {@link IField}
	 * 
	 * @param javaField
	 *            the java field
	 * @return the fully qualified name, or null if it could not be resolved.
	 * @throws JavaModelException 
	 */
	public static SourceType resolveFieldType(final IField javaField, final CompilationUnit ast) {
		if (javaField == null || ast == null) {
			return null;
		}
		try {
			final ASTNode matchNode = NodeFinder.perform(ast, javaField.getNameRange());
			// matchNode should be a SimpleName belonging to a
			// VariableDeclarationFragment, itself belonging to a
			// FieldDeclaration:
			// FieldDeclaration: [Javadoc] { ExtendedModifier } SourceType
			// VariableDeclarationFragment {, VariableDeclarationFragment}
			if (matchNode == null || matchNode.getParent() == null || matchNode.getParent().getParent() == null
					|| matchNode.getParent().getParent().getNodeType() != ASTNode.FIELD_DECLARATION) {
				return null;
			}
			final FieldDeclaration fieldDeclaration = ((FieldDeclaration) matchNode.getParent().getParent());
			return SourceType.from(fieldDeclaration);
		} catch (JavaModelException e) {
			Logger.error("Failed to retrieve type for field " + javaField.getElementName(), e);
		}
		return null;
	}

	private static List<Annotation> resolveParameterAnnotations(
			final ILocalVariable localVariable,
			final IVariableBinding paramBinding) throws JavaModelException {
		final List<Annotation> paramAnnotations = new ArrayList<Annotation>();
		final IAnnotationBinding[] annotationBindings = paramBinding.getAnnotations();
		for (int j = 0; j < annotationBindings.length; j++) {
			if(j < localVariable.getAnnotations().length) {
				final IAnnotation javaAnnotation = localVariable.getAnnotations()[j];
				final IAnnotationBinding javaAnnotationBinding = annotationBindings[j];
				paramAnnotations.add(JdtUtils.toAnnotation(javaAnnotationBinding, javaAnnotation));
			}
		}
		return paramAnnotations;
	}
	
	/**
	 * Returns the Return SourceType for the given method or null of the return type
	 * could not be found or is 'void'
	 * 
	 * @param methodBinding
	 * @param methodDeclaration 
	 * @return
	 */
	private static SourceType getReturnType(final IMethodBinding methodBinding, final MethodDeclaration methodDeclaration) {
		try {
			if (methodBinding.getReturnType() != null && methodBinding.getReturnType().getJavaElement() != null) {
				return  SourceType.from(methodBinding.getReturnType(), methodDeclaration.getStartPosition(), methodDeclaration.getLength());
			}
		} 
		// https://issues.jboss.org/browse/JBIDE-15084: when compilation error (not syntax), retrieving return type may result in
		// an IllegalArgumentException
		catch(IllegalArgumentException e) {
			Logger.debug("Caught an IllegalArgumentException while trying to retrieve return type on method {}: {}", methodBinding, e.getMessage());
		}
		return null;
	}

	/**
	 * Return true if the given superType parameter is actually a super type of
	 * the given subType parameter, ie, the superType belongs to the supertypes
	 * in the subtype's hierarchy.
	 * 
	 * @param superType
	 *            the suspected super type
	 * @param subType
	 *            the suspected sub type
	 * @return true or false
	 * @throws CoreException
	 */
	public static boolean isTypeOrSuperType(IType superType, IType subType)
			throws CoreException {
		if (subType == null || superType == null) {
			return false;
		}
		if (superType.getHandleIdentifier().equals(
				subType.getHandleIdentifier())) {
			return true;
		}
		final ITypeHierarchy hierarchy = JdtUtils.resolveTypeHierarchy(subType,
				subType.getJavaProject(), true, new NullProgressMonitor());
		final List<IType> allSupertypes = Arrays.asList(hierarchy
				.getAllSupertypes(subType));
		for (IType type : allSupertypes) {
			if (type.getHandleIdentifier().equals(
					superType.getHandleIdentifier())) {
				return true;
			}
		}
		return false;
	}

	public static String getReadableMethodSignature(final IMethod method) {
		StringBuilder name = new StringBuilder();
		name.append(method.getElementName()).append("(");
		String comma = "";
		String[] parameterTypes = method.getParameterTypes();
		try {
			String[] parameterNames = method.getParameterNames();
			for (int i = 0; i < method.getParameterTypes().length; ++i) {
				name.append(comma);
				name.append(Signature.toString(parameterTypes[i]));
				name.append(" ");
				name.append(parameterNames[i]);
				comma = ", ";
			}
		} catch (JavaModelException e) {
		}

		name.append(")");

		return name.toString();
	}

	/**
	 * Returns a short/displayable form of the given fully qualified name. For
	 * example, removes the {@code java.lang} package name for String, converts
	 * to the primitive type for {@link Long}, {@link Integer}, {@link Double},
	 * {@link Float}, {@link Boolean}, etc. If the given type is already a
	 * primitive type, it remains unchanged.
	 *
	 * @param typeName
	 *            the name
	 * @return the short/displayable name
	 */
	public static String toDisplayableTypeName(final String typeName) {
		if (typeName == null) {
			return null;
		} else if (typeName.startsWith(List.class.getName() + "<")) {
			final String parameterType = typeName.substring(List.class.getName().length() + 1, typeName.length() - 1);
			return "List<" + toDisplayableTypeName(parameterType) + ">";
		} else if (typeName.startsWith(Set.class.getName() + "<")) {
			final String parameterType = typeName.substring(Set.class.getName().length() + 1, typeName.length() - 1);
			return "Set<" + toDisplayableTypeName(parameterType) + ">";
		} else if (typeName.startsWith(SortedSet.class.getName() + "<")) {
			final String parameterType = typeName.substring(SortedSet.class.getName().length() + 1, typeName.length() - 1);
			return "SortedSet<" + toDisplayableTypeName(parameterType) + ">";
		} else if (typeName.contains(".")) {
			return typeName.substring(typeName.lastIndexOf(".") + 1);
		}
		return typeName;
	}

	/**
	 * @param methodSignature the method signature
	 * @return the type of the first parameter or the return type, depending if the method name starts with "set" or "get", respectively.
	 * @throws JavaModelException 
	 * @throws IllegalArgumentException 
	 */
	public static SourceType getPropertyType(final IJavaMethodSignature methodSignature) throws IllegalArgumentException, JavaModelException {
		if(methodSignature != null) {
			final IMethod javaMethod = methodSignature.getJavaMethod();
			if(javaMethod != null && javaMethod.getElementName().startsWith("set") && Signature.getParameterCount(javaMethod.getSignature()) == 1) {
				return methodSignature.getMethodParameters().get(0).getType();
			}
		}
		return null;
	}
	
	/**
	 * @return {@code true} if the given method is a valid setter, ie, its name starts with "set" and it has a single parameter.
	 * @throws JavaModelException if the method signature is not readable
	 * @throws IllegalArgumentException 
	 */
	public static boolean isSetter(final IMethod javaMethod) throws JavaModelException {
		return javaMethod != null && javaMethod.getElementName().startsWith("set") ;
	}

	/**
	 * Makes the given {@link IMember}'s {@link ICompilationUnit} if it is not already the case (and if it exists).
	 * @throws JavaModelException
	 * @See {@link ICompilationUnit#isConsistent()}
	 * @See {@link ICompilationUnit#makeConsistent(IProgressMonitor)}
	 */
	public static void makeConsistentIfNecessary(final IMember javaMember) throws JavaModelException {
		if(javaMember!= null && javaMember.getCompilationUnit() != null && javaMember.getCompilationUnit().exists() && !javaMember.getCompilationUnit().isConsistent()) {
			Logger.trace("Making {} consistent...", javaMember.getCompilationUnit().getElementName());
			javaMember.getCompilationUnit().makeConsistent(new NullProgressMonitor());
		}
	}
}
