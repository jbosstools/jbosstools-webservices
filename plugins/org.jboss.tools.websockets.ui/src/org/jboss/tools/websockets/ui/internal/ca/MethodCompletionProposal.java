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

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ITrackedNodePosition;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.formatter.IndentManipulation;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.javadoc.JavaDocLocations;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.ASTProvider;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.text.java.JavaTypeCompletionProposal;
import org.eclipse.jdt.internal.ui.text.java.hover.JavadocBrowserInformationControlInput;
import org.eclipse.jdt.ui.SharedASTProvider;
import org.eclipse.jface.internal.text.html.HTMLPrinter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension4;
import org.eclipse.text.edits.MalformedTreeException;

/**
 * 
 * @author Viacheslav Kabanovich
 *
 */
@SuppressWarnings("restriction")
public class MethodCompletionProposal extends JavaTypeCompletionProposal implements ICompletionProposalExtension4 {
	private final IJavaProject fJavaProject;
	private final WebsocketMethodInfo methodInfo;
	private final int position;
	private final int prefixLength;

	public MethodCompletionProposal(IJavaProject jproject, ICompilationUnit cu, 
			WebsocketMethodInfo methodInfo,
			int start, int length, int position, int prefixLength,
			String completionProposal) {
		super(completionProposal, cu, start, length, JavaPluginImages.get(JavaPluginImages.IMG_MISC_PUBLIC), methodInfo.displayName, 20000);
		Assert.isNotNull(jproject);
		Assert.isNotNull(methodInfo.methodName);
		Assert.isNotNull(methodInfo.paramTypes);
		Assert.isNotNull(cu);

		this.methodInfo = methodInfo;
		
		this.position = position;
		this.prefixLength = prefixLength;

		fJavaProject = jproject;

		setReplacementString(completionProposal);
	}

