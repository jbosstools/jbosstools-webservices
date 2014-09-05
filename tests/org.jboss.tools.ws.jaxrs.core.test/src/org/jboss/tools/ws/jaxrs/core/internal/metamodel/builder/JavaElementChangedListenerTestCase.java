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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.JBossJaxrsCorePlugin;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils;
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author xcoulon
 *
 */
public class JavaElementChangedListenerTestCase {

	@ClassRule
	public static WorkspaceSetupRule workspaceSetupRule = new WorkspaceSetupRule("org.jboss.tools.ws.jaxrs.tests.sampleproject");
	
	@Rule
	public JaxrsMetamodelMonitor metamodelMonitor = new JaxrsMetamodelMonitor("org.jboss.tools.ws.jaxrs.tests.sampleproject", true);
	
	private JaxrsMetamodel metamodel = null;
	
	@Before
	public void setup() {
		metamodel = metamodelMonitor.getMetamodel();
		assertThat(metamodel, notNullValue());
	}
	
	@Before
	public void startListeners() {
		JBossJaxrsCorePlugin.getDefault().resumeListeners();
	}
	
	@After
	public void stopListeners() {
		JBossJaxrsCorePlugin.getDefault().pauseListeners();
	}
	
	@Test
	// FIXME: should be ignored if the JavaElementChangedListener feature is removed. This would now take place during validation. 
	public void shouldRemoveApplicationWhenRemovingUnderlyingType() throws CoreException {
		// pre-conditions
		final IType applicationType = metamodel.findJavaApplications().iterator().next().getJavaElement();
		// operation
		final ICompilationUnit workingCopy = applicationType.getCompilationUnit().getWorkingCopy(new NullProgressMonitor());
		JavaElementsUtils.delete(workingCopy.findPrimaryType());
		
		// verifications
		assertThat(metamodel.findJavaApplications().size(), equalTo(0));
	}
}
