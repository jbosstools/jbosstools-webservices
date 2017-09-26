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
package org.jboss.tools.ws.jaxws.ui.view;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.wsdl.extensions.soap12.SOAP12Operation;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.apache.axis.soap.MessageFactoryImpl;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.jboss.tools.ws.jaxws.ui.JBossJAXWSUIMessages;
import org.jboss.tools.ws.jaxws.ui.JBossJAXWSUIPlugin;
import org.jboss.tools.ws.jaxws.ui.dialogs.WSDLBrowseDialog;
import org.jboss.tools.ws.jaxws.ui.schema.SchemaUtils;
import org.jboss.tools.ws.jaxws.ui.tester.JAXWSTester2;
import org.jboss.tools.ws.jaxws.ui.tester.TesterWSDLUtils;
import org.jboss.tools.ws.ui.JBossWSUIPlugin;
import org.jboss.tools.ws.ui.utils.WSTestUtils;
import org.jboss.tools.ws.ui.views.CustomTestEntry;
import org.jboss.tools.ws.ui.views.RequestBodyComposite;
import org.jboss.tools.ws.ui.views.TestEntry;
import org.jboss.tools.ws.ui.views.WSTestStatus;
import org.jboss.tools.ws.ui.views.WSType;
import org.jboss.tools.ws.ui.views.WebServicesTestView;
import org.jboss.wise.ui.internal.util.WiseUtil;
import org.w3c.dom.Element;

public class JAXWSType implements WSType {

	private WebServicesTestView view;
	private RequestBodyComposite body;

	@Override
	public String getType() {
		return JBossJAXWSUIPlugin.JAX_WS;
	}

	@Override
	public IStatus handleWSTest(IProgressMonitor monitor, String url, String uid, String pwd) {
		JAXWSTestEntry wsEntry = getWSEntry(view.getCurrentEntry());
		
		RequestBodyRunnable runnable = new RequestBodyRunnable();
		Display.getDefault().syncExec(runnable);
		String requestBody = runnable.getRequestBody();
		
		//if request body changed we need to acquire all info again
		if(!requestBody.equals(wsEntry.getBody())) {
			wsEntry = new JAXWSTestEntry();
			wsEntry.setBody(requestBody);
			view.getCurrentEntry().setCustomEntry(wsEntry);
		}
		String lookForOpName = getOpNameFromRequestBody();
		
		if (wsEntry != null && wsEntry.getAction() == null) {
			boolean result = getWSDLSpecifics(monitor, url, lookForOpName);
			if (!result)
				return Status.OK_STATUS;
		}

		try {
			// count the request submission events
			JBossWSUIPlugin.getDefault().countRequestSubmitted("JAX-WS"); //$NON-NLS-1$

			monitor.worked(10);
			JAXWSTester2 tester = new JAXWSTester2();
			boolean itRan = false;
			String[] serviceNSMessage = wsEntry.getNsMessage();
			String action = wsEntry.getAction();
			while (!monitor.isCanceled()) {
				try {
					if (!itRan && serviceNSMessage != null && serviceNSMessage.length == 3) {
						itRan = true; // call the service
						tester.doTest(monitor, url, action, serviceNSMessage[0], serviceNSMessage[1],
								serviceNSMessage[2], requestBody, uid, pwd);
					} else {
						break;
					}
				} catch (InterruptedException ie) {
					monitor.setCanceled(true);
				}
			}
			if (monitor.isCanceled()) {
				WSTestStatus status = new WSTestStatus(IStatus.OK, JBossWSUIPlugin.PLUGIN_ID,
						JBossJAXWSUIMessages.JAXRSWSTestView_Message_Service_Invocation_Cancelled);
				return status;
			}
			if (!itRan) {
				WSTestStatus status = new WSTestStatus(IStatus.OK, JBossWSUIPlugin.PLUGIN_ID,
						JBossJAXWSUIMessages.JAXRSWSTestView_Message_Unsuccessful_Test);
				return status;
			}
			monitor.worked(70);
			String result = tester.getResultBody();
			String cleanedUp = WSTestUtils.addNLsToXML(result);

			WSTestStatus status = new WSTestStatus(IStatus.OK, JBossWSUIPlugin.PLUGIN_ID,
					JBossJAXWSUIMessages.JAXRSWSTestView_JAXWS_Success_Status);
			status.setResultsText(cleanedUp);
			monitor.worked(10);

			status.setHeaders(tester.getResultHeaders());
			monitor.worked(10);
			return status;
		} catch (Exception e) {

			// try and drill down to find the root cause
			Throwable innerE = e.getCause();

			// if we can't find it, just go with th exception
			if (innerE == null) {
				WSTestStatus status = new WSTestStatus(IStatus.OK, JBossWSUIPlugin.PLUGIN_ID,
						JBossJAXWSUIMessages.JAXRSWSTestView_Exception_Status + e.getLocalizedMessage());
				status.setResultsText(e.toString());
				// this fix is to address JBIDE-11294 and the fact that we shouldn't actually
				// log this exception from deep
				// within the WS API.
				if ((!e.getLocalizedMessage().contains("Unsupported endpoint address: REPLACE_WITH_ACTUAL_URL"))) { //$NON-NLS-1$
					JBossWSUIPlugin.log(e);
				}
				return status;
			}

			// continue to drill down until we find the innermost one
			while (innerE.getCause() != null) {
				innerE = innerE.getCause();
			}

			// Now report that
			WSTestStatus status = new WSTestStatus(IStatus.OK, JBossWSUIPlugin.PLUGIN_ID,
					JBossJAXWSUIMessages.JAXRSWSTestView_Exception_Status + innerE.getLocalizedMessage());
			status.setResultsText(innerE.toString());

			// this fix is to address JBIDE-11294 and the fact that we shouldn't actually
			// log this exception from deep
			// within the WS API.
			if ((!innerE.getLocalizedMessage().contains("Unsupported endpoint address: REPLACE_WITH_ACTUAL_URL"))) { //$NON-NLS-1$
				JBossWSUIPlugin.log(e);
			}
			return status;
		}
	}

