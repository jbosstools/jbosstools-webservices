package org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder;

import static org.eclipse.jdt.core.IJavaElementDelta.CHANGED;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.IJavaElementDelta;
import org.jboss.tools.ws.jaxrs.core.internal.utils.ConstantUtils;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsElement;

public class JaxrsElementChangedEvent extends EventObject {

	/** serialVersionUID */
	private static final long serialVersionUID = -1674380823340833954L;

	private final IJaxrsElement<?> element;

	private final int deltaKind;

	private final int flags;

	public static final int F_NONE = 0;

	public static final int F_ELEMENT_KIND = 1;

	public static final int F_PATH_VALUE = 2;

	public static final int F_HTTP_METHOD_VALUE = 4;

	public static final int F_PATH_PARAM_VALUE = 8;

	public static final int F_QUERY_PARAM_VALUE = 16;

	public static final int F_MATRIX_PARAM_VALUE = 32;

	public static final int F_CONSUMED_MEDIATYPES_VALUE = 64;

	public static final int F_PRODUCED_MEDIATYPES_VALUE = 128;

	public static final int F_METHOD_PARAMETERS = 256;

	public static final int F_METHOD_RETURN_TYPE = 512;

	/** Full constructor.
	 * 
	 * @param element
	 * @param deltaKind */
	public JaxrsElementChangedEvent(IJaxrsElement<?> element, int deltaKind) {
		this(element, deltaKind, 0);
	}

	/** Full constructor.
	 * 
	 * @param element
	 * @param deltaKind
	 * @param flags */
	public JaxrsElementChangedEvent(IJaxrsElement<?> element, int deltaKind, int flags) {
		super(element);
		this.element = element;
		this.deltaKind = deltaKind;
		this.flags = flags;
		if (this.deltaKind == CHANGED && this.flags == F_NONE) {
			Logger.debug("*** No flag to describe the change ?!? ***");
		}
	}

	/** @return the element */
	public IJaxrsElement<?> getElement() {
		return element;
	}

	/** @return the deltaKind */
	public int getDeltaKind() {
		return deltaKind;
	}

	/** @return the flags */
	public int getFlags() {
		return flags;
	}

	/** {@inheritDoc} (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString() */
	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append("JaxrsElementChange: [").append(ConstantUtils.getStaticFieldName(IJavaElementDelta.class, deltaKind))
				.append("] ").append(element.getJavaElement().getElementName());

		try {
			if (flags != F_NONE) {
				List<String> matchFlags = new ArrayList<String>();
				for (Field field : JaxrsElementChangedEvent.class.getFields()) {
					if ((flags & field.getInt(field)) > 0) {
						matchFlags.add(ConstantUtils.getStaticFieldName(JaxrsElementChangedEvent.class,
								field.getInt(field), "F_"));
					}
				}
				s.append(":{");
				for (Iterator<String> iterator = matchFlags.iterator(); iterator.hasNext();) {
					String flag = iterator.next();
					s.append(flag);
					if (iterator.hasNext()) {
						s.append("+");
					}
				}
				s.append("}");
			}
		} catch (IllegalArgumentException e) {
		} catch (IllegalAccessException e) {
		}

		return s.toString();
	}
}
