package org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain;

import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementDelta.F_APPLICATION_PATH_VALUE;

import org.eclipse.core.resources.IResource;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementCategory;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsApplication;

public class JaxrsWebxmlApplication extends JaxrsBaseElement implements IJaxrsApplication {

	private String applicationPath;

	private final IResource webxmlResource;

	/**
	 * Full constructor.
	 * 
	 * @param metamodel
	 */
	public JaxrsWebxmlApplication(String applicationPath, IResource webxmlResource, JaxrsMetamodel metamodel) {
		super(metamodel);
		this.applicationPath = formatApplicationPath(applicationPath);
		this.webxmlResource = webxmlResource;
	}

	@Override
	public String getApplicationPath() {
		return applicationPath;
	}

	public int update(JaxrsWebxmlApplication eventApplication) {
		if (eventApplication != null) {
			String eventApplicationPath = formatApplicationPath(eventApplication.getApplicationPath());
			if ((eventApplicationPath.equals(this.applicationPath))) {
				this.applicationPath = eventApplicationPath;
				return F_APPLICATION_PATH_VALUE;
			}
		}
		return 0;
	}

	private String formatApplicationPath(final String eventApplicationPath) {
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
		JaxrsWebxmlApplication other = (JaxrsWebxmlApplication) obj;
		if (webxmlResource == null && other.webxmlResource != null) {
			return false;
		} else if (webxmlResource != null && other.webxmlResource == null) {
			return false;
		} else if (webxmlResource != null && other.webxmlResource != null && !webxmlResource.getFullPath().equals(other.webxmlResource.getFullPath())) {
			return false;
		}
		return true;
	}

}
