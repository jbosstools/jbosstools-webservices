/******************************************************************************* 
 * Copyright (c) 2011-2012 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.ws.ui.views;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
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
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.apache.axis.soap.MessageFactoryImpl;
import org.apache.axis.utils.XMLUtils;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.CoolItem;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.ScrolledPageBook;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.ViewPart;
import org.jboss.tools.ws.ui.JBossWSUIPlugin;
import org.jboss.tools.ws.ui.dialogs.WSTesterURLInputsDialog;
import org.jboss.tools.ws.ui.messages.JBossWSUIMessages;
import org.jboss.tools.ws.ui.utils.JAXRSTester;
import org.jboss.tools.ws.ui.utils.JAXWSTester2;
import org.jboss.tools.ws.ui.utils.ResultsXMLStorage;
import org.jboss.tools.ws.ui.utils.ResultsXMLStorageInput;
import org.jboss.tools.ws.ui.utils.SOAPDOMParser;
import org.jboss.tools.ws.ui.utils.SchemaUtils;
import org.jboss.tools.ws.ui.utils.TesterWSDLUtils;
import org.jboss.tools.ws.ui.utils.TreeParent;
import org.jboss.tools.ws.ui.utils.WSTestUtils;
import org.jboss.wise.ui.internal.util.WiseUtil;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * View for testing web services (JAX-WS & JAX-RS)
 * @author bfitzpat
 *
 */
public class JAXRSWSTestView2 extends ViewPart {

	private static final String PAGE1_KEY = "page1"; //$NON-NLS-1$
	private static final String PAGE2_KEY = "page2"; //$NON-NLS-1$
	private static final String DEFAULT_TEXT_EDITOR_ID = "org.eclipse.ui.DefaultTextEditor"; //$NON-NLS-1$
	private static final String XML_EDITOR_ID = "org.eclipse.wst.xml.ui.internal.tabletree.XMLMultiPageEditorPart"; //$NON-NLS-1$
	private static final String DELETE = "DELETE";//$NON-NLS-1$
	private static final String PUT = "PUT";//$NON-NLS-1$
	private static final String POST = "POST";//$NON-NLS-1$
	private static final String GET = "GET";//$NON-NLS-1$
	private static final String OPTIONS = "OPTIONS";//$NON-NLS-1$
	private static final String JAX_WS = "JAX-WS"; //$NON-NLS-1$
	private static final String JAX_RS = "JAX-RS"; //$NON-NLS-1$
	private static final String EMPTY_STRING = ""; //$NON-NLS-1$
	private static final String RESULT_HEADER_DELIMITER = "%";//$NON-NLS-1$
	private static final String HTTPS_STRING = "https";//$NON-NLS-1$
	private static final String[] TREE_COLUMNS = new String[] { "name", "value" }; //$NON-NLS-1$ //$NON-NLS-2$
	
	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "org.jboss.tools.ws.ui.tester.views.TestWSView";//$NON-NLS-1$

	/* UI controls */
	private Text resultsText;
	private Browser resultsBrowser;
	private Combo urlCombo;
	private DelimitedStringList dlsList;
	private Combo methodCombo;
	private Text bodyText;
	private List resultHeadersList;

	private TreeViewer treeRequestBody;
	private ScrolledPageBook requestPageBook;
	private ShowInTreeAction treeAction;
	private ShowRawRequestAction rawRequestAction;

	private DelimitedStringList parmsList;

	private SOAPEnvelope envelope;
	private SOAPBody soapbody;
	private MenuItem openInXMLEditorAction;
	private MenuItem openResponseTagInXMLEditor;
	private Menu resultsTextMenu;
	private MenuItem copyMenuAction;
	private Menu resultsHeaderMenu;
	private MenuItem copyResultHeaderMenuAction;

	private String[] serviceNSMessage = null;
//	private String actionText = null;

	private FormToolkit toolkit;
	private ScrolledForm form;
	private ImageRegistry mImageRegistry;

	private static final String IMG_DESC_WSDL = "icons/obj16/wsdl.gif"; //$NON-NLS-1$
	private static final String IMG_DESC_START = "icons/obj16/run.gif"; //$NON-NLS-1$

	private static final String IMG_DESC_SHOWRAW = "icons/obj16/binary.gif"; //$NON-NLS-1$
	private static final String IMG_DESC_SHOWTREE = "icons/obj16/hierarchicalLayout.gif"; //$NON-NLS-1$
	private static final String IMG_DESC_SHOWWEB = "icons/obj16/web.gif"; //$NON-NLS-1$
	private static final String IMG_DESC_SHOWEDITOR = "icons/obj16/properties.gif"; //$NON-NLS-1$
	private static final String IMG_DESC_SAVE = "icons/obj16/save_edit.gif"; //$NON-NLS-1$

	private ToolItem openWSDLToolItem;
	private ToolItem startToolItem;
	private ScrolledPageBook pageBook;
	private ShowRawAction rawAction;
	private ShowInBrowserAction browserAction;
	
	private TestHistory history = new TestHistory();
	private TestHistoryEntry currentHistoryEntry = null;
	private Button useBasicAuthCB;
	private boolean restoringFromHistoryEntry = false;

	/**
	 * The constructor.
	 */
	public JAXRSWSTestView2() {
	}
	
	public void setWSDLURL( String url ) {
		this.urlCombo.setText(url);
		this.methodCombo.setText(JAX_WS);
		setControlsForWSType(JAX_WS);
		setControlsForMethodType(methodCombo.getText());
		setControlsForSelectedURL();
	}
	
	public void setJAXRS ( String url, String method ) {
		this.urlCombo.setText(url);
		this.bodyText.setText(EMPTY_STRING);
		this.treeRequestBody.setInput(null);
		this.resultsText.setText(EMPTY_STRING);
		this.resultsBrowser.setText(EMPTY_STRING);
//		getCurrentHistoryEntry().setUrl(url);
		String uCaseMethod = method.toUpperCase();
		if (uCaseMethod.equalsIgnoreCase(GET))
			this.methodCombo.setText(GET);
		else if (uCaseMethod.equalsIgnoreCase(POST))
			this.methodCombo.setText(POST);
		else if (uCaseMethod.equalsIgnoreCase(PUT))
			this.methodCombo.setText(PUT);
		else if (uCaseMethod.equalsIgnoreCase(DELETE))
			this.methodCombo.setText(DELETE);
		else if (uCaseMethod.equalsIgnoreCase(OPTIONS))
			this.methodCombo.setText(OPTIONS);
		getCurrentHistoryEntry().setMethod(methodCombo.getText());
		setControlsForWSType(JAX_RS);
		setControlsForMethodType(methodCombo.getText());
		setControlsForSelectedURL();
	}

	private void getImages() {
		mImageRegistry = new ImageRegistry();
		mImageRegistry.put(IMG_DESC_WSDL, ImageDescriptor
				.createFromURL(JBossWSUIPlugin.getDefault().getBundle()
						.getEntry(IMG_DESC_WSDL)));
		mImageRegistry.put(IMG_DESC_START, ImageDescriptor
				.createFromURL(JBossWSUIPlugin.getDefault().getBundle()
						.getEntry(IMG_DESC_START)));
		mImageRegistry.put(IMG_DESC_SHOWRAW, ImageDescriptor
				.createFromURL(JBossWSUIPlugin.getDefault().getBundle()
						.getEntry(IMG_DESC_SHOWRAW)));
		mImageRegistry.put(IMG_DESC_SHOWWEB, ImageDescriptor
				.createFromURL(JBossWSUIPlugin.getDefault().getBundle()
						.getEntry(IMG_DESC_SHOWWEB)));
		mImageRegistry.put(IMG_DESC_SHOWEDITOR, ImageDescriptor
				.createFromURL(JBossWSUIPlugin.getDefault().getBundle()
						.getEntry(IMG_DESC_SHOWEDITOR)));
		mImageRegistry.put(IMG_DESC_SAVE, ImageDescriptor
				.createFromURL(JBossWSUIPlugin.getDefault().getBundle()
						.getEntry(IMG_DESC_SAVE)));
		mImageRegistry.put(IMG_DESC_SHOWTREE, ImageDescriptor
				.createFromURL(JBossWSUIPlugin.getDefault().getBundle()
						.getEntry(IMG_DESC_SHOWTREE)));
	}

	private void createRequestToolbar ( ExpandableComposite parent ) {

		// create a couple of actions for toggling views
		rawRequestAction = new ShowRawRequestAction();
		rawRequestAction.setChecked(true);
		treeAction = new ShowInTreeAction();

		ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
		ToolBar toolbar = toolBarManager.createControl(parent);

		toolBarManager.add(rawRequestAction);
		toolBarManager.add(treeAction);

		toolBarManager.update(true);

		parent.setTextClient(toolbar);
	}
	
