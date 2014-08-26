/******************************************************************************* 
 * Copyright (c) 2013 - 2014 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.ws.jaxrs.ui.internal.validation;

import static org.jboss.tools.ws.jaxrs.core.validation.IJaxrsValidation.JAXRS_PROBLEM_MARKER_ID;
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
import org.eclipse.wst.validation.internal.provisional.core.IMessage;
import org.eclipse.wst.validation.internal.provisional.core.IReporter;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.jboss.tools.common.validation.ValidationErrorManager;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsBaseElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsJavaElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsParameterAggregator;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.jdt.RangeUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsParameterAggregatorField;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsParameterAggregatorProperty;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResourceField;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResourceProperty;
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
			if (element == null) {
				continue;
			}
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
			if (element.getResource() == null) {
				continue;
			}
			final IMarker[] elementMarkers = element.getResource().findMarkers(
					JAXRS_PROBLEM_MARKER_ID, true, IResource.DEPTH_INFINITE);
			switch (element.getElementKind().getCategory()) {
			case APPLICATION:
			case HTTP_METHOD:
			case NAME_BINDING:
			case PROVIDER:
			case PARAM_CONVERTER_PROVIDER:
			case PARAMETER_AGGREGATOR:
			case RESOURCE:
				for (IMarker marker : elementMarkers) {
					markers.add(marker);
				}
				break;
			case RESOURCE_METHOD:
			case RESOURCE_FIELD:
			case RESOURCE_PROPERTY:
			case PARAMETER_AGGREGATOR_FIELD:
			case PARAMETER_AGGREGATOR_PROPERTY:
				final ISourceRange methodSourceRange = ((JaxrsJavaElement<?>) element).getJavaElement()
						.getSourceRange();
				for (IMarker marker : elementMarkers) {
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

	public static IMessage[] findJaxrsMessages(final IReporter reporter, final IJaxrsElement... elements) {
		@SuppressWarnings("unchecked")
		final List<IMessage> messages = reporter.getMessages();

		return messages.toArray(new IMessage[messages.size()]);
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
		return project.findMarkers(JAXRS_PROBLEM_MARKER_ID, false, 0);
	}

	/**
	 * @param element
	 * @throws CoreException
	 */
	public static void deleteJaxrsMarkers(final JaxrsBaseElement element) throws CoreException {
		if (element.getResource() == null) {
			return;
		}
		element.getResource().deleteMarkers(JAXRS_PROBLEM_MARKER_ID, false,
				IResource.DEPTH_INFINITE);
	}

	/**
	 * @param element
	 * @throws CoreException
	 */
	public static void deleteJaxrsMarkers(final IResource resource) throws CoreException {
		resource.deleteMarkers(JAXRS_PROBLEM_MARKER_ID, false, IResource.DEPTH_INFINITE);
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
		metamodel.getProject().deleteMarkers(JAXRS_PROBLEM_MARKER_ID, false,
				IResource.DEPTH_INFINITE);
		metamodel.resetProblemLevel();
		final List<IJaxrsElement> allElements = metamodel.getAllElements();
		for (IJaxrsElement element : allElements) {
			((JaxrsBaseElement) element).resetProblemLevel();
		}
	}

	public static Matcher<IMarker[]> havePreferenceKey(final String expectedProblemType) {
		return new BaseMatcher<IMarker[]>() {
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
		};
	}
	
	public static Matcher<IMarker> hasPreferenceKey(final String expectedProblemType) {
		return new BaseMatcher<IMarker>() {
			@Override
			public boolean matches(Object item) {
				if (item instanceof IMarker) {
					final  IMarker marker = (IMarker) item;
					final String preferenceKey = marker.getAttribute(
							ValidationErrorManager.PREFERENCE_KEY_ATTRIBUTE_NAME, "");
					if (preferenceKey.equals(expectedProblemType)) {
						return true;
					}
				}
				return false;
			}
			
			@Override
			public void describeTo(Description description) {
				description.appendText("marker contains preference_key (\"" + expectedProblemType + "\" problem type)");
			}
		};
	}

	public static Matcher<IMarker> matchesLocation(final ISourceRange range) {
		return new BaseMatcher<IMarker>() {

			@Override
			public boolean matches(Object item) {
				if (item instanceof IMarker) {
					final IMarker marker = (IMarker) item;
					final int markerStart = marker.getAttribute(IMarker.CHAR_START, 0);
					final int markerEnd = marker.getAttribute(IMarker.CHAR_END, 0);
					return (markerStart == range.getOffset() && markerEnd == (range.getOffset() + range.getLength()));
				}
				return false;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("marker location:" + range);
			}

		};
	}

	public static void printMarkers(final List<IMarker> markers) {
		TestLogger.debug("Found {} markers", markers.size());
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
		final ICompilationUnit compilationUnit = (ICompilationUnit) javaAnnotation
				.getAncestor(IJavaElement.COMPILATION_UNIT);
		final ISourceRange sourceRange = javaAnnotation.getSourceRange();
		final IInvocationContext invocationContext = getInvocationContext(compilationUnit, sourceRange.getOffset(),
				sourceRange.getLength());
		final IProblemLocation[] problemLocations = getProblemLocations(javaAnnotation);
		final IJavaCompletionProposal[] proposals = new JaxrsMarkerResolutionGenerator().getCorrections(
				invocationContext, problemLocations);
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

	public static void applyProposals(final IJaxrsElement jaxrsElement, final IJavaCompletionProposal[] proposals)
			throws CoreException {
		final FileEditorInput input = new FileEditorInput((IFile) jaxrsElement.getResource());
		final IDocumentProvider provider = DocumentProviderRegistry.getDefault().getDocumentProvider(input);
		try {
			provider.connect(input);
		} catch (CoreException e) {
			fail("Failed to connect provider to input:" + e.getMessage());
		}
		final IDocument document = provider.getDocument(input);
		for (IJavaCompletionProposal proposal : proposals) {
			proposal.apply(document);
		}
		provider.saveDocument(new NullProgressMonitor(), input, document, true);
	}

	public static IProblem[] findJavaProblems(final IResource resource) throws JavaModelException {
		final CompilationUnit ast = JdtUtils.parse(JavaCore.create(resource), null);
		final IProblem[] problems = ast.getProblems();
		// only care about errors
		final List<IProblem> errors = new ArrayList<IProblem>();
		for (IProblem problem : problems) {
			if (problem.isError()) {
				errors.add(problem);
			}
		}
		return errors.toArray(new IProblem[errors.size()]);
	}

	/**
	 * @param markers
	 * @param metamodel
	 * @return a set of {@link IJaxrsEndpoint} whose underlying {@link IJaxrsElement} have one of the given {@link IMarker}s
	 * @throws JavaModelException 
	 */
	public static List<IJaxrsEndpoint> findEndpointsByMarkers(final IMarker[] markers, final JaxrsMetamodel metamodel) throws JavaModelException {
		final Set<IJaxrsEndpoint> endpoints = new HashSet<IJaxrsEndpoint>();
		for(IMarker marker : markers) {
			final Set<IJaxrsElement> elements = metamodel.findElements(marker.getResource());
			for(IJaxrsElement element : elements) {
				switch (element.getElementKind().getCategory()) {
				case RESOURCE:
					final JaxrsResource resource = (JaxrsResource) element;
					for (IJaxrsResourceField resourceField : resource.getAllFields()) {
						if(matchesLocation(marker, resourceField)) {
							endpoints.addAll(metamodel.findEndpoints(resourceField));
						}
					}
					for (IJaxrsResourceProperty resourceProperty : resource.getAllProperties()) {
						if(matchesLocation(marker, resourceProperty)) {
							endpoints.addAll(metamodel.findEndpoints(resourceProperty));
						}
					}
					for (IJaxrsResourceMethod resourceMethod : resource.getAllMethods()) {
						if(matchesLocation(marker, resourceMethod)) {
							endpoints.addAll(metamodel.findEndpoints(resourceMethod));
						}
					}
					break;
				case PARAMETER_AGGREGATOR:
					final JaxrsParameterAggregator parameterAggregator = (JaxrsParameterAggregator) element;
					for (IJaxrsParameterAggregatorField parameterAggregatorField : parameterAggregator.getAllFields()) {
						if(matchesLocation(marker, parameterAggregatorField)) {
							endpoints.addAll(metamodel.findEndpoints(parameterAggregatorField));
						}
					}
					for (IJaxrsParameterAggregatorProperty parameterAggregatorProperty : parameterAggregator.getAllProperties()) {
						if(matchesLocation(marker, parameterAggregatorProperty)) {
							endpoints.addAll(metamodel.findEndpoints(parameterAggregatorProperty));
						}
					}
					continue;
				default:
					if(matchesLocation(marker, element)) {
						endpoints.addAll(metamodel.findEndpoints(element));
					}
				}
			}
		}
		
		return new ArrayList<IJaxrsEndpoint>(endpoints);
	}

	/**
	 * @return {@code true} if the given {@link IMarker} is located in the underlying {@link IJavaElement} of the given {@link IJaxrsElement}, {@code false} otherwise.
	 * 
	 * @param marker
	 * @param element
	 * @throws JavaModelException
	 */
	private static boolean matchesLocation(IMarker marker, IJaxrsElement element) throws JavaModelException {
		if (element instanceof JaxrsJavaElement<?>) {
			final ISourceRange sourceRange = ((JaxrsJavaElement<?>) element).getJavaElement().getSourceRange();
			final int markerStartPosition = marker.getAttribute(IMarker.CHAR_START, 0);
			if (RangeUtils.matches(sourceRange, markerStartPosition)) {
				return true;
			}
		}
		return false;
	}
}