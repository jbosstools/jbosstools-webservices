/**
 * JBoss, a Division of Red Hat
 * Copyright 2006, Red Hat Middleware, LLC, and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
* This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboos.tools.ws.creation.core.utils;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jst.ws.internal.common.J2EEUtils;

public class JBossWSCreationUtils {
	
	public static IPath getWorkspace(){
		return ResourcesPlugin.getWorkspace().getRoot().getLocation();
	}
	
	public static IProject getProjectByName(String project){
		String projectString = replaceEscapecharactors(project);
		return ResourcesPlugin.getWorkspace().getRoot().getProject(
				getProjectNameFromFramewokNameString(projectString));
	}
	
	public static IPath getProjectRoot(String project){
		String projectString = replaceEscapecharactors(project);
		return ResourcesPlugin.getWorkspace().getRoot().getProject(
				getProjectNameFromFramewokNameString(projectString)).getLocation();
	}

	public static String  pathToWebProjectContainer(String project) {
		IPath projectRoot = getProjectRoot(project);
		IPath currentDynamicWebProjectDir = J2EEUtils.getWebContentPath(
				getProjectByName(project));
		IPath currentDynamicWebProjectDirWithoutProjectRoot = J2EEUtils.getWebContentPath(
				getProjectByName(project)).removeFirstSegments(1).makeAbsolute();
		if(projectRoot.toOSString().contains(getWorkspace().toOSString())){
			return getWorkspace()
						.append(currentDynamicWebProjectDir).toOSString();
		}else{
			return projectRoot
						.append(currentDynamicWebProjectDirWithoutProjectRoot).toOSString();
		}
		
	}
	
	public static String  pathToWebProjectContainerWEBINF(String project) {
		IPath projectRoot = getProjectRoot(project);
		IPath webContainerWEBINFDir = J2EEUtils.getWebInfPath(
				getProjectByName(project));
		IPath webContainerWEBINFDirWithoutProjectRoot = J2EEUtils.getWebInfPath(
				getProjectByName(project)).removeFirstSegments(1).makeAbsolute();
		if(projectRoot.toOSString().contains(getWorkspace().toOSString())){
			return getWorkspace()
						.append(webContainerWEBINFDir).toOSString();
		}else{
			return projectRoot
						.append(webContainerWEBINFDirWithoutProjectRoot).toOSString();
		}
	}
	
	
	//Fix for the windows build not working
	private static String replaceEscapecharactors(String vulnarableString){
		if (vulnarableString.indexOf("/")!=-1){
			vulnarableString = vulnarableString.replace('/', File.separator.charAt(0));
		}
		return vulnarableString;
	}
	
	
	private static String getProjectNameFromFramewokNameString(String frameworkProjectString){
		if (frameworkProjectString.indexOf(getSplitCharactor())== -1){
			return frameworkProjectString;
		}else{
			return frameworkProjectString.split(getSplitCharactors())[1];
		}
	}
	
	
	private static String getSplitCharactor(){
		//Windows check (because from inside wtp in return I received a hard coded path)
		if (File.separatorChar == '\\'){
			return "\\" ;
		}else{
			return File.separator;
		}
	}
	
	
	private static String getSplitCharactors(){
		//Windows check (because from inside wtp in return I received a hard coded path)
		if (File.separatorChar == '\\'){
			return "\\" + File.separator;
		}else{
			return File.separator;
		}
	}
	
	
	
	
	
}
