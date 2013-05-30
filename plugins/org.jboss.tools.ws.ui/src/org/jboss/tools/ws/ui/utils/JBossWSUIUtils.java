/******************************************************************************* 
 * Copyright (c) 2008 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/

package org.jboss.tools.ws.ui.utils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.jboss.tools.ws.core.utils.StatusUtils;
import org.jboss.tools.ws.creation.core.utils.JBossWSCreationUtils;
import org.jboss.tools.ws.ui.JBossWSUIPlugin;
import org.jboss.tools.ws.ui.messages.JBossWSUIMessages;

/**
 * @author Grid Qian
 */
public class JBossWSUIUtils {
	private static String JAVA = ".java"; //$NON-NLS-1$
	private static String CLASS = ".class"; //$NON-NLS-1$
	private static final String WS_NATIVE_JAR = "jbossws-native-client.jar"; //$NON-NLS-1$
	private static final String WS_CXF_JAR = "jbossws-cxf-client.jar"; //$NON-NLS-1$
	private static final String WS_METRO_JAR = "jbossws-metro-client.jar"; //$NON-NLS-1$
	private static final String CLIENT_FOLDER = "client"; //$NON-NLS-1$
	private static final String CLIENT_AS7_FOLDER = "modules:org:jboss:ws:jaxws-client:main"; //$NON-NLS-1$
	private static final String CLIENT_AS71_FOLDER = "modules:system:layers:base:org:jboss:ws:jaxws-client:main"; //$NON-NLS-1$
	private static final String WS_IMPL = "Implementation-Title"; //$NON-NLS-1$
	private static final String WS_VERSION = "Implementation-Version"; //$NON-NLS-1$
	private static final String WS_JAR = "jbossws-client.jar"; //$NON-NLS-1$

	public static String addAnotherNodeToPath(String currentPath, String newNode) {
		return currentPath + File.separator + newNode;
	}

	public static String addNodesToPath(String currentPath, String[] newNode) {
		String returnPath = currentPath;
		for (int i = 0; i < newNode.length; i++) {
			returnPath = returnPath + File.separator + newNode[i];
		}
		return returnPath;
	}
	
	public static IStatus validateClassName(String name, IJavaElement context) {
		IStatus status = null;
		String[] sourceComplianceLevels = getSourceComplianceLevels(context);
		status = JavaConventions.validateClassFileName(name + CLASS, sourceComplianceLevels[0], sourceComplianceLevels[1]);
		if (status != null && status.getSeverity() == IStatus.ERROR) {
			return status;
		}
		File file = JBossWSCreationUtils.findFileByPath(name + JAVA, context.getJavaProject().getProject().getLocation().toOSString());
		if (file != null && file.exists()) {
			status = StatusUtils.warningStatus(JBossWSUIMessages.Error_JBossWS_GenerateWizard_ClassName_Same);
		}
		return status;
	}

	public static IStatus validatePackageName(String name, IJavaElement context) {
		IStatus status = null;
		if (context == null || !context.exists()) {
			status = JavaConventions.validatePackageName(name,
					JavaCore.VERSION_1_3, JavaCore.VERSION_1_3);
			if (status != null && !status.isOK()) {
				return status;
			}
		}
		String[] sourceComplianceLevels = getSourceComplianceLevels(context);
		status = JavaConventions.validatePackageName(name,
				sourceComplianceLevels[0], sourceComplianceLevels[1]);
		if (status != null && status.getSeverity() == IStatus.ERROR) {
			return status;
		}

		IPackageFragmentRoot[] roots = null;
		try {
			IResource[] srcFolders = JBossWSCreationUtils.getJavaSourceRoots(context.getJavaProject());
			roots = new IPackageFragmentRoot[srcFolders.length];
			int i = 0;
			for (IResource src : srcFolders) {
				roots[i] = context.getJavaProject().getPackageFragmentRoot(src);
				i++;
			}
		} catch (JavaModelException e) {
			JBossWSUIPlugin.log(e);
		}
		for (IPackageFragmentRoot root : roots) {
			if (root != null) {
				IPackageFragment pack = root.getPackageFragment(name);
				try {
					IPath rootPath = root.getPath();
					IPath outputPath = root.getJavaProject()
							.getOutputLocation();
					if (rootPath.isPrefixOf(outputPath)
							&& !rootPath.equals(outputPath)) {
						// if the bin folder is inside of our root, don't allow
						// to name a package
						// like the bin folder
						IPath packagePath = pack.getPath();
						if (outputPath.isPrefixOf(packagePath)) {
							status = StatusUtils
									.warningStatus(JBossWSUIMessages.Error_JBossWS_GenerateWizard_IsOutputFolder);
							return status;
						}
					}
					if (pack.exists()) {
						if (pack.containsJavaResources()
								|| !pack.hasSubpackages()) {
							status = StatusUtils
									.warningStatus(JBossWSUIMessages.Error_JBossWS_GenerateWizard_PackageExists);
						} else {
							status = StatusUtils
									.warningStatus(JBossWSUIMessages.Error_JBossWS_GenerateWizard_PackageNotShown);
						}
						return status;
					} else {
						if (pack.getResource() == null) {
							continue;
						}
						URI location = pack.getResource().getLocationURI();
						if (location != null) {
							IFileStore store = EFS.getStore(location);
							if (store.fetchInfo().exists()) {
								status = StatusUtils
										.warningStatus(JBossWSUIMessages.Error_JBossWS_GenerateWizard_PackageExistsDifferentCase);
								return status;
							}
						}
					}
				} catch (CoreException e) {
					JBossWSUIPlugin.log(e);
				}
			}
		}
		return status;
	}

