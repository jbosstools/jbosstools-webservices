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
package org.jboss.tools.websockets.ui.test.ca;

import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.ISourceViewer;
import org.jboss.tools.common.base.test.contentassist.JavaContentAssistantTestCase;
import org.jboss.tools.test.util.TestProjectProvider;
import org.jboss.tools.websockets.ui.internal.ca.CAMessages;
import org.jboss.tools.websockets.ui.internal.ca.SocketProposalComputer;
import org.jboss.tools.websockets.ui.test.Activator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 * @author Viacheslav Kabanovich
 *
 */
public class WebsocketsContentAssistTestCase extends JavaContentAssistantTestCase {
	private static final String PROJECT_NAME = "WebsocketsTest";

	TestProjectProvider provider = null;

	public WebsocketsContentAssistTestCase() {}

	@Override
	@BeforeClass
	public void setUp() throws Exception {
		provider = new TestProjectProvider(Activator.PLUGIN_ID, null, PROJECT_NAME, true); 
		project = provider.getProject();
	}

	@Override
	@AfterClass
	protected void tearDown() throws Exception {
		if(provider != null) {
			provider.dispose();
		}
	}

	/**
	 * Content assist is invoked on class name. Class has no Websocket annotated methods.
	 */
	@Test
	public void testServerEndpointWithNoMethods() {
		List<ICompletionProposal> ps = getProposals("/src/websockets/MyServerEndpoint.java", "MyServerEndpoint", 3);
		
		assertProposal(CAMessages.onCloseProposalLabel, ps, true);
		assertProposal(CAMessages.onOpenProposalLabel, ps, true);
		assertProposal(CAMessages.onMessageTextProposalLabel, ps, true);
		assertProposal(CAMessages.onMessageBinaryProposalLabel, ps, true);
		assertProposal(CAMessages.onMessagePongProposalLabel, ps, true);
		assertProposal(CAMessages.onOpenProposalLabel, ps, true);
	}

	/**
	 * Content assist is invoked on class name. Class has method '@OnMessage' with text message.
	 */
	@Test
	public void testServerEndpointWithOnMessage() {
		List<ICompletionProposal> ps = getProposals("/src/websockets/MyServer2Endpoint.java", "MyServer2Endpoint", 3);
		
		assertProposal(CAMessages.onCloseProposalLabel, ps, true);
		assertProposal(CAMessages.onOpenProposalLabel, ps, true);
		assertProposal(CAMessages.onMessageTextProposalLabel, ps, false);
		assertProposal(CAMessages.onMessageBinaryProposalLabel, ps, true);
		assertProposal(CAMessages.onMessagePongProposalLabel, ps, true);
		assertProposal(CAMessages.onOpenProposalLabel, ps, true);
	}

	/**
	 * Content assist is invoked after '@OnM' text.
	 */
	@Test
	public void testClientEndpointWithPrefix() {
		List<ICompletionProposal> ps = getProposals("/src/websockets/MyClientEndpoint.java", "@OnM", 4);
		
		assertProposal(CAMessages.onCloseProposalLabel, ps, false);
		assertProposal(CAMessages.onOpenProposalLabel, ps, false);
		assertProposal(CAMessages.onMessageTextProposalLabel, ps, true);
		assertProposal(CAMessages.onMessageBinaryProposalLabel, ps, true);
		assertProposal(CAMessages.onMessagePongProposalLabel, ps, true);
		assertProposal(CAMessages.onOpenProposalLabel, ps, false);
	}

	/**
	 * Content assist is invoked after 'onc' text.
	 */
	@Test
	public void testClientEndpointWithAlternaticePrefix() {
		List<ICompletionProposal> ps = getProposals("/src/websockets/MyClient2Endpoint.java", "onc", 3);
		
		assertProposal(CAMessages.onCloseProposalLabel, ps, true);
		assertProposal(CAMessages.onOpenProposalLabel, ps, false);
		assertProposal(CAMessages.onMessageTextProposalLabel, ps, false);
		assertProposal(CAMessages.onMessageBinaryProposalLabel, ps, false);
		assertProposal(CAMessages.onMessagePongProposalLabel, ps, false);
		assertProposal(CAMessages.onOpenProposalLabel, ps, false);
	}

	/**
	 * Content assist is invoked after 'clo' text.
	 */
	@Test
	public void testClientEndpointWithNonMatchingPrefix() {
		List<ICompletionProposal> ps = getProposals("/src/websockets/MyClient3Endpoint.java", "clo", 3);
		
		assertProposal(CAMessages.onCloseProposalLabel, ps, false);
		assertProposal(CAMessages.onOpenProposalLabel, ps, false);
		assertProposal(CAMessages.onMessageTextProposalLabel, ps, false);
		assertProposal(CAMessages.onMessageBinaryProposalLabel, ps, false);
		assertProposal(CAMessages.onMessagePongProposalLabel, ps, false);
		assertProposal(CAMessages.onOpenProposalLabel, ps, false);
	}

	/**
	 * Content assist is invoked in a class that is not Websockets enpoint annotated.
	 */
	@Test
	public void testNonRelevantType() {
		List<ICompletionProposal> ps = getProposals("/src/websockets/Util.java", "Util", 3);
		
		assertProposal(CAMessages.onCloseProposalLabel, ps, false);
		assertProposal(CAMessages.onOpenProposalLabel, ps, false);
		assertProposal(CAMessages.onMessageTextProposalLabel, ps, false);
		assertProposal(CAMessages.onMessageBinaryProposalLabel, ps, false);
		assertProposal(CAMessages.onMessagePongProposalLabel, ps, false);
		assertProposal(CAMessages.onOpenProposalLabel, ps, false);
	}

	List<ICompletionProposal> getProposals(String filePath, String searchString, int searchPos) {
		openEditor(filePath);
		ISourceViewer v = getViewer();
		String text = v.getDocument().get();
		int offset = text.indexOf(searchString);
		assertTrue("Substring " + searchString + " is not found.", offset >= 0);
		offset += searchPos;
		JavaContentAssistInvocationContext context = new JavaContentAssistInvocationContext(v, offset, editorPart);
		SocketProposalComputer computer = new SocketProposalComputer();
		return computer.computeCompletionProposals(context, new NullProgressMonitor());
	}

	ICompletionProposal assertProposal(String label, List<ICompletionProposal> ps, boolean isExpected) {
		ICompletionProposal p = findByLabel(label, ps);
		if(isExpected) {
			assertNotNull("Proposal '" + label + "' is not found.", p);
		} else {
			assertNull("Proposal '" + label + "' is not legal.", p);
		}
		return p;
	}

	ICompletionProposal findByLabel(String label, List<ICompletionProposal> ps) {
		for (ICompletionProposal p: ps) {
			if(label.equals(p.getDisplayString())) {
				return p;
			}
		}
		return null;
	}
}
