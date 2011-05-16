package org.jboss.tools.ws.creation.core.commands;

import javax.wsdl.Definition;
import javax.wsdl.WSDLException;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.common.frameworks.datamodel.AbstractDataModelOperation;
import org.eclipse.wst.ws.internal.wsrt.IWebService;
import org.eclipse.wst.ws.internal.wsrt.WebServiceScenario;
import org.jboss.tools.ws.core.utils.StatusUtils;
import org.jboss.tools.ws.creation.core.data.ServiceModel;
import org.jboss.tools.ws.creation.core.messages.JBossWSCreationCoreMessages;
import org.jboss.tools.ws.creation.core.utils.JBossWSCreationUtils;

@SuppressWarnings("restriction")
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
			} else if (!new Path(location)
					.append(JBossWSCreationCoreMessages.Bin)
					.append(JBossWSCreationCoreMessages.Command).toFile()
					.exists()) {
				return StatusUtils
						.errorStatus(JBossWSCreationCoreMessages.Error_WS_Location);
			}
		} catch (CoreException e1) {
			return StatusUtils
					.errorStatus(JBossWSCreationCoreMessages.Error_WS_Location);
		}
		model.setTarget(JBossWSCreationCoreMessages.Value_Target_0);
		if (scenario == WebServiceScenario.TOPDOWN) {
			model.setWsdlURI(ws.getWebServiceInfo().getWsdlURL());
			Definition definition = null;
			try {
				definition = JBossWSCreationUtils.readWSDL(model.getWsdlURI());
			} catch (WSDLException e) {
				return StatusUtils.errorStatus(JBossWSCreationCoreMessages.Error_Read_WSDL);
			}
			model.setWsdlDefinition(definition);
			model.setCustomPackage(""); //$NON-NLS-1$
		} else {
			model.addServiceClasses(ws.getWebServiceInfo().getImplURL());
		}

		return Status.OK_STATUS;
	}

	public ServiceModel getWebServiceDataModel() {
		return model;
	}

}
