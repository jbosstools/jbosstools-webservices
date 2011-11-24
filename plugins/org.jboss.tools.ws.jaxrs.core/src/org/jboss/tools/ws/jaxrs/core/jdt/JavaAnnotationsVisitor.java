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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TypedRegion;

/**
 * A visitor for a single annotation on a java member (can be a method or a
 * type).
 * 
 * @author xcoulon
 */
public class JavaAnnotationsVisitor extends ASTVisitor {

	/** the annotated member name. */
	private final String memberName;

	/** the annotated member type. */
	private final int memberType;

	/** the name of the annotation. */
	private final List<String> annotationNames = new ArrayList<String>();

	/** the bindings for the matching annotation. */
	private final List<Annotation> annotations = new ArrayList<Annotation>();

	/**
	 * Full Constructor to resolve a single annotation from its fully qualified
	 * name.
	 * 
	 * @param name
	 *            the member name
	 * @param memberType
	 *            the member type
	 * @param name
	 *            the annotation name
	 */
	public JavaAnnotationsVisitor(final IMember member, final String annotationName) {
		super();
		this.memberName = member.getElementName();
		this.memberType = member.getElementType();
		this.annotationNames.add(annotationName);
	}

	/**
	 * Full Constructor to resolve a multiple annotations from their fully
	 * qualified name.
	 * 
	 * @param name
	 *            the member name
	 * @param memberType
	 *            the member type
	 * @param name
	 *            the annotation name
	 */
	public JavaAnnotationsVisitor(final IMember member, final List<String> annotationNames) {
		super();
		this.memberName = member.getElementName();
		this.memberType = member.getElementType();
		this.annotationNames.addAll(annotationNames);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.AnnotationTypeDeclaration)
	 */
	@Override
	public final boolean visit(final AnnotationTypeDeclaration node) {
		if (memberType == IJavaElement.TYPE && node.getName().getFullyQualifiedName().equals(memberName)) {
			visitExtendedModifiers((List<?>) node.getStructuralProperty(AnnotationTypeDeclaration.MODIFIERS2_PROPERTY));
			return false;
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.
	 *      TypeDeclaration)
	 */
	@Override
	public final boolean visit(final TypeDeclaration node) {
		if (memberType == IJavaElement.TYPE && node.getName().getFullyQualifiedName().equals(memberName)) {
			visitExtendedModifiers((List<?>) node.getStructuralProperty(TypeDeclaration.MODIFIERS2_PROPERTY));
			return false;
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.
	 *      MethodDeclaration)
	 */
	@Override
	public final boolean visit(final MethodDeclaration node) {
		if (memberType == IJavaElement.METHOD && node.getName().getFullyQualifiedName().equals(memberName)) {
			visitExtendedModifiers((List<?>) node.getStructuralProperty(MethodDeclaration.MODIFIERS2_PROPERTY));
			return false;
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.
	 *      MethodDeclaration)
	 */
	@Override
	public final boolean visit(final FieldDeclaration node) {
		if (memberType == IJavaElement.FIELD) {
			VariableDeclarationFragment fragment = (VariableDeclarationFragment) (node.fragments().get(0));
			if (fragment.getName().toString().equals(memberName)) {
				visitExtendedModifiers((List<?>) node.getStructuralProperty(FieldDeclaration.MODIFIERS2_PROPERTY));
				return false;
			}
		}
		return true;
	}

	/**
	 * Visits the modifiers.
	 * 
	 * @param modifiers
	 *            the modifiers
	 */
	private void visitExtendedModifiers(final List<?> modifiers) {
		for (Object modifier : modifiers) {
			if (modifier instanceof org.eclipse.jdt.core.dom.Annotation) {
				final org.eclipse.jdt.core.dom.Annotation annotation = (org.eclipse.jdt.core.dom.Annotation) modifier;
				IAnnotationBinding annotationBinding = ((org.eclipse.jdt.core.dom.Annotation) modifier)
						.resolveAnnotationBinding();
				if (annotationBinding != null) {
					final String qualifiedName = annotationBinding.getAnnotationType().getQualifiedName();
					final String name = annotationBinding.getAnnotationType().getName();
					if (annotationNames.contains(qualifiedName) || annotationNames.contains(name)) {
						final String annotationName = annotationBinding.getAnnotationType().getQualifiedName();
						final Map<String, List<String>> annotationElements = resolveAnnotationElements(annotationBinding);
						final TypedRegion typedRegion = new TypedRegion(annotation.getStartPosition(),
								annotation.getLength(), IDocument.DEFAULT_CONTENT_TYPE);
						final IAnnotation javaAnnotation = (IAnnotation) annotationBinding.getJavaElement();
						annotations
								.add(new Annotation(javaAnnotation, annotationName, annotationElements, typedRegion));
					}
				}
			}
		}
	}

	/**
	 * Returns the Annotation element matching the annotation name given in the
	 * visitor constructor. This method should only be called when the
	 * constructor with a single annotation name was used.
	 * 
	 * @return the annotation found on the target java element
	 * @throws JavaModelException
	 *             in case of underlying exception
	 */
	public final Annotation getResolvedAnnotation() throws JavaModelException {
		assert annotationNames.size() == 1;
		if (annotations.size() == 0) {
			return null;
		}
		return annotations.get(0);
	}

	/**
	 * Returns the Annotation elements matching the annotations name given in
	 * the visitor constructor. The matching annotations are indexed by their
	 * associated Java type's fully qualified names. This method should only be
	 * called when the constructor with multiple annotation names was used.
	 * 
	 * @return the annotation found on the target java element
	 * @throws JavaModelException
	 *             in case of underlying exception
	 */
	public final Map<String, Annotation> getResolvedAnnotations() throws JavaModelException {
		final Map<String, Annotation> resolvedJavaAnnotations = new HashMap<String, Annotation>();
		for (Annotation annotation : annotations) {
			resolvedJavaAnnotations.put(annotation.getName(), annotation);
		}
		return resolvedJavaAnnotations;
	}

	private static Map<String, List<String>> resolveAnnotationElements(IAnnotationBinding annotationBinding) {
		final Map<String, List<String>> annotationElements = new HashMap<String, List<String>>();
		try {
			for (IMemberValuePairBinding binding : annotationBinding.getAllMemberValuePairs()) {
				final List<String> values = new ArrayList<String>();
				if (binding.getValue() instanceof Object[]) {
					for (Object v : (Object[]) binding.getValue()) {
						values.add(v.toString());
					}
				} else {
					values.add(binding.getValue().toString());
				}
				annotationElements.put(binding.getName(), values);
			}
			// if the code is not valid, the underlying DefaultValuePairBinding
			// may throw a NPE:
			// at
			// org.eclipse.jdt.core.dom.DefaultValuePairBinding.<init>(DefaultValuePairBinding.java:31)
			// at
			// org.eclipse.jdt.core.dom.AnnotationBinding.getAllMemberValuePairs(AnnotationBinding.java:98)
		} catch (Throwable e) {
			// silently ignore
		}
		return annotationElements;
	}

}