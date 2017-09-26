/******************************************************************************* 
 * Copyright (c) 2017 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.ws.ui.views;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.widgets.Composite;
/**
 * @since 2.0
 */
public interface WSType {
	
	String getType();
	IStatus handleWSTest(IProgressMonitor monitor, String url, String uid, String pwd);
	void fillAdditionalRequestDetails(Composite parent);
	List<IAction> getAdditonalToolActions();
	void setWebServicesView(WebServicesTestView view);
	void updateControlsForSelectedEntry(TestEntry entry);
	
}
