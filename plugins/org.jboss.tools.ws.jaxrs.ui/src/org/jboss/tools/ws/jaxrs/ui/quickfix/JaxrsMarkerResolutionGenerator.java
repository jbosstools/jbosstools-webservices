  package org.jboss.tools.ws.jaxrs.ui.quickfix;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import org.jboss.tools.common.validation.ValidationErrorManager;
import org.jboss.tools.ws.jaxrs.core.utils.JdtUtils;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.ui.internal.validation.JaxrsMarkerResolutionIds;

public class JaxrsMarkerResolutionGenerator implements IMarkerResolutionGenerator2 {

	@Override
	public IMarkerResolution[] getResolutions(IMarker marker) {
		return getMarkerResolutions(marker);
	}

	@Override
	public boolean hasResolutions(IMarker marker) {
		return getMarkerResolutions(marker).length > 0;
	}

	/**
	 * Null-safe extraction of the potential marker resolutions bound to this marker.
	 * 
	 * @param marker
	 *            the marker
	 * @return a array of marker resolutions. If no resolution is bound to the marker, the returned array is empty (not
	 *         null).
	 */
	private IMarkerResolution[] getMarkerResolutions(final IMarker marker) {
		try {
			final int quickfixId = getQuickFixID(marker);
			final ICompilationUnit compilationUnit = JdtUtils.getCompilationUnit(marker.getResource());
			final IType type = (IType) JdtUtils.getElementAt(compilationUnit,
					marker.getAttribute(IMarker.CHAR_START, 0), IJavaElement.TYPE);
			if (type != null) {
				switch (quickfixId) {
				case JaxrsMarkerResolutionIds.HTTP_METHOD_MISSING_TARGET_ANNOTATION_QUICKFIX_ID:
				case JaxrsMarkerResolutionIds.NAME_BINDING_MISSING_TARGET_ANNOTATION_QUICKFIX_ID:
					return new IMarkerResolution[] { new AddTargetAnnotationMarkerResolution(type) };
				case JaxrsMarkerResolutionIds.HTTP_METHOD_MISSING_RETENTION_ANNOTATION_QUICKFIX_ID:
				case JaxrsMarkerResolutionIds.NAME_BINDING_MISSING_RETENTION_ANNOTATION_QUICKFIX_ID:
					return new IMarkerResolution[] { new AddRetentionAnnotationMarkerResolution(type) };
				case JaxrsMarkerResolutionIds.HTTP_METHOD_INVALID_TARGET_ANNOTATION_VALUE_QUICKFIX_ID:
				case JaxrsMarkerResolutionIds.NAME_BINDING_INVALID_TARGET_ANNOTATION_VALUE_QUICKFIX_ID:
					return new IMarkerResolution[] { new UpdateTargetAnnotationValueMarkerResolution(type) };
				case JaxrsMarkerResolutionIds.HTTP_METHOD_INVALID_RETENTION_ANNOTATION_VALUE_QUICKFIX_ID:
				case JaxrsMarkerResolutionIds.NAME_BINDING_INVALID_RETENTION_ANNOTATION_VALUE_QUICKFIX_ID:
					return new IMarkerResolution[] { new UpdateRetentionAnnotationValueMarkerResolution(type) };
				}
			}
		} catch (CoreException e) {
			Logger.error("Failed to retrieve marker resolution", e);
		}
		return new IMarkerResolution[0];
	}

	/**
	 * return message id or -1 if impossible to find
	 * 
	 * @param marker
	 * @return
	 */
	private int getQuickFixID(IMarker marker) throws CoreException {
		return ((Integer) marker.getAttribute(ValidationErrorManager.MESSAGE_ID_ATTRIBUTE_NAME, -1));
	}

}
