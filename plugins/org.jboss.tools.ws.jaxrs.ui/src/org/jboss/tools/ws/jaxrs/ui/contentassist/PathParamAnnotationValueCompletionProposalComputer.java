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

import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PATH_PARAM;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.ui.text.IJavaPartitions;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsMetamodelLocator;
import org.jboss.tools.ws.jaxrs.core.utils.Annotation;
import org.jboss.tools.ws.jaxrs.core.utils.JdtUtils;
import org.jboss.tools.ws.jaxrs.ui.JBossJaxrsUIPlugin;
import org.jboss.tools.ws.jaxrs.ui.internal.text.ContentAssistCompletionProposal;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;

/**
 * Computes proposals for <code>java.ws.rs.PathParam</code> annotation values in the compilation unit context.
 * 
 * @author xcoulon
 */
public class PathParamAnnotationValueCompletionProposalComputer implements IJavaCompletionProposalComputer {

	/** Icon for completion proposals. */
	private final Image icon = JBossJaxrsUIPlugin.getDefault().getImage("url_mapping.gif");

	/** {@inheritDoc} */
	@Override
	public void sessionStarted() {
	}

	/** {@inheritDoc} */
	@Override
	public void sessionEnded() {
	}

	/** {@inheritDoc} */
	@Override
	public final List<ICompletionProposal> computeCompletionProposals(final ContentAssistInvocationContext context,
			final IProgressMonitor monitor) {
		JavaContentAssistInvocationContext javaContext = (JavaContentAssistInvocationContext) context;
		try {
			final IJavaProject project = javaContext.getProject();
			final IJaxrsMetamodel metamodel = JaxrsMetamodelLocator.get(project);
			// skip if the JAX-RS Nature is not configured for this project
			if (metamodel == null) {
				return Collections.emptyList();
			}
			synchronized(metamodel) {
			final int invocationOffset = context.getInvocationOffset();
			final ICompilationUnit compilationUnit = javaContext.getCompilationUnit();
			final Annotation annotation = JdtUtils.resolveAnnotationAt(invocationOffset, compilationUnit);
			if (annotation != null && annotation.getFullyQualifiedName().equals(PATH_PARAM)) {
				final IJavaElement javaMethod = annotation.getJavaAnnotation().getAncestor(IJavaElement.METHOD);
				final IJaxrsResourceMethod resourceMethod = (IJaxrsResourceMethod) metamodel.findElement(javaMethod);
				if (resourceMethod != null) {
					return internalComputePathParamProposals(javaContext, resourceMethod);
				}
			}
			}

		} catch (Exception e) {
			Logger.error("Failed to compute completion proposal", e);
		}
		return Collections.emptyList();
	}

	/**
	 * Computes the valid proposals for the <code>javax.ws.rs.PathParam</code> annotation value. The proposals are based
	 * on:
	 * <ul>
	 * <li>The values of the <code>javax.ws.rs.Path</code> annotations, both at the method and at the type level
	 * (inclusion),</li>
	 * <li>The values of the sibling <code>javax.ws.rs.PathParam</code> annotations (exclusion).
	 * </ul>
	 * 
	 * @param javaContext
	 *            the invocation context
	 * @param method
	 *            the enclosing java method
	 * @param compilationUnit
	 *            the compilation unit (AST3)
	 * @return the list of computed completion proposals
	 * @throws CoreException
	 *             in case of underlying exception
	 * @throws BadLocationException
	 * @throws org.eclipse.jface.text.BadLocationException
	 */
	private List<ICompletionProposal> internalComputePathParamProposals(
			final JavaContentAssistInvocationContext javaContext, final IJaxrsResourceMethod resourceMethod)
			throws CoreException, BadLocationException {
		final List<ICompletionProposal> completionProposals = new ArrayList<ICompletionProposal>();
		final ITypedRegion region = getRegion(javaContext);
		String matchValue = javaContext.getDocument().get(region.getOffset(),
				javaContext.getInvocationOffset() - region.getOffset());
		if (!matchValue.isEmpty() && matchValue.charAt(0) == '\"') {
			matchValue = matchValue.substring(1);
		}
		List<String> proposals = new ArrayList<String>(resourceMethod.getPathParamValueProposals().keySet());
		Collections.sort(proposals);
		for (String proposal : proposals) {
			if (proposal.startsWith(matchValue)) {
				completionProposals.add(generateCompletionProposal(resourceMethod.getJavaElement(), region, proposal));

			}
		}
		return completionProposals;
	}

	private ICompletionProposal generateCompletionProposal(IMember member, ITypedRegion region, String proposalValue)
			throws CoreException {
		String replacementValue = "\"" + proposalValue + "\"";
		String displayString = proposalValue + " - JAX-RS Mapping";
		StyledString displayStyledString = new StyledString(displayString);
		displayStyledString.setStyle(proposalValue.length(), displayString.length() - proposalValue.length(),
				StyledString.QUALIFIER_STYLER);
		return new ContentAssistCompletionProposal(replacementValue, displayStyledString, region, icon, member);
	}

	/**
	 * Resolves the typed region for the given java content assist invocation context.
	 * 
	 * @param javaContext
	 *            the java content assist invocation context
	 * @return the typed region
	 */
	private ITypedRegion getRegion(final JavaContentAssistInvocationContext javaContext) {
		IDocument document = javaContext.getDocument();
		IDocumentPartitioner documentPartitioner = ((IDocumentExtension3) document)
				.getDocumentPartitioner(IJavaPartitions.JAVA_PARTITIONING);
		return documentPartitioner.getPartition(javaContext.getInvocationOffset());
	}

	/** {@inheritDoc} */
	@Override
	public final List<IContextInformation> computeContextInformation(final ContentAssistInvocationContext context,
			final IProgressMonitor monitor) {
		return Collections.emptyList();
	}

	/** {@inheritDoc} */
	@Override
	public final String getErrorMessage() {
		return null;
	}

}
