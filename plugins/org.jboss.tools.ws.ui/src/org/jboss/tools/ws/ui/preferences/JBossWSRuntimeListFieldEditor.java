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
import java.util.Arrays;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.jboss.tools.common.ui.widget.editor.BaseFieldEditor;
import org.jboss.tools.common.ui.widget.editor.ButtonFieldEditor;
import org.jboss.tools.common.ui.widget.editor.IFieldEditor;
import org.jboss.tools.common.ui.widget.editor.IFieldEditorFactory;
import org.jboss.tools.ws.core.classpath.JBossWSRuntime;
import org.jboss.tools.ws.core.classpath.JBossWSRuntimeManager;
import org.jboss.tools.ws.ui.messages.JBossWSUIMessages;
import org.jboss.tools.ws.ui.utils.JBossWSUIUtils;

/**
 * @author Grid Qian
 */
public class JBossWSRuntimeListFieldEditor extends BaseFieldEditor {

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

	private Map<JBossWSRuntime, JBossWSRuntime> changed = new HashMap<JBossWSRuntime, JBossWSRuntime>();

	private JBossWSRuntime checkedElement = new JBossWSRuntime();

	private List<JBossWSRuntime> added = new ArrayList<JBossWSRuntime>();

	private List<JBossWSRuntime> removed = new ArrayList<JBossWSRuntime>();

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
	public JBossWSRuntimeListFieldEditor(String name, String label,
			Object defaultValue) {
		super(name, label, defaultValue);
	}

	/**
	 * TBD
	 * 
	 * @return JBossWSRuntime;
	 */
	public JBossWSRuntime getDefaultJBossWSRuntime() {
		return checkedElement;
	}

	public void setDefaultJBossWSRuntime(JBossWSRuntime rt) {
		checkedElement = rt;
	}

	/**
	 * TBD
	 * 
	 * @return List&lt;JBossWSRuntime&gt;
	 */
	public List<JBossWSRuntime> getAddedJBossWSRuntimes() {
		return added;
	}

	/**
	 * TBD
	 * 
	 * @return List&lt;JBossWSRuntime&gt;
	 */
	public Map<JBossWSRuntime, JBossWSRuntime> getChangedJBossWSRuntimes() {
		return changed;
	}

