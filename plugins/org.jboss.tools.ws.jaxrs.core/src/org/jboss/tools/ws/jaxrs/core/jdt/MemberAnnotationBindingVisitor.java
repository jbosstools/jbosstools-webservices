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

import java.util.List;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

/**
 * A visitor for a single annotation on a java member (can be a method or a
 * type).
 * 
 * @author xcoulon
 * 
 */
public class MemberAnnotationBindingVisitor extends ASTVisitor {

	/** the name of the member. */
	private final String memberName;

	/** the type of the member. */
	private final int memberType;

	/** the name of the annotation. */
	private final String annotationName;

	/** the bindings for the matching annotation. */
	private IAnnotationBinding annotationBinding = null;

	/**
	 * Full Constructor to resolve annotation binding from its fully qualified
	 * name.
	 * 
	 * @param mName
	 *            the member name
	 * @param mType
	 *            the member type
	 * @param aName
	 *            the annotation name
	 */
	public MemberAnnotationBindingVisitor(final String mName, final int mType, final String aName) {
		super();
		this.memberName = mName;
		this.memberType = mType;
		this.annotationName = aName;
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
			if (modifier instanceof Annotation) {
				IAnnotationBinding binding = ((Annotation) modifier).resolveAnnotationBinding();
				if (annotationName != null
						&& (annotationName.equals(binding.getAnnotationType().getQualifiedName()) || annotationName
								.equals(binding.getAnnotationType().getName()))) {
					annotationBinding = binding;
					break;
				}
			}
		}
	}

	/**
	 * @return the annotation binding found during the call to visit().
	 */
	public final IAnnotationBinding getAnnotationBinding() {
		return annotationBinding;
	}

}