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

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

/**
 * {@link ASTVisitor} that will look for a given java {@link IField} and attempt to resolve
 * the fully qualified name of its type.
 * @author xcoulon
 *
 */
public class JavaFieldsVisitor extends ASTVisitor {

	/** The Java field to look up.*/
	private final IField javaField;

	/** The resolve type name of the field.*/
	private String resolvedFieldTypeName;
	
	/**
	 * Constructor
	 * @param javaField the java field to look-up.
	 */
	public JavaFieldsVisitor(final IField javaField) {
		this.javaField = javaField;
	}

	@Override
	public boolean visit(FieldDeclaration node) {
		@SuppressWarnings("unchecked")
		final List<VariableDeclarationFragment> fragments = (List<VariableDeclarationFragment>) node.getStructuralProperty(FieldDeclaration.FRAGMENTS_PROPERTY);
		for(VariableDeclarationFragment fragment : fragments) {
			final String name = fragment.getName().getIdentifier();
			if(name.equals(javaField.getElementName())) {
				final ITypeBinding typeBinding = node.getType().resolveBinding();
				resolvedFieldTypeName = typeBinding.getQualifiedName();
				break;
			}
		}
		return false;
	}
	

	/**
	 * Voluntary skipping method declaration visit to reduce cost of field type name resoolution.
	 * 
	 */
	@Override
	public boolean visit(MethodDeclaration node) {
		return false;
	}

	/**
	 * @return the resolvedFieldTypeName
	 */
	public String getResolvedFieldTypeName() {
		return resolvedFieldTypeName;
	}

}