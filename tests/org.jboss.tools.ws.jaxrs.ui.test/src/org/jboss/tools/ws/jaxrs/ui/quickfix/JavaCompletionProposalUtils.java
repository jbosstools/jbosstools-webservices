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

package org.jboss.tools.ws.jaxrs.ui.quickfix;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.text.IDocument;
import org.jboss.tools.common.quickfix.IQuickFix;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsJavaElement;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;

/**
 * Utility class to execute the completion proposals during the tests.
 * @author xcoulon
 *
 */
public class JavaCompletionProposalUtils {

	/**
	 * apply the completion proposal on the underlying {@link IResource} of the given {@link JaxrsJavaElement}.
	 */
	public static void applyCompletionProposal(final IJavaCompletionProposal completionProposal, final JaxrsJavaElement<?> element) throws CoreException {
		final ITextFileBufferManager manager= FileBuffers.getTextFileBufferManager();
		final IPath path= element.getResource().getFullPath();
		manager.connect(path, LocationKind.IFILE, new NullProgressMonitor());
		final ITextFileBuffer fBuffer = manager.getTextFileBuffer(path, LocationKind.IFILE);
		final IDocument document = fBuffer.getDocument();
		completionProposal.apply(document);
		fBuffer.commit(null, true);
		manager.disconnect(path, LocationKind.IFILE, null);
		// now, update the element with the latest changes
		element.update(element.getJavaElement(), JdtUtils.parse(element.getJavaElement().getCompilationUnit(), null));
	}

	/**
	 * apply the completion proposal on the underlying {@link IResource} of the given {@link JaxrsJavaElement}.
	 */
	public static void applyMarkerResolution(final IQuickFix quickfix, final IResource resource) throws CoreException {
		final ITextFileBufferManager manager= FileBuffers.getTextFileBufferManager();
		final IPath path= resource.getFullPath();
		manager.connect(path, LocationKind.IFILE, new NullProgressMonitor());
		final ITextFileBuffer fBuffer = manager.getTextFileBuffer(path, LocationKind.IFILE);
		final IDocument document = fBuffer.getDocument();
		quickfix.apply(document);
		//ResourcesUtils.replaceContent(resource, document.get());
		fBuffer.commit(null, true);
		manager.disconnect(path, LocationKind.IFILE, null);
	}
	

}
