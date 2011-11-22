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

package org.jboss.tools.ws.jaxrs.core.internal.utils;

import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.jboss.tools.ws.jaxrs.core.WorkbenchUtils;
import org.jboss.tools.ws.jaxrs.core.builder.AbstractMetamodelBuilderTestCase;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsMetamodelBuilder;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsResource;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("Requires core code refactoring first")
public class PathParamValidationTestCase extends AbstractMetamodelBuilderTestCase {

	@Test
	public void shouldReportValidationErrorsWhenPathParamDoesNotMatchPath() throws CoreException {
		// pre-conditions : add a standard class : no new root resource (yet)
		ICompilationUnit fooCompilationUnit = WorkbenchUtils.createCompilationUnit(javaProject, "FooResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "FooResource.java", bundle);
		IMarker[] markers = fooCompilationUnit.getResource().findMarkers(JaxrsMetamodelBuilder.JAXRS_PROBLEM, true,
				IResource.DEPTH_INFINITE);
		Assert.assertEquals("No marker expected", 0, markers.length);
		// operation : replace @PathParam("id") with @PathParam("ide")
		WorkbenchUtils.replaceAllOccurrencesOfCode(fooCompilationUnit, "@PathParam(\"id\")", "@PathParam(\"ide\")",
				true);
		// post-conditions: expect a validation error
		markers = fooCompilationUnit.getResource().findMarkers(JaxrsMetamodelBuilder.JAXRS_PROBLEM, true,
				IResource.DEPTH_INFINITE);
		Assert.assertEquals("Wrong number of markers", 1, markers.length);

	}

	@Test
	public void shouldNotReportValidationErrorsWhenPathParamDoesMatchPath() throws JavaModelException, CoreException {
		// pre-conditions : add a standard class : no new root resource (yet)
		ICompilationUnit fooCompilationUnit = WorkbenchUtils.createCompilationUnit(javaProject, "FooResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "FooResource.java", bundle);
		List<IJaxrsResource> resources = metamodel.getAllResources();
		Assert.assertEquals(6, resources.size());
		IMarker[] markers = fooCompilationUnit.getResource().findMarkers(JaxrsMetamodelBuilder.JAXRS_PROBLEM, true,
				IResource.DEPTH_INFINITE);
		Assert.assertEquals("No marker expected", 0, markers.length);
		// operation : change both @Path and @PathParam values
		WorkbenchUtils
				.replaceAllOccurrencesOfCode(fooCompilationUnit, "@Path(\"{id}\")", "@PathParam(\"{ide}\")", true);
		WorkbenchUtils.replaceAllOccurrencesOfCode(fooCompilationUnit, "@PathParam(\"id\")", "@PathParam(\"ide\")",
				true);
		// post-conditions: expect no validation error
		markers = fooCompilationUnit.getResource().findMarkers(JaxrsMetamodelBuilder.JAXRS_PROBLEM, true,
				IResource.DEPTH_INFINITE);
		Assert.assertEquals("No marker expected", 0, markers.length);

	}

}
