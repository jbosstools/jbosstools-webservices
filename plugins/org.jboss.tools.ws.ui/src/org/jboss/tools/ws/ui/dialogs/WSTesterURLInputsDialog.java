/******************************************************************************* 
 * Copyright (c) 2012 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.ws.ui.dialogs;

import java.util.StringTokenizer;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.jboss.tools.ws.ui.messages.JBossWSUIMessages;

/**
 * Dialog that pops up if the URL has parameters that need values. 
 * This should only appear if it's a JAX-RS endpoint being tested from the
 * JAX-RS tooling.
 * @author bfitzpat
 *
 */
public class WSTesterURLInputsDialog extends TitleAreaDialog {

	private TreeViewer treeRequestBody;
	private static final String NAME_COLUMN = "name";//$NON-NLS-1$
	private static final String VALUE_COLUMN = "value";//$NON-NLS-1$
	private static final String[] TREE_COLUMNS = new String[] { NAME_COLUMN, VALUE_COLUMN };
	private String stashedURL;
	private Parameter[] parms;
	private Button mOKButton = null;

	/**
	 * Constructor
	 * @param parentShell
	 * @param url
	 */
	public WSTesterURLInputsDialog(Shell parentShell, String url) {
		super(parentShell);
		stashedURL = url;
		parms = parseURLParms(stashedURL);
	}
	
	/**
	 * Return the modified URL with values
	 * @return
	 */
	public String getURL() {
		return replaceParmsWithValues();
	}
	
	/*
	 * If we have values for parms, send them back
	 * @return
	 */
	private String replaceParmsWithValues () {
		if (parms.length > 0 && stashedURL != null && stashedURL.trim().length() > 0) {
			String modified = stashedURL;
			for (int i = 0; i < parms.length; i++) {
				Parameter parm = parms[i];
				if (parm.value != null && parm.value.trim().length() > 0) {
					modified = modified.replace(parm.originalString, parm.value);
				}
			}
			return modified;
		}
		return stashedURL;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle(JBossWSUIMessages.WSTesterURLInputsDialog_DialogTitle);
		setMessage(JBossWSUIMessages.WSTesterURLInputsDialog_DialogMessage);
		Composite area = (Composite) super.createDialogArea(parent);

		Font font = parent.getFont();

		Composite content = new Composite(area, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		content.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		content.setLayoutData(data);
		content.setFont(font);	

		Label label = new Label(content, SWT.NONE);
		label.setText(JBossWSUIMessages.WSTesterURLInputsDialog_URLParms_Label);
		GridData gridData = new GridData();
		gridData.horizontalSpan = 2;
		label.setLayoutData(gridData);

		treeRequestBody = new TreeViewer(area, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.FULL_SELECTION );
		this.treeRequestBody.setAutoExpandLevel(TreeViewer.ALL_LEVELS);
		GridData gd11 = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd11.heightHint = 100;
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
				if (element instanceof Parameter && property.equalsIgnoreCase(NAME_COLUMN)) {
					return true;
				} else if (element instanceof Parameter && property.equalsIgnoreCase(VALUE_COLUMN)) {
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
				if (element instanceof Parameter && columnIndex == 0) {
					return ((Parameter)element).name;
				} else if (element instanceof Parameter && columnIndex == 1) {
					Parameter tp = (Parameter) element;
					return tp.value;
				}
				return null;
			}
		});
		
		treeRequestBody.setContentProvider(new ITreeContentProvider(){
			
			Parameter[] parms;
			
			@Override
			public void dispose() {
			}

			@Override
			public void inputChanged(Viewer viewer, Object oldInput,
					Object newInput) {
				if (newInput instanceof Parameter[]) {
					parms = (Parameter[]) newInput;
				}
			}

			@Override
			public Object[] getElements(Object inputElement) {
				if (inputElement instanceof Parameter[]) {
					return this.parms;
				}
				return null;
			}

			@Override
			public Object[] getChildren(Object parentElement) {
				if (parentElement instanceof Parameter[]) {
					return new Object[] {this.parms};
				}
				return null;
			}

			@Override
			public Object getParent(Object element) {
				if (element instanceof Parameter) {
					return parms;
				}
				return null;
			}

			@Override
			public boolean hasChildren(Object element) {
				if (element instanceof Parameter[]) {
					return ((Parameter[])element).length > 0;
				}
				return false;
			}
		});
		
