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

package org.jboss.tools.ws.jaxrs.ui.internal.validation;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResourceField;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.utils.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.utils.ParameterType;

/**
 * Validates that a given {@link IType} used in a {@link IJaxrsResourceMethod}
 * or a {@link IJaxrsResourceField} is one of the following:
 * <ul>
 * <li>Primitive types.</li>
 * <li>Types that have a constructor that accepts a single String argument.</li>
 * <li>Types that have a static method named valueOf or fromString with a single
 * String argument that return an instance of the type.</li>
 * <li>{@code List<T>} , {@code Set<T>}, or {@code SortedSet<T>}, where
 * {@code T} satisfies 2 or 3 above.</li>
 * 
 * @author xcoulon
 *
 */
public class JaxrsParameterValidatorDelegate {

	/**
	 * Validates a parameter, given its erasure and it (optional) type arguments
	 * 
	 * @param type
	 *            the parameter type
	 * @return {@code true} if the given arguments are valid against the JAX-RS
	 *         specification, {@code false} otherwise.
	 * @throws CoreException
	 */
	public boolean validate(final ParameterType type, final IJavaProject javaProject,
			final IProgressMonitor progressMonitor) throws CoreException {
		if (type.isPrimitive()) {
			return true;
		}
		final IType erasureType = JdtUtils.resolveType(type.getErasureName(), javaProject, progressMonitor);
		if(erasureType == null) {
			return false;
		}
		if (validate(erasureType)) {
			return true;
		}
		final String erasureQualifiedName = erasureType.getFullyQualifiedName();
		if (erasureQualifiedName.equals(Set.class.getName()) || erasureQualifiedName.equals(List.class.getName())
				|| erasureQualifiedName.equals(SortedSet.class.getName())) {
			for (String typeArgumentName : type.getTypeArgumentNames()) {
				final IType typeArgument = JdtUtils.resolveType(typeArgumentName, javaProject, progressMonitor);
				if (validate(typeArgument)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Checks that the given {@link IType} has one of the following:
	 * <ul>
	 * <li>a constructor that accepts a single String argument.</li>
	 * <li>a static method named {@code valueOf} with a single String argument
	 * that return an instance of the type.</li>
	 * <li>a static method named {@code fromString} with a single String
	 * argument that return an instance of the type.</li>
	 * </ul>
	 * 
	 * @param type
	 *            the type to validate
	 * @return {@code true} if it matches, {@code false} otherwise.
	 * @throws JavaModelException
	 */
	private boolean validate(final IType type) throws JavaModelException {
		final String qualifiedName = type.getFullyQualifiedName();
		final String simpleName = Signature.getSimpleName(qualifiedName);
		// trying with variations of the simpleName vs qualifiedName, resolved or not.
		for (String singleStringParameter : new String[] { Signature.createTypeSignature("String", false), Signature.createTypeSignature("String", true),
				Signature.createTypeSignature("java.lang.String", false), Signature.createTypeSignature("java.lang.String", true) }) {
			if (type.getMethod(simpleName, new String[] { singleStringParameter }).exists()) {
				return true;
			}
			// trying with variations of the simpleName vs qualifiedName, resolved or not.
			for (String expectedReturnType : new String[] { Signature.createTypeSignature(simpleName, false),
					Signature.createTypeSignature(qualifiedName, false),
					Signature.createTypeSignature(qualifiedName, true) }) {
				final IMethod fromStringMethod = type.getMethod("fromString", new String[] { singleStringParameter });
				if (fromStringMethod.exists() && isStatic(fromStringMethod)
						&& fromStringMethod.getReturnType().equals(expectedReturnType)) {
					return true;
				}
				final IMethod valueOfMethod = type.getMethod("valueOf", new String[] { singleStringParameter });
				if (valueOfMethod.exists() && isStatic(valueOfMethod)
						&& valueOfMethod.getReturnType().equals(expectedReturnType)) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Checks that the given method has a {@code static} modifier flag.
	 * 
	 * @param method
	 *            the method to validate
	 * @return {@code true} if it matches, {@code false} otherwise.
	 * @throws JavaModelException
	 */
	private static boolean isStatic(final IMethod method) throws JavaModelException {
		return ((method.getFlags() & Flags.AccStatic) > 0);
	}

}
