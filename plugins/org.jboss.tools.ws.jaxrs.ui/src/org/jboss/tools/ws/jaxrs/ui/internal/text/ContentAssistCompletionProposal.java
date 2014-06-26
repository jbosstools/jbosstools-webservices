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

package org.jboss.tools.ws.jaxrs.ui.internal.text;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.java.hover.JavadocHover;
import org.eclipse.jface.internal.text.html.BrowserInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension3;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension4;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension5;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension6;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchSite;

@SuppressWarnings("restriction")
public class ContentAssistCompletionProposal extends BasicCompletionProposal implements ICompletionProposal, ICompletionProposalExtension,
		ICompletionProposalExtension3, ICompletionProposalExtension4, ICompletionProposalExtension5,
		ICompletionProposalExtension6 {

	private IInformationControlCreator creator;

	private final StyledString displayStyledString;
	/**
	 * Full Constructor
	 * @param replacementString
	 * @param proposalDisplayString
	 * @param region
	 * @param icon
	 * @param member
	 */
	public ContentAssistCompletionProposal(final ICompilationUnit compilationUnit, final String replacementString, final StyledString displayStyledString,
			final ITypedRegion region, final Image icon, final IMember member) {
		super(compilationUnit, replacementString, displayStyledString.getString(), region.getOffset(), region.getLength(), icon, member);
		this.displayStyledString = displayStyledString;
	}

	@Override
	public boolean isAutoInsertable() {
		return false;
	}

	@Override
	public StyledString getStyledDisplayString() {
		return displayStyledString;
	}

	@Override
	public boolean isValidFor(IDocument document, int offset) {
		return false;
	}

	@Override
	public char[] getTriggerCharacters() {
		return null;
	}

	/*
	 * @see ICompletionProposalExtension#getContextInformationPosition()
	 */
	@Override
	public int getContextInformationPosition() {
		if (getContextInformation() == null) {
			return getReplacementOffset() - 1;
		}
		return getReplacementOffset() + getReplacementString().length();
	}

	@Override
	public IInformationControlCreator getInformationControlCreator() {
		Shell shell = JavaPlugin.getActiveWorkbenchShell();
		if (shell == null || !BrowserInformationControl.isAvailable(shell)) {
			return null;
		}
		if (creator == null) {
			JavadocHover.PresenterControlCreator presenterControlCreator = new JavadocHover.PresenterControlCreator(
					getSite());
			creator = new JavadocHover.HoverControlCreator(presenterControlCreator, true);
		}
		return creator;
	}

	private IWorkbenchSite getSite() {
		IWorkbenchPage page = JavaPlugin.getActivePage();
		if (page != null) {
			IWorkbenchPart part = page.getActivePart();
			if (part != null)
				return part.getSite();
		}
		return null;
	}

	@Override
	public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
		return null;
	}

	@Override
	public int getPrefixCompletionStart(IDocument document, int completionOffset) {
		return 0;
	}

}
