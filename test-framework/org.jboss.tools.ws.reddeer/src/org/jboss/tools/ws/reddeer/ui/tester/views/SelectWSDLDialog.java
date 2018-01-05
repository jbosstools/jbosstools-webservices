/*******************************************************************************
 * Copyright (c) 2010-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.ws.reddeer.ui.tester.views;

import java.util.Arrays;
import java.util.List;

import org.eclipse.reddeer.swt.api.Combo;
import org.eclipse.reddeer.swt.api.Shell;
import org.eclipse.reddeer.swt.condition.ShellIsActive;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.exception.SWTLayerException;
import org.eclipse.reddeer.common.exception.RedDeerException;
import org.eclipse.reddeer.common.exception.WaitTimeoutExpiredException;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.combo.LabeledCombo;
import org.eclipse.reddeer.swt.impl.list.DefaultList;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.jboss.tools.common.reddeer.label.IDELabel;
import org.jboss.tools.ws.jaxws.ui.JBossJAXWSUIMessages;
import org.jboss.tools.ws.reddeer.ui.dialogs.InputDialog;
import org.jboss.tools.ws.ui.messages.JBossWSUIMessages;

/**
 * Represents Select WSDL dialog<br/>.
 * Is invoked from {@link WsTesterView}.
 *
 * @see org.jboss.tools.ws.ui.views.WSDLBrowseDialog
 * 
 * @author Lukas Jungmann
 * @author jjankovi 
 * @author Radoslav Rabara
 *
 */
public class SelectWSDLDialog extends DefaultShell {

	public final String TITLE;
	/**
	 * Constructs Select WSDL dialog.
	 */
	public SelectWSDLDialog() {
		super(JBossJAXWSUIMessages.WSDLBrowseDialog_Dialog_Title);
		TITLE = this.getText();
	}

	/**
	 * Click on "URL..." button. {@link InputDialog} is invoked as a result of
	 * this action.
	 */
	public void openURL() {
		new PushButton(JBossJAXWSUIMessages.WSDLBrowseDialog_URL_Browse).click();
	}

	/**
	 * Sets the specified <var>uri</var> to URI field.
	 *
	 * @param uri the specified uri that will be set in URI field
	 */
	public void setURI(String uri) {
		//WSDL URI:
		getURICombo().setText(uri);
	}

	/**
	 * Returns URI from the URI field.
	 * @return URI
	 */
	public String getURI() {
		//WSDL URI:
		return getURICombo().getText();
	}

	/**
	 * Returns all services.
	 * @return list of services
	 */
	public List<String> getServices() {
		//Service:
		return new LabeledCombo(JBossJAXWSUIMessages.WSDLBrowseDialog_Service_Field)
				.getItems();
	}

	/**
	 * Select the specified <var>service</var>
	 *
	 * @param service service name that will be selected
	 */
	public void selectService(String service) {
		new LabeledCombo(JBossJAXWSUIMessages.WSDLBrowseDialog_Service_Field)
				.setSelection(service);
	}

	/**
	 * Returns all ports.
	 * @return list of ports
	 */
	public List<String> getPorts() {
		//Port:
		return new LabeledCombo(JBossJAXWSUIMessages.WSDLBrowseDialog_Port_Field)
				.getItems();
	}

	/**
	 * Selects the specified <var>port</var>.
	 *
	 * @param port port name that will be selected
	 */
	public void selectPort(String port) {
		//Service:
		new LabeledCombo(JBossJAXWSUIMessages.WSDLBrowseDialog_Port_Field)
				.setSelection(port);
	}

	/**
	 * Returns all operations.
	 * @return list of operations
	 */
	public List<String> getOperations() {
		return Arrays.asList(getOperationsList().getListItems());
	}

	/**
	 * Selects the specified <var>operation</var>.
	 * @param operation operation name that will be selected
	 */
	public void selectOperation(String operation) {
		getOperationsList().select(operation);
	}

	/**
	 * Click on the OK button.
	 */
	public void ok() {
		new PushButton(IDELabel.Button.OK).click();
		new WaitWhile(new ShellIsAvailable(this));
		
		// when replacing some existing WS message, press OK to confirm
		Shell incorrectShell = null;
		try {
			incorrectShell = new DefaultShell("Message May Be Incorrect for Selected WSDL");
		} catch (RedDeerException sle) {
			// no WS message replacing - no dialog appeared
		}
		if(incorrectShell != null) {
			new PushButton(incorrectShell, IDELabel.Button.YES).click();
			new WaitWhile(new ShellIsAvailable(incorrectShell));
		}
	}

	private org.eclipse.reddeer.swt.api.List getOperationsList() {
		//Operation:
		return new DefaultList(JBossJAXWSUIMessages.WSDLBrowseDialog_Operation_Field);
	}

	private Combo getURICombo() {
		return new LabeledCombo(JBossJAXWSUIMessages.WSDLBrowseDialog_WSDL_URI_Field);
	}
}