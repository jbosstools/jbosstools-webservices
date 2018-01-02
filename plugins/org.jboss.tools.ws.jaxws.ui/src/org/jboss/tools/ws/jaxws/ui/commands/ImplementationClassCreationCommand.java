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
package org.jboss.tools.ws.jaxws.ui.commands;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.wsdl.Port;
import javax.wsdl.Service;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.WildcardType;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.common.frameworks.datamodel.AbstractDataModelOperation;
import org.jboss.tools.ws.jaxws.core.data.ServiceModel;
import org.jboss.tools.ws.jaxws.core.util.StatusUtils;
import org.jboss.tools.ws.jaxws.ui.JBossJAXWSUIMessages;
import org.jboss.tools.ws.jaxws.ui.JBossJAXWSUIPlugin;
import org.jboss.tools.ws.jaxws.ui.utils.JBossWSCreationUtils;

public class ImplementationClassCreationCommand extends
		AbstractDataModelOperation {

	private static final String PREFIX_JAXWS_ANNOTATION_CLASS = "javax.jws"; //$NON-NLS-1$
	private static final String DEFAULT_CU_SUFFIX = ".java"; //$NON-NLS-1$

	private static final String ANNOTATION_WEB_SERVICE_FULLNAME = "javax.jws.WebService"; //$NON-NLS-1$
	private static final String ANNOTATION_TYPE_NAME_WEBSERVICE = "WebService";; //$NON-NLS-1$
	private static final String ANNOTATION_PROPERTY_SERVICE_NAME = "serviceName"; //$NON-NLS-1$
	private static final String ANNOTATION_PROPERTY_ENDPOINT_INTERFACE = "endpointInterface"; //$NON-NLS-1$
	private static final String ANNOTATION_PROPERTY_TNS = "targetNamespace"; //$NON-NLS-1$
	private static final String IMPL_PACKAGE = ".impl"; //$NON-NLS-1$
	public static final String LINE_SEPARATOR = System.getProperty("line.separator"); //$NON-NLS-1$

	private ServiceModel model;
	private IJavaProject project;
	private String packageName;
	private String serviceName;
	private String targetNamespace;

	public ImplementationClassCreationCommand(ServiceModel model) {
		this.model = model;
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {
		
		IStatus status = Status.OK_STATUS;
		// if the user does not check the generate implementation class button,
		// do nothing
		if (!model.isGenImplementation()) {
			return status;
		}
		
		project = model.getJavaProject();
		packageName = model.getCustomPackage();
		List<ICompilationUnit> portTypeUnits = JBossWSCreationUtils
				.findJavaUnitsByAnnotation(project, JBossJAXWSUIMessages.Webservice_Annotation, packageName);
		if (portTypeUnits.size() == 0 ) {
			return status;
		}
		
		packageName = portTypeUnits.get(0).getParent().getElementName() + IMPL_PACKAGE;
		IPackageFragment pack = null;
		try {
			pack = createImplPackage(packageName);
		} catch (JavaModelException e1) {
			status = StatusUtils.errorStatus(JBossJAXWSUIMessages.Error_Message_Failed_to_Generate_Implementation,e1);
			return status;
		}
		Service service = model.getService();
		serviceName = service.getQName().getLocalPart();
		targetNamespace = model.getWsdlDefinition().getTargetNamespace();
		Iterator<?> iter = service.getPorts().values().iterator();
		List<String> ptList = new LinkedList<String>();
		while (iter.hasNext()) {
			Port port = (Port) iter.next();
			ptList.add(port.getBinding().getPortType().getQName().getLocalPart().toLowerCase());
		}
		
		boolean isOverWrite = false;
		try {
			for (ICompilationUnit portType : portTypeUnits) {
				if (!portType.findPrimaryType().isInterface()) {
					continue;
				}
				String clsName = getClassName(portType.getElementName());
				String implClsName = getImplClassName(clsName);
				if (!ptList.contains(clsName.toLowerCase())) {
					continue;
				}				
				if (!isOverWrite && findImplClass(implClsName)) {
					if (!isOverwriteClass()) {
						break;
					} 
				    isOverWrite = true;
				}
				generateImplClass(portType, pack, clsName, implClsName);
				model.addServiceClasses(new StringBuffer(packageName).append(".").append(implClsName).toString()); //$NON-NLS-1$
			}
		} catch (CoreException e) {
			status = StatusUtils
					.errorStatus(
							JBossJAXWSUIMessages.Error_Message_Failed_to_Generate_Implementation,
							e);
			JBossJAXWSUIPlugin.getDefault().logError(e);
		} catch (BadLocationException e) {
			status = StatusUtils
					.errorStatus(
							JBossJAXWSUIMessages.Error_Message_Failed_to_Generate_Implementation,
							e);
			JBossJAXWSUIPlugin.getDefault().logError(e);
		}
		return status;
	}

	@SuppressWarnings("unchecked")
	protected void generateImplClass(ICompilationUnit portType, IPackageFragment pack, String ptCls, String clsName)
			throws CoreException, BadLocationException {
		ASTParser astp = ASTParser.newParser(AST.JLS3);
		astp.setKind(ASTParser.K_COMPILATION_UNIT);
		astp.setSource(portType);
		CompilationUnit cu = (CompilationUnit) astp.createAST(null);
		String implFileName = getJavaFileName(clsName);
		ICompilationUnit icu = pack.createCompilationUnit(implFileName,"", true, null); //$NON-NLS-1$
		
		// create a working copy with a new owner
		icu.becomeWorkingCopy(null);
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setSource(icu);
		parser.setResolveBindings(false);
		parser.setFocalPosition(0);
		CompilationUnit implCu = (CompilationUnit) parser.createAST(null);
		AST ast = implCu.getAST();

		// creation of a Document and ASTRewrite
		String source = icu.getBuffer().getContents();
		Document document = new Document(source);
		implCu.recordModifications();

		// start to add content into implementation class

		// add package declaration for impl class:
		PackageDeclaration implPackage = ast.newPackageDeclaration();
		implPackage.setName(ast.newName(pack.getElementName()));
		implCu.setPackage(implPackage);

		// add imports for implementation class
		addImportsToImplementationClass(implCu, cu, ptCls);

		// add class declaration
		TypeDeclaration type = ast.newTypeDeclaration();
		type.setInterface(false);
		// add WebService annotation
		String endpoint = getPortTypeFullName(portType.getParent().getElementName(), ptCls);
		NormalAnnotation ann = null;
		if (serviceName != null) {
			ann = createAnnotation(ast, serviceName, endpoint, targetNamespace);
		} else {
			ann = createAnnotation(ast, clsName, endpoint, targetNamespace);
		}
		type.modifiers().add(ann);
		type.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
		type.setName(ast.newSimpleName(clsName));
		type.superInterfaceTypes().add(ast.newSimpleType(ast.newName(ptCls)));

		// add Logger variable declaration
		// createLoggerField(ast, type, portTypeName);

		// add method implementation
		TypeDeclaration inTD = (TypeDeclaration) cu.types().get(0);
		// firstly, get all methods that declared in Interface class and then
		// add corresponding methods to
		// the impl class
		MethodDeclaration[] methods = inTD.getMethods();
		for (int i = 0; i < methods.length; i++) {
			MethodDeclaration newMethod = createMethodForImplClass(ast,
					methods[i]);
			type.bodyDeclarations().add(newMethod);
		}
		implCu.types().add(type);

		// try to save the Java file
		TextEdit edits = implCu.rewrite(document, icu.getJavaProject()
				.getOptions(true));
		edits.apply(document);
		String newSource = document.get();
		icu.getBuffer().setContents(newSource);
		icu.reconcile(ICompilationUnit.NO_AST, false, null, null);

		icu.commitWorkingCopy(true, null);
		icu.discardWorkingCopy();

	}

	private String getImplPackageName() {
		return packageName;
	}

	private String getJavaFileName(String className) {
		return className + DEFAULT_CU_SUFFIX;
	}

	private String getImplClassName(String className) {
		return className + "Impl"; //$NON-NLS-1$
	}

	private String getClassName(String className) {
		String clsName = className.substring(0, className.length() - 5);
		return clsName;
	}

	private String getPortTypeFullName(String packageName, String className) {
		return packageName + "." + className; //$NON-NLS-1$
	}

	@SuppressWarnings("unchecked")
	private void addImportsToImplementationClass(CompilationUnit implCU,
			CompilationUnit serviceCU, String serviceName) {
		List<ImportDeclaration> imports = getImportsWithoutJaxwsAnnotation(serviceCU);
		AST implAST = implCU.getAST();

		// add imports for implementation class
		for (ImportDeclaration id : imports) {
			ImportDeclaration newId = implAST.newImportDeclaration();
			newId.setName(implAST.newName(id.getName().getFullyQualifiedName()));
			implCU.imports().add(newId);
		}

		// import port type interface
		ImportDeclaration importDec = implAST.newImportDeclaration();
		importDec.setName(implAST.newName(serviceCU.getPackage().getName().toString()));
		importDec.setOnDemand(true);
		implCU.imports().add(importDec);
		// importDec = implAST.newImportDeclaration();
		// importDec.setName(implAST.newName(LOGGER_CLASS_FULLNAME));
		// implCU.imports().add(importDec);

		// import jaxws WebService
		importDec = implAST.newImportDeclaration();
		// hardcode here?
		importDec.setName(implAST.newName(ANNOTATION_WEB_SERVICE_FULLNAME));
		implCU.imports().add(importDec);
	}

	/*
	 * create web service annotation
	 */
	@SuppressWarnings("unchecked")
	protected NormalAnnotation createAnnotation(AST ast, String serviceName,
			String endpoint, String targetNamespace) {
		NormalAnnotation ann = ast.newNormalAnnotation();
		ann.setTypeName(ast.newSimpleName(ANNOTATION_TYPE_NAME_WEBSERVICE));

		MemberValuePair member = createMemberValuePair(ast,
				ANNOTATION_PROPERTY_SERVICE_NAME, serviceName);
		ann.values().add(member);
		member = createMemberValuePair(ast,
				ANNOTATION_PROPERTY_ENDPOINT_INTERFACE, endpoint);
		ann.values().add(member);
		if (targetNamespace != null) {
			member = createMemberValuePair(ast, ANNOTATION_PROPERTY_TNS,
					targetNamespace);
			ann.values().add(member);
		}
		return ann;
	}

	private MemberValuePair createMemberValuePair(AST ast, String propertyName,
			String Value) {
		MemberValuePair member = ast.newMemberValuePair();
		member.setName(ast.newSimpleName(propertyName));
		StringLiteral value = ast.newStringLiteral();
		value.setLiteralValue(Value);
		member.setValue(value);

		return member;
	}

	@SuppressWarnings("unchecked")
	protected MethodDeclaration createMethodForImplClass(AST ast,
			MethodDeclaration inMethod) {

		MethodDeclaration md = ast.newMethodDeclaration();
		md.setConstructor(false);
		@SuppressWarnings("rawtypes")
		List modifiers = md.modifiers();
		modifiers.add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
		md.setName(ast
				.newSimpleName(inMethod.getName().getFullyQualifiedName()));

		Type sType = copyTypeFromOtherASTNode(ast, inMethod.getReturnType2());
		md.setReturnType2(sType);
		
		@SuppressWarnings("rawtypes")
		List thrownExceptions = inMethod.thrownExceptions();
		for (Object obj : thrownExceptions) {
			if (obj instanceof SimpleName) {
				SimpleName sname = (SimpleName) obj;
				Name newName = ast.newName(sname.getFullyQualifiedName());
				md.thrownExceptions().add(newName);
			}
		}
		
		@SuppressWarnings("rawtypes")
		List parameters = inMethod.parameters();

		for (Object obj : parameters) {
			SingleVariableDeclaration implSvd = ast
					.newSingleVariableDeclaration();
			SingleVariableDeclaration svd = (SingleVariableDeclaration) obj;
			implSvd.setName(ast.newSimpleName(svd.getName()
					.getFullyQualifiedName()));
			implSvd.setType(copyTypeFromOtherASTNode(ast, svd.getType()));
			md.parameters().add(implSvd);
		}
		
		// create method body
		Block block = ast.newBlock();
		// add log info statement
		// block.statements().add(createLoggerInvokeStatement(ast,
		// md.getName().getFullyQualifiedName()));

		Type returnType = inMethod.getReturnType2();
		ReturnStatement rs = ast.newReturnStatement();

		if (returnType.isPrimitiveType()) {
			if (((PrimitiveType) returnType).getPrimitiveTypeCode().equals(
					PrimitiveType.BOOLEAN)) {
				BooleanLiteral bl = ast.newBooleanLiteral(false);
				rs.setExpression(bl);
			} else if (!((PrimitiveType) returnType).getPrimitiveTypeCode()
					.equals(PrimitiveType.VOID)) {
				NumberLiteral nl = ast.newNumberLiteral();
				nl.setToken("0"); //$NON-NLS-1$
				rs.setExpression(nl);
			}

		} else if (returnType.isSimpleType()) {
			String typeName = ((SimpleType) returnType).getName()
					.getFullyQualifiedName();
			if ("String".equals(typeName)) { //$NON-NLS-1$

				StringLiteral sl = ast.newStringLiteral();
				sl.setLiteralValue(""); //$NON-NLS-1$
				rs.setExpression(sl);
			} else {
				rs.setExpression(ast.newNullLiteral());
			}

		} else {
			rs.setExpression(ast.newNullLiteral());
		}

		block.statements().add(rs);

		md.setBody(block);

		return md;
	}

	@SuppressWarnings("unchecked")
	private Type copyTypeFromOtherASTNode(AST ast, Type type) {
		if (type instanceof PrimitiveType) {
			return ast.newPrimitiveType(((PrimitiveType) type)
					.getPrimitiveTypeCode());
		} else if (type instanceof SimpleType) {
			SimpleType simpleType = (SimpleType) type;

			return ast.newSimpleType(ast.newName(simpleType.getName()
					.getFullyQualifiedName()));
		} else if (type instanceof ArrayType) {
			ArrayType atype = (ArrayType) type;
			return ast.newArrayType(copyTypeFromOtherASTNode(ast,
					atype.getComponentType()));
		} else if (type instanceof ParameterizedType) {
			ParameterizedType ptype = (ParameterizedType) type;
			ParameterizedType newParaType = ast
					.newParameterizedType(copyTypeFromOtherASTNode(ast,
							ptype.getType()));
			for (Object arg : ptype.typeArguments()) {
				if (arg instanceof Type) {
					Type newArg = copyTypeFromOtherASTNode(ast, (Type) arg);
					newParaType.typeArguments().add(newArg);
				}
			}
			return newParaType;
		} else if (type instanceof WildcardType) {
			WildcardType sourcetype = (WildcardType) type;
			WildcardType wtype = ast.newWildcardType();
			wtype.setBound(sourcetype.getBound());
			return wtype;
		}

		return null;
	}
	

	private IPackageFragment createImplPackage(String implPackage) throws JavaModelException {
		IPackageFragmentRoot root = JBossWSCreationUtils.getPackageFragmentRoot(project, model.getJavaSourceFolder());
		return root.createPackageFragment(implPackage,false, null);
	}

	private boolean isOverwriteClass() throws JavaModelException {
		boolean b = MessageDialog
				.openConfirm(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
						JBossJAXWSUIMessages.Confirm_Override_ImplClass,
						JBossJAXWSUIMessages.Error_JBossWS_GenerateWizard_WSImpl_Overwrite);
		return b;
	}

	private boolean findImplClass(String claName) throws JavaModelException {
		boolean b = false;
		IPackageFragmentRoot root = JBossWSCreationUtils.getPackageFragmentRoot(project, model.getJavaSourceFolder());
		String implPackageName = getImplPackageName();
		IPackageFragment pack = root.getPackageFragment(implPackageName);
		if (pack.getCompilationUnit(claName + ".java").exists()) { //$NON-NLS-1$
			b = true;
		}
		return b;
	}

	protected List<ImportDeclaration> getImportsWithoutJaxwsAnnotation(
			CompilationUnit cu) {
		List<ImportDeclaration> importList = new ArrayList<ImportDeclaration>();
		@SuppressWarnings("rawtypes")
		List imports = cu.imports();
		for (Object obj : imports) {
			ImportDeclaration id = (ImportDeclaration) obj;
			String imClsName = id.getName().getFullyQualifiedName();
			if (!imClsName.startsWith(PREFIX_JAXWS_ANNOTATION_CLASS)) {
				importList.add(id);
			}
		}

		return importList;
	}

}
