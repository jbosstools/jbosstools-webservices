package org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder;

import static org.eclipse.jdt.core.IJavaElement.ANNOTATION;
import static org.eclipse.jdt.core.IJavaElement.COMPILATION_UNIT;
import static org.eclipse.jdt.core.IJavaElement.FIELD;
import static org.eclipse.jdt.core.IJavaElement.JAVA_PROJECT;
import static org.eclipse.jdt.core.IJavaElement.METHOD;
import static org.eclipse.jdt.core.IJavaElement.PACKAGE_FRAGMENT_ROOT;
import static org.eclipse.jdt.core.IJavaElement.TYPE;
import static org.eclipse.jdt.core.IJavaElementDelta.ADDED;
import static org.eclipse.jdt.core.IJavaElementDelta.CHANGED;
import static org.eclipse.jdt.core.IJavaElementDelta.REMOVED;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementChangedEvent.F_NONE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.HttpMethod;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsElementFactory;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsParamField;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.CompilationUnitsRepository;
import org.jboss.tools.ws.jaxrs.core.jdt.JaxrsAnnotationsScanner;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsResourceMethod;

public class JavaElementChangedProcessor {

	private final JaxrsElementFactory factory = new JaxrsElementFactory();

	public List<JaxrsElementChangedEvent> processEvents(List<JavaElementChangedEvent> events,
			IProgressMonitor progressMonitor) {
		final List<JaxrsElementChangedEvent> impacts = new ArrayList<JaxrsElementChangedEvent>();
		try {
			progressMonitor.beginTask("Processing {} Java change(s)...", events.size());
			Logger.debug("Processing {} Java change(s)...", events.size());
			for (JavaElementChangedEvent event : events) {
				impacts.addAll(processEvent(event, progressMonitor));
				progressMonitor.worked(1);
			}
		} catch (CoreException e) {
			Logger.error("Failed while processing Java changes", e);
			impacts.clear();
		} finally {
			progressMonitor.done();
			Logger.debug("Done processing Java changes.");

		}
		return impacts;
	}

	private List<JaxrsElementChangedEvent> processEvent(JavaElementChangedEvent event, IProgressMonitor progressMonitor)
			throws CoreException {
		Logger.debug("Processing {} Java change", event);
		final IJavaElement element = event.getElement();
		final CompilationUnit ast = event.getCompilationUnitAST();
		final int deltaKind = event.getDeltaKind();
		// final int[] flags = event.getFlags();
		final int elementType = event.getElementType();
		// if no metamodel existed for the given project, one is automatically
		// created. Yet, this applies only to project having the JAX-RS Facet
		JaxrsMetamodel metamodel = JaxrsMetamodel.get(element.getJavaProject());
		if (metamodel == null) {
			metamodel = JaxrsMetamodel.create(element.getJavaProject());
		}

		switch (deltaKind) {
		case ADDED:
			switch (elementType) {
			case JAVA_PROJECT:
				return processAddition(element, metamodel, progressMonitor);
			case PACKAGE_FRAGMENT_ROOT:
				return processAddition(element, metamodel, progressMonitor);
			case COMPILATION_UNIT:
				return processAddition((ICompilationUnit) element, ast, metamodel, progressMonitor);
			case TYPE:
				return processAddition((IType) element, ast, metamodel, progressMonitor);
			case METHOD:
				return processAddition((IMethod) element, ast, metamodel, progressMonitor);
			case FIELD:
				return processAddition((IField) element, ast, metamodel, progressMonitor);
			case ANNOTATION:
				return processAddition((IAnnotation) element, ast, metamodel, progressMonitor);
			}
			break;
		case CHANGED:
			switch (elementType) {
			case METHOD:
				return processChange((IMethod) element, ast, metamodel, progressMonitor);
			case ANNOTATION:
				return processChange((IAnnotation) element, ast, metamodel, progressMonitor);
			}
			break;
		case REMOVED:
			switch (elementType) {
			case COMPILATION_UNIT:
				return processRemoval((ICompilationUnit) element, ast, metamodel, progressMonitor);
			case PACKAGE_FRAGMENT_ROOT:
				return processRemoval((IPackageFragmentRoot) element, metamodel, progressMonitor);
			case TYPE:
				return processRemoval((IType) element, ast, metamodel, progressMonitor);
			case METHOD:
				return processRemoval((IMethod) element, ast, metamodel, progressMonitor);
			case ANNOTATION:
				return processRemoval((IAnnotation) element, ast, metamodel, progressMonitor);
			case FIELD:
				return processRemoval((IField) element, ast, metamodel, progressMonitor);
			}
			break;
		}
		return Collections.emptyList();
	}

