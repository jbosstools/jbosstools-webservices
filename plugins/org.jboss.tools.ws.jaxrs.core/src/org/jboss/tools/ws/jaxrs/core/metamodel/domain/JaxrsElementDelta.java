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
package org.jboss.tools.ws.jaxrs.core.metamodel.domain;

import static org.eclipse.jdt.core.IJavaElementDelta.ADDED;
import static org.eclipse.jdt.core.IJavaElementDelta.CHANGED;
import static org.eclipse.jdt.core.IJavaElementDelta.REMOVED;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.IJavaElementDelta;
import org.jboss.tools.ws.jaxrs.core.internal.utils.ConstantUtils;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.jdt.Flags;

public class JaxrsElementDelta implements Comparable<JaxrsElementDelta> {

	/** Meaning that the change occurred at a finer level. */
	public static final int F_FINE_GRAINED = 0x1;

	public static final int F_ELEMENT_KIND = 0x2;

	public static final int F_SOURCE_TYPE = 0x4;

	public static final int F_PATH_ANNOTATION = 0x8;

	public static final int F_APPLICATION_PATH_ANNOTATION = 0x10;

	public static final int F_APPLICATION_CLASS_NAME = 0x20;

	public static final int F_APPLICATION_PATH_VALUE_OVERRIDE = 0x40;

	public static final int F_APPLICATION_HIERARCHY = 0x80;

	public static final int F_HTTP_METHOD_ANNOTATION = 0x100;

	public static final int F_PATH_PARAM_ANNOTATION = 0x200;

	public static final int F_QUERY_PARAM_ANNOTATION = 0x400;

	public static final int F_MATRIX_PARAM_ANNOTATION = 0x800;

	public static final int F_DEFAULT_VALUE_ANNOTATION = 0x1000;

	public static final int F_CONSUMES_ANNOTATION = 0x2000;

	public static final int F_PRODUCES_ANNOTATION = 0x4000;

	public static final int F_METHOD_PARAMETERS = 0x8000;

	public static final int F_METHOD_RETURN_TYPE = 0x10000;

	public static final int F_TARGET_ANNOTATION = 0x20000;

	public static final int F_RETENTION_ANNOTATION = 0x40000;

	public static final int F_PROVIDER_ANNOTATION = 0x80000;

	public static final int F_ENCODED_ANNOTATION = 0x100000;

	public static final int F_PROVIDER_HIERARCHY = 0x200000;

	public static final int F_NAME_BINDING_ANNOTATION = 0x400000;

	public static final int F_PARAM_CONVERTER_PROVIDER_HIERARCHY = 0x800000;

	public static final int F_BEAN_PARAM_ANNOTATION = 0x1000000;
	
	public static final int F_FORM_PARAM_ANNOTATION = 0x2000000;
	
	public static final int F_HEADER_PARAM_ANNOTATION = 0x4000000;
	
	private final IJaxrsElement element;

	private final int deltaKind;

	private Flags flags;

	/**
	 * Full constructor.
	 * 
	 * @param element
	 * @param deltaKind
	 * @param flags
	 */
	public JaxrsElementDelta(final IJaxrsElement element, final int deltaKind, final int flags) {
		this.element = element;
		this.deltaKind = deltaKind;
		this.flags = new Flags(flags);
		if (this.deltaKind == CHANGED && !this.flags.hasValue()) {
			Logger.debug("*** No flag to describe the change ?!? ***");
		}
	}

	/**
	 * Full constructor.
	 * 
	 * @param element
	 * @param deltaKind
	 * @param flags
	 */
	public JaxrsElementDelta(final IJaxrsElement element, final int deltaKind, final Flags flags) {
		this.element = element;
		this.deltaKind = deltaKind;
		this.flags = flags;
		if (this.deltaKind == CHANGED && !this.flags.hasValue()) {
			Logger.debug("*** No flag to describe the change ?!? ***");
		}
	}

	/**
	 * Returns <code>true</code> if the associated {@link IJaxrsElement} element
	 * is not <code>null</code> and if the associated change flags are not equal
	 * to {@link JaxrsElementDelta.F_NONE} if the kind of change was
	 * {@link IJavaElementDelta.CHANGED}. Returns <code>false<code> otherwise.
	 * 
	 * @return
	 */
	public boolean isRelevant() {
		return this.element != null
				&& (this.deltaKind == ADDED || this.deltaKind == REMOVED || (this.deltaKind == CHANGED && this.flags.hasValue()));
	}

	/** @return the element */
	public IJaxrsElement getElement() {
		return element;
	}

	/**
	 * @return the deltaKind 
	 * @see IJavaElementDelta#ADDED
	 * @see IJavaElementDelta#CHANGED
	 * @see IJavaElementDelta#REMOVED
	 */
	public int getDeltaKind() {
		return deltaKind;
	}

	/** @return the flags */
	public Flags getFlags() {
		return flags;
	}

	/**
	 * Adds the given flag to the current flags unless these particular flags have
	 * already been set.
	 * 
	 * @param flags
	 *            the flags to add.
	 */
	public void addFlag(final Flags flags) {
		this.flags.addFlags(flags);
	}

	/**
	 * Adds the given flag to the current flags unless these particular flags have
	 * already been set.
	 * 
	 * @param flags
	 *            the flags to add.
	 */
	public void addFlag(final int flags) {
		this.flags.addFlags(flags);
	}

	/**
	 * {@inheritDoc} (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append("JaxrsElementChange: ").append(element.toString()).append(" [")
				.append(ConstantUtils.getStaticFieldName(IJavaElementDelta.class, deltaKind)).append("] ");
		try {
			if (flags.hasValue()) {
				List<String> matchFlags = new ArrayList<String>();
				for (Field field : JaxrsElementDelta.class.getFields()) {
					if ((flags.hasValue(field.getInt(field)))) {
						matchFlags.add(ConstantUtils.getStaticFieldName(JaxrsElementDelta.class, field.getInt(field),
								"F_"));
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
		final EnumElementCategory elementKind = this.element.getElementKind().getCategory();
		final EnumElementCategory otherElementKind = other.getElement().getElementKind().getCategory();
		return elementKind.ordinal() - otherElementKind.ordinal();
	}


}
