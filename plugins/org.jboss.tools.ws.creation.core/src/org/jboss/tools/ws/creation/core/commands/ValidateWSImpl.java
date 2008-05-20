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

package org.jboss.tools.ws.creation.core.commands;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.wst.common.frameworks.datamodel.AbstractDataModelOperation;
import org.jboss.tools.ws.core.utils.StatusUtils;
import org.jboss.tools.ws.creation.core.data.ServiceModel;
import org.jboss.tools.ws.creation.core.messages.JBossWSCreationCoreMessages;
import org.jboss.tools.ws.creation.core.utils.JBossWSCreationUtils;

/**
 * @author Grid Qian
 */
public class ValidateWSImpl extends AbstractDataModelOperation {

	private ServiceModel model;

	public ValidateWSImpl(ServiceModel model) {
		this.model = model;
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {
		String implClass = model.getServiceClass();
		String project = model.getWebProjectName();
		ICompilationUnit unit = null;
		try {
			unit = JBossWSCreationUtils.getJavaProjectByName(project).findType(
					implClass).getCompilationUnit();
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		try {
			if(!unit.getSource().contains(JBossWSCreationCoreMessages.WEBSERVICE_ANNOTATION)){
				return StatusUtils.errorStatus(JBossWSCreationCoreMessages.ERROR_NO_ANNOTATION);
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		return Status.OK_STATUS;
	}

}
