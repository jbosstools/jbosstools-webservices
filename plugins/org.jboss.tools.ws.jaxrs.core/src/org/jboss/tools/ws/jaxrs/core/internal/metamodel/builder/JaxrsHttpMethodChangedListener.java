/**
 * 
 */
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.search.JavaElementsSearcher;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElementChangedListener;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta;
import org.jboss.tools.ws.jaxrs.core.utils.Annotation;
import org.jboss.tools.ws.jaxrs.core.utils.CompilationUnitsRepository;
import org.jboss.tools.ws.jaxrs.core.utils.JdtUtils;

/**
 * A specific {@link IJaxrsElementChangedListener} that will respond to changes on {@link JaxrsHttpMethod}s.
 * 
 * @author xcoulon
 *
 */
public class JaxrsHttpMethodChangedListener implements IJaxrsElementChangedListener {
	/**
	 * Will attempt to create {@link JaxrsResourceMethod} when a new
	 * {@link JaxrsHttpMethod} has been created, or will remove some existing
	 * {@link JaxrsResourceMethod} when a {@link JaxrsHttpMethod} has been
	 * removed (if relevant).
	 * 
	 * @param delta
	 *            the JAX-RS changed that occurred and that should be processed,
	 *            if relevant
	 */
	@Override
	public void notifyElementChanged(JaxrsElementDelta delta) {
		// FIXME: report this whole fix in a separate JIRA
		if (delta != null && delta.getElement().getElementKind() == EnumElementKind.HTTP_METHOD) {
			try {
				final JaxrsHttpMethod httpMethod = (JaxrsHttpMethod) delta.getElement();
				// ignore events on built-in HTTP Methods
				if(httpMethod.isBuiltIn()) {
					return;
				}
				final JaxrsMetamodel metamodel = (JaxrsMetamodel) delta.getElement().getMetamodel();
				final String httpMethodClassName = httpMethod.getJavaClassName();
				final List<IMethod> affectedMethods = getAffectedMethods(delta);
				final JavaElementDelta affectedMethodsDelta = new JavaElementDelta(metamodel.getJavaProject(), null, IJavaElementDelta.CHANGED, 0);
				for(IMethod affectedMethod : affectedMethods) {
					//FIXME: do we need an intermediate level with methods ?
					final CompilationUnit ast = CompilationUnitsRepository.getInstance().getAST(affectedMethod.getCompilationUnit());
					final JavaElementDelta affectedMethodDelta = new JavaElementDelta(affectedMethod, ast, IJavaElementDelta.CHANGED, 0);
					final Annotation httpMethodAnnotation = JdtUtils.resolveAnnotation(affectedMethod, ast, httpMethodClassName);
					if(httpMethodAnnotation != null) {
						affectedMethodDelta.addAffectedAnnotation(new JavaElementDelta(httpMethodAnnotation.getJavaAnnotation(), ast, delta.getDeltaKind(), 0));
						affectedMethodsDelta.addAffectedElementDelta(affectedMethodDelta);
					}
				}
				final JavaElementChangedBuildJob elementChangedBuildJob = new JavaElementChangedBuildJob(new ElementChangedEvent(affectedMethodsDelta, IJavaElementDelta.CHANGED));
				elementChangedBuildJob.execute();
			} catch(CoreException e) {
				Logger.error("Failed to process change after HTTP Method addition/removal", e);
			}
		}
	}

	/**
	 * @param delta the {@link IJavaElementDelta}
	 * @return the list of {@link IMethod} that are affected by the given change.
	 * @throws CoreException 
	 */
	private List<IMethod> getAffectedMethods(final JaxrsElementDelta delta) throws CoreException {
		final JaxrsMetamodel metamodel = (JaxrsMetamodel) delta.getElement().getMetamodel();
		final JaxrsHttpMethod httpMethod = (JaxrsHttpMethod) delta.getElement();
		final String httpMethodClassName = httpMethod.getJavaClassName();
		
		switch (delta.getDeltaKind()) {
		case IJavaElementDelta.ADDED:
			// find all Java Methods that have the added HTTP Method and process a fake 'annotation addition' event on these methods
			return JavaElementsSearcher.findAnnotatedMethods(metamodel.getJavaProject(), httpMethodClassName, new NullProgressMonitor());
		case IJavaElementDelta.REMOVED:
			// retrieve all known JAX-RS Resource Methods that have the remove HTTP Method process a fake 'annotation removal' event on these resource methods
			final List<IJaxrsResourceMethod> annotatedResourceMethods = metamodel.findResourceMethodsByAnnotation(httpMethodClassName);
			final List<IMethod> affectedMethods = new ArrayList<IMethod>();
			for(IJaxrsResourceMethod resourceMethod : annotatedResourceMethods) {
				affectedMethods.add(resourceMethod.getJavaElement());
			}
			return affectedMethods;
		default:
			return Collections.emptyList();
		}
	}
}
