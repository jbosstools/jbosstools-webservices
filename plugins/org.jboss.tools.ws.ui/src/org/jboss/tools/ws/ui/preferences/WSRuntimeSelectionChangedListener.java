/******************************************************************************* 
 * Copyright (c) 2008 - 2014 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.ws.ui.preferences;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Label;
import org.jboss.tools.ws.core.classpath.JBossWSRuntime;

public class WSRuntimeSelectionChangedListener implements
		ISelectionChangedListener {
	
	private Label impl;
	private Label vdetail;
	
	public WSRuntimeSelectionChangedListener(Label impl, Label vdetail) {
		this.impl = impl;
		this.vdetail = vdetail;
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		ISelection selection = event.getSelection();
		if (selection instanceof StructuredSelection) {
			JBossWSRuntime runtime = (JBossWSRuntime)((StructuredSelection)selection).getFirstElement();
			if (runtime == null) {
				return;
			}
			impl.setText(runtime.getImpl());
			vdetail.setText(runtime.getVersionDetail());
		}
	}

}
