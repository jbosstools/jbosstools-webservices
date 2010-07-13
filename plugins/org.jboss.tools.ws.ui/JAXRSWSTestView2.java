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

import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPEnvelope;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.CoolItem;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
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
import org.eclipse.ui.part.ViewPart;
import org.eclipse.wst.internet.monitor.core.internal.provisional.IMonitor;
import org.eclipse.wst.internet.monitor.core.internal.provisional.MonitorCore;
import org.jboss.tools.ws.ui.JBossWSUIPlugin;
import org.jboss.tools.ws.ui.messages.JBossWSUIMessages;
import org.jboss.tools.ws.ui.utils.JAXRSTester;
import org.jboss.tools.ws.ui.utils.JAXWSTester2;
import org.jboss.tools.ws.ui.utils.ResultsXMLStorage;
import org.jboss.tools.ws.ui.utils.ResultsXMLStorageInput;
import org.jboss.tools.ws.ui.utils.WSTestUtils;

/**
 * View for testing web services (JAX-WS & JAX-RS)
 * @author bfitzpat
 *
 */
@SuppressWarnings("restriction")
public class JAXRSWSTestView2 extends ViewPart {

	private static final String DEFAULT_TEXT_EDITOR_ID = "org.eclipse.ui.DefaultTextEditor"; //$NON-NLS-1$
	private static final String XML_EDITOR_ID = "org.eclipse.wst.xml.ui.internal.tabletree.XMLMultiPageEditorPart"; //$NON-NLS-1$
	private static final String TCPIP_VIEW_ID = "org.eclipse.wst.internet.monitor.view";//$NON-NLS-1$
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
	private Button testButton = null;
	private Button wsdlButton = null;
	private Text actionText;
	private Text resultsText;
	private Combo urlCombo;
	private DelimitedStringList dlsList;
	private Combo methodCombo;
	private Combo wsTypeCombo;
	private Text bodyText;
	private TabFolder tabGroup;
	private TabItem bodyTab;
	private TabItem headerTab;
	private List resultHeadersList;
	private TabItem resultHeadersTab;
	private TabItem resultTab;
	private TabFolder resultTabGroup;

	private TabItem parmsTab;

	private DelimitedStringList parmsList;
	private Button openTCPIPMonitorButton;
	private Button addTCPIPMonitorButton;

	private SOAPEnvelope envelope;
	private SOAPBody soapbody;
	private MenuItem openInXMLEditorAction;
	private MenuItem openResponseTagInXMLEditor;
	private Menu resultsTextMenu;
	private MenuItem copyMenuAction;
	private Menu resultsHeaderMenu;
	private MenuItem copyResultHeaderMenuAction;
	
	private boolean showSampleButton = false;
	private String[] serviceNSMessage = null;
	
	private FormToolkit toolkit;
	private ScrolledForm form;
	private ImageRegistry mImageRegistry;

