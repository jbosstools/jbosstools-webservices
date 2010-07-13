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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.wsdl.Definition;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;

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
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
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
import org.jboss.tools.ws.ui.messages.JBossWSUIMessages;
import org.jboss.tools.ws.ui.utils.JAXRSTester;
import org.jboss.tools.ws.ui.utils.JAXWSTester2;
import org.jboss.tools.ws.ui.utils.ResultsXMLStorage;
import org.jboss.tools.ws.ui.utils.ResultsXMLStorageInput;
import org.jboss.tools.ws.ui.utils.TesterWSDLUtils;
import org.jboss.tools.ws.ui.utils.WSTestUtils;
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
	private static final String JAX_WS = "JAX-WS"; //$NON-NLS-1$
	private static final String JAX_RS = "JAX-RS"; //$NON-NLS-1$
	private static final String EMPTY_STRING = ""; //$NON-NLS-1$
	private static final String RESULT_HEADER_DELIMITER = "%";//$NON-NLS-1$

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
	private String actionText = null;

	private FormToolkit toolkit;
	private ScrolledForm form;
	private ImageRegistry mImageRegistry;

	private static final String IMG_DESC_WSDL = "icons/obj16/wsdl.gif"; //$NON-NLS-1$
	private static final String IMG_DESC_START = "icons/obj16/run.gif"; //$NON-NLS-1$

	private static final String IMG_DESC_SHOWRAW = "icons/obj16/binary.gif"; //$NON-NLS-1$
	private static final String IMG_DESC_SHOWWEB = "icons/obj16/web.gif"; //$NON-NLS-1$
	private static final String IMG_DESC_SHOWEDITOR = "icons/obj16/properties.gif"; //$NON-NLS-1$
	private static final String IMG_DESC_SAVE = "icons/obj16/save_edit.gif"; //$NON-NLS-1$

	private ToolItem openWSDLToolItem;
	private ToolItem startToolItem;
	private ScrolledPageBook pageBook;
	private ShowRawAction rawAction;
	private ShowInBrowserAction browserAction;

	/**
	 * The constructor.
	 */
	public JAXRSWSTestView2() {
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

	private String getCurrentTestType() {
		if (methodCombo.getText().equalsIgnoreCase(JAX_WS))
			return JAX_WS;
		return JAX_RS;
	}

	private void createURLAndToolbar( ) {
		urlCombo = new Combo(form.getBody(), SWT.BORDER | SWT.DROP_DOWN);
		GridData gdURL = new GridData(SWT.FILL, SWT.NONE, true, false);
		urlCombo.setLayoutData(gdURL);
		toolkit.adapt(urlCombo);

		urlCombo.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
			}

			public void keyReleased(KeyEvent e) {
				setControlsForSelectedURL();
				if (e.keyCode == SWT.CR && e.stateMask == SWT.CTRL) {
					handleTest(getCurrentTestType());
				}
			}
		});
		urlCombo.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
			public void widgetSelected(SelectionEvent e) {
				setControlsForSelectedURL();
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
		String[] methods = {JAX_WS, GET, POST, PUT, DELETE};
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
				WSDLBrowseDialog wbDialog =  new WSDLBrowseDialog(getSite().getShell());
				int rtnCode = wbDialog.open();
				if (rtnCode == Window.OK){
					serviceNSMessage = null;
					actionText = null;
					Definition wsdlDef = wbDialog.getWSDLDefinition();
					String output = TesterWSDLUtils.getSampleSOAPInputMessage(wsdlDef, 
							wbDialog.getServiceTextValue(), 
							wbDialog.getPortTextValue(), 
							wbDialog.getBindingValue(), 
							wbDialog.getOperationTextValue());
					String endpointURL = TesterWSDLUtils.getEndpointURL(wsdlDef, 
							wbDialog.getServiceTextValue(), 
							wbDialog.getPortTextValue(), 
							wbDialog.getBindingValue(), 
							wbDialog.getOperationTextValue());
					String actionURL = TesterWSDLUtils.getActionURL(wsdlDef, 
							wbDialog.getServiceTextValue(), 
							wbDialog.getPortTextValue(), 
							wbDialog.getBindingValue(), 
							wbDialog.getOperationTextValue());
					serviceNSMessage = TesterWSDLUtils.getNSServiceNameAndMessageNameArray(wsdlDef, 
							wbDialog.getServiceTextValue(), 
							wbDialog.getPortTextValue(), 
							wbDialog.getBindingValue(), 
							wbDialog.getOperationTextValue());
					String soapIn = generateSampleSOAP(output);
					bodyText.setText(soapIn);
					urlCombo.setText(endpointURL);
					actionText = actionURL;
					setControlsForWSType(getCurrentTestType());
					setControlsForMethodType(methodCombo.getText());
					setControlsForSelectedURL();
				}
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

		ExpandableComposite ec = toolkit.createExpandableComposite(sectionClient, 
				ExpandableComposite.TREE_NODE| ExpandableComposite.TITLE_BAR |
				ExpandableComposite.CLIENT_INDENT);
		ec.setText(JBossWSUIMessages.JAXRSWSTestView2_Headers_Section);
		dlsList = new DelimitedStringList(ec, SWT.None, false, false);
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
		bodyText = toolkit.createText(ec5, EMPTY_STRING, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		ec5.setClient(bodyText);
		GridData gd9 = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd9.minimumHeight = 200;
		ec5.setLayoutData(gd9);
		ec5.addExpansionListener(new FormExpansionAdapter());

		section.addExpansionListener(new FormExpansionAdapter());
		section.setClient(sectionClient);  	    
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
		gd7.heightHint = 1;
		//		gd10.minimumHeight = 100;
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

		methodCombo.setText(JAX_WS);
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

	private String generateSampleSOAP ( String innerText ) {
		String soapIn = "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\" ?>\n" + //$NON-NLS-1$
		"<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" " + //$NON-NLS-1$
		"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +  //$NON-NLS-1$
		"xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +  //$NON-NLS-1$
		">\n" + //$NON-NLS-1$
		"<soap:Body>\n";//$NON-NLS-1$
		if (innerText != null)
			soapIn = soapIn + innerText;
		soapIn = soapIn +
		"</soap:Body>\n" + //$NON-NLS-1$
		"</soap:Envelope>";	 //$NON-NLS-1$
		return soapIn;
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

	private void setControlsForSelectedURL() {
		if (urlCombo.getText().trim().length() > 0) {
			String urlText = urlCombo.getText();
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
	}

	/*
	 * Enable/disable controls based on the WS technology type
	 * and the method.
	 * 
	 * @param methodType
	 */
	private void setControlsForMethodType ( String methodType ) {
		if (getCurrentTestType().equalsIgnoreCase(JAX_RS) &&
				methodType.equalsIgnoreCase(GET)) {
			bodyText.setEnabled(false);
		} else {
			bodyText.setEnabled(true);
		}
	}

	/*
	 * Enable/disable controls based on the WS technology type
	 * @param wsType
	 */
	private void setControlsForWSType ( String wsType ) {
		if (wsType.equalsIgnoreCase(JAX_WS)) {
			bodyText.setEnabled(true);
			parmsList.setEnabled(false);
			parmsList.removeAll();
			dlsList.setEnabled(false);

			String emptySOAP = 
				generateSampleSOAP(null);
			emptySOAP = WSTestUtils.addNLsToXML(emptySOAP);

			if (bodyText.getText().trim().length() == 0) {
				bodyText.setText(emptySOAP);
			}
			openWSDLToolItem.setEnabled(true);
		}
		else if (wsType.equalsIgnoreCase(JAX_RS)) {
			bodyText.setEnabled(true);
			parmsList.setEnabled(true);
			dlsList.setEnabled(true);
			openWSDLToolItem.setEnabled(false);

			if (bodyText.getText().trim().length() > 0) {
				bodyText.setText(EMPTY_STRING);
			}
		}
		setMenusForCurrentState();
	}

	/*
	 * Actually perform the test based on which type of activity it is 
	 */
	private void handleTest(final String wsTech) {

		String urlText = urlCombo.getText();
		try {
			new URL(urlText);
		} catch (MalformedURLException mue) {
			// do nothing, but return since we don't have a working URL
			return;
		}

		if (urlCombo.getItemCount() > 0) {
			java.util.List<String> aList = Arrays.asList(urlCombo.getItems());
			if (!aList.contains(urlCombo.getText())) {
				urlCombo.add(urlCombo.getText());
			}
		} else {
			urlCombo.add(urlCombo.getText());
		}

		final String url = urlCombo.getText();
		final String action = actionText;
		final String body = bodyText.getText();
		final String method = methodCombo.getText();
		final String headers = dlsList.getSelection();
		final String parms = parmsList.getSelection();

		Job aJob = new Job(JBossWSUIMessages.JAXRSWSTestView_Invoking_WS_Status) {
			protected IStatus run(IProgressMonitor monitor) {
				IStatus status = Status.OK_STATUS;
				// execute the task ...
				if (wsTech.equalsIgnoreCase(JAX_RS)) {
					status = handleRSTest(monitor, url, method, body, parms, headers);
				}
				else if (wsTech.equalsIgnoreCase(JAX_WS)) {
					status = handleWSTest(monitor, url, action, body);
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
								JAXRSWSTestView2.this.resultsText.setText(status.getResultsText());
								JAXRSWSTestView2.this.resultsBrowser.setText(status.getResultsText());
								JAXRSWSTestView2.this.form.reflow(true);
							}
							else if (status.getMessage() != null) { 
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
	private IStatus handleWSTest(final IProgressMonitor monitor, String url, String action, String body) {
		try {

			envelope = null;
			soapbody = null;
			monitor.worked(10);
			JAXWSTester2 tester = new JAXWSTester2();
			boolean itRan = false;
			while (!monitor.isCanceled()) {
				try {
					if (!itRan && serviceNSMessage != null && serviceNSMessage.length == 3) { 
						itRan = true;
						// 	call the service
						tester.doTest(monitor, url, action, serviceNSMessage[0], serviceNSMessage[1], serviceNSMessage[2], body);
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
			WSTestStatus status = new WSTestStatus(IStatus.OK, 
					JBossWSUIPlugin.PLUGIN_ID, 
					JBossWSUIMessages.JAXRSWSTestView_Exception_Status + e.getLocalizedMessage());
			status.setResultsText(e.toString());
			JBossWSUIPlugin.log(e);
			return status;
		}
	}

	/*
	 * Actually call the RESTful WS to test it
	 */
	private IStatus handleRSTest(final IProgressMonitor monitor, String address, String method, String body, String parms, String headersStr) {

		if (method.equalsIgnoreCase(GET))
			body = EMPTY_STRING;

		// if no actual text in the request body, set to null
		if (body.trim().length() == 0) body = null;

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
			tester.doTest(address, parameters, headers, method, body);

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