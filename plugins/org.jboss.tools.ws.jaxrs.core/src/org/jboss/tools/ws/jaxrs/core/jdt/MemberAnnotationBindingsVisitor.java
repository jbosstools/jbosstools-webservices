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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jface.text.ITypedRegion;
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

public class MemberAnnotationBindingsVisitor extends ASTVisitor {

	/** the name of the method for which its parameters are looked for. */
	private final String methodName;

	/** the name of the annotation. */
	private final String annotationName;

	/** the bindings for the matching annotation. */
	private final Map<IAnnotationBinding, ITypedRegion> annotationBindings = new HashMap<IAnnotationBinding, ITypedRegion>();

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
	public MemberAnnotationBindingsVisitor(final IMethod method, final String aName) {
		super();
		this.methodName = method.getElementName();
		this.annotationName = aName;
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
		for (Object param : parameters) {
			if (param instanceof SingleVariableDeclaration) {
				SingleVariableDeclaration variable = (SingleVariableDeclaration) param;
				List<?> modifiers = (List<?>) ((SingleVariableDeclaration) param)
						.getStructuralProperty(SingleVariableDeclaration.MODIFIERS2_PROPERTY);
				for (Object modifier : modifiers) {
					if (modifier instanceof Annotation) {
						IAnnotationBinding annotationBinding = ((Annotation)modifier).resolveAnnotationBinding();
						if (annotationName != null
								&& (annotationName.equals(annotationBinding.getAnnotationType().getQualifiedName()) || annotationName
										.equals(annotationBinding.getAnnotationType().getName()))) {
							int startPosition = variable.getStartPosition();
							int length = variable.getLength();
							annotationBindings.put(annotationBinding, new TypedRegion(startPosition, length, variable.getType().toString()));
						}
					}
				}
			}
		}
	}

	/**
	 * @return the annotation binding found during the call to visit().
	 */
	public final Map<IAnnotationBinding, ITypedRegion> getAnnotationBindings() {
		return annotationBindings;
	}

}