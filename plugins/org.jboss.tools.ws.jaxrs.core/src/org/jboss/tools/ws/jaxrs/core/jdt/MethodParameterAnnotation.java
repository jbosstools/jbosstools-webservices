package org.jboss.tools.ws.jaxrs.core.jdt;

import org.eclipse.jface.text.TypedRegion;

@Deprecated
public class MethodParameterAnnotation {

	private final String annotationTypeName;
	private final String annotationValue;
	private final TypedRegion region;

	public MethodParameterAnnotation(String annotationTypeName, String annotationValue, TypedRegion region) {
		this.annotationTypeName = annotationTypeName;
		this.annotationValue = annotationValue;
		this.region = region;
	}

	/**
	 * @return the annotationTypeName
	 */
	public String getAnnotationTypeName() {
		return annotationTypeName;
	}

	/**
	 * @return the annotationValue
	 */
	public String getAnnotationValue() {
		return annotationValue;
	}

	/**
	 * @return the region
	 */
	public TypedRegion getRegion() {
		return region;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((annotationTypeName == null) ? 0 : annotationTypeName.hashCode());
		result = prime * result + ((annotationValue == null) ? 0 : annotationValue.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MethodParameterAnnotation other = (MethodParameterAnnotation) obj;
		if (annotationTypeName == null) {
			if (other.annotationTypeName != null)
				return false;
		} else if (!annotationTypeName.equals(other.annotationTypeName))
			return false;
		if (annotationValue == null) {
			if (other.annotationValue != null)
				return false;
		} else if (!annotationValue.equals(other.annotationValue))
			return false;
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "MethodParameterAnnotation [annotationTypeName=" + annotationTypeName + ", annotationValue="
				+ annotationValue + "]";
	}

}
