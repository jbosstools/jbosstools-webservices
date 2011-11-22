package org.jboss.tools.ws.jaxrs.core.metamodel;

import java.util.List;

import org.eclipse.jdt.core.IMember;
import org.eclipse.wst.validation.ValidatorMessage;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;

public interface IJaxrsElement<T extends IMember> {

	IJaxrsMetamodel getMetamodel();

	T getJavaElement();

	EnumElementKind getElementKind();

	List<Annotation> getAnnotations();

	boolean hasErrors();

	/** @param annotation
	 * @return the flag(s) that indicate the nature of the change */
	int addOrUpdateAnnotation(Annotation annotation);

	int removeAnnotation(String handleIdentifier);

	EnumKind getKind();

	List<ValidatorMessage> validate();

}
