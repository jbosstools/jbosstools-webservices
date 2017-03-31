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

package org.jboss.tools.ws.jaxrs.core.domain;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel; 
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsMetamodelLocator;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author xcoulon
 * 
 */
public class JaxrsMetamodelLocatorTestCase {

	@ClassRule
	public static WorkspaceSetupRule workspaceSetupRule = new WorkspaceSetupRule("org.jboss.tools.ws.jaxrs.tests.sampleproject");
	
	@Rule
	public JaxrsMetamodelMonitor metamodelMonitor = new JaxrsMetamodelMonitor("org.jboss.tools.ws.jaxrs.tests.sampleproject", false);
	
	private JaxrsMetamodel metamodel = null;

	private IJavaProject javaProject = null;

	@Before
	public void setup() throws CoreException {
		metamodel = metamodelMonitor.getMetamodel();
		javaProject = metamodel.getJavaProject();
		metamodel.remove();
	}
	
	@After
	public void reopenProjects() throws CoreException {
		//javaProject.open(new NullProgressMonitor());
	}

	@Test
	public void shouldNotGetMetamodelFromProjectIfMissing() throws CoreException {
		// pre-condition
		// operation
		final JaxrsMetamodel metamodel = JaxrsMetamodelLocator.get(javaProject);
		// validation
		assertThat(metamodel, nullValue());
	}

	@Test
	public void shouldNotGetMetamodelFromJavaProjectIfMissing() throws CoreException {
		// pre-condition
		// operation
		final JaxrsMetamodel metamodel = JaxrsMetamodelLocator.get(javaProject);
		// validation
		assertThat(metamodel, nullValue());
	}
	
	@Test
	public void shouldGetMetamodelFromProjectIfNotMissing() throws CoreException {
		// pre-condition
		JaxrsMetamodelLocator.get(javaProject, true);
		// operation
		final JaxrsMetamodel metamodel = JaxrsMetamodelLocator.get(javaProject);
		// validation
		assertThat(metamodel, notNullValue());
	}
	
	@Test
	public void shouldGetMetamodelFromJavaProjectEvenIfMissing() throws CoreException {
		// pre-condition
		// operation
		final JaxrsMetamodel metamodel = JaxrsMetamodelLocator.get(javaProject, true);
		// validation
		assertThat(metamodel, notNullValue());
	}
	
	@Test
	public void shouldGetMetamodelFromJavaProjectIfNotMissing() throws CoreException {
		// pre-condition
		JaxrsMetamodelLocator.get(javaProject, true);
		// operation
		final JaxrsMetamodel metamodel = JaxrsMetamodelLocator.get(javaProject);
		// validation
		assertThat(metamodel, notNullValue());
	}
	
	@Test
	public void shouldNotGetMetamodelFromClosedProject() throws CoreException {
		// pre-condition
		javaProject.getProject().close(new NullProgressMonitor());
		// operation
		final JaxrsMetamodel metamodel = JaxrsMetamodelLocator.get(javaProject);
		// validation
		assertThat(metamodel, nullValue());

	}

	@Test
	public void shouldNotGetMetamodelFromClosedJavaProject() throws CoreException {
		// pre-condition
		javaProject.close();
		// operation
		final JaxrsMetamodel metamodel = JaxrsMetamodelLocator.get(javaProject);
		// validation
		assertThat(metamodel, nullValue());
	}

	@Test
	public void shouldNotGetMetamodelFromNullProject() throws CoreException {
		// pre-condition
		// operation
		final JaxrsMetamodel metamodel = JaxrsMetamodelLocator.get((IProject)null);
		// validation
		assertThat(metamodel, nullValue());
	}

	@Test
	public void shouldNotGetMetamodelFromNullJavaProject() throws CoreException {
		// pre-condition
		// operation
		final JaxrsMetamodel metamodel = JaxrsMetamodelLocator.get((IJavaProject)null);
		// validation
		assertThat(metamodel, nullValue());
	}
}
