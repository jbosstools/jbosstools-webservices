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

package org.jboss.tools.ws.jaxrs.core.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.SourceRange;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.VariableDeclaration;

/**
 * @author xcoulon
 *
 */
public class ParameterType {

	/**
	 * Factory method for the {@link ParameterType}
	 * @param declaration the {@link VariableDeclaration} 
	 * @return the {@link ParameterType}
	 */
	public static ParameterType from(final VariableDeclaration declaration) {
		final IVariableBinding paramBinding = declaration.resolveBinding();
		if (paramBinding != null) {
			final String erasureName = paramBinding.getType().getErasure().getQualifiedName();
			final List<String> typeArgumentNames = new ArrayList<String>();
			final ISourceRange nameRange = new SourceRange(declaration.getStartPosition(), declaration.getLength());
			for (ITypeBinding typeArgumentBinding : paramBinding.getType().getTypeArguments()) {
				typeArgumentNames.add(typeArgumentBinding.getQualifiedName());
			}
			return new ParameterType(erasureName, typeArgumentNames, paramBinding.getType().isPrimitive(), nameRange);
		}
		return null;
	}

	/**
	 * Factory method for the {@link ParameterType}
	 * @param parameter the {@link FieldDeclaration} 
	 * @return the {@link ParameterType}
	 */
	public static ParameterType from(final FieldDeclaration declaration) {
		final ITypeBinding paramBinding = declaration.getType().resolveBinding();
		if (paramBinding != null) {
			final String erasureName = paramBinding.getErasure().getQualifiedName();
			final List<String> typeArgumentNames = new ArrayList<String>();
			final ISourceRange nameRange = new SourceRange(declaration.getStartPosition(), declaration.getLength());
			for (ITypeBinding typeArgumentBinding : paramBinding.getTypeArguments()) {
				typeArgumentNames.add(typeArgumentBinding.getQualifiedName());
			}
			return new ParameterType(erasureName, typeArgumentNames, paramBinding.isPrimitive(), nameRange);
		}
		return null;
	}
	



	
	/**
	 * The qualified name of the parameter's type, including erasure and type
	 * arguments.
	 */
	private final String qualifiedName;

	/** The qualified name of the parameter's erasure. */
	private final String erasureName;

	/** The qualified names of the parameter's type argument (or empty list). */
	private List<String> typeArgumentNames;

	/**
	 * boolean flag to indicate if the parameter (erasure) is a primitive type
	 * or not.
	 */
	private final boolean isPrimitive;
	
	/** The name range of the type in the source.*/
	private final ISourceRange nameRange;

	/**
	 * Full constructor 
	 * @param erasureName
	 * @param typeArgumentNames
	 * @param isPrimitive
	 */
	private ParameterType(final String erasureName, final List<String> typeArgumentNames, final boolean isPrimitive, final ISourceRange nameRange) {
		super();
		this.erasureName = erasureName;
		this.typeArgumentNames = typeArgumentNames;
		this.isPrimitive = isPrimitive;
		final StringBuffer typeNameBuffer = new StringBuffer(erasureName);
		if (!typeArgumentNames.isEmpty()) {
			typeNameBuffer.append('<');
			for (Iterator<String> typeArgumentNameIterator = typeArgumentNames.iterator(); typeArgumentNameIterator
					.hasNext();) {
				typeNameBuffer.append(typeArgumentNameIterator.next());
				if (typeArgumentNameIterator.hasNext()) {
					typeNameBuffer.append(',');
				}
			}
			typeNameBuffer.append('>');
		}
		this.qualifiedName = typeNameBuffer.toString();
		this.nameRange = nameRange;
	}
	
	/**
	 * @return the parameter's Erasure.
	 */
	public String getErasureName() {
		return erasureName;
	}

	/**
	 * @return {@code true} if this parameter is a primitive type, {@code false}
	 *         otherwise.
	 */
	public boolean isPrimitive() {
		return this.isPrimitive;
	}

	/**
	 * @return the parameter's list of Type Arguments, or empty list.
	 */
	public List<String> getTypeArgumentNames() {
		return typeArgumentNames;
	}

	/**
	 * @return the parameter fully qualified type name (including type arguments)
	 */
	public String getQualifiedName() {
		return this.qualifiedName;
	}

	/** @return the parameter type name in a shorter/displayable form. */
	public String getDisplayableTypeName() {
		return JdtUtils.toDisplayableTypeName(qualifiedName);
	}

	/**
	 * @return the name range for the parameter
	 */
	public ISourceRange getNameRange() {
		return this.nameRange;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((qualifiedName == null) ? 0 : qualifiedName.hashCode());
		return result;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ParameterType other = (ParameterType) obj;
		if (qualifiedName == null) {
			if (other.qualifiedName != null) {
				return false;
			}
		} else if (!qualifiedName.equals(other.qualifiedName)) {
			return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		return qualifiedName;
	}

}
