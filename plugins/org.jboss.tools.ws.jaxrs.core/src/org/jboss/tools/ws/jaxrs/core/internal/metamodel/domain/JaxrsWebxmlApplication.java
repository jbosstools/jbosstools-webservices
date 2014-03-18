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

package org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain;

import static org.eclipse.jdt.core.IJavaElementDelta.CHANGED;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_APPLICATION_CLASS_NAME;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_APPLICATION_PATH_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_NONE;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.APPLICATION;

import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsApplication;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta;
import org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames;
import org.jboss.tools.ws.jaxrs.core.utils.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.utils.WtpUtils;

/**
 * JAX-RS Application defined as part of a web deployment descriptor.
 * 
 * @author xcoulon
 *
 */
public class JaxrsWebxmlApplication extends AbstractJaxrsBaseElement implements IJaxrsApplication {

	/**
	 * Initialize the JaxrsWebxmlApplication builder with the given {@link IResource}
	 * @param resource the underlying web.xml resource file.
	 * @return
	 */
	public static Builder from(final IResource resource) {
		return new Builder(resource);
	}

	/**
	 * Fluent Builder.
	 * 
	 * @author xcoulon
	 * 
	 */
	public static class Builder {

		private final IResource webxmlResource;
		private final IJavaProject javaProject;
		private JaxrsMetamodel metamodel;
		private String javaClassName;
		private String applicationPath;

		public Builder(final IResource webxmlResource) {
			this.webxmlResource = webxmlResource;
			this.javaProject = JavaCore.create(webxmlResource.getProject());
		}

		public Builder inMetamodel(final JaxrsMetamodel metamodel) {
			this.metamodel = metamodel;
			return this;
		}

		public JaxrsWebxmlApplication build() throws CoreException {
			final long start = System.currentTimeMillis();
			try {
				final IType applicationType = JdtUtils.resolveType(APPLICATION, javaProject,
						new NullProgressMonitor());
				// occurs when the project has the jax-rs nature (the builder is
				// called), but no jaxrs library is in the classpath
				if (applicationType == null) {
					return null;
				}
				final List<IType> applicationClasses = JdtUtils.findSubtypes(applicationType);
				// web.xml-based JAX-RS applications declared in the web deployment
				// descriptor
				for (IType applicationClass : applicationClasses) {
					javaClassName = applicationClass.getFullyQualifiedName();
					applicationPath = WtpUtils.getApplicationPath(webxmlResource, javaClassName);
					if (applicationPath != null) {
						final JaxrsWebxmlApplication webxmlApplication = new JaxrsWebxmlApplication(this);
						webxmlApplication.joinMetamodel();
						final JaxrsJavaApplication overridenJaxrsJavaApplication = webxmlApplication.getOverridenJaxrsJavaApplication();
						if(overridenJaxrsJavaApplication != null) {
							overridenJaxrsJavaApplication.setApplicationPathOverride(webxmlApplication.getApplicationPath());
						}
						return webxmlApplication;
					}
				}
				// no match found
				return null;
			} finally {
				final long end = System.currentTimeMillis();
				Logger.tracePerf("Built JAX-RS WebXmlApplication in {}ms", (end - start));
			}
		}
	}

	/** The application path defined by the servlet mapping. */
	private String applicationPath;

	/** The Application class name, defined by the servlet name. */
	private String javaClassName;

	/** The underlying web.xml resource. */
	private final IResource webxmlResource;

	/**
	 * Full constructor.
	 * 
	 */
	private JaxrsWebxmlApplication(final Builder builder) {
		super(builder.metamodel);
		this.applicationPath = normalizeApplicationPath(builder.applicationPath);
		this.javaClassName = builder.javaClassName;
		this.webxmlResource = builder.webxmlResource;
	}

	@Override
	public boolean isBinary() {
		final IJavaProject javaProject = getMetamodel().getJavaProject();
		IPackageFragmentRoot fragment = javaProject.getPackageFragmentRoot(webxmlResource);
		if (fragment != null && fragment.exists() && fragment.isArchive()) {
			return true;
		}
		return false;
	}

	@Override
	public boolean isWebXmlApplication() {
		return true;
	}

	@Override
	public boolean isJavaApplication() {
		return false;
	}

	/**
	 * @return true if the applicationClassName given in the constructor matches
	 *         an existing Java Application in the metamodel, false otherwise.
	 */
	public boolean isOverride() {
		return (this.javaClassName != null) && !this.javaClassName.equals(JaxrsClassnames.APPLICATION);
	}

