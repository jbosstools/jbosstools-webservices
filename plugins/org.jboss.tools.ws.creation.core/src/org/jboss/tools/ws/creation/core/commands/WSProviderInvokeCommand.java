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

import org.eclipse.core.runtime.Path;
import org.jboss.tools.ws.creation.core.data.ServiceModel;
import org.jboss.tools.ws.creation.core.utils.JBossWSCreationUtils;

/**
 * @author Grid Qian
 */
public class WSProviderInvokeCommand extends AbstractGenerateCodeCommand {

	private static String WSPROVIDER_FILE_NAME_LINUX = "wsprovide.sh"; 
	private static String WSPROVIDER_FILE_NAME_WIN = "wsprovide.bat";
	
	public WSProviderInvokeCommand(ServiceModel model) {
		super(model);
	}


	@Override
	protected String getCommandLineFileName_linux() {
		return WSPROVIDER_FILE_NAME_LINUX;
	}

	@Override
	protected String getCommandLineFileName_win() {
		return WSPROVIDER_FILE_NAME_WIN;
	}

	@Override
	protected String getCommandlineArgs() {
		String commandLine;
		String project = model.getWebProjectName();
		String projectRoot = JBossWSCreationUtils.getProjectRoot(project)
				.toOSString();
		commandLine = "-s " + projectRoot + Path.SEPARATOR + "src";

		if (model.isGenWSDL()) {
			commandLine += " -w ";
		}

		commandLine += " -o " + projectRoot + Path.SEPARATOR
				+ "build/classes/ ";

		commandLine += " -r " + projectRoot + Path.SEPARATOR + "WebContent"
				+ Path.SEPARATOR + "wsdl ";

		commandLine += " -c " + projectRoot + Path.SEPARATOR
				+ "build/classes/ ";

		commandLine += model.getServiceClasses().get(0);

		return commandLine;

	}
}