	public static String[] getSourceComplianceLevels(IJavaElement context) {
		if (context != null) {
			IJavaProject javaProject = context.getJavaProject();
			if (javaProject != null) {
				return new String[] {
						javaProject.getOption(JavaCore.COMPILER_SOURCE, true),
						javaProject.getOption(JavaCore.COMPILER_COMPLIANCE,
								true) };
			}
		}
		return new String[] { JavaCore.getOption(JavaCore.COMPILER_SOURCE),
				JavaCore.getOption(JavaCore.COMPILER_COMPLIANCE) };
	}
	
	
	public static String[] getWSRuntimeDetail(String serverHome) {
		String[] strs = null;
		File jbosswsHomeDir = new File(serverHome);
		
		String jarPath = JBossWSUIUtils.addNodesToPath(jbosswsHomeDir.getAbsolutePath(), new String[] {CLIENT_FOLDER, WS_CXF_JAR});
		strs = getWSRuntimeDetailFromPath(jarPath);
		if (strs != null) {
			return strs;
		}
		jarPath = JBossWSUIUtils.addNodesToPath(jbosswsHomeDir.getAbsolutePath(), new String[] {CLIENT_FOLDER, WS_NATIVE_JAR});
		strs = getWSRuntimeDetailFromPath(jarPath);
		if (strs != null) {
			return strs;
		}
		jarPath = JBossWSUIUtils.addNodesToPath(jbosswsHomeDir.getAbsolutePath(), new String[] {CLIENT_FOLDER, WS_METRO_JAR});
		strs = getWSRuntimeDetailFromPath(jarPath);
		if (strs != null) {
			return strs;
		}
		jarPath = JBossWSUIUtils.addNodesToPath(jbosswsHomeDir.getAbsolutePath(), new String[] {CLIENT_FOLDER, WS_JAR});
		strs = getWSRuntimeDetailFromPath(jarPath);
		if (strs != null) {
			return strs;
		}
		
        String as7 = JBossWSUIUtils.addNodesToPath(jbosswsHomeDir.getAbsolutePath(), CLIENT_AS7_FOLDER.split(":")); //$NON-NLS-1$
        File as7File = new File(as7);
        if (as7File.exists() && as7File.isDirectory()) {
            File[] files = as7File.listFiles();
            String jarName = ""; //$NON-NLS-1$
            for (int i = 0 ; i < files.length ; i++) {
            	jarName = files[i].getName();
            	if (jarName.contains(WS_CXF_JAR.substring(0, WS_CXF_JAR.length()-5)) 
            			|| jarName.contains(WS_CXF_JAR.substring(0, WS_NATIVE_JAR.length()-5))
            			|| jarName.contains(WS_CXF_JAR.substring(0, WS_METRO_JAR.length()-5))) {
            		strs = getWSRuntimeDetailFromPath(files[i].getAbsolutePath());
            		if (strs != null) {
            			return strs;
            		}
            	}
            }
        }

        String as71 = JBossWSUIUtils.addNodesToPath(jbosswsHomeDir.getAbsolutePath(), CLIENT_AS71_FOLDER.split(":")); //$NON-NLS-1$
        File as71File = new File(as71);
        if (as71File.exists() && as71File.isDirectory()) {
            File[] files71 = as71File.listFiles();
            String jarName71 = ""; //$NON-NLS-1$
            for (int i = 0 ; i < files71.length ; i++) {
                jarName71 = files71[i].getName();
                if (jarName71.contains(WS_CXF_JAR.substring(0, WS_CXF_JAR.length()-5)) 
                        || jarName71.contains(WS_CXF_JAR.substring(0, WS_NATIVE_JAR.length()-5))
                        || jarName71.contains(WS_CXF_JAR.substring(0, WS_METRO_JAR.length()-5))) {
                    strs = getWSRuntimeDetailFromPath(files71[i].getAbsolutePath());
                    if (strs != null) {
                        return strs;
                    }
                }
            }
        }
        
        return new String[] {"", ""}; //$NON-NLS-1$ //$NON-NLS-2$;			
	}
	
    public static String[] getWSRuntimeDetailFromPath(String path) {
    	File jarFile = new File(path);
		if (!jarFile.isFile()) {
			return null;
		}
		String[] strs = new String[] {"", ""}; //$NON-NLS-1$ //$NON-NLS-2$
			JarFile jar = null;
			try {
				jar = new JarFile(jarFile);
			    Attributes attributes = jar.getManifest().getMainAttributes();
			    strs[0] = attributes.getValue(WS_IMPL);
			    strs[1] = attributes.getValue(WS_VERSION);
			} catch (IOException e) {
				return strs;
			} finally {
			    if (jar != null) {
			        try {
                        jar.close();
                    } catch (IOException e) {
                        // ignore
                    }
			    }
			}
		return strs;
	}

	public static Label[] createWSRuntimeDetailsGroup(Composite root, int horizontalSpan) {
		Group wsRuntimeDetails = new Group(root, SWT.NONE);
		wsRuntimeDetails.setText(JBossWSUIMessages.Runtime_Details);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = horizontalSpan;
		wsRuntimeDetails.setLayout(new GridLayout(2, false));
		wsRuntimeDetails.setLayoutData(gd);
		
		new Label(wsRuntimeDetails, SWT.NONE).setText(JBossWSUIMessages.Runtime_Details_Impl);
		Label impl = new Label(wsRuntimeDetails, SWT.NONE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		impl.setLayoutData(gd);
		
		new Label(wsRuntimeDetails, SWT.NONE).setText(JBossWSUIMessages.Runtime_Details_Version);
		Label vDetail = new Label(wsRuntimeDetails, SWT.NONE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		vDetail.setLayoutData(gd);
		
		Label[] labels = new Label[] {impl, vDetail};
		
		return labels;
		
		
	}

}