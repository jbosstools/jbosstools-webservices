package org.jboss.tools.ws.creation.core.commands;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.List;

import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.common.frameworks.datamodel.AbstractDataModelOperation;
import org.jboss.tools.ws.core.JbossWSCorePlugin;
import org.jboss.tools.ws.creation.core.data.ServiceModel;
import org.jboss.tools.ws.creation.core.utils.JBossStatusUtils;
import org.jboss.tools.ws.creation.core.utils.JBossWSCreationUtils;

public class WSDL2JavaCommand extends AbstractDataModelOperation{

	private ServiceModel model;
	
	
	public WSDL2JavaCommand(ServiceModel model){
		this.model = model;
	}
	
	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {

		String runtimeLocation = JbossWSCorePlugin.getDefault().getPreferenceStore().getString("jbosswsruntimelocation");
		String commandLocation = runtimeLocation + Path.SEPARATOR + "bin";		
		String command =  "sh wsconsume.sh ";
		if(System.getProperty("os.name").toLowerCase().indexOf("win") >= 0){
			command =  "cmd.exe /c wsconsume.bat ";		   
		}		
		String args = getCommandlineArgs();		
		command += " -k " + args + " " + model.getWsdlURI();
		
		try {
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
            	return JBossStatusUtils.errorStatus(result.toString());
            }
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		refreshProject(model.getWebProjectName(), monitor);
		
		
		return Status.OK_STATUS;
	}
	
	private void refreshProject(String project, IProgressMonitor monitor){
		try {
			JBossWSCreationUtils.getProjectByName(project).refreshLocal(2, monitor);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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

/*	private List<String> getEnv(){
		List<String> env = new ArrayList<String>();
		
		String project = model.getWebProjectName();
		String projectRoot = JBossWSCreationUtils.getProjectRoot(project).toOSString();
		env.add("o=" + projectRoot + Path.SEPARATOR + "src");
		
		String customePkg = model.getPackageText();		
		if(customePkg != null && !"".equals(customePkg)){
			env.add(" p=" + customePkg);
		}
		
		String bindingFileLocation = model.getBindingFileLocation();
		if(bindingFileLocation != null && !"".equals(bindingFileLocation)){
			File bindingFile = new File(bindingFileLocation);
			if(bindingFile.exists()){
				env.add("b=" + bindingFileLocation);
			}
		}
		
		return env;
		
	}
*/
}
