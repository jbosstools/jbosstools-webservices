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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.forms.widgets.ScrolledPageBook;
import org.jboss.tools.ws.ui.JBossWSUIPlugin;
import org.jboss.tools.ws.ui.messages.JBossWSUIMessages;

/**
 * @since 2.0
 */
public class ShowRawAction extends ToggleAction {

	private ScrolledPageBook pageBook;
	private String pageId;

	public ShowRawAction(ScrolledPageBook pageBook, String pageId) {
		this.pageBook = pageBook;
		this.pageId = pageId;
	}

	public void run() {
		pageBook.showPage(pageId);
	}

	@Override
	public String getToolTipText() {
		return JBossWSUIMessages.JAXRSWSTestView2_ShowRaw_Tooltip;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return JBossWSUIPlugin.getImageDescriptor(JBossWSUIPlugin.IMG_DESC_SHOWRAW);
	}
}