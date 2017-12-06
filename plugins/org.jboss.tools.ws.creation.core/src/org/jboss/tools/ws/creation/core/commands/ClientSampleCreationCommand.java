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

package org.jboss.tools.ws.creation.core.commands;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.PrimitiveType.Code;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.wst.common.frameworks.datamodel.AbstractDataModelOperation;
import org.jboss.tools.ws.core.utils.StatusUtils;
import org.jboss.tools.ws.creation.core.JBossWSCreationCorePlugin;
import org.jboss.tools.ws.creation.core.data.ServiceModel;
import org.jboss.tools.ws.creation.core.messages.JBossWSCreationCoreMessages;
import org.jboss.tools.ws.creation.core.utils.JBossWSCreationUtils;

/**
 * @author Grid Qian
 * 
 *         create a sample class to call web service according to wsdl
 */
public class ClientSampleCreationCommand extends AbstractDataModelOperation {

	public static final String LINE_SEPARATOR = System
			.getProperty("line.separator"); //$NON-NLS-1$
	private static final String PACAKAGE = ".*"; //$NON-NLS-1$

	private ServiceModel model;
	private int serviceNum = 1;
	private int portNum = 1;
	private int argsNum = 0;

	public ClientSampleCreationCommand(ServiceModel model) {
		this.model = model;
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {
		argsNum = 0;
		IStatus status = Status.OK_STATUS;
		IJavaProject project = model.getJavaProject();
		
		// find web service client classes
		List<ICompilationUnit> clientUnits = JBossWSCreationUtils
				.findJavaUnitsByAnnotation(
						project,
						JBossWSCreationCoreMessages.WebserviceClient_Annotation,
						model.getCustomPackage());
		
		// find web service classes
		List<ICompilationUnit> serviceUnits = JBossWSCreationUtils
				.findJavaUnitsByAnnotation(
						project,
						JBossWSCreationCoreMessages.Webservice_Annotation,
						model.getCustomPackage());
		
		if (clientUnits.size() == 0) {
			return status;
		}
		List<String> packageList = new LinkedList<String>();
		for (ICompilationUnit unit : clientUnits) {
			if (!packageList.contains(unit.getParent().getElementName())) {
				packageList.add(unit.getParent().getElementName());
			}
		}
		for (int j = 0; j < packageList.size(); j++) {
			status = createImplClass(packageList.get(j), project, clientUnits,
					serviceUnits);
			if (!status.isOK()) {
				break;
			}
		}

		return status;
	}

	private IStatus createImplClass(String packageName, IJavaProject project,
			List<ICompilationUnit> clientUnits,
			List<ICompilationUnit> serviceUnits) {
		// create a client sample class
		ICompilationUnit clientCls = createJavaClass(packageName
				+ JBossWSCreationCoreMessages.Client_Sample_Package_Name,
				JBossWSCreationCoreMessages.Client_Sample_Class_Name, false,
				null, project);
		if (clientCls == null) {
			return StatusUtils
					.errorStatus(JBossWSCreationCoreMessages.Error_Create_Client_Sample);
		}

		// add imports to client sample class
		try {
			clientCls.createImport(packageName + PACAKAGE, null, null);
			clientCls.save(null, true);
		} catch (Exception e1) {
			JBossWSCreationCorePlugin.getDefault().logError(e1);
			return StatusUtils
					.errorStatus(JBossWSCreationCoreMessages.Error_Create_Client_Sample);
		}

		// create main method
		IType clientClsType = clientCls.findPrimaryType();

		StringBuffer sb = new StringBuffer();
		sb.append("public static void main(String[] args) {"); //$NON-NLS-1$
		sb.append(LINE_SEPARATOR);
		sb.append("        System.out.println(\"***********************\");"); //$NON-NLS-1$
		sb.append(LINE_SEPARATOR);
		createWebServiceClient(clientUnits, serviceUnits, sb, packageName);
		sb.append("        System.out.println(\"***********************\");"); //$NON-NLS-1$
		sb.append(LINE_SEPARATOR);
		sb.append("        System.out.println(\"").append( //$NON-NLS-1$
				JBossWSCreationCoreMessages.Client_Sample_Run_Over)
				.append("\");"); //$NON-NLS-1$
		sb.append(LINE_SEPARATOR);
		sb.append("}"); //$NON-NLS-1$
		try {
			clientClsType.createMethod(sb.toString(), null, true, null);
			clientCls.save(null, true);
		} catch (JavaModelException e) {
			JBossWSCreationCorePlugin.getDefault().logError(e);
			return StatusUtils
					.errorStatus(JBossWSCreationCoreMessages.Error_Create_Client_Sample);
		}
		return Status.OK_STATUS;
	}

	/**
	 * create a java class
	 * 
	 * @param packageName
	 * @param className
	 * @param isInterface
	 * @param interfaceName
	 * @param javaProject
	 * @return
	 */
	public ICompilationUnit createJavaClass(String packageName,
			String className, boolean isInterface, String interfaceName,
			IJavaProject javaProject) {
		try {
			IPackageFragmentRoot root = JBossWSCreationUtils.getPackageFragmentRoot(javaProject, model.getJavaSourceFolder());
			if (packageName == null) {
				packageName = ""; //$NON-NLS-1$
			}
			IPackageFragment pkg = root.createPackageFragment(packageName,
					false, null);
			ICompilationUnit wrapperCls = pkg.createCompilationUnit(className
					+ ".java", "", true, null); //$NON-NLS-1$//$NON-NLS-2$
			if (!packageName.equals("")) { //$NON-NLS-1$
				wrapperCls.createPackageDeclaration(packageName, null);
			}

			String clsContent = ""; //$NON-NLS-1$
			if (isInterface) {
				clsContent = "public interface " + className + " {" //$NON-NLS-1$ //$NON-NLS-2$
						+ LINE_SEPARATOR;
				clsContent += "}" + LINE_SEPARATOR; //$NON-NLS-1$
			} else {
				clsContent = "public class " + className; //$NON-NLS-1$
				if (interfaceName != null) {
					clsContent += " implements " + interfaceName; //$NON-NLS-1$
				}
				clsContent += " {" + LINE_SEPARATOR; //$NON-NLS-1$
				clsContent += "}" + LINE_SEPARATOR; //$NON-NLS-1$
			}
			wrapperCls.createType(clsContent, null, true, null);

			wrapperCls.save(null, true);
			return wrapperCls;
		} catch (Exception e) {
			JBossWSCreationCorePlugin.getDefault().logError(e);
			return null;
		}
	}

	/**
	 * create a code block used to new a web service client
	 * 
	 * @param clientUnits
	 * @param serviceUnits
	 * @param sb
	 */
	private void createWebServiceClient(List<ICompilationUnit> clientUnits,
			List<ICompilationUnit> serviceUnits, StringBuffer sb,
			String packageName) {
		sb.append("        System.out.println(\"" //$NON-NLS-1$
				+ "Create Web Service Client...\");"); //$NON-NLS-1$
		sb.append(LINE_SEPARATOR);
		for (ICompilationUnit unit : clientUnits) {
			// parse the unit
			if (!packageName.equals(unit.getParent().getElementName())) {
				continue;
			}
			ASTParser parser = ASTParser.newParser(AST.JLS3);
			parser.setSource(unit);
			parser.setResolveBindings(false);
			parser.setFocalPosition(0);
			CompilationUnit result = (CompilationUnit) parser.createAST(null);
			@SuppressWarnings("rawtypes")
			List types = result.types();
			TypeDeclaration typeDec = (TypeDeclaration) types.get(0);
			sb.append("        " + typeDec.getName()); //$NON-NLS-1$
			sb.append(" service").append(serviceNum).append(" = new "); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(typeDec.getName());
			sb.append("();"); //$NON-NLS-1$
			sb.append(LINE_SEPARATOR);

			MethodDeclaration methodDec[] = typeDec.getMethods();

			// create web service from web serivce client methods
			for (MethodDeclaration method : methodDec) {
				if (method.modifiers().get(0) instanceof NormalAnnotation) {
					NormalAnnotation anno = (NormalAnnotation) method
							.modifiers().get(0);
					if (anno.getTypeName().getFullyQualifiedName()
							.equals(JBossWSCreationCoreMessages.WebEndpoint)) {
						createWebService(serviceUnits, method, sb);
						portNum += 1;
					}
				}
			}
			serviceNum += 1;
		}
	}

	/**
	 * create a code block used to new a web service from a method of web
	 * service client
	 * 
	 * @param serviceUnits
	 * @param method
	 * @param sb
	 */
	private void createWebService(List<ICompilationUnit> serviceUnits,
			MethodDeclaration method, StringBuffer sb) {
		sb.append("        System.out.println(\"" //$NON-NLS-1$
				+ "Create Web Service...\");"); //$NON-NLS-1$
		sb.append(LINE_SEPARATOR);
		sb.append("        " + method.getReturnType2().toString()); //$NON-NLS-1$
		sb.append(" port").append(portNum).append(" = "); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("service").append(serviceNum).append("."); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(method.getName()).append("();"); //$NON-NLS-1$
		sb.append(LINE_SEPARATOR);

		for (ICompilationUnit unit : serviceUnits) {
			// parse the unit
			ASTParser parser = ASTParser.newParser(AST.JLS3);
			parser.setSource(unit);
			parser.setResolveBindings(false);
			parser.setFocalPosition(0);
			CompilationUnit result = (CompilationUnit) parser.createAST(null);
			@SuppressWarnings("rawtypes")
			List types = result.types();
			TypeDeclaration typeDec1 = (TypeDeclaration) types.get(0);
			if (typeDec1.getName().toString()
					.equals(method.getReturnType2().toString())) {
				callWebServiceOperation(typeDec1, sb);
			}
		}
	}

	/**
	 * create a code block to call web service operation
	 * 
	 * @param typeDec
	 * @param sb
	 */
	private void callWebServiceOperation(TypeDeclaration typeDec,
			StringBuffer sb) {
		sb.append("        System.out.println(\"" //$NON-NLS-1$
				+ "Call Web Service Operation...\");"); //$NON-NLS-1$
		sb.append(LINE_SEPARATOR);

		MethodDeclaration methodDec[] = typeDec.getMethods();

		// call web serivce Operation
		for (MethodDeclaration method : methodDec) {
			if (method.getReturnType2().toString().equals("void")) { //$NON-NLS-1$
				sb.append("        System.out.println(\"Server said: "); //$NON-NLS-1$
				sb.append("port").append(portNum).append("."); //$NON-NLS-1$//$NON-NLS-2$
				sb.append(method.getName()).append("() is a void method!\");"); //$NON-NLS-1$
				sb.append(LINE_SEPARATOR);
			} else {
				sb.append("        System.out.println(\"Server said: \" + "); //$NON-NLS-1$
				sb.append("port").append(portNum).append("."); //$NON-NLS-1$ //$NON-NLS-2$
				sb.append(method.getName()).append("("); //$NON-NLS-1$

				boolean noNull = true;
				for (int j = 0; j < method.parameters().size(); j++) {
					noNull = createWebServiceOperationParameters(
							method.parameters(), sb, j)
							&& noNull;
				}
				sb.append("));"); //$NON-NLS-1$
				sb.append(LINE_SEPARATOR);
				if (!noNull) {
					sb.append("        //Please input the parameters instead of 'null' for the upper method!"); //$NON-NLS-1$
					sb.append(LINE_SEPARATOR);
					sb.append(LINE_SEPARATOR);
				}
			}
		}
	}

	/**
	 * create a code block used to create the parameters of a web service
	 * operation
	 * 
	 * @param list
	 * @param sb
	 * @param j
	 */
	@SuppressWarnings("static-access")
	private boolean createWebServiceOperationParameters(
			@SuppressWarnings("rawtypes") List list, StringBuffer sb, int j) {
		SingleVariableDeclaration para = (SingleVariableDeclaration) list
				.get(j);
		if (para.getType().isPrimitiveType()) {
			PrimitiveType type = (PrimitiveType) para.getType();
			Code code = type.getPrimitiveTypeCode();
			if (type.INT.equals(code)) {
				sb.append("Integer.parseInt(args["); //$NON-NLS-1$
			} else if (type.BOOLEAN.equals(code)) {
				sb.append("Boolean.parseBoolean(args["); //$NON-NLS-1$
			} else if (type.BYTE.equals(code)) {
				sb.append("Byte.parseByte(args["); //$NON-NLS-1$
			} else if (type.SHORT.equals(code)) {
				sb.append("Short.parseShort((args["); //$NON-NLS-1$
			} else if (type.LONG.equals(code)) {
				sb.append("Long.parseLong(args["); //$NON-NLS-1$
			} else if (type.FLOAT.equals(code)) {
				sb.append("Float.parseFloat(args["); //$NON-NLS-1$
			} else if (type.DOUBLE.equals(code)) {
				sb.append("Double.parseDouble(args["); //$NON-NLS-1$
			} else {
				if (type.CHAR.equals(code)) {
					sb.append("args[").append(argsNum).append("]"); //$NON-NLS-1$ //$NON-NLS-2$
					countArgs(j, sb, list);
					argsNum += 1;
				}
				return true;
			}
			sb.append(argsNum).append("])"); //$NON-NLS-1$
			countArgs(j, sb, list);
			argsNum += 1;
			return true;
		} else if ("String".equals(para.getType().toString())) { //$NON-NLS-1$
			sb.append("args[").append(argsNum).append("]"); //$NON-NLS-1$ //$NON-NLS-2$
			countArgs(j, sb, list);
			argsNum += 1;
			return true;
		}
		if (list.get(j) instanceof Object) {
			sb.append("null"); //$NON-NLS-1$
			countArgs(j, sb, list);
			return false;
		}
		return true;
	}

	private void countArgs(int j, StringBuffer sb,
			@SuppressWarnings("rawtypes") List list) {
		if (j != list.size() - 1) {
			sb.append(","); //$NON-NLS-1$
		}
	}

}
