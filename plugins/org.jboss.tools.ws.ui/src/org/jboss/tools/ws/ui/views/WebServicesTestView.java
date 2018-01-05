/******************************************************************************* 
 * Copyright (c) 2011 - 2014 Red Hat, Inc. and others.
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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.ScrolledPageBook;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.ViewPart;
import org.jboss.tools.ws.ui.JBossWSUIPlugin;
import org.jboss.tools.ws.ui.messages.JBossWSUIMessages;
import org.jboss.tools.ws.ui.utils.ResultsXMLStorage;
import org.jboss.tools.ws.ui.utils.ResultsXMLStorageInput;
import org.jboss.tools.ws.ui.utils.XMLParser;
import org.jboss.tools.ws.ui.utils.WSTestUtils;

/**
 * View for testing web services
 * 
 * @author bfitzpat
 * @since 2.0
 *
 */
public class WebServicesTestView extends ViewPart {

	private static final String PAGE1_KEY = "page1"; //$NON-NLS-1$
	private static final String PAGE2_KEY = "page2"; //$NON-NLS-1$
	private static final String EMPTY_STRING = ""; //$NON-NLS-1$
	private static final String HTTPS_STRING = "https";//$NON-NLS-1$

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "org.jboss.tools.ws.ui.tester.views.TestWSView";//$NON-NLS-1$
	private static final String DEFAULT_TEXT_EDITOR_ID = "org.eclipse.ui.DefaultTextEditor"; //$NON-NLS-1$
	private static final String XML_EDITOR_ID = "org.eclipse.wst.xml.ui.internal.tabletree.XMLMultiPageEditorPart"; //$NON-NLS-1$

	/* UI controls */
	private Text resultsText;
	private Browser resultsBrowser;
	private Combo urlCombo;
	private ComboViewer methodComboViewer;
	private ListViewer resultHeadersListViewer;
	private Composite additionalDetailsComposite;
	private Section requestSection;
	private Button useBasicAuthCB;
	private FormToolkit toolkit;
	private ScrolledForm form;
	private ToolBarManager toolBarManager;

	private MenuItem openInXMLEditorAction;
	private Menu resultsTextMenu;
	private MenuItem copyMenuAction;
	private Menu resultsHeaderMenu;
	private MenuItem copyResultHeaderMenuAction;

	private Action startAction;

	private TestHistory history = new TestHistory();
	private TestEntry currentEntry;

	private java.util.List<WSType> wsTypes;

	/**
	 * The constructor.
	 */
	public WebServicesTestView() {
		wsTypes = JBossWSUIPlugin.getSupportedWSTypes();
		for (WSType ws : wsTypes) {
			ws.setWebServicesView(this);
		}
	}