	@Override
	public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
		return methodInfo.methodName;
	}

	private CompilationUnit getRecoveredAST(IDocument document, int offset, Document recoveredDocument) {
		CompilationUnit ast= SharedASTProvider.getAST(fCompilationUnit, SharedASTProvider.WAIT_ACTIVE_ONLY, null);
		if (ast != null) {
			recoveredDocument.set(document.get());
			return ast;
		}

		char[] content= document.get().toCharArray();

		// clear prefix to avoid compile errors
		int index= offset - 1;
		while (index >= 0 && Character.isJavaIdentifierPart(content[index])) {
			content[index]= ' ';
			index--;
		}

		recoveredDocument.set(new String(content));

		final ASTParser parser = ASTParser.newParser(ASTProvider.SHARED_AST_LEVEL);
		parser.setResolveBindings(true);
		parser.setStatementsRecovery(true);
		parser.setSource(content);
		parser.setUnitName(fCompilationUnit.getElementName());
		parser.setProject(fCompilationUnit.getJavaProject());
		return (CompilationUnit) parser.createAST(new NullProgressMonitor());
	}

	@Override
	protected boolean updateReplacementString(IDocument document, char trigger, int offset, ImportRewrite importRewrite) throws CoreException, BadLocationException {
		Document recoveredDocument= new Document();
		CompilationUnit unit = getRecoveredAST(document, offset, recoveredDocument);
		if (importRewrite == null) {
			importRewrite = StubUtility.createImportRewrite(unit, true); // create a dummy import rewriter to have one
		}

		ITypeBinding declaringType = null;
		ChildListPropertyDescriptor descriptor = null;
		ASTNode node= NodeFinder.perform(unit, position, 1);
		if (node instanceof SimpleName || node instanceof Modifier) {
			node = node.getParent();
			if(node instanceof MarkerAnnotation) {
				node = node.getParent();
			}
		}
		if(node instanceof MethodDeclaration || node instanceof FieldDeclaration) {
			node = node.getParent();
		}
		if (node instanceof AnonymousClassDeclaration) {
			declaringType= ((AnonymousClassDeclaration) node).resolveBinding();
			descriptor= AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY;
		} else if (node instanceof AbstractTypeDeclaration) {
			AbstractTypeDeclaration declaration= (AbstractTypeDeclaration) node;
			descriptor= declaration.getBodyDeclarationsProperty();
			declaringType= declaration.resolveBinding();
		}
		if (declaringType != null) {
			ASTRewrite rewrite = ASTRewrite.create(unit.getAST());
			CodeGenerationSettings settings = JavaPreferencesSettings.getCodeGenerationSettings(fJavaProject);
				
			String delimiter = StubUtility.getLineDelimiterUsed(unit.getTypeRoot());

			StringBuffer content = new StringBuffer();
			if(methodInfo.annotation != null) {
				importRewrite.addImport(methodInfo.annotation);
				int dot = methodInfo.annotation.lastIndexOf('.');
				String simpleName = methodInfo.annotation.substring(dot + 1);
				content.append("@").append(simpleName).append(delimiter); //$NON-NLS-1$
			}
			content.append("public void " + methodInfo.methodName + "("); //$NON-NLS-1$ //$NON-NLS-2$
			boolean first = true;
			for (int i = 0; i < methodInfo.paramTypes.length; i++) {
				String paramType = methodInfo.paramTypes[i];
				if(!first) {
					content.append(", "); //$NON-NLS-1$
				} else {
					first = false;
				}
				importRewrite.addImport(paramType);
				String simpleName = paramType.substring(paramType.lastIndexOf('.') + 1);
				String name = methodInfo.paramNames != null ? methodInfo.paramNames[i] : simpleName.substring(0, 1).toLowerCase() + simpleName.substring(1);
				content.append(simpleName).append(" ").append(name); //$NON-NLS-1$
			}
			content.append(") {").append(delimiter).append("\t\t"); //$NON-NLS-1$ //$NON-NLS-2$
			String body = StubUtility.getMethodBodyContent(false, fJavaProject, declaringType.getName(), declaringType.getName(), "", delimiter); //$NON-NLS-1$
			if(body == null ||body.length() == 0) {
				body = delimiter;
			}
			content.append(body);
			content.append("}").append(delimiter); //$NON-NLS-1$

				MethodDeclaration stub = (MethodDeclaration) rewrite.createStringPlaceholder(
						CodeFormatterUtil.format(CodeFormatter.K_CLASS_BODY_DECLARATIONS, content.toString(), 0, delimiter, unit.getTypeRoot().getJavaProject()), ASTNode.METHOD_DECLARATION);

				ListRewrite rewriter = rewrite.getListRewrite(node, descriptor);
				rewriter.insertFirst(stub, null);

				ITrackedNodePosition position= rewrite.track(stub);
				try {
					rewrite.rewriteAST(recoveredDocument, fJavaProject.getOptions(true)).apply(recoveredDocument);

					String generatedCode = recoveredDocument.get(position.getStartPosition(), position.getLength());
					int generatedIndent = IndentManipulation.measureIndentUnits(getIndentAt(recoveredDocument, position.getStartPosition(), settings), settings.tabWidth, settings.indentWidth);

					String indent= getIndentAt(document, getReplacementOffset(), settings);
					if(this.position > offset && indent.length() == 0) {
						indent = "\t"; //$NON-NLS-1$
					}
					String replacementString = IndentManipulation.changeIndent(generatedCode, generatedIndent, settings.tabWidth, settings.indentWidth, indent, TextUtilities.getDefaultLineDelimiter(document));
					if(this.position > offset) {
						replacementString = "\t" + replacementString + delimiter; //$NON-NLS-1$
						setReplacementLength(getReplacementLength());
						setReplacementOffset(getReplacementOffset() + this.position - offset);
					} else if(prefixLength > 0) {
						setReplacementLength(getReplacementLength() + prefixLength);
						setReplacementOffset(getReplacementOffset() - prefixLength);
					}
					setReplacementString(replacementString);
				} catch (MalformedTreeException exception) {
					JavaPlugin.log(exception);
				} catch (BadLocationException exception) {
					JavaPlugin.log(exception);
				}
		}
		return true;
	}

	private static String getIndentAt(IDocument document, int offset, CodeGenerationSettings settings) {
		try {
			IRegion region= document.getLineInformationOfOffset(offset);
			return IndentManipulation.extractIndentString(document.get(region.getOffset(), region.getLength()), settings.tabWidth, settings.indentWidth);
		} catch (BadLocationException e) {
			return ""; //$NON-NLS-1$
		}
	}

	@Override
	public boolean isAutoInsertable() {
		return false;
	}

	@Override
	public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
		String info = methodInfo.info;
		if (info != null && info.length() > 0) {
			StringBuffer buffer= new StringBuffer();
			HTMLPrinter.insertPageProlog(buffer, 0, getCSSStyles());

			buffer.append(info);

			IJavaElement element= null;
			try {
				element= fJavaProject;
				if (element instanceof IMember) {
					String base= JavaDocLocations.getBaseURL(element, false);
					if (base != null) {
						int endHeadIdx= buffer.indexOf("</head>"); //$NON-NLS-1$
						buffer.insert(endHeadIdx, "\n<base href='" + base + "'>\n"); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}

			HTMLPrinter.addPageEpilog(buffer);
			info= buffer.toString();

			return new JavadocBrowserInformationControlInput(null, element, info, 0);
		}
		return null;
	}

}
