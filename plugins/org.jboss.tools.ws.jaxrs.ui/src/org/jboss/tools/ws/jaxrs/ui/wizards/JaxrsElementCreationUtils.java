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

package org.jboss.tools.ws.jaxrs.ui.wizards;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage.ImportsManager;
import org.eclipse.jst.j2ee.model.ModelProviderManager;
import org.eclipse.jst.javaee.web.WebApp;
import org.eclipse.jst.javaee.web.WebAppDeploymentDescriptor;
import org.eclipse.jst.javaee.web.WebFactory;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsApplication;
import org.jboss.tools.ws.jaxrs.core.wtp.WtpUtils;
import org.jboss.tools.ws.jaxrs.ui.JBossJaxrsUIPlugin;

/**
 * Utility class for JAX-RS elements creation.
 * 
 * @author xcoulon
 *
 */
@SuppressWarnings("restriction")
public class JaxrsElementCreationUtils {

	/**
	 * Proivate constructor of the utility class
	 */
	private JaxrsElementCreationUtils() {

	}

	/**
	 * Computes the suggested package fragment from the given compilation unit.
	 * 
	 * @param compilationUnit
	 *            the {@link ICompilationUnit} to process
	 * @return {@link IPackageFragment} the suggested package fragment
	 */
	public static IPackageFragment getSuggestedPackage(final ICompilationUnit compilationUnit) {
		final IPackageFragment selectedPackage = (IPackageFragment) compilationUnit
				.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
		if(!selectedPackage.isDefaultPackage()) {
			try {
				final IPackageFragment parentPackageFragment = selectedPackage.getJavaProject().findPackageFragment(
						selectedPackage.getPath().removeLastSegments(1));
				return getSuggestedPackage(parentPackageFragment);
			} catch (JavaModelException e) {
				Logger.error("Failed to retrieve parent package for '" + selectedPackage.getElementName() + "'", e);
			}
		}
		return getSuggestedPackage(selectedPackage);
	}

	/**
	 * Computes the suggested package fragment from the given package fragment.
	 * 
	 * @param selectedPackage
	 *            the {@link IPackageFragment} to process
	 * @return {@link IPackageFragment} the suggested package fragment
	 */
	public static IPackageFragment getSuggestedPackage(final IPackageFragment selectedPackage) {
		final IPath selectedPackagePath = selectedPackage.getPath();
		final IPackageFragmentRoot selectedSourceFolder = (IPackageFragmentRoot) selectedPackage
				.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
		if (selectedPackage.isDefaultPackage()) {
			return selectedSourceFolder.getPackageFragment("rest");
		} else {
			final String suggestedPackageName = selectedPackagePath.append("rest")
					.makeRelativeTo(selectedSourceFolder.getPath()).toString().replace("/", ".");
			return selectedSourceFolder.getPackageFragment(suggestedPackageName);
		}
	}

	/**
	 * Suggests a name for the {@link IJaxrsApplication} based on the content of the given {@code suggestedPackage}: returns 'RestApplication' or if it exists, returns 'RestApplication1', etc.
	 * @param basePackage the base package
	 * @return the name of the application
	 */
	public static String getSuggestedApplicationTypeName(final IPackageFragment basePackage) {
		final String defaultTypeName = "RestApplication";
		try {
			int i = 1;
			final String packagePrefix = basePackage.getElementName() + ".";
			String typeFullyQualifiedName = packagePrefix + defaultTypeName;
			// try with typeFullyQualifiedName + suffix until no result is found
			while (basePackage.getJavaProject().findType(typeFullyQualifiedName) != null) {
				i++;
				typeFullyQualifiedName = packagePrefix + defaultTypeName + i;
			}
			return typeFullyQualifiedName.substring(packagePrefix.length());
		} catch (JavaModelException e) {
			Logger.error("Failed to check if project contains type '" + defaultTypeName + "'", e);
		}
		return null;
	}

	/**
	 * Returns the first {@link IPackageFragmentRoot} in the given {@link IJavaProject}, or {@code null} if none was found.
	 * @param javaProject
	 * @return the first {@link IPackageFragmentRoot} or {@code null}
	 * @throws JavaModelException
	 */
	public static IPackageFragmentRoot getFirstPackageFragmentRoot(final IJavaProject javaProject) throws JavaModelException {
		if (javaProject != null && javaProject.getPackageFragmentRoots().length > 0) {
			return javaProject.getPackageFragmentRoots()[0];
		}
		return null;
	}


