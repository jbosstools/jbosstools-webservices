package org.jboss.tools.ws.creation.core.commands;

import java.io.File;

import javax.wsdl.WSDLException;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.jboss.tools.ws.core.utils.StatusUtils;
import org.eclipse.wst.common.frameworks.datamodel.AbstractDataModelOperation;
import org.eclipse.wst.ws.internal.wsrt.IWebService;
import org.eclipse.wst.ws.internal.wsrt.WebServiceScenario;
import org.jboss.tools.ws.core.JBossWSCorePlugin;
import org.jboss.tools.ws.creation.core.JBossWSCreationCore;
import org.jboss.tools.ws.creation.core.data.ServiceModel;
import org.jboss.tools.ws.creation.core.messages.JBossWSCreationCoreMessages;
import org.jboss.tools.ws.creation.core.utils.JBossWSCreationUtils;
import org.jboss.tools.ws.creation.core.utils.WSDLPropertyReader;

public class InitialCommand extends AbstractDataModelOperation {

	private ServiceModel model;
	private IWebService ws;
	private int scenario;

	public InitialCommand(ServiceModel model, IWebService ws, int scenario) {
		this.model = model;
		this.ws = ws;
		this.scenario = scenario;
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {

		try {
			String location = JBossWSCreationUtils.getJBossWSRuntimeLocation(JBossWSCreationUtils.getProjectByName(model.getWebProjectName()));
			if (location.equals("")) { //$NON-NLS-1$
				return StatusUtils
						.errorStatus(JBossWSCreationCoreMessages.Error_WS_Location);
			} else if(!new Path(location).append(JBossWSCreationCoreMessages.Bin).append(JBossWSCreationCoreMessages.Command).toFile().exists()){
				return StatusUtils
				.errorStatus(JBossWSCreationCoreMessages.Error_WS_Location);
			}
		} catch (CoreException e1) {
			return StatusUtils.errorStatus(JBossWSCreationCoreMessages.Error_WS_Location);
		}
		model.setTarget(JBossWSCreationCoreMessages.Value_Target_0);
		if (scenario == WebServiceScenario.TOPDOWN) {
			try {
				model.setWsdlURI(ws.getWebServiceInfo().getWsdlURL());
				WSDLPropertyReader reader = new WSDLPropertyReader();
				reader.readWSDL(ws.getWebServiceInfo().getWsdlURL());
				model.setCustomPackage(reader.packageFromTargetNamespace());
				model.setServiceList(reader.getServiceList());
				model.setPortTypeList(reader.getPortTypeList());

			} catch (WSDLException e) {
				JBossWSCreationCore.getDefault().logError(e);
				return StatusUtils.errorStatus(e.getLocalizedMessage(), e);
			}
		} else {
			model.addServiceClasses(ws.getWebServiceInfo().getImplURL());
		}

		return Status.OK_STATUS;
	}

	public ServiceModel getWebServiceDataModel() {

		return model;
	}

}
