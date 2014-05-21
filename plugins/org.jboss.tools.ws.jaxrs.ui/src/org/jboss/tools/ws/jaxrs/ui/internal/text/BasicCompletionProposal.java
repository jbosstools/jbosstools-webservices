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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.java.hover.JavadocBrowserInformationControlInput;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.internal.text.html.HTMLPrinter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.jboss.tools.ws.jaxrs.core.utils.JdtUtils;
import org.jboss.tools.ws.jaxrs.ui.contentassist.MemberDeclarationVisitor;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;
import org.osgi.framework.Bundle;

@SuppressWarnings("restriction")
public class BasicCompletionProposal implements IJavaCompletionProposal {

	/**
	 * The maximum relevance value, to ensure the proposal is at the top of the
	 * list.
	 */
	public static final int MAX_RELEVANCE = 1000;

	protected String additionalProposalInfo;

	protected final IMember member;

	protected final String replacementString;

	protected final int replacementOffset;

	protected final int replacementLength;

	protected final Image icon;

	protected final String proposalDisplayString;

	protected final int relevance;

	protected String fgCSSStyles;

	private String additionImport;

	private ICompilationUnit compilationUnit;

	/**
	 * Full Constructor
	 * 
	 * @param replacementString
	 * @param proposalDisplayString
	 * @param region
	 * @param icon
	 * @param member
	 */
	public BasicCompletionProposal(final String replacementString, final String proposalDisplayString,
			final int replacementOffset, final int replacementLength, final Image icon, final IMember member) {
		this.replacementString = replacementString;
		this.replacementOffset = replacementOffset;
		this.replacementLength = replacementLength;
		this.icon = icon;
		this.proposalDisplayString = proposalDisplayString;
		this.relevance = MAX_RELEVANCE;
		this.member = member;
	}

	protected void includeImportDeclarationAddition(final ICompilationUnit compilationUnit, final String fullyQualifiedName) {
		this.compilationUnit = compilationUnit;
		this.additionImport = fullyQualifiedName;
	}

	@Override
	public Point getSelection(IDocument document) {
		return new Point(replacementOffset + replacementString.length(), 0);
	}

	@Override
	public void apply(IDocument document) {
		apply(document, (char) 0, 0);
	}

	public void apply(IDocument document, char trigger, int offset) {
		try {
			document.replace(replacementOffset, replacementLength, replacementString);
			if(this.additionImport != null) {
				compilationUnit.createImport(additionImport, null, null);
			}
		} catch (BadLocationException e) {
			Logger.warn("Failed to replace document content with selected proposal", e);
		} catch (JavaModelException e) {
			Logger.warn("Failed to replace document content with selected proposal", e);
		}
	}

	@Override
	public String getAdditionalProposalInfo() {
		if (additionalProposalInfo == null) {
			additionalProposalInfo = ((JavadocBrowserInformationControlInput) getAdditionalProposalInfo(new NullProgressMonitor()))
					.getHtml();
		}
		if (additionalProposalInfo != null) {
			return additionalProposalInfo.toString();
		}
		return null;
	}

	public Object getAdditionalProposalInfo(final IProgressMonitor monitor) {
		if (member != null) {
			try {
				final CompilationUnit compilationUnit = JdtUtils.parse(member, monitor);
				final MemberDeclarationVisitor memberDeclarationVisitor = new MemberDeclarationVisitor(member);
				compilationUnit.accept(memberDeclarationVisitor);
				final String sourceOverview = memberDeclarationVisitor.getSourceOverview();
				final StringBuffer buffer = new StringBuffer();
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
		}
		return null;
	}

	/**
	 * Returns the style information for displaying HTML (Javadoc) content.
	 * 
	 * @return the CSS styles
	 * @since 3.3
	 */
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
	public String getDisplayString() {
		if (proposalDisplayString != null) {
			return proposalDisplayString;
		}
		return null;
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

}
