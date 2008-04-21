package org.jboos.tools.ws.creation.core.commands;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.wst.common.frameworks.datamodel.AbstractDataModelOperation;
import org.jboos.tools.ws.creation.core.data.ServiceModel;

public class WSDL2JavaCommnad extends AbstractDataModelOperation{

	private ServiceModel model;
	
	public WSDL2JavaCommnad(ServiceModel model){
		this.model = model;
	}
	
	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {

		return null;
	}

}
