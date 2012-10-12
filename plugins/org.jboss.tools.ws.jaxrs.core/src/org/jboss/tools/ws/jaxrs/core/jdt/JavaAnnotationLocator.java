/******************************************************************************* 
 * Copyright (c) 2012 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.ws.jaxrs.core.jdt;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

/**
 * @author Xavier Coulon
 * 
 */
public class JavaAnnotationLocator extends ASTVisitor {

	private final int location;
	private Annotation locatedAnnotation;

	public JavaAnnotationLocator(final int location) {
		this.location = location;
	}

	/**
	 * @return the locatedJavaAnnotation
	 */
	public Annotation getLocatedAnnotation() {
		return locatedAnnotation;
	}

	/**
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.AnnotationTypeDeclaration)
	 */
	@Override
	public boolean visit(AnnotationTypeDeclaration node) {
		visitExtendedModifiers((List<?>) node.getStructuralProperty(AnnotationTypeDeclaration.MODIFIERS2_PROPERTY));
		// no need to visit furthermore
		return false;
	}

	/**
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.FieldDeclaration)
	 */
	@Override
	public boolean visit(FieldDeclaration node) {
		visitExtendedModifiers((List<?>) node.getStructuralProperty(FieldDeclaration.MODIFIERS2_PROPERTY));
		// no need to visit furthermore
		return false;
	}

	/**
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.MethodDeclaration)
	 */
	@Override
	public boolean visit(MethodDeclaration node) {
		visitExtendedModifiers((List<?>) node.getStructuralProperty(MethodDeclaration.MODIFIERS2_PROPERTY));
		// visit children to look for SingleVariableDeclaration
		return true;
	}

	/**
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.TypeDeclaration)
	 */
	@Override
	public boolean visit(TypeDeclaration node) {
		visitExtendedModifiers((List<?>) node.getStructuralProperty(TypeDeclaration.MODIFIERS2_PROPERTY));
		// no need to visit furthermore
		return false;
	}

	/**
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.SingleVariableDeclaration)
	 */
	@Override
	public boolean visit(SingleVariableDeclaration node) {
		visitExtendedModifiers((List<?>) node.getStructuralProperty(SingleVariableDeclaration.MODIFIERS2_PROPERTY));
		// no need to visit furthermore
		return false;
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
				if (DOMUtils.nodeMatches(annotation, location)) {
					final IAnnotationBinding annotationBinding = annotation.resolveAnnotationBinding();
					if (annotationBinding != null) {
						this.locatedAnnotation = BindingUtils.toAnnotation(annotationBinding);
					}
				}
			}
		}
	}

}
