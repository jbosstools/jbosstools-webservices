package org.jboss.tools.ws.jaxrs.core.internal.metamodel.validation;

public class JaxrsMarkerResolutionIds {

	/** QuickFix for missing <code>@Retention</code> annotation.*/
	public static final int HTTP_METHOD_MISSING_RETENTION_ANNOTATION_QUICKFIX_ID = 1;
	
	/** QuickFix for invalid <code>@Retention</code> annotation value.*/
	public static final int HTTP_METHOD_INVALID_RETENTION_ANNOTATION_VALUE_QUICKFIX_ID = 2;
	
	/** QuickFix for missing <code>@Target</code> annotation.*/
	public static final int HTTP_METHOD_MISSING_TARGET_ANNOTATION_QUICKFIX_ID = 3;
	
	/** QuickFix for invalid <code>@Target</code> annotation value.*/
	public static final int HTTP_METHOD_INVALID_TARGET_ANNOTATION_VALUE_QUICKFIX_ID = 4;
	
	/** QuickFix for missing <code>@ApplicationPath</code> annotation.*/
	public static final int JAVA_APPLICATION_MISSING_APPLICATION_PATH_ANNOTATION_QUICKFIX_ID = 5;
	
	/** QuickFix for invalid <code>javax.ws.rs.core.Application</code> subclass hierarchy .*/
	public static final int JAVA_APPLICATION_INVALID_TYPE_HIERARCHY_QUICKFIX_ID = 6;

	/**
	 * Private constructor for the utility class.
	 */
	private JaxrsMarkerResolutionIds() {
		super();
	}
}
