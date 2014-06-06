package org.jboss.tools.ws.jaxrs.ui.internal.validation;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.DocumentProviderRegistry;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.jboss.tools.common.validation.ValidationErrorManager;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.AbstractJaxrsBaseElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.utils.Annotation;
import org.jboss.tools.ws.jaxrs.core.utils.JdtUtils;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.TestLogger;
import org.jboss.tools.ws.jaxrs.ui.quickfix.JaxrsMarkerResolutionGenerator;
import org.junit.Assert;

/**
 * @author Xavier Coulon The class name says it all.
 */
@SuppressWarnings("restriction")
public class ValidationUtils {

	/**
	 * Converts the given {@link IResource} elements into a set of {@link IFile}
	 * s
	 * 
	 * @param elements
	 * @return the set containing the given elements
	 */
	public static Set<IFile> toSet(final IResource... elements) {
		final Set<IFile> result = new HashSet<IFile>();
		for (IResource element : elements) {
			result.add((IFile) element);
		}
		return result;
	}

	/**
	 * find JAX-RS Markers on the given resources.
	 * 
	 * @param element
	 * @return
	 * @throws CoreException
	 */
	public static IMarker[] findJaxrsMarkers(final IJaxrsElement... elements) throws CoreException {
		final List<IMarker> markers = new ArrayList<IMarker>();
		for (IJaxrsElement element : elements) {
			final IMarker[] elementMarkers = element.getResource().findMarkers(
					JaxrsMetamodelValidator.JAXRS_PROBLEM_MARKER_ID, true, IResource.DEPTH_INFINITE);
			switch (element.getElementKind().getCategory()) {
			case APPLICATION:
			case HTTP_METHOD:
			case NAME_BINDING:
			case PROVIDER:
			case PARAM_CONVERTER_PROVIDER:
			case RESOURCE:
				for (IMarker marker : elementMarkers) {
					markers.add(marker);
				}
				break;
			case RESOURCE_METHOD:
				final IMarker[] resourceMarkers = elementMarkers;
				final ISourceRange methodSourceRange = ((JaxrsResourceMethod) element).getJavaElement()
						.getSourceRange();
				for (IMarker marker : resourceMarkers) {
					final int markerCharStart = marker.getAttribute(IMarker.CHAR_START, -1);
					if (markerCharStart >= methodSourceRange.getOffset()
							&& markerCharStart <= (methodSourceRange.getOffset() + methodSourceRange.getLength())) {
						markers.add(marker);
					}
				}
				break;
			default:
				break;
			}
		}
		printMarkers(markers);
		return markers.toArray(new IMarker[markers.size()]);
	}

	/**
	 * Finds JAX-RS Markers <strong>at the project level only</strong>, not on
	 * the children resources of this project !
	 * 
	 * @param project
	 * @return
	 * @throws CoreException
	 */
	public static IMarker[] findJaxrsMarkers(IProject project) throws CoreException {
		return project.findMarkers(JaxrsMetamodelValidator.JAXRS_PROBLEM_MARKER_ID, false, 0);
	}

	/**
	 * @param element
	 * @throws CoreException
	 */
	public static void deleteJaxrsMarkers(final AbstractJaxrsBaseElement element) throws CoreException {
		element.getResource().deleteMarkers(JaxrsMetamodelValidator.JAXRS_PROBLEM_MARKER_ID, false,
				IResource.DEPTH_INFINITE);
	}

	/**
	 * @param element
	 * @throws CoreException
	 */
	public static void deleteJaxrsMarkers(final IResource resource) throws CoreException {
		resource.deleteMarkers(JaxrsMetamodelValidator.JAXRS_PROBLEM_MARKER_ID, false, IResource.DEPTH_INFINITE);
	}

	/**
	 * Reset JAX-RS Markers on the given {@link IJaxrsMetamodel} and all its
	 * children elements.
	 * 
	 * @param metamodel
	 *            the metamodel to clean
	 * @throws CoreException
	 */
	public static void deleteJaxrsMarkers(final JaxrsMetamodel metamodel) throws CoreException {
		metamodel.getProject().deleteMarkers(JaxrsMetamodelValidator.JAXRS_PROBLEM_MARKER_ID, false,
				IResource.DEPTH_INFINITE);
		metamodel.resetProblemLevel();
		final List<IJaxrsElement> allElements = metamodel.getAllElements();
		for (IJaxrsElement element : allElements) {
			((AbstractJaxrsBaseElement) element).resetProblemLevel();
		}
	}

	public static Matcher<IMarker[]> hasPreferenceKey(String javaApplicationInvalidTypeHierarchy) {
		return new MarkerPreferenceKeyMatcher(javaApplicationInvalidTypeHierarchy);
	}

