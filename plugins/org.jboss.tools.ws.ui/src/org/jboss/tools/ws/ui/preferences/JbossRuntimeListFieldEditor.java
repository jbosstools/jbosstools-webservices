/******************************************************************************* 
 * Copyright (c) 2008 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/

package org.jboss.tools.ws.ui.preferences;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TableColumn;
import org.jboss.tools.ws.ui.messages.JbossWSUIMessages;
import org.jboss.tools.ws.ui.utils.UIUtils;
import org.jboss.tools.ws.core.classpath.JbossWSRuntime;

/**
 * @author Grid Qian
 */
public class JbossRuntimeListFieldEditor extends BaseFieldEditor {

	// ------------------------------------------------------------------------
	// Layout parameters
	// ------------------------------------------------------------------------

	static final int GL_COLUMNS = 2;
	static final int GL_HINT_HEIGHT = 200;
	static final int TC_DEFAULT_WIDTH = 21;
	static final int TC_NAME_WIDTH = 100;
	static final int TC_VERSION_WIDTH = 50;
	static final int TC_PATH_WIDTH = 100;

	// ------------------------------------------------------------------------
	// Field declarations
	// ------------------------------------------------------------------------

	private CheckboxTableViewer tableView = null;

	private Composite root = null;

	private ActionPanel actionPanel;

	private Map<JbossWSRuntime, JbossWSRuntime> changed = new HashMap<JbossWSRuntime, JbossWSRuntime>();

	private JbossWSRuntime checkedElement = new JbossWSRuntime();

	private List<JbossWSRuntime> added = new ArrayList<JbossWSRuntime>();

	private List<JbossWSRuntime> removed = new ArrayList<JbossWSRuntime>();

	// ------------------------------------------------------------------------
	// Constructors
	// ------------------------------------------------------------------------

	/**
	 * Control for editing jbossWSRuntime list
	 * 
	 * @param name
	 *            String
	 * @param label
	 *            String
	 * @param defaultValue
	 *            Object
	 */
	public JbossRuntimeListFieldEditor(String name, String label,
			Object defaultValue) {
		super(name, label, defaultValue);
	}

	/**
	 * TBD
	 * 
	 * @return JbossWSRuntime;
	 */
	public JbossWSRuntime getDefaultJbossWSRuntime() {
		return checkedElement;
	}

	public void setDefaultJbossWSRuntime(JbossWSRuntime rt) {
		checkedElement = rt;
	}

	/**
	 * TBD
	 * 
	 * @return List&lt;JbossWSRuntime&gt;
	 */
	public List<JbossWSRuntime> getAddedJbossWSRuntimes() {
		return added;
	}

	/**
	 * TBD
	 * 
	 * @return List&lt;JbossWSRuntime&gt;
	 */
	public Map<JbossWSRuntime, JbossWSRuntime> getChangedJbossWSRuntimes() {
		return changed;
	}

	/**
	 * TBD
	 * 
	 * @return List&lt;JbossWSRuntime&gt;
	 */
	public List<JbossWSRuntime> getRemoved() {
		return removed;
	}

	/**
	 * TBD
	 * 
	 * @param composite
	 *            Object - instance of Composite
	 * @return Object[]
	 */
	@Override
	public Object[] getEditorControls(Object composite) {

		root = new Composite((Composite) composite, SWT.NONE);
		GridData gd = new GridData();
		gd.horizontalAlignment = GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		root.setLayoutData(gd);

		root.setLayout(new FormLayout());
		createTableView();
		createActionBar();

		FormData tableData = new FormData();
		tableData.left = new FormAttachment(0, 5);
		tableData.right = new FormAttachment(actionPanel, -5);
		tableData.top = new FormAttachment(0, 5);
		tableData.bottom = new FormAttachment(100, -5);
		tableView.getControl().setLayoutData(tableData);

		FormData actionsData = new FormData();
		actionsData.top = new FormAttachment(0, 5);
		actionsData.bottom = new FormAttachment(100, -5);
		actionsData.right = new FormAttachment(100, -5);
		actionPanel.setLayoutData(actionsData);
		return new Control[] { root };
	}

