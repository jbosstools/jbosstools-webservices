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

package org.jboss.tools.ws.jaxrs.ui.wizards;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage.ImportsManager;
import org.eclipse.jst.j2ee.model.ModelProviderManager;
import org.eclipse.jst.javaee.web.WebApp;
import org.eclipse.jst.javaee.web.WebAppDeploymentDescriptor;
import org.eclipse.jst.javaee.web.WebFactory;
import org.jboss.tools.ws.jaxrs.core.wtp.WtpUtils;

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
		return getSuggestedPackage(selectedPackage);
	}

	/**
	 * Computes the suggested package fragment from the given package fragment.
	 * 
	 * @param compilationUnit
	 *            the {@link ICompilationUnit} to process
	 * @return {@link IPackageFragment} the suggested package fragment
	 */
	public static IPackageFragment getSuggestedPackage(final IPackageFragment selectedPackage) {
		final IPath selectedPackagePath = selectedPackage.getPath();
		final IPackageFragmentRoot selectedSourceFolder = (IPackageFragmentRoot) selectedPackage
				.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
		if (selectedPackage.isDefaultPackage()) {
			return selectedSourceFolder.getPackageFragment("rest");
		} else {
			final String suggestedPackageName = selectedPackagePath.removeLastSegments(1).append("rest")
					.makeRelativeTo(selectedSourceFolder.getPath()).toString().replace("/", ".");
			return selectedSourceFolder.getPackageFragment(suggestedPackageName);
		}
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
		final ISourceRange range = member.getSourceRange();
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
		buf.replace(range.getOffset(), 0, sb.toString());
	}

	/**
	 * @param annotationFullyQualifiedName
	 * @return
	 */
	public static String getSimpleName(final String annotationFullyQualifiedName) {
		final int i = annotationFullyQualifiedName.lastIndexOf('.');
		final String annotationSimpleName = annotationFullyQualifiedName.substring(i + 1);
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

}
