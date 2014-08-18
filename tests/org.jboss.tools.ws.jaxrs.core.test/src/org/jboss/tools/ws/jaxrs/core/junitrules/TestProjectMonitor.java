/**
 * 
 */
package org.jboss.tools.ws.jaxrs.core.junitrules;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
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
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaProject;
import org.jboss.tools.ws.jaxrs.core.JBossJaxrsCorePlugin;
import org.jboss.tools.ws.jaxrs.core.JBossJaxrsCoreTestPlugin;
import org.jboss.tools.ws.jaxrs.core.configuration.ProjectNatureUtils;
import org.jboss.tools.ws.jaxrs.core.internal.utils.TestLogger;
import org.jboss.tools.ws.jaxrs.core.jdt.CompilationUnitsRepository;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.wtp.WtpUtils;
import org.junit.Assert;
import org.junit.rules.ExternalResource;
import org.osgi.framework.Bundle;

/**
 * @author xcoulon
 *
 */
@SuppressWarnings("restriction")
public class TestProjectMonitor extends ExternalResource {

	private final String projectName;

	private IProject project = null;

	private IJavaProject javaProject = null;

	private TestProjectSynchronizator synchronizor = null;

	public TestProjectMonitor(final String projectName) {
		this.projectName = projectName;
	}
	
	@Override
	protected void before() throws Throwable {
		TestLogger.debug("***********************************************");
		TestLogger.debug("* Setting up test project...");
		TestLogger.debug("***********************************************");
		long startTime = System.currentTimeMillis();
		try {
			setupProject();
		} catch (CoreException e) {
			fail(e.getMessage());
		} finally {
			long endTime = System.currentTimeMillis();
			TestLogger.debug("***********************************************");
			TestLogger.debug("*** Test project setup in " + (endTime - startTime) + "ms.");
			TestLogger.debug("***********************************************");
		}
	}

