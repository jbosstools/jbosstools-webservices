package org.jboos.tools.ws.creation.core.commands;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.common.frameworks.datamodel.AbstractDataModelOperation;
import org.eclipse.wst.ws.internal.wsrt.IWebService;
import org.eclipse.wst.ws.internal.wsrt.WebServiceScenario;
import org.eclipse.wst.wsdl.WSDLFactory;
import org.eclipse.wst.wsdl.internal.impl.wsdl4j.WSDLFactoryImpl;
import org.eclipse.wst.wsdl.internal.util.WSDLDefinitionFactory;
import org.eclipse.wst.wsdl.internal.util.WSDLUtil;
import org.eclipse.wst.wsdl.util.WSDLParser;
import org.jboos.tools.ws.creation.core.data.ServiceModel;

public class InitialCommnad extends AbstractDataModelOperation{

	private ServiceModel model;
	private IWebService ws;
	private int scenario;
	
	public InitialCommnad(ServiceModel model ,IWebService ws, int scenario){
		this.model = model;
		this.ws = ws;
		this.scenario = scenario;		
	}
	
	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {
		
		model.setWsdlURI(ws.getWebServiceInfo().getWsdlURL());

		return Status.OK_STATUS;
	}

	public ServiceModel getWebServiceDataModel()
	{		
		
		return model;
	}
	
}
