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
package org.jboss.tools.ws.creation.ui.wsrt;

import org.eclipse.wst.ws.internal.wsrt.AbstractWebServiceRuntime;
import org.eclipse.wst.ws.internal.wsrt.IWebService;
import org.eclipse.wst.ws.internal.wsrt.IWebServiceClient;
import org.eclipse.wst.ws.internal.wsrt.WebServiceClientInfo;
import org.eclipse.wst.ws.internal.wsrt.WebServiceInfo;

@SuppressWarnings("restriction")
public class JBossWebServiceRuntime extends AbstractWebServiceRuntime{

	@Override
	public IWebService getWebService(WebServiceInfo info) {
		return new JBossWebService(info);
	}

	@Override
	public IWebServiceClient getWebServiceClient(WebServiceClientInfo info) {
		return new JBossWebServiceClient(info);
	}

}
