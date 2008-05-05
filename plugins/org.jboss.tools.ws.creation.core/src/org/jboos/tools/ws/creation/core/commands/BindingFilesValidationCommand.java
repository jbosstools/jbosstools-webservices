package org.jboos.tools.ws.creation.core.commands;

import java.io.IOException;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.command.internal.env.core.common.StatusUtils;
import org.eclipse.wst.common.frameworks.datamodel.AbstractDataModelOperation;
import org.jboos.tools.ws.creation.core.data.ServiceModel;
import org.jboos.tools.ws.creation.core.messages.JBossWSCreationCoreMessages;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class BindingFilesValidationCommand extends AbstractDataModelOperation{

	private ServiceModel model;
	
	
	public BindingFilesValidationCommand(ServiceModel model){
		this.model = model;
	}
	
	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {
		IStatus status = Status.OK_STATUS;   
		 SAXParserFactory spf = SAXParserFactory.newInstance();

		    // Create the XMLReader to be used to check for errors.
		    XMLReader reader = null;
		    try {
		      SAXParser parser = spf.newSAXParser();
		      reader = parser.getXMLReader();
		    } catch (Exception e) {
		      //if no SAXParserFactory implementation is available, break this command
		      return Status.OK_STATUS;
		    }

		   
		    // Use the XMLReader to parse the entire file.
		    try {
		      InputSource is = new InputSource("");
		      reader.parse(is);
		    } catch (SAXException e) {
		    	/*status = StatusUtils.errorStatus(
		    			JBossWSCreationCoreMessages
								new String[]{e.getLocalizedMessage()}), e);*/
		    } catch (IOException e) {
		      System.err.println(e);
		      System.exit(1);
		    }
		    return null;
		  }
	
	
}
