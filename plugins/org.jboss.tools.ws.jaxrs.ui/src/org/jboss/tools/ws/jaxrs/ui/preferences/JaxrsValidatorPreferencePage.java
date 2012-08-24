/******************************************************************************* 
 * Copyright (c) 2009 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.ws.jaxrs.ui.preferences;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.jboss.tools.common.ui.preferences.SeverityConfigurationBlock.SectionDescription;
import org.jboss.tools.common.ui.preferences.SeverityPreferencePage;
import org.jboss.tools.ws.jaxrs.core.JBossJaxrsCorePlugin;

/**
 * @author Xavier Coulon
 */
public class JaxrsValidatorPreferencePage extends SeverityPreferencePage {

	/** The JAX-RS Validation preference page ID (at the workspace level). */
	public static final String PREF_ID = "org.jboss.tools.ws.jaxrs.ui.JAXRSValidatorPreferencePage"; //$NON-NLS-1$
	/** The JAX-RS Validation property page ID (at the project level). */
	public static final String PROP_ID = "org.jboss.tools.ws.jaxrs.ui.propertyPages.JaxrsValidatorPreferencePage"; //$NON-NLS-1$

	public JaxrsValidatorPreferencePage() {
		setPreferenceStore(JBossJaxrsCorePlugin.getDefault().getPreferenceStore());
		setTitle(JaxrsPreferencesMessages.JAXRS_VALIDATOR_PREFERENCE_PAGE_JAXRS_VALIDATOR);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.preferences.PropertyAndPreferencePage#getPreferencePageID()
	 */
	@Override
	protected String getPreferencePageID() {
		return PREF_ID;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.preferences.PropertyAndPreferencePage#getPropertyPageID()
	 */
	@Override
	protected String getPropertyPageID() {
		return PROP_ID;
	}

	@SuppressWarnings("restriction")
	@Override
	public void createControl(Composite parent) {
		IWorkbenchPreferenceContainer container = (IWorkbenchPreferenceContainer) getContainer();
		fConfigurationBlock = new JaxrsValidatorConfigurationBlock(getNewStatusChangedListener(), getProject(), container);

		super.createControl(parent);
	}
	
	@Override
	protected SectionDescription[] getAllSections() {
		return JaxrsValidatorConfigurationBlockDescriptionProvider.getInstance().getSections();
	}
}