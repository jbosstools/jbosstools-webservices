package org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder;

import org.eclipse.jdt.core.IJavaElement;

@Deprecated
public interface IJavaElementKind extends IJavaElement {

	public static final int UNKNOWN_TYPE = -1;
	public static final int TYPE_ANNOTATION = 17;
	public static final int TYPE_SUPERTYPES = 18;
	public static final int TYPE_PARAMETER = 19;
	public static final int FIELD_ANNOTATION = 20;
	public static final int METHOD_ANNOTATION = 21;
	public static final int METHOD_SIGNATURE = 22;

}
