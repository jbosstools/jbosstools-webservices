package org.jboss.tools.ws.jaxrs.core;

import org.jboss.tools.ws.jaxrs.core.builder.FullBuilderTestCase;
import org.jboss.tools.ws.jaxrs.core.internal.builder.HttpMethodChangesTestCase;
import org.jboss.tools.ws.jaxrs.core.internal.builder.JaxrsAnnotationScannerTestCase;
import org.jboss.tools.ws.jaxrs.core.internal.builder.ProviderChangesTestCase;
import org.jboss.tools.ws.jaxrs.core.internal.builder.ResourceChangesTestCase;
import org.jboss.tools.ws.jaxrs.core.internal.builder.ResourceMethodChangesTestCase;
import org.jboss.tools.ws.jaxrs.core.internal.configuration.ProjectBuilderUtilsTestCase;
import org.jboss.tools.ws.jaxrs.core.internal.configuration.ProjectNatureUtilsTestCase;
import org.jboss.tools.ws.jaxrs.core.internal.utils.PathParamValidationTestCase;
import org.jboss.tools.ws.jaxrs.core.utils.JdtUtilsTestCase;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ FullBuilderTestCase.class, HttpMethodChangesTestCase.class, JaxrsAnnotationScannerTestCase.class,
		ProviderChangesTestCase.class, ResourceChangesTestCase.class, ResourceMethodChangesTestCase.class,
		ProjectBuilderUtilsTestCase.class, ProjectNatureUtilsTestCase.class, PathParamValidationTestCase.class,
		JdtUtilsTestCase.class })
public class AllTests {

}
