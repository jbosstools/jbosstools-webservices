package org.jboss.tools.ws.jaxrs.core.jdt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;

public class JavaMethodSignaturesVisitor extends ASTVisitor {

	private final ICompilationUnit compilationUnit;

	private final List<JavaMethodSignature> methodSignatures = new ArrayList<JavaMethodSignature>();

	public JavaMethodSignaturesVisitor(ICompilationUnit compilationUnit2) {
		this.compilationUnit = compilationUnit2;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse
	 * .jdt.core.dom.MethodDeclaration)
	 */
	@Override
	public boolean visit(MethodDeclaration declaration) {
		try {
			IMethod method = (IMethod) compilationUnit.getElementAt(declaration.getStartPosition());
			final IMethodBinding methodBinding = declaration.resolveBinding();
			// sometimes, the binding cannot be resolved
			if (methodBinding == null) {
				Logger.warn("Could not resolve bindings form method " + method.getElementName());
			} else {
				final IType returnedType = methodBinding.getReturnType() != null ? (IType) methodBinding
						.getReturnType().getJavaElement() : null;
				final ITypeBinding[] parameterTypeBindings = methodBinding.getParameterTypes();
				List<JavaMethodParameter> methodParameters = new ArrayList<JavaMethodParameter>();
				@SuppressWarnings("unchecked")
				List<SingleVariableDeclaration> parameters = declaration.parameters();
				for (int i = 0; i < parameterTypeBindings.length; i++) {
					final ITypeBinding parameterTypeBinding = parameterTypeBindings[i];
					final String paramTypeName = parameterTypeBinding.getQualifiedName();
					final String paramName = parameters.get(i).getName().getFullyQualifiedName();
					final List<Annotation> paramAnnotations = new ArrayList<Annotation>();
					final IAnnotationBinding[] parameterAnnotationBindings = methodBinding.getParameterAnnotations(i);
					for (IAnnotationBinding parameterAnnotationBinding : parameterAnnotationBindings) {
						final String annotationName = parameterAnnotationBinding.getAnnotationType().getBinaryName();
						// ((IType)parameterAnnotationBinding.getAnnotationType().getJavaElement()).getF
						final Map<String, List<String>> annotationElements = resolveAnnotationElements(parameterAnnotationBinding);
						// method parameters are not eligible java elements...
						paramAnnotations.add(new Annotation(null, annotationName, annotationElements));
					}
					methodParameters.add(new JavaMethodParameter(paramName, paramTypeName, paramAnnotations));
				}
				// TODO : add support for thrown exceptions
				methodSignatures.add(new JavaMethodSignature(method, returnedType, methodParameters));
			}
		} catch (JavaModelException e) {
			Logger.error("Failed to analyse compilation unit methods", e);
		}
		return true;
	}

	private static Map<String, List<String>> resolveAnnotationElements(IAnnotationBinding annotationBinding) {
		final Map<String, List<String>> annotationElements = new HashMap<String, List<String>>();
		for (IMemberValuePairBinding binding : annotationBinding.getAllMemberValuePairs()) {
			final List<String> values = new ArrayList<String>();
			if (binding.getValue() instanceof Object[]) {
				for (Object v : (Object[]) binding.getValue()) {
					values.add(v.toString());
				}
			} else {
				values.add(binding.getValue().toString());
			}
			annotationElements.put(binding.getName(), values);
		}
		return annotationElements;
	}

	/** @return the methodDeclarations */
	public List<JavaMethodSignature> getMethodSignatures() {
		return methodSignatures;
	}
}