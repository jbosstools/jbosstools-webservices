/******************************************************************************* 
 * Copyright (c) 2010 - 2014 Red Hat, Inc. and others.  
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Sybase, Inc./Eclipse - DTP - initial API and implementation 
 * Red Hat, Inc. - added Edit button
 ******************************************************************************/
package org.jboss.tools.ws.jaxrs.ui.view;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.ws.jaxrs.ui.JBossJAXRSUIMessages;
import org.jboss.tools.ws.ui.views.WSProperty;

/**
 * Composite that provides a method of managing a delimited list of strings.
 * 
 * @author brianf
 * @since 2.0
 */
public class PropertiesList extends Composite {

	private static final String DELIMITER = "="; //$NON-NLS-1$

	// ui elements
	private Button addButton;
	private Button removeAllButton;
	private Button removeButton;
	private Button editButton;
	private Text addText;
	private Label warningLabel;

	private ListViewer listViewer;

	private Set<WSProperty> wsProperties = new LinkedHashSet<>();
	private java.util.List<Listener> propertiesListeners = new ArrayList<>();

	// Warning
	private String mWarning = null;
	
	/**
	 * Constructor
	 * 
	 * @param parent
	 * @param style
	 */
	public PropertiesList(Composite parent, int style) {
		super(parent, style);
		int additionalStyles = SWT.NONE;

		final GridLayout gridLayout = new GridLayout();
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		gridLayout.numColumns = 2;
		setLayout(gridLayout);
		warningLabel = new Label(this, SWT.NONE);
		GridData wlGridData = new GridData(GridData.FILL_HORIZONTAL);
		wlGridData.horizontalSpan = 2;
		warningLabel.setLayoutData(wlGridData);
		warningLabel.setForeground(this.warningLabel.getDisplay().getSystemColor(SWT.COLOR_RED));
		warningLabel.setBackground(parent.getBackground());

		addText = new Text(this, SWT.BORDER | additionalStyles);
		addText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		addText.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent arg0) {
				updatePropertyButtons();
				
			}
		});

		addButton = new Button(this, SWT.NONE);
		addButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		addButton.setText("Add");//$NON-NLS-1$
		addButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				addStringToList();
				fireChangedEvent();
			}
		});
		addButton.setEnabled(false);
		final Composite mSpacerComposite = new Composite(this, SWT.NONE);
		final GridData gridData = new GridData();
		gridData.heightHint = 10;
		gridData.horizontalSpan = 2;
		mSpacerComposite.setLayoutData(gridData);
		mSpacerComposite.setLayout(new GridLayout());
		mSpacerComposite.setBackground(parent.getBackground());

		List propsList = new List(this, SWT.BORDER | SWT.V_SCROLL);
		GridData propsListData = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL);
		propsListData.verticalSpan = 3;
		propsList.setLayoutData(propsListData);
		propsList.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				updatePropertyButtons();
			}
		});

		listViewer = new ListViewer(propsList);
		listViewer.setContentProvider(new ArrayContentProvider());
		listViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				WSProperty rProperty = (WSProperty) element;
				return rProperty.toString();
			}
		});

		listViewer.setInput(wsProperties);

		editButton = new Button(this, SWT.NONE);
		editButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		editButton.setText("Edit");//$NON-NLS-1$
		editButton.setEnabled(false);
		editButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				editInList();
				fireChangedEvent();
			}
		});

		removeButton = new Button(this, SWT.NONE);
		removeButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		removeButton.setText("Remove");//$NON-NLS-1$
		removeButton.setEnabled(false);
		removeButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				removeFromList();
				fireChangedEvent();
			}
		});

		removeAllButton = new Button(this, SWT.NONE);
		removeAllButton.setEnabled(false);
		removeAllButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		removeAllButton.setText("Remove All");//$NON-NLS-1$
		removeAllButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				removeAll();
				updatePropertyButtons();
				fireChangedEvent();
			}
		});
		updatePropertyButtons();
	}

	public void removeAll() {
		wsProperties.clear();
		listViewer.refresh();
		updatePropertyButtons();
	}

	/**
	 * @since 2.0
	 */
	public void setProperties(Set<WSProperty> properties) {
		wsProperties = properties;
		listViewer.setInput(wsProperties);
	}

	/**
	 * Add a new string to the list
	 */
	private void addStringToList() {
		if (addText.getText().length() > 0) {
			String text = this.addText.getText().trim();
			if (text.indexOf('&') > -1) {
				openDelimiterDialog(JBossJAXRSUIMessages.DelimitedStringList_Msg_Text_ParseByAmpersand,
						JBossJAXRSUIMessages.DelimitedStringList_Msg_Title_ParseByAmpersand, text, "&"); //$NON-NLS-1$
			} else if (text.indexOf(',') > -1) {
				openDelimiterDialog(JBossJAXRSUIMessages.DelimitedStringList_Msg_Text_ParseByComma,
						JBossJAXRSUIMessages.DelimitedStringList_Msg_Title_ParseByComma, text, ","); //$NON-NLS-1$
			} else {
				WSProperty prop = parseString(text);
				if(!wsProperties.add(prop)) {
					//key is already in the list
					wsProperties.remove(prop);
					wsProperties.add(prop);
				}
			}
			this.removeAllButton.setEnabled(true);
			this.addText.setSelection(0, text.length());
			this.addText.setFocus();
			listViewer.refresh();
		}
	}

	private void openDelimiterDialog(String dialogText, String dialogTitle, String text, String delimiter) {
		MessageDialog dialog = new MessageDialog(null, dialogTitle, null, dialogText, MessageDialog.QUESTION,
				new String[] { JBossJAXRSUIMessages.DelimitedStringList_Msg_Yes_Btn,
						JBossJAXRSUIMessages.DelimitedStringList_Msg_No_Btn },
				0); // yes is the default
		int result = dialog.open();
		if (result == 0) {
			wsProperties.addAll(parseString(text, delimiter));
		}
	}

	public static WSProperty parseString(String prop) {
		String[] p = prop.split(DELIMITER);
		return new WSProperty(p[0], p[1]);
	}

	public static Set<WSProperty> parseString(String str_list, String delimiter) {
		Set<WSProperty> properties = new LinkedHashSet<>();
		StringTokenizer tk = new StringTokenizer(str_list, delimiter);
		while (tk.hasMoreTokens()) {
			properties.add(parseString(tk.nextToken()));
		}
		return properties;
	}

	private void editInList() {
		WSProperty selection = getSelection();
		if (selection != null) {
			InputDialog editDialog = new InputDialog(getShell(),
					JBossJAXRSUIMessages.JBossWS_DelimitedStringList_EditValue_Dialog_Title,
					JBossJAXRSUIMessages.JBossWS_DelimitedStringList_EditValue_Dialog_Message, selection.toString(), null);
			if (editDialog.open() == Window.OK) {
				WSProperty prop = parseString(editDialog.getValue());
				if(!wsProperties.add(prop)) {
					//key is already in the list
					wsProperties.remove(prop);
					wsProperties.add(prop);
				}
				listViewer.refresh();
			}
		}
		updatePropertyButtons();
	}

	/**
	 * Remove a string from the list
	 */
	private void removeFromList() {
		WSProperty wProp = getSelection();
		if(wProp != null) 	{
			wsProperties.remove(wProp);
		}
		listViewer.refresh();
		updatePropertyButtons();
	}

	/**
	 * Return the current selection
	 */
	public WSProperty getSelection() {
		Object obj = listViewer.getStructuredSelection().getFirstElement();
		return obj == null ? null : (WSProperty) obj;
	}

	/**
	 * Update button state based on what's selected
	 */
	public void updatePropertyButtons() {
		removeButton.setEnabled(false);
		editButton.setEnabled(false);
		removeAllButton.setEnabled(false);

		if (!wsProperties.isEmpty()) {

			removeAllButton.setEnabled(true);

			if (getSelection() != null) {
				removeButton.setEnabled(true);
				editButton.setEnabled(true);
			}
		}

		String value = addText.getText();
		boolean flag = value != null && value.trim().length() > 0;
		boolean valid = validateText(value);
		if (!valid) {
			warningLabel.setText(mWarning);
		} else {
			warningLabel.setText(""); //$NON-NLS-1$
		}
		addButton.setEnabled(flag && valid);
	}

	private boolean validateText(String text) {
		if (text != null && text.trim().length() > 0 && text.indexOf(DELIMITER) == -1) {
			this.mWarning = JBossJAXRSUIMessages.DelimitedStringList_NO_EQUALS_DELIMITER_WARNING;
			return false;
		}
		this.mWarning = null;
		return true;
	}

	/**
	 * Set focus to the add text box
	 */
	public boolean setFocus() {
		if (this.addText != null)
			return this.addText.setFocus();
		return false;
	}
	
	public void addPropertiesListener(Listener listener) {
		propertiesListeners.add(listener);
	}
	
	private void fireChangedEvent() {
		Event e = new Event();
		e.data = wsProperties;
		for(Listener listener: propertiesListeners) {
			listener.handleEvent(e);
		}
	}
	
	
}
