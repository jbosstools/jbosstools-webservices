package org.jboss.tools.ws.jaxrs.core.junitrules;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.jboss.tools.ws.jaxrs.core.JBossJaxrsCoreTestPlugin;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.internal.utils.TestLogger;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.junit.Assert;
import org.osgi.framework.Bundle;

public class ResourcesUtils {

	private static Bundle bundle = JBossJaxrsCoreTestPlugin.getDefault().getBundle();

	/**
	 * Replace the first occurrence of the given old content with the new
	 * content. Fails if the old content is not found (avoids weird side effects
	 * in the rest of the test).
	 * 
	 * @param compilationUnit
	 * @param oldContent
	 * @param newContent
	 * @param useWorkingCopy
	 * @throws CoreException
	 */
	public static IType replaceFirstOccurrenceOfCode(final String typeName, final IJavaProject javaProject,
			final String oldContent, String newContent, final boolean useWorkingCopy) throws CoreException {
		final IType type = JdtUtils.resolveType(typeName, javaProject, new NullProgressMonitor());
		final ICompilationUnit unit = JavaElementsUtils.getCompilationUnit(type.getCompilationUnit(), useWorkingCopy);
		final IBuffer buffer = ((IOpenable) unit).getBuffer();
		final int offset = buffer.getContents().indexOf(oldContent);
		Assert.assertTrue("Old content '" + oldContent + "' not found", offset != -1);
		buffer.replace(offset, oldContent.length(), newContent);
		JavaElementsUtils.saveAndClose(unit);
		return type;
	}

	public static <T extends IMember> T replaceFirstOccurrenceOfCode(final T member, final String oldContent, final String newContent,
			final boolean useWorkingCopy) throws JavaModelException {
		final ICompilationUnit compilationUnit = member.getCompilationUnit();
		final ICompilationUnit unit = useWorkingCopy ? JavaElementsUtils.createWorkingCopy(compilationUnit) : compilationUnit;
		final ISourceRange sourceRange = member.getSourceRange();
		final IBuffer buffer = ((IOpenable) unit).getBuffer();
		final int offset = buffer.getContents().indexOf(oldContent, sourceRange.getOffset());
		Assert.assertTrue("Old content not found: '" + oldContent + "'", offset != -1);
		buffer.replace(offset, oldContent.length(), newContent);
		// IJavaElement modifiedMethod =
		// workingCopy.getElementAt(sourceRange.getOffset());
		JavaElementsUtils.saveAndClose(unit);
		@SuppressWarnings("unchecked")
		T modifiedElement = (T) unit.getElementAt(sourceRange.getOffset());
		return modifiedElement;
	}

	public static void removeSourceRange(final ICompilationUnit compilationUnit, final ISourceRange rangeToRemove,
			final boolean useWorkingCopy) throws JavaModelException {
		final ICompilationUnit unit = useWorkingCopy ? JavaElementsUtils.createWorkingCopy(compilationUnit) : compilationUnit;
		final IBuffer buffer = ((IOpenable) unit).getBuffer();
		buffer.replace(rangeToRemove.getOffset(), rangeToRemove.getLength(), "");
		// IJavaElement modifiedMethod =
		// workingCopy.getElementAt(sourceRange.getOffset());
		JavaElementsUtils.saveAndClose(unit);
	}
	
	public static void replaceAllOccurrencesOfCode(final ICompilationUnit compilationUnit, final String oldContent,
			final String newContent, final boolean useWorkingCopy) throws JavaModelException {
	
		ICompilationUnit unit = JavaElementsUtils.getCompilationUnit(compilationUnit, useWorkingCopy);
		IBuffer buffer = ((IOpenable) unit).getBuffer();
		int offset = 0;
		boolean atLeastOneMatch = false;
		while ((offset = buffer.getContents().indexOf(oldContent, offset)) != -1) {
			buffer.replace(offset, oldContent.length(), newContent);
			offset = offset + newContent.length();
			atLeastOneMatch = true;
		}
		if (!atLeastOneMatch) {
			fail("No match for '" + oldContent + "' in:\n" + compilationUnit.getSource());
		}
		JavaElementsUtils.saveAndClose(unit);
	
	}

	public static void replaceAllOccurrencesOfCode(IType type, String oldContent, String newContent,
			boolean useWorkingCopy) throws JavaModelException {
		replaceAllOccurrencesOfCode(type.getCompilationUnit(), oldContent, newContent, useWorkingCopy);
	}

