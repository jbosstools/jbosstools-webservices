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

package org.jboss.tools.ws.core.classpath;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.jboss.tools.ws.core.JbossWSCorePlugin;
import org.jboss.tools.ws.core.facet.delegate.IJBossWSFacetDataModelProperties;
import org.jboss.tools.ws.core.messages.JbossWSCoreMessages;

/**
 * @author Grid Qian
 */
public class JbossWSRuntimeManager {

	private static JbossWSRuntimeListConverter converter = new JbossWSRuntimeListConverter();

	private Map<String, JbossWSRuntime> runtimes = new HashMap<String, JbossWSRuntime>();

	/**
	 * Private constructor
	 */
	private JbossWSRuntimeManager() {
		load();
	}

	/**
	 * This class make Java Runtime responsible for solving synchronization
	 * problems during initialization if there is any
	 * 
	 */
	static class JbossWSRuntimeManagerHolder {
		private static final JbossWSRuntimeManager INSTANCE = new JbossWSRuntimeManager();
	}

	/**
	 * Return JbossWSRuntimeManaher instance
	 * 
	 * @return JbossWSRuntimeManager instance
	 */
	public static JbossWSRuntimeManager getInstance() {
		return JbossWSRuntimeManagerHolder.INSTANCE;
	}

	/**
	 * Return Array of configured JbossWSRuntimes
	 * 
	 * @return JbossWSRuntime[]
	 */
	public JbossWSRuntime[] getRuntimes() {
		Collection<JbossWSRuntime> c = runtimes.values();
		return c.toArray(new JbossWSRuntime[runtimes.size()]);
	}

	/**
	 * Add new JbossWSRuntime
	 * 
	 * @param runtime
	 *            JbossWSRuntime
	 */
	public void addRuntime(JbossWSRuntime runtime) {
		if (runtimes.size() == 0) {
			runtime.setDefault(true);
		}

		JbossWSRuntime oldDefaultRuntime = getDefaultRuntime();
		if (oldDefaultRuntime != null && runtime.isDefault()) {
			oldDefaultRuntime.setDefault(false);
		}
		runtimes.put(runtime.getName(), runtime);
		save();
	}

	/**
	 * Add new JbossWSRuntime with given parameters
	 * 
	 * @param name
	 *            String - runtime name
	 * @param path
	 *            String - runtime home folder
	 * @param version
	 *            String - string representation of version number
	 * @param defaultRt
	 *            boolean - default flag
	 */
	public void addRuntime(String name, String path, String version,
			boolean defaultRt) {
		JbossWSRuntime jbossWSRt = new JbossWSRuntime();
		jbossWSRt.setHomeDir(path);
		jbossWSRt.setName(name);
		jbossWSRt.setVersion(version);
		jbossWSRt.setDefault(defaultRt);
		addRuntime(jbossWSRt);
	}

	/**
	 * Return JbossWSRuntime by given name
	 * 
	 * @param name
	 *            String - JbossWSRuntime name
	 * @return JbossWSRuntime - found JbossWSRuntime instance or null
	 */
	public JbossWSRuntime findRuntimeByName(String name) {
		for (JbossWSRuntime jbossWSRuntime : runtimes.values()) {
			if (jbossWSRuntime.getName().equals(name)) {
				return jbossWSRuntime;
			}
		}
		return null;
	}

	public List<String> getAllRuntimeJars(JbossWSRuntime rt){
		List<String> jarList = new ArrayList<String>();
		if (rt != null) {
			if (rt.isUserConfigClasspath()) {
				jarList.addAll(rt.getLibraries());
				 
			} else {
				IPath wsPath = new Path(rt.getHomeDir());
				if (wsPath != null) {
					IPath libPath = wsPath
							.append(JbossWSCoreMessages.Dir_Lib);
					List<File> libs = getJarsOfFolder(libPath.toFile());
					libPath = wsPath
							.append(JbossWSCoreMessages.Dir_Client);
					List<File> clientJars = getJarsOfFolder(libPath.toFile());
					
					jarList = mergeTwoList(libs, clientJars);
				}
			}
			
		}
		return jarList;
	}
	

	
	private List<File> getJarsOfFolder(File folder){
		List<File> jars = new ArrayList<File>();
		if(folder.isDirectory()){
			for(File file: folder.listFiles()){
				if(file.isFile() && (file.getName().endsWith(".jar") || file.getName().endsWith(".zip"))){
					jars.add(file);
				}else if (folder.isDirectory()){
					jars.addAll(getJarsOfFolder(file));
				}
			}
		}
		
		return jars;
	}
	
