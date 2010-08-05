/******************************************************************************* 
 * Copyright (c) 2010 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.ws.ui.views;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.ws.ui.messages.JBossWSUIMessages;

/**
 * Simple UID/PWD dialog for authentication
 * @author bfitzpat
 *
 */
public class UidPwdDialog extends TitleAreaDialog {
	private Text uidText;
	private Text pwdText;

	private static String uid;
	private static String pwd;

	public UidPwdDialog(Shell parentShell) {
		super(parentShell);
	}

	protected Control createDialogArea(Composite parent) {
		setTitle(JBossWSUIMessages.UidPwdDialog_Title);
		setMessage(JBossWSUIMessages.UidPwdDialog_Description);

		Composite comp = new Composite (parent,SWT.NONE);

		GridLayout layout = new GridLayout(2, false);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.horizontalSpan = 2;
		comp.setLayoutData(gridData);
		comp.setLayout(layout);
		
		Label usernameLabel = new Label(comp, SWT.RIGHT);
		usernameLabel.setText(JBossWSUIMessages.UidPwdDialog_UID_Label);

		uidText = new Text(comp, SWT.SINGLE | SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		uidText.setLayoutData(data);
		if (uid != null && uid.trim().length() > 0) {
			uidText.setText(uid);
		}
		uidText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				uid = uidText.getText();
			}
		});

		Label passwordLabel = new Label(comp, SWT.RIGHT);
		passwordLabel.setText(JBossWSUIMessages.UidPwdDialog_PWD_Label);

		pwdText = new Text(comp, SWT.SINGLE | SWT.PASSWORD | SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		pwdText.setLayoutData(data);
		if (pwd != null && pwd.trim().length() > 0) {
			pwdText.setText(pwd);
		}
		pwdText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				pwd = pwdText.getText();
			}
		});

		return comp;
	}

	public String getUID() {
		return uid;
	}
	
	public String getPwd() {
		return pwd;
	}
}