package org.jboos.tools.ws.creation.core.commands;

import javax.wsdl.WSDLException;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.ws.axis2.consumption.core.utils.DefaultCodegenUtil;
import org.eclipse.wst.command.internal.env.core.common.StatusUtils;
import org.eclipse.wst.common.frameworks.datamodel.AbstractDataModelOperation;
import org.eclipse.wst.ws.internal.wsrt.IWebService;
import org.eclipse.wst.ws.internal.wsrt.WebServiceScenario;
import org.eclipse.wst.wsdl.WSDLFactory;
import org.eclipse.wst.wsdl.internal.impl.wsdl4j.WSDLFactoryImpl;
import org.eclipse.wst.wsdl.internal.util.WSDLDefinitionFactory;
import org.eclipse.wst.wsdl.internal.util.WSDLUtil;
import org.eclipse.wst.wsdl.util.WSDLParser;
import org.jboos.tools.ws.creation.core.data.ServiceModel;
import org.jboos.tools.ws.creation.core.messages.JBossWSCreationCoreMessages;
import org.jboos.tools.ws.creation.core.utils.WSDLPropertyReader;

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
		IStatus status = Status.OK_STATUS;
		
		model.setTarget(JBossWSCreationCoreMessages.getString("VALUE_TARGET_2"));
		if (scenario == WebServiceScenario.TOPDOWN) {
			try{
			model.setWsdlURI(ws.getWebServiceInfo().getWsdlURL());
			WSDLPropertyReader reader = new WSDLPropertyReader();
			reader.readWSDL(ws.getWebServiceInfo().getWsdlURL());
			model.setCustomPackage(reader.packageFromTargetNamespace());
			}catch (WSDLException e) {
				return StatusUtils.errorStatus(e.getLocalizedMessage(), e);
			}
		}

		return Status.OK_STATUS;
	}

	public ServiceModel getWebServiceDataModel()
	{		
		
		return model;
	}
	
}
