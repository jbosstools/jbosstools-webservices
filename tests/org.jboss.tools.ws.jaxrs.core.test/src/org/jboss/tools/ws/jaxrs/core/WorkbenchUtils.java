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

package org.jboss.tools.ws.jaxrs.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkbenchUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkbenchUtils.class);

	@BeforeClass
	public static void setupWorkspace() throws Exception {
		setAutoBuild(ResourcesPlugin.getWorkspace(), false);
		if (JavaCore.getClasspathVariable("M2_REPO") == null) {
			String m2RepoPath = System.getProperty("user.home") + File.separator + ".m2" + File.separator
					+ "repository";
			AbstractCommonTestCase.LOGGER.info("Adding new Classpath variable entry for 'M2_REPO' bound to "
					+ m2RepoPath);
			JavaCore.setClasspathVariable("M2_REPO", new Path(m2RepoPath), new NullProgressMonitor());
		}
		// ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE,
		// new NullProgressMonitor());
		// removeSampleProject();
	}

	/**
	 * @throws CoreException
	 */
	public static void setAutoBuild(IWorkspace workspace, boolean value) throws CoreException {
		if (workspace.isAutoBuilding() != value) {
			IWorkspaceDescription description = workspace.getDescription();
			description.setAutoBuilding(value);
			workspace.setDescription(description);
		}
	}

	public static String retrieveSampleProjectName(Class<?> clazz) {
		RunWithProject annotation = clazz.getAnnotation(RunWithProject.class);
		while (annotation == null && clazz.getSuperclass() != null) {
			clazz = clazz.getSuperclass();
			annotation = clazz.getAnnotation(RunWithProject.class);
		}
		Assert.assertNotNull("Unable to locate @RunWithProject annotation", annotation);
		return annotation.value();

	}

	/**
	 * @return
	 * @throws JavaModelException
	 */
	public static IPackageFragment createPackage(IJavaProject javaProject, String pkgName) throws JavaModelException {
		IFolder folder = javaProject.getProject().getFolder("src/main/java");
		IPackageFragmentRoot packageFragmentRoot = javaProject.getPackageFragmentRoot(folder);
		IPackageFragment packageFragment = packageFragmentRoot.getPackageFragment(pkgName);
		if (!packageFragment.exists()) {
			packageFragment = packageFragmentRoot.createPackageFragment("org.jboss.tools.ws.jaxrs.sample", false,
					new NullProgressMonitor());
		}
		Assert.assertTrue("Target package does not exist", packageFragment.exists());
		return packageFragment;
	}

	public static IPath getSampleProjectPath(String projectName) {
		IPath path = null;
		if (System.getProperty("user.dir") != null) {
			path = new Path(System.getProperty("user.dir")).append("projects").append(projectName).makeAbsolute();
		} else {
			Assert.fail("The sample project was not found in the launcher workspace under name '" + projectName + "'");

		}
		WorkbenchTasks.LOGGER.debug(projectName + " path=" + path.toOSString());
		return path;
	}

	public static String getResourceContent(String name, Bundle bundle) {
		InputStream is = null;
		BufferedReader reader = null;
		try {
			// fail : wrong location : project is sample project, but should
			// look in test bundle...
			URL url = FileLocator.find(bundle, new Path("resources").append(name), null);
			is = url.openStream();
			if (is != null) {
				StringBuilder sb = new StringBuilder();
				String line;
				reader = new BufferedReader(new InputStreamReader(is, ResourcesPlugin.getEncoding()));
				while ((line = reader.readLine()) != null) {
					sb.append(line).append("\n");
				}
				return sb.toString();
			}

			Assert.fail("Failed to locate file from path " + name);
		} catch (Exception e) {
			Assert.fail("Failed to retrieve file content from " + name + ": " + e.getMessage());
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
				}
			}
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {

				}
			}
		}
		return null;
	}

	/**
	 * Create a compilation unit from the given filename content, in the given
	 * package, with the given name
	 * 
	 * @param fileName
	 *            the filename containing the source code, in the /resources
	 *            folder of the test bundle, or null if the created compilation
	 *            unit must remain empty after its creation
	 * @param pkg
	 *            the target package
	 * @param unitName
	 *            the target compilation unit name
	 * @return the created compilation unit
	 * @throws JavaModelException
	 */
	public static ICompilationUnit createCompilationUnit(IJavaProject javaProject, String fileName, String pkg,
			String unitName, Bundle bundle) throws JavaModelException {
		String contents = "";
		if (fileName != null) {
			contents = getResourceContent(fileName, bundle);
		}
		IPackageFragmentRoot sourceFolder = javaProject.findPackageFragmentRoot(javaProject.getProject().getFullPath()
				.append("src/main/java"));
		IPackageFragment packageFragment = sourceFolder.getPackageFragment(pkg);
		ICompilationUnit foocompilationUnit = packageFragment.createCompilationUnit(unitName, contents, true,
				new NullProgressMonitor());
		saveAndClose(foocompilationUnit);
		return foocompilationUnit;
	}

	public static void appendCompilationUnitType(ICompilationUnit compilationUnit, String resourceName, Bundle bundle)
			throws CoreException {
		IType type = compilationUnit.findPrimaryType();
		Assert.assertTrue("Type does not exist", type.exists());
		int offset = type.getSourceRange().getOffset() + type.getSourceRange().getLength();
		String content = getResourceContent(resourceName, bundle);
		insertCodeAtLocation(type, content, offset);

	}

	private static void insertCodeAtLocation(IType type, String content, int offset) throws CoreException {
		ICompilationUnit compilationUnit = type.getCompilationUnit();
		ICompilationUnit workingCopy = createWorkingCopy(compilationUnit);
		IBuffer buffer = ((IOpenable) workingCopy).getBuffer();
		buffer.replace(offset, 0, content + "\n"); // append a new line at the
													// same time
		saveAndClose(workingCopy);
		String subSource = type.getCompilationUnit().getSource().substring(offset, offset + content.length());
		Assert.assertEquals("Content was not inserted", content, subSource);
	}

	/**
	 * Removes the first occurrence of the given content (not a regexp)
	 * 
	 * @param type
	 * @param content
	 * @throws JavaModelException
	 */
	public static void removeFirstOccurrenceOfCode(IType type, String content) throws JavaModelException {
		replaceFirstOccurrenceOfCode(type, content, "");
	}

	/**
	 * Replaces the first occurrence of the given old content (not a regexp)
	 * with the given new content
	 * 
	 * @param compilationUnit
	 * @param oldContent
	 * @param newContent
	 * @throws JavaModelException
	 */
	public static void replaceFirstOccurrenceOfCode(IType type, String oldContent, String newContent)
			throws JavaModelException {
		replaceFirstOccurrenceOfCode(type.getCompilationUnit(), oldContent, newContent);
	}

	public static void replaceFirstOccurrenceOfCode(ICompilationUnit compilationUnit, String oldContent,
			String newContent) throws JavaModelException {
		ICompilationUnit workingCopy = createWorkingCopy(compilationUnit);
		IBuffer buffer = ((IOpenable) workingCopy).getBuffer();
		int offset = buffer.getContents().indexOf(oldContent);
		Assert.assertTrue("Old content not found", offset != -1);
		buffer.replace(offset, oldContent.length(), newContent);
		saveAndClose(workingCopy);
	}

	public static void replaceFirstOccurrenceOfCode(ICompilationUnit compilationUnit, String[] oldContents,
			String[] newContents) throws JavaModelException {
		ICompilationUnit workingCopy = createWorkingCopy(compilationUnit);
		Assert.assertEquals("Wrong parameters", oldContents.length, newContents.length);
		for (int i = 0; i < oldContents.length; i++) {
			IBuffer buffer = ((IOpenable) workingCopy).getBuffer();
			int offset = buffer.getContents().indexOf(oldContents[i]);
			Assert.assertTrue("Old content not found", offset != -1);
			buffer.replace(offset, oldContents[i].length(), newContents[i]);
		}
		saveAndClose(workingCopy);
	}

	public static void replaceFirstOccurrenceOfCode(IMethod method, String oldContent, String newContent)
			throws JavaModelException {
		ICompilationUnit workingCopy = createWorkingCopy(method.getCompilationUnit());
		ISourceRange sourceRange = method.getSourceRange();
		IBuffer buffer = ((IOpenable) workingCopy).getBuffer();
		int offset = buffer.getContents().indexOf(oldContent, sourceRange.getOffset());
		Assert.assertTrue("Old content not found: '" + oldContent + "'", offset != -1);
		buffer.replace(offset, oldContent.length(), newContent);
		saveAndClose(workingCopy);
	}

	public static void replaceAllOccurrencesOfCode(ICompilationUnit compilationUnit, String oldContent,
			String newContent) throws JavaModelException {
		ICompilationUnit workingCopy = createWorkingCopy(compilationUnit);
		IBuffer buffer = ((IOpenable) workingCopy).getBuffer();
		int offset = 0;
		while ((offset = buffer.getContents().indexOf(oldContent)) != -1) {
			buffer.replace(offset, oldContent.length(), newContent);
		}
		saveAndClose(workingCopy);
	}

	public static void addImport(IType type, String name) throws JavaModelException {
		LOGGER.debug("Adding import " + name);
		ICompilationUnit compilationUnit = type.getCompilationUnit();
		addImport(compilationUnit, name);
	}

	public static void addImport(ICompilationUnit compilationUnit, String name) throws JavaModelException {
		LOGGER.debug("Adding import " + name);
		NullProgressMonitor monitor = new NullProgressMonitor();
		ICompilationUnit workingCopy = createWorkingCopy(compilationUnit);
		workingCopy.createImport(name, null, monitor);
		saveAndClose(workingCopy);
	}

	public static void removeImport(ICompilationUnit compilationUnit, String name) throws JavaModelException {
		LOGGER.debug("Removing import " + name);
		IProgressMonitor monitor = new NullProgressMonitor();
		ICompilationUnit workingCopy = createWorkingCopy(compilationUnit);
		workingCopy.getImport(name).delete(true, monitor);
		saveAndClose(workingCopy);
	}

	public static void modifyImport(ICompilationUnit compilationUnit, String oldImport, String newImport)
			throws JavaModelException {
		LOGGER.debug("Modifying import " + oldImport + " -> " + newImport);
		String oldImportStmt = "import " + oldImport;
		String newImportStmt = "import " + newImport;
		ICompilationUnit workingCopy = createWorkingCopy(compilationUnit);
		IBuffer buffer = ((IOpenable) workingCopy).getBuffer();
		int offset = buffer.getContents().indexOf(oldImportStmt);
		buffer.replace(offset, oldImportStmt.length(), newImportStmt);
		saveAndClose(workingCopy);
	}

	public static void removeMethod(ICompilationUnit compilationUnit, String methodName) throws JavaModelException {
		LOGGER.debug("Removing method " + methodName);
		ICompilationUnit workingCopy = createWorkingCopy(compilationUnit);
		for (IMethod method : compilationUnit.findPrimaryType().getMethods()) {
			if (method.getElementName().equals(methodName)) {
				ISourceRange sourceRange = method.getSourceRange();
				IBuffer buffer = ((IOpenable) workingCopy).getBuffer();
				buffer.replace(sourceRange.getOffset(), sourceRange.getLength(), "");
				saveAndClose(workingCopy);
				return;
			}
		}
		Assert.fail("Method not found.");
	}

	public static void removeMethod(IType javaType, String methodName) throws JavaModelException {
		LOGGER.info("Removing method " + javaType.getElementName() + "." + methodName + "(...)");
		removeMethod(javaType.getCompilationUnit(), methodName);
	}

	public static IMethod addMethod(IType javaType, String contents) throws JavaModelException {
		LOGGER.info("Adding method into type " + javaType.getElementName());
		ICompilationUnit workingCopy = createWorkingCopy(javaType.getCompilationUnit());
		ISourceRange sourceRange = javaType.getMethods()[0].getSourceRange();
		IBuffer buffer = ((IOpenable) workingCopy).getBuffer();
		// insert before 1 method
		buffer.replace(sourceRange.getOffset(), 0, contents);
		saveAndClose(workingCopy);
		// return the last method of the java type, assuming it is the one given
		// in parameter
		return javaType.getMethods()[0];
	}

	public static void addMethodAnnotation(IMethod method, String annotation) throws JavaModelException {
		ICompilationUnit compilationUnit = method.getCompilationUnit();
		ICompilationUnit workingCopy = createWorkingCopy(compilationUnit);
		ISourceRange sourceRange = method.getSourceRange();
		IBuffer buffer = ((IOpenable) workingCopy).getBuffer();
		buffer.replace(sourceRange.getOffset(), 0, annotation + "\n");
		saveAndClose(workingCopy);
		return;
	}

	public static void addTypeAnnotation(IType type, String annotation) throws JavaModelException, CoreException {
		LOGGER.info("Adding annotation " + annotation + " on type " + type.getElementName());
		insertCodeAtLocation(type, annotation, type.getSourceRange().getOffset());
	}

	public static void removeMethodAnnotation(IType javaType, String methodName, String annotation)
			throws JavaModelException {
		LOGGER.info("Removing annotation " + annotation + " on " + javaType.getElementName() + "." + methodName
				+ "(...)");
		ICompilationUnit compilationUnit = javaType.getCompilationUnit();
		ICompilationUnit workingCopy = createWorkingCopy(compilationUnit);
		for (IMethod method : compilationUnit.findPrimaryType().getMethods()) {
			if (method.getElementName().equals(methodName)) {
				ISourceRange sourceRange = method.getSourceRange();
				IBuffer buffer = ((IOpenable) workingCopy).getBuffer();
				int index = buffer.getContents().indexOf(annotation, sourceRange.getOffset());
				Assert.assertTrue(
						"Annotation not found",
						(index >= sourceRange.getOffset())
								&& (index <= sourceRange.getOffset() + sourceRange.getLength()));
				buffer.replace(index, annotation.length(), "");
				saveAndClose(workingCopy);
				return;
			}
		}
		Assert.fail("Method not found.");
	}

	public static void removeMethodAnnotation(IMethod method, String annotation) throws JavaModelException {
		ICompilationUnit compilationUnit = method.getCompilationUnit();
		ICompilationUnit workingCopy = createWorkingCopy(compilationUnit);
		ISourceRange sourceRange = method.getSourceRange();
		IBuffer buffer = ((IOpenable) workingCopy).getBuffer();
		int index = buffer.getContents().indexOf(annotation, sourceRange.getOffset());
		Assert.assertTrue("Annotation not found: '" + annotation + "'", (index >= sourceRange.getOffset())
				&& (index <= sourceRange.getOffset() + sourceRange.getLength()));
		buffer.replace(index, annotation.length(), "");
		saveAndClose(workingCopy);
	}

	public static void addMethodParameter(IMethod method, String parameter) throws JavaModelException {
		ICompilationUnit compilationUnit = method.getCompilationUnit();
		ICompilationUnit workingCopy = createWorkingCopy(compilationUnit);
		ISourceRange sourceRange = method.getSourceRange();
		IBuffer buffer = ((IOpenable) workingCopy).getBuffer();
		String[] parameterNames = method.getParameterNames();
		int offset = buffer.getContents().indexOf("public", sourceRange.getOffset());
		int index = buffer.getContents().indexOf("(", offset);
		if (parameterNames.length == 0) {
			buffer.replace(index + 1, 0, parameter);
		} else {
			buffer.replace(index + 1, 0, parameter + ",");
		}
		saveAndClose(workingCopy);
	}

	/**
	 * @param compilationUnit
	 * @return
	 * @throws JavaModelException
	 */
	private static ICompilationUnit createWorkingCopy(ICompilationUnit compilationUnit) throws JavaModelException {
		LOGGER.debug("Creating working copy...");
		ICompilationUnit workingCopy = compilationUnit.getWorkingCopy(new NullProgressMonitor());
		LOGGER.debug("Working copy created.");
		return workingCopy;
	}

	/**
	 * @param compilationUnit
	 * @throws JavaModelException
	 */
	private static void saveAndClose(ICompilationUnit compilationUnit) throws JavaModelException {
		try {
			if (compilationUnit.isWorkingCopy()) {
				LOGGER.debug("Reconciling unit...");
				compilationUnit.reconcile(ICompilationUnit.NO_AST, false, null, null);
				// Commit changes
				LOGGER.debug("Commiting changes...");
				compilationUnit.commitWorkingCopy(false, null);
				// Destroy working copy
				LOGGER.debug("Discarding working copy...");
				compilationUnit.discardWorkingCopy();
			}
			// explicitly trigger the project build
			compilationUnit.getJavaProject().getProject().build(IncrementalProjectBuilder.AUTO_BUILD, null);

		} catch (Exception e) {
			LOGGER.error("Failed to build project", e);
		}
	}

	public static void move(ICompilationUnit compilationUnit, String targetPackageName, Bundle bundle)
			throws CoreException {
		/*
		 * ICompilationUnit destContainer =
		 * createCompilationUnit(compilationUnit.getJavaProject(), null,
		 * targetPackageName, compilationUnit.getElementName(), bundle);
		 * MoveElementsOperation operation = new MoveElementsOperation( new
		 * IJavaElement[] { compilationUnit }, new IJavaElement[] {
		 * destContainer }, true); operation.run(new NullProgressMonitor());
		 */
		IPackageFragment packageFragment = WorkbenchUtils.createPackage(compilationUnit.getJavaProject(),
				"org.jboss.tools.ws.jaxrs.sample");
		compilationUnit.move(packageFragment, null, compilationUnit.getElementName(), false, new NullProgressMonitor());

		saveAndClose(compilationUnit);
	}

	public static void delete(ICompilationUnit compilationUnit) throws CoreException {
		compilationUnit.delete(true, new NullProgressMonitor());
		saveAndClose(compilationUnit);
	}

	/*
	 * public static CompilationUnitEditor getCompilationUnitEditor(IFile file)
	 * throws PartInitException { IEditorPart editorPart = null;
	 * PlatformUI.isWorkbenchRunning(); PlatformUI.getWorkbench().isStarting();
	 * IWorkbenchPage page =
	 * PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
	 * editorPart = page.openEditor(new FileEditorInput(file),
	 * IDE.getEditorDescriptor(file).getId()); return (CompilationUnitEditor)
	 * editorPart; }
	 */

	public static void delete(IType type) throws JavaModelException {
		ICompilationUnit compilationUnit = type.getCompilationUnit();
		type.delete(true, new NullProgressMonitor());
		saveAndClose(compilationUnit);
	}

	public static void rename(ICompilationUnit compilationUnit, String name) throws JavaModelException {
		compilationUnit.rename(name, true, new NullProgressMonitor());
		saveAndClose(compilationUnit);
	}

	public static void rename(IMethod method, String name) throws JavaModelException {
		method.rename(name, true, new NullProgressMonitor());
		saveAndClose(method.getCompilationUnit());
	}

}