	private void createResponseToolbar ( ExpandableComposite parent ) {

		// create a couple of actions for toggling views
		rawAction = new ShowRawAction();
		rawAction.setChecked(true);
		browserAction = new ShowInBrowserAction();

		ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
		ToolBar toolbar = toolBarManager.createControl(parent);

		toolBarManager.add(new FileSaveAction());
		toolBarManager.add(new OpenInXMLEditorAction());
		toolBarManager.add(rawAction);
		toolBarManager.add(browserAction);

		toolBarManager.update(true);

		parent.setTextClient(toolbar);
	}

	class FormExpansionAdapter extends ExpansionAdapter {
		public void expansionStateChanged(ExpansionEvent e) {
			form.setRedraw(false);
			form.reflow(true);
			form.layout(true, true);
			form.setRedraw(true);
		}
	}

	class OpenInXMLEditorAction extends Action {
		@Override
		public void run() {
			openXMLEditor(resultsText.getText());
		}
		@Override
		public String getToolTipText() {
			return JBossWSUIMessages.JAXRSWSTestView2_OpenInEditor_Action;
		}
		@Override
		public ImageDescriptor getImageDescriptor() {
			return mImageRegistry.getDescriptor(IMG_DESC_SHOWEDITOR);
		}
	}

	class FileSaveAction extends Action {
		@Override
		public void run() {
			IStatus status =
				WSTestUtils.saveTextToFile(resultsText.getText());
			if (status.getCode() == IStatus.ERROR) {
				MessageDialog.openError(new Shell(Display.getCurrent()),
						JBossWSUIMessages.JAXRSWSTestView2_SaveResponseText_Error,
						status.getMessage());
			}
		}
		@Override
		public String getToolTipText() {
			return JBossWSUIMessages.JAXRSWSTestView2_SaveResponseText_tooltip;
		}
		@Override
		public ImageDescriptor getImageDescriptor() {
			return mImageRegistry.getDescriptor(IMG_DESC_SAVE);
		}
	}

	class ToggleAction extends Action {
		public ToggleAction ( ) {
			super(null, IAction.AS_CHECK_BOX);
		}
	}

	class ShowInBrowserAction extends ToggleAction {
		public void run() {
			if (rawAction.isChecked()) rawAction.setChecked(false);
			if (JAXRSWSTestView2.this.resultsText.getText().length() > 0 ) {
				JAXRSWSTestView2.this.resultsBrowser.setText
				(JAXRSWSTestView2.this.resultsText.getText());
			}
			JAXRSWSTestView2.this.pageBook.showPage(PAGE2_KEY);
		}
		@Override
		public String getToolTipText() {
			return JBossWSUIMessages.JAXRSWSTestView2_ShowInBrowser_Tooltip;
		}
		@Override
		public ImageDescriptor getImageDescriptor() {
			return mImageRegistry.getDescriptor(IMG_DESC_SHOWWEB);
		}
	}

	class ShowRawAction extends ToggleAction {
		public void run() {
			if (browserAction.isChecked()) browserAction.setChecked(false);
			JAXRSWSTestView2.this.pageBook.showPage(PAGE1_KEY);
		}
		@Override
		public String getToolTipText() {
			return JBossWSUIMessages.JAXRSWSTestView2_ShowRaw_Tooltip;
		}
		@Override
		public ImageDescriptor getImageDescriptor() {
			return mImageRegistry.getDescriptor(IMG_DESC_SHOWRAW);
		}
	}

	class ShowRawRequestAction extends ToggleAction {
		public void run() {
			if (treeAction.isChecked()) treeAction.setChecked(false);
			
			JAXRSWSTestView2.this.requestPageBook.showPage(PAGE1_KEY);
		}
		@Override
		public String getToolTipText() {
			return JBossWSUIMessages.JAXRSWSTestView2_ShowRequestXML_toolbar_btn;
		}
		@Override
		public ImageDescriptor getImageDescriptor() {
			return mImageRegistry.getDescriptor(IMG_DESC_SHOWRAW);
		}
	}

	class ShowInTreeAction extends ToggleAction {
		public void run() {
			if (rawRequestAction.isChecked()) rawRequestAction.setChecked(false);
			String rawRequestBody = JAXRSWSTestView2.this.bodyText.getText();

            if (rawRequestBody.trim().length() > 0 ) {
				JAXRSWSTestView2.this.treeRequestBody.setInput(rawRequestBody);
			} else {
				JAXRSWSTestView2.this.treeRequestBody.setInput(null);
			}
			JAXRSWSTestView2.this.requestPageBook.showPage(PAGE2_KEY);
		}
		@Override
		public String getToolTipText() {
			return JBossWSUIMessages.JAXRSWSTestView2_ShowRequestTree_toolbar_btn;
		}
		@Override
		public ImageDescriptor getImageDescriptor() {
			return mImageRegistry.getDescriptor(IMG_DESC_SHOWTREE);
		}
	}

	private String getCurrentTestType() {
		if (methodCombo.getText().equalsIgnoreCase(JAX_WS))
			return JAX_WS;
		return JAX_RS;
	}
	
