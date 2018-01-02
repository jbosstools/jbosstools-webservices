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
package org.jboss.tools.ws.jaxws.ui.commands;

import java.io.File;
import java.util.List;

import org.jboss.tools.ws.jaxws.core.data.ServiceModel;


public class WSDL2JavaCommand extends AbstractGenerateCodeCommand{

	private static String WSCONSUEM_FILE_NAME_LINUX = "wsconsume.sh";  //$NON-NLS-1$
	private static String WSCONSUEM_FILE_NAME_WIN = "wsconsume.bat"; //$NON-NLS-1$
	
	public WSDL2JavaCommand(ServiceModel model){
		super(model);
	}
	

	@Override
	protected String getCommandLineFileName_linux() {
		return WSCONSUEM_FILE_NAME_LINUX;
	}

	@Override
	protected String getCommandLineFileName_win() {
		return WSCONSUEM_FILE_NAME_WIN;
	}
	
	@Override
	protected void addCommandlineArgs(List<String> command) {
		if(model.getCustomPackage() != null && !"".equals(model.getCustomPackage())){ //$NON-NLS-1$
			command.add("-p"); //$NON-NLS-1$
			command.add(model.getCustomPackage());
		}
		
		List<String> bindingFiles = model.getBindingFiles();
		for(String bindingFileLocation: bindingFiles){
			File bindingFile = new File(bindingFileLocation);
			if(bindingFile.exists()){
				command.add("-b"); //$NON-NLS-1$
				command.add(bindingFileLocation);
			}
		}
		
		if(model.getCatalog() != null && !"".equals(model.getCatalog().trim())){ //$NON-NLS-1$
			File catalog = new File(model.getCatalog());
			if(catalog.exists()){
				command.add("-c"); //$NON-NLS-1$
				command.add(model.getCatalog());
			}
		}
		
		if(model.getTarget() != null){
			command.add("-t"); //$NON-NLS-1$
			command.add(model.getTarget());
		}
		
		if(model.enableSOAP12()){
			command.add("-e"); //$NON-NLS-1$
		}
		command.add("-v"); //$NON-NLS-1$
	}

}
