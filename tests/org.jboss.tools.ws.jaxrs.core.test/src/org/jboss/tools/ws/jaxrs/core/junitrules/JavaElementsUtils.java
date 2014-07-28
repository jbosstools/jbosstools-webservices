/**
 * 
 */
package org.jboss.tools.ws.jaxrs.core.junitrules;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IProblemRequestor;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.ui.internal.wizards.datatransfer.DataTransferMessages;
import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionUtils;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.internal.utils.TestLogger;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.junit.Assert;

/**
 * Java code manipulation utility class.
 * 
 * @author xcoulon
 *
 */
@SuppressWarnings("restriction")
public class JavaElementsUtils {

	public static Annotation createAnnotation(final String className) {
		return createAnnotation(null, className, null);
	}

	public static Annotation createAnnotation(final String className, final String value) {
		return createAnnotation(null, className, value);
	}

	public static Annotation createAnnotation(final IAnnotation annotation, final String name, final String value) {
		Map<String, List<String>> values = new HashMap<String, List<String>>();
		values.put("value", Arrays.asList(value));
		return new Annotation(annotation, name, values);
	}

	/**
	 * Returns a <strong>new annotation</strong> built from the given one, with
	 * overriden values
	 * 
	 * @param annotation
	 * @param values
	 * @return
	 * @throws JavaModelException
	 */
	public static Annotation createAnnotation(final Annotation annotation, final String... values)
			throws JavaModelException {
		final Map<String, List<String>> elements = CollectionUtils.toMap("value", Arrays.asList(values));
		return new Annotation(annotation.getJavaAnnotation(), annotation.getFullyQualifiedName(), elements);
	}

	/**
	 * Removes the first occurrence of the given content (not a regexp)
	 * 
	 * @param member
	 * @param content
	 * @throws JavaModelException
	 */
	public static void removeFirstOccurrenceOfCode(final ICompilationUnit compilationUnit, final String content,
			final boolean useWorkingCopy) throws JavaModelException {
		replaceFirstOccurrenceOfCode(compilationUnit, content, "", useWorkingCopy);
	}

	public static void removeFirstOccurrenceOfCode(final IType type, final String content,
			final boolean useWorkingCopy) throws JavaModelException {
		replaceFirstOccurrenceOfCode(type.getCompilationUnit(), content, "", useWorkingCopy);
	}
	
	/**
	 * Replace the first occurrence of the given old content with the new
	 * content. Fails if the old content is not found (avoids weird side effects
	 * in the rest of the test).
	 * 
	 * @param compilationUnit
	 * @param oldContent
	 * @param newContent
	 * @param useWorkingCopy
	 * @throws JavaModelException
	 */
	public static void replaceFirstOccurrenceOfCode(final ICompilationUnit compilationUnit, final String oldContent,
			final String newContent, final boolean useWorkingCopy) throws JavaModelException {
		ICompilationUnit unit = getCompilationUnit(compilationUnit, useWorkingCopy);
		IBuffer buffer = ((IOpenable) unit).getBuffer();
		int offset = buffer.getContents().indexOf(oldContent);
		Assert.assertTrue("Old content '" + oldContent + "' not found", offset != -1);
		buffer.replace(offset, oldContent.length(), newContent);
		saveAndClose(unit);
	}

	public static ICompilationUnit getCompilationUnit(final ICompilationUnit compilationUnit, final boolean useWorkingCopy)
			throws JavaModelException {
		return useWorkingCopy ? createWorkingCopy(compilationUnit) : compilationUnit;
	}

