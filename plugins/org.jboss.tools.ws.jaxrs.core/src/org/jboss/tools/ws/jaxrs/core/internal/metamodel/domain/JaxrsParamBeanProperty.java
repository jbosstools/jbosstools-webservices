/**
 * 
 */
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.wst.validation.ValidatorMessage;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsParamBeanProperty;

/**
 * @author xcoulon
 * 
 */
public class JaxrsParamBeanProperty extends JaxrsElement<IMethod> implements IJaxrsParamBeanProperty {

	public JaxrsParamBeanProperty(IMethod element, Annotation annotation, JaxrsMetamodel metamodel) {
		super(element, annotation, metamodel);
	}

	@Override
	public EnumElementKind getElementKind() {
		return EnumElementKind.RESOURCE_FIELD;
	}

	@Override
	public List<ValidatorMessage> validate() {
		List<ValidatorMessage> messages = new ArrayList<ValidatorMessage>();
		return messages;
	}


	@Override
	public EnumKind getKind() {
		return null;
	}

}
