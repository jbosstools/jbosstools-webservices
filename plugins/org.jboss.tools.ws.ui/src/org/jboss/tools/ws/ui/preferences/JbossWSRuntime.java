package org.jboss.tools.ws.ui.preferences;

public class JbossWSRuntime {

	String name = null;

	String homeDir = null;

	boolean defaultRt = false;

	/**
	 * Default constructor
	 */
	public JbossWSRuntime() {
	}

	/**
	 * Get JbossWSRuntime name
	 * 
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get path to home directory
	 * 
	 * @return home directory path as string
	 */
	public String getHomeDir() {
		return homeDir;
	}

	/**
	 * Set JbossWSRuntime name
	 * 
	 * @param name
	 *            new JbossWSRuntime name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Set home directory
	 * 
	 * @param homeDir
	 *            new JbossWSRuntime's home directory
	 */
	public void setHomeDir(String homeDir) {
		this.homeDir = homeDir;
	}

	/**
	 * Mark runtime as default
	 * 
	 * @param b
	 *            new value for default property
	 */
	public void setDefault(boolean b) {
		this.defaultRt = b;
	}

	/**
	 * Get default flag
	 * 
	 * @return default property
	 */
	public boolean isDefault() {
		return defaultRt;
	}

	/**
	 * Calculate path to seam-gen
	 * 
	 * @return absolute path to seam-gen folder
	 */
	public String getSeamGenDir() {
		return getHomeDir() + "/seam-gen"; //$NON-NLS-1$
	}

	/**
	 * Calculate path to source templates
	 * 
	 * @return absolute path to source templates
	 */
	public String getSrcTemplatesDir() {
		return getSeamGenDir() + "/src"; //$NON-NLS-1$
	}

	/**
	 * Calculate path to view templates
	 * 
	 * @return absolute path to view templates
	 */
	public String getViewTemplatesDir() {
		return getSeamGenDir() + "/view"; //$NON-NLS-1$
	}

	/**
	 * Calculate path to resource templates
	 * 
	 * @return absolute path to resource templates
	 */
	public String getResourceTemplatesDir() {
		return getSeamGenDir() + "/resources"; //$NON-NLS-1$
	}

	/**
	 * Calculate path to test templates
	 * 
	 * @return absolute path to test templates
	 */
	public String getTestTemplatesDir() {
		return getSeamGenDir() + "/test"; //$NON-NLS-1$
	}

	/**
	 * Calculate path to templates root directory
	 * 
	 * @return absolute path to templates root directory
	 */
	public String getTemplatesDir() {
		return getSeamGenDir();
	}

}
