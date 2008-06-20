package org.jboss.tools.ws.creation.core.commands;

import java.io.File;
import java.util.List;

import org.eclipse.core.runtime.Path;
import org.jboss.tools.ws.creation.core.data.ServiceModel;
import org.jboss.tools.ws.creation.core.utils.JBossWSCreationUtils;

public class WSDL2JavaCommand extends AbstractGenerateCodeCommand{

	private static String WSCONSUEM_FILE_NAME_LINUX = "wsconsume.sh"; 
	private static String WSCONSUEM_FILE_NAME_WIN = "wsconsume.bat";
	
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
	protected String getCommandlineArgs() {
		String commandLine;
		String project = model.getWebProjectName();
		String projectRoot = JBossWSCreationUtils.getProjectRoot(project).toOSString();
		commandLine = "-s " + projectRoot + Path.SEPARATOR + "src";
		
		if(model.getCustomPackage() != null && !"".equals(model.getCustomPackage())){
			commandLine += " -p " + model.getCustomPackage(); 
		}
		
		List<String> bindingFiles = model.getBindingFiles();
		for(String bindingFileLocation: bindingFiles){
			File bindingFile = new File(bindingFileLocation);
			if(bindingFile.exists()){
				commandLine += " -b " + bindingFileLocation;
			}
		}
		
		if(model.getCatalog() != null && !"".equals(model.getCatalog().trim())){
			File catalog = new File(model.getCatalog());
			if(catalog.exists()){
				commandLine += " -c " + model.getCatalog();
			}
		}
		
		if(model.getTarget() != null){
			commandLine += " -t " + model.getTarget();
		}
		 
		
		return commandLine;
	}

}
