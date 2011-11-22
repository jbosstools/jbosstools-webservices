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
import java.util.List;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jface.text.TypedRegion;

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

/**
 * A visitor for multiple annotations on a java member (can be a method or a
 * type).
 * 
 * @author xcoulon
 * 
 */
// TODO : very similar to MemberAnnotationBindingVisitor in common bundle, but
// this one manages several annotations of the same type on a single member (ex
// : multiple <code>javax.ws.rs.PathParam</code> on a method parameters)

public class MethodParametersVisitor extends ASTVisitor {

	/** the name of the method for which its parameters are looked for. */
	private final String methodName;

	/** the bindings for the matching annotation. */
	private final List<MethodParameter> methodParameters = new ArrayList<MethodParameter>();

	/**
	 * Full Constructor to resolve annotation binding from its fully qualified
	 * name.
	 * 
	 * @param mName
	 *            the member name
	 * @param mType
	 *            the member type
	 * @param aName
	 *            the annotation name
	 */
	public MethodParametersVisitor(final IMethod method) {
		super();
		this.methodName = method.getElementName();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.
	 *      MethodDeclaration)
	 */
	@Override
	public final boolean visit(final MethodDeclaration node) {
		if (node.getName().getFullyQualifiedName().equals(methodName)) {
			visitExtendedModifiers((List<?>) node.getStructuralProperty(MethodDeclaration.PARAMETERS_PROPERTY));
		}
		return false;
	}

	/**
	 * Visits the modifiers.
	 * 
	 * @param modifiers
	 *            the modifiers
	 */
	private void visitExtendedModifiers(final List<?> parameters) {
		for (Object p : parameters) {
			if (p instanceof SingleVariableDeclaration) {
				SingleVariableDeclaration param = (SingleVariableDeclaration) p;
				final String parameterName = param.getName().getFullyQualifiedName();
				final IVariableBinding paramBinding = param.resolveBinding();
				final String paramTypeName = paramBinding.getType().getQualifiedName();
				final List<MethodParameterAnnotation> annotations = new ArrayList<MethodParameterAnnotation>();
				final List<?> modifiers = (List<?>) (param
						.getStructuralProperty(SingleVariableDeclaration.MODIFIERS2_PROPERTY));
				for (Object modifier : modifiers) {
					if (modifier instanceof Annotation) {
						final Annotation annotation = (Annotation) modifier;
						IAnnotationBinding annotationBinding = annotation.resolveAnnotationBinding();
						final String qualifiedName = annotationBinding.getAnnotationType().getQualifiedName();
						final String annotationValue = JdtUtils.resolveAnnotationAttributeValue(annotationBinding,
								"value");
						final TypedRegion typedRegion = new TypedRegion(annotation.getStartPosition(),
								annotation.getLength(), param.getType().toString());
						annotations.add(new MethodParameterAnnotation(qualifiedName, annotationValue, typedRegion));
					}
				}
				MethodParameter methodParameter = new MethodParameter(parameterName, paramTypeName, annotations);
				methodParameters.add(methodParameter);
			}
		}
	}

	public List<MethodParameter> getMethodParameters() {
		return methodParameters;
	}

}