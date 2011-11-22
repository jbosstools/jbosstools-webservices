package org.jboss.tools.ws.jaxrs.core;

import org.jboss.tools.ws.jaxrs.core.internal.configuration.ProjectBuilderUtilsTestCase;
import org.jboss.tools.ws.jaxrs.core.internal.configuration.ProjectNatureUtilsTestCase;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.ElementChangedEventScannerTestCase;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JavaElementChangedEventFilterTestCase;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JavaElementChangedProcessorTestCase;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementChangedProcessorTestCase;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsMetamodelFullBuildJobTestCase;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsElementFactoryTestCase;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodelTestCase;
import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionUtilsTestCase;
import org.jboss.tools.ws.jaxrs.core.internal.utils.PathParamValidationTestCase;
import org.jboss.tools.ws.jaxrs.core.jdt.CompilationUnitsRepositoryTestCase;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtilsTestCase;
import org.jboss.tools.ws.jaxrs.core.pubsub.PubSubServiceTestCase;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ ProjectBuilderUtilsTestCase.class, ProjectNatureUtilsTestCase.class, JdtUtilsTestCase.class,
		CompilationUnitsRepositoryTestCase.class, ElementChangedEventScannerTestCase.class,
		JavaElementChangedEventFilterTestCase.class, JavaElementChangedProcessorTestCase.class,
		JaxrsElementChangedProcessorTestCase.class, JaxrsMetamodelFullBuildJobTestCase.class,
		JaxrsElementFactoryTestCase.class, JaxrsMetamodelTestCase.class, CollectionUtilsTestCase.class,
		PathParamValidationTestCase.class, PubSubServiceTestCase.class })
public class AllTests {

}
