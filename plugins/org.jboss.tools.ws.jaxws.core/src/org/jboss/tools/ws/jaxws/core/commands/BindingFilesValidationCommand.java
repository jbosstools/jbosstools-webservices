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
package org.jboss.tools.ws.jaxws.core.commands;

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
import org.jboss.tools.ws.jaxws.core.data.ServiceModel;
import org.jboss.tools.ws.jaxws.core.messages.JBossJAXWSCoreMessages;
import org.jboss.tools.ws.jaxws.core.util.StatusUtils;
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
	 * so the xml file is not validated with xml schema
	 */
	private IStatus validateXMLFile(XMLReader reader, String filename){
		try {
			InputSource is = new InputSource(filename);
			reader.parse(is);

		} catch (SAXException e) {
			return StatusUtils
					.errorStatus(
							NLS.bind(JBossJAXWSCoreMessages.Error_Message_Invalid_Binding_File,
											new String[] {filename, e.getLocalizedMessage() }), e);
			
		} catch (IOException e) {
			return StatusUtils
					.errorStatus(
							NLS.bind(JBossJAXWSCoreMessages.Error_Message_Invalid_Binding_File,
											new String[] {filename, e.getLocalizedMessage() }), e);
		}
		return Status.OK_STATUS;
	}
	
	
	
}
