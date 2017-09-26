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

import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;

/**
 * @since 2.0
 */
public class FormExpansionAdapter extends ExpansionAdapter {

	private WebServicesTestView view;

	public FormExpansionAdapter(WebServicesTestView view) {
		this.view = view;
	}

	public void expansionStateChanged(ExpansionEvent e) {
		view.redrawRequestDetails();
	}
}