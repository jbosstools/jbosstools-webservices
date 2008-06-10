package org.jboss.tools.ws.creation.core.commands;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.common.frameworks.datamodel.AbstractDataModelOperation;
import org.jboss.tools.ws.core.utils.StatusUtils;
import org.jboss.tools.ws.creation.core.JBossWSCreationCore;
import org.jboss.tools.ws.creation.core.data.ServiceModel;
import org.jboss.tools.ws.creation.core.messages.JBossWSCreationCoreMessages;
import org.jboss.tools.ws.creation.core.utils.JBossWSCreationUtils;

public class WSDL2JavaCommand extends AbstractDataModelOperation{

	private ServiceModel model;
	private static String WSCONSUEM_FILE_NAME_LINUX = "wsconsume.sh"; 
	private static String WSCONSUEM_FILE_NAME_WIN = "wsconsume.bat";
	
	public WSDL2JavaCommand(ServiceModel model){
		this.model = model;
	}
	
	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {
		IStatus status = Status.OK_STATUS;
		
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(model
				.getWebProjectName());
		
		try {
			String runtimeLocation = JBossWSCreationUtils.getJbossWSRuntimeLocation(project);
			String commandLocation = runtimeLocation + Path.SEPARATOR + "bin";		
			IPath path = new Path(commandLocation);
			String command =  "sh " + WSCONSUEM_FILE_NAME_LINUX;
			if(System.getProperty("os.name").toLowerCase().indexOf("win") >= 0){
				command =  "cmd.exe /c " + WSCONSUEM_FILE_NAME_WIN;
				path.append(WSCONSUEM_FILE_NAME_WIN);
			}else{
				path.append(WSCONSUEM_FILE_NAME_LINUX);
			}
			
			if(!path.toFile().exists()){
				return StatusUtils.errorStatus(
						NLS.bind(JBossWSCreationCoreMessages.Error_Message_Command_File_Not_Found,
								new String[] {path.toOSString()}));
			}
			
			String args = getCommandlineArgs();		
			command += " -k " + args + " " + model.getWsdlURI();
			Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec(command, null, new File(commandLocation));
			InputStreamReader ir = new InputStreamReader(proc.getErrorStream());
            LineNumberReader input = new LineNumberReader(ir);            
            String str = input.readLine();
            StringBuffer result = new StringBuffer();
            while(str != null){                
                result.append(str).append("\t\r");
                str = input.readLine();
                
           }
            int exitValue = proc.waitFor();
            if(exitValue != 0){
            	return StatusUtils.errorStatus(result.toString());
            }
            
            // log the result of the command execution
            JBossWSCreationCore.getDefault().logInfo(convertInputStreamToString(proc.getInputStream()));
		} catch (IOException e) {
			JBossWSCreationCore.getDefault().logError(e);
			
		} catch (InterruptedException e) {
			// ignore 
		} catch (CoreException e) {	
			JBossWSCreationCore.getDefault().logError(e);
			//unable to get runtime location
			return e.getStatus();
		}
		
		refreshProject(model.getWebProjectName(), monitor);
		
		
		return status;
	}
	
	private String convertInputStreamToString(InputStream input) throws IOException{
		InputStreamReader ir = new InputStreamReader(input);
        LineNumberReader reader = new LineNumberReader(ir);            
        String str = reader.readLine();
        StringBuffer result = new StringBuffer();
        while(str != null){                
            result.append(str).append("\t\r");
            str = reader.readLine();
            
       }
		return result.toString();
	}
	private void refreshProject(String project, IProgressMonitor monitor){
		try {
			JBossWSCreationUtils.getProjectByName(project).refreshLocal(2, monitor);
		} catch (CoreException e) {
			e.printStackTrace();
			JBossWSCreationCore.getDefault().logError(e);
		}
	}
	
	private String getCommandlineArgs(){
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