	private boolean getWSDLSpecifics( String opName ) {
		
		if (opName != null) {
			String opNameInBody = getOpNameFromRequestBody();
			if (opNameInBody != null) {
				boolean isRequestSOAP12 = 
					TesterWSDLUtils.isRequestBodySOAP12(getCurrentHistoryEntry().getBody());
				String urlText = urlCombo.getText();
				if (urlText != null) {
					try {
						URL tempURL = new URL(urlText);
						Definition 	wsdlDef =
							TesterWSDLUtils.readWSDLURL(tempURL);
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
								if (bindOp.getName().contentEquals(opNameInBody) || bindOp.getName().contentEquals(tempOpName)) {
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
												Map<?,?> services = wsdlDef.getAllServices();
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
															nsArray =  new String[] {ns, serviceName, portName};
                                                            if (actionURL != null) {
                                                                getCurrentHistoryEntry().setAction(actionURL);
                                                            }
															if (nsArray != null) {
																getCurrentHistoryEntry().setServiceNSMessage(nsArray);
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
		
		WSDLBrowseDialog wbDialog =  new WSDLBrowseDialog(getSite().getShell());
		if (urlCombo.getText().length() > 0) {
			wbDialog.setURLText(urlCombo.getText());
		}
		if (opName != null) {
			wbDialog.setInitialOperationTextValue(opName);
		}
		int rtnCode = wbDialog.open();
		if (rtnCode == Window.OK){
			
			getCurrentHistoryEntry().setServiceNSMessage(null);
			getCurrentHistoryEntry().setAction(""); //$NON-NLS-1$
			getCurrentHistoryEntry().setWsdlDef(null);
			getCurrentHistoryEntry().setServiceName(null);
			getCurrentHistoryEntry().setPortName(null);
			getCurrentHistoryEntry().setBindingName(null);
			getCurrentHistoryEntry().setOperationName(null);

			serviceNSMessage = null;
			
			Definition wsdlDef = wbDialog.getWSDLDefinition();
			getCurrentHistoryEntry().setWsdlDef(wsdlDef);
			getCurrentHistoryEntry().setServiceName(wbDialog.getServiceTextValue());
			getCurrentHistoryEntry().setPortName(wbDialog.getPortTextValue());
			getCurrentHistoryEntry().setBindingName(wbDialog.getBindingValue());
			getCurrentHistoryEntry().setOperationName(wbDialog.getOperationTextValue());
			getCurrentHistoryEntry().setUrl(wbDialog.getWSDLText());
			urlCombo.setText(wbDialog.getWSDLText());
			
			String output = WiseUtil.getSampleSOAPInputMessageFromWISE(wsdlDef, 
					wbDialog.getServiceTextValue(), 
					wbDialog.getPortTextValue(), 
					wbDialog.getBindingValue(), 
					wbDialog.getOperationTextValue());

			String endpointURL = TesterWSDLUtils.getEndpointURL(wsdlDef, 
					wbDialog.getServiceTextValue(), 
					wbDialog.getPortTextValue(), 
					wbDialog.getBindingValue(), 
					wbDialog.getOperationTextValue());
			getCurrentHistoryEntry().setUrl(endpointURL);
			
			String actionURL = TesterWSDLUtils.getActionURL(wsdlDef, 
					wbDialog.getServiceTextValue(), 
					wbDialog.getPortTextValue(), 
					wbDialog.getBindingValue(), 
					wbDialog.getOperationTextValue());
			getCurrentHistoryEntry().setAction(actionURL);
			
			serviceNSMessage = TesterWSDLUtils.getNSServiceNameAndMessageNameArray(wsdlDef, 
					wbDialog.getServiceTextValue(), 
					wbDialog.getPortTextValue(), 
					wbDialog.getBindingValue(), 
					wbDialog.getOperationTextValue());
			getCurrentHistoryEntry().setServiceNSMessage(serviceNSMessage);
			
			boolean isSOAP12 = TesterWSDLUtils.isSOAP12(wsdlDef, 
					wbDialog.getServiceTextValue(), 
					wbDialog.getPortTextValue());
			getCurrentHistoryEntry().setSOAP12(isSOAP12);
			
			String headerText = SchemaUtils.getSampleSOAPMessageHeader(wsdlDef, 
					wbDialog.getServiceTextValue(), 
					wbDialog.getPortTextValue(), 
					wbDialog.getBindingValue(), 
					wbDialog.getOperationTextValue());

			String soapIn = generateSampleSOAP(headerText, output, isSOAP12);
			if (opName != null) {
				if (bodyText.getText().length() > 0) {
					
					String opNameInBody = getOpNameFromRequestBody();
					if (opNameInBody == null) {
						bodyText.setText(soapIn);
						treeRequestBody.setInput(soapIn);
						getCurrentHistoryEntry().setBody(soapIn);
						getCurrentHistoryEntry().setAction(actionURL);
					} else if (opNameInBody.contentEquals(getCurrentHistoryEntry().getOperationName())) {
						// ignore
					} else {
						if (MessageDialog.openQuestion(getSite().getShell(),
								JBossWSUIMessages.JAXRSWSTestView2_Title_Msg_May_Be_Out_of_Date, 
								JBossWSUIMessages.JAXRSWSTestView2_Text_Msg_May_Be_Out_of_Date)) {
								
									bodyText.setText(soapIn);
									treeRequestBody.setInput(soapIn);
									getCurrentHistoryEntry().setBody(soapIn);
									getCurrentHistoryEntry().setAction(actionURL);
									
							}
					}
				}
			} else if (bodyText.getText().length() > 0) {
				
				String opNameInBody = getOpNameFromRequestBody();
				boolean isRequestSOAP12 = TesterWSDLUtils.isRequestBodySOAP12(getCurrentHistoryEntry().getBody());
				
				if (opNameInBody == null || isSOAP12 != isRequestSOAP12 ) {

		            bodyText.setText(soapIn);
					treeRequestBody.setInput(soapIn);
					getCurrentHistoryEntry().setBody(soapIn);
					getCurrentHistoryEntry().setAction(actionURL);
				} else if (opNameInBody.contentEquals(getCurrentHistoryEntry().getOperationName())) {
					// ignore
				} else {
					if (MessageDialog.openQuestion(getSite().getShell(),
							JBossWSUIMessages.JAXRSWSTestView2_Title_Msg_May_Be_Out_of_Date, 
							JBossWSUIMessages.JAXRSWSTestView2_Text_Msg_May_Be_Out_of_Date)) {
							
								bodyText.setText(soapIn);
			                    treeRequestBody.setInput(soapIn);
			                    
								getCurrentHistoryEntry().setBody(soapIn);
								getCurrentHistoryEntry().setAction(actionURL);
								
						}
				}
			}
			
			setControlsForWSType(getCurrentTestType());
			setControlsForMethodType(methodCombo.getText());
			setControlsForSelectedURL();
			return true;
		}
		return false;
	}

	private void createURLAndToolbar( ) {
		urlCombo = new Combo(form.getBody(), SWT.BORDER | SWT.DROP_DOWN);
		GridData gdURL = new GridData(SWT.FILL, SWT.NONE, true, false);
		urlCombo.setLayoutData(gdURL);
		toolkit.adapt(urlCombo);

		urlCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (e.getSource().equals(urlCombo)) {
					String newURL = urlCombo.getText();
					String oldURL = null;
					if (getCurrentHistoryEntry() != null && getCurrentHistoryEntry().getUrl() != null && getCurrentHistoryEntry().getUrl().trim().length() > 0) {
						oldURL = getCurrentHistoryEntry().getUrl();
					}
					if (newURL != null && oldURL != null && newURL.contentEquals(oldURL)) {
						// ignore it - they're the same
					} else if (newURL != null && oldURL == null) {
						setControlsForSelectedURL();
						getCurrentHistoryEntry().setUrl(newURL);
						getCurrentHistoryEntry().setAction(null);
					}
				}
			}
		});
		urlCombo.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
			}

			public void keyReleased(KeyEvent e) {
				setControlsForSelectedURL();
				getCurrentHistoryEntry().setUrl(urlCombo.getText());
				getCurrentHistoryEntry().setAction(null);
				if (e.keyCode == SWT.CR) {
					handleTest(getCurrentTestType());
				}
			}
		});
		urlCombo.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
			public void widgetSelected(SelectionEvent e) {
				String testURL = urlCombo.getText();
				TestHistoryEntry entry = history.findEntryByURL(testURL);
//				System.out.println(new Timestamp(System.currentTimeMillis()));
//				System.out.println(history.toString());
				if (entry != null) {
					try {
						currentHistoryEntry = (TestHistoryEntry) entry.clone();
//						currentHistoryEntry = entry;
//						getCurrentHistoryEntry().setAction(null);
						setControlsForSelectedEntry(entry);
					} catch (CloneNotSupportedException e1) {
						e1.printStackTrace();
					}
				} else {
					getCurrentHistoryEntry().setUrl(urlCombo.getText());
					getCurrentHistoryEntry().setAction(null);
					setControlsForSelectedURL();
				}
			}
		});

		Composite comp1 = toolkit.createComposite(form.getBody());
		comp1.setLayoutData(new GridData(SWT.END, SWT.NONE, false, false));
		comp1.setLayout(new FillLayout());

		CoolBar coolBar = new CoolBar(comp1, SWT.FLAT);
		coolBar.setLocked(true);
		coolBar.setBackground(form.getBody().getBackground());

		CoolItem wsMethodCoolItem = new CoolItem(coolBar, SWT.PUSH | SWT.FLAT);
		methodCombo = new Combo(coolBar, SWT.BORDER | SWT.READ_ONLY);
		methodCombo.setBackground(form.getBody().getBackground());
		String[] methods = {JAX_WS, GET, POST, PUT, DELETE, OPTIONS};
		methodCombo.setItems(methods);
		methodCombo.pack();
		Point size = methodCombo.computeSize (SWT.DEFAULT, SWT.DEFAULT);
		wsMethodCoolItem.setPreferredSize (wsMethodCoolItem.computeSize (size.x, size.y));
		wsMethodCoolItem.setControl(methodCombo);
		methodCombo.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
			public void widgetSelected(SelectionEvent e) {
				getCurrentHistoryEntry().setMethod(methodCombo.getText());
				setControlsForWSType(getCurrentTestType());
				setControlsForMethodType(methodCombo.getText());
				setControlsForSelectedURL();
			}
		});
		toolkit.adapt(methodCombo);

		CoolItem topCoolItem = new CoolItem(coolBar, SWT.FLAT);

		ToolBar topToolBar = new ToolBar(coolBar,SWT.HORIZONTAL| SWT.FLAT);
		topToolBar.setBackground(form.getBody().getBackground());
		openWSDLToolItem = new ToolItem(topToolBar, SWT.PUSH);
		openWSDLToolItem.setImage(mImageRegistry.get(IMG_DESC_WSDL));
		openWSDLToolItem.setToolTipText(JBossWSUIMessages.JAXRSWSTestView2_GetFromWSDL_Tooltip);
		openWSDLToolItem.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				getWSDLSpecifics(null);
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});

		startToolItem = new ToolItem(topToolBar, SWT.PUSH| SWT.FLAT);
		startToolItem.setImage(mImageRegistry.get(IMG_DESC_START));
		startToolItem.setToolTipText(JBossWSUIMessages.JAXRSWSTestView2_Go_Tooltip);
		startToolItem.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				handleTest(getCurrentTestType());
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});

		topToolBar.pack(); 
		size = topToolBar.getSize();
		topCoolItem.setControl(topToolBar);
		topCoolItem.setSize(topCoolItem.computeSize(size.x, size.y));
		toolkit.adapt(coolBar);
	}

	private void createRequestSide( SashForm sashForm ) {
		Section section = toolkit.createSection(sashForm, 
				Section.TITLE_BAR|
				Section.TWISTIE|Section.EXPANDED);
		section.setText(JBossWSUIMessages.JAXRSWSTestView2_RequestDetails_Section);

		Composite sectionClient = toolkit.createComposite(section);
		sectionClient.setLayout(new GridLayout());
		sectionClient.setLayoutData(new GridData());

		useBasicAuthCB = toolkit.createButton(sectionClient, 
				JBossWSUIMessages.JAXRSWSTestView2_Checkbox_Basic_Authentication, SWT.CHECK);
		GridData gd10 = new GridData(SWT.FILL, SWT.NONE, true, false);
		gd10.horizontalIndent = 3;
		useBasicAuthCB.setLayoutData(gd10);

		ExpandableComposite ec = toolkit.createExpandableComposite(sectionClient, 
				ExpandableComposite.TREE_NODE| ExpandableComposite.TITLE_BAR |
				ExpandableComposite.CLIENT_INDENT);
		ec.setText(JBossWSUIMessages.JAXRSWSTestView2_Headers_Section);
		dlsList = new DelimitedStringList(ec, SWT.None, false, false);
		dlsList.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e) {
				getCurrentHistoryEntry().setHeaders(dlsList.getSelection());
				getCurrentHistoryEntry().setAction(null);
			}
		});
		ec.setClient(dlsList);
		toolkit.adapt(dlsList);
		GridData gd2 = new GridData(SWT.FILL, SWT.FILL, true, false);
		ec.setLayoutData(gd2);
		ec.addExpansionListener(new FormExpansionAdapter());

		ExpandableComposite ec3 = toolkit.createExpandableComposite(sectionClient, 
				ExpandableComposite.TREE_NODE| ExpandableComposite.TITLE_BAR |
				ExpandableComposite.CLIENT_INDENT);
		ec3.setText(JBossWSUIMessages.JAXRSWSTestView2_Parameters_Section);
		parmsList = new DelimitedStringList(ec3, SWT.None, false, false);
		parmsList.setShowUpDown(false);
		parmsList.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e) {
				getCurrentHistoryEntry().setParms(parmsList.getSelection());
				getCurrentHistoryEntry().setAction(null);
			}
		});
		ec3.setClient(parmsList);
		toolkit.adapt(parmsList);
		GridData gd4 = new GridData(SWT.FILL, SWT.FILL, true, false);
		ec3.setLayoutData(gd4);
		ec3.addExpansionListener(new FormExpansionAdapter());

		ExpandableComposite ec5 = toolkit.createExpandableComposite(sectionClient, 
				ExpandableComposite.TWISTIE|
				ExpandableComposite.CLIENT_INDENT |
				ExpandableComposite.EXPANDED);
		ec5.setText(JBossWSUIMessages.JAXRSWSTestView2_BodyText_Section);
		requestPageBook = toolkit.createPageBook(ec5, SWT.NONE);

		createRequestToolbar(ec5);

		Composite page1 = requestPageBook.createPage(PAGE1_KEY);
		page1.setLayout(new GridLayout());
		bodyText = toolkit.createText(page1, EMPTY_STRING, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		GridData gd7 = new GridData(SWT.FILL, SWT.FILL, true, true);
		//		gd7.minimumHeight = 100;
		gd7.heightHint = 1;
		bodyText.setLayoutData(gd7);

		requestPageBook.showPage(PAGE1_KEY);

		Composite page2 = requestPageBook.createPage(PAGE2_KEY);
		page2.setLayout(new GridLayout());
		treeRequestBody = new TreeViewer(page2, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.FULL_SELECTION );
		JAXRSWSTestView2.this.treeRequestBody.setAutoExpandLevel(TreeViewer.ALL_LEVELS);
		GridData gd11 = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd11.heightHint = 1;
		//		gd10.minimumHeight = 100;
		toolkit.adapt(treeRequestBody.getTree());
		treeRequestBody.getTree().setLayoutData(gd11);
		treeRequestBody.getTree().setHeaderVisible(true);
		TreeColumn nameColumn = new TreeColumn(treeRequestBody.getTree(), SWT.LEFT);
		nameColumn.setText(JBossWSUIMessages.JAXRSWSTestView2_Name_column);
		nameColumn.setWidth(200);
		TreeColumn valueColumn = new TreeColumn(treeRequestBody.getTree(), SWT.LEFT);
		valueColumn.setText(JBossWSUIMessages.JAXRSWSTestView2_Value_column);
		valueColumn.setWidth(200);
		
		treeRequestBody.setColumnProperties(TREE_COLUMNS);
		
		treeRequestBody.setLabelProvider(new ITableLabelProvider() {

			@Override
			public void addListener(ILabelProviderListener listener) {
			}

			@Override
			public void dispose() {
			}

			@Override
			public boolean isLabelProperty(Object element, String property) {
				if (element instanceof TreeParent && property.equalsIgnoreCase("name")) { //$NON-NLS-1$
					return true;
				} else if (element instanceof TreeParent && property.equalsIgnoreCase("value")) { //$NON-NLS-1$
					return true;
				}
				return false;
			}

			@Override
			public void removeListener(ILabelProviderListener listener) {
			}

			@Override
			public Image getColumnImage(Object element, int columnIndex) {
				return null;
			}

			@Override
			public String getColumnText(Object element, int columnIndex) {
				if (element instanceof TreeParent && columnIndex == 0) {
					return ((TreeParent)element).getName();
				} else if (element instanceof TreeParent && columnIndex == 1) {
					TreeParent tp = (TreeParent) element;
					if (tp.getData() != null && tp.getData() instanceof Element) {
						Element tpelement = (Element) tp.getData();
						if (tpelement.getChildNodes() != null && tpelement.getChildNodes().getLength() > 0) {
							Node node = tpelement.getChildNodes().item(0);
							if (node.getNodeType() == Node.TEXT_NODE) {
								return node.getTextContent();
							}
						}
					}
				}
				return null;
			}
		});
		
		treeRequestBody.setContentProvider(new ITreeContentProvider(){
			
			String text;
			TreeParent tree;

			@Override
			public void dispose() {
			}

			@Override
			public void inputChanged(Viewer viewer, Object oldInput,
					Object newInput) {
				if (newInput instanceof String) {
					text = (String) newInput;
					
		            SOAPDOMParser parser = new SOAPDOMParser();
					parser.parseXmlFile(text);
					if (parser.getRoot().getChildren().length > 0)
						tree = (TreeParent) parser.getRoot().getChildren()[0];
					else 
						tree = null;
				}
			}

			@Override
			public Object[] getElements(Object inputElement) {
				if (inputElement instanceof String && tree != null) {
					return new Object[] {this.tree};
				} else if (inputElement instanceof TreeParent) {
					return ((TreeParent)inputElement).getChildren();
				}
				return null;
			}

			@Override
			public Object[] getChildren(Object parentElement) {
				if (parentElement == null && tree != null) {
					return new Object[] {this.tree};
				} else if (parentElement instanceof TreeParent && ((TreeParent)parentElement).hasChildren()) {
					return ((TreeParent)parentElement).getChildren();
				}
				return null;
			}

			@Override
			public Object getParent(Object element) {
				if (element instanceof TreeParent) {
					return ((TreeParent)element).getParent();
				}
				return null;
			}

			@Override
			public boolean hasChildren(Object element) {
				if (element instanceof TreeParent) {
					return ((TreeParent)element).hasChildren();
				}
				return false;
			}
		});
		
		treeRequestBody.setCellModifier(new ICellModifier() {

			/* (non-Javadoc)
			 * @see org.eclipse.jface.viewers.ICellModifier#canModify(java.lang.Object, java.lang.String)
			 */
			public boolean canModify(Object element, String property) {
				if (element instanceof TreeParent && property.equalsIgnoreCase("value")) {//$NON-NLS-1$
					TreeParent tp = (TreeParent) element;
					if (tp.getData() != null && tp.getData() instanceof Element) {
						Element tpelement = (Element) tp.getData();
						if (tpelement.getChildNodes() != null && tpelement.getChildNodes().getLength() > 0) {
							Node node = tpelement.getChildNodes().item(0);
							if (node.getNodeType() == Node.TEXT_NODE && node.getNodeValue().trim().length() > 0) {
								return true;
							}
						}
					}
				}
				return false;
			}

			/* (non-Javadoc)
			 * @see org.eclipse.jface.viewers.ICellModifier#getValue(java.lang.Object, java.lang.String)
			 */
			public Object getValue(Object element, String property) {
				TreeParent tp = (TreeParent) element;
				if (tp.getData() != null && tp.getData() instanceof Element) {
					Element tpelement = (Element) tp.getData();
					if (tpelement.getChildNodes() != null && tpelement.getChildNodes().getLength() > 0) {
						Node node = tpelement.getChildNodes().item(0);
						return node.getTextContent();
					}
				}
				return null;
			}

			/* (non-Javadoc)
			 * @see org.eclipse.jface.viewers.ICellModifier#modify(java.lang.Object, java.lang.String, java.lang.Object)
			 */
			public void modify(Object element, String property, Object value) {
				TreeItem ti = (TreeItem) element;
				TreeParent tp = (TreeParent) ti.getData();
				if (tp.getData() != null && tp.getData() instanceof Element) {
					Element tpelement = (Element) tp.getData();
					if (tpelement.getChildNodes() != null && tpelement.getChildNodes().getLength() > 0) {
						Node node = tpelement.getChildNodes().item(0);
						if (node.getNodeType() == Node.TEXT_NODE) {
							node.setTextContent((String)value);
							treeRequestBody.update(tp, null);
							SOAPDOMParser parser = new SOAPDOMParser();
							String updatedOut =
									parser.updateValue((String) treeRequestBody.getInput(), tp, (String)value);
							if (updatedOut != null && updatedOut.trim().length() > 0) {
								Stack<String> pathStack = new Stack<String>();
								pathStack.push(ti.getText());
								TreeItem tiPath = ti;
								while (tiPath.getParentItem() != null ) {
									tiPath = tiPath.getParentItem();
									pathStack.push(tiPath.getText());
								}
								bodyText.setText(updatedOut);
								getCurrentHistoryEntry().setBody(bodyText.getText());
								treeRequestBody.setInput(updatedOut);
								treeRequestBody.setAutoExpandLevel(TreeViewer.ALL_LEVELS);
								while (!pathStack.isEmpty()) {
									TreeItem[] items = treeRequestBody.getTree().getItems();
									String find = pathStack.pop();
									/*boolean found =*/ findTreeItem(find, items);
								}
							}
						}
					}
				}
			}
			
		});
		treeRequestBody.setCellEditors(new CellEditor[] { null, new TextCellEditor(treeRequestBody.getTree()) });

		requestPageBook.showPage(PAGE1_KEY);

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true); //GridData.FILL_HORIZONTAL);
		gd.heightHint = 1;
		gd.minimumHeight = 100;
		requestPageBook.setLayoutData(gd);

		requestPageBook.showPage(PAGE1_KEY);

		bodyText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (!restoringFromHistoryEntry)
					getCurrentHistoryEntry().setBody(bodyText.getText());
