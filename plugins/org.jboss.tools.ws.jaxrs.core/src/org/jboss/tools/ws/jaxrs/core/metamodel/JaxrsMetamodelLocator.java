package org.jboss.tools.ws.jaxrs.core.metamodel;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.IJavaProject;
import org.jboss.tools.ws.jaxrs.core.JBossJaxrsCorePlugin;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;

public class JaxrsMetamodelLocator {

	public static final QualifiedName METAMODEL_QUALIFIED_NAME = new QualifiedName(JBossJaxrsCorePlugin.PLUGIN_ID,
			"metamodel");

	/** singleton instance. */
	private static JaxrsMetamodelLocator locator = new JaxrsMetamodelLocator();

	/** Singleton constructor */
	private JaxrsMetamodelLocator() {

	}

	/** Singleton accessor
	 * 
	 * @return */
	public static JaxrsMetamodelLocator getInstance() {
		return locator;
	}

	/** Accessor to the metamodel from the given project's session properties.
	 * 
	 * @param javaProject
	 *            the java project
	 * @return the metamodel or null if none was found
	 * @throws CoreException
	 *             in case of underlying exception */
	public static JaxrsMetamodel get(IJavaProject javaProject) throws CoreException {
		if (javaProject == null || javaProject.getProject() == null) {
			return null;
		}
		return (JaxrsMetamodel) javaProject.getProject().getSessionProperty(METAMODEL_QUALIFIED_NAME);
	}

	/** Accessor to the metamodel from the given project's session properties.
	 * 
	 * @param project
	 *            the project
	 * @return the metamodel or null if none was found
	 * @throws CoreException
	 *             in case of underlying exception */
	public JaxrsMetamodel get(IProject project) throws CoreException {
		return (JaxrsMetamodel) project.getSessionProperty(METAMODEL_QUALIFIED_NAME);
	}

}
