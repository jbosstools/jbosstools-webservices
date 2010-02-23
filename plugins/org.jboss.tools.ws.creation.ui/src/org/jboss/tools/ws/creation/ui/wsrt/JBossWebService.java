/******************************************************************************* 
 * Copyright (c) 2010 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.ws.creation.ui.wsrt;

import java.util.Vector;

import org.eclipse.wst.command.internal.env.core.ICommandFactory;
import org.eclipse.wst.command.internal.env.core.SimpleCommandFactory;
import org.eclipse.wst.common.environment.IEnvironment;
import org.eclipse.wst.common.frameworks.datamodel.AbstractDataModelOperation;
import org.eclipse.wst.ws.internal.wsrt.AbstractWebService;
import org.eclipse.wst.ws.internal.wsrt.IContext;
import org.eclipse.wst.ws.internal.wsrt.ISelection;
import org.eclipse.wst.ws.internal.wsrt.WebServiceInfo;
import org.eclipse.wst.ws.internal.wsrt.WebServiceScenario;
import org.jboss.tools.ws.creation.core.commands.AddApplicationXMLCommand;
import org.jboss.tools.ws.creation.core.commands.BindingFilesValidationCommand;
import org.jboss.tools.ws.creation.core.commands.ImplementationClassCreationCommand;
import org.jboss.tools.ws.creation.core.commands.InitialCommand;
import org.jboss.tools.ws.creation.core.commands.Java2WSCommand;
import org.jboss.tools.ws.creation.core.commands.MergeWebXMLCommand;
import org.jboss.tools.ws.creation.core.commands.ValidateWSImplCommand;
import org.jboss.tools.ws.creation.core.commands.WSDL2JavaCommand;
import org.jboss.tools.ws.creation.core.data.ServiceModel;

/**
 * @author Grid Qian
 */
@SuppressWarnings("restriction")
public class JBossWebService extends AbstractWebService {

	public JBossWebService(WebServiceInfo info){
		super(info);
	}
	
	@Override
	public ICommandFactory assemble(IEnvironment env, IContext ctx,
			ISelection sel, String project, String earProject) {
		Vector<AbstractDataModelOperation> commands = new Vector<AbstractDataModelOperation>();
		commands.add(new AddApplicationXMLCommand(earProject));
		return new SimpleCommandFactory(commands);
	}

	@Override
	public ICommandFactory deploy(IEnvironment env, IContext ctx,
			ISelection sel, String project, String earProject) {
		return null;
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public ICommandFactory develop(IEnvironment env, IContext ctx,
			ISelection sel, String project, String earProject) {
		
		Vector commands = new Vector();
		ServiceModel model = new ServiceModel();
		model.setWebProjectName(project);
		if (ctx.getScenario().getValue() == WebServiceScenario.TOPDOWN)	{ 
			commands.add(new InitialCommand(model, this, WebServiceScenario.TOPDOWN));
			commands.add(new BindingFilesValidationCommand(model));
			commands.add(new WSDL2JavaCommand(model));
			commands.add(new ImplementationClassCreationCommand(model));
			commands.add(new MergeWebXMLCommand(model));
		}
		else if (ctx.getScenario().getValue() == WebServiceScenario.BOTTOMUP){
			commands.add(new InitialCommand(model, this, WebServiceScenario.BOTTOMUP));
			commands.add(new ValidateWSImplCommand(model));
			commands.add(new Java2WSCommand(model));
			commands.add(new MergeWebXMLCommand(model));
			//commands.add(new JBossWSRuntimeCommand(ResourcesPlugin.getWorkspace().getRoot().getProject(project)));
		}
		
		return new SimpleCommandFactory(commands);
	}

	@Override
	public ICommandFactory install(IEnvironment env, IContext ctx,
			ISelection sel, String project, String earProject) {
		return null;
	}

	@Override
	public ICommandFactory run(IEnvironment env, IContext ctx, ISelection sel,
			String project, String earProject) {
		return null;
	}

}