	void setupProject() throws CoreException, InvocationTargetException, JavaModelException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		if (workspace.isAutoBuilding()) {
			IWorkspaceDescription description = workspace.getDescription();
			description.setAutoBuilding(false);
			workspace.setDescription(description);
			TestLogger.info("Workspace auto-build disabled.");
		}
		// clear CompilationUnit repository
		CompilationUnitsRepository.getInstance().clear();
		JBossJaxrsCorePlugin.getDefault().pauseListeners();
		this.project = WorkbenchTasks.getTargetWorkspaceProject(projectName);
		this.project.open(new NullProgressMonitor());
		this.javaProject = JavaCore.create(project);
		this.javaProject.open(new NullProgressMonitor());
		Assert.assertNotNull("JavaProject not found", javaProject.exists());
		Assert.assertTrue("JavaProject not open", javaProject.isOpen());
		Assert.assertTrue("Project not open", project.isOpen());
		Assert.assertNotNull("Project not found", javaProject.getProject().exists());
		Assert.assertTrue("Project is not a JavaProject", JavaProject.hasJavaNature(javaProject.getProject()));
		this.synchronizor = new TestProjectSynchronizator(projectName);
		workspace.addResourceChangeListener(synchronizor);
		// make sure JAX-RS Nature is *NOT* installed at this stage.
		ProjectNatureUtils.uninstallProjectNature(project, ProjectNatureUtils.JAXRS_NATURE_ID);
		//buildProject();
	}

	@Override
	protected void after() {
		long startTime = System.currentTimeMillis();
		try {
			TestLogger.debug("**********************************************************************************");
			TestLogger.debug("*** Synchronizing the workspace back to its initial state after run");
			TestLogger.debug("**********************************************************************************");
			// remove listener before sync' to avoid desync...
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(synchronizor);
			if(synchronizor.resync()) {
				buildProject();
			}
		} catch (CoreException e) {
			fail(e.getMessage());
		} catch (InvocationTargetException e) {
			fail(e.getMessage());
		} catch (InterruptedException e) {
			fail(e.getMessage());
		} finally {
			long endTime = System.currentTimeMillis();
			TestLogger.info("Test Workspace sync'd in " + (endTime - startTime) + "ms.");
		}
	}

	public IProject getProject() {
		return project;
	}

	public IJavaProject getJavaProject() {
		return javaProject;
	}
	
	public static void buildWorkspace(final IProgressMonitor progressMonitor) throws CoreException,
			OperationCanceledException, InterruptedException {
		ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.AUTO_BUILD, new NullProgressMonitor());
		Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
	}

	
	/********************************************************************************************
	 * 
	 * Java manipulation utility methods (a.k.a., Helpers)
	 * 
	 ********************************************************************************************/
	public void buildProject()
			throws CoreException, OperationCanceledException, InterruptedException {
		buildProject(IncrementalProjectBuilder.FULL_BUILD);
	}

	public void buildProject(final int buildKind)
			throws CoreException, OperationCanceledException, InterruptedException {
		project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		project.build(buildKind, new NullProgressMonitor());
		Job.getJobManager().join(ResourcesPlugin.FAMILY_MANUAL_BUILD, null);
	}

	/**
	 * Iterates through the project's package fragment roots and returns the one
	 * (source or binary) which project relative path matches the given path.
	 * @param path
	 * @param project
	 * @param progressMonitor
	 * 
	 * @return
	 * @throws JavaModelException
	 */
	public IPackageFragmentRoot resolvePackageFragmentRoot(final String path) throws JavaModelException {
		for (IPackageFragmentRoot packageFragmentRoot : javaProject.getAllPackageFragmentRoots()) {
			final String fragmentPath = packageFragmentRoot.getPath().makeRelativeTo(javaProject.getPath())
					.toPortableString();
			if (fragmentPath.equals(path)) {
				return packageFragmentRoot;
			}
		}
		fail("Entry with path " + path + " not found in project.");
		return null;
	}

	public IPackageFragmentRoot addClasspathEntry(final String name) throws CoreException, OperationCanceledException, InterruptedException {
		IPath path = javaProject.getProject().getLocation().append("lib").addTrailingSeparator().append(name);
		if (!path.toFile().exists() || !path.toFile().canRead()) {
			TestLogger.warn("Following library does not exist or is not readable: {} ", path.toFile());
		}
		IClasspathEntry[] classpathEntries = javaProject.getRawClasspath();
		IClasspathEntry newLibraryEntry = JavaCore.newLibraryEntry(path, null, null);
		classpathEntries = (IClasspathEntry[]) ArrayUtils.add(classpathEntries, newLibraryEntry);
		javaProject.setRawClasspath(classpathEntries, new NullProgressMonitor());
		buildProject();
		for (IPackageFragmentRoot fragment : javaProject.getAllPackageFragmentRoots()) {
			if (fragment.getRawClasspathEntry().equals(newLibraryEntry)) {
				return fragment;
			}
		}
		return null;
	}
	
	/**
	 * Remove the first referenced library those absolute path contains the
	 * given name.
	 * 
	 * @param javaProject
	 * @param name
	 * @param progressMonitor
	 * @throws CoreException
	 * @throws InterruptedException
	 * @throws OperationCanceledException
	 */
	public List<IPackageFragmentRoot> removeClasspathEntry(final String name) throws CoreException, OperationCanceledException, InterruptedException {
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
			javaProject.setRawClasspath(classpathEntries, new NullProgressMonitor());
		}
		// needs to explicitely reopen the java project after setting the new
		// classpath entries
		javaProject.open(new NullProgressMonitor());
		buildProject();
		return fragments;
	}

	public boolean removeReferencedLibrarySourceAttachment(final String name) throws OperationCanceledException,
			CoreException, InterruptedException {
		final IClasspathEntry[] classpathEntries = javaProject.getRawClasspath();
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
		javaProject.setRawClasspath(classpathEntries, new NullProgressMonitor());
		// refresh/build project
		buildProject();
		return found;
	}

	public IType resolveType(final String typeName) throws CoreException {
		return JdtUtils.resolveType(typeName, javaProject, new NullProgressMonitor());
	}

	public IMethod resolveMethod(final String typeName, final String methodName)
			throws CoreException {
		final IType type = resolveType(typeName);
		return resolveMethod(type, methodName);
	}

	public IMethod resolveMethod(final IType type, final String methodName) throws CoreException {
		for (IMethod method : type.getMethods()) {
			if (method.getElementName().equals(methodName)) {
				return method;
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
	public ICompilationUnit createCompilationUnit(final String fileName, final String pkg,
			final String unitName) throws JavaModelException {
		String contents = "";
		if (fileName != null) {
			contents = ResourcesUtils.getBundleResourceContent(fileName);
		}
		IPackageFragmentRoot sourceFolder = javaProject.findPackageFragmentRoot(javaProject.getProject().getFullPath()
				.append("src/main/java"));
		IPackageFragment packageFragment = sourceFolder.getPackageFragment(pkg);
		ICompilationUnit foocompilationUnit = packageFragment.createCompilationUnit(unitName, contents, true,
				new NullProgressMonitor());
		JavaElementsUtils.saveAndClose(foocompilationUnit);
		return foocompilationUnit;
	}

	/**
	 * @return
	 * @throws JavaModelException
	 */
	public IPackageFragment createPackage(final String pkgName) throws JavaModelException {
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

	public IFile replaceDeploymentDescriptorWith(final String webxmlReplacementName) throws Exception {
		IFile webxmlResource = WtpUtils.getWebDeploymentDescriptor(project);
		if (webxmlResource != null && webxmlReplacementName == null) {
			webxmlResource.delete(true, new NullProgressMonitor());
		} else if (webxmlResource == null && webxmlReplacementName == null) {
			// nothing to do: file does not exist and should be removed ;-)
			return null;
		}
		if (webxmlReplacementName == null) {
			return null;
		}
		Bundle bundle = JBossJaxrsCoreTestPlugin.getDefault().getBundle();
		InputStream stream = FileLocator.openStream(bundle, new Path("resources").append(webxmlReplacementName), false);
		assertThat(stream, notNullValue());
		if (webxmlResource != null) {
			ResourcesUtils.replaceContent(webxmlResource, stream);
			return webxmlResource;
		} else {
			return ResourcesUtils.createFileFromStream(WtpUtils.getWebInfFolder(project), "web.xml", stream);
		}
	}

	public IFile replaceDotProjectFileWith(final String dotProjectReplacementName) throws Exception {
		IFile dotProjectResource = project.getFile(".project");
		if (dotProjectResource != null && dotProjectReplacementName == null) {
			dotProjectResource.delete(true, new NullProgressMonitor());
		} else if (dotProjectResource == null && dotProjectReplacementName == null) {
			// nothing to do: file does not exist and should be removed ;-)
			return null;
		}
		if (dotProjectReplacementName == null) {
			return null;
		}
		Bundle bundle = JBossJaxrsCoreTestPlugin.getDefault().getBundle();
		InputStream stream = FileLocator.openStream(bundle, new Path("resources").append(dotProjectReplacementName), false);
		assertThat(stream, notNullValue());
		if (dotProjectResource != null) {
			ResourcesUtils.replaceContent(dotProjectResource, stream);
			return dotProjectResource;
		} else {
			return ResourcesUtils.createFileFromStream(project.getFolder("."), ".project", stream);
		}
	}

	/**
	 * @return
	 * @throws CoreException
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws OperationCanceledException
	 * @throws InvocationTargetException
	 */
	public IResource deleteDeploymentDescriptor() throws Exception {
		IResource webxmlResource = WtpUtils.getWebDeploymentDescriptor(project);
		webxmlResource.delete(true, new NullProgressMonitor());
		return webxmlResource;
	}
	
	
	public IResource getWebDeploymentDescriptor() throws CoreException {
		return WtpUtils.getWebDeploymentDescriptor(project);
	}

	
}
