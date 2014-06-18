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
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder;

import static org.eclipse.jdt.core.IJavaElementDelta.ADDED;
import static org.eclipse.jdt.core.IJavaElementDelta.CHANGED;
import static org.eclipse.jdt.core.IJavaElementDelta.REMOVED;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.jboss.tools.ws.jaxrs.core.internal.utils.ConstantUtils;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.wtp.WtpUtils;

/**
 * Scans and filters the IJavaElementDelta and IResourceDelta (including their children and annotations) and returns a
 * list of JavaElementChangedEvents that match with the JavaElementChangedEventFilter rules.
 * 
 * @author xcoulon
 * @see @{IJavaElementDelta}
 * @see @{IResourceDelta}
 * @see @{JavaElementChangedEvent}
 * @see @{JavaElementChangedEventFilter}
 */
public class ResourceDeltaScanner {

	private final ResourceDeltaFilter filter = new ResourceDeltaFilter();

	public List<ResourceDelta> scanAndFilterEvent(final IResourceDelta delta, final IProgressMonitor progressMonitor)
			throws CoreException {
		try {
			progressMonitor.beginTask("Analysing changes", 1);
			Logger.debug("Some resources changed...");
			return scanDelta(delta);
		} finally {
			progressMonitor.done();
		}
	}

	/**
	 * Analyses the given ResourceDelta and its children and returns a list of relevant events for further processing.
	 * These events may be {@link JavaElementChangedEvent} or {@link ResourceDelta} depending on the nature of the affected
	 * resource (ie: a resource of a compilation unit or a web deployement descriptor/web fragment, respectively).
	 * 
	 * @param delta
	 *            the delta to analyse
	 * @return the list of relevant events.
	 * @throws CoreException
	 *             in case of underlying exception.
	 */
	private List<ResourceDelta> scanDelta(final IResourceDelta delta) throws CoreException {
		final List<ResourceDelta> events = new ArrayList<ResourceDelta>();
		if (delta == null) {
			return Collections.emptyList();
		}
		
		final IResource resource = delta.getResource();
		// skip as the project is closed
		if (resource.getType() == IResource.PROJECT && !((IProject) resource).isOpen()) {
			return Collections.emptyList();
		}
		// FIXME: duplicate code: testing that the resouce if a java file here,
		// and in the filter too.
		final int flags = delta.getFlags();
		if (WtpUtils.isWebDeploymentDescriptor(resource)) {
			events.add(new ResourceDelta(resource, delta.getKind(), 0));
		} else {

			final boolean isJavaFile = resource.getType() == IResource.FILE
					&& ("java").equals(resource.getFileExtension());
			final boolean javaFileAdded = isJavaFile && delta.getKind() == ADDED;
			final boolean javaFileChanged = isJavaFile && delta.getKind() == CHANGED;
			final boolean javaFileRemoved = isJavaFile && delta.getKind() == REMOVED;
			final boolean javaFileMarkersChanged = javaFileChanged && (delta.getFlags() & IResourceDelta.MARKERS) != 0;
			if ((javaFileAdded || javaFileRemoved)) {
				Logger.debug("File {}  {}", resource,
						ConstantUtils.getStaticFieldName(IResourceDelta.class, delta.getKind()));
				ResourceDelta event = new ResourceDelta(resource, delta.getKind(), flags);
				if (filter.applyRules(event)) {
					events.add(event);
				}
			} else if (javaFileChanged && !javaFileMarkersChanged) {
				Logger.debug("File {}  {}", resource,
						ConstantUtils.getStaticFieldName(IResourceDelta.class, delta.getKind()));
				ResourceDelta event = new ResourceDelta(resource, delta.getKind(), flags);
				if (filter.applyRules(event)) {
					events.add(event);
				}
			} else if (javaFileChanged && javaFileMarkersChanged) {
				IMarkerDelta[] markerDeltas = delta.getMarkerDeltas();
				for (IMarkerDelta markerDelta : markerDeltas) {
					int severity = markerDelta.getAttribute(IMarker.SEVERITY, 0);
					String type = markerDelta.getType();
					String message = markerDelta.getAttribute(IMarker.MESSAGE, "");
					if (severity == IMarker.SEVERITY_ERROR && type.equals(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER)) {
						Logger.debug("Marker delta: {} [{}] {}: \"{}\" at line {} (id={})", markerDelta.getResource()
								.getName(), ConstantUtils.getStaticFieldName(IResourceDelta.class,
								markerDelta.getKind()), ConstantUtils.getStaticFieldName(IMarker.class, severity,
								"SEVERITY_"), message, markerDelta.getAttribute(IMarker.LINE_NUMBER), markerDelta
								.getId());
						int flag = markerDelta.getKind() == IResourceDelta.ADDED ? IJavaElementDeltaFlag.F_MARKER_ADDED
								: IJavaElementDeltaFlag.F_MARKER_REMOVED;
						ResourceDelta event = new ResourceDelta(resource, CHANGED, flag);
						if (filter.applyRules(event)) {
							events.add(event);
						}
					}
				}
			} else {
				Logger.trace("**ignored** Delta {} [{}]", delta.getResource().getFullPath(),
						ConstantUtils.getStaticFieldName(IResourceDelta.class, delta.getKind()));
			}
		}
		for (IResourceDelta childDelta : delta.getAffectedChildren()) {
			events.addAll(scanDelta(childDelta));
		}
		return events;
	}

}
