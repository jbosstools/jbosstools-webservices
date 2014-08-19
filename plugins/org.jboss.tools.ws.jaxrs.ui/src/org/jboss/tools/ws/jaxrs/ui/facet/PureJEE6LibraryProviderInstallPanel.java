/******************************************************************************* 
 * Copyright (c) 2008 - 2014 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/

package org.jboss.tools.ws.jaxrs.ui.facet;

import org.eclipse.jst.common.project.facet.ui.libprov.LibraryProviderOperationPanel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

/**
 * Installation Panel for the custom JAXRS/JEE6 Library Provider. This Panel
 * provides a single label.
 * 
 * @author xcoulon
 * 
 */
public class PureJEE6LibraryProviderInstallPanel extends LibraryProviderOperationPanel {

	/**
	 * {@inheritDoc}
	 * 
	 * @see
	 * org.eclipse.jst.common.project.facet.ui.libprov.LibraryProviderOperationPanel
	 * #createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public Control createControl(Composite parent) {
		Label label = new Label(parent, SWT.LEFT);
		label.setText("Project's classpath configuration is left to the user. Web application descriptor will not be modified when this Facet is installed.");
		return label;
	}

}
