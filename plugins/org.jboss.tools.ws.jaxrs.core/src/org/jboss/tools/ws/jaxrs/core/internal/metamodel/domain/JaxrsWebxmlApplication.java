package org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain;

import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementDelta.F_APPLICATION_PATH_VALUE;

import org.eclipse.core.resources.IResource;
import org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementCategory;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsApplication;

public class JaxrsWebxmlApplication extends JaxrsBaseElement implements IJaxrsApplication {

	private String applicationPath;

	private String javaClassName;

	private final IResource webxmlResource;
	
	/**
	 * Full constructor.
	 * 
	 * @param metamodel
	 */
	public JaxrsWebxmlApplication(final String applicationClassName, final String applicationPath, final IResource webxmlResource, final JaxrsMetamodel metamodel) {
		super(metamodel);
		this.applicationPath = normalizeApplicationPath(applicationPath);
		this.webxmlResource = webxmlResource;
		this.javaClassName = applicationClassName;
		
	}

	/**
	 * @return true if the applicationClassName given in the constructor matches an existing Java Application in the
	 *         metamodel, false otherwise.
	 */
	public boolean isOverride() {
		return (this.javaClassName != null) &&
				!this.javaClassName.equals(EnumJaxrsClassname.APPLICATION.qualifiedName);
	}

	/**
	 * @return the Java application whose underlying Java Type fully qualified name matches the given application class
	 *         name in the constructor, null otherwise.
	 */
	public JaxrsJavaApplication getOverridenJaxrsJavaApplication() {
		return metamodel.getJavaApplication(javaClassName);
	}
	
	@Override
	public String getApplicationPath() {
		return applicationPath;
	}

	public String getJavaClassName() {
		return javaClassName;
	}
	
	
	public int update(JaxrsWebxmlApplication eventApplication) {
		if (eventApplication != null) {
			String eventApplicationPath = normalizeApplicationPath(eventApplication.getApplicationPath());
			if (!(eventApplicationPath.equals(this.applicationPath))) {
				this.applicationPath = eventApplicationPath;
				return F_APPLICATION_PATH_VALUE;
			}
		}
		return 0;
	}

	private String normalizeApplicationPath(final String eventApplicationPath) {
		String path = eventApplicationPath.replace("/*", "/");
		if (path.length() > 1 && path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		return path;
	}

	@Override
	public EnumElementCategory getElementCategory() {
		return EnumElementCategory.APPLICATION;
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
		} else if (webxmlResource != null && other.webxmlResource != null && !webxmlResource.getFullPath().equals(other.webxmlResource.getFullPath())) {
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
		return ("WebxmlApplication '" + javaClassName + "': " + getApplicationPath());
	}


}
