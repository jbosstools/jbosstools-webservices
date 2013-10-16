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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.jboss.tools.ws.jaxrs.core.JBossJaxrsCorePlugin;
import org.jboss.tools.ws.jaxrs.core.builder.AbstractMetamodelBuilderTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author xcoulon
 *
 */
public class JavaElementChangedListenerTestCase extends AbstractMetamodelBuilderTestCase {

	@Before
	public void startListeners() {
		JBossJaxrsCorePlugin.getDefault().resumeListeners();
	}
	
	@After
	public void stopListeners() {
		JBossJaxrsCorePlugin.getDefault().pauseListeners();
	}
	
	@Test
	public void shouldRemoveApplicationWhenRemovingUnderlyingType() throws JavaModelException {
		// pre-conditions
		final IType applicationType = metamodel.getJavaApplications().get(0).getJavaElement();
		// operation
		final ICompilationUnit workingCopy = applicationType.getCompilationUnit().getWorkingCopy(new NullProgressMonitor());
		workingCopy.findPrimaryType().delete(true, new NullProgressMonitor());
		// verifications
		assertThat(metamodel.getJavaApplications().size(), equalTo(0));
	}
}
