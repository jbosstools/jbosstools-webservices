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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.jboss.tools.ws.core.JBossWSCorePlugin;
import org.jboss.tools.ws.core.facet.delegate.IJBossWSFacetDataModelProperties;
import org.jboss.tools.ws.core.messages.JBossWSCoreMessages;

/**
 * @author Grid Qian
 */
public class JBossWSRuntimeManager {

	private static JBossWSRuntimeListConverter converter = new JBossWSRuntimeListConverter();

	private Map<String, JBossWSRuntime> runtimes = new HashMap<String, JBossWSRuntime>();

	/**
	 * Private constructor
	 */
	private JBossWSRuntimeManager() {
		load();
	}

	/**
	 * This class make Java Runtime responsible for solving synchronization
	 * problems during initialization if there is any
	 * 
	 */
	static class JBossWSRuntimeManagerHolder {
		private static final JBossWSRuntimeManager INSTANCE = new JBossWSRuntimeManager();
	}

	/**
	 * Return JBossWSRuntimeManaher instance
	 * 
	 * @return JBossWSRuntimeManager instance
	 */
	public static JBossWSRuntimeManager getInstance() {
		return JBossWSRuntimeManagerHolder.INSTANCE;
	}

	/**
	 * Return Array of configured JBossWSRuntimes
	 * 
	 * @return JBossWSRuntime[]
	 */
	public JBossWSRuntime[] getRuntimes() {
		Collection<JBossWSRuntime> c = runtimes.values();
		return c.toArray(new JBossWSRuntime[runtimes.size()]);
	}

	/**
	 * Add new JBossWSRuntime
	 * 
	 * @param runtime
	 *            JBossWSRuntime
	 */
	public void addRuntime(JBossWSRuntime runtime) {
		if (runtimes.size() == 0) {
			runtime.setDefault(true);
		}

		JBossWSRuntime oldDefaultRuntime = getDefaultRuntime();
		if (oldDefaultRuntime != null && runtime.isDefault()) {
			oldDefaultRuntime.setDefault(false);
		}
		runtimes.put(runtime.getName(), runtime);
		save();
	}

	/**
	 * Add new JBossWSRuntime with given parameters
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
		JBossWSRuntime jbossWSRt = new JBossWSRuntime();
		jbossWSRt.setHomeDir(path);
		jbossWSRt.setName(name);
		jbossWSRt.setVersion(version);
		jbossWSRt.setDefault(defaultRt);
		addRuntime(jbossWSRt);
	}

	/**
	 * Return JBossWSRuntime by given name
	 * 
	 * @param name
	 *            String - JBossWSRuntime name
	 * @return JBossWSRuntime - found JBossWSRuntime instance or null
	 */
	public JBossWSRuntime findRuntimeByName(String name) {
		for (JBossWSRuntime jbossWSRuntime : runtimes.values()) {
			if (jbossWSRuntime.getName().equals(name)) {
				return jbossWSRuntime;
			}
		}
		return null;
	}

	public List<String> getAllRuntimeJars(JBossWSRuntime rt){
		List<String> jarList = new ArrayList<String>();
		if (rt != null) {
			if (rt.isUserConfigClasspath()) {
				jarList.addAll(rt.getLibraries());
				 
			} else {
				IPath wsPath = new Path(rt.getHomeDir());
				if (wsPath != null) {
					IPath libPath = wsPath
							.append(JBossWSCoreMessages.Dir_Lib);
					List<File> libs = getJarsOfFolder(libPath.toFile());
					libPath = wsPath
							.append(JBossWSCoreMessages.Dir_Client);
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
	 * Remove given JBossWSRuntime from manager
	 * 
	 * @param rt
	 *            JBossWSRuntime
	 */
	public void removeRuntime(JBossWSRuntime rt) {
		runtimes.remove(rt.getName());
	}

	/**
	 * Save preference value and force save changes to disk
	 */
	public void save() {
		JBossWSCorePlugin.getDefault().getPreferenceStore().setValue(
				JBossWSCoreMessages.WS_Location, converter.getString(runtimes));
		IPreferenceStore store = JBossWSCorePlugin.getDefault()
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
	public void setDefaultRuntime(JBossWSRuntime runtime) {
		JBossWSRuntime[] runtimes = getRuntimes();
		for (int i = 0; i < runtimes.length; i++) {
			runtimes[i].setDefault(false);
		}
		runtime.setDefault(true);
	}

	/**
	 * Return first default JBossWSRuntime
	 * 
	 * @return JBossWSRuntime
	 */
	public JBossWSRuntime getDefaultRuntime() {
		for (JBossWSRuntime rt : runtimes.values()) {
			if (rt.isDefault()) {
				return rt;
			}
		}
		return null;
	}

	/**
	 * Return list of available JBossWSRuntime names
	 * 
	 * @return List&lt;String&gt;
	 */
	public List<String> getRuntimeNames() {
		JBossWSRuntime[] rts = getRuntimes();
		List<String> result = new ArrayList<String>();
		for (JBossWSRuntime jbossWSRuntime : rts) {
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
		JBossWSRuntime[] rts = getRuntimes();
		List<String> result = new ArrayList<String>();
		for (JBossWSRuntime jbossWSRuntime : rts) {
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
		JBossWSRuntime o = findRuntimeByName(oldName);
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
		IPreferenceStore ps = JBossWSCorePlugin.getDefault()
				.getPreferenceStore();

		String runtimeListString = ps
				.getString(JBossWSCoreMessages.WS_Location);

		runtimes = converter.getMap(runtimeListString);
	}

}