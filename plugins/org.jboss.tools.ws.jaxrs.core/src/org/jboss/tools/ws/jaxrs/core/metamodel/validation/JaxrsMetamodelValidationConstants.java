package org.jboss.tools.ws.jaxrs.core.metamodel.validation;

public class JaxrsMetamodelValidationConstants {

	/** The custom 'JAX-RS Problem' marker type. */
	public static final String JAXRS_PROBLEM_TYPE = "org.jboss.tools.ws.jaxrs.metamodelMarker";

	/** QuickFix for missing <code>@Retention</code> annotation.*/
	public static final int HTTP_METHOD_MISSING_RETENTION_ANNOTATION_ID = 1;
	
	/** QuickFix for invalid <code>@Retention</code> annotation value.*/
	public static final int HTTP_METHOD_INVALID_RETENTION_ANNOTATION_VALUE_ID = 2;
	
	/** QuickFix for missing <code>@Target</code> annotation.*/
	public static final int HTTP_METHOD_MISSING_TARGET_ANNOTATION_ID = 3;
	
	/** QuickFix for invalid <code>@Target</code> annotation value.*/
	public static final int HTTP_METHOD_INVALID_TARGET_ANNOTATION_VALUE_ID = 4;
	
	/** QuickFix for missing <code>@ApplicationPath</code> annotation.*/
	public static final int JAVA_APPLICATION_MISSING_APPLICATION_PATH_ANNOTATION_ID = 5;
	
	/** QuickFix for invalid <code>javax.ws.rs.core.Application</code> subclass hierarchy .*/
	public static final int JAVA_APPLICATION_INVALID_TYPE_HIERARCHY_ID = 6;

}