	private static final String IMG_DESC_WSDL = "icons/obj16/wsdl.gif"; //$NON-NLS-1$
	private static final String IMG_DESC_START = "icons/obj16/run.gif"; //$NON-NLS-1$
	private static final String IMG_DESC_STOP = "icons/obj16/progress_stop.gif"; //$NON-NLS-1$
	
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
		mImageRegistry.put(IMG_DESC_STOP, ImageDescriptor
				.createFromURL(JBossWSUIPlugin.getDefault().getBundle()
						.getEntry(IMG_DESC_STOP)));
	}
	
	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		
		getImages();
		
		toolkit = new FormToolkit(parent.getDisplay());
		form = toolkit.createScrolledForm(parent);
		
		GridLayout layout = new GridLayout(2, false);
  	    form.getBody().setLayout(layout);
  	    
		urlCombo = new Combo(form.getBody(), SWT.BORDER | SWT.DROP_DOWN);
		GridData gdURL = new GridData(SWT.FILL, SWT.NONE, true, false);
		urlCombo.setLayoutData(gdURL);
		toolkit.adapt(urlCombo);
		
		Composite comp1 = toolkit.createComposite(form.getBody());
		comp1.setLayout(new FillLayout());
		
		CoolBar coolBar = new CoolBar(comp1,SWT.BORDER);
		toolkit.adapt(coolBar);
		
		CoolItem wsTypeCoolItem = new CoolItem(coolBar, SWT.PUSH);
		wsTypeCombo = new Combo(coolBar, SWT.READ_ONLY | SWT.BORDER);
        String[] items = {JAX_WS, JAX_RS};
        wsTypeCombo.setItems(items);
        wsTypeCombo.pack();
        Point size = wsTypeCombo.computeSize (SWT.DEFAULT, SWT.DEFAULT);
        wsTypeCoolItem.setPreferredSize (wsTypeCoolItem.computeSize (size.x, size.y));
        wsTypeCoolItem.setControl(wsTypeCombo);
		wsTypeCombo.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
			public void widgetSelected(SelectionEvent e) {
				setControlsForWSType(wsTypeCombo.getText());
				setControlsForMethodType(methodCombo.getText());
				setControlsForSelectedURL();
			}
		});
		toolkit.adapt(wsTypeCombo);
		
		CoolItem wsMethodCoolItem = new CoolItem(coolBar, SWT.PUSH);
		methodCombo = new Combo(coolBar, SWT.BORDER | SWT.READ_ONLY);
		String[] methods = {GET, POST, PUT, DELETE};
		methodCombo.setItems(methods);
		methodCombo.pack();
        size = methodCombo.computeSize (SWT.DEFAULT, SWT.DEFAULT);
        wsMethodCoolItem.setPreferredSize (wsMethodCoolItem.computeSize (size.x, size.y));
        wsMethodCoolItem.setControl(methodCombo);
		methodCombo.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
			public void widgetSelected(SelectionEvent e) {
				setControlsForMethodType(methodCombo.getText());
			}
		});
		toolkit.adapt(methodCombo);

        CoolItem topCoolItem = new CoolItem(coolBar, SWT.NONE);
       
        ToolBar topToolBar = new ToolBar(coolBar,SWT.HORIZONTAL);
        ToolItem openWSDLToolItem = new ToolItem(topToolBar, SWT.PUSH);
        openWSDLToolItem.setImage(mImageRegistry.get(IMG_DESC_WSDL));
        openWSDLToolItem.setToolTipText("Get from WSDL");
        
        ToolItem startToolItem = new ToolItem(topToolBar, SWT.PUSH);
        startToolItem.setImage(mImageRegistry.get(IMG_DESC_START));
        startToolItem.setToolTipText("Go");

        ToolItem stopToolItem = new ToolItem(topToolBar, SWT.PUSH);
        stopToolItem.setImage(mImageRegistry.get(IMG_DESC_STOP));
        stopToolItem.setToolTipText("Stop");
        toolkit.adapt(coolBar);

        topToolBar.pack(); 
        size = topToolBar.getSize();
        topCoolItem.setControl(topToolBar);
        topCoolItem.setSize(topCoolItem.computeSize(size.x, size.y));
        
		SashForm topBottomForm = new SashForm(form.getBody(), SWT.NONE);
		topBottomForm.setOrientation(SWT.VERTICAL);
		toolkit.adapt(topBottomForm);
		topBottomForm.setLayout(new GridLayout());
		GridData gd8 = new GridData(SWT.FILL, SWT.NONE, true, false);
		gd8.horizontalSpan = 2;
		topBottomForm.setLayoutData(gd8);
		
		SashForm sashForm = new SashForm(topBottomForm, SWT.NONE);
		sashForm.setOrientation(SWT.HORIZONTAL);
		toolkit.adapt(sashForm);
		GridLayout sashLayout = new GridLayout(2, false);
		sashForm.setLayout(sashLayout);
		GridData gd3 = new GridData(SWT.FILL, SWT.NONE, true, false);
		gd3.horizontalSpan = 2;
		sashForm.setLayoutData(gd3);

		ExpandableComposite ec = toolkit.createExpandableComposite(sashForm, 
			     ExpandableComposite.TWISTIE|
			     ExpandableComposite.CLIENT_INDENT);
		ec.setText("Headers");
		dlsList = new DelimitedStringList(ec, SWT.None);
		ec.setClient(dlsList);
		toolkit.adapt(dlsList);
		GridData gd = new GridData(SWT.FILL, SWT.NONE, true, false);
		 ec.setLayoutData(gd);
		ec.addExpansionListener(new ExpansionAdapter() {
			  public void expansionStateChanged(ExpansionEvent e) {
			   form.reflow(true);
			  }
		});
		
		ExpandableComposite ec2 = toolkit.createExpandableComposite(sashForm, 
			     ExpandableComposite.TWISTIE|
			     ExpandableComposite.CLIENT_INDENT);
		ec2.setText("Response Headers");
		resultsText = toolkit.createText(ec2, "", SWT.BORDER | SWT.V_SCROLL | SWT.WRAP | SWT.READ_ONLY );
		ec2.setClient(resultsText);
		GridData gd2 = new GridData(SWT.FILL, SWT.FILL, true, false);
		 ec2.setLayoutData(gd2);
		ec2.addExpansionListener(new ExpansionAdapter() {
			  public void expansionStateChanged(ExpansionEvent e) {
			   form.reflow(true);
			  }
		});

		SashForm sashForm2 = new SashForm(topBottomForm, SWT.NONE);
		sashForm2.setOrientation(SWT.HORIZONTAL);
		toolkit.adapt(sashForm);
		GridLayout sashLayout2 = new GridLayout(2, false);
		sashForm2.setLayout(sashLayout2);
		GridData gd5 = new GridData(SWT.FILL, SWT.NONE, true, false);
		gd5.horizontalSpan = 2;
		sashForm2.setLayoutData(gd5);

		ExpandableComposite ec3 = toolkit.createExpandableComposite(sashForm2, 
			     ExpandableComposite.TWISTIE|
			     ExpandableComposite.CLIENT_INDENT);
		ec3.setText("Parameters");
		parmsList = new DelimitedStringList(ec3, SWT.None);
		ec3.setClient(parmsList);
		toolkit.adapt(parmsList);
		GridData gd4 = new GridData(SWT.FILL, SWT.FILL, true, false);
		 ec3.setLayoutData(gd4);
		ec3.addExpansionListener(new ExpansionAdapter() {
			  public void expansionStateChanged(ExpansionEvent e) {
			   form.reflow(true);
			  }
		});

		ExpandableComposite ec4 = toolkit.createExpandableComposite(sashForm2, 
			     ExpandableComposite.TWISTIE|
			     ExpandableComposite.CLIENT_INDENT);
		ec4.setText("Response Body");

		Composite comp2 = toolkit.createComposite(ec4);
		comp2.setLayout(new GridLayout());
		
		Composite comp3 = toolkit.createComposite(comp2);
		comp3.setLayout(new GridLayout(4, false));
		comp3.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		
		Button saveBtn = toolkit.createButton(comp3, "Save", SWT.PUSH);
		saveBtn.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		Button editorBtn = toolkit.createButton(comp3, "Open in Editor", SWT.PUSH);
		editorBtn.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		Button rawBtn = toolkit.createButton(comp3, "Show Raw", SWT.PUSH);
		rawBtn.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		Button browserBtn = toolkit.createButton(comp3, "Show in Browser", SWT.PUSH);
		browserBtn.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

		bodyText = new Text(comp2, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		GridData gd7 = new GridData(SWT.FILL, SWT.FILL, true, true);
		bodyText.setLayoutData(gd7);
		
		ec4.setClient(comp2);
		GridData gd6 = new GridData(SWT.FILL, SWT.FILL, true, false);
		 ec4.setLayoutData(gd6);
		ec4.addExpansionListener(new ExpansionAdapter() {
			  public void expansionStateChanged(ExpansionEvent e) {
			   form.reflow(true);
			  }
		});

		SashForm sashForm3 = new SashForm(topBottomForm, SWT.NONE);
		sashForm3.setOrientation(SWT.HORIZONTAL);
		toolkit.adapt(sashForm);
		GridLayout sashLayout3 = new GridLayout(2, false);
		sashForm3.setLayout(sashLayout3);
		GridData gd10 = new GridData(SWT.FILL, SWT.NONE, true, false);
//		gd10.horizontalSpan = 1;
		sashForm3.setLayoutData(gd10);

		ExpandableComposite ec5 = toolkit.createExpandableComposite(sashForm3, 
			     ExpandableComposite.TWISTIE|
			     ExpandableComposite.CLIENT_INDENT);
		ec5.setText("Body Text");
		bodyText = toolkit.createText(ec5, "", SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		ec5.setClient(bodyText);
		GridData gd9 = new GridData(SWT.FILL, SWT.FILL, true, true);
		 ec5.setLayoutData(gd9);
		ec5.addExpansionListener(new ExpansionAdapter() {
			  public void expansionStateChanged(ExpansionEvent e) {
			   form.reflow(true);
			  }
		});

//		Label methodLabel = new Label(topHalf, SWT.NONE);
//		methodLabel.setText(JBossWSUIMessages.JAXRSWSTestView_HTTP_Method_Label);
//		methodLabel.setLayoutData(new GridData());
//
//		methodCombo = new Combo(topHalf, SWT.DROP_DOWN | SWT.READ_ONLY);
//		methodCombo.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
//		methodCombo.add(GET);
//		methodCombo.add(POST);
//		methodCombo.add(PUT);
//		methodCombo.add(DELETE);
//		methodCombo.setText(GET);
//		methodCombo.addSelectionListener(new SelectionListener() {
//			public void widgetDefaultSelected(SelectionEvent e) {
//				widgetSelected(e);
//			}
//			public void widgetSelected(SelectionEvent e) {
//				setControlsForMethodType(methodCombo.getText());
//			}
//		});
//
//		Label urlLabel = new Label(topHalf, SWT.NONE);
//		urlLabel.setText(JBossWSUIMessages.JAXRSWSTestView_Service_URL_Label);
//		urlLabel.setLayoutData(new GridData());
//
//
//		Label actionLabel = new Label(topHalf, SWT.NONE);
//		actionLabel.setText(JBossWSUIMessages.JAXRSWSTestView_Action_URL_Label);
//		actionLabel.setLayoutData(new GridData());
//
//		actionText = new Text(topHalf, SWT.BORDER);
//		actionText.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
//
//		tabGroup = new TabFolder(topHalf, SWT.BORDER);
//
//		bodyTab = new TabItem(tabGroup, SWT.NONE, 0);
//		bodyTab.setText(JBossWSUIMessages.JAXRSWSTestView_Request_Body_Label);
//
//		parmsTab = new TabItem(tabGroup, SWT.NONE, 1);
//		parmsTab.setText(JBossWSUIMessages.JAXRSWSTestView_Request_Parameters_Label);
//
//		parmsList = new DelimitedStringList(tabGroup, SWT.None);
//		parmsTab.setControl(parmsList);
//		GridData parmsListGD = new GridData(SWT.FILL, SWT.FILL, true, true);
//		parmsListGD.horizontalSpan = 2;
//		parmsList.setLayoutData(parmsListGD);
//
//		headerTab = new TabItem(tabGroup, SWT.NONE, 2);
//		bodyText = new Text(tabGroup, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
//		GridData btGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
//		bodyText.setLayoutData(btGridData);
//		bodyTab.setControl(bodyText);
//
//		headerTab.setText(JBossWSUIMessages.JAXRSWSTestView_Request_Header_Label);
//		GridData hgGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
//		hgGridData.horizontalSpan = 2;
//		tabGroup.setLayoutData(hgGridData);
//
//		dlsList = new DelimitedStringList(tabGroup, SWT.None);
//		headerTab.setControl(dlsList);
//		GridData dlsListGD = new GridData(SWT.FILL, SWT.FILL, true, true);
//		dlsListGD.horizontalSpan = 2;
//		dlsList.setLayoutData(dlsListGD);
//
//		Composite buttonBar = new Composite ( topHalf, SWT.NONE);
//		GridData buttonBarGD = new GridData(SWT.FILL, SWT.NONE, true, false);
//		buttonBarGD.horizontalSpan = 2;
//		buttonBar.setLayoutData(buttonBarGD);
//		buttonBar.setLayout(new RowLayout());
//
//		testButton = new Button (buttonBar, SWT.PUSH);
//		testButton.setText(JBossWSUIMessages.JAXRSWSTestView_Invoke_Label);
//
//		testButton.addSelectionListener(new SelectionListener() {
//			public void widgetSelected(SelectionEvent e) {
//				handleTest(wsTypeCombo.getText());
//			}
//			public void widgetDefaultSelected(SelectionEvent e) {
//				widgetSelected(e);
//			}
//		});
//
//		wsdlButton = new Button (buttonBar, SWT.PUSH);
//		wsdlButton.setText(JBossWSUIMessages.JAXRSWSTestView_Button_Get_From_WSDL);
//
//		wsdlButton.addSelectionListener(new SelectionListener() {
//			public void widgetSelected(SelectionEvent e) {
//				WSDLBrowseDialog wbDialog =  new WSDLBrowseDialog(getSite().getShell());
//				int rtnCode = wbDialog.open();
//				if (rtnCode == Window.OK){
//					serviceNSMessage = null;
//					Definition wsdlDef = wbDialog.getWSDLDefinition();
//					String output = TesterWSDLUtils.getSampleSOAPInputMessage(wsdlDef, 
//							wbDialog.getServiceTextValue(), 
//							wbDialog.getPortTextValue(), 
//							wbDialog.getBindingValue(), 
//							wbDialog.getOperationTextValue());
//					String endpointURL = TesterWSDLUtils.getEndpointURL(wsdlDef, 
//							wbDialog.getServiceTextValue(), 
//							wbDialog.getPortTextValue(), 
//							wbDialog.getBindingValue(), 
//							wbDialog.getOperationTextValue());
//					String actionURL = TesterWSDLUtils.getActionURL(wsdlDef, 
//							wbDialog.getServiceTextValue(), 
//							wbDialog.getPortTextValue(), 
//							wbDialog.getBindingValue(), 
//							wbDialog.getOperationTextValue());
//					serviceNSMessage = TesterWSDLUtils.getNSServiceNameAndMessageNameArray(wsdlDef, 
//							wbDialog.getServiceTextValue(), 
//							wbDialog.getPortTextValue(), 
//							wbDialog.getBindingValue(), 
//							wbDialog.getOperationTextValue());
//					String soapIn = generateSampleSOAP(output);
//					bodyText.setText(soapIn);
//					urlCombo.setText(endpointURL);
//					actionText.setText(actionURL);
//					setControlsForWSType(wsTypeCombo.getText());
//					setControlsForMethodType(methodCombo.getText());
//					setControlsForSelectedURL();
//				}
//			}
//			public void widgetDefaultSelected(SelectionEvent e) {
//				widgetSelected(e);
//			}
//		});
//
//		addTCPIPMonitorButton = new Button(buttonBar, SWT.PUSH);
//		addTCPIPMonitorButton.setText(JBossWSUIMessages.JAXRSWSTestView_Configure_Monitor_Button);
//
//		addTCPIPMonitorButton.addSelectionListener(new SelectionListener() {
//			public void widgetSelected(SelectionEvent e) {
//				configureMonitor();
//			}
//			public void widgetDefaultSelected(SelectionEvent e) {
//				widgetSelected(e);
//			}
//		});
//
//		openTCPIPMonitorButton = new Button(buttonBar, SWT.PUSH);
//		openTCPIPMonitorButton.setText(JBossWSUIMessages.JAXRSWSTestView_Open_Monitor_Button);
//
//		openTCPIPMonitorButton.addSelectionListener(new SelectionListener() {
//			public void widgetSelected(SelectionEvent e) {
//				openMonitor();
//			}
//			public void widgetDefaultSelected(SelectionEvent e) {
//				widgetSelected(e);
//			}
//		});
//
//		Button sampleButton = new Button(buttonBar, SWT.PUSH);
//		sampleButton.setText(JBossWSUIMessages.JAXRSWSTestView_Set_Sample_Data_Label);
//		sampleButton.setVisible(showSampleButton);
//
//		sampleButton.addSelectionListener(new SelectionListener() {
//			public void widgetSelected(SelectionEvent e) {
//				setupSample();
//			}
//			public void widgetDefaultSelected(SelectionEvent e) {
//				widgetSelected(e);
//			}
//		});
//
//		Composite bottomHalf = new Composite (sashForm, SWT.NONE);
//		bottomHalf.setLayout(new GridLayout(2, false));
//
//		resultTabGroup = new TabFolder(bottomHalf, SWT.BORDER);
//		GridData rtGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
//		rtGridData.horizontalSpan = 2;
//		resultTabGroup.setLayoutData(rtGridData);
//
//		resultTab = new TabItem(resultTabGroup, SWT.NONE, 0);
//		resultTab.setText(JBossWSUIMessages.JAXRSWSTestView_Results_Body_Label);
//		resultsText = new Text(resultTabGroup, SWT.BORDER | SWT.V_SCROLL | SWT.WRAP | SWT.READ_ONLY );
//		resultsText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
//
//		resultsTextMenu = new Menu(resultsText.getShell(), SWT.POP_UP);
//		
//		copyMenuAction = new MenuItem(resultsTextMenu, SWT.PUSH);
//		copyMenuAction.setText(JBossWSUIMessages.JAXRSWSTestView_CopyResultsMenu);
//		copyMenuAction.setAccelerator(SWT.CTRL + 'C');
//		copyMenuAction.addSelectionListener(new SelectionListener(){
//
//			public void widgetDefaultSelected(SelectionEvent arg0) {
//				if (resultsText.getSelectionCount() == 0)
//					resultsText.selectAll();
//				resultsText.copy();
//			}
//
//			public void widgetSelected(SelectionEvent arg0) {
//				widgetDefaultSelected(arg0);
//			}
//		});
//		new MenuItem(resultsTextMenu, SWT.SEPARATOR);
//		
//		openInXMLEditorAction = new MenuItem(resultsTextMenu, SWT.PUSH);
//		openInXMLEditorAction.setText(JBossWSUIMessages.JAXRSWSTestView_Open_Result_in_XML_Editor);
//		openInXMLEditorAction.setAccelerator(SWT.CTRL + 'O');
//		openInXMLEditorAction.addSelectionListener(new SelectionListener() {
//
//			public void widgetSelected(SelectionEvent arg0) {
//				String string = resultsText.getText();
//				openXMLEditor(string);
//			}
//
//			public void widgetDefaultSelected(SelectionEvent arg0) {
//				widgetSelected(arg0);
//			}
//		});
//
//		openResponseTagInXMLEditor = new MenuItem(resultsTextMenu, SWT.PUSH);
//		openResponseTagInXMLEditor.setText(JBossWSUIMessages.JAXRSWSTestView_Open_Response_Tag_Contents_in_XML_Editor);
//		openResponseTagInXMLEditor.setAccelerator(SWT.CTRL + 'R');
//		openResponseTagInXMLEditor.addSelectionListener(new SelectionListener() {
//
//			public void widgetSelected(SelectionEvent arg0) {
//				String string = null;
//				try {
//					SOAPBody body = null;
//					if (envelope != null){
//						body = envelope.getBody();
//					} else if (soapbody != null) {
//						body = soapbody;
//					}
//
//					NodeList list = body.getChildNodes();
//					for (int i = 0; i< list.getLength(); i++){
//						Node node = list.item(i);
//						if (node.getNodeName().contains("Response")){ //$NON-NLS-1$
//							NodeList list2 = node.getChildNodes();
//							for (int j = 0; j<list2.getLength(); j++){
//								Node node2 = list2.item(j);
//								if (node2.getNodeName().contains("Result")){ //$NON-NLS-1$
//									Node node3 = node2.getChildNodes().item(0);
//									if (node3.getNodeType() == Node.TEXT_NODE) {
//										string = node3.getNodeValue();
//										break;
//									} else if (node3.getNodeType() == Node.ELEMENT_NODE) {
//										Element element = (Element) node3;
//										string = XMLUtils.ElementToString(element);
//										break;
//									}
//								}
//							}
//							if (string != null) break;
//						}
//					}
//					if (string != null){
//						openXMLEditor(string);
//					}
//				} catch (SOAPException e) {
//					JBossWSUIPlugin.log(e);
//				}
//			}
//
//			public void widgetDefaultSelected(SelectionEvent arg0) {
//				widgetSelected(arg0);
//			}
//		});
//		
//		resultsText.setMenu(resultsTextMenu);		
//
//		resultTab.setControl(resultsText);
//		
//		resultsText.addFocusListener(new FocusListener() {
//			
//			public void focusLost(FocusEvent arg0) {
//			}
//			
//			public void focusGained(FocusEvent arg0) {
//				setMenusForCurrentState();
//			}
//		});
//
//		resultHeadersTab = new TabItem(resultTabGroup, SWT.NONE, 1);
//		resultHeadersTab.setText(JBossWSUIMessages.JAXRSWSTestView_Results_Header_Label);
//		resultHeadersList = new List(resultTabGroup, SWT.V_SCROLL);
//		resultHeadersTab.setControl(resultHeadersList);
//		GridData rdlsListGD = new GridData(SWT.FILL, SWT.FILL, true, true);
//		rdlsListGD.horizontalSpan = 2;
//		resultHeadersList.setLayoutData(dlsListGD);
//
//		resultsHeaderMenu = new Menu(resultHeadersList.getShell(), SWT.POP_UP);
//		
//		copyResultHeaderMenuAction = new MenuItem(resultsHeaderMenu, SWT.PUSH);
//		copyResultHeaderMenuAction.setText(JBossWSUIMessages.JAXRSWSTestView_CopyResultMenu_Text);
//		copyResultHeaderMenuAction.setAccelerator(SWT.CTRL + 'C');
//		copyResultHeaderMenuAction.addSelectionListener(new SelectionListener(){
//
//			public void widgetDefaultSelected(SelectionEvent arg0) {
//				if (resultHeadersList.getSelectionCount() == 0)
//					resultHeadersList.selectAll();
//				Display display = Display.getDefault();
//				final Clipboard cb = new Clipboard(display);
//				TextTransfer textTransfer = TextTransfer.getInstance();
//		        cb.setContents(resultHeadersList.getSelection() ,
//		            new Transfer[] { textTransfer });
//			}
//
//			public void widgetSelected(SelectionEvent arg0) {
//				widgetDefaultSelected(arg0);
//			}
//		});
//		
//		resultHeadersList.setMenu(resultsHeaderMenu);
//		
//		resultHeadersList.addMouseListener(new MouseListener() {
//			public void mouseDoubleClick(MouseEvent arg0) {
//			}
//			public void mouseDown(MouseEvent arg0) {
//				setMenusForCurrentState();
//			}
//			public void mouseUp(MouseEvent arg0) {
//			}
//		});
//
//		wsTypeCombo.setText(JAX_WS);
//		setControlsForWSType(wsTypeCombo.getText());
//		setControlsForMethodType(methodCombo.getText());
//		setControlsForSelectedURL();
//		setMenusForCurrentState();
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
			if (wsTypeCombo.getText().equalsIgnoreCase(JAX_WS)) {
				openResponseTagInXMLEditor.setEnabled(enabled);
			} else if (wsTypeCombo.getText().equalsIgnoreCase(JAX_RS) ){
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
				if (window.getWorkbench().getEditorRegistry().findEditor(XML_EDITOR_ID) != null) {
					page.openEditor(input, XML_EDITOR_ID);
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
				testButton.setEnabled(true);
				addTCPIPMonitorButton.setEnabled(true);
			} catch (MalformedURLException mue) {
				testButton.setEnabled(false);
				addTCPIPMonitorButton.setEnabled(false);

				return;
			}
		} else {
			testButton.setEnabled(false);
			addTCPIPMonitorButton.setEnabled(false);
		}
	}

	/*
	 * Open the TCP/IP Monitor View 
	 */
	private void openMonitor() {
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().
			getActivePage().showView(TCPIP_VIEW_ID);
		} catch (PartInitException e) {
			JBossWSUIPlugin.log(e);
		}
	}

	private IMonitor findMonitor(String urlToCheck) {
		IMonitor monitor = null;

		IMonitor[] monitors = MonitorCore.getMonitors();
		if (monitors != null && monitors.length > 0) {
			for (int i= 0; i < monitors.length; i++) {
				if (urlToCheck.contains(monitors[i].getRemoteHost())) {
					monitor = monitors[i];
					break;
				}
			}
		}
		return monitor;
	}

	/*
	 * Configure a TCP/IP Monitor entry so we can monitor it 
	 */
	private void configureMonitor() {
		if (urlCombo.getText().trim().length() > 0) {
			String oldUrl = urlCombo.getText();
			IMonitor monitor = findMonitor(oldUrl);
			if (monitor == null) {

				URL tempURL = null;
				try {
					tempURL = new URL(oldUrl);
				} catch (MalformedURLException e) {
					// ignore
				}
				AddMonitorDialog dialog = new AddMonitorDialog(getSite().getShell());
				if (tempURL != null) {
					dialog.getMonitor().setRemoteHost(tempURL.getHost());
					if (tempURL.getPort() > 0) 
						dialog.getMonitor().setRemotePort(tempURL.getPort());
				}
				if (dialog.open() == Window.CANCEL)
					return;
				monitor = dialog.getMonitor();
			}

			if (monitor != null) {
				monitor = findMonitor(oldUrl);
				if (monitor != null) {
					if (!monitor.isRunning()) {
						try {
							monitor.start();
							int port = monitor.getLocalPort();
							int remotePort = monitor.getRemotePort();
							String host = monitor.getRemoteHost();
							String newUrl = null;
							if (oldUrl.contains(host + ':' + remotePort)) {
								newUrl = oldUrl.replace(host + ':' + remotePort, "localhost:" + port); //$NON-NLS-1$
							} else if (oldUrl.contains(host + ':' + port)) {
								// do nothing - host/port combo is already correct
								newUrl = oldUrl;
							} else {
								newUrl = oldUrl.replace(host, "localhost:" + port); //$NON-NLS-1$
							}
							urlCombo.setText(newUrl);
						} catch (CoreException e) {
							// if we hit an error, open a dialog
							ErrorDialog dialog = new ErrorDialog(this.getSite().getShell(), 
									JBossWSUIMessages.JAXRSWSTestView_Error_Title_Starting_Monitor,
									JBossWSUIMessages.JAXRSWSTestView_Error_Msg_Starting_Monitor,
									new Status(IStatus.ERROR, JBossWSUIPlugin.PLUGIN_ID, 
									e.getLocalizedMessage(), e), IStatus.ERROR);
							dialog.open();
						}
					}
				}
			}
		}		
	}

	/*
	 * Enable/disable controls based on the WS technology type
	 * and the method.
	 * 
	 * @param methodType
	 */
	private void setControlsForMethodType ( String methodType ) {
		if (wsTypeCombo.getText().equalsIgnoreCase(JAX_RS) &&
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
			actionText.setEnabled(true);
			bodyText.setEnabled(true);
			methodCombo.setEnabled(false);
			parmsList.setEnabled(false);
			parmsList.removeAll();
			dlsList.setEnabled(false);
			parmsTab.getControl().setEnabled(false);
			headerTab.getControl().setEnabled(true);
			methodCombo.setText(POST);

			String emptySOAP = "<?xml version=\"1.0\" standalone=\"yes\" ?>" + //$NON-NLS-1$
			"<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" " + //$NON-NLS-1$
			"xmlns:ns=\"INSERT_URL_HERE\">" + //$NON-NLS-1$
			"<soap:Body>" + //$NON-NLS-1$
			"</soap:Body>" + //$NON-NLS-1$
			"</soap:Envelope>";	 //$NON-NLS-1$
			emptySOAP = WSTestUtils.addNLsToXML(emptySOAP);

			if (bodyText.getText().trim().length() == 0) {
				bodyText.setText(emptySOAP);
			}
			wsdlButton.setEnabled(true);
		}
		else if (wsType.equalsIgnoreCase(JAX_RS)) {
			actionText.setEnabled(false);
			bodyText.setEnabled(true);
			methodCombo.setEnabled(true);
			parmsList.setEnabled(true);
			dlsList.setEnabled(true);
			parmsTab.getControl().setEnabled(true);
			headerTab.getControl().setEnabled(true);
			methodCombo.setText(GET);
			wsdlButton.setEnabled(false);

			if (bodyText.getText().trim().length() > 0) {
				bodyText.setText(""); //$NON-NLS-1$
			}
		}
		setMenusForCurrentState();
	}

	/*
	 * Sets up the controls to call a public sample RESTful WS that does
	 * a postal code lookup or a JAX-WS service that does a 
	 * Shakespeare lookup. 
	 */
	private void setupSample() {
		// go to http://www.geonames.org/export/web-services.html for example
		//http://ws.geonames.org/postalCodeSearch?postalcode=9011&maxRows=10
		if (wsTypeCombo.getText().equalsIgnoreCase(JAX_RS)) {
			urlCombo.setText("http://ws.geonames.org/postalCodeSearch?"); //$NON-NLS-1$
			parmsList.setSelection("postalcode=80920,maxRows=10"); //$NON-NLS-1$
			dlsList.setSelection("content-type=application/xml"); //$NON-NLS-1$
			methodCombo.setText(GET);
			tabGroup.setSelection(parmsTab);
			bodyText.setText(EMPTY_STRING);
		}
		else if (wsTypeCombo.getText().equalsIgnoreCase(JAX_WS)) {
			String soapIn = "<?xml version=\"1.0\" standalone=\"yes\" ?>" + //$NON-NLS-1$
			"<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" " + //$NON-NLS-1$
			"xmlns:ns=\"http://xmlme.com/WebServices\">" + //$NON-NLS-1$
			"<soap:Body>" + //$NON-NLS-1$
			"<ns:GetSpeech>" + //$NON-NLS-1$
			"<ns:Request>slings and arrows</ns:Request>"+ //$NON-NLS-1$
			"</ns:GetSpeech>"+ //$NON-NLS-1$
			"</soap:Body>" + //$NON-NLS-1$
			"</soap:Envelope>";	 //$NON-NLS-1$
			soapIn = WSTestUtils.addNLsToXML(soapIn);

			urlCombo.setText("http://www.xmlme.com/WSShakespeare.asmx"); //$NON-NLS-1$
			actionText.setText("http://xmlme.com/WebServices/GetSpeech"); //$NON-NLS-1$
			bodyText.setText(soapIn);
			parmsList.setSelection(EMPTY_STRING);
			dlsList.setSelection(EMPTY_STRING);
			tabGroup.setSelection(bodyTab);
		}
		setControlsForWSType(wsTypeCombo.getText());
		setControlsForMethodType(methodCombo.getText());
		setControlsForSelectedURL();
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
		final String action = actionText.getText();
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
							if (status.getResultsText() != null)
								resultsText.setText(status.getResultsText());
							else if (status.getMessage() != null) 
								resultsText.setText(status.getMessage());
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
//			JAXWSTester tester = new JAXWSTester();
//			tester.doTest(url, action, body);
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
//			tester.doTest(url, action, serviceNSMessage[0], serviceNSMessage[1], serviceNSMessage[2], body);
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
//		urlCombo.setFocus();
		form.setFocus();
	}

}