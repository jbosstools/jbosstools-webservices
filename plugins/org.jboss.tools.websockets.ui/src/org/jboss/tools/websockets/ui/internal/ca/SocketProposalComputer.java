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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IAnnotatable;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.ui.text.java.CompletionProposalLabelProvider;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.viewers.StyledString;
import org.jboss.tools.common.util.EclipseJavaUtil;
import org.jboss.tools.websockets.core.WebsocketConstants;
import org.jboss.tools.websockets.ui.WebsocketsUIPlugin;

/**
 * 
 * @author Viacheslav Kabanovich
 *
 */
public class SocketProposalComputer implements IJavaCompletionProposalComputer, WebsocketConstants {
	static String ON_CLOSE_METHOD_NAME = "onClose"; //$NON-NLS-1$
	static String ON_ERROR_METHOD_NAME = "onError"; //$NON-NLS-1$
	static String ON_MESSAGE_METHOD_NAME = "onMessage"; //$NON-NLS-1$
	static String ON_OPEN_METHOD_NAME = "onOpen"; //$NON-NLS-1$
	static String MESSAGE_PARAM = "message"; //$NON-NLS-1$

	static WebsocketMethodInfo ON_CLOSE_INFO = new WebsocketMethodInfo(
			ON_CLOSE_METHOD_NAME,
			new String[]{SESSION_TYPE, CLOSE_REASON_TYPE}, null, ON_CLOSE_ANNOTATION, 
			createMethodProposalLabel(CAMessages.onCloseProposalLabel), 
			CAMessages.onCloseProposalInfo);

	static WebsocketMethodInfo ON_ERROR_INFO = new WebsocketMethodInfo(
			ON_ERROR_METHOD_NAME,
			new String[]{SESSION_TYPE, "java.lang.Throwable"}, null, ON_ERROR_ANNOTATION, //$NON-NLS-1$
			createMethodProposalLabel(CAMessages.onErrorProposalLabel), 
			CAMessages.onErrorProposalInfo);

	static WebsocketMethodInfo ON_OPEN_INFO = new WebsocketMethodInfo(
			ON_OPEN_METHOD_NAME,
			new String[]{SESSION_TYPE, ENDPOINT_CONFIG_TYPE}, null, ON_OPEN_ANNOTATION, 
			createMethodProposalLabel(CAMessages.onOpenProposalLabel), 
			CAMessages.onOpenProposalInfo);

	static WebsocketMethodInfo ON_MESSAGE_TEXT_INFO = new WebsocketMethodInfo(
			ON_MESSAGE_METHOD_NAME,
			new String[]{"java.lang.String"}, new String[]{MESSAGE_PARAM}, ON_MESSAGE_ANNOTATION, //$NON-NLS-1$
			createMethodProposalLabel(CAMessages.onMessageTextProposalLabel), 
			CAMessages.onMessageTextProposalInfo);

	static WebsocketMethodInfo ON_MESSAGE_BINARY_INFO = new WebsocketMethodInfo(
			ON_MESSAGE_METHOD_NAME,
			new String[]{"byte[]"}, new String[]{MESSAGE_PARAM}, ON_MESSAGE_ANNOTATION, //$NON-NLS-1$
			createMethodProposalLabel(CAMessages.onMessageBinaryProposalLabel), 
			CAMessages.onMessageBinaryProposalInfo);

	static WebsocketMethodInfo ON_MESSAGE_PONG_INFO = new WebsocketMethodInfo(
			ON_MESSAGE_METHOD_NAME,
			new String[]{PONG_MESSAGE_TYPE}, new String[]{MESSAGE_PARAM}, ON_MESSAGE_ANNOTATION,
			createMethodProposalLabel(CAMessages.onMessagePongProposalLabel), 
			CAMessages.onMessagePongProposalInfo);

	CompletionProposalLabelProvider labelProvider = new CompletionProposalLabelProvider();
	
	@Override
	public void sessionStarted() {
	}

