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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;

/**
 * Visitor to retrieve {@link JavaMethodSignature}s one given or all {@link IMethod}(s) in a {@link ICompilationUnit}.
 * @author xcoulon
 *
 */
public class JavaMethodSignaturesVisitor extends ASTVisitor {

	private final IMethod method;

	private final Map<String, JavaMethodSignature> methodSignatures = new HashMap<String, JavaMethodSignature>();

	/**
	 * Constructor to use when you need all Java Method signatures in the given
	 * compilation unit
	 * 
	 * @param method
	 */
	public JavaMethodSignaturesVisitor() {
		this.method = null;
	}

	/**
	 * Constructor to use when you only need a single Java Method signature
	 * 
	 * @param method
	 */
	public JavaMethodSignaturesVisitor(IMethod method) {
		this.method = method;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse
	 * .jdt.core.dom.MethodDeclaration)
	 */
	@Override
	public boolean visit(MethodDeclaration declaration) {
		try {
			final IMethodBinding methodBinding = declaration.resolveBinding();
			// method bindings may not be resolved (eg : duplicate method - see JBIDE-13580)
			if(methodBinding == null) { 
				return false; // no need to check the children here
			}
			final IMethod method = (IMethod) methodBinding.getJavaElement();
			if (this.method != null && !this.method.getHandleIdentifier().equals(method.getHandleIdentifier())) {
				return false; // no need to check the children
			}
			final IType returnedType = getReturnType(methodBinding);
			// .getReturnType().getJavaElement() : null;
			List<JavaMethodParameter> methodParameters = new ArrayList<JavaMethodParameter>();
			@SuppressWarnings("unchecked")
			List<SingleVariableDeclaration> parameters = declaration.parameters();
			for (int i = 0; i < parameters.size(); i++) {
				final SingleVariableDeclaration parameter = parameters.get(i);
				final String paramName = parameter.getName().getFullyQualifiedName();
				final IVariableBinding paramBinding = parameter.resolveBinding();
				final String paramTypeName = paramBinding.getType().getQualifiedName();
				final List<Annotation> paramAnnotations = new ArrayList<Annotation>();
				final IAnnotationBinding[] annotationBindings = paramBinding.getAnnotations();
				for (int j = 0; j < annotationBindings.length; j++) {
					final ILocalVariable localVariable = method.getParameters()[i];
					final IAnnotation javaAnnotation = localVariable.getAnnotations()[j];
					final IAnnotationBinding javaAnnotationBinding = annotationBindings[j];
					paramAnnotations.add(BindingUtils.toAnnotation(javaAnnotationBinding, javaAnnotation));
				}
				// final ISourceRange sourceRange = new
				// SourceRange(parameter.getStartPosition(),
				// parameter.getLength());
				methodParameters.add(new JavaMethodParameter(paramName, paramTypeName, paramAnnotations));
			}

			// TODO : add support for thrown exceptions
			this.methodSignatures.put(method.getHandleIdentifier(), new JavaMethodSignature(method, returnedType, methodParameters));
		} catch (JavaModelException e) {
			Logger.error("Failed to analyse compilation unit methods", e);
		}
		return false; // no need to check the children
	}

	/**
	 * Returns the ReturnType for the given method or null of the return type
	 * could not be found or is 'void'
	 * 
	 * @param methodBinding
	 * @return
	 */
	private IType getReturnType(final IMethodBinding methodBinding) {
		try {
			if (methodBinding.getReturnType() != null && methodBinding.getReturnType().getJavaElement() != null) {
				return (IType) methodBinding.getReturnType().getJavaElement().getAdapter(IType.class);
			}
		} 
		// https://issues.jboss.org/browse/JBIDE-15084: when compilation error (not syntax), retrieving return type may result in
		// an IllegalArgumentException
		catch(IllegalArgumentException e) {
			Logger.debug("Caught an IllegalArgumentException while trying to retrieve return type on method {}", methodBinding);
		}
		return null;
	}

	/** @return the methodDeclarations */
	public JavaMethodSignature getMethodSignature() {
		if (this.methodSignatures.isEmpty() || this.method == null) {
			Logger.debug("*** no method signature found ?!? ***");
			return null;
		}
		return this.methodSignatures.get(this.method.getHandleIdentifier());

	}

	/** @return the methodDeclarations indexed by the {@link IMethod} identifier */
	public Map<String, JavaMethodSignature> getMethodSignatures() {
		return this.methodSignatures;
	}
}