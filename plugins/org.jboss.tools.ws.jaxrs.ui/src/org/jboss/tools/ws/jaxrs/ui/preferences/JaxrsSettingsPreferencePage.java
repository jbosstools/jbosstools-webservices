/******************************************************************************* 
 * Copyright (c) 2012 - 2017 Red Hat, Inc. and others.
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.ws.jaxrs.ui.preferences;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jboss.tools.common.ui.preferences.SettingsPage;
import org.jboss.tools.common.ui.widget.editor.IFieldEditor;
import org.jboss.tools.common.ui.widget.editor.IFieldEditorFactory;
import org.jboss.tools.ws.jaxrs.core.configuration.ProjectNatureUtils;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;

/**
 * @author Alexey Kazakov
 * @author Jeff Maury
 */
public class JaxrsSettingsPreferencePage extends SettingsPage {

	public static final String ID = "org.jboss.tools.ws.jaxrs.ui.preferences.JaxrsSettingsPreferencePage";

	private IProject project;
	private boolean jaxrsEnabled;
	private boolean initialState;

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.PropertyPage#setElement(org.eclipse.core.runtime.IAdaptable)
	 */
	@Override
	public void setElement(IAdaptable element) {
		super.setElement(element);
		project = (IProject) getElement().getAdapter(IProject.class);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {
		Composite root = new Composite(parent, SWT.NONE);
		try {

		GridData gd = new GridData();

		gd.horizontalSpan = 1;
		gd.horizontalAlignment = GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = false;

		GridLayout gridLayout = new GridLayout(1, false);
		root.setLayout(gridLayout);

		Composite generalGroup = new Composite(root, SWT.NONE);
		generalGroup.setLayoutData(gd);
		gridLayout = new GridLayout(4, false);

		generalGroup.setLayout(gridLayout);

			initialState = isJaxrsEnabled(project);
		IFieldEditor jaxrsSupportCheckBox = IFieldEditorFactory.INSTANCE.createCheckboxEditor(
				JaxrsPreferencesMessages.JAXRS_SETTINGS_PREFERENCE_PAGE_JAXRS_SUPPORT,
				JaxrsPreferencesMessages.JAXRS_SETTINGS_PREFERENCE_PAGE_JAXRS_SUPPORT, initialState);
		jaxrsSupportCheckBox.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				Object value = evt.getNewValue();
				if (value instanceof Boolean) {
					boolean v = ((Boolean) value).booleanValue();
					setEnabledJaxrsSuport(v);
				}
			}
		});
		jaxrsEnabled = isJaxrsEnabled(project);
		registerEditor(jaxrsSupportCheckBox, generalGroup);

		validate();

		} catch (CoreException e) {
			Logger.error("Failed to display JAX-RS settings page", e);
		}
		return root;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	@Override
	protected void performDefaults() {
		try {
			getEditor(JaxrsPreferencesMessages.JAXRS_SETTINGS_PREFERENCE_PAGE_JAXRS_SUPPORT).setValue(
					isJaxrsEnabled(project));
			validate();
		} catch (CoreException e) {
			Logger.error("Failed to restore defaults on JAX-RS settings page", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performOk()
	 */
	@Override
	public boolean performOk() {
		try {
			if (isJaxrsEnabled() != initialState) {
				if (isJaxrsEnabled()) {
					addJaxrsSupport(project);
				} else {
					removeJaxrsSupport(project);
				}
				initialState = isJaxrsEnabled();
			}
		} catch (CoreException e) {
			Logger.error("Failed to apply changes on JAX-RS settings page", e);
		}
		return true;
	}

	private void addJaxrsSupport(IProject project) throws CoreException {
		if (project == null) {
			return;
		}
		ProjectNatureUtils.installProjectNature(project, ProjectNatureUtils.JAXRS_NATURE_ID);
	}

	private void removeJaxrsSupport(IProject project) throws CoreException {
		ProjectNatureUtils.uninstallProjectNature(project, ProjectNatureUtils.JAXRS_NATURE_ID);
	}

	private boolean isJaxrsEnabled(IProject project) throws CoreException {
		return ProjectNatureUtils.isProjectNatureInstalled(project, ProjectNatureUtils.JAXRS_NATURE_ID);
	}

	private boolean isJaxrsEnabled() {
		return jaxrsEnabled;
	}

	public void setEnabledJaxrsSuport(boolean enabled) {
		jaxrsEnabled = enabled;
		editorRegistry.get(JaxrsPreferencesMessages.JAXRS_SETTINGS_PREFERENCE_PAGE_JAXRS_SUPPORT).setValue(enabled);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jboss.tools.common.ui.preferences.SettingsPage#validate()
	 */
	@Override
	protected void validate() {
	}
}