/**
 * 
 */
package org.jboss.tools.ws.jaxrs.core.junitrules;

import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author xcoulon
 *
 */
public class TestWatcher extends org.junit.rules.TestWatcher {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestWatcher.class);

	protected void starting(org.junit.runner.Description description) {
		LOGGER.debug("***********************************************");
		LOGGER.debug("* Starting {}.{}", description.getClassName(), description.getMethodName());
		LOGGER.debug("***********************************************");
	};
	
	@Override
	protected void failed(Throwable e, Description description) {
		LOGGER.debug("***********************************************");
		LOGGER.debug("* Failed {}.{}: {}", description.getClassName(), description.getMethodName(), e.getMessage());
		LOGGER.debug("***********************************************");
	}
	
	@Override
	protected void finished(Description description) {
		LOGGER.debug("***********************************************");
		LOGGER.debug("* Finished {}.{}", description.getClassName(), description.getMethodName());
		LOGGER.debug("***********************************************");
	}
}