	@Override
	public void fillAdditionalRequestDetails(Composite parent) {
		body = new RequestBodyComposite();
		body.createControl(view, parent);
	}

	private String generateSampleSOAP(String headerText, String innerText, boolean isSOAP12) {
		if (innerText != null && !innerText.trim().isEmpty()) {
			if (innerText.trim().startsWith("<?xml version=\"1.0\"")) { //$NON-NLS-1$
				return innerText;
			}
		}

		String prefix = TesterWSDLUtils.SOAP_PREFIX;
		String soapURI = TesterWSDLUtils.SOAP_NS_URI;
		if (isSOAP12) {
			prefix = TesterWSDLUtils.SOAP12_PREFIX;
			soapURI = TesterWSDLUtils.SOAP12_ENVELOPE_NS_URI;
		}
		StringBuffer buffer = new StringBuffer();
		buffer.append("<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\" ?>\n");//$NON-NLS-1$
		buffer.append("<" + prefix + ":Envelope xmlns:" + prefix + "=\"" + soapURI + "\" ");//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		buffer.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");//$NON-NLS-1$
		buffer.append("xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" >\n");//$NON-NLS-1$
		buffer.append("<" + prefix + ":Header>\n");//$NON-NLS-1$ //$NON-NLS-2$
		if (headerText != null)
			buffer.append(headerText);
		buffer.append("</" + prefix + ":Header>\n"); //$NON-NLS-1$ //$NON-NLS-2$
		;
		buffer.append("<" + prefix + ":Body>\n");//$NON-NLS-1$ //$NON-NLS-2$
		if (innerText != null)
			buffer.append(innerText);
		buffer.append("</" + prefix + ":Body>\n"); //$NON-NLS-1$ //$NON-NLS-2$
		;
		buffer.append("</" + prefix + ":Envelope>");//$NON-NLS-1$ //$NON-NLS-2$
		return buffer.toString();
	}

	@Override
	public List<IAction> getAdditonalToolActions() {

		List<IAction> actions = new ArrayList<IAction>();

		class GetWSDLSpecificsAction extends Action {

			@Override
			public ImageDescriptor getImageDescriptor() {
				return JBossJAXWSUIPlugin.getImageDescriptor(JBossJAXWSUIPlugin.IMG_DESC_WSDL);
			}

			@Override
			public void run() {
				final String url = view.getURL();
				Job getSpecificsJob = new Job("Get WSDL") {

					@Override
					protected IStatus run(IProgressMonitor monitor) {
						getWSDLSpecifics(monitor, url, null);
						return Status.OK_STATUS;
					}
				};
				getSpecificsJob.setUser(true);
				getSpecificsJob.schedule();
			}

			@Override
			public String getToolTipText() {
				return JBossJAXWSUIMessages.JAXRSWSTestView2_GetFromWSDL_Tooltip;
			}

		}

		actions.add(new GetWSDLSpecificsAction());
		return actions;
	}

	public boolean getWSDLSpecifics(IProgressMonitor monitor, String url, String opName) {
		if (opName != null) {
			JAXWSTestEntry history = getWSEntry(view.getCurrentEntry());
			String opNameInBody = getOpNameFromRequestBody();
			if (opNameInBody != null) {
				boolean isRequestSOAP12 = TesterWSDLUtils.isRequestBodySOAP12(history.getBody());
				if (url != null) {
					try {
						URL tempURL = new URL(url);
						Definition wsdlDef = TesterWSDLUtils.readWSDLURL(tempURL);
						Map<?, ?> bindings = wsdlDef.getAllBindings();
						Iterator<?> iter = bindings.entrySet().iterator();
						while (iter.hasNext()) {
							Entry<?, ?> mapEntry = (Entry<?, ?>) iter.next();
							Binding binding = (Binding) mapEntry.getValue();
							Iterator<?> iter2 = binding.getBindingOperations().iterator();
							while (iter2.hasNext()) {
								BindingOperation bindOp = (BindingOperation) iter2.next();
								String tempOpName = opNameInBody;
								if (tempOpName.indexOf(':') > -1) {
									tempOpName = tempOpName.substring(tempOpName.indexOf(':') + 1);
								}
								if (bindOp.getName().contentEquals(opNameInBody)
										|| bindOp.getName().contentEquals(tempOpName)) {
									Iterator<?> iter3 = bindOp.getExtensibilityElements().iterator();
									while (iter3.hasNext()) {
										ExtensibilityElement extEl = (ExtensibilityElement) iter3.next();
										if (extEl.getElementType().getLocalPart().contentEquals("operation")) { //$NON-NLS-1$
											String actionURL = null;
											String[] nsArray = null;
											if (!isRequestSOAP12 && extEl instanceof SOAPOperation) {
												SOAPOperation soapOp = (SOAPOperation) extEl;
												actionURL = soapOp.getSoapActionURI();
											} else if (isRequestSOAP12 && extEl instanceof SOAP12Operation) {
												SOAP12Operation soapOp = (SOAP12Operation) extEl;
												actionURL = soapOp.getSoapActionURI();
											}
											if (actionURL != null) {
												PortType portType = binding.getPortType();
												String ns = portType.getQName().getNamespaceURI();

												QName bindingQName = binding.getQName();
												Map<?, ?> services = wsdlDef.getAllServices();
												Iterator<?> iter4 = services.entrySet().iterator();
												while (iter4.hasNext()) {
													Entry<?, ?> serviceEntry = (Entry<?, ?>) iter4.next();
													Service service = (Service) serviceEntry.getValue();
													Iterator<?> iter5 = service.getPorts().entrySet().iterator();
													while (iter5.hasNext()) {
														Entry<?, ?> portEntry = (Entry<?, ?>) iter5.next();
														Port port = (Port) portEntry.getValue();
														if (port.getBinding().getQName().equals(bindingQName)) {
															String serviceName = service.getQName().getLocalPart();
															String portName = port.getName();
															nsArray = new String[] { ns, serviceName, portName };
															if (actionURL != null) {
																history.setAction(actionURL);
															}
															if (nsArray != null) {
																history.setNsMessage(nsArray);
																return true;
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					} catch (MalformedURLException e) {
						e.printStackTrace();
					} catch (WSDLException e) {
						e.printStackTrace();
					}
				}
			}
		}
		final WSDLBrowseDialog wbDialog = new WSDLBrowseDialog(view.getSite().getShell());
		if (url.length() > 0) {
			wbDialog.setURLText(url);
		}
		if (opName != null) {
			wbDialog.setInitialOperationTextValue(opName);
		}
		WSDLDialogRunnable runnable = new WSDLDialogRunnable(wbDialog);
		Display.getDefault().syncExec(runnable);
		if (runnable.getResult() == Window.OK) {
			TestEntry entry = view.getCurrentEntry();
			entry.setUrl(wbDialog.getWSDLText());
			JAXWSTestEntry jaxwsEntry = getWSEntry(entry);

			Definition wsdlDef = wbDialog.getWSDLDefinition();
			String output = WiseUtil.getSampleSOAPInputMessageFromWISE(monitor, wsdlDef, wbDialog.getServiceTextValue(),
					wbDialog.getPortTextValue(), wbDialog.getBindingValue(), wbDialog.getOperationTextValue());
			String actionURL = TesterWSDLUtils.getActionURL(wsdlDef, wbDialog.getServiceTextValue(),
					wbDialog.getPortTextValue(), wbDialog.getBindingValue(), wbDialog.getOperationTextValue());
			String[] serviceNSMessage = TesterWSDLUtils.getNSServiceNameAndMessageNameArray(wsdlDef,
					wbDialog.getServiceTextValue(), wbDialog.getPortTextValue(), wbDialog.getBindingValue(),
					wbDialog.getOperationTextValue());
			boolean isSOAP12 = TesterWSDLUtils.isSOAP12(wsdlDef, wbDialog.getServiceTextValue(),
					wbDialog.getPortTextValue());
			String headerText = SchemaUtils.getSampleSOAPMessageHeader(wsdlDef, wbDialog.getServiceTextValue(),
					wbDialog.getPortTextValue(), wbDialog.getBindingValue(), wbDialog.getOperationTextValue());

			String soapIn = generateSampleSOAP(headerText, output, isSOAP12);
			String opNameInBody = getOpNameFromRequestBody();
			if (opNameInBody != null && !opNameInBody.equals(wbDialog.getOperationTextValue())) {
				QuestionDialogRunnable questionDialogRunnable = new QuestionDialogRunnable();
				Display.getDefault().syncExec(questionDialogRunnable);
				if (!questionDialogRunnable.getResult()) {
					return false;
				}
			}

			jaxwsEntry.setWsdlDefintion(wsdlDef);
			jaxwsEntry.setServiceName(wbDialog.getServiceTextValue());
			jaxwsEntry.setPortName(wbDialog.getPortTextValue());
			jaxwsEntry.setBindingName(wbDialog.getBindingValue());
			jaxwsEntry.setOperation(wbDialog.getOperationTextValue());
			entry.setUrl(wbDialog.getWSDLText());

			jaxwsEntry.setNsMessage(serviceNSMessage);
			jaxwsEntry.setSoap12(isSOAP12);

			jaxwsEntry.setBody(soapIn);
			jaxwsEntry.setAction(actionURL);
			updateView(wbDialog.getWSDLText(), soapIn);
		}
		return true;
	}

	private String getOpNameFromRequestBody() {
		MessageFactory factory = new MessageFactoryImpl();
		String lookForOpName = null;
		String requestBody = getWSEntry(view.getCurrentEntry()).getBody();
		if (requestBody == null) {
			return null;
		} else {
			try {
				SOAPMessage message = factory.createMessage(null,
						new ByteArrayInputStream(requestBody.trim().getBytes()));
				SOAPBody body = message.getSOAPBody();
				Iterator<?> elements = body.getChildElements();
				if (elements.hasNext()) {
					Element element = (Element) elements.next();
					lookForOpName = element.getNodeName();
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (SOAPException e) {
				e.printStackTrace();
			}
		}
		return lookForOpName;
	}

	@Override
	public void setWebServicesView(WebServicesTestView view) {
		this.view = view;
	}

	class WSDLDialogRunnable implements Runnable {

		private WSDLBrowseDialog wbDialog;
		private int result;

		public WSDLDialogRunnable(WSDLBrowseDialog wbDialog) {
			this.wbDialog = wbDialog;
		}

		@Override
		public void run() {
			result = wbDialog.open();
		}

		public int getResult() {
			return result;
		}

	}

	class QuestionDialogRunnable implements Runnable {

		private boolean result;

		@Override
		public void run() {
			result = MessageDialog.openQuestion(view.getSite().getShell(),
					JBossJAXWSUIMessages.JAXRSWSTestView2_Title_Msg_May_Be_Out_of_Date,
					JBossJAXWSUIMessages.JAXRSWSTestView2_Text_Msg_May_Be_Out_of_Date);

		}

		public boolean getResult() {
			return result;
		}

	}

	private void updateView(final String wsdlURL, final String requestBody) {
		Display.getDefault().syncExec(new Runnable() {

			@Override
			public void run() {
				view.setURL(wsdlURL);
				body.setBodyText(requestBody);

			}
		});
	}

	@Override
	public void updateControlsForSelectedEntry(TestEntry entry) {
		body.setBodyText(getWSEntry(entry).getBody());
	}

	public JAXWSTestEntry getWSEntry(TestEntry entry) {
		CustomTestEntry customEntry = entry.getCustomEntry();
		if (!(customEntry instanceof JAXWSTestEntry)) {
			customEntry = new JAXWSTestEntry();
			entry.setCustomEntry(customEntry);
		}
		return (JAXWSTestEntry) customEntry;
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
}
