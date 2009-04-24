package org.jboss.tools.ws.creation.core.commands;

import java.io.File;
import java.util.List;

import org.jboss.tools.ws.creation.core.data.ServiceModel;

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
	protected void addCommandlineArgs(List<String> command) {
		
		if(model.getCustomPackage() != null && !"".equals(model.getCustomPackage())){
			command.add("-p");
			command.add(model.getCustomPackage());
		}
		
		List<String> bindingFiles = model.getBindingFiles();
		for(String bindingFileLocation: bindingFiles){
			File bindingFile = new File(bindingFileLocation);
			if(bindingFile.exists()){
				command.add("-b");
				command.add(bindingFileLocation);
			}
		}
		
		if(model.getCatalog() != null && !"".equals(model.getCatalog().trim())){
			File catalog = new File(model.getCatalog());
			if(catalog.exists()){
				command.add("-c");
				command.add(model.getCatalog());
			}
		}
		
		if(model.getTarget() != null){
			command.add("-t");
			command.add(model.getTarget());
		}
	}

}
