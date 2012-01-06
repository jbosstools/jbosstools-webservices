/******************************************************************************* 
 * Copyright (c) 2008 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.ws.jaxrs.core;

import org.jboss.tools.ws.jaxrs.core.internal.configuration.ProjectBuilderUtilsTestCase;
import org.jboss.tools.ws.jaxrs.core.internal.configuration.ProjectNatureUtilsTestCase;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.ElementChangedEventScannerTestCase;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JavaElementChangedEventFilterTestCase;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JavaElementChangedProcessorTestCase;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementChangedProcessorTestCase;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsMetamodelBuilderTestCase;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.ResourceChangedListenerTestCase;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.ResourceChangedProcessorTestCase;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsElementFactoryTestCase;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodelTestCase;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.validation.JaxrsMetamodelValidatorTestCase;
import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionUtilsTestCase;
import org.jboss.tools.ws.jaxrs.core.jdt.CompilationUnitsRepositoryTestCase;
import org.jboss.tools.ws.jaxrs.core.jdt.JaxrsAnnotationScannerTestCase;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtilsTestCase;
import org.jboss.tools.ws.jaxrs.core.pubsub.PubSubServiceTestCase;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ ProjectBuilderUtilsTestCase.class, ProjectNatureUtilsTestCase.class, JdtUtilsTestCase.class,
		CompilationUnitsRepositoryTestCase.class, ElementChangedEventScannerTestCase.class,
		JavaElementChangedEventFilterTestCase.class, JavaElementChangedProcessorTestCase.class,
		JaxrsElementChangedProcessorTestCase.class, JaxrsMetamodelTestCase.class,
		JaxrsElementFactoryTestCase.class, JaxrsMetamodelBuilderTestCase.class, CollectionUtilsTestCase.class,
		PubSubServiceTestCase.class, JaxrsMetamodelValidatorTestCase.class,
		ResourceChangedProcessorTestCase.class, ResourceChangedListenerTestCase.class,
		JaxrsAnnotationScannerTestCase.class})
public class AllTests {

}
