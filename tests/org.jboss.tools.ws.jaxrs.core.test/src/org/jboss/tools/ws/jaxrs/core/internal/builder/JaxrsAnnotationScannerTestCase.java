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

package org.jboss.tools.ws.jaxrs.core.internal.builder;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.tools.ws.jaxrs.core.AbstractCommonTestCase;
import org.jboss.tools.ws.jaxrs.core.JBossJaxrsCoreTestsPlugin;
import org.jboss.tools.ws.jaxrs.core.WorkbenchTasks;
import org.jboss.tools.ws.jaxrs.core.WorkbenchUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.MediaTypeCapabilities;
import org.jboss.tools.ws.jaxrs.core.utils.JdtUtils;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Bundle;

public class JaxrsAnnotationScannerTestCase extends AbstractCommonTestCase {

	protected Bundle bundle = JBossJaxrsCoreTestsPlugin.getDefault().getBundle();

	@Test
	public void shouldFindProjectProviders() throws CoreException {
		List<IType> providers = JAXRSAnnotationsScanner
				.findProviderTypes(javaProject, false, new NullProgressMonitor());
		// 20 in resteasy libs + 5 in the project, but only 5 are returned
		Assert.assertEquals("Wrong number of resources found", 5, providers.size());
	}

	@Test
	public void shouldFindAllProviders() throws CoreException {
		List<IType> providers = JAXRSAnnotationsScanner.findProviderTypes(javaProject, true, new NullProgressMonitor());
		// 20 in resteasy libs + 5 in the project, all are returned
		Assert.assertEquals("Wrong number of resources found", 25, providers.size());
	}

	@Test
	public void shouldFindSubresourceLocator() throws CoreException {
		List<IType> httpMethodTypes = JAXRSAnnotationsScanner.findHTTPMethodTypes(javaProject,
				new NullProgressMonitor());
		Assert.assertEquals("Wrong number of resources found", 6, httpMethodTypes.size());
		List<String> httpMethodNames = new ArrayList<String>();
		for (IType httpMethodType : httpMethodTypes) {
			httpMethodNames.add(httpMethodType.getFullyQualifiedName());
		}
		IType productsResourceLocator = JdtUtils.resolveType(
				"org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator", javaProject,
				new NullProgressMonitor());
		Assert.assertNotNull("productsResourceLocator not found", productsResourceLocator);
		List<IMethod> methods = JAXRSAnnotationsScanner.findResourceMethods(productsResourceLocator, httpMethodNames,
				new NullProgressMonitor());
		Assert.assertEquals("Wrong number of resourceMethods found", 1, methods.size());
	}

