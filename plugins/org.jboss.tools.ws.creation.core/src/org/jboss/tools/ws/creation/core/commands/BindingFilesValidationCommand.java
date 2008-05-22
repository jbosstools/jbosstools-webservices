package org.jboss.tools.ws.creation.core.commands;

import java.io.IOException;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.common.frameworks.datamodel.AbstractDataModelOperation;
import org.jboss.tools.ws.creation.core.data.ServiceModel;
import org.jboss.tools.ws.creation.core.messages.JBossWSCreationCoreMessages;
import org.jboss.tools.ws.creation.core.utils.JBossStatusUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class BindingFilesValidationCommand extends AbstractDataModelOperation {

	private ServiceModel model;

	public BindingFilesValidationCommand(ServiceModel model) {
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
			//if no SAXParserFactory implementation is available, ignore this command
			return Status.OK_STATUS;
		}

		
		for (String filename : model.getBindingFiles()) {
			IStatus bStatus = validateXMLFile(reader, filename);
			if(bStatus != Status.OK_STATUS){
				return bStatus;
			}
		}
		
		
		return status;
	}
	
	/*
	 * just ensure that the file is a readable xml file to avoid breaking code generation
	 */
	private IStatus validateXMLFile(XMLReader reader, String filename){
		try {
			InputSource is = new InputSource(filename);
			reader.parse(is);

		} catch (SAXException e) {
			return JBossStatusUtils
					.errorStatus(
							NLS.bind(JBossWSCreationCoreMessages.ERROR_MESSAGE_INVALID_BINDING_FILE,
											new String[] {filename, e.getLocalizedMessage() }), e);
			
		} catch (IOException e) {
			return JBossStatusUtils
					.errorStatus(
							NLS.bind(JBossWSCreationCoreMessages.ERROR_MESSAGE_INVALID_BINDING_FILE,
											new String[] {filename, e.getLocalizedMessage() }), e);
		}
		return Status.OK_STATUS;
	}
	
	
	
}
