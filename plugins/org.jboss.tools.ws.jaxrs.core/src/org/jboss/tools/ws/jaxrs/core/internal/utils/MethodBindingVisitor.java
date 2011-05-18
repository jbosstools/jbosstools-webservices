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

package org.jboss.tools.ws.jaxrs.core.internal.utils;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

/**
 * A visitor for a single annotation on a java member (can be a method or a
 * type).
 * 
 * @author xcoulon
 * 
 */
public class MethodBindingVisitor extends ASTVisitor {

	/** the method to match during the visit. */
	private final IMethod method;

	/** the bindings for the matching method. */
	private IMethodBinding methodBinding = null;


	/**
	 * Full Constructor to resolve method binding.
	 * 
	 * @param m
	 *            the method to match during the visit
	 */
	public MethodBindingVisitor(final IMethod m) {
		super();
		this.method = m;
	}
	
	@Override
	public final boolean visit(final MethodDeclaration node) {
		try {
			ISourceRange sourceRange = method.getSourceRange();
			int position = node.getStartPosition();
			int begin = sourceRange.getOffset();
			int end = sourceRange.getOffset() + sourceRange.getLength();
			if (node.getName().getFullyQualifiedName().equals(method.getElementName())
					// need to take care about comments above a method declaration
					&& begin <= position
					&& position <= end) {
				methodBinding = node.resolveBinding();
			}
		} catch (JavaModelException e) {
			Logger.error("Error while resolving method bindings", e);
		}
		return false;
	}
	
	/**
	 * @return the method binding that correspond to the method given in the instance constructor.
	 */
	public final IMethodBinding getMethodBinding() {
		return methodBinding;
	}

}