	/**
	 * Process the addition of a Java Element (can be a JavaProject or a Java
	 * Package Fragment root).
	 * 
	 * @param scope
	 *            the java element that may contain JAX-RS items
	 * @param metamodel
	 *            the metamodel associated with the current Java project
	 * @param progressMonitor
	 *            the progress monitor
	 * @return a list of impacts (ie, JAX-RS elements that where created)
	 * @throws CoreException
	 *             in case of underlying exception
	 */
	private List<JaxrsElementChangedEvent> processAddition(final IJavaElement scope, final JaxrsMetamodel metamodel,
			final IProgressMonitor progressMonitor) throws CoreException {
		final List<JaxrsElementChangedEvent> impacts = new ArrayList<JaxrsElementChangedEvent>();
		if(metamodel.getElement(scope) == null) {
			// process this type as it is not already known from the metamodel
			// let's see if the given project contains JAX-RS HTTP Methods
			final List<IType> matchingHttpMethodTypes = JaxrsAnnotationsScanner.findHTTPMethodTypes(scope, progressMonitor);
			for (IType type : matchingHttpMethodTypes) {
				final CompilationUnit ast = JdtUtils.parse(type, progressMonitor);
				final Annotation annotation = JdtUtils.resolveAnnotation(type, ast, HttpMethod.class);
				final JaxrsHttpMethod httpMethod = factory.createHttpMethod(annotation, ast, metamodel);
				if (httpMethod != null) {
					impacts.add(new JaxrsElementChangedEvent(httpMethod, ADDED));
				}
			}
			// let's see if the given project contains JAX-RS HTTP Resources
			final List<IType> matchingResourceTypes = JaxrsAnnotationsScanner.findResources(scope, progressMonitor);
			for (IType type : matchingResourceTypes) {
				final CompilationUnit ast = JdtUtils.parse(type, progressMonitor);
				final JaxrsResource resource = factory.createResource(type, ast, metamodel);
				if (resource != null) {
					impacts.add(new JaxrsElementChangedEvent(resource, ADDED));
				}
			}
		}

		return impacts;
	}

	private List<JaxrsElementChangedEvent> processAddition(ICompilationUnit element, CompilationUnit ast,
			final JaxrsMetamodel metamodel, IProgressMonitor progressMonitor) throws CoreException {
		final List<JaxrsElementChangedEvent> impacts = new ArrayList<JaxrsElementChangedEvent>();
		for (IType type : element.getTypes()) {
			impacts.addAll(processAddition(type, ast, metamodel, progressMonitor));
		}
		return impacts;
	}

	/**
	 * @param type
	 * @param progressMonitor
	 * @throws CoreException
	 */
	private List<JaxrsElementChangedEvent> processAddition(final IType javaType, final CompilationUnit ast,
			final JaxrsMetamodel metamodel, final IProgressMonitor progressMonitor) throws CoreException {
		final List<JaxrsElementChangedEvent> impacts = new ArrayList<JaxrsElementChangedEvent>();
		// let's see if the given type can be an HTTP Method (ie, is annotated
		// with @HttpMethod)
		final IJaxrsHttpMethod httpMethod = factory.createHttpMethod(javaType, ast, metamodel);
		if (httpMethod != null) {
			impacts.add(new JaxrsElementChangedEvent(httpMethod, ADDED));
		}
		// now,let's see if the given type can be a Resource (with or without
		// @Path)
		final JaxrsResource resource = factory.createResource(javaType, ast, metamodel);
		if (resource != null) {
			impacts.add(new JaxrsElementChangedEvent(resource, ADDED));
		}
		// TODO: now,let's see if the given type can be a Provider

		return impacts;
	}

	private List<JaxrsElementChangedEvent> processAddition(IField javaField, CompilationUnit ast,
			JaxrsMetamodel metamodel, IProgressMonitor progressMonitor) throws CoreException {
		final List<JaxrsElementChangedEvent> impacts = new ArrayList<JaxrsElementChangedEvent>();
		// let's see if the added field has some JAX-RS annotation on itself.
		final JaxrsParamField field = factory.createField(javaField, ast, metamodel);
		if (field != null) {
			impacts.add(new JaxrsElementChangedEvent(field, ADDED));
		}
		return impacts;
	}

