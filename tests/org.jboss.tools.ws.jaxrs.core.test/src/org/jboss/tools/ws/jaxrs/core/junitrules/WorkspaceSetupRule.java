/**
 * 
 */
package org.jboss.tools.ws.jaxrs.core.junitrules;

import java.util.Date;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.jboss.tools.ws.jaxrs.core.JBossJaxrsCorePlugin;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author xcoulon
 *
 */
public class WorkspaceSetupRule extends ExternalResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkspaceSetupRule.class);
	
	private final String projectName;
	
	public WorkspaceSetupRule(final String projectName) {
		this.projectName = projectName;
	}
	
	@Override
	protected void before() throws Throwable {
		long startTime = new Date().getTime();
		LOGGER.debug("***********************************************");
		LOGGER.debug("* Setting up test workspace...");
		LOGGER.debug("***********************************************");
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
			long endTime = new Date().getTime();
			LOGGER.debug("***********************************************");
			LOGGER.debug("*** Test workspace setup done in {} ms.", (endTime - startTime));
			LOGGER.debug("***********************************************");
		}
	}

}