	/**
	 * Deletes the IResource and waits for jobs to complete. 
	 * Attempts a fixed number of times before throwing the {@link CoreException} (in case of FS lock or other temporary issue).
	 * @param resource
	 * @throws CoreException
	 * @throws InterruptedException 
	 */
	public static void delete(IResource resource) throws CoreException {
		if(resource == null) {
			return;
		}
		final int maxAttempts = 5;
		for (int i = 1; i <= maxAttempts; i++) {
			try {
				resource.delete(true, new NullProgressMonitor());
				WorkbenchTasks.waitForTasksToComplete(getWorkspace(resource));
				return;
			} catch (CoreException e) {
				Logger.warn("Attempt #" + i + " to remove " + resource.getFullPath().toOSString() + " failed", e);
				if(i == maxAttempts) {
					throw e;
				} else {
					try {
						Thread.sleep(TimeUnit.SECONDS.toMillis(1));
					} catch (InterruptedException e1) {
						fail("Failed to wait before attempting to delete the resource: " + e.getMessage());
					}
				}
			}
		}
	}

	/**
	 * Creates a file with the given name and the given content in the given
	 * folder.
	 * 
	 * @param folder
	 * @param fileName
	 * @param stream
	 * @throws CoreException
	 * @throws IOException
	 */
	public static IFile createFileFromStream(IFolder folder, String fileName, InputStream stream)
			throws CoreException, IOException {
		if (!folder.exists()) {
			folder.create(true, true, new NullProgressMonitor());
		}
		folder.getFile(fileName).create(stream, true, null);
		TestLogger.debug("Content of {}", folder.getFile(fileName).getProjectRelativePath().toPortableString());
		final InputStream contents = folder.getFile(fileName).getContents();
		final char[] buffer = new char[0x10000];
		final StringBuilder out = new StringBuilder();
		final Reader in = new InputStreamReader(contents, "UTF-8");
		int read;
		do {
			read = in.read(buffer, 0, buffer.length);
			if (read > 0) {
				out.append(buffer, 0, read);
			}
		} while (read >= 0);
		in.close();
		TestLogger.debug(out.toString());
		return (IFile) folder.findMember(fileName);
	}

	/**
	 * Replaces the content of the given resource with the given stream.
	 * 
	 * @param webxmlResource
	 * @param stream
	 * @throws CoreException
	 * @throws IOException
	 */
	public static void replaceContent(final IResource resource, final InputStream stream) throws CoreException, IOException {
		final IProject project = resource.getProject();
		final IFile file = project.getFile(resource.getProjectRelativePath());
		file.setContents(stream, true, false, null);
		WorkbenchTasks.waitForTasksToComplete(getWorkspace(resource));
		if(TestLogger.isDebugEnabled()) {
			TestLogger.debug("Replaced content:\n" + IOUtils.toString(file.getContents()));
		}
	}

	/**
	 * Replaces the content of the given resource with the given stream.
	 * 
	 * @param webxmlResource
	 * @param stream
	 * @throws CoreException
	 * @throws IOException
	 */
	public static void replaceContent(final IResource resource, final String oldContent, final String newContent)
			throws CoreException, IOException {
		// pre-condition: verify that resource exists
		assertThat(resource.exists(), is(true));
		final IProject project = resource.getProject();
		final IFile file = project.getFile(resource.getProjectRelativePath());
		final String content = IOUtils.toString(file.getContents());
		// pre-condition: verify that resource contains old content
		assertNotEquals("Could not find occurrence of" + oldContent + "\nin\n" + content, content.indexOf(oldContent), -1);
		// operation
		final String modifiedContent = content.replace(oldContent, newContent);
		file.setContents(IOUtils.toInputStream(modifiedContent), true, false, null);
		TestLogger.debug("Content:\n" + modifiedContent);
	}

	/**
	 * Replaces the content of the given resource with the given stream.
	 * 
	 * @param webxmlResource
	 * @param stream
	 * @throws CoreException
	 * @throws IOException
	 */
	public static void replaceContent(final IResource resource, final InputStream stream, final boolean useWorkingCopy)
			throws CoreException, IOException {
		final IProject project = resource.getProject();
		final IFile file = project.getFile(resource.getProjectRelativePath());
		ICompilationUnit unit = JavaElementsUtils.getCompilationUnit(JdtUtils.getCompilationUnit(file), useWorkingCopy);
		IBuffer buffer = ((IOpenable) unit).getBuffer();
		buffer.setContents(IOUtils.toString(stream));
		JavaElementsUtils.saveAndClose(unit);
	}

	public static String getBundleResourceContent(final String name) {
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
	 * @return the {@link IWorkspace} associated with the given {@link IResource}
	 * @param resource the {@link IResource} to process
	 */
	public static IWorkspace getWorkspace(final IResource resource) {
		return resource.getProject().getWorkspace();
	}
}
