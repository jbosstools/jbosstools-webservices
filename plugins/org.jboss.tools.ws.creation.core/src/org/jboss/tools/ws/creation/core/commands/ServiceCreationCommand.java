package org.jboss.tools.ws.creation.core.commands;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
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

public class ServiceCreationCommand extends AbstractDataModelOperation {

	private ServiceModel model;
	private IResource resource;
	
	public static final String LINE_SEPARATOR = System
			.getProperty("line.separator"); //$NON-NLS-1$

	public ServiceCreationCommand(ServiceModel model) {
		this.model = model;
	}
	
	public IResource getResource() {
		return this.resource;
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

		// find the class, make sure it's in the project and open if we find it
		if (model.getServiceClasses() != null && model.getServiceClasses().size() == 1) {
			String clazzName = model.getServiceClasses().get(0);
			IResource test = findClass(project, clazzName);
			if (test != null) {
				this.resource = test;
			} else {
				ICompilationUnit createdClass =
					createJavaClass(model.getCustomPackage(), JBossWSCreationUtils
						.classNameFromQualifiedName(model.getServiceClasses().get(0)),
						project);
				if (createdClass != null) {
					this.resource = createdClass.getResource();
				}
			}
		}
		
		return null;
	}

	public IResource findClass(IJavaProject project, String className) {
		try {
			IType type = project.findType(className);
			if (type != null) {
				if (type.getResource() != null)
					return type.getResource();
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		return null;
	}

	private ICompilationUnit createJavaClass(String packageName,
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
			clsContent.append("@WebService()").append(LINE_SEPARATOR); //$NON-NLS-1$
			clsContent.append("public class ").append(className).append(" {" + LINE_SEPARATOR); //$NON-NLS-1$ //$NON-NLS-2$
			clsContent.append("}").append(LINE_SEPARATOR); //$NON-NLS-1$
			wrapperCls.createType(clsContent.toString(), null, true, null);
			
			wrapperCls.createImport("javax.jws.WebMethod", null,null); //$NON-NLS-1$
			wrapperCls.createImport("javax.jws.WebService", null,null); //$NON-NLS-1$
			
			IType serviceClsType = wrapperCls.findPrimaryType();
			clsContent = new StringBuffer();
			clsContent.append("@WebMethod()"); //$NON-NLS-1$
			clsContent.append(LINE_SEPARATOR);
			clsContent.append("public String sayHello(String name) {"); //$NON-NLS-1$
			clsContent.append(LINE_SEPARATOR);
			clsContent.append("    System.out.println(\"Hello: \" + name);"); //$NON-NLS-1$
			clsContent.append(LINE_SEPARATOR);
			clsContent.append("    return \"Hello \" + name + \"!\";"); //$NON-NLS-1$
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
