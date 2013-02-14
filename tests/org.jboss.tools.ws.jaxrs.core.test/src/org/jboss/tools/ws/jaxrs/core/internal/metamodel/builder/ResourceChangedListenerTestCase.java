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
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.jboss.tools.ws.jaxrs.core.JBossJaxrsCorePlugin;
import org.jboss.tools.ws.jaxrs.core.builder.AbstractMetamodelBuilderTestCase;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.metamodel.JaxrsMetamodelLocator;
import org.junit.Test;

/**
 * @author Xavier Coulon
 *
 */
public class ResourceChangedListenerTestCase extends AbstractMetamodelBuilderTestCase {

	@Test
	public void shouldRemoveMetamodelWhileClosingProject() throws CoreException {
		// pre-conditions
		final JaxrsMetamodel previousMetamodel = JaxrsMetamodelLocator.get(project);
		assertThat(previousMetamodel, notNullValue());
		JBossJaxrsCorePlugin.getDefault().pauseListeners();
		// operation
		project.close(new NullProgressMonitor());
		project.open(new NullProgressMonitor());
		// verifications
		final JaxrsMetamodel newMetamodel = JaxrsMetamodelLocator.get(project);
		assertThat(newMetamodel, nullValue());
		assertThat(previousMetamodel, not(equalTo(newMetamodel)));
	}
}
