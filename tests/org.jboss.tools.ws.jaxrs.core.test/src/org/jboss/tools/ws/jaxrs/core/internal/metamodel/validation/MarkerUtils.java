package org.jboss.tools.ws.jaxrs.core.internal.metamodel.validation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ISourceRange;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.jboss.tools.common.validation.ValidationErrorManager;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsBaseElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;

/**
 * @author Xavier Coulon The class name says it all.
 */
public class MarkerUtils {

	/**
	 * find JAX-RS Markers on the given resources.
	 * 
	 * @param element
	 * @return
	 * @throws CoreException
	 */
	public static IMarker[] findJaxrsMarkers(final IJaxrsElement... elements) throws CoreException {
		List<IMarker> markers = new ArrayList<IMarker>();
		for (IJaxrsElement element : elements) {
			final IMarker[] elementMarkers = element.getResource().findMarkers(JaxrsValidationConstants.JAXRS_PROBLEM_TYPE, true,
					IResource.DEPTH_INFINITE);
			switch (element.getElementKind().getCategory()) {
			case APPLICATION:
			case HTTP_METHOD:
			case PROVIDER:
			case RESOURCE:
				for(IMarker marker : elementMarkers) {
					markers.add(marker);
				}
				break;
			case RESOURCE_METHOD:
				final IMarker[] resourceMarkers = elementMarkers;
				final List<IMarker> resourceMethodMarkers = new ArrayList<IMarker>();
				final ISourceRange methodSourceRange = ((JaxrsResourceMethod) element).getJavaElement()
						.getSourceRange();

				for (IMarker marker : resourceMarkers) {
					final int markerCharStart = marker.getAttribute(IMarker.CHAR_START, -1);
					if (markerCharStart >= methodSourceRange.getOffset()
							&& markerCharStart <= (methodSourceRange.getOffset() + methodSourceRange.getLength())) {
						resourceMethodMarkers.add(marker);
					}
				}
				markers.addAll(resourceMethodMarkers);
				break;
			default:
				break;
			}
		}
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
		return project.findMarkers(JaxrsValidationConstants.JAXRS_PROBLEM_TYPE, false, 0);
	}

	/**
	 * @param element
	 * @throws CoreException
	 */
	public static void deleteJaxrsMarkers(final JaxrsBaseElement element) throws CoreException {
		element.getResource().deleteMarkers(JaxrsValidationConstants.JAXRS_PROBLEM_TYPE, false,
				IResource.DEPTH_INFINITE);
	}

	/**
	 * @param element
	 * @throws CoreException
	 */
	public static void deleteJaxrsMarkers(final IResource resource) throws CoreException {
		resource.deleteMarkers(JaxrsValidationConstants.JAXRS_PROBLEM_TYPE, false, IResource.DEPTH_INFINITE);
	}

	/**
	 * Reset JAX-RS Markers
	 * @param element
	 * @throws CoreException
	 */
	public static void deleteJaxrsMarkers(final IProject project) throws CoreException {
		project.deleteMarkers(JaxrsValidationConstants.JAXRS_PROBLEM_TYPE, false, IResource.DEPTH_INFINITE);
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

}