	@SuppressWarnings("deprecation")
	@Override
	public List<ICompletionProposal> computeCompletionProposals(ContentAssistInvocationContext context, IProgressMonitor monitor) {
		List<ICompletionProposal> result = new ArrayList<ICompletionProposal>();
		if(!(context instanceof JavaContentAssistInvocationContext)) {
			return result;
		}

		JavaContentAssistInvocationContext javaContext = (JavaContentAssistInvocationContext)context;
		ICompilationUnit cu = javaContext.getCompilationUnit();
	
		int offset = javaContext.getInvocationOffset();
		IDocument doc = javaContext.getDocument();
		
		Set<WebsocketMethodInfo> allowedMethods = new HashSet<WebsocketMethodInfo>();
		allowedMethods.add(ON_CLOSE_INFO);
		allowedMethods.add(ON_ERROR_INFO);
		allowedMethods.add(ON_MESSAGE_TEXT_INFO);
		allowedMethods.add(ON_MESSAGE_BINARY_INFO);
		allowedMethods.add(ON_MESSAGE_PONG_INFO);
		allowedMethods.add(ON_OPEN_INFO);

		Visitor visitor = null;

		try {
			cu.reconcile();

			IJavaElement el = cu.getElementAt(offset);
			if(!(el instanceof IMember)) {
				return result;
			}
			IMember m = (IMember)el;
			IType type = m instanceof IType ? (IType)m : m.getDeclaringType();
			if(type == null) {
				return result;
			}
			
			IAnnotation a1 = EclipseJavaUtil.findAnnotation(type, type, SERVER_END_POINT_TYPE);
			IAnnotation a2 = EclipseJavaUtil.findAnnotation(type, type, CLIENT_END_POINT_TYPE);
			if(a1 == null && a2 == null) {
				return result;
			}
			
			visitor = check(cu, context);
			if(visitor == null) {
				return result;
			}

			for (IMethod method: type.getMethods()) {
				if(allowedMethods.isEmpty()) break;
				String a = findEndpointMethodAnnotation(type, method);
				if(a == null) continue;
				if(ON_CLOSE_ANNOTATION.equals(a)) {
					allowedMethods.remove(ON_CLOSE_INFO);
				} else if(ON_ERROR_ANNOTATION.equals(a)) {
					allowedMethods.remove(ON_ERROR_INFO);
				} else if(ON_OPEN_ANNOTATION.equals(a)) {
					allowedMethods.remove(ON_OPEN_INFO);
				} else if(ON_MESSAGE_ANNOTATION.equals(a)) {
					if(hasPongParameter(method)) {
						allowedMethods.remove(ON_MESSAGE_PONG_INFO);
					} else if(hasBinaryParameter(method)) {
						allowedMethods.remove(ON_MESSAGE_BINARY_INFO);
					} else {
						allowedMethods.remove(ON_MESSAGE_TEXT_INFO);
					}
				}
			}
			if(allowedMethods.isEmpty()) {
				return result;
			}			
		} catch (JavaModelException e) {
			WebsocketsUIPlugin.pluginLog().logError(e);
		}
		
		MatchInfo match = null;
		if(visitor.isHeader) {
			match = new MatchInfo();
			match.prefix = "";
			match.matches.add(ON_CLOSE);
			match.matches.add(ON_ERROR);
			match.matches.add(ON_MESSAGE);
			match.matches.add(ON_OPEN);
		} else {
			match = computePrefix(doc, javaContext.getInvocationOffset());
		}
		if(match == null) {
			return result;
		}
		
		int length = visitor.endOffset - visitor.startOffset;
		if(allowedMethods.contains(ON_CLOSE_INFO) && match.contains(ON_CLOSE)) {
			MethodCompletionProposal proposal = new MethodCompletionProposal(
					cu.getJavaProject(), cu, ON_CLOSE_INFO,
					javaContext.getInvocationOffset(),
					length, visitor.startOffset, match.prefix.length(),
					"@OnClose public void onClose() {}" //$NON-NLS-1$
			);
			result.add(proposal);
		}
		if(allowedMethods.contains(ON_OPEN_INFO) && match.contains(ON_OPEN)) {
			MethodCompletionProposal proposal = new MethodCompletionProposal(
					cu.getJavaProject(), cu, ON_OPEN_INFO,
					javaContext.getInvocationOffset(),
					length, visitor.startOffset, match.prefix.length(), 
					"@OnOpen public void onOpen() {}" //$NON-NLS-1$
			);
			result.add(proposal);
		}
		if(allowedMethods.contains(ON_ERROR_INFO) && match.contains(ON_ERROR)) {
			MethodCompletionProposal proposal = new MethodCompletionProposal(
					cu.getJavaProject(), cu, ON_ERROR_INFO,
					javaContext.getInvocationOffset(),
					length, visitor.startOffset, match.prefix.length(), 
					"@OnError public void onError() {}" //$NON-NLS-1$
			);
			result.add(proposal);
		}
		WebsocketMethodInfo[] messages = {ON_MESSAGE_TEXT_INFO, ON_MESSAGE_BINARY_INFO, ON_MESSAGE_PONG_INFO};
		if(match.contains(ON_MESSAGE)) {
			for (WebsocketMethodInfo info: messages) {
				if(!allowedMethods.contains(info)) continue;
				MethodCompletionProposal proposal = new MethodCompletionProposal(
					cu.getJavaProject(), cu, info,
					javaContext.getInvocationOffset(),
					length, visitor.startOffset, match.prefix.length(), 
					"@OnMessage public String onMessage() {}" //$NON-NLS-1$
				);
				result.add(proposal);
			}
		}
		return result;
	}
	
