package org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder;

import java.util.Arrays;
import java.util.EventObject;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.tools.ws.jaxrs.core.internal.utils.ConstantUtils;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;

/**
 * Event raised when a Java Element of interest for the metamodel is changed.
 * 
 * @author xcoulon
 * 
 */
public class JavaElementChangedEvent extends EventObject {

	/** generated serial version UID */
	private static final long serialVersionUID = 8821221398378359798L;

	public static final int[] NO_FLAG = new int[0];

	/** The java element that changed. */
	private final IJavaElement element;

	/** The (detailed) Java Element kind. */
	private final int elementType;

	/** The kind of change. */
	private final int deltaKind;

	/** Some flags to describe more precisely the kind of change. */
	private final int[] flags;

	/**
	 * the compilation unit AST retrieved from the change event, or null if the
	 * event was not of the POST_RECONCILE type.
	 */
	private final CompilationUnit compilationUnitAST;

	/**
	 * Full constructor.
	 * 
	 * @param element
	 *            The java element that changed.
	 * @param compilationUnitAST
	 *            The associated compilation unit AST (or null if it does not
	 *            apply to the given element)
	 * @param elementType
	 *            The (detailed) Java Element kind.
	 * @param deltaKind
	 *            The kind of change.
	 * @param compilationUnitAST
	 *            the associated compilation unit AST
	 * @param flags
	 *            the detailed kind of change.
	 * @see IJavaElementDelta for element change kind values.
	 */
	public JavaElementChangedEvent(IJavaElement element, int deltaKind, CompilationUnit compilationUnitAST, int[] flags) {
		super(element);
		this.element = element;
		this.elementType = element.getElementType();
		this.deltaKind = deltaKind;
		if (element instanceof IMember && deltaKind != IJavaElementDelta.REMOVED) {
			assert compilationUnitAST != null;
		}
		this.compilationUnitAST = compilationUnitAST;
		this.flags = flags;
	}

	/**
	 * @return the element
	 */
	public IJavaElement getElement() {
		return element;
	}

	/**
	 * @return the elementType
	 */
	public int getElementType() {
		return elementType;
	}

	/**
	 * @return the deltaKind
	 */
	public int getDeltaKind() {
		return deltaKind;
	}

	/**
	 * @return the compilationUnitAST
	 */
	public CompilationUnit getCompilationUnitAST() {
		return compilationUnitAST;
	}

	/**
	 * @return the flags
	 */
	public int[] getFlags() {
		return flags;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder("JavaElementChangedEvent [").append(ConstantUtils.getStaticFieldName(
				IJavaElement.class, elementType));
		result.append(" '").append(element.getElementName()).append("' ");
		if (JdtUtils.isWorkingCopy(element)) {
			result.append(" [*working copy*]");
		}
		if (compilationUnitAST != null) {
			result.append("[with AST] ");
		} else {
			result.append("[*without* AST] ");
		}
		result.append(ConstantUtils.getStaticFieldName(IJavaElementDeltaFlag.class, deltaKind).toLowerCase());
		if (flags.length > 0) {
			result.append(":{");
			for (int i = 0; i < flags.length; i++) {
				result.append(ConstantUtils.getStaticFieldName(IJavaElementDeltaFlag.class, flags[i], "F_"));
				if (i < flags.length - 1) {
					result.append("+");
				}
			}
			result.append("}");
		}
		result.append("]");
		return result.toString();
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
		result = prime * result + deltaKind;
		result = prime * result + ((element == null) ? 0 : element.hashCode());
		result = prime * result + elementType;
		result = prime * result + Arrays.hashCode(flags);
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
		JavaElementChangedEvent other = (JavaElementChangedEvent) obj;
		if (deltaKind != other.deltaKind)
			return false;
		if (element == null)
			if (other.element != null)
				return false;
			else if (element.getElementType() != other.element.getElementType()
					|| !element.getElementName().equals(other.element.getElementName()))
				return false;
		if (elementType != other.elementType)
			return false;
		/*
		 * if (compilationUnitAST == null) if (other.compilationUnitAST != null)
		 * return false;
		 */
		if (!Arrays.equals(flags, other.flags))
			return false;
		return true;
	}
}
