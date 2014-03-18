/******************************************************************************* 
 * Copyright (c) 2009 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.ws.jaxrs.ui.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jboss.tools.ws.jaxrs.ui.JBossJaxrsUIPlugin;

/**
 * @author Xavier Coulon
 */
public class JaxrsPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	
	/**
	 * @see plugin.xml descriptor for ID
	 */
	public static final String ID = JBossJaxrsUIPlugin.PLUGIN_ID;

	@Override
	protected Control createContents(Composite parent) {
		Composite root = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(1, false);
		root.setLayout(gl);

		return root;
	}

	public void init(IWorkbench workbench) {
	}
}