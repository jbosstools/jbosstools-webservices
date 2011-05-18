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

package org.jboss.tools.ws.jaxrs.ui.cnf.action;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.navigator.CommonViewer;
import org.jboss.tools.ws.jaxrs.core.metamodel.ResourceMethod;
import org.jboss.tools.ws.jaxrs.ui.cnf.UriPathTemplateElement;
import org.jboss.tools.ws.jaxrs.ui.cnf.UriPathTemplateMethodMappingElement;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;

public class OpenJavaEditorAction extends Action implements ISelectionChangedListener {

	private ISelection selection = null;

	public OpenJavaEditorAction() {
		super("Open in Java Editor");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run() {
		ITreeSelection treeSelection = ((ITreeSelection) selection);
		for (Object o : treeSelection.toList()) {
			try {
				if (o instanceof UriPathTemplateElement) {
					ResourceMethod lastMethod = ((UriPathTemplateElement) o).getLastMethod();
					IMethod javaMethod = lastMethod.getJavaElement();
					JavaUI.revealInEditor(JavaUI.openInEditor(javaMethod), (IJavaElement) javaMethod);
				} else if (o instanceof UriPathTemplateMethodMappingElement) {
					ResourceMethod lastMethod = ((UriPathTemplateMethodMappingElement) o).getLastMethod();
					IMethod javaMethod = lastMethod.getJavaElement();
					JavaUI.revealInEditor(JavaUI.openInEditor(javaMethod), (IJavaElement) javaMethod);
				}
			} catch (Exception e) {
				Logger.error("Failed to open Java editor", e);
			}
		}
		super.run();
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		Object source = event.getSource();
		if (source instanceof CommonViewer) {
			this.selection = ((CommonViewer) source).getSelection();
		}
	}

	public void setSelection(ISelection selection) {
		this.selection = selection;

	}

}
