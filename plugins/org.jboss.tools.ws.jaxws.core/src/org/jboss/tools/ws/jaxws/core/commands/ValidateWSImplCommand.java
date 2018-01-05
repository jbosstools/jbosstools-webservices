/******************************************************************************* 
 * Copyright (c) 2008 - 2014 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/

package org.jboss.tools.ws.jaxws.core.commands;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.common.frameworks.datamodel.AbstractDataModelOperation;
import org.jboss.tools.ws.jaxws.core.JBossJAXWSCorePlugin;
import org.jboss.tools.ws.jaxws.core.data.ServiceModel;
import org.jboss.tools.ws.jaxws.core.messages.JBossJAXWSCoreMessages;
import org.jboss.tools.ws.jaxws.core.util.StatusUtils;

/**
 * @author Grid Qian
 */
public class ValidateWSImplCommand extends AbstractDataModelOperation {

	private ServiceModel model;

	public ValidateWSImplCommand(ServiceModel model) {
		this.model = model;
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {

		String implClass = model.getServiceClasses().get(0);
		try {
			IType type = model.getJavaProject().findType(implClass);
			if (type != null) {
				if(!type.getPackageFragment().exists()|| type.getPackageFragment().isDefaultPackage()){
					return StatusUtils.errorStatus(JBossJAXWSCoreMessages.Error_No_Package);					
				}
				if (!type.getAnnotation(
						JBossJAXWSCoreMessages.Webservice_Annotation)
						.exists()
						&& !type
								.getAnnotation(
										JBossJAXWSCoreMessages.Webservice_Annotation_Prefix)
								.exists()) {
					return StatusUtils
							.errorStatus(JBossJAXWSCoreMessages.Error_No_Annotation);
				}
			} else {
				return StatusUtils.errorStatus(NLS.bind(
						JBossJAXWSCoreMessages.Error_No_Class,
						new String[] { implClass, model.getJavaProject().getProject().getName() }));
			}
		} catch (JavaModelException e) {
			JBossJAXWSCorePlugin.getDefault().logError(e);
			return StatusUtils.errorStatus(NLS.bind(
					JBossJAXWSCoreMessages.Error_No_Class, new String[] {
							implClass, model.getJavaProject().getProject().getName() }));
		}
		return Status.OK_STATUS;
	}

}
