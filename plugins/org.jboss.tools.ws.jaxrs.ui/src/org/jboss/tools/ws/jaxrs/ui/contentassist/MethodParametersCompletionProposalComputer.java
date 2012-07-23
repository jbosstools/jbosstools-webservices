package org.jboss.tools.ws.jaxrs.ui.contentassist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.ui.text.IJavaPartitions;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TypedRegion;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.metamodel.JaxrsMetamodelLocator;
import org.jboss.tools.ws.jaxrs.ui.JBossJaxrsUIPlugin;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;

/**
 * Computes proposals for method parameters based on the Path values found on the method and on the parent type. Also,
 * depending on the HttpMethod, a single content object may be proposed, too. For example, with @POST and @PUT request,
 * there should be a body to process, but not in @GET and @DELETE, nor in @HEAD and @OPTIONS.
 * If the java method is annotated with a custom HttpMethod, then the content object will be proposed, too.
 * 
 * 
 * @author xcoulon
 */
public class MethodParametersCompletionProposalComputer implements IJavaCompletionProposalComputer {

	/** Icon for completion proposals. */
	private final Image icon = JBossJaxrsUIPlugin.getDefault().createImage("url_mapping.gif");

	/** {@inheritDoc} */
	@Override
	public void sessionStarted() {
	}

	/** {@inheritDoc} */
	@Override
	public void sessionEnded() {
	}

	@Override
	public List<ICompletionProposal> computeCompletionProposals(ContentAssistInvocationContext context,
			IProgressMonitor monitor) {
		final List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();
		final JavaContentAssistInvocationContext javaContext = (JavaContentAssistInvocationContext) context;
		try {
			final IJavaProject project = javaContext.getProject();
			final IJaxrsMetamodel metamodel = JaxrsMetamodelLocator.get(project);
			// skip if the JAX-RS Nature is not configured for this project
			if (metamodel == null) {
				return Collections.emptyList();
			}
			final IJavaElement invocationElement = javaContext.getCompilationUnit().getElementAt(
					context.getInvocationOffset());
			if (invocationElement.getElementType() == IJavaElement.TYPE) {
					final ITypedRegion region = new TypedRegion(javaContext.getInvocationOffset(), 0, null);
					proposals.add(new MethodParametersCompletionProposal("Foo !", new StyledString("Foo!"), region, icon, (IMember) invocationElement));
			}
		} catch (Exception e) {
			Logger.error("Failed to compute completion proposal", e);
		}
		return proposals; 
	}
	
	/**
	 * Resolves the typed region for the given java content assist invocation context.
	 * 
	 * @param javaContext
	 *            the java content assist invocation context
	 * @return the typed region
	 */
	@SuppressWarnings("unused")
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
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public final String getErrorMessage() {
		return null;
	}

}
