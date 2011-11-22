package org.jboss.tools.ws.jaxrs.core.internal.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Collections utility class.
 * 
 * @author xcoulon */
public class CollectionUtils {

	/** Private constructor of the utility class. */
	private CollectionUtils() {

	}

	/** Compares two lists and returns the elements of the given 'test' list that
	 * are not equal to their counterpart (ie, same index) in the 'control'
	 * list. <b>Warning</b> : This method does not return diffs for elements of
	 * the 'control' list that are not in the 'test' list.
	 * 
	 * @param source
	 * @param target */
	public static <T extends Comparable<T>> List<T> compare(List<T> test, List<T> control) {

		ArrayList<T> diffs = new ArrayList<T>();
		if (test == null && control == null) {
			// do nothing
		} else if (test == null && control != null) {
			diffs.addAll(control);
		} else if (test != null && control == null) {
			diffs.addAll(test);
		} else {
			// Collections.sort(test);
			// Collections.sort(control);
			int min = Math.min(test.size(), control.size());
			for (int i = 0; i < min; i++) {
				if (!test.get(i).equals(control.get(i))) {
					diffs.add(test.get(i));
				}
			}
			if (test.size() > control.size()) {
				diffs.addAll(test.subList(min, test.size()));
			} else {
				diffs.addAll(control.subList(min, control.size()));
			}
		}

		return diffs;
	}

	/** Converts a list of elements into an array of java.lang.String, using the
	 * toString() method of the given elements.
	 * 
	 * @param list
	 * @return */
	public static String[] toArray(List<? extends Object> list) {
		String[] values = new String[list.size()];
		for (int i = 0; i < list.size(); i++) {
			values[i] = list.get(i).toString();
		}
		return values;
	}

	/** Convert the given value(s) into a list of java.lang.String objects.
	 * 
	 * @param values
	 *            the value(s) to convert. The value can be a single string of
	 *            an array of strings.
	 * @return the list of strings */
	public static List<String> asStringList(final Object values) {
		if (values == null) {
			return null;
		}
		if (values instanceof String) {
			return Arrays.asList((String) values);
		}
		List<String> list = new ArrayList<String>();
		for (Object value : (Object[]) values) {
			list.add((String) value);
		}
		return list;
	}

}