//				getCurrentHistoryEntry().setAction(null);
			}
		});
		bodyText.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
			}

			public void keyReleased(KeyEvent e) {
				getCurrentHistoryEntry().setBody(bodyText.getText());
				if (e.keyCode == SWT.CR && e.stateMask == SWT.CTRL) {
					handleTest(getCurrentTestType());
				}
			}
		});
		ec5.setClient(requestPageBook);
		GridData gd9 = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd9.minimumHeight = 200;
		ec5.setLayoutData(gd9);
		ec5.addExpansionListener(new FormExpansionAdapter());
		
		section.addExpansionListener(new FormExpansionAdapter());
		section.setClient(sectionClient);  	    
	}
	
//	private boolean findString(String searchString, TreeItem[] treeItems) {
//		for (TreeItem treeItem : treeItems) {
//			for (int i = 0; i < tree.getColumnCount(); i++) {
//				String text = treeItem.getText(i);
//				if ((text.toUpperCase().contains(searchString.toUpperCase()))) {
//					tree.setSelection(treeItem);
//					return true;
//				}
//			}
//		}
//
//		return false;
//	}
	
	private boolean findTreeItem ( String name, TreeItem[] treeItems ) {
		for (TreeItem treeItem : treeItems) {
			for (int i = 0; i < treeRequestBody.getTree().getColumnCount(); i++) {
				String text = treeItem.getText(i);
				if ((text.toUpperCase().contains(name.toUpperCase()))) {
					treeRequestBody.getTree().setSelection(treeItem);
					return true;
				}
				if (treeItem.getItemCount() > 0) {
					return findTreeItem (name, treeItem.getItems());
				}
			}
		}
		return false;
	}
	
	private TestHistoryEntry getCurrentHistoryEntry() {
		if (this.currentHistoryEntry == null) {
			this.currentHistoryEntry = new TestHistoryEntry();
		}
		return this.currentHistoryEntry;
	}

	private void createResponseSide ( SashForm sashForm ) {
		Section section2 = toolkit.createSection(sashForm, 
				Section.TITLE_BAR|
				Section.TWISTIE|Section.EXPANDED);
		section2.setText(JBossWSUIMessages.JAXRSWSTestView2_ResponseDetails_Section);

		Composite sectionClient2 = toolkit.createComposite(section2);
		sectionClient2.setLayout(new GridLayout());
		sectionClient2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		ExpandableComposite ec2 = toolkit.createExpandableComposite(sectionClient2, 
				ExpandableComposite.TREE_NODE| ExpandableComposite.TITLE_BAR |
				ExpandableComposite.CLIENT_INDENT );
		ec2.setText(JBossWSUIMessages.JAXRSWSTestView2_ResponseHeaders_Section);
		ec2.setLayout(new GridLayout());
		resultHeadersList = new List(ec2, SWT.V_SCROLL | SWT.BORDER );
		resultHeadersList.add(EMPTY_STRING);
		resultHeadersList.add(EMPTY_STRING);
		resultHeadersList.add(EMPTY_STRING);
		resultHeadersList.add(EMPTY_STRING);
		ec2.setClient(resultHeadersList);
		resultsHeaderMenu = new Menu(resultHeadersList.getShell(), SWT.POP_UP);

		copyResultHeaderMenuAction = new MenuItem(resultsHeaderMenu, SWT.PUSH);
		copyResultHeaderMenuAction.setText(JBossWSUIMessages.JAXRSWSTestView_CopyResultMenu_Text);
		copyResultHeaderMenuAction.setAccelerator(SWT.CTRL + 'C');
		copyResultHeaderMenuAction.addSelectionListener(new SelectionListener(){

			public void widgetDefaultSelected(SelectionEvent arg0) {
				if (resultHeadersList.getSelectionCount() == 0)
					resultHeadersList.selectAll();
				Display display = Display.getDefault();
				final Clipboard cb = new Clipboard(display);
				TextTransfer textTransfer = TextTransfer.getInstance();
				cb.setContents(resultHeadersList.getSelection() ,
						new Transfer[] { textTransfer });
			}

			public void widgetSelected(SelectionEvent arg0) {
				widgetDefaultSelected(arg0);
			}
		});

		resultHeadersList.setMenu(resultsHeaderMenu);

		resultHeadersList.addMouseListener(new MouseListener() {
			public void mouseDoubleClick(MouseEvent arg0) {
			}
			public void mouseDown(MouseEvent arg0) {
				setMenusForCurrentState();
			}
			public void mouseUp(MouseEvent arg0) {
			}
		});

		GridData gd6 = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd6.heightHint = 1;
		gd6.minimumHeight = 50;
		ec2.setLayoutData(gd6);
		ec2.addExpansionListener(new FormExpansionAdapter());

		ExpandableComposite ec4 = toolkit.createExpandableComposite(sectionClient2, 
				ExpandableComposite.TWISTIE| ExpandableComposite.TITLE_BAR |
				ExpandableComposite.CLIENT_INDENT |
				ExpandableComposite.EXPANDED);
		ec4.setText(JBossWSUIMessages.JAXRSWSTestView2_ResponseBody_Section);

		createResponseToolbar(ec4);

		pageBook = toolkit.createPageBook(ec4, SWT.NONE);

		Composite page1 = pageBook.createPage(PAGE1_KEY);
		page1.setLayout(new GridLayout());
		resultsText = toolkit.createText(page1, EMPTY_STRING, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		GridData gd7 = new GridData(SWT.FILL, SWT.FILL, true, true);
		//		gd7.minimumHeight = 100;
		gd7.heightHint = 1;
		resultsText.setLayoutData(gd7);

		pageBook.showPage(PAGE1_KEY);

		Composite page2 = pageBook.createPage(PAGE2_KEY);
		page2.setLayout(new GridLayout());
		resultsBrowser = new Browser(page2, SWT.BORDER | SWT.WRAP );// | SWT.V_SCROLL);
		GridData gd10 = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd10.heightHint = 1;
		toolkit.adapt(resultsBrowser);
		resultsBrowser.setLayoutData(gd10);

		pageBook.showPage(PAGE2_KEY);

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true); //GridData.FILL_HORIZONTAL);
		gd.heightHint = 1;
		gd.minimumHeight = 100;
		pageBook.setLayoutData(gd);

		pageBook.showPage(PAGE1_KEY);

		resultsTextMenu = new Menu(resultsText.getShell(), SWT.POP_UP);

		copyMenuAction = new MenuItem(resultsTextMenu, SWT.PUSH);
		copyMenuAction.setText(JBossWSUIMessages.JAXRSWSTestView_CopyResultsMenu);
		copyMenuAction.setAccelerator(SWT.CTRL + 'C');
		copyMenuAction.addSelectionListener(new SelectionListener(){

			public void widgetDefaultSelected(SelectionEvent arg0) {
				if (resultsText.getSelectionCount() == 0)
					resultsText.selectAll();
				resultsText.copy();
			}

			public void widgetSelected(SelectionEvent arg0) {
				widgetDefaultSelected(arg0);
			}
		});
		new MenuItem(resultsTextMenu, SWT.SEPARATOR);

		openInXMLEditorAction = new MenuItem(resultsTextMenu, SWT.PUSH);
		openInXMLEditorAction.setText(JBossWSUIMessages.JAXRSWSTestView_Open_Result_in_XML_Editor);
		openInXMLEditorAction.setAccelerator(SWT.CTRL + 'O');
		openInXMLEditorAction.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent arg0) {
				String string = resultsText.getText();
				openXMLEditor(string);
			}

			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		openResponseTagInXMLEditor = new MenuItem(resultsTextMenu, SWT.PUSH);
		openResponseTagInXMLEditor.setText(JBossWSUIMessages.JAXRSWSTestView_Open_Response_Tag_Contents_in_XML_Editor);
		openResponseTagInXMLEditor.setAccelerator(SWT.CTRL + 'R');
		openResponseTagInXMLEditor.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent arg0) {
				String string = null;
				try {
					SOAPBody body = null;
					if (envelope != null){
						body = envelope.getBody();
					} else if (soapbody != null) {
						body = soapbody;
					}

					NodeList list = body.getChildNodes();
					for (int i = 0; i< list.getLength(); i++){
						Node node = list.item(i);
						if (node.getNodeName().contains("Response")){ //$NON-NLS-1$
							NodeList list2 = node.getChildNodes();
							for (int j = 0; j<list2.getLength(); j++){
								Node node2 = list2.item(j);
								if (node2.getNodeName().contains("Result")){ //$NON-NLS-1$
									Node node3 = node2.getChildNodes().item(0);
									if (node3.getNodeType() == Node.TEXT_NODE) {
										string = node3.getNodeValue();
										break;
									} else if (node3.getNodeType() == Node.ELEMENT_NODE) {
										Element element = (Element) node3;
										string = XMLUtils.ElementToString(element);
										break;
									}
								}
							}
							if (string != null) break;
						}
					}
					if (string != null){
						openXMLEditor(string);
					}
				} catch (SOAPException e) {
					JBossWSUIPlugin.log(e);
				}
			}

			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		resultsText.setMenu(resultsTextMenu);		

		resultsText.addFocusListener(new FocusListener() {

			public void focusLost(FocusEvent arg0) {
			}

			public void focusGained(FocusEvent arg0) {
				setMenusForCurrentState();
			}
		});

		ec4.setClient(pageBook);
		GridData gd8 = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd8.heightHint = 1;
		ec4.setLayoutData(gd8);
		ec4.addExpansionListener(new FormExpansionAdapter());

		section2.addExpansionListener(new FormExpansionAdapter());
		section2.setClient(sectionClient2);  	    
	}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {

		getImages();

		toolkit = new FormToolkit(parent.getDisplay());
		form = toolkit.createScrolledForm(parent);
		form.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event e) {
				form.setRedraw(false);
				form.reflow(true);
				form.layout(true, true);
				form.setRedraw(true);
			}
		});
		GridLayout layout = new GridLayout(2, false);
		layout.verticalSpacing = 1;
		form.getBody().setLayout(layout);

		createURLAndToolbar();

		SashForm sashForm = new SashForm(form.getBody(), SWT.NONE);
		sashForm.setOrientation(SWT.HORIZONTAL);
		toolkit.adapt(sashForm);
		GridLayout sashLayout = new GridLayout(2, false);
		sashForm.setLayout(sashLayout);
		GridData gd3 = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd3.horizontalSpan = 2;
		gd3.widthHint = 1;
		sashForm.setLayoutData(gd3);

		createRequestSide(sashForm);

		createResponseSide(sashForm);

		toolkit.paintBordersFor(form);
		form.reflow(true);

		methodCombo.setText(GET); // changed from JAX-WS to GET via JBIDE-14861
		setControlsForWSType(getCurrentTestType());
		setControlsForMethodType(methodCombo.getText());
		setControlsForSelectedURL();
		setMenusForCurrentState();
	}

	@Override
	public void dispose() {
		toolkit.dispose();
		mImageRegistry.dispose();
		super.dispose();
	}

	private String generateSampleSOAP ( String headerText, String innerText, boolean isSOAP12 ) {
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
		buffer.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " );//$NON-NLS-1$
		buffer.append("xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" >\n");//$NON-NLS-1$
		buffer.append("<" + prefix + ":Header>\n");//$NON-NLS-1$ //$NON-NLS-2$
		if (headerText != null) 
			buffer.append(headerText);
		buffer.append("</" + prefix + ":Header>\n");;//$NON-NLS-1$ //$NON-NLS-2$
		buffer.append("<" + prefix + ":Body>\n");//$NON-NLS-1$ //$NON-NLS-2$
		if (innerText != null)
			buffer.append(innerText);
		buffer.append("</" + prefix + ":Body>\n");;//$NON-NLS-1$ //$NON-NLS-2$
		buffer.append("</" + prefix + ":Envelope>");//$NON-NLS-1$ //$NON-NLS-2$ 
		
//		String soapIn = "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\" ?>\n" + //$NON-NLS-1$
//		"<" + prefix + ":Envelope xmlns:" + prefix + "=\"" + soapURI + "\" " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
//		"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +  //$NON-NLS-1$
//		"xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +  //$NON-NLS-1$
//		">\n" + //$NON-NLS-1$
//		"<" + prefix + ":Body>\n";//$NON-NLS-1$ //$NON-NLS-2$
//		if (innerText != null)
//			soapIn = soapIn + innerText;
//		soapIn = soapIn +
//		"</" + prefix + ":Body>\n" + //$NON-NLS-1$ //$NON-NLS-2$
//		"</" + prefix + ":Envelope>";	 //$NON-NLS-1$ //$NON-NLS-2$
		return buffer.toString();
	}

	private void setMenusForCurrentState() {
		if (resultsText!= null && !resultsText.isDisposed()){
			boolean enabled = resultsText.getText().trim().length() > 0; 
			copyMenuAction.setEnabled(enabled);
			openInXMLEditorAction.setEnabled(enabled);
			if (getCurrentTestType().equalsIgnoreCase(JAX_WS)) {
				openResponseTagInXMLEditor.setEnabled(enabled);
			} else if (getCurrentTestType().equalsIgnoreCase(JAX_RS) ){
				openResponseTagInXMLEditor.setEnabled(false);
			}
		}
		if (resultHeadersList != null && !resultHeadersList.isDisposed()) {
			boolean enabled = resultHeadersList.getItemCount() > 0;
			copyResultHeaderMenuAction.setEnabled(enabled);
		}
	}

	private void openXMLEditor (String text){
		IWorkbenchWindow window = getSite().getWorkbenchWindow();
		IStorage storage = new ResultsXMLStorage(text);
		IStorageEditorInput input = new ResultsXMLStorageInput(storage);
		IWorkbenchPage page = window.getActivePage();
		if (page != null) {
			try {
				if (WSTestUtils.isTextXML(text)) {
					if (window.getWorkbench().getEditorRegistry().findEditor(XML_EDITOR_ID) != null) {
						page.openEditor(input, XML_EDITOR_ID);
					} else {
						page.openEditor(input, DEFAULT_TEXT_EDITOR_ID);
					}
				} else {
					page.openEditor(input, DEFAULT_TEXT_EDITOR_ID);
				}
			} catch (PartInitException e) {
				JBossWSUIPlugin.log(e);
			}			
		}
	}

	private void setControlsForSelectedEntry ( TestHistoryEntry entry ) {
		if (entry != null) {
			restoringFromHistoryEntry = true;
			if (entry.getWsTech() != null) {
				setControlsForWSType(entry.getWsTech());
				if (entry.getWsTech().equalsIgnoreCase(JAX_WS))
					methodCombo.setText(JAX_WS);
			}
			if (entry.getMethod() != null && entry.getWsTech().equalsIgnoreCase(JAX_RS)) {
				methodCombo.setText(entry.getMethod());
				setControlsForMethodType(entry.getMethod());
			}
			if (bodyText.isEnabled()) {
				bodyText.setText(entry.getBody());
			}
			if (!treeRequestBody.getTree().isDisposed()) {
				if (entry.getBody().trim().length() > 0) {
					if (SOAPDOMParser.isXMLLike(entry.getBody()))
						treeRequestBody.setInput(entry.getBody());
					else
						treeRequestBody.setInput(null);
				} else {
					treeRequestBody.setInput(null);
				}
			}
			if (dlsList.isEnabled()) {
				dlsList.setSelection(entry.getHeaders());
			}
			if (parmsList.isEnabled()) {
				parmsList.setSelection(entry.getParms());
			}
			if (resultHeadersList.isEnabled()) {
				resultHeadersList.removeAll();
				String[] headers = entry.getResultHeadersList();
				if (headers != null && headers.length > 0) {
					for (int i = 0; i < headers.length; i++) { 
						resultHeadersList.add(headers[i]);
					}
				}
			}
			if (resultsText.isEnabled() && resultsBrowser.isEnabled()) {
				resultsText.setText(entry.getResultText());
				resultsBrowser.setText(entry.getResultText());
			}
			if (entry.getUrl().trim().length() > 0) {
				String urlText = entry.getUrl();
				try {
					new URL(urlText);
					startToolItem.setEnabled(true);
				} catch (MalformedURLException mue) {
					startToolItem.setEnabled(false);
					return;
				}
			} else {
				startToolItem.setEnabled(false);
			}
			restoringFromHistoryEntry = false;
		}
	}
	
	private void setControlsForSelectedURL() {
		if (urlCombo.getText().trim().length() > 0) {
			String urlText = urlCombo.getText();
			try {
				new URL(urlText);
				startToolItem.setEnabled(true);
				
				// per JBIDE-6919, if we encounter an "https" url make sure 
				// basic authorization checkbox is checked
				// per JBIDE-12981, only set this when the user updates
				// the url, not every time they run the test
				if (urlText.trim().startsWith(HTTPS_STRING)) {
					useBasicAuthCB.setSelection(true);
				}
			} catch (MalformedURLException mue) {
				startToolItem.setEnabled(false);
				return;
			}
		} else {
			startToolItem.setEnabled(false);
		}
	}

	/*
	 * Enable/disable controls based on the WS technology type
	 * and the method.
	 * 
	 * @param methodType
	 */
	private void setControlsForMethodType ( String methodType ) {
		if (getCurrentTestType().equalsIgnoreCase(JAX_RS) &&
				(methodType.equalsIgnoreCase(GET) ||
				 methodType.equalsIgnoreCase(OPTIONS))) {
			bodyText.setEnabled(false);
			treeRequestBody.getTree().setEnabled(false);
		} else {
			bodyText.setEnabled(true);
			treeRequestBody.getTree().setEnabled(true);
		}
	}

	/*
	 * Enable/disable controls based on the WS technology type
	 * @param wsType
	 */
	private void setControlsForWSType ( String wsType ) {
		if (wsType.equalsIgnoreCase(JAX_WS)) {
			bodyText.setEnabled(true);
			treeRequestBody.getTree().setEnabled(true);
			parmsList.setEnabled(false);
			parmsList.removeAll();
			dlsList.setEnabled(false);

			String emptySOAP = 
				generateSampleSOAP(null, null, false);
			emptySOAP = WSTestUtils.addNLsToXML(emptySOAP);

			if (bodyText.getText().trim().length() == 0) {
				bodyText.setText(emptySOAP);
				treeRequestBody.setInput(emptySOAP);
			}
			openWSDLToolItem.setEnabled(true);
		}
		else if (wsType.equalsIgnoreCase(JAX_RS)) {
			bodyText.setEnabled(true);
			treeRequestBody.getTree().setEnabled(true);
			parmsList.setEnabled(true);
			dlsList.setEnabled(true);
			openWSDLToolItem.setEnabled(false);

			if (bodyText.getText().trim().length() > 0) {
				bodyText.setText(EMPTY_STRING);
				treeRequestBody.setInput(null);
//				getCurrentHistoryEntry().setBody(EMPTY_STRING);
			}
		}
		setMenusForCurrentState();
	}

	private String getOpNameFromRequestBody () {
		MessageFactory factory = new MessageFactoryImpl();
		String lookForOpName = null;
		try {
            SOAPMessage message =
				factory.createMessage(null, new ByteArrayInputStream(getCurrentHistoryEntry().getBody().trim().getBytes()));
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
		return lookForOpName;
	}
	
	/*
	 * Actually perform the test based on which type of activity it is 
	 */
	private void handleTest(final String wsTech) {

		String urlText = urlCombo.getText();
		
		// if we need to configure incoming parameters in the URL (i.e. from JAX-RS tooling) 
		if (urlText.endsWith("}")) { //$NON-NLS-1$
			WSTesterURLInputsDialog dialog = new WSTesterURLInputsDialog(this.getSite().getShell(), urlText);
			int rtn_code = dialog.open();
			if (rtn_code == Window.OK) {
				urlText = dialog.getURL();
				urlCombo.setText(urlText);
			} else {
				return;
			}
		}
		
		try {
			new URL(urlText);
		} catch (MalformedURLException mue) {
			// do nothing, but return since we don't have a working URL
			return;
		}

		String lookForOpName = null;
		
		if (wsTech.contentEquals(JAX_WS)) {
			lookForOpName = getOpNameFromRequestBody();

			if (getCurrentHistoryEntry().getAction() == null ) {
				boolean result = getWSDLSpecifics(lookForOpName);
				if (!result)
					return;
			}

		}

		if (urlCombo.getItemCount() > 0) {
			java.util.List<String> aList = Arrays.asList(urlCombo.getItems());
			if (!aList.contains(urlCombo.getText())) {
				urlCombo.add(urlCombo.getText());
			}
		} else {
			urlCombo.add(urlCombo.getText());
		}
		
		getCurrentHistoryEntry().setWsTech(wsTech);
		
		if (!getCurrentHistoryEntry().getUrl().contentEquals(urlText)) {
			getCurrentHistoryEntry().setUrl(urlText);
		}
		
		final String url = getCurrentHistoryEntry().getUrl();
		final String action = getCurrentHistoryEntry().getAction();
		String tempBody = getCurrentHistoryEntry().getBody();

		// if it's XML, clean up the empty space and crlf between tags
		if (SOAPDOMParser.isXMLLike(tempBody)) {
			tempBody = tempBody.replaceAll(">\\s+<", "><");  //$NON-NLS-1$//$NON-NLS-2$
		}
		final String body = tempBody;
		final String method = getCurrentHistoryEntry().getMethod();
		final String headers = getCurrentHistoryEntry().getHeaders();
		final String parms = getCurrentHistoryEntry().getParms();
		
		String tempUID = null;
		String tempPwd = null;
		
		// per JBIDE-6919, if we encounter an "https" url make sure 
		// basic authorization checkbox is checked
		// per JBIDE-12981, moved this check and set to setControlsForSelectedURL()
//		if (url.trim().startsWith(HTTPS_STRING)) {
//			useBasicAuthCB.setSelection(true);
//		}
		
		// If basic authorization checkbox is checked, use the uid/pwd
		if (useBasicAuthCB.getSelection()) {
			UidPwdDialog authDialog = new UidPwdDialog(getSite().getShell());
			int rtnCode = authDialog.open();
			if (rtnCode == Window.OK) {
				tempUID = authDialog.getUID();
				tempPwd = authDialog.getPwd();
			} else {
				return;
			}
		}
		final String uid = tempUID;
		final String pwd = tempPwd;

		Job aJob = new Job(JBossWSUIMessages.JAXRSWSTestView_Invoking_WS_Status) {
			protected IStatus run(IProgressMonitor monitor) {
				IStatus status = Status.OK_STATUS;
				// execute the task ...
				if (wsTech.equalsIgnoreCase(JAX_RS)) {
					status = handleRSTest(monitor, url, method, body, parms, headers, uid, pwd);
				}
				else if (wsTech.equalsIgnoreCase(JAX_WS)) {
					status = handleWSTest(monitor, url, action, body, uid, pwd);
				}
				monitor.done();
				return status;  
			}
		};
		// true to indicate that this job was initiated by a UI end user
		aJob.setUser(true);		
		aJob.addJobChangeListener(new IJobChangeListener() {

			public void sleeping(IJobChangeEvent event) {};
			public void scheduled(IJobChangeEvent event) {};
			public void running(IJobChangeEvent event) {};
			public void done(final IJobChangeEvent event) {
				if (event.getResult() instanceof WSTestStatus) {
					final WSTestStatus status = (WSTestStatus) event.getResult();
					PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
						public void run() {
							if (status.getResultsText() != null) {
								String results = status.getResultsText();
								if (SOAPDOMParser.isValidXML(results)) {
									results = SOAPDOMParser.prettyPrint(results);
								} else {
									results = SOAPDOMParser.prettyPrintJSON(results);
								}
								getCurrentHistoryEntry().setResultText(results);
								getCurrentHistoryEntry().setUrl(urlCombo.getText());
								JAXRSWSTestView2.this.resultsText.setText(results);
								JAXRSWSTestView2.this.resultsBrowser.setText(results);
								JAXRSWSTestView2.this.form.reflow(true);
							}
							else if (status.getMessage() != null) { 
								getCurrentHistoryEntry().setResultText(status.getMessage());
								getCurrentHistoryEntry().setUrl(urlCombo.getText());
								JAXRSWSTestView2.this.resultsText.setText(status.getMessage());
								JAXRSWSTestView2.this.resultsBrowser.setText(status.getMessage());
								JAXRSWSTestView2.this.form.reflow(true);
							}
							resultHeadersList.removeAll();
							String[] headers =
								DelimitedStringList.parseString(status.getHeadersList(), RESULT_HEADER_DELIMITER);
							if (headers != null && headers.length > 0) {
								for (int i = 0; i < headers.length; i++) { 
									resultHeadersList.add(headers[i]);
								}
							}
							getCurrentHistoryEntry().setResultHeadersList(headers);
							if (JAXRSWSTestView2.this.resultsText.getText().trim().length() == 0) {
								if (headers != null && headers.length > 0) {
									getCurrentHistoryEntry().setResultText(
											JBossWSUIMessages.JAXRSWSTestView2_Msg_No_Results_Check_Headers);
									JAXRSWSTestView2.this.resultsText.setText(
											JBossWSUIMessages.JAXRSWSTestView2_Msg_No_Results_Check_Headers);
									JAXRSWSTestView2.this.form.reflow(true);
								}
							}
							TestHistoryEntry oldEntry = history.findEntryByURL(getCurrentHistoryEntry().getUrl());
							if (oldEntry != null) {
								history.replaceEntry(oldEntry, getCurrentHistoryEntry());
							} else {
								try {
									history.addEntry((TestHistoryEntry) getCurrentHistoryEntry().clone());
								} catch (CloneNotSupportedException e) {
									e.printStackTrace();
								}
							}
//							System.out.println("Replaced or added entry\n" + history.toString());
						}
					});
				}
			}

			public void awake(IJobChangeEvent event) {};
			public void aboutToRun(IJobChangeEvent event) {};
		});
		aJob.schedule();

		setMenusForCurrentState();
	}

	/*
	 * Actually call the WS and displays the result 
	 */
	private IStatus handleWSTest(final IProgressMonitor monitor, String url, String action, String body, String uid, String pwd) {
		try {

			envelope = null;
			soapbody = null;
			monitor.worked(10);
			JAXWSTester2 tester = new JAXWSTester2();
			boolean itRan = false;
			serviceNSMessage = getCurrentHistoryEntry().getServiceNSMessage();
			while (!monitor.isCanceled()) {
				try {
					if (!itRan && serviceNSMessage != null && serviceNSMessage.length == 3) { 
						itRan = true;
						// 	call the service
						tester.doTest(monitor, url, action, serviceNSMessage[0], serviceNSMessage[1], serviceNSMessage[2], body, uid, pwd);
					} else {
						break;
					}
				} catch (InterruptedException ie) {
					monitor.setCanceled(true);
				}
			}
			if (monitor.isCanceled()) {
				WSTestStatus status = new WSTestStatus(IStatus.OK, 
						JBossWSUIPlugin.PLUGIN_ID, 
						JBossWSUIMessages.JAXRSWSTestView_Message_Service_Invocation_Cancelled);
				return status;
			}
			if (!itRan) {
				WSTestStatus status = new WSTestStatus(IStatus.OK, 
						JBossWSUIPlugin.PLUGIN_ID, 
						JBossWSUIMessages.JAXRSWSTestView_Message_Unsuccessful_Test);
				return status;
			}
			monitor.worked(70);
			String result = tester.getResultBody();
			envelope = tester.getResultSOAP();
			soapbody = tester.getResultSOAPBody();
			String cleanedUp = WSTestUtils.addNLsToXML(result);

			WSTestStatus status = new WSTestStatus(IStatus.OK, 
					JBossWSUIPlugin.PLUGIN_ID, 
					JBossWSUIMessages.JAXRSWSTestView_JAXWS_Success_Status);
			status.setResultsText(cleanedUp);
			monitor.worked(10);

			String listText = EMPTY_STRING;
			if (tester.getResultHeaders() != null) {
				Iterator<?> iter = tester.getResultHeaders().entrySet().iterator();
				while (iter.hasNext()) {
					String text = EMPTY_STRING;
					Entry<?, ?> entry = (Entry<?, ?>) iter.next();
					if (entry.getKey() == null) 
						text = entry.getValue().toString();
					else
						text = text + entry.toString();
					listText = listText + text;
					if (iter.hasNext()) {
						listText = listText + RESULT_HEADER_DELIMITER;
					}
				}
			}
			status.setHeadersList(listText);
			monitor.worked(10);
			return status;
		} catch (Exception e) {

			// try and drill down to find the root cause
			Throwable innerE = e.getCause();
			
			// if we can't find it, just go with th exception
			if (innerE == null) {
				WSTestStatus status = new WSTestStatus(IStatus.OK, 
						JBossWSUIPlugin.PLUGIN_ID, 
						JBossWSUIMessages.JAXRSWSTestView_Exception_Status + e.getLocalizedMessage());
				status.setResultsText(e.toString());
				// this fix is to address JBIDE-11294 and the fact that we shouldn't actually log this exception from deep 
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
			WSTestStatus status = new WSTestStatus(IStatus.OK, 
					JBossWSUIPlugin.PLUGIN_ID, 
					JBossWSUIMessages.JAXRSWSTestView_Exception_Status + innerE.getLocalizedMessage());
			status.setResultsText(innerE.toString());
			
			// this fix is to address JBIDE-11294 and the fact that we shouldn't actually log this exception from deep 
			// within the WS API. 
			if ((!innerE.getLocalizedMessage().contains("Unsupported endpoint address: REPLACE_WITH_ACTUAL_URL"))) { //$NON-NLS-1$
				JBossWSUIPlugin.log(e);
			}
			return status;
		}
	}

	/*
	 * Actually call the RESTful WS to test it
	 */
	private IStatus handleRSTest(final IProgressMonitor monitor, String address, String method, String body, String parms, String headersStr, String uid, String pwd) {

		if (method.equalsIgnoreCase(GET))
			body = EMPTY_STRING;

		// if no actual text in the request body, set to null
		if (body != null && body.trim().length() == 0) body = null;

		monitor.worked(10);

		// Process parameters for web service call
		HashMap<String, String> parameters = new HashMap<String, String>();
		if (parms != null && parms.length() > 0) {
			String[] parsedList = DelimitedStringList.parseString(parms , ","); //$NON-NLS-1$
			if (parsedList != null && parsedList.length > 0) {
				for (int i = 0; i < parsedList.length; i++) {
					String nameValuePair = parsedList[i];
					String[] nameAndValue = DelimitedStringList.parseString(nameValuePair, "="); //$NON-NLS-1$
					if (nameAndValue != null && nameAndValue.length == 2) {
						parameters.put(nameAndValue[0], nameAndValue[1]);
					}
				}
			}
		}

		monitor.worked(10);
		// Process headers for web service call
		HashMap<String, String> headers = new HashMap<String, String>();
		if (headersStr != null && headersStr.length() > 0) {
			String[] parsedList = DelimitedStringList.parseString(headersStr , ","); //$NON-NLS-1$
			if (parsedList != null && parsedList.length > 0) {
				for (int i = 0; i < parsedList.length; i++) {
					String nameValuePair = parsedList[i];
					String[] nameAndValue = DelimitedStringList.parseString(nameValuePair, "="); //$NON-NLS-1$
					if (nameAndValue != null && nameAndValue.length == 2) {
						headers.put(nameAndValue[0], nameAndValue[1]);
					}
				}
			}
		}

		JAXRSTester tester = new JAXRSTester();

		// now actually call it
		try {

			// call the service
			tester.doTest(address, parameters, headers, method, body, null, -1, uid, pwd);

			String result = tester.getResultBody();

			// put the results in the result text field
			String cleanedUp = WSTestUtils.addNLsToXML(result);

			WSTestStatus status = new WSTestStatus(IStatus.OK, 
					JBossWSUIPlugin.PLUGIN_ID, 
					JBossWSUIMessages.JAXRSWSTestView_JAXRS_Success_Status);
			status.setResultsText(cleanedUp);

			String listText = EMPTY_STRING;
			if (tester.getResultHeaders() != null) {
				Iterator<?> iter = tester.getResultHeaders().entrySet().iterator();
				while (iter.hasNext()) {
					String text = EMPTY_STRING;
					Entry<?, ?> entry = (Entry<?, ?>) iter.next();
					if (entry.getKey() == null) 
						text = entry.getValue().toString();
					else
						text = text + entry.toString();
					listText = listText + text;
					if (iter.hasNext()) {
						listText = listText + RESULT_HEADER_DELIMITER;
					}
				}
			}

			status.setHeadersList(listText);
			monitor.worked(10);
			return status;

		} catch (Exception e) {
			String result = tester.getResultBody();
			if (result.isEmpty()) 
				result = e.getLocalizedMessage();

			// put the results in the result text field
			String cleanedUp = WSTestUtils.addNLsToXML(result);

			WSTestStatus status = new WSTestStatus(IStatus.OK, 
					JBossWSUIPlugin.PLUGIN_ID, 
					JBossWSUIMessages.JAXRSWSTestView_JAXRS_Success_Status);
			status.setResultsText(cleanedUp);

			String listText = EMPTY_STRING;
			if (tester.getResultHeaders() != null) {
				Iterator<?> iter = tester.getResultHeaders().entrySet().iterator();
				while (iter.hasNext()) {
					String text = EMPTY_STRING;
					Entry<?, ?> entry = (Entry<?, ?>) iter.next();
					if (entry.getKey() == null) 
						text = entry.getValue().toString();
					else
						text = text + entry.toString();
					listText = listText + text;
					if (iter.hasNext()) {
						listText = listText + RESULT_HEADER_DELIMITER;
					}
				}
			}

			status.setHeadersList(listText);
			monitor.worked(10);
			return status;
		}
	}

	/**
	 * Passing the focus request to the control.
	 */
	public void setFocus() {
		// set initial focus to the URL text combo
		urlCombo.setFocus();
	}

}