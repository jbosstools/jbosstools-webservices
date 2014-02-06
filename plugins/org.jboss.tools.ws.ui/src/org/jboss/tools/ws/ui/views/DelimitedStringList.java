/******************************************************************************* 
 * Copyright (c) 2010 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Sybase, Inc./Eclipse - DTP - initial API and implementation 
 * Red Hat, Inc. - added Edit button
 ******************************************************************************/
package org.jboss.tools.ws.ui.views;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.ws.ui.messages.JBossWSUIMessages;

import com.ibm.icu.util.StringTokenizer;

/**
 * Composite that provides a method of managing a delimited list of strings.
 * 
 * @author brianf
 */
public class DelimitedStringList extends Composite {

	// delimiter used
	private static final String DELIMITER = ",";//$NON-NLS-1$

	// ui elements
	private Button mAddButton;
	private Button mClearAllButton;
	private Button mRemoveButton;
	private Button mDownButton;
	private Button mUpButton;
	private Button mEditButton;
	private List mPropsList;
	private Text mAddText;
	private boolean isReadOnly = false;
	private Label mWarningLabel;
	private boolean showUpDown = true;

	// current delimiter
	private String mDelimiter = DELIMITER;
	
	// Warning
	private String mWarning = null;

	// change listeners
	private ListenerList changeListeners;

	private class AddTextModifyListener implements ModifyListener {

		private DelimitedStringList parent;

		public AddTextModifyListener(DelimitedStringList parent) {
			this.parent = parent;
		}

		public void modifyText(ModifyEvent e) {
			updatePropertyButtons();
			fireChangedEvent(parent);
		}
	}

	/**
	 * Constructor
	 * 
	 * @param parent
	 * @param style
	 */
	public DelimitedStringList(Composite parent, int style) {
		this(parent, style, false, true);
	}

	/**
	 * Constructor
	 * 
	 * @param parent
	 * @param style
	 */
	public DelimitedStringList(Composite parent, int style, boolean isReadOnly, boolean showUpDown) {
		super(parent, style);
		this.isReadOnly = isReadOnly;
		this.showUpDown = showUpDown;
		this.changeListeners = new ListenerList();
		
		int additionalStyles = SWT.NONE;
		if (isReadOnly){
			additionalStyles = SWT.READ_ONLY;
		}
		
		final GridLayout gridLayout = new GridLayout();
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		gridLayout.numColumns = 2;
		setLayout(gridLayout);
		{
			this.mWarningLabel = new Label(this, SWT.NONE);
			GridData wlGridData = new GridData(GridData.FILL_HORIZONTAL);
			wlGridData.horizontalSpan = 2;
			this.mWarningLabel.setLayoutData(wlGridData);
			this.mWarningLabel.setForeground(this.mWarningLabel.getDisplay().getSystemColor(SWT.COLOR_RED));
			this.mWarningLabel.setBackground(parent.getBackground());
		}
		{
			this.mAddText = new Text(this, SWT.BORDER | additionalStyles);
			this.mAddText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			this.mAddText.addModifyListener(new AddTextModifyListener(this));
		}
		{
			this.mAddButton = new Button(this, SWT.NONE);
			this.mAddButton.setLayoutData(new GridData(
					GridData.HORIZONTAL_ALIGN_FILL));
			this.mAddButton.setText("Add");//$NON-NLS-1$
			this.mAddButton.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(SelectionEvent e) {
					addStringToList();
					fireChangedEvent(e.getSource());
				}
			});
			
