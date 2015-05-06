/******************************************************************************* 
 * Copyright (c) 2015 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.websockets.ui.internal.ca;

import org.eclipse.osgi.util.NLS;

/**
 * 
 * @author Viacheslav Kabanovich
 *
 */
public class CAMessages {
	private static final String BUNDLE_NAME = "org.jboss.tools.websockets.ui.internal.ca.messages"; //$NON-NLS-1$

	public static String onCloseProposalLabel;
	public static String onErrorProposalLabel;
	public static String onMessageTextProposalLabel;
	public static String onMessageBinaryProposalLabel;
	public static String onMessagePongProposalLabel;
	public static String onOpenProposalLabel;

	public static String onCloseProposalInfo;
	public static String onErrorProposalInfo;
	public static String onMessageTextProposalInfo;
	public static String onMessageBinaryProposalInfo;
	public static String onMessagePongProposalInfo;
	public static String onOpenProposalInfo;

	static {
		NLS.initializeMessages(BUNDLE_NAME, CAMessages.class);
	}
}