	/**
	 * This is a callback that will allow us to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
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
		GridLayout layout = GridLayoutFactory.fillDefaults().create();
		layout.verticalSpacing = 1;
		GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
		form.getBody().setLayout(layout);
		form.getBody().setLayoutData(gridData);

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

		methodComboViewer.setSelection(new StructuredSelection(wsTypes.get(0)));

		updateControlsForCurrentURL();
		updateControlsForCurrentWSType();
		updateMenuForResponse();

	}

	public String getURL() {
		return urlCombo.getText();
	}

	public void setURL(String url) {
		urlCombo.setText(url);
	}

	public void setType(String url, String type) {
		for (WSType wsType : wsTypes) {
			if (wsType.getType().equals(type)) {
				methodComboViewer.setSelection(new StructuredSelection(wsType));
				getCurrentEntry().setWsTech(wsType);
				break;
			}
		}
		urlCombo.setText(url);
		getCurrentEntry().setCustomEntry(null);
		getCurrentEntry().setResult(null);
		getCurrentEntry().setResultHeaders(null);
		getCurrentEntry().setUrl(url);
		updateControlsForCurrentURL();
		updateControlsForCurrentWSType();
		updateMenuForResponse();
	}

	public void redrawRequestDetails() {
		form.setRedraw(false);
		form.reflow(true);
		form.layout(true, true);
		form.setRedraw(true);
	}

	public FormToolkit getToolkit() {
		return toolkit;
	}

	private void createResponseToolbar(ScrolledPageBook pageBook, ExpandableComposite parent) {
		ShowRawAction rawAction = new ShowRawAction(pageBook, PAGE1_KEY);
		rawAction.setChecked(true);
		ShowInBrowserAction browserAction = new ShowInBrowserAction(pageBook);

		rawAction.addPropertyChangeListener(new IPropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent arg0) {
				if (rawAction.isChecked() && browserAction.isChecked()) {
					browserAction.setChecked(false);
				}

			}
		});

		browserAction.addPropertyChangeListener(new IPropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent arg0) {
				if (browserAction.isChecked() && rawAction.isChecked()) {
					rawAction.setChecked(false);
				}

			}
		});

		ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
		ToolBar toolbar = toolBarManager.createControl(parent);

		toolBarManager.add(new FileSaveAction());
		toolBarManager.add(new OpenInXMLEditorAction());
		toolBarManager.add(rawAction);
		toolBarManager.add(browserAction);

		toolBarManager.update(true);

		parent.setTextClient(toolbar);
	}

	private WSType getCurrentTestType() {
		if (methodComboViewer != null && methodComboViewer.getSelection() != null) {
			return (WSType) ((IStructuredSelection) methodComboViewer.getSelection()).getFirstElement();
		}
		return null;
	}

	private void createURLAndToolbar() {
		Composite urlAndToolComposite = new Composite(form.getBody(), SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(urlAndToolComposite);
		GridData comp1data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		urlAndToolComposite.setLayoutData(comp1data);

		urlCombo = new Combo(urlAndToolComposite, SWT.BORDER | SWT.DROP_DOWN);
		GridData gdURL = new GridData(SWT.FILL, SWT.CENTER, true, false);
		urlCombo.setLayoutData(gdURL);

		urlCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (e.getSource().equals(urlCombo)) {
					String newURL = urlCombo.getText();
					updateControlsForCurrentURL();
					getCurrentEntry().setUrl(newURL);
				}
			}
		});
		urlCombo.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
			}

			public void keyReleased(KeyEvent e) {
				updateControlsForCurrentURL();
				getCurrentEntry().setUrl(urlCombo.getText());
				if (e.keyCode == SWT.CR && startAction.isEnabled()) {
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
				TestEntry entry = history.findEntryByURL(testURL);
				if (entry != null) {
					try {
						currentEntry = (TestEntry) entry.clone();
						methodComboViewer.setSelection(new StructuredSelection(currentEntry.getWsTech()));
						updateControlsForSelectedEntry(entry);
					} catch (CloneNotSupportedException e1) {
						e1.printStackTrace();
					}
				} else {
					getCurrentEntry().setUrl(urlCombo.getText());
					updateControlsForCurrentURL();
				}
			}
		});

		Combo methodCombo = new Combo(urlAndToolComposite, SWT.BORDER | SWT.READ_ONLY);
		methodCombo.setBackground(form.getBody().getBackground());
		methodComboViewer = new ComboViewer(methodCombo);
		methodComboViewer.setContentProvider(ArrayContentProvider.getInstance());

		methodComboViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof WSType) {
					return ((WSType) element).getType();
				}
				return null;
			}
		});
		methodComboViewer.setInput(wsTypes);

		methodComboViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent e) {
				updateControlsForCurrentWSType();
				java.util.List<IAction> actions = ((WSType) e.getStructuredSelection().getFirstElement())
						.getAdditonalToolActions();

				updateToolBar(actions);
			}
		});

		ToolBar toolbar = new ToolBar(urlAndToolComposite, SWT.FLAT);

		toolBarManager = new ToolBarManager(toolbar);

		if (getCurrentTestType() != null) {
			java.util.List<IAction> actions = getCurrentTestType().getAdditonalToolActions();
			for (IAction action : actions) {
				toolBarManager.add(action);
			}
			toolBarManager.update(true);
		}

		startAction = new Action() {

			@Override
			public void run() {
				handleTest(getCurrentTestType());
			}

			@Override
			public String getToolTipText() {
				return JBossWSUIMessages.JAXRSWSTestView2_Go_Tooltip;
			}

			@Override
			public ImageDescriptor getImageDescriptor() {
				return JBossWSUIPlugin.getImageDescriptor(JBossWSUIPlugin.IMG_DESC_START);
			}
		};
		updateToolBar(null);
		toolkit.adapt(urlAndToolComposite);

	}

	private void updateToolBar(java.util.List<IAction> additionalActions) {
		toolBarManager.removeAll();
		if (additionalActions != null) {
			for (IAction action : additionalActions) {
				toolBarManager.add(action);
			}
		}
		toolBarManager.add(startAction);
		toolBarManager.update(true);
	}

	private void createRequestSide(SashForm sashForm) {
		requestSection = toolkit.createSection(sashForm, Section.TITLE_BAR | Section.TWISTIE | Section.EXPANDED);
		requestSection.setText(JBossWSUIMessages.JAXRSWSTestView2_RequestDetails_Section);

		Composite sectionComposite = toolkit.createComposite(requestSection);
		sectionComposite.setLayout(new GridLayout());
		sectionComposite.setLayoutData(new GridData());

		useBasicAuthCB = toolkit.createButton(sectionComposite,
				JBossWSUIMessages.JAXRSWSTestView2_Checkbox_Basic_Authentication, SWT.CHECK);
		GridData gd10 = new GridData(SWT.FILL, SWT.NONE, true, false);
		gd10.horizontalIndent = 3;
		useBasicAuthCB.setLayoutData(gd10);

		additionalDetailsComposite = new Composite(sectionComposite, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(additionalDetailsComposite);
		GridData detailsData = new GridData(SWT.FILL, SWT.NONE, true, false);
		additionalDetailsComposite.setLayoutData(detailsData);

		requestSection.addExpansionListener(new FormExpansionAdapter(this));
		requestSection.setClient(sectionComposite);
	}

	public TestEntry getCurrentEntry() {
		if (this.currentEntry == null) {
			this.currentEntry = new TestEntry();
		}
		return this.currentEntry;
	}

	private void createResponseSide(SashForm sashForm) {
		Section section2 = toolkit.createSection(sashForm, Section.TITLE_BAR | Section.TWISTIE | Section.EXPANDED);
		section2.setText(JBossWSUIMessages.JAXRSWSTestView2_ResponseDetails_Section);

		Composite sectionClient2 = toolkit.createComposite(section2);
		sectionClient2.setLayout(new GridLayout());
		sectionClient2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		ExpandableComposite ec2 = toolkit.createExpandableComposite(sectionClient2,
				ExpandableComposite.TREE_NODE | ExpandableComposite.TITLE_BAR | ExpandableComposite.CLIENT_INDENT);
		ec2.setText(JBossWSUIMessages.JAXRSWSTestView2_ResponseHeaders_Section);
		ec2.setLayout(new GridLayout());
		List resultHeadersList = new List(ec2, SWT.V_SCROLL | SWT.BORDER);
		resultHeadersListViewer = new ListViewer(resultHeadersList);
		resultHeadersListViewer.setContentProvider(new ArrayContentProvider());
		resultHeadersListViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				WSProperty rProperty = (WSProperty) element;
				return rProperty.toString();
			}
		});
		ec2.setClient(resultHeadersList);
		resultsHeaderMenu = new Menu(resultHeadersList.getShell(), SWT.POP_UP);

		copyResultHeaderMenuAction = new MenuItem(resultsHeaderMenu, SWT.PUSH);
		copyResultHeaderMenuAction.setText(JBossWSUIMessages.JAXRSWSTestView_CopyResultMenu_Text);
		copyResultHeaderMenuAction.setAccelerator(SWT.CTRL + 'C');
		copyResultHeaderMenuAction.addSelectionListener(new SelectionListener() {

			public void widgetDefaultSelected(SelectionEvent arg0) {
				if (resultHeadersList.getSelectionCount() == 0)
					resultHeadersList.selectAll();
				Display display = Display.getDefault();
				final Clipboard cb = new Clipboard(display);
				TextTransfer textTransfer = TextTransfer.getInstance();
				cb.setContents(resultHeadersList.getSelection(), new Transfer[] { textTransfer });
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
				updateMenuForResponse();
			}

			public void mouseUp(MouseEvent arg0) {
			}
		});

		GridData gd6 = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd6.heightHint = 1;
		gd6.minimumHeight = 50;
		ec2.setLayoutData(gd6);
		ec2.addExpansionListener(new FormExpansionAdapter(this));

		ExpandableComposite ec4 = toolkit.createExpandableComposite(sectionClient2, ExpandableComposite.TWISTIE
				| ExpandableComposite.TITLE_BAR | ExpandableComposite.CLIENT_INDENT | ExpandableComposite.EXPANDED);
		ec4.setText(JBossWSUIMessages.JAXRSWSTestView2_ResponseBody_Section);

		ScrolledPageBook pageBook = toolkit.createPageBook(ec4, SWT.NONE);

		createResponseToolbar(pageBook, ec4);

		Composite page1 = pageBook.createPage(PAGE1_KEY);
		page1.setLayout(new GridLayout());
		resultsText = toolkit.createText(page1, EMPTY_STRING, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		GridData gd7 = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd7.heightHint = 1;
		resultsText.setLayoutData(gd7);

		pageBook.showPage(PAGE1_KEY);

		Composite page2 = pageBook.createPage(PAGE2_KEY);
		page2.setLayout(new GridLayout());
		resultsBrowser = new Browser(page2, SWT.BORDER | SWT.WRAP);
		GridData gd10 = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd10.heightHint = 1;
		toolkit.adapt(resultsBrowser);
		resultsBrowser.setLayoutData(gd10);

		pageBook.showPage(PAGE2_KEY);

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = 1;
		gd.minimumHeight = 100;
		pageBook.setLayoutData(gd);

		pageBook.showPage(PAGE1_KEY);

		resultsTextMenu = new Menu(resultsText.getShell(), SWT.POP_UP);

		copyMenuAction = new MenuItem(resultsTextMenu, SWT.PUSH);
		copyMenuAction.setText(JBossWSUIMessages.JAXRSWSTestView_CopyResultsMenu);
		copyMenuAction.setAccelerator(SWT.CTRL + 'C');
		copyMenuAction.addSelectionListener(new SelectionListener() {

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

		resultsText.setMenu(resultsTextMenu);

		resultsText.addFocusListener(new FocusListener() {

			public void focusLost(FocusEvent arg0) {
			}

			public void focusGained(FocusEvent arg0) {
				updateMenuForResponse();
			}
		});

		ec4.setClient(pageBook);
		GridData gd8 = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd8.heightHint = 1;
		ec4.setLayoutData(gd8);
		ec4.addExpansionListener(new FormExpansionAdapter(this));

		section2.addExpansionListener(new FormExpansionAdapter(this));
		section2.setClient(sectionClient2);
	}

	private void updateMenuForResponse() {
		if (resultsText != null && !resultsText.isDisposed()) {
			boolean enabled = resultsText.getText().trim().length() > 0;
			copyMenuAction.setEnabled(enabled);
			openInXMLEditorAction.setEnabled(enabled);
		}
		if (resultHeadersListViewer != null && !resultHeadersListViewer.getControl().isDisposed()) {
			boolean enabled = resultHeadersListViewer.getList().getItemCount() > 0;
			copyResultHeaderMenuAction.setEnabled(enabled);
		}
	}

	private void updateControlsForSelectedEntry(TestEntry entry) {
		if (entry != null) {
			if (entry.getWsTech() != null) {
				methodComboViewer.setSelection(new StructuredSelection(entry.getWsTech()));
				entry.getWsTech().updateControlsForSelectedEntry(entry);
			}
			if (resultsText.isEnabled() && resultsBrowser.isEnabled()) {
				resultsText.setText(entry.getResult());
				resultsBrowser.setText(entry.getResult());
			}
			if (entry.getUrl().trim().length() > 0) {
				String urlText = entry.getUrl();
				try {
					new URL(urlText);
					startAction.setEnabled(true);
				} catch (MalformedURLException mue) {
					startAction.setEnabled(false);
					return;
				}
			} else {
				startAction.setEnabled(false);
			}
		}
	}

	private void updateControlsForCurrentURL() {
		if (urlCombo.getText().trim().length() > 0) {
			String urlText = urlCombo.getText();
			try {
				new URL(urlText);
				startAction.setEnabled(true);

				// per JBIDE-6919, if we encounter an "https" url make sure
				// basic authorization checkbox is checked
				// per JBIDE-12981, only set this when the user updates
				// the url, not every time they run the test
				if (urlText.trim().startsWith(HTTPS_STRING)) {
					useBasicAuthCB.setSelection(true);
				}
			} catch (MalformedURLException mue) {
				startAction.setEnabled(false);
				return;
			}
		} else {
			startAction.setEnabled(false);
		}
	}

	private void updateControlsForCurrentWSType() {
		for (Control c : additionalDetailsComposite.getChildren()) {
			c.dispose();
		}
		WSType type = getCurrentTestType();
		if (type != null) {
			type.fillAdditionalRequestDetails(additionalDetailsComposite);
		}
		additionalDetailsComposite.layout();
		requestSection.layout();

	}

	/*
	 * Actually perform the test based on which type of activity it is
	 */
	private void handleTest(final WSType wsTech) {
		String urlText = urlCombo.getText();
		try {
			new URL(urlText);
		} catch (MalformedURLException mue) {
			// do nothing, but return since we don't have a working URL
			return;
		}

		// If basic authorization checkbox is checked, use the uid/pwd
		String tempUID = null;
		String tempPwd = null;
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
		startAction.setEnabled(false);

		Job aJob = new Job(JBossWSUIMessages.JAXRSWSTestView_Invoking_WS_Status) {
			protected IStatus run(IProgressMonitor monitor) {
				IStatus status = Status.OK_STATUS;
				status = wsTech.handleWSTest(monitor, urlText, uid, pwd);
				monitor.done();
				return status;
			}
		};
		aJob.setUser(true);
		aJob.addJobChangeListener(new JobChangeAdapter() {

			public void done(final IJobChangeEvent event) {
				try {
					if (event.getResult() instanceof WSTestStatus) {
						PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
							public void run() {
								TestEntry historyEntry = getCurrentEntry();
								WSTestStatus status = (WSTestStatus) event.getResult();
								if (status.getResultsText() != null) {
									String results = status.getResultsText();
									if (XMLParser.isValidXML(results)) {
										results = XMLParser.prettyPrint(results);
									} else {
										results = XMLParser.prettyPrintJSON(results);
									}
									historyEntry.setResult(results);
									historyEntry.setUrl(urlCombo.getText());
									historyEntry.setWsTech(
											(WSType) methodComboViewer.getStructuredSelection().getFirstElement());
									resultsText.setText(results);
									resultsBrowser.setText(results);
									form.reflow(true);
								} else if (status.getMessage() != null) {
									historyEntry.setResult(status.getMessage());
									historyEntry.setUrl(urlCombo.getText());
									historyEntry.setWsTech(
											(WSType) methodComboViewer.getStructuredSelection().getFirstElement());
									resultsText.setText(status.getMessage());
									resultsBrowser.setText(status.getMessage());
									form.reflow(true);
								}
								Map<String, java.util.List<String>> headersMap = status.getHeaders();
								Set<WSProperty> headers = new LinkedHashSet<>();
								if (headersMap != null) {
									for (String key : headersMap.keySet()) {
										Object obj = headersMap.get(key);
										String objStringValue = ""; //$NON-NLS-1$
										if (obj != null) {
											objStringValue = obj.toString();
										}
										headers.add(new WSProperty(key, objStringValue));
									}
								}
								resultHeadersListViewer.setInput(headers);
								historyEntry.setResultHeaders(headers);
								TestEntry oldEntry = history.findEntryByURL(getCurrentEntry().getUrl());
								if (oldEntry != null) {
									history.replaceEntry(oldEntry, getCurrentEntry());
								} else {
									try {
										history.addEntry(getCurrentEntry().clone());
									} catch (CloneNotSupportedException e) {
										e.printStackTrace();
									}
								}
							}
						});
					}
				} finally {
					PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
						public void run() {
							startAction.setEnabled(true);
							updateMenuForResponse();

							if (urlCombo.getItemCount() > 0) {
								java.util.List<String> aList = Arrays.asList(urlCombo.getItems());
								if (aList.contains(urlCombo.getText())) {
									return;
								}
							}
							urlCombo.add(urlCombo.getText());
						}
					});
				}
			}
		});
		aJob.schedule();
	}

	/**
	 * Passing the focus request to the control.
	 */
	public void setFocus() {
		// set initial focus to the URL text combo
		urlCombo.setFocus();
	}

	@Override
	public void dispose() {
		toolkit.dispose();
		super.dispose();
	}

	private void openXMLEditor(String text) {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IStorage storage = new ResultsXMLStorage(text);
		IStorageEditorInput input = new ResultsXMLStorageInput(storage);
		IWorkbenchPage page = window.getActivePage();
		if (page != null) {
			try {
				if (WSTestUtils.isTextXML(text)) {
					if (PlatformUI.getWorkbench().getEditorRegistry().findEditor(XML_EDITOR_ID) != null) {
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

	class FileSaveAction extends Action {

		@Override
		public void run() {
			IStatus status = WSTestUtils.saveTextToFile(resultsText.getText());
			if (status.getCode() == IStatus.ERROR) {
				MessageDialog.openError(new Shell(Display.getCurrent()),
						JBossWSUIMessages.JAXRSWSTestView2_SaveResponseText_Error, status.getMessage());
			}
		}

		@Override
		public String getToolTipText() {
			return JBossWSUIMessages.JAXRSWSTestView2_SaveResponseText_tooltip;
		}

		@Override
		public ImageDescriptor getImageDescriptor() {
			return JBossWSUIPlugin.getImageDescriptor(JBossWSUIPlugin.IMG_DESC_SAVE);
		}
	}

	class ShowInBrowserAction extends ToggleAction {

		private ScrolledPageBook pageBook;

		public ShowInBrowserAction(ScrolledPageBook pageBook) {
			this.pageBook = pageBook;
		}

		public void run() {
			pageBook.showPage(PAGE2_KEY);
		}

		@Override
		public String getToolTipText() {
			return JBossWSUIMessages.JAXRSWSTestView2_ShowInBrowser_Tooltip;
		}

		@Override
		public ImageDescriptor getImageDescriptor() {
			return JBossWSUIPlugin.getImageDescriptor(JBossWSUIPlugin.IMG_DESC_SHOWWEB);
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
			return JBossWSUIPlugin.getImageDescriptor(JBossWSUIPlugin.IMG_DESC_SHOWEDITOR);
		}
	}

}