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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jboss.tools.ws.core.classpath.JBossWSRuntime;
import org.jboss.tools.ws.core.classpath.JBossWSRuntimeManager;
import org.jboss.tools.ws.ui.messages.JBossWSUIMessages;
import org.jboss.tools.ws.ui.utils.JBossWSUIUtils;

/**
 * @author Grid Qian
 */
public class JBossWSRuntimePreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	public JBossWSRuntimePreferencePage() {
		super();
		noDefaultAndApplyButton();
	}

	private static final int COLUMNS = 3;

	JBossWSRuntimeListFieldEditor jbossWSRuntimes = new JBossWSRuntimeListFieldEditor(
			"rtlist", JBossWSUIMessages.JBossWS_Preference_Page_Runtimes, new ArrayList<JBossWSRuntime>(Arrays.asList(JBossWSRuntimeManager.getInstance().getRuntimes()))); //$NON-NLS-1$

	Label impl;
	Label vDetail;
	
	/**
	 * Create contents of JBossWS preferences page. JBossWSRuntime list editor
	 * is created
	 */
	@Override
	protected Control createContents(Composite parent) {
		Composite root = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(COLUMNS, false);
		root.setLayout(gl);
		jbossWSRuntimes.doFillIntoGrid(root);
		
		Label[] texts = JBossWSUIUtils.createWSRuntimeDetailsGroup(root, 2);
		impl = texts[0];
		vDetail = texts[1];
		
		jbossWSRuntimes.getTableView().addSelectionChangedListener(new WSRuntimeSelectionChangedListener(impl, vDetail));

		return root;
	}

	/**
	 * Inherited from IWorkbenchPreferencePage
	 */
	public void init(IWorkbench workbench) {
	}

	/**
	 * Save JBossWSRuntime list
	 */
	@Override
	protected void performApply() {
		for (JBossWSRuntime rt : jbossWSRuntimes.getAddedJBossWSRuntimes()) {
			JBossWSRuntimeManager.getInstance().addRuntime(rt);
		}
		jbossWSRuntimes.getAddedJBossWSRuntimes().clear();
		for (JBossWSRuntime rt : jbossWSRuntimes.getRemoved()) {
			JBossWSRuntimeManager.getInstance().removeRuntime(rt);
		}
		jbossWSRuntimes.getRemoved().clear();
		JBossWSRuntime defaultRuntime = jbossWSRuntimes
				.getDefaultJBossWSRuntime();

		// reset default runtime
		for (JBossWSRuntime jbossWSRuntime : JBossWSRuntimeManager
				.getInstance().getRuntimes()) {
			jbossWSRuntime.setDefault(false);
		}
		// set default runtime
		if (defaultRuntime != null) {
			defaultRuntime.setDefault(true);
		}

		jbossWSRuntimes.setDefaultJBossWSRuntime(null);
		Map<JBossWSRuntime, JBossWSRuntime> changed = jbossWSRuntimes
				.getChangedJBossWSRuntimes();
		for (JBossWSRuntime c : changed.keySet()) {
			JBossWSRuntime o = changed.get(c);
			o.setHomeDir(c.getHomeDir());
			o.setVersion(c.getVersion());
			String oldName = o.getName();
			String newName = c.getName();
			if (!oldName.equals(newName)) {
				JBossWSRuntimeManager.getInstance().changeRuntimeName(oldName,
						newName);
			}
			o.setDefault(c.isDefault());
			o.setUserConfigClasspath(c.isUserConfigClasspath());
			o.setLibraries(c.getLibraries());
		}
		jbossWSRuntimes.getChangedJBossWSRuntimes().clear();

		JBossWSRuntimeManager.getInstance().save();
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
	 */
	@Override
	public boolean performOk() {
		performApply();
		return super.performOk();
	}

	public JBossWSRuntimeListFieldEditor getJBossWSRuntimes() {
		return jbossWSRuntimes;
	}
}