	/**
	 * @param type
	 * @param progressMonitor
	 * @throws CoreException
	 */
	private List<JaxrsElementChangedEvent> processAddition(final IMethod javaMethod, final CompilationUnit ast,
			final JaxrsMetamodel metamodel, final IProgressMonitor progressMonitor) throws CoreException {
		final List<JaxrsElementChangedEvent> impacts = new ArrayList<JaxrsElementChangedEvent>();
		final IJaxrsResourceMethod resourceMethod = factory.createResourceMethod(javaMethod, ast, metamodel);
		if (resourceMethod != null) {
			impacts.add(new JaxrsElementChangedEvent(resourceMethod, ADDED));
		}
		return impacts;
	}

	/**
	 * @param javaAnnotation
	 * @param progressMonitor
	 * @throws CoreException
	 */
	private List<JaxrsElementChangedEvent> processAddition(final IAnnotation javaAnnotation, final CompilationUnit ast,
			final JaxrsMetamodel metamodel, final IProgressMonitor progressMonitor) throws CoreException {
		final List<JaxrsElementChangedEvent> impacts = new ArrayList<JaxrsElementChangedEvent>();
		final IJaxrsElement<?> jaxrsElement = metamodel.getElement(javaAnnotation.getParent());
		if (jaxrsElement == null) {
			final List<IJaxrsElement<?>> elements = factory.createElement(javaAnnotation, ast, metamodel);
			for (IJaxrsElement<?> element : elements) {
				if (element != null) {
					impacts.add(new JaxrsElementChangedEvent(element, ADDED));
				}
			}
		} else {
			final Annotation annotation = JdtUtils.resolveAnnotation(javaAnnotation, ast);
			final int flags = jaxrsElement.addOrUpdateAnnotation(annotation);
			if (flags > 0) {
				impacts.add(new JaxrsElementChangedEvent(jaxrsElement, CHANGED, flags));
			}
		}
		return impacts;
	}

	/**
	 * @param javaAnnotation
	 * @param progressMonitor
	 * @throws CoreException
	 */
	// FIXME : same code as method processAddition(annotation, etc..) ?!?
	private List<JaxrsElementChangedEvent> processChange(final IAnnotation javaAnnotation, final CompilationUnit ast,
			final JaxrsMetamodel metamodel, final IProgressMonitor progressMonitor) throws CoreException {
		final List<JaxrsElementChangedEvent> impacts = new ArrayList<JaxrsElementChangedEvent>();
		Annotation annotation = JdtUtils.resolveAnnotation(javaAnnotation, ast);
		if (annotation != null) {
			final IJaxrsElement<?> jaxrsElement = metamodel.getElement(annotation);
			if (jaxrsElement == null) {
				final List<IJaxrsElement<?>> elements = factory.createElement(javaAnnotation, ast, metamodel);
				for (IJaxrsElement<?> element : elements) {
					if (element != null) {
						impacts.add(new JaxrsElementChangedEvent(element, ADDED));
					}
				}
			} else {
				final int flags = jaxrsElement.addOrUpdateAnnotation(annotation);
				if (flags > 0) {
					impacts.add(new JaxrsElementChangedEvent(jaxrsElement, CHANGED, flags));
				}
			}
		}
		return impacts;
	}

	private List<JaxrsElementChangedEvent> processChange(IMethod javaMethod, CompilationUnit ast,
			JaxrsMetamodel metamodel, IProgressMonitor progressMonitor) throws CoreException {
		final List<JaxrsElementChangedEvent> impacts = new ArrayList<JaxrsElementChangedEvent>();
		final IJaxrsElement<?> jaxrsElement = metamodel.getElement(javaMethod);
		if (jaxrsElement != null && jaxrsElement.getElementKind() == EnumElementKind.RESOURCE_METHOD) {
			final int flag = ((JaxrsResourceMethod) jaxrsElement).update(CompilationUnitsRepository.getInstance()
					.getMethodSignature(javaMethod));
			if (flag != F_NONE) {
				impacts.add(new JaxrsElementChangedEvent(jaxrsElement, CHANGED, flag));
			}
		}
		return impacts;
	}

