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
package org.jboss.tools.ws.creation.core.commands;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.wst.common.frameworks.datamodel.AbstractDataModelOperation;
import org.jboss.tools.ws.core.utils.StatusUtils;
import org.jboss.tools.ws.creation.core.JBossWSCreationCorePlugin;
import org.jboss.tools.ws.creation.core.data.ServiceModel;
import org.jboss.tools.ws.creation.core.messages.JBossWSCreationCoreMessages;
import org.jboss.tools.ws.creation.core.utils.JBossWSCreationUtils;

/**
 * @author Brian Fitzpatrick
 *
 */
public class RSServiceSampleCreationCommand extends AbstractDataModelOperation {

	private ServiceModel model;
	public static final String LINE_SEPARATOR = System
			.getProperty("line.separator"); //$NON-NLS-1$

	public RSServiceSampleCreationCommand(ServiceModel model) {
		this.model = model;
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {
		IJavaProject project = null;
		try {
			project = JBossWSCreationUtils.getJavaProjectByName(model
					.getWebProjectName());
		} catch (JavaModelException e) {
			JBossWSCreationCorePlugin.getDefault().logError(e);
			return StatusUtils
					.errorStatus(JBossWSCreationCoreMessages.Error_Create_Client_Sample);
		}

		createRESTAnnotatedJavaClass (model.getCustomPackage(), JBossWSCreationUtils
				.classNameFromQualifiedName(model.getServiceClasses().get(0)),
				project);
		createRESTApplicationClass (model.getCustomPackage(), model.getApplicationClassName(),
				project);
		
		return null;
	}

	private ICompilationUnit createRESTApplicationClass(String packageName,
			String className, IJavaProject project) {
		try {
			IPath srcPath = new Path(JBossWSCreationUtils
					.getJavaProjectSrcLocation(project.getProject()));
			srcPath = project.getPath().append(
					srcPath.makeRelativeTo(project.getProject()
							.getLocation()));
			IPackageFragmentRoot root = project
					.findPackageFragmentRoot(srcPath);
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

			StringBuffer clsContent = new StringBuffer(); 
			clsContent.append("public class ").append(className).append(" extends Application").append(" {" + LINE_SEPARATOR); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			clsContent.append("}").append(LINE_SEPARATOR); //$NON-NLS-1$
			wrapperCls.createType(clsContent.toString(), null, true, null);
			
			wrapperCls.createImport("java.util.Set", null,null); //$NON-NLS-1$
			wrapperCls.createImport("java.util.HashSet", null,null); //$NON-NLS-1$
			wrapperCls.createImport("javax.ws.rs.core.Application", null,null); //$NON-NLS-1$
			
			IType serviceClsType = wrapperCls.findPrimaryType();
			clsContent = new StringBuffer();
			clsContent.append("private Set<Object> singletons = new HashSet<Object>();" + LINE_SEPARATOR); //$NON-NLS-1$
			serviceClsType.createField(clsContent.toString(), null, false, null);
			
			clsContent = new StringBuffer();
			clsContent.append("private Set<Class<?>> empty = new HashSet<Class<?>>();" + LINE_SEPARATOR); //$NON-NLS-1$
			serviceClsType.createField(clsContent.toString(), null, false, null);

			clsContent = new StringBuffer();
			clsContent.append(LINE_SEPARATOR);
			clsContent.append("public " + className + "(){" + LINE_SEPARATOR); //$NON-NLS-1$ //$NON-NLS-2$
			clsContent.append("     singletons.add(new " + JBossWSCreationUtils //$NON-NLS-1$
					.classNameFromQualifiedName(model.getServiceClasses().get(0)) + "());" + LINE_SEPARATOR); //$NON-NLS-1$
			clsContent.append("}" + LINE_SEPARATOR); //$NON-NLS-1$
			serviceClsType.createMethod(clsContent.toString(), null, true, null);

			clsContent = new StringBuffer();
			clsContent.append(LINE_SEPARATOR);
			clsContent.append("@Override" + LINE_SEPARATOR); //$NON-NLS-1$
			clsContent.append("public Set<Class<?>> getClasses() {" + LINE_SEPARATOR); //$NON-NLS-1$
			clsContent.append("     return empty;" + LINE_SEPARATOR); //$NON-NLS-1$
			clsContent.append("}" + LINE_SEPARATOR); //$NON-NLS-1$
			serviceClsType.createMethod(clsContent.toString(), null, true, null);

			clsContent = new StringBuffer();
			clsContent.append(LINE_SEPARATOR);
			clsContent.append("@Override" + LINE_SEPARATOR); //$NON-NLS-1$
			clsContent.append("public Set<Object> getSingletons() {" + LINE_SEPARATOR); //$NON-NLS-1$
			clsContent.append("     return singletons;" + LINE_SEPARATOR); //$NON-NLS-1$
			clsContent.append("}" + LINE_SEPARATOR); //$NON-NLS-1$
			serviceClsType.createMethod(clsContent.toString(), null, true, null);

			wrapperCls.save(null, true);
			return wrapperCls;
		} catch (Exception e) {
			JBossWSCreationCorePlugin.getDefault().logError(e);
			return null;
		}
	}

	private ICompilationUnit createRESTAnnotatedJavaClass(String packageName,
			String className, IJavaProject project) {
		try {
			IPath srcPath = new Path(JBossWSCreationUtils
					.getJavaProjectSrcLocation(project.getProject()));
			srcPath = project.getPath().append(
					srcPath.makeRelativeTo(project.getProject()
							.getLocation()));
			IPackageFragmentRoot root = project
					.findPackageFragmentRoot(srcPath);
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

			StringBuffer clsContent = new StringBuffer(); 
			clsContent.append("@Path(\"/" + model.getServiceName() + "\")").append(LINE_SEPARATOR); //$NON-NLS-1$ //$NON-NLS-2$
			clsContent.append("public class ").append(className).append(" {" + LINE_SEPARATOR); //$NON-NLS-1$ //$NON-NLS-2$
			clsContent.append("}").append(LINE_SEPARATOR); //$NON-NLS-1$
			wrapperCls.createType(clsContent.toString(), null, true, null);
			
			wrapperCls.createImport("javax.ws.rs.Produces", null,null); //$NON-NLS-1$
			wrapperCls.createImport("javax.ws.rs.GET", null,null); //$NON-NLS-1$
			wrapperCls.createImport("javax.ws.rs.Path", null,null); //$NON-NLS-1$
			
			IType serviceClsType = wrapperCls.findPrimaryType();
			clsContent = new StringBuffer();
			clsContent.append("@GET()"); //$NON-NLS-1$
			clsContent.append(LINE_SEPARATOR);
			clsContent.append("@Produces(\"text/plain\")"); //$NON-NLS-1$
			clsContent.append(LINE_SEPARATOR);
			clsContent.append("public String sayHello() {"); //$NON-NLS-1$
			clsContent.append(LINE_SEPARATOR);
			clsContent.append("    return \"Hello World!\";"); //$NON-NLS-1$
			clsContent.append(LINE_SEPARATOR);
			clsContent.append("}"); //$NON-NLS-1$
			serviceClsType.createMethod(clsContent.toString(), null, true, null);
			wrapperCls.save(null, true);
			return wrapperCls;
		} catch (Exception e) {
			JBossWSCreationCorePlugin.getDefault().logError(e);
			return null;
		}
	}

}
