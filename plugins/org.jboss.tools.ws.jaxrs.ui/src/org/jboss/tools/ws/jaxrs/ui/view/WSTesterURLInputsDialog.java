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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
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
import org.jboss.tools.ws.jaxrs.ui.JBossJAXRSUIMessages;
import org.jboss.tools.ws.jaxrs.ui.cnf.action.JAXRSPathTemplateParser;
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
	private static final String TYPE_COLUMN = "datatype";//$NON-NLS-1$
	private static final String[] TREE_COLUMNS = new String[] { NAME_COLUMN, VALUE_COLUMN, TYPE_COLUMN };
	private String stashedURL;
	private URLTemplateParameter[] parms;
 	private Button mOKButton = null;
	
	/**
	 * Constructor
	 * @param parentShell
	 * @param url
	 */
	public WSTesterURLInputsDialog(Shell parentShell, String url) {
		super(parentShell);
		stashedURL = url;
		parms = JAXRSPathTemplateParser.parse(stashedURL);
	}
	
	/**
	 * Return the modified URL with values
	 * @return
	 */
	public String getURL() {
		return replaceParmsWithValues();
	}
	
	/*
	 * See http://munkymorgy.blogspot.com/2010/01/java-string-search-and-replace.html
	 */
	private String replaceString(String input, String find, String replace, boolean casesensitive){
		String input_case_adjusted = input;
		if (casesensitive == false) {
			//For Case Insensitive searches
			//Lowercase everything (but replace in the original string)
			input_case_adjusted = input.toLowerCase() ;
			find                = find.toLowerCase() ;
		}

		int    startPosition = input_case_adjusted.indexOf(find);
		String start         = ""; //$NON-NLS-1$
		String end           = ""; //$NON-NLS-1$

		if (startPosition >= 0) {
			if (startPosition > 0) {
				start = input.substring(0, startPosition);
			}
			end = input.substring(startPosition + find.length());

			return start + replace + end;
		} else {
			return input;
		}
	}
	
	/*
	 * If we have values for parms, send them back
	 * @return
	 */
	private String replaceParmsWithValues () {
		if (parms.length > 0 && stashedURL != null && stashedURL.trim().length() > 0) {
			String modified = stashedURL;
			for (URLTemplateParameter parm : parms) {
				if (parm.getValue() != null) {
					modified = replaceString(modified, parm.getOriginalContent(), parm.getReplacementContent(), true);
				}
			}
			modified = modified.replace("//", "/"); //$NON-NLS-1$ //$NON-NLS-2$
			
			// fix JBIDE-11728
			if (modified.startsWith("http:/") && !modified.startsWith("http://")) { //$NON-NLS-1$ //$NON-NLS-2$
				modified = modified.replace("http:/", "http://"); //$NON-NLS-1$ //$NON-NLS-2$
			} else if (modified.startsWith("https:/") && !modified.startsWith("https://")) { //$NON-NLS-1$ //$NON-NLS-2$
				modified = modified.replace("https:/", "https://"); //$NON-NLS-1$ //$NON-NLS-2$
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
		setTitle(JBossJAXRSUIMessages.WSTesterURLInputsDialog_DialogTitle);
		setMessage(JBossJAXRSUIMessages.WSTesterURLInputsDialog_DialogMessage);
		Composite area = (Composite) super.createDialogArea(parent);

		Font font = parent.getFont();

		Composite content = new Composite(area, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		content.setLayout(layout);
		layout.marginLeft=5;
		layout.marginRight=5;
		GridData data = new GridData(GridData.FILL_BOTH);
		content.setLayoutData(data);
		content.setFont(font);	

		Label label = new Label(content, SWT.NONE);
		label.setText(JBossJAXRSUIMessages.WSTesterURLInputsDialog_URLParms_Label);
		GridData gridData = new GridData();
		gridData.horizontalSpan = 2;
		label.setLayoutData(gridData);

		treeRequestBody = new TreeViewer(content, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.FULL_SELECTION );
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
		TreeColumn typeColumn = new TreeColumn(treeRequestBody.getTree(), SWT.LEFT);
		typeColumn.setText(JBossJAXRSUIMessages.WSTesterURLInputsDialog_Type_Column);
		typeColumn.setWidth(200);
		
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
				if (element instanceof URLTemplateParameter && property.equalsIgnoreCase(NAME_COLUMN)) {
					return true;
				} else if (element instanceof URLTemplateParameter && property.equalsIgnoreCase(VALUE_COLUMN)) {
					return true;
				} else if (element instanceof URLTemplateParameter && property.equalsIgnoreCase(TYPE_COLUMN)) {
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
				if (element instanceof URLTemplateParameter) {

					URLTemplateParameter parameter = (URLTemplateParameter) element;
					if (columnIndex == 0) {
						final StringBuilder nameBuilder = new StringBuilder();
						nameBuilder.append(parameter.getName());
						// suffix the parameter name with an asterix to indicate
						// it is mandatory.
						if (parameter.isMandatory()) {
							nameBuilder.append("* "); //$NON-NLS-1$
						}
						return nameBuilder.toString();
					} else if (columnIndex == 1) {
						return parameter.getValue();
					} else if (columnIndex == 2) {
						return parameter.getDatatype();
					}
				}
				return null;
			}
		});
		
		treeRequestBody.setContentProvider(new ITreeContentProvider(){
			
			URLTemplateParameter[] parms;
			
			@Override
			public void dispose() {
			}

			@Override
			public void inputChanged(Viewer viewer, Object oldInput,
					Object newInput) {
				if (newInput instanceof URLTemplateParameter[]) {
					parms = (URLTemplateParameter[]) newInput;
				}
			}

			@Override
			public Object[] getElements(Object inputElement) {
				if (inputElement instanceof URLTemplateParameter[]) {
					return this.parms;
				}
				return null;
			}

			@Override
			public Object[] getChildren(Object parentElement) {
				if (parentElement instanceof URLTemplateParameter[]) {
					return new Object[] {this.parms};
				}
				return null;
			}

			@Override
			public Object getParent(Object element) {
				if (element instanceof URLTemplateParameter) {
					return parms;
				}
				return null;
			}

			@Override
			public boolean hasChildren(Object element) {
				if (element instanceof URLTemplateParameter[]) {
					return ((URLTemplateParameter[])element).length > 0;
				}
				return false;
			}
		});
		
		treeRequestBody.setCellModifier(new ICellModifier() {

			/* (non-Javadoc)
			 * @see org.eclipse.jface.viewers.ICellModifier#canModify(java.lang.Object, java.lang.String)
			 */
			public boolean canModify(Object element, String property) {
				if (element instanceof URLTemplateParameter && property.equalsIgnoreCase(VALUE_COLUMN)) {
					return true;
				}
				return false;
			}

			/* (non-Javadoc)
			 * @see org.eclipse.jface.viewers.ICellModifier#getValue(java.lang.Object, java.lang.String)
			 */
			public Object getValue(Object element, String property) {
				if (element instanceof URLTemplateParameter && property.equalsIgnoreCase(VALUE_COLUMN)) {
					return ((URLTemplateParameter) element).getValue();
				}
				return null;
			}

			/* (non-Javadoc)
			 * @see org.eclipse.jface.viewers.ICellModifier#modify(java.lang.Object, java.lang.String, java.lang.Object)
			 */
			public void modify(Object element, String property, Object value) {
				if (element instanceof TreeItem && property.equalsIgnoreCase(VALUE_COLUMN)) {
					TreeItem ti = (TreeItem) element;
					URLTemplateParameter parm = (URLTemplateParameter) ti.getData();
					parm.setValue((String) value);
					treeRequestBody.refresh(true);
					validate();
				}
			}
			
		});
		treeRequestBody.setCellEditors(new CellEditor[] { null, new TextCellEditor(treeRequestBody.getTree()), null });
		treeRequestBody.setInput(parms);

		// mention at the bottom of the grid to indicate that some values are mandatory
		Label mandatoryValuesLabel = new Label(content, SWT.NONE);
		mandatoryValuesLabel.setText(JBossJAXRSUIMessages.WSTesterURLInputsDialog_URLParms_Mandatory);
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		mandatoryValuesLabel.setLayoutData(gridData);
		return area;
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
		newShell.setText(JBossJAXRSUIMessages.WSTesterURLInputsDialog_Window_Title);
		super.configureShell(newShell);
	}

	
	/**
	 * 
	 * Validate all parameters and return an error message as soon as one param
	 * is invalid
	 * 
	 * @return an error message for the first error detected, null otherwise.
	 */
	private String validateParmValues() {
		for (URLTemplateParameter parameter : parms) {
			final String errorMessage = parameter.validate();
			if (errorMessage != null) {
				return errorMessage;
			}
		}
		return null;
	}
	
	/**
	 * Validate the parameter values and pop up a message for the first invalid value.
	 * Updated for JBIDE-15593 to just show warning, no error, and allow user to submit
	 * request with bad parms if desired.
	 */
	private void validate() {
		// validates two different types of parameters in the url
		// parms with regex: {id:[0-9][0-9]*}
		// parms without regex: {id} (always valid, no way to test)
		final String errorMessage = validateParmValues();
		boolean hasError = errorMessage != null;
		if (hasError) {
			setMessage(errorMessage, IMessageProvider.WARNING);
		}
		else
			setMessage(JBossJAXRSUIMessages.WSTesterURLInputsDialog_DialogMessage);
	}

}