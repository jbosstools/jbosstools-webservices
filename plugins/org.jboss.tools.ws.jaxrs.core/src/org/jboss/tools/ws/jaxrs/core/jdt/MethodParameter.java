package org.jboss.tools.ws.jaxrs.core.jdt;

import java.util.List;

@Deprecated
public class MethodParameter {

	private final String name;
	private final String typeName;
	private final List<MethodParameterAnnotation> annotations;

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the typeName
	 */
	public String getTypeName() {
		return typeName;
	}

	/**
	 * @return the annotations
	 */
	public List<MethodParameterAnnotation> getAnnotations() {
		return annotations;
	}

	public MethodParameter(String name, String typeName, List<MethodParameterAnnotation> annotations) {
		this.name = name;
		this.typeName = typeName;
		this.annotations = annotations;
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
		result = prime * result + ((annotations == null) ? 0 : annotations.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((typeName == null) ? 0 : typeName.hashCode());
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
		MethodParameter other = (MethodParameter) obj;
		if (annotations == null) {
			if (other.annotations != null)
				return false;
		} else if (!annotations.equals(other.annotations))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (typeName == null) {
			if (other.typeName != null)
				return false;
		} else if (!typeName.equals(other.typeName))
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
		return "MethodParameter [name=" + name + ", typeName=" + typeName + ", annotations=" + annotations + "]";
	}

}