	class MatchInfo {
		String prefix;
		Set<String> matches = new HashSet<String>();

		boolean contains(String name) {
			return matches.contains(name);
		}
	}

	MatchInfo computePrefix(IDocument doc, int offset) {
		MatchInfo match = new MatchInfo();
		int length = 8;
		if(offset < 8) length = offset;
		try {
			String s = doc.get(offset - length, length);
			int i = s.lastIndexOf("@"); //$NON-NLS-1$
			if(i >= 0) {
				s = s.substring(i);
				match.prefix = s.substring(1);
				match(match);
				match.prefix = s;
			} else {
				for (int k = s.length() - 1; k >= 0; k--) {
					if(!Character.isLetter(s.charAt(k))) {
						match.prefix = s.substring(k + 1);
						match(match);
						break;
					}
				}
			}
			if(!match.matches.isEmpty()) {
				for (int q = offset - match.prefix.length() - 1; q >= 0; q--) {
					char ch = doc.getChar(q);
					if(ch == ';' || ch == '{' || ch == '}' || ch == '\r' || ch =='\n') {
						break;
					} else if(Character.isWhitespace(ch)) {
						continue;
					} else {
						match.matches.clear();
						break;
					}
				}
			}
		} catch (BadLocationException e) {
			WebsocketsUIPlugin.pluginLog().logError(e);
		}
		
		return match.matches.isEmpty() ? null : match; 
	}

	@Override
	public List<IContextInformation> computeContextInformation(ContentAssistInvocationContext context, IProgressMonitor monitor) {
		return new ArrayList<IContextInformation>();
	}

	@Override
	public String getErrorMessage() {
		return null;
	}

	@Override
	public void sessionEnded() {
	}

	static StyledString createMethodProposalLabel(String name) {
		StyledString nameBuffer = new StyledString();
		nameBuffer.append(name);
		int i = name.indexOf("-"); //$NON-NLS-1$
		if(i > 0) {
			nameBuffer.setStyle(i, name.length() - i, StyledString.QUALIFIER_STYLER);
		}
		return nameBuffer;
	}

	private Visitor check(ICompilationUnit unit, ContentAssistInvocationContext context) {
		CompilationUnit ast = parse(unit);
		Visitor visitor = new Visitor(context);
		ast.accept(visitor);
		return visitor.startOffset < 0 ? null : visitor;
	}

