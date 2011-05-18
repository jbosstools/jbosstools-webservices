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

package org.jboss.tools.ws.jaxrs.ui.contentassist;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;

/**
 * AST Visitor meant to retrieve the binding for the region enclosing the offset
 * location given in the constructor. This class is used to determine the
 * element binding of the enclosing region during content assist processing.
 * 
 * @author xcoulon
 * 
 */
public class TypedRegionVisitor extends ASTVisitor {

	/** The offset location in the compilation unit. */
	private final int offset;
	
	/** the resolved element binding. */
	private IAnnotationBinding binding;

	/**
	 * The full constructor.
	 * @param o the offset location
	 */
	public TypedRegionVisitor(final int o) {
		this.offset = o;
	}

	/**
	 * {@inheritDoc}
	 *  (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.SingleMemberAnnotation)
	 */
	@Override
	public boolean visit(SingleMemberAnnotation node) {
		int startPosition = node.getStartPosition();
		int endPosition = startPosition + node.getLength();
		if (startPosition <= offset && offset <= endPosition) {
			binding = node.resolveAnnotationBinding();
		}
		return super.visit(node);
	}

	/**
	 * @return the binding
	 */
	public final IAnnotationBinding getBinding() {
		return binding;
	}

}
