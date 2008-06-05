package org.jboss.tools.ws.ui.preferences;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.jboss.tools.ws.core.JbossWSCorePlugin;

public class JbossWSRuntimeManager {

		private static JbossWSRuntimeListConverter converter = new JbossWSRuntimeListConverter();

		private Map<String, JbossWSRuntime> runtimes = new HashMap<String, JbossWSRuntime>();

		/**
		 * Private constructor
		 */
		private JbossWSRuntimeManager() {
			IPreferenceStore ps = JbossWSCorePlugin.getDefault().getPreferenceStore();
			
			String runtimeListString = ps.getDefaultString("jbosswsruntimelocation");

			runtimes = converter.getMap(runtimeListString);
		}

		/**
		 *	This class make Java Runtime responsible for solving synchronization 
		 *	problems during initialization if there is any  
		 *  
		 */
		static class JbossWSRuntimeManagerHolder {
			private static final JbossWSRuntimeManager INSTANCE = new JbossWSRuntimeManager();
		}

		/**
		 * Return SeamRuntimeManaher instance
		 * 
		 * @return
		 * 		SeamRuntimeManager instance
		 */
		public static JbossWSRuntimeManager getInstance() {
			return JbossWSRuntimeManagerHolder.INSTANCE;
		}

		/**
		 * Return Array of configured SeamRuntimes
		 * 
		 * @return
		 * 		SeamRuntime[]
		 */
		public JbossWSRuntime[] getRuntimes() {
			Collection<JbossWSRuntime> c = runtimes.values();
			return c.toArray(new JbossWSRuntime[runtimes.size()]);
		}

		/**
		 * Add new SeamRuntime
		 * 
		 * @param runtime
		 * 		SeamRuntime
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
		 * Add new SeamRuntime with given parameters
		 * 
		 * @param name
		 * 	String - runtime name
		 * @param path
		 * 	String - runtime home folder
		 * @param version
		 * 	String - string representation of version number 
		 * @param defaultRt
		 * 	boolean - default flag
		 */
		public void addRuntime(String name, String path,
				boolean defaultRt) {
			JbossWSRuntime seamRt = new JbossWSRuntime();
			seamRt.setHomeDir(path);
			seamRt.setName(name);
			seamRt.setDefault(defaultRt);
			addRuntime(seamRt);
		}

		/**
		 * Return SeamRuntime by given name
		 * 
		 * @param name
		 * 	String - SeamRuntime name
		 * @return
		 * 	SeamRuntime - found SeamRuntime instance or null
		 */
		public JbossWSRuntime findRuntimeByName(String name) {
			for (JbossWSRuntime seamRuntime : runtimes.values()) {
				if (seamRuntime.getName().equals(name)) {
					return seamRuntime;
				}
			}
			return null;
		}

		/**
		 * Remove given SeamRuntime from manager
		 * @param rt
		 * 	SeamRuntime
		 */
		public void removeRuntime(JbossWSRuntime rt) {
			runtimes.remove(rt.getName());
		}


		/**
		 * Save preference value and force save changes to disk
		 */
		public void save() {
			JbossWSCorePlugin.getDefault().getPreferenceStore().setValue(
					"jbosswsruntimelocation",
					converter.getString(runtimes));
			IPreferenceStore store = JbossWSCorePlugin.getDefault().getPreferenceStore();
			if (store instanceof IPersistentPreferenceStore) {
				try {
					((IPersistentPreferenceStore) store).save();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		/**
		 * Marks this runtime as default. Marks other runtimes with the same version as not default.
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
		 * Return first default SeamRuntime
		 * 
		 * @return
		 * 	SeamRuntime
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
		 * If project has seam facet then this method returns default seam runtime for proper version of facet.
		 * Otherwise return first default runtime.  
		 * @param project
		 * @return
		 */
		public static JbossWSRuntime getDefaultRuntimeForProject(IProject project) {
			if(project==null) {
				throw new IllegalArgumentException("Project must not be null.");
			}
			try {
				IProjectFacet facet = ProjectFacetsManager.getProjectFacet("jbossws.core");
				IFacetedProject facetedProject = ProjectFacetsManager.create(project);
				if(facetedProject!=null) {
					return getInstance().getDefaultRuntime();
				}
			} catch (CoreException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
			return getInstance().getDefaultRuntime();
		}

		/**
		 * Return list of available SeamRuntime names
		 * 
		 * @return
		 * 	List&lt;String&gt;
		 */
		public List<String> getRuntimeNames() {
			JbossWSRuntime[] rts = getRuntimes();
			List<String> result = new ArrayList<String>();
			for (JbossWSRuntime seamRuntime : rts) {
				result.add(seamRuntime.getName());
			}
			return result;
		}

		/**
		 * Return a list of all runtime names
		 * 
		 * @return
		 * 	List of all runtime names
		 */
		public List<String> getAllRuntimeNames() {
			JbossWSRuntime[] rts = getRuntimes();
			List<String> result = new ArrayList<String>();
			for (JbossWSRuntime seamRuntime : rts) {
				result.add(seamRuntime.getName());
			}
			return result;
		}

		/**
		 * TBD
		 * 
		 * @param oldName
		 * 	old runtime name
		 * @param newName
		 * 	new runtime name
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
			IProject[] ps = ResourcesPlugin.getWorkspace().getRoot().getProjects();
//			for (int i = 0; i < ps.length; i++) {
//				ISeamProject sp = SeamCorePlugin.getSeamProject(ps[i], false);
//				if (sp != null && oldName.equals(sp.getRuntimeName())) {
//					sp.setRuntimeName(newName);
//				}
//			}
		}
}