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

/**
 * Utility class that carries flags that are used to qualify an element change.
 * The underlying flags' values are supposed to be n^2 based values, which means
 * that two different flags can be summed up and retrieved afterwards. Yet,
 * adding twice the same flag should not be permitted and this is what this
 * utility class is all about.
 * 
 * @author xcoulon
 * 
 * 
 */
public class Flags {

	int flags;

	/**
	 * Default constructor. Starting with <code>flags=0</code>.
	 */
	public Flags() {
		this.flags = 0;
	}

	/**
	 * Default constructor with a given initial <code>flags</code> value.
	 * 
	 * @param flags
	 */
	public Flags(final int flags) {
		this.flags = flags;
	}

	/**
	 * @return the flags value.
	 */
	public int getValue() {
		return flags;
	}

	/**
	 * Sets the flags value by avoiding duplication.
	 * 
	 * @param otherFlags
	 *            the flags to add.
	 */
	public void addFlags(final int otherFlags) {
		int i = 1;
		while (i <= otherFlags) {
			if ((this.flags & i) == 0 && (otherFlags & i) != 0) {
				this.flags += i;
			}
			i = i<<1;
		}
	}

	/**
	 * Sets the flags value by avoiding duplication.
	 * 
	 * @param otherFlags
	 *            the flags to add.
	 */
	public void addFlags(final Flags otherFlags) {
		if (otherFlags != null) {
			addFlags(otherFlags.getValue());
		}
	}

	/**
	 * Returns
	 * <code>true<code> if the internal flags have been set to a value bigger than <code>zero</code>
	 * , false otherwise.
	 * 
	 * @return true or false.
	 */
	public boolean hasValue() {
		return this.flags > 0;
	}

	/**
	 * @return {@code true} if the internal flags have been set to at least one
	 *         of the given values , {@code false} otherwise.
	 * @param values the values to check 
	 */
	public boolean hasValue(int... values) {
		for(int value : values) {
			if((this.flags & value) > 0) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return Integer.toString(flags);
	}
}
