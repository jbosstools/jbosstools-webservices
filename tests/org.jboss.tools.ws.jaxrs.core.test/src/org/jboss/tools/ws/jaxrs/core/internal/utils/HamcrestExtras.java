/**
 * 
 */
package org.jboss.tools.ws.jaxrs.core.internal.utils;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.Flags;

/**
 * Utility class to provide with custom {@link Matcher} for the test cases
 * @author xcoulon
 *
 */
public class HamcrestExtras {

	/**
	 * @return a values matcher for {@link Flags}
	 * @param values the values to look for in the {@link Flags} that will be tested.
	 * 
	 */
	public static Matcher<Flags> flagMatches(final int... values) {
		return new BaseMatcher<Flags>() {

			@Override
			public boolean matches(Object item) {
				if(item instanceof Flags) {
					return ((Flags)item).hasValue(values);
				}
				return false;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("Flags contains one or more of " + values);
				
			}

		};
	}
}