	@SuppressWarnings("unchecked")
	protected void createTableView() {
		tableView = CheckboxTableViewer.newCheckList(root, SWT.V_SCROLL
				| SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);

		TableColumn tc1 = new TableColumn(tableView.getTable(), SWT.CENTER);
		tc1.setWidth(TC_DEFAULT_WIDTH);
		tc1.setResizable(false);

		TableColumn tc2 = new TableColumn(tableView.getTable(), SWT.LEFT);
		tc2.setWidth(TC_NAME_WIDTH);
		tc2.setText(JbossWSUIMessages.JBossWS_Runtime_List_Field_Editor_Name);

		TableColumn tc3 = new TableColumn(tableView.getTable(), SWT.LEFT);
		tc3.setWidth(TC_PATH_WIDTH);
		tc3.setText(JbossWSUIMessages.JBossWS_Runtime_List_Field_Editor_Path);

		tableView.setContentProvider(new IStructuredContentProvider() {

			@SuppressWarnings("unchecked")
			public Object[] getElements(Object inputElement) {
				if (inputElement instanceof List) {
					return ((List<JbossWSRuntime>) inputElement).toArray();
				} else {
					throw new IllegalArgumentException(
							JbossWSUIMessages.JBossWS_Runtime_List_Field_Editor_Inputelement_Must_Be
									+ JbossWSUIMessages.JBossWS_Runtime_List_Field_Editor_An_Instance_Of_List);
				}
			}

			public void dispose() {
			}

			public void inputChanged(Viewer viewer, Object oldInput,
					Object newInput) {
				viewer.refresh();
			}
		});

		tableView.setLabelProvider(new ITableLabelProvider() {

			private static final int TC_DEFAULT_NUMBER = 0;
			private static final int TC_NAME_NUMBER = 1;
			private static final int TC_PATH_NUMBER = 2;

			public void addListener(ILabelProviderListener listener) {
			}

			public void dispose() {
			}

			public boolean isLabelProperty(Object element, String property) {
				return false;
			}

			public void removeListener(ILabelProviderListener listener) {
			}

			public Image getColumnImage(Object element, int columnIndex) {
				return null;
			}

			public String getColumnText(Object element, int columnIndex) {
				JbossWSRuntime rt = (JbossWSRuntime) element;
				if (columnIndex == TC_DEFAULT_NUMBER) {
					return ""; //$NON-NLS-1$
				}
				if (columnIndex == TC_NAME_NUMBER) {
					return rt.getName();
				}
				if (columnIndex == TC_PATH_NUMBER) {
					return rt.getHomeDir();
				}
				return ""; //$NON-NLS-1$
			}
		});

		tableView.setInput(getValue());
		tableView.getTable().setLinesVisible(true);
		tableView.getTable().setHeaderVisible(true);
		tableView.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				JbossWSRuntime selRt = (JbossWSRuntime) event.getElement();
				if (event.getChecked()) {
					JbossWSRuntime deselRt = null;
					Object[] selRts = tableView.getCheckedElements();

					for (int i = 0; i < selRts.length; i++) {
						JbossWSRuntime rt = (JbossWSRuntime) selRts[i];
						if (rt != selRt) {
							deselRt = rt;
							break;
						}
					}

					if (deselRt != null) {
						Object[] newChecked = new Object[selRts.length - 1];
						checkedElement = null;
						int i = 0;
						for (Object object : selRts) {
							JbossWSRuntime rt = (JbossWSRuntime) object;
							if (rt == selRt) {
								newChecked[i] = rt;
								checkedElement = rt;
								i++;
							}
						}
						tableView.setCheckedElements(newChecked);
					} else {
						checkedElement = (JbossWSRuntime) event.getElement();
					}
				} else {
					if (checkedElement == selRt) {
						checkedElement = null;
					}
				}
				pcs.firePropertyChange(getName(), null, getValue());
			}
		});

		for (JbossWSRuntime rt : (List<JbossWSRuntime>) getValue()) {
			if (rt.isDefault()) {
				tableView.setChecked(rt, true);
				checkedElement = rt;
			}
		}
	}

	protected void createActionBar() {
		actionPanel = new ActionPanel(root, new BaseAction[] { new AddAction(),
				new EditAction(), new RemoveAction() });
		tableView.addSelectionChangedListener(actionPanel);
	}

	/**
	 * Checks all runtimes and set default one if user did not do it.
	 */
	@SuppressWarnings("unchecked")
	private void setDefaultRuntime() {
		List<JbossWSRuntime> runtimes = (List<JbossWSRuntime>) getValue();
		boolean checked = false;
		for (JbossWSRuntime jbossWSRuntime : runtimes) {

			if (checkedElement == jbossWSRuntime) {
				checked = true;
				break;
			}
		}
		if (!checked) {
			tableView.setChecked(runtimes.get(0), true);
			checkedElement = runtimes.get(0);
		}

	}

	/**
	 * Return array of Controls that forms and editor
	 * 
	 * @return Control[]
	 */
	@Override
	public Object[] getEditorControls() {
		return new Control[] { root };
	}

	/**
	 * Return number of controls in editor
	 * 
	 * @return int
	 */
	@Override
	public int getNumberOfControls() {
		return 1;
	}

	/**
	 * Fill wizard page with editors
	 * 
	 * @param parent
	 *            Composite - parent composite
	 */
	@Override
	public void doFillIntoGrid(Object parent) {
		Assert.isTrue(parent instanceof Composite,
				JbossWSUIMessages.Error_JBossWS_Basic_Editor_Composite);
		Assert.isTrue(((Composite) parent).getLayout() instanceof GridLayout,
				JbossWSUIMessages.Error_JBossWS_Basic_Editor_Support);
		Composite aComposite = (Composite) parent;
		getEditorControls(aComposite);
		GridLayout gl = (GridLayout) ((Composite) parent).getLayout();

		GridData gd = new GridData();
		gd.horizontalSpan = gl.numColumns;
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalAlignment = GridData.FILL;

		((Control) getEditorControls()[0]).setLayoutData(gd);
	}

	/**
	 * Wizard page for editing JbossWS Runtime parameters
	 * 
	 */
	public static class JbossWSRuntimeWizardPage extends WizardPage implements
			PropertyChangeListener {

		private static final String SRT_NAME = "name";
		private static final String SRT_HOMEDIR = "homeDir";

		private static final int GL_PARENT_COLUMNS = 1;
		private static final int GL_CONTENT_COLUMNS = 3;

		List<JbossWSRuntime> value = null;

		IFieldEditor name = createTextEditor(SRT_NAME,
				JbossWSUIMessages.JBossWS_Runtime_List_Field_Editor_Name2, ""); //$NON-NLS-1$ 

		IFieldEditor homeDir = createBrowseFolderEditor(
				SRT_HOMEDIR,
				JbossWSUIMessages.JBossWS_Runtime_List_Field_Editor_Home_Folder,
				""); //$NON-NLS-1$ 

		JbossWSRuntime current = null;

		public JbossWSRuntimeWizardPage(List<JbossWSRuntime> editedList) {
			super(
					JbossWSUIMessages.JBossWS_Runtime_List_Field_Editor_New_Runtime);

			setMessage(JbossWSUIMessages.JBossWS_Runtime_List_Field_Editor_Create_A_Runtime);
			setTitle(JbossWSUIMessages.JBossWS_Runtime_List_Field_Editor_Runtime);
			value = editedList;
		}

		/**
		 * Create Wizard page content
		 * 
		 * @param parent
		 *            Composite - parent composite
		 */
		public void createControl(Composite parent) {
			parent.setLayout(new GridLayout(GL_PARENT_COLUMNS, false));
			GridData dg = new GridData();
			dg.horizontalAlignment = GridData.FILL;
			dg.grabExcessHorizontalSpace = true;
			Composite root = new Composite(parent, SWT.NONE);
			root.setLayoutData(dg);
			GridLayout gl = new GridLayout(GL_CONTENT_COLUMNS, false);
			root.setLayout(gl);
			name.doFillIntoGrid(root);
			name.addPropertyChangeListener(this);
			homeDir.doFillIntoGrid(root);
			homeDir.addPropertyChangeListener(this);
			setPageComplete(false);
			setControl(root);
		}

		/**
		 * Process evt: setup default values based on JbossWS Home folder and
		 * validate user input
		 * 
		 * @param evt
		 *            PropertyChangeEvent describes changes in wizard
		 */
		public void propertyChange(java.beans.PropertyChangeEvent evt) {
			if ("homeDir".equals(evt.getPropertyName())) {
				if (name.getValueAsString() == null
						|| "".equals(name.getValueAsString().trim())
						|| this.getErrorMessage() != null) {
					String homeDirName = homeDir.getValueAsString();
					if (homeDirName != null && !"".equals(homeDirName.trim())) {
						File folder = new File(homeDirName);
						homeDirName = folder.getName();
					}
					name.setValue(homeDirName);
				}
			}

			if (name.getValueAsString() == null || "".equals(//$NON-NLS-1$
					name.getValueAsString().toString().trim())) {
				setErrorMessage(JbossWSUIMessages.Error_JBossWS_Runtime_List_Field_Editor_Name_Cannot_Be_Empty);
				setPageComplete(false);
				return;
			}

			if (!name.getValueAsString().matches(
					"[a-zA-Z_][a-zA-Z0-9_\\-\\. ]*")) { //$NON-NLS-1$
				setErrorMessage(JbossWSUIMessages.Error_JBossWS_Runtime_List_Field_Editor_Runtime_Name_Is_Not_Correct);
				setPageComplete(false);
				return;
			}
			for (JbossWSRuntime rt : value) {
				if (current != null && current.getName().equals(rt.getName())) {
					continue;
				}
				if (rt.getName().equals(name.getValueAsString())) {
					setErrorMessage(JbossWSUIMessages.JBossWS_Runtime_List_Field_Editor_Runtime
							+ name.getValueAsString()
							+ JbossWSUIMessages.JBossWS_Runtime_List_Field_Editor_Already_Exists);
					setPageComplete(false);
					return;
				}
			}

			if (current != null
					&& current.getName().equals(name.getValueAsString())
					&& current.getHomeDir().equals(homeDir.getValueAsString())) {
				setErrorMessage(null);
				setPageComplete(false);
				return;
			}

			if (homeDir.getValueAsString() == null
					|| "".equals(homeDir.getValueAsString().trim())) { //$NON-NLS-1$
				setErrorMessage(JbossWSUIMessages.Error_JBossWS_Runtime_List_Field_Editor_Path_To_Home_Diretory_Cannot_Be_Empty);
				setPageComplete(false);
				return;
			}

			if (!runtimeExist(homeDir.getValueAsString())) {
				setErrorMessage(JbossWSUIMessages.Label_JBOSSWS_Runtime_Load_Error);
				setPageComplete(false);
				return;
			}

			setErrorMessage(null);
			setPageComplete(true);
		}

		/**
		 * Return JbossWS Runtime instance initialized by user input
		 * 
		 * @return JbossWSRuntime instance
		 */
		public JbossWSRuntime getRuntime() {
			JbossWSRuntime newRt = new JbossWSRuntime();
			newRt.setName(name.getValueAsString());
			newRt.setHomeDir(homeDir.getValueAsString());
			return newRt;
		}

		public IFieldEditor createTextEditor(String name, String label,
				String defaultValue) {
			CompositeEditor editor = new CompositeEditor(name, label,
					defaultValue);
			editor.addFieldEditors(new IFieldEditor[] {
					new LabelFieldEditor(name, label),
					new TextFieldEditor(name, label, defaultValue) });
			return editor;
		}

		public IFieldEditor createBrowseFolderEditor(String name, String label,
				String defaultValue) {
			CompositeEditor editor = new CompositeEditor(name, label,
					defaultValue);
			editor
					.addFieldEditors(new IFieldEditor[] {
							new LabelFieldEditor(name, label),
							new TextFieldEditor(name, label, defaultValue),
							new ButtonFieldEditor(
									name,
									createSelectFolderAction(JbossWSUIMessages.JBossWS_SWT_Field_Editor_Factory_Browse),
									defaultValue) });
			return editor;
		}

		public ButtonFieldEditor.ButtonPressedAction createSelectFolderAction(
				String buttonName) {
			return new ButtonFieldEditor.ButtonPressedAction(buttonName) {
				@Override
				public void run() {
					DirectoryDialog dialog = new DirectoryDialog(Display
							.getCurrent().getActiveShell());
					dialog.setFilterPath(getFieldEditor().getValueAsString());
					dialog
							.setMessage(JbossWSUIMessages.JBossWS_SWT_Field_Editor_Factory_Select_Home_Folder);
					dialog.setFilterPath(getFieldEditor().getValueAsString());
					String directory = dialog.open();
					if (directory != null) {
						getFieldEditor().setValue(directory);
					}
				}
			};
		}

		private boolean runtimeExist(String path) {

			File jbosswsHomeDir = new File(path);
			if (!jbosswsHomeDir.isDirectory())
				return false;
			String[] newNode = { JbossWSUIMessages.Bin,
					JbossWSUIMessages.Commond };
			String jbosswsBinPath = UIUtils.addNodesToPath(jbosswsHomeDir
					.getAbsolutePath(), newNode);
			if (new File(jbosswsBinPath).isFile()) {
				return true;
			}
			return false;
		}
	}

	/**
	 * Wizard collect information and creates new JbossWSRuntime instances.
	 * 
	 */
	public static class JbossWSRuntimeNewWizard extends Wizard {

		JbossWSRuntimeWizardPage page1 = null;
		List<JbossWSRuntime> added = null;
		List<JbossWSRuntime> value = null;

		public JbossWSRuntimeNewWizard(List<JbossWSRuntime> exist,
				List<JbossWSRuntime> added) {
			super();
			setWindowTitle(JbossWSUIMessages.JBossWS_Runtime_List_Field_Editor_New_Runtime);
			page1 = new JbossWSRuntimeWizardPage(exist);
			addPage(page1);
			this.value = exist;
			this.added = added;
		}

		/**
		 * Do finish steps
		 * 
		 * @return boolean
		 */
		@Override
		public boolean performFinish() {
			JbossWSRuntime rt = page1.getRuntime();
			added.add(rt);
			value.add(rt);
			
			return true;
		}
		
	}

	/**
	 * Wizard for editing JbossWS Runtime parameters: name and path to
	 * home folder
	 * 
	 */
	public static class JbossWSRuntimeEditWizard extends Wizard {
		JbossWSRuntimeWizardPage page1 = null;
		List<JbossWSRuntime> added = null;
		Map<JbossWSRuntime, JbossWSRuntime> changed = null;
		List<JbossWSRuntime> value = null;
		JbossWSRuntime source = null;

		/**
		 * Constructor with almost all initialization parameters
		 * 
		 * @param existing
		 *            List&lt;JbossWSRuntime&gt; - edited list of JbossWS
		 *            Runtimes
		 * @param source
		 *            JbossWSRuntime - edited JbossWS Runtime
		 * @param added
		 *            List&lt;JbossWSRuntime&gt; - TBD
		 * @param changed
		 *            List&lt;JbossWSRuntime&gt; - TBD
		 */
		public JbossWSRuntimeEditWizard(List<JbossWSRuntime> existing,
				JbossWSRuntime source, List<JbossWSRuntime> added,
				Map<JbossWSRuntime, JbossWSRuntime> changed) {
			super();
			setWindowTitle(JbossWSUIMessages.JBossWS_Runtime_List_Field_Editor_Edit_Runtime);
			page1 = new JbossWSRuntimeWizardPage(existing);
			page1
					.setMessage(JbossWSUIMessages.JBossWS_Runtime_List_Field_Editor_Modify_Runtime);
			page1
					.setTitle(JbossWSUIMessages.JBossWS_Runtime_List_Field_Editor_Edit_Runtime);
			addPage(page1);
			this.value = existing;
			this.added = added;
			this.changed = changed;
			this.source = source;
			page1.name.setValue(source.getName());
			page1.homeDir.setValue(source.getHomeDir());
			page1.current = source;
		}

		/**
		 * Perform operations to finish editing JbossWS Runtime parameters
		 * 
		 * @return boolean - always true
		 */
		@Override
		public boolean performFinish() {
			JbossWSRuntime rt = page1.getRuntime();
			if (rt.getName().equals(source.getName())
					&& rt.getHomeDir().equals(source.getHomeDir())) {
				return true;
			}
			if (added.contains(source) || changed.containsKey(source)) {
				source.setName(rt.getName());
				source.setHomeDir(rt.getHomeDir());
			} else {
				changed.put(rt, source);
				int i = value.indexOf(source);
				if (i >= 0) {
					value.set(i, rt);
				} else {
					value.remove(source);
					value.add(rt);
				}
			}
			return true;
		}
	}

	/**
	 * Composite that holds list of BaseActions and presents them as column of
	 * buttons
	 * 
	 */
	public static class ActionPanel extends Composite implements
			ISelectionChangedListener {

		private BaseAction[] actions = null;

		/**
		 * Constructor creates panel with style, grid layout and buttons
		 * represented the actions
		 * 
		 * @param parent
		 *            Composite
		 * @param style
		 *            int
		 * @param actions
		 *            BaseAction[]
		 */
		public ActionPanel(Composite parent, int style, BaseAction[] actions) {
			super(parent, style);
			this.actions = actions;
			setLayout(new GridLayout(1, false));
			for (BaseAction action : this.actions) {
				new ActionButton(this, SWT.PUSH, action);
			}
		}

		/**
		 * Constructor creates panel with default style, grid layout and buttons
		 * represented the actions
		 * 
		 * @param parent
		 *            Composite
		 * @param actions
		 *            BaseAction[]
		 */
		public ActionPanel(Composite parent, BaseAction[] actions) {
			this(parent, SWT.NONE, actions);
		}

		/**
		 * Listen to the selection changes and update actions state
		 * (enable/disable)
		 * 
		 * @param event
		 *            SelectionChangeEvent
		 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
		 */
		public void selectionChanged(SelectionChangedEvent event) {
			for (BaseAction action : actions) {
				action.setSelection(event.getSelection());
			}
		}
	}

	/**
	 * Class represents an BaseAction as SWT button control and runs action when
	 * button is prtessed
	 * 
	 */
	public static class ActionButton implements IPropertyChangeListener {

		private Button button;
		private BaseAction action;

		/**
		 * Create Button control with parent control and style that represents
		 * action
		 * 
		 * @param parent
		 *            Composite
		 * @param style
		 *            int
		 * @param action
		 *            BaseAction
		 */
		public ActionButton(Composite parent, int style, BaseAction action) {
			this.button = new Button(parent, style);
			this.action = action;

			GridData gd = new GridData(GridData.FILL_HORIZONTAL,
					GridData.CENTER, false, false);

			gd.horizontalAlignment = GridData.FILL;
			gd.verticalAlignment = GridData.VERTICAL_ALIGN_BEGINNING;
			this.button.setLayoutData(gd);
			this.action.addPropertyChangeListener(this);
			this.button.setText(action.getText());
			this.button.setEnabled(action.isEnabled());
			this.button.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					ActionButton.this.action.run();
				}

				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});

		}

		/**
		 * Return SWT button control that calls provided action
		 * 
		 * @return Control - button swt control
		 */
		public Control getControl() {
			return button;
		}

		/**
		 * Update enabled/disabled button state
		 * 
		 * @param event
		 *            PropertyChangeEvent
		 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
		 */
		public void propertyChange(PropertyChangeEvent event) {
			if (event.getProperty().equals(IAction.ENABLED)) {
				button.setEnabled(((Boolean) event.getNewValue())
						.booleanValue());
			}
		}
	}

	/**
	 * Action that changes state enable/disable based on current table selection
	 * 
	 */
	public abstract class BaseAction extends Action {

		JbossWSRuntime[] runtimes = new JbossWSRuntime[0];

		/**
		 * Constructor creates action with provided name
		 * 
		 * @param name
		 *            String - action name
		 */
		public BaseAction(String name) {
			super(name);
			updateEnablement();
		}

		/**
		 * Set current selection
		 * 
		 * @param selection
		 *            ISelection - selected items
		 */
		public void setSelection(ISelection selection) {
			if (selection instanceof IStructuredSelection) {
				List<JbossWSRuntime> rts = new ArrayList<JbossWSRuntime>();
				for (Object rt : ((IStructuredSelection) selection).toArray()) {
					rts.add((JbossWSRuntime) rt);
				}
				runtimes = rts.toArray(new JbossWSRuntime[] {});
			} else {
				runtimes = new JbossWSRuntime[0];
			}
			updateEnablement();
		}

		protected abstract void updateEnablement();
	}

	/**
	 * Action that invokes New JbossWS Runtime Dialog
	 * 
	 */
	public class AddAction extends BaseAction {

		static final String ACTION_NAME = "&Add";

		/**
		 * Constructor create Add action with default name
		 */
		public AddAction() {
			super(ACTION_NAME);
			// This action is always available
			setEnabled(true);
		}

		/**
		 * Do nothing, because Add action should be always available
		 */
		@Override
		protected void updateEnablement() {
			// Add button is always available
		}

		/**
		 * Invoke New JbossWS Runtime Dialog
		 * 
		 * @see org.eclipse.jface.action.Action#run()
		 */
		@SuppressWarnings("unchecked")
		@Override
		public void run() {
			Wizard wiz = new JbossWSRuntimeNewWizard(
					(List<JbossWSRuntime>) getValue(), added);
			WizardDialog dialog = new WizardDialog(Display.getCurrent()
					.getActiveShell(), wiz);
			dialog.open();
			tableView.refresh();
			setDefaultRuntime();
		}
	}

	/**
	 * Action starts an editing selected JbossWS Runtime in Edit JbossWS Runtime
	 * dialog
	 * 
	 */
	public class EditAction extends BaseAction {

		static final String ACTION_NAME = "&Edit";

		/**
		 * Create EditAction with default name
		 * 
		 * @param text
		 */
		public EditAction() {
			super(ACTION_NAME);
		}

		/**
		 * Edit action is enabled when the only JbossWS Runtime is selected
		 */
		@Override
		protected void updateEnablement() {
			// available when the only JbossWSRuntime is selected
			setEnabled(runtimes.length == 1);
		}

		/**
		 * Start editing selected JbossWS Runtime in Edit JbossWS Runtime Wizard
		 * Dialog
		 * 
		 * @see org.eclipse.jface.action.Action#run()
		 */
		@SuppressWarnings("unchecked")
		@Override
		public void run() {
			JbossWSRuntime selected = runtimes[0];
			Wizard wiz = new JbossWSRuntimeEditWizard(
					(List<JbossWSRuntime>) getValue(), runtimes[0], added,
					changed);
			WizardDialog dialog = new WizardDialog(Display.getCurrent()
					.getActiveShell(), wiz);
			dialog.open();
			tableView.refresh();
			if (changed.containsValue(selected)) {
				JbossWSRuntime c = findChangedRuntime(selected);
				if (c != null) {
					tableView.setSelection(new StructuredSelection(c));
				}
			}
		}

		private JbossWSRuntime findChangedRuntime(JbossWSRuntime source) {
			for (JbossWSRuntime r : changed.keySet()) {
				if (source == changed.get(r)) {
					return r;
				}
			}
			return null;
		}
	}

	/**
	 * Action deletes all selected JbossWS Runtimes. A warning message is shown
	 * for used JbossWS Runtimes
	 * 
	 */
	public class RemoveAction extends BaseAction {

		static final String ACTION_NAME = "&Remove";

		/**
		 * Create DeleteAction action with default name
		 */
		public RemoveAction() {
			super(ACTION_NAME);
		}

		@Override
		protected void updateEnablement() {
			setEnabled(runtimes.length > 0);
		}

		/**
		 * Remove all selected JbossWS Runtimes one by one
		 * 
		 * @see org.eclipse.jface.action.Action#run()
		 */
		@Override
		public void run() {
			for (JbossWSRuntime rt : runtimes) {
				removeRuntime(rt);
			}
			tableView.refresh();
			setDefaultRuntime();
		}

		@SuppressWarnings("unchecked")
		private void removeRuntime(JbossWSRuntime r) {
			boolean used = isRuntimeUsed(r.getName());
			String title = JbossWSUIMessages.JBossWS_Runtime_Delete_Confirm_Title;
			String message = (used) ? NLS.bind(
					JbossWSUIMessages.JBossWS_Runtime_Delete_Used_Confirm, r
							.getName()) : NLS.bind(
					JbossWSUIMessages.JBossWS_Runtime_Delete_Not_Used_Confirm,
					r.getName());
			boolean b = MessageDialog.openConfirm(tableView.getControl()
					.getShell(), title, message);
			if (b) {
				if (changed.containsKey(r)) {
					r = changed.remove(r);
				}
				removed.add(r);
				if (added.contains(r)) {
					added.remove(r);
				}
				((List) getValue()).remove(r);
			}
			if (checkedElement == r) {
				checkedElement = null;
			}
		}

		private boolean isRuntimeUsed(String runtimeName) {
			// IProject[] ps = ResourcesPlugin.getWorkspace().getRoot()
			// .getProjects();
			// for (int i = 0; i < ps.length; i++) {
			// ISeamProject sp = SeamCorePlugin.getSeamProject(ps[i], false);
			// if (sp != null && runtimeName.equals(sp.getRuntimeName())) {
			// return true;
			// }
			// }
			return false;
		}
	}
}