	static class MarkerPreferenceKeyMatcher extends BaseMatcher<IMarker[]> {

		final String expectedProblemType;

		MarkerPreferenceKeyMatcher(final String expectedProblemType) {
			this.expectedProblemType = expectedProblemType;
		}

		@Override
		public boolean matches(Object item) {
			if (item instanceof IMarker[]) {
				for (IMarker marker : (IMarker[]) item) {
					final String preferenceKey = marker.getAttribute(
							ValidationErrorManager.PREFERENCE_KEY_ATTRIBUTE_NAME, "");
					if (preferenceKey.equals(expectedProblemType)) {
						return true;
					}
				}
			}
			return false;
		}

		@Override
		public void describeTo(Description description) {
			description.appendText("marker contains preference_key (\"" + expectedProblemType + "\" problem type)");
		}

	}

	public static void printMarkers(final List<IMarker> markers) {
		for (IMarker marker : markers) {
			TestLogger.debug(" Marker with severity={}: {}", marker.getAttribute(IMarker.SEVERITY, 0),
					marker.getAttribute(IMarker.MESSAGE, ""));
		}
	}

	/**
	 * @param annotation
	 * @return
	 * @throws JavaModelException
	 * @throws CoreException
	 */
	public static IJavaCompletionProposal[] getJavaCompletionProposals(final Annotation annotation)
			throws JavaModelException, CoreException {
		final IAnnotation javaAnnotation = annotation.getJavaAnnotation();
		final ICompilationUnit compilationUnit = (ICompilationUnit) javaAnnotation.getAncestor(IJavaElement.COMPILATION_UNIT);
		final ISourceRange sourceRange = javaAnnotation.getSourceRange();
		final IInvocationContext invocationContext = getInvocationContext(compilationUnit, sourceRange.getOffset(), sourceRange.getLength());
		final IProblemLocation[] problemLocations = getProblemLocations(javaAnnotation);
		final IJavaCompletionProposal[] proposals = new JaxrsMarkerResolutionGenerator().getCorrections(invocationContext, problemLocations);
		return proposals;
	}
	
	private static IInvocationContext getInvocationContext(final ICompilationUnit compilationUnit, final int offset,
			final int length) {
		return new IInvocationContext() {

			@Override
			public int getSelectionOffset() {
				return offset;
			}

			@Override
			public int getSelectionLength() {
				return length;
			}

			@Override
			public ASTNode getCoveringNode() {
				return null;
			}

			@Override
			public ASTNode getCoveredNode() {
				return null;
			}

			@Override
			public ICompilationUnit getCompilationUnit() {
				return compilationUnit;
			}

			@Override
			public CompilationUnit getASTRoot() {
				try {
					return JdtUtils.parse(compilationUnit, null);
				} catch (JavaModelException e) {
					Assert.fail("Failed to parse the compilationUnit");
				}
				return null;
			}
		};
	}

	private static IProblemLocation[] getProblemLocations(final IAnnotation annotation) throws JavaModelException {
		final CompilationUnit ast = JdtUtils.parse(annotation.getAncestor(IJavaElement.COMPILATION_UNIT), null);
		final IProblem[] problems = ast.getProblems();
		for (IProblem problem : problems) {
			if ((problem.getSourceStart() >= annotation.getSourceRange().getOffset())
					&& (problem.getSourceStart() <= (annotation.getSourceRange().getLength() + annotation
							.getSourceRange().getOffset()))) {
				return new IProblemLocation[] { new ProblemLocation(problem) };
			}
		}
		return null;
	}

	public static void applyProposals(final IJaxrsElement jaxrsElement, final IJavaCompletionProposal[] proposals) throws CoreException {
		final FileEditorInput input = new FileEditorInput((IFile) jaxrsElement.getResource());
		final IDocumentProvider provider = DocumentProviderRegistry.getDefault().getDocumentProvider(input);
		try {
			provider.connect(input);
		} catch (CoreException e) {
			fail("Failed to connect provider to input:" + e.getMessage());
		}
		final IDocument document = provider.getDocument(input);
		for(IJavaCompletionProposal proposal : proposals) {
			proposal.apply(document);
		}
		provider.saveDocument(new NullProgressMonitor(), input, document, true);
	}

	public static IProblem[] findJavaProblems(final IResource resource) throws JavaModelException {
		final CompilationUnit ast = JdtUtils.parse(JavaCore.create(resource),null);
		final IProblem[] problems = ast.getProblems();
		// only care about errors
		final List<IProblem> errors = new ArrayList<IProblem>();
		for(IProblem problem : problems) {
			if(problem.isError()) {
				errors.add(problem);
			}
		}
		return errors.toArray(new IProblem[errors.size()]);
	}
	
}