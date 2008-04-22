package org.jboos.tools.ws.creation.core.commands;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.wst.common.frameworks.datamodel.AbstractDataModelOperation;
import org.jboos.tools.ws.creation.core.data.ServiceModel;
import org.jboos.tools.ws.creation.core.utils.JBossWSCreationUtils;

public class WSDL2JavaCommnad extends AbstractDataModelOperation{

	private ServiceModel model;
	
	
	public WSDL2JavaCommnad(ServiceModel model){
		this.model = model;
	}
	
	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {

		PreferenceStore prs = new PreferenceStore("jbosswsui.properties");
		try {
			prs.load();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String runtimeLocation = prs.getString("jbosswsruntimelocation");//JBossWSUIPlugin.getDefault().getPreferenceStore().getString("jbosswsruntimelocation");
		String binLocation = runtimeLocation + "bin";
		
		String commandLine = binLocation + Path.SEPARATOR + "wsconsume.sh";		   
		
		String args = getCommandlineArgs();
		
		commandLine = commandLine + " -k -o " + args + " " + model.getWsdlURI();
		commandLine = "sh " + commandLine;
		
	    
		
		try {
				 
			InputStreamReader ir = new InputStreamReader(Runtime.getRuntime().exec(commandLine).getInputStream());
            LineNumberReader input = new LineNumberReader(ir);
            
            String str = "";
            for (int i = 1; str != null; i++)
            {
                str = input.readLine();
                System.out.println(str);
           }
            
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		refreshProject(model.getWebProjectName());
		
		return Status.OK_STATUS;
	}
	
	private void refreshProject(String project){
		try {
			JBossWSCreationUtils.getProjectByName(project).refreshLocal(2, new NullProgressMonitor());
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private String getCommandlineArgs(){
		String project = model.getWebProjectName();
		String projectRoot = JBossWSCreationUtils.getProjectRoot(project).toOSString();
		String targetSrc = projectRoot + Path.SEPARATOR + "src";
		
		return targetSrc;
		
	}

}
