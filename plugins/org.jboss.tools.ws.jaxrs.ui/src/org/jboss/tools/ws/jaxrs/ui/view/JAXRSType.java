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
package org.jboss.tools.ws.jaxrs.ui.view;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.jboss.tools.ws.jaxrs.ui.JBossJAXRSUIMessages;
import org.jboss.tools.ws.jaxrs.ui.JBossJaxrsUIPlugin;
import org.jboss.tools.ws.jaxrs.ui.utils.JAXRSTester;
import org.jboss.tools.ws.ui.utils.WSTestUtils;
import org.jboss.tools.ws.ui.views.CustomTestEntry;
import org.jboss.tools.ws.ui.views.RequestBodyComposite;
import org.jboss.tools.ws.ui.views.TestEntry;
import org.jboss.tools.ws.ui.views.WSTestStatus;
import org.jboss.tools.ws.ui.views.WSType;
import org.jboss.tools.ws.ui.views.WebServicesTestView;

public abstract class JAXRSType implements WSType {

	private RequestHeadersAndParamsComposite headersAndParams;
	private RequestBodyComposite body;

	protected WebServicesTestView view;

	@Override
	public IStatus handleWSTest(IProgressMonitor monitor, String url, String uid, String pwd) {

		// if we need to configure incoming parameters in the URL (i.e. from JAX-RS
		// tooling)
		if (url.endsWith("}")) { //$NON-NLS-1$
			OpenInputsDialogRunnable inputsDialog = new OpenInputsDialogRunnable(url);
			Display.getDefault().syncExec(inputsDialog);
			if(!inputsDialog.isOk) {
				return Status.OK_STATUS;
			}
			url = inputsDialog.getUrl();
		}
		

		JAXRSTester tester = new JAXRSTester();
		try {
			// count the request submission events
			JBossJaxrsUIPlugin.getDefault().countRequestSubmitted(getType());
			JAXRSTestEntry entry = getRSTestEntry(view.getCurrentEntry());

			// call the service
			String requestBody = null;
			if (requestBody()) {
				RequestBodyRunnable runnable = new RequestBodyRunnable();
				Display.getDefault().syncExec(runnable);
				requestBody = runnable.getRequestBody();
			}
			tester.doTest(url, entry.getRequestParams(), entry.getRequestHeaders(), getType(), requestBody, null, -1,
					uid, pwd);

			String result = tester.getResultBody();

			// put the results in the result text field
			String cleanedUp = WSTestUtils.addNLsToXML(result);

			WSTestStatus status = new WSTestStatus(IStatus.OK, JBossJaxrsUIPlugin.PLUGIN_ID,
					JBossJAXRSUIMessages.JAXRSWSTestView_JAXRS_Success_Status);
			status.setResultsText(cleanedUp);
			status.setHeaders(tester.getResultHeaders());
			monitor.worked(10);
			return status;
		} catch (Exception e) {
			return new WSTestStatus(IStatus.ERROR, JBossJaxrsUIPlugin.PLUGIN_ID, e.getMessage());
		}

	}

	@Override
	public void fillAdditionalRequestDetails(Composite parent) {
		headersAndParams = new RequestHeadersAndParamsComposite();
		headersAndParams.createControl(view, parent);

		if (requestBody()) {
			body = new RequestBodyComposite();
			body.createControl(view, parent);
		}

	}

	@Override
	public void setWebServicesView(WebServicesTestView view) {
		this.view = view;
	}

	@Override
	public List<IAction> getAdditonalToolActions() {
		return null;
	}

	public abstract boolean requestBody();

	public static JAXRSTestEntry getRSTestEntry(TestEntry entry) {
		CustomTestEntry customEntry = entry.getCustomEntry();
		if (!(customEntry instanceof JAXRSTestEntry)) {
			customEntry = new JAXRSTestEntry();
			entry.setCustomEntry(customEntry);
		}
		return (JAXRSTestEntry) customEntry;
	}

	@Override
	public void updateControlsForSelectedEntry(TestEntry entry) {
		JAXRSTestEntry rsEntry = getRSTestEntry(entry);
		headersAndParams.setHeadersAndParamsValues(rsEntry);
		if (body != null) {
			body.setBodyText(rsEntry.getBody());
		}

	}

	class RequestBodyRunnable implements Runnable {

		private String requestBody;

		@Override
		public void run() {
			requestBody = body.getBodyText();

		}

		public String getRequestBody() {
			return requestBody;
		}

	}
	
	class OpenInputsDialogRunnable implements Runnable {
		
		private String url;
		private boolean isOk;
		private WSTesterURLInputsDialog dialog;
		
		public OpenInputsDialogRunnable(String url) {
			this.url = url;
		}
		
		@Override
		public void run() {
			
			dialog = new WSTesterURLInputsDialog(view.getSite().getShell(), url);
			int rtn_code = dialog.open();
			if (rtn_code == Window.OK) {
				view.setURL(dialog.getURL());
				isOk = true;
			} else {
				isOk = false;
			}
		}
		
		public boolean isOk() {
			return isOk;
		}
		
		public String getUrl() {
			return dialog.getURL();
		}
		
		
		
	}

}
