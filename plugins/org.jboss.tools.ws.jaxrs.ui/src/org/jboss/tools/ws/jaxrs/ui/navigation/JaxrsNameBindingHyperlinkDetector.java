/******************************************************************************* 
 * Copyright (c) 2014 - 2014 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.ws.jaxrs.ui.navigation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICodeAssist;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.text.JavaWordFinder;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.texteditor.ITextEditor;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsNameBinding;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsJavaElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsMetamodelLocator;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;

/**
 * @author xcoulon
 *
 */
@SuppressWarnings("restriction")
public class JaxrsNameBindingHyperlinkDetector extends AbstractHyperlinkDetector {
	/**
	 * Default Constructor
	 */
	public JaxrsNameBindingHyperlinkDetector() {
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.hyperlink.IHyperlinkDetector#detectHyperlinks(org.eclipse.jface.text.ITextViewer,
	 *      org.eclipse.jface.text.IRegion, boolean)
	 */
	@Override
	public IHyperlink[] detectHyperlinks(final ITextViewer textViewer, final IRegion region,
			final boolean canShowMultipleHyperlinks) {
		final ITextEditor textEditor = (ITextEditor) getAdapter(ITextEditor.class);
		if (region == null || !(textEditor instanceof JavaEditor)) {
			return null;
		}
		final int offset = region.getOffset();
		final IJavaElement input = EditorUtility.getActiveEditorJavaInput();
		if (input == null) {
			return null;
		}
		final JaxrsMetamodel metamodel = getMetamodel(input);
		if(metamodel == null) {
			return null;
		}
		final IRegion wordRegion = getCurrentWordRegion(textEditor, offset);
		if (wordRegion == null || wordRegion.getLength() == 0) {
			return null;
		}
		final IJavaElement[] selectedJavaElements = getSelectedElements(input, wordRegion);

		final List<IJaxrsJavaElement> targets = findTargets(metamodel, selectedJavaElements, (ICompilationUnit) input.getAncestor(IJavaElement.COMPILATION_UNIT));
		if(targets != null && !targets.isEmpty()){
			final IHyperlink[] result = new IHyperlink[targets.size()];
			for(int i = 0; i < targets.size(); i++) {
				result[i] = new JaxrsNameBindingAnnotationHyperlink(targets.get(i), wordRegion);
			}
			return result;
		}
		return null;
			
	}

	/**
	 * 
	 * @param input
	 *            the current {@link IJavaElement}
	 * @param wordRegion
	 *            the selected {@link IRegion} in the {@link ITextEditor}
	 * @return an array of {@link IJavaElement} matching the selection or an
	 *         empty array if none was found or if an error occurred.
	 */
	private IJavaElement[] getSelectedElements(final IJavaElement input, final IRegion wordRegion) {
		try {
			return ((ICodeAssist) input).codeSelect(wordRegion.getOffset(), wordRegion.getLength());
		} catch (JavaModelException e) {
			// element does not exist or wordRegion was out of range.
			// We should silently ignore such a case, in which case, no navigation hyperlink shall be provided.
			Logger.debug("Failed to retrieve the selected Java Elements in the editor for" + input.getElementName(), e);
			return new IJavaElement[0];
		}
	}

	/**
	 * If the given selectedJavaElement match a custom Name Binding annotation
	 * defined in the given {@link JaxrsMetamodel}, then return the list of
	 * associated elements with this name binding.
	 * 
	 * @param metamodel
	 *            the JAX-RS Metamodel associated with the given element in the
	 *            current text editor
	 * @param selectedJavaElements
	 *            the selected java elements
	 * @param currentCompilationUnit
	 *            the {@link ICompilationUnit} opened in the current text editor
	 * @return the list of target {@link IJavaElement} or empty list if none
	 *         match.
	 */
	private List<IJaxrsJavaElement> findTargets(final JaxrsMetamodel metamodel, final IJavaElement[] selectedJavaElements, final ICompilationUnit currentCompilationUnit) {
		final List<IJaxrsJavaElement> targetElements = new ArrayList<IJaxrsJavaElement>();
		for(IJavaElement selectedJavaElement : selectedJavaElements) {
			final IJaxrsElement associatedJaxrsElement = metamodel.findElement(selectedJavaElement);
			if(associatedJaxrsElement != null && associatedJaxrsElement.getElementKind() == EnumElementKind.NAME_BINDING) {
				final JaxrsNameBinding nameBinding = (JaxrsNameBinding) associatedJaxrsElement;
				final Collection<IJaxrsElement> matchingElements = metamodel.findElementsByAnnotation(nameBinding.getJavaClassName());
				for(IJaxrsElement matchingElement : matchingElements) {
					final IJavaElement matchingJavaElement = ((IJaxrsJavaElement)matchingElement).getJavaElement();
					// skip if the matching/target Java element is part of the current compilation unit
					if(!matchingJavaElement.getAncestor(IJavaElement.COMPILATION_UNIT).equals(currentCompilationUnit)) {
						targetElements.add((IJaxrsJavaElement) matchingElement);
					}
				}
			}
		}
		return targetElements;
	}

	/**
	 * @return the selected {@link IRegion}
	 * @param textEditor the current {@link ITextEditor}
	 * @param offset the position in the text editor
	 */
	private IRegion getCurrentWordRegion(final ITextEditor textEditor, final int offset) {
		final IDocument document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
		final IRegion wordRegion = JavaWordFinder.findWord(document, offset);
		return wordRegion;
	}

	/**
	 * @param input the current {@link IJavaElement}
	 * @return the {@link JaxrsMetamodel} associated with the given element or {@code null} if none exists (or if an exception was thrown).
	 */
	private JaxrsMetamodel getMetamodel(final IJavaElement input) {
		try {
			return JaxrsMetamodelLocator.get(input.getJavaProject());
		} catch (CoreException e) {
			Logger.error("Failed to retrieve JAX-RS Metamodel for " + input.getElementName(), e);
			return null;
		}
	}

}
