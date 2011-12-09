package org.jboss.tools.ws.jaxrs.core.internal.utils;

import java.lang.reflect.Field;

public class ConstantUtils {

	public static String getStaticFieldName(final Class<?> clazz, final int value) {
		return getStaticFieldName(clazz, value, "");
	}

	public static String getStaticFieldName(final Class<?> clazz, final int value, final String fieldPrefix) {
		for (Field field : clazz.getFields()) {
			try {
				Object f = field.get(null);
				if (f instanceof Integer && field.getName().startsWith(fieldPrefix)
						&& ((Integer) f).intValue() == value) {
					return toCamelCase(field.getName().substring(fieldPrefix.length()));
				}
			} catch (IllegalArgumentException e) {
				System.err.println("Unable to retrieve field name from value " + value + ": " + e.getMessage());
				return "**error**";
			} catch (IllegalAccessException e) {
				System.err.println("Unable to retrieve field name from value " + value + ": " + e.getMessage());
				return "**error**";
			}
		}
		return "** " + value + " **";
	}

	public static int[] splitConstants(final Class<?> clazz, final int flags) {
		return splitConstants(clazz, flags, "");
	}

	public static int[] splitConstants(final Class<?> clazz, final int flags, final String fieldPrefix) {
		int[] constants = new int[100];
		int size = 0;
		for (Field field : clazz.getFields()) {
			try {
				int value = field.getInt(null);
				if (field.getName().startsWith(fieldPrefix) && (value & flags) != 0) {
					constants[size] = value;
					size++;
				}
			} catch (IllegalArgumentException e) {
				System.err.println("Unable to retrieve fields from value " + flags + ": " + e.getMessage());
			} catch (IllegalAccessException e) {
				System.err.println("Unable to retrieve fields from flags " + flags + ": " + e.getMessage());
			}
		}
		int[] result = new int[size];
		System.arraycopy(constants, 0, result, 0, size);
		return result;
	}

	public static String toCamelCase(String s) {
		String[] parts = s.split("_");
		String camelCaseString = "";
		for (String part : parts) {
			camelCaseString = camelCaseString + toProperCase(part);
		}
		return camelCaseString;
	}

	static String toProperCase(String s) {
		return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
	}
}
