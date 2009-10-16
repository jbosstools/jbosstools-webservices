/*******************************************************************************
 * Copyright (c) 2007 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.ws.creation.ui.project.facet;

import java.text.MessageFormat;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.common.frameworks.datamodel.DataModelEvent;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.frameworks.datamodel.IDataModelListener;
import org.eclipse.wst.common.project.facet.ui.AbstractFacetWizardPage;
import org.eclipse.wst.common.project.facet.ui.IFacetWizardPage;
import org.jboss.tools.ws.core.facet.delegate.JBossWSFacetInstallDataModelProvider;
import org.jboss.tools.ws.creation.core.messages.JBossWSCreationCoreMessages;

/**
 * @author Dennyxu
 * 
 */
public class JBossWSFacetInstallPage extends AbstractFacetWizardPage implements
		IFacetWizardPage, IDataModelListener, IMessageNotifier {

 

	private IDataModel model;
	private JBossWSRuntimeConfigBlock block;

	public JBossWSFacetInstallPage() {
		super("jbosswsfacet"); //$NON-NLS-1$
	}

	public void setConfig(Object config) {
		this.model = (IDataModel) config;
		String JbossWSVersion = (String)model.getProperty(JBossWSFacetInstallDataModelProvider.FACET_VERSION_STR);
		setTitle(MessageFormat.format(JBossWSCreationCoreMessages.JBossWSFacetInstallPage_Title, JbossWSVersion)); 
		setDescription(MessageFormat.format(JBossWSCreationCoreMessages.JBossWSFacetInstallPage_Description, JbossWSVersion));
	}

	public void createControl(Composite parent) {
		block = new JBossWSRuntimeConfigBlock(model);
		block.setMessageNotifier(this);
		setControl(block.createControl(parent));
		setPageComplete(block.isPageComplete());
	}

	public void propertyChanged(DataModelEvent event) {
		
	}

	public void notify(String msg) {
		setErrorMessage(msg);
		setPageComplete(block.isPageComplete());
	}
	
	

}