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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.java.hover.JavadocBrowserInformationControlInput;
import org.eclipse.jdt.internal.ui.text.java.hover.JavadocHover;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.internal.text.html.BrowserInformationControl;
import org.eclipse.jface.internal.text.html.HTMLPrinter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension3;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension4;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension5;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension6;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchSite;
import org.jboss.tools.ws.jaxrs.core.utils.JdtUtils;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;
import org.osgi.framework.Bundle;

@SuppressWarnings("restriction")
public class AnnotationCompletionProposal implements ICompletionProposal, ICompletionProposalExtension,
		ICompletionProposalExtension3, ICompletionProposalExtension4, ICompletionProposalExtension5,
		ICompletionProposalExtension6, IJavaCompletionProposal {

	/** The maximum relevance value, to ensure the proposal is at the top of the
	 * list. */
	public static final int MAX_RELEVANCE = 1000;

	private String additionalProposalInfo;

	private final IMember member;

	private final String replacementString;

	private final int replacementOffset;

	private final int replacementLength;

	private final Image icon;

	private final StyledString displayStyledString;

	private final int relevance;

	private final int cursorPosition;

	private String fgCSSStyles;

	private IInformationControlCreator creator;

	public AnnotationCompletionProposal(final String replacementString, final StyledString displayStyledString,
			final ITypedRegion region, final Image icon, final IMember member) {
		this.replacementString = replacementString;
		this.replacementOffset = region.getOffset();
		this.replacementLength = region.getLength();
		this.icon = icon;
		this.displayStyledString = displayStyledString;
		this.cursorPosition = replacementString.length();
		this.relevance = MAX_RELEVANCE;
		this.member = member;
	}

	@Override
	public Point getSelection(IDocument document) {
		return new Point(replacementOffset + cursorPosition, 0);
	}

	@Override
	public void apply(IDocument document) {
		apply(document, (char) 0, 0);
	}

	@Override
	public void apply(IDocument document, char trigger, int offset) {
		try {
			document.replace(replacementOffset, replacementLength, replacementString);
			// cursorPosition++;
		} catch (BadLocationException e) {
			Logger.warn("Failed to replace document content with selected proposal", e);
		}
	}

	@Override
	public String getAdditionalProposalInfo() {
		if (additionalProposalInfo == null) {
			additionalProposalInfo = ((JavadocBrowserInformationControlInput) getAdditionalProposalInfo(new NullProgressMonitor()))
					.getHtml();
		}
		return additionalProposalInfo.toString();
	}

	@Override
	public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
		CompilationUnit compilationUnit;
		try {
			compilationUnit = JdtUtils.parse(member, monitor);
			MemberDeclarationVisitor memberDeclarationVisitor = new MemberDeclarationVisitor(member);
			compilationUnit.accept(memberDeclarationVisitor);
			String sourceOverview = memberDeclarationVisitor.getSourceOverview();
			StringBuffer buffer = new StringBuffer();
			HTMLPrinter.insertPageProlog(buffer, 0, getCSSStyles());
			buffer.append("Matching URI Template mapping defined in the <strong>@Path</strong> annotation value below:</br></br>");
			buffer.append(sourceOverview.replaceFirst("@Path.*\n", "<strong>$0</strong>").replaceAll("\n", "<br/>")
					.replaceAll("\t", "  ").replaceAll("<br/>\\s*", "<br/>"));
			HTMLPrinter.addPageEpilog(buffer);
			this.additionalProposalInfo = buffer.toString();
			return new JavadocBrowserInformationControlInput(null, null, this.additionalProposalInfo, 0);
		} catch (JavaModelException e) {
			// do nothing
		}
		return null;
	}

	/** Returns the style information for displaying HTML (Javadoc) content.
	 * 
	 * @return the CSS styles
	 * @since 3.3 */
	protected String getCSSStyles() {
		if (fgCSSStyles == null) {
			Bundle bundle = Platform.getBundle(JavaPlugin.getPluginId());
			URL url = bundle.getEntry("/JavadocHoverStyleSheet.css"); //$NON-NLS-1$
			if (url != null) {
				BufferedReader reader = null;
				try {
					url = FileLocator.toFileURL(url);
					reader = new BufferedReader(new InputStreamReader(url.openStream()));
					StringBuffer buffer = new StringBuffer(200);
					String line = reader.readLine();
					while (line != null) {
						buffer.append(line);
						buffer.append('\n');
						line = reader.readLine();
					}
					fgCSSStyles = buffer.toString();
				} catch (IOException ex) {
					JavaPlugin.log(ex);
				} finally {
					try {
						if (reader != null)
							reader.close();
					} catch (IOException e) {
					}
				}

			}
		}
		String css = fgCSSStyles;
		if (css != null) {
			FontData fontData = JFaceResources.getFontRegistry().getFontData(
					PreferenceConstants.APPEARANCE_JAVADOC_FONT)[0];
			css = HTMLPrinter.convertTopLevelFont(css, fontData);
		}
		return css;
	}

	@Override
	public boolean isAutoInsertable() {
		return false;
	}

	@Override
	public String getDisplayString() {
		return displayStyledString.getString();
	}

	@Override
	public StyledString getStyledDisplayString() {
		return displayStyledString;
	}

	@Override
	public Image getImage() {
		return icon;
	}

	@Override
	public IContextInformation getContextInformation() {
		return null;
	}

	@Override
	public int getRelevance() {
		return relevance;
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
			return replacementOffset - 1;
		}
		return replacementOffset + cursorPosition;
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
