/******************************************************************************* 
 * Copyright (c) 2015 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.websockets.core;

public interface WebsocketConstants {

	public static String WEBSOCKET_PACK = "javax.websocket."; //$NON-NLS-1$
	public static String SERVER_END_POINT_TYPE = WEBSOCKET_PACK + "server.ServerEndpoint"; //$NON-NLS-1$
	public static String CLIENT_END_POINT_TYPE = WEBSOCKET_PACK + "ClientEndpoint"; //$NON-NLS-1$
	public static String SESSION_TYPE = WEBSOCKET_PACK + "Session"; //$NON-NLS-1$
	public static String CLOSE_REASON_TYPE = WEBSOCKET_PACK + "CloseReason"; //$NON-NLS-1$
	public static String ENDPOINT_CONFIG_TYPE = WEBSOCKET_PACK + "EndpointConfig"; //$NON-NLS-1$

	public static String ON_CLOSE = "OnClose"; //$NON-NLS-1$
	public static String ON_ERROR = "OnError"; //$NON-NLS-1$
	public static String ON_MESSAGE = "OnMessage"; //$NON-NLS-1$
	public static String ON_OPEN = "OnOpen"; //$NON-NLS-1$

	public static String ON_CLOSE_ANNOTATION = WEBSOCKET_PACK + ON_CLOSE;
	public static String ON_ERROR_ANNOTATION = WEBSOCKET_PACK + ON_ERROR;
	public static String ON_MESSAGE_ANNOTATION = WEBSOCKET_PACK + ON_MESSAGE;
	public static String ON_OPEN_ANNOTATION = WEBSOCKET_PACK + ON_OPEN;

	public static String PONG_MESSAGE_TYPE = WEBSOCKET_PACK + "PongMessage"; //$NON-NLS-1$

}
