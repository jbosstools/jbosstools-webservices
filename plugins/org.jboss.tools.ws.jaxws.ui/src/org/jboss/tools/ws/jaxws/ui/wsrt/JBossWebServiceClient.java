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

package org.jboss.tools.ws.jaxws.ui.wsrt;

import java.util.Vector;

import org.eclipse.wst.command.internal.env.core.ICommandFactory;
import org.eclipse.wst.command.internal.env.core.SimpleCommandFactory;
import org.eclipse.wst.common.environment.IEnvironment;
import org.eclipse.wst.common.frameworks.datamodel.AbstractDataModelOperation;
import org.eclipse.wst.ws.internal.wsrt.AbstractWebServiceClient;
import org.eclipse.wst.ws.internal.wsrt.IContext;
import org.eclipse.wst.ws.internal.wsrt.ISelection;
import org.eclipse.wst.ws.internal.wsrt.WebServiceClientInfo;
import org.eclipse.wst.ws.internal.wsrt.WebServiceScenario;
import org.jboss.tools.ws.jaxws.core.commands.BindingFilesValidationCommand;
import org.jboss.tools.ws.jaxws.core.data.ServiceModel;
import org.jboss.tools.ws.jaxws.ui.commands.ClientSampleCreationCommand;
import org.jboss.tools.ws.jaxws.ui.commands.InitialClientCommand;
import org.jboss.tools.ws.jaxws.ui.commands.RemoveClientJarsCommand;
import org.jboss.tools.ws.jaxws.ui.commands.WSDL2JavaCommand;

/**
 * @author Grid Qian
 */
@SuppressWarnings({ "restriction"})
public class JBossWebServiceClient extends AbstractWebServiceClient {

	public JBossWebServiceClient(WebServiceClientInfo info) {
		super(info);
	}

	public ICommandFactory assemble(IEnvironment env, IContext ctx,
			ISelection sel, String project, String earProject) {
		return null;
	}

	public ICommandFactory deploy(IEnvironment env, IContext ctx,
			ISelection sel, String project, String earProject) {
		return null;
	}

	public ICommandFactory develop(IEnvironment env, IContext ctx,
			ISelection sel, String project, String earProject) {
		Vector<AbstractDataModelOperation> commands = new Vector<AbstractDataModelOperation>();
		ServiceModel model = new ServiceModel();
		model.setWebProjectName(project);
		commands.add(new InitialClientCommand(model, this, WebServiceScenario.CLIENT));
		commands.add(new BindingFilesValidationCommand(model));
		commands.add(new WSDL2JavaCommand(model));
		commands.add(new ClientSampleCreationCommand(model));
		commands.add(new RemoveClientJarsCommand(model));
		return new SimpleCommandFactory(commands);
	}

	public ICommandFactory run(IEnvironment env, IContext ctx, ISelection sel,
			String project, String earProject) {
		return null;
	}

	@Override
	public ICommandFactory install(IEnvironment env, IContext ctx,
			ISelection sel, String project, String earProject) {
		return null;
	}

}
