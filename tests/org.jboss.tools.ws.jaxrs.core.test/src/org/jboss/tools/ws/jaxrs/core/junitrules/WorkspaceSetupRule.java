/**
 * 
 */
package org.jboss.tools.ws.jaxrs.core.junitrules;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.jboss.tools.ws.jaxrs.core.JBossJaxrsCorePlugin;
import org.jboss.tools.ws.jaxrs.core.internal.utils.TestLogger;
import org.junit.rules.ExternalResource;

/**
 * @author xcoulon
 *
 */
public class WorkspaceSetupRule extends ExternalResource {

	private final String projectName;
	
	public WorkspaceSetupRule(final String projectName) {
		this.projectName = projectName;
	}
	
	@Override
	protected void before() throws Throwable {
		long startTime = System.currentTimeMillis();
		TestLogger.debug("***********************************************");
		TestLogger.debug("*** Setting up test workspace...");
		TestLogger.debug("***********************************************");
		JBossJaxrsCorePlugin.getDefault().pauseListeners();
		try {
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			if (workspace.isAutoBuilding()) {
				IWorkspaceDescription description = workspace.getDescription();
				description.setAutoBuilding(false);
				workspace.setDescription(description);
			}
			WorkbenchTasks.synchronizeProject(projectName);
		} finally {
			long endTime = System.currentTimeMillis();
			TestLogger.debug("***********************************************");
			TestLogger.debug("*** Test workspace setup done in {} ms.", (endTime - startTime));
			TestLogger.debug("***********************************************");
		}
	}

}
