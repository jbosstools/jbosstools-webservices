/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.ws.reddeer.ui.preferences;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.reddeer.core.reference.ReferencedComposite;
import org.eclipse.reddeer.jface.preference.PreferencePage;
import org.eclipse.reddeer.swt.api.TableItem;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.label.DefaultLabel;
import org.eclipse.reddeer.swt.impl.table.DefaultTable;

/**
 * Represents Preference page: 
* 		Web Services -> JBossWS Preferences
* 
 * @author jjankovi
 * @author Radoslav Rabara
 */
public class JBossWSRuntimePreferencePage extends PreferencePage {
	
	public JBossWSRuntimePreferencePage(ReferencedComposite referencedComposite) {
		super(referencedComposite, "Web Services", "JBossWS Preferences");
	}
	
	public void add() {
		new PushButton("Add").click();
	}
	
	public void edit() {
		new PushButton("Edit").click();
	}
	
	public void remove() {
		new PushButton("Remove").click();
	}
	
	public List<JBossWSRuntimeItem> getAllJBossWSRuntimes() {
		List<JBossWSRuntimeItem> runtimes =  new ArrayList<JBossWSRuntimeItem>();
		
		DefaultTable runtimesTable = getRuntimesTable();
		
		for (int i = 0; i < runtimesTable.rowCount(); i++) {
			TableItem row = runtimesTable.getItem(i);
			String name = row.getText(1);
			String version = row.getText(2);
			String path = row.getText(3);
			runtimes.add(new JBossWSRuntimeItem(name, version, path));
		}
		
		return runtimes;
	}
	
	public void select(int index) {
		getRuntimesTable().select(index);
	}
	
	public String getRuntimeImplementation() {
		return new DefaultLabel(3).getText();
	}
	
	public String getRuntimeVersion() {
		return new DefaultLabel(4).getText();
	}
	
	private DefaultTable getRuntimesTable() {
		return new DefaultTable();
	}
}
