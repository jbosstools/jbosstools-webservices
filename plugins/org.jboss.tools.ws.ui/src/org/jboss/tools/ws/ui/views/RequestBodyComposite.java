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
package org.jboss.tools.ws.ui.views;

import java.util.Stack;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.ScrolledPageBook;
import org.jboss.tools.ws.ui.JBossWSUIPlugin;
import org.jboss.tools.ws.ui.messages.JBossWSUIMessages;
import org.jboss.tools.ws.ui.utils.XMLParser;
import org.jboss.tools.ws.ui.utils.TreeParent;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @since 2.0
 */
public class RequestBodyComposite {
	
	public static final String PAGE1_KEY = "page1"; //$NON-NLS-1$
	public static final String PAGE2_KEY = "page2"; //$NON-NLS-1$
	private static final String[] TREE_COLUMNS = new String[] { "name", "value" }; //$NON-NLS-1$ //$NON-NLS-2$
	
	private Text bodyText;
	private TreeViewer treeRequestBody;
	
	public void createControl(WebServicesTestView view, Composite parent) {
		ExpandableComposite bodyComposite = view.getToolkit().createExpandableComposite(parent,
				ExpandableComposite.TWISTIE | ExpandableComposite.CLIENT_INDENT | ExpandableComposite.EXPANDED);
		bodyComposite.setText(JBossWSUIMessages.JAXRSWSTestView2_BodyText_Section);
		ScrolledPageBook requestPageBook = view.getToolkit().createPageBook(bodyComposite, SWT.NONE);

		createRequestToolbar(requestPageBook, bodyComposite);

		Composite page1 = requestPageBook.createPage(PAGE1_KEY);
		page1.setLayout(new GridLayout());
		bodyText = view.getToolkit().createText(page1, "", SWT.BORDER | SWT.WRAP | SWT.V_SCROLL); //$NON-NLS-1$
		GridData gd7 = new GridData(SWT.FILL, SWT.FILL, true, true);
		// gd7.minimumHeight = 100;
		gd7.heightHint = 1;
		bodyText.setLayoutData(gd7);

		requestPageBook.showPage(PAGE1_KEY);

		Composite page2 = requestPageBook.createPage(PAGE2_KEY);
		page2.setLayout(new GridLayout());
		treeRequestBody = new TreeViewer(page2, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.FULL_SELECTION);
		treeRequestBody.setAutoExpandLevel(TreeViewer.ALL_LEVELS);
		GridData gd11 = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd11.heightHint = 1;
		// gd10.minimumHeight = 100;
		view.getToolkit().adapt(treeRequestBody.getTree());
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
					return ((TreeParent) element).getName();
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

		treeRequestBody.setContentProvider(new ITreeContentProvider() {

			String text;
			TreeParent tree;

			@Override
			public void dispose() {
			}

			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				if (newInput instanceof String) {
					text = (String) newInput;

					XMLParser parser = new XMLParser();
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
					return new Object[] { this.tree };
				} else if (inputElement instanceof TreeParent) {
					return ((TreeParent) inputElement).getChildren();
				}
				return null;
			}

			@Override
			public Object[] getChildren(Object parentElement) {
				if (parentElement == null && tree != null) {
					return new Object[] { this.tree };
				} else if (parentElement instanceof TreeParent && ((TreeParent) parentElement).hasChildren()) {
					return ((TreeParent) parentElement).getChildren();
				}
				return null;
			}

			@Override
			public Object getParent(Object element) {
				if (element instanceof TreeParent) {
					return ((TreeParent) element).getParent();
				}
				return null;
			}

			@Override
			public boolean hasChildren(Object element) {
				if (element instanceof TreeParent) {
					return ((TreeParent) element).hasChildren();
				}
				return false;
			}
		});

