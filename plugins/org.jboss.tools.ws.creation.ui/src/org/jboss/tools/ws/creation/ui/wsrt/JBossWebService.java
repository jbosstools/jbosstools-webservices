package org.jboss.tools.ws.creation.ui.wsrt;

import java.awt.image.SampleModel;
import java.util.Vector;

import org.eclipse.wst.command.internal.env.core.ICommandFactory;
import org.eclipse.wst.command.internal.env.core.SimpleCommandFactory;
import org.eclipse.wst.common.environment.IEnvironment;
import org.eclipse.wst.ws.internal.wsrt.AbstractWebService;
import org.eclipse.wst.ws.internal.wsrt.IContext;
import org.eclipse.wst.ws.internal.wsrt.ISelection;
import org.eclipse.wst.ws.internal.wsrt.WebServiceInfo;
import org.eclipse.wst.ws.internal.wsrt.WebServiceScenario;
import org.jboos.tools.ws.creation.core.commands.WSDL2JavaCommnad;
import org.jboos.tools.ws.creation.core.data.ServiceModel;

public class JBossWebService extends AbstractWebService {

	public JBossWebService(WebServiceInfo info){
		super(info);
	}
	
	@Override
	public ICommandFactory assemble(IEnvironment env, IContext ctx,
			ISelection sel, String project, String earProject) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ICommandFactory deploy(IEnvironment env, IContext ctx,
			ISelection sel, String project, String earProject) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ICommandFactory develop(IEnvironment env, IContext ctx,
			ISelection sel, String project, String earProject) {
		
		Vector commands = new Vector();
		ServiceModel model = new ServiceModel();
		if (ctx.getScenario().getValue() == WebServiceScenario.BOTTOMUP)	{ 
			commands.add(new WSDL2JavaCommnad(model));
		}
		
		return new SimpleCommandFactory(commands);
	}

	@Override
	public ICommandFactory install(IEnvironment env, IContext ctx,
			ISelection sel, String project, String earProject) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ICommandFactory run(IEnvironment env, IContext ctx, ISelection sel,
			String project, String earProject) {
		// TODO Auto-generated method stub
		return null;
	}

}
