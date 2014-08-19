/******************************************************************************* 
 * Copyright (c) 2011 - 2014 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.ws.jaxrs.ui.quickfix;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.SourceRange;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import org.jboss.tools.common.validation.ValidationErrorManager;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsMetamodelLocator;
import org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.ui.internal.validation.JaxrsMarkerResolutionIds;

public class JaxrsMarkerResolutionGenerator implements IMarkerResolutionGenerator2, IQuickFixProcessor {

	@Override
	public IMarkerResolution[] getResolutions(final IMarker marker) {
		return getMarkerResolutions(marker);
	}

	@Override
	public boolean hasResolutions(final IMarker marker) {
		return getMarkerResolutions(marker).length > 0;
	}

	/**
	 * Null-safe extraction of the potential marker resolutions bound to this
	 * marker.
	 * 
	 * @param marker
	 *            the marker
	 * @return a array of marker resolutions. If no resolution is bound to the
	 *         marker, the returned array is empty (not null).
	 */
	private IMarkerResolution[] getMarkerResolutions(final IMarker marker) {
		try {
			final int quickfixId = getQuickFixID(marker);
			final ICompilationUnit compilationUnit = JdtUtils.getCompilationUnit(marker.getResource());
			final IType type = (IType) JdtUtils.getElementAt(compilationUnit,
					marker.getAttribute(IMarker.CHAR_START, 0), IJavaElement.TYPE);
			if (type != null) {
				switch (quickfixId) {
				case JaxrsMarkerResolutionIds.HTTP_METHOD_MISSING_TARGET_ANNOTATION_QUICKFIX_ID:
					return new IMarkerResolution[] { new AddHttpMethodTargetAnnotationMarkerResolution(type) };
				case JaxrsMarkerResolutionIds.NAME_BINDING_MISSING_TARGET_ANNOTATION_QUICKFIX_ID:
					return new IMarkerResolution[] { new AddNameBindingTargetAnnotationMarkerResolution(type) };
				case JaxrsMarkerResolutionIds.HTTP_METHOD_INVALID_TARGET_ANNOTATION_VALUE_QUICKFIX_ID:
					return new IMarkerResolution[] { new UpdateHttpMethodTargetAnnotationValueMarkerResolution(type) };
				case JaxrsMarkerResolutionIds.NAME_BINDING_INVALID_TARGET_ANNOTATION_VALUE_QUICKFIX_ID:
					return new IMarkerResolution[] { new UpdateHttpMethodTargetAnnotationValueMarkerResolution(type) };
				case JaxrsMarkerResolutionIds.HTTP_METHOD_MISSING_RETENTION_ANNOTATION_QUICKFIX_ID:
					return new IMarkerResolution[] { new AddHttpMethodRetentionAnnotationMarkerResolution(type) };
				case JaxrsMarkerResolutionIds.NAME_BINDING_MISSING_RETENTION_ANNOTATION_QUICKFIX_ID:
					return new IMarkerResolution[] { new AddNameBindingRetentionAnnotationMarkerResolution(type) };
				case JaxrsMarkerResolutionIds.HTTP_METHOD_INVALID_RETENTION_ANNOTATION_VALUE_QUICKFIX_ID:
					return new IMarkerResolution[] { new UpdateHttpMethodRetentionAnnotationValueMarkerResolution(type) };
				case JaxrsMarkerResolutionIds.NAME_BINDING_INVALID_RETENTION_ANNOTATION_VALUE_QUICKFIX_ID:
					return new IMarkerResolution[] { new UpdateNameBindingRetentionAnnotationValueMarkerResolution(type) };
				}
			}
		} catch (CoreException e) {
			Logger.error("Failed to retrieve marker resolution", e);
		}
		return new IMarkerResolution[0];
	}

	/**
	 * return message id or -1 if impossible to find
	 * 
	 * @param marker
	 * @return
	 */
	private int getQuickFixID(final IMarker marker) throws CoreException {
		return ((Integer) marker.getAttribute(ValidationErrorManager.MESSAGE_ID_ATTRIBUTE_NAME, -1));
	}

	@Override
	public boolean hasCorrections(final ICompilationUnit unit, final int problemId) {
		return false;
	}

	@Override
	public IJavaCompletionProposal[] getCorrections(final IInvocationContext context, final IProblemLocation[] locations)
			throws CoreException {
		final CompilationUnit astRoot = context.getASTRoot();
		final List<IJavaCompletionProposal> completionProposals = new ArrayList<IJavaCompletionProposal>();
		for (IProblemLocation problemLocation : locations) {
			if (problemLocation.getMarkerType().equals(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER)
					&& problemLocation.getProblemId() == IProblem.MissingValueForAnnotationMember) {
				final ASTNode coveredNode = problemLocation.getCoveredNode(astRoot);
				final String qualifiedName = getQualifiedName(coveredNode);
				final IAnnotation javaElement = getJavaAnnotation(coveredNode);
				final IJavaCompletionProposal completionProposal = generateMissingAttributesCompletionProposal(
						qualifiedName, problemLocation, javaElement);
				if (completionProposal != null) {
					completionProposals.add(completionProposal);
				}
			}
		}
		return completionProposals.toArray(new IJavaCompletionProposal[completionProposals.size()]);
	}

	private String getQualifiedName(final ASTNode node) {
		if (node instanceof Expression) {
			final ITypeBinding binding = ((Expression) node).resolveTypeBinding();
			if (binding != null) {
				return binding.getQualifiedName();
			}
		}
		return null;
	}

	private IAnnotation getJavaAnnotation(final ASTNode node) {
		if (node instanceof Annotation) {
			final IAnnotationBinding binding = ((Annotation) node).resolveAnnotationBinding();
			if (binding != null) {
				return (IAnnotation) binding.getJavaElement();
			}
		} else if (node instanceof SimpleName) {
			return getJavaAnnotation(node.getParent());
		}
		return null;
	}

	private IJavaCompletionProposal generateMissingAttributesCompletionProposal(final String qualifiedName,
			final IProblemLocation problemLocation, final IAnnotation annotation) throws JavaModelException {
		if (qualifiedName == null || annotation == null) {
			return null;
		}
		final IJaxrsElement jaxrsElement = findJaxrsElement(annotation.getParent());
		// skip if the problem is not linked to a JAX-RS element.
		if (jaxrsElement == null) {
			return null;
		}
		final ICompilationUnit compilationUnit = (ICompilationUnit) annotation
				.getAncestor(IJavaElement.COMPILATION_UNIT);
		if (qualifiedName.equals(JaxrsClassnames.TARGET)) {
			switch (jaxrsElement.getElementKind()) {
			case HTTP_METHOD:
				return new AddHttpMethodTargetValuesCompletionProposal(compilationUnit,
						findEffectiveSourceRange(compilationUnit, problemLocation));
			case NAME_BINDING:
				return new AddHttpMethodTargetValuesCompletionProposal(compilationUnit,
						findEffectiveSourceRange(compilationUnit, problemLocation));
			default:
				return null;
			}
		} else if (qualifiedName.equals(JaxrsClassnames.RETENTION)) {
			switch (jaxrsElement.getElementKind()) {
			case HTTP_METHOD:
				return new AddHttpMethodRetentionValueCompletionProposal(compilationUnit,
						findEffectiveSourceRange(compilationUnit, problemLocation));
			case NAME_BINDING:
				return new AddNameBindingRetentionValueCompletionProposal(compilationUnit,
						findEffectiveSourceRange(compilationUnit, problemLocation));
			default:
				return null;
			}
		} else if (qualifiedName.equals(JaxrsClassnames.HTTP_METHOD)) {
			final IJavaElement httpMethodType = annotation.getAncestor(IJavaElement.TYPE);
			return new AddHttpMethodValueCompletionProposal(compilationUnit, "\"" + httpMethodType.getElementName()
					+ "\"", findEffectiveSourceRange(compilationUnit, problemLocation));
		}
		return null;
	}

	/**
	 * 
	 * @param compilationUnit
	 * @param problemLocation
	 * @return the effective length to use when preparing the completion
	 *         proposal, which will include optional parenthesis immediately
	 *         following the given problem location (including spaces)
	 * @throws JavaModelException
	 */
	public static SourceRange findEffectiveSourceRange(final ICompilationUnit compilationUnit,
			final IProblemLocation problemLocation) {
		try {
			final String source = compilationUnit.getSource();
			int position = problemLocation.getOffset() + problemLocation.getLength() ;
			char c = Character.MIN_VALUE;
			while ((c = source.charAt(position)) == ' ') {
				position++;
			}
			// ok, let's look to the closing parenthesis, then
			if(c == '(') {
				position++;
				while ((c = source.charAt(position)) == ' ') {
					position++;
				}
				if(c == ')') {
					return new SourceRange(problemLocation.getOffset(), position - problemLocation.getOffset() + 1);
				}
			}
			return new SourceRange(problemLocation.getOffset(), problemLocation.getLength());
			
		} catch (JavaModelException e) {
			// let's ignore and return the given length
			return new SourceRange(problemLocation.getOffset(), problemLocation.getLength());
		}

	}

	/**
	 * @param javaElement
	 *            the java element
	 * @return the {@link IJaxrsElement} associated with the given
	 *         {@link IJavaElement}, or {@code null} if none was found.
	 */
	private static IJaxrsElement findJaxrsElement(final IJavaElement javaElement) {
		if (javaElement != null) {
			try {
				return JaxrsMetamodelLocator.get(javaElement.getJavaProject()).findElement(javaElement);
			} catch (CoreException e) {
				Logger.error("Failed to retrieve JAX-RS Elements associated with '" + javaElement.getElementName()
						+ "'", e);
			}
		}
		return null;
	}

}
