/******************************************************************************* 
 * Copyright (c) 2014-2016 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Mickael Istria (Red Hat Inc.) - initial implementation 
 ******************************************************************************/
package org.jboss.tools.ws.jaxrs.ui.internal.importer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.common.project.facet.core.libprov.ILibraryProvider;
import org.eclipse.jst.common.project.facet.core.libprov.LibraryInstallDelegate;
import org.eclipse.jst.common.project.facet.core.libprov.LibraryProviderFramework;
import org.eclipse.jst.ws.jaxrs.core.internal.IJAXRSCoreConstants;
import org.eclipse.jst.ws.jaxrs.core.internal.project.facet.IJAXRSFacetInstallDataModelProperties;
import org.eclipse.jst.ws.jaxrs.core.internal.project.facet.JAXRSFacetInstallDataModelProvider;
import org.eclipse.ui.wizards.datatransfer.ProjectConfigurator;
import org.eclipse.ui.wizards.datatransfer.RecursiveFileFinder;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.jboss.tools.ws.jaxrs.ui.JBossJaxrsUIPlugin;

public class JaxRsConfigurator implements ProjectConfigurator {

	@Override
	public boolean canConfigure(IProject project, Set<IPath> ignoredDirectories, IProgressMonitor monitor) {
		RecursiveFileFinder webXMLFinder = new RecursiveFileFinder("web.xml", ignoredDirectories);
		InputStream content = null;
		BufferedReader reader = null;
		try { 
			project.accept(webXMLFinder);
			IFile webXml = webXMLFinder.getFile();
			content = webXml.getContents();
			reader = new BufferedReader(new InputStreamReader(content, webXml.getCharset()));
			boolean found = false;
			String line = null;
			while (!found && (line = reader.readLine()) != null) {
				found |= line.contains("javax.ws.rs.Application");
				found |= line.contains("javax.ws.rs.core.Application");
			}
			return found;
		} catch (Exception ex) {
			return false;
		} finally {
			try {
				reader.close();
				content.close();
			} catch (IOException ex) {
				// annoying exception handling 
			}
		}
	}

	@Override
	public void configure(IProject project, Set<IPath> ignoredDirectories, IProgressMonitor monitor) {
		try {
			IFacetedProject facetedProject = ProjectFacetsManager.create(project, true, monitor);

			IProjectFacet JAXRS_FACET = ProjectFacetsManager.getProjectFacet("jst.jaxrs");
			if (!facetedProject.hasProjectFacet(JAXRS_FACET)) {
				IDataModel aFacetInstallDataModel = (IDataModel) new JAXRSFacetInstallDataModelProvider().create();
				LibraryInstallDelegate libraryDelegate = new LibraryInstallDelegate(facetedProject, JAXRS_FACET.getLatestVersion());
				ILibraryProvider provider = LibraryProviderFramework.getProvider(IJAXRSCoreConstants.NO_OP_LIBRARY_ID);
				libraryDelegate.setLibraryProvider(provider);
				aFacetInstallDataModel.setProperty(IJAXRSFacetInstallDataModelProperties.LIBRARY_PROVIDER_DELEGATE, libraryDelegate);
				facetedProject.installProjectFacet(JAXRS_FACET.getLatestVersion(), aFacetInstallDataModel, monitor);
			}
		} catch (Exception ex) {
			JBossJaxrsUIPlugin.getDefault().getLog().log(new Status(
					IStatus.ERROR,
					JBossJaxrsUIPlugin.PLUGIN_ID,
					ex.getMessage(),
					ex));
		}

	}

	@Override
	public boolean shouldBeAnEclipseProject(IContainer container, IProgressMonitor monitor) {
		return false; // TODO can we make sure a given directory is a jax-rs project?
	}

	@Override
	public Set<IFolder> getFoldersToIgnore(IProject project, IProgressMonitor monitor) {
		return null;
	}

	@Override
	public Set<File> findConfigurableLocations(File root, IProgressMonitor monitor) {
		// No easy way to deduce project roots from jee files...
		return Collections.EMPTY_SET;
	}
}
