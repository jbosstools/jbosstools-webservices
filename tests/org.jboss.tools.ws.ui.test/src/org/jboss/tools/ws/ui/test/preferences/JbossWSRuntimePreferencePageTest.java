/******************************************************************************* 
 * Copyright (c) 2008 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 

package org.jboss.tools.ws.ui.test.preferences;

import org.eclipse.jface.preference.PreferenceDialog;
import org.jboss.tools.test.util.WorkbenchUtils;
import org.jboss.tools.ws.ui.preferences.JbossWSRuntimePreferencePage;

import junit.framework.TestCase;

/**
 * @author Grid Qian
 */
public class JbossWSRuntimePreferencePageTest extends TestCase {
	
	/**
	 * Test that preference page is showed up without errors
	 */
	public void testShowSeamPreferencePage() {
		
		PreferenceDialog prefDialog = 
			WorkbenchUtils.createPreferenceDialog("org.jboss.tools.ws.ui.preferences.JbossWSRuntimePreferencePage");

		try {
			prefDialog.setBlockOnOpen(false);
			prefDialog.open();
			
			Object selectedPage = prefDialog.getSelectedPage();
			assertTrue("Selected page is not an instance of JbossWSRuntimePreferencePage", selectedPage instanceof JbossWSRuntimePreferencePage);
		} finally {
			prefDialog.close();
		}
	}
}
