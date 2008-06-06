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

package org.jboss.tools.ws.ui.preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jboss.tools.ws.core.classpath.JbossWSRuntime;
import org.jboss.tools.ws.core.classpath.JbossWSRuntimeManager;
import org.jboss.tools.ws.ui.messages.JbossWSUIMessages;

/**
 * @author Grid Qian
 */
public class JbossWSRuntimePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public JbossWSRuntimePreferencePage() {
		super();
		noDefaultAndApplyButton();
	}

	private static final int COLUMNS = 3;

	JbossRuntimeListFieldEditor jbossWSRuntimes = new JbossRuntimeListFieldEditor(
			"rtlist", JbossWSUIMessages.JBOSSWS_PREFERENCE_PAGE_RUNTIMES, new ArrayList<JbossWSRuntime>(Arrays.asList(JbossWSRuntimeManager.getInstance().getRuntimes()))); //$NON-NLS-1$

	/**
	 * Create contents of JbossWS preferences page. JbossWSRuntime list editor is
	 * created
	 * 
	 * @return Control
	 */
	@Override
	protected Control createContents(Composite parent) {
		Composite root = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(COLUMNS, false);
		root.setLayout(gl);
		jbossWSRuntimes.doFillIntoGrid(root);

		return root;
	}

	/**
	 * Inherited from IWorkbenchPreferencePage
	 * 
	 * @param workbench
	 *            {@link IWorkbench}
	 * 
	 */
	public void init(IWorkbench workbench) {
	}

	/**
	 * Save JbossWSRuntime list
	 */
	@Override
	protected void performApply() {
		for (JbossWSRuntime rt : jbossWSRuntimes.getAddedJbossWSRuntimes()) {
			JbossWSRuntimeManager.getInstance().addRuntime(rt);
		}
		jbossWSRuntimes.getAddedJbossWSRuntimes().clear();
		for (JbossWSRuntime rt : jbossWSRuntimes.getRemoved()) {
			JbossWSRuntimeManager.getInstance().removeRuntime(rt);
		}
		jbossWSRuntimes.getRemoved().clear();
		JbossWSRuntime defaultRuntime = jbossWSRuntimes.getDefaultJbossWSRuntime();
		// reset default runtime 
		for (JbossWSRuntime jbossWSRuntime : JbossWSRuntimeManager.getInstance().getRuntimes()) {
			jbossWSRuntime.setDefault(false);
		}
		// set deafult runtime
		defaultRuntime.setDefault(true);
		jbossWSRuntimes.setDefaultJbossWSRuntime(null);
		Map<JbossWSRuntime, JbossWSRuntime> changed = jbossWSRuntimes.getChangedJbossWSRuntimes();
		for (JbossWSRuntime c : changed.keySet()) {
			JbossWSRuntime o = changed.get(c);
			o.setHomeDir(c.getHomeDir());
			String oldName = o.getName();
			String newName = c.getName();
			if (!oldName.equals(newName)) {
				JbossWSRuntimeManager.getInstance().changeRuntimeName(oldName, newName);
			}
		}
		jbossWSRuntimes.getChangedJbossWSRuntimes().clear();

		JbossWSRuntimeManager.getInstance().save();
	}

	/**
	 * Restore original preferences values
	 */
	@Override
	protected void performDefaults() {
		setValid(true);
		setMessage(null);
		performApply();
	}

	/**
	 * See {@link PreferencePage} for details
	 * 
	 * @return Boolean
	 */
	@Override
	public boolean performOk() {
		performApply();
		return super.performOk();
	}
}
