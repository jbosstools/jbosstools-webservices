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

package org.jboss.tools.ws.jaxrs.ui.contentassist;

import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;

public class MemberDeclarationVisitor extends ASTVisitor {

	private final IMember member;

	private String sourceOverview;

	public MemberDeclarationVisitor(IMember member) {
		this.member = member;
	}

	/**
	 * @return the sourceOverview
	 */
	public String getSourceOverview() {
		return sourceOverview;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.
	 * MethodDeclaration)
	 */
	@Override
	public boolean visit(MethodDeclaration node) {
		if (sourceOverview == null) {
			try {
				ISourceRange sourceRange = member.getSourceRange();
				int memberStartPosition = sourceRange.getOffset();
				int memberEndPosition = memberStartPosition + sourceRange.getLength();
				int nodeStartPosition = node.getStartPosition();
				if (memberStartPosition <= nodeStartPosition && nodeStartPosition <= memberEndPosition) {
					Javadoc javaDoc = node.getJavadoc();
					Block body = node.getBody();
					node.getLocationInParent();
					int beginIndex = (javaDoc != null) ? (javaDoc.getStartPosition() + javaDoc.getLength()):member.getSourceRange().getOffset();
					sourceOverview = member.getCompilationUnit().getSource()
							.substring(beginIndex, body.getStartPosition())
							.trim();
				}
			} catch (JavaModelException e) {
				Logger.warn("Failed to visit node", e);
			}
		}
		return super.visit(node);
	}

}
