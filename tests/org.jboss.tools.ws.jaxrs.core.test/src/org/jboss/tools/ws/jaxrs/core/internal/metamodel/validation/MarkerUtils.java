package org.jboss.tools.ws.jaxrs.core.internal.metamodel.validation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ISourceRange;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsBaseElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;

/**
 * @author Xavier Coulon
 * The class name says it all. 
 */
public class MarkerUtils {

	/**
	 * @param element
	 * @return
	 * @throws CoreException
	 */
	public static IMarker[] findJaxrsMarkers(final JaxrsBaseElement element) throws CoreException {
		switch (element.getElementCategory()) {
		case HTTP_METHOD:
		case RESOURCE:
			return element.getResource().findMarkers(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, true,
					IResource.DEPTH_INFINITE);
		case RESOURCE_METHOD:
			final IMarker[] markers = element.getResource().findMarkers(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE,
					true, IResource.DEPTH_INFINITE);
			final List<IMarker> resourceMethodMarkers = new ArrayList<IMarker>();
			final ISourceRange methodSourceRange = ((JaxrsResourceMethod) element).getJavaElement().getSourceRange();
	
			for (IMarker marker : markers) {
				final int markerCharStart = marker.getAttribute(IMarker.CHAR_START, -1);
				if (markerCharStart >= methodSourceRange.getOffset()
						&& markerCharStart <= (methodSourceRange.getOffset() + methodSourceRange.getLength())) {
					resourceMethodMarkers.add(marker);
				}
			}
			return resourceMethodMarkers.toArray(new IMarker[resourceMethodMarkers.size()]);
		default:
			return new IMarker[0];
		}
	}
	
	/**
	 * @param element
	 * @throws CoreException
	 */
	public static void deleteJaxrsMarkers(final JaxrsBaseElement element) throws CoreException {
		element.getResource()
				.deleteMarkers(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, false, IResource.DEPTH_INFINITE);
	}

	/**
	 * @param element
	 * @throws CoreException
	 */
	public static void deleteJaxrsMarkers(final IProject project) throws CoreException {
		project.deleteMarkers(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, false, IResource.DEPTH_INFINITE);
	}

}