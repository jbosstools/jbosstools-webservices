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

package org.jboss.tools.ws.jaxrs.core.junitrules;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.wizards.datatransfer.IImportStructureProvider;
import org.jboss.tools.ws.jaxrs.core.internal.utils.TestLogger;

/**
 * Copy of the org.eclipse.ui.wizards.datatransfer.FileSystemStructureProvider
 * with the following changes : - Removed the singleton nature of the original
 * class - Only returns modified files between source and destination
 * directories
 * 
 * @author xcoulon
 * 
 */
@SuppressWarnings("restriction")
public class SyncFileSystemStructureProvider implements IImportStructureProvider {

	private final IPath source;

	private final IPath destination;

	private final List<String> ignoredRelativePaths = new ArrayList<String>();

	public Object ignoreRelativeSourcePaths;

	/**
	 * Creates an instance of <code>SyncFileSystemStructureProvider</code>.
	 * 
	 * @param source
	 */
	public SyncFileSystemStructureProvider(IPath source, IPath destination) {
		super();
		this.source = source;
		this.destination = destination;
	}

	/*
	 * (non-Javadoc) Method declared on IImportStructureProvider
	 */
	@Override
	public List<File> getChildren(Object element) {
		File folder = (File) element;
		String[] children = folder.list();
		int childrenLength = children == null ? 0 : children.length;
		List<File> result = new ArrayList<File>(childrenLength);

		for (int i = 0; i < childrenLength; i++) {
			File sourceFile = new File(folder, children[i]);
			IPath relativeSourcePath = new Path(sourceFile.getAbsolutePath()).makeRelativeTo(source);
			// always add the sub directories
			if (ignoredRelativePaths.contains(relativeSourcePath.lastSegment())) {
				continue;
			}

			if (sourceFile.isDirectory() && !ignoredRelativePaths.contains(relativeSourcePath.lastSegment())) {
				result.addAll(getChildren(sourceFile));
			}
			// only add the other files if they are missing or were modified in
			// the destination destination
			else {
				IPath relativeDestinationPath = destination.append(relativeSourcePath);
				File destinationFile = new File(relativeDestinationPath.toOSString());
				if (!destinationFile.exists() || destinationFile.lastModified() > sourceFile.lastModified()) {
					TestLogger.debug("Adding '" + relativeSourcePath + "' to sync operation.");
					result.add(sourceFile);
				}
			}
		}

		return result;
	}

	/*
	 * (non-Javadoc) Method declared on IImportStructureProvider
	 */
	@Override
	public InputStream getContents(Object element) {
		try {
			return new FileInputStream((File) element);
		} catch (FileNotFoundException e) {
			IDEWorkbenchPlugin.log(e.getLocalizedMessage(), e);
			return null;
		}
	}

	/*
	 * (non-Javadoc) Method declared on IImportStructureProvider
	 */
	@Override
	public String getFullPath(Object element) {
		return ((File) element).getPath();
	}

	/*
	 * (non-Javadoc) Method declared on IImportStructureProvider
	 */
	@Override
	public String getLabel(Object element) {

		// Get the name - if it is empty then return the path as it is a file
		// root
		File file = (File) element;
		String name = file.getName();
		if (name.length() == 0) {
			return file.getPath();
		}
		return name;
	}

	/*
	 * (non-Javadoc) Method declared on IImportStructureProvider
	 */
	@Override
	public boolean isFolder(Object element) {
		return ((File) element).isDirectory();
	}

	public void ignoreRelativeSourcePath(String relativePath) {
		this.ignoredRelativePaths.add(relativePath);

	}

	public void ignoreRelativeSourcePaths(String... relativePaths) {
		this.ignoredRelativePaths.addAll(Arrays.asList(relativePaths));
		
	}
}
