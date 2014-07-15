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
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.SourceRange;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.VariableDeclaration;

/**
 * A Java SourceType, with support for generic types, and location in source code (as a field declaration, a method return type or argument declaration).
 * @author xcoulon
 *
 */
public class SourceType {

	/**
	 * Factory method for the {@link SourceType}
	 * @param declaration the {@link VariableDeclaration} 
	 * @return the {@link SourceType}
	 */
	public static SourceType from(final VariableDeclaration declaration) {
		final IVariableBinding paramBinding = declaration.resolveBinding();
		if (paramBinding != null) {
			final String erasureName = paramBinding.getType().getErasure().getQualifiedName();
			final IType erasureType = (IType) paramBinding.getType().getErasure().getJavaElement();
			final List<IType> typeArguments = new ArrayList<IType>();
			final ISourceRange nameRange = new SourceRange(declaration.getStartPosition(), declaration.getLength());
			for (ITypeBinding typeArgumentBinding : paramBinding.getType().getTypeArguments()) {
				typeArguments.add((IType) typeArgumentBinding.getJavaElement());
			}
			return new SourceType(erasureName, erasureType, typeArguments, paramBinding.getType().isPrimitive(), nameRange);
		}
		return null;
	}

	/**
	 * Factory method for the {@link SourceType}
	 * @param parameter the {@link FieldDeclaration} 
	 * @return the {@link SourceType}
	 */
	public static SourceType from(final FieldDeclaration declaration) {
		if(declaration != null && declaration.getType() != null) {
			return from(declaration.getType().resolveBinding(), declaration.getStartPosition(), declaration.getLength());
		}
		return null;
	}
	
	/**
	 * Factory method for the {@link SourceType}
	 * @param typeBinding the {@link ITypeBinding} 
	 * @param startPosition 
	 * @param length 
	 * @return the {@link SourceType}
	 */
	public static SourceType from(final ITypeBinding typeBinding, final int startPosition, final int length) {
		if(typeBinding != null) {
			final String erasureName = typeBinding.getErasure().getQualifiedName();
			final IType erasureType = (IType) typeBinding.getErasure().getJavaElement();
			final List<IType> typeArguments = new ArrayList<IType>();
			final ISourceRange nameRange = new SourceRange(startPosition, length);
			for (ITypeBinding typeArgumentBinding : typeBinding.getTypeArguments()) {
				IJavaElement je = typeArgumentBinding.getJavaElement();
				// For template classes it could be <T> and je above would be instance of
				// org.eclipse.jdt.internal.core.TypeParameter so instanceof check is required 
				// before converting to IType
				if (je instanceof IType) {
					typeArguments.add((IType) je);
				}
			}
			return new SourceType(erasureName, erasureType, typeArguments, typeBinding.isPrimitive(), nameRange);
			
		}
		return null;
	}

	/**
	 * @return {@code true} if the source type exists, false otherwise.
	 */
	public boolean exists() {
		if(this.erasureType == null || !this.erasureType.exists()) {
			return false;
		}
		for(IType typeArg : typeArguments) {
			if(typeArg == null || !typeArg.exists()) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * The qualified name of the parameter's type, including erasure and type
	 * arguments.
	 */
	private final String displayName;

	/** The fully qualified name of the type's erasure. */
	private final String erasureName;
	
	/** The type's erasure. */
	private final IType erasureType;

	/** The types of this source type's arguments (or empty list). */
	private List<IType> typeArguments;

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
	 * @param typeArguments
	 * @param isPrimitive
	 */
	private SourceType(final String erasureName, final IType erasureType, final List<IType> typeArguments, final boolean isPrimitive, final ISourceRange nameRange) {
		super();
		this.erasureName = erasureName;
		this.erasureType = erasureType;
		this.typeArguments = typeArguments;
		this.isPrimitive = isPrimitive;
		final StringBuffer displayNameBuffer = new StringBuffer(erasureName);
		if (!typeArguments.isEmpty()) {
			displayNameBuffer.append('<');
			for (Iterator<IType> typeArgumentsIterator = typeArguments.iterator(); typeArgumentsIterator
					.hasNext();) {
				displayNameBuffer.append(typeArgumentsIterator.next().getElementName());
				if (typeArgumentsIterator.hasNext()) {
					displayNameBuffer.append(',');
				}
			}
			displayNameBuffer.append('>');
		}
		this.displayName = displayNameBuffer.toString();
		this.nameRange = nameRange;
	}
	
	/**
	 * @return the fully qualified name of the SourceType Erasure.
	 */
	public String getErasureName() {
		return erasureName;
	}
	
	/**
	 * @return the Erasure as an {@link IType}.
	 */
	public IType getErasureType() {
		return erasureType;
	}


	/**
	 * @return {@code true} if this parameter is a primitive type, {@code false}
	 *         otherwise.
	 */
	public boolean isPrimitive() {
		return this.isPrimitive;
	}
	
	/**
	 * @return the parameter's list of SourceType Arguments, or empty list.
	 */
	public List<IType> getTypeArguments() {
		return typeArguments;
	}

	/** @return the parameter type name in a shorter/displayable form. */
	public String getDisplayableTypeName() {
		return JdtUtils.toDisplayableTypeName(displayName);
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
		result = prime * result + ((displayName == null) ? 0 : displayName.hashCode());
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
		SourceType other = (SourceType) obj;
		if (displayName == null) {
			if (other.displayName != null) {
				return false;
			}
		} else if (!displayName.equals(other.displayName)) {
			return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		return displayName;
	}


}
