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
import org.jboss.tools.ws.core.JBossWSCorePlugin;
import org.jboss.tools.ws.core.classpath.JBossWSRuntime;
import org.jboss.tools.ws.core.classpath.JBossWSRuntimeListConverter;
import org.jboss.tools.ws.core.classpath.JBossWSRuntimeManager;
import org.jboss.tools.ws.core.messages.JBossWSCoreMessages;
import org.jboss.tools.ws.ui.preferences.JBossWSRuntimeListFieldEditor;
import org.jboss.tools.ws.ui.preferences.JBossWSRuntimePreferencePage;

import junit.framework.TestCase;

/**
 * @author Grid Qian
 */
public class JBossWSRuntimePreferencePageTest extends TestCase {

	/**
	 * Test that preference page is showed up without errors
	 */
	public void testShowJBossWSRuntimePreferencePage() {

		PreferenceDialog prefDialog = WorkbenchUtils
				.createPreferenceDialog("org.jboss.tools.ws.ui.preferences.JBossWSRuntimePreferencePage");
		try {
			Object object = openPreferencepage(prefDialog);
			assertTrue(
					"Selected page is not an instance of JBossWSRuntimePreferencePage",
					object instanceof JBossWSRuntimePreferencePage);
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
	public void testDisplayJBossWSRuntimePreferencePage() {
		PreferenceDialog prefDialog = WorkbenchUtils
				.createPreferenceDialog("org.jboss.tools.ws.ui.preferences.JBossWSRuntimePreferencePage");
		JBossWSRuntimePreferencePage selectedPage = null;
		try {
			Object object = openPreferencepage(prefDialog);
			String runtime = getRuntimeList();
			selectedPage = (JBossWSRuntimePreferencePage) object;
			JBossWSRuntimeListFieldEditor jbossWSRuntimes = selectedPage
					.getJBossWSRuntimes();
			if (runtime.equals("")) {
				assertTrue(
						"The JBoss Ws Runtime locations are not displayed",
						((ArrayList<JBossWSRuntime>) jbossWSRuntimes.getValue())
								.size() == 0);
			} else {
				JBossWSRuntimeListConverter converter = new JBossWSRuntimeListConverter();
				Map<String, JBossWSRuntime> runtimes = converter
						.getMap(runtime);
				assertTrue(
						"The JBoss Ws Runtime locations are not displayed",
						runtimes.values().size() == ((ArrayList<JBossWSRuntime>) jbossWSRuntimes
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
	public void testSetAndDisplayJBossWSRuntimePreferencePage() {
		setRuntimeList();
		PreferenceDialog prefDialog = WorkbenchUtils
				.createPreferenceDialog("org.jboss.tools.ws.ui.preferences.JBossWSRuntimePreferencePage");
		JBossWSRuntimePreferencePage selectedPage = null;
		try {
			Object object = openPreferencepage(prefDialog);
			String runtime = getRuntimeList();
			selectedPage = (JBossWSRuntimePreferencePage) object;
			JBossWSRuntimeListFieldEditor jbossWSRuntimes = selectedPage
					.getJBossWSRuntimes();
			assertTrue("The preference store for jboss ws runtime is wrong",
					!runtime.equals(""));
			JBossWSRuntimeListConverter converter = new JBossWSRuntimeListConverter();
			Map<String, JBossWSRuntime> runtimes = converter.getMap(runtime);
			assertTrue(
					"The JBoss Ws Runtime locations are not displayed correctly",
					runtimes.values().size() == ((ArrayList<JBossWSRuntime>) jbossWSRuntimes
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
		JBossWSCorePlugin.getDefault().getPreferenceStore().setValue(
				JBossWSCoreMessages.WS_Location, runtime);
		IPreferenceStore store = JBossWSCorePlugin.getDefault()
				.getPreferenceStore();
		if (store instanceof IPersistentPreferenceStore) {
			try {
				((IPersistentPreferenceStore) store).save();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		JBossWSRuntimeManager.getInstance().load();

	}

	private String getRuntimeList() {
		IPreferenceStore ps = JBossWSCorePlugin.getDefault()
				.getPreferenceStore();
		String runtimeListString = ps
				.getString(JBossWSCoreMessages.WS_Location);
		return runtimeListString;
	}

}
