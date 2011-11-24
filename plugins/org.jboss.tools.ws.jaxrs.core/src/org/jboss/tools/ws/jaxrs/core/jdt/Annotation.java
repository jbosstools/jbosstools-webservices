package org.jboss.tools.ws.jaxrs.core.jdt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.text.TypedRegion;

public class Annotation {

	private final IAnnotation javaAnnotation;

	private final String javaAnnotationName;

	private final Map<String, List<String>> javaAnnotationElements;

	private TypedRegion region;

	/**
	 * Full constructor
	 * 
	 * @param annotation
	 * @param name
	 * @param annotationElements
	 */
	public Annotation(IAnnotation annotation, String name, Map<String, List<String>> annotationElements,
			final TypedRegion region) {
		this.javaAnnotation = annotation;
		this.javaAnnotationName = name;
		this.javaAnnotationElements = new HashMap<String, List<String>>(annotationElements);
		this.region = region;
	}

	public boolean update(Annotation annotation) {
		assert annotation != null;

		if (this.javaAnnotationElements.equals(annotation.getJavaAnnotationElements())) {
			return false;
		}
		this.javaAnnotationElements.clear();
		this.javaAnnotationElements.putAll(annotation.getJavaAnnotationElements());
		if (annotation.getRegion() != null) {
			this.region = annotation.getRegion();
		}
		return true;
	}

	public IAnnotation getJavaAnnotation() {
		return javaAnnotation;
	}

	public IJavaElement getJavaParent() {
		return javaAnnotation.getParent();
	}

	public String getName() {
		return javaAnnotationName;
	}

	public Map<String, List<String>> getJavaAnnotationElements() {
		return javaAnnotationElements;
	}

	public TypedRegion getRegion() {
		return region;
	}

	/** @return the value */
	public List<String> getValues(String elementName) {
		return javaAnnotationElements.get(elementName);
	}

	/** @return the value */
	public String getValue(String elementName) {
		final List<String> values = javaAnnotationElements.get(elementName);
		if (values != null) {
			assert !(values.size() > 1);
			if (values.size() == 1) {
				return values.get(0);
			}
		}
		return null;
	}

	public boolean match(IAnnotation annotation) {
		if (this.javaAnnotation == null && annotation != null) {
			return false;
		}
		if (this.javaAnnotation != null && annotation == null) {
			return false;
		}
		return this.javaAnnotation.getHandleIdentifier().equals(annotation.getHandleIdentifier());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Annotation [" + javaAnnotationName + " " + javaAnnotationElements + "]";
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
		result = prime * result + ((javaAnnotation == null) ? 0 : javaAnnotation.hashCode());
		result = prime * result + ((javaAnnotationElements == null) ? 0 : javaAnnotationElements.hashCode());
		result = prime * result + ((javaAnnotationName == null) ? 0 : javaAnnotationName.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Annotation other = (Annotation) obj;
		if (javaAnnotationElements == null) {
			if (other.javaAnnotationElements != null) {
				return false;
			}
		} else if (!javaAnnotationElements.equals(other.javaAnnotationElements)) {
			return false;
		}
		if (javaAnnotationName == null) {
			if (other.javaAnnotationName != null) {
				return false;
			}
		} else if (!javaAnnotationName.equals(other.javaAnnotationName)) {
			return false;
		}
		return true;
	}

}
