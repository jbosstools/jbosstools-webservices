package org.jboos.tools.ws.creation.core.commands;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.wst.common.frameworks.datamodel.AbstractDataModelOperation;
import org.eclipse.wst.ws.internal.wsrt.IWebService;
import org.eclipse.wst.ws.internal.wsrt.WebServiceScenario;
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
		
		if (scenario == WebServiceScenario.TOPDOWN) {
			model.setWsdlURI(ws.getWebServiceInfo().getWsdlURL());
			model.setDatabindingType(Axis2Constants.DATA_BINDING_ADB);
			DefaultCodegenUtil defaultCodegenUtil = new DefaultCodegenUtil(model);
			defaultCodegenUtil.populateModelParamsFromWSDL();
			model.setServicesXML(true);
			model.setServerXMLCheck(true);
			ServiceContext.getInstance().setServiceName(model.getServiceName());
		}

		return null;
	}

}