			this.mAddButton.setEnabled(false);
		}
		{
			final Composite mSpacerComposite = new Composite(this, SWT.NONE);
			final GridData gridData = new GridData();
			gridData.heightHint = 10;
			gridData.horizontalSpan = 2;
			mSpacerComposite.setLayoutData(gridData);
			mSpacerComposite.setLayout(new GridLayout());
			mSpacerComposite.setBackground(parent.getBackground());
		}
		{
			this.mPropsList = new List(this, SWT.BORDER | SWT.V_SCROLL);
			final GridData gridData = new GridData(
					GridData.HORIZONTAL_ALIGN_FILL
							| GridData.VERTICAL_ALIGN_FILL);
			if (getShowUpDown())
				gridData.verticalSpan = 5;
			else
				gridData.verticalSpan = 3;
			this.mPropsList.setLayoutData(gridData);
			this.mPropsList.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(SelectionEvent e) {
					updatePropertyButtons();
				}
			});
		}
		if (getShowUpDown()) {
			this.mUpButton = new Button(this, SWT.NONE);
			this.mUpButton.setLayoutData(new GridData(
					GridData.HORIZONTAL_ALIGN_FILL));
			this.mUpButton.setText("Up");//$NON-NLS-1$
			this.mUpButton.setEnabled(false);
			this.mUpButton.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(SelectionEvent e) {
					moveUpInList();
					fireChangedEvent(e.getSource());
				}
			});

			this.mDownButton = new Button(this, SWT.NONE);
			this.mDownButton.setLayoutData(new GridData(
					GridData.HORIZONTAL_ALIGN_FILL));
			this.mDownButton.setText("Down");//$NON-NLS-1$
			this.mDownButton.setEnabled(false);
			this.mDownButton.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(SelectionEvent e) {
					moveDownInList();
					fireChangedEvent(e.getSource());
				}
			});
		}
		{
			this.mEditButton = new Button(this, SWT.NONE);
			this.mEditButton.setLayoutData(new GridData(
					GridData.HORIZONTAL_ALIGN_FILL));
			this.mEditButton.setText("Edit");//$NON-NLS-1$
			this.mEditButton.setEnabled(false);
			this.mEditButton.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(SelectionEvent e) {
					editInList();
					fireChangedEvent(e.getSource());
				}
			});
		}
		{
			this.mRemoveButton = new Button(this, SWT.NONE);
			this.mRemoveButton.setLayoutData(new GridData(
					GridData.HORIZONTAL_ALIGN_FILL));
			this.mRemoveButton.setText("Remove");//$NON-NLS-1$
			this.mRemoveButton.setEnabled(false);
			this.mRemoveButton.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(SelectionEvent e) {
					removeFromList();
					fireChangedEvent(e.getSource());
				}
			});
		}
		{
			this.mClearAllButton = new Button(this, SWT.NONE);
			this.mClearAllButton.setEnabled(false);
			this.mClearAllButton.setLayoutData(new GridData(
					GridData.HORIZONTAL_ALIGN_FILL));
			this.mClearAllButton.setText("Clear All");//$NON-NLS-1$
			this.mClearAllButton.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(SelectionEvent e) {
					DelimitedStringList.this.mPropsList.removeAll();
					updatePropertyButtons();
					fireChangedEvent(e.getSource());
				}
			});
		}
		updatePropertyButtons();
	}

	public void removeAll() {
		this.mPropsList.removeAll();
		updatePropertyButtons();
	}
	
	public void dispose() {
		super.dispose();
	}

	protected void checkSubclass() {
		// empty
	}

	/**
	 * Add a new string to the list
	 */
	private void addStringToList() {
		if (this.mAddText.getText().length() > 0) {
			String text = this.mAddText.getText().trim();
			if (text.indexOf('&') > -1) {
				MessageDialog dialog = new MessageDialog(
						null, JBossWSUIMessages.DelimitedStringList_Msg_Title_ParseByAmpersand, null, 
						JBossWSUIMessages.DelimitedStringList_Msg_Text_ParseByAmpersand,
						MessageDialog.QUESTION,
						new String[] {JBossWSUIMessages.DelimitedStringList_Msg_Yes_Btn, JBossWSUIMessages.DelimitedStringList_Msg_No_Btn},
						0); // yes is the default
				int result = dialog.open();
				if (result == 0) {
					String[] parsed = parseString(text, "&"); //$NON-NLS-1$
					for (int i = 0; i < parsed.length; i++) {
						this.mPropsList.add(parsed[i]);
					}
				}
			} else if (text.indexOf(',') > -1) {
					MessageDialog dialog = new MessageDialog(
							null, JBossWSUIMessages.DelimitedStringList_Msg_Title_ParseByComma, null, 
							JBossWSUIMessages.DelimitedStringList_Msg_Text_ParseByComma,
							MessageDialog.QUESTION,
							new String[] {JBossWSUIMessages.DelimitedStringList_Msg_Yes_Btn, JBossWSUIMessages.DelimitedStringList_Msg_No_Btn},
							0); // yes is the default
					int result = dialog.open();
					if (result == 0) {
						String[] parsed = parseString(text, ","); //$NON-NLS-1$
						for (int i = 0; i < parsed.length; i++) {
							this.mPropsList.add(parsed[i]);
						}
					}
			} else {
				this.mPropsList.add(text);
			}
			if (!isReadOnly){
				this.mClearAllButton.setEnabled(true);
			}
			this.mAddText.setSelection(0, text.length());
			this.mAddText.setFocus();
		}
	}
	
	private void editInList() {
		if (this.mPropsList.getSelectionCount() == 1) {
			int index = this.mPropsList.getSelectionIndex();
			String toEdit = this.mPropsList.getItem(index);
			
			InputDialog editDialog = new InputDialog(getShell(), 
					JBossWSUIMessages.JBossWS_DelimitedStringList_EditValue_Dialog_Title, 
					JBossWSUIMessages.JBossWS_DelimitedStringList_EditValue_Dialog_Message, 
					toEdit, null);
			int rtn_code = editDialog.open();
			if (rtn_code == Window.OK) {
				this.mPropsList.remove(index);
				this.mPropsList.add(editDialog.getValue(), index);
			}
		}

		this.mPropsList.setFocus();

		updatePropertyButtons();
	}

	/**
	 * Remove a string from the list
	 */
	private void removeFromList() {
		if (this.mPropsList.getSelectionCount() == 1) {
			int index = this.mPropsList.getSelectionIndex();
			this.mPropsList.remove(index);
			try {
				if (this.mPropsList.getItem(index) != null) {
					this.mPropsList.select(index);
				}
			}
			catch (IllegalArgumentException e) {
				// do nothing
			}
		}

		this.mPropsList.setFocus();

		updatePropertyButtons();
	}

	/**
	 * Move a string up in the list
	 */
	private void moveUpInList() {
		if (this.mPropsList.getSelectionCount() == 1) {
			int selection = this.mPropsList.getSelectionIndex();
			int destination = -1;
			if (selection > 0) {
				destination = selection - 1;
				String value_a = this.mPropsList.getItem(destination);
				String value_b = this.mPropsList.getItem(selection);

				String[] items = this.mPropsList.getItems();
				this.mPropsList.removeAll();

				int counter = 0;
				while (counter < items.length) {
					if (counter == destination) {
						this.mPropsList.add(value_b);
						this.mPropsList.add(value_a);
						counter = counter + 2;
					}
					else {
						this.mPropsList.add(items[counter]);
						counter++;
					}
				}

				this.mPropsList.setSelection(destination);
			}
		}
		this.mPropsList.setFocus();
		updatePropertyButtons();
	}

	/**
	 * Move a string down in the list
	 */
	private void moveDownInList() {
		if (this.mPropsList.getSelectionCount() == 1) {
			int selection = this.mPropsList.getSelectionIndex();
			int destination = -1;
			if (selection < this.mPropsList.getItemCount() - 1) {

				destination = selection + 1;

				String value_a = this.mPropsList.getItem(destination);
				String value_b = this.mPropsList.getItem(selection);

				String[] items = this.mPropsList.getItems();
				this.mPropsList.removeAll();

				int counter = 0;
				while (counter < items.length) {
					if (counter == selection) {
						this.mPropsList.add(value_a);
						this.mPropsList.add(value_b);
					}
					else if (counter != destination) {
						this.mPropsList.add(items[counter]);
					}
					counter++;
				}

				this.mPropsList.setSelection(destination);

			}
		}
		this.mPropsList.setFocus();
		updatePropertyButtons();
	}

	/**
	 * Return the current selection
	 */
	public String getSelection() {
		String selectString = ""; //$NON-NLS-1$
		for (int i = 0; i < this.mPropsList.getItems().length; i++) {
			String testitem = this.mPropsList.getItem(i).trim();
			selectString = selectString + testitem;
			if (i < this.mPropsList.getItems().length - 1) {
				selectString = selectString + this.mDelimiter;
			}
		}
		return selectString;
	}

	/**
	 * Update button state based on what's selected
	 */
	public void updatePropertyButtons() {
		if (!isReadOnly){
			if (this.mUpButton != null) {
				this.mUpButton.setEnabled(false);
				this.mDownButton.setEnabled(false);
			}
			this.mRemoveButton.setEnabled(false);
			this.mEditButton.setEnabled(false);
			this.mClearAllButton.setEnabled(false);
				
			if (this.mPropsList.getItemCount() > 0) {
				
				this.mClearAllButton.setEnabled(true);
				
				if(this.mPropsList.getSelectionCount() == 1){
					int selection = this.mPropsList.getSelectionIndex();
	
					this.mRemoveButton.setEnabled(true);
					this.mEditButton.setEnabled(true);
	
					if (this.mUpButton != null) {
						if (selection - 1 >= 0)
							this.mUpButton.setEnabled(true);
		
						if (selection + 1 < this.mPropsList.getItemCount())
							this.mDownButton.setEnabled(true);
					}
				}
			}
	
			String value = ""; //$NON-NLS-1$
			value = this.mAddText.getText();
			boolean flag = value != null && value.trim().length() > 0;
			boolean valid = validateText(value);
			if (!valid) {
				this.mWarningLabel.setText(this.mWarning);
			} else {
				this.mWarningLabel.setText(""); //$NON-NLS-1$
			}
			this.mAddButton.setEnabled(flag && valid);
			
		} 
	}

	private boolean validateText(String text) {
//		if (text != null && text.trim().length() > 0 && text.indexOf(",") > -1) { //$NON-NLS-1$
//			this.mWarning = JBossWSUIMessages.DelimitedStringList_NO_COMMAS_WARNING;
//			return false;
//		}
		/*else*/ if (text != null && text.trim().length() > 0 && text.indexOf("=") == -1) { //$NON-NLS-1$
			this.mWarning = JBossWSUIMessages.DelimitedStringList_NO_EQUALS_DELIMITER_WARNING;
			return false;
		}
		this.mWarning = null;
		return true;
	}

	/**
	 * Set focus to the add text box
	 */
	public boolean setFocus() {
		if (this.mAddText != null)
			return this.mAddText.setFocus();
		return false;
	}

	/**
	 * Set the initial delimited list
	 * 
	 * @param str_list
	 */
	public void setSelection(String str_list) {
		String[] str_array = parseString(str_list);
		this.mPropsList.setItems(str_array);
		updatePropertyButtons();
	}

	/**
	 * Parse the list
	 * 
	 * @param str_list
	 * @return
	 */
	private String[] parseString(String str_list) {
		if (str_list != null) {
			StringTokenizer tk = new StringTokenizer(str_list, this.mDelimiter);
			String[] pieces = new String[tk.countTokens()];
			int index = 0;
			while (tk.hasMoreTokens())
				pieces[index++] = tk.nextToken();
			return pieces;
		} 
		return new String[0];
	}

	public static String[] parseString(String str_list, String delimiter) {
		if (str_list != null) {
			StringTokenizer tk = new StringTokenizer(str_list, delimiter);
			String[] pieces = new String[tk.countTokens()];
			int index = 0;
			while (tk.hasMoreTokens())
				pieces[index++] = tk.nextToken();
			return pieces;
		}
		return new String[0];
	}

	/**
	 * Change the delimiter
	 * 
	 * @param delimiter
	 */
	public void setDelimiter(String delimiter) {
		this.mDelimiter = delimiter;
	}

	/**
	 * Return the delimiter
	 * 
	 * @return
	 */
	public String getDelimiter() {
		return this.mDelimiter;
	}

	public String getWarning() {
		return this.mWarning;
	}

	/**
	 * If we changed, fire a changed event.
	 * 
	 * @param source
	 */
	private void fireChangedEvent(Object source) {
		ChangeEvent e = new ChangeEvent(source);
		// inform any listeners of the resize event
		Object[] listeners = this.changeListeners.getListeners();
		for (int i = 0; i < listeners.length; ++i) {
			((ChangeListener) listeners[i]).stateChanged(e);
		}
	}

	/**
	 * Add a change listener
	 * 
	 * @param listener
	 */
	public void addChangeListener(ChangeListener listener) {
		this.changeListeners.add(listener);
	}

	/**
	 * Remove a change listener.
	 * 
	 * @param listener
	 */
	public void removeChangeListener(ChangeListener listener) {
		this.changeListeners.remove(listener);
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		mAddButton.setEnabled(enabled);
		mAddText.setEnabled(enabled);
		mClearAllButton.setEnabled(enabled);
		if (this.mUpButton != null) {
			mUpButton.setEnabled(enabled);
			mDownButton.setEnabled(enabled);
		}
		mEditButton.setEnabled(enabled);
		mPropsList.setEnabled(enabled);
		mRemoveButton.setEnabled(enabled);
		if (enabled)
			updatePropertyButtons();
	}

	
	public void setShowUpDown ( boolean flag ) {
		this.showUpDown = flag;
	}
	
	public boolean getShowUpDown() {
		return this.showUpDown;
	}
}
