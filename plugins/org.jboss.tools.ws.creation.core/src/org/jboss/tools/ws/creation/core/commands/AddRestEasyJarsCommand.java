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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.channels.FileChannel;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
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
 * add jars necessary for RestEasy web services
 */
public class AddRestEasyJarsCommand extends AbstractDataModelOperation{

	private static final String SCANNOTATION_JAR = "scannotation.jar"; //$NON-NLS-1$
	private static final String REST_EASY = "RestEasy"; //$NON-NLS-1$
	private static final String JAXRS_API_JAR = "jaxrs-api.jar"; //$NON-NLS-1$
	private static final String LIB = "lib"; //$NON-NLS-1$
	private static final String RESTEASY_JAXRS_JAR = "resteasy-jaxrs.jar"; //$NON-NLS-1$
	private ServiceModel model;
	private static String WEB_XML = "web.xml"; //$NON-NLS-1$
	
	public AddRestEasyJarsCommand(ServiceModel model) {
		this.model = model;
	}
	
	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {		
		return executeOverride(monitor);
	}
	
	public IStatus executeOverride(IProgressMonitor monitor) {
		IStatus status = Status.OK_STATUS;
		IJavaProject project = null;
		try {
			project = JBossWSCreationUtils.getJavaProjectByName(model
					.getWebProjectName());
		} catch (JavaModelException e) {
			JBossWSCreationCorePlugin.getDefault().logError(e);
			return StatusUtils.errorStatus(JBossWSCreationCoreMessages.Error_Create_Client_Sample);
		}
		status = addJars(project);
		return status;
	}
	
	private File findLibDir ( File in ) {
		File[] children = 
			in.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				if (dir.isDirectory() && name.equals(LIB)) {
					return true;
				}
				return false;
			}
		});
		if (children != null ) {
			for (int i = 0; i < children.length; i++) {
				File libDir = (File) children[i];
				if (libDir.exists() && libDir.isDirectory()) {
					File[] jars = libDir.listFiles(new FilenameFilter() {
						public boolean accept(File dir, String name) {
							if (name.equalsIgnoreCase(JAXRS_API_JAR)) {
								return true;
							}
							return false;
						}
					});
					if (jars != null && jars.length > 0) {
						return libDir;
					}
				}
			}
		}
		return null;
	}

	private IStatus addJars (IJavaProject project ) {
		IStatus status = Status.OK_STATUS;
		
		try {
			String path =
				JBossWSCreationUtils.getJBossWSRuntimeLocation(project.getProject());
			File runtime = new File(path);
			if (runtime.exists()) {
				File findJar = findLibDir(runtime);
				if (findJar == null) {
					File parent = runtime.getParentFile();
					if (parent.exists() && parent.isDirectory()) {
						File[] restEasyDir = parent.listFiles(new FilenameFilter() {
							public boolean accept(File dir, String name) {
								if (name.equalsIgnoreCase(REST_EASY)) {
									return true;
								}
								return false;
							}
						});
						if (restEasyDir != null && restEasyDir.length > 0) {
							findJar = findLibDir(restEasyDir[0]);
						}
					}
				}
				if (findJar == null) { 
					// if it's still null, resteasy's not installed??
					status = StatusUtils.errorStatus(JBossWSCreationCoreMessages.AddRestEasyJarsCommand_RestEasy_JARS_Not_Found);
				} else {
					File[] jaxrsJar = findJar.listFiles(new FilenameFilter() {
						public boolean accept(File dir, String name) {
							if (name.equalsIgnoreCase(JAXRS_API_JAR)) {
								return true;
							}
							return false;
						}
					}); 
					File[] resteasyJar = findJar.listFiles(new FilenameFilter() {
						public boolean accept(File dir, String name) {
							if (name.equalsIgnoreCase(RESTEASY_JAXRS_JAR)) { 
								return true;
							}
							return false;
						}
					});
					File[] scannotationsJar = findJar.listFiles(new FilenameFilter() {
						public boolean accept(File dir, String name) {
							if (name.equalsIgnoreCase(SCANNOTATION_JAR)) {
								return true;
							}
							return false;
						}
					}); 
					addJarToClassPath(project, jaxrsJar[0]);
					addJarToClassPath(project, resteasyJar[0]);
					addJarToClassPath(project, scannotationsJar[0]);
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		
		return status;
	}
	
	private IStatus addJarToClassPath ( IJavaProject project, File jar) {
		
		IStatus status = Status.OK_STATUS;
		try {
			copyFileToLibsDir(project.getProject(), jar);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return status;
	}
	
	private void copyFileToLibsDir ( IProject pro, File jar ) throws IOException {
		File file = JBossWSCreationUtils.findFileByPath(WEB_XML, pro.getLocation().toOSString());
        if(file != null){
        	String libdir = file.getParentFile().getPath() + File.separatorChar + LIB;
        	File libdirFile = new File(libdir);
        	if (!libdirFile.exists()) {
        		try {
					libdirFile.createNewFile();
				} catch (IOException e) {
					throw e;
				}
        	}
        	try {
        	    
                // Create channel on the source
                FileChannel srcChannel = 
                  new FileInputStream(jar).getChannel();
            
                // Create channel on the destination
                FileChannel dstChannel = 
                  new FileOutputStream(libdirFile.getAbsolutePath() + File.separatorChar + jar.getName()).getChannel();
            
                // Copy file contents from source to destination
                dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
            
                // Close the channels
                srcChannel.close();
                dstChannel.close();
                
            } catch (IOException e) {
            	throw e;
            }        
        }
	}
	
}