	/**
	 * TBD
	 * 
	 * @return List&lt;JBossWSRuntime&gt;
	 */
	public List<JBossWSRuntime> getRemoved() {
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
		tc2.setText(JBossWSUIMessages.JBossWS_Runtime_List_Field_Editor_Name);

		TableColumn tc3 = new TableColumn(tableView.getTable(), SWT.LEFT);
		tc3.setWidth(TC_VERSION_WIDTH);
		tc3.setText(JBossWSUIMessages.JBossWS_Runtime_List_Field_Editor_Version);

		TableColumn tc4 = new TableColumn(tableView.getTable(), SWT.LEFT);
		tc4.setWidth(TC_PATH_WIDTH);
		tc4.setText(JBossWSUIMessages.JBossWS_Runtime_List_Field_Editor_Path);

		tableView.setContentProvider(new IStructuredContentProvider() {

			public Object[] getElements(Object inputElement) {
				if (inputElement instanceof List) {
					return ((List<JBossWSRuntime>) inputElement).toArray();
				} else {
					throw new IllegalArgumentException(
							JBossWSUIMessages.Error_JBossWS_Runtime_List_Field_Editor_Inputelement_Must_Be_An_Instance_Of_List);
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
			private static final int TC_VERSION_NUMBER = 2;
			private static final int TC_PATH_NUMBER = 3;

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
				JBossWSRuntime rt = (JBossWSRuntime) element;
				if (columnIndex == TC_DEFAULT_NUMBER) {
					return ""; //$NON-NLS-1$
				}
				if (columnIndex == TC_NAME_NUMBER) {
					return rt.getName();
				}
				if (columnIndex == TC_VERSION_NUMBER) {
					return rt.getVersion().toString();
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
				JBossWSRuntime selRt = (JBossWSRuntime) event.getElement();
				if (event.getChecked()) {
					JBossWSRuntime deselRt = null;
					Object[] selRts = tableView.getCheckedElements();

					for (int i = 0; i < selRts.length; i++) {
						JBossWSRuntime rt = (JBossWSRuntime) selRts[i];
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
							JBossWSRuntime rt = (JBossWSRuntime) object;
							if (rt == selRt) {
								newChecked[i] = rt;
								checkedElement = rt;
								i++;
							}
						}
						tableView.setCheckedElements(newChecked);
					} else {
						checkedElement = (JBossWSRuntime) event.getElement();
					}
				} else {
					if (checkedElement == selRt) {
						checkedElement = null;
					}
				}
				pcs.firePropertyChange(getName(), null, getValue());
			}
		});

		for (JBossWSRuntime rt : (List<JBossWSRuntime>) getValue()) {
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
		List<JBossWSRuntime> runtimes = (List<JBossWSRuntime>) getValue();
		boolean checked = false;
		for (JBossWSRuntime jbossWSRuntime : runtimes) {
			if (checkedElement == jbossWSRuntime) {
				checked = true;
				tableView.setChecked(checkedElement, true);
				break;
			}
		}
		if (!checked && runtimes.size() > 0) {
			if (tableView.getCheckedElements() == null
					|| tableView.getCheckedElements().length == 0) {
				tableView.setChecked(runtimes.get(0), true);
				checkedElement = runtimes.get(0);
			}
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
				JBossWSUIMessages.Error_JBossWS_Basic_Editor_Composite);
		Assert.isTrue(((Composite) parent).getLayout() instanceof GridLayout,
				JBossWSUIMessages.Error_JBossWS_Basic_Editor_Support);
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
	 * Wizard page for editing JBossWS Runtime parameters
	 * 
	 */
	public static class JBossWSRuntimeWizardPage extends WizardPage implements
			PropertyChangeListener {

		private static final String SRT_NAME = "name"; //$NON-NLS-1$
		private static final String SRT_VERSION = "version"; //$NON-NLS-1$
		private static final String SRT_HOMEDIR = "homeDir"; //$NON-NLS-1$

		private static final int GL_PARENT_COLUMNS = 1;
		private static final int GL_CONTENT_COLUMNS = 3;

		List<JBossWSRuntime> value = null;

		IFieldEditor name = IFieldEditorFactory.INSTANCE.createTextEditor(SRT_NAME,
				JBossWSUIMessages.JBossWS_Runtime_List_Field_Editor_Name2, ""); //$NON-NLS-1$ 

		IFieldEditor version = IFieldEditorFactory.INSTANCE.createComboEditor(SRT_VERSION,
				JBossWSUIMessages.JBossWS_Runtime_List_Field_Editor_Version, getWSFacetVersions(), ""); //$NON-NLS-1$ 

		IFieldEditor homeDir = IFieldEditorFactory.INSTANCE.createBrowseFolderEditor(SRT_HOMEDIR,
			    JBossWSUIMessages.JBossWS_Runtime_List_Field_Editor_Home_Folder,""); //$NON-NLS-1$ 
		
		Label impl;
		Label vDetail;

		JBossWSRuntime current = null;
		IFieldEditor jars = null;

		public JBossWSRuntimeWizardPage(List<JBossWSRuntime> editedList) {
			super(JBossWSUIMessages.JBossWS_Runtime_List_Field_Editor_New_Runtime);
			setMessage(JBossWSUIMessages.JBossWS_Runtime_List_Field_Editor_Create_A_Runtime);
			setTitle(JBossWSUIMessages.JBossWS_Runtime_List_Field_Editor_Runtime);
			value = editedList;
		}

		/**
		 * Create Wizard page content
		 * 
		 * @param parent Composite - parent composite
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
			version.doFillIntoGrid(root);
			version.addPropertyChangeListener(this);
			if (current != null)
				version.setValue(current.getVersion());
			else
				version.setValue(getWSFacetVersions().get(1));
			homeDir.doFillIntoGrid(root);
			homeDir.addPropertyChangeListener(this);
			
			Label[] texts = JBossWSUIUtils.createWSRuntimeDetailsGroup(root, 3);
			impl = texts[0];
			vDetail = texts[1];
			if (current != null) {
			    impl.setText(current.getImpl());
			    vDetail.setText(current.getVersionDetail());
			}

			jars = new JBossWSLibraryListFieldEditor("", "", current); //$NON-NLS-1$ //$NON-NLS-2$
			jars.doFillIntoGrid(root);
						
			jars.addPropertyChangeListener(this);
			setPageComplete(false);
			setControl(root);
		}

		/**
		 * Process evt: setup default values based on JBossWS Home folder and
		 * validate user input
		 * 
		 * @param evt PropertyChangeEvent describes changes in wizard
		 */
		public void propertyChange(java.beans.PropertyChangeEvent evt) {
			if ("homeDir".equals(evt.getPropertyName())) { //$NON-NLS-1$
				if (name.getValueAsString() == null || "".equals(name.getValueAsString().trim()) //$NON-NLS-1$
						|| this.getErrorMessage() != null) {
					String homeDirName = homeDir.getValueAsString();
					if (homeDirName != null && !"".equals(homeDirName.trim())) { //$NON-NLS-1$
						File folder = new File(homeDirName);
						homeDirName = folder.getName();
					}
					name.setValue(homeDirName);					
				}
			}

			if (name.getValueAsString() == null || "".equals(//$NON-NLS-1$
					name.getValueAsString().toString().trim())) {
				setErrorMessage(JBossWSUIMessages.Error_JBossWS_Runtime_List_Field_Editor_Name_Cannot_Be_Empty);
				setPageComplete(false);
				return;
			}

			if (!name.getValueAsString().matches("[a-zA-Z_][a-zA-Z0-9_\\-\\. ]*")) { //$NON-NLS-1$
				setErrorMessage(JBossWSUIMessages.Error_JBossWS_Runtime_List_Field_Editor_Runtime_Name_Is_Not_Correct);
				setPageComplete(false);
				return;
			}
			for (JBossWSRuntime rt : value) {
				if (current != null && current.getName().equals(rt.getName())) {
					continue;
				}
				if (rt.getName().equals(name.getValueAsString())) {
					setErrorMessage(NLS.bind(JBossWSUIMessages.JBossWS_Runtime_List_Field_Editor_Runtime_Already_Exists,
									name.getValueAsString()));
					setPageComplete(false);
					return;
				}
			}

			if (jars != null) {
				JBossWSRuntime jarJbws = (JBossWSRuntime) jars.getValue();
				if (current != null
						&& current.getName().equals(name.getValueAsString())
						&& current.getHomeDir().equals(homeDir.getValueAsString())
						&& current.getVersion().equals(version.getValueAsString())
						&& current.isUserConfigClasspath() == jarJbws.isUserConfigClasspath()
						&& (!jarJbws.isUserConfigClasspath() || hasSameLibraies(
								current.getLibraries(), jarJbws.getLibraries()))) {	
					setErrorMessage(null);
					setPageComplete(false);
					return;
				}
	
				if (jarJbws.isUserConfigClasspath()&& jarJbws.getLibraries().size() == 0) {
					setErrorMessage(JBossWSUIMessages.JBossRuntimeListFieldEditor_ErrorMessageAtLeastOneJar);
					setPageComplete(false);
					return;
				}
			}

			if (homeDir.getValueAsString() == null || "".equals(homeDir.getValueAsString().trim())) { //$NON-NLS-1$
				setErrorMessage(JBossWSUIMessages.Error_JBossWS_Runtime_List_Field_Editor_Path_To_Home_Diretory_Cannot_Be_Empty);
				setPageComplete(false);
				return;
			} 

			if (!runtimeExist(homeDir.getValueAsString())) {
				setErrorMessage(JBossWSUIMessages.Error_JBossWS_Label_Runtime_Load);
				setPageComplete(false);
				return;
			} else if ("homeDir".equals(evt.getPropertyName())) { //$NON-NLS-1$
				String[] strs = JBossWSUIUtils.getWSRuntimeDetail(homeDir.getValueAsString());
				impl.setText(strs[0]);
				vDetail.setText(strs[1]);
			}
			
			setErrorMessage(null);
			setPageComplete(true);
		}

		private boolean hasSameLibraies(List<String> lib1, List<String> lib2) {
			if (lib1.size() != lib2.size())
				return false;
			for (String jar : lib1) {
				if (!lib2.contains(jar))
					return false;
			}
			return true;
		}

		/*
		 * get facet version.
		 */
		private List<String> getWSFacetVersions() {
			List<String> versions = new ArrayList<String>();
			IProjectFacet wsFacet = ProjectFacetsManager.getProjectFacet("jbossws.core"); //$NON-NLS-1$
			for (IProjectFacetVersion version : wsFacet.getVersions()) {
				versions.add(version.getVersionString());
			}
			return versions;
		}

		/**
		 * Return JBossWS Runtime instance initialized by user input
		 * 
		 * @return JBossWSRuntime instance
		 */
		public JBossWSRuntime getRuntime() {
			JBossWSRuntime newRt = new JBossWSRuntime();
			newRt.setName(name.getValueAsString());
			newRt.setVersion(version.getValueAsString());
			newRt.setHomeDir(homeDir.getValueAsString());
			newRt.setImpl(impl.getText());
			newRt.setVersionDetail(vDetail.getText());
			JBossWSRuntime rt = (JBossWSRuntime) jars.getValue();
			newRt.setLibraries(rt.getLibraries());
			newRt.setUserConfigClasspath(rt.isUserConfigClasspath());
			return newRt;
		}

		public ButtonFieldEditor.ButtonPressedAction createSelectFolderAction(
				String buttonName) {
			return new ButtonFieldEditor.ButtonPressedAction(buttonName) {
				@Override
				public void run() {
					DirectoryDialog dialog = new DirectoryDialog(Display.getCurrent().getActiveShell());
					dialog.setFilterPath(getFieldEditor().getValueAsString());
					dialog.setMessage(JBossWSUIMessages.JBossWS_SWT_Field_Editor_Factory_Select_Home_Folder);
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
			String[] newNode = { JBossWSUIMessages.Bin, JBossWSUIMessages.Command };
			String jbosswsBinPath = JBossWSUIUtils.addNodesToPath(
					jbosswsHomeDir.getAbsolutePath(), newNode);
			if (new File(jbosswsBinPath).isFile()) {
				return true;
			}
			return false;
		}
	}

	/**
	 * Wizard collect information and creates new JBossWSRuntime instances.
	 * 
	 */
	public static class JBossWSRuntimeNewWizard extends Wizard {

		JBossWSRuntimeWizardPage page1 = null;
		List<JBossWSRuntime> added = null;
		List<JBossWSRuntime> value = null;

		public JBossWSRuntimeNewWizard(List<JBossWSRuntime> exist,List<JBossWSRuntime> added) {
			super();
			setWindowTitle(JBossWSUIMessages.JBossWS_Runtime_List_Field_Editor_New_Runtime);
			page1 = new JBossWSRuntimeWizardPage(exist);
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
			JBossWSRuntime rt = page1.getRuntime();
			added.add(rt);
			value.add(rt);

			return true;
		}

		protected JBossWSRuntime getRuntime() {
			return page1.getRuntime();
		}

	}

	/**
	 * Wizard for editing JBossWS Runtime parameters: name and path to home
	 * folder
	 * 
	 */
	public static class JBossWSRuntimeEditWizard extends Wizard {
		JBossWSRuntimeWizardPage page1 = null;
		List<JBossWSRuntime> added = null;
		Map<JBossWSRuntime, JBossWSRuntime> changed = null;
		List<JBossWSRuntime> value = null;
		JBossWSRuntime source = null;

		/**
		 * Constructor with almost all initialization parameters
		 * 
		 * @param existing
		 *            List&lt;JBossWSRuntime&gt; - edited list of JBossWS
		 *            Runtimes
		 * @param source
		 *            JBossWSRuntime - edited JBossWS Runtime
		 * @param added
		 *            List&lt;JBossWSRuntime&gt; - TBD
		 * @param changed
		 *            List&lt;JBossWSRuntime&gt; - TBD
		 */
		public JBossWSRuntimeEditWizard(List<JBossWSRuntime> existing,
				JBossWSRuntime source, List<JBossWSRuntime> added,
				Map<JBossWSRuntime, JBossWSRuntime> changed) {
			super();
			setWindowTitle(JBossWSUIMessages.JBossWS_Runtime_List_Field_Editor_Edit_Runtime);
			page1 = new JBossWSRuntimeWizardPage(existing);
			page1.setMessage(JBossWSUIMessages.JBossWS_Runtime_List_Field_Editor_Modify_Runtime);
			page1.setTitle(JBossWSUIMessages.JBossWS_Runtime_List_Field_Editor_Edit_Runtime);
			addPage(page1);
			this.value = existing;
			this.added = added;
			this.changed = changed;
			this.source = source;
			page1.name.setValue(source.getName());
			page1.version.setValue(source.getVersion());
			page1.homeDir.setValue(source.getHomeDir());
			page1.current = source;
		}

		/**
		 * Perform operations to finish editing JBossWS Runtime parameters
		 * 
		 * @return boolean - always true
		 */
		@Override
		public boolean performFinish() {
			JBossWSRuntime rt = page1.getRuntime();

			if (added.contains(source) || changed.containsKey(source)) {
				source.setName(rt.getName());
				source.setHomeDir(rt.getHomeDir());
				source.setVersion(rt.getVersion());
				source.setUserConfigClasspath(rt.isUserConfigClasspath());
				source.setLibraries(rt.getLibraries());
				source.setImpl(rt.getImpl());
				source.setVersionDetail(rt.getVersionDetail());
			} else {
				changed.put(rt, source);
				if (source.isDefault()) {
					rt.setDefault(true);
				}
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
			if (actions == null) {
				this.actions = new BaseAction[0];
			} else {
				this.actions = Arrays.copyOf(actions, actions.length);
			}
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

		JBossWSRuntime[] runtimes = new JBossWSRuntime[0];

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
				List<JBossWSRuntime> rts = new ArrayList<JBossWSRuntime>();
				for (Object rt : ((IStructuredSelection) selection).toArray()) {
					rts.add((JBossWSRuntime) rt);
				}
				runtimes = rts.toArray(new JBossWSRuntime[] {});
			} else {
				runtimes = new JBossWSRuntime[0];
			}
			updateEnablement();
		}

		protected abstract void updateEnablement();
	}

	/**
	 * Action that invokes New JBossWS Runtime Dialog
	 * 
	 */
	public class AddAction extends BaseAction {

		/**
		 * Constructor create Add action with default name
		 */
		public AddAction() {
			super(JBossWSUIMessages.JBossRuntimeListFieldEditor_ActionAdd);
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
		 * Invoke New JBossWS Runtime Dialog
		 * 
		 * @see org.eclipse.jface.action.Action#run()
		 */
		@SuppressWarnings("unchecked")
		@Override
		public void run() {
			Wizard wiz = new JBossWSRuntimeNewWizard(
					(List<JBossWSRuntime>) getValue(), added);
			WizardDialog dialog = new WizardDialog(Display.getCurrent()
					.getActiveShell(), wiz);
			dialog.open();
			tableView.refresh();
			setDefaultRuntime();
		}
	}

	/**
	 * Action starts an editing selected JBossWS Runtime in Edit JBossWS Runtime
	 * dialog
	 * 
	 */
	public class EditAction extends BaseAction {

		/**
		 * Create EditAction with default name
		 * 
		 * @param text
		 */
		public EditAction() {
			super(JBossWSUIMessages.JBossRuntimeListFieldEditor_ActionEdit);
		}

		/**
		 * Edit action is enabled when the only JBossWS Runtime is selected
		 */
		@Override
		protected void updateEnablement() {
			// available when the only JBossWSRuntime is selected
			setEnabled(runtimes.length == 1);
		}

		/**
		 * Start editing selected JBossWS Runtime in Edit JBossWS Runtime Wizard
		 * Dialog
		 * 
		 * @see org.eclipse.jface.action.Action#run()
		 */
		@SuppressWarnings("unchecked")
		@Override
		public void run() {
			JBossWSRuntime selected = runtimes[0];
			Wizard wiz = new JBossWSRuntimeEditWizard(
					(List<JBossWSRuntime>) getValue(), runtimes[0], added,
					changed);
			WizardDialog dialog = new WizardDialog(Display.getCurrent()
					.getActiveShell(), wiz);
			dialog.open();
			tableView.refresh();
			JBossWSRuntime c = null;
			if (changed.containsValue(selected)) {
				c = findChangedRuntime(selected);
				if (c != null) {
					tableView.setSelection(new StructuredSelection(c));
				}
			}
			if (c != null && c.isDefault()) {
				checkedElement = c;
			}
			setDefaultRuntime();
		}

		private JBossWSRuntime findChangedRuntime(JBossWSRuntime source) {
			for (JBossWSRuntime r : changed.keySet()) {
				if (source == changed.get(r)) {
					return r;
				}
			}
			return null;
		}
	}

	/**
	 * Action deletes all selected JBossWS Runtimes. A warning message is shown
	 * for used JBossWS Runtimes
	 * 
	 */
	public class RemoveAction extends BaseAction {

		/**
		 * Create DeleteAction action with default name
		 */
		public RemoveAction() {
			super(JBossWSUIMessages.JBossRuntimeListFieldEditor_ActionRemove);
		}

		@Override
		protected void updateEnablement() {
			setEnabled(runtimes.length > 0);
		}

		/**
		 * Remove all selected JBossWS Runtimes one by one
		 * 
		 * @see org.eclipse.jface.action.Action#run()
		 */
		@Override
		public void run() {
			for (JBossWSRuntime rt : runtimes) {
				removeRuntime(rt);
			}
			tableView.refresh();
			setDefaultRuntime();
		}

		private void removeRuntime(JBossWSRuntime r) {
			boolean used = JBossWSRuntimeManager.isRuntimeUsed(r.getName());
			String title = JBossWSUIMessages.JBossWS_Runtime_Delete_Confirm_Title;
			String message = (used) ? NLS.bind(
					JBossWSUIMessages.JBossWS_Runtime_Delete_Used_Confirm, r
							.getName()) : NLS.bind(
					JBossWSUIMessages.JBossWS_Runtime_Delete_Not_Used_Confirm,
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
				((List<?>) getValue()).remove(r);
			}
			if (checkedElement == r) {
				checkedElement = null;
			}
		}
	}
	
	public CheckboxTableViewer getTableView() {
		return tableView;
	}
}