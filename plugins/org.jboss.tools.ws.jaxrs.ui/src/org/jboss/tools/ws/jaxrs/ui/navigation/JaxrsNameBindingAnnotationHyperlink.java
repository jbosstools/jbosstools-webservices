/******************************************************************************* 
 * Copyright (c) 2014 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.ws.jaxrs.ui.navigation;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.PartInitException;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsJavaElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;

/**
 * @author xcoulon
 *
 */
public class JaxrsNameBindingAnnotationHyperlink implements IHyperlink {
	
	private final IJaxrsJavaElement targetElement;
	
	private final IRegion hyperlinkRegion;
	
	/**
	 * Full constructor
	 * @param targetElement the target element of this link.
	 */
	public JaxrsNameBindingAnnotationHyperlink(final IJaxrsJavaElement targetElement, final IRegion hyperlinkRegion) {
		this.targetElement = targetElement;
		this.hyperlinkRegion = hyperlinkRegion;
	}

	/**
	 * {@inheritDoc}
	 * @see org.eclipse.jface.text.hyperlink.IHyperlink#getHyperlinkRegion()
	 */
	@Override
	public IRegion getHyperlinkRegion() {
		return hyperlinkRegion;
	}

	/**
	 * {@inheritDoc}
	 * @see org.eclipse.jface.text.hyperlink.IHyperlink#getTypeLabel()
	 */
	@Override
	public String getTypeLabel() {
		return getHyperlinkText();
	}

	/**
	 * {@inheritDoc}
	 * @see org.eclipse.jface.text.hyperlink.IHyperlink#getHyperlinkText()
	 */
	@Override
	public String getHyperlinkText() {
		return "Open " + getDisplayNameText(targetElement);
	}

	public static String getDisplayNameText(final IJaxrsJavaElement element) {
		if(element instanceof IJaxrsJavaElement) {
			final IJavaElement javaElement = ((IJaxrsJavaElement)element).getJavaElement();
			if(javaElement.getElementType() == IJavaElement.TYPE) {
				return ((IType)javaElement).getFullyQualifiedName();
			}
			return ((IJaxrsResourceMethod) element).getParentResource().getJavaElement().getFullyQualifiedName() + '.' + javaElement.getElementName() + "(...)";
		}
		return element.getName();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.eclipse.jface.text.hyperlink.IHyperlink#open()
	 */
	@Override
	public void open() {
		try {
			JavaUI.openInEditor(targetElement.getJavaElement());
		} catch (PartInitException e) {
			Logger.error("Failed to open " + getDisplayNameText(targetElement) + " in a Java Editor", e);
		} catch (JavaModelException e) {
			Logger.error("Failed to open " + getDisplayNameText(targetElement) + " in a Java Editor", e);
		}
	}
}