	@Test
	public void shouldFindAllMethodsInTypeScope() throws CoreException {
		List<IType> httpMethodTypes = JAXRSAnnotationsScanner.findHTTPMethodTypes(javaProject,
				new NullProgressMonitor());
		Assert.assertEquals("Wrong number of resources found", 6, httpMethodTypes.size());
		List<String> httpMethodNames = new ArrayList<String>();
		for (IType httpMethodType : httpMethodTypes) {
			httpMethodNames.add(httpMethodType.getFullyQualifiedName());
		}
		IType resource = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				new NullProgressMonitor());
		Assert.assertNotNull("CustomerResource not found", resource);
		List<IMethod> methods = JAXRSAnnotationsScanner.findResourceMethods(resource, httpMethodNames,
				new NullProgressMonitor());
		Assert.assertEquals("Wrong number of resourceMethods found", 6, methods.size());
	}

	@Test
	public void shouldFindSingleMethodInMethodScope() throws CoreException {
		List<IType> httpMethodTypes = JAXRSAnnotationsScanner.findHTTPMethodTypes(javaProject,
				new NullProgressMonitor());
		Assert.assertEquals("Wrong number of resources found", 6, httpMethodTypes.size());
		List<String> httpMethodNames = new ArrayList<String>();
		for (IType httpMethodType : httpMethodTypes) {
			httpMethodNames.add(httpMethodType.getFullyQualifiedName());
		}
		IType resource = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				new NullProgressMonitor());
		Assert.assertNotNull("CustomerResource not found", resource);
		IMethod method = resource.getMethods()[0];
		List<IMethod> methods = JAXRSAnnotationsScanner.findResourceMethods(method, httpMethodNames,
				new NullProgressMonitor());
		Assert.assertEquals("Wrong number of resourceMethods found", 1, methods.size());
	}

	@Test
	public void shouldFindAllResources() throws CoreException {
		List<IType> httpMethodTypes = JAXRSAnnotationsScanner.findHTTPMethodTypes(javaProject,
				new NullProgressMonitor());
		Assert.assertEquals("Wrong number of resources found", 6, httpMethodTypes.size());
		List<String> httpMethodNames = new ArrayList<String>();
		for (IType httpMethodType : httpMethodTypes) {
			httpMethodNames.add(httpMethodType.getFullyQualifiedName());
		}
		// include a resource that is annotated with @Path (root resource), but
		// with no jaxrs* method inside, to check the
		// that a single annotation at type level is enough, even if the
		// rootresource is invalid in term of usage
		WorkbenchUtils.createCompilationUnit(javaProject, "FooResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "FooResource.java", bundle);

		List<IType> types = JAXRSAnnotationsScanner.findResources(javaProject, httpMethodNames,
				new NullProgressMonitor());
		Assert.assertEquals("Wrong number of resources found", 6, types.size());
	}

	@Test
	public void shouldFindHttpMethods() throws CoreException {
		List<IType> resources = JAXRSAnnotationsScanner.findHTTPMethodTypes(javaProject, new NullProgressMonitor());
		// @GET, @PUT, @POST, @DELETE, @OPTIONS, @HEAD
		Assert.assertEquals("Wrong number of resources found", 6, resources.size());
	}

	@Test
	public void shouldFindHttpMethodsForProfiling() throws CoreException {
		// first call
		List<IType> resources = JAXRSAnnotationsScanner.findHTTPMethodTypes(javaProject, new NullProgressMonitor());
		// second call to method, to see if it goes faster after the jars have
		// be scanned once
		resources = JAXRSAnnotationsScanner.findHTTPMethodTypes(javaProject, new NullProgressMonitor());
		Assert.assertEquals("Wrong number of resources found", 6, resources.size());
	}

	@Test
	public void shouldResolvedMimeTypesOnMethodLevel() throws JavaModelException, CoreException {
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.PurchaseOrderResource",
				javaProject, new NullProgressMonitor());
		Assert.assertNotNull("Type not found", type);
		CompilationUnit compilationUnit = JdtUtils.parse(type, null);
		IMethod method = null;
		for (IMethod m : type.getMethods()) {
			if (m.getElementName().equals("getOrder")) {
				method = m;
				break;
			}
		}
		Assert.assertNotNull("ResourceMethod not found", method);
		MediaTypeCapabilities mediaTypesCapabilities = JAXRSAnnotationsScanner.resolveMediaTypeCapabilities(method,
				compilationUnit, Produces.class);
		Assert.assertEquals("Wrong result", 3, mediaTypesCapabilities.size());
		Assert.assertEquals("Wrong result", "application/vnd.bytesparadise.order+xml", mediaTypesCapabilities.get(0));
		Assert.assertEquals("Wrong result", "application/xml", mediaTypesCapabilities.get(1));
		Assert.assertEquals("Wrong result", "application/json", mediaTypesCapabilities.get(2));
	}

	@Test
	public void shouldResolvedDefinedMimeTypesOnTypeLevel() throws JavaModelException, CoreException {
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BookResource", javaProject,
				new NullProgressMonitor());
		Assert.assertNotNull("Type not found", type);
		CompilationUnit compilationUnit = JdtUtils.parse(type, null);
		MediaTypeCapabilities mediaTypesCapabilities = JAXRSAnnotationsScanner.resolveMediaTypeCapabilities(type,
				compilationUnit, Produces.class);
		Assert.assertEquals("Wrong result", 2, mediaTypesCapabilities.size());
		Assert.assertEquals("Wrong result", "application/xml", mediaTypesCapabilities.get(0));
		Assert.assertEquals("Wrong result", "application/json", mediaTypesCapabilities.get(1));
	}

	@Test
	public void shouldResolvedDefaultMimeTypesOnTypeLevel() throws JavaModelException, CoreException {
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BookResource", javaProject,
				new NullProgressMonitor());
		Assert.assertNotNull("Type not found", type);
		CompilationUnit compilationUnit = JdtUtils.parse(type, null);
		MediaTypeCapabilities mediaTypeCapabilities = JAXRSAnnotationsScanner.resolveMediaTypeCapabilities(type,
				compilationUnit, Consumes.class);
		Assert.assertTrue("Wrong result", mediaTypeCapabilities.isEmpty());
	}

	@Test
	public void shouldResolvedDefinedMimeTypesOnMethodLevel() throws JavaModelException, CoreException {
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BookResource", javaProject,
				new NullProgressMonitor());
		Assert.assertNotNull("Type not found", type);
		CompilationUnit compilationUnit = JdtUtils.parse(type, null);
		IMethod method = null;
		for (IMethod m : type.getMethods()) {
			if (m.getElementName().equals("getProduct")) {
				method = m;
				break;
			}
		}
		Assert.assertNotNull("ResourceMethod not found", method);
		MediaTypeCapabilities mediaTypesCapabilities = JAXRSAnnotationsScanner.resolveMediaTypeCapabilities(method,
				compilationUnit, Produces.class);
		Assert.assertEquals("Wrong result", "application/vnd.bytesparadise.book+xml", mediaTypesCapabilities.get(0));
		Assert.assertEquals("Wrong result", "application/xml", mediaTypesCapabilities.get(1));
		Assert.assertEquals("Wrong result", "application/json", mediaTypesCapabilities.get(2));
	}

	@Test
	public void shouldResolvedDefaultMimeTypesOnMethodLevel() throws JavaModelException, CoreException {
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BookResource", javaProject,
				new NullProgressMonitor());
		Assert.assertNotNull("Type not found", type);
		CompilationUnit compilationUnit = JdtUtils.parse(type, null);
		IMethod method = null;
		for (IMethod m : type.getMethods()) {
			if (m.getElementName().equals("getProduct")) {
				method = m;
				break;
			}
		}
		Assert.assertNotNull("ResourceMethod not found", method);
		MediaTypeCapabilities mediaTypeCapabilities = JAXRSAnnotationsScanner.resolveMediaTypeCapabilities(method,
				compilationUnit, Consumes.class);
		Assert.assertTrue("Wrong result", mediaTypeCapabilities.isEmpty());
	}

	@Test
	public void shouldStillResolvedMimeTypesAfterLibraryRemoved() throws JavaModelException, CoreException,
			OperationCanceledException, InterruptedException {
		NullProgressMonitor progressMonitor = new NullProgressMonitor();
		try {
			IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BookResource", javaProject,
					progressMonitor);
			Assert.assertNotNull("Type not found", type);
			CompilationUnit compilationUnit = JdtUtils.parse(type, null);
			boolean removed = WorkbenchTasks.removeReferencedLibrary(javaProject, "jaxrs-api-2.0.1.GA.jar",
					progressMonitor);
			Assert.assertTrue("Referenced library not removed", removed);
			MediaTypeCapabilities mediaTypesCapabilities = JAXRSAnnotationsScanner.resolveMediaTypeCapabilities(type,
					compilationUnit, Produces.class);
			// works even if the library was removed (does not take compilation
			// error into account)
			Assert.assertEquals("Wrong result", 2, mediaTypesCapabilities.size());
		} finally {
			WorkbenchTasks.addJavaProjectLibrary(javaProject, "jaxrs-api-2.0.1.GA.jar", progressMonitor);

		}
	}

}
