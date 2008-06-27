package org.jboss.tools.ws.creation.core.commands;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

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

abstract  class AbstractGenerateCodeCommand extends AbstractDataModelOperation{

	protected ServiceModel model;
	private  String cmdFileName_linux; 
	private  String cmdFileName_win;
	
	public AbstractGenerateCodeCommand(ServiceModel model){
		this.model = model;
		cmdFileName_linux = getCommandLineFileName_linux();
		cmdFileName_win = getCommandLineFileName_win();
	}
	
	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {
		IStatus status = Status.OK_STATUS;
		
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(model
				.getWebProjectName());
		
		try {
			String runtimeLocation = JBossWSCreationUtils.getJBossWSRuntimeLocation(project);
			String commandLocation = runtimeLocation + Path.SEPARATOR + "bin";		
			IPath path = new Path(commandLocation);
			String command =  "sh " + cmdFileName_linux;
			if(System.getProperty("os.name").toLowerCase().indexOf("win") >= 0){
				command =  "cmd.exe /c " + cmdFileName_win;
				path = path.append(cmdFileName_win);
			}else{
				path = path.append(cmdFileName_linux);
			}
			
			if(!path.toFile().getAbsoluteFile().exists()){
				return StatusUtils.errorStatus(
						NLS.bind(JBossWSCreationCoreMessages.Error_Message_Command_File_Not_Found,
								new String[] {path.toOSString()}));
			}
			
			String args = getCommandlineArgs();		
			command += " -k " + args + " " + model.getWsdlURI();
			Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec(command, null, new File(commandLocation));
            int exitValue = proc.waitFor();
            
            if(exitValue != 0){
            	return StatusUtils.errorStatus(convertInputStreamToString(proc.getErrorStream()));
            }
            
            // log the result of the command execution
            String resultOutput = convertInputStreamToString(proc.getInputStream());
            if(resultOutput != null && resultOutput.indexOf("[ERROR]") >= 0){
            	JBossWSCreationCore.getDefault().logError(resultOutput);
            	IStatus errorStatus = StatusUtils.errorStatus(resultOutput);
            	status = StatusUtils
						.errorStatus(
								JBossWSCreationCoreMessages.Error_Message_Failed_To_Generate_Code,
								new CoreException(errorStatus));
            }else{
            	JBossWSCreationCore.getDefault().logInfo(resultOutput);
            }
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
	
	abstract protected String getCommandlineArgs();
	
	abstract protected String getCommandLineFileName_linux();
	abstract protected String getCommandLineFileName_win();
	

}