	/**
	 * @return the Java application whose underlying Java Type fully qualified
	 *         name matches the given application class name in the constructor,
	 *         null otherwise.
	 */
	public JaxrsJavaApplication getOverridenJaxrsJavaApplication() {
		if (getMetamodel() != null) {
			return getMetamodel().findJavaApplicationByTypeName(javaClassName);
		}
		return null;
	}

	@Override
	public String getApplicationPath() {
		return applicationPath;
	}

	public String getJavaClassName() {
		return javaClassName;
	}
	
	public int update(IResource webxmlResource) throws CoreException {
		return update(from(webxmlResource).build());
	}


	public int update(JaxrsWebxmlApplication transientWebXmlAppl) throws CoreException {
		int flags = F_NONE;
		if(transientWebXmlAppl == null) {
			remove();
		} else {
			final JaxrsElementDelta delta = new JaxrsElementDelta(this, CHANGED);
			final String eventApplicationPath = normalizeApplicationPath(transientWebXmlAppl.getApplicationPath());
			if (!(eventApplicationPath.equals(this.applicationPath))) {
				final JaxrsJavaApplication currentJavaApplication = getMetamodel().findJavaApplicationByTypeName(javaClassName);
				if(currentJavaApplication != null) {
					currentJavaApplication.setApplicationPathOverride(eventApplicationPath);
				}	
				this.applicationPath = eventApplicationPath;
				delta.addFlag(F_APPLICATION_PATH_ANNOTATION);
			}
			final String eventJavaClassName = transientWebXmlAppl.getJavaClassName();
			if (!(eventJavaClassName.equals(this.javaClassName))) {
				final JaxrsJavaApplication previousJavaApplication = getMetamodel().findJavaApplicationByTypeName(javaClassName);
				if(previousJavaApplication != null) {
					previousJavaApplication.unsetApplicationPathOverride();
				}	
				final JaxrsJavaApplication nextJavaApplication = getMetamodel().findJavaApplicationByTypeName(eventJavaClassName);
				if(nextJavaApplication != null) {
					nextJavaApplication.setApplicationPathOverride(applicationPath);
				}	
				this.javaClassName = eventJavaClassName;
				delta.addFlag(F_APPLICATION_CLASS_NAME);
			}
			if(hasMetamodel()) {
				getMetamodel().update(delta);
			}
		}
		return flags;
	}
	
	private String normalizeApplicationPath(final String eventApplicationPath) {
		String path = eventApplicationPath.replace("/*", "/");
		if (path.length() > 1 && path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		return path;
	}
	
	

	@Override
	public void remove() throws CoreException {
		final JaxrsJavaApplication overridenJaxrsJavaApplication = getOverridenJaxrsJavaApplication();
		if(overridenJaxrsJavaApplication != null) {
			overridenJaxrsJavaApplication.unsetApplicationPathOverride();
		}
		super.remove();
	}

	/**
	 * @return {@code true} if this element should be removed (ie, it does not meet the requirements to be a {@link JaxrsWebxmlApplication} anymore) 
	 */
	@Override
	boolean isMarkedForRemoval() {
		return false;
	}

	@Override
	public EnumElementKind getElementKind() {
		return EnumElementKind.APPLICATION_WEBXML;
	}

	@Override
	public IResource getResource() {
		return webxmlResource;
	}

	@Override
	public String getName() {
		return webxmlResource != null ? webxmlResource.getName() : "*unknown resource*";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((webxmlResource == null) ? 0 : webxmlResource.getFullPath().hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		// compare resource location
		JaxrsWebxmlApplication other = (JaxrsWebxmlApplication) obj;
		if (webxmlResource == null && other.webxmlResource != null) {
			return false;
		} else if (webxmlResource != null && other.webxmlResource == null) {
			return false;
		} else if (webxmlResource != null && other.webxmlResource != null
				&& !webxmlResource.getFullPath().equals(other.webxmlResource.getFullPath())) {
			return false;
		}
		// compare java class name
		if (javaClassName == null && other.javaClassName != null) {
			return false;
		} else if (javaClassName != null && other.javaClassName == null) {
			return false;
		} else if (javaClassName != null && other.javaClassName != null && !javaClassName.equals(other.javaClassName)) {
			return false;
		}
		// don't compare application path, this is something that can change

		//
		return true;
	}

	@Override
	public String toString() {
		return ("WebxmlApplication '" + javaClassName + "' -> " + applicationPath);
	}

	@Override
	public String getIdentifier() {
		return getResource().getFullPath().toPortableString();
	}


}
