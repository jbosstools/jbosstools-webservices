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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceDialog;
import org.jboss.tools.test.util.WorkbenchUtils;
import org.jboss.tools.ws.core.JbossWSCorePlugin;
import org.jboss.tools.ws.core.classpath.JbossWSRuntime;
import org.jboss.tools.ws.core.classpath.JbossWSRuntimeListConverter;
import org.jboss.tools.ws.core.classpath.JbossWSRuntimeManager;
import org.jboss.tools.ws.core.messages.JbossWSCoreMessages;
import org.jboss.tools.ws.ui.preferences.JbossRuntimeListFieldEditor;
import org.jboss.tools.ws.ui.preferences.JbossWSRuntimePreferencePage;

import junit.framework.TestCase;

/**
 * @author Grid Qian
 */
public class JbossWSRuntimePreferencePageTest extends TestCase {

	/**
	 * Test that preference page is showed up without errors
	 */
	public void testShowJbossWSRuntimePreferencePage() {

		PreferenceDialog prefDialog = WorkbenchUtils
				.createPreferenceDialog("org.jboss.tools.ws.ui.preferences.JbossWSRuntimePreferencePage");
		try {
			Object object = openPreferencepage(prefDialog);
			assertTrue(
					"Selected page is not an instance of JbossWSRuntimePreferencePage",
					object instanceof JbossWSRuntimePreferencePage);
		} finally {
			prefDialog.close();
		}
	}

	private Object openPreferencepage(PreferenceDialog prefDialog) {
		prefDialog.setBlockOnOpen(false);
		prefDialog.open();
		return prefDialog.getSelectedPage();
	}

	/**
	 * Test correct contents in that preference page
	 */
	@SuppressWarnings("unchecked")
	public void testDisplayJbossWSRuntimePreferencePage() {
		PreferenceDialog prefDialog = WorkbenchUtils
				.createPreferenceDialog("org.jboss.tools.ws.ui.preferences.JbossWSRuntimePreferencePage");
		JbossWSRuntimePreferencePage selectedPage = null;
		try {
			Object object = openPreferencepage(prefDialog);
			String runtime = getRuntimeList();
			selectedPage = (JbossWSRuntimePreferencePage) object;
			JbossRuntimeListFieldEditor jbossWSRuntimes = selectedPage
					.getJbossWSRuntimes();
			if (runtime.equals("")) {
				assertTrue(
						"The JBoss Ws Runtime locations are not displayed",
						((ArrayList<JbossWSRuntime>) jbossWSRuntimes.getValue())
								.size() == 0);
			} else {
				JbossWSRuntimeListConverter converter = new JbossWSRuntimeListConverter();
				Map<String, JbossWSRuntime> runtimes = converter
						.getMap(runtime);
				assertTrue(
						"The JBoss Ws Runtime locations are not displayed",
						runtimes.values().size() == ((ArrayList<JbossWSRuntime>) jbossWSRuntimes
								.getValue()).size());
			}
		} finally {
			prefDialog.close();
		}

	}

	/**
	 * Set and Test correct contents in that preference page
	 */
	@SuppressWarnings("unchecked")
	public void testSetAndDisplayJbossWSRuntimePreferencePage() {
		setRuntimeList();
		PreferenceDialog prefDialog = WorkbenchUtils
				.createPreferenceDialog("org.jboss.tools.ws.ui.preferences.JbossWSRuntimePreferencePage");
		JbossWSRuntimePreferencePage selectedPage = null;
		try {
			Object object = openPreferencepage(prefDialog);
			String runtime = getRuntimeList();
			selectedPage = (JbossWSRuntimePreferencePage) object;
			JbossRuntimeListFieldEditor jbossWSRuntimes = selectedPage
					.getJbossWSRuntimes();
			assertTrue("The preference store for jboss ws runtime is wrong",
					!runtime.equals(""));
			JbossWSRuntimeListConverter converter = new JbossWSRuntimeListConverter();
			Map<String, JbossWSRuntime> runtimes = converter.getMap(runtime);
			assertTrue(
					"The JBoss Ws Runtime locations are not displayed correctly",
					runtimes.values().size() == ((ArrayList<JbossWSRuntime>) jbossWSRuntimes
							.getValue()).size());
		} finally {
			prefDialog.close();
		}

	}

	private void setRuntimeList() {
		String jbosshome = System.getProperty("jbosstools.test.jboss.home.4.2",
				"/home/grid/Software/jboss-4.2.2.GA");
		String runtime = "name|jboss-4.2.2.GA|version|2.0|homeDir|" + jbosshome
				+ "|default|false|userConfig|true|libraries|" + jbosshome
				+ "/lib/commons-codec.jar,name|jboss|version|2.0|homeDir|"
				+ jbosshome + "|default|true|userConfig|false|libraries|";
		JbossWSCorePlugin.getDefault().getPreferenceStore().setValue(
				JbossWSCoreMessages.WS_Location, runtime);
		IPreferenceStore store = JbossWSCorePlugin.getDefault()
				.getPreferenceStore();
		if (store instanceof IPersistentPreferenceStore) {
			try {
				((IPersistentPreferenceStore) store).save();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		JbossWSRuntimeManager.getInstance().load();

	}

	private String getRuntimeList() {
		IPreferenceStore ps = JbossWSCorePlugin.getDefault()
				.getPreferenceStore();
		String runtimeListString = ps
				.getString(JbossWSCoreMessages.WS_Location);
		return runtimeListString;
	}

}
