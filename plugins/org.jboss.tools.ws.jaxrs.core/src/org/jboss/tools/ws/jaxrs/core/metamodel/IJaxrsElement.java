package org.jboss.tools.ws.jaxrs.core.metamodel;

import org.eclipse.core.resources.IResource;

public interface IJaxrsElement {
	
	public abstract IJaxrsMetamodel getMetamodel();

	public abstract EnumElementKind getElementKind();

	public abstract EnumElementCategory getElementCategory();
	
	public abstract IResource getResource();
	
	public abstract String getName();	


}
