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

import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.SourceRange;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

/**
 * Visitor that will "visit" an ASTNode and its children until it finds the
 * expected MemberValue pair to retain its location in the compilation unit
 * source code
 * 
 * @author Xavier Coulon
 */
public class MemberValuePairLocationRetriever extends ASTVisitor {

	private final IAnnotation javaAnnotation;
	private final ISourceRange javaAnnotationSourceRange;
	private final String memberName;

	private ISourceRange locatedSourceRange = null;

	/**
	 * Constructor
	 * 
	 * @throws JavaModelException
	 */
	public MemberValuePairLocationRetriever(final IAnnotation javaAnnotation, final String memberName)
			throws JavaModelException {
		this.javaAnnotation = javaAnnotation;
		this.memberName = memberName;
		this.javaAnnotationSourceRange = javaAnnotation.getSourceRange();
	}

	public ISourceRange getMemberValuePairSourceRange() {
		return locatedSourceRange;
	}

	/**
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.TypeDeclaration)
	 */
	@Override
	public boolean visit(AnnotationTypeDeclaration node) {
		final IJavaElement ancestor = javaAnnotation.getAncestor(IJavaElement.TYPE);
		if (ancestor != null && ancestor.exists()
				&& ancestor.getElementName().equals(node.getName().getFullyQualifiedName())) {
			// keep searching
			return true;
		}
		// wrong path, stop searching from this branch of the AST
		return false;
	}

	/**
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.TypeDeclaration)
	 */
	@Override
	public boolean visit(TypeDeclaration node) {
		final IJavaElement ancestor = javaAnnotation.getAncestor(IJavaElement.TYPE);
		if (ancestor != null && ancestor.exists()
				&& ancestor.getElementName().equals(node.getName().getFullyQualifiedName())) {
			// keep searching
			return true;
		}
		// wrong path, stop searching from this branch of the AST
		return false;
	}

	/**
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.FieldDeclaration)
	 */
	@Override
	public boolean visit(VariableDeclarationFragment node) {
		final IJavaElement ancestor = javaAnnotation.getAncestor(IJavaElement.FIELD);
		if (ancestor != null && ancestor.exists()
				&& ancestor.getElementName().equals(node.getName().getFullyQualifiedName())) {
			// keep searching
			return true;
		}
		// wrong path, stop searching from this branch of the AST
		return false;
	}

	/**
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.MethodInvocation)
	 */
	@Override
	public boolean visit(MethodDeclaration node) {
		final IJavaElement ancestor = javaAnnotation.getAncestor(IJavaElement.METHOD);
		if (ancestor != null && ancestor.exists()
				&& ancestor.getElementName().equals(node.getName().getFullyQualifiedName())) {
			// keep searching
			return true;
		}
		// wrong path, stop searching from this branch of the AST
		return false;
	}

	/**
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.SingleMemberAnnotation)
	 */
	@Override
	public boolean visit(SingleMemberAnnotation node) {
		if (RangeUtils.matches(javaAnnotationSourceRange, node.getStartPosition())) {
			this.locatedSourceRange = new SourceRange(node.getValue().getStartPosition(), node.getValue().getLength());
		}
		// no need to keep searching (and there should be no child node anyway..)
		return false;
	}

	/**
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.NormalAnnotation)
	 */
	@Override
	public boolean visit(NormalAnnotation node) {
		final IJavaElement ancestor = javaAnnotation.getAncestor(IJavaElement.ANNOTATION);
		if (ancestor != null && ancestor.exists()
				&& ancestor.getElementName().equals(node.getTypeName().getFullyQualifiedName())) {
			// move down to the MemberValuePair node(s)
			return true;
		}
		// wrong path, stop searching from this branch of the AST
		return false;
	}

	/**
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.MemberValuePair)
	 */
	@Override
	public boolean visit(MemberValuePair node) {
		if (RangeUtils.matches(javaAnnotationSourceRange, node.getStartPosition())
				&& node.getName().getFullyQualifiedName().equals(memberName)) {
			this.locatedSourceRange = new SourceRange(node.getStartPosition(), node.getLength());
		}
		// no need to drill down from here anyway (there should be no child node..)
		return false;
	}

}
