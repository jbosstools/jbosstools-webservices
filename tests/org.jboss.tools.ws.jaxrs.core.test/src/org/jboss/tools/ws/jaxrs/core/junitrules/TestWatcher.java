/**
 * 
 */
package org.jboss.tools.ws.jaxrs.core.junitrules;

import org.jboss.tools.ws.jaxrs.core.internal.utils.TestLogger;
import org.junit.runner.Description;

/**
 * @author xcoulon
 *
 */
public class TestWatcher extends org.junit.rules.TestWatcher {

	protected void starting(org.junit.runner.Description description) {
		TestLogger.debug("***********************************************");
		TestLogger.debug("* Starting {}.{}", description.getClassName(), description.getMethodName());
		TestLogger.debug("***********************************************");
	};
	
	@Override
	protected void failed(Throwable e, Description description) {
		TestLogger.debug("***********************************************");
		TestLogger.debug("* Failed {}.{}: {}", description.getClassName(), description.getMethodName(), e.getMessage());
		TestLogger.debug("***********************************************");
	}
	
	@Override
	protected void finished(Description description) {
		TestLogger.debug("***********************************************");
		TestLogger.debug("* Finished {}.{}", description.getClassName(), description.getMethodName());
		TestLogger.debug("***********************************************");
	}
}
