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
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder;

import static org.eclipse.jdt.core.IJavaElementDelta.CHANGED;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.IJavaElementDelta;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsBaseElement;
import org.jboss.tools.ws.jaxrs.core.internal.utils.ConstantUtils;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsElement;

public class JaxrsElementDelta implements Comparable<JaxrsElementDelta> {

	/** No change. */
	public static final int F_NONE = 0;

	/** Meaning that the change occurred at a finer level. */
	public static final int F_FINE_GRAINED = 0x1;

	public static final int F_ELEMENT_KIND = 0x2;

	public static final int F_PATH_VALUE = 0x4;

	public static final int F_APPLICATION_PATH_VALUE = 0x8;

	public static final int F_APPLICATION_PATH_VALUE_ORVERRIDE = 0x10;

	public static final int F_APPLICATION_HIERARCHY = 0x20;

	public static final int F_HTTP_METHOD_VALUE = 0x40;

	public static final int F_PATH_PARAM_VALUE = 0x80;

	public static final int F_QUERY_PARAM_VALUE = 0x100;

	public static final int F_MATRIX_PARAM_VALUE = 0x200;

	public static final int F_DEFAULT_VALUE_VALUE = 0x400;

	public static final int F_CONSUMED_MEDIATYPES_VALUE = 0x800;

	public static final int F_PRODUCED_MEDIATYPES_VALUE = 0x1000;

	public static final int F_METHOD_PARAMETERS = 0x2000;

	public static final int F_METHOD_RETURN_TYPE = 0x4000;

	public static final int F_TARGET_VALUE = 0x8000;
	
	public static final int F_RETENTION_VALUE = 0x10000;

	private final IJaxrsElement element;
	
	private final int deltaKind;
	
	private final int flags;
	
	/**
	 * Full constructor.
	 * 
	 * @param element
	 * @param deltaKind
	 */
	public JaxrsElementDelta(IJaxrsElement element, int deltaKind) {
		this(element, deltaKind, 0);
	}

	/**
	 * Full constructor.
	 * 
	 * @param element
	 * @param deltaKind
	 * @param flags
	 */
	public JaxrsElementDelta(IJaxrsElement element, int deltaKind, int flags) {
		this.element = element;
		this.deltaKind = deltaKind;
		this.flags = flags;
		if (this.deltaKind == CHANGED && this.flags == F_NONE) {
			Logger.debug("*** No flag to describe the change ?!? ***");
		}
	}

	/** @return the element */
	public IJaxrsElement getElement() {
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

	/**
	 * {@inheritDoc} (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append("JaxrsElementChange: [").append(ConstantUtils.toCamelCase(element.getElementCategory().toString()))
				.append(" ").append(ConstantUtils.getStaticFieldName(IJavaElementDelta.class, deltaKind)).append("] ")
				.append(element.getName());

		try {
			if (flags != F_NONE) {
				List<String> matchFlags = new ArrayList<String>();
				for (Field field : JaxrsElementDelta.class.getFields()) {
					if ((flags & field.getInt(field)) > 0) {
						matchFlags.add(ConstantUtils.getStaticFieldName(JaxrsElementDelta.class,
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

	@Override
	public int compareTo(JaxrsElementDelta other) {
		final EnumElementKind elementKind = this.element.getElementKind();
		final EnumElementKind otherElementKind = other.getElement().getElementKind();
		return elementKind.ordinal() - otherElementKind.ordinal();
	}

}