	private static CompilationUnit parse(ICompilationUnit unit) {
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(unit);
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null); // parse
	}

	class Visitor extends ASTVisitor {
		int offset = -1;
		IDocument doc = null;
		int startOffset = -1;
		int endOffset = -1;
		boolean isHeader = false;

		public Visitor(ContentAssistInvocationContext context) {
			offset = context.getInvocationOffset();
			startOffset = offset;
			endOffset = offset;
			doc = context.getDocument();
		}

		@Override
		public boolean visit(TypeDeclaration node) {
			SimpleName n = node.getName();
			if(offset >= n.getStartPosition() && offset <= n.getStartPosition() + n.getLength()) {
				//Called on class name. Method will be added to the end.
				startOffset = node.getStartPosition() + node.getLength() - 1;
				endOffset = startOffset;
				isHeader = true;
				return false;
			}
			if(isBeforeEnd(node.getName()) || isBeforeEnd(node.getSuperclassType())) {
				startOffset = endOffset = -1;
				return false;
			}
			for (Object o: node.superInterfaceTypes()) {
				if(o instanceof ASTNode && isBeforeEnd((ASTNode)o)) {
					startOffset = endOffset = -1;
					return false;
				}
			}
			
			return true;
		}

		boolean isBeforeEnd(ASTNode n) {
			return n != null && offset <= n.getStartPosition() + n.getLength();
		}
		
		@Override
		public boolean visit(Block node) {
			if(offset > node.getStartPosition() && offset < node.getStartPosition() + node.getLength()) {
				startOffset = endOffset = -1;
			}
			return false;
		}

		@Override
		public boolean visit(FieldDeclaration node) {
			return visitFieldOrMethod(node);
		}

		@Override
		public boolean visit(MethodDeclaration node) {
			return visitFieldOrMethod(node);
		}

		boolean visitFieldOrMethod(BodyDeclaration node) {
			if(offset > node.getStartPosition() && offset < node.getStartPosition() + node.getLength()) {
				try {
					String text = doc.get(node.getStartPosition(), offset - node.getStartPosition());
					int i = text.indexOf("@"); //$NON-NLS-1$
					if(i < 0) {
						if(text.trim().length() > 0) {
							startOffset = endOffset = -1;
						}
						return false; //ok
					}
					if(text.substring(0, i).trim().length() > 0) {
						startOffset = endOffset = -1;
						return false;
					}
					text = text.substring(i + 1); //name of annotation
					if(!isPrefix(text)) {
						startOffset = endOffset = -1;
					}
				} catch (BadLocationException e) {
					WebsocketsUIPlugin.pluginLog().logError(e);
				}
			}
			return false;
		}
		
	}

	boolean isPrefix(String text) {
		MatchInfo match = new MatchInfo();
		match.prefix = text;
		match(match);
		return !match.matches.isEmpty();
	}

	private void match(MatchInfo matchInfo) {
		matchName(matchInfo, ON_CLOSE);
		matchName(matchInfo, ON_ERROR);
		matchName(matchInfo, ON_MESSAGE);
		matchName(matchInfo, ON_OPEN);
	}

	private void matchName(MatchInfo matchInfo, String tryName) {
		if(tryName.toLowerCase().startsWith(matchInfo.prefix.toLowerCase())) {
			matchInfo.matches.add(tryName);
		}
	}

	static Set<String> ON_ANNOTATIONS_QNAMES = new HashSet<String>();
	static Set<String> ON_ANNOTATIONS_NAMES = new HashSet<String>();
	static {
		ON_ANNOTATIONS_QNAMES.add(ON_CLOSE_ANNOTATION);
		ON_ANNOTATIONS_QNAMES.add(ON_ERROR_ANNOTATION);
		ON_ANNOTATIONS_QNAMES.add(ON_MESSAGE_ANNOTATION);
		ON_ANNOTATIONS_QNAMES.add(ON_OPEN_ANNOTATION);
		ON_ANNOTATIONS_NAMES.add(ON_CLOSE);
		ON_ANNOTATIONS_NAMES.add(ON_ERROR);
		ON_ANNOTATIONS_NAMES.add(ON_MESSAGE);
		ON_ANNOTATIONS_NAMES.add(ON_OPEN);
	}
	
	String findEndpointMethodAnnotation(IType sourceType, IAnnotatable member) throws JavaModelException {
		for (IAnnotation annotation: member.getAnnotations()) {
			String name = annotation.getElementName();
			if(ON_ANNOTATIONS_QNAMES.contains(name)) {
				return name;
			}
			if(ON_ANNOTATIONS_NAMES.contains(name)) {
				String qName = EclipseJavaUtil.resolveType(sourceType, name);
				if(qName != null) {
					IType annotationType = sourceType.getJavaProject().findType(qName);
					if(annotationType != null && ON_ANNOTATIONS_QNAMES.contains(annotationType.getFullyQualifiedName())) {
						return annotationType.getFullyQualifiedName();
					}
				}				
			}
 		}		
		return null;
	}

	boolean hasPongParameter(IMethod method) {
		for (String type: method.getParameterTypes()) {
			String qType = EclipseJavaUtil.resolveTypeAsString(method.getDeclaringType(), type);
			if(PONG_MESSAGE_TYPE.equals(qType)) {
				return true;
			}
		}
		return false;
	}

	boolean hasBinaryParameter(IMethod method) {
		for (String type: method.getParameterTypes()) {
			String qType = EclipseJavaUtil.resolveTypeAsString(method.getDeclaringType(), type);
			if("byte[]".equals(qType) || "java.nio.ByteBuffer".equals(qType) //$NON-NLS-1$ //$NON-NLS-2$
					|| "java.io.InputStream".equals(qType)) { //$NON-NLS-1$ 
				return true;
			}
		}
		return false;
	}
}