	public static IMethod removeMethod(final ICompilationUnit compilationUnit, final String methodName, final boolean useWorkingCopy)
			throws JavaModelException {
		TestLogger.debug("Removing method " + methodName);
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

	public static void removeType(final IType type, final boolean useWorkingCopy) throws JavaModelException {
		TestLogger.info("Removing type " + type.getElementName() + "...");
		ICompilationUnit unit = getCompilationUnit(type.getCompilationUnit(), useWorkingCopy);
		unit.getType(type.getElementName()).delete(true, new NullProgressMonitor());
		saveAndClose(unit);
	}

	public static IMethod createMethod(final IType javaType, final String contents, final boolean useWorkingCopy)
			throws JavaModelException {
		TestLogger.info("Adding method into type " + javaType.getElementName());
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

	public static IField createField(final IType type, final String contents, final boolean useWorkingCopy) throws JavaModelException {
		TestLogger.info("Adding type into type " + type.getElementName());
		ICompilationUnit unit = useWorkingCopy ? createWorkingCopy(type.getCompilationUnit()) : type
				.getCompilationUnit();
		
		ISourceRange sourceRange = (type.getFields().length > 0) ? type.getFields()[0].getSourceRange() : type.getMethods()[0].getSourceRange();
		IBuffer buffer = ((IOpenable) unit).getBuffer();
		// insert before 1 method
		buffer.replace(sourceRange.getOffset(), 0, contents);
		saveAndClose(unit);
		// return the last method of the java type, assuming it is the one given
		// in parameter
		return type.getFields()[0];
	}

	public static IField removeField(final IField field, final boolean useWorkingCopy) throws JavaModelException {
		TestLogger.info("Removing field " + field.getElementName());
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

	public static ILocalVariable getLocalVariable(final IMethod method, final String variableName) throws JavaModelException {
		for (ILocalVariable param : method.getParameters()) {
			if (param.getElementName().equals(variableName)) {
				return param;
			}
		}
		Assert.fail("Failed to locate method parameter named '" + variableName + "'");
		return null;
	}

	public static IField getField(final IType type, final String fieldName) throws JavaModelException {
		for (IField field : type.getFields()) {
			if (field.getElementName().equals(fieldName)) {
				return field;
			}
		}
		Assert.fail("Failed to locate field named '" + fieldName + "'");
		return null;
	}

	public static IMethod getMethod(final IType type, final String methodName) throws JavaModelException {
		for (IMethod method : type.getMethods()) {
			if (method.getElementName().equals(methodName)) {
				return method;
			}
		}
		Assert.fail("Failed to locate method named '" + methodName + "'");
		return null;
	}

	public static IAnnotation addMethodAnnotation(final IMethod method, final String annotationStmt, final boolean useWorkingCopy)
			throws CoreException {
		ICompilationUnit compilationUnit = method.getCompilationUnit();
		ICompilationUnit unit = getCompilationUnit(compilationUnit, useWorkingCopy);
		ISourceRange sourceRange = method.getSourceRange();
		IBuffer buffer = ((IOpenable) unit).getBuffer();
		buffer.replace(sourceRange.getOffset(), 0, annotationStmt + "\n");
		saveAndClose(unit);
		// look for compilation unit errors (they can explain some issues if tests fail)
		final IMarker[] javaMarkers = method.getResource().findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
		boolean hasErrors = false;
		if(javaMarkers.length > 0) {
			Logger.debug("Reporting java problems on Resource '{}':", method.getResource().getName());
			for(IMarker javaMarker : javaMarkers) {
				if(javaMarker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_ERROR) {
					Logger.debug("  Found problem on line {}: {}", javaMarker.getAttribute(IMarker.LINE_NUMBER), javaMarker.getAttribute(IMarker.MESSAGE));
					hasErrors=true;
				}
			}
		}
		if(hasErrors) {
			Assert.fail("Resource '" + method.getResource().getName() + "' has java problems. Check the logs for more details.");
		}
		final IMethod foundMethod = (IMethod) compilationUnit.getElementAt(method.getSourceRange().getOffset());
		String annotationName = StringUtils.substringBetween(annotationStmt, "@", "(");
		if(annotationName == null) {
			annotationName = annotationStmt.substring("@".length());
		}
		for (IAnnotation annotation : foundMethod.getAnnotations()) {
			if (annotation.getElementName().equals(annotationName)) {
				return annotation;
			}
		}
		Assert.fail("Annotation '" + annotationName + "' not found on method " + foundMethod.getSource());
		return null;
	}

	public static IAnnotation addTypeAnnotation(final IType type, final String annotationStmt, final boolean useWorkingCopy)
			throws JavaModelException, CoreException {
		TestLogger.info("Adding annotation " + annotationStmt + " on type " + type.getElementName());
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

	private static void insertCodeAtLocation(final ICompilationUnit compilationUnit, final String content, final int offset,
			final boolean useWorkingCopy) throws CoreException {
		ICompilationUnit unit = getCompilationUnit(compilationUnit, useWorkingCopy);
		IBuffer buffer = ((IOpenable) unit).getBuffer();
		buffer.replace(offset, 0, content + "\n"); // append a new line at the
													// same time
		saveAndClose(unit);
		String subSource = compilationUnit.getSource().substring(offset, offset + content.length());
		Assert.assertEquals("Content was not inserted", content, subSource);
	}

	public static IAnnotation removeMethodAnnotation(final IMethod method, final IAnnotation annotation, final boolean useWorkingCopy)
			throws JavaModelException {
		ICompilationUnit compilationUnit = method.getCompilationUnit();
		ICompilationUnit unit = getCompilationUnit(compilationUnit, useWorkingCopy);
		ISourceRange sourceRange = annotation.getSourceRange();
		IBuffer buffer = ((IOpenable) unit).getBuffer();
		buffer.replace(sourceRange.getOffset(), sourceRange.getLength(), "");
		saveAndClose(unit);
		return annotation;
	}

	public static IAnnotation removeTypeAnnotation(final IType type, final IAnnotation annotation,
			final boolean useWorkingCopy) throws JavaModelException {
		ICompilationUnit compilationUnit = type.getCompilationUnit();
		ICompilationUnit unit = getCompilationUnit(compilationUnit, useWorkingCopy);
		ISourceRange sourceRange = annotation.getSourceRange();
		IBuffer buffer = ((IOpenable) unit).getBuffer();
		buffer.replace(sourceRange.getOffset(), sourceRange.getLength(), "");
		saveAndClose(unit);
		return annotation;
	}

	public static IAnnotation addFieldAnnotation(final IField field, final String annotationStmt, final boolean useWorkingCopy)
			throws CoreException {
		TestLogger.info("Adding annotation " + annotationStmt + " on type " + field.getElementName());
		insertCodeAtLocation(field.getCompilationUnit(), annotationStmt, field.getSourceRange().getOffset(),
				useWorkingCopy);
		final String annotationName = StringUtils.substringBetween(annotationStmt, "@", "(");
		for (IAnnotation annotation : field.getAnnotations()) {
			if (annotation.getElementName().equals(annotationName)) {
				return annotation;
			}
		}
		return null;
	}

	public static void removeFieldAnnotation(final IField field, final String annotationStmt, final boolean useWorkingCopy)
			throws JavaModelException {
		final ICompilationUnit compilationUnit = field.getCompilationUnit();
		final ICompilationUnit unit = getCompilationUnit(compilationUnit, useWorkingCopy);
		final ISourceRange sourceRange = field.getSourceRange();
		final IBuffer buffer = ((IOpenable) unit).getBuffer();
		final int index = buffer.getContents().indexOf(annotationStmt, sourceRange.getOffset());
		Assert.assertTrue("SimpleAnnotation not found: '" + annotationStmt + "'", (index >= sourceRange.getOffset())
				&& (index <= sourceRange.getOffset() + sourceRange.getLength()));
		buffer.replace(index, annotationStmt.length(), "");
		saveAndClose(unit);
	}

	public static IMethod addMethodParameter(final IMethod method, final String parameter, final boolean useWorkingCopy)
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

	public static ICompilationUnit createWorkingCopy(final ICompilationUnit compilationUnit) throws JavaModelException {
		TestLogger.debug("Creating working copy...");
		// ICompilationUnit workingCopy = compilationUnit.getWorkingCopy(new
		// NullProgressMonitor());
		ICompilationUnit workingCopy = compilationUnit.getWorkingCopy(new WorkingCopyOwner() {
	
			@Override
			public IProblemRequestor getProblemRequestor(ICompilationUnit workingCopy) {
				return new IProblemRequestor() {
	
					@Override
					public boolean isActive() {
						return true;
					}
	
					@Override
					public void endReporting() {
					}
	
					@Override
					public void beginReporting() {
					}
	
					@Override
					public void acceptProblem(IProblem problem) {
					}
				};
			}
		}, new NullProgressMonitor());
		TestLogger.debug("Working copy created.");
		return workingCopy;
	}

	/**
	 * @param unit
	 * @throws JavaModelException
	 */
	public static void saveAndClose(final ICompilationUnit unit) throws JavaModelException {
		try {
			if (unit.isWorkingCopy()) {
				TestLogger.debug("Reconciling unit...");
				unit.reconcile(AST.JLS8, ICompilationUnit.FORCE_PROBLEM_DETECTION, unit.getOwner(),
						new NullProgressMonitor());
				// Commit changes
				TestLogger.debug("Commiting working copy...");
				unit.commitWorkingCopy(true, null);
				// Destroy working copy
				TestLogger.debug("Discarding working copy...");
				unit.discardWorkingCopy();
			} else {
				unit.save(new NullProgressMonitor(), true);
			}
			// explicitly trigger the project build
			unit.getResource().refreshLocal(IResource.DEPTH_ONE, new NullProgressMonitor());
			unit.getJavaProject().getProject().build(IncrementalProjectBuilder.AUTO_BUILD, null);
	
		} catch (Exception e) {
			TestLogger.error("Failed to build project", e);
		}
	}

	public static void delete(final ICompilationUnit compilationUnit) throws CoreException {
		compilationUnit.delete(true, new NullProgressMonitor());
	}

	public static void delete(final IAnnotation annotation, final boolean useWorkingCopy) throws CoreException {
		final IMember parent = (IMember) annotation.getParent();
		ICompilationUnit unit = getCompilationUnit(parent.getCompilationUnit(), useWorkingCopy);
		IBuffer buffer = ((IOpenable) unit).getBuffer();
		final ISourceRange sourceRange = annotation.getSourceRange();
		buffer.replace(sourceRange.getOffset(), sourceRange.getLength(), "");
		saveAndClose(unit);
	}

	public static void delete(final IMember element) throws JavaModelException {
		ICompilationUnit compilationUnit = element.getCompilationUnit();
		element.delete(true, new NullProgressMonitor());
		saveAndClose(compilationUnit);
	}

	public static IMethod renameMethod(final ICompilationUnit compilationUnit, final String oldName, final String newName,
			final boolean useWorkingCopy) throws JavaModelException {
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

	/**
	 * @param monitor
	 * @param description
	 * @param projectName
	 * @param workspace
	 * @param project
	 * @throws InvocationTargetException
	 */
	static void createProject(final IProjectDescription description, final String projectName,
			final IWorkspace workspace, final IProject project) throws InvocationTargetException {
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
		NullProgressMonitor monitor = new NullProgressMonitor();
		try {
			monitor.beginTask(DataTransferMessages.WizardProjectsImportPage_CreateProjectsTask, 100);
			project.create(desc, new SubProgressMonitor(monitor, 30));
			project.open(IResource.BACKGROUND_REFRESH, new SubProgressMonitor(monitor, 70));
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		} finally {
			monitor.done();
		}
	}

	public static Annotation getAnnotation(final IMember member, final String annotationName) throws JavaModelException {
		if (annotationName == null) {
			return null;
		}
		return JdtUtils.resolveAnnotation(member, JdtUtils.parse(member, null), annotationName);
	}

	public static IType appendCompilationUnitType(final ICompilationUnit compilationUnit, final String resourceName,
			final boolean useWorkingCopy) throws CoreException {
		String content = ResourcesUtils.getBundleResourceContent(resourceName);
		int offset = 0;
		IType lastType = getLastTypeInSource(compilationUnit);
		if (lastType != null) {
			offset = lastType.getSourceRange().getOffset() + lastType.getSourceRange().getLength();
		}
		insertCodeAtLocation(compilationUnit, content, offset, useWorkingCopy);
		return getLastTypeInSource(compilationUnit);
	}

	private static IType getLastTypeInSource(final ICompilationUnit compilationUnit) throws JavaModelException {
		IType[] types = compilationUnit.getTypes();
		if (types != null && types.length > 0) {
			return types[types.length - 1];
		}
		return null;
	}

	public static void addImportDeclaration(final ICompilationUnit compilationUnit, String importType) throws JavaModelException {
		compilationUnit.createPackageDeclaration(importType, new NullProgressMonitor());
	}

	public static IResource getResource(final String typeName, final IJavaProject javaProject) throws CoreException {
		final IType type = JdtUtils.resolveType(typeName, javaProject, null);
		if(type != null) {
			return type.getResource();
		}
		return null;
	}

		
}
