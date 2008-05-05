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

package org.jboss.tools.ws.core.facet.delegate;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.common.project.facet.core.IDelegate;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.jboss.tools.ws.core.JbossWSCoreMessages;
import org.jboss.tools.ws.core.command.JbossWSClassPathCommand;

public class JbossWSInstallRuntimeDelegate implements IDelegate {

	public void execute(IProject project, IProjectFacetVersion arg1, Object arg2,
			IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(JbossWSCoreMessages.PROGRESS_INSTALL_JBOSSWS_RUNTIME, 2 );

		JbossWSClassPathCommand command = new JbossWSClassPathCommand(project); 
	    command.executeOverride(monitor);
		monitor.worked( 1 );
		monitor.done();
	}

}
