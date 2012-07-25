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

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IProblemRequestor;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.ui.internal.wizards.datatransfer.DataTransferMessages;
import org.jboss.tools.ws.jaxrs.core.internal.utils.WtpUtils;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
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

	/** @throws CoreException */
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
	 * Create a compilation unit from the given filename content, in the given package, with the given name
	 * 
	 * @param fileName
	 *            the filename containing the source code, in the /resources folder of the test bundle, or null if the
	 *            created compilation unit must remain empty after its creation
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

	public static IType appendCompilationUnitType(ICompilationUnit compilationUnit, String resourceName, Bundle bundle,
			boolean useWorkingCopy) throws CoreException {
		String content = getResourceContent(resourceName, bundle);
		int offset = 0;
		IType lastType = getLastTypeInSource(compilationUnit);
		if (lastType != null) {
			offset = lastType.getSourceRange().getOffset() + lastType.getSourceRange().getLength();
		}
		insertCodeAtLocation(compilationUnit, content, offset, useWorkingCopy);
		return getLastTypeInSource(compilationUnit);
	}

	private static IType getLastTypeInSource(ICompilationUnit compilationUnit) throws JavaModelException {
		IType[] types = compilationUnit.getTypes();
		if (types != null && types.length > 0) {
			return types[types.length - 1];
		}
		return null;
	}

	private static void insertCodeAtLocation(ICompilationUnit compilationUnit, String content, int offset,
			boolean useWorkingCopy) throws CoreException {
		ICompilationUnit unit = getCompilationUnit(compilationUnit, useWorkingCopy);
		IBuffer buffer = ((IOpenable) unit).getBuffer();
		buffer.replace(offset, 0, content + "\n"); // append a new line at the
													// same time
		saveAndClose(unit);
		String subSource = compilationUnit.getSource().substring(offset, offset + content.length());
		Assert.assertEquals("Content was not inserted", content, subSource);
	}

	/**
	 * Removes the first occurrence of the given content (not a regexp)
	 * 
	 * @param type
	 * @param content
	 * @throws JavaModelException
	 */
	public static void removeFirstOccurrenceOfCode(IType type, String content, boolean useWorkingCopy)
			throws JavaModelException {
		replaceFirstOccurrenceOfCode(type, content, "", useWorkingCopy);
	}

	public static void replaceFirstOccurrenceOfCode(ICompilationUnit compilationUnit, String oldContent,
			String newContent, boolean useWorkingCopy) throws JavaModelException {
		ICompilationUnit unit = getCompilationUnit(compilationUnit, useWorkingCopy);
		IBuffer buffer = ((IOpenable) unit).getBuffer();
		int offset = buffer.getContents().indexOf(oldContent);
		Assert.assertTrue("Old content not found", offset != -1);
		buffer.replace(offset, oldContent.length(), newContent);
		saveAndClose(unit);
	}

	/**
	 * @param compilationUnit
	 * @param useWorkingCopy
	 * @return
	 * @throws JavaModelException
	 */
	public static ICompilationUnit getCompilationUnit(ICompilationUnit compilationUnit, boolean useWorkingCopy)
			throws JavaModelException {
		return useWorkingCopy ? createWorkingCopy(compilationUnit) : compilationUnit;
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

	public static <T extends IMember> T replaceFirstOccurrenceOfCode(T member, String oldContent, String newContent,
			boolean useWorkingCopy) throws JavaModelException {
		ICompilationUnit compilationUnit = member.getCompilationUnit();
		ICompilationUnit unit = useWorkingCopy ? createWorkingCopy(compilationUnit) : member.getCompilationUnit();
		ISourceRange sourceRange = member.getSourceRange();
		IBuffer buffer = ((IOpenable) unit).getBuffer();
		int offset = buffer.getContents().indexOf(oldContent, sourceRange.getOffset());
		Assert.assertTrue("Old content not found: '" + oldContent + "'", offset != -1);
		buffer.replace(offset, oldContent.length(), newContent);
		// IJavaElement modifiedMethod =
		// workingCopy.getElementAt(sourceRange.getOffset());
		saveAndClose(unit);
		@SuppressWarnings("unchecked")
		T modifiedElement = (T) compilationUnit.getElementAt(sourceRange.getOffset());
		return modifiedElement;
	}

	public static void replaceAllOccurrencesOfCode(ICompilationUnit compilationUnit, String oldContent,
			String newContent, boolean useWorkingCopy) throws JavaModelException {

		ICompilationUnit unit = getCompilationUnit(compilationUnit, useWorkingCopy);
		IBuffer buffer = ((IOpenable) unit).getBuffer();
		int offset = 0;
		while ((offset = buffer.getContents().indexOf(oldContent, offset)) != -1) {
			buffer.replace(offset, oldContent.length(), newContent);
			offset = offset + newContent.length();

		}
		saveAndClose(unit);
	}

	public static void createImport(IType type, String name) throws JavaModelException {
		LOGGER.debug("Adding import " + name);
		ICompilationUnit compilationUnit = type.getCompilationUnit();
		createImport(compilationUnit, name);
	}

	public static void createImport(ICompilationUnit compilationUnit, String name) throws JavaModelException {
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

	public static IMethod removeMethod(ICompilationUnit compilationUnit, String methodName, boolean useWorkingCopy)
			throws JavaModelException {
		LOGGER.debug("Removing method " + methodName);
		ICompilationUnit unit = getCompilationUnit(compilationUnit, useWorkingCopy);
		for (IMethod method : unit.findPrimaryType().getMethods()) {
			if (method.getElementName().equals(methodName)) {
				ISourceRange sourceRange = method.getSourceRange();
				IBuffer buffer = ((IOpenable) unit).getBuffer();
				buffer.replace(sourceRange.getOffset(), sourceRange.getLength(), "");
				saveAndClose(unit);
				return method;
			}
		}
		Assert.fail("Method not found.");
		return null;
	}

	public static void removeMethod(IType javaType, String methodName, boolean useWorkingCopy)
			throws JavaModelException {
		LOGGER.info("Removing method " + javaType.getElementName() + "." + methodName + "(...)");
		removeMethod(javaType.getCompilationUnit(), methodName, useWorkingCopy);
	}

	public static void removeType(IType type, boolean useWorkingCopy) throws JavaModelException {
		LOGGER.info("Removing type " + type.getElementName() + "...");
		ICompilationUnit unit = getCompilationUnit(type.getCompilationUnit(), useWorkingCopy);
		unit.getType(type.getElementName()).delete(true, new NullProgressMonitor());
		saveAndClose(unit);
	}

	public static IMethod createMethod(IType javaType, String contents, boolean useWorkingCopy)
			throws JavaModelException {
		LOGGER.info("Adding method into type " + javaType.getElementName());
		ICompilationUnit unit = javaType.getCompilationUnit();
		if (useWorkingCopy) {
			unit = createWorkingCopy(unit);
		}
		ISourceRange sourceRange = javaType.getMethods()[0].getSourceRange();
		IBuffer buffer = ((IOpenable) unit).getBuffer();
		// insert before 1 method
		buffer.replace(sourceRange.getOffset(), 0, contents);
		saveAndClose(unit);
		// return the last method of the java type, assuming it is the one given
		// in parameter
		return javaType.getMethods()[0];
	}

	public static IField createField(IType type, String contents, boolean useWorkingCopy) throws JavaModelException {
		LOGGER.info("Adding type into type " + type.getElementName());
		ICompilationUnit unit = useWorkingCopy ? createWorkingCopy(type.getCompilationUnit()) : type
				.getCompilationUnit();
		ISourceRange sourceRange = type.getFields()[0].getSourceRange();
		IBuffer buffer = ((IOpenable) unit).getBuffer();
		// insert before 1 method
		buffer.replace(sourceRange.getOffset(), 0, contents);
		saveAndClose(unit);
		// return the last method of the java type, assuming it is the one given
		// in parameter
		return type.getFields()[0];
	}

	public static IField removeField(IField field, boolean useWorkingCopy) throws JavaModelException {
		LOGGER.info("Removing field " + field.getElementName());
		ICompilationUnit unit = useWorkingCopy ? createWorkingCopy(field.getCompilationUnit()) : field
				.getCompilationUnit();
		ISourceRange sourceRange = field.getSourceRange();
		IBuffer buffer = ((IOpenable) unit).getBuffer();
		// remove
		buffer.replace(sourceRange.getOffset(), sourceRange.getLength(), "");
		saveAndClose(unit);
		// return the last method of the java type, assuming it is the one given
		// in parameter
		return field;

	}

	/**
	 * @param type
	 * @return
	 * @throws JavaModelException
	 */
	public static IMethod getMethod(IType type, String name) throws JavaModelException {
		for (IMethod method : type.getMethods()) {
			if (method.getElementName().equals(name)) {
				return method;
			}
		}
		Assert.fail("Failed to locate method named '" + name + "'");
		return null;
	}

	public static IAnnotation addMethodAnnotation(IMethod method, String annotationStmt, boolean useWorkingCopy)
			throws JavaModelException {
		ICompilationUnit compilationUnit = method.getCompilationUnit();
		ICompilationUnit unit = getCompilationUnit(compilationUnit, useWorkingCopy);
		ISourceRange sourceRange = method.getSourceRange();
		IBuffer buffer = ((IOpenable) unit).getBuffer();
		buffer.replace(sourceRange.getOffset(), 0, annotationStmt + "\n");
		saveAndClose(unit);
		method = (IMethod) compilationUnit.getElementAt(method.getSourceRange().getOffset());
		String annotationName = StringUtils.substringBetween(annotationStmt, "@", "(");
		for (IAnnotation annotation : method.getAnnotations()) {
			if (annotation.getElementName().equals(annotationName)) {
				return annotation;
			}
		}
		Assert.fail("SimpleAnnotation '" + annotationName + "'not found on method " + method.getSource());
		return null;
	}

	public static IAnnotation addTypeAnnotation(IType type, String annotationStmt, boolean useWorkingCopy)
			throws JavaModelException, CoreException {
		LOGGER.info("Adding annotation " + annotationStmt + " on type " + type.getElementName());
		insertCodeAtLocation(type.getCompilationUnit(), annotationStmt, type.getSourceRange().getOffset(),
				useWorkingCopy);
		String annotationName = StringUtils.substringBetween(annotationStmt, "@", "(");
		for (IAnnotation annotation : type.getAnnotations()) {
			if (annotation.getElementName().equals(annotationName)) {
				return annotation;
			}
		}
		return null;
	}

	public static void removeMethodAnnotation(IType javaType, String methodName, String annotationStmt)
			throws JavaModelException {
		LOGGER.info("Removing annotation " + annotationStmt + " on " + javaType.getElementName() + "." + methodName
				+ "(...)");
		ICompilationUnit compilationUnit = javaType.getCompilationUnit();
		ICompilationUnit workingCopy = createWorkingCopy(compilationUnit);
		for (IMethod method : compilationUnit.findPrimaryType().getMethods()) {
			if (method.getElementName().equals(methodName)) {
				ISourceRange sourceRange = method.getSourceRange();
				IBuffer buffer = ((IOpenable) workingCopy).getBuffer();
				int index = buffer.getContents().indexOf(annotationStmt, sourceRange.getOffset());
				Assert.assertTrue("SimpleAnnotation not found", (index >= sourceRange.getOffset())
						&& (index <= sourceRange.getOffset() + sourceRange.getLength()));
				buffer.replace(index, annotationStmt.length(), "");
				saveAndClose(workingCopy);
				return;
			}
		}
		Assert.fail("Method not found.");
	}

	public static void removeMethodAnnotation(IMethod method, String annotationStmt) throws JavaModelException {
		ICompilationUnit compilationUnit = method.getCompilationUnit();
		ICompilationUnit workingCopy = createWorkingCopy(compilationUnit);
		ISourceRange sourceRange = method.getSourceRange();
		IBuffer buffer = ((IOpenable) workingCopy).getBuffer();
		int index = buffer.getContents().indexOf(annotationStmt, sourceRange.getOffset());
		Assert.assertTrue("SimpleAnnotation not found: '" + annotationStmt + "'", (index >= sourceRange.getOffset())
				&& (index <= sourceRange.getOffset() + sourceRange.getLength()));
		buffer.replace(index, annotationStmt.length(), "");
		saveAndClose(workingCopy);
	}

	public static IAnnotation removeMethodAnnotation(IMethod method, IAnnotation annotation, boolean useWorkingCopy)
			throws JavaModelException {
		ICompilationUnit compilationUnit = method.getCompilationUnit();
		ICompilationUnit unit = getCompilationUnit(compilationUnit, useWorkingCopy);
		ISourceRange sourceRange = annotation.getSourceRange();
		IBuffer buffer = ((IOpenable) unit).getBuffer();
		buffer.replace(sourceRange.getOffset(), sourceRange.getLength(), "");
		saveAndClose(unit);
		return annotation;
	}

	public static IAnnotation addFieldAnnotation(IField field, String annotationStmt, boolean useWorkingCopy)
			throws CoreException {
		LOGGER.info("Adding annotation " + annotationStmt + " on type " + field.getElementName());
		insertCodeAtLocation(field.getCompilationUnit(), annotationStmt, field.getSourceRange().getOffset(),
				useWorkingCopy);
		String annotationName = StringUtils.substringBetween(annotationStmt, "@", "(");
		for (IAnnotation annotation : field.getAnnotations()) {
			if (annotation.getElementName().equals(annotationName)) {
				return annotation;
			}
		}
		return null;
	}

	public static void removeFieldAnnotation(IField field, String annotationStmt, boolean useWorkingCopy)
			throws JavaModelException {
		ICompilationUnit compilationUnit = field.getCompilationUnit();
		ICompilationUnit unit = getCompilationUnit(compilationUnit, useWorkingCopy);
		ISourceRange sourceRange = field.getSourceRange();
		IBuffer buffer = ((IOpenable) unit).getBuffer();
		int index = buffer.getContents().indexOf(annotationStmt, sourceRange.getOffset());
		Assert.assertTrue("SimpleAnnotation not found: '" + annotationStmt + "'", (index >= sourceRange.getOffset())
				&& (index <= sourceRange.getOffset() + sourceRange.getLength()));
		buffer.replace(index, annotationStmt.length(), "");
		saveAndClose(unit);
	}

	public static IMethod addMethodParameter(IMethod method, String parameter, boolean useWorkingCopy)
			throws JavaModelException {
		ICompilationUnit unit = getCompilationUnit(method.getCompilationUnit(), useWorkingCopy);
		ISourceRange sourceRange = method.getSourceRange();
		IBuffer buffer = ((IOpenable) unit).getBuffer();
		String[] parameterNames = method.getParameterNames();
		int offset = buffer.getContents().indexOf("public", sourceRange.getOffset());
		int index = buffer.getContents().indexOf("(", offset);
		if (parameterNames.length == 0) {
			buffer.replace(index + 1, 0, parameter);
		} else {
			buffer.replace(index + 1, 0, parameter + ",");
		}
		saveAndClose(unit);
		return (IMethod) method.getCompilationUnit().getElementAt(sourceRange.getOffset());
	}

	/**
	 * @param compilationUnit
	 * @return
	 * @throws JavaModelException
	 */
	public static ICompilationUnit createWorkingCopy(ICompilationUnit compilationUnit) throws JavaModelException {
		LOGGER.debug("Creating working copy...");
		// ICompilationUnit workingCopy = compilationUnit.getWorkingCopy(new
		// NullProgressMonitor());
		ICompilationUnit workingCopy = compilationUnit.getWorkingCopy(new WorkingCopyOwner() {

			/*
			 * (non-Javadoc)
			 * @see org.eclipse.jdt.core.WorkingCopyOwner#getProblemRequestor(org .eclipse.jdt.core.ICompilationUnit)
			 */
			@Override
			public IProblemRequestor getProblemRequestor(ICompilationUnit workingCopy) {
				// TODO Auto-generated method stub
				return new IProblemRequestor() {

					@Override
					public boolean isActive() {
						// TODO Auto-generated method stub
						return true;
					}

					@Override
					public void endReporting() {
						// TODO Auto-generated method stub

					}

					@Override
					public void beginReporting() {
						// TODO Auto-generated method stub

					}

					@Override
					public void acceptProblem(IProblem problem) {
						// LOGGER.debug("Reporting problem: {} on {}", problem, new
						// String(problem.getOriginatingFileName()));

					}
				};
			}
		}, new NullProgressMonitor());

		// ICompilationUnit workingCopy =
		// JavaPlugin.getDefault().getWorkingCopyManager().getWorkingCopy();
		LOGGER.debug("Working copy created.");
		return workingCopy;
	}

	/**
	 * @param unit
	 * @throws JavaModelException
	 */
	public static void saveAndClose(ICompilationUnit unit) throws JavaModelException {
		try {
			if (unit.isWorkingCopy()) {
				LOGGER.debug("Reconciling unit...");

				unit.reconcile(AST.JLS3, ICompilationUnit.FORCE_PROBLEM_DETECTION, unit.getOwner(),
						new NullProgressMonitor());
				// Commit changes
				LOGGER.debug("Commiting working copy...");
				unit.commitWorkingCopy(true, null);
				// Destroy working copy
				LOGGER.debug("Discarding working copy...");
				unit.discardWorkingCopy();
			} else {
				unit.save(new NullProgressMonitor(), true);
			}
			// explicitly trigger the project build
			unit.getJavaProject().getProject().build(IncrementalProjectBuilder.AUTO_BUILD, null);

		} catch (Exception e) {
			LOGGER.error("Failed to build project", e);
		}
	}

	public static void move(ICompilationUnit compilationUnit, String targetPackageName, Bundle bundle)
			throws CoreException {
		/*
		 * ICompilationUnit destContainer = createCompilationUnit(compilationUnit.getJavaProject(), null,
		 * targetPackageName, compilationUnit.getElementName(), bundle); MoveElementsOperation operation = new
		 * MoveElementsOperation( new IJavaElement[] { compilationUnit }, new IJavaElement[] { destContainer }, true);
		 * operation.run(new NullProgressMonitor());
		 */
		IPackageFragment packageFragment = WorkbenchUtils.createPackage(compilationUnit.getJavaProject(),
				"org.jboss.tools.ws.jaxrs.sample");
		compilationUnit.move(packageFragment, null, compilationUnit.getElementName(), false, new NullProgressMonitor());

		saveAndClose(compilationUnit);
	}

	public static void delete(ICompilationUnit compilationUnit) throws CoreException {
		compilationUnit.delete(true, new NullProgressMonitor());

		// saveAndClose(compilationUnit);
	}

	public static void delete(IAnnotation annotation, boolean useWorkingCopy) throws CoreException {
		final IMember parent = (IMember) annotation.getParent();
		ICompilationUnit unit = getCompilationUnit(parent.getCompilationUnit(), useWorkingCopy);
		IBuffer buffer = ((IOpenable) unit).getBuffer();
		final ISourceRange sourceRange = annotation.getSourceRange();
		buffer.replace(sourceRange.getOffset(), sourceRange.getLength(), "");
		saveAndClose(unit);
	}

	/*
	 * public static CompilationUnitEditor getCompilationUnitEditor(IFile file) throws PartInitException { IEditorPart
	 * editorPart = null; PlatformUI.isWorkbenchRunning(); PlatformUI.getWorkbench().isStarting(); IWorkbenchPage page =
	 * PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(); editorPart = page.openEditor(new
	 * FileEditorInput(file), IDE.getEditorDescriptor(file).getId()); return (CompilationUnitEditor) editorPart; }
	 */

	public static void delete(IMember element) throws JavaModelException {
		ICompilationUnit compilationUnit = element.getCompilationUnit();
		element.delete(true, new NullProgressMonitor());
		saveAndClose(compilationUnit);
	}

	public static void rename(ICompilationUnit compilationUnit, String name) throws JavaModelException {
		compilationUnit.rename(name, true, new NullProgressMonitor());
		saveAndClose(compilationUnit);
	}

	public static IMethod renameMethod(ICompilationUnit compilationUnit, String oldName, String newName,
			boolean useWorkingCopy) throws JavaModelException {
		ICompilationUnit unit = getCompilationUnit(compilationUnit, useWorkingCopy);
		for (IMethod method : unit.findPrimaryType().getMethods()) {
			if (method.getElementName().equals(oldName)) {
				method.rename(newName, true, new NullProgressMonitor());
				saveAndClose(unit);
				return method;
			}
		}
		Assert.fail("Method not found");
		return null;
	}

	public static void removeSourceFolder(IProject project, IProgressMonitor progressMonitor) throws JavaModelException {
		IFolder srcFolder = project.getFolder(new Path("src"));
		IJavaProject javaProject = JavaCore.create(project);
		List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>(Arrays.asList(javaProject.getRawClasspath()));
		IClasspathEntry srcEntry = JavaCore.newSourceEntry(srcFolder.getFullPath());
		for (Iterator<IClasspathEntry> entryIterator = entries.iterator(); entryIterator.hasNext();) {
			IClasspathEntry entry = entryIterator.next();
			if (entry.equals(srcEntry)) {
				entries.remove(entry);
			}
		}
		javaProject.setRawClasspath(entries.toArray(new IClasspathEntry[entries.size()]), progressMonitor);
	}

	/**
	 * Iterates through the project's package fragment roots and returns the one (source or binary) which project
	 * relative path matches the given path.
	 * 
	 * @param project
	 * @param path
	 * @param progressMonitor
	 * @return
	 * @throws JavaModelException
	 */
	public static IPackageFragmentRoot getPackageFragmentRoot(IJavaProject project, String path,
			IProgressMonitor progressMonitor) throws JavaModelException {
		for (IPackageFragmentRoot packageFragmentRoot : project.getAllPackageFragmentRoots()) {
			final String fragmentPath = packageFragmentRoot.getPath().makeRelativeTo(project.getPath())
					.toPortableString();
			if (fragmentPath.equals(path)) {
				return packageFragmentRoot;
			}
		}
		fail("Entry with path " + path + " not found in project.");
		return null;
	}

	/**
	 * @param monitor
	 * @param description
	 * @param projectName
	 * @param workspace
	 * @param project
	 * @throws InvocationTargetException
	 */
	static void createProject(IProgressMonitor monitor, IProjectDescription description, String projectName,
			IWorkspace workspace, IProject project) throws InvocationTargetException {
		// import from file system

		// import project from location copying files - use default project
		// location for this workspace
		// if location is null, project already exists in this location or
		// some error condition occured.
		IProjectDescription desc = workspace.newProjectDescription(projectName);
		desc.setBuildSpec(description.getBuildSpec());
		desc.setComment(description.getComment());
		desc.setDynamicReferences(description.getDynamicReferences());
		desc.setNatureIds(description.getNatureIds());
		desc.setReferencedProjects(description.getReferencedProjects());
		description = desc;

		try {
			monitor.beginTask(DataTransferMessages.WizardProjectsImportPage_CreateProjectsTask, 100);
			project.create(description, new SubProgressMonitor(monitor, 30));
			project.open(IResource.BACKGROUND_REFRESH, new SubProgressMonitor(monitor, 70));
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		} finally {
			monitor.done();
		}
	}

	/**
	 * Remove the first referenced library those absolute path contains the given name.
	 * 
	 * @param javaProject
	 * @param name
	 * @param progressMonitor
	 * @throws CoreException
	 * @throws InterruptedException
	 * @throws OperationCanceledException
	 */
	public static List<IPackageFragmentRoot> removeClasspathEntry(IJavaProject javaProject, String name,
			IProgressMonitor progressMonitor) throws CoreException, OperationCanceledException, InterruptedException {
		IClasspathEntry[] classpathEntries = javaProject.getRawClasspath();
		int index = 0;
		List<IPackageFragmentRoot> fragments = null;
		for (IClasspathEntry entry : classpathEntries) {
			if (entry.getPath().toFile().getAbsolutePath().contains(name)) {
				fragments = new ArrayList<IPackageFragmentRoot>();
				for (IPackageFragmentRoot fragment : javaProject.getAllPackageFragmentRoots()) {
					if (fragment.getRawClasspathEntry().equals(entry)) {
						fragments.add(fragment);
					}
				}
				break;
			}
			index++;
		}
		if (index < classpathEntries.length) {
			classpathEntries = (IClasspathEntry[]) ArrayUtils.remove(classpathEntries, index);
			javaProject.setRawClasspath(classpathEntries, progressMonitor);
		}
		WorkbenchTasks.buildProject(javaProject.getProject(), progressMonitor);
		return fragments;
	}

	public static boolean removeReferencedLibrarySourceAttachment(IJavaProject javaProject, String name,
			IProgressMonitor progressMonitor) throws OperationCanceledException, CoreException, InterruptedException {
		IClasspathEntry[] classpathEntries = javaProject.getRawClasspath();
		boolean found = false;
		for (int i = 0; i < classpathEntries.length; i++) {
			IClasspathEntry classpathEntry = classpathEntries[i];
			IPath path = classpathEntry.getPath();
			if (path.toFile().getAbsolutePath().contains(name)) {
				if (!path.isAbsolute()) {
					path = JavaCore.getClasspathVariable("M2_REPO").append(path.makeRelativeTo(new Path("M2_REPO")));
				}
				classpathEntries[i] = JavaCore.newLibraryEntry(path, null, null, classpathEntry.getAccessRules(),
						classpathEntry.getExtraAttributes(), classpathEntry.isExported());
				found = true;
			}
		}
		javaProject.setRawClasspath(classpathEntries, progressMonitor);
		// refresh/build project
		WorkbenchTasks.buildProject(javaProject.getProject(), progressMonitor);
		return found;
	}

	public static Annotation getAnnotation(final IMember member, final String annotationName)
			throws JavaModelException {
		if (annotationName == null) {
			return null;
		}
		return JdtUtils.resolveAnnotation(member, JdtUtils.parse(member, null), annotationName);
	}

	public static Annotation getAnnotation(final IMember member, final String annotationName, String... values)
			throws JavaModelException {
		Annotation annotation = JdtUtils.resolveAnnotation(member, JdtUtils.parse(member, null), annotationName);

		Map<String, List<String>> elements = new HashMap<String, List<String>>();
		elements.put("value", Arrays.asList(values));
		annotation.update(new Annotation(annotation.getJavaAnnotation(), annotation.getName(), elements, null));
		return annotation;
	}

	public static IType getType(String typeName, IJavaProject javaProject) throws CoreException {
		return JdtUtils.resolveType(typeName, javaProject, null);
	}

	/**
	 * Creates a file with the given name and the given content in the given folder.
	 * 
	 * @param folder
	 * @param fileName
	 * @param stream
	 * @throws CoreException
	 * @throws IOException
	 */
	public static IResource createContent(IFolder folder, String fileName, InputStream stream) throws CoreException,
			IOException {
		if (!folder.exists()) {
			folder.create(true, true, new NullProgressMonitor());
		}
		folder.getFile(fileName).create(stream, true, null);
		LOGGER.debug("Content of {}", folder.getFile(fileName).getProjectRelativePath().toPortableString());
		final InputStream contents = folder.getFile(fileName).getContents();
		final char[] buffer = new char[0x10000];
		StringBuilder out = new StringBuilder();
		Reader in = new InputStreamReader(contents, "UTF-8");
		int read;
		do {
			read = in.read(buffer, 0, buffer.length);
			if (read > 0) {
				out.append(buffer, 0, read);
			}
		} while (read >= 0);
		LOGGER.debug(out.toString());
		return folder.findMember(fileName);
	}

	/**
	 * Replaces the content of the given resource with the given stream.
	 * 
	 * @param webxmlResource
	 * @param stream
	 * @throws CoreException
	 * @throws IOException
	 */
	public static void replaceContent(IResource resource, InputStream stream) throws CoreException, IOException {
		final IProject project = resource.getProject();
		final IFile file = project.getFile(resource.getProjectRelativePath());
		if (file.exists()) {
			file.delete(true, new NullProgressMonitor());
		}
		file.create(stream, true, null);
		LOGGER.debug("Content:");
		final InputStream contents = file.getContents();
		final char[] buffer = new char[0x10000];
		StringBuilder out = new StringBuilder();
		Reader in = new InputStreamReader(contents, "UTF-8");
		int read;
		do {
			read = in.read(buffer, 0, buffer.length);
			if (read > 0) {
				out.append(buffer, 0, read);
			}
		} while (read >= 0);
		LOGGER.debug(out.toString());
	}

	/**
	 * @return
	 * @throws CoreException
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws OperationCanceledException
	 * @throws InvocationTargetException
	 */
	public static IResource replaceDeploymentDescriptorWith(IJavaProject javaProject, String webxmlReplacementName,
			Bundle bundle) throws Exception {
		IFolder webInfFolder = WtpUtils.getWebInfFolder(javaProject.getProject());
		IResource webxmlResource = webInfFolder.findMember("web.xml");
		if (webxmlResource != null && webxmlReplacementName == null) {
			webxmlResource.delete(true, new NullProgressMonitor());
		} else if (webxmlResource == null && webxmlReplacementName == null) {
			// nothing to do: file does not exist and should be removed ;-)
			return null;
		}
		if (webxmlReplacementName == null) {
			return null;
		}
		InputStream stream = FileLocator.openStream(bundle, new Path("resources").append(webxmlReplacementName), false);
		assertThat(stream, notNullValue());
		if (webxmlResource != null) {
			replaceContent(webxmlResource, stream);
			return webxmlResource;
		} else {
			return createContent(webInfFolder, "web.xml", stream);
		}
	}

}