		treeRequestBody.setCellModifier(new ICellModifier() {

			/* (non-Javadoc)
			 * @see org.eclipse.jface.viewers.ICellModifier#canModify(java.lang.Object, java.lang.String)
			 */
			public boolean canModify(Object element, String property) {
				if (element instanceof Parameter && property.equalsIgnoreCase(VALUE_COLUMN)) {
					return true;
				}
				return false;
			}

			/* (non-Javadoc)
			 * @see org.eclipse.jface.viewers.ICellModifier#getValue(java.lang.Object, java.lang.String)
			 */
			public Object getValue(Object element, String property) {
				if (element instanceof Parameter && property.equalsIgnoreCase(VALUE_COLUMN)) {
					return ((Parameter) element).value;
				}
				return null;
			}

			/* (non-Javadoc)
			 * @see org.eclipse.jface.viewers.ICellModifier#modify(java.lang.Object, java.lang.String, java.lang.Object)
			 */
			public void modify(Object element, String property, Object value) {
				if (element instanceof TreeItem && property.equalsIgnoreCase(VALUE_COLUMN)) {
					TreeItem ti = (TreeItem) element;
					Parameter parm = (Parameter) ti.getData();
					parm.value = (String) value;
					treeRequestBody.refresh(true);
					validate();
				}
			}
			
		});
		treeRequestBody.setCellEditors(new CellEditor[] { null, new TextCellEditor(treeRequestBody.getTree()) });
		treeRequestBody.setInput(parms);

		return area;
	}

	/*
	 * Internal class for managing url parm details
	 */
	private class Parameter {
		String name;
		String value = ""; //$NON-NLS-1$
		String regEx;
		String originalString;
	}
	
	/*
	 * Take the parms from the url and shove them into a local class for easier management
	 * @param url
	 * @return
	 */
	private Parameter[] parseURLParms ( String url ) {
		if (url != null && url.trim().length() > 0 && url.endsWith("}")) { //$NON-NLS-1$
			String[] parsed = parseString(url.substring(url.indexOf('{')), "{"); //$NON-NLS-1$
			if (parsed != null && parsed.length > 0) {
				Parameter[] parms = new Parameter[parsed.length];
				for (int i = 0; i < parsed.length; i++) {
					String temp = parsed[i];
					temp = temp.replace('}', ' ').trim();
					String[] nameRegEx = parseString(temp, ":"); //$NON-NLS-1$
					String parmName;
					String regEx = null;
					if (nameRegEx.length > 1) {
						parmName = nameRegEx[0];
						regEx = nameRegEx[1];
					} else {
						parmName = temp;
					}
					parms[i] = new Parameter();
					parms[i].name = parmName;
					parms[i].regEx = regEx;
					parms[i].originalString = '{' + parsed[i];
				}
				return parms;
			}
		}
		return new Parameter[0];
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		this.mOKButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
		this.mOKButton.setEnabled(true);
		validate();
		setErrorMessage(null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
	 */
	@Override
	protected void configureShell(Shell newShell) {
		newShell.setText(JBossWSUIMessages.WSTesterURLInputsDialog_Window_Title);
		super.configureShell(newShell);
	}

	
	/*
	 * Validate the parm values and send back a message
	 * @return
	 */
	private String validateParmValues() {
		String errorMessage = ""; //$NON-NLS-1$
		if (parms != null && parms.length > 0) {
			for (int i = 0; i < parms.length; i++) {
				Parameter parm = parms[i];
				if (parm.regEx != null && parm.regEx.trim().length() > 0) {
					boolean valid = parm.value.matches(parm.regEx);
					if (!valid) {
						errorMessage = JBossWSUIMessages.WSTesterURLInputsDialog_Validation_Error_String;
						errorMessage = NLS.bind(errorMessage, new String[] 
								{ parm.name, parm.value, parm.regEx });
						break;
					}
				}
			}
		}
		return errorMessage;
	}
	
	/*
	 * Validate the parameter values and pop up a message for the first invalid value.
	 */
	private void validate() {
		// validates two different types of parameters in the url
		// parms with regex: {id:[0-9][0-9]*}
		// parms without regex: {id} (always valid, no way to test)
		String errorMessage = validateParmValues();
		boolean hasError = errorMessage.trim().length() > 0;
		if (hasError) {
			setErrorMessage(errorMessage);
		}
		else
			setErrorMessage(null);
		if (this.mOKButton != null && !this.mOKButton.isDisposed())
			this.mOKButton.setEnabled(!hasError);
	}

	/*
	 * Parses the incoming string by the token.
	 * @param str_list String list
	 * @param token Token to use to break up the string
	 * @return String array 
	 */
	private String[] parseString(String str_list, String token) {
		StringTokenizer tk = new StringTokenizer(str_list, token);
		String[] pieces = new String[tk.countTokens()];
		int index = 0;
		while (tk.hasMoreTokens())
			pieces[index++] = tk.nextToken();
		return pieces;
	}
}