	/**
	 * Adds the given annotation with the given values on the given member, and
	 * adds the given annotation name in the import declarations.
	 * 
	 * @param member
	 * @param annotationFullyQualifiedName
	 * @param annotationValues
	 * @param imports
	 * @throws JavaModelException
	 */
	public static void addAnnotation(final IMember member, final String annotationFullyQualifiedName,
			final List<String> annotationValues, final ImportsManager imports) throws JavaModelException {
		final ISourceRange sourceRange = member.getSourceRange();
		final ISourceRange javaDocRange = member.getJavadocRange();
		final IBuffer buf = member.getCompilationUnit().getBuffer();
		final String lineDelimiter = StubUtility.getLineDelimiterUsed(member.getJavaProject());
		final StringBuffer sb = new StringBuffer();
		final String annotationSimpleName = getSimpleName(annotationFullyQualifiedName);
		imports.addImport(annotationFullyQualifiedName);
		sb.append("@").append(annotationSimpleName);
		if (annotationValues != null && !annotationValues.isEmpty()) {
			sb.append('(');
			if (annotationValues.size() > 1) {
				sb.append('{');
			}
			for (Iterator<String> iterator = annotationValues.iterator(); iterator.hasNext();) {
				sb.append('"').append(iterator.next()).append('"');
				if (iterator.hasNext()) {
					sb.append(',');
				}
			}
			if (annotationValues.size() > 1) {
				sb.append('}');
			}
			sb.append(")");
		}
		sb.append(lineDelimiter);
		// insert the annotation ending with a line delimiter just before the
		// beginning of the code for the created type (but after the import
		// declarations)
		if(javaDocRange == null) {
			buf.replace(sourceRange.getOffset(), 0, sb.toString());
		} else {
			buf.replace(javaDocRange.getOffset() + javaDocRange.getLength() + lineDelimiter.length(), 0, sb.toString());
		}
		JavaModelUtil.reconcile(member.getCompilationUnit());
	}

	/**
	 * @param fullyQualifiedName
	 * @return the simple name associated with the given fully qualified name
	 */
	public static String getSimpleName(final String fullyQualifiedName) {
		final int i = fullyQualifiedName.lastIndexOf('.');
		final String annotationSimpleName = fullyQualifiedName.substring(i + 1);
		return annotationSimpleName;
	}

	/**
	 * Returns the {@link WebApp} associated with the given {@link IJavaProject}
	 * 
	 * @param javaProject
	 *            the java project
	 * @return the web app
	 * @throws CoreException 
	 */
	public static WebApp getWebApp(final IJavaProject javaProject) throws CoreException {
		if (WtpUtils.hasWebDeploymentDescriptor(javaProject.getProject())) {
			return (WebApp) ModelProviderManager.getModelProvider(javaProject.getProject()).getModelObject();
		}
		final WebAppDeploymentDescriptor deploymentDescriptor = WebFactory.eINSTANCE.createWebAppDeploymentDescriptor();
		final WebApp webApp = WebFactory.eINSTANCE.createWebApp();
		deploymentDescriptor.setWebApp(webApp);
		return webApp;
	}

	/**
	 * Retrieves the top-level package in the given {@link IPackageFragmentRoot} of an {@link IJavaProject}, ie, the deepest {@link IPackageFragment} which has no {@link ICompilationUnit} element.
	 * @param packageFragmentRoot the base packageFragmentRoot
	 * @return the top-level {@link IPackageFragment}
	 * @throws CoreException 
	 */
	public static IPackageFragment getProjectTopLevelPackage(final IPackageFragmentRoot packageFragmentRoot) throws CoreException {
		IFolder currentFolder = (IFolder) packageFragmentRoot.getResource();
		while(hasUniqueChildFolder(currentFolder)) {
			currentFolder = (IFolder) currentFolder.members()[0];
		}
		if(currentFolder != null) {
			return packageFragmentRoot.getJavaProject().findPackageFragment(currentFolder.getFullPath());
		}
		
		throw new CoreException(new Status(IStatus.ERROR, JBossJaxrsUIPlugin.PLUGIN_ID, "Cannot retrieve the currentFolder for the given packageFragmentRoot."));
	}
	
	/**
	 * Checks if the given {@code parentFolder} contains a single child element and that child element is an {@link IFolder} too.
	 * @param parentFolder the parent element (a {@link IFolder}) to analyze
	 * @return {@code true} if the folder contains a unique subfolder and nothing else, {@code false otherwise}.
	 * @throws CoreException 
	 */
	private static boolean hasUniqueChildFolder(final IFolder parentFolder) throws CoreException {
		if (parentFolder == null)
			return false;
		final IResource[] childElements = parentFolder.members();
		if(childElements.length == 1 && childElements[0].getType() == IResource.FOLDER) {
			return true;
		}
		return false;
	}

}