	// if two folders have the same jar file, one of them will be ignored.
	private List<String> mergeTwoList(List<File> jarList1, List<File> jarList2){
		List<String> rtList = new ArrayList<String>();
		List<String> distinctFileNames = new ArrayList<String>();
		
		for(File jarFile: jarList1){
			distinctFileNames.add(jarFile.getName());
			rtList.add(jarFile.getAbsolutePath());
		}
		for(File jarFile: jarList2){
			if(!distinctFileNames.contains(jarFile.getName())){
				rtList.add(jarFile.getAbsolutePath());
			}
		}
		
		return rtList;
		
	}
	
	/**
	 * Remove given JbossWSRuntime from manager
	 * 
	 * @param rt
	 *            JbossWSRuntime
	 */
	public void removeRuntime(JbossWSRuntime rt) {
		runtimes.remove(rt.getName());
	}

	/**
	 * Save preference value and force save changes to disk
	 */
	public void save() {
		JbossWSCorePlugin.getDefault().getPreferenceStore().setValue(
				JbossWSCoreMessages.WS_Location, converter.getString(runtimes));
		IPreferenceStore store = JbossWSCorePlugin.getDefault()
				.getPreferenceStore();
		if (store instanceof IPersistentPreferenceStore) {
			try {
				((IPersistentPreferenceStore) store).save();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Marks this runtime as default. Marks other runtimes with the same version
	 * as not default.
	 * 
	 * @param runtime
	 */
	public void setDefaultRuntime(JbossWSRuntime runtime) {
		JbossWSRuntime[] runtimes = getRuntimes();
		for (int i = 0; i < runtimes.length; i++) {
			runtimes[i].setDefault(false);
		}
		runtime.setDefault(true);
	}

	/**
	 * Return first default JbossWSRuntime
	 * 
	 * @return JbossWSRuntime
	 */
	public JbossWSRuntime getDefaultRuntime() {
		for (JbossWSRuntime rt : runtimes.values()) {
			if (rt.isDefault()) {
				return rt;
			}
		}
		return null;
	}

	/**
	 * Return list of available JbossWSRuntime names
	 * 
	 * @return List&lt;String&gt;
	 */
	public List<String> getRuntimeNames() {
		JbossWSRuntime[] rts = getRuntimes();
		List<String> result = new ArrayList<String>();
		for (JbossWSRuntime jbossWSRuntime : rts) {
			result.add(jbossWSRuntime.getName());
		}
		return result;
	}

	/**
	 * Return a list of all runtime names
	 * 
	 * @return List of all runtime names
	 */
	public List<String> getAllRuntimeNames() {
		JbossWSRuntime[] rts = getRuntimes();
		List<String> result = new ArrayList<String>();
		for (JbossWSRuntime jbossWSRuntime : rts) {
			result.add(jbossWSRuntime.getName());
		}
		return result;
	}

	/**
	 * TBD
	 * 
	 * @param oldName
	 *            old runtime name
	 * @param newName
	 *            new runtime name
	 */
	public void changeRuntimeName(String oldName, String newName) {
		JbossWSRuntime o = findRuntimeByName(oldName);
		if (o == null) {
			return;
		}
		o.setName(newName);
		onRuntimeNameChanged(oldName, newName);
	}

	private void onRuntimeNameChanged(String oldName, String newName) {
		IProjectFacet facet = ProjectFacetsManager
				.getProjectFacet("jbossws.core");
		Set<IFacetedProject> facetedProjects = null;
		try {
			facetedProjects = ProjectFacetsManager.getFacetedProjects(facet);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (IFacetedProject facetedProject : facetedProjects) {
			QualifiedName qRuntimeName = IJBossWSFacetDataModelProperties.PERSISTENCE_PROPERTY_QNAME_RUNTIME_NAME;
			String name = null;
			try {
				name = facetedProject.getProject().getPersistentProperty(
						qRuntimeName);
				if (name != null && name.equals(oldName)) {
					facetedProject.getProject().setPersistentProperty(
							qRuntimeName, newName);
				}
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	public static boolean isRuntimeUsed(String name) {
		IProjectFacet facet = ProjectFacetsManager
				.getProjectFacet("jbossws.core");
		Set<IFacetedProject> facetedProjects = null;
		try {
			facetedProjects = ProjectFacetsManager.getFacetedProjects(facet);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (IFacetedProject facetedProject : facetedProjects) {
			QualifiedName qRuntimeName = IJBossWSFacetDataModelProperties.PERSISTENCE_PROPERTY_QNAME_RUNTIME_NAME;
			try {
				if (name.equals(facetedProject.getProject()
						.getPersistentProperty(qRuntimeName))) {
					return true;
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	public void load() {
		IPreferenceStore ps = JbossWSCorePlugin.getDefault()
				.getPreferenceStore();

		String runtimeListString = ps
				.getString(JbossWSCoreMessages.WS_Location);

		runtimes = converter.getMap(runtimeListString);
	}

}