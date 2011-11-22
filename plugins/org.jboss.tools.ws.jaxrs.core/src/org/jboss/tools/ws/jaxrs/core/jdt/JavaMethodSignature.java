package org.jboss.tools.ws.jaxrs.core.jdt;

import java.util.List;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;

public class JavaMethodSignature implements Comparable<JavaMethodSignature> {

	private final IMethod javaMethod;
	private final IType returnedType;
	private final List<JavaMethodParameter> methodParameters;

	public JavaMethodSignature(IMethod javaMethod, IType returnedType, List<JavaMethodParameter> methodParameters) {
		this.javaMethod = javaMethod;
		this.returnedType = returnedType;
		this.methodParameters = methodParameters;
	}

	/** @return the method */
	public IMethod getJavaMethod() {
		return javaMethod;
	}

	public IType getReturnedType() {
		return returnedType;
	}

	public List<JavaMethodParameter> getMethodParameters() {
		return methodParameters;
	}

	@Override
	public String toString() {
		return "JavaMethodSignature [javaMethod=" + javaMethod + ", returnedTypeName=" + returnedType
				+ ", methodParameters=" + methodParameters + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((javaMethod == null) ? 0 : javaMethod.hashCode());
		result = prime * result + ((methodParameters == null) ? 0 : methodParameters.hashCode());
		result = prime * result + ((returnedType == null) ? 0 : returnedType.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JavaMethodSignature other = (JavaMethodSignature) obj;
		if (javaMethod == null) {
			if (other.javaMethod != null)
				return false;
		} else if (!javaMethod.equals(other.javaMethod))
			return false;
		if (methodParameters == null) {
			if (other.methodParameters != null)
				return false;
		} else if (!methodParameters.equals(other.methodParameters))
			return false;
		if (returnedType == null) {
			if (other.returnedType != null)
				return false;
		} else if (!returnedType.equals(other.returnedType))
			return false;
		return true;
	}

	@Override
	public int compareTo(JavaMethodSignature other) {
		return hashCode() - other.hashCode();
	}
}