		treeRequestBody.setCellModifier(new ICellModifier() {

			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.jface.viewers.ICellModifier#canModify(java.lang.Object,
			 * java.lang.String)
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

			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.jface.viewers.ICellModifier#getValue(java.lang.Object,
			 * java.lang.String)
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

			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.jface.viewers.ICellModifier#modify(java.lang.Object,
			 * java.lang.String, java.lang.Object)
			 */
			public void modify(Object element, String property, Object value) {
				TreeItem ti = (TreeItem) element;
				TreeParent tp = (TreeParent) ti.getData();
				if (tp.getData() != null && tp.getData() instanceof Element) {
					Element tpelement = (Element) tp.getData();
					if (tpelement.getChildNodes() != null && tpelement.getChildNodes().getLength() > 0) {
						Node node = tpelement.getChildNodes().item(0);
						if (node.getNodeType() == Node.TEXT_NODE) {
							node.setTextContent((String) value);
							treeRequestBody.update(tp, null);
							XMLParser parser = new XMLParser();
							String updatedOut = parser.updateValue((String) treeRequestBody.getInput(), tp,
									(String) value);
							if (updatedOut != null && updatedOut.trim().length() > 0) {
								Stack<String> pathStack = new Stack<String>();
								pathStack.push(ti.getText());
								TreeItem tiPath = ti;
								while (tiPath.getParentItem() != null) {
									tiPath = tiPath.getParentItem();
									pathStack.push(tiPath.getText());
								}
								setBodyText(updatedOut);
								treeRequestBody.setAutoExpandLevel(TreeViewer.ALL_LEVELS);
								while (!pathStack.isEmpty()) {
									TreeItem[] items = treeRequestBody.getTree().getItems();
									String find = pathStack.pop();
									/* boolean found = */ findTreeItem(treeRequestBody, find, items);
								}
							}
						}
					}
				}
			}

		});
		treeRequestBody.setCellEditors(new CellEditor[] { null, new TextCellEditor(treeRequestBody.getTree()) });

		requestPageBook.showPage(PAGE1_KEY);

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true); // GridData.FILL_HORIZONTAL);
		gd.heightHint = 1;
		gd.minimumHeight = 100;
		requestPageBook.setLayoutData(gd);

		requestPageBook.showPage(PAGE1_KEY);
		bodyComposite.setClient(requestPageBook);
		GridData gd9 = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd9.minimumHeight = 200;
		bodyComposite.setLayoutData(gd9);
		bodyComposite.addExpansionListener(new FormExpansionAdapter(view));
	}
	
	private void createRequestToolbar(ScrolledPageBook requestPageBook, ExpandableComposite parent) {

		// create a couple of actions for toggling views
		ShowRawAction rawRequestAction = new ShowRawAction(requestPageBook, PAGE1_KEY);
		rawRequestAction.setChecked(true);
		ShowInTreeAction treeAction = new ShowInTreeAction(requestPageBook);

		ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
		ToolBar toolbar = toolBarManager.createControl(parent);

		toolBarManager.add(rawRequestAction);
		toolBarManager.add(treeAction);

		toolBarManager.update(true);

		parent.setTextClient(toolbar);
	}
	
	private boolean findTreeItem(TreeViewer treeRequestBody, String name, TreeItem[] treeItems) {
		for (TreeItem treeItem : treeItems) {
			for (int i = 0; i < treeRequestBody.getTree().getColumnCount(); i++) {
				String text = treeItem.getText(i);
				if ((text.toUpperCase().contains(name.toUpperCase()))) {
					treeRequestBody.getTree().setSelection(treeItem);
					return true;
				}
				if (treeItem.getItemCount() > 0) {
					return findTreeItem(treeRequestBody, name, treeItem.getItems());
				}
			}
		}
		return false;
	}

	public void setBodyText(String text) {
		bodyText.setText(text);
		treeRequestBody.setInput(text);
	}
	
	public String getBodyText() {
		return bodyText.getText();
	}
	
	class ShowInTreeAction extends ToggleAction {
		
		private ScrolledPageBook requestPageBook;
		
		public ShowInTreeAction(ScrolledPageBook requestPageBook) {
			this.requestPageBook =requestPageBook;
		}

		public void run() {
			requestPageBook.showPage(PAGE2_KEY);
		}

		@Override
		public String getToolTipText() {
			return JBossWSUIMessages.JAXRSWSTestView2_ShowRequestTree_toolbar_btn;
		}

		@Override
		public ImageDescriptor getImageDescriptor() {
			return JBossWSUIPlugin.getImageDescriptor(JBossWSUIPlugin.IMG_DESC_SHOWTREE);
		}
	}

}
