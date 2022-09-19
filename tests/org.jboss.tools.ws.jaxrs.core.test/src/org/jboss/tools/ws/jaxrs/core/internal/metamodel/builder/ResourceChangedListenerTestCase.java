/******************************************************************************* 
 * Copyright (c) 2008 - 2014 Red Hat, Inc. and others. 
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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.jboss.tools.ws.jaxrs.core.JBossJaxrsCorePlugin;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsMetamodelLocator;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Xavier Coulon
 *
 */
public class ResourceChangedListenerTestCase {

	@ClassRule
	public static WorkspaceSetupRule workspaceSetupRule = new WorkspaceSetupRule("org.jboss.tools.ws.jaxrs.tests.sampleproject");
	
	@Rule
	public JaxrsMetamodelMonitor metamodelMonitor = new JaxrsMetamodelMonitor("org.jboss.tools.ws.jaxrs.tests.sampleproject", false);
	
	private IProject project = null;

	@Before
	public void setup() throws CoreException {
		project = metamodelMonitor.getMetamodel().getProject();
	}

	@Test
	// @see https://issues.jboss.org/browse/JBIDE-15827
	public void shouldRemoveMetamodelWhileClosingProject() throws CoreException {
		// pre-conditions
		final JaxrsMetamodel previousMetamodel = JaxrsMetamodelLocator.get(project);
		assertThat(previousMetamodel, notNullValue());
		// operation
		project.close(new NullProgressMonitor());
		project.open(new NullProgressMonitor());
		// verifications
		final JaxrsMetamodel newMetamodel = JaxrsMetamodelLocator.get(project);
		assertThat(newMetamodel, nullValue());
		assertThat(previousMetamodel, not(equalTo(newMetamodel)));
	}
	
	@Test
	public void shouldNotFailWhenClosingProject() throws CoreException {
		// pre-conditions
		final BlockingQueue<Boolean> queue = new ArrayBlockingQueue<Boolean>(1);
		JBossJaxrsCorePlugin.getDefault().resumeListeners();
		ILogListener logListener = new ILogListener() {

			@Override
			public void logging(IStatus status, String plugin) {
				if(status.getSeverity() == IStatus.ERROR && plugin.equals(JBossJaxrsCorePlugin.PLUGIN_ID)) {
					queue.add(Boolean.TRUE);
				}
				
			}
		};
		JBossJaxrsCorePlugin.getDefault().getLog().addLogListener(logListener);
		// operation
		project.close(new NullProgressMonitor());
		JBossJaxrsCorePlugin.getDefault().getLog().removeLogListener(logListener);
		// verifications
		assertThat(JaxrsMetamodelLocator.get(project), nullValue());
		assertThat(queue.size(), equalTo(0));
	}
}