	/**
	 * @param element
	 * @param progressMonitor
	 * @return
	 * @throws CoreException
	 */
	private List<JaxrsElementChangedEvent> processRemoval(IPackageFragmentRoot packageFragmentRoot,
			final JaxrsMetamodel metamodel, IProgressMonitor progressMonitor) throws CoreException {
		final List<JaxrsElementChangedEvent> impacts = new ArrayList<JaxrsElementChangedEvent>();
		final List<IJaxrsElement<?>> elements = metamodel.getElements(packageFragmentRoot);
		for (IJaxrsElement<?> element : elements) {
			metamodel.remove(element);
			impacts.add(new JaxrsElementChangedEvent(element, REMOVED));
		}
		return impacts;
	}

	/**
	 * @param element
	 * @param ast
	 * @param progressMonitor
	 * @return
	 * @throws CoreException
	 */
	private List<JaxrsElementChangedEvent> processRemoval(ICompilationUnit compilationUnit, CompilationUnit ast,
			final JaxrsMetamodel metamodel, IProgressMonitor progressMonitor) throws CoreException {
		final List<JaxrsElementChangedEvent> impacts = new ArrayList<JaxrsElementChangedEvent>();
		final List<IJaxrsElement<?>> elements = metamodel.getElements(compilationUnit);
		for (IJaxrsElement<?> element : elements) {
			metamodel.remove(element);
			CompilationUnitsRepository.getInstance().removeAST(compilationUnit);
			impacts.add(new JaxrsElementChangedEvent(element, REMOVED));
		}
		return impacts;
	}

	/**
	 * @param javaType
	 * @param progressMonitor
	 * @throws CoreException
	 */
	private List<JaxrsElementChangedEvent> processRemoval(final IType javaType, final CompilationUnit ast,
			final JaxrsMetamodel metamodel, final IProgressMonitor progressMonitor) throws CoreException {
		final List<JaxrsElementChangedEvent> impacts = new ArrayList<JaxrsElementChangedEvent>();
		final IJaxrsElement<?> element = metamodel.getElement(javaType);
		// if item does not exist yet, then don't care about the removed type
		if (element != null) {
			metamodel.remove(element);
			impacts.add(new JaxrsElementChangedEvent(element, REMOVED));
		}
		return impacts;
	}

	/**
	 * @param javaAnnotation
	 * @param progressMonitor
	 * @throws CoreException
	 */
	private List<JaxrsElementChangedEvent> processRemoval(final IAnnotation javaAnnotation, final CompilationUnit ast,
			final JaxrsMetamodel metamodel, final IProgressMonitor progressMonitor) throws CoreException {
		final List<JaxrsElementChangedEvent> impacts = new ArrayList<JaxrsElementChangedEvent>();
		final IJaxrsElement<?> element = metamodel.getElement(javaAnnotation);
		if (element != null) {
			// The logic is the same for all the kinds of elements
			final int flag = element.removeAnnotation(javaAnnotation.getHandleIdentifier());
			if (element.getKind() == EnumKind.UNDEFINED) {
				metamodel.remove(element);
				impacts.add(new JaxrsElementChangedEvent(element, REMOVED));
			} else {
				impacts.add(new JaxrsElementChangedEvent(element, CHANGED, flag));
			}
		}
		return impacts;
	}

	private List<JaxrsElementChangedEvent> processRemoval(IField field, CompilationUnit ast, JaxrsMetamodel metamodel,
			IProgressMonitor progressMonitor) {
		final List<JaxrsElementChangedEvent> impacts = new ArrayList<JaxrsElementChangedEvent>();
		final List<IJaxrsElement<?>> elements = metamodel.getElements(field);
		for (IJaxrsElement<?> element : elements) {
			metamodel.remove(element);
			impacts.add(new JaxrsElementChangedEvent(element, REMOVED));
		}
		return impacts;
	}

	private List<JaxrsElementChangedEvent> processRemoval(IMethod method, CompilationUnit ast,
			JaxrsMetamodel metamodel, IProgressMonitor progressMonitor) {
		final List<JaxrsElementChangedEvent> impacts = new ArrayList<JaxrsElementChangedEvent>();
		final List<IJaxrsElement<?>> elements = metamodel.getElements(method);
		for (IJaxrsElement<?> element : elements) {
			if (element.getElementKind() == EnumElementKind.RESOURCE_METHOD) {
				metamodel.remove(element);
				impacts.add(new JaxrsElementChangedEvent(element, REMOVED));
			}
		}
		return impacts;
